package kernel.memory;

import kernel.MemoryLayout;
import kernel.hardware.Timer;
import kernel.trace.logging.Logger;

public class GarbageCollector
{
	
	private static boolean _isInitialized = false;
	
	private static int _gcCylce = 0;
	
	public static int infoLastRunCollectedBytes = 0;
	public static int infoLastRunCollectedObjects = 0;
	public static int infoLastRunCompactedEmptyObjects = 0;
	public static int infoLastRunTimeMs = 0;
	
	public static void initialize()
	{
		if (_isInitialized)
			return;
		
		_isInitialized = true;
		Logger.info("GC", "Initialized");
	}
	
	public static boolean isInitialized()
	{
		return _isInitialized;
	}
	
	public static void run()
	{
		int startT = Timer.ticks();
		
		int objects = resetMark();
		markFromStaticRoots();
		// MarkFromStack();
		infoLastRunCollectedBytes = sweep();
		MemoryManager.invalidateLastAlloc();
		infoLastRunCompactedEmptyObjects = compactIfNeeded();
		infoLastRunCollectedObjects = objects - MemoryManager.getObjectCount();
		
		int endT = Timer.ticks();
		infoLastRunTimeMs = Timer.ticksToMs(endT - startT);
		_gcCylce++;
	}
	
	private static int compactIfNeeded()
	{
		if (shouldCompact())
			return MemoryManager.compactEmptyObjects();
		
		return 0;
	}
	
	private static boolean shouldCompact()
	{
		return _gcCylce % 1 == 0;
	}
	
	private static int resetMark()
	{
		int objects = 0;
		Object o = MemoryManager.getStaticAllocRoot();
		
		while (o != null)
		{
			o.markUnused();
			o = o._r_next;
			objects++;
		}
		
		return objects;
	}
	
	/*
	 * Mark all objects that are reachable from the stack.
	 * This causes some issues atm so its basically ignored by only calling the gc
	 * in situations where the stack is not used.
	 */
	private static void markFromStack()
	{
		// TODO: Push all registers to stack
		int varAtTopOfStack = 0;
		int scanUntil = MAGIC.addr(varAtTopOfStack);
		for (int i = MemoryLayout.PROGRAM_STACK_COMPILER_TOP; i > scanUntil; i -= MAGIC.ptrSize)
		{
			int mem = MAGIC.rMem32(i);
			if (pointsToHeap(mem))
			{
				Object o = MAGIC.cast2Obj(mem);
				markRecursive(o);
			}
		}
	}
	
	// brute force ftw
	private static boolean pointsToHeap(int addr)
	{
		Object o = MemoryManager.getStaticAllocRoot();
		while (o != null)
		{
			if (MAGIC.cast2Ref(o) == addr)
				return true;
			
			o = o._r_next;
		}
		return false;
	}
	
	private static void markFromStaticRoots()
	{
		Object end = MemoryManager.getDynamicAllocRoot();
		Object o = MemoryManager.getStaticAllocRoot();
		while (o != end)
		{
			markRecursive(o);
			o = o._r_next;
		}
	}
	
	private static void markRecursive(Object o)
	{
		if (o == null || o.isMarked())
			return;
		
		if (!(o instanceof Object))
			return;
		
		o.markUsed();
		
		// Skip _r_type and _r_next
		// we do not mark r_next and r_type is in static objects
		for (int relocIndex = 2; relocIndex < o._r_relocEntries; relocIndex++)
		{
			Object entry = o.readRelocEntry(relocIndex);
			
			if (entry != null)
				markRecursive(entry);
		}
	}
	
	private static int sweep()
	{
		int sweepedBytes = 0;
		Object toRemove = MemoryManager.getDynamicAllocRoot();
		Object nextObject;
		while (toRemove != null)
		{
			nextObject = toRemove._r_next;
			
			if (!toRemove.isMarked())
			{
				sweepedBytes += MemoryManager.objectSize(toRemove);
				MemoryManager.removeFromNextChain(toRemove);
				EmptyObject replacedWithEO = MemoryManager.replaceWithEmptyObject(toRemove);
				MemoryManager.insertIntoEmptyObjectChain(replacedWithEO);
			}
			
			toRemove = nextObject;
		}
		return sweepedBytes;
	}
}
