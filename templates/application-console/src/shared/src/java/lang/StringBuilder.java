package java.lang;

/**
 * @author Konloch
 * @since 7/17/2024
 */
public class StringBuilder
{
	private char[] value;
	private int count;
	
	//constructor initializes with a default capacity
	public StringBuilder()
	{
		value = new char[16]; // Initial capacity
		count = 0;
	}
	
	public StringBuilder(Object a)
	{
		this();
		append(a.toString());
	}
	
	public StringBuilder(int a)
	{
		this();
		append((char) a);
	}
	
	//append an int to the end of the builder
	public StringBuilder append(int i)
	{
		append((char) i);
		return this;
	}
	
	//append an int to the end of the builder
	public StringBuilder append(Object o)
	{
		append(o.toString());
		return this;
	}
	
	//append a string to the end of the builder
	public StringBuilder append(String str)
	{
		if (str == null)
			str = "null";
		
		int len = str.length();
		ensureCapacity(count + len);
		str.getChars(0, len, value, count);
		count += len;
		return this;
	}
	
	//append a single character to the end of the builder
	public StringBuilder append(char c)
	{
		ensureCapacity(count + 1);
		value[count++] = c;
		return this;
	}
	
	//ensure capacity of the underlying char array
	private void ensureCapacity(int minCapacity)
	{
		if (minCapacity > value.length)
		{
			int newCapacity = (value.length + 1) * 2;
			
			//overflow check
			if (newCapacity < 0)
				throw new OutOfMemoryError(new StringBuilder("Out of memory: ").append(newCapacity));
			
			while (newCapacity < minCapacity)
			{
				//double the capacity
				newCapacity = (newCapacity + 1) * 2;
				
				//overflow check
				if (newCapacity < 0)
					throw new OutOfMemoryError(new StringBuilder("Out of memory: ").append(newCapacity));
			}
			
			//expand the array and clone it
			value = Arrays.clone(newCapacity, value);
		}
	}
	
	//convert builder content to a String
	public String toString()
	{
		return new String(value, 0, count);
	}
}