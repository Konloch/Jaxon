package formats.fonts;

import kernel.display.Bitmap;
import kernel.display.GraphicsContext;
import java.util.BitHelper;

public abstract class AFont
{
	public abstract int width();
	
	public abstract int height();
	
	public abstract int spacingW();
	
	public abstract int spacingH();
	
	public abstract int characterBitmapLine(int ch, int line);
	
	public abstract boolean vertical();
	
	public void renderToDisplay(GraphicsContext display, int x, int y, int ch, int color)
	{
		int fontWidth = width();
		int fontHeight = height();
		boolean fontVertical = vertical();
		
		for (int charLine = 0; charLine < fontWidth; charLine++)
		{
			int b = characterBitmapLine(ch, charLine);
			for (int lineBit = 0; lineBit < fontHeight; lineBit++)
			{
				int bit = BitHelper.getBit(b, lineBit);
				int posX = x;
				int posY = y;
				if (fontVertical)
				{
					posX += charLine;
					posY += lineBit;
				}
				else
				{
					posX += lineBit;
					posY += charLine;
				}
				
				if (bit == 1)
					display.pixel(posX, posY, color);
			}
		}
	}
	
	public void renderToBitmap(Bitmap display, int x, int y, int ch, int color)
	{
		int fontWidth = width();
		int fontHeight = height();
		boolean fontVertical = vertical();
		
		for (int charLine = 0; charLine < fontWidth; charLine++)
		{
			int b = characterBitmapLine(ch, charLine);
			for (int lineBit = 0; lineBit < fontHeight; lineBit++)
			{
				int bit = BitHelper.getBit(b, lineBit);
				int posX = x;
				int posY = y;
				if (fontVertical)
				{
					posX += charLine;
					posY += lineBit;
				}
				else
				{
					posX += lineBit;
					posY += charLine;
				}
				
				if (bit == 1)
					display.setPixel(posX, posY, color);
			}
		}
	}
}
