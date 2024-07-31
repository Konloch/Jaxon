package kernel.display.text;

public class TM3Brush
{
	private byte _color;
	
	public TM3Brush()
	{
		SetFg(TM3Color.GREY);
		SetBg(TM3Color.BLACK);
	}
	
	public TM3Brush(byte fg, byte bg)
	{
		SetFg(fg);
		SetBg(bg);
	}
	
	@SJC.Inline
	public void Set(byte fg, byte bg)
	{
		SetFg(fg);
		SetBg(bg);
	}
	
	@SJC.Inline
	public void SetFg(byte fg)
	{
		this._color = TM3Color.SetFg(this._color, fg);
	}
	
	@SJC.Inline
	public void SetBg(byte bg)
	{
		this._color = TM3Color.SetBg(this._color, bg);
	}
	
	@SJC.Inline
	public void SetFgBright(boolean isBright)
	{
		this._color = TM3Color.SetFgBright(this._color, isBright);
	}
	
	@SJC.Inline
	public void SetBgBright(boolean isBright)
	{
		this._color = TM3Color.SetBgBright(this._color, isBright);
	}
	
	@SJC.Inline
	public byte Color()
	{
		return _color;
	}
	
	@SJC.Inline
	public void SetColor(byte color)
	{
		this._color = color;
	}
}