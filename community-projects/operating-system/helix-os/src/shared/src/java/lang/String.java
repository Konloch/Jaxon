package java.lang;

import kernel.Kernel;

public class String
{
	private byte[] value;
	private int count;
	
	public String(byte[] value)
	{
		this.value = value;
		this.count = value.length;
	}
	
	@SJC.Inline
	public int length()
	{
		return count;
	}
	
	@SJC.Inline
	public byte get(int i)
	{
		return value[i];
	}
	
	@SJC.Inline
	public byte[] getBytes()
	{
		return value;
	}
	
	public char[] toCharArray()
	{
		char[] copy = new char[count];
		for (int i = 0; i < count; i++)
		{
			copy[i] = (char) value[i];
		}
		return copy;
	}
	
	public String toUpperCase()
	{
		Kernel.panic("toUpperCase is a dummy method");
		return "to upper case dummy method";
	}
	
	public byte[] toByteArray()
	{
		byte[] copy = new byte[count];
		for (int i = 0; i < count; i++)
		{
			copy[i] = (byte) value[i];
		}
		return copy;
	}
	
	public String LeftPad(int length, char c)
	{
		int pad = length - count;
		if (pad <= 0)
		{
			return this;
		}
		byte[] padded = new byte[length];
		for (int i = 0; i < pad; i++)
		{
			padded[i] = (byte) c;
		}
		for (int i = 0; i < count; i++)
		{
			padded[i + pad] = value[i];
		}
		return new String(padded);
	}
	
	public String append(String other)
	{
		byte[] appended = new byte[count + other.count];
		for (int i = 0; i < count; i++)
		{
			appended[i] = value[i];
		}
		for (int i = 0; i < other.count; i++)
		{
			appended[i + count] = other.value[i];
		}
		return new String(appended);
	}
	
	public String append(int i)
	{
		return append(Integer.toString(i, 10));
	}
	
	public String append(char c)
	{
		byte[] appended = new byte[count + 1];
		for (int i = 0; i < count; i++)
		{
			appended[i] = value[i];
		}
		appended[count] = (byte) c;
		return new String(appended);
	}
}