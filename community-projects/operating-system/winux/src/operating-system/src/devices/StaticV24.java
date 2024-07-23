package devices;

public class StaticV24 {
    public static final int BPSBASE = 115200;

    public static int basePort=0x3F8; //com1: 0x3F8, com2: 0x2F8
    public static boolean enabled=true;

    public static int setMode(int bps, int dataBits, int stopBits, boolean parity, boolean oddParity) {
        int divisor, config=0;
        byte dummy=0;

        MAGIC.ignore(dummy);
        if (BPSBASE % bps != 0) return -1;
        if (dataBits < 5 || dataBits > 8) return -2;
        config=dataBits-5;
        if (stopBits == 2) config |= 0x04;
        else if (stopBits != 1) return -3;
        if (parity) {
            if (oddParity) config |= 0x08;
            else config |= 0x18;
        }

        divisor = BPSBASE / bps;
        MAGIC.wIOs8(basePort+3, (byte)0x80);
        MAGIC.wIOs8(basePort, (byte)divisor);
        MAGIC.wIOs8(basePort+1, (byte)(divisor>>8));
        MAGIC.wIOs8(basePort+3, (byte)config);
        MAGIC.wIOs8(basePort+1, (byte)0x03);
        dummy = MAGIC.rIOs8(basePort+2);

        return 0;
    }

    public static void print(char c) {
        if (!enabled) return;
        if (c=='\n') {
            _print('\r'); _print('\n');
        }
        else {
            _print(c);
        }
    }

    private static void _print(char c) {
        while ((MAGIC.rIOs8(basePort+5) & (byte)0x20) == (byte)0);
        MAGIC.wIOs8(basePort, (byte)c);
    }

    private static void doWriteInt(int x) {
        if (x>0) {
            doWriteInt(x/10);
            print((char)(48+x%10));
        }
    }

    private static void doWriteLong(long x) {
        if (x>0l) {
            doWriteLong(x/10l);
            print((char)(48+(int)(x%10l)));
        }
    }

    public static void print(int x) {
        if (x==0) {
            print('0');
            return;
        }
        if (x<0) {
            print('-');
            x=(-x);
        }
        doWriteInt(x);
    }

    public static void printHex(int i) {
        printHex(i, 8);
    }

    public static void printHex(int i, int digits) {
        int v, p;

        for (p=0; p<digits; p++) {
            v=(i>>>((digits-1-p)<<2))&0xF;
            if (v<10) print((char)(v+'0')); //'0'..'9'
            else print((char)(v+('A'-10))); //'A'..'Z'
        }
    }

    public static void printHexln(int i) {
        printHexln(i, 8);
    }

    public static void printHexln(int i, int digits) {
        printHex(i, digits);
        println();
    }

    public static void print(long x) {
        if (x==0l) {
            print('0');
            return;
        }
        if (x<0l) {
            print('-');
            x=(-x);
        }
        doWriteLong(x);
    }

    public static void printIP(int ip) {
        int i=24;
        while (true) {
            print((ip>>>i)&0xFF);
            if ((i-=8)<0) return;
            print('.');
        }
    }

    public static void printMAC(long mac) {
        int i=40;
        while (true) {
            printHex(((int)(mac>>>i))&0xFF, 2);
            if ((i-=8)<0) return;
            print(':');
        }
    }

    public static void print(String str) {
        int i;
        if (str==null) { print("<null>"); return; }
        for (i=0; i<str.length(); i++) print(str.charAt(i));
    }

    public static void print(byte[] ba) {
        int i;
        if (ba==null) { print("<null>"); return; }
        for (i=0; i<ba.length; i++) print((char)ba[i]);
    }

    public static void println() {
        print('\n');
    }

    public static void println(char c) {
        print(c);
        println();
    }

    public static void println(int i) {
        print(i);
        println();
    }

    public static void println(long l) {
        print(l);
        println();
    }

    public static void println(String str) {
        print(str);
        println();
    }
}