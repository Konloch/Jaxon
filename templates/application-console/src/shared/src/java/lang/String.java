package java.lang;

/**
 * @author Konloch
 * @since 7/17/2024
 */
public class String
{
	public char[] value;
	public int count;
	
	public String()
	{
	
	}
	
	public String(byte[] src)
	{
		this.count = src.length;
		this.value = new char[count];
		
		//convert byte to char assuming ASCII
		for (int i = 0; i < count; i++)
			this.value[i] = (char) (src[i] & 0xFF);
	}
	
	public String(char[] src)
	{
		this.value = src;
		count = src.length;
	}
	
	public String(char[] src, int length)
	{
		this.value = src;
		this.count = length;
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
		this.count = length;
	}
	
	public int length()
	{
		return count;
	}
	
	public boolean startsWith(String prefix)
	{
		if (prefix.count > this.count)
			return false;
		
		for (int i = 0; i < prefix.count; i++)
			if (this.value[i] != prefix.value[i])
				return false;
		
		return true;
	}
	
	public boolean endsWith(String suffix)
	{
		if (suffix.count > this.count)
			return false;
		
		for (int i = 0; i < suffix.count; i++)
			if (this.value[this.count - suffix.count + i] != suffix.value[i])
				return false;
		
		return true;
	}
	
	public String concat(String str)
	{
		char[] newValue = new char[this.count + str.count];
		if (this.count >= 0)
			Arrays.copy(this.value, 0, newValue, 0, this.count);
		
		if (str.count >= 0)
			Arrays.copy(str.value, 0, newValue, this.count, str.count);
		
		return new String(newValue);
	}
	
	public char charAt(int index)
	{
		if (index < 0 || index >= this.count)
			throw new IndexOutOfBoundsException(new StringBuilder("Index Out Of Bounds: ")
					.append(index)
					.append(", Length: ")
					.append(this.count));
		
		return this.value[index];
	}
	
	public int indexOf(String str)
	{
		if (str.count > this.count)
			return -1;
		
		for (int i = 0; i <= this.count - str.count; i++)
		{
			int j;
			for (j = 0; j < str.count; j++)
				if (this.value[i + j] != str.value[j])
					break;
			
			if (j == str.count)
				return i;
		}
		
		return -1;
	}
	
	public int indexOf(char ch)
	{
		for (int i = 0; i < this.count; i++)
			if (this.value[i] == ch)
				return i;
		
		return -1;
	}
	
	public int lastIndexOf(String str)
	{
		if (str.count > this.count)
			return -1;
		
		for (int i = this.count - str.count; i >= 0; i--)
		{
			int j;
			for (j = 0; j < str.count; j++)
				if (this.value[i + j] != str.value[j])
					break;
			
			if (j == str.count)
				return i;
		}
		
		return -1;
	}
	
	public int lastIndexOf(char ch)
	{
		for (int i = this.count - 1; i >= 0; i--)
			if (this.value[i] == ch)
				return i;
		
		return -1;
	}
	
	public String substring(int start)
	{
		if (start < 0 || start > this.count)
			throw new IndexOutOfBoundsException(new StringBuilder("Invalid start index: ")
					.append(start));
		
		char[] subValue = new char[this.count - start];
		Arrays.copy(this.value, start, subValue, 0, this.count - start);
		return new String(subValue);
	}
	
	public String substring(int start, int end)
	{
		if (start < 0 || start > this.count || end < start || end > this.count)
			throw new IndexOutOfBoundsException(new StringBuilder("Index Out Of Bounds: start=")
					.append(start).append(", end=")
					.append(end));
		
		char[] subValue = new char[end - start];
		Arrays.copy(this.value, start, subValue, 0, end - start);
		return new String(subValue);
	}
	
	public void getChars(int srcBegin, int srcEnd, char[] dst, int dstBegin)
	{
		if (srcBegin < 0 || srcEnd > count || srcBegin > srcEnd)
			throw new IndexOutOfBoundsException(new StringBuilder("Index Out Of Bounds: start=")
					.append(srcBegin).append(", end=")
					.append(srcEnd));
		
		int dstIndex = dstBegin;
		for (int i = srcBegin; i < srcEnd; i++)
			dst[dstIndex++] = value[i];
	}
	
	public static String valueOf(boolean value)
	{
		return value ? "true" : "false";
	}
	
	public static String valueOf(byte value)
	{
		return valueOf((long) value);
	}
	
	public static String valueOf(short value)
	{
		return valueOf((long) value);
	}
	
	public static String valueOf(int value)
	{
		return valueOf((long) value);
	}
	
	public static String valueOf(long value)
	{
		boolean isNegative = value < 0;
		
		if (isNegative)
			value = -value;
		
		if (value == 0)
			return "0";
		
		StringBuilder temp = new StringBuilder();
		while (value > 0)
		{
			temp.append((char) ('0' + (value % 10)));
			value /= 10;
		}
		
		if (isNegative)
			temp.append('-');
		
		return temp.toString();
	}
	
	public static String valueOf(Object value)
	{
		return value.toString();
	}
	
	public boolean equals(Object o)
	{
		if (this == o)
			return true;
		if (!(o instanceof String))
			return false;
		
		String s = (String) o;
		
		if (count != s.count)
			return false;
		
		for (int i = 0; i < count; i++)
			if (value[i] != s.value[i])
				return false;
		
		return true;
	}
	
	public int hashCode()
	{
		int result = 17;
		result = 31 * result + count;
		for (int i = 0; i < count; i++)
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
		byte[] byteArray = new byte[count * 3];
		int index = 0;
		
		for (int i = 0; i < count; i++)
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