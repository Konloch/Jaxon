/* Copyright (C) 2008, 2009 Stefan Frenz
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

package sjc.backend.atmel;

/**
 * ValueInfo: info about a register / register pair or an absolute value
 *
 * @author S. Frenz
 * @version 091005 removed unneeded methods
 * version 090207 added copyright notice
 * version 081020 added VI_MEMABS
 * version 081017 initial version
 */

public class ValueInfo
{
	public final static int VI_ABS = 1; //an absolute value
	public final static int VI_MEMREL = 2; //a register pair relative memory location
	public final static int VI_MEMABS = 3; //an absolute memory address
	
	public int type, imm, memreg;
	public ValueInfo next; //allow simple listing
	
	public ValueInfo set(int it, int ii)
	{
		type = it;
		imm = ii;
		return this;
	}
	
	public ValueInfo set(int it, int ii, int im)
	{
		type = it;
		imm = ii;
		memreg = im;
		return this;
	}
}
