package memory;

import bios.BIOS;

/**
 * This class provides various static methods for memory management
 * provided.
 * <p>
 * Internally, the free memory is managed using EmptyObjects, which are arranged in a
 * are arranged in a “growing downwards” list. This means that the first
 * EmptyObject has the highest address in the memory, the last the lowest.
 * </p>
 */
public final class Memory
{
	/**
	 * Base address of the IDT.
	 */
	public static final int IDT_BASE_ADDRESS = 0x7E00;
	
	/**
	 * Size of the IDT in bytes.
	 */
	public static final int IDT_SIZE = 48;
	
	/**
	 * Base address of the buffer for reading the memory areas.
	 */
	public static final int MEMORY_MAP_BUFFER_BASE_ADDRESS = 0x7E80;
	
	/**
	 * Size of the buffer for reading the memory areas, in bytes.
	 */
	public static final int MEMORY_MAP_BUFFER_SIZE = 20;
	
	private static final int PAGE_DIRECTORY_SIZE = 0x1000;
	
	private static final int PAGE_TABLES_SIZE = 0x400000;
	
	/**
	 * Reference to the {@link EmptyObject} with the highest address.
	 */
	static EmptyObject lastEmptyObject = null;
	
	private static boolean isInitialized = false;
	
	/**
	 * Initializes the {@link Memory} class. The state and behavior
	 * of the class before this method is called is not defined.
	 * Multiple calls have no effect.
	 */
	public static void init()
	{
		// Make sure that it is only initialized once.
		if (isInitialized)
		{
			return;
		}
		
		initVirtualMemory();
		
		MemoryMapBuffer buffer = (MemoryMapBuffer) MAGIC.cast2Struct(MEMORY_MAP_BUFFER_BASE_ADDRESS);
		
		// Determine the memory areas available according to the BIOS.
		BIOS.regs.EBX = 0;
		do
		{
			BIOS.regs.EAX = 0xE820;
			BIOS.regs.EDX = 0x534D4150;
			BIOS.regs.ECX = 20;
			BIOS.regs.ES = 0x0;
			BIOS.regs.EDI = MEMORY_MAP_BUFFER_BASE_ADDRESS;
			
			BIOS.rint(0x15);
			
			// Place all free memory areas above 1MB under
			// Memory management.
			if (buffer.base + buffer.length > getMinManagedAddress() && buffer.type == 1)
			{ // type == 1 => frei
				
				// Only support addresses that can be addressed with 32 bits.
				if (buffer.base <= Integer.MAX_VALUE && buffer.base + buffer.length <= Integer.MAX_VALUE)
				{
					
					addSegment((int) buffer.base, (int) buffer.length);
				}
			}
		} while (BIOS.regs.EBX != 0 && (BIOS.regs.FLAGS & BIOS.F_CARRY) == 0);
		
		isInitialized = true;
	}
	
	private static int getPageDirectoryBaseAddress()
	{
		return (MAGIC.imageBase + MAGIC.rMem32(MAGIC.imageBase + 4) + 4095) & ~0xFFF;
	}
	
	private static int getPageTablesBaseAddress()
	{
		return getPageDirectoryBaseAddress() + PAGE_DIRECTORY_SIZE;
	}
	
	private static int getMinManagedAddress()
	{
		return (getPageTablesBaseAddress() + PAGE_TABLES_SIZE + 3) & ~3;
	}
	
	/**
	 * Adds the specified memory area, the {@link Memory}, as a free memory area to be
	 * free memory area to be managed.
	 *
	 * @param base Base address of the free memory area to be managed.
	 * @param length Length of the free memory area to be managed.
	 */
	private static void addSegment(int base, int length)
	{
		// If the image is not in the area, manage the entire area
		// with an EmptyObject, otherwise manage the sections before and after the
		// image each with its own EmptyObject.
		if (getMinManagedAddress() <= base)
			addEmptyObject(base, length);
		else if (getMinManagedAddress() + EmptyObject.getMinimumSize() < base + length)
				addEmptyObject(getMinManagedAddress(), length - (getMinManagedAddress() - base));
	}
	
	/**
	 * Adds a new {@link EmptyObject} to the free memory list,
	 * that covers the specified memory range.
	 * <p>
	 * A {@link EmptyObject} is only created if its length is
	 * is greater than {@link EmptyObject#getMinimumSize()}.
	 * </p>
	 *
	 * @param base Base address of the memory area in which the
	 * {@link EmptyObject} is placed.
	 * @param length Length of the memory area in which the
	 * {@link EmptyObject} is placed.
	 */
	private static void addEmptyObject(int base, int length)
	{
		if (length >= EmptyObject.getMinimumSize())
		{
			// Create EmptyObject
			Object eo = MAGIC.cast2Obj(base + MAGIC.getInstRelocEntries("EmptyObject") * 4);
			MAGIC.assign(eo._r_relocEntries, MAGIC.getInstRelocEntries("EmptyObject"));
			MAGIC.assign(eo._r_scalarSize, length - eo._r_relocEntries * 4);
			MAGIC.assign(eo._r_type, MAGIC.clssDesc("EmptyObject"));
			
			// Append EmptyObject to the list.
			if (lastEmptyObject != null && MAGIC.cast2Ref(lastEmptyObject) > base)
			{
				EmptyObject previousEmptyObject = lastEmptyObject;
				while (previousEmptyObject._r_next != null && MAGIC.cast2Ref(previousEmptyObject._r_next) > base)
				{
					previousEmptyObject = (EmptyObject) previousEmptyObject._r_next;
				}
				
				MAGIC.assign(eo._r_next, previousEmptyObject._r_next);
				MAGIC.assign(previousEmptyObject._r_next, eo);
			}
			else
			{
				MAGIC.assign(eo._r_next, (Object) lastEmptyObject);
				lastEmptyObject = (EmptyObject) eo;
			}
		}
	}
	
