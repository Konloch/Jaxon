/* Copyright (C) 2006, 2007, 2008, 2009 Stefan Frenz and Patrick Schmidt
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

package sjc.emulation;

/**
 * Class representing a basic RAM memory block including byte order handling
 *
 * @author Patrick Schmidt
 * @version 090207 added copyright notice
 * version 060608 initial version
 */
public class BasicRAM extends AddressRange
{
	
	/**
	 * Image of this memory block
	 */
	private final byte[] mem;
	
	/**
	 * The byte order of this block true for little endian false for big endian
	 */
	private final boolean byteOrder;
	
	/**
	 * Standard constructor
	 *
	 * @param startAddr the starting address of this block
	 * @param size      the size of this memory block
	 * @param order     the byte order for this block, true for little endian, false
	 *                  for big endian
	 */
	public BasicRAM(int size, boolean order)
	{
		super(AddressRange.RAM, 0, size);
		byteOrder = order;
		mem = new byte[size];
	}
	
	/**
	 * @see AddressRange#write8(int, byte)
	 */
	public void write8(int address, byte b)
	{
		mem[address] = b;
	}
	
	/**
	 * @see AddressRange#write16(int, short)
	 */
	public void write16(int address, short s)
	{
		if (byteOrder)
		{
			mem[address] = (byte) s;
			mem[address + 1] = (byte) (s >>> 8);
		}
		else
		{
			mem[address] = (byte) (s >>> 8);
			mem[address + 1] = (byte) s;
		}
	}
	
	/**
	 * @see AddressRange#write32(int, int)
	 */
	public void write32(int address, int i)
	{
		if (byteOrder)
		{
			mem[address] = (byte) i;
			mem[address + 1] = (byte) (i >>> 8);
			mem[address + 2] = (byte) (i >>> 16);
			mem[address + 3] = (byte) (i >>> 24);
		}
		else
		{
			mem[address] = (byte) (i >>> 24);
			mem[address + 1] = (byte) (i >>> 16);
			mem[address + 2] = (byte) (i >>> 8);
			mem[address + 3] = (byte) i;
		}
	}
	
	/**
	 * @see AddressRange#write64(int, long)
	 */
	public void write64(int address, long l)
	{
		if (byteOrder)
		{
			write32(address, (int) l);
			write32(address + 4, (int) (l >>> 32));
		}
		else
		{
			write32(address, (int) (l >>> 32));
			write32(address + 4, (int) l);
		}
	}
	
	/**
	 * @see AddressRange#read8(int)
	 */
	public byte read8(int address)
	{
		return mem[address];
	}
	
	/**
	 * @see AddressRange#read16(int)
	 */
	public short read16(int address)
	{
		if (byteOrder)
			return (short) (((int) mem[address] & 0xFF) + (((int) mem[address + 1] & 0xFF) << 8));
		return (short) ((((int) mem[address] & 0xFF) << 8) + ((int) mem[address + 1] & 0xFF));
	}
	
	/**
	 * @see AddressRange#read32(int)
	 */
	public int read32(int address)
	{
		if (byteOrder)
			return ((int) mem[address] & 0xFF) + (((int) mem[address + 1] & 0xFF) << 8) + (((int) mem[address + 2] & 0xFF) << 16) + (((int) mem[address + 3] & 0xFF) << 24);
		return (((int) mem[address] & 0xFF) << 24) + (((int) mem[address + 1] & 0xFF) << 16) + (((int) mem[address + 2]) << 8) + ((int) mem[address + 3]);
	}
	
	/**
	 * @see AddressRange#read64(int)
	 */
	public long read64(int address)
	{
		if (byteOrder)
		{
			return ((long) read32(address) & 0xFFFFFFFFl) + ((long) (read32(address + 4)) << 32);
		}
		return ((long) read32(address) << 32) + ((long) read32(address + 4) & 0xFFFFFFFFl);
	}
	
	/**
	 * Method initializing the basic ram with a given image at the starting
	 * address
	 *
	 * @param address the address to copy the image to
	 * @param raw     the image to copy
	 */
	public void initRAM(int address, byte[] raw)
	{
		int cnt;
		for (cnt = 0; cnt < raw.length; cnt++)
			mem[address + cnt] = raw[cnt];
	}
}
