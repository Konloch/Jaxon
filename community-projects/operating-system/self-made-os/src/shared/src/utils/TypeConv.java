package utils;


public class TypeConv
{
	public static byte[] toBytes(short s)
	{
		byte[] result = new byte[2];
		for (int i = 1; i >= 0; i--)
		{
			result[i] = (byte) (s & 0xFF);
			s >>= 8;
		}
		return result;
	}
	
	public static byte[] toBytes(int in)
	{
		byte[] result = new byte[4];
		for (int i = 3; i >= 0; i--)
		{
			result[i] = (byte) (in & 0xFF);
			in >>= 8;
		}
		return result;
	}
	
	public static byte[] toBytes(long l)
	{
		byte[] result = new byte[8];
		for (int i = 7; i >= 0; i--)
		{
			result[i] = (byte) (l & 0xFF);
			l >>= 8;
		}
		return result;
	}
	
	public static String byteToString(byte b)
	{
		return longToString(b);
	}
	
	public static String shortToString(short s)
	{
		return longToString(s);
	}
	
	public static String intToString(int i)
	{
		return longToString(i);
	}
	
	public static String longToString(long l)
	{
		//long max + sign
		char[] output = new char[20];
		int index = 19;
		long j = 1;
		//ensure we have a positive number so the modulo doesn't mess up
		long lpos;
		if (l < 0)
			lpos = l * -1;
		if (l == 0)
			return "0";
		else
			lpos = l;
		while (lpos > 0)
		{
			output[index--] = (char) (lpos % 10 + '0');
			lpos /= 10;
		}
		//add minus if number is negative
		if (l < 0)
			output[index] = '-';
		return String.compactStringFront(output);
	}
}
