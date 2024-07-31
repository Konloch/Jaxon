package kernel.display.text;

public class TM3Brush
{
	private byte _color;
	
	public TM3Brush()
	{
		setFg(TM3Color.GREY);
		setBg(TM3Color.BLACK);
	}
	
	public TM3Brush(byte fg, byte bg)
	{
		setFg(fg);
		setBg(bg);
	}
	
	@SJC.Inline
	public void set(byte fg, byte bg)
	{
		setFg(fg);
		setBg(bg);
	}
	
	@SJC.Inline
	public void setFg(byte fg)
	{
		this._color = TM3Color.setFg(this._color, fg);
	}
	
	@SJC.Inline
	public void setBg(byte bg)
	{
		this._color = TM3Color.setBg(this._color, bg);
	}
	
	@SJC.Inline
	public void setFgBright(boolean isBright)
	{
		this._color = TM3Color.setFgBright(this._color, isBright);
	}
	
	@SJC.Inline
	public void setBgBright(boolean isBright)
	{
		this._color = TM3Color.setBgBright(this._color, isBright);
	}
	
	@SJC.Inline
	public byte color()
	{
		return _color;
	}
	
	@SJC.Inline
	public void setColor(byte color)
	{
		this._color = color;
	}
}