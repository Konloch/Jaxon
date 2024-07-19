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

import sjc.osio.TextPrinter;

/**
 * Abstract declaration of an emulator
 *
 * @author Patrick Schmidt, S. Frenz
 * @version 090207 added copyright notice
 * version 060608 initial version
 */
public abstract class Emulator
{
	
	private static char[] buf;
	
	/**
	 * Method to convert an integer to a hex string
	 *
	 * @param val the integer to convert
	 * @return the string-representation of the string in hex
	 */
	public static String toHexString(int val)
	{
		int i, t;
		buf[0] = '0';
		buf[1] = 'x';
		for (i = 0; i < 8; i++)
		{
			t = (val >>> (i << 2)) & 0xF;
			if (t >= 0 && t <= 9)
				buf[9 - i] = (char) (t + 48);
			else
				buf[9 - i] = (char) (t + 55);
		}
		return new String(buf, 0, 10);
	}
	
	/**
	 * Method to transform an integer into a String
	 *
	 * @param val the value to transform
	 * @return the String representation of the given value
	 */
	public static String toDecString(int val)
	{
		boolean negative = (val < 0);
		int digit, temp, count, j, div, count2;
		if (val == -2147483648)
			return "-2147483648";
		if (val == 0)
			return "0";
		if (negative)
			val = -val;
		temp = val;
		count = 0;
		while (temp > 0)
		{
			temp = temp / 10;
			count++;
		}
		temp = val;
		count2 = 0;
		if (negative)
		{
			buf[count2] = '-';
			count2++;
		}
		while (count > 0)
		{
			div = 1;
			for (j = 1; j < count; j++)
				div = div * 10;
			digit = temp / div;
			buf[count2] = (char) (digit + 48);
			temp = temp - (digit * div);
			count--;
			count2++;
		}
		return new String(buf, 0, count2);
	}
	
	/**
	 * Method to transform an integer into its hex representation
	 *
	 * @param val the value to transform
	 * @return the hex representation of the given value as a String
	 */
	public static String toLongHexString(long val)
	{
		int i, t;
		buf[0] = '0';
		buf[1] = 'x';
		for (i = 0; i < 16; i++)
		{
			t = (int) (val >>> (i << 2)) & 0xF;
			if (t >= 0 && t <= 9)
				buf[17 - i] = (char) (t + 48);
			else
				buf[17 - i] = (char) (t + 55);
		}
		return new String(buf, 0, 18);
	}
	
	/**
	 * Pointer size
	 */
	public int relocBytes;
	
	/**
	 * Mask to align the stack
	 */
	public int stackClearBits;
	
	/**
	 * Relative start of method code inside method object
	 */
	public int codeStart;
	
	/**
	 * First element in the list of registered memory blocks
	 */
	private AddressRange firstBlock;
	
	/**
	 * Viewer to print debug information to
	 */
	protected TextPrinter out;
	
	/**
	 * An optional breakpoint listener
	 */
	protected BreakPointListener listener;
	
	/**
	 * The first condition in the list of registered breakpoint conditions
	 */
	public Condition firstBreakPointC;
	
	/**
	 * The first condition in the list of memory breakpoint conditions
	 */
	public Condition firstMemC;
	
	/**
	 * The step over condition
	 */
	public Condition stepOverC;
	
	/**
	 * Method checking whether a registered breakpoint matches a memory access
	 *
	 * @param address the address accessed
	 * @param size    the size of the read/write operation
	 * @param type    the type of the accessed memory
	 * @param read    true if this is a read, false otherwise
	 */
	private void memCondCheck(int address, int size, boolean type, boolean read)
	{
		Condition temp = firstMemC;
		while (temp != null)
		{
			if (temp.hit(address, size, type, read))
				listener.breakPointOccurred(temp);
			temp = temp.next;
		}
	}
	
	/**
	 * Method checking whether a registered breakpoint matches
	 */
	protected void breakCondCheck()
	{
		Condition temp = firstBreakPointC;
		while (temp != null)
		{
			if (temp.hit(this))
				listener.breakPointOccurred(temp);
			temp = temp.next;
		}
	}
	
	/**
	 * Method to register a memory block in this emulator
	 *
	 * @param block an instance of this block
	 */
	public void registerBlock(AddressRange block)
	{
		if (firstBlock == null)
		{
			firstBlock = block;
		}
		else
		{
			block.nextBlock = firstBlock;
			firstBlock = block;
		}
	}
	
