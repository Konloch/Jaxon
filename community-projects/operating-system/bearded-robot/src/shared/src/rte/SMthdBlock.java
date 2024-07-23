package rte;

/**
 * This class provides meta information on methods that can be used at runtime.
 * can be used at runtime.
 */
@SuppressWarnings("all")
public class SMthdBlock
{
	/**
	 * The simple method name with the fully qualified parameter types in
	 * round brackets.
	 */
	public String namePar;
	
	/**
	 * The class to which this method belongs.
	 */
	public SClassDesc owner;
	
	/**
	 * The next method of the same unit.
	 */
	public SMthdBlock nextMthd;
	
	/**
	 * The modifiers of the method.
	 */
	public int modifier;
	
	/**
	 * Code byte line number assignment
	 */
	public int[] lineInCodeOffset;
}
