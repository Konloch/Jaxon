package java.lang;

/**
 * @author Konloch
 * @since 7/18/2024
 */
public class Arrays
{
	public static boolean[] clone(boolean[] src)
	{
		return clone(src, 0, 0, src.length);
	}
	
	public static boolean[] clone(boolean[] src, int srcPos, int destPos, int length)
	{
		boolean[] newValue = new boolean[length];
		copy(src, srcPos, newValue, destPos, length);
		return newValue;
	}
	
	public static byte[] clone(byte[] src)
	{
		return clone(src, 0, 0, src.length);
	}
	
	public static byte[] clone(byte[] src, int srcPos, int destPos, int length)
	{
		byte[] newValue = new byte[length];
		copy(src, srcPos, newValue, destPos, length);
		return newValue;
	}
	
	public static short[] clone(short[] src)
	{
		return clone(src, 0, 0, src.length);
	}
	
	public static short[] clone(short[] src, int srcPos, int destPos, int length)
	{
		short[] newValue = new short[length];
		copy(src, srcPos, newValue, destPos, length);
		return newValue;
	}
	
	public static char[] clone(char[] src)
	{
		return clone(src, 0, 0, src.length);
	}
	
	public static char[] clone(char[] src, int srcPos, int destPos, int length)
	{
		char[] newValue = new char[length];
		copy(src, srcPos, newValue, destPos, length);
		return newValue;
	}
	
	public static int[] clone(int[] src)
	{
		return clone(src, 0, 0, src.length);
	}
	
	public static int[] clone(int[] src, int srcPos, int destPos, int length)
	{
		int[] newValue = new int[length];
		copy(src, srcPos, newValue, destPos, length);
		return newValue;
	}
	
	public static long[] clone(long[] src)
	{
		return clone(src, 0, 0, src.length);
	}
	
	public static long[] clone(long[] src, int srcPos, int destPos, int length)
	{
		long[] newValue = new long[length];
		copy(src, srcPos, newValue, destPos, length);
		return newValue;
	}
	
	public static double[] clone(double[] src)
	{
		return clone(src, 0, 0, src.length);
	}
	
	public static double[] clone(double[] src, int srcPos, int destPos, int length)
	{
		double[] newValue = new double[length];
		copy(src, srcPos, newValue, destPos, length);
		return newValue;
	}
	
	public static float[] clone(float[] src)
	{
		return clone(src, 0, 0, src.length);
	}
	
	public static float[] clone(float[] src, int srcPos, int destPos, int length)
	{
		float[] newValue = new float[length];
		copy(src, srcPos, newValue, destPos, length);
		return newValue;
	}
	
	public static Object[] clone(Object[] src)
	{
		return clone(src, 0, 0, src.length);
	}
	
