/* Copyright (C) 2008, 2009, 2010, 2012 Stefan Frenz
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
import sjc.debug.CodePrinter;

/**
 * StThrow: throw-statement
 *
 * @author S. Frenz
 * @version 120923 added support for code printer
 * version 101210 adopted changed Architecture
 * version 101015 adopted changed Expr
 * version 091102 adopted changed Stmt
 * version 091005 adopted changed Expr
 * version 091001 adopted changed memory interface
 * version 090724 adopted changed Expr
 * version 090718 adopted changed Expr
 * version 090619 adopted changed Architecture
 * version 090508 adopted changes in Stmt
 * version 090506 added flow analysis information
 * version 090207 added copyright notice
 * version 080610 added throwable reporting
 * version 080605 initial version
 */

public class StThrow extends Stmt
{
	protected Expr throwVal;
	private UnitList runtimeClass;
	
	protected StThrow(int fid, int il, int ic)
	{
		super(fid, il, ic);
	}
	
	public void printToken(CodePrinter prnt)
	{
		prnt.stmtThrow(throwVal);
	}
	
	protected int innerResolve(int flowCode, Unit unitContext, Mthd mthdContext, Context ctx)
	{
		if (!throwVal.resolve(unitContext, mthdContext, getExprFromStmtFlowCode(flowCode) | Expr.RF_CHECKREAD, null, ctx))
			return FA_ERROR;
		if (throwVal.isCheckedExceptionType(ctx))
		{
			if (!mthdContext.handlesThrowable(this, throwVal.qid.unitDest, ctx))
				return FA_ERROR;
		}
		else if (!throwVal.isThrowableType(ctx))
		{
			printPos(ctx, "thrown expression has to be of type Throwable");
			return FA_ERROR;
		}
		if (ctx.dynaMem)
			runtimeClass = unitContext.getRefUnit(ctx.rteDynamicRuntime, true);
		return flowCode | FA_NEXT_IS_UNREACHABLE;
	}
	
	protected void innerGenOutput(Context ctx)
	{
		int regExc;
		if (ctx.arch.prepareFreeReg(0, 0, 0, StdTypes.T_PTR) != 0)
		{ //should be 0 always
			compErr(ctx, "no free reg at beginning of throw");
			return;
		}
		regExc = ctx.arch.allocReg();
		throwVal.genOutputVal(regExc, ctx);
		genThrow(regExc, runtimeClass, ctx);
		ctx.arch.deallocRestoreReg(regExc, 0, 0);
	}
	
	public static void genThrow(int regExc, UnitList runtimeClass, Context ctx)
	{
		if (ctx.dynaMem)
			ctx.arch.genSaveUnitContext();
		ctx.arch.genPush(regExc, StdTypes.T_PTR);
		if (ctx.dynaMem)
		{
			ctx.arch.genLoadUnitContext(ctx.arch.regClss, runtimeClass.relOff);
			ctx.arch.genCall(ctx.rteDoThrowMd.relOff, ctx.arch.regClss, ctx.rteDoThrowMd.parSize);
			ctx.arch.genRestUnitContext();
		}
		else
			ctx.arch.genCallConst(ctx.rteDoThrowMd, ctx.rteDoThrowMd.parSize);
	}
}
