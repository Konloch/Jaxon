package kernel.hardware.keyboard;

import java.util.IDebug;
import java.lang.StringBuilder;

public class KeyEvent implements IDebug
{
	public char key;
	public boolean isDown;
	
	public KeyEvent(char key, boolean isDown)
	{
		this.key = key;
		this.isDown = isDown;
	}
	
	public KeyEvent()
	{
		key = 0;
		isDown = false;
	}
	
	@Override
	public String debug()
	{
		return new StringBuilder().append("KeyEvent {").append("Key: ")
				.append(kernel.hardware.keyboard.Key.name(key)).append(", IsDown: ").append(isDown).append("}").toString();
	}
	
}
