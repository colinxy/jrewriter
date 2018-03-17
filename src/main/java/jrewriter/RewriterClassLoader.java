package jrewriter;

import java.io.*;
import javassist.*;
import java.util.*;


public class RewriterClassLoader extends ClassLoader {
    private ClassPool pool;

    String[] whitelist = {
        "jrewrite.",
        "java.",
        "sun.",
    };

    public RewriterClassLoader(ClassLoader parent) {
        super(parent);

        pool = ClassPool.getDefault();
        // XXX: debug
        CtClass.debugDump = "./debugDump";
    }

    @Override
    public Class<?> loadClass(String className)
        throws ClassNotFoundException {
        Class<?> loaded = findLoadedClass(className);
        if (loaded != null)
            return loaded;

        if (Arrays.stream(whitelist)
            .anyMatch(wl -> className.startsWith(wl))) {
            System.out.println("loading " + className + " by system");

            return super.loadClass(className);
        } else {
            System.out.println("loading " + className);

            CtClass cc;
            try {
                cc = pool.get(className);
            } catch (NotFoundException ex) {
                ex.printStackTrace();
                throw new RuntimeException(ex);
            }

            // Rewriter rewriter = new GetSetRewriter(pool, cc);
            Rewriter rewriter = new IncrementRewriter(pool, cc);
            byte[] bytecode = rewriter.toBytecode();

            return defineClass(className, bytecode, 0, bytecode.length);
        }
    }
}
