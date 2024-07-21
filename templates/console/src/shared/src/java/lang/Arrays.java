package java.lang;

/**
 * @author Konloch
 * @since 7/18/2024
 */
public class Arrays
{
	public static boolean[] clone(boolean[] array)
	{
		return clone(array.length, array);
	}
	
	public static boolean[] clone(int newCapacity, boolean[] array)
	{
		boolean[] newValue = new boolean[newCapacity];
		for (int i = 0; i < array.length; i++)
			newValue[i] = array[i];
		
		return newValue;
	}
	
	public static byte[] clone(byte[] array)
	{
		return clone(array.length, array);
	}
	
	public static byte[] clone(int newCapacity, byte[] array)
	{
		byte[] newValue = new byte[newCapacity];
		for (int i = 0; i < array.length; i++)
			newValue[i] = array[i];
		
		return newValue;
	}
	
	public static short[] clone(short[] array)
	{
		return clone(array.length, array);
	}
	
	public static short[] clone(int newCapacity, short[] array)
	{
		short[] newValue = new short[newCapacity];
		for (int i = 0; i < array.length; i++)
			newValue[i] = array[i];
		
		return newValue;
	}
	
	public static char[] clone(char[] array)
	{
		return clone(array.length, array);
	}
	
	public static char[] clone(int newCapacity, char[] array)
	{
		char[] newValue = new char[newCapacity];
		for (int i = 0; i < array.length; i++)
			newValue[i] = array[i];
		
		return newValue;
	}
	
	public static int[] clone(int[] array)
	{
		return clone(array.length, array);
	}
	
	public static int[] clone(int newCapacity, int[] array)
	{
		int[] newValue = new int[newCapacity];
		for (int i = 0; i < array.length; i++)
			newValue[i] = array[i];
		
		return newValue;
	}
	
	public static long[] clone(long[] array)
	{
		return clone(array.length, array);
	}
	
	public static long[] clone(int newCapacity, long[] array)
	{
		long[] newValue = new long[newCapacity];
		for (int i = 0; i < array.length; i++)
			newValue[i] = array[i];
		
		return newValue;
	}
	
	public static Object[] clone(Object[] array)
	{
		return clone(array.length, array);
	}
	
	public static Object[] clone(int newCapacity, Object[] array)
	{
		Object[] newValue = new Object[newCapacity];
		for (int i = 0; i < array.length; i++)
			newValue[i] = array[i];
		
		return newValue;
	}
	
	public static Object[] merge(Object[] partA, Object[] partB)
	{
		Object[] array = new Object[partA.length + partB.length];
		
		int index = 0;
		for (Object arg : partA)
			array[index++] = arg;
		
		for (Object dir : partB)
			array[index++] = dir;
		
		return array;
	}
}
