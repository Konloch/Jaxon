package os.interrupt;

import devices.StaticV24;
import os.screen.Color;
import os.screen.Cursor;
import os.screen.Terminal;
import os.utils.MethodFinder;
import rte.SMthdBlock;
import rte.SPackage;

public class BlueScreen {

    private static final int EIP_OFFSET = 9*4;
    private static final int STACK_BEGINNING = 0x9BFFC;

    public static void show() {
        int i;
        // 0x00b3 <=> tableV / vertical line
        Cursor c = Cursor.staticCursor;

        c.setColor(Color.GRAY | Color.BRIGHT, Color.BLUE | Color.BRIGHT);
        c.setCursor(0, 0);
        for(i = 0; i < Terminal.COLS * Terminal.ROWS; i++) {
            c.print(' ');
        }

        c.println("\u00b3     EBP    \u00b3    EIP     \u00b3");

        do {
            c.print("\u00b3 0x");
            c.printHex(StackTraverser.getEbp());

            c.print(" \u00b3 0x");
            c.printHex(StackTraverser.getEip());
            c.print(" \u00b3 ");

            StaticV24.print("Hi hi hi this is 0x");
            StaticV24.printHexln(StackTraverser.getEip());

            SMthdBlock method = MethodFinder.getMethodName(StackTraverser.getEip());
            if(method != null) {
                if(method.lineInCodeOffset != null) {
                    c.print(method.lineInCodeOffset.length);
                    c.print(": ");
                }
                printPackage(method.owner.pack, c);
                c.println(method.namePar);
            } else {
                c.println("no method found");
            }

        } while(StackTraverser.next());

    }

    private static void printPackage(SPackage p, Cursor c) {
        if(p == null || p.name == null)
            return;

        printPackage(p.outer, c);
        c.print(p.name);
        c.print('.');
    }

}
