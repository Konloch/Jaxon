package java.lang;

/**
 * The abstract class {@link Number} is the parent class of the classes
 * {@link Byte}, {@link Integer}, {@link Long} and {@link Short}.
 * <p>
 * Child classes of {@link Number} must provide methods that convert the
 * represented numeric value to {@link byte}, {@link double},
 * {@link float}, {@link int}, {@link long} and {@link short}.
 * </p>
 */
public abstract class Number
{
	
	/**
	 * Returns the value of the specified number as {@link byte}. This can
	 * Include rounding and truncation.
	 *
	 * @return The numeric value represented by this object
	 * after it has been converted to the type {@link byte}.
	 */
	public byte byteValue()
	{
		return (byte) this.intValue();
	}
	
	/**
	 * Returns the value of the specified number as {@link double}. This can
	 * Include rounding and truncation.
	 *
	 * @return The numeric value represented by this object
	 * after it has been converted to the type {@link double}.
	 */
	public abstract double doubleValue();
	
	/**
	 * Returns the value of the specified number as {@link float}. This can
	 * Include rounding and truncation.
	 *
	 * @return The numeric value represented by this object
	 * after it has been converted to the type {@link float}.
	 */
	public abstract float floatValue();
	
	/**
	 * Returns the value of the specified number as {@link int}. This can
	 * Include rounding and truncation.
	 *
	 * @return The numeric value represented by this object
	 * after it has been converted to the type {@link int}.
	 */
	public abstract int intValue();
	
	/**
	 * Returns the value of the specified number as {@link long}. This can
	 * Include rounding and truncation.
	 *
	 * @return The numeric value represented by this object
	 * after it has been converted to the type {@link long}.
	 */
	public abstract long longValue();
	
	/**
	 * Returns the value of the specified number as {@link short}. This can
	 * Include rounding and truncation.
	 *
	 * @return The numeric value represented by this object
	 * after it has been converted to the type {@link short}.
	 */
	public short shortValue()
	{
		return (short) this.intValue();
	}
}
