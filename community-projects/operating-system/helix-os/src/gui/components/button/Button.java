package gui.components.button;

import formats.fonts.AFont;
import gui.Widget;

public class Button extends Widget
{
	public AFont font;
	
	protected int _bg;
	protected int _fg;
	protected String _text;
	protected String _name;
	
	public IButtonListener EventListener;
	
	public Button(String name, int x, int y, int width, int height, int fg, int bg, String text, AFont font, IButtonListener listener)
	{
		super(name, x, y, width, height);
		_fg = fg;
		_bg = bg;
		this.font = font;
		EventListener = listener;
		_text = text;
		_name = name;
		
	}
	
	@Override
	public void draw()
	{
		renderTarget.Rectangle(0, 0, width, height, _bg);
	}
	
	public void click()
	{
		if (EventListener != null)
			EventListener.onButtonClicked(new ButtonClickedEventArgs(_name));
	}
}
