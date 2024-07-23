package interrupts;

import memory.Memory;
import video.Printer;

/**
 * Interrupt management class
 */
public class Interrupts
{
	/**
	 * Standard interrupt handler
	 */
	private static class DefaultHandler extends InterruptHandler
	{
		
		/**
		 * Stops system and outputs interrupt information.
		 *
		 * @param number Number of the interrupt
		 * @param errorCode Error code for interrupts with error code, otherwise zero.
		 */
		@SuppressWarnings({"InfiniteLoopStatement", "StatementWithEmptyBody"})
		@Override
		public void onInterrupt(int number, boolean hasErrorCode, int errorCode)
		{
			Printer.directPrintString("Interrupt:", 0, 24, Printer.WHITE, Printer.BLACK);
			Printer.directPrintInt(number, 10, 0, 11, 24, Printer.WHITE, Printer.BLACK);
			while (true);
		}
	}
	
	/**
	 * IDT of the protected mode.
	 */
	public static final Idt IDT = (Idt) MAGIC.cast2Struct(Memory.IDT_BASE_ADDRESS);
	
	/**
	 * Limit of the IDT.
	 */
	public static final int IDT_LIMIT = 8 * Idt.SIZE - 1;
	
	/**
	 * The references to the interrupt handlers.
	 */
	public static final InterruptHandler[] HANDLERS = new InterruptHandler[Idt.SIZE];
	
	private Interrupts()
	{
	}
	
