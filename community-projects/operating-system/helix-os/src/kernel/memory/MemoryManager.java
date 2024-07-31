package kernel.memory;

import kernel.Kernel;
import kernel.bios.call.MemMap;
import rte.SClassDesc;
import java.util.BitHelper;

public class MemoryManager
{
	public static final BootableImage BOOT_IMAGE = (BootableImage) MAGIC.cast2Struct(MAGIC.imageBase);
	
	/*
	 * The DynamicAllocRoot marks the beginning of the dynamic allocation.
	 * It is allocated as the first object after the static allocation,
	 * and marks the beginning of the dynamic allocations.
	 */
	private static DynamicAllocRoot _dynamicAllocRoot;
	
	/*
	 * The EmptyObjectRoot marks the beginning of the empty object chain.
	 * Empty objects are used to keep track of free memory regions.
	 */
	private static EmptyObject _emptyObjectRoot;
	
	/**
	 * The last allocation address used by the memory manager.
	 * This variable keeps track of the last memory address that was allocated.
	 * Should only be accessed through the LastAlloc() and SetLastAlloc() methods.
	 */
	private static int _lastAllocationAddress = -1;
	
	private static int _gc_allocationSizeSinceLastGC;
	private static boolean _gcEnabled;
	private static boolean _gcRunning;
	
	public static void initialize()
	{
		_gcEnabled = false;
		_gcRunning = false;
		
		initEmptyObjects();
		if (_emptyObjectRoot == null)
		{
			Kernel.panic("No viable memory regions found");
		}
		
		_dynamicAllocRoot = (DynamicAllocRoot) allocateObject(MAGIC.getInstScalarSize("DynamicAllocRoot"), MAGIC.getInstRelocEntries("DynamicAllocRoot"), MAGIC.clssDesc("DynamicAllocRoot"));
		
		_gc_allocationSizeSinceLastGC = 0;
	}
	
	public static boolean shouldCollectGarbage()
	{
		if (_gcEnabled == false)
			return false;

		return _gc_allocationSizeSinceLastGC > 12 * 1024;
	}
	
	public static void enableGarbageCollection()
	{
		_gcEnabled = true;
	}
	
	public static void disableGarbageCollection()
	{
		_gcEnabled = false;
	}
	
	public static void triggerGarbageCollection()
	{
		if (_gcEnabled == true && !_gcRunning && GarbageCollector.isInitialized())
		{
			_gcRunning = true;
			GarbageCollector.run();
			_gcRunning = false;
			_gc_allocationSizeSinceLastGC = 0;
		}
	}
	
	@SJC.Inline
	public static Object getStaticAllocRoot()
	{
		return MAGIC.cast2Obj(BOOT_IMAGE.firstHeapObject);
	}
	
	@SJC.Inline
	public static DynamicAllocRoot getDynamicAllocRoot()
	{
		return _dynamicAllocRoot;
	}
	
	@SJC.Inline
	public static EmptyObject getEmptyObjectRoot()
	{
		return _emptyObjectRoot;
	}
	
	@SJC.Inline
	public static int getObjectSize(Object o)
	{
		return o._r_scalarSize + o._r_relocEntries * MAGIC.ptrSize;
	}
	
	/**
	 * Invalidates the last allocation address.
	 * Will be needed for garbage collection.
	 */
	@SJC.Inline
	public static void invalidateLastAlloc()
	{
		_lastAllocationAddress = -1;
	}
	
	/**
	 * Returns the last allocated object.
	 * If the last allocation address is not cached, it will be recalculated.
	 *
	 * @return The last allocated object.
	 */
	@SJC.Inline
	public static Object getLastAlloc()
	{
		if (_lastAllocationAddress != -1)
			return MAGIC.cast2Obj(_lastAllocationAddress);
		
		// Recalculate the last allocation address
		// DynamicAllocRoot is null when we create the root object
		Object obj = _dynamicAllocRoot != null ? _dynamicAllocRoot : getStaticAllocRoot();
		while (obj._r_next != null)
		{
			obj = obj._r_next;
		}
		
		_lastAllocationAddress = MAGIC.cast2Ref(obj);
		return obj;
	}
	
	/**
	 * Sets the last allocated object cache value.
	 */
	@SJC.Inline
	public static void setLastAlloc(Object o)
	{
		if (o != null)
			_lastAllocationAddress = MAGIC.cast2Ref(o);
		else
			invalidateLastAlloc();
	}
	
	public static int getEmptyObjectCount()
	{
		int count = 0;
		Object eo = _emptyObjectRoot;
		while (eo != null)
		{
			count++;
			eo = eo._r_next;
		}
		return count;
	}
	
