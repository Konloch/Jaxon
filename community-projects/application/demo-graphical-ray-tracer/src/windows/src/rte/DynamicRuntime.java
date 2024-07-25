package rte;

/**
 * @author S. Frenz
 */
public class DynamicRuntime
{
	public final static int _hndStdIn = MAGIC.imageBase - 512 + 0; //0x0401000;
	public final static int _hndStdOut = MAGIC.imageBase - 512 + 4; //0x0401004;
	public final static int _hndErrOut = MAGIC.imageBase - 512 + 8; //0x0401008;
	public final static int _ptrParam = MAGIC.imageBase - 512 + 12; //0x040100C;
	public final static int _cntParam = MAGIC.imageBase - 512 + 16; //0x0401010;
	
	public final static int _Kernel_GetStdHandle = MAGIC.imageBase - 512 + 20; //0x0401014;
	public final static int _Kernel_GetCommandLineW = MAGIC.imageBase - 512 + 24; //0x0401018;
	public final static int _Kernel_ExitProcess = MAGIC.imageBase - 512 + 28; //0x040101C;
	public final static int _Kernel_WriteFile = MAGIC.imageBase - 512 + 32; //0x0401020;
	public final static int _Kernel_ReadFile = MAGIC.imageBase - 512 + 36; //0x0401024;
	public final static int _Kernel_CreateFileA = MAGIC.imageBase - 512 + 40; //0x0401028;
	public final static int _Kernel_GetFileSize = MAGIC.imageBase - 512 + 44; //0x040102C;
	public final static int _Kernel_CloseHandle = MAGIC.imageBase - 512 + 48; //0x0401030;
	public final static int _Kernel_LoadLibraryA = MAGIC.imageBase - 512 + 52; //0x0401034;
	public final static int _Kernel_GetProcAddress = MAGIC.imageBase - 512 + 56; //0x0401038;
	public final static int _Kernel_VirtualAlloc = MAGIC.imageBase - 512 + 60; //0x040103C;
	public final static int _Kernel_VirtualFree = MAGIC.imageBase - 512 + 64; //0x0401040;
	
	public final static int _Shell_CommandLineToArgvW = MAGIC.imageBase - 512 + 72; //0x0401048;
	
	public static int objCnt;
	
	private final static int MEMBLOCK = 1024 * 1024; //1 MB per block
	private final static int BLOCKMAX = 64 * 1024;   //allocate objects up to 64 KB in memblock
	private static int nextFreeAddr, memFree;
	
	public static Object newInstance(int scalarSize, int relocEntries, SClassDesc type)
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
		me = MAGIC.cast2Obj(addr + rs); //place object
		me._r_type = type; //set type
		objCnt++;
		return me;
	}
	
	private static int allocMem(int size)
	{
		int result = 0;
		
		MAGIC.inline(0x6A, 0x40);                   //push flProtect (READ_WRITE)
		MAGIC.inline(0x68);
		MAGIC.inline32(0x1000); //push flAllocationType (MEM_COMMIT)
		MAGIC.inline(0xFF, 0x75);
		MAGIC.inlineOffset(1, size); //push [ebp+8] (size)
		MAGIC.inline(0x6A, 0x00);                   //push lpAddress (0)
		MAGIC.inline(0xFF, 0x15);
		MAGIC.inline32(_Kernel_VirtualAlloc); //call
		MAGIC.inline(0x89, 0x45);
		MAGIC.inlineOffset(1, result); //mov [ebp-4],eax
		if (result == 0)
			rtError();
		return result;
	}
	
	public static SArray newArray(int length, int arrDim, int entrySize, int stdType, Object unitType)
	{
		int scS, rlE;
		SArray me;
		
		scS = MAGIC.getInstScalarSize("SArray");
		rlE = MAGIC.getInstRelocEntries("SArray");
		if (arrDim > 1 || entrySize < 0)
			rlE += length; //objects => rlE will become bigger
		else
			scS += length * entrySize; //entrySize>0 => scS will become bigger
		me = (SArray) newInstance(scS, rlE, MAGIC.clssDesc("SArray"));
		me.length = length;
		me._r_dim = arrDim;
		me._r_stdType = stdType;
		me._r_unitType = unitType;
		return me;
	}
	
	public static void newMultArray(SArray[] parent, int curLevel, int destLevel, int length, int arrDim, int entrySize, int stdType, Object unitType)
	{
		int i;
		
		if (curLevel + 1 < destLevel)
		{ //step down one level
			curLevel++;
			for (i = 0; i < parent.length; i++)
			{
				newMultArray((SArray[]) ((Object) parent[i]), curLevel, destLevel, length, arrDim, entrySize, stdType, unitType);
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
	
	public static boolean isInstance(Object o, SClassDesc dest, boolean asCast)
	{
		SClassDesc check;
		
		if (o == null)
		{
			return asCast; //null matches all
			//null is not an instance
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
	
	public static SIntfMap isImplementation(Object o, SIntfDesc dest, boolean asCast)
	{
		rtError();
		return null;
	}
	
	public static boolean isArray(SArray o, int stdType, Object unitType, int arrDim, boolean asCast)
	{
		SClassDesc clss;
		
		//in fact o is of type "Object", _r_type has to be checked below - but this check is faster than "instanceof" and conversion
		if (o == null)
		{
			return asCast; //null matches all
			//null is not an instance
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
		clss = (SClassDesc) o._r_unitType;
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
	
	public static void checkArrayStore(SArray dest, SArray newEntry)
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
				isInstance(newEntry, (SClassDesc) dest._r_unitType, true); //check instance
			}
			else
			{
				isImplementation(newEntry, (SIntfDesc) dest._r_unitType, true); //check implementation
			}
		}
	}
	
	public static void rtError()
	{
		MAGIC.inline(0x6A, 0xFF);                            //push byte -1 (error code)
		MAGIC.inline(0xFF, 0x15);
		MAGIC.inline32(_Kernel_ExitProcess); //call
	}
}
