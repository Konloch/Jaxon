package rte;

public class SPackage
{
	public static SPackage root;
	public String name; //simple name of the package
	public SPackage outer; //higher-level package, noPackOuter deactivated*
	public SPackage subPacks; //first lower-level package
	public SPackage nextPack; //next package at the same level
	public SClassDesc units; //first unit of the current package
}
