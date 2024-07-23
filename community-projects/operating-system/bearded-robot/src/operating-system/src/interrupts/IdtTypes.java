package interrupts;

/**
 * Constants to differentiate between different IDT types.
 */
public class IdtTypes
{
	/**
	 * Real mode IDT
	 */
	public static final int REAL_MODE = 0;
	
	/**
	 * Protected mode IDT
	 */
	public static final int PROTECTED_MODE = 1;
	
	private IdtTypes()
	{
	}
}
