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
 * Method representing a complete method disassembly. Organized as a binary
 * tree
 *
 * @author Patrick Schmidt
 * @version 090207 added copyright notice
 * version 060613 initial version
 */
public class MethodDisassembly
{
	
	/**
	 * First instruction pointer for the method
	 */
	public int firstIP;
	
	/**
	 * Last instruction pointer for the method
	 */
	public int lastIP;
	
	/**
	 * The first mnemonic for the method
	 */
	public Mnemonic firstMnemo;
	
	/**
	 * The name of this method
	 */
	protected String methodName;
	
	/**
	 * Left branch containing the method disassembly with an ip less than the
	 * current
	 */
	protected MethodDisassembly left;
	
	/**
	 * Right branch containing the method disassembly with an ip greater than the
	 * current
	 */
	protected MethodDisassembly right;
	
	/**
	 * Standard constructor initializing the mnemonic of this element
	 *
	 * @param first the first mnemonic of the method disassembly
	 * @param mn    the name of the corresponding method
	 */
	public MethodDisassembly(Mnemonic first, String mn)
	{
		Mnemonic temp;
		temp = first;
		methodName = mn;
		firstIP = first.startIP;
		firstMnemo = first;
		while (temp.next != null)
			temp = temp.next;
		lastIP = temp.startIP;
	}
	
	/**
	 * Method to add a single instruction to this methoddisassembly
	 *
	 * @param mn the mnemonic to add
	 */
	public void addInstruction(Mnemonic mn)
	{
		firstMnemo.add(mn);
	}
	
	/**
	 * Method to add a MethodDisassemby to this binary tree
	 *
	 * @param newMethod the new MethodDisassemby to add
	 */
	public void add(MethodDisassembly newMethod)
	{
		add(this, newMethod);
	}
	
	/**
	 * Method to obtain a method disassembly to a given instruction pointer
	 *
	 * @param iP the instruction pointer to look for
	 * @return the corresponding method disassembly or null if there is no such
	 * entry
	 */
	public MethodDisassembly getMethod(int iP)
	{
		return getMethod(this, iP);
	}
	
	/**
	 * Recursive method to obtain a MethodDisassembly with the given instruction
	 * pointer
	 *
	 * @param curr the entry in the binary tree to examine
	 * @param iP   the instruction pointer to look for
	 * @return the corresponding MethodDisassemby or null if there is no such
	 * entry
	 */
	private MethodDisassembly getMethod(MethodDisassembly curr, int iP)
	{
		if (curr == null)
			return null;
		if (iP >= curr.firstIP && iP <= curr.lastIP)
			return curr;
		if (iP < curr.firstIP)
			return getMethod(curr.left, iP);
		if (iP > curr.lastIP)
			return getMethod(curr.right, iP);
		return null;
	}
	
	/**
	 * Internal method to add a given entry to the binary tree. The order of
	 * insertion is:
	 * (firstIP of current MethodDisassembly) > (lastIP of MethodDisassembly to
	 * insert) => insert in left branch
	 * (lastIP of current MethodDisassembly) < (firstIP of MethodDisassembly to
	 * insert) => insert in right branch
	 * If these bounds are violated (overlaping methods) nothing is done
	 *
	 * @param current   the current examined element of the binary tree
	 * @param newMethod the new method to insert
	 */
	private void add(MethodDisassembly current, MethodDisassembly newMethod)
	{
		if (current.firstIP > newMethod.lastIP)
		{
			if (current.left == null)
			{
				current.left = newMethod;
				return;
			}
			else
				add(current.left, newMethod);
		}
		else if (current.lastIP < newMethod.firstIP)
		{
			if (current.right == null)
			{
				current.right = newMethod;
				return;
			}
			else
				add(current.right, newMethod);
		}
	}
	
	
	//============================ DEBUG =========================================
	
	/**
	 * Method to print the whole method - for debug purposes only
	 */
	public void print()
	{
		Mnemonic temp = firstMnemo;
		while (temp != null)
		{
			//System.out.println(Integer.toHexString(temp.iP) + " " + temp.mnemo);
			temp = temp.next;
		}
	}
}
