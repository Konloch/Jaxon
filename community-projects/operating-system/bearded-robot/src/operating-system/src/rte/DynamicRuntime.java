package rte;

import memory.Memory;

/**
 * Provides various methods that are necessary for the runtime environment.
 */
@SuppressWarnings({"UnusedDeclaration", "SpellCheckingInspection", "JavaDoc"}) // TODO: JavaDoc
public class DynamicRuntime
{
	static Object firstDynamicObject = null;
	
	/**
	 * Is called if a new object is to be created at runtime.
	 * <p>
	 * This method is called when the new keyword is used to create objects.
	 * of objects is used. This method should therefore never be called directly
	 * called directly.
	 * </p>
	 *
	 * @param scalarSize The sum of the sizes of all base data type attributes,
	 * which the object type defines.
	 * @param relocEntries The number of reference attributes that the object type defines.
	 * object type defines.
	 * @param type The class descriptor of the object type.
	 * @return A new object.
	 */
	public static Object newInstance(int scalarSize, int relocEntries, SClassDesc type)
	{
		// ATTENTION: The new keyword must not be used within this method block and the
		// methods called in it, the new keyword must not be used.
		
		// Determine last object.
		Object lastObj = type;
		while (lastObj._r_next != null)
		{
			lastObj = lastObj._r_next;
		}
		
		// Reserve memory
		Object obj = Memory.allocate(relocEntries, scalarSize);
		if (firstDynamicObject == null)
			firstDynamicObject = obj;
		
		// Set the fields of the object.
		MAGIC.assign(obj._r_scalarSize, scalarSize);
		MAGIC.assign(obj._r_relocEntries, relocEntries);
		MAGIC.assign(obj._r_type, type);
		
		// Append the object to the object chain.
		//noinspection ConstantConditions
		MAGIC.assign(lastObj._r_next, obj);
		
		return obj;
	}
	
	/**
	 * Is called if a new array is to be created at runtime.
	 * <p>
	 * This method is called when the new keyword is used to create arrays.
	 * of arrays is used. This method should therefore never be called directly
	 * called directly.
	 * </p>
	 *
	 * @param length
	 * @param arrDim
	 * @param entrySize
	 * @param stdType
	 * @param unitType
	 * @return A new array.
	 */
	public static SArray newArray(int length, int arrDim, int entrySize, int stdType, Object unitType)
	{
		int scS, rlE;
		SArray me;
		
		if (stdType == 0 && unitType._r_type != MAGIC.clssDesc("SClassDesc"))
			MAGIC.inline(0xCC); //check type of unitType, we don't support interface arrays
		scS = MAGIC.getInstScalarSize("SArray");
		rlE = MAGIC.getInstRelocEntries("SArray");
		if (arrDim > 1 || entrySize < 0)
			rlE += length;
		else
			scS += length * entrySize;
		me = (SArray) newInstance(scS, rlE, (SClassDesc) MAGIC.clssDesc("SArray"));
		MAGIC.assign(me.length, length);
		MAGIC.assign(me._r_dim, arrDim);
		MAGIC.assign(me._r_stdType, stdType);
		MAGIC.assign(me._r_unitType, unitType);
		return me;
	}
	
	/**
	 * Is called if a new multidimensional array is to be created at runtime.
	 * is to be created at runtime.
	 * <p>
	 * This method is called when the new keyword is used to create
	 * of multidimensional arrays is used. This method should therefore
	 * never be called directly.
	 * </p>
	 *
	 * @param length
	 * @param arrDim
	 * @param entrySize
	 * @param stdType
	 * @param clssType
	 */
	public static void newMultArray(SArray[] parent, int curLevel, int destLevel, int length, int arrDim, int entrySize, int stdType, SClassDesc clssType)
	{
		int i;
		
		if (curLevel + 1 < destLevel)
		{ //step down one level
			curLevel++;
			for (i = 0; i < parent.length; i++)
				newMultArray((SArray[]) ((Object) parent[i]), curLevel, destLevel, length, arrDim, entrySize, stdType, clssType);
		}
		else
		{ //create the new entries
			destLevel = arrDim - curLevel;
			for (i = 0; i < parent.length; i++)
				parent[i] = newArray(length, destLevel, entrySize, stdType, clssType);
		}
	}
	
