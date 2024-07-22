package java.lang;

/**
 * @author Konloch
 * @since 7/20/2024
 */
public class IndexOutOfBoundsException extends RuntimeException
{
	public IndexOutOfBoundsException(String detailMessage)
	{
		super(detailMessage);
	}
	
	public IndexOutOfBoundsException(StringBuilder detailMessage)
	{
		this(detailMessage.toString());
	}
}
