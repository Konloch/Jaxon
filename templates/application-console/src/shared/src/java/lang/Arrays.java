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
	
	public static boolean[] merge(boolean[] partA, boolean[] partB)
	{
		boolean[] array = new boolean[partA.length + partB.length];
		
		int index = 0;
		for (boolean arg : partA)
			array[index++] = arg;
		
		for (boolean dir : partB)
			array[index++] = dir;
		
		return array;
	}
	
	public static byte[] merge(byte[] partA, byte[] partB)
	{
		byte[] array = new byte[partA.length + partB.length];
		
		int index = 0;
		for (byte arg : partA)
			array[index++] = arg;
		
		for (byte dir : partB)
			array[index++] = dir;
		
		return array;
	}
	
	public static short[] merge(short[] partA, short[] partB)
	{
		short[] array = new short[partA.length + partB.length];
		
		int index = 0;
		for (short arg : partA)
			array[index++] = arg;
		
		for (short dir : partB)
			array[index++] = dir;
		
		return array;
	}
	
	public static char[] merge(char[] partA, char[] partB)
	{
		char[] array = new char[partA.length + partB.length];
		
		int index = 0;
		for (char arg : partA)
			array[index++] = arg;
		
		for (char dir : partB)
			array[index++] = dir;
		
		return array;
	}
	
	public static int[] merge(int[] partA, int[] partB)
	{
		int[] array = new int[partA.length + partB.length];
		
		int index = 0;
		for (int arg : partA)
			array[index++] = arg;
		
		for (int dir : partB)
			array[index++] = dir;
		
		return array;
	}
	
	public static long[] merge(long[] partA, long[] partB)
	{
		long[] array = new long[partA.length + partB.length];
		
		int index = 0;
		for (long arg : partA)
			array[index++] = arg;
		
		for (long dir : partB)
			array[index++] = dir;
		
		return array;
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
