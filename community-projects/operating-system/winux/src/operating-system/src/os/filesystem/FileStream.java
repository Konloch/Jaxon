package os.filesystem;

import os.screen.Cursor;

public class FileStream {

    private File f;
    private int ptr;

    public FileStream(File f) {
        this.f = f;
        this.ptr = 0;
    }

    public char next() {
        if(!hasNext())
            return 0;

        return f.content[ptr++];
    }

    @SJC.Inline
    public boolean hasNext() {
        return ptr < f.size();
    }

    int l = 0;
    public char[] getLine() {
        char[] line;
        int i;
        int start = ptr;

        if(ptr >= f.size())
            return new char[0];

        while(ptr < f.size() && f.content[ptr] != '\n') {
            ptr++;
        }

        line = new char[ptr - start];
        for(i = 0; i < line.length; i++) {
            line[i] = f.content[start+i];
        }

        // skip newline char
        ptr++;
        return line;
    }
}
