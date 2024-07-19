/* Copyright (C) 2010 Stefan Frenz
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

package sjc.frontend.sjava;

import sjc.compbase.StringList;

/**
 * StLoop: loop container
 *
 * @author S. Frenz
 * @version 100504 adopted changed StBreakable
 * version 100411 initial version
 */

public abstract class StLoop extends StBreakable
{
	protected Stmt loStmt;
	
	protected StLoop(StBreakable io, StringList ila, int fid, int il, int ic)
	{
		super(io, ila, fid, il, ic);
	}
	
	protected boolean isBreakContDest(boolean named, boolean contNotBreak)
	{
		return true; //loops are always breakable and continuable
	}
}
