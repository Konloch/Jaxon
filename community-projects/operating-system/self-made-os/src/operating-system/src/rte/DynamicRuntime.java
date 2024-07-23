package rte;

import hardware.Serial;
import kernel.BIOS;

public class DynamicRuntime
{
	private static SEmptyObject firstEObj = null;
	private static SEmptyObject lastEObj = null;
	private static Object lastRootsetObject = null;
	private static final int FIRST_OBJECT_IMG_BASE_OFFSET = 16;
	private static final int BIOS_MEMSEG_LEN_OFFSET = 8;
	private static final int BIOS_MEMSEG_TYPE_OFFSET = 16;
	private static final int SJC_OBJECT_ADDR = MAGIC.imageBase + FIRST_OBJECT_IMG_BASE_OFFSET;
	
	private static void saveLastRootsetObject()
	{
		Object obj = MAGIC.cast2Obj(MAGIC.rMem32(SJC_OBJECT_ADDR));
		while (obj._r_next != null)
		{
			obj = obj._r_next;
		}
		lastRootsetObject = obj;
	}
	
	public static void initializeEmptyObjects()
	{
		//save last rootset obj for gc later
		saveLastRootsetObject();
		//get memsegs from bios and evaluate if it is free
		int conIndex = 0;
		do
		{
			BIOS.writeMemMapToBuff(conIndex);
			conIndex = BIOS.regs.EBX;
			
			//if segment is free, allocate empty object
			int memSegType = MAGIC.rMem32(BIOS.MEM_READ_BUF + BIOS_MEMSEG_TYPE_OFFSET);
			if (memSegType == 1)
			{
				int baseAdd = (int) MAGIC.rMem64(BIOS.MEM_READ_BUF);
				//dont touch any addresses in bios addr room, we don't know what exactly lies there
				if (baseAdd > BIOS.BIOS_ADDR_MAX)
				{
					//TODO: check if we even have enough space for the emptyObj + another basic object
					int len = (int) MAGIC.rMem64(BIOS.MEM_READ_BUF + BIOS_MEMSEG_LEN_OFFSET);
					//if len is less or exactly the size of an empty object, don't even bother creating an object
					if (len <= MAGIC.getInstScalarSize("SEmptyObject") + MAGIC.getInstRelocEntries("SEmptyObject") * 4)
					{
						continue;
					}
					int maxAddress = baseAdd + len - 1;
					//2 cases: either the imageBase object lies in the memory segment, in which case we have to
					//take the object and chase the r_next chain until we get to the end of it
					//or we get a memory segment which doesn't contain it, and then we have to be careful to not f up
					//the r_next chain that must point to the new eObj
					if (MAGIC.rMem32(SJC_OBJECT_ADDR) >= baseAdd && MAGIC.rMem32(SJC_OBJECT_ADDR) <= maxAddress)
					{ //sjc obj in segment
						//get imageBase first object and go through the next pointer until we find no more objects
						Object obj = MAGIC.cast2Obj(MAGIC.rMem32(SJC_OBJECT_ADDR));
						while (obj._r_next != null)
						{
							obj = obj._r_next;
						}
						
						int nextFreeAddress = MAGIC.cast2Ref(obj) + obj._r_scalarSize;
						for (int i = nextFreeAddress; i < nextFreeAddress + MAGIC.getInstScalarSize("SEmptyObject"); i++)
						{
							MAGIC.wMem8(i, (byte) 0);
						}
						//we add ?? bytes to make space for the ? relocs it contains
						nextFreeAddress += MAGIC.getInstRelocEntries("SEmptyObject") * MAGIC.ptrSize;
						
						//nextFreeAddress is now the correct address to allocate the object at
						Object eObj = MAGIC.cast2Obj(nextFreeAddress);
						//we set relocsEntries, type and scalarsize
						MAGIC.assign(eObj._r_relocEntries, MAGIC.getInstRelocEntries("SEmptyObject"));
						MAGIC.assign(eObj._r_type, MAGIC.clssDesc("SEmptyObject"));
						//scalarSize is the maximum address minus the current address plus 1
						MAGIC.assign(eObj._r_scalarSize, maxAddress - nextFreeAddress + 1);
						
						//set old objects next pointer to eObj
						MAGIC.assign(obj._r_next, (Object) eObj);
						
						//remember the first eObj we set so it can point to future eObjs
						if (firstEObj == null)
						{
							firstEObj = (SEmptyObject) eObj;
						}
						else
						{
							SEmptyObject temp = firstEObj;
							while (temp.nextEmptyObject != null)
							{
								temp = temp.nextEmptyObject;
							}
							MAGIC.assign(temp.nextEmptyObject, (SEmptyObject) eObj);
							//the eObj in the segment before the sjc segment needs to point to the first sjc object
							MAGIC.assign(temp._r_next, MAGIC.cast2Obj(SJC_OBJECT_ADDR));
							MAGIC.assign(((SEmptyObject) eObj).prevEmptyObject, temp);
						}
						lastEObj = (SEmptyObject) eObj;
					}
					else
					{ //no sjc objs in segment
						//because there shouldn't be an preexisting objects in this segment, we can just use the baseAddress
						//of the segment as the baseAddress of our eObj
						int nextFreeAddress = baseAdd;
						for (int i = nextFreeAddress; i < nextFreeAddress + MAGIC.getInstScalarSize("SEmptyObject"); i++)
						{
							MAGIC.wMem8(i, (byte) 0);
						}
						//we add 12 bytes to make space for the 3 relocs it contains
						nextFreeAddress += 12;
						Object eObj = MAGIC.cast2Obj(nextFreeAddress);
						//we set relocsEntries, type and scalarsize
						MAGIC.assign(eObj._r_relocEntries, 3);
						MAGIC.assign(eObj._r_type, MAGIC.clssDesc("SEmptyObject"));
						//scalarSize is the maximum address minus the current address plus 1
						MAGIC.assign(eObj._r_scalarSize, maxAddress - nextFreeAddress + 1);
						
						//we have our object, now we must correct the r_next hierarchy
						//if there is no firstEObj, we must once again follow the sjc object pointer in the base image
						//otherwise, we can just point the last eObj in the hierarchy of firstEObj to this new eObj,
						//because it is guaranteed to be the last object in the overall object hierarchy
						if (firstEObj == null)
						{
							firstEObj = (SEmptyObject) eObj;
							//get base image object
							Object obj = MAGIC.cast2Obj(MAGIC.rMem32(SJC_OBJECT_ADDR));
							while (obj._r_next != null)
							{
								obj = obj._r_next;
							}
							MAGIC.assign(obj._r_next, eObj);
						}
						else
						{
							//get first eobj
							SEmptyObject temp = firstEObj;
							//traverse hierarchy
							while (temp.nextEmptyObject != null)
							{
								temp = temp.nextEmptyObject;
							}
							MAGIC.assign(temp.nextEmptyObject, (SEmptyObject) eObj);
							MAGIC.assign(temp._r_next, eObj);
							MAGIC.assign(((SEmptyObject) eObj).prevEmptyObject, temp);
						}
						lastEObj = (SEmptyObject) eObj;
					}
					
				}
			}
		} while (conIndex != 0);
	}
	
