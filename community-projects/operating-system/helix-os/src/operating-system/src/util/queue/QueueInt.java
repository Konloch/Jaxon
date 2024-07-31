package util.queue;

public class QueueInt {
    private final int _size;
    private final int[] _buffer;
    private int _headIdx;
    private int _tailIdx;
    private int _count;

    public QueueInt(int size) {
        this._size = size;
        _buffer = new int[size];
        _headIdx = 0;
        _tailIdx = 0;
        _count = 0;
    }

    public void Put(int c) {
        _buffer[_headIdx] = c;
        IncHead();
    }

    public int Get() {
        int c = _buffer[_tailIdx];
        IncTail();
        return c;
    }

    public int Average() {
        int sum = 0;
        for (int i = 0; i < _count; i++) {
            sum += _buffer[i];
        }
        return sum / _count;
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
