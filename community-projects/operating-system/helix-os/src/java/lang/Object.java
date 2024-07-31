package java.lang;

import kernel.Kernel;
import rte.SClassDesc;

/*
 * ScalarSize = 9 (2 ints + 1 bool)
 * RelocEntries = 2 (type and next)
 */
public class Object
{
	public final SClassDesc _r_type = null;
	public final Object _r_next = null;
	public final int _r_relocEntries = 0;
	public final int _r_scalarSize = 0;
	private int _flags = 0;
	
	public static final int FLAG_USED = 1;
	public static final int FLAG_OTHER = 1 << 2;
	
	@SJC.Inline
	public void MarkUnused()
	{
		_flags &= ~FLAG_USED;
	}
	
	@SJC.Inline
	public void MarkUsed()
	{
		_flags |= FLAG_USED;
	}
	
	@SJC.Inline
	public boolean IsMarked()
	{
		return (_flags & FLAG_USED) == 1;
	}
	
	@SJC.Inline
	public int RelocEntriesCount()
	{
		return _r_relocEntries;
	}
	
	@SJC.Inline
	public Object ReadRelocEntry(int relocIndex)
	{
		if (relocIndex > RelocEntriesCount() || relocIndex < 0)
		{
			Kernel.panic("Requested Index out of range".append(Integer.toString(relocIndex).append(" ").append(Integer.toString(_r_relocEntries))));
		}
		int baseAddr = MAGIC.cast2Ref(this);
		baseAddr -= MAGIC.ptrSize;
		int addr = MAGIC.rMem32(baseAddr - relocIndex * MAGIC.ptrSize);
		if (addr == 0)
		{
			return null;
		}
		
		return MAGIC.cast2Obj(addr);
	}
	
	@SJC.Inline
	public int AddressTop()
	{
		return MAGIC.cast2Ref(this) + _r_scalarSize;
	}
	
	@SJC.Inline
	public int AddressBottom()
	{
		return MAGIC.cast2Ref(this) - _r_relocEntries * MAGIC.ptrSize;
	}
	
	@SJC.Inline
	public boolean ContainsAddress(int addr)
	{
		return addr >= AddressBottom() && addr < AddressTop();
	}
}