	/**
	 * Is called when the object class hierarchy is checked at runtime.
	 * is called.
	 * <p>
	 * This method is called when the instanceof keyword is used to check the object class hierarchy.
	 * checking the object class hierarchy is used. Therefore
	 * this method should never be called directly.
	 * </p>
	 *
	 * @param o
	 * @param dest
	 * @param asCast
	 * @return true if the type of the object is derived from the class,
	 * otherwise false.
	 */
	public static boolean isInstance(Object o, Object dest, boolean asCast)
	{
		SClassDesc check;
		
		if (o == null)
			return asCast; // true: null matches all; false: null is not an instance
		
		check = o._r_type;
		while (check != null)
		{
			if (check == dest)
				return true;
			check = check.parent;
		}
		
		if (asCast)
			MAGIC.inline(0xCC);
		return false;
	}
	
	/**
	 * Is called when the implementation of an interface is checked at runtime.
	 * is checked.
	 * <p>
	 * This method is called when the instanceof keyword is used for the
	 * check for interface implementations is used. Therefore, this
	 * method should never be called directly.
	 * </p>
	 *
	 * @param o
	 * @param dest
	 * @param asCast
	 * @return true if the type of the object implements the interface, otherwise
	 * false.
	 */
	public static SIntfMap isImplementation(Object o, SIntfDesc dest, boolean asCast)
	{
		SIntfMap check;
		
		if (o == null)
			return null;
		check = o._r_type.implementations;
		while (check != null)
		{
			if (check.owner == dest)
				return check;
			check = check.next;
		}
		if (asCast)
			MAGIC.inline(0xCC);
		return null;
	}
	
	/**
	 * Is called if the type of an array is set at runtime.
	 * <p>
	 * This method is called when the instanceof keyword is used to
	 * Check for array types is used. Therefore, this method should never
	 * called directly.
	 * </p>
	 *
	 * @param o
	 * @param stdType
	 * @param clssType
	 * @param arrDim
	 * @param asCast
	 * @return true if the array is of the searched type, otherwise false.
	 */
	public static boolean isArray(SArray o, int stdType, Object clssType, int arrDim, boolean asCast)
	{
		SClassDesc clss;
		
		//in fact o is of type "Object", _r_type has to be checked below - but this check is faster than "instanceof" and conversion
		if (o == null)
			return asCast; // true: null matches all; false: null is not an instance
		
		if (o._r_type != MAGIC.clssDesc("SArray"))
		{ //will never match independently of arrDim
			if (asCast)
				MAGIC.inline(0xCC);
			return false;
		}
		
		if (clssType == MAGIC.clssDesc("SArray"))
		{ //special test for arrays
			if (o._r_unitType == MAGIC.clssDesc("SArray"))
				arrDim--; //an array of SArrays, make next test to ">=" instead of ">"
			if (o._r_dim > arrDim)
				return true; //at least one level has to be left to have an object of type SArray
			if (asCast)
				MAGIC.inline(0xCC);
			return false;
		}
		
		//no specials, check arrDim and check for standard type
		if (o._r_stdType != stdType || o._r_dim < arrDim)
		{ //check standard types and array dimension
			if (asCast)
				MAGIC.inline(0xCC);
			return false;
		}
		
		//noinspection ConstantConditions
		if (stdType != 0)
		{
			if (o._r_dim == arrDim)
				return true; //array of standard-type matching
			if (asCast)
				MAGIC.inline(0xCC);
			return false;
		}
		
		//array of objects, make deep-check for class type (PicOS does not support interface arrays)
		if (o._r_unitType._r_type != MAGIC.clssDesc("SClassDesc"))
			MAGIC.inline(0xCC);
		
		clss = (SClassDesc) o._r_unitType;
		while (clss != null)
		{
			if (clss == clssType)
				return true;
			clss = clss.parent;
		}
		
		if (asCast)
			MAGIC.inline(0xCC);
		
		return false;
	}
	
	/**
	 * Checks whether the array and element types match when saving elements in an array.
	 * element types match.
	 *
	 * @param dest
	 * @param newEntry
	 */
	public static void checkArrayStore(SArray dest, SArray newEntry)
	{
		if (dest._r_dim > 1)
			isArray(newEntry, dest._r_stdType, dest._r_unitType, dest._r_dim - 1, true);
		else if (dest._r_unitType == null)
			MAGIC.inline(0xCC);
		else
			isInstance(newEntry, dest._r_unitType, true);
	}
}
