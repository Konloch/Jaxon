package java.lang;

import os.screen.Cursor;

public class String {
    private char[] value;
    private int count;

    public String(char[] arr) {
        this.value = arr;
        this.count = arr.length;
    }

    public String(char[] arr, int start, int end) {
        int i;
        this.count = end - start;
        this.value = new char[this.count];

        for(i = 0; i < this.count; i++) {
            this.value[i] = arr[i + start];
        }
    }

    /**
     * Substring from beginIndex to end.
     * <p>
     * No checks if {@code 0 <= start < length()}.
     *
     * @param beginIndex inclusive start index
     * @return new String
     */
    @SJC.Inline
    public String substring(int beginIndex) {
        return substring(beginIndex, this.count);
    }

    /**
     * Substring from start (inclusive) to end (exclusive).
     * <p>
     * If {@code end > start} and {@code 0 <= start < length()} and {@code 0 < end <= length()},
     * it returns empty String.
     *
     * @param start inclusive start index
     * @param end   exclusive start index
     * @return new String
     */
    public String substring(int start, int end) {
        char[] result;

        if(start >= end || start < 0 || end < 0 || start >= count || end > count)
            return "";

        result = new char[end - start];
        for (int i = start; i < end; i++)
            result[i - start] = value[i];

        return new String(result);
    }

    /**
     * Get index of char from start point. Returns -1 if char not found.
     *
     * @param c     char to look for
     * @param start starting index ({@code start >= 0})
     * @return character index or -1
     */
    public int indexOf(char c, int start) {
        for (int i = start; i < count; i++) {
            if (value[i] == c)
                return i;
        }
        return -1;
    }

    /**
     * Get index of character ch. -1 if not found.
     *
     * @param ch Character to search
     * @return index of ch, -1 if not found
     */
    public int indexOf(char ch) {
        return indexOf(ch, 0);
    }

    /**
     * Get length of String
     *
     * @return string length
     */
    @SJC.Inline
    public int length() {
        return count;
    }

    /**
     * Get character index i.
     * Throws ArrayIndexOutOfBoundsException if {@code i < 0} or {@code i >= {@link String#length()}}.
     *
     * @param i Index
     * @return Character at specified index
     */
    @SJC.Inline
    public char charAt(int i) {
        return value[i];
    }

    public boolean startsWith(String other) {
        int i;
        if (other.length() > this.count)
            return false;

        for (i = 0; i < other.length(); i++) {
            if (this.charAt(i) != other.charAt(i))
                return false;
        }
        return true;
    }

    /**
     * Split String at specified chars. Multiple consecutive chars are ignored.
     *
     * {@code "you   only live    once".split(' ')} => {@code {"you", "only", "live", "once}}
     *
     * @param c Character to split string at
     * @return Split string array
     */
    public String[] split(char c) {
        String[] result;
        int from, to;
        int argIndex, argTotal;

        int startAt = 0;
        while(startAt < count && value[startAt] == c)
            startAt++;

        // count number of arguments needed
        from = startAt;
        argTotal = 1;
        while((from = indexOf(c, from)) >= 0) {
            argTotal++;
            // skip whitespace
            while(from < count && value[from] == c) {
                from++;
            }
        }

        result = new String[argTotal];

        // initialize array indexes
        from = startAt;
        for(argIndex = 0; argIndex < argTotal; argIndex++) {
            to = indexOf(c, from);
            if(to < 0)
                to = count;

            result[argIndex] = substring(from, to);
            // skip whitespace
            from = to;
            while(from < count && value[from] == c) {
                from++;
            }
        }
        return result;
    }

    /**
     * Check if one string's characters equals the other's, including its length.
     *
     * @param other Other string to compare against
     * @return true if length is the same as well the characters inside the string
     */
    public boolean equals(String other) {
        if(count != other.count)
            return false;
        for (int i = 0; i < count; i++) {
            if(value[i] != other.value[i])
                return false;
        }
        return true;
    }

    /**
     * Get character array of string. Note that changing values of the array changes the values of the string!
     *
     * @return char array
     */
    public char[] toCharArray() {
        return value;
    }
}