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

import sjc.emulation.Condition;
import sjc.emulation.Emulator;

/**
 * Class representing a standard breakpoint set by a user
 *
 * @author Patrick Schmidt
 * @version 090207 added copyright notice
 * version 060620 initial version
 */
public class BreakPoint extends Condition
{
	
	/**
	 * The instruction pointer at which this breakpoint is set
	 */
	private final int insPtr;
	
	/**
	 * Standard constructor for this breakpoint
	 *
	 * @param iP the instruction pointer
	 */
	public BreakPoint(int iP)
	{
		insPtr = iP;
	}
	
	/**
	 * @see Condition#hit(Emulator)
	 */
	public boolean hit(Emulator emul)
	{
		return emul.getCurrentIP() == insPtr;
	}
	
	/**
	 * @see Condition#hit(int, int, boolean, boolean)
	 */
	public boolean hit(int address, int size, boolean type, boolean read)
	{
		return false;
	}
	
	/**
	 * @see Condition#addToEmulator(Emulator)
	 */
	public void addToEmulator(Emulator emul)
	{
		this.next = emul.firstBreakPointC;
		emul.firstBreakPointC = this;
	}
	
	/**
	 * @see Object#toString()
	 */
	public String toString()
	{
		return "IP == ".concat(Emulator.toHexString(insPtr));
	}
}