	/**
	 * Allocate an object of the specified size.
	 *
	 * @param relocEntries Number of reference attributes.
	 * @param scalarSize Number of attributes with basic data type.
	 * @return An allocated object.
	 */
	public static Object allocate(int relocEntries, int scalarSize)
	{
		EmptyObject eo = lastEmptyObject;
		int objMemorySize = (relocEntries * 4 + scalarSize + 3) & ~3;
		
		// Search through the EmptyObject list
		while (eo != null)
		{
			// The first EmptyObject with a suitable size is used.
			if (eo.getSize() >= objMemorySize + EmptyObject.getMinimumSize())
			{
				// Shorten EmptyObject
				MAGIC.assign(eo._r_scalarSize, eo._r_scalarSize - objMemorySize);
				
				// Create new object
				int eoRef = MAGIC.cast2Ref(eo);
				int objBase = eoRef + eo._r_scalarSize;
				
				// Initialize memory with 0.
				for (int i = objBase; i < objBase + objMemorySize; i += 4)
				{
					MAGIC.wMem32(i, 0);
				}
				
				return MAGIC.cast2Obj(objBase + relocEntries * 4);
			}
			eo = (EmptyObject) eo._r_next;
			// TODO: Memory allocation, can also take place if the size of an EmptyObject corresponds exactly to “size”.
		}
		return null;
	}
	
	/**
	 * Releases the memory occupied by an object.
	 *
	 * @param obj Object whose memory area is to be released.
	 */
	public static void free(Object obj)
	{
		int size = obj._r_scalarSize + obj._r_relocEntries * 4;
		int addr = MAGIC.cast2Ref(obj) - obj._r_relocEntries * 4;
		addEmptyObject(addr, size);
	}
	
	/**
	 * Merges adjacent EmptyObjects into a single EmptyObject
	 * together.
	 */
	public static void join()
	{
		EmptyObject previousEmptyObject = null;
		EmptyObject eo = lastEmptyObject;
		while (eo != null && eo._r_next != null)
		{
			// Check whether eo._r_next and eo are adjacent.
			if (MAGIC.cast2Ref(eo._r_next) + eo._r_next._r_scalarSize + eo._r_relocEntries * 4 == MAGIC.cast2Ref(eo))
			{
				
				// Expand eo._r_next by the size of eo.
				MAGIC.assign(eo._r_next._r_scalarSize, eo._r_next._r_scalarSize + eo._r_scalarSize + eo._r_relocEntries * 4);
				
				// Correct concatenation
				eo = (EmptyObject) eo._r_next;
				if (previousEmptyObject == null)
					lastEmptyObject = eo;
				else
					MAGIC.assign(previousEmptyObject._r_next, (Object) eo);
			}
			else
			{
				eo = (EmptyObject) eo._r_next;
			}
		}
	}
	
	public static void initVirtualMemory()
	{
		createDirectory();
		createTables();
		
		setCR3(getPageDirectoryBaseAddress());
		enableVirtualMemory();
	}
	
	private static void createDirectory()
	{
		for (int i = 0; i < 1024; i++)
		{
			int tableAddr = getPageTablesBaseAddress() + 4096 * i;
			MAGIC.wMem32(getPageDirectoryBaseAddress() + 4 * i, tableAddr | 3);
		}
	}
	
	private static void createTables()
	{
		// First Page
		MAGIC.wMem32(getPageTablesBaseAddress(), 0);
		
		for (int i = 1; i < 1024 * 1024 - 1; i++)
		{
			int pageAddr = 4096 * i;
			MAGIC.wMem32(getPageTablesBaseAddress() + 4 * i, pageAddr | 3);
		}
		
		// Last Page
		int pageAddr = 4096 * (1024 * 1024 - 1);
		MAGIC.wMem32(getPageTablesBaseAddress() + 4 * (1024 * 1024 - 1), pageAddr);
	}
	
	private static void setCR3(int addr)
	{
		MAGIC.inline(0x8B, 0x45);
		MAGIC.inlineOffset(1, addr); //mov eax,[ebp+8]
		MAGIC.inline(0x0F, 0x22, 0xD8); //mov cr3,eax
	}
	
	private static void enableVirtualMemory()
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
	
}
