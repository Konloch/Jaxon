package dbg;

import bios.BIOS;
import interrupts.InterruptHandler;
import interrupts.Interrupts;
import memory.Memory;
import rte.SClassDesc;
import rte.SMthdBlock;
import rte.SPackage;
import video.Printer;

/**
 * An implementation of {@link interrupts.InterruptHandler}, which generates a
 * blue screen is generated.
 */
class BluescreenExceptionHandler extends InterruptHandler
{
	private static final int fg = Printer.WHITE;
	
	private static final int bg = Printer.BLUE;
	
	@Override
	public void onInterrupt(int number, boolean hasErrorCode, int errorCode)
	{
		Interrupts.disableIRQs();
		
		// Switch back to text mode
		BIOS.regs.EAX = 0x0003;
		BIOS.rint(0x10);
		
		Printer.fillScreen(bg);
		
		int ebp = 0;
		MAGIC.inline(0x89, 0x6D);
		MAGIC.inlineOffset(1, ebp); //mov [ebp+xx],ebp
		ebp = MAGIC.rMem32(ebp); // Interrupt-Handler ausschließen
		
		printNumber(number); // Interruptnummer ausgeben
		printRegisters(ebp);
		printStackTrace(ebp, hasErrorCode);
		
		if (number == 14 && hasErrorCode)
			printPageFaultInfo(errorCode);
		
		while (true);
	}
	
	/**
	 * Outputs the number of an interrupt.
	 *
	 * @param number The interrupt number.
	 */
	private static void printNumber(int number)
	{
		int x = 0;
		x += Printer.directPrintString("INTERRUPT: ", x, 0, fg, bg);
		x += Printer.directPrintInt(number, 10, 0, x, 0, fg, bg);
		x += Printer.directPrintString(" (0x", x, 0, fg, bg);
		x += Printer.directPrintInt(number, 16, 2, x, 0, fg, bg);
		Printer.directPrintChar(')', x, 0, fg, bg);
	}
	
	/**
	 * Outputs register values stored on the stack.
	 *
	 * @param ebp EBP address, which identifies the stack frame.
	 */
	private static void printRegisters(int ebp)
	{
		PushAValues regs = (PushAValues) MAGIC.cast2Struct(ebp + 4);
		
		int x = 0;
		int y = 2;
		
		Printer.directPrintString("Registers:", x, y, fg, bg);
		y++;
		
		x = Printer.directPrintString("EAX=0x", x, y, fg, bg);
		Printer.directPrintInt(regs.eax, 16, 8, x, y, fg, bg);
		y++;
		x = 0;
		
		x = Printer.directPrintString("EBX=0x", x, y, fg, bg);
		Printer.directPrintInt(regs.ebx, 16, 8, x, y, fg, bg);
		y++;
		x = 0;
		
		x = Printer.directPrintString("ECX=0x", x, y, fg, bg);
		Printer.directPrintInt(regs.ecx, 16, 8, x, y, fg, bg);
		y++;
		x = 0;
		
		x = Printer.directPrintString("EDX=0x", x, y, fg, bg);
		Printer.directPrintInt(regs.edx, 16, 8, x, y, fg, bg);
		y++;
		x = 0;
		
		x = Printer.directPrintString("EBP=0x", x, y, fg, bg);
		Printer.directPrintInt(regs.ebp, 16, 8, x, y, fg, bg);
		y++;
		x = 0;
		
		x = Printer.directPrintString("ESP=0x", x, y, fg, bg);
		Printer.directPrintInt(regs.esp, 16, 8, x, y, fg, bg);
		y++;
		x = 0;
		
		x = Printer.directPrintString("ESI=0x", x, y, fg, bg);
		Printer.directPrintInt(regs.esi, 16, 8, x, y, fg, bg);
		y++;
		x = 0;
		
		x = Printer.directPrintString("EDI=0x", x, y, fg, bg);
		Printer.directPrintInt(regs.edi, 16, 8, x, y, fg, bg);
	}
	
	/**
	 * Creates a StackTrace for a stack frame.
	 *
	 * @param ebp EBP address that identifies the stack frame.
	 * @param hasErrorCode Whether the ISR supplies an error code.
	 */
	private static void printStackTrace(int ebp, boolean hasErrorCode)
	{
		int x = 0;
		int y = 12;
		
		Printer.directPrintString("Stack Trace:", 0, y, fg, bg);
		y++;
		
		int eip = 0;
		SMthdBlock mthdBlock = null;
		do
		{
			if (MAGIC.rMem32(ebp) == MAGIC.rMem32(ebp + 12) && MAGIC.rMem32(ebp) != 0) // falls ISR
				eip = MAGIC.rMem32(ebp + (hasErrorCode ? 40 : 36)); // mit oder ohne Parameter
			else
				eip = MAGIC.rMem32(ebp + 4);
			
			ebp = MAGIC.rMem32(ebp);
			
			// Ermittle SMthdBlock zu EIP.
			mthdBlock = EipToMthdBlock(eip);
			
			// Ausgabe
			x = Printer.directPrintString("0x", 0, y, fg, bg);
			x += Printer.directPrintInt(eip, 16, 8, x, y, fg, bg);
			x += Printer.directPrintString(": ", x, y, fg, bg);
			printMthdBlock(mthdBlock, x, y++);
		} while (mthdBlock != null && !"main()".equals(mthdBlock.namePar) && !"Kernel".equals(mthdBlock.owner.name));
	}
	
