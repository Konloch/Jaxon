package os.utils.stringTemplate;

import os.utils.OutStream;
import os.utils.Range;

public class Placeholder {
    public static final char LEFT = 'l';
    public static final char CENTER = 'c';
    public static final char RIGHT = 'r';
    public static final char IS_HEX = 'x';
    public static final char LENGTH_MAX = '<';

    public final int length;
    public final int lengthMax;
    public final char orientation;
    // if isHex = true, 0x is printed before number
    public final boolean isHex;

    /**
     * Count number of placeholders (noted with '{}', escaped with backslash) in String.
     *
     * @param t text block with placeholders
     * @return number of placeholders in text
     */
    public static int countPlaceholders(String t) {
        int amount = 0;
        int i = 0;
        Range r;

        while((r = nextPlaceholder(t, i)) != null) {
            amount++;
            i = r.end + 1;
        }

        return amount;
    }

    /**
     * Get next placeholder start and end index.
     *
     * @param t template string
     * @param start start index
     * @return index of '{' and '}' token, -1 if not found
     */
    public static Range nextPlaceholder(String t, int start) {
        int end;
        for(; start < t.length(); start++) {
            if(t.charAt(start) == '\\') {
                start++;

            } else if(t.charAt(start) == '{') {
                end = t.indexOf('}', start+1);
                if(end >= 0)
                    return new Range(start, end);
                return null;
            }
        }
        return null;
    }

    /**
     * Parse placeholder. Look for next placeholder from start index.
     *
     * @param t text block
     * @param start '{' char index
     * @return placeholder object
     */
    public static Placeholder parseNextPlaceholder(String t, int start) {
        Range range = nextPlaceholder(t, start);
        if(range == null)
            return null;

        return parseNextPlaceholder(t, range);
    }

    /**
     * Parse placeholder in given range. No particular care has been given to grammar correctness.
     *
     * @param t text block
     * @param range '{' and '}' index
     * @return placeholder object
     */
    public static Placeholder parseNextPlaceholder(String t, Range range) {
        int i;
        Placeholder place = new Placeholder();
        boolean isMax = false;

        for(i = range.start+1; i < range.end; i++) {
            switch(t.charAt(i)) {
                case '0':
                case '1':
                case '2':
                case '3':
                case '4':
                case '5':
                case '6':
                case '7':
                case '8':
                case '9':
                    if(isMax)
                        MAGIC.assign(place.lengthMax, (place.lengthMax * 10) + (t.charAt(i) - '0'));
                    else
                        MAGIC.assign(place.length, (place.length * 10) + (t.charAt(i) - '0'));
                    break;

                case LEFT:
                case CENTER:
                case RIGHT:
                    MAGIC.assign(place.orientation, t.charAt(i));
                    break;

                case IS_HEX:
                    MAGIC.assign(place.isHex, true);
                    break;

                case LENGTH_MAX:
                    isMax = true;
                    break;

            }
        }

        return place;
    }

    public Placeholder() {
        this.length = 0;
        this.lengthMax = -1;
        this.orientation = LEFT;
        this.isHex = false;
    }

    public Placeholder(int length, char orientation, boolean isHex) {
        this.length = length;
        this.lengthMax = -1;
        this.orientation = orientation;
        this.isHex = isHex;
    }

    /**
     * Add whitespace to the left of content.
     *
     * @param out Output object to print whitespaces to
     * @param contentLength Length of placeholder content to determine amount of whitespace needed
     */
    public void padLeft(OutStream out, int contentLength) {
        int spaces = 0;

        if(orientation == RIGHT)
            spaces = length - contentLength;
        else if(orientation == CENTER)
            spaces = (length - contentLength) / 2;

        while(spaces --> 0)
            out.print(' ');
    }

    /**
     * Add whitespace to the right of content.
     *
     * If placeholder is centered and the amount of whitespace to the left and right uneven (aka
     * not cleanly divisible by 2), add one more extra whitespace to the right.
     *
     * @param out Output object to print whitespaces to
     * @param contentLength Length of placeholder content to determine amount of whitespace needed
     */
    public void padRight(OutStream out, int contentLength) {
        int spaces = 0;

        if(orientation == LEFT)
            spaces = length - contentLength;
        else if(orientation == CENTER)
            spaces = (length - contentLength) / 2 + (length - contentLength) % 2;

        while(spaces --> 0)
            out.print(' ');
    }
}
