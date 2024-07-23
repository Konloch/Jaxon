package interrupts;

/**
 * Structure for accessing the IDT of the protected mode.
 */
class Idt extends STRUCT
{
	/**
	 * Number of table entries
	 */
	public static final int SIZE = 48;
	
	/**
	 * Table entries
	 *
	 * @see interrupts.IdtEntry
	 */
	@SJC(count = SIZE)
	public IdtEntry[] entries;
}
