package jrewriter;

import java.util.List;
import java.util.ArrayList;
import javassist.*;
import javassist.bytecode.*;


public class RefFinder {

    public static void fieldRef(CtClass cc) throws BadBytecode {
        ClassFile cf = cc.getClassFile();
        ConstPool constPool = cf.getConstPool();

        List<FieldInfo> fields = cf.getFields();
        List<MethodInfo> methods = cf.getMethods();

        for (MethodInfo minfo : methods) {
            CodeAttribute ca = minfo.getCodeAttribute();
            CodeIterator ci = ca.iterator();

            while (ci.hasNext()) {
                int index = ci.next();

                int constPoolIndex;
                switch (ci.byteAt(index)) {
                case Opcode.GETFIELD:
                    constPoolIndex = ci.u16bitAt(index+1);
                    System.out.println(minfo.getName()
                                       + ": getfield "
                                       + constPoolIndex);
                    break;
                case Opcode.PUTFIELD:
                    constPoolIndex = ci.u16bitAt(index+1);
                    System.out.println(minfo.getName()
                                       + ": putfield "
                                       + constPoolIndex);
                    break;
                }
            }
        }

        // if this method operates on the same object that it
        // references the field from, do nothing
    }

    public static void incrementRef() throws BadBytecode {
        // TODO: code to identify sequence of bytecode that increments field
    }
}
