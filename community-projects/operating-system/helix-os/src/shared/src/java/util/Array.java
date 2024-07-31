package java.util;

public class Array
{
	public static byte[] copyOf(byte[] original, int newLength)
	{
		byte[] copy = new byte[newLength];
		for (int i = 0; i < Math.Min(original.length, newLength); i++)
			copy[i] = original[i];
		
		return copy;
	}
	
	public static void reverse(char[] a)
	{
		int i = a.length - 1;
		int j = 0;
		while (i > j)
		{
			char temp = a[i];
			a[i] = a[j];
			a[j] = temp;
			i--;
			j++;
		}
	}
	
	public static void reverseByteBuffer(int buffer, int byte_offset, int len)
	{
		int i = len - 1;
		int j = 0;
		while (i > j)
		{
			byte temp = MAGIC.rMem8(buffer + i * byte_offset);
			MAGIC.wMem8(buffer + i * byte_offset, MAGIC.rMem8(buffer + j * byte_offset));
			MAGIC.wMem8(buffer + j * byte_offset, temp);
			i--;
			j++;
		}
	}
	
	public static void reverseByteBuffer(byte[] buffer)
	{
		int i = buffer.length - 1;
		int j = 0;
		while (i > j)
		{
			byte temp = buffer[i];
			buffer[i] = buffer[j];
			buffer[j] = temp;
			i--;
			j++;
		}
	}
}
