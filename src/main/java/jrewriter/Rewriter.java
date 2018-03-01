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

    public Rewriter(ClassPool pool, String className) {
        try {
            cc = pool.get(className);
            classFile = cc.getClassFile();
            constPool = classFile.getConstPool();
        } catch (NotFoundException ex) {
            ex.printStackTrace();
            throw new RuntimeException(ex.getMessage());
        }
    }

    public byte[] toBytecode() {
        throw new UnsupportedOperationException(
            "Use one of the subclasses");
    }
}
