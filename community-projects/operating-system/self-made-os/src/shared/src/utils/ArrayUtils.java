package utils;

public class ArrayUtils
{
	public static char[] cleanBuffer(char[] buf)
	{
		for (int i = 0; i < buf.length; i++)
		{
			buf[i] = '\0';
		}
		return buf;
	}
	
	//gives a sub array of a, which contains low to high inclusive
	public static String[] subArray(String[] a, int low, int high)
	{
		int count = high - low + 1;
		//check if low is even contained in a, if not return empty array
		if (low > a.length - 1)
		{
			return new String[0];
		}
		String[] retval = new String[count];
		int j = low;
		for (int i = 0; i < count; i++)
		{
			//would be out of bounds
			if (j > a.length - 1)
				return retval;
			retval[i] = a[j];
			j++;
		}
		return retval;
	}
}
