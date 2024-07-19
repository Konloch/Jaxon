/* Copyright (C) 2006, 2007, 2008, 2009, 2010, 2012 Stefan Frenz
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

package sjc.osio;

/**
 * BinWriter: interface to write binary data
 *
 * @author S. Frenz
 * @version 120227 cleaned up "package sjc." typo
 * version 100504 added default implementation for setAddress
 * version 090303 adopted changed osio package structure
 * version 090207 added copyright notice
 * version 060628 changed signature of write to support offsets
 * version 060613 initial version
 */

public abstract class BinWriter
{
	public abstract boolean open(String fname);
	
	public abstract void close();
	
	public abstract boolean write(byte[] what, int offset, int len);
	
	public abstract boolean setSize(int addBytes);
	
	public boolean setAddress(int address)
	{
		return false; //not supported
	}
}
