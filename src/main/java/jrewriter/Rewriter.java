package jrewriter;

import javassist.*;
import java.io.IOException;


public class Rewriter {
    CtClass cc;

    public Rewriter(ClassPool pool, String className) {
        try {
            cc = pool.get(className);
        } catch (NotFoundException ex) {
            ex.printStackTrace();
            throw new RuntimeException(ex.getMessage());
        }
    }

    public byte[] toBytecode() {
        // find direct field references
        RefFinder.fieldRef(cc);
        // then rewrite with getter and setter

        // TODO: error handling
        try {
            return cc.toBytecode();
        } catch (IOException ex) {
            ex.printStackTrace();
            throw new RuntimeException(ex.getMessage());
        } catch (CannotCompileException ex) {
            ex.printStackTrace();
            throw new Error(ex.getMessage());
        }
    }

    public static void addGetter() {
        // CtNewMethod.getter(String methodName, CtField field)
    }

    public static void addSetter() {
        // CtNewMethod.setter(String methodName, CtField field)
    }
}
