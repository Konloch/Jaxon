package os.screen;

public class Color {
    public static final int BLACK = 0;
    public static final int BLUE = 1;
    public static final int GREEN = 2;
    public static final int CYAN = 3;
    public static final int RED = 4;
    public static final int VIOLET = 5;
    public static final int BROWN = 6;
    public static final int GRAY = 7;

    public static final int BRIGHT = 8;

    public static int DEFAULT;

    static {
        DEFAULT = mix(GRAY, BLACK);
    }

    @SJC.Inline
    public static int mix(int foreground, int background) {
        return (((background & 0x0F) << 4) | (foreground & 0x0F));
    }
}
