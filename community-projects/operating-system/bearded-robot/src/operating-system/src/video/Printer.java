package video;

/**
 * Provides various methods for displaying data on the screen.
 */
public class Printer
{
	public static final int BLACK = 0;
	public static final int BLUE = 1;
	public static final int GREEN = 2;
	public static final int TURQUOISE = 3;
	public static final int RED = 4;
	public static final int PURPLE = 5;
	public static final int BROWN = 6;
	public static final int LIGHT_GRAY = 7;
	public static final int GRAY = 8;
	public static final int LIGHT_BLUE = 9;
	public static final int LIGHT_GREEN = 10;
	public static final int CYAN = 11;
	public static final int LIGHT_RED = 12;
	public static final int PINK = 13;
	public static final int YELLOW = 14;
	public static final int WHITE = 15;
	
	/**
	 * Number of character columns on the screen.
	 */
	public static final int SCREEN_WIDTH = 80;
	
	/**
	 * Number of character lines on the screen.
	 */
	public static final int SCREEN_HEIGHT = 25;
	
	/**
	 * Field for accessing the character representation of a value.
	 */
	private static final char[] digits = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
	
	/**
	 * The color in which the {@link Printer} instance outputs characters.
	 */
	private byte color = 0x0F; // Weiß auf Schwarz
	
	/**
	 * The relative memory position to which the next character is written.
	 * is written.
	 */
	private int cursorPosition = 0;
	
	/**
	 * Clears and colors the entire screen.
	 *
	 * @param color The color with which the screen is colored.
	 */
	public static void fillScreen(int color)
	{
		color = (color << 4) + (color & 0xf);
		for (VideoChar vidChar : VideoMemory.std.chars)
		{
			vidChar.ascii = (byte) ' ';
			vidChar.color = (byte) color;
		}
	}
	
	/**
	 * Outputs a character on the screen.
	 *
	 * @param value The character to be printed.
	 * @param x The column in which the character is printed.
	 * @param y The line in which the character is printed.
	 * @param fg The color of the character.
	 * @param bg The background color of the character.
	 */
	public static void directPrintChar(char value, int x, int y, int fg, int bg)
	{
		int index = (x + 80 * y) % 2000;
		VideoMemory.std.chars[index].ascii = (byte) value;
		VideoMemory.std.chars[index].color = (byte) (((bg & 0x0F) << 4) + (fg & 0x0F));
	}
	
	/**
	 * Outputs a character string on the screen.
	 *
	 * @param value The character string to be printed.
	 * @param x The column in which the first character is printed.
	 * @param y The line in which the first character is printed.
	 * @param fg The color of the characters.
	 * @param bg The background color of the characters.
	 * @return The number of characters that were printed.
	 */
	public static int directPrintString(String value, int x, int y, int fg, int bg)
	{
		for (int i = 0; i < value.length(); i++)
		{
			directPrintChar(value.charAt(i), x++, y, fg, bg);
		}
		return value.length();
	}
	
	/**
	 * Outputs an int value on the screen.
	 *
	 * @param value The int value that is printed.
	 * @param base The number base which is used to display the value.
	 * @param length
	 * @param length The minimum number of characters to be printed.
	 * Is filled with 0.
	 * @param x The column in which the first character is printed.
	 * @param y The row in which the first character is printed.
	 * @param fg The color of the characters.
	 * @param bg The background color of the characters.
	 * @return The number of characters that were printed.
	 */
	public static int directPrintInt(int value, int base, int length, int x, int y, int fg, int bg)
	{
		if (base < 2 || base > 16)
		{
			base = 10; // If the base is invalid, 10 is used.
		}
		
		byte color = (byte) (((bg & 0x0F) << 4) + (fg & 0x0F));
		int index = (x + 80 * y) % 2000;
		int intLength = 0;
		int charCount = 0;
		
		// If the value is negative, print a '-' and replace the value
		// with its amount.
		if (value < 0)
		{
			VideoMemory.std.chars[index].ascii = (byte) '-';
			VideoMemory.std.chars[index++].color = color;
			value = -value;
			charCount++;
		}
		
		// Count the characters of the value.
		int tmp = value;
		do
		{
			intLength++;
			tmp /= base;
		} while (tmp != 0);
		charCount += (length > intLength) ? length : intLength;
		
		// Fill with leading zeros.
		while (length-- > intLength)
		{
			VideoMemory.std.chars[index].ascii = (byte) '0';
			VideoMemory.std.chars[index++].color = color;
		}
		
		// Print the value
		while (intLength-- > 0)
		{
			VideoMemory.std.chars[index + intLength].ascii = (byte) digits[value % base];
			VideoMemory.std.chars[index + intLength].color = color;
			value /= base;
		}
		
		return charCount;
	}
	
	/**
	 * Sets foreground color and background color.
	 *
	 * @param fg The foreground color.
	 * @param bg The background color.
	 */
	public void setColor(int fg, int bg)
	{
		fg &= 0x0F;
		bg &= 0x0F;
		this.color = (byte) ((bg << 4) + fg);
	}
	
	/**
	 * Places the cursor in a specific row and column.
	 *
	 * @param x The column.
	 * @param y The row.
	 */
	public void setCursor(int x, int y)
	{
		this.cursorPosition = (x % SCREEN_WIDTH) + SCREEN_WIDTH * (y % SCREEN_HEIGHT);
	}
	
