package jrewriter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;
import java.util.stream.*;
import javassist.*;
import javassist.bytecode.*;
import javassist.expr.*;

import static jrewriter.BytecodeSeqMatcher.Exactly;
import static jrewriter.BytecodeSeqMatcher.Matcher;
import static jrewriter.BytecodeSeqMatcher.Or;
import static jrewriter.BytecodeSeqMatcher.Pairs;
import static jrewriter.BytecodeSeqMatcher.Skip;


public class IncrementRewriter extends Rewriter {
    final Logger logger = LoggerFactory.getLogger(IncrementRewriter.class);

    final int AIINCSTATIC = 203;
    final int AIINCFIELD  = 204;
    boolean useAiinc = false;

    private static final String getUnsafeCode =
        "java.lang.reflect.Field $theUnsafeField;\n" +
        "try {\n" +
        "    $theUnsafeField = sun.misc.Unsafe.class.getDeclaredField(\"theUnsafe\");\n" +
        "} catch (NoSuchFieldException ex) {\n" +
        "    ex.printStackTrace();\n" +
        "    throw new Error(ex);\n" +
        "}\n" +
        "$theUnsafeField.setAccessible(true);\n" +
        "try {\n" +
        "    $theUnsafe = (sun.misc.Unsafe) $theUnsafeField.get(null);\n" +
        "} catch (IllegalAccessException ex) {\n" +
        "    ex.printStackTrace();\n" +
        "    throw new Error(ex);\n" +
        "}\n";

    private static final String getOffsetCode =
        "java.lang.reflect.Field field${name};\n" +
        "try {\n" +
        "    field${name} = {class}.class.getDeclaredField(\"{name}\");\n" +
        "} catch (NoSuchFieldException ex) {\n" +
        "    ex.printStackTrace();\n" +
        "    throw new Error(ex);\n" +
        "}\n";

    // sequence of bytecode that does increment
    final Matcher[] incrementMatcher = {
        Or(Exactly(Opcode.DUP), Skip),
        Or(Opcode.GETFIELD, Opcode.GETSTATIC),
        // TODO: support increment result
        // Or(Exactly(Opcode.DUP_X1), Skip),
        Or(Opcode.ICONST_0, Opcode.ICONST_1,
           Opcode.ICONST_2, Opcode.ICONST_3,
           Opcode.ICONST_4, Opcode.ICONST_5, Opcode.ICONST_M1),
        Exactly(Opcode.IADD),
        // Or(Exactly(Opcode.DUP_X1), Skip),
        // matches PUTFIELD if 3 bytecode before is GETFIELD
        // matches PUTSTATIC if 3 bytecode before is GETSTATIC
        Pairs(3, Opcode.GETFIELD,  Opcode.PUTFIELD,
              3, Opcode.GETSTATIC, Opcode.PUTSTATIC)
    };


    public IncrementRewriter(ClassPool pool, CtClass cc) {
        this(pool, cc, false);
    }

    public IncrementRewriter(ClassPool pool, CtClass cc,
                             boolean useAiinc) {
        super(pool, cc);
        this.useAiinc = useAiinc;
    }

    public void rewrite() {
        // interface fields are static and final
        if (classFile.isInterface()) return;

        try {
            prepUnsafeOffsets();
            rewriteIncrements();
        } catch (BadBytecode | CannotCompileException ex) {
            ex.printStackTrace();
            throw new Error(ex.getMessage());
        }
    }

