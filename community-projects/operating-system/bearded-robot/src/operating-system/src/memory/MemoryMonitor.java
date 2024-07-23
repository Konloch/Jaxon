package memory;

import scheduling.Task;
import video.Printer;

public class MemoryMonitor extends Task
{
	@Override
	protected void onSchedule()
	{
		EmptyObject tmp = Memory.lastEmptyObject;
		int i = 10;
		while (tmp != null)
		{
			int b = MAGIC.cast2Ref(tmp) - tmp._r_relocEntries * 4;
			int e = MAGIC.cast2Ref(tmp) + tmp._r_scalarSize;
			Printer.directPrintString("     ", 0, i, Printer.WHITE, Printer.BLACK);
			Printer.directPrintInt(i - 10, 10, 0, 0, i, Printer.WHITE, Printer.BLACK);
			Printer.directPrintInt(b, 16, 8, 5, i, Printer.WHITE, Printer.BLACK);
			Printer.directPrintInt(e, 16, 8, 20, i, Printer.WHITE, Printer.BLACK);
			tmp = (EmptyObject) tmp._r_next;
			i++;
			Printer.directPrintString("               ", 0, i, Printer.WHITE, Printer.BLACK);
		}
	}
}