	public int getCursorX()
	{
		return this.cursorPosition % SCREEN_WIDTH;
	}
	
	public int getCursorY()
	{
		return this.cursorPosition / SCREEN_WIDTH;
	}
	
	/**
	 * Outputs a character on the screen.
	 *
	 * @param value The character that is printed.
	 */
	public void print(char value)
	{
		VideoMemory.std.chars[cursorPosition].ascii = (byte) value;
		VideoMemory.std.chars[cursorPosition].color = this.color;
		this.cursorPosition = (cursorPosition + 1) % (SCREEN_WIDTH * SCREEN_HEIGHT);
	}
	
	/**
	 * Outputs an int value on the screen.
	 *
	 * @param value The int value that is printed.
	 */
	public void print(int value)
	{
		int x = this.cursorPosition % SCREEN_WIDTH;
		int y = this.cursorPosition / SCREEN_WIDTH;
		this.cursorPosition += directPrintInt(value, 10, 0, x, y, this.color, this.color >> 4);
	}
	
	/**
	 * Outputs a long value on the screen.
	 *
	 * @param value The long value that is printed.
	 */
	public void print(long value)
	{
		int intLength = 0;
		
		// If the value is negative, print a '-' and replace the value
		// with its amount.
		if (value < 0)
		{
			VideoMemory.std.chars[this.cursorPosition].ascii = (byte) '-';
			VideoMemory.std.chars[this.cursorPosition++].color = color;
			value = -value;
		}
		
		// Count the characters of the value.
		long tmp = value;
		do
		{
			intLength++;
			tmp /= 10;
		} while (tmp != 0);
		
		// Print the value
		while (intLength-- > 0)
		{
			VideoMemory.std.chars[this.cursorPosition + intLength].ascii = (byte) digits[(int) (value % 10)];
			VideoMemory.std.chars[this.cursorPosition++ + intLength].color = color;
			value /= 10;
		}
	}
	
	/**
	 * Outputs a character string on the screen.
	 *
	 * @param value The character string to be printed.
	 */
	public void print(String value)
	{
		for (int i = 0; i < value.length(); i++)
		{
			print(value.charAt(i));
		}
	}
	
	/**
	 * Outputs the string representation of an object on the screen.
	 *
	 * @param object The object whose string representation is printed.
	 */
	public void print(Object object)
	{
		print(object.toString());
	}
	
	/**
	 * Outputs a byte value in hexadecimal notation with “0x” prefix.
	 *
	 * @param value The byte value to be printed.
	 */
	public void printHex(byte value)
	{
		print('0');
		print('x');
		for (int i = 4; i >= 0; i -= 4)
		{
			int d = ((value >> i) & 0xF);
			print((char) ((d > 9) ? d + 55 : d + 48));
		}
	}
	
	/**
	 * Outputs a short value in hexadecimal notation with “0x” prefix.
	 *
	 * @param value The short value to be printed.
	 */
	public void printHex(short value)
	{
		print('0');
		print('x');
		for (int i = 12; i >= 0; i -= 4)
		{
			int d = ((value >> i) & 0xF);
			print((char) ((d > 9) ? d + 55 : d + 48));
		}
	}
	
	/**
	 * Outputs an int value in hexadecimal notation with “0x” prefix.
	 *
	 * @param value The int value to be printed.
	 */
	public void printHex(int value)
	{
		print('0');
		print('x');
		for (int j = 28; j >= 0; j -= 4)
		{
			int d = ((value >> j) & 0xF);
			print((char) ((d > 9) ? d + 55 : d + 48));
		}
	}
	
	/**
	 * Outputs a long value in hexadecimal notation with “0x” prefix.
	 *
	 * @param value The long value to be printed.
	 */
	public void printHex(long value)
	{
		print('0');
		print('x');
		for (int i = 60; i >= 0; i -= 4)
		{
			int d = (int) ((value >> i) & 0xF);
			print((char) ((d > 9) ? d + 55 : d + 48));
		}
	}
	
	/**
	 * Sets the cursor to the beginning of the next line.
	 */
	public void println()
	{
		this.cursorPosition = (this.cursorPosition / SCREEN_WIDTH + 1) * SCREEN_WIDTH;
	}
	
	/**
	 * Outputs a character on the screen and moves the cursor to the next line.
	 * next line.
	 *
	 * @param c The character to be printed.
	 */
	public void println(char c)
	{
		print(c);
		println();
	}
	
	/**
	 * Outputs an int value on the screen and moves the cursor to the next line.
	 * next line.
	 *
	 * @param value The int value to be printed.
	 */
	public void println(int value)
	{
		print(value);
		println();
	}
	
	/**
	 * Outputs a long value on the screen and moves the cursor to the next line.
	 * next line.
	 *
	 * @param value The long value which is printed.
	 */
	public void println(long value)
	{
		print(value);
		println();
	}
	
	/**
	 * Outputs a character string on the screen and moves the cursor to the next line.
	 * next line.
	 *
	 * @param value The character string to be printed.
	 */
	public void println(String value)
	{
		print(value);
		println();
	}
	
	/**
	 * Outputs the string representation of an object on the screen
	 * and moves the cursor to the next line.
	 *
	 * @param object The object whose string representation is printed.
	 */
	public void println(Object object)
	{
		print(object.toString());
	}
	
}
