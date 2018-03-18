package jrewriter;

import java.io.IOException;
import java.util.List;
import javassist.*;
import javassist.bytecode.*;
import javassist.expr.*;


public class GetSetRewriter extends Rewriter {
    public GetSetRewriter(ClassPool pool, CtClass cc) {
        super(pool, cc);
    }

    public void rewrite() {
        // find direct field references
        // then rewrite with getter and setter

        try {
            RefFinder.fieldRef(cc);
            RefFinder.incrementRef(cc);

            addGettersSetters();
            rewriteAccess();
        } catch (BadBytecode | CannotCompileException ex) {
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

    public void rewriteAccess() throws CannotCompileException {
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

                            // TODO: same problem as getter
                            String set = String.format("$0.set$%s($1);", field);
                            if (klass.equals(cc.getName()))
                                f.replace(set);
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
