package java.lang;

/**
 * @author Konloch
 * @since 7/17/2024
 */
public class String
{
	public char[] value;
	public int length;
	
	public String()
	{
	
	}
	
	public String(byte[] src)
	{
		this.length = src.length;
		this.value = new char[length];
		
		//convert byte to char assuming ASCII
		for (int i = 0; i < length; i++)
			this.value[i] = (char) (src[i] & 0xFF);
	}
	
	public String(char[] src)
	{
		this.value = src;
		length = src.length;
	}
	
	public String(char[] src, int length)
	{
		this.value = src;
		this.length = length;
	}
	
	public String(char[] src, int srcPos, int length)
	{
		if(srcPos == 0)
			this.value = src;
		else
		{
			this.value = new char[length];
			Arrays.copy(src, srcPos, value, 0, value.length);
		}
		this.length = length;
	}
	
	public int length()
	{
		return length;
	}
	
	public boolean startsWith(String prefix)
	{
		if (prefix.length > this.length)
			return false;
		
		for (int i = 0; i < prefix.length; i++)
			if (this.value[i] != prefix.value[i])
				return false;
		
		return true;
	}
	
	public boolean endsWith(String suffix)
	{
		if (suffix.length > this.length)
			return false;
		
		for (int i = 0; i < suffix.length; i++)
			if (this.value[this.length - suffix.length + i] != suffix.value[i])
				return false;
		
		return true;
	}
	
	public String concat(String str)
	{
		char[] newValue = new char[this.length + str.length];
		if (this.length >= 0)
			Arrays.copy(this.value, 0, newValue, 0, this.length);
		
		if (str.length >= 0)
			Arrays.copy(str.value, 0, newValue, this.length, str.length);
		
		return new String(newValue);
	}
	
	public char charAt(int index)
	{
		if (index < 0 || index >= this.length)
			throw new IndexOutOfBoundsException(new StringBuilder("Index Out Of Bounds: ")
					.append(index)
					.append(", Length: ")
					.append(this.length));
		
		return this.value[index];
	}
	
	public int indexOf(String str)
	{
		if (str.length > this.length)
			return -1;
		
		for (int i = 0; i <= this.length - str.length; i++)
		{
			int j;
			for (j = 0; j < str.length; j++)
				if (this.value[i + j] != str.value[j])
					break;
			
			if (j == str.length)
				return i;
		}
		
		return -1;
	}
	
	public int indexOf(char ch)
	{
		for (int i = 0; i < this.length; i++)
			if (this.value[i] == ch)
				return i;
		
		return -1;
	}
	
	public int lastIndexOf(String str)
	{
		if (str.length > this.length)
			return -1;
		
		for (int i = this.length - str.length; i >= 0; i--)
		{
			int j;
			for (j = 0; j < str.length; j++)
				if (this.value[i + j] != str.value[j])
					break;
			
			if (j == str.length)
				return i;
		}
		
		return -1;
	}
	
	public int lastIndexOf(char ch)
	{
		for (int i = this.length - 1; i >= 0; i--)
			if (this.value[i] == ch)
				return i;
		
		return -1;
	}
	
	public String substring(int start)
	{
		if (start < 0 || start > this.length)
			throw new IndexOutOfBoundsException(new StringBuilder("Invalid start index: ")
					.append(start));
		
		char[] subValue = new char[this.length - start];
		Arrays.copy(this.value, start, subValue, 0, this.length - start);
		return new String(subValue);
	}
	
	public String substring(int start, int end)
	{
		if (start < 0 || start > this.length || end < start || end > this.length)
			throw new IndexOutOfBoundsException(new StringBuilder("Index Out Of Bounds: start=")
					.append(start).append(", end=")
					.append(end));
		
		char[] subValue = new char[end - start];
		Arrays.copy(this.value, start, subValue, 0, end - start);
		return new String(subValue);
	}
	
	public void getChars(int srcBegin, int srcEnd, char[] dst, int dstBegin)
	{
		if (srcBegin < 0 || srcEnd > length || srcBegin > srcEnd)
			throw new IndexOutOfBoundsException(new StringBuilder("Index Out Of Bounds: start=")
					.append(srcBegin).append(", end=")
					.append(srcEnd));
		
		int dstIndex = dstBegin;
		for (int i = srcBegin; i < srcEnd; i++)
			dst[dstIndex++] = value[i];
	}
	
	public boolean equals(Object o)
	{
		if (this == o)
			return true;
		if (!(o instanceof String))
			return false;
		
		String s = (String) o;
		
		if (length != s.length)
			return false;
		
		for (int i = 0; i < length; i++)
			if (value[i] != s.value[i])
				return false;
		
		return true;
	}
	
	public int hashCode()
	{
		int result = 17;
		result = 31 * result + length;
		for (int i = 0; i < length; i++)
			result = 31 * result + value[i];
		return result;
	}
	
	public String toString()
	{
		return this;
	}
	
	/**
	 * Default as UTF-8
	 *
	 * @return The String represented as a UTF-8 byte array
	 */
	public byte[] toByteArray()
	{
		byte[] byteArray = new byte[length * 3];
		int index = 0;
		
		for (int i = 0; i < length; i++)
		{
			char c = value[i];
			if (c <= 0x7F)
				byteArray[index++] = (byte) c;
			else if (c <= 0x7FF)
			{
				byteArray[index++] = (byte) (0xC0 | (c >> 6));
				byteArray[index++] = (byte) (0x80 | (c & 0x3F));
			}
			else
			{
				byteArray[index++] = (byte) (0xE0 | (c >> 12));
				byteArray[index++] = (byte) (0x80 | ((c >> 6) & 0x3F));
				byteArray[index++] = (byte) (0x80 | (c & 0x3F));
			}
		}
		
		byte[] result = new byte[index];
		Arrays.copy(byteArray, 0, result, 0, index);
		return result;
	}
}