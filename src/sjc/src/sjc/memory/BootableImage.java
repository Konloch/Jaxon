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

import sjc.compbase.Context;
import sjc.compbase.Unit;

/**
 * BootableImage: container of a to be compiled image
 *
 * @author S. Frenz
 * @version 151108 added support for debug lister
 * version 120228 moved getNewOutputLocation to ImageContainer
 * version 120227 cleaned up "package sjc." typo
 * version 101124 moved memBlockLen increase to allow memory access check
 * version 100619 adde call to architecture if headerData is given
 * version 100510 fixed too restrictive allocateArray
 * version 100507 renamed misleading parameters in newArray
 * version 100112 added support for reportInternalMemoryError
 * version 091001 adopted changed memory interface
 * version 090207 added copyright notice
 * version 080202 added support for noSizeScalars-flag
 * version 070920 fixed bug in finalizeImage
 * version 070918 added support for multidimensional arrays
 * version 070729 added image block size check for indirect scalar allocation
 * version 070703 replaced statistic method by a more versatile one
 * version 070628 changed stackClearBits to allocClearBits
 * version 070615 added method to request the indirect part of the last allocated object
 * version 070528 added statistic method
 * version 070526 adopted removal of Context.rteSArray* short-paths
 * version 070506 added support for runtime environment without objects
 * version 070504 added support for self-allocation of ctx.rteSClassDesc without backfixing
 * version 070331 added support for additional indirect scalars in SArray
 * version 070127 optimized access to err-flag
 * version 061222 option to have header replaced
 * version 061220 fixed entering of type.outputAddr
 * version 061217 made header optional
 * version 061128 added enterInitObject
 * version 060902 moved to memory package to support bootstrapped compilation
 * version 060817 removed abstraction to make compression easier
 * version 060817 added version history
 */

public class BootableImage extends ImageContainer
{
	private boolean memBad, standardHeader;
	private boolean debugListerSupportEnabled;
	private OutputLocation firstObj, lastObj, startOfImage;
	private MemoryObjectDebugInfo firstObjDebugInfo, lastObjDebugInfo;
	private Context ctx;
	
	//memory block contains information about the image (fixed by loader, writer, system... all!)
	// off= 0: 32 Bit: ptr: start in memory
	// off= 4: 32 Bit: int: size of memory block
	// off= 8: 32 Bit: ptr: class descriptor address of method to start
	// off=12: 32 Bit: ptr: address of first code byte in method to start
	// off=16: 32 Bit: ptr: address of first object
	// off=20: 32 Bit: ptr: address of RAM-init object if in embedded mode
	// off=24: 32 Bit: int: codeStart
	// off=28: 32 Bit: int: lo to hi: 0xAA relocBytes stackClearBits 0x55 (arch parameters incl. endianess)
	
	public void init(int size, int base, boolean iSH, byte[] headerData, Context ictx)
	{
		int i;
		
		ctx = ictx;
		memBlock = new byte[size];
		baseAddress = base; //this is the offset to which the image should be loaded to
		firstObj = lastObj = null;
		startOfImage = new OutputLocation(base);
		if (standardHeader = iSH)
		{
			memBlockLen = (32 + ctx.arch.allocClearBits) & ~ctx.arch.allocClearBits; //reserve memory for info above
			putInt(startOfImage, 0, base);
			putInt(startOfImage, 28, 0x550000AA | (ctx.arch.allocClearBits << 16) | (ctx.arch.relocBytes << 8));
		}
		else
		{
			if (headerData != null)
			{
				if (headerData.length > memBlock.length)
				{
					ctx.out.print("header is bigger than image container, ");
					memError();
				}
				else
				{
					ctx.arch.setHeaderLength(headerData.length);
					memBlockLen = headerData.length;
					for (i = 0; i < memBlockLen; i++)
						memBlock[i] = headerData[i];
				}
			}
			else
				memBlockLen = 0;
		}
		if (debugListerSupportEnabled)
		{
			lastObjDebugInfo = firstObjDebugInfo = new MemoryObjectDebugInfo();
			firstObjDebugInfo.pointer = base;
			firstObjDebugInfo.relocs = 0;
			firstObjDebugInfo.scalarSize = memBlockLen;
			firstObjDebugInfo.location = startOfImage;
		}
	}
	
	public void finalizeImage(Object startUnitLoc, Object startMthdCodeLoc, int relCodeStart)
	{
		startUnit = getAddrAsInt(startUnitLoc, 0);
		startCode = getAddrAsInt(startMthdCodeLoc, 0) + relCodeStart;
		if (standardHeader)
		{
			putInt(startOfImage, 4, memBlockLen); //enter used size
			putInt(startOfImage, 8, startUnit); //enter and remember startup-unit
			putInt(startOfImage, 12, startCode); //enter and remember address to exec
			putInt(startOfImage, 16, getAddrAsInt(firstObj, 0)); //enter address of first object
			putInt(startOfImage, 24, relCodeStart); //enter relative start of code
		}
	}
	
