package java.lang;

import kernel.Kernel;
import util.NoAllocConv;

public class Long {
    public static byte[] BUFFER = MAGIC.toByteArray("00000000000000000000000000000000000000000000000000000000", true);

    public static String toString(long i, int base) {
        if (i == 0) {
            return "0";
        }

        for (int j = 0; j < BUFFER.length; j++) {
            BUFFER[j] = (byte) 0;
        }

        int digitCount = NoAllocConv.ItoA(BUFFER, BUFFER.length, i, base);

        int newLength = digitCount;
        if (i < 0) {
            newLength += 1;
        }

        int offest = BUFFER.length - digitCount;
        byte[] chars = new byte[newLength];
        if (i < 0) {
            chars[0] = (byte) '-';
            for (int j = 0; j < digitCount; j++) {
                chars[j + 1] = BUFFER[j + offest];
            }
        } else {
            for (int j = 0; j < digitCount; j++) {
                chars[j] = BUFFER[j + offest];
            }
        }

        return new String(chars);
    }

    @SJC.Inline
    public int ToIntOrPanic(long l) {
        if (l > Integer.MAX || l < Integer.MIN) {
            Kernel.panic("Long to int conversion failed");
        }
        return (int) l;
    }

    public static long parseLong(String s) {
        Kernel.panic("Long.parseLong not implemented");
        return -1;
    }

    public static long parseLong(String s, int a) {
        Kernel.panic("Long.parseLong not implemented");
        return -1;
    }
}
