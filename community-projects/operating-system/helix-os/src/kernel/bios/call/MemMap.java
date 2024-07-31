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
	
	public boolean HasNext()
	{
		return memMapContinuationIndex != 0;
	}
	
	public MemMapEntry Next()
	{
		if (_first)
		{
			_first = false;
			MemMapEntry e = Exec(0);
			memMapContinuationIndex = GetMemMapContinuationIndex();
			return e;
		}
		if (memMapContinuationIndex == 0)
		{
			return null;
		}
		MemMapEntry e = Exec(memMapContinuationIndex);
		memMapContinuationIndex = GetMemMapContinuationIndex();
		return e;
	}
	
	public MemMapEntry NextFree()
	{
		MemMapEntry e = null;
		while (HasNext())
		{
			e = Next();
			if (e.IsFree())
			{
				return e;
			}
		}
		return null;
	}
	
	public static MemMapEntry Exec(int idx)
	{
		ExecMemMap(idx);
		return ReadMemMap();
	}
	
	public static void ExecMemMap(int idx)
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
	public static int GetMemMapContinuationIndex()
	{
		return Registers.EBX;
	}
	
	@SJC.Inline
	public static long GetMemMapBase()
	{
		return MAGIC.rMem64(MemoryLayout.BIOS_BUFFER_MEMMAP_START);
	}
	
	@SJC.Inline
	public static long GetMemMapLength()
	{
		return MAGIC.rMem64(MemoryLayout.BIOS_BUFFER_MEMMAP_START + 8);
	}
	
	@SJC.Inline
	public static int GetMemMapType()
	{
		return MAGIC.rMem32(MemoryLayout.BIOS_BUFFER_MEMMAP_START + 16);
	}
	
	@SJC.Inline
	public static boolean MemMapTypeIsFree()
	{
		return MAGIC.rMem32(MemoryLayout.BIOS_BUFFER_MEMMAP_START + 16) == 1;
	}
	
	private static MemMapEntry ReadMemMap()
	{
		long base = GetMemMapBase();
		long length = GetMemMapLength();
		int type = GetMemMapType();
		return new MemMapEntry(base, length, type);
	}
}
