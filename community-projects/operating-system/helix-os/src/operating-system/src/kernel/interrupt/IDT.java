package kernel.interrupt;

import arch.x86;
import kernel.Kernel;
import kernel.MemoryLayout;
import kernel.trace.logging.Logger;
import java.util.BitHelper;
import java.lang.StringBuilder;

/**
 * The Interrupt Descriptor Table
 */
public class IDT
{
	/*
	 * The IDT can be placed somewhere in memory.
	 * In my system it starts at the lowest free address in the reserved stack
	 * region.
	 * Question: Could this not lead to the IDT being overwritten by the stack?
	 */
	private static final int SEGMENT_CODE = 1;
	private static final int REQUESTED_PRIV_LEVEL_OS = 0;
	private static boolean _initialized = false;
	
	public static void initialize()
	{
		PIC.Initialize();
		
		int dscAddr = MAGIC.cast2Ref(MAGIC.clssDesc("SystemInterrupts"));
		writeTableEntry(0, codeOffset(dscAddr, MAGIC.mthdOff("SystemInterrupts", "DivByZeroHandler")));
		writeTableEntry(1, codeOffset(dscAddr, MAGIC.mthdOff("SystemInterrupts", "DebugHandler")));
		writeTableEntry(2, codeOffset(dscAddr, MAGIC.mthdOff("SystemInterrupts", "NmiHandler")));
		writeTableEntry(3, codeOffset(dscAddr, MAGIC.mthdOff("SystemInterrupts", "BreakpointHandler")));
		writeTableEntry(4, codeOffset(dscAddr, MAGIC.mthdOff("SystemInterrupts", "OverflowHandler")));
		writeTableEntry(5, codeOffset(dscAddr, MAGIC.mthdOff("SystemInterrupts", "BoundRangeExceededHandler")));
		writeTableEntry(6, codeOffset(dscAddr, MAGIC.mthdOff("SystemInterrupts", "InvalidOpcodeHandler")));
		writeTableEntry(7, codeOffset(dscAddr, MAGIC.mthdOff("SystemInterrupts", "ReservedHandler")));
		writeTableEntry(8, codeOffset(dscAddr, MAGIC.mthdOff("SystemInterrupts", "DoubleFaultHandler")));
		for (int j = 9; j < 13; j++)
			writeTableEntry(j, codeOffset(dscAddr, MAGIC.mthdOff("SystemInterrupts", "ReservedHandler")));
		writeTableEntry(13, codeOffset(dscAddr, MAGIC.mthdOff("SystemInterrupts", "GeneralProtectionFaultHandler")));
		writeTableEntry(14, codeOffset(dscAddr, MAGIC.mthdOff("SystemInterrupts", "PageFaultHandler")));
		for (int j = 15; j < 32; j++)
			writeTableEntry(j, codeOffset(dscAddr, MAGIC.mthdOff("SystemInterrupts", "ReservedHandler")));
		for (int j = 32; j < MemoryLayout.IDT_ENTRIES; j++)
			writeTableEntry(j, codeOffset(dscAddr, MAGIC.mthdOff("SystemInterrupts", "IgnoreHandler"))); // IRQ 0-255
		
		loadTableProtectedMode();
		_initialized = true;
	}
	
	public static void registerIrqHandler(int irq, int handlerAddr)
	{
		if (!_initialized)
		{
			Kernel.panic("IDT not initialized");
			return;
		}
		Logger.info("IDT", new StringBuilder(64).append("Registering IRQ handler for IRQ ").append(irq).append(" at 0x").append(handlerAddr, 16).toString());
		writeTableEntry(irq + 32, handlerAddr);
	}
	
	@SJC.Inline
	public static void enable()
	{
		x86.sti();
	}
	
	@SJC.Inline
	public static void disable()
	{
		x86.cli();
	}
	
	@SJC.Inline
	public static void loadTableProtectedMode()
	{
		x86.ldit(MemoryLayout.IDT_BASE, MemoryLayout.IDT_SIZE - 1);
	}
	
	@SJC.Inline
	public static void loadTableRealMode()
	{
		x86.ldit(0, 1023);
	}
	
	public static int codeOffset(int classDesc, int mthdOff)
	{
		int code = MAGIC.rMem32(classDesc + mthdOff) + MAGIC.getCodeOff();
		return code;
	}
	
	private static void writeTableEntry(int i, int handlerAddr)
	{
		IDTEntry entry = (IDTEntry) MAGIC.cast2Struct(MemoryLayout.IDT_BASE + i * 8);
		entry.offsetLow = (short) BitHelper.getRange(handlerAddr, 0, 16);
		entry.selector = getSelector(SEGMENT_CODE, REQUESTED_PRIV_LEVEL_OS, false);
		entry.zero = 0;
		entry.typeAttr = (byte) 0x8E; // 10001110
		entry.offsetHigh = (short) BitHelper.getRange(handlerAddr, 16, 16);
	}
	
	/*
	 * The selector is a 16-bit value that contains the following fields:
	 * 0-1: Requested privilege level (0 = OS, ..., 3 = User)
	 * 2: Table indicator (0 = GDT, 1 = LDT)
	 * 3-13: Index of the segment descriptor in the GDT or LDT
	 */
	private static short getSelector(int segment, int privLevel, boolean tableLDT)
	{
		int selector = 0;
		selector = BitHelper.setRange(selector, 0, 2, privLevel);
		selector = BitHelper.setFlag(selector, 2, tableLDT);
		selector = BitHelper.setRange(selector, 3, 13, segment);
		return (short) selector;
	}
}
