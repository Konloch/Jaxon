package java.lang;

import rte.SClassDesc;

/**
 * The class {@link java.lang.Object} is the root of the object hierarchy. Each
 * class has {@link java.lang.Object} as its parent class. All objects, arrays
 * including arrays, implement the methods of this class.
 */
@SuppressWarnings("all")
public class Object
{
	/**
	 * The {@link rte.SClassDesc} of the object type.
	 */
	public final SClassDesc _r_type = null;
	
	/**
	 * The next {@link java.lang.Object} in the object storage chain.
	 */
	public final Object _r_next = null;
	
	/**
	 * The number of reference attributes defined by the object type.
	 */
	public final int _r_relocEntries = 0;
	
	/**
	 * The sum of the sizes of all basic data type attributes defined by the object type defined.
	 */
	public final int _r_scalarSize = 0;
	
	/**
	 * Indicates whether an object is selected. 0 means “not selected” everything other “marked”.
	 */
	public final int _selected = 0;
	
	public String toString()
	{
		return this._r_type.name;
	}
	
	public boolean equals(Object obj)
	{
		return this == obj;
	}
}
