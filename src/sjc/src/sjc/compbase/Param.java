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
 * Param: declaration of a parameter inside a method declaration
 *
 * @author S. Frenz
 * @version 090207 added copyright notice
 * version 070903 removed checkNameAndType
 * version 070731 adopted renaming of id to name
 * version 070727 adopted change of type of id
 * version 070111 adopted change in printPos and compErr
 * version 061203 optimized calls to printPos and compErr
 * version 060607 initial version
 */

public class Param extends AccVar
{
	public Param nextParam;
	
	public Param(int fid, int il, int ic)
	{
		super(fid, il, ic);
	}
}
