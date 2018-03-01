package jrewriter;

import java.io.IOException;
import java.util.List;
import javassist.*;
import javassist.bytecode.*;
import javassist.expr.*;


public class IncrementRewriter extends Rewriter {
    public IncrementRewriter(ClassPool pool, String className) {
        super(pool, className);
    }

    public byte[] toBytecode() {
        // try {
        // } catch (IOException ex) {
        //     ex.printStackTrace();
        // }
        return null;
    }

    public void addToConstPool() {
        // constPool.addMethodRef();
    }
}