	/**
	 * Initializes the interrupt functionality.
	 */
	public static void init()
	{
		
		// Exceptions
		setIsr(0x00, MAGIC.mthdOff("Interrupts", "isr0")); // 0 - Divide-by-zero
		setIsr(0x01, MAGIC.mthdOff("Interrupts", "isr1")); // 1 - Debug exception
		setIsr(0x02, MAGIC.mthdOff("Interrupts", "isr2")); // 2 - Non-Maskable Interrupt (NMI)
		setIsr(0x03, MAGIC.mthdOff("Interrupts", "isr3")); // 3 - Breakpoint (INT 3)
		setIsr(0x04, MAGIC.mthdOff("Interrupts", "isr4")); // 4 - Overflow (INTO)
		setIsr(0x05, MAGIC.mthdOff("Interrupts", "isr5")); // 5 - Bound exception
		setIsr(0x06, MAGIC.mthdOff("Interrupts", "isr6")); // 6 - Invalid Opcode
		setIsr(0x07, MAGIC.mthdOff("Interrupts", "isr7")); // 7 - reserved
		setIsr(0x08, MAGIC.mthdOff("Interrupts", "isr8")); // 8 - Double Fault
		setIsr(0x09, MAGIC.mthdOff("Interrupts", "isr9")); // 9 - reserved
		setIsr(0x0A, MAGIC.mthdOff("Interrupts", "isr10")); // 10 - reserved
		setIsr(0x0B, MAGIC.mthdOff("Interrupts", "isr11")); // 11 - reserved
		setIsr(0x0C, MAGIC.mthdOff("Interrupts", "isr12")); // 12 - reserved
		setIsr(0x0D, MAGIC.mthdOff("Interrupts", "isr13")); // 13 - General Protection Error
		setIsr(0x0E, MAGIC.mthdOff("Interrupts", "isr14")); // 14 - Page Fault
		setIsr(0x0F, MAGIC.mthdOff("Interrupts", "isr15")); // 15 - reserved
		setIsr(0x10, MAGIC.mthdOff("Interrupts", "isr16")); // 16 - reserved
		setIsr(0x11, MAGIC.mthdOff("Interrupts", "isr17")); // 17 - reserved
		setIsr(0x12, MAGIC.mthdOff("Interrupts", "isr18")); // 18 - reserved
		setIsr(0x13, MAGIC.mthdOff("Interrupts", "isr19")); // 19 - reserved
		setIsr(0x14, MAGIC.mthdOff("Interrupts", "isr20")); // 20 - reserved
		setIsr(0x15, MAGIC.mthdOff("Interrupts", "isr21")); // 21 - reserved
		setIsr(0x16, MAGIC.mthdOff("Interrupts", "isr22")); // 22 - reserved
		setIsr(0x17, MAGIC.mthdOff("Interrupts", "isr23")); // 23 - reserved
		setIsr(0x18, MAGIC.mthdOff("Interrupts", "isr24")); // 24 - reserved
		setIsr(0x19, MAGIC.mthdOff("Interrupts", "isr25")); // 25 - reserved
		setIsr(0x1A, MAGIC.mthdOff("Interrupts", "isr26")); // 26 - reserved
		setIsr(0x1B, MAGIC.mthdOff("Interrupts", "isr27")); // 27 - reserved
		setIsr(0x1C, MAGIC.mthdOff("Interrupts", "isr28")); // 28 - reserved
		setIsr(0x1D, MAGIC.mthdOff("Interrupts", "isr29")); // 29 - reserved
		setIsr(0x1E, MAGIC.mthdOff("Interrupts", "isr30")); // 30 - reserved
		setIsr(0x1F, MAGIC.mthdOff("Interrupts", "isr31")); // 31 - reserved
		
		// Hardware-Interrupts
		setIsr(0x20, MAGIC.mthdOff("Interrupts", "isr32")); // 32 - timer (IRQ0)
		setIsr(0x21, MAGIC.mthdOff("Interrupts", "isr33")); // 33 - Keyboard (IRQ1)
		setIsr(0x22, MAGIC.mthdOff("Interrupts", "isr34")); // 34 - IRQ2
		setIsr(0x23, MAGIC.mthdOff("Interrupts", "isr35")); // 35 - IRQ3
		setIsr(0x24, MAGIC.mthdOff("Interrupts", "isr36")); // 36 - IRQ4
		setIsr(0x25, MAGIC.mthdOff("Interrupts", "isr37")); // 37 - IRQ5
		setIsr(0x26, MAGIC.mthdOff("Interrupts", "isr38")); // 38 - IRQ6
		setIsr(0x27, MAGIC.mthdOff("Interrupts", "isr39")); // 39 - IRQ7
		setIsr(0x28, MAGIC.mthdOff("Interrupts", "isr40")); // 40 - IRQ8
		setIsr(0x29, MAGIC.mthdOff("Interrupts", "isr41")); // 41 - IRQ9
		setIsr(0x2A, MAGIC.mthdOff("Interrupts", "isr42")); // 42 - IRQ10
		setIsr(0x2B, MAGIC.mthdOff("Interrupts", "isr43")); // 43 - IRQ11
		setIsr(0x2C, MAGIC.mthdOff("Interrupts", "isr44")); // 44 - IRQ12
		setIsr(0x2D, MAGIC.mthdOff("Interrupts", "isr45")); // 45 - IRQ13
		setIsr(0x2E, MAGIC.mthdOff("Interrupts", "isr46")); // 46 - IRQ14
		setIsr(0x2F, MAGIC.mthdOff("Interrupts", "isr47")); // 47 - IRQ15
		
		// Set default handler
		InterruptHandler defaultHandler = new DefaultHandler();
		for (int i = 0; i < Idt.SIZE; i++)
		{
			HANDLERS[i] = defaultHandler;
		}
		
		// Load IDT
		loadInterruptDescriptorTable(IdtTypes.PROTECTED_MODE);
		
		// Initialize PICs
		Pics.init();
	}
	
	/**
	 * Initializes an IDT entry with the specified method offset.
	 *
	 * @param n Index of the IDT entry.
	 * @param methodOffset Method offset of an ISR.
	 */
	private static void setIsr(int n, int methodOffset)
	{
		int codeOffset = MAGIC.getCodeOff();
		int classReference = MAGIC.cast2Ref(MAGIC.clssDesc("Interrupts"));
		int isrAddress = MAGIC.rMem32(classReference + methodOffset) + codeOffset;
		IDT.entries[n].offsetLo = (short) (isrAddress & 0xFFFF);
		IDT.entries[n].selector = 0x8;
		IDT.entries[n].zero = 0;
		IDT.entries[n].flags = (byte) 0x8E;
		IDT.entries[n].offsetHi = (short) ((isrAddress >> 16) & 0xFFFF);
	}
	
