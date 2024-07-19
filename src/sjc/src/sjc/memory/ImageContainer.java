/* Copyright (C) 2006, 2007, 2008, 2009, 2010, 2012 Stefan Frenz
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
 * ImageContainer: abstract container of an image that should be written to disk
 *
 * @author S. Frenz
 * @version 120228 got getNewOutputObject from BootableImage and renamed it to getStructOutputObject to adopt changed MemoryInterface
 * version 101122 fixed memory image bound check
 * version 100505 fixed memory image bound check, simplified getCRC, overwrote added putByteArray for better performance
 * version 100112 added support for reportInternalMemoryError
 * version 091102 added null-check in getAddrLong (now valid even with relocation)
 * version 091001 adopted changed memory interface
 * version 090207 added copyright notice
 * version 080706 added alignBlock
 * version 071215 removed getByte as it is no longer needed
 * version 060902 initial version
 */

public class ImageContainer extends MemoryImage
{
	private long relocation;
	
	protected static class Location
	{
		protected int address;
		
		protected Location(int ia)
		{
			address = ia;
		}
	}
	
	protected static class OutputLocation extends Location
	{
		protected OutputLocation indirObj;
		
		protected OutputLocation(int ia)
		{
			super(ia);
		}
	}
	
	protected static class StructLocation extends Location
	{
		protected StructLocation(int ia)
		{
			super(ia);
		}
	}
	
	public byte[] memBlock;
	public int memBlockLen;
	public int baseAddress, startUnit, startCode;
	
	public void setRelocation(long newRelocation)
	{
		relocation = newRelocation;
	}
	
	public void alignBlock(int alignmentMask)
	{
		memBlockLen = (memBlockLen + alignmentMask) & ~alignmentMask;
	}
	
	public boolean checkMemoryLocation(Object loc, int offset, int len)
	{
		int addr = getAddrAsInt(loc, offset) - baseAddress;
		return addr >= 0 && addr + len - 1 < memBlockLen;
	}
	
	public void putByte(Object loc, int offset, byte val)
	{
		putValue(getAddrAsInt(loc, offset) - baseAddress, val, 1);
	}
	
	public void putByteArray(Object loc, int offset, byte[] code, int size)
	{
		int addr = getAddrAsInt(loc, offset) - baseAddress, i = 0;
		if (addr < 0 || addr + size > memBlockLen)
			reportInternalMemoryError();
		else
			while (i < size)
				memBlock[addr++] = code[i++];
	}
	
	public void putShort(Object loc, int offset, short val)
	{
		putValue(getAddrAsInt(loc, offset) - baseAddress, val, 2);
	}
	
	public void putInt(Object loc, int offset, int val)
	{
		putValue(getAddrAsInt(loc, offset) - baseAddress, val, 4);
	}
	
	public void putLong(Object loc, int offset, long val)
	{
		putInt(loc, offset, (int) val);
		putInt(loc, offset + 4, (int) (val >>> 32));
	}
	
	public int getAddrAsInt(Object loc, int offset)
	{
		return loc == null ? offset : ((Location) loc).address + offset;
	}
	
	public long getAddrAsLong(Object loc, int offset)
	{
		return loc == null ? (long) offset : (long) ((Location) loc).address + (long) offset + relocation;
	}
	
	public Object getIndirScalarObject(Object loc)
	{
		return loc instanceof OutputLocation ? ((OutputLocation) loc).indirObj : null;
	}
	
	public Object getStructOutputObject(Object loc, int offset)
	{
		return new StructLocation(getAddrAsInt(loc, offset));
	}
	
	public int getBaseAddress()
	{
		return baseAddress;
	}
	
	public boolean appendImage(BinWriter w)
	{
		return w.write(memBlock, 0, memBlockLen);
	}
	
	public void appendImagePart(BinWriter fw, Object loc, int startOff, int size)
	{
		if (size == 0)
			return;
		fw.write(memBlock, ((OutputLocation) loc).address + startOff - baseAddress, size);
	}
	
	public int getCRC(int poly, int blockSize)
	{
		int i, j, stop, crc = -1;
		
		stop = (memBlockLen + --blockSize) & ~blockSize;
		for (i = 0; i < stop; i++)
		{
			if (i < memBlockLen)
				crc ^= ((int) memBlock[i]) & 0xFF;
			for (j = 0; j < 8; j++)
			{
				if ((crc & 1) != 0)
					crc = (crc >>> 1) ^ poly;
				else
					crc = crc >>> 1;
			}
		}
		return crc;
	}
	
	protected void reportInternalMemoryError()
	{
		//only for debugging, text out will be done in BootableImage
	}
	
	private void putValue(int index, int value, int bytes)
	{
		if (index < 0 || index + bytes > memBlockLen)
			reportInternalMemoryError();
		else
			while (bytes-- > 0)
			{
				memBlock[index++] = (byte) value;
				value = value >>> 8;
			}
	}
}
