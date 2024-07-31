package kernel.hardware;

/**
 * Real-Time Clock
 * 
 * Binary Coded Decimal (BCD) is a way to store binary numbers in which each
 * digit of a decimal number is stored in a separate byte.
 * 0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09,
 * 0x10, 0x11, 0x12, 0x13, 0x14, 0x15, 0x16, 0x17, 0x18, 0x19,
 * and so on until it gets to 0x59, then back to 0x00.
 */
public class RTC {
    private static final int RTC_DATA = 0x71;
    private static final int RTC_BASE = 0x70;

    /*
     * Read current hours in integer format.
     */
    @SJC.Inline
    public static int ReadHour() {
        int hours = ReadHourBCD();
        hours = HourBcdToBinary(hours);
        return hours;
    }

    /*
     * Read current minutes in integer format.
     */
    @SJC.Inline
    public static int ReadMinute() {
        int minutes = ReadMinuteBCD();
        minutes = BcdToBinary(minutes);
        return minutes;
    }

    /*
     * Read current seconds in integer format.
     */
    @SJC.Inline
    public static int ReadSecond() {
        int seconds = ReadSecondBCD();
        seconds = BcdToBinary(seconds);
        return seconds;
    }

    /*
     * Read current day in integer format.
     */
    @SJC.Inline
    public static int ReadDayOfMonth() {
        int day = ReadDay();
        day = BcdToBinary(day);
        return day;
    }

    /*
     * Read current month in integer format.
     */
    @SJC.Inline
    public static int ReadMonthOfYear() {
        int month = ReadMonth();
        month = BcdToBinary(month);
        return month;
    }

    /*
     * Read current year in integer format.
     * The year is the last two digits of the year.
     */
    @SJC.Inline
    public static int ReadYearOfCentury() {
        int year = ReadYear();
        year = BcdToBinary(year);
        return year;
    }

    @SJC.Inline
    public static int ReadSecondBCD() {
        return ReadRTC((byte) 0);
    }

    @SJC.Inline
    public static int ReadMinuteBCD() {
        return ReadRTC((byte) 2);
    }

    @SJC.Inline
    public static int ReadHourBCD() {
        return ReadRTC((byte) 4);
    }

    @SJC.Inline
    public static int ReadDay() {
        return ReadRTC((byte) 7);
    }

    @SJC.Inline
    public static int ReadMonth() {
        return ReadRTC((byte) 8);
    }

    @SJC.Inline
    public static int ReadYear() {
        return ReadRTC((byte) 9);
    }

    @SJC.Inline
    public static int ReadRTC(byte field) {
        MAGIC.wIOs8(RTC_BASE, field);
        int value = MAGIC.rIOs8(RTC_DATA);
        return value;
    }

    @SJC.Inline
    private static int BcdToBinary(int value) {
        value = (value & 0xF) + ((value >> 4) * 10);
        return value;
    }

    @SJC.Inline
    private static int HourBcdToBinary(int hours) {
        hours = (hours & 0xF) + ((hours & 0x70) >> 4) * 10 + (hours & 0x80);
        return hours;
    }

}
