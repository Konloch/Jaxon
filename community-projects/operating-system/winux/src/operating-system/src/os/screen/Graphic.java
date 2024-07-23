package os.screen;

public class Graphic {
    public static final int HEIGHT = 200;
    public static final int WIDTH = 320;

    public static final int GRAPHIC_START = 0xA0000;
    public static final int GRAPHIC_END = GRAPHIC_START + (HEIGHT * WIDTH);

    protected static class GraphicMem extends STRUCT {
        @SJC(count=320*200)
        public byte[] img;
    }

    public GraphicMem mem;
    private int ptr = 0;

    public Graphic() {
        mem = (GraphicMem) MAGIC.cast2Struct(0xA0000);
    }

    public void setPtrAndInc(byte val) {
        mem.img[ptr++] = val;
        if(ptr >= GRAPHIC_END)
            ptr = GRAPHIC_START;
    }

    public void setPtrCoord(int y, int x) {
        if(y < 0 || y >= HEIGHT || x < 0 || x >= WIDTH)
            return;

        ptr = x + (y * WIDTH);
    }
}
