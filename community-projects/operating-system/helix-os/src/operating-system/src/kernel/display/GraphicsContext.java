package kernel.display;

public abstract class GraphicsContext {

    public abstract void Activate();

    public abstract int Width();

    public abstract int Height();

    public abstract int Rgb(int r, int g, int b);

    public abstract int Argb(int a, int r, int g, int b);

    public abstract void Swap();

    public abstract void ClearScreen();

    public abstract void Pixel(int x, int y, int col);

    public abstract void Bitmap(int x, int y, Bitmap bitmap, boolean transparent);

    public abstract boolean Contains(int x, int y);

    public abstract void Rectangle(int x, int y, int width, int height, int color);

}
