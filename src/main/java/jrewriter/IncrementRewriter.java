package jrewriter;

import java.io.*;
import java.util.*;
import javassist.*;
import javassist.bytecode.*;
import javassist.expr.*;


public class IncrementRewriter extends Rewriter {
    // sequence of bytecode that does increment
    final int[] INCREMENT = {
        Opcode.DUP,
        Opcode.GETFIELD,
        // TODO: also ICONST family, (ILOAD family)
        Opcode.ICONST_1,
        // TODO: also ISUB
        Opcode.IADD,
        Opcode.PUTFIELD,
    };

    public IncrementRewriter(ClassPool pool, CtClass cc) {
        super(pool, cc);
    }

    public void rewrite() {
        try {
            prepUnsafeOffset();
            rewriteIncrement();
        } catch (BadBytecode | CannotCompileException ex) {
            ex.printStackTrace();
            throw new Error(ex.getMessage());
        }
    }

    public void prepUnsafeOffset() throws CannotCompileException {
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

    public void rewriteIncrement() throws BadBytecode {

        // locate sequence of bytecode that does increment
        final List<MethodInfo> methods = classFile.getMethods();
        for (MethodInfo minfo : methods) {
            if (RewriterClassLoader.DEBUG)
                System.out.println(
                    "==> " + minfo.getName() + ":" + minfo.getDescriptor());

            CodeAttribute ca = minfo.getCodeAttribute();
            CodeIterator ci = ca.iterator();

            int index = getNextSequence(ci, INCREMENT);
            while (index > 0) {
                // TODO: ILOAD works as well as ICONST, but with argument

                // BEG index+0
                // index+0: DUP
                // index+1: GETFIELD
                // index+2: <index>
                // index+4: ICONST
                // index+5: IADD
                // index+6: PUTFIELD
                // index+7: <index>
                // END index+9
                final int constPoolIndex = ci.u16bitAt(index+2);
                String field = constPool.getFieldrefName(constPoolIndex);
                String klass = constPool.getFieldrefClassName(constPoolIndex);

                if (RewriterClassLoader.DEBUG)
                    System.out.println("getfield " + klass + "." + field);

                int delta = 0;
                int iconst = ci.byteAt(index+4);
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

                index = getNextSequence(ci, INCREMENT);
            }

            // $theUnsafe.getAndAddInt need max_stack >= 5
            ca.computeMaxStack();
        }

        // TODO: static fields use getstatic/putstatic
        // static: $theUnsafe.getAndAddInt(Type.class, offset$field, delta)
        // TODO: long
        // $theUnsafe.getAndAddLong
    }

    /**
     * returns -1 if got to the end of CodeIterator
     */
    private int getNextSequence(CodeIterator ci, int[] seq) throws BadBytecode {
        int index = -1;
        int beginIndex = -1;    // begin index of the matched sequence

        boolean next = true;
        int toMatch = 0;
        while (true) {
            // found a match!
            if (toMatch == seq.length)
                return beginIndex;

            if (next) {
                if (!ci.hasNext())
                    return -1;
                index = ci.next();
            }

            if (ci.byteAt(index) != seq[toMatch]) {
                beginIndex = -1;
                next = (toMatch == 0);
                // TODO: this assume there is no duplicate in seq
                toMatch = 0;
                continue;
            }

            if (toMatch == 0) {
                beginIndex = index;
            }
            toMatch++;
            next = true;
        }
        // throw new RuntimeException("getNextSequence bug");
    }
}
