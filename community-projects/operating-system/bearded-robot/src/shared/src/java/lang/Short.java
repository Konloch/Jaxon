package java.lang;

/**
 * The {@link Short} class encapsulates a value with the
 * Standard data type {@link Short}. An object of type {@link Short}
 * contains a single field of type {@link Short}.
 */
public class Short extends Number
{
	/**
	 * A constant that specifies the maximum value that can be assigned to a {@link Short}
	 * can be assigned. (2¹⁶-1)
	 */
	public static final short MAX_VALUE = 32767;
	
	/**
	 * A constant that specifies the minimum value that can be assigned to a {@link Short}
	 * can be assigned. (-2¹⁶)
	 */
	public static final short MIN_VALUE = -32768;
	
	/**
	 * The number of bits used to represent a {@link Short} value
	 * in two's complement binary format.
	 */
	public static final int SIZE = 16;
	
	/**
	 * The {@link Short} value that this object represents.
	 */
	private final short value;
	
	/**
	 * Creates a new instance of the {@link Short} class, which represents
	 * represents the specified {@link Short} value.
	 *
	 * @param value The value to be represented by the {@link Short} object.
	 * is to be represented.
	 */
	public Short(short value)
	{
		this.value = value;
	}
	
	/**
	 * Returns the value of this {@link Short} as {@link double}.
	 *
	 * @return The numeric value represented by this object
	 * after it has been converted to the type {@link double}.
	 */
	@Override
	public double doubleValue()
	{
		return (double) this.value;
	}
	
	/**
	 * Returns the value of this {@link Short} as {@link float}.
	 *
	 * @return The numerical value represented by this object
	 * after it has been converted to the type {@link float}.
	 */
	@Override
	public float floatValue()
	{
		return (float) this.value;
	}
	
	/**
	 * Returns the value of this {@link Short} as {@link int}.
	 *
	 * @return The numeric value represented by this object
	 * after it has been converted to the type {@link int}.
	 */
	@Override
	public int intValue()
	{
		return (int) this.value;
	}
	
	/**
	 * Returns the value of this {@link Short} as {@link long}.
	 *
	 * @return The numeric value represented by this object
	 * after it has been converted to the type {@link long}.
	 */
	@Override
	public long longValue()
	{
		return (long) this.value;
	}
}
