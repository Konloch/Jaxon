package rte;

import interrupts.Interrupts;
import memory.Memory;
import scheduling.Task;

public class GarbageCollector extends Task
{
	private final int imageTop;
	
	private Object firstImageObject = null;
	
	public GarbageCollector()
	{
		this.imageTop = MAGIC.imageBase + MAGIC.rMem32(MAGIC.imageBase + 4);
		this.firstImageObject = MAGIC.cast2Obj(MAGIC.rMem32(MAGIC.imageBase + 16));
	}
	
	@Override
	protected void onSchedule()
	{
		if (DynamicRuntime.firstDynamicObject != null)
		{
			Interrupts.disableIRQs();
			
			// Select all dynamically created objects.
			Object obj = DynamicRuntime.firstDynamicObject;
			while (obj != null)
			{
				MAGIC.assign(obj._selected, 1);
				obj = obj._r_next;
			}
			
			// Remove selection of objects that can be reached from the root set are.
			Object imageObject = this.firstImageObject;
			while (imageObject != null && MAGIC.cast2Ref(imageObject) < this.imageTop)
			{
				followRelocEntries(imageObject);
				imageObject = imageObject._r_next;
			}
			
			// Continue to release selected objects.
			Object prev = null;
			Object now = DynamicRuntime.firstDynamicObject;
			while (now != null)
			{
				//noinspection ConstantConditions
				if (now._selected != 0 && prev != null)
				{
					MAGIC.assign(prev._r_next, now._r_next);
					Object tmp = now;
					now = now._r_next;
					Memory.free(tmp);
				}
				else
				{
					prev = now;
					now = now._r_next;
				}
			}
			
			// Attempts to merge EmptyObjects.
			Memory.join();
			
			Interrupts.enableIRQs();
		}
	}
	
	private void followRelocEntries(Object obj)
	{
		int ref = MAGIC.cast2Ref(obj);
		// Go through all RelocEntries in sequence. _r_type and _r_next
		// are skipped.
		for (int i = 3; i <= obj._r_relocEntries; i++)
		{
			int targetAddr = MAGIC.rMem32(ref - 4 * i);
			Object target = MAGIC.cast2Obj(targetAddr);

			//noinspection ConstantConditions
			if (targetAddr >= this.imageTop && target._selected != 0)
			{
				MAGIC.assign(target._selected, 0);
				followRelocEntries(target);
			}
		}
	}
	
}
