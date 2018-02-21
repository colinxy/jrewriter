package jrewriter;

import java.io.*;
import javassist.ClassPool;
import javassist.NotFoundException;


public class RewriterClassLoader extends ClassLoader {
    private ClassPool pool;

    public RewriterClassLoader(ClassLoader parent) {
        super(parent);

        pool = ClassPool.getDefault();
    }

    @Override
    public Class<?> loadClass(String className)
        throws ClassNotFoundException {

        if (className.startsWith("jrewrite.")) {
            // do not load myself
            return super.loadClass(className);
        } else {
            System.out.println("loading " + className);
            RefFinder.fieldRef(pool, className);
            return super.loadClass(className);
        }
    }
}
