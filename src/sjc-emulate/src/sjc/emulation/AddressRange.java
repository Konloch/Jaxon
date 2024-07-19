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
 * Class representing a memory block in the simulator
 *
 * @author Patrick Schmidt
 * @value 060608 initial version
 */
public abstract class AddressRange
{
	
	/**
	 * Type for a basic RAM memory block
	 */
	public static final boolean RAM = false;
	
	/**
	 * Type for an IO memory block
	 */
	public static final boolean IO = true;
	
	/**
	 * The starting address of this memory block
	 */
	protected int startAddr;
	
	/**
	 * The length of this memory block
	 */
	protected int length;
	
	/**
	 * The type of this memory block
	 */
	protected boolean type;
	
	/**
	 * Link to the next block in this linked list
	 */
	protected AddressRange nextBlock;
	
	/**
	 * Standard constructor
	 *
	 * @param type  the type of this memory block
	 * @param start the starting address
	 * @param size  the size of this block
	 */
	public AddressRange(boolean it, int start, int size)
	{
		type = it;
		startAddr = start;
		length = size;
	}
	
	/**
	 * Method to write a 8 bit value to this block
	 *
	 * @param address the address where to write this value
	 * @param s       the value to write
	 */
	public abstract void write8(int address, byte b);
	
	/**
	 * Method to write a 16 bit value to this block
	 *
	 * @param address the address where to write this value
	 * @param s       the value to write
	 */
	public abstract void write16(int address, short s);
	
	/**
	 * Method to write a 32 bit value to this block
	 *
	 * @param address the address where to write this value
	 * @param s       the value to write
	 */
	public abstract void write32(int address, int i);
	
	/**
	 * Method to write a 16 bit value to this block
	 *
	 * @param address the address where to write this value
	 * @param s       the value to write
	 */
	public abstract void write64(int address, long l);
	
	/**
	 * Method to read 8 bytes from this block
	 *
	 * @param address the address from where to read
	 * @return a byte containing the value of this memory position
	 */
	public abstract byte read8(int address);
	
	/**
	 * Method to read 16 bytes from this block
	 *
	 * @param address the address from where to read
	 * @return a short containing the value of this memory position
	 */
	public abstract short read16(int address);
	
	/**
	 * Method to read 32 bytes from this block
	 *
	 * @param address the address from where to read
	 * @return a int containing the value of this memory position
	 */
	public abstract int read32(int address);
	
	/**
	 * Method to read 64 bytes from this block
	 *
	 * @param address the address from where to read
	 * @return a long containing the value of this memory position
	 */
	public abstract long read64(int address);
}