	public static int getObjectCount()
	{
		int size = 0;
		Object o = getStaticAllocRoot();
		while (o != null)
		{
			o = o._r_next;
			size++;
		}
		return size;
	}
	
	public static int GetDynamicObjectCount()
	{
		int size = 0;
		Object o = getDynamicAllocRoot();
		while (o != null)
		{
			o = o._r_next;
			size++;
		}
		return size;
	}
	
	public static int getFreeSpace()
	{
		int freeSpace = 0;
		Object eo = _emptyObjectRoot;
		while (eo != null)
		{
			freeSpace += getObjectSize(eo);
			eo = eo._r_next;
		}
		return freeSpace;
	}
	
	public static int getUsedSpace()
	{
		int usedSpace = 0;
		Object o = getStaticAllocRoot();
		while (o != null)
		{
			usedSpace += getObjectSize(o);
			o = o._r_next;
		}
		return usedSpace;
	}
	
	public static int getPadding(int offset, int align)
	{
		return (align - (offset % align)) % align;
	}
	
	public static Object allocateObject(int scalarSize, int relocEntries, SClassDesc type)
	{
		int paddedScalarSize = scalarSize + getPadding(scalarSize, 4);
		int newObjectTotalSize = paddedScalarSize + relocEntries * MAGIC.ptrSize;
		newObjectTotalSize += getPadding(newObjectTotalSize, 4);
		
		EmptyObject emptyObj = findEmptyObjectFitting(newObjectTotalSize);
		if (emptyObj == null)
			Kernel.panic("Out of memory");
		
		int newObjectBottom = 0;
		Object newObject = null;
		if (getObjectSize(emptyObj) == newObjectTotalSize)
		{
			// The new object fits exactly into the empty object
			// We can replace the empty object with the new object
			removeFromEmptyObjectChain(emptyObj);
			// The empty object will be overwritten
			newObjectBottom = emptyObj.AddressBottom();
		}
		else if (emptyObj.UnreservedScalarSize() >= newObjectTotalSize)
		{
			// The new object does not fit exactly into the empty object
			// We need to split the empty object
			newObjectBottom = emptyObj.AddressTop() - newObjectTotalSize;
			emptyObj.ShrinkBy(newObjectTotalSize);
		}
		else
			Kernel.panic("Failed to allocate object");
		
		newObject = writeObject(newObjectBottom, getPadding(newObjectBottom, 4), paddedScalarSize, relocEntries, type, true);
		insertIntoNextChain(getLastAlloc(), newObject);
		setLastAlloc(newObject);
		_gc_allocationSizeSinceLastGC += newObjectTotalSize;
		return newObject;
	}
	
	public static EmptyObject replaceWithEmptyObject(Object o)
	{
		if (o == null)
		{
			return null;
		}
		
		int startOfObject = o.AddressBottom();
		int endOfObject = o.AddressTop();
		
		return fillRegionWithEmptyObject(startOfObject, endOfObject);
	}
	
	public static EmptyObject fillRegionWithEmptyObject(long start, long end)
	{
		int emptyObjStart = (int) start;
		int emptyObjEnd = (int) end;
		int emptyObjScalarSize = emptyObjEnd - emptyObjStart - EmptyObject.RelocEntriesSize();
		int padding = getPadding(emptyObjStart, 4); // should be 0 since object is aligned
		EmptyObject eo = (EmptyObject) writeObject(emptyObjStart, padding, emptyObjScalarSize, EmptyObject.RelocEntries(), EmptyObject.Type(), true);
		return eo;
	}
	
	private static void initEmptyObjects()
	{
		int contIndex = 0;
		do
		{
			MemMap.ExecMemMap(contIndex);
			contIndex = MemMap.GetMemMapContinuationIndex();
			boolean isFree = MemMap.MemMapTypeIsFree();
			long base = MemMap.GetMemMapBase();
			long length = MemMap.GetMemMapLength();
			long end = base + length;
			
			if (base < BOOT_IMAGE.memoryStart + BOOT_IMAGE.memorySize)
				base = BitHelper.alignUp(BOOT_IMAGE.memoryStart + BOOT_IMAGE.memorySize + 1, 4);
			
			if (!isFree || base >= end || length <= EmptyObject.MinimumClassSize())
				continue;
			
			EmptyObject eo = fillRegionWithEmptyObject(base, end);
			insertIntoEmptyObjectChain(eo);
		} while (contIndex != 0);
	}
	
