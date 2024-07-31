package kernel.display.vesa;

import kernel.Kernel;
import kernel.bios.call.DisplayModes;
import kernel.display.Bitmap;
import kernel.display.GraphicsContext;
import kernel.memory.Memory;
import kernel.trace.logging.Logger;

public class VESAGraphics extends GraphicsContext
{
	public VESAMode curMode;
	private Bitmap buffer;
	private boolean needsRedraw;
	
	public VESAGraphics(VESAMode mode)
	{
		if (mode == null)
		{
			Kernel.panic("VESAGraphics.setMode: mode is null");
		}
		if (mode.ColorDepth != 32)
		{
			Kernel.panic("VESAGraphics.setMode: only 32 bit color depth is supported");
		}
		buffer = new Bitmap(mode.XRes, mode.YRes, false);
		needsRedraw = true;
		curMode = mode;
		
		Logger.info("VESA", "SetMode to ".append(curMode.debug()));
	}
	
	@Override
	public void Activate()
	{
		Logger.info("VESA", "Activate VESA Graphics Mode");
		DisplayModes.SetVesaMode(curMode.ModeNr);
	}
	
	@Override
	public int Width()
	{
		return this.curMode.XRes;
	}
	
	@Override
	public int Height()
	{
		return this.curMode.YRes;
	}
	
	@Override
	public int Rgb(int r, int g, int b)
	{
		int red, green, blue;
		red = Math.Clamp(r, 0, 255);
		green = Math.Clamp(g, 0, 255);
		blue = Math.Clamp(b, 0, 255);
		return (blue << 0) | (green << 8) | (red << 16) | (255 << 24);
	}
	
	@Override
	public int Argb(int a, int r, int g, int b)
	{
		int red, green, blue, alpha;
		red = Math.Clamp(r, 0, 255);
		green = Math.Clamp(g, 0, 255);
		blue = Math.Clamp(b, 0, 255);
		alpha = Math.Clamp(a, 0, 255);
		return (blue << 0) | (green << 8) | (red << 16) | (alpha << 24);
	}
	
	@Override
	public void Pixel(int x, int y, int col)
	{
		if (x < 0 || y < 0 || x >= curMode.XRes || y >= curMode.YRes)
		{
			return;
		}
		buffer.SetPixel(x, y, col);
		needsRedraw = true;
	}
	
	@Override
	public void Bitmap(int x, int y, Bitmap bitmap, boolean transparent)
	{
		if (curMode == null || bitmap == null)
		{
			Kernel.panic("VESAGraphics.setBitmap: mode or bitmap is null");
			return;
		}
		buffer.Blit(x, y, bitmap, transparent);
		needsRedraw = true;
	}
	
	@Override
	public void Swap()
	{
		if (needsRedraw)
		{
			int from = MAGIC.addr(buffer.PixelData[0]);
			int to = curMode.LfbAddress;
			int len = buffer.PixelData.length;
			Memory.memcopy32(from, to, len);
		}
		needsRedraw = false;
	}
	
	@Override
	public void ClearScreen()
	{
		buffer.Clear();
		needsRedraw = true;
	}
	
	@Override
	public boolean Contains(int x, int y)
	{
		return x >= 0 && y >= 0 && x < curMode.XRes && y < curMode.YRes;
	}
	
	@Override
	public void Rectangle(int x, int y, int width, int height, int color)
	{
		buffer.Rectangle(x, y, width, height, color);
		needsRedraw = true;
	}
}
