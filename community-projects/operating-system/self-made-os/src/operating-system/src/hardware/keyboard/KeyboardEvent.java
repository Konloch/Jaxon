package hardware.keyboard;

public class KeyboardEvent
{
	//MODIFIER
	public final boolean ALT, SHIFT, CONTROL;
	//TOGGLES
	public final boolean CAPSLOCK, SCROLLLOCK, NUMLOCK;
	public final int KEYCODE;
	
	KeyboardEvent(boolean alt, boolean shift, boolean control, boolean capslock, boolean scrolllock, boolean numlock, int keycode)
	{
		ALT = alt;
		SHIFT = shift;
		CONTROL = control;
		CAPSLOCK = capslock;
		SCROLLLOCK = scrolllock;
		NUMLOCK = numlock;
		KEYCODE = keycode;
	}
}
