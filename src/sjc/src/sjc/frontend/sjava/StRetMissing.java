/* Copyright (C) 2005, 2006, 2007, 2008, 2009, 2010, 2012 Stefan Frenz
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
 * StRetMissing: pseudo-statement to handle "return missing"
 *
 * @author S. Frenz
 * @version 120923 added support for code printer
 * version 100424 removed support for runtime check
 * version 091102 adopted changed Stmt
 * version 091001 adopted changed memory interface
 * version 090619 adopted changed Architecture
 * version 090508 adopted changes in Stmt
 * version 090506 initial version
 */

public class StRetMissing extends Stmt
{
	protected StRetMissing(int fid, int il, int ic)
	{
		super(fid, il, ic);
	}
	
	public void printToken(CodePrinter prnt)
	{
		prnt.stmtReturnMissing();
	}
	
	protected int resolve(int flowCode, Unit unitContext, Mthd mthdContext, Context ctx)
	{
		if ((flowCode & FA_NEXT_IS_UNREACHABLE) == 0)
			flowWarn(ctx, "reached end of method without return");
		return flowCode;
	}
	
	protected int innerResolve(int flowCode, Unit unitContext, Mthd mthdContext, Context ctx)
	{ //should never be called as resolve is overwritten
		//should never be called (resolve is overwritten)
		return FA_ERROR;
	}
	
	protected void innerGenOutput(Context ctx)
	{
		//nothing to do
	}
}
