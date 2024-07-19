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

import sjc.compbase.*;

/**
 * Stmt: basic abstraction of a statement
 *
 * @author S. Frenz
 * @version 120925 added support for code printer
 * version 101015 adopted changed Expr and added dead code flag
 * version 100504 added isSuperThisCallStmt and resolveSuperThisCallStmt
 * version 091109 added FA_HAS_SHORTCUT and FA_HAS_CONTINUE
 * version 091102 inserted source-hint-generation in genOutput
 * version 090724 added FA_INSIDE_LOOP
 * version 090508 merged resolve and doStmtFlowAnalysis
 * version 090506 adopted FA_* constants to basic flow analysis
 * version 090505 added support for doFlowAnalysis
 * version 090207 added copyright notice
 * version 070908 optimized signature of resolve
 * version 070114 reduced access level where possible
 * version 060607 initial version
 */

public abstract class Stmt extends TokenAbstrPrintable
{
	protected final static int FA_NO_FLOWCHANGE = 0;
	protected final static int FA_HAS_ENDBLOCK = 0x01;
	protected final static int FA_HAS_CONTINUE = 0x02;
	protected final static int FA_HAS_SHORTCUT = 0x04;
	protected final static int FA_INSIDE_LOOP = 0x08;
	protected final static int FA_NEXT_IS_UNREACHABLE = 0x10;
	protected final static int FA_DEAD_CODE = 0x20;
	protected final static int FA_ERROR = 0x40;
	
	protected Stmt nextStmt;
	
	protected Stmt(int fid, int il, int ic)
	{
		super(fid, il, ic);
	}
	
	protected abstract int innerResolve(int flowEntryCode, Unit unitContext, Mthd mthdContext, Context ctx);
	
	protected abstract void innerGenOutput(Context ctx);
	
	protected int resolve(int flowCode, Unit unitContext, Mthd mthdContext, Context ctx)
	{
		if ((flowCode & FA_NEXT_IS_UNREACHABLE) != 0)
		{
			flowWarn(ctx, ERR_UNREACHABLE_CODE);
			flowCode &= ~FA_NEXT_IS_UNREACHABLE; //print warning only once
		}
		return innerResolve(flowCode, unitContext, mthdContext, ctx);
	}
	
	protected boolean isSuperThisCallStmt(Context ctx)
	{
		//default: not special statement
		return false;
	}
	
	protected boolean resolveSuperThisCallStmt(Unit unitContext, Mthd mthdContext, Context ctx)
	{
		//default: current statement is not the required special first constructor statement (call to this or super)
		printPos(ctx, "expected call to this(.) or super(.)");
		return false;
	}
	
	protected int getExprFromStmtFlowCode(int stmtFlowCode)
	{
		int exprResolveFlags = Expr.RF_NONE;
		if ((stmtFlowCode & FA_INSIDE_LOOP) != 0)
			exprResolveFlags |= Expr.RF_INSIDE_LOOP;
		if ((stmtFlowCode & FA_DEAD_CODE) != 0)
			exprResolveFlags |= Expr.RF_DEAD_CODE;
		return exprResolveFlags;
	}
	
	protected void genOutput(Context ctx)
	{
		ctx.arch.insertSourceHint(this);
		innerGenOutput(ctx);
	}
}
