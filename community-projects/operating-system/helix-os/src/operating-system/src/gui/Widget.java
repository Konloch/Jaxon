package gui;

import kernel.display.Bitmap;

public abstract class Widget
{
	public int width;
	public int height;
	public int x;
	public int y;
	public boolean isSelected;
	public String name;
	public Bitmap renderTarget;
	private boolean _needsRedraw;
	
	public Widget(String name, int x, int y, int width, int height)
	{
		this.x = x;
		this.y = y;
		this.width = width;
		this.height = height;
		this.name = name;
		_needsRedraw = true;
		renderTarget = new Bitmap(width, height, false);
	}
	
	public abstract void draw();
	
	public void setDirty()
	{
		_needsRedraw = true;
	}
	
	public void clearDirty()
	{
		_needsRedraw = false;
	}
	
	public boolean needsRedraw()
	{
		return _needsRedraw;
	}
	
	public boolean contains(int x, int y)
	{
		return x >= this.x && x <= this.x + width && y >= this.y && y <= this.y + height;
	}
}
