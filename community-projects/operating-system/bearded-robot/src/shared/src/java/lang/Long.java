package java.lang;

/**
 * The {@link Long} class encapsulates a value with the
 * Standard data type {@link Long}. An object of type {@link Long}
 * contains a single field of type {@link Long}.
 */
public class Long extends Number
{
	/**
	 * A constant that specifies the maximum value that can be assigned to a {@link Long}
	 * can be assigned. (2⁶³-1)
	 */
	public static final long MAX_VALUE = 9223372036854775807L;
	
	/**
	 * A constant that specifies the minimum value that can be assigned to a {@link Long}
	 * can be assigned. (-2⁶³)
	 */
	public static final long MIN_VALUE = -9223372036854775808L;
	
	/**
	 * The number of bits used to represent a {@link Long} value
	 * in two's complement binary format.
	 */
	public static final int SIZE = 64;
	
	/**
	 * The {@link Long} value that this object represents.
	 */
	private final long value;
	
	/**
	 * Creates a new instance of the {@link Long} class, which represents
	 * represents the specified {@link Long} value.
	 *
	 * @param value The value to be represented by the {@link Long} object.
	 * is to be represented.
	 */
	public Long(long value)
	{
		this.value = value;
	}
	
	/**
	 * Returns the value of this {@link Long} as {@link double}.
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
	 * Returns the value of this {@link Long} as {@link float}.
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
	 * Returns the value of this {@link Long} as {@link int}.
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
	 * Returns the value of this {@link Long} as {@link Long}.
	 *
	 * @return The numeric value represented by this object
	 * after it has been converted to the type {@link Long}.
	 */
	@Override
	public long longValue()
	{
		return this.value;
	}
}
