package os.utils;

public class NumberHelper {

    public static int getIntWidth(int n) {
        int w = 0;
        if(n < 0) {w++; n = -n;}
        do {n /= 10; w++;
        } while(n > 0);
        return w;
    }

    public static int getLongWidth(long n) {
        int w = 0;
        if(n < 0) {w++; n = -n;}
        do {n /= 10; w++;
        } while(n > 0);
        return w;
    }

    private static class DigitGenerator {
        public final int number;
        private int reversed;
        private int digitsLeft;
        private boolean isNegative;

        public DigitGenerator(int x) {
            this.number = x;
            this.isNegative = x < 0;

            if(isNegative)
                x = -x;

            this.reversed = 0;
            this.digitsLeft = 0;

            do {
                this.reversed = (this.reversed * 10) + (x % 10);
                x /= 10;
                this.digitsLeft++;
            } while(x > 0);
        }

        public int charsLeft() {
            return digitsLeft + (isNegative ? 1 : 0);
        }

        public char next() {
            char c;


            if(isNegative) {
                isNegative = false;
                return '-';
            }

            c = (char) (reversed % 10 + '0');
            reversed /= 10;
            digitsLeft--;
            return c;
        }
    }
}
