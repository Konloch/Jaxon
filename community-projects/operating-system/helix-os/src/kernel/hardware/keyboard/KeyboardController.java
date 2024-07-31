package kernel.hardware.keyboard;

import kernel.hardware.keyboard.layout.ALayout;
import kernel.interrupt.IDT;
import kernel.interrupt.PIC;
import kernel.schedule.Scheduler;
import kernel.trace.logging.Logger;
import java.util.BitHelper;
import java.util.queue.QueueKeyEvent;

public class KeyboardController
{
	public static final int IRQ_KEYBOARD = 1;
	private static final int PORT_KEYCODE = 0x60;
	private static final int KEYCODE_EXTEND1 = 0xE0;
	private static final int KEYCODE_EXTEND2 = 0xE1;
	
	private static final int MASK_10000000 = 1 << 7;
	private static final int MASK_01111111 = ~MASK_10000000;
	private static final int MASK_1000000010000000 = (1 << 7) | (1 << 15);
	private static final int MASK_0111111101111111 = ~MASK_1000000010000000;
	
	private static QueueKeyEvent _eventBuffer;
	private static ALayout _layout;
	
	private static boolean _shiftPressed;
	private static boolean _ctrlPressed;
	private static boolean _altPressed;
	private static boolean _capsLocked;
	
	public static void Initialize()
	{
		_layout = null;
		packet = new int[3];
		_eventBuffer = new QueueKeyEvent(32);
		for (int i = 0; i < _eventBuffer.capacity(); i++)
		{
			_eventBuffer.put(new KeyEvent());
		}
		
		int dscAddr = MAGIC.cast2Ref(MAGIC.clssDesc("KeyboardController"));
		int handlerOffset = IDT.CodeOffset(dscAddr, MAGIC.mthdOff("KeyboardController", "KeyboardHandler"));
		IDT.RegisterIrqHandler(IRQ_KEYBOARD, handlerOffset);
	}
	
	@SJC.Interrupt
	public static void KeyboardHandler()
	{
		KeyboardController.Handle();
		PIC.Acknowledge(IRQ_KEYBOARD);
	}
	
	public static void SetLayout(ALayout layout)
	{
		_layout = layout;
	}
	
	@SJC.Inline
	public static boolean HasNewEvent()
	{
		return _eventBuffer.containsNewElements();
	}
	
	static int expectedLength = -1;
	private static int[] packet;
	private static int cycle = 0;
	
	public static void Handle()
	{
		byte code = MAGIC.rIOs8(PORT_KEYCODE);
		if (code >= 0xE2)
		{
			Logger.warning("KeyC", "Ignoring ScanCode >0xE2");
			return;
		}
		
		if (cycle == 0)
		{
			if (code == KEYCODE_EXTEND1)
			{
				expectedLength = 2;
			}
			else if (code == KEYCODE_EXTEND2)
			{
				expectedLength = 3;
			}
			else
			{
				expectedLength = 1;
			}
		}
		
		packet[cycle++] = code;
		
		if (cycle == expectedLength)
		{
			cycle = 0;
			expectedLength = 0;
			KeyEvent event = _eventBuffer.peek();
			ReadPacket(event);
			_eventBuffer.incHead();
			
			packet[0] = 0;
			packet[1] = 0;
			packet[2] = 0;
			
			if (event.Key == Key.F10 && event.IsDown)
			{
				PIC.Acknowledge(IRQ_KEYBOARD);
				Scheduler.taskBreak();
			}
		}
	}
	
	public static KeyEvent ReadEvent()
	{
		return _eventBuffer.get();
	}
	
	private static void ReadPacket(KeyEvent readInto)
	{
		int keyCode = ReadKeyCode();
		boolean isBreak = IsBreakCode(keyCode);
		if (isBreak)
		{
			keyCode = UnsetBreakCode(keyCode);
		}
		
		char logicalKey = _layout.LogicalKey(keyCode, IsUpper(), _altPressed);
		UpdateKeyboardState(logicalKey, isBreak);
		readInto.Key = logicalKey;
		readInto.IsDown = !isBreak;
	}
	
	private static void UpdateKeyboardState(char logicalKey, boolean isBreak)
	{
		switch (logicalKey)
		{
			case Key.LSHIFT:
			case Key.RSHIFT:
				if (isBreak)
				{
					_shiftPressed = false;
				}
				else
				{
					_shiftPressed = true;
				}
				break;
			case Key.LCTRL:
			case Key.RCTRL:
				if (isBreak)
				{
					_ctrlPressed = false;
				}
				else
				{
					_ctrlPressed = true;
				}
				break;
			case Key.LALT:
			case Key.RALT:
				if (isBreak)
				{
					_altPressed = false;
				}
				else
				{
					_altPressed = true;
				}
				break;
			case Key.CAPSLOCK:
				if (isBreak)
				{
					_capsLocked = false;
				}
				else
				{
					_capsLocked = true;
				}
				break;
		}
	}
	
	private static int ReadKeyCode()
	{
		int c0 = Integer.ubyte(packet[0]);
		int keyCode = 0;
		if (c0 == KEYCODE_EXTEND1)
		{
			int c1 = Integer.ubyte(packet[1]);
			
			// 0xE0_2A
			keyCode = BitHelper.setRange(keyCode, 8, 8, c0);
			keyCode = BitHelper.setRange(keyCode, 0, 8, c1);
		}
		else if (c0 == KEYCODE_EXTEND2)
		{
			int c1 = Integer.ubyte(packet[1]);
			int c2 = Integer.ubyte(packet[2]);
			
			// 0xE1_2A_2A
			keyCode = BitHelper.setRange(keyCode, 16, 8, c0);
			keyCode = BitHelper.setRange(keyCode, 8, 8, c1);
			keyCode = BitHelper.setRange(keyCode, 0, 8, c2);
		}
		else
		{
			// 0x2A
			keyCode = Integer.ubyte(c0);
		}
		return keyCode;
	}
	
	/*
	 * Unset the break code bits.
	 * For normal codes, the 8th bit is set.
	 * For E0 codes, the 8th bit is set.
	 * For E1 codes, the 8th and 16th bits are set.
	 */
	@SJC.Inline
	private static int UnsetBreakCode(int keyCode)
	{
		if (keyCode > 0xE10000)
		{
			return keyCode & MASK_0111111101111111;
		}
		else
		{
			return keyCode & MASK_01111111;
		}
	}
	
	@SJC.Inline
	private static boolean IsBreakCode(int keyCode)
	{
		return (keyCode & MASK_10000000) != 0;
	}
	
	@SJC.Inline
	private static boolean IsUpper()
	{
		if (_shiftPressed)
		{
			return true;
		}
		if (_capsLocked)
		{
			return true;
		}
		return false;
	}
}
