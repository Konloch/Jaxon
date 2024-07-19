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
 * Class to represent a single instruction for disassembling. For being able
 * to group several mnemonics to a method, this class also incorporates a
 * linked list
 *
 * @author Patrick Schmidt
 * @version 090207 added copyright notice
 * version 060613 initial version
 */
public class Mnemonic
{
	
	/**
	 * The instruction pointer for this mnemonic
	 */
	public int startIP;
	
	/**
	 * The mnemonic to display
	 */
	public String mnemo;
	
	/**
	 * Parameters for the given instruction
	 */
	public String parameters;
	
	/**
	 * True if the instruction is a destination of a jump
	 */
	public boolean isJumpDest;
	
	/**
	 * The complete compiled String of the displaying application
	 */
	public String compString;
	
	/**
	 * Reference to the next mnemonic in this list
	 */
	public Mnemonic next;
	
	/**
	 * Standard constructor initializing the values
	 *
	 * @param insP the instruction pointer
	 * @param m    the mnemonic of this instruction
	 * @param isJD true if this instruction is destination of a jump
	 */
	public Mnemonic(int insP, String m, String p, boolean isJD)
	{
		startIP = insP;
		parameters = p;
		mnemo = m;
		isJumpDest = isJD;
	}
	
	/**
	 * Method to add a mnemonic to the end of this list
	 *
	 * @param mn the mnemonic to add
	 */
	public void add(Mnemonic mn)
	{
		Mnemonic temp = this;
		while (temp.next != null)
			temp = temp.next;
		temp.next = mn;
	}
}
