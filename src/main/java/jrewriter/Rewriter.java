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
        // TODO: add getter and setter for all class
        for (CtField field : cc.getDeclaredFields()) {
            addGetter(field);
            addSetter(field);
        }
    }

    public void rewrite() throws CannotCompileException {
        CtBehavior[] behaviors = cc.getDeclaredBehaviors();

        for (CtBehavior behavior : behaviors) {
            if (behavior.getName().startsWith("get$")
                || behavior.getName().startsWith("set$")) {
                continue;
            }

            behavior.instrument(
                new ExprEditor() {
                    public void edit(FieldAccess f) throws CannotCompileException {
                        if (f.isReader()) {
                            String klass = f.getClassName();
                            String field = f.getFieldName();
                            System.out.println("getfield "
                                               + klass + " " + field);

                            // TODO: this will generate a invokevirtual
                            // but we want a invokespecial
                            String get = String.format("$_ = $0.get$%s();", field);
                            // TODO: create getter and setter for all classes
                            // before rewriting field access
                            if (klass.equals(cc.getName()))
                                f.replace(get);
                        } else if (f.isWriter()) {
                            String klass = f.getClassName();
                            String field = f.getFieldName();
                            System.out.println("putfield "
                                               + klass + " " + field);

                        }
                    }
                });
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
}
