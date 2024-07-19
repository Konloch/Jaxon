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
import sjc.compbase.Expr;
import sjc.compbase.Mthd;
import sjc.compbase.Unit;
import sjc.debug.CodePrinter;

/**
 * StExpr: single expression as statement
 *
 * @author S. Frenz
 * @version 120923 added support for code printer
 * version 101015 adopted changed Expr
 * version 100504 added tryToResolveSpecialFirstConstrStmt
 * version 091103 removed unused variables/methods
 * version 091102 adopted changed Stmt
 * version 091005 adopted changed Expr
 * version 090724 adopted changed Expr
 * version 090718 adopted changed Expr
 * version 090508 adopted changes in Stmt
 * version 090207 added copyright notice
 * version 080613 adopted hasEffect->effectType, support for MARKER.stopBlockCoding()
 * version 080605 better error report for effectless expressions
 * version 070909 optimized signature of Expr.resolve
 * version 070908 optimized signature of Stmt.resolve
 * version 070114 reduced access level where possible
 * version 070111 adapted change in printPos and compErr
 * version 061229 removed access to firstInstr
 * version 061203 optimized calls to printPos and compErr
 * version 060822 optimized constructor
 * version 060607 initial version
 */

public class StExpr extends Stmt
{
	protected final static int EF_STOP = -1;
	
	protected Expr ex;
	
	protected StExpr(int fid, int il, int ic)
	{
		super(fid, il, ic);
	}
	
	public void printToken(CodePrinter prnt)
	{
		prnt.stmtExpr(ex);
	}
	
	protected int innerResolve(int flowCode, Unit unitContext, Mthd mthdContext, Context ctx)
	{
		if (!ex.resolve(unitContext, mthdContext, getExprFromStmtFlowCode(flowCode), null, ctx))
			return FA_ERROR;
		if (ex.effectType == Expr.EF_NONE)
		{
			printPos(ctx, "expression without effect");
			return FA_ERROR;
		}
		return flowCode;
	}
	
	protected boolean isSuperThisCallStmt(Context ctx)
	{
		return ex.isSuperThisCall(ctx);
	}
	
	protected boolean resolveSuperThisCallStmt(Unit unitContext, Mthd mthdContext, Context ctx)
	{
		return ex.resolveSuperThisCall(unitContext, mthdContext, ctx);
	}
	
	protected void innerGenOutput(Context ctx)
	{
		if (ex.effectType == EF_STOP)
			nextStmt = null; //kill following statements
		else
			ex.genOutput(ctx); //generate output, most likely generate value (discard result)
	}
}
