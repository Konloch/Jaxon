package app;

public class DrawBitmap
{
	public int width, height;
	public long background;
	
	public DrawBitmap(int width, int height)
	{
		this.width = width;
		this.height = height;
		background = RGBfCol.create(0.078f, 0.361f, 0.753f);
	}
	
	public void renderLine(int[] dest, WindowDraw cTAi)
	{
		int x;
		for (x = 0; x < width; x++)
		{
			cTAi.x = x;
			dest[x] = RGBfCol.toRGB(background);
		}
	}
}