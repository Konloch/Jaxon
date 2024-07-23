package rte;

public class SClassDesc
{ //class with additional instance variables
	public SClassDesc parent; //already exists: extended class
	public SIntfMap implementations; //already available: Interfaces
	public SClassDesc nextUnit; //next unit of the current package
	public String name; //simple name of the unit
	public SPackage pack; //owning package, noClassPack deactivated*
	public SMthdBlock mthds; //first method of the unit
	public int modifier; //modifier of the unit, noClassMod deactivated*
}