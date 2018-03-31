package jrewriter;

import java.io.*;
import java.util.*;
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
    // TODO: support GETSTATIC/PUTSTATIC
    final Matcher[] incrementMatcher = {
        // Or(Exactly(Opcode.DUP), Skip),
        Exactly(Opcode.DUP),
        Or(Opcode.GETFIELD, Opcode.GETSTATIC),
        Or(Opcode.ICONST_0, Opcode.ICONST_1,
           Opcode.ICONST_2, Opcode.ICONST_3,
           Opcode.ICONST_4, Opcode.ICONST_5, Opcode.ICONST_M1),
        Exactly(Opcode.IADD),
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
                String field = constPool.getFieldrefName(constPoolIndex);
                String klass = constPool.getFieldrefClassName(constPoolIndex);

                if (constPoolIndex == ci.u16bitAt(getIndex+6)) {

                    if (RewriterClassLoader.DEBUG) {
                        String code = ci.byteAt(getIndex) == Opcode.GETFIELD
                            ? "GETFIELD "
                            : "GETSTATIC ";
                        System.out.println(code + klass + "." + field);
                    }

                    int iconst = ci.byteAt(getIndex+3);
                    int delta = 0;
                    switch (iconst) {
                    case Opcode.ICONST_0: delta = 0; break;
                    case Opcode.ICONST_1: delta = 1; break;
                    case Opcode.ICONST_2: delta = 2; break;
                    case Opcode.ICONST_3: delta = 3; break;
                    case Opcode.ICONST_4: delta = 4; break;
                    case Opcode.ICONST_5: delta = 5; break;
                    case Opcode.ICONST_M1: delta = -1; break;
                    default: throw new Error("Unknown instruction: " + iconst);
                    }

                    // $theUnsafe
                    int unsafeIndex = constPool.addFieldrefInfo(
                        constPool.addClassInfo(classFile.getName()),
                        "$theUnsafe",
                        Descriptor.of("sun.misc.Unsafe"));
                    // offset$field
                    int offsetIndex = constPool.addFieldrefInfo(
                        constPool.getFieldrefClass(constPoolIndex),
                        "offset$" + field,
                        Descriptor.of("long"));
                    // $theUnsafe.getAndAddInt(Object o, long offset, int delta)
                    int atomicAddIndex = constPool.addMethodrefInfo(
                        constPool.addClassInfo("sun.misc.Unsafe"),
                        "getAndAddInt",
                        "(Ljava/lang/Object;JI)I");

                    // TODO: support GETSTATIC/PUTSTATIC
                    // top of stack: obj
                    ci.writeByte(Opcode.NOP, index);
                    // top of stack: obj
                    ci.writeByte(Opcode.GETSTATIC, index+1);
                    ci.write16bit(unsafeIndex, index+2);
                    // top of stack: unsafe obj
                    ci.writeByte(Opcode.SWAP, index+4);
                    // top of stack: obj unsafe
                    ci.writeByte(Opcode.GETSTATIC, index+5);
                    ci.write16bit(offsetIndex, index+6);
                    // top of stack: offset obj unsafe
                    ci.writeByte(iconst, index+8);
                    // top of stack: delta offset obj unsafe
                    // $theUnsafe.getAndAddInt(obj, offset$field, delta)
                    ci.insertAt(index+9, new byte[] {
                            (byte)Opcode.INVOKEVIRTUAL,
                            (byte)(atomicAddIndex & 0xFF00),
                            (byte)(atomicAddIndex & 0x00FF),
                            // top of stack: val
                            (byte)Opcode.POP});

                }

                index = getNextSequence(ci, incrementMatcher);
            }

            // $theUnsafe.getAndAddInt need max_stack >= 5
            ca.computeMaxStack();
        }

        // TODO: static fields use getstatic/putstatic
        // static: $theUnsafe.getAndAddInt(Type.class, offset$field, delta)
        // TODO: long
        // $theUnsafe.getAndAddLong
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
}
