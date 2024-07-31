package kernel.display;

import kernel.Kernel;
import kernel.memory.Memory;

public class Bitmap
{
	public int width;
	public int height;
	public int[] pixelData;
	public boolean isTransparent;
	
	public Bitmap(int width, int height, int[] pixelData)
	{
		this.width = width;
		this.height = height;
		this.pixelData = pixelData;
		isTransparent = anyTransparency();
	}
	
	public Bitmap(int width, int height, boolean isTransparent)
	{
		this.width = width;
		this.height = height;
		this.isTransparent = isTransparent;
		pixelData = new int[this.width * this.height];
	}
	
	public void clear()
	{
		int from = MAGIC.addr(pixelData[0]);
		int len = pixelData.length;
		Memory.Memset(from, len * 4, (byte) 0);
	}
	
	public int getPixel(int x, int y)
	{
		if (x < 0 || y < 0 || x >= width || y >= height)
			Kernel.panic("Bitmap.GetPixel: out of bounds (".append(x).append(", ").append(y).append(")"));
		
		int index = index(x, y);
		
		if (index < 0 || index >= pixelData.length)
			Kernel.panic("Bitmap.GetPixel: index out of bounds");
		
		return pixelData[index];
	}
	
	@SJC.Inline
	public void setPixel(int x, int y, int color)
	{
		if (x < 0 || y < 0 || x >= width || y >= height)
		{
			return;
		}
		
		pixelData[x + y * width] = color;
	}
	
	@SJC.Inline
	public void darken(int amount)
	{
		for (int i = 0; i < pixelData.length; i++)
		{
			int color = pixelData[i];
			int r = (color >> 16) & 0xFF;
			int g = (color >> 8) & 0xFF;
			int b = color & 0xFF;
			
			r = (r - amount) & ~((r - amount) >> 31);
			g = (g - amount) & ~((g - amount) >> 31);
			b = (b - amount) & ~((b - amount) >> 31);
			pixelData[i] = 0xFF000000 | (r << 16) | (g << 8) | b;
		}
	}
	
	public void rectangle(int x, int y, int width, int height, int color)
	{
		if (pixelData == null)
			Kernel.panic("Bitmap.Rectangle: PixelData is null");
		
		if (pixelData.length == 0)
			Kernel.panic("Bitmap.Rectangle: PixelData is len 0");
		
		// only draw visible part
		if (x < 0)
		{
			width += x;
			x = 0;
		}
		
		if (x + width > this.width)
			width = this.width - x;
		
		if (y < 0)
		{
			height += y;
			y = 0;
		}
		
		if (y + height > this.height)
			height = this.height - y;
		
		// this should not happen but it does and im confused
		// somehow returning fixes it but it makes no sense
		if (width <= 0 || height <= 0)
			return;
		
		for (int i = 0; i < height; i++)
		{
			int index = index(x, y + i);
			int addr = MAGIC.addr(pixelData[index]);
			Memory.Memset32(addr, width, color);
		}
	}
	
	public void blit(int x, int y, Bitmap bitmap, boolean transparent)
	{
		if (bitmap == null)
			Kernel.panic("VESAGraphics.setBitmap: mode or bitmap is null");
		
		int off_bitmap_x = 0;
		int off_bitmap_y = 0;
		int up_to_x = bitmap.width;
		int up_to_y = bitmap.height;
		
		if (x < 0)
		{
			off_bitmap_x = -x;
			up_to_x += x;
			x = 0;
		}
		
		if (y < 0)
		{
			off_bitmap_y = -y;
			up_to_y += y;
			y = 0;
		}
		
		if (x + up_to_x > width)
			up_to_x = width - x;
		
		if (y + up_to_y > height)
			up_to_y = height - y;
		
		if (up_to_x <= 0 || up_to_y <= 0)
			Kernel.panic("Bitmap.blit: up_to_x <= 0 || up_to_y <= 0");
		
		if (transparent)
		{
			for (int cur_y = 0; cur_y < up_to_y; cur_y++)
			{
				for (int cur_x = 0; cur_x < up_to_x; cur_x++)
				{
					int self_index = index(x + cur_x, y + cur_y);
					int bitmap_index = bitmap.index(off_bitmap_x + cur_x, off_bitmap_y + cur_y);
					pixelData[self_index] = blend(bitmap.pixelData[bitmap_index], pixelData[self_index]);
				}
			}
		}
		else
		{
			for (int cur_y = 0; cur_y < up_to_y; cur_y++)
			{
				int index = index(x, y + cur_y);
				int addr = MAGIC.addr(pixelData[index]);
				int bitmap_index = bitmap.index(off_bitmap_x, off_bitmap_y + cur_y);
				int bitmap_addr = MAGIC.addr(bitmap.pixelData[bitmap_index]);
				Memory.Memcopy32(bitmap_addr, addr, up_to_x);
			}
		}
		
	}
	
	public Bitmap scale(int newWidth, int newHeight)
	{
		int[] newPixelData = new int[newWidth * newHeight];
		int x_ratio = (width << 16) / newWidth + 1;
		int y_ratio = (height << 16) / newHeight + 1;
		
		for (int i = 0; i < newHeight; i++)
		{
			for (int j = 0; j < newWidth; j++)
			{
				int x = (j * x_ratio) >> 16;
				int y = (i * y_ratio) >> 16;
				newPixelData[j + i * newWidth] = pixelData[x + y * width];
			}
		}
		
		return new Bitmap(newWidth, newHeight, newPixelData);
	}
	
	@SJC.Inline
	private int blend(int withAlpha, int noAlpha)
	{
		int alpha = (withAlpha >> 24) & 0xFF;
		
		if (alpha == 0)
			return noAlpha;
		
		if (alpha == 255)
			return withAlpha;
		
		int alpha_rem = 255 - alpha;
		int r = (alpha * ((withAlpha >> 16) & 0xFF) + alpha_rem * ((noAlpha >> 16) & 0xFF)) >> 8;
		int g = (alpha * ((withAlpha >> 8) & 0xFF) + alpha_rem * ((noAlpha >> 8) & 0xFF)) >> 8;
		int b = (alpha * (withAlpha & 0xFF) + alpha_rem * (noAlpha & 0xFF)) >> 8;
		return 0xFF000000 | (r << 16) | (g << 8) | b;
	}
	
	@SJC.Inline
	public int row(int y)
	{
		return y * width;
	}
	
	@SJC.Inline
	public int index(int x, int y)
	{
		return x + y * width;
	}
	
	public boolean anyTransparency()
	{
		for (int i = 0; i < pixelData.length; i++)
		{
			if ((pixelData[i] & 0xFF000000) != 0)
				return true;
		}
		
		return false;
	}
}
