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
	
	public String(byte[] value)
	{
		this.count = value.length;
		this.value = new char[count];
		
		//convert byte to char assuming ASCII
		for (int i = 0; i < count; i++)
			this.value[i] = (char) (value[i] & 0xFF);
	}
	
	public String(char[] value)
	{
		this.value = value;
		count = value.length;
	}
	
	public String(char[] value, int length)
	{
		this.value = value;
		count = length;
	}
	
	public String(char[] value, int start, int length)
	{
		//TODO implement start
		this.value = value;
		count = length;
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
		for (int i = 0; i < this.count; i++)
			newValue[i] = this.value[i];
		
		for (int i = 0; i < str.count; i++)
			newValue[this.count + i] = str.value[i];
		
		return new String(newValue);
	}
	
	public char charAt(int index)
	{
		if (index < 0 || index >= this.count)
			return 0;
		//TODO re-add when exceptions are implemented
		//throw new IOException("Index: " + index + ", Length: " + this.count);
		
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
		for (int i = start; i < this.count; i++)
			subValue[i - start] = this.value[i];
		return new String(subValue);
	}
	
	public String substring(int start, int end)
	{
		if (start < 0 || start > this.count || end < start || end > this.count)
			throw new IndexOutOfBoundsException(new StringBuilder("Invalid indices: start=")
					.append(start)
					.append("end=")
					.append(end));
		
		char[] subValue = new char[end - start];
		for (int i = start; i < end; i++)
			subValue[i - start] = this.value[i];
		
		return new String(subValue);
	}
	
	public void getChars(int srcBegin, int srcEnd, char[] dst, int dstBegin)
	{
		if (srcBegin < 0 || srcEnd > count || srcBegin > srcEnd)
			throw new IndexOutOfBoundsException(new StringBuilder("Invalid indices: start=")
					.append(srcEnd)
					.append("end=")
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
}