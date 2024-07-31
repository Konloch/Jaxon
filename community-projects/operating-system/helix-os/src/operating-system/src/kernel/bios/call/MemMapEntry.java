package kernel.bios.call;

public class MemMapEntry {
    public final long Base;
    public final long Length;
    public final int Type;

    public MemMapEntry(long base, long length, int type) {
        this.Base = base;
        this.Length = length;
        this.Type = type;
    }

    public boolean IsFree() {
        return Type == 1;
    }
}