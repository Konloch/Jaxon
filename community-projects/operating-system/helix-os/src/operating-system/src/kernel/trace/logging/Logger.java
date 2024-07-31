package kernel.trace.logging;

import kernel.hardware.RTC;
import java.lang.StringBuilder;
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
		log.setCategory(category);
		log.setMessage(message);
		log.setPriority(priority);
		
		if (_logTime)
			log.setTimeHMS(GetTimeHMS());
		
		logSerial(log);
		logBuffer.Put(log);
		logTicks++;
	}
	
	public static void logSerial(LogEntry entry)
	{
		logSerial(entry.timeHMS());
		logSerial(" [");
		logSerial(entry.oriorityString());
		logSerial("] ");
		logSerial(entry.category());
		logSerial(": ");
		logSerial(entry.message());
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
	public static LogEntry GetChronologicalLog(int i)
	{
		return logBuffer.PeekBack(i);
	}
	
	@SJC.Inline
	public static int LogTicks()
	{
		return logTicks;
	}
	
	private static String GetTimeHMS()
	{
		int hours = RTC.ReadHour();
		int minutes = RTC.ReadMinute();
		int seconds = RTC.ReadSecond();
		boolean hoursIsTwoDigits = hours >= 10;
		boolean minutesIsTwoDigits = minutes >= 10;
		boolean secondsIsTwoDigits = seconds >= 10;
		StringBuilder sb = new StringBuilder(10);
		
		if (!hoursIsTwoDigits)
			sb.append('0');
		
		sb.append(hours);
		sb.append(':');
		
		if (!minutesIsTwoDigits)
			sb.append('0');
		
		sb.append(minutes);
		sb.append(':');
		
		if (!secondsIsTwoDigits)
			sb.append('0');
		
		sb.append(seconds);
		return sb.toString();
	}
}