	/**
	 * Loads the interrupt descriptor table of the specified type.
	 *
	 * @param idtType The type of IDT to be loaded.
	 * @see interrupts.IdtTypes
	 */
	public static void loadInterruptDescriptorTable(int idtType)
	{
		long tmp;
		
		switch (idtType)
		{
			case IdtTypes.REAL_MODE:
				tmp = 1023;
				break;
			case IdtTypes.PROTECTED_MODE:
				tmp = (((long) Memory.IDT_BASE_ADDRESS) << 16) | (long) IDT_LIMIT;
				break;
			default:
				return;
		}
		
		MAGIC.inline(0x0F, 0x01, 0x5D);
		MAGIC.inlineOffset(1, tmp); // lidt [ebp-0x08/tmp]
	}
	
	/**
	 * Enables the interrupts.
	 */
	@SJC.Inline
	public static void enableIRQs()
	{
		MAGIC.inline(0xFB);
	}
	
	/**
	 * Disables the interrupts.
	 */
	@SJC.Inline
	public static void disableIRQs()
	{
		MAGIC.inline(0xFA);
	}
	
	
	// This is followed by the interrupt service routines, each of which calls its
	// interrupt handler is called. For hardware interrupts, there is also an
	// interrupt confirmation
	
	@SuppressWarnings("UnusedDeclaration")
	@SJC.Interrupt
	public static void isr0()
	{
		Interrupts.disableIRQs();
		HANDLERS[0].onInterrupt(0, false, 0);
		Interrupts.enableIRQs();
	}
	
	@SuppressWarnings("UnusedDeclaration")
	@SJC.Interrupt
	public static void isr1()
	{
		Interrupts.disableIRQs();
		HANDLERS[1].onInterrupt(1, false, 0);
		Interrupts.enableIRQs();
	}
	
	@SuppressWarnings("UnusedDeclaration")
	@SJC.Interrupt
	public static void isr2()
	{
		Interrupts.disableIRQs();
		HANDLERS[2].onInterrupt(2, false, 0);
		Interrupts.enableIRQs();
	}
	
	@SuppressWarnings("UnusedDeclaration")
	@SJC.Interrupt
	public static void isr3()
	{
		Interrupts.disableIRQs();
		HANDLERS[3].onInterrupt(3, false, 0);
		Interrupts.enableIRQs();
	}
	
	@SuppressWarnings("UnusedDeclaration")
	@SJC.Interrupt
	public static void isr4()
	{
		Interrupts.disableIRQs();
		HANDLERS[4].onInterrupt(4, false, 0);
		Interrupts.enableIRQs();
	}
	
	@SuppressWarnings("UnusedDeclaration")
	@SJC.Interrupt
	public static void isr5()
	{
		Interrupts.disableIRQs();
		HANDLERS[5].onInterrupt(5, false, 0);
		Interrupts.enableIRQs();
	}
	
	@SuppressWarnings("UnusedDeclaration")
	@SJC.Interrupt
	public static void isr6()
	{
		Interrupts.disableIRQs();
		HANDLERS[6].onInterrupt(6, false, 0);
		Interrupts.enableIRQs();
	}
	
	@SuppressWarnings("UnusedDeclaration")
	@SJC.Interrupt
	public static void isr7()
	{
		Interrupts.disableIRQs();
		HANDLERS[7].onInterrupt(7, false, 0);
		Interrupts.enableIRQs();
	}
	
	@SuppressWarnings({"UnusedDeclaration", "UnnecessaryBoxing"})
	@SJC.Interrupt
	public static void isr8(int arg)
	{
		Interrupts.disableIRQs();
		HANDLERS[8].onInterrupt(8, true, arg);
		Interrupts.enableIRQs();
	}
	
	@SuppressWarnings({"UnusedDeclaration", "UnnecessaryBoxing"})
	@SJC.Interrupt
	public static void isr9()
	{
		Interrupts.disableIRQs();
		HANDLERS[9].onInterrupt(9, false, 0);
		Interrupts.enableIRQs();
	}
	
