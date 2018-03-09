package jrewriter;

import java.io.*;
import java.util.List;
import javassist.*;
import javassist.bytecode.*;
import javassist.expr.*;


public class IncrementRewriter extends Rewriter {
    public IncrementRewriter(ClassPool pool, String className) {
        super(pool, className);
    }

    public byte[] toBytecode() {
        try {
            addUnsafe();
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

    public void addUnsafe() throws CannotCompileException {
        FieldInfo unsafe = new FieldInfo(
            constPool, "$theUnsafe", Descriptor.of("sun.misc.Unsafe"));
        unsafe.setAccessFlags(AccessFlag.STATIC | AccessFlag.PRIVATE);
        classFile.addField(unsafe);

        // add static initializer
        CtConstructor staticInit = cc.makeClassInitializer();
        staticInit.insertBefore(
            "{" +
            "java.lang.reflect.Field $theUnsafeField;" +
            "try {" +
            "    $theUnsafeField = sun.misc.Unsafe.class.getDeclaredField(\"theUnsafe\");" +
            "} catch (NoSuchFieldException ex) {" +
            "    ex.printStackTrace();" +
            "    throw new Error(ex);" +
            "}" +
            "$theUnsafeField.setAccessible(true);" +
            "try {" +
            "    $theUnsafe = (sun.misc.Unsafe) $theUnsafeField.get(null);" +
            "} catch (IllegalAccessException ex) {" +
            "    ex.printStackTrace();" +
            "    throw new Error(ex);" +
            "}" +
            "}");

    }

    public void rewriteIncrement() {
        // TODO

        // locate sequence of bytecode that does increment

        // get java.lang.reflect.Field objects for relavant fields
        // <Class>.class.getDeclaredField("field")

        // get offset
        // for statis fields: $theUnsafe.staticFieldOffset(field)
        // for object fields: $theUnsafe.objectFieldOffset(field)

        // $theUnsafe.getAndAddInt
        // $theUnsafe.getAndAddLong
    }
}
