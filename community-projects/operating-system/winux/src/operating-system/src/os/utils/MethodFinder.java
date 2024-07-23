package os.utils;

import devices.StaticV24;
import os.interrupt.StackTraverser;
import rte.SClassDesc;
import rte.SMthdBlock;
import rte.SPackage;

public class MethodFinder {

    @SJC.Inline
    public static int getEIP() {
        int ebp = 0;

        // get current ebp offset
        // mov [ebp+xx],ebp
        MAGIC.inline(0x89, 0x6D);
        MAGIC.inlineOffset(1, ebp);
        StackTraverser.reset(ebp);

        // 9 PUSHA register values
        return MAGIC.rMem32(ebp + (9 << 2));
    }

    public static SMthdBlock getMethodName(int eip) { ;
        return loopPackages(eip, SPackage.root);
    }

    private static SMthdBlock loopPackages(int eip, SPackage p) {
        SMthdBlock method = null;

        while(p != null) {
            StaticV24.println(p.name);
            method = loopClasses(eip, p.units);
            if(method != null)
                break;

            method = loopPackages(eip, p.subPacks);
            if(method != null)
                break;

            p = p.nextPack;
        }

        return method;
    }

    private static SMthdBlock loopClasses(int eip, SClassDesc c) {
        SMthdBlock method = null;
        while(c != null) {
            StaticV24.print("/");
            StaticV24.println(c.name);
            method = loopMethods(eip, c.mthds);
            if(method != null)
                break;

            c = c.nextUnit;
        }

        return method;
    }

    private static SMthdBlock loopMethods(int eip, SMthdBlock m) {
        while(m != null) {
            StaticV24.print("/-/");
            StaticV24.print(m.namePar);
            for(int i = m.namePar.length(); i < 60; i++)
                StaticV24.print(' ');
            if(inRange(eip, m))
                break;
            m = m.nextMthd;
        }

        return m;
    }

    private static boolean inRange(int eip, SMthdBlock m) {
        int start = MAGIC.cast2Ref(m);
        int end = start + m._r_scalarSize;
        StaticV24.print("0x");
        StaticV24.printHex(start);
        StaticV24.print(" <= 0x");
        StaticV24.printHex(eip);
        StaticV24.print(" < 0x");
        StaticV24.printHex(end);
        StaticV24.print(" = ");
        StaticV24.println((start <= eip && eip < end) ? "true\n\n\n\n\n\n" : "f");
        return start <= eip && eip < end;
    }
}
