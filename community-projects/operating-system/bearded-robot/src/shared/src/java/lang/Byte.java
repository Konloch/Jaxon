package java.lang;

/**
 * The {@link Byte} class encapsulates a value with the
 * standard data type {@link int}. An object of type {@link Byte}
 * contains a single field of type {@link Byte}.
 */
public class Byte extends Number
{
	/**
	 * A constant that specifies the maximum value that can be assigned to a {@link Byte}
	 * can be assigned. (2⁷)
	 */
	public static final byte MAX_VALUE = 127;
	
	/**
	 * A constant that specifies the minimum value that can be assigned to a {@link Byte}
	 * can be assigned. (-2⁷)
	 */
	public static final byte MIN_VALUE = -128;
	
	/**
	 * The number of bits used to represent a {@link Byte} value
	 * in two's complement binary format.
	 */
	public static final int SIZE = 8;
	
	/**
	 * The {@link int} value that this object represents.
	 */
	private final byte value;
	
	/**
	 * Creates a new instance of the {@link Byte} class, which represents
	 * represents the specified {@link Byte} value.
	 *
	 * @param value The value to be represented by the {@link Byte} object.
	 * is to be represented.
	 */
	public Byte(byte value)
	{
		this.value = value;
	}
	
	/**
	 * Returns the value of this {@link Byte} as {@link double}.
	 *
	 * @return The numerical value represented by this object
	 * after it has been converted to the type {@link double}.
	 */
	@Override
	public double doubleValue()
	{
		return (double) this.value;
	}
	
	/**
	 * Returns the value of this {@link Byte} as {@link float}.
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
	 * Returns the value of this {@link Byte} as {@link int}.
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
	 * Returns the value of this {@link Byte} as {@link long}.
	 *
	 * @return The numerical value represented by this object
	 * after it has been converted to the type {@link long}.
	 */
	@Override
	public long longValue()
	{
		return (long) this.value;
	}
}