    public void prepUnsafeOffsets() throws CannotCompileException {
        FieldInfo unsafe = new FieldInfo(
            constPool, "$theUnsafe", Descriptor.of("sun.misc.Unsafe"));
        unsafe.setAccessFlags(AccessFlag.FINAL | AccessFlag.STATIC | AccessFlag.PRIVATE);
        classFile.addField(unsafe);

        StringBuilder sb = new StringBuilder();
        // get java.lang.reflect.Field objects for relavant fields
        // <Class>.class.getDeclaredField("field")

        // new because we want to avoid concurrent modification
        final List<FieldInfo> fields = new ArrayList<>(classFile.getFields());
        for (FieldInfo finfo : fields) {
            if (!Descriptor.of("long").equals(finfo.getDescriptor())
                && !Descriptor.of("int").equals(finfo.getDescriptor())
                || (finfo.getAccessFlags​() & AccessFlag.FINAL) != 0) {
                continue;
            }

            FieldInfo offset = new FieldInfo(
                constPool, "offset$" + finfo.getName(), Descriptor.of("long"));
            // AccessFlag.PUBLIC because field could be referenced from another class
            offset.setAccessFlags(AccessFlag.FINAL | AccessFlag.STATIC | AccessFlag.PUBLIC);
            classFile.addField(offset);

            String getOffset = getOffsetCode;
            if ((finfo.getAccessFlags() & AccessFlag.STATIC) != 0)
                getOffset += "offset${name} = $theUnsafe.staticFieldOffset(field${name});\n";
            else
                getOffset += "offset${name} = $theUnsafe.objectFieldOffset(field${name});\n";

            sb.append(getOffset
                      .replace("{name}", finfo.getName())
                      .replace("{class}", classFile.getName()));
        }

        String getOffsets = sb.toString();

        // add static initializer
        CtConstructor staticInit = cc.makeClassInitializer();
        staticInit.insertAfter("{" + getUnsafeCode + getOffsets + "}");
    }

