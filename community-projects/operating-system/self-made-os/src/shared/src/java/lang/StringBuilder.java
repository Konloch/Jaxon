package java.lang;

import utils.TypeConv;

public class StringBuilder
{
	private static final int DEFAULT_CAPACITY = 16;
	private static final int DEFAULT_EXPANSION_FACTOR = 2;
	private char[] buf;
	private int length, capacity;
	
	//constructors
	public StringBuilder()
	{
		length = 0;
		capacity = DEFAULT_CAPACITY;
		buf = new char[capacity];
	}
	
	public StringBuilder(int capacity)
	{
		if (capacity < 0)
			MAGIC.inline(0xCC);
		length = 0;
		this.capacity = capacity;
		buf = new char[capacity];
	}
	
	public StringBuilder(String value)
	{
		length = 0;
		capacity = value.length() + DEFAULT_CAPACITY;
		buf = new char[capacity];
		appendInternal(value.toCharArray());
	}
	
	public StringBuilder(String value, int capacity)
	{
		if (capacity < 0)
			MAGIC.inline(0xCC);
		length = 0;
		this.capacity = value.length() + capacity;
		appendInternal(value.toCharArray());
	}
	
	//appending
	@SJC.Inline
	private void appendInternal(char[] val)
	{
		while (length + val.length >= capacity)
			expandBuffer();
		for (char c : val)
		{
			appendInternal(c);
		}
	}
	
	@SJC.Inline
	private void appendInternal(char c)
	{
		while (length + 1 >= capacity)
			expandBuffer();
		buf[length] = c;
		length++;
	}
	
	public void append(char c)
	{
		appendInternal(c);
	}
	
	public void append(char[] val)
	{
		appendInternal(val);
	}
	
	public void append(String s)
	{
		appendInternal(s.toCharArray());
	}
	
	public void append(byte b)
	{
		appendInternal(TypeConv.byteToString(b).toCharArray());
	}
	
	public void append(short s)
	{
		appendInternal(TypeConv.shortToString(s).toCharArray());
	}
	
	public void append(int i)
	{
		appendInternal(TypeConv.intToString(i).toCharArray());
	}
	
	public void append(long l)
	{
		appendInternal(TypeConv.longToString(l).toCharArray());
	}
	
	//retrieval
	public String getString()
	{
		return String.compactString(buf);
	}
	
	//size
	//expands string buffer to given size if needed
	public void ensureSize(int size)
	{
		if (size > capacity)
			expandBufferInternal(size);
	}
	
	//expands buffer by DEFAULT_EXPANSION_FACTOR
	private void expandBuffer()
	{
		expandBuffer(DEFAULT_EXPANSION_FACTOR);
	}
	
	private void expandBuffer(int factor)
	{
		//check for overflow
		if (capacity * factor < 0)
		{
			MAGIC.inline(0xCC);
		}
		expandBufferInternal(capacity * factor);
	}
	
	private void expandBufferInternal(int newCapacity)
	{
		//sanity check
		if (newCapacity < capacity)
		{
			MAGIC.inline(0xCC);
		}
		//expand, copy, length can stay the same because effectively not changed
		capacity = newCapacity;
		char[] newBuf = new char[capacity];
		int i = 0;
		for (char c : buf)
		{
			newBuf[i] = c;
			i++;
		}
		buf = newBuf;
	}
}
