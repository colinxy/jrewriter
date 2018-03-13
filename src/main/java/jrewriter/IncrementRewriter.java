package jrewriter;

import java.io.*;
import java.util.*;
import javassist.*;
import javassist.bytecode.*;
import javassist.expr.*;


public class IncrementRewriter extends Rewriter {
    public IncrementRewriter(ClassPool pool, String className) {
        super(pool, className);
    }

    public byte[] toBytecode() {
        try {
            prepUnsafeOffset();
            rewriteIncrement();

            return cc.toBytecode();
        } catch (IOException ex) {
            ex.printStackTrace();
            throw new RuntimeException(ex.getMessage());
        } catch (CannotCompileException ex) {
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
        List<FieldInfo> fields = new ArrayList<>(classFile.getFields());
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

            // TODO: might not work when the field is from super class
            // getDeclaredField only returns the field declared in this class,
            // not superClass
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

            sb.append(getOffset
                      .replace("{name}", finfo.getName())
                      .replace("{class}", classFile.getName()));
        }

        System.out.println(sb.toString());
        String getOffsets = sb.toString();

        // add static initializer
        CtConstructor staticInit = cc.makeClassInitializer();
        staticInit.insertBefore("{" + getUnsafe + getOffsets + "}");
    }

    public void rewriteIncrement() {
        // TODO

        // first locate sequence of bytecode that does increment

        // replace with
        // $theUnsafe.getAndAddInt
        // $theUnsafe.getAndAddLong

        // add methods to constPool

        // static: $theUnsafe.getAndAddInt(Type.class, offset$field, delta)
        // object: $theUnsafe.getAndAddInt(this, offset$field, delta)
    }

    private int getNextIncrement(CodeIterator ci) {
        return -1;
    }
}
