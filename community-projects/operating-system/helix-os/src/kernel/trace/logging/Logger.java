package kernel.trace.logging;

import kernel.hardware.RTC;
import java.util.StrBuilder;
import java.util.queue.QueueLogEntry;

public class Logger
{
	private static QueueLogEntry logBuffer;
	private static byte minimumLogLevel = 0;
	private static boolean initialized = false;
	private static int logTicks = 0;
	private static boolean _logTime;
	
	public static final byte NONE = 0;
	public static final byte TRACE = 1;
	public static final byte INFO = 2;
	public static final byte WARNING = 3;
	public static final byte ERROR = 4;
	public static final byte FATAL = 5;
	
	public static void initialize(byte logLevel, int capactiy, boolean logTime)
	{
		logBuffer = new QueueLogEntry(capactiy);
		for (int i = 0; i < capactiy; i++)
			logBuffer.Put(new LogEntry("", "", NONE, ""));
		
		initialized = true;
		minimumLogLevel = logLevel;
		_logTime = logTime;
	}
	
	@SJC.Inline
	public static void trace(String category, String message)
	{
		log(category, message, TRACE);
	}
	
	@SJC.Inline
	public static void info(String category, String message)
	{
		log(category, message, INFO);
	}
	
	@SJC.Inline
	public static void warning(String category, String message)
	{
		log(category, message, WARNING);
	}
	
	@SJC.Inline
	public static void error(String category, String message)
	{
		log(category, message, ERROR);
	}
	
	@SJC.Inline
	public static void fatal(String category, String message)
	{
		log(category, message, FATAL);
	}
	
	public static void log(String category, String message, byte priority)
	{
		if (priority < minimumLogLevel || !initialized)
			return;
		
		LogEntry log = logBuffer.Get();
		log.SetCategory(category);
		log.SetMessage(message);
		log.SetPriority(priority);
		if (_logTime)
			log.SetTimeHMS(getTimeHMS());
		logSerial(log);
		logBuffer.Put(log);
		logTicks++;
	}
	
	public static void logSerial(LogEntry entry)
	{
		logSerial(entry.TimeHMS());
		logSerial(" [");
		logSerial(entry.PriorityString());
		logSerial("] ");
		logSerial(entry.Category());
		logSerial(": ");
		logSerial(entry.Message());
		logSerial("\n");
	}
	
	public static void logSerial(String str)
	{
		if (str == null)
		{
			logSerial("null");
			return;
		}
		
		if (str.length() == 0)
			return;
		
		for (int i = 0; i < str.length(); i++)
		{
			byte c = (byte) str.get(i);
			MAGIC.wIOs8(0xe9, c);
		}
	}
	
	@SJC.Inline
	public static LogEntry getChronologicalLog(int i)
	{
		return logBuffer.PeekBack(i);
	}
	
	@SJC.Inline
	public static int logTicks()
	{
		return logTicks;
	}
	
	private static String getTimeHMS()
	{
		int hours = RTC.ReadHour();
		int minutes = RTC.ReadMinute();
		int seconds = RTC.ReadSecond();
		boolean hoursIsTwoDigits = hours >= 10;
		boolean minutesIsTwoDigits = minutes >= 10;
		boolean secondsIsTwoDigits = seconds >= 10;
		StrBuilder sb = new StrBuilder(10);
		
		if (!hoursIsTwoDigits)
			sb.Append('0');
		
		sb.Append(hours);
		sb.Append(':');
		
		if (!minutesIsTwoDigits)
			sb.Append('0');
		
		sb.Append(minutes);
		sb.Append(':');
		
		if (!secondsIsTwoDigits)
			sb.Append('0');
		
		sb.Append(seconds);
		return sb.toString();
	}
}
