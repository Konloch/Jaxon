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
 * Class representing a breakpoint for a "step over" over a call. Only condition
 * checked is the position of the stack pointer
 *
 * @author Patrick Schmidt
 * @version 090207 added copyright notice
 * version 060620 initial version
 */
public class StackCond extends Condition
{
	
	/**
	 * The position of the stack pointer to look after
	 */
	private final int stackPtr;
	
	/**
	 * Standard constructor
	 *
	 * @param sPtr the position of the stack pointer to look after
	 */
	public StackCond(int sPtr)
	{
		stackPtr = sPtr;
	}
	
	/**
	 * @see Condition#hit(Emulator)
	 */
	public boolean hit(Emulator emul)
	{
		return stackPtr <= emul.getCurrentSP();
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
		this.next = emul.stepOverC;
		emul.stepOverC = this;
	}
	
	/**
	 * @see Object#toString()
	 */
	public String toString()
	{
		return null;
	}
	
}
