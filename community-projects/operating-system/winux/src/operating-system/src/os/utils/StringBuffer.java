package os.utils;

public class StringBuffer {

    private char[] s;
    private int ptr;

    public StringBuffer() {
        this(16);
    }

    public StringBuffer(char[] arr) {
        this(arr.length);
        for(char c : arr) {
            s[ptr++] = c;
        }
    }

    public StringBuffer(String str) {
        this(str.length());
        for (int i = 0; i < str.length(); i++) {
            s[ptr++] = str.charAt(i);
        }
    }

    public StringBuffer(int bufferSize) {
        this.s = new char[bufferSize];
        this.ptr = 0;
    }



    private void ensureSize(int newSize) {
        char[] tmp;
        int bufferSize, i;
        if(newSize <= s.length)
            return;

        bufferSize = 1;
        while(bufferSize < newSize)
            bufferSize <<= 1;

        tmp = new char[bufferSize];
        for(i = 0; i < ptr; i++) {
            tmp[i] = s[i];
        }

        s = tmp;
    }

    public StringBuffer append(int x) {
        int reverse, digits;
        int neg = 0;

        if(x < 0) {
            neg = 1;
            x = -x;
        }

        reverse = 0;
        digits = 0;
        do {
            reverse = (reverse * 10) + (x % 10);
            x /= 10;
            digits++;
        } while(x > 0);

        ensureSize(ptr + digits + neg);

        if(neg == 1) {
            s[ptr++] = '-';
        }

        while(digits-- > 0) {
            s[ptr++] = (char) (reverse % 10 + '0');
            reverse /= 10;
        }

        return this;
    }

    public StringBuffer append(String str) {
        int i;
        ensureSize(ptr + str.length());

        for(i = 0; i < str.length(); i++) {
            s[ptr++] = str.charAt(i);
        }

        return this;
    }

    public StringBuffer append(StringBuffer other) {
        int i;
        ensureSize(ptr + other.size());

        for(i = 0; i < other.size(); i++) {
            s[ptr++] = other.s[i];
        }

        return this;
    }

    public StringBuffer append(char c) {
        ensureSize(ptr + 1);
        s[ptr++] = c;
        return this;
    }

    public char[] getCharArr() {
        return s;
    }

    public String toString() {
        return new String(s, 0, ptr);
    }

    public int size() {
        return ptr;
    }

    public void delete(int cX) {
        int i;
        if(cX < 0 || cX >= s.length)
            return;

        for(i = cX + 1; i < ptr; i++) {
            s[i-1] = s[i];
        }

        ptr--;
    }

    public void deleteFrom(int start) {
        if(0 <= start && start < ptr)
            ptr = start;
    }

    public char charAt(int i) {
        return s[i];
    }
}
