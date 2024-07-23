package hardware.keyboard;


public class Keyboard
{
	private static KeyboardLayout layout = new QWERTYLayout();
	//input and output buffer
	private static ByteRingBuffer inputBuffer;
	private static KeyboardEventRingBuffer outputBuffer;
	//modifier pressed?
	private static boolean SHIFT, CTRL, ALT;
	//any other key pressed since make?
	private static boolean SHIFT_STANDALONE, CTRL_STANDALONE, ALT_STANDALONE;
	//toggles
	private static boolean CAPSLOCK, SCROLLLOCK, NUMLOCK;
	//toggle locks until break
	private static boolean CAPSLOCK_LOCK, SCROLLLOCK_LOCK, NUMLOCK_LOCK;
	
	static
	{
		inputBuffer = new ByteRingBuffer();
		outputBuffer = new KeyboardEventRingBuffer();
	}
	
	public static void changeKeyboardLayout(KeyboardLayout newLayout)
	{
		layout = newLayout;
	}
	
	//interface to interrupt
	public static void storeKeycode()
	{
		byte b = MAGIC.rIOs8(0x60);
		if ((b & 0xFF) >= (0xE2))
		{
			return;
		}
		inputBuffer.writeByte(b);
	}
	
	//interface to loop
	//TODO: TBD P4b
	
	//processes the input buffer and tokenizes it into the output buffer for getNextKeyboardEvent()
	public static void processInputBuffer()
	{
		while (inputBuffer.canRead())
		{
			int bitmask = 0;
			byte b = inputBuffer.readByte();
			bitmask = (bitmask | (b & 0xFF));
			if ((b & 0xFF) == 0xE0)
			{ //sequence, read one more byte
				byte b1 = inputBuffer.readByte();
				bitmask = (bitmask << 8) | (b1 & 0xFF);
			}
			else if ((b & 0xFF) == 0xE1)
			{ //sequence, read two more bytes
				byte b1 = inputBuffer.readByte();
				byte b2 = inputBuffer.readByte();
				bitmask = (((bitmask << 8) | (b1 & 0xFF)) << 8) | (b2 & 0xFF);
			}
			int key;
			//remember if break key for later
			boolean breakKey = (bitmask & 0xFF) >= 0x80;
			//get key
			key = layout.translatePhysToLogicalKey(bitmask & 0xFFFFFF7F, SHIFT, CTRL, ALT);
			switch (key)
			{
				//region modifiers
				case Key.LEFT_SHIFT:
				case Key.RIGHT_SHIFT:
					if (breakKey)
					{
						SHIFT = false;
						if (SHIFT_STANDALONE)
						{
							outputBuffer.writeEvent(new KeyboardEvent(ALT, false, CTRL, CAPSLOCK, SCROLLLOCK, NUMLOCK, key));
							SHIFT_STANDALONE = false;
						}
					}
					else
					{
						SHIFT = true;
						SHIFT_STANDALONE = true;
					}
					break;
				case Key.LEFT_CONTROL:
				case Key.RIGHT_CONTROL:
					if (breakKey)
					{
						CTRL = false;
						if (CTRL_STANDALONE)
						{
							outputBuffer.writeEvent(new KeyboardEvent(ALT, SHIFT, false, CAPSLOCK, SCROLLLOCK, NUMLOCK, key));
							CTRL_STANDALONE = false;
						}
					}
					else
					{
						CTRL = true;
						CTRL_STANDALONE = true;
					}
					break;
				case Key.LEFT_ALT:
				case Key.RIGHT_ALT:
					if (breakKey)
					{
						ALT = false;
						if (ALT_STANDALONE)
						{
							outputBuffer.writeEvent(new KeyboardEvent(false, SHIFT, CTRL, CAPSLOCK, SCROLLLOCK, NUMLOCK, key));
							ALT_STANDALONE = false;
						}
					}
					else
					{
						ALT = true;
						ALT_STANDALONE = true;
					}
					break;
				//endregion
				
				//region toggles
				case Key.CAPSLOCK:
					if (!CAPSLOCK_LOCK)
					{
						CAPSLOCK = !CAPSLOCK;
						CAPSLOCK_LOCK = true;
					}
					if (breakKey)
					{
						CAPSLOCK_LOCK = false;
					}
					break;
				case Key.SCROLLLOCK:
					if (!SCROLLLOCK_LOCK)
					{
						SCROLLLOCK = !SCROLLLOCK;
						SCROLLLOCK_LOCK = true;
					}
					if (breakKey)
					{
						CAPSLOCK_LOCK = false;
					}
					break;
				case Key.NUMLOCK:
					if (!NUMLOCK_LOCK)
					{
						NUMLOCK = !NUMLOCK;
						NUMLOCK_LOCK = true;
					}
					if (breakKey)
					{
						NUMLOCK_LOCK = false;
					}
					break;
				//endregion
				
				//everything else
				default:
					if (!breakKey)
					{
						outputBuffer.writeEvent(new KeyboardEvent(ALT, SHIFT, CTRL, CAPSLOCK, SCROLLLOCK, NUMLOCK, key));
						//invalidate flags for standalone keys, because other make key has been sent
						SHIFT_STANDALONE = ALT_STANDALONE = CTRL_STANDALONE = false;
					}
			}
		}
	}
	
	//returns the next KeyboardEvent
	public static KeyboardEvent getNextKeyboardEvent()
	{
		return outputBuffer.readEvent();
	}
	
	//returns whether or not there is a new KeyboardEvent available
	public static boolean eventAvailable()
	{
		return outputBuffer.canRead();
	}
	
	//completely clears all keyboard related buffers
	public static void clearAllBuffers()
	{
		inputBuffer.clearBuffer();
		outputBuffer.clearBuffer();
	}
	
	
}