	@SuppressWarnings({"UnusedDeclaration", "UnnecessaryBoxing"})
	@SJC.Interrupt
	public static void isr10(int arg)
	{
		Interrupts.disableIRQs();
		HANDLERS[10].onInterrupt(10, true, arg);
		Interrupts.enableIRQs();
	}
	
	@SuppressWarnings({"UnusedDeclaration", "UnnecessaryBoxing"})
	@SJC.Interrupt
	public static void isr11(int arg)
	{
		Interrupts.disableIRQs();
		HANDLERS[11].onInterrupt(11, true, arg);
		Interrupts.enableIRQs();
	}
	
	@SuppressWarnings({"UnusedDeclaration", "UnnecessaryBoxing"})
	@SJC.Interrupt
	public static void isr12(int arg)
	{
		Interrupts.disableIRQs();
		HANDLERS[12].onInterrupt(12, true, arg);
		Interrupts.enableIRQs();
	}
	
	@SuppressWarnings({"UnusedDeclaration", "UnnecessaryBoxing"})
	@SJC.Interrupt
	public static void isr13(int arg)
	{
		Interrupts.disableIRQs();
		HANDLERS[13].onInterrupt(13, true, arg);
		Interrupts.enableIRQs();
	}
	
	@SuppressWarnings({"UnusedDeclaration", "UnnecessaryBoxing"})
	@SJC.Interrupt
	public static void isr14(int arg)
	{
		Interrupts.disableIRQs();
		HANDLERS[14].onInterrupt(14, true, arg);
		Interrupts.enableIRQs();
	}
	
	@SuppressWarnings("UnusedDeclaration")
	@SJC.Interrupt
	public static void isr15()
	{
		Interrupts.disableIRQs();
		HANDLERS[15].onInterrupt(15, false, 0);
		Interrupts.enableIRQs();
	}
	
	@SuppressWarnings("UnusedDeclaration")
	@SJC.Interrupt
	public static void isr16()
	{
		Interrupts.disableIRQs();
		HANDLERS[16].onInterrupt(16, false, 0);
		Interrupts.enableIRQs();
	}
	
	@SuppressWarnings("UnusedDeclaration")
	@SJC.Interrupt
	public static void isr17()
	{
		Interrupts.disableIRQs();
		HANDLERS[17].onInterrupt(17, false, 0);
		Interrupts.enableIRQs();
	}
	
	@SuppressWarnings("UnusedDeclaration")
	@SJC.Interrupt
	public static void isr18()
	{
		Interrupts.disableIRQs();
		HANDLERS[18].onInterrupt(18, false, 0);
		Interrupts.enableIRQs();
	}
	
	@SuppressWarnings("UnusedDeclaration")
	@SJC.Interrupt
	public static void isr19()
	{
		Interrupts.disableIRQs();
		HANDLERS[19].onInterrupt(19, false, 0);
		Interrupts.enableIRQs();
	}
	
	@SuppressWarnings("UnusedDeclaration")
	@SJC.Interrupt
	public static void isr20()
	{
		Interrupts.disableIRQs();
		HANDLERS[20].onInterrupt(20, false, 0);
		Interrupts.enableIRQs();
	}
	
	@SuppressWarnings("UnusedDeclaration")
	@SJC.Interrupt
	public static void isr21()
	{
		Interrupts.disableIRQs();
		HANDLERS[21].onInterrupt(21, false, 0);
		Interrupts.enableIRQs();
	}
	
	@SuppressWarnings("UnusedDeclaration")
	@SJC.Interrupt
	public static void isr22()
	{
		Interrupts.disableIRQs();
		HANDLERS[22].onInterrupt(22, false, 0);
		Interrupts.enableIRQs();
	}
	
	@SuppressWarnings("UnusedDeclaration")
	@SJC.Interrupt
	public static void isr23()
	{
		Interrupts.disableIRQs();
		HANDLERS[23].onInterrupt(23, false, 0);
		Interrupts.enableIRQs();
	}
	
	@SuppressWarnings("UnusedDeclaration")
	@SJC.Interrupt
	public static void isr24()
	{
		Interrupts.disableIRQs();
		HANDLERS[24].onInterrupt(24, false, 0);
		Interrupts.enableIRQs();
	}
	
