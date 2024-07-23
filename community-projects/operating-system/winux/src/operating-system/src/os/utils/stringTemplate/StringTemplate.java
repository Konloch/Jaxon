package os.utils.stringTemplate;

import os.screen.Terminal;
import os.utils.NumberHelper;
import os.utils.OutStream;
import os.utils.Range;

/**
 * "{}" are placeholders. "{" can be escaped via "\\{"
 *
 * Specify placeholder length with number inside curly braces (no space). "{3}" -> placeholder at least of length 3
 * (padding done with spaces). Values are always left aligned.
 *
 * To circumvent missing variadic functions, use {@link StringTemplate#start(OutStream)} to start outputting and
 * use p and hex to fill in the missing gaps.
 *
 * Note: lengthMax only works for Strings (eg. "{3<5}" means printed String is at least 3 chars and at most 5 chars wide).
 *
 * Eg.:
 *
 * StringTemplate s = new StringTemplate("My name is {} and I am {} years old and I was born on {2}.{2}.{4}");
 *
 * s.start(myTerminal);
 *
 * s.p("Matt").hex(21).p(1).p(4).(1999);
 */
public class StringTemplate {

    private final String templateString;
    private final Placeholder[] placeholders;
    private final Range[] parts;

    private int outIndex = -1;
    private OutStream out = null;

    public StringTemplate(String template) {
        int i, current;
        Range next;
        this.templateString = template;
        this.placeholders = new Placeholder[Placeholder.countPlaceholders(template)];
        this.parts = new Range[this.placeholders.length + 1];

        current = 0;
        for(i = 0; i < this.placeholders.length; i++) {
            // from countPlaceholders this implicitly shouldn't return null
            next = Placeholder.nextPlaceholder(template, current);

            this.parts[i] = new Range(current, next.start);
            this.placeholders[i] = Placeholder.parseNextPlaceholder(template, next.start);

            current = next.end + 1;
        }

        this.parts[i] = new Range(current, template.length());
    }

    public int getAmountOfPlaceholders() {
        return placeholders.length;
    }

    public StringTemplate start(OutStream out) {
        outIndex = 0;
        this.out = out;
        out.print(templateString, parts[outIndex].start, parts[outIndex].end);
        return this;
    }

    private void nextPart() {
        outIndex++;
        out.print(templateString, parts[outIndex].start, parts[outIndex].end);
        if(outIndex >= placeholders.length)
            outIndex = 0;
    }


    public StringTemplate p(String s) {
        Placeholder p = placeholders[outIndex];
        p.padLeft(out, s.length());

        if(p.lengthMax >= 0) {
            out.print(s, 0, p.lengthMax);
        } else {
            out.print(s);
        }

        p.padRight(out, s.length());
        nextPart();
        return this;
    }

    public StringTemplate p(byte n) {
        Placeholder p = placeholders[outIndex];
        int width = p.isHex ? 4 : NumberHelper.getIntWidth(n);
        p.padLeft(out, width);

        if(p.isHex) {
            out.print("0x");
            out.printHex(n);
        } else {
            out.print((int) n);
        }

        p.padRight(out, width);
        nextPart();
        return this;
    }

    public StringTemplate p(short n) {
        Placeholder p = placeholders[outIndex];
        int width = p.isHex ? 6 : NumberHelper.getIntWidth(n);
        p.padLeft(out, width);

        if(p.isHex) {
            out.print("0x");
            out.printHex(n);
        } else {
            out.print((int) n);
        }

        p.padRight(out, width);
        nextPart();
        return this;
    }

    public StringTemplate p(int n) {
        Placeholder p = placeholders[outIndex];
        int width = p.isHex ? 10 : NumberHelper.getIntWidth(n);
        p.padLeft(out, width);

        if(p.isHex) {
            out.print("0x");
            out.printHex(n);
        } else {
            out.print(n);
        }

        p.padRight(out, width);
        nextPart();
        return this;
    }

    public StringTemplate p(long n) {
        Placeholder p = placeholders[outIndex];
        int width = p.isHex ? 18 : NumberHelper.getLongWidth(n);
        p.padLeft(out, width);

        if(p.isHex) {
            out.print("0x");
            out.printHex(n);
        } else {
            out.print(n);
        }

        p.padRight(out, width);
        nextPart();
        return this;
    }

    public void p(boolean b) {
        if(b)
            p("true");
        else
            p("false");
    }
}
