package kernel.trace.logging;

public class LogEntry
{
	private String _message;
	private String _category;
	private String _timeHMS;
	private byte _priority;
	
	public LogEntry(String category, String message, byte priority, String time_HMS)
	{
		this._category = category;
		this._message = message;
		this._priority = priority;
		this._timeHMS = time_HMS;
	}
	
	@SJC.Inline
	public String category()
	{
		return _category;
	}
	
	@SJC.Inline
	public String message()
	{
		return _message;
	}
	
	@SJC.Inline
	public byte priority()
	{
		return _priority;
	}
	
	@SJC.Inline
	public String oriorityString()
	{
		switch (_priority)
		{
			case Logger.NONE:
				return "NONE";
			case Logger.TRACE:
				return "TRACE";
			case Logger.INFO:
				return "INFO";
			case Logger.WARNING:
				return "WARNING";
			case Logger.ERROR:
				return "ERROR";
			case Logger.FATAL:
				return "FATAL";
			default:
				return "UNKNOWN";
		}
	}
	
	@SJC.Inline
	public String timeHMS()
	{
		return _timeHMS;
	}
	
	@SJC.Inline
	public void setCategory(String category)
	{
		this._category = category;
	}
	
	@SJC.Inline
	public void setMessage(String message)
	{
		this._message = message;
	}
	
	@SJC.Inline
	public void setPriority(byte priority)
	{
		this._priority = priority;
	}
	
	@SJC.Inline
	public void setTimeHMS(String time_HMS)
	{
		this._timeHMS = time_HMS;
	}
	
}
