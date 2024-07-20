package java.lang;

/**
 * @author Konloch
 * @since 7/20/2024
 */
//normally this would be an exception, but to make runtime exceptions work in SJC they need to be an error
public class RuntimeException extends Error
{
	public RuntimeException(String detailMessage)
	{
		super(detailMessage);
	}
	
	public RuntimeException(StringBuilder detailMessage)
	{
		this(detailMessage.toString());
	}
}