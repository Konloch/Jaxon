package gui.components.button;

import java.util.IDebug;

public class ButtonClickedEventArgs implements IDebug
{
	public String buttonName;
	
	public ButtonClickedEventArgs(String buttonName)
	{
		this.buttonName = buttonName;
	}
	
	@Override
	public String debug()
	{
		return "ButtonClickedEventArgs: ".append(buttonName);
	}
}