	@SuppressWarnings("UnusedDeclaration")
	@SJC.Interrupt
	public static void isr25()
	{
		Interrupts.disableIRQs();
		HANDLERS[25].onInterrupt(25, false, 0);
		Interrupts.enableIRQs();
	}
	
	@SuppressWarnings("UnusedDeclaration")
	@SJC.Interrupt
	public static void isr26()
	{
		Interrupts.disableIRQs();
		HANDLERS[26].onInterrupt(26, false, 0);
		Interrupts.enableIRQs();
	}
	
	@SuppressWarnings("UnusedDeclaration")
	@SJC.Interrupt
	public static void isr27()
	{
		Interrupts.disableIRQs();
		HANDLERS[27].onInterrupt(27, false, 0);
		Interrupts.enableIRQs();
	}
	
	@SuppressWarnings("UnusedDeclaration")
	@SJC.Interrupt
	public static void isr28()
	{
		Interrupts.disableIRQs();
		HANDLERS[28].onInterrupt(28, false, 0);
		Interrupts.enableIRQs();
	}
	
	@SuppressWarnings("UnusedDeclaration")
	@SJC.Interrupt
	public static void isr29()
	{
		Interrupts.disableIRQs();
		HANDLERS[29].onInterrupt(29, false, 0);
		Interrupts.enableIRQs();
	}
	
	@SuppressWarnings("UnusedDeclaration")
	@SJC.Interrupt
	public static void isr30()
	{
		Interrupts.disableIRQs();
		HANDLERS[30].onInterrupt(30, false, 0);
		Interrupts.enableIRQs();
	}
	
	@SuppressWarnings("UnusedDeclaration")
	@SJC.Interrupt
	public static void isr31()
	{
		Interrupts.disableIRQs();
		HANDLERS[31].onInterrupt(31, false, 0);
		Interrupts.enableIRQs();
	}
	
	@SuppressWarnings("UnusedDeclaration")
	@SJC.Interrupt
	public static void isr32()
	{
		Interrupts.disableIRQs();
		HANDLERS[32].onInterrupt(32, false, 0);
		MAGIC.wIOs8(Pics.MASTER, (byte) 0x20);
		Interrupts.enableIRQs();
	}
	
	@SuppressWarnings("UnusedDeclaration")
	@SJC.Interrupt
	public static void isr33()
	{
		Interrupts.disableIRQs();
		HANDLERS[33].onInterrupt(33, false, 0);
		MAGIC.wIOs8(Pics.MASTER, (byte) 0x20);
		Interrupts.enableIRQs();
	}
	
	@SuppressWarnings("UnusedDeclaration")
	@SJC.Interrupt
	public static void isr34()
	{
		Interrupts.disableIRQs();
		HANDLERS[34].onInterrupt(34, false, 0);
		MAGIC.wIOs8(Pics.MASTER, (byte) 0x20);
		Interrupts.enableIRQs();
	}
	
	@SuppressWarnings("UnusedDeclaration")
	@SJC.Interrupt
	public static void isr35()
	{
		Interrupts.disableIRQs();
		HANDLERS[35].onInterrupt(35, false, 0);
		MAGIC.wIOs8(Pics.MASTER, (byte) 0x20);
		Interrupts.enableIRQs();
	}
	
	@SuppressWarnings("UnusedDeclaration")
	@SJC.Interrupt
	public static void isr36()
	{
		Interrupts.disableIRQs();
		HANDLERS[36].onInterrupt(36, false, 0);
		MAGIC.wIOs8(Pics.MASTER, (byte) 0x20);
		Interrupts.enableIRQs();
	}
	
	@SuppressWarnings("UnusedDeclaration")
	@SJC.Interrupt
	public static void isr37()
	{
		Interrupts.disableIRQs();
		HANDLERS[37].onInterrupt(37, false, 0);
		MAGIC.wIOs8(Pics.MASTER, (byte) 0x20);
		Interrupts.enableIRQs();
	}
	
