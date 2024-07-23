package os.screen;

public class Cursor {

    private int pos = Terminal.MEM_START;
    private byte color = 0x70;

    public static Cursor staticCursor;
    static {
        staticCursor = new Cursor();
    }

    // setColor and setCursor
    @SJC.Inline
    public void setColor(int fg, int bg) {
        // Layout
        // Bit  7 | 6 5 4 | 3 | 2 1 0
        // Fct  * |  bg   | * |  fg
        color = (byte) Color.mix(fg, bg);
    }

    @SJC.Inline
    public void setColor(byte c) {
        this.color = c;
    }

    @SJC.Inline
    public byte getColor() {
        return this.color;
    }

    public void setCursor(int newX, int newY) {
        if(  newX < 0 || newY < 0 || newX >= Terminal.COLS || newY >= Terminal.ROWS) {
            this.pos = Terminal.MEM_START;
        } else {
            this.pos = Terminal.MEM_START + ((newY * Terminal.COLS * 2) + (newX * 2));
        }
    }
    
    // print overloads
    // only chars are actually written to memory, thus only the char-method does boundary-checks
    public void print(char c) {

        if(c == '\n') {

            // crazy arithmetic that 50% of the time works every time
            // maaaaybe switch to x-y-coordinates instead of juggling with memory addresses
            if(pos >= Terminal.MEM_END - (Terminal.COLS * 2)) {
                // wrap to beginning
                pos = Terminal.MEM_START;
            } else {
                // add remaining space of the line to position
                pos += (Terminal.COLS * 2) - ((pos - Terminal.MEM_START) % (Terminal.COLS * 2));
            }
            return;
        }

        MAGIC.wMem8(pos++, (byte)c);
        MAGIC.wMem8(pos++, color);

        // loop back to beginning?
        if(pos >= Terminal.MEM_END) {
            pos = Terminal.MEM_START;
        }
    }
    

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
    
    // println overloads
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

    // debugging prints
    public static void directPrintInt(int value, int base, int len, int x, int y, int col) {
        Cursor cursor = new Cursor();
        cursor.setCursor(x, y);
        cursor.color = (byte) col;

        if(base == 10)
            cursor.print(value);
        else if(base == 16)
            cursor.printHex(value);
    }

    public static void directPrintInt(int value, int x, int y, int col) {
        Cursor cursor = new Cursor();
        cursor.setCursor(x, y);
        cursor.color = (byte) col;
        
        cursor.print(value);
    }
    
    public static void directPrintChar(char c, int x, int y, int col) {
        Cursor cursor = new Cursor();
        cursor.setCursor(x, y);
        cursor.color = (byte) col;
        cursor.print(c);
    }
    
    public static void directPrintString(String s, int x, int y, int col) {
        Cursor cursor = new Cursor();
        cursor.setCursor(x, y);
        cursor.color = (byte) col;
        cursor.print(s);
    }
}
