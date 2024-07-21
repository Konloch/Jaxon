package java.lang;

/**
 * @author Konloch
 * @since 7/21/2024
 */
public class BufferOverflowException extends RuntimeException
{
	public BufferOverflowException(String detailMessage)
	{
		super(detailMessage);
	}
	
	public BufferOverflowException(StringBuilder detailMessage)
	{
		this(detailMessage.toString());
	}
}
