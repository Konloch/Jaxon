package kernel.hardware.keyboard;

import java.util.IDebug;
import java.lang.StringBuilder;

public class KeyEvent implements IDebug
{
	public char Key;
	public boolean IsDown;
	
	public KeyEvent(char key, boolean isDown)
	{
		Key = key;
		IsDown = isDown;
	}
	
	public KeyEvent()
	{
		Key = 0;
		IsDown = false;
	}
	
	@Override
	public String debug()
	{
		return new StringBuilder().append("KeyEvent {").append("Key: ").append(kernel.hardware.keyboard.Key.Name(Key)).append(", IsDown: ").append(IsDown).append("}").toString();
	}
	
}
