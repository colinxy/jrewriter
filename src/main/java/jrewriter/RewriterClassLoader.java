package jrewriter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import javassist.*;
import java.util.*;


public class RewriterClassLoader extends ClassLoader {
    final Logger logger = LoggerFactory.getLogger(RewriterClassLoader.class);

    private ClassPool pool;

    final static boolean DEBUG = !Optional
        .ofNullable(System.getenv("JREWRITER_DEBUG"))
        .orElse("")
        .equals("");

    final String[] whitelist = {
        "java.",
        "javafx.",
        "javax.",
        "jrewrite.",
        "sun.",
    };

    public RewriterClassLoader(ClassLoader parent) {
        super(parent);

        pool = ClassPool.getDefault();
        if (DEBUG)
            CtClass.debugDump = "./debugDump";
    }

    @Override
    public Class<?> loadClass(String className)
        throws ClassNotFoundException {
        Class<?> loaded = findLoadedClass(className);
        if (loaded != null)
            return loaded;

        for (String wl : whitelist) {
            if (className.startsWith(wl)) {
                logger.debug("System loading " + className);

                return super.loadClass(className);
            }
        }
        logger.info("Loading " + className);

        CtClass cc;
        try {
            cc = pool.get(className);
        } catch (NotFoundException ex) {
            ex.printStackTrace();
            throw new ClassNotFoundException(className + " not found", ex);
        }

        // Rewriter rewriter = new GetSetRewriter(pool, cc);
        Rewriter rewriter = new IncrementRewriter(pool, cc);
        byte[] bytecode = rewriter.toBytecode();

        logger.info("Loaded " + className);
        return defineClass(className, bytecode, 0, bytecode.length);
    }
}
