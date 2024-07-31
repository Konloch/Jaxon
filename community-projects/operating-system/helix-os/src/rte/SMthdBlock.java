package rte;

public class SMthdBlock
{ // class with additional instance variables
	public String namePar; // simple name, fully qualified parameter types
	public SMthdBlock nextMthd; // next method of the current class
	public int modifier; // Modifier of the method
	public int[] lineInCodeOffset; // optional line assignment to code bytes**
	public SClassDesc owner;
}