	/**
	 * Method to write a byte in the memory of this emulator
	 *
	 * @param type    the type of the required memory block
	 * @param address the address where the byte should be written to
	 * @param b       the byte to write
	 */
	public void write8(boolean type, int address, byte b)
	{
		AddressRange tempBlock = firstBlock;
		if (firstMemC != null)
			memCondCheck(address, 1, type, false);
		while (tempBlock != null)
		{
			if ((tempBlock.type == type) && (tempBlock.startAddr <= address) && (tempBlock.startAddr + tempBlock.length > address))
			{
				tempBlock.write8(address, b);
				return;
			}
			tempBlock = tempBlock.nextBlock;
		}
	}
	
	/**
	 * Method to write a short in the memory of this emulator
	 *
	 * @param type    the type of the required memory block
	 * @param address the address where the short should be written to
	 * @param s       the short to write
	 */
	public void write16(boolean type, int address, short s)
	{
		AddressRange tempBlock = firstBlock;
		if (firstMemC != null)
			memCondCheck(address, 2, type, false);
		while (tempBlock != null)
		{
			if ((tempBlock.type == type) && (tempBlock.startAddr <= address) && (tempBlock.startAddr + tempBlock.length > address))
			{
				tempBlock.write16(address, s);
				return;
			}
			tempBlock = tempBlock.nextBlock;
		}
	}
	
	/**
	 * Method to write an int in the memory of this emulator
	 *
	 * @param type    the type of the required memory block
	 * @param address the address where the int should be written to
	 * @param i       the int to write
	 */
	public void write32(boolean type, int address, int i)
	{
		AddressRange tempBlock = firstBlock;
		if (firstMemC != null)
			memCondCheck(address, 4, type, false);
		while (tempBlock != null)
		{
			if ((tempBlock.type == type) && (tempBlock.startAddr <= address) && (tempBlock.startAddr + tempBlock.length > address))
			{
				tempBlock.write32(address, i);
				return;
			}
			tempBlock = tempBlock.nextBlock;
		}
	}
	
	/**
	 * Method to write a long in the memory of this emulator
	 *
	 * @param type    the type of the required memory block
	 * @param address the address where the long should be written to
	 * @param l       the long to write
	 */
	public void write64(boolean type, int address, long l)
	{
		AddressRange tempBlock = firstBlock;
		if (firstMemC != null)
			memCondCheck(address, 8, type, false);
		while (tempBlock != null)
		{
			if ((tempBlock.type == type) && (tempBlock.startAddr <= address) && (tempBlock.startAddr + tempBlock.length > address))
			{
				tempBlock.write64(address, l);
				return;
			}
			tempBlock = tempBlock.nextBlock;
		}
	}
	
	/**
	 * Method to read a byte from the memory of this emulator
	 *
	 * @param type    the type of the required memory block
	 * @param address the address from where to read the byte
	 * @return the value at the given memory address
	 */
	public byte read8(boolean type, int address)
	{
		AddressRange tempBlock = firstBlock;
		if (firstMemC != null)
			memCondCheck(address, 1, type, true);
		while (tempBlock != null)
		{
			if ((tempBlock.type == type) && (tempBlock.startAddr <= address) && (tempBlock.startAddr + tempBlock.length > address))
			{
				return tempBlock.read8(address);
			}
			tempBlock = tempBlock.nextBlock;
		}
		return (byte) 0;
	}
	
	/**
	 * Method to read a short from the memory of this emulator
	 *
	 * @param type    the type of the required memory block
	 * @param address the address from where to read the short
	 * @return the value at the given memory address
	 */
	public short read16(boolean type, int address)
	{
		AddressRange tempBlock = firstBlock;
		if (firstMemC != null)
			memCondCheck(address, 2, type, true);
		while (tempBlock != null)
		{
			if ((tempBlock.type == type) && (tempBlock.startAddr <= address) && (tempBlock.startAddr + tempBlock.length > address))
			{
				return tempBlock.read16(address);
			}
			tempBlock = tempBlock.nextBlock;
		}
		return (short) 0;
	}
	
	/**
	 * Method to read an int from the memory of this emulator
	 *
	 * @param type    the type of the required memory block
	 * @param address the address from where to read the int
	 * @return the value at the given memory address
	 */
	public int read32(boolean type, int address)
	{
		AddressRange tempBlock = firstBlock;
		if (firstMemC != null)
			memCondCheck(address, 4, type, true);
		while (tempBlock != null)
		{
			if ((tempBlock.type == type) && (tempBlock.startAddr <= address) && (tempBlock.startAddr + tempBlock.length > address))
			{
				return tempBlock.read32(address);
			}
			tempBlock = tempBlock.nextBlock;
		}
		return 0;
	}
	