	/**
	 * Writes an object to memory.
	 *
	 * @param ptrNextFree  The pointer to the next free memory location.
	 * @param scalarSize   The size of the scalar fields in the object.
	 * @param relocEntries The number of relocation entries in the object.
	 * @param type         The type of the object.
	 * @return The allocated object.
	 */
	private static Object writeObject(int ptrNextFree, int padding, int scalarSize, int relocEntries, SClassDesc type, boolean clearMemory)
	{
		// Each reloc entry is a pointer
		int relocsSize = relocEntries * MAGIC.ptrSize;
		
		int startOfObject = ptrNextFree;
		int lengthOfObject = relocsSize + scalarSize + padding;
		
		if (lengthOfObject % 4 != 0)
			Kernel.panic("Object size not aligned");
		
		if (startOfObject % 4 != 0)
			Kernel.panic("Object start not aligned");
		
		if (clearMemory == true)
			Memory.memset32(startOfObject, lengthOfObject / 4, 0);
		
		// cast2Obj expects the pointer to the first scalar field.
		// It needs space because relocs will be stored in front of the object
		int firstScalarField = startOfObject + relocsSize + padding;
		
		Object obj = MAGIC.cast2Obj(firstScalarField);
		MAGIC.assign(obj._r_type, type);
		MAGIC.assign(obj._r_scalarSize, scalarSize);
		MAGIC.assign(obj._r_relocEntries, relocEntries);
		
		return obj;
	}
	
	@SJC.Inline
	private static void insertIntoNextChain(Object insertAfter, Object o)
	{
		MAGIC.assign(o._r_next, insertAfter._r_next);
		MAGIC.assign(insertAfter._r_next, o);
	}
	
	public static void removeFromNextChain(Object removeThis)
	{
		Object t = getStaticAllocRoot();
		while (t._r_next != removeThis)
		{
			t = t._r_next;
		}
		MAGIC.assign(t._r_next, removeThis._r_next);
	}
	
	/*
	 * Adds an empty object to the chain of empty objects.
	 * The chain is sorted by the address of the empty objects.
	 */
	public static void insertIntoEmptyObjectChain(EmptyObject toInsert)
	{
		if (toInsert == null)
			return;
		
		if (_emptyObjectRoot == null)
			_emptyObjectRoot = toInsert;
		else
		{
			int toInsertAddr = MAGIC.cast2Ref(toInsert);
			Object eo = _emptyObjectRoot;
			Object prev = null;
			while (eo != null)
			{
				if (MAGIC.cast2Ref(eo) > toInsertAddr)
					break;
				
				prev = eo;
				eo = eo._r_next;
			}
			
			if (prev == null)
			{
				MAGIC.assign(toInsert._r_next, eo);
				_emptyObjectRoot = toInsert;
				return;
			}
			
			MAGIC.assign(toInsert._r_next, eo);
			MAGIC.assign(prev._r_next, (Object) toInsert);
		}
	}
	
	public static int compactEmptyObjects()
	{
		int compactedObjects = 0;
		Object eo = _emptyObjectRoot;
		while (eo != null)
		{
			int prevTop = eo.AddressTop();
			Object next = eo._r_next;
			while (next != null)
			{
				int distance = next.AddressBottom() - prevTop;
				if (distance > 4)
					break;
				
				prevTop = next.AddressTop();
				EmptyObject nextEO = (EmptyObject) next;
				next = next._r_next;
				Memory.memset32(nextEO.AddressBottom(), 12 / 4, 0);
				
				compactedObjects++;
			}
			
			if (next != null)
			{
				int expandBy = prevTop - eo.AddressTop();
				if (expandBy > 4)
				{
					MAGIC.assign(eo._r_next, next);
					if (eo instanceof EmptyObject)
					{
						// Memory.Memset32(eo.AddressTop(), expandBy / 4, 0);
						((EmptyObject) eo).ExpandBy(expandBy);
					}
					else
						Kernel.panic(eo._r_type.name);
				}
			}
			
			eo = eo._r_next;
		}
		return compactedObjects;
	}
	
	@SJC.Inline
	public static void removeFromEmptyObjectChain(EmptyObject emptyObj)
	{
		if (_emptyObjectRoot == emptyObj)
			_emptyObjectRoot = emptyObj.Next();
		else
		{
			EmptyObject t = _emptyObjectRoot;
			
			while (t.Next() != emptyObj)
			{
				t = t.Next();
			}
			
			t.SetNext(emptyObj.Next());
		}
	}
	
	@SJC.Inline
	private static EmptyObject findEmptyObjectFitting(int objectSize)
	{
		EmptyObject emptyObj = _emptyObjectRoot;
		while (emptyObj != null)
		{
			if (emptyObj.UnreservedScalarSize() >= objectSize || getObjectSize(emptyObj) == objectSize)
				return emptyObj;
			
			emptyObj = emptyObj.Next();
		}
		return null;
	}
}
