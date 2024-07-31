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
	public String Category()
	{
		return _category;
	}
	
	@SJC.Inline
	public String Message()
	{
		return _message;
	}
	
	@SJC.Inline
	public byte Priority()
	{
		return _priority;
	}
	
	@SJC.Inline
	public String PriorityString()
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
	public String TimeHMS()
	{
		return _timeHMS;
	}
	
	@SJC.Inline
	public void SetCategory(String category)
	{
		this._category = category;
	}
	
	@SJC.Inline
	public void SetMessage(String message)
	{
		this._message = message;
	}
	
	@SJC.Inline
	public void SetPriority(byte priority)
	{
		this._priority = priority;
	}
	
	@SJC.Inline
	public void SetTimeHMS(String time_HMS)
	{
		this._timeHMS = time_HMS;
	}
	
}