    public void rewriteIncrements() throws BadBytecode {

        // locate sequence of bytecode that does increment
        final List<MethodInfo> methods = classFile.getMethods();
        for (MethodInfo minfo : methods) {
            logger.debug("At "+ minfo.getName() +":"+ minfo.getDescriptor());

            CodeAttribute ca = minfo.getCodeAttribute();
            if (ca == null) {
                logger.debug("Code attribute not specified");
                continue;
            }
            CodeIterator ci = ca.iterator();

            int index = getNextSequence(ci, incrementMatcher);
            while (index > 0) {
                // TODO: ILOAD works as well as ICONST, but with argument

                // getfield/putfield
                // BEG index+0
                // index+0: DUP
                // index+1: GETFIELD   <-- getIndex
                // index+2: <index>
                // index+4: ICONST
                // index+5: IADD
                // index+6: PUTFIELD   <-- getIndex+5
                // index+7: <index>
                // END index+9

                // getstatic/putstatic
                // BEG index+0
                // index+0: GETSTATIC  <-- getIndex
                // index+1: <index>
                // index+3: ICONST
                // index+4: IADD
                // index+5: PUTSTATIC  <-- getIndex+5
                // index+6: <index>
                // END index+8

                boolean isStatic = ci.byteAt(index) != Opcode.DUP;
                int getIndex = isStatic ? index : index+1;
                int constPoolIndex = ci.u16bitAt(getIndex+1);
                String field = constPool.getFieldrefName(constPoolIndex);
                String klass = constPool.getFieldrefClassName(constPoolIndex);

                boolean validate =
                    // get/set same field
                    constPoolIndex == ci.u16bitAt(getIndex+6)
                    // GETSTATIC or
                    && (isStatic && ci.byteAt(getIndex) == Opcode.GETSTATIC
                        // GETFIELD
                        || !isStatic && ci.byteAt(getIndex) == Opcode.GETFIELD);

                if (validate) {
                    logger.info(String.format(
                                    "%s.%s index %d: %s #%d  (%s.%s)",
                                    cc.getName(),
                                    minfo.getName(),
                                    getIndex,
                                    (ci.byteAt(getIndex) == Opcode.GETFIELD
                                     ? "GETFIELD"
                                     : "GETSTATIC"),
                                    constPoolIndex,
                                    klass,
                                    field));

                    int iconst = ci.byteAt(getIndex+3);

                    // Class (only needed for static field)
                    int classIndex = constPool.getFieldrefClass(constPoolIndex);
                    // $theUnsafe
                    int unsafeIndex = constPool.addFieldrefInfo(
                        constPool.addClassInfo(classFile.getName()),
                        "$theUnsafe",
                        Descriptor.of("sun.misc.Unsafe"));
                    // offset$field
                    int offsetIndex = constPool.addFieldrefInfo(
                        classIndex,
                        "offset$" + field,
                        Descriptor.of("long"));
                    // $theUnsafe.getAndAddInt(Object o, long offset, int delta)
                    int atomicAddIndex = constPool.addMethodrefInfo(
                        constPool.addClassInfo("sun.misc.Unsafe"),
                        "getAndAddInt",
                        "(Ljava/lang/Object;JI)I");

                    logger.debug(String.format(
                                     "classIndex: %d, unsafeIndex: %d, " +
                                     "offsetIndex: %d, atomicAddIndex: %d",
                                     classIndex,
                                     unsafeIndex,
                                     offsetIndex,
                                     atomicAddIndex));

                    if (isStatic && !useAiinc) {
                        byte[][] bytecode = {
                            opcodeWithArg(Opcode.GETSTATIC, unsafeIndex, 2),
                            // top of stack: unsafe
                            classIndex <= 0xFFFF
                                ? opcodeWithArg(Opcode.LDC, classIndex, 1)
                                : opcodeWithArg(Opcode.LDC_W, classIndex, 2),
                            // top of stack: Class unsafe
                            opcodeWithArg(Opcode.GETSTATIC, offsetIndex, 2),
                            // top of stack: offset Class unsafe
                            {(byte)iconst},
                            // top of stack: delta offset Class unsafe
                            opcodeWithArg(Opcode.INVOKEVIRTUAL, atomicAddIndex, 2),
                            // top of stack: val
                            {(byte)Opcode.POP},
                        };

                        // 8 bytes for GETSTATIC/PUTSTATIC
                        replaceBytecode(ci, index, 8, bytecode);
                    } else if (!isStatic && !useAiinc) {
                        byte[][] bytecode = {
                            // top of stack: obj
                            opcodeWithArg(Opcode.GETSTATIC, unsafeIndex, 2),
                            // top of stack: unsafe obj
                            {(byte)Opcode.SWAP},
                            // top of stack: obj unsafe
                            opcodeWithArg(Opcode.GETSTATIC, offsetIndex, 2),
                            // top of stack: offset obj unsafe
                            {(byte)iconst},
                            // top of stack: delta offset obj unsafe
                            opcodeWithArg(Opcode.INVOKEVIRTUAL, atomicAddIndex, 2),
                            // top of stack: val
                            {(byte)Opcode.POP},
                        };

                        // 9 bytes for GETFIELD/PUTFIELD
                        replaceBytecode(ci, index, 9, bytecode);
                    } else if (isStatic && useAiinc) {
                        // AIINCSTATIC
                        byte[][] bytecode = {
                            {(byte)iconst},
                            // top of stack: delta
                            opcodeWithArg(AIINCSTATIC, constPoolIndex, 2),
                            // top of stack: previous_val
                            {(byte)Opcode.POP},
                        };

                        // 8 bytes for GETSTATIC/PUTSTATIC
                        replaceBytecode(ci, index, 8, bytecode);
                    } else {    // !isStatic && useAiinc
                        // AIINCFIELD
                        byte[][] bytecode = {
                            // top of stack: obj
                            {(byte)iconst},
                            // top of stack: delta obj
                            opcodeWithArg(AIINCFIELD, constPoolIndex, 2),
                            // top of stack: previous_val
                            {(byte)Opcode.POP},
                        };

                        // 9 bytes for GETFIELD/PUTFIELD
                        replaceBytecode(ci, index, 9, bytecode);
                    }
                }

                index = getNextSequence(ci, incrementMatcher);
            }

            // $theUnsafe.getAndAddInt need max_stack >= 5
            if (!useAiinc)
                ca.computeMaxStack();
        }

        // TODO: long
        // $theUnsafe.getAndAddLong
    }

