package rte;

/**
 * This class provides metadata for Java packages that can be used at runtime.
 * can be used at runtime.
 */
@SuppressWarnings("ALL")
public class SPackage
{
	
	/**
	 * The root or default package;
	 */
	public static SPackage root;
	
	/**
	 * Simple name of the package.
	 */
	public String name;
	
	/**
	 * The package in which this package is contained.
	 */
	public SPackage outer;
	
	/**
	 * The first package contained in this package.
	 */
	public SPackage subPacks;
	
	/**
	 * The next package in the same package as this package.
	 */
	public SPackage nextPack;
	
	/**
	 * The first unit contained in this package.
	 */
	public SClassDesc units;
}
