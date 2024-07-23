package rte;

public class SPackage
{ //additional class
	public static SPackage root; //static field for root package
	public String name; //simple name of the package
	public SPackage outer; //higher-level package, noPackOuter disabled*
	public SPackage subPacks; //first lower-level package
	public SPackage nextPack; //next package at the same level
	public SClassDesc units; //first unit of the current package
}