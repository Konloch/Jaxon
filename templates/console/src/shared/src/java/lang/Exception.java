package java.lang;

/**
 * @author Konloch
 * @since 7/20/2024
 */
public class Exception extends Throwable
{
	public Exception(String detailMessage)
	{
		super(detailMessage);
	}
	
	public Exception(StringBuilder detailMessage)
	{
		this(detailMessage.toString());
	}
}
