package jrewriter;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.NotFoundException;
import javassist.bytecode.*;
import java.util.List;


public class RefFinder {

    public static void fieldRef(CtClass cc) {
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

        // if this method operates on the same object that it
        // references the field from, do nothing
    }
}
