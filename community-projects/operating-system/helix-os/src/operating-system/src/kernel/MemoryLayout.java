package kernel;

public class MemoryLayout
{
	public static final int KERNEL_BASE = 0x0;
	
	/*
	 * The memory limit is set to 1GB. This is the maximum amount of memory that
	 * can be allocated by the kernel. If the kernel tries to allocate more memory
	 * than this limit, it will panic.
	 */
	public static final int MEMORY_LIMIT = 1024 * 1024 * 1024;
	
	public static final int REAL_MODE_INTERRUPT_TABLE_START = KERNEL_BASE;
	public static final int REAL_MODE_INTERRUPT_TABLE_SIZE = 1024;
	public static final int REAL_MODE_INTERRUPT_TABLE_END = REAL_MODE_INTERRUPT_TABLE_START + REAL_MODE_INTERRUPT_TABLE_SIZE;
	
	public static final int BOOTLOADER_START = 0x7C00;
	public static final int BOOTLOADER_END = 0x7DFF;
	
	public static final int INTERNAL_FREE_MEMORY_START = 0x07E00;
	public static final int INTERNAL_FREE_MEMORY_END = 0x9FFFF;
	
	public final static int IDT_BASE = INTERNAL_FREE_MEMORY_START;
	public final static int IDT_ENTRIES = 256;
	public final static int IDT_ENTRY_SIZE = 8;
	public final static int IDT_SIZE = IDT_ENTRIES * IDT_ENTRY_SIZE;
	public final static int IDT_END = IDT_BASE + IDT_SIZE;
	
	public static final int BIOS_BUFFER_MEMMAP_START = IDT_END + 24;
	public static final int BIOS_BUFFER_MEMMAP_SIZE = 20;
	public static final int BIOS_BUFFER_MEMMAP_END = BIOS_BUFFER_MEMMAP_START + BIOS_BUFFER_MEMMAP_SIZE;
	
	public final static int BIOS_MEMORY = 0x60000;
	public final static int BIOS_STKEND = BIOS_MEMORY + 0x1000;
	public final static int BIOS_STKBSE = BIOS_STKEND - 0x28;
	
	public static final int PROGRAM_STACK_TOP = INTERNAL_FREE_MEMORY_END;
	public static final int PROGRAM_STACK_BOTTOM = BIOS_STKEND;
	public static final int PROGRAM_STACK_COMPILER_TOP = 0x9BFFC;
	
	public static final int VGA_VID_BUFFER_START = 0xA0000;
	public static final int VGA_TEXT_BUFFER_START = 0xB8000;
}
