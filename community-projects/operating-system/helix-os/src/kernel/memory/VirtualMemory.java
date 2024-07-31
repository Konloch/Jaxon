package kernel.memory;

import kernel.Kernel;
import java.util.BitHelper;

public class VirtualMemory
{
	private static final int FOUR_MB = 1024 * 1024 * 4;
	private static final int PAGES = 1024;
	private static final int PAGE_SIZE = 4096;
	
	private static Object _pageTable;
	private static Object _pageDirectory;
	private static int _pageDirectoryAddr;
	private static int _pageTableAddr;
	
	public static void enableVirtualMemory()
	{
		allocatePageTable();
		allocatePageDirectory();
		writePageTable();
		writePageDirectory();
		setCR3(_pageDirectoryAddr);
		enableVirtualMemoryInternal();
	}
	
	public static void setCR3(int addr)
	{
		MAGIC.inline(0x8B, 0x45);
		MAGIC.inlineOffset(1, addr); // mov eax,[ebp+8]
		MAGIC.inline(0x0F, 0x22, 0xD8); // mov cr3,eax
	}
	
	public static void enableVirtualMemoryInternal()
	{
		MAGIC.inline(0x0F, 0x20, 0xC0); // mov eax,cr0
		MAGIC.inline(0x0D, 0x00, 0x00, 0x01, 0x80); // or eax,0x80010000
		MAGIC.inline(0x0F, 0x22, 0xC0); // mov cr0,eax
	}
	
	public static int getCR2()
	{
		int cr2 = 0;
		MAGIC.inline(0x0F, 0x20, 0xD0); // mov e/rax,cr2
		MAGIC.inline(0x89, 0x45);
		MAGIC.inlineOffset(1, cr2); // mov [ebp-4],eax
		return cr2;
	}
	
	private static void allocatePageDirectory()
	{
		// This is the worst possible way to align the table to 4096b but apparently I'm
		// too stupid to do it properly. Now we need 8MB but who cares tbh ¯\_(ツ)_/¯
		_pageDirectory = MemoryManager.allocateObject(MAGIC.getInstScalarSize("Object") + FOUR_MB * 2, MAGIC.getInstRelocEntries("Object"), MAGIC.clssDesc("Object"));
		
		int reservedSpaceStartPageDirectory = MAGIC.cast2Ref(_pageDirectory) + MAGIC.getInstScalarSize("Object");
		_pageDirectoryAddr = BitHelper.alignUp(reservedSpaceStartPageDirectory, 4096);
		
		if (_pageDirectory == null || _pageDirectoryAddr % 4096 != 0)
			Kernel.panic("PageTable not aligned to 4k: ".append(Integer.toString(_pageDirectoryAddr % 4096)));
	}
	
	private static void writePageDirectory()
	{
		if (_pageDirectory == null)
			Kernel.panic("PageDirectory not allocated");
		
		for (int i = 0; i < PAGES; i++)
		{
			int pageTableAddr = _pageTableAddr + i * PAGE_SIZE;
			MAGIC.wMem32(i * 4 + _pageDirectoryAddr, pageTableAddr | 0x03);
		}
	}
	
	private static void allocatePageTable()
	{
		// Bad code. See above.
		_pageTable = MemoryManager.allocateObject(MAGIC.getInstScalarSize("Object") + FOUR_MB * 2, MAGIC.getInstRelocEntries("Object"), MAGIC.clssDesc("Object"));
		
		int reservedSpaceStartMemPageTable = MAGIC.cast2Ref(_pageTable) + MAGIC.getInstScalarSize("Object");
		_pageTableAddr = BitHelper.alignUp(reservedSpaceStartMemPageTable, 4096);
		
		if (_pageTable == null || _pageTableAddr % 4096 != 0)
		{
			Kernel.panic("PageTable not aligned to 4k: ".append(Integer.toString(_pageTableAddr % 4096)));
		}
	}
	
	private static void writePageTable()
	{
		if (_pageTable == null)
			Kernel.panic("PageTable not allocated");
		
		// First page is a null page. Crash
		MAGIC.wMem32(_pageTableAddr, 0);
		
		for (int i = 1; i < PAGES * PAGES - 1; i++)
		{
			// The lower 12 bits are used for attribues
			// Since the 12 bits are cut off the address is automatically aligned to 4kb
			MAGIC.wMem32(i * 4 + _pageTableAddr, (i << 12) | 0x03);
		}
		
		// Last page is a null page. Crash
		MAGIC.wMem32(((PAGES * PAGES - 1) * 4) + _pageTableAddr, 0);
	}
}