	public static Object newInstance(int scS, int rlE, SClassDesc type)
	{
		/*
		int objPtr = MAGIC.imageBase;
		//offset für ptr zum nächsten Objekt
		objPtr+=16;
		//cast ptr to obj
		Object ob = MAGIC.cast2Obj(objPtr);
		//TODO: this case should never happen?
		if (ob == null) {
			while(true);
		}
		while(ob._r_next != null) {
			//traverse objects
			ob = ob._r_next;
		}
		//return to pointer and add scalar size after last object
		objPtr = MAGIC.cast2Ref(ob)+ob._r_scalarSize;
		//align to 4
		if(objPtr % 4 != 0){
			objPtr += 4 - (objPtr % 4);
		}
		for(int i = objPtr; i<objPtr+scS+rlE*4+4;i++) {
			MAGIC.wMem8(i, (byte)0); //initialize with 0
		}
		objPtr += rlE*4;//offset object pointer to make space for the relocs
		Object newOb = MAGIC.cast2Obj(objPtr);//we now have the correct address for the new object in objPtr
		MAGIC.assign(ob._r_next, newOb);
		MAGIC.assign(newOb._r_relocEntries, rlE);
		MAGIC.assign(newOb._r_scalarSize, scS);
		MAGIC.assign(newOb._r_type, type);
		return newOb;*/
		
		//find emptyobject that can fit new instance
		SEmptyObject eObj = firstEObj;
		boolean perfectFit = false;
		scS = (scS + 3) & ~3;
		//TODO: hier stand +16, YTF?!?
		int newObjLen = scS + 4 * rlE;
		while (true)
		{
			//object fits in free memory in EmptyObject
			if ((eObj._r_scalarSize - MAGIC.getInstScalarSize("SEmptyObject")) >= newObjLen)
			{
				break;
			}
			else if ((eObj._r_scalarSize + eObj._r_relocEntries * MAGIC.ptrSize) == newObjLen)
			{//object fits EXACTLY into the eObj
				perfectFit = true;
				break;
			}
			if (eObj.nextEmptyObject == null)
			{
				MAGIC.inline(0xCC); //out of memory
			}
			eObj = eObj.nextEmptyObject; //traverse
		}
		
		//TODO: implement eObj is kil for when we can remove the eObj to fit (?? bytes per eObj!)
		//calculate new address and subtract object size from eObj scalarsize
		int newObjAddr = MAGIC.cast2Ref(eObj) + eObj._r_scalarSize - scS;
		Object newObjNext = eObj._r_next;
		Object eObjPointee = null;
		if (!perfectFit)
		{
			MAGIC.assign(eObj._r_scalarSize, eObj._r_scalarSize - newObjLen);
		}
		else
		{
			//we have to correct the eObj pointer list, because after nulling the memory all our relocs are gone
			if (eObj.prevEmptyObject != null)
				MAGIC.assign(eObj.prevEmptyObject.nextEmptyObject, eObj.nextEmptyObject);
			if (eObj.nextEmptyObject != null)
				MAGIC.assign(eObj.nextEmptyObject.prevEmptyObject, eObj.prevEmptyObject);
			eObjPointee = MAGIC.cast2Obj(MAGIC.rMem32(SJC_OBJECT_ADDR));
			while (eObjPointee._r_next != (Object) eObj)
			{
				eObjPointee = eObjPointee._r_next;
			}
		}
		/*
		Serial.print('+');
		Serial.print(eObj._r_scalarSize);
		Serial.print(',');
		Serial.print(newObjLen);
		Serial.print(';');
		Serial.print(scS);
		Serial.print(',');
		Serial.print(rlE);
		Serial.print('\n');*/
		//the address from which we have to start nulling memory
		for (int i = newObjAddr - (rlE * MAGIC.ptrSize); i < newObjAddr + scS; i++)
		{
			MAGIC.wMem8(i, (byte) 0);
		}
		
		//assign the actual object
		Object newOb = MAGIC.cast2Obj(newObjAddr);
		MAGIC.assign(newOb._r_relocEntries, rlE);
		MAGIC.assign(newOb._r_scalarSize, scS);
		MAGIC.assign(newOb._r_type, type);
		
		//correct next pointers
		MAGIC.assign(newOb._r_next, newObjNext);
		if (!perfectFit)
			MAGIC.assign(eObj._r_next, newOb);
		else
			MAGIC.assign(eObjPointee._r_next, newOb);
		return newOb;
	}
	