	/**
	 * Method to read a long from the memory of this emulator
	 *
	 * @param type    the type of the required memory block
	 * @param address the address from where to read the long
	 * @return the value at the given memory address
	 */
	public long read64(boolean type, int address)
	{
		AddressRange tempBlock = firstBlock;
		if (firstMemC != null)
			memCondCheck(address, 8, type, true);
		while (tempBlock != null)
		{
			if ((tempBlock.type == type) && (tempBlock.startAddr <= address) && (tempBlock.startAddr + tempBlock.length > address))
			{
				return tempBlock.read64(address);
			}
			tempBlock = tempBlock.nextBlock;
		}
		return 0;
	}
	
	/**
	 * Method initializing the basic memory of this emulator with a given image
	 *
	 * @param raw      the image to load into the memory
	 * @param memSize  the size of the RAM of this emulator
	 * @param debugOut a Viewer to pass messages to
	 * @return true if the operation succeeds, false otherwise
	 */
	public boolean initFromRawOut(byte[] raw, int memSize, TextPrinter debugOut)
	{
		BasicRAM ram;
		int startAddr;
		// determine byte order
		if (raw[28] == (byte) 0xAA && raw[31] == (byte) 0x55)
		{
			relocBytes = (int) raw[29] & 0xFF;
			stackClearBits = (int) raw[30] & 0xFF;
			ram = new BasicRAM(memSize, true);
		}
		else if (raw[28] == (byte) 0x55 && raw[31] == (byte) 0xAA)
		{
			relocBytes = (int) raw[30] & 0xFF;
			stackClearBits = (int) raw[29] & 0xFF;
			ram = new BasicRAM(memSize, false);
		}
		else
		{
			debugOut.println("Invalid image");
			return false;
		}
		buf = new char[19];
		// set first memory block
		firstBlock = ram;
		// read the address where to write the image
		ram.write8(0, raw[0]);
		ram.write8(1, raw[1]);
		ram.write8(2, raw[2]);
		ram.write8(3, raw[3]);
		startAddr = ram.read32(0);
		ram.write32(0, 0);
		// init the ram
		ram.initRAM(startAddr, raw);
		// set viewer output
		out = debugOut;
		// init registers
		codeStart = ram.read32(startAddr + 24);
		return initArchitecture(ram.read32(startAddr + 8), ram.read32(startAddr + 12));
	}
	
	/**
	 * Method to init the class descriptor and the instruction pointer registers
	 * with the the given parameters
	 *
	 * @param classDesc the address of the class descriptor
	 * @param startIP   the address to set the instruction pointer to
	 * @return
	 */
	public abstract boolean initArchitecture(int classDesc, int startIP);
	
	/**
	 * Method to perform a single step in the emulator
	 *
	 * @param into true for step into, false for step over
	 */
	public abstract boolean step(boolean into);
	
	/**
	 * Method to obtain the current instruction pointer
	 *
	 * @return the current instruction pointer of the emulator
	 */
	public abstract int getCurrentIP();
	
	/**
	 * Method to obtain the current stack pointer
	 *
	 * @return the current stack pointer of the emulator
	 */
	public abstract int getCurrentSP();
	
	/**
	 * Method to obtain the first instruction of the method corresponding
	 * to the given parameter currIP
	 *
	 * @param currIP the instruction inside the method
	 * @return the first instruction (may be used as startIP in other calls) of the method
	 */
	public abstract int getStartOfMethod(int currIP);
	
	/**
	 * Method to obtain the last instruction of the method corresponding
	 * to the given parameter currIP
	 *
	 * @param currIP the instruction inside the method
	 * @return the last instruction of the method
	 */
	public abstract int getEndOfMethod(int currIP);
	
	/**
	 * Method to obtain a method disassembly from a given position of the
	 * instruction pointer
	 *
	 * @param startIP the instruction pointer from where to disassemble the method
	 * @return the corresponding method disassembly
	 */
	public abstract MethodDisassembly getMnemonicList(int startIP);
	
	/**
	 * Method to set a Breakpoint-Listener which is informed if a breakpoint
	 * occurs
	 *
	 * @param l the breakpoint listener
	 */
	public void setBreakPointListener(BreakPointListener l)
	{
		listener = l;
	}
}
