package rte;

/**
 * @author S. Frenz
 */
public class DynamicRuntime
{
	public static int objCnt;
	
	private final static int MEMBLOCK = 1024 * 1024; //1 MB per block
	private final static int BLOCKMAX = 64 * 1024;   //allocate objects up to 64 KB in memblock
	private static int nextFreeAddr, memFree;
	
	public static Object newInstance(int scalarSize, int relocEntries, rte.SClassDesc type)
	{
		int addr, rs, size;
		Object me;
		
		//get informations
		rs = relocEntries * MAGIC.ptrSize;
		scalarSize = (scalarSize + MAGIC.ptrSize - 1) & ~(MAGIC.ptrSize - 1);
		size = rs + scalarSize;
		if (size > BLOCKMAX)
			addr = allocMem(size); //allocate special block
		else
		{ //allocate inside the pool
			if (size > memFree)
			{
				nextFreeAddr = allocMem(MEMBLOCK);
				memFree = MEMBLOCK;
			}
			addr = nextFreeAddr;
			memFree -= size;
			nextFreeAddr += size;
		}
		//prepare object
		setMem32(addr, size >> 2, 0); //clear memory
		me = MAGIC.cast2Obj(addr + rs); //place object
		me._r_type = type; //set type
		objCnt++;
		return me;
	}
	
	private static int allocMem(int size)
	{
		int result;
		
		result = brk(0);
		if (result <= 0)
		{
			System.print("Error allocating memory");
			exit(-1);
		}
		size = (size + 0xFFF) & 0xFFFFF000;
		brk(result + size);
		return result;
	}
	
	private static void setMem32(int addr, int cnt, int val)
	{
		MAGIC.inline(0x57);             //push edi
		MAGIC.inline(0xFC);             //cld
		MAGIC.inline(0x8B, 0x7D, 0x10); //mov edi,[ebp+16]
		MAGIC.inline(0x8B, 0x45, 0x08); //mov eax,[ebp+8]
		MAGIC.inline(0x8B, 0x4D, 0x0C); //mov ecx,[ebp+12]
		MAGIC.inline(0xF3, 0xAB);       //rep stosd
		MAGIC.inline(0x5F);             //pop edi
	}
	
	public static rte.SArray newArray(int length, int arrDim, int entrySize, int stdType, Object unitType)
	{
		int scS, rlE;
		rte.SArray me;
		
		scS = MAGIC.getInstScalarSize("SArray");
		rlE = MAGIC.getInstRelocEntries("SArray");
		if (arrDim > 1 || entrySize < 0)
			rlE += length; //objects => rlE will become bigger
		else
			scS += length * entrySize; //entrySize>0 => scS will become bigger
		me = (rte.SArray) newInstance(scS, rlE, MAGIC.clssDesc("SArray"));
		me.length = length;
		me._r_dim = arrDim;
		me._r_stdType = stdType;
		me._r_unitType = unitType;
		return me;
	}
	
	public static void newMultArray(rte.SArray[] parent, int curLevel, int destLevel, int length, int arrDim, int entrySize, int stdType, Object unitType)
	{
		int i;
		
		if (curLevel + 1 < destLevel)
		{ //step down one level
			curLevel++;
			for (i = 0; i < parent.length; i++)
			{
				newMultArray((rte.SArray[]) ((Object) parent[i]), curLevel, destLevel, length, arrDim, entrySize, stdType, unitType);
			}
		}
		else
		{ //create the new entries
			destLevel = arrDim - curLevel;
			for (i = 0; i < parent.length; i++)
			{
				parent[i] = newArray(length, destLevel, entrySize, stdType, unitType);
			}
		}
	}
	
	public static boolean isInstance(Object o, rte.SClassDesc dest, boolean asCast)
	{
		rte.SClassDesc check;
		
		if (o == null)
		{
			if (asCast)
				return true; //null matches all
			return false; //null is not an instance
		}
		check = o._r_type;
		while (check != null)
		{
			if (check == dest)
				return true;
			check = check.parent;
		}
		if (asCast)
			rtError();
		return false;
	}
	
	public static rte.SIntfMap isImplementation(Object o, rte.SIntfDesc dest, boolean asCast)
	{
		rtError();
		return null;
	}
	
	public static boolean isArray(rte.SArray o, int stdType, Object unitType, int arrDim, boolean asCast)
	{
		rte.SClassDesc clss;
		
		//in fact o is of type "Object", _r_type has to be checked below - but this check is faster than "instanceof" and conversion
		if (o == null)
		{
			if (asCast)
				return true; //null matches all
			return false; //null is not an instance
		}
		if (o._r_type != MAGIC.clssDesc("SArray"))
		{ //will never match independently of arrDim
			if (asCast)
				rtError();
			return false;
		}
		if (unitType == MAGIC.clssDesc("SArray"))
		{ //special test for arrays
			if (o._r_unitType == MAGIC.clssDesc("SArray"))
				arrDim--; //an array of SArrays, make next test to ">=" instead of ">"
			if (o._r_dim > arrDim)
				return true; //at least one level has to be left to have an object of type SArray
			if (asCast)
				rtError();
			return false;
		}
		//no specials, check arrDim and check for standard type
		if (o._r_stdType != stdType || o._r_dim < arrDim)
		{ //check standard types and array dimension
			if (asCast)
				rtError();
			return false;
		}
		if (stdType != 0)
			return true; //array of standard-type
		//array of objects, make deep-check for class type
		clss = (rte.SClassDesc) o._r_unitType;
		while (clss != null)
		{
			if (clss == unitType)
				return true;
			clss = clss.parent;
		}
		if (asCast)
			rtError();
		return false;
	}
	
	public static void checkArrayStore(rte.SArray dest, rte.SArray newEntry)
	{
		//in fact newEntry is of type "Object", SArray elements are not valid always - but this is faster for _r_dim>1 which needs SArray parm
		if (dest._r_dim > 1)
			isArray(newEntry, dest._r_stdType, dest._r_unitType, dest._r_dim - 1, true); //check array
		else if (dest._r_unitType == null)
			rtError();
		else
		{
			if (dest._r_unitType._r_type == MAGIC.clssDesc("SClassDesc"))
			{
				isInstance(newEntry, (rte.SClassDesc) dest._r_unitType, true); //check instance
			}
			else
			{
				isImplementation(newEntry, (rte.SIntfDesc) dest._r_unitType, true); //check implementation
			}
		}
	}
	
	public static void rtError()
	{
		System.println("Runtime error");
		exit(-1);
	}
	
	public static void exit(int status)
	{
		MAGIC.inline(0xB8, 0x01, 0x00, 0x00, 0x00); //mov eax,1 (terminate process)
		MAGIC.inline(0x8B, 0x5D, 0x08);             //mov ebx,[ebp+8] (exit code)
		MAGIC.inline(0xCD, 0x80);                   //call kernel
	}
	
	private static int brk(int newEnd)
	{
		int tmp = 0;
		MAGIC.inline(0xB8, 0x2D, 0x00, 0x00, 0x00); //mov eax,45 (brk)
		MAGIC.inline(0x8B, 0x5D, 0x08);             //mov ebx,[ebp+8] (end_data_segment)
		MAGIC.inline(0xCD, 0x80);                   //call kernel
		MAGIC.inline(0x89, 0x45, 0xFC);             //mov [ebp-4],eax
		return tmp;
	}
}