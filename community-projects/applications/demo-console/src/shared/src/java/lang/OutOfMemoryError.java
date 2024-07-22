package java.lang;

/**
 * @author Konloch
 * @since 7/20/2024
 */
public class OutOfMemoryError extends Error
{
	public OutOfMemoryError(String detailMessage)
	{
		super(detailMessage);
	}
	
	public OutOfMemoryError(StringBuilder detailMessage)
	{
		this(detailMessage.toString());
	}
}
