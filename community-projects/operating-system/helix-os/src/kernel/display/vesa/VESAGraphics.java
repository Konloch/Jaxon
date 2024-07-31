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
			Kernel.panic("VESAGraphics.setMode: mode is null");
		
		if (mode.colorDepth != 32)
			Kernel.panic("VESAGraphics.setMode: only 32 bit color depth is supported");
		
		buffer = new Bitmap(mode.xRes, mode.yRes, false);
		needsRedraw = true;
		curMode = mode;
		
		Logger.info("VESA", "SetMode to ".append(curMode.debug()));
	}
	
	@Override
	public void activate()
	{
		Logger.info("VESA", "Activate VESA Graphics Mode");
		DisplayModes.setVesaMode(curMode.modeNr);
	}
	
	@Override
	public int width()
	{
		return this.curMode.xRes;
	}
	
	@Override
	public int height()
	{
		return this.curMode.yRes;
	}
	
	@Override
	public int rgb(int r, int g, int b)
	{
		int red, green, blue;
		red = Math.clamp(r, 0, 255);
		green = Math.clamp(g, 0, 255);
		blue = Math.clamp(b, 0, 255);
		return (blue << 0) | (green << 8) | (red << 16) | (255 << 24);
	}
	
	@Override
	public int argb(int a, int r, int g, int b)
	{
		int red, green, blue, alpha;
		red = Math.clamp(r, 0, 255);
		green = Math.clamp(g, 0, 255);
		blue = Math.clamp(b, 0, 255);
		alpha = Math.clamp(a, 0, 255);
		return (blue << 0) | (green << 8) | (red << 16) | (alpha << 24);
	}
	
	@Override
	public void pixel(int x, int y, int col)
	{
		if (x < 0 || y < 0 || x >= curMode.xRes || y >= curMode.yRes)
			return;
		
		buffer.setPixel(x, y, col);
		needsRedraw = true;
	}
	
	@Override
	public void bitmap(int x, int y, Bitmap bitmap, boolean transparent)
	{
		if (curMode == null || bitmap == null)
			Kernel.panic("VESAGraphics.setBitmap: mode or bitmap is null");
		
		buffer.blit(x, y, bitmap, transparent);
		needsRedraw = true;
	}
	
	@Override
	public void swap()
	{
		if (needsRedraw)
		{
			int from = MAGIC.addr(buffer.pixelData[0]);
			int to = curMode.lfbAddress;
			int len = buffer.pixelData.length;
			Memory.Memcopy32(from, to, len);
		}
		
		needsRedraw = false;
	}
	
	@Override
	public void clearScreen()
	{
		buffer.clear();
		needsRedraw = true;
	}
	
	@Override
	public boolean contains(int x, int y)
	{
		return x >= 0 && y >= 0 && x < curMode.xRes && y < curMode.yRes;
	}
	
	@Override
	public void rectangle(int x, int y, int width, int height, int color)
	{
		buffer.rectangle(x, y, width, height, color);
		needsRedraw = true;
	}
}
