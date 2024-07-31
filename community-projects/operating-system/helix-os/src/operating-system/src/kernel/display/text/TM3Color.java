package kernel.display.text;

import java.util.BitHelper;

/// Bit 76543210
///     ||||||||
///     |||||^^^-fore colour
///     ||||^----fore colour bright bit
///     |^^^-----back colour
//      ^--------back colour bright bit OR enables blinking Text.
///
/// Blinking can be set via https://www.reddit.com/r/osdev/comments/70fcig/blinking_text/?rdt=51833
public class TM3Color
{
	public static final byte BLACK = 0;
	public static final byte BLUE = 1;
	public static final byte GREEN = 2;
	public static final byte TURQUOISE = 3;
	public static final byte RED = 4;
	public static final byte VIOLET = 5;
	public static final byte BROWN = 6;
	public static final byte GREY = 7;
	public static final byte DARK_GREY = 8;
	public static final byte LIGHT_BLUE = 9;
	public static final byte LIGHT_GREEN = 10;
	public static final byte LIGHT_TURQUOISE = 11;
	public static final byte LIGHT_RED = 12;
	public static final byte LIGHT_VIOLET = 13;
	public static final byte LIGHT_BROWN = 14;
	public static final byte WHITE = 15;
	
	@SJC.Inline
	public static byte Set(byte fg, byte bg)
	{
		byte color = 0;
		color = SetFg(color, fg);
		color = SetBg(color, bg);
		return color;
	}
	
	@SJC.Inline
	public static byte Set(byte fg, byte bg, boolean fgIsBright, boolean bgIsBright)
	{
		byte color = 0;
		color = SetFg(color, fg);
		color = SetBg(color, bg);
		color = SetFgBright(color, fgIsBright);
		color = SetBgBright(color, bgIsBright);
		return color;
	}
	
	@SJC.Inline
	public static byte SetFg(byte color, byte fg)
	{
		return (byte) BitHelper.setRange(color, 0, 4, fg);
	}
	
	@SJC.Inline
	public static byte SetBg(byte color, byte bg)
	{
		return (byte) BitHelper.setRange(color, 4, 4, bg);
	}
	
	@SJC.Inline
	public static byte SetFgBright(byte color, boolean isBright)
	{
		return (byte) BitHelper.setFlag(color, 3, isBright);
	}
	
	@SJC.Inline
	public static byte SetBgBright(byte color, boolean isBright)
	{
		return (byte) BitHelper.setFlag(color, 7, isBright);
	}
}
