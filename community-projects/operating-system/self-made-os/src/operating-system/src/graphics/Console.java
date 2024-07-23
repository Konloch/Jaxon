package graphics;

import utils.TypeConv;

//Basic console output
public abstract class Console
{
	//scrollable console
	private static char[] HEX_ARRAY;
	private static int color = ConsoleColors.DEFAULT_CONSOLE_COLOR;
	private static boolean cursor = true;
	
	static
	{
		HEX_ARRAY = "0123456789ABCDEF".toCharArray();
	}
	
	public static void clearConsole()
	{
		VideoController.clearVideoMemory();
		if (cursor)
			VideoController.updateCursor();
	}
	
	public static void disableCursor()
	{
		VideoController.disableCursor();
	}
	
	public static void enableCursor()
	{
		VideoController.enableCursor();
	}
	
	//dynamische Methoden
	
	public static void setColor(int fg, int bg, boolean blinking)
	{
		//enforce sane defaults if args are out of bounds
		if (fg < ConsoleColors.FG_BLACK || fg > ConsoleColors.FG_WHITE)
		{
			fg = ConsoleColors.FG_WHITE;
		}
		if (bg < ConsoleColors.BG_BLACK || bg > ConsoleColors.BG_LIGHTGREY)
		{
			bg = ConsoleColors.BG_BLACK;
		}
		color = fg | bg;
		if (blinking)
			color |= ConsoleColors.BLINKING;
	}
	
	public static void setDefaultColor()
	{
		setColor(ConsoleColors.FG_WHITE, ConsoleColors.BG_BLACK, false);
	}
	
	public static void setCursor(int newX, int newY)
	{
		//TODO: what shall do with out of bounds args
		VideoController.setPos(newX, newY);
		if (cursor)
			VideoController.updateCursor();
	}
	
	public static VideoCharCopy[] getCurrentVideoMemory()
	{
		return VideoController.getCurrentVideoMem();
	}
	
	public static void print(char c)
	{
		VideoController.handleChar(c, color);
		if (cursor)
			VideoController.updateCursor();
	}
	
	public static void print(int x)
	{
		print((long) x);
	}
	
	public static void print(long x)
	{
		print(TypeConv.longToString(x));
		if (cursor)
			VideoController.updateCursor();
	}
	
	public static void print(String str)
	{
		if (str == null)
			return;
		for (int i = 0; i < str.length(); i++)
		{
			print(str.charAt(i));
		}
		if (cursor)
			VideoController.updateCursor();
	}
	
	//https://stackoverflow.com/questions/9655181/how-to-convert-a-byte-array-to-a-hex-string-in-java
	//1 byte
	public static void printHex(byte b)
	{
		char[] hexChars = new char[2];
		int v = b & 0xFF;
		hexChars[0] = HEX_ARRAY[v >>> 4];
		hexChars[1] = HEX_ARRAY[v & 0x0F];
		print("0x".concat(new String(hexChars)));
		if (cursor)
			VideoController.updateCursor();
	}
	
	//2 bytes
	public static void printHex(short s)
	{
		printHex(TypeConv.toBytes(s));
	}
	
	//4 bytes
	public static void printHex(int x)
	{
		printHex(TypeConv.toBytes(x));
	}
	
	public static void printHex(long x)
	{
		printHex(TypeConv.toBytes(x));
	}
	
	public static void printHex(byte[] b)
	{
		char[] hexChars = new char[b.length * 2];
		for (int i = 0; i < b.length; i++)
		{
			int v = b[i] & 0xFF;
			hexChars[i * 2] = HEX_ARRAY[v >>> 4];
			hexChars[i * 2 + 1] = HEX_ARRAY[v & 0x0F];
		}
		print("0x".concat(new String(hexChars)));
		if (cursor)
			VideoController.updateCursor();
	}
	
	public static void println()
	{
		print("\n");
		if (cursor)
			VideoController.updateCursor();
	}
	
	//vorgegebene Methoden
	public static void println(char c)
	{
		print(c);
		println();
		if (cursor)
			VideoController.updateCursor();
	}
	
	public static void println(int i)
	{
		print(i);
		println();
		if (cursor)
			VideoController.updateCursor();
	}
	
	public static void println(long l)
	{
		print(l);
		println();
		if (cursor)
			VideoController.updateCursor();
	}
	
	public static void println(String str)
	{
		print(str);
		println();
		if (cursor)
			VideoController.updateCursor();
	}
	
	//static debug
	public static void directPrintInt(int value, int base, int len, int x, int y, int color)
	{
		return;
	}
	
	public static void directPrintChar(char c, int x, int y, int cl)
	{
		VideoController.writeCharDirectly(c, x, y, cl);
	}
	
	public static void debug(String msg)
	{
		for (int i = 0; i < msg.length(); i++)
		{
			VideoController.writeCharDebug(msg.charAt(i), ConsoleColors.DEFAULT_CONSOLE_COLOR);
		}
	}
	
	public static void debugHex(byte b)
	{
		char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();
		char[] hexChars = new char[2];
		int v = b & 0xFF;
		hexChars[0] = HEX_ARRAY[v >>> 4];
		hexChars[1] = HEX_ARRAY[v & 0x0F];
		debug("0x".concat(new String(hexChars)));
		VideoController.updateCursor();
	}
	
	//USE WITH GREAT CARE
	public static void resetConsole()
	{
		VideoController.clearVideoMemory();
	}
	
	public static void debugPrint(int value)
	{
		printRecursiveInt(value);
	}
	
	public static void debugPrintln(int value)
	{
		debugPrint(value);
		println();
	}
	
	//Handling Decimals
	private static void printRecursiveInt(long value)
	{
		//Höchste Stelle als erstes ausgeben
		int charOffset = 48;
		char currChar = (char) (value % 10 + charOffset);
		value /= 10;
		if (value > 0)
			printRecursiveInt(value);
		print(currChar);
	}
	
	public static void writeVideoMemory(VideoCharCopy[] myVidMem)
	{
		//save pos, then restore it
		VideoController.setPos(0, 0);
		for (int i = 0; i < VideoMemory.VIDEO_MEMORY_LENGTH; i++)
		{
			VideoController.handleChar(myVidMem[i].ascii, myVidMem[i].color);
		}
	}
	
	public static int getXPos()
	{
		return VideoController.getXPos();
	}
	
	public static int getYPos()
	{
		return VideoController.getYPos();
	}
	
	public static void setPos(int X, int Y)
	{
		VideoController.setPos(X, Y);
	}
}