	@SuppressWarnings("UnusedDeclaration")
	@SJC.Interrupt
	public static void isr38()
	{
		Interrupts.disableIRQs();
		HANDLERS[38].onInterrupt(38, false, 0);
		MAGIC.wIOs8(Pics.MASTER, (byte) 0x20);
		Interrupts.enableIRQs();
	}
	
	@SuppressWarnings("UnusedDeclaration")
	@SJC.Interrupt
	public static void isr39()
	{
		Interrupts.disableIRQs();
		HANDLERS[39].onInterrupt(39, false, 0);
		MAGIC.wIOs8(Pics.MASTER, (byte) 0x20);
		Interrupts.enableIRQs();
	}
	
	@SuppressWarnings("UnusedDeclaration")
	@SJC.Interrupt
	public static void isr40()
	{
		Interrupts.disableIRQs();
		HANDLERS[40].onInterrupt(40, false, 0);
		MAGIC.wIOs8(Pics.MASTER, (byte) 0x20);
		MAGIC.wIOs8(Pics.SLAVE, (byte) 0x20);
		Interrupts.enableIRQs();
	}
	
	@SuppressWarnings("UnusedDeclaration")
	@SJC.Interrupt
	public static void isr41()
	{
		Interrupts.disableIRQs();
		HANDLERS[41].onInterrupt(41, false, 0);
		MAGIC.wIOs8(Pics.MASTER, (byte) 0x20);
		MAGIC.wIOs8(Pics.SLAVE, (byte) 0x20);
		Interrupts.enableIRQs();
	}
	
	@SuppressWarnings("UnusedDeclaration")
	@SJC.Interrupt
	public static void isr42()
	{
		Interrupts.disableIRQs();
		HANDLERS[42].onInterrupt(42, false, 0);
		MAGIC.wIOs8(Pics.MASTER, (byte) 0x20);
		MAGIC.wIOs8(Pics.SLAVE, (byte) 0x20);
		Interrupts.enableIRQs();
	}
	
	@SuppressWarnings("UnusedDeclaration")
	@SJC.Interrupt
	public static void isr43()
	{
		Interrupts.disableIRQs();
		HANDLERS[43].onInterrupt(43, false, 0);
		MAGIC.wIOs8(Pics.MASTER, (byte) 0x20);
		MAGIC.wIOs8(Pics.SLAVE, (byte) 0x20);
		Interrupts.enableIRQs();
	}
	
	@SuppressWarnings("UnusedDeclaration")
	@SJC.Interrupt
	public static void isr44()
	{
		Interrupts.disableIRQs();
		HANDLERS[44].onInterrupt(44, false, 0);
		MAGIC.wIOs8(Pics.MASTER, (byte) 0x20);
		MAGIC.wIOs8(Pics.SLAVE, (byte) 0x20);
		Interrupts.enableIRQs();
	}
	
	@SuppressWarnings("UnusedDeclaration")
	@SJC.Interrupt
	public static void isr45()
	{
		Interrupts.disableIRQs();
		HANDLERS[45].onInterrupt(45, false, 0);
		MAGIC.wIOs8(Pics.MASTER, (byte) 0x20);
		MAGIC.wIOs8(Pics.SLAVE, (byte) 0x20);
		Interrupts.enableIRQs();
	}
	
	@SuppressWarnings("UnusedDeclaration")
	@SJC.Interrupt
	public static void isr46()
	{
		Interrupts.disableIRQs();
		HANDLERS[46].onInterrupt(46, false, 0);
		MAGIC.wIOs8(Pics.MASTER, (byte) 0x20);
		MAGIC.wIOs8(Pics.SLAVE, (byte) 0x20);
		Interrupts.enableIRQs();
	}
	
	@SuppressWarnings("UnusedDeclaration")
	@SJC.Interrupt
	public static void isr47()
	{
		Interrupts.disableIRQs();
		HANDLERS[47].onInterrupt(47, false, 0);
		MAGIC.wIOs8(Pics.MASTER, (byte) 0x20);
		MAGIC.wIOs8(Pics.SLAVE, (byte) 0x20);
		Interrupts.enableIRQs();
	}
	
}