	public byte[] getInfoBlock(String appendSig)
	{
		byte[] data;
		int i, appLen = 0;
		
		if (appendSig != null)
			appLen = appendSig.length();
		data = new byte[32 + appLen];
		for (i = 0; i < 32; i++)
			data[i] = memBlock[i];
		for (i = 0; i < appLen; i++)
			data[i + 32] = (byte) appendSig.charAt(i);
		return data;
	}
	
	public Object allocate(int scalarSize, int indirScalarSize, int relocEntries, Object typeLoc)
	{
		int relocSize, size;
		OutputLocation ret = null;
		MemoryObjectDebugInfo modi = null;
		
		if (memBad)
			return null;
		relocSize = relocEntries * ctx.arch.relocBytes;
		size = (relocSize + scalarSize + ctx.arch.allocClearBits) & ~ctx.arch.allocClearBits;
		if (memBlockLen + size > memBlock.length)
		{ //no space left
			memError();
			return null;
		}
		ret = new OutputLocation(memBlockLen + relocSize + baseAddress); //calculate address
		memBlockLen += size; //increase "used memory"
		if (!streamObjects)
		{
			if (lastObj != null)
				ctx.arch.putRef(lastObj, -2 * ctx.arch.relocBytes, ret, 0); //enter in next-pointer of last object
			else
				firstObj = ret;
			if (!noSizeScalars)
			{
				putInt(ret, 0, relocEntries); //remember relocSize on first scalar
				putInt(ret, 4, scalarSize); //remember scalarSize on second scalar
			}
			lastObj = ret; //remember for next allocate
		}
		if (indirScalarSize != 0)
		{
			indirScalarSize = (indirScalarSize + ctx.arch.allocClearBits) & ~ctx.arch.allocClearBits;
			if (memBlockLen + indirScalarSize > memBlock.length)
			{ //no space left
				memError();
				return null;
			}
			putInt(ret, ctx.indirScalarSizeOff, indirScalarSize); //remember indirScalarSize
			ret.indirObj = new OutputLocation(baseAddress + memBlockLen);
			ctx.arch.putRef(ret, ctx.indirScalarAddrOff, ret.indirObj, 0); //remember indirScalarAddr
			memBlockLen += indirScalarSize;
		}
		//enter type
		if (typeLoc != null)
			ctx.arch.putRef(ret, -ctx.arch.relocBytes, typeLoc, 0);
		//statistics and debugging info
		objectsAllocated++;
		if (debugListerSupportEnabled)
		{
			modi = new MemoryObjectDebugInfo();
			modi.location = ret;
			modi.relocs = relocEntries;
			modi.pointer = ret.address;
			modi.scalarSize = size - relocSize;
			lastObjDebugInfo.next = modi;
			lastObjDebugInfo = modi;
		}
		return ret;
	}
	
	public void allocationDebugHint(Object source)
	{
		if (!debugListerSupportEnabled || lastObjDebugInfo == null)
			return;
		lastObjDebugInfo.source = source;
	}
	
	private void memError()
	{
		ctx.out.println("no space left in memory block");
		memBad = ctx.err = true;
	}
	
	public Object allocateArray(int entries, int dim, int entrySize, int stdType, Object extTypeLoc)
	{
		Object obj = null;
		Unit array = ctx.rteSArray;
		
		if (dim > 1 || entrySize < 0)
		{
			obj = allocate(array.instScalarTableSize, array.instIndirScalarTableSize, array.instRelocTableEntries + entries * ctx.arch.relocBytes, array.outputLocation);
		}
		else
		{
			if (stdType == 0)
				return null; //positive entrySize requires stdType to be set
			if (ctx.indirScalars)
				obj = allocate(array.instScalarTableSize, array.instIndirScalarTableSize + entries * entrySize, array.instRelocTableEntries, array.outputLocation);
			else
				obj = allocate(array.instScalarTableSize + entries * entrySize, 0, array.instRelocTableEntries, array.outputLocation);
		}
		if (obj == null)
			return null; //allocation failed
		putInt(obj, ctx.rteSArrayLength, entries);
		putInt(obj, ctx.rteSArrayDim, dim);
		putInt(obj, ctx.rteSArrayStd, stdType);
		ctx.arch.putRef(obj, ctx.rteSArrayExt, extTypeLoc, 0);
		return obj;
	}
	
	public void enterInitObject(Object loc)
	{
		if (standardHeader)
			putInt(startOfImage, 20, getAddrAsInt(loc, 0));
	}
	
	public int getCurrentAllocAmountHint()
	{
		return memBlockLen;
	}
	
	protected void reportInternalMemoryError()
	{
		ctx.out.println("invalid memory access");
		ctx.err = true;
	}
	
	public void enableDebugListerSupport()
	{
		debugListerSupportEnabled = true;
	}
	
	public MemoryObjectDebugInfo getFirstDebugListerObject()
	{
		return firstObjDebugInfo;
	}
}
