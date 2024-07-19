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
import sjc.debug.CodePrinter;

/**
 * ExPrePst: expression with pre- or post-operator
 *
 * @author S. Frenz
 * @version 120925 added support for code printer
 * version 101015 adopted changed Expr
 * version 091005 adopted changed Expr
 * version 091001 adopted changed memory interface
 * version 090724 adopted changed Expr
 * version 090718 adopted changed Expr
 * version 090619 adopted changed Architecture
 * version 090207 added copyright notice
 * version 080616 removed unneccessary overwriting of calcConstantType
 * version 080613 adopted hasEffect->effectType
 * version 070909 optimized signature of Expr.resolve
 * version 070727 replaced exSubResolve by resolve as there is nothing done anymore
 * version 070628 adopted changes for "Expr extends TypeRef"
 * version 070531 adopted removal of Architecture.genLoadFromMem
 * version 070114 reduced access level where possible
 * version 070111 adapted change in printPos and compErr
 * version 070106 removed genOutputAddr
 * version 061229 removed access to firstInstr
 * version 061203 optimized calls to printPos and compErr
 * version 060607 initial version
 */

public class ExPrePst extends Expr
{
	private final Expr ex;
	private final int op;
	private final boolean pre; //true for pre-, false for post-operation
	
	protected ExPrePst(Expr iex, int iop, boolean ipr, int fid, int il, int ic)
	{
		super(fid, il, ic);
		ex = iex;
		op = iop;
		pre = ipr;
	}
	
	public void printExpression(CodePrinter codePrnt)
	{
		codePrnt.exprPrePst(ex, op & 0xFFFF, pre);
	}
	
	public boolean resolve(Unit unitContext, Mthd mthdContext, int resolveFlags, TypeRef preferredType, Context ctx)
	{
		//check normal expressions
		if (!ex.resolve(unitContext, mthdContext, resolveFlags | RF_CHECKREAD, preferredType, ctx))
			return false;
		if (!ex.canGenAddr(unitContext, false, resolveFlags, ctx))
		{
			ex.printPos(ctx, "invalid operand for unary operation");
			return false;
		}
		//get resulting type
		getTypeOf(ex);
		//check type of operation
		switch (op >>> 16)
		{
			case Ops.S_PFX:
				if (arrDim > 0 || (baseType != T_BYTE && baseType != T_SHRT && baseType != T_INT && baseType != T_LONG))
				{
					printPos(ctx, "pre-/postfix operation only allowed for numbers");
					return false;
				}
				break;
			default:
				printPos(ctx, "unknown pre-/postfix operation");
				return false;
		}
		//everything OK
		effectType = EF_NORM; //pre/post-operations always have effect
		return true;
	}
	
	public void genOutput(Context ctx)
	{ //statement consists only of this expression
		int opPar = op & 0xFFFF;
		int addr, restore;
		
		//pre-op or post-op does not matter
		restore = ctx.arch.prepareFreeReg(0, 0, 0, StdTypes.T_PTR);
		addr = ctx.arch.allocReg();
		ex.genOutputAddr(addr, ctx);
		switch (opPar)
		{
			case Ops.P_DEC:
				ctx.arch.genDecMem(addr, baseType);
				break;
			case Ops.P_INC:
				ctx.arch.genIncMem(addr, baseType);
				break;
			default:
				compErr(ctx, "ExPrePst.genOutput with invalid preOp");
				return;
		}
		ctx.arch.deallocRestoreReg(addr, 0, restore);
	}
	
	public void genOutputVal(int reg, Context ctx)
	{
		int opPar = op & 0xFFFF;
		int addr, restore;
		
		if (pre)
		{ //pre-op
			restore = ctx.arch.prepareFreeReg(0, 0, reg, StdTypes.T_PTR);
			addr = ctx.arch.allocReg();
			ex.genOutputAddr(addr, ctx);
			switch (opPar)
			{
				case Ops.P_DEC:
					ctx.arch.genDecMem(addr, baseType);
					break;
				case Ops.P_INC:
					ctx.arch.genIncMem(addr, baseType);
					break;
				default:
					compErr(ctx, "ExPrePst.genOutputVal with invalid preOp");
					return;
			}
			ctx.arch.genLoadVarVal(reg, addr, null, 0, baseType);
			ctx.arch.deallocRestoreReg(addr, reg, restore);
		}
		else
		{ //post-op
			restore = ctx.arch.prepareFreeReg(reg, 0, 0, StdTypes.T_PTR);
			addr = ctx.arch.allocReg();
			ex.genOutputAddr(addr, ctx);
			ctx.arch.genLoadVarVal(reg, addr, null, 0, baseType);
			switch (opPar)
			{
				case Ops.P_DEC:
					ctx.arch.genDecMem(addr, baseType);
					break;
				case Ops.P_INC:
					ctx.arch.genIncMem(addr, baseType);
					break;
				default:
					compErr(ctx, "ExPrePst.genOutputVal with invalid pstOp");
					return;
			}
			ctx.arch.deallocRestoreReg(addr, 0, restore);
		}
	}
}