	/**
	 * Returns information about a PageFault.
	 *
	 * @param arg The argument of the PageFault.
	 */
	private static void printPageFaultInfo(int arg)
	{
		int x = 20;
		int y = 2;
		
		Printer.directPrintString("Page Fault:", x, y++, fg, bg);
		switch (arg)
		{
			case 0:
				Printer.directPrintString("Tried to read a non-present page entry.", x, y++, fg, bg);
				break;
			case 1:
				Printer.directPrintString("Tried to read a page and caused a protection fault.", x, y++, fg, bg);
				break;
			case 2:
				Printer.directPrintString("Tried to write to a non-present page entry.", x, y++, fg, bg);
				break;
			case 3:
				Printer.directPrintString("Tried to write a page and caused a protection fault.", x, y++, fg, bg);
				break;
		}
		
		int cr2 = Memory.getCR2();
		int addr = cr2 & 0xFFFFF000;
		x += Printer.directPrintString("Page Address=", x, y, fg, bg);
		Printer.directPrintInt(addr, 21, 8, x, y, fg, bg);
	}
	
	/**
	 * Returns the fully qualified name of a {@link rte.SMthdBlock}.
	 *
	 * @param block The {@link rte.SMthdBlock} whose name is to be output.
	 * is to be output.
	 * @param x The column from which to write.
	 * @param y The row in which to write.
	 */
	private static void printMthdBlock(SMthdBlock block, int x, int y)
	{
		if (block == null)
		{
			Printer.directPrintString("unknown", x, y, fg, bg);
		}
		else
		{
			String fullName = block.namePar;
			fullName = ".".concat(fullName);
			// Methodennamen an Unitnamen anhängen
			fullName = block.owner.name.concat(fullName);
			SPackage pack = block.owner.pack;
			// an Packagenamen anhängen
			while (pack != null && pack.name != null)
			{
				fullName = ".".concat(fullName);
				fullName = pack.name.concat(fullName);
				pack = pack.outer;
			}
			Printer.directPrintString(fullName, x, y, fg, bg);
		}
	}
	
	/**
	 * Determines the {@link rte.SMthdBlock} to an instruction address.
	 *
	 * @param eip The address for which a matching {@link rte.SMthdBlock}
	 * is to be searched for.
	 * @return The {@link rte.SMthdBlock} in whose code area the address
	 * is located or null if no matching {@link rte.SMthdBlock}
	 * could be found.
	 */
	private static SMthdBlock EipToMthdBlock(int eip)
	{
		return searchMthdBlockInPackage(SPackage.root, eip);
	}
	
	/**
	 * Searches in a {@link rte.SPackage} for a {@link rte.SMthdBlock},
	 * which matches the specified instruction address.
	 *
	 * @param pkg The {@link rte.SPackage} in which to search.
	 * @param eip An instruction address.
	 * @return The {@link rte.SMthdBlock} matching the instruction address or
	 * null if no matching {@link rte.SMthdBlock} could be found.
	 */
	private static SMthdBlock searchMthdBlockInPackage(SPackage pkg, int eip)
	{
		SMthdBlock block = null;
		
		SClassDesc unit = pkg.units;
		while (block == null && unit != null)
		{
			block = searchMthdBlockInUnit(unit, eip);
			unit = unit.nextUnit;
		}
		
		if (block == null && pkg.nextPack != null)
		{
			block = searchMthdBlockInPackage(pkg.nextPack, eip);
		}
		if (block == null && pkg.subPacks != null)
		{
			block = searchMthdBlockInPackage(pkg.subPacks, eip);
		}
		
		return block;
	}
	
	/**
	 * Searches in a {@link rte.SClassDesc} for a {@link rte.SMthdBlock},
	 * which matches the specified instruction address.
	 *
	 * @param unit The {@link rte.SClassDesc} in which to search.
	 * @param eip An instruction address.
	 * @return The {@link rte.SMthdBlock} matching the instruction address or
	 * null if no matching {@link rte.SMthdBlock} could be found.
	 */
	private static SMthdBlock searchMthdBlockInUnit(SClassDesc unit, int eip)
	{
		SMthdBlock block = unit.mthds;
		while (block != null)
		{
			int blockBase = MAGIC.cast2Ref(block) + MAGIC.getCodeOff();
			if (eip >= blockBase && eip < blockBase + block._r_scalarSize - MAGIC.getCodeOff())
			{
				break;
			}
			block = block.nextMthd;
		}
		
		return block;
	}
	
	private static class PushAValues extends STRUCT
	{
		int edi;
		int esi;
		int ebp;
		int esp;
		int ebx;
		int edx;
		int ecx;
		int eax;
	}
}
