package keyboard;

import container.IntegerRingBuffer;
import interrupts.Interrupts;
import scheduling.Task;

/**
 * Keyboard management class
 */
public class Keyboard extends Task
{
	// Constants for key values that cannot be represented as char.
	public static final int UNSPECIFIED = 0;
	public static final int F1 = 1;
	public static final int F2 = 2;
	public static final int F3 = 3;
	public static final int F4 = 4;
	public static final int F5 = 5;
	public static final int F6 = 6;
	public static final int F7 = 7;
	public static final int F8 = 8;
	public static final int F9 = 9;
	public static final int F10 = 10;
	public static final int F11 = 11;
	public static final int F12 = 12;
	public static final int SHIFT_LEFT = 13;
	public static final int SHIFT_RIGHT = 14;
	public static final int CTRL = 15;
	public static final int ALT = 16;
	public static final int CAPS_LOCK = 17;
	public static final int RETURN = 18;
	public static final int NUM_LOCK = 19;
	public static final int BACKSPACE = 20;
	public static final int UP = 21;
	public static final int DOWN = 22;
	public static final int LEFT = 23;
	public static final int RIGHT = 24;
	
	// Flags values of modification buttons
	public static final int FLAG_SHIFT = 1;
	public static final int FLAG_CTRL = 2;
	public static final int FLAG_CAPS_LOCK = 4;
	public static final int FLAG_ALT = 8;
	public static final int FLAG_NUM_LOCK = 16;
	
	/**
	 * Buffer for received keyboard scan codes.
	 */
	IntegerRingBuffer buffer = new IntegerRingBuffer(32);
	
	/**
	 * The interrupt handler for IRQ1.
	 */
	private final KeyboardInterruptHandler interruptHandler = new KeyboardInterruptHandler();
	
	/**
	 * The layout of the keyboard.
	 */
	private final Layout layout = new Layout();
	
	/**
	 * The listeners that are notified of incoming keyboard events.
	 */
	private KeyboardListener listenerRoot = null;
	
	/**
	 * Flags that indicate the state of the modification keys.
	 */
	private int toggleFlags = 0;
	
	private static Keyboard instance = null;
	
	/**
	 * Initializes the keyboard functionality.
	 */
	public static Keyboard initstance()
	{
		if (instance == null)
			instance = new Keyboard();
		
		return instance;
	}
	
	private Keyboard()
	{
		Interrupts.HANDLERS[33] = interruptHandler;
	}
	
	public void addListener(KeyboardListener listener)
	{
		listener.next = this.listenerRoot;
		this.listenerRoot = listener;
	}
	
	public void removeListener(KeyboardListener listener)
	{
		if (this.listenerRoot == null)
			return;
		
		if (this.listenerRoot == listener)
		{
			this.listenerRoot = this.listenerRoot.next;
			return;
		}
		
		KeyboardListener prev = this.listenerRoot;
		KeyboardListener now = this.listenerRoot.next;
		while (true)
		{
			if (now == listener)
			{
				prev.next = now.next;
				return;
			}
			prev = now;
			now = now.next;
		}
	}
	
	/**
	 * Processes the scan codes currently in the buffer.
	 */
	public void onSchedule()
	{
		while (this.buffer.size() > 0)
		{
			int scanCode = this.buffer.front();
			this.buffer.pop();
			
			int keyCode = scanCode & 0xFFFFFF7F;
			boolean isDown = (scanCode & 0x80) == 0;
			
			int value = layout.value(keyCode);
			boolean isChar = layout.isCharacter(keyCode);
			
			if (!isChar)
			{
				switch (value)
				{
					case SHIFT_LEFT:
					case SHIFT_RIGHT:
						toggleFlags = (isDown) ? (toggleFlags | FLAG_SHIFT) : (toggleFlags & ~FLAG_SHIFT);
						break;
					case CTRL:
						toggleFlags = (isDown) ? (toggleFlags | FLAG_CTRL) : (toggleFlags & ~FLAG_CTRL);
						break;
					case CAPS_LOCK:
						if (isDown)
							toggleFlags = ((toggleFlags & FLAG_CAPS_LOCK) == FLAG_CAPS_LOCK) ? (toggleFlags & ~FLAG_CAPS_LOCK) : (toggleFlags | FLAG_CAPS_LOCK);
						break;
					case ALT:
						toggleFlags = (isDown) ? (toggleFlags | FLAG_ALT) : (toggleFlags & ~FLAG_ALT);
						break;
					case NUM_LOCK:
						if (isDown)
							toggleFlags = ((toggleFlags & FLAG_NUM_LOCK) == FLAG_NUM_LOCK) ? (toggleFlags & ~FLAG_NUM_LOCK) : (toggleFlags | FLAG_NUM_LOCK);
						break;
				}
			}
			
			KeyboardListener listener = this.listenerRoot;
			while (listener != null)
			{
				if (isDown)
					listener.onKeyDown(value, keyCode, isChar, toggleFlags);
				else
					listener.onKeyUp(value, keyCode, isChar, toggleFlags);
				
				listener = listener.next;
			}
		}
	}
	
	/**
	 * Determines whether the modification button(s) for the second level of the
	 * key values are active.
	 *
	 * @return true if the second level of the keys is to be used, otherwise
	 * false.
	 */
	public boolean isMod1()
	{
		boolean caps = (toggleFlags & FLAG_CAPS_LOCK) == FLAG_CAPS_LOCK;
		return caps == ((toggleFlags & FLAG_SHIFT) != FLAG_SHIFT);
	}
	
	/**
	 * Determines whether the Num-Lock is active.
	 *
	 * @return true if the Num-Lock is active, otherwise false.
	 */
	public boolean isNumLk()
	{
		return ((toggleFlags & FLAG_NUM_LOCK) == FLAG_NUM_LOCK);
	}
}
