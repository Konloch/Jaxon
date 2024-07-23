package os.utils;

import os.screen.Terminal;

public abstract class OutStream {

    public abstract void print(char c);

    public void print(int x) {
        int reverse, digits;

        if(x < 0) {
            print('-');
            x = -x;
        }

        reverse = 0;
        digits = 0;
        do {
            reverse = (reverse * 10) + (x % 10);
            x /= 10;
            digits++;
        } while(x > 0);

        while(digits-- > 0) {
            print((char) (reverse % 10 + '0'));
            reverse /= 10;
        }
    }

    public void printHex(byte b) {
        String hexChars = "0123456789ABCDEF";
        print(hexChars.charAt((b >>> 4) & 0x0F));
        print(hexChars.charAt(b & 0x0F));
    }

    public void printHex(short s) {
        printHex((byte) (s >>> 8));
        printHex((byte) (s & 0xFF));
    }

    public void printHex(int x) {
        printHex((short) (x >>> 16));
        printHex((short) (x & 0xFFFF));
    }

    public void printHex(long x) {
        printHex((int) (x >>> 32));
        printHex((int) (x & 0xFFFFFFFF));
    }

    public void print(long x) {
        long reverse, digits;

        if(x < 0) {
            print('-');
            x = -x;
        }

        reverse = 0;
        digits = 0;
        do {
            reverse = (reverse * 10) + (x % 10);
            x /= 10;
            digits++;
        } while(x > 0);

        while(digits-- > 0) {
            print((char) (reverse % 10 + '0'));
            reverse /= 10;
        }
    }

    public void print(String str) {
        int i;
        for (i=0; i<str.length(); i++) print(str.charAt(i));
    }

    public void print(String str, int start, int end) {
        int i;
        if(start < 0)
            start = 0;
        if(end > str.length())
            end = str.length();

        for(i = start; i < end; i++) print(str.charAt(i));
    }

    // println overloads
    @SJC.Inline
    public void println() {
        print('\n');
    }

    public void println(char c) {
        print(c);
        println();
    }

    public void println(int i) {
        print(i);
        println();
    }

    public void println(long l) {
        print(l);
        println();
    }

    public void println(String str) {
        print(str);
        println();
    }
}
