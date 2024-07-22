package java.lang;

/**
 * @author Konloch
 * @since 7/21/2024
 */
public class NullPointerException extends RuntimeException
{
	public NullPointerException(String detailMessage)
	{
		super(detailMessage);
	}
	
	public NullPointerException(StringBuilder detailMessage)
	{
		this(detailMessage.toString());
	}
}
