package os.interrupt;

import devices.StaticV24;
import os.keyboard.KeyboardController;
import os.screen.Cursor;

public class Handler {
    public static int time = 0;

    // 0x00
    @SJC.Interrupt
    public static void divByZero() {
        StaticV24.println("Div by 0");
        Cursor.staticCursor.println("Div by 0");
    }

    // 0x01
    @SJC.Interrupt
    public static void debug() {
        StaticV24.println("Debug Exception");
        Cursor.staticCursor.println("Debug Exception");
    }

    // 0x02
    @SJC.Interrupt
    public static void nmi() {
        StaticV24.println("NMI Exception");
        Cursor.staticCursor.println("NMI Exception");
    }

    // 0x03
    @SJC.Interrupt
    public static void breakpoint() {
        int ebp = 0;

        // get current ebp offset
        // mov [ebp+xx],ebp
        MAGIC.inline(0x89, 0x6D);
        MAGIC.inlineOffset(1, ebp);
        StackTraverser.reset(ebp);
        BlueScreen.show();
        while (true) ;
    }

    // 0x04
    @SJC.Interrupt
    public static void intoOverflow() {
        StaticV24.println("INTO Overflow");
        Cursor.staticCursor.println("INTO Overflow");
    }

    // 0x05
    @SJC.Interrupt
    public static void indexOutOfRange() {
        StaticV24.println("Index Out Of Range");
        Cursor.staticCursor.println("Index Out Of Range");
    }

    // 0x06
    @SJC.Interrupt
    public static void invalidOpcode() {
        StaticV24.println("Invalid Opcode");
        Cursor.staticCursor.println("Invalid Opcode");
    }

    // 0x08
    @SJC.Interrupt
    public static void doubleFault() {
        StaticV24.println("Double fault");
        Cursor.staticCursor.println("Double fault");
    }

    // 0x0D
    @SJC.Interrupt
    public static void generalProtectionError() {
        StaticV24.println("General Protection Error");
        Cursor.staticCursor.println("General Protection Error");
    }

    // 0x0E
    @SJC.Interrupt
    public static void pageFault() {
        StaticV24.println("Page Fault");
        Cursor.staticCursor.println("Page Fault");
    }

    // 0x20
    @SJC.Interrupt
    public static void timer() {
        Handler.time++;
        MAGIC.wIOs8(Interrupt.MASTER, (byte) 0x20);
    }

    // 0x21
    @SJC.Interrupt
    public static void keyboard() {
        KeyboardController.processIOBuffer();
        MAGIC.wIOs8(Interrupt.MASTER, (byte) 0x20);
    }

    // 0x22 - ...
    @SJC.Interrupt
    public static void otherDevices() {
        StaticV24.println("Other Devices");
        Cursor.staticCursor.println("Other Devices");
    }

    // 0x22 device
    @SJC.Interrupt
    public static void dev_22() {
        StaticV24.println("Device 0x22");
        Cursor.staticCursor.println("Device 0x22");
    }

    // 0x23 device
    @SJC.Interrupt
    public static void dev_23() {
        StaticV24.println("Device 0x23");
        Cursor.staticCursor.println("Device 0x23");
    }

    // 0x24 device
    @SJC.Interrupt
    public static void dev_24() {
        StaticV24.println("Device 0x24");
        Cursor.staticCursor.println("Device 0x24");
    }

    // 0x25 device
    @SJC.Interrupt
    public static void dev_25() {
        StaticV24.println("Device 0x25");
        Cursor.staticCursor.println("Device 0x25");
    }

    // 0x26 device
    @SJC.Interrupt
    public static void dev_26() {
        StaticV24.println("Device 0x26");
        Cursor.staticCursor.println("Device 0x26");
    }

    // 0x27 device
    @SJC.Interrupt
    public static void dev_27() {
        StaticV24.println("Device 0x27");
        Cursor.staticCursor.println("Device 0x27");
    }

    // 0x28 device
    @SJC.Interrupt
    public static void dev_28() {
        StaticV24.println("Device 0x28");
        Cursor.staticCursor.println("Device 0x28");
    }

    // 0x29 device
    @SJC.Interrupt
    public static void dev_29() {
        StaticV24.println("Device 0x29");
        Cursor.staticCursor.println("Device 0x29");
    }

    // 0x2A device
    @SJC.Interrupt
    public static void dev_2A() {
        StaticV24.println("Device 0x2A");
        Cursor.staticCursor.println("Device 0x2A");
    }

    // 0x2B device
    @SJC.Interrupt
    public static void dev_2B() {
        StaticV24.println("Device 0x2B");
        Cursor.staticCursor.println("Device 0x2B");
    }

    // 0x2C device
    @SJC.Interrupt
    public static void dev_2C() {
        StaticV24.println("Device 0x2C");
        Cursor.staticCursor.println("Device 0x2C");
    }

    // 0x2D device
    @SJC.Interrupt
    public static void dev_2D() {
        StaticV24.println("Device 0x2D");
        Cursor.staticCursor.println("Device 0x2D");
    }

    // 0x2E device
    @SJC.Interrupt
    public static void dev_2E() {
        StaticV24.println("Device 0x2E");
        Cursor.staticCursor.println("Device 0x2E");
    }

    // 0x2F device
    @SJC.Interrupt
    public static void dev_2F() {
        StaticV24.println("Device 0x2F");
        Cursor.staticCursor.println("Device 0x2F");
    }

    @SJC.Interrupt
    public static void unhandled() {
        StaticV24.println("Unhandled");
        Cursor.staticCursor.println("Unhandled");
    }
}