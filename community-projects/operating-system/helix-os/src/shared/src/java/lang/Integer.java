package java.lang;

import java.util.NoAllocConv;

public class Integer
{
	public static final int MAX = 2147483647;
	public static final int MIN = -2147483648;
	
	/*
	 * I hate you Java.
	 */
	@SJC.Inline
	public static int ubyte(int i)
	{
		return i & 0xFF;
	}
	
	@SJC.Inline
	public static int ushort(int i)
	{
		return i & 0xFFFF;
	}
	
	public static byte[] BUFFER = MAGIC.toByteArray("0000000000000000000000000000", true);
	
	public static String toString(int i, int base)
	{
		if (i == 0)
			return "0";
		
		for (int j = 0; j < BUFFER.length; j++)
			BUFFER[j] = (byte) 0;
		
		int digitCount = NoAllocConv.iToA(BUFFER, BUFFER.length, i, base);
		
		int newLength = digitCount;
		if (i < 0)
		{
			newLength += 1;
		}
		
		int offest = BUFFER.length - digitCount;
		byte[] chars = new byte[newLength];
		if (i < 0)
		{
			chars[0] = (byte) '-';
			for (int j = 0; j < digitCount; j++)
				chars[j + 1] = BUFFER[j + offest];
		}
		else
		{
			for (int j = 0; j < digitCount; j++)
				chars[j] = BUFFER[j + offest];
		}
		
		return new String(chars);
	}
	
	@SJC.Inline
	public static String toString(int i)
	{
		return toString(i, 10);
	}
}