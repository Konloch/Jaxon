/* Copyright (C) 2005, 2006, 2007, 2008, 2009, 2010, 2011, 2012 Stefan Frenz
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
 * StReturn: return-statement
 *
 * @author S. Frenz
 * @version 120923 added support for code printer
 * version 110705 hardened return value check
 * version 101015 adopted changed Expr
 * version 100504 added support for StBreakable.genOutputCleanup
 * version 091111 added support for instance final-var-init
 * version 091102 adopted changed Stmt
 * version 091005 adopted changed Expr
 * version 090724 adopted changed Expr
 * version 090718 adopted changed Expr
 * version 090508 adopted changes in Stmt
 * version 090507 added flow analysis information
 * version 090207 added copyright notice
 * version 080711 fixed insertion of implicit conversion for interface return value
 * version 070909 optimized signature of Expr.resolve, adopted StBlock extends StBreakable
 * version 070908 optimized signature of Stmt.resolve
 * version 070628 adopted changes for "Expr extends TypeRef"
 * version 070114 reduced access level where possible
 * version 070111 adapted change in printPos and compErr
 * version 061229 removed access to firstInstr
 * version 061203 optimized calls to printPos and compErr
 * version 061111 minor changes
 * version 060620 added test for implicit conversion
 * version 060607 initial version
 */

public class StReturn extends Stmt
{
	protected Expr retVal;
	private final StBreakable outer;
	private StBreakable outest;
	
	protected StReturn(StBreakable io, int fid, int il, int ic)
	{
		super(fid, il, ic);
		outer = io;
	}
	
	public void printToken(CodePrinter prnt)
	{
		prnt.stmtReturn(retVal);
	}
	
	protected int innerResolve(int flowCode, Unit unitContext, Mthd mthdContext, Context ctx)
	{
		int res;
		ExEnc enc; //enclosure for implicit conversion
		
		if (retVal != null)
		{ //the programmer wants us to return something, check type
			if (mthdContext.retType.baseType == TypeRef.T_VOID)
			{
				printPos(ctx, "can not return anything in void method");
				return FA_ERROR;
			}
			if (mthdContext.isConstructor)
			{
				printPos(ctx, "can not return anything in constructor");
				return FA_ERROR;
			}
			if (!retVal.resolve(unitContext, mthdContext, getExprFromStmtFlowCode(flowCode) | Expr.RF_CHECKREAD, mthdContext.retType, ctx))
				return FA_ERROR;
			if (retVal.baseType == StdTypes.T_NULL)
			{
				res = mthdContext.retType.getRegType(ctx);
				if (res == StdTypes.T_PTR)
					retVal.baseType = StdTypes.T_NNPT;
				else if (res == StdTypes.T_DPTR)
					retVal.baseType = StdTypes.T_NDPT;
				else
				{
					printPos(ctx, "null does not match declared type (");
					mthdContext.retType.printType(ctx.out);
					ctx.out.print(")");
					return FA_ERROR;
				}
			}
			else
			{
				res = retVal.compareType(mthdContext.retType, true, ctx); //get type-compare
				if (res == TypeRef.C_NP || res == TypeRef.C_TT)
				{
					printPos(ctx, "type of return-expression (");
					retVal.printType(ctx.out);
					ctx.out.print(") does not match declared type (");
					mthdContext.retType.printType(ctx.out);
					ctx.out.print(")");
					return FA_ERROR;
				}
				if (res == TypeRef.C_OT && mthdContext.retType.isIntfType())
				{ //insert implicit conversion of object to interface
					enc = new ExEnc(fileID, line, col);
					enc.convertTo = mthdContext.retType;
					enc.ex = retVal;
					if (!enc.implConvResolve(unitContext, ctx))
					{
						ctx.out.print(" in inserted implicit conversion");
						return FA_ERROR;
					}
					retVal = enc;
				}
			}
		}
		else
		{ //the programmer wants to return nothing, check context
			if (!mthdContext.isConstructor && mthdContext.retType.baseType != TypeRef.T_VOID)
			{
				printPos(ctx, "method needs to return something (");
				mthdContext.retType.printType(ctx.out);
				ctx.out.print(")");
				return FA_ERROR;
			}
		}
		if (mthdContext.checkInitVars != null && !mthdContext.checkVarWriteState(this, ctx))
		{
			ctx.out.print(" before return");
			return FA_ERROR;
		}
		outest = outer;
		while (outest.outer != null)
			outest = outest.outer;
		return flowCode | FA_NEXT_IS_UNREACHABLE;
	}
	
	protected void innerGenOutput(Context ctx)
	{
		int reg, type;
		
		if (retVal != null)
		{
			type = retVal.getRegType(ctx);
			if (ctx.arch.prepareFreeReg(0, 0, 0, type) != 0)
			{ //there must be free registers
				compErr(ctx, "no free reg at beginning of return-statement");
				return;
			}
			reg = ctx.arch.allocReg();
			retVal.genOutputVal(reg, ctx);
			outer.genOutputCleanup(null, ctx);
			ctx.arch.genMoveToPrimary(reg, type);
			ctx.arch.deallocRestoreReg(reg, 0, 0);
		}
		else
			outer.genOutputCleanup(null, ctx);
		ctx.arch.genJmp(outest.breakDest);
	}
}
