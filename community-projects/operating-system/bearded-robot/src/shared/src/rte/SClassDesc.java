package rte;

/**
 * This class represents class descriptors that can be used,
 * to obtain meta information about objects at runtime.
 */
@SuppressWarnings("all")
public class SClassDesc
{
	/**
	 * The class descriptor of the class from which the class of this
	 * class descriptor is derived from.
	 */
	public SClassDesc parent;
	
	/**
	 * The list of interface descriptors of the interfaces implemented by the class
	 * of this class descriptor.
	 */
	public SIntfMap implementations;
	
	/**
	 * The next unit in the same package.
	 */
	public SClassDesc nextUnit;
	
	/**
	 * The simple name of the unit.
	 */
	public String name;
	
	/**
	 * The package in which the class is contained.
	 */
	public SPackage pack;
	
	/**
	 * The first method of the unit.
	 */
	public SMthdBlock mthds;
	
	/**
	 * The modifiers of the unit.
	 */
	public int modifier;
}
