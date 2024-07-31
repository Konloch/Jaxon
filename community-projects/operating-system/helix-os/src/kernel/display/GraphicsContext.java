package kernel.display;

public abstract class GraphicsContext
{
	
	public abstract void activate();
	
	public abstract int width();
	
	public abstract int height();
	
	public abstract int rgb(int r, int g, int b);
	
	public abstract int argb(int a, int r, int g, int b);
	
	public abstract void swap();
	
	public abstract void clearScreen();
	
	public abstract void pixel(int x, int y, int col);
	
	public abstract void bitmap(int x, int y, Bitmap bitmap, boolean transparent);
	
	public abstract boolean contains(int x, int y);
	
	public abstract void rectangle(int x, int y, int width, int height, int color);
	
}
