package keyboard;

/**
 * Basic type for listeners that can react to keyboard events.
 */
public abstract class KeyboardListener
{
	
	KeyboardListener next = null;
	
	/**
	 * Is called up when a button is pressed.
	 *
	 * @param value The key value depending on the layout.
	 * @param keyCode The key code.
	 * @param isChar true if the value parameter corresponds to an ASCII value,
	 * otherwise false.
	 * @param flags Flags that indicate the state of the modification keys.
	 */
	public abstract void onKeyDown(int value, int keyCode, boolean isChar, int flags);
	
	/**
	 * Is called up when a button is released.
	 *
	 * @param value The key value depending on the layout.
	 * @param keyCode The key code.
	 * @param isChar true if the value parameter corresponds to an ASCII value,
	 * otherwise false.
	 * @param flags Flags that indicate the state of the modification keys.
	 */
	public abstract void onKeyUp(int value, int keyCode, boolean isChar, int flags);
}
