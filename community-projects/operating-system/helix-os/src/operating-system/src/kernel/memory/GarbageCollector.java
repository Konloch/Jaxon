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
		int startT = Timer.Ticks();
		
		int objects = ResetMark();
		MarkFromStaticRoots();
		// MarkFromStack();
		infoLastRunCollectedBytes = Sweep();
		MemoryManager.invalidateLastAlloc();
		infoLastRunCompactedEmptyObjects = compactIfNeeded();
		infoLastRunCollectedObjects = objects - MemoryManager.getObjectCount();
		
		int endT = Timer.Ticks();
		infoLastRunTimeMs = Timer.TicksToMs(endT - startT);
		_gcCylce++;
	}
	
	private static int compactIfNeeded()
	{
		if (ShouldCompact())
			return MemoryManager.compactEmptyObjects();
		
		return 0;
	}
	
	private static boolean ShouldCompact()
	{
		return _gcCylce % 1 == 0;
	}
	
	private static int ResetMark()
	{
		int objects = 0;
		Object o = MemoryManager.getStaticAllocRoot();
		
		while (o != null)
		{
			o.MarkUnused();
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
	@SuppressWarnings("unused")
	private static void MarkFromStack()
	{
		// TODO: Push all registers to stack
		int varAtTopOfStack = 0;
		int scanUntil = MAGIC.addr(varAtTopOfStack);
		for (int i = MemoryLayout.PROGRAM_STACK_COMPILER_TOP; i > scanUntil; i -= MAGIC.ptrSize)
		{
			int mem = MAGIC.rMem32(i);
			if (PointsToHeap(mem))
			{
				Object o = MAGIC.cast2Obj(mem);
				MarkRecursive(o);
			}
		}
	}
	
	// brute force ftw
	private static boolean PointsToHeap(int addr)
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
	
	private static void MarkFromStaticRoots()
	{
		Object end = MemoryManager.getDynamicAllocRoot();
		Object o = MemoryManager.getStaticAllocRoot();
		while (o != end)
		{
			MarkRecursive(o);
			o = o._r_next;
		}
	}
	
	private static void MarkRecursive(Object o)
	{
		if (o == null || o.IsMarked())
			return;
		
		if (!(o instanceof Object))
			return;
		
		o.MarkUsed();
		
		// Skip _r_type and _r_next
		// we do not mark r_next and r_type is in static objects
		for (int relocIndex = 2; relocIndex < o._r_relocEntries; relocIndex++)
		{
			Object entry = o.ReadRelocEntry(relocIndex);
			
			if (entry != null)
				MarkRecursive(entry);
		}
	}
	
	private static int Sweep()
	{
		int sweepedBytes = 0;
		Object toRemove = MemoryManager.getDynamicAllocRoot();
		Object nextObject = null;
		while (toRemove != null)
		{
			nextObject = toRemove._r_next;
			
			if (!toRemove.IsMarked())
			{
				sweepedBytes += MemoryManager.getObjectSize(toRemove);
				MemoryManager.removeFromNextChain(toRemove);
				EmptyObject replacedWithEO = MemoryManager.replaceWithEmptyObject(toRemove);
				MemoryManager.insertIntoEmptyObjectChain(replacedWithEO);
			}

			toRemove = nextObject;
		}
		return sweepedBytes;
	}
}
