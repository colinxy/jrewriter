package jrewriter;

import java.io.IOException;
import java.util.List;
import javassist.*;
import javassist.bytecode.*;
import javassist.expr.*;


public class Rewriter {
    CtClass cc;
    ClassFile classFile;
    ConstPool constPool;

    public Rewriter(ClassPool pool, CtClass cc) {
        this.cc = cc;
        // requires !cc.isFrozen()
        if (cc.isFrozen())
            throw new Error(cc.getName() + " already loaded");
        classFile = cc.getClassFile();
        constPool = classFile.getConstPool();
    }

    public byte[] toBytecode() {
        // already loaded
        if (!cc.isFrozen()) {
            rewrite();
        }
        try {
            return cc.toBytecode();
        } catch (CannotCompileException ex) {
            ex.printStackTrace();
            throw new Error(ex);
        } catch (IOException ex) {
            ex.printStackTrace();
            throw new RuntimeException(ex);
        }
    }

    public void rewrite() {
        throw new UnsupportedOperationException(
            "Use one of the subclasses");
    }
}
