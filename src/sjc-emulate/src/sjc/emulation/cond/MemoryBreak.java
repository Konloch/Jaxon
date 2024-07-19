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

package sjc.emulation.cond;

import sjc.emulation.AddressRange;
import sjc.emulation.Condition;
import sjc.emulation.Emulator;

/**
 * Class representing a breakpoint condition for memory access
 *
 * @author Patrick Schmidt
 * @version 090207 added copyright notice
 * version 060620 initial version
 */
public class MemoryBreak extends Condition
{
	
	/**
	 * The memory address to which to listen
	 */
	private final int memAddr;
	
	/**
	 * The type of the memory watched
	 */
	private final boolean type;
	
	/**
	 * Flag to determine whether to observe a read or write to the memory
	 */
	private final boolean read;
	
	/**
	 * Standard construction
	 *
	 * @param a the memory position to observe
	 * @param t the type of the memory to observe
	 * @param r flag to determine whether a read or write is observed
	 */
	public MemoryBreak(int a, boolean t, boolean r)
	{
		memAddr = a;
		type = t;
		read = r;
	}
	
	/**
	 * @see Condition#addToEmulator(Emulator)
	 */
	public void addToEmulator(Emulator emul)
	{
		this.next = emul.firstMemC;
		emul.firstMemC = this;
	}
	
	/**
	 * @see Condition#hit(int, int, boolean, boolean)
	 */
	public boolean hit(int address, int size, boolean t, boolean r)
	{
		return read == r && type == t && memAddr >= address && memAddr < address + size;
	}
	
	/**
	 * @see Condition#hit(Emulator)
	 */
	public boolean hit(Emulator emul)
	{
		return false;
	}
	
	/**
	 * @see Object#toString()
	 */
	public String toString()
	{
		String result = "Address==".concat(Emulator.toHexString(memAddr));
		if (type == AddressRange.RAM)
			result = result.concat(" RAM ");
		else
			result = result.concat(" IO  ");
		if (read)
			result = result.concat("for read");
		else
			result = result.concat("for write");
		return result;
	}
}
