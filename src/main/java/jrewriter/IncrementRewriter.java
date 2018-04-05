package jrewriter;

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
    // sequence of bytecode that does increment
    final Matcher[] incrementMatcher = {
        Or(Exactly(Opcode.DUP), Skip),
        Or(Opcode.GETFIELD, Opcode.GETSTATIC),
        // TODO
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
        super(pool, cc);
    }

    public void rewrite() {
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

        String getUnsafe =
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

        StringBuilder sb = new StringBuilder();
        // get java.lang.reflect.Field objects for relavant fields
        // <Class>.class.getDeclaredField("field")

        // new because we want to avoid concurrent modification
        final List<FieldInfo> fields = new ArrayList<>(classFile.getFields());
        for (FieldInfo finfo : fields) {
            // System.out.println(finfo.getName()+" "+finfo.getDescriptor());

            if (!Descriptor.of("long").equals(finfo.getDescriptor())
                && !Descriptor.of("int").equals(finfo.getDescriptor())
                || (finfo.getAccessFlags​() & AccessFlag.FINAL) != 0) {
                continue;
            }

            // System.out.println(finfo.getName()+" "+finfo.getDescriptor());

            FieldInfo offset = new FieldInfo(
                constPool, "offset$" + finfo.getName(), Descriptor.of("long"));
            // AccessFlag.PUBLIC because field could be referenced from another class
            offset.setAccessFlags(AccessFlag.FINAL | AccessFlag.STATIC | AccessFlag.PUBLIC);
            classFile.addField(offset);

            String getOffset =
                "java.lang.reflect.Field field${name};\n" +
                "try {\n" +
                "    field${name} = {class}.class.getDeclaredField(\"{name}\");\n" +
                "} catch (NoSuchFieldException ex) {\n" +
                "    ex.printStackTrace();\n" +
                "    throw new Error(ex);\n" +
                "}\n";

            if ((finfo.getAccessFlags() & AccessFlag.STATIC) != 0)
                getOffset += "offset${name} = $theUnsafe.staticFieldOffset(field${name});\n";
            else
                getOffset += "offset${name} = $theUnsafe.objectFieldOffset(field${name});\n";

            // getOffset += "System.out.println(\"{name}: \" + offset${name});\n";

            sb.append(getOffset
                      .replace("{name}", finfo.getName())
                      .replace("{class}", classFile.getName()));
        }

        String getOffsets = sb.toString();

        // add static initializer
        CtConstructor staticInit = cc.makeClassInitializer();
        staticInit.insertBefore("{" + getUnsafe + getOffsets + "}");
    }

    public void rewriteIncrements() throws BadBytecode {

        // locate sequence of bytecode that does increment
        final List<MethodInfo> methods = classFile.getMethods();
        for (MethodInfo minfo : methods) {
            if (RewriterClassLoader.DEBUG)
                System.out.println(
                    "==> " + minfo.getName() + ":" + minfo.getDescriptor());

            CodeAttribute ca = minfo.getCodeAttribute();
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

                final boolean isStatic = ci.byteAt(index) != Opcode.DUP;
                final int getIndex = isStatic ? index : index+1;
                final int constPoolIndex = ci.u16bitAt(getIndex+1);
                final String field = constPool.getFieldrefName(constPoolIndex);
                final String klass = constPool.getFieldrefClassName(constPoolIndex);

                boolean validate =
                    // get/set same field
                    constPoolIndex == ci.u16bitAt(getIndex+6)
                    // GETSTATIC or
                    && (isStatic && ci.byteAt(getIndex) == Opcode.GETSTATIC
                        // GETFIELD
                        || !isStatic && ci.byteAt(getIndex) == Opcode.GETFIELD);

                if (validate) {

                    if (RewriterClassLoader.DEBUG) {
                        String code = ci.byteAt(getIndex) == Opcode.GETFIELD
                            ? "GETFIELD "
                            : "GETSTATIC ";
                        System.out.println(code + klass + "." + field);
                    }

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

                    if (isStatic) {
                        // 8 bytes for GETSTATIC/PUTSTATIC
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

                        replaceBytecode(ci, index, 8, bytecode);
                    } else {
                        // 9 bytes for GETFIELD/PUTFIELD
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

                        replaceBytecode(ci, index, 9, bytecode);
                    }
                }

                index = getNextSequence(ci, incrementMatcher);
            }

            // $theUnsafe.getAndAddInt need max_stack >= 5
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

            ci.write(replacement[i], index);
            index += replacement[i].length;
        }

        for (int j = index; j < end; j++) {
            ci.writeByte(Opcode.NOP, j);
        }

        if (i < replacement.length) {
            // // useless Java generics: left as reference
            // // Java's gnerics is pure rubbish with primitive types
            // Stream<Byte> toWriteStream = IntStream
            //     .range(i, replacement.length)
            //     // Stream.skip does not work
            //     // because byte is not available for generics
            //     .mapToObj(j -> replacement[j])
            //     // stream of byte[]
            //     // need to flatMap stream of Byte[] (boxed)
            //     .flatMap(byteArr -> {
            //             Byte[] boxed = new Byte[byteArr.length];
            //             for (int k = 0; k < byteArr.length; k++)
            //                 boxed[k] = byteArr[k];
            //             return Arrays.stream(boxed);
            //         }); // boxing byte[] is not available out of the box

            byte[] toWrite;

            int length = 0;
            for (int j = i; j < replacement.length; j++)
                length += replacement[j].length;
            toWrite = new byte[length];
            int idx = 0;
            for (int j = i; j < replacement.length; j++)
                for (byte b : replacement[j])
                    toWrite[idx++] = b;

            // insertAt expects opcode boundary
            ci.insertAt(end, toWrite);
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
                (byte)(argument & 0xFF00),
                (byte)(argument & 0x00FF),
            };
        case 3:
            return new byte[] {
                (byte)opcode,
                (byte)(argument & 0xFF0000),
                (byte)(argument & 0x00FF00),
                (byte)(argument & 0x0000FF),
            };
        case 4:
            return new byte[] {
                (byte)opcode,
                (byte)(argument & 0xFF000000),
                (byte)(argument & 0x00FF0000),
                (byte)(argument & 0x0000FF00),
                (byte)(argument & 0x000000FF),
            };
        default:
            throw new IllegalArgumentException(
                "Invalid argument nBytes, expected 1 <= nBytes <= 4, got " +
                nBytes);
        }
    }
}