	public static Object[] clone(Object[] src, int srcPos, int destPos, int length)
	{
		Object[] newValue = new Object[length];
		copy(src, srcPos, newValue, destPos, length);
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
	
	public static double[] merge(double[] partA, double[] partB)
	{
		double[] array = new double[partA.length + partB.length];
		
		int index = 0;
		for (double arg : partA)
			array[index++] = arg;
		
		for (double dir : partB)
			array[index++] = dir;
		
		return array;
	}
	
	public static float[] merge(float[] partA, float[] partB)
	{
		float[] array = new float[partA.length + partB.length];
		
		int index = 0;
		for (float arg : partA)
			array[index++] = arg;
		
		for (float dir : partB)
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
	
	public static void copy(boolean[] src, int srcPos, boolean[] dest, int destPos, int length)
	{
		if (src == null || dest == null)
			throw new NullPointerException("Source or destination array is null");
		
		if (srcPos < 0 || destPos < 0 || length < 0 || srcPos + length > src.length || destPos + length > dest.length)
			throw new IndexOutOfBoundsException("Invalid array index or length");
		
		for (int i = 0; i < length; i++)
			dest[destPos + i] = src[srcPos + i];
	}
	
	public static void copy(byte[] src, int srcPos, byte[] dest, int destPos, int length)
	{
		if (src == null || dest == null)
			throw new NullPointerException("Source or destination array is null");
		
		if (srcPos < 0 || destPos < 0 || length < 0 || srcPos + length > src.length || destPos + length > dest.length)
			throw new IndexOutOfBoundsException("Invalid array index or length");
		
		for (int i = 0; i < length; i++)
			dest[destPos + i] = src[srcPos + i];
	}
	
	public static void copy(short[] src, int srcPos, short[] dest, int destPos, int length)
	{
		if (src == null || dest == null)
			throw new NullPointerException("Source or destination array is null");
		
		if (srcPos < 0 || destPos < 0 || length < 0 || srcPos + length > src.length || destPos + length > dest.length)
			throw new IndexOutOfBoundsException("Invalid array index or length");
		
		for (int i = 0; i < length; i++)
			dest[destPos + i] = src[srcPos + i];
	}
	
	public static void copy(char[] src, int srcPos, char[] dest, int destPos, int length)
	{
		if (src == null || dest == null)
			throw new NullPointerException("Source or destination array is null");
		
		if (srcPos < 0 || destPos < 0 || length < 0 || srcPos + length > src.length || destPos + length > dest.length)
			throw new IndexOutOfBoundsException("Invalid array index or length");
		
		for (int i = 0; i < length; i++)
			dest[destPos + i] = src[srcPos + i];
	}
	
	public static void copy(int[] src, int srcPos, int[] dest, int destPos, int length)
	{
		if (src == null || dest == null)
			throw new NullPointerException("Source or destination array is null");
		
		if (srcPos < 0 || destPos < 0 || length < 0 || srcPos + length > src.length || destPos + length > dest.length)
			throw new IndexOutOfBoundsException("Invalid array index or length");
		
		for (int i = 0; i < length; i++)
			dest[destPos + i] = src[srcPos + i];
	}
	
	public static void copy(long[] src, int srcPos, long[] dest, int destPos, int length)
	{
		if (src == null || dest == null)
			throw new NullPointerException("Source or destination array is null");
		
		if (srcPos < 0 || destPos < 0 || length < 0 || srcPos + length > src.length || destPos + length > dest.length)
			throw new IndexOutOfBoundsException("Invalid array index or length");
		
		for (int i = 0; i < length; i++)
			dest[destPos + i] = src[srcPos + i];
	}
	
	public static void copy(double[] src, int srcPos, double[] dest, int destPos, int length)
	{
		if (src == null || dest == null)
			throw new NullPointerException("Source or destination array is null");
		
		if (srcPos < 0 || destPos < 0 || length < 0 || srcPos + length > src.length || destPos + length > dest.length)
			throw new IndexOutOfBoundsException("Invalid array index or length");
		
		for (int i = 0; i < length; i++)
			dest[destPos + i] = src[srcPos + i];
	}
	
	public static void copy(float[] src, int srcPos, float[] dest, int destPos, int length)
	{
		if (src == null || dest == null)
			throw new NullPointerException("Source or destination array is null");
		
		if (srcPos < 0 || destPos < 0 || length < 0 || srcPos + length > src.length || destPos + length > dest.length)
			throw new IndexOutOfBoundsException("Invalid array index or length");
		
		for (int i = 0; i < length; i++)
			dest[destPos + i] = src[srcPos + i];
	}
	
	public static void copy(Object[] src, int srcPos, Object[] dest, int destPos, int length)
	{
		if (src == null || dest == null)
			throw new NullPointerException("Source or destination array is null");
		
		if (srcPos < 0 || destPos < 0 || length < 0 || srcPos + length > src.length || destPos + length > dest.length)
			throw new IndexOutOfBoundsException("Invalid array index or length");
		
		for (int i = 0; i < length; i++)
			dest[destPos + i] = src[srcPos + i];
	}
}