    /**
     * Replace bytecode in range [index, index+size) with replacement
     *
     * replacement has to be an array of opcodes with their arguments
     * e.g. {{Opcode.GETSTATIC, 100}, {Opcode.DUP}}
     */
    private void replaceBytecode(CodeIterator ci, int index, int size,
                                 byte[][] replacement) throws BadBytecode {
        int end = index + size;
        int i = 0;
        for (; i < replacement.length; i++) {
            if (index + replacement[i].length > end)
                break;
            if (i != replacement.length-1
                && index + replacement[i].length == end)
                break;

            ci.write(replacement[i], index);
            index += replacement[i].length;
        }

        if (i < replacement.length) {
            byte[] toWrite;

            int leftBytes = 0;
            for (int j = i; j < replacement.length; j++)
                leftBytes += replacement[j].length;
            toWrite = new byte[leftBytes];
            int idx = 0;
            for (int j = i; j < replacement.length; j++)
                for (byte b : replacement[j])
                    toWrite[idx++] = b;

            // insertAt expects opcode boundary
            // also do not insertAt the end (breaks goto offset)
            ci.insertAt(index, toWrite);
            // From documentation: If the instruction at the given
            // index is at the beginning of a block statement, then
            // the bytecode is inserted within that block

            // TODO: robustness
            // also from documentation: returns the index indicating
            // the first byte of the inserted byte sequence, which
            // might be different from pos

            index += leftBytes;
            end += leftBytes;
        }

        for (int j = index; j < end; j++) {
            ci.writeByte(Opcode.NOP, j);
        }
    }

    private int getNextSequence(CodeIterator ci, Matcher[] matchers)
        throws BadBytecode {

        int index = -1;
        int beginIndex = -1;    // begin index of the matched sequence,
                                // indexing into ci (ci.byteAt(beginIndex))

        List<Integer> matchedSeq = new LinkedList<>();
        int toMatchIdx = 0;     // indexing into matchedSeq
        int matcherIdx = 0;     // indexing into matchers
        while (true) {
            if (matcherIdx == matchers.length)
                return beginIndex;

            if (matchedSeq.size() <= toMatchIdx) {
                if (!ci.hasNext())
                    return -1;
                index = ci.next();
                matchedSeq.add(ci.byteAt(index));
            }

            toMatchIdx = matchers[matcherIdx].match(matchedSeq, toMatchIdx);
            if (toMatchIdx == -1) {
                beginIndex = -1;

                // clear states
                matcherIdx = 0;
                toMatchIdx = 0;
                matchedSeq.remove(0); // right shift by 1

                continue;
            }

            if (matcherIdx == 0)
                beginIndex = index;
            matcherIdx++;
        }
    }

    private byte[] opcodeWithArg(int opcode, int argument, int nBytes) {
        switch (nBytes) {
        case 1:
            return new byte[] {
                (byte)opcode,
                (byte)(argument & 0xFF),
            };
        case 2:
            return new byte[] {
                (byte)opcode,
                (byte)((argument & 0xFF00) >> 8),
                (byte)(argument & 0x00FF),
            };
        case 3:
            return new byte[] {
                (byte)opcode,
                (byte)((argument & 0xFF0000) >> 16),
                (byte)((argument & 0x00FF00) >> 8),
                (byte)(argument & 0x0000FF),
            };
        case 4:
            return new byte[] {
                (byte)opcode,
                (byte)((argument & 0xFF000000) >> 24),
                (byte)((argument & 0x00FF0000) >> 16),
                (byte)((argument & 0x0000FF00) >> 8),
                (byte)(argument & 0x000000FF),
            };
        default:
            throw new IllegalArgumentException(
                "Invalid argument nBytes, expected 1 <= nBytes <= 4, got " +
                nBytes);
        }
    }
}
