package apps.tests;

import scheduling.Task;

/**
 * Test application that intentionally generates a PageFault.
 */
public class VirtualMemoryTest extends Task
{
	@Override
	protected void onSchedule()
	{
		// Schreibtest
		//MAGIC.wMem32(0, 0);
		
		// Lesetest
		Task task = null;
		int size = task._r_scalarSize;
	}
}
