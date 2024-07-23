package kernel;

import hardware.Serial;
import rte.DynamicRuntime;
import rte.SClassDesc;

public class VirtualMemory
{
	private static final int CR0_BITMASK = 0x80010000;
	private static final int PAGECOUNT = 1024;
	private static final int PAGEDIRECTORY_BASEADDR = 0xA000; //1024*32 bit = 4k
	private static PageTable pt;
	
	static void enableVirtualMemory()
	{
		writePageDirectoryAndTable();
		setCR3(PAGEDIRECTORY_BASEADDR);
		enableVirtualMemoryInternal();
	}
	
	private static void setCR3(int addr)
	{
		MAGIC.inline(0x8B, 0x45);
		MAGIC.inlineOffset(1, addr); //mov eax,[ebp+8]
		MAGIC.inline(0x0F, 0x22, 0xD8); //mov cr3,eax
	}
	
	private static void enableVirtualMemoryInternal()
	{
		MAGIC.inline(0x0F, 0x20, 0xC0); //mov eax,cr0
		MAGIC.inline(0x0D, 0x00, 0x00, 0x01, 0x80); //or eax,0x80010000
		MAGIC.inline(0x0F, 0x22, 0xC0); //mov cr0,eax
	}
	
	public static int getCR2()
	{
		int cr2 = 0;
		MAGIC.inline(0x0F, 0x20, 0xD0); //mov e/rax,cr2
		MAGIC.inline(0x89, 0x45);
		MAGIC.inlineOffset(1, cr2); //mov [ebp-4],eax
		return cr2;
	}
	
	private static void writePageDirectoryAndTable()
	{
		//get dynamic runtime object for pagetable, because it takes up 4M of memory
		pt = (PageTable) DynamicRuntime.newInstance(MAGIC.getInstScalarSize("PageTable") + 1024 * 1024 * 4, MAGIC.getInstRelocEntries("PageTable"), (SClassDesc) MAGIC.clssDesc("PageTable"));
		int PAGETABLE_BASEADDR = MAGIC.cast2Ref(pt) + MAGIC.getInstScalarSize("PageTable");
		//Sanity check for table address, as it needs to be aligned at a 4k boundary
		if ((PAGETABLE_BASEADDR & 0xFFF) > 0)
		{
			Serial.print("PageTable not aligned to 4k");
			MAGIC.inline(0xCC);
		}
		
		//PAGEDIR
		for (int i = 0; i < PAGECOUNT; i++)
		{
			//left-shift i by 12 to align to 4k boundaries (4096)
			MAGIC.wMem32(i * 4 + PAGEDIRECTORY_BASEADDR, ((i << 12) + PAGETABLE_BASEADDR) | 0x3);
		}
		//PAGETABLE
		//first page special
		MAGIC.wMem32(PAGETABLE_BASEADDR, 0);
		for (int i = 1; i < PAGECOUNT * PAGECOUNT - 1; i++)
		{
			MAGIC.wMem32(i * 4 + PAGETABLE_BASEADDR, (i << 12) | 0x3);
		}
		//last page special
		MAGIC.wMem32(((PAGECOUNT * PAGECOUNT - 1) * 4) + PAGETABLE_BASEADDR, 0);
	}
	
}
