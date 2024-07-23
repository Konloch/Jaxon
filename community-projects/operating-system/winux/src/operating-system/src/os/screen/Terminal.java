package os.screen;

import os.keyboard.ASCII;
import os.keyboard.KeyEvent;
import os.keyboard.KeyboardController;
import os.utils.OutStream;

public class Terminal extends OutStream {
    public static final int COLS = 80;
    public static final int ROWS = 25;

    public static final int MEM_START = 0xB8000;
    public static final int MEM_END = MEM_START + (COLS * ROWS * 2);

    public static Terminal focused = null;
    public static TerminalMemory mem;
    static {
        mem = (TerminalMemory) MAGIC.cast2Struct(MEM_START);
    }

    private int[][] content;
    private int x, y;

    private int color;

    private boolean cursorEnabled;

    public Terminal() {
        this((char) 0);
    }

    public Terminal(char fill) {
        this(fill, Color.mix(Color.GRAY, Color.BLACK));
    }

    public Terminal(char fill, int color) {
        content = new int[Terminal.ROWS][Terminal.COLS];

        x = y = 0;
        this.color = color;

        cursorEnabled = false;

        color <<= 8;
        for(int y = 0; y < content.length; y++) {
            for(int x = 0; x < content[y].length; x++) {
                content[y][x] = (short) (color | fill);
            }
        }
    }

    @SJC.Inline
    private int calculateMemValue(char c) {
        return (color << 8) | c;
    }

    public void clear() {
        int i;
        for(i = MEM_START; i < MEM_END; i++) {
            MAGIC.wMem8(i, (byte) 0);
        }
    }

    public void focus() {
        Terminal.focused = this;
        int ptr = 0;
        for (int y = 0; y < ROWS; y++) {
            for (int x = 0; x < COLS; x++) {
                mem.screen[ptr++] = (short) content[y][x];
            }
        }

        if(cursorEnabled)
            enableCursor();
        else
            disableCursor();
    }

    public void moveCursorHorizontally(int amount) {
        // todo optimize
        if(amount < 0) {
            while(amount++ != 0) {
                if(--x < 0) {
                    x = COLS - 1;
                    if(--y < 0)
                        y = ROWS - 1;
                }
            }
        } else if(amount > 0) {
            while(amount-- != 0) {
                if(++x >= COLS) {
                    x = 0;
                    if(++y >= ROWS) {
                        y = 0;
                    }
                }
            }
        }
    }

    /*
     * ------- BLINKING CURSOR SECTION ----------
     */
    @SJC.Inline
    private void writeCursor(int b1, int b2) {
        MAGIC.wIOs8(0x03D4, (byte) b1);
        MAGIC.wIOs8(0x03D5, (byte) b2);
    }

    public void enableCursor() {
        cursorEnabled = true;
//        MAGIC.wIOs8(0x3D4, (byte) 0x0A);
//        MAGIC.wIOs8(0x3D5, (byte) 0x02);
        writeCursor(0x0A, 0x01);

        updateCursorPos();
    }

    public  void disableCursor() {
        cursorEnabled = false;
//        MAGIC.wIOs8(0x3D4, (byte) 0x0A);
//        MAGIC.wIOs8(0x3D5, (byte) 0x20);
        writeCursor(0x0A, 0x20);
    }

    public void moveCursor(int x, int y) {
//        MAGIC.wIOs8(0x3D4, (byte) 0x0F);
//        MAGIC.wIOs8(0x3D5, (byte) (x));
//        MAGIC.wIOs8(0x3D4, (byte) 0x0E);
//        MAGIC.wIOs8(0x3D5, (byte) (y));
        int pos = x + (y * COLS);
        this.x = x;
        this.y = y;
        writeCursor(0x0F, pos);
        writeCursor(0x0E, pos >>> 8);
    }

    @SJC.Inline
    public void updateCursorPos() {
        moveCursor(x, y);
    }

    public int getCursorPosition() {
        int pos = 0;
        MAGIC.wIOs8(0x3D4, (byte) 0x0F);
        pos |= MAGIC.rIOs8(0x3D5);
        MAGIC.wIOs8(0x3D4, (byte) 0x0E);
        pos |= ((int)MAGIC.rIOs8(0x3D5)) << 8;
        return pos;
    }
    /*
     * -------- /BLINKING CURSOR SECTION DONE/ ---------
     */


    public void setColor(int fg, int bg) {
        // Layout
        // Bit  7 | 6 5 4 | 3 | 2 1 0
        // Fct  * |  bg   | * |  fg
        color = Color.mix(fg, bg);
    }

    public void setColor(int c) {
        this.color = c & 0xFF;
    }

    public int getColor() {
        return this.color;
    }

    public void setPos(int newX, int newY) {
        if((0 <= newX && newX <= COLS) && (0 <= newY && newY <= ROWS)) {
            x = newX;
            y = newY;
            if(cursorEnabled)
                moveCursor(newX, newY);
        }
    }

    @SJC.Inline
    public int getX() {
        return x;
    }

    @SJC.Inline
    public int getY() {
        return y;
    }

    public void writeTo(int tmpX, int tmpY, String s) {
        int xOld = x;
        int yOld = y;

        setPos(tmpX, tmpY);
        print(s);
        setPos(xOld, yOld);
    }

    // print overloads
    // only chars are actually written to memory, thus only the char-method does boundary-checks
    public void print(char c) {

        if(c == '\n') {
            x = 0;
            if(++y >= ROWS)
                y = 0;
            return;
        } else if(c == '\t') {
            print(' ');
            print(' ');
            print(' ');
            print(' ');
            return;
        }

        int value = calculateMemValue(c);
        content[y][x] = value;

        if(focused == this) {
            int index = (y * Terminal.COLS) + x;
            if(0 <= index && index < (COLS * ROWS))
                mem.screen[index] = (short) value;
        }

        if(++x >= COLS) {
            x = 0;
            if(++y >= ROWS)
                y = 0;
        }
    }

    public static class TerminalMemory extends STRUCT {
        @SJC(count = Terminal.COLS * Terminal.ROWS)
        public short[] screen;
    }
}
