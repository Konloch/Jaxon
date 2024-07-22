package java.lang;

/**
 * @author Konloch
 * @since 7/17/2024
 */
public class StringBuilder
{
	private byte[] value;
	private int count;
	
	//constructor initializes with a default capacity
	public StringBuilder()
	{
		value = new byte[16]; // Initial capacity
		count = 0;
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
	public StringBuilder append(byte c)
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
			{
				return;
				//TODO
				//throw new OutOfMemoryError();
			}
			
			while (newCapacity < minCapacity)
			{
				newCapacity = (newCapacity + 1) * 2; //double the capacity
				//overflow check
				if (newCapacity < 0)
				{
					return;
					//TODO
					//throw new OutOfMemoryError();
				}
			}
			
			byte[] newValue = new byte[newCapacity];
			for (int i = 0; i < count; i++)
				newValue[i] = value[i];
			
			value = newValue;
		}
	}
	
	//convert builder content to a String
	public String toString()
	{
		return new String(value, 0, count);
	}
}
