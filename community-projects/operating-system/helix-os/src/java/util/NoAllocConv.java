package java.util;

import kernel.Kernel;

/*
 * A utility class for various conversions.
 * Performs conversions without allocating memory.
 */
public class NoAllocConv
{
	public static final byte[] ALPHABET = MAGIC.toByteArray("0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ", false);
	
	/**
	 * Converts an integer to a string representation in the specified base.
	 * Treats the integer as an unsigned integer.
	 *
	 * @param buffer      The memory buffer to store the string representation.
	 * @param byte_offset The byte offset between each character in the buffer.
	 * @param max_len     The maximum length of the string representation.
	 * @param n           The <b>unsigned</b> integer to convert.
	 * @param base        The base to use for the conversion (between 2 and 36).
	 * @return The number of digits in the string representation.
	 * @panic If the base is out of range or the requested
	 * length is negative.
	 */
	public static int iToA(int buffer, int byte_offset, int max_len, int n, int base)
	{
		if (base < 2 || base > 36)
			Kernel.panic("ConversionHelper: requested base out of range");
		
		// Special case for 0
		if (n == 0)
		{
			MAGIC.wMem8(buffer, (byte) '0');
			return 1;
		}
		
		n = Math.abs(n);
		max_len = Math.clamp(max_len, 0, max_len);
		
		// Prints each digit of the number but in reverse order
		int digit_count = 0;
		while (n > 0 && digit_count < max_len)
		{
			int digit = n % base;
			byte c = ALPHABET[digit];
			n /= base;
			MAGIC.wMem8(buffer + digit_count * byte_offset, c);
			digit_count++;
		}
		
		Array.reverseByteBuffer(buffer, byte_offset, digit_count);
		return digit_count;
	}
	
	/*
	 * Converts a byte to a character.
	 * 0..9 -> '0'..'9'
	 * 10..36 -> 'A'..'Z'
	 * Other values -> '\0'
	 */
	@SJC.Inline
	public static char iToC(int n)
	{
		if (n >= 0 && n <= 9)
			return (char) (n + '0');
		else if (n >= 10 && n <= 36)
			return (char) (n - 10 + 'A');
		else
			return '\0';
	}
	
	public static int iToA(byte[] buffer, int max_len, long n, int base)
	{
		if (base < 2 || base > 36)
			Kernel.panic("ConversionHelper: requested base out of range");
		
		// Special case for 0
		if (n == 0)
		{
			buffer[0] = (byte) '0';
			return 1;
		}
		
		n = Math.abs(n);
		max_len = Math.clamp(max_len, 0, max_len);
		
		// Prints each digit of the number but in reverse order
		int digit_count = 0;
		while (n > 0 && digit_count < max_len)
		{
			int digit = (int) (n % base);
			byte c = ALPHABET[digit];
			n /= base;
			buffer[digit_count] = c;
			digit_count++;
		}
		
		Array.reverseByteBuffer(buffer);
		return digit_count;
	}
	
}
