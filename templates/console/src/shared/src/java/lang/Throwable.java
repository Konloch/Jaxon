package java.lang;

/**
 * @author Konloch
 * @since 7/20/2024
 */
public class Throwable
{
	public final String detailMessage;
	
	public Throwable(String detailMessage)
	{
		this.detailMessage = detailMessage;
	}
	
	public Throwable(StringBuilder detailMessage)
	{
		this.detailMessage = detailMessage.toString();
	}
}