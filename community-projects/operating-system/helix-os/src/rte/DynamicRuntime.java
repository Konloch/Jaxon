package rte;

import kernel.Kernel;
import kernel.MemoryLayout;
import kernel.memory.MemoryManager;

public class DynamicRuntime
{
	static final int SIZE_FOR_PANIC_CALL = 1024;
	static int stackExtreme = MemoryLayout.PROGRAM_STACK_BOTTOM + SIZE_FOR_PANIC_CALL;
	
	/*
	 * Gets called if the function to be called would exceed stackExtreme.
	 * Panics since unrecoverable stack overflow.
	 */
	@SJC.StackExtreme
	static void stackExtremeError()
	{
		// make space for panic call
		stackExtreme -= SIZE_FOR_PANIC_CALL;
		Kernel.panic("Stack Overflow");
	}
	
	public static void nullException()
	{
		Kernel.panic("Null Pointer Exception");
	}
	
	public static Object newInstance(int scalarSize, int relocEntries, SClassDesc type)
	{
		return MemoryManager.allocateObject(scalarSize, relocEntries, type);
	}
	
	/*
	 * Copied from SJC manual
	 */
	public static SArray newArray(int length, int arrDim, int entrySize, int stdType, Object unitType)
	{
		int scS = MAGIC.getInstScalarSize("SArray");
		int rlE = MAGIC.getInstRelocEntries("SArray");
		
		if (arrDim > 1 || entrySize < 0)
			rlE += length; // Array mit Reloc-Elementen
		else
			scS += length * entrySize; // Array mit skalaren Elementen
		
		SArray obj = (SArray) MemoryManager.allocateObject(scS, rlE, MAGIC.clssDesc("SArray"));
		MAGIC.assign(obj.length, length);
		MAGIC.assign(obj._r_dim, arrDim);
		MAGIC.assign(obj._r_stdType, stdType);
		MAGIC.assign(obj._r_unitType, unitType);
		return obj;
	}
	
	/*
	 * Copied from SJC manual
	 */
	public static void newMultArray(SArray[] parent, int currentDimension, int destLevel, int length, int arrDim, int entrySize, int stdType, Object unitType)
	{
		int temp;
		
		// more than one dimension follows
		if (currentDimension + 1 < destLevel)
		{
			currentDimension++; // Increase current dimension
			for (temp = 0; temp < parent.length; temp++) // fill each element with array
				newMultArray((SArray[]) ((Object) parent[temp]), currentDimension, destLevel, length, arrDim, entrySize, stdType, unitType);
		}
		
		// last dimension to be created
		else
		{
			destLevel = arrDim - currentDimension; // Target dimension of an element
			for (temp = 0; temp < parent.length; temp++) // fill each element with target type
				parent[temp] = newArray(length, destLevel, entrySize, stdType, unitType);
		}
	}
	
	/*
	 * Copied from SJC manual
	 */
	public static boolean isInstance(Object o, SClassDesc dest, boolean asCast)
	{
		// Check for zero
		if (o == null)
		{
			// null may always be converted
			if (asCast)
				return true;
			
			// null is not an instance
			return false;
		}
		
		// for further comparisons Determine object type
		SClassDesc check = o._r_type;
		
		// search for suitable class
		while (check != null)
		{
			// suitable class found
			if (check == dest)
				return true;
			
			// Try parent class
			check = check.parent;
		}
		
		// Conversion error
		if (asCast)
			Kernel.panic("Conversion error");
		
		// Object does not match class
		return false;
	}
	
	/*
	 * Copied from SJC manual
	 */
	public static SIntfMap isImplementation(Object o, SIntfDesc dest, boolean asCast)
	{
		// zero implements nothing
		if (o == null)
			return null;
		
		// Determine the list of interface maps
		SIntfMap check = o._r_type.implementations;
		
		// search for suitable interface
		while (check != null)
		{
			// Interface found, deliver map
			if (check.owner == dest)
				return check;
			
			// try next interface map
			check = check.next;
		}
		
		// Conversion error
		if (asCast)
			Kernel.panic("Conversion error");
		
		// Object does not match interface
		return null;
	}
	
	/*
	 * Copied from SJC manual
	 */
	public static boolean isArray(SArray o, int stdType, Object unitType, int dim, boolean asCast)
	{
		// o is actually Object, check below!
		// Check for zero
		if (o == null)
		{
			// null may always be converted
			if (asCast)
				return true;
			
			// null is not an instance
			return false;
		}
		
		// Array check
		if (o._r_type != MAGIC.clssDesc("SArray"))
		{
			if (asCast)
				Kernel.panic("isArray: Conversion error");
			
			// no Array
			return false;
		}
		
		// Special treatment for SArray
		if (unitType == MAGIC.clssDesc("SArray"))
		{
			// Array from SArray
			if (o._r_unitType == MAGIC.clssDesc("SArray"))
				dim--;
			
			// Sufficient residual depth
			if (o._r_dim > dim)
				return true;
			
			if (asCast)
				Kernel.panic("isArray: Conversion error");
			
			// no SArray
			return false;
		}
		
		// necessary conditions
		if (o._r_stdType != stdType || o._r_dim < dim)
		{
			if (asCast)
				Kernel.panic("isArray: Conversion error");
			
			// Array with non-matching elements
			return false;
		}
		
		// Array of basic types
		if (stdType != 0)
		{
			// suitable depth
			if (o._r_dim == dim)
				return true;
			
			if (asCast)
				Kernel.panic("isArray: Conversion error");
			
			// Array not with matching elements
			return false;
		}
		
		// Type test required
		if (o._r_unitType._r_type == MAGIC.clssDesc("SClassDesc"))
		{
			// Instances
			SClassDesc clss = (SClassDesc) o._r_unitType;
			while (clss != null)
			{
				if (clss == unitType)
					return true;
				clss = clss.parent;
			}
		}
		else // Interfaces not supported
			Kernel.panic("isArray: Interface not supported");
		
		if (asCast)
			Kernel.panic("isArray: Conversion error");
		
		// Array with non-matching elements
		return false;
	}
	
	/*
	 * Copied from SJC manual
	 */
	public static void checkArrayStore(SArray dest, SArray newEntry)
	{
		// newEntry is actually “Object”, the check must be carried out in isArray!
		
		// Check the array via isArray, if the dimension of the target array is greater than 1
		if (dest._r_dim > 1)
			isArray(newEntry, dest._r_stdType, dest._r_unitType, dest._r_dim - 1, true);
			
		// Assignment error, if target array off has no reloc elements
		else if (dest._r_unitType == null)
			Kernel.panic("Zuweisungsfehler");
			
		// Instance check in all other cases
		else
		{
			if (dest._r_unitType._r_type == MAGIC.clssDesc("SClassDesc"))
				isInstance(newEntry, (SClassDesc) dest._r_unitType, true);
			else
				isImplementation(newEntry, (SIntfDesc) dest._r_unitType, true);
		}
	}
}