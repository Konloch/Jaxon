package java.lang;

import utils.ArrayUtils;

public class String
{
	private final char[] value;
	private final int count;
	
	public String(char[] value)
	{
		this.value = value;
		this.count = value.length;
	}
	
	//returns a string with compacted value, removing all null bytes at the end
	public static String compactString(char[] value)
	{
		int realLen = value.length - 1;
		while (true)
		{
			if (realLen == 0)
				return "";
			if (value[realLen] == 0)
				realLen--; //last char is null byte, so cut it off
			else
				break;
		}
		char[] compactValue = new char[realLen + 1];
		for (int i = 0; i <= realLen; i++)
		{
			compactValue[i] = value[i];
		}
		return new String(compactValue);
	}
	
	public static String compactStringFront(char[] value)
	{
		int realStart = 0;
		while (true)
		{
			if (value[realStart] == 0)
				realStart++;
			else
				break;
		}
		char[] compactValue = new char[value.length - realStart];
		int realI = realStart;
		for (int i = 0; i < value.length - realI; i++)
		{
			compactValue[i] = value[realStart];
			realStart++;
		}
		return new String(compactValue);
	}
	
	@SJC.Inline
	public int length()
	{
		return count;
	}
	
	@SJC.Inline
	public char charAt(int i)
	{
		return value[i];
	}
	
	@SJC.Inline
	public char[] toCharArray()
	{
		return value;
	}
	
	@SJC.Inline
	public String reverse()
	{
		char[] rev = new char[count];
		for (int i = 0, j = count - 1; i <= j; i++, j--)
		{
			rev[i] = value[j];
			rev[j] = value[i];
		}
		return new String(rev);
	}
	
	@SJC.Inline
	public String concat(String s)
	{
		char[] buf = new char[s.count + this.count];
		//copy over this string first
		int index = 0;
		for (int i = 0; i < this.count; i++)
		{
			buf[index++] = this.value[i];
		}
		//copy over s after
		for (int i = 0; i < s.count; i++)
		{
			buf[index++] = s.value[i];
		}
		return new String(buf);
	}
	
	public boolean equals(String s)
	{
		if (this.length() != s.length())
			return false;
		for (int i = 0; i < s.length(); i++)
		{
			if (this.charAt(i) != s.charAt(i))
				return false;
		}
		return true;
	}
	
	public String removeNewlines()
	{
		char[] buf = new char[this.length()];
		int readIndex = 0;
		int writeIndex = 0;
		for (; readIndex < this.length(); readIndex++)
		{
			if (this.charAt(readIndex) != '\n')
			{
				buf[writeIndex] = this.charAt(readIndex);
				writeIndex++;
			}
		}
		return compactString(buf);
	}
	
	//splits a string into up to 128 substrings according to splitList
	public class CharSplit
	{
		public final String leftSide;
		public final String rightSide;
		public final char delimiter;
		
		CharSplit(String l, String r, char d)
		{
			leftSide = l;
			rightSide = r;
			delimiter = d;
		}
	}
	
	public String[] split(char delimiter, String s)
	{
		String[] splitStrings = new String[10];
		int sIn = 0;
		char[] currBuffer = new char[1024];
		int cIn = 0;
		for (int i = 0; i < s.length(); i++)
		{
			char c = s.charAt(i);
			if (c == delimiter && cIn > 0)
			{
				splitStrings[sIn] = String.compactString(currBuffer);
				sIn++;
				if (sIn > 9)
					MAGIC.inline(0xCC);
				currBuffer = ArrayUtils.cleanBuffer(currBuffer);
				cIn = 0;
			}
			else
			{
				currBuffer[cIn] = c;
				cIn++;
			}
		}
		//gotta put the remainder of the buffer in a string after being done going over every char
		splitStrings[sIn] = String.compactString(currBuffer);
		sIn++;
		String[] retval = new String[sIn];
		for (int i = 0; i < sIn; i++)
		{
			retval[i] = splitStrings[i];
		}
		//CharSplit sp = new CharSplit("foo", "bar", ' ');
		return retval;
	}
	
	
}