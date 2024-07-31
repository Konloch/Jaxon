package kernel.trace.logging;

import kernel.hardware.RTC;
import util.StrBuilder;
import util.queue.QueueLogEntry;

public class Logger {
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

    public static void Initialize(byte logLevel, int capactiy, boolean logTime) {
        logBuffer = new QueueLogEntry(capactiy);
        for (int i = 0; i < capactiy; i++) {
            logBuffer.Put(new LogEntry("", "", NONE, ""));
        }
        initialized = true;
        minimumLogLevel = logLevel;
        _logTime = logTime;
    }

    @SJC.Inline
    public static void Trace(String category, String message) {
        log(category, message, TRACE);
    }

    @SJC.Inline
    public static void Info(String category, String message) {
        log(category, message, INFO);
    }

    @SJC.Inline
    public static void Warning(String category, String message) {
        log(category, message, WARNING);
    }

    @SJC.Inline
    public static void Error(String category, String message) {
        log(category, message, ERROR);
    }

    @SJC.Inline
    public static void Fatal(String category, String message) {
        log(category, message, FATAL);
    }

    public static void log(String category, String message, byte priority) {
        if (priority < minimumLogLevel || !initialized)
            return;

        LogEntry log = logBuffer.Get();
        log.SetCategory(category);
        log.SetMessage(message);
        log.SetPriority(priority);
        if (_logTime)
            log.SetTimeHMS(GetTimeHMS());
        LogSerial(log);
        logBuffer.Put(log);
        logTicks++;
    }

    public static void LogSerial(LogEntry entry) {
        LogSerial(entry.TimeHMS());
        LogSerial(" [");
        LogSerial(entry.PriorityString());
        LogSerial("] ");
        LogSerial(entry.Category());
        LogSerial(": ");
        LogSerial(entry.Message());
        LogSerial("\n");
    }

    public static void LogSerial(String str) {
        if (str == null) {
            LogSerial("null");
            return;
        }

        if (str.length() == 0) {
            return;
        }

        for (int i = 0; i < str.length(); i++) {
            byte c = (byte) str.get(i);
            MAGIC.wIOs8(0xe9, c);
        }
    }

    @SJC.Inline
    public static LogEntry GetChronologicalLog(int i) {
        return logBuffer.PeekBack(i);
    }

    @SJC.Inline
    public static int LogTicks() {
        return logTicks;
    }

    private static String GetTimeHMS() {
        int hours = RTC.ReadHour();
        int minutes = RTC.ReadMinute();
        int seconds = RTC.ReadSecond();
        boolean hoursIsTwoDigits = hours >= 10;
        boolean minutesIsTwoDigits = minutes >= 10;
        boolean secondsIsTwoDigits = seconds >= 10;
        StrBuilder sb = new StrBuilder(10);
        if (!hoursIsTwoDigits) {
            sb.Append('0');
        }
        sb.Append(hours);
        sb.Append(':');
        if (!minutesIsTwoDigits) {
            sb.Append('0');
        }
        sb.Append(minutes);
        sb.Append(':');
        if (!secondsIsTwoDigits) {
            sb.Append('0');
        }
        sb.Append(seconds);
        return sb.toString();
    }
}
