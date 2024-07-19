/* Copyright (C) 2006, 2007, 2008, 2009, 2010, 2012, 2015 Stefan Frenz
 *
 * This file is part of SJC, the Small Java Compiler written by Stefan Frenz.
 *
 * SJC is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * SJC is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with SJC. If not, see <http://www.gnu.org/licenses/>.
 */

package sjc.memory;

import sjc.osio.BinWriter;

/**
 * MemoryImage: memory abstraction of the target system
 *
 * @author S. Frenz
 * @version 151108 added support for DebugLister
 * version 120228 renamed getNewOutputObject to getStructOutputObject as structs need to be distinguishable from normal output objects
 * version 101122 added checkMemoryLocation
 * version 100505 added putByteArray
 * version 091001 publish only Objects instead of addresses
 * version 090301 moved allocateMultiArray to Context
 * version 090207 added copyright notice
 * version 080706 added alignBlock
 * version 080616 added allocateMultiArray
 * version 080202 added flag for removal of scalars in objects (kind of streamObjects, needed for alternate newInstance)
 * version 071215 removed copyVal and getByte as they are no longer needed
 * version 070703 replaced statistic method by a more versatile one
 * version 070615 added method to request the indirect part of the last allocated object
 * version 070528 added statistic method
 * version 070504 adopted change of naming and semantic in finalizeImage
 * version 070303 added support for movable indirect scalars
 * version 061128 added enterInitObject
 * version 060902 initial version
 */

public abstract class MemoryImage
{
	public boolean streamObjects, noSizeScalars;
	public int objectsAllocated;
	
	public abstract boolean checkMemoryLocation(Object loc, int offset, int len);
	
	public abstract int getAddrAsInt(Object loc, int offset);
	
	public abstract long getAddrAsLong(Object loc, int offset);
	
	public abstract void putByte(Object loc, int offset, byte val);
	
	public abstract void putShort(Object loc, int offset, short val);
	
	public abstract void putInt(Object loc, int offset, int val);
	
	public abstract void putLong(Object loc, int offset, long val);
	
	public abstract int getBaseAddress();
	
	protected abstract boolean appendImage(BinWriter w);
	
	public abstract void appendImagePart(BinWriter fw, Object loc, int startOff, int size);
	
	public void enableDebugListerSupport()
	{
		//default: debug lister not supported
	}
	
	public MemoryObjectDebugInfo getFirstDebugListerObject()
	{
		//default: debug lister not supported
		return null;
	}
	
	public void putByteArray(Object loc, int offset, byte[] arr, int size)
	{
		int i = 0;
		//copy each single byte, should be overwritten for better performance
		while (i < size)
			putByte(loc, offset++, arr[i++]);
	}
	
	public Object allocate(int scalarSize, int indirScalarSize, int relocEntries, Object typeLoc)
	{
		//not all children have to implement this, return "no object allocated"
		return null;
	}
	
	public Object allocateArray(int entries, int dim, int entrySize, int stdType, Object extTypeLoc)
	{
		//not all children have to implement this, return "no array allocated"
		return null;
	}
	
	public void allocationDebugHint(Object source)
	{
		//not all children have to implement this, do nothing
	}
	
	public void finalizeImage(Object unitLoc, Object mthdLoc, int relCodeStart)
	{
		//not all children have to implement this, do nothing
	}
	
	public void enterInitObject(Object loc)
	{
		//not all children have to implement this, do nothing
	}
	
	public Object getStructOutputObject(Object loc, int offset)
	{
		//not all children have to implement this, do nothing
		return null;
	}
	
	public Object getIndirScalarObject(Object loc)
	{
		//not all children have to implement this, return "no indirect address"
		return null;
	}
	
	public int getCurrentAllocAmountHint()
	{
		//not all children have to implement this, return "nothing allocated"
		return 0;
	}
	
	public void alignBlock(int alignment)
	{
		//not all children have to implement this
	}
}
