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
 * Class representing a condition for a break condition
 *
 * @author Patrick Schmidt
 * @version 090207 added copyright notice
 * version 060608 initial version
 */
public abstract class Condition
{
	
	/**
	 * Pointer to the next condition
	 */
	public Condition next;
	
	/**
	 * Method determining whether the current condition is a hit
	 *
	 * @param emul the emulator in which the condition is registered
	 * @return true if this is a hit, false otherwise
	 */
	public abstract boolean hit(Emulator emul);
	
	/**
	 * Method called by the emulator, if a memory access occured
	 *
	 * @param address the address accessed
	 * @param size    the size of data read from or written to the memory
	 * @param type    the type of the memory
	 * @param read    true for a read, false for a write
	 * @return true, if this is a hit, false otherwise
	 */
	public abstract boolean hit(int address, int size, boolean type, boolean read);
	
	/**
	 * As there is an order of conditions different types of conditions are
	 * added to different lists in an emulator
	 *
	 * @param emul Emulator to which this condition is added
	 */
	public abstract void addToEmulator(Emulator emul);
	
	/**
	 * @see Object#toString()
	 */
	public abstract String toString();
}
