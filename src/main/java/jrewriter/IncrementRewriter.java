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

            // classFile.write(
            //     new DataOutputStream(new FileOutputStream("Simple.class")));
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
        // constPool.addClassInfo("sun/misc/Unsafe");

        FieldInfo unsafe = new FieldInfo(
            constPool, "$theUnsafe", Descriptor.of("sun.misc.Unsafe"));
        unsafe.setAccessFlags(AccessFlag.STATIC | AccessFlag.PRIVATE);
        classFile.addField(unsafe);

        // add static initializer
        CtConstructor staticInit = cc.makeClassInitializer();
        staticInit.insertBefore(
            "{" +
            "java.lang.reflect.Field theUnSafeField;" +
            "try {" +
            "    theUnSafeField = sun.misc.Unsafe.class.getDeclaredField(\"theUnsafe\");" +
            "} catch (NoSuchFieldException ex) {" +
            "    ex.printStackTrace();" +
            "    throw new RuntimeException(ex);" +
            "}" +
            "theUnSafeField.setAccessible(true);" +
            "try {" +
            "    $theUnsafe = (sun.misc.Unsafe) theUnSafeField.get(null);" +
            "} catch (IllegalAccessException ex) {" +
            "    ex.printStackTrace();" +
            "    throw new RuntimeException(ex);" +
            "}" +
            "}");
    }
}


// class IncrementExpr extends Expr {
//     public void replace(String statement) {
//         // TODO
//     }
// }
