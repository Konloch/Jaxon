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
 * Class holding statistics about methods
 *
 * @author Patrick Schmidt
 * @version 090207 added copyright notice
 * version 060621 initial version
 */
public class MethodStatistics
{
	
	/**
	 * The method pointer
	 */
	public int mthPtr;
	
	/**
	 * The first instruction pointer of this method
	 */
	public int firstIP;
	
	/**
	 * The last instruction pointer of this mehod
	 */
	public int lastIP;
	
	/**
	 * Number of calls of method
	 */
	public long numberOfCalls;
	
	/**
	 * Number of executed instructions in this method
	 */
	public long numberOfExecIns;
	
	/**
	 * Left branch of this binary tree
	 */
	public MethodStatistics left;
	
	/**
	 * Right branch of this binary tree
	 */
	public MethodStatistics right;
	
	/**
	 * Standard construction initializing
	 *
	 * @param fi the first IP of this method
	 * @param li the last IP of this method
	 */
	public MethodStatistics(int mp, int fi, int li)
	{
		mthPtr = mp;
		firstIP = fi;
		lastIP = li;
		numberOfCalls = 0L;
		numberOfExecIns = 0L;
	}
	
	/**
	 * Method to get the statistics of method containing an instruction at the
	 * given position
	 *
	 * @param i the startIP of the instruction contained in the desired method
	 * @return the corresponding method statistics or null
	 */
	public MethodStatistics getMethodStatistics(int i)
	{
		return getMethodStatistics(i, this);
	}
	
	/**
	 * Method to add a method statistic to the this binary tree
	 *
	 * @param m the method statistic to add
	 */
	public void addMethodStatistic(MethodStatistics m)
	{
		addMethodStatistic(m, this);
	}
	
	/**
	 * Method to reset the statistics
	 */
	public void reset()
	{
		numberOfCalls = 0l;
		numberOfExecIns = 0l;
		if (left != null)
			left.reset();
		if (right != null)
			right.reset();
	}
	
	/**
	 * Recursive method determining the method statistic containing the
	 * instruction at the given ip
	 *
	 * @param i    the ip of the instruction contained by the desired method
	 * @param curr the currently examined method
	 * @return the corresponding method or null
	 */
	private MethodStatistics getMethodStatistics(int i, MethodStatistics curr)
	{
		if (curr == null)
			return null;
		if (curr.firstIP <= i && curr.lastIP >= i)
			return curr;
		if (firstIP > i)
			return getMethodStatistics(i, curr.left);
		if (lastIP < i)
			return getMethodStatistics(i, curr.right);
		return null;
	}
	
	/**
	 * Recursive method to insert a method statistic into this binary tree
	 *
	 * @param m    the method statistic to insert
	 * @param curr the currently examined method
	 */
	private void addMethodStatistic(MethodStatistics m, MethodStatistics curr)
	{
		if (m.lastIP < curr.firstIP)
		{
			if (curr.left == null)
				curr.left = m;
			else
				addMethodStatistic(m, curr.left);
		}
		else if (m.firstIP > curr.lastIP)
		{
			if (curr.right == null)
				curr.right = m;
			else
				addMethodStatistic(m, curr.right);
		}
	}
}
