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
    }


        // if this method operates on the same object that it
        // references the field from, do nothing


    public static void incrementRef(CtClass cc) throws BadBytecode {
        ClassFile cf = cc.getClassFile();
        ConstPool constPool = cf.getConstPool();

        List<MethodInfo> methods = cf.getMethods();

        for (MethodInfo minfo : methods) {
            CodeAttribute ca = minfo.getCodeAttribute();
            CodeIterator ci = ca.iterator();
            CodeIterator temp = ca.iterator();

            while (ci.hasNext()) {
                ci.next();
                int index = temp.next();

                int constPoolIndex;
                int startIndex;
                int endIndex;

                if (temp.byteAt(index) == Opcode.GETFIELD)
                {
                    startIndex = index;
                    constPoolIndex = temp.u16bitAt(index+1);
                    System.out.println("getfield "+constPoolIndex);
                    index = temp.next();
                    if (temp.byteAt(index) == Opcode.ICONST_1)
                    {
                        System.out.println("ICONST_1");
                        index = temp.next();
                        if (temp.byteAt(index) == Opcode.IADD)
                        {
                            System.out.println("IADD");
                            index = temp.next();
                            if (temp.byteAt(index) == Opcode.PUTFIELD) {
                                if (constPoolIndex == temp.u16bitAt(index+1)) {
                                    System.out.println("putfield "+constPoolIndex);
                                    endIndex = temp.next();
                                }
                            }
                        }
                    }
                }
            }
    }
    }
}
