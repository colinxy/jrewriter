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
        "sun.",
        // this project's dependencies (cannot be loaded again)
        "jrewriter.",
        "javassist.",
        "org.slf4j.",
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
        logger.debug("from " + pool.find(className) +
                     " with " + this);
        logger.debug(pool.toString()); // classpath

        CtClass cc;
        try {
            cc = pool.get(className);
        } catch (NotFoundException ex) {
            logger.error(
                "Failed to load class " + className + " with " + this,
                ex);
            throw new ClassNotFoundException(className + " not found", ex);
        }

        // Rewriter rewriter = new GetSetRewriter(pool, cc);
        Rewriter rewriter = new IncrementRewriter(pool, cc);
        byte[] bytecode = rewriter.toBytecode();

        logger.info("Loaded " + className);
        return defineClass(className, bytecode, 0, bytecode.length);
    }

    public ClassPath insertClassPath(ClassPath cp) {
        return pool.insertClassPath(cp);
    }
}
