package kernel.bios.call;

import kernel.MemoryLayout;
import kernel.bios.BIOS;

public class MemMap extends BIOS
{
	
	private int memMapContinuationIndex = -1;
	private boolean _first = true;
	
	public MemMap()
	{
		super();
	}
	
	public boolean hasNext()
	{
		return memMapContinuationIndex != 0;
	}
	
	public MemMapEntry next()
	{
		if (_first)
		{
			_first = false;
			MemMapEntry e = exec(0);
			memMapContinuationIndex = getMemMapContinuationIndex();
			return e;
		}
		
		if (memMapContinuationIndex == 0)
			return null;
		
		MemMapEntry e = exec(memMapContinuationIndex);
		memMapContinuationIndex = getMemMapContinuationIndex();
		return e;
	}
	
	public MemMapEntry nextFree()
	{
		MemMapEntry e;
		
		while (hasNext())
		{
			e = next();
			if (e.isFree())
				return e;
		}
		
		return null;
	}
	
	public static MemMapEntry exec(int idx)
	{
		execMemMap(idx);
		return readMemMap();
	}
	
	public static void execMemMap(int idx)
	{
		Registers.EAX = 0x0000E820;
		Registers.EDX = 0x534D4150;
		Registers.EBX = idx;
		Registers.ES = (short) (MemoryLayout.BIOS_BUFFER_MEMMAP_START >>> 4);
		Registers.EDI = MemoryLayout.BIOS_BUFFER_MEMMAP_START & 0xF;
		Registers.ECX = MemoryLayout.BIOS_BUFFER_MEMMAP_SIZE;
		rint(0x15);
	}
	
	@SJC.Inline
	public static int getMemMapContinuationIndex()
	{
		return Registers.EBX;
	}
	
	@SJC.Inline
	public static long getMemMapBase()
	{
		return MAGIC.rMem64(MemoryLayout.BIOS_BUFFER_MEMMAP_START);
	}
	
	@SJC.Inline
	public static long getMemMapLength()
	{
		return MAGIC.rMem64(MemoryLayout.BIOS_BUFFER_MEMMAP_START + 8);
	}
	
	@SJC.Inline
	public static int getMemMapType()
	{
		return MAGIC.rMem32(MemoryLayout.BIOS_BUFFER_MEMMAP_START + 16);
	}
	
	@SJC.Inline
	public static boolean memMapTypeIsFree()
	{
		return MAGIC.rMem32(MemoryLayout.BIOS_BUFFER_MEMMAP_START + 16) == 1;
	}
	
	private static MemMapEntry readMemMap()
	{
		long base = getMemMapBase();
		long length = getMemMapLength();
		int type = getMemMapType();
		return new MemMapEntry(base, length, type);
	}
}
