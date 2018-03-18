package jrewriter;

import java.io.*;
import java.util.*;
import javassist.*;
import javassist.bytecode.*;
import javassist.expr.*;


public class IncrementRewriter extends Rewriter {
    // sequence of bytecode that does increment
    final int[] INCREMENT = {
        Opcode.GETFIELD,
        Opcode.ICONST_1,
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
            offset.setAccessFlags(AccessFlag.FINAL | AccessFlag.STATIC | AccessFlag.PRIVATE);
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

        // System.out.println(sb.toString());
        String getOffsets = sb.toString();

        // add static initializer
        CtConstructor staticInit = cc.makeClassInitializer();
        staticInit.insertBefore("{" + getUnsafe + getOffsets + "}");
    }

    public void rewriteIncrement() throws BadBytecode {
        // TODO

        // locate sequence of bytecode that does increment
        final List<MethodInfo> methods = classFile.getMethods();
        for (MethodInfo minfo : methods) {
            System.out.println(minfo.getName()+" "+minfo.getDescriptor());

            CodeAttribute ca = minfo.getCodeAttribute();
            CodeIterator ci = ca.iterator();

            int index = getNextSequence(ci, INCREMENT);
            while (index > 0) {
                int constPoolIndex = ci.u16bitAt(index+1);
                System.out.println("GETFIELD " + constPoolIndex);

                index = getNextSequence(ci, INCREMENT);
            }
        }

        // replace with
        // $theUnsafe.getAndAddInt
        // $theUnsafe.getAndAddLong

        // add methods to constPool

        // static: $theUnsafe.getAndAddInt(Type.class, offset$field, delta)
        // object: $theUnsafe.getAndAddInt(obj, offset$field, delta)
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
