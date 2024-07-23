package rte;

public class SMthdBlock
{
	public final static int M_STAT = 0x00000020; //"static" modifier, taken from compiler
	
	public SClassDesc owner;
	public String namePar; //simple name, fully qualified parameter types
	public SMthdBlock nextMthd; //next method of the current class
	public int modifier; //Modifier of the method
	public int[] lineInCodeOffset; //optional line assignment to code bytes**
}