	//creates a new array object
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
	
	//create a new multi level array
	//aus dem Handbuch kopiert :/
	public static void newMultArray(SArray[] parent, int curLevel, int destLevel, int length, int arrDim, int entrySize, int stdType, Object unitType)
	{
		int i; //temporäre Variable
		if (curLevel + 1 < destLevel)
		{ //es folgt noch mehr als eine Dimension
			curLevel++; //aktuelle Dimension erhöhen
			for (i = 0; i < parent.length; i++) //jedes Element mit Array befüllen
				newMultArray((SArray[]) ((Object) parent[i]), curLevel, destLevel, length, arrDim, entrySize, stdType, unitType);
		}
		else
		{ //letzte anzulegende Dimension
			destLevel = arrDim - curLevel; //Zieldimension eines Elementes
			for (i = 0; i < parent.length; i++) //jedes Element mit Zieltyp befüllen
				parent[i] = newArray(length, destLevel, entrySize, stdType, unitType);
		}
	}
	
	//aus dem Handbuch kopiert :/
	public static boolean isInstance(Object o, SClassDesc dest, boolean asCast)
	{
		SClassDesc check; //temporäre Variable
		if (o == null)
		{ //Prüfung auf null
			if (asCast)
				return true; //null darf immer konvertiert werden
			return false; //null ist keine Instanz
		}
		check = o._r_type; //für weitere Vergleiche Objekttyp ermitteln
		while (check != null)
		{ //suche passende Klasse
			if (check == dest)
				return true; //passende Klasse gefunden
			check = check.parent; //Elternklasse versuchen
		}
		if (asCast)
			MAGIC.inline(0xCC); //Konvertierungsfehler
		return false; //Objekt passt nicht zu Klasse
	}
	
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
	
