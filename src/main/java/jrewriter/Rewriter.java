package jrewriter;

import java.io.IOException;
import java.util.List;
import javassist.*;
import javassist.bytecode.*;


public class Rewriter {
    CtClass cc;
    ClassFile classFile;
    ConstPool constPool;

    public Rewriter(ClassPool pool, String className) {
        try {
            cc = pool.get(className);
            classFile = cc.getClassFile();
            constPool = cc.getClassFile().getConstPool();
        } catch (NotFoundException ex) {
            ex.printStackTrace();
            throw new RuntimeException(ex.getMessage());
        }
    }

    public byte[] toBytecode() {
        // find direct field references
        // then rewrite with getter and setter

        // TODO: error handling
        try {
            RefFinder.fieldRef(cc);

            addGettersSetters();
            rewrite();
            cc.writeFile();
            return cc.toBytecode();
        } catch (IOException ex) {
            ex.printStackTrace();
            throw new RuntimeException(ex.getMessage());
        } catch (BadBytecode | NotFoundException | CannotCompileException ex) {
            ex.printStackTrace();
            throw new Error(ex.getMessage());
        }
    }

    public void addGettersSetters() throws CannotCompileException {
        for (CtField field : cc.getDeclaredFields()) {
            addGetter(field);
            addSetter(field);
        }
    }

    public void rewrite() throws BadBytecode {
        List<MethodInfo> methods = classFile.getMethods();

        // TODO: lookup javassist.expr.ExprEditor and
        // javassist.expr.FieldAccess

        for (MethodInfo minfo : methods) {
            CodeAttribute ca = minfo.getCodeAttribute();
            CodeIterator ci = ca.iterator();

            while (ci.hasNext()) {
                int index = ci.next();

                int constPoolIndex;
                switch (ci.byteAt(index)) {
                case Opcode.GETFIELD:
                    rewriteGetter(ci, index);
                    break;
                case Opcode.PUTFIELD:
                    rewriteSetter(ci, index);
                    break;
                }
            }
        }
    }

    private void addGetter(CtField field)
        throws CannotCompileException {

        CtMethod getter = CtNewMethod.getter(
            "get$" + field.getName(), field);
        cc.addMethod(getter);
    }

    private void addSetter(CtField field)
        throws CannotCompileException {

        CtMethod setter = CtNewMethod.setter(
            "set$" + field.getName(), field);
        cc.addMethod(setter);
    }

    private void rewriteGetter(CodeIterator ci, int index) {
        int constPoolIndex = ci.u16bitAt(index+1);
        int tag = constPool.getTag(index);
        switch (tag) {
        case ConstPool.CONST_Fieldref:
            break;
        }

        // invoke the getter with invokespecial
        // TODO: work under polymorphism?
    }

    private void rewriteSetter(CodeIterator ci, int index) {

    }
}
