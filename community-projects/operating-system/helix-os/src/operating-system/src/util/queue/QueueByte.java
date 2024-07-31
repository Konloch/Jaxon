package util.queue;

public class QueueByte {
    private final int _size;
    private final byte[] _buffer;
    private int _headIdx;
    private int _tailIdx;
    private int _count;

    public QueueByte(int size) {
        this._size = size;
        _buffer = new byte[size];
        _headIdx = 0;
        _tailIdx = 0;
        _count = 0;
    }

    public void Put(byte c) {
        _buffer[_headIdx] = c;
        IncHead();
    }

    public byte Get() {
        byte c = _buffer[_tailIdx];
        IncTail();
        return c;
    }

    @SJC.Inline
    public int Count() {
        return _count;
    }

    @SJC.Inline
    public boolean IsEmpty() {
        return _count == 0;
    }

    @SJC.Inline
    public boolean ContainsNewElements() {
        return _headIdx != _tailIdx;
    }

    @SJC.Inline
    private void IncHead() {
        _headIdx = (_headIdx + 1) % _size;
        _count++;
    }

    @SJC.Inline
    private void IncTail() {
        _tailIdx = (_tailIdx + 1) % _size;
        _count--;
    }
}
