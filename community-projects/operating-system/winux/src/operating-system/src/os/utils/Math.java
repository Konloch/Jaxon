package os.utils;

public class Math {

    public static int max(int a, int b) {
        return (a>=b) ? a : b;
    }

    public static int min(int a, int b) {
        return (a<=b) ? a : b;
    }

    public static int parseInt(String s, int errVal) {
        int result, ptr, c;
        boolean negative;

        if(s == null || s.length() == 0)
            return errVal;

        result = 0;
        if(s.charAt(0) == '-') {
            ptr = 1;
            negative = true;
        } else {
            ptr = 0;
            negative = false;
        }

        while(ptr < s.length()) {
            c = s.charAt(ptr) - '0';
            if(c >= 0 && c <= 9)
                result = (result * 10) + c;
            else
                return errVal;

            ptr++;
        }


        if(negative)
            return -result;
        return result;
    }

}
