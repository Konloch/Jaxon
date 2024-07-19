/* Copyright (C) 2005, 2006, 2007, 2008, 2009 Stefan Frenz
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

package sjc.compbase;


/**
 * IndirUnitMapList: list of interfaces
 *
 * @author S. Frenz
 * @version 091001 adopted changed memory interface
 * version 090207 added copyright notice
 * version 071215 renamed from JIntfList to IndirUnitMapList and moved to compbase
 * version 070730 changed Intf to Unit
 * version 070114 reduced access level where possible
 * version 060607 initial version
 */

public class IndirUnitMapList
{
	public Unit intf;
	public IndirUnitMapList next;
	public Mthd[] map; //needed for generating the corresponding SIntfMap
	public boolean outputGenerated; //needed for generating the corresponding SIntfMap
	public Object outputLocation; //needed for generating the corresponding SIntfMap
	
	public IndirUnitMapList(Unit ii, IndirUnitMapList in)
	{
		intf = ii;
		next = in;
	}
}
