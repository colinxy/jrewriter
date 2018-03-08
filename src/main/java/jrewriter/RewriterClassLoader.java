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
    }

    @Override
    public Class<?> loadClass(String className)
        throws ClassNotFoundException {

        if (Arrays.stream(whitelist)
            .anyMatch(wl -> className.startsWith(wl))) {
            System.out.println("loading " + className + " by system");

            return super.loadClass(className);
        } else {
            System.out.println("loading " + className);

            // Rewriter rewriter = new GetSetRewriter(pool, className);
            Rewriter rewriter = new IncrementRewriter(pool, className);
            byte[] b = rewriter.toBytecode();
            return defineClass(className, b, 0, b.length);
        }
    }
}
