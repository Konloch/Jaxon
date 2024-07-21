package java.lang;

/**
 * @author Konloch
 * @since 7/18/2024
 */
public class Arrays
{
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
