package os.keyboard;

public class KeyBuffer {
    public static final int DEFAULT_SIZE = 128;

    private final KeyEvent[] buffer;
    private int start;
    private int end;

    public KeyBuffer() {
        this.buffer = new KeyEvent[DEFAULT_SIZE];
        this.start = 0;
        this.end = 0;
    }

    public KeyBuffer(int size) {
        this.buffer = new KeyEvent[size];
        this.start = 0;
        this.end = 0;
    }

    @SJC.Inline
    public boolean isEmpty() {
        return start == end;
    }

    /**
     * Calculates (!) size of buffer.
     */
    public int getSize() {
        int size = end - start;
        if (size < 0)
            size += buffer.length;
        return size;
    }

    /**
     * Add KeyEvent to buffer. If buffer is full (size == buffer.length), the last entry is overwritten.
     */
    public void add(KeyEvent keyEvent) {
        buffer[end++] = keyEvent;

        // didn't do modulo arithmetic because
        // CPU might be more efficient with branch predictions
        if(end == buffer.length)
            end = 0;
        if(start == end) {
            start++;
            if(start == buffer.length)
                start = 0;
        }
    }

    /**
     * Get FIFO key event (pop might be misleading, because it's not a stack)
     *
     * If buffer is empty, it returns null.
     */
    public KeyEvent pop() {
        if (isEmpty())
            return null;

        KeyEvent value = buffer[start];
        buffer[start] = null;
        if(++start == buffer.length)
            start = 0;
        return value;
    }

    /**
     * Get FIFO key event without advancing buffer pointer and thus removing it from the buffer.
     *
     * If buffer is empty, it returns null.
     */
    public KeyEvent peek() {
        if(isEmpty())
            return null;
        return buffer[start];
    }
}
