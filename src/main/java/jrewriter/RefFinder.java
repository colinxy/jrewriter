package jrewriter;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.NotFoundException;
import javassist.bytecode.*;
import java.util.List;


public class RefFinder {

    public static void fieldRef(ClassPool pool, String className) {
        CtClass cc;
        try {
            cc = pool.get(className);
        } catch (NotFoundException ex) {
            ex.printStackTrace();
            throw new RuntimeException(ex.getMessage());
        }
        ClassFile cf = cc.getClassFile();
        ConstPool constPool = cf.getConstPool();

        List<FieldInfo> fields = cf.getFields();
        List<MethodInfo> methods = cf.getMethods();

        for (MethodInfo minfo : methods) {
            CodeAttribute ca = minfo.getCodeAttribute();
            CodeIterator ci = ca.iterator();

            while (ci.hasNext()) {
                int index;
                try {
                    index = ci.next();
                } catch (BadBytecode ex) {
                    ex.printStackTrace();
                    throw new RuntimeException(ex.getMessage());
                }

                int constPoolIndex;
                switch (ci.byteAt(index)) {
                case Opcode.GETFIELD:
                    constPoolIndex = ci.u16bitAt(index+1);
                    System.out.println("getfield " + constPoolIndex);
                    break;
                }
            }
        }
    }
}
