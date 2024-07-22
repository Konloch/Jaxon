package java.lang;

/**
 * @author Konloch
 * @since 7/20/2024
 */
public class Error extends Throwable
{
	public Error(String detailMessage)
	{
		super(detailMessage);
	}
	
	public Error(StringBuilder detailMessage)
	{
		this(detailMessage.toString());
	}
}
