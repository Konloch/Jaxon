/* Copyright (C) 2005, 2006, 2007, 2008, 2009, 2012 Stefan Frenz
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

import sjc.compbase.Context;
import sjc.compbase.Mthd;
import sjc.compbase.Unit;
import sjc.debug.CodePrinter;

/**
 * StEmpty: empty statement
 *
 * @author S. Frenz
 * @version 120923 added support for code printer
 * version 091103 overwrote genOutput to avoid source hinting for empty statement
 * version 091102 adopted changed Stmt
 * version 090508 adopted changes in Stmt
 * version 090207 added copyright notice
 * version 070908 optimized signature of Stmt.resolve
 * version 070114 reduced access level where possible
 * version 061229 removed access to firstInstr
 * version 060607 initial version
 */

public class StEmpty extends Stmt
{
	protected StEmpty(int fid, int il, int ic)
	{
		super(fid, il, ic);
	}
	
	public void printToken(CodePrinter prnt)
	{
		prnt.stmtEmpty();
	}
	
	protected int innerResolve(int flowCode, Unit unitContext, Mthd mthdContext, Context ctx)
	{
		return flowCode; //nothing to resolve
	}
	
	protected void genOutput(Context ctx)
	{
		//don't give source hint for empty statements
	}
	
	protected void innerGenOutput(Context ctx)
	{
		//empty statement
	}
}
