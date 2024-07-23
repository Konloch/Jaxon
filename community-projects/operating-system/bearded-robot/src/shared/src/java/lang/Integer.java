package java.lang;

/**
 * The {@link Integer} class encapsulates a value with the
 * default data type {@link int}. An object of type {@link java.lang.Integer}
 * contains a single field of type {@link int}.
 */
public class Integer extends Number
{
	/**
	 * A constant that specifies the maximum value that can be assigned to a {@link int}
	 * can be assigned. (2³¹-1)
	 */
	public static final int MAX_VALUE = 2147483647;
	
	/**
	 * A constant that specifies the minimum value that can be assigned to a {@link int}
	 * can be assigned. (-2³¹)
	 */
	public static final int MIN_VALUE = -2147483648;
	
	/**
	 * The number of bits used to represent a {@link int} value
	 * in two's complement binary format.
	 */
	public static final int SIZE = 32;
	
	/**
	 * The {@link int} value that this object represents.
	 */
	private final int value;
	
	/**
	 * Creates a new instance of the {@link Integer} class, which represents
	 * represents the specified {@link int} value.
	 *
	 * @param value The value to be represented by the {@link Integer} object.
	 * is to be represented.
	 */
	public Integer(int value)
	{
		this.value = value;
	}
	
	/**
	 * Returns the value of this {@link Integer} as {@link double}.
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
	 * Returns the value of this {@link Integer} as {@link float}.
	 *
	 * @return The numeric value represented by this object
	 * after it has been converted to the type {@link float}.
	 */
	@Override
	public float floatValue()
	{
		return (float) this.value;
	}
	
	/**
	 * Returns the value of this {@link Integer} as {@link int}.
	 *
	 * @return The numeric value represented by this object
	 * after it has been converted to the type {@link int}.
	 */
	@Override
	public int intValue()
	{
		return this.value;
	}
	
	/**
	 * Returns the value of this {@link Integer} as {@link long}.
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