	public static boolean isArray(SArray o, int stdType, SClassDesc clssType, int arrDim, boolean asCast)
	{
		SClassDesc clss;
		
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
	
	public static void checkArrayStore(SArray dest, SArray newEntry)
	{
		if (dest._r_dim > 1)
			isArray(newEntry, dest._r_stdType, (SClassDesc) dest._r_unitType, dest._r_dim - 1, true);
		else if (dest._r_unitType == null)
			MAGIC.inline(0xCC);
		else
			isInstance(newEntry, (SClassDesc) dest._r_unitType, true);
	}
	
	public static void collectGarbage()
	{
		Object rootObj = MAGIC.cast2Obj(MAGIC.rMem32(SJC_OBJECT_ADDR));
		if (!isInstance(rootObj, (SClassDesc) MAGIC.clssDesc("Object"), false))
			MAGIC.inline(0xCC);
		markGarbage(rootObj);
		sweepGarbage(rootObj);
	}
	
	private static void markGarbage(Object rootObj)
	{
		markRelocs(rootObj, 1);
		do
		{
			//mark shit
			//mark all relocs it has
			rootObj = rootObj._r_next;
			markRelocs(rootObj, 1);
			//mark the object itself
			//advance to next rootset obj
		} while (rootObj != lastRootsetObject);
	}
	
	private static void markRelocs(Object obj, int d)
	{
		if (obj == null)
			return;
		if (obj._r_used == 1)
			return;
		MAGIC.assign(obj._r_used, 1);
		int baseAddr = MAGIC.cast2Ref(obj);
		baseAddr -= MAGIC.ptrSize;
		for (int i = 2; i < obj._r_relocEntries; i++)
		{
			int addr = MAGIC.rMem32(baseAddr - i * MAGIC.ptrSize);
			if (addr == 0)
			{
				continue;
			}
			Object o = MAGIC.cast2Obj(addr);
			//sanity check if o is an object, then call recursively on o
			if (!isInstance(o, (SClassDesc) MAGIC.clssDesc("Object"), false))
				continue;
			markRelocs(o, d + 1);
		}
	}
	
	private static void sweepGarbage(Object obj)
	{
		SEmptyObject lastSeenEmptyObject = null;
		Object prevObject = null;
		Object nextObject;
		if (obj == null)
			MAGIC.inline(0xCC);
		while (obj != null)
		{
			nextObject = obj._r_next;
			if (isInstance(obj, (SClassDesc) MAGIC.clssDesc("SEmptyObject"), false))
			{
				lastSeenEmptyObject = (SEmptyObject) obj;
			}
			else if (obj._r_used == 0)
			{
				int freeMem = obj._r_relocEntries * MAGIC.ptrSize + obj._r_scalarSize;
				//deletion because unused
				//check if emptyObject even fits
				if (freeMem < MAGIC.getInstScalarSize("SEmptyObject") + MAGIC.getInstRelocEntries("SEmptyObject") * MAGIC.ptrSize)
				{
					//TODO: check if prev object is eObj and make bigger or check if we can combine with following object
					if (isInstance(prevObject, (SClassDesc) MAGIC.clssDesc("SEmptyObject"), false))
					{
						//previous object is an empty object, so instead of becoming one ourselves we just tell it to expand
						//only thing we need to correct are _next_ pointers
						SEmptyObject eObj = (SEmptyObject) prevObject;
						Serial.print(" fast-delete:");
						Serial.print(obj._r_type.name);
						MAGIC.assign(eObj._r_scalarSize, eObj._r_scalarSize + freeMem);
						MAGIC.assign(eObj._r_next, obj._r_next);
						obj = prevObject;
					}
				}
				else
				{//eObj fits in hole
					int objBaseAddr = MAGIC.cast2Ref(obj);
					objBaseAddr -= obj._r_relocEntries * MAGIC.ptrSize;
					int eObjAddr = objBaseAddr + MAGIC.getInstRelocEntries("SEmptyObject") * MAGIC.ptrSize;
					//null memory
					for (int i = objBaseAddr; i < eObjAddr + MAGIC.getInstScalarSize("SEmptyObject"); i++)
					{
						MAGIC.wMem8(i, (byte) 0);
					}
					Object eObj = MAGIC.cast2Obj(eObjAddr);
					MAGIC.assign(eObj._r_type, MAGIC.clssDesc("SEmptyObject"));
					MAGIC.assign(eObj._r_relocEntries, MAGIC.getInstRelocEntries("SEmptyObject"));
					MAGIC.assign(eObj._r_scalarSize, freeMem - MAGIC.getInstRelocEntries("SEmptyObject") * MAGIC.ptrSize);
					//normal pointers
					if (prevObject != null)
						MAGIC.assign(prevObject._r_next, eObj);
					MAGIC.assign(eObj._r_next, nextObject);
					//eObj pointers
					MAGIC.assign(((SEmptyObject) eObj).prevEmptyObject, lastSeenEmptyObject);
					if (lastSeenEmptyObject != null)
					{
						MAGIC.assign(((SEmptyObject) eObj).nextEmptyObject, lastSeenEmptyObject.nextEmptyObject);
						MAGIC.assign(lastSeenEmptyObject.nextEmptyObject, (SEmptyObject) eObj);
						if (((SEmptyObject) eObj).nextEmptyObject != null)
							MAGIC.assign(((SEmptyObject) eObj).nextEmptyObject.prevEmptyObject, (SEmptyObject) eObj);
					}
					else
					{
						MAGIC.assign(((SEmptyObject) eObj).nextEmptyObject, firstEObj);
						MAGIC.assign(firstEObj.prevEmptyObject, (SEmptyObject) eObj);
						firstEObj = (SEmptyObject) eObj;
					}
					lastSeenEmptyObject = (SEmptyObject) eObj;
				}
			}
			else
			{
				MAGIC.assign(obj._r_used, 0);
				prevObject = obj;
			}
			obj = nextObject;
		}
	}
}