package kernel.memory;

import kernel.MemoryLayout;
import kernel.hardware.Timer;
import kernel.trace.logging.Logger;

public class GarbageCollector
{
	
	private static boolean _isInitialized = false;
	
	private static int _gcCylce = 0;
	
	public static int InfoLastRunCollectedBytes = 0;
	public static int InfoLastRunCollectedObjects = 0;
	public static int InfoLastRunCompactedEmptyObjects = 0;
	public static int InfoLastRunTimeMs = 0;
	
	public static void Initialize()
	{
		if (_isInitialized)
		{
			return;
		}
		_isInitialized = true;
		Logger.info("GC", "Initialized");
	}
	
	public static boolean IsInitialized()
	{
		return _isInitialized;
	}
	
	public static void Run()
	{
		int startT = Timer.ticks();
		
		int objects = ResetMark();
		MarkFromStaticRoots();
		// MarkFromStack();
		InfoLastRunCollectedBytes = Sweep();
		MemoryManager.InvalidateLastAlloc();
		InfoLastRunCompactedEmptyObjects = CompactIfNeeded();
		InfoLastRunCollectedObjects = objects - MemoryManager.GetObjectCount();
		
		int endT = Timer.ticks();
		InfoLastRunTimeMs = Timer.ticksToMs(endT - startT);
		_gcCylce++;
	}
	
	private static int CompactIfNeeded()
	{
		if (ShouldCompact())
		{
			return MemoryManager.CompactEmptyObjects();
		}
		return 0;
	}
	
	private static boolean ShouldCompact()
	{
		return _gcCylce % 1 == 0;
	}
	
	private static int ResetMark()
	{
		int objects = 0;
		Object o = MemoryManager.GetStaticAllocRoot();
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
		Object o = MemoryManager.GetStaticAllocRoot();
		while (o != null)
		{
			if (MAGIC.cast2Ref(o) == addr)
			{
				return true;
			}
			o = o._r_next;
		}
		return false;
	}
	
	private static void MarkFromStaticRoots()
	{
		Object end = MemoryManager.GetDynamicAllocRoot();
		Object o = MemoryManager.GetStaticAllocRoot();
		while (o != end)
		{
			MarkRecursive(o);
			o = o._r_next;
		}
	}
	
	private static void MarkRecursive(Object o)
	{
		if (o == null || o.IsMarked())
		{
			return;
		}
		
		if (!(o instanceof Object))
		{
			return;
		}
		
		o.MarkUsed();
		
		// Skip _r_type and _r_next
		// we do not mark r_next and r_type is in static objects
		for (int relocIndex = 2; relocIndex < o._r_relocEntries; relocIndex++)
		{
			Object entry = o.ReadRelocEntry(relocIndex);
			if (entry != null)
			{
				MarkRecursive(entry);
			}
		}
	}
	
	private static int Sweep()
	{
		int sweepedBytes = 0;
		Object toRemove = MemoryManager.GetDynamicAllocRoot();
		Object nextObject = null;
		while (toRemove != null)
		{
			nextObject = toRemove._r_next;
			
			if (!toRemove.IsMarked())
			{
				sweepedBytes += MemoryManager.ObjectSize(toRemove);
				MemoryManager.RemoveFromNextChain(toRemove);
				EmptyObject replacedWithEO = MemoryManager.ReplaceWithEmptyObject(toRemove);
				MemoryManager.InsertIntoEmptyObjectChain(replacedWithEO);
			}
			toRemove = nextObject;
		}
		return sweepedBytes;
	}
}
