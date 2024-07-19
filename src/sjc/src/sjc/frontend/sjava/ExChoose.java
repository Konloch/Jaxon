/* Copyright (C) 2006, 2007, 2008, 2009, 2010, 2012 Stefan Frenz
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

import sjc.backend.Architecture;
import sjc.backend.Instruction;
import sjc.compbase.*;
import sjc.debug.CodePrinter;

/**
 * ExChoose: choosing expression
 *
 * @author S. Frenz
 * @version 120925 added support for code printer
 * version 101015 adopted changed Expr
 * version 091111 added support for instance final-var-init
 * version 091109 rewriting of var's written state checks
 * version 091005 adopted changed Expr with support for preferredType in resolving
 * version 090724 adopted changed Expr
 * version 090718 adopted changed Expr
 * version 090207 added copyright notice
 * version 080508 added flow hints
 * version 070910 support for constant null in center expression
 * version 070909 optimized signature of Expr.resolve
 * version 070727 replaced exSubResolve by resolve as there is nothing done anymore
 * version 070628 adopted changes for "Expr extends TypeRef"
 * version 070114 reduced access level where possible
 * version 070111 adapted change in printPos and compErr
 * version 070106 removed genOutputAddr
 * version 061229 removed access to firstInstr
 * version 061225 support for null-conversion
 * version 061203 optimized calls to printPos and compErr
 * version 061126 initial version
 */

public class ExChoose extends ExBin
{
	protected Expr ce; //extends ExBin, so le and ri are recycled
	
	protected ExChoose(int iop, int ira, int fid, int il, int ic)
	{
		super(iop, ira, fid, il, ic);
	}
	
	public void printExpression(CodePrinter codePrnt)
	{
		codePrnt.exprChoose(le, ce, ri);
	}
	
	public boolean resolve(Unit unitContext, Mthd mthdContext, int resolveFlags, TypeRef preferredType, Context ctx)
	{
		boolean trRes = false, faRes = false;
		VrblStateList preState, trState;
		
		//resolve left sub-expression
		if (!le.resolve(unitContext, mthdContext, resolveFlags | RF_CHECKREAD, ctx.boolType, ctx))
			return false; //left side always has impact on following code
		
		//resolve middle and right sub-expression
		preState = ctx.copyVrblListState(mthdContext.vars, mthdContext.checkInitVars);
		trRes = ce.resolve(unitContext, mthdContext, resolveFlags | RF_CHECKREAD, preferredType, ctx);
		trState = ctx.copyVrblListState(mthdContext.vars, mthdContext.checkInitVars);
		ctx.setVrblListState(mthdContext.vars, mthdContext.checkInitVars, preState);
		faRes = ri.resolve(unitContext, mthdContext, resolveFlags | RF_CHECKREAD, preferredType == null ? ce : preferredType, ctx);
		if (le.calcConstantType(ctx) == StdTypes.T_INT)
		{
			if (le.getConstIntValue(ctx) == 1)
			{
				//SUN-Java warns: ri.flowWarn(ctx, ERR_UNREACHABLE_CODE);
				ctx.setVrblListState(mthdContext.vars, mthdContext.checkInitVars, trState);
			}
			else
			{
				//SUN-Java warns: ce.flowWarn(ctx, ERR_UNREACHABLE_CODE);
				//do not change var-state as right state is the only relevant
			}
		}
		else
			ctx.setVrblStateCombined(mthdContext.vars, mthdContext.checkInitVars, trState);
		ctx.recycleVrblStatelist(trState);
		ctx.recycleVrblStatelist(preState);
		if (!trRes || !faRes)
			return false;
		
		//check types
		if (!le.isBoolType())
		{
			printPos(ctx, "need boolean type before choose operator");
			return false;
		}
		if (ce.baseType == StdTypes.T_NULL)
		{
			if (!nullCopy(ce, ri, ctx))
				return false;
		}
		else if (ri.baseType == StdTypes.T_NULL)
		{
			if (!nullCopy(ri, ce, ctx))
				return false;
		}
		switch (ce.compareType(ri, true, ctx))
		{
			case TypeRef.C_TT:
			case TypeRef.C_EQ:
				getTypeOf(ce);
				break;
			case TypeRef.C_OT:
				getTypeOf(ri);
				break;
			default:
				printPos(ctx, "cannot implicitly convert ");
				ce.printType(ctx.out);
				ctx.out.print("!=");
				ri.printType(ctx.out);
				return false;
		}
		return true;
	}
	
	public int calcConstantType(Context ctx)
	{
		if (le.calcConstantType(ctx) == StdTypes.T_INT)
		{
			if (le.getConstIntValue(ctx) == 1)
				return ce.calcConstantType(ctx);
			else
				return ri.calcConstantType(ctx);
		}
		return 0;
	}
	
	public int getConstIntValue(Context ctx)
	{
		if (le.calcConstantType(ctx) == StdTypes.T_INT)
		{
			if (le.getConstIntValue(ctx) == 1)
				return ce.getConstIntValue(ctx);
			else
				return ri.getConstIntValue(ctx);
		}
		return 0;
	}
	
	public long getConstLongValue(Context ctx)
	{
		if (le.calcConstantType(ctx) == StdTypes.T_INT)
		{
			if (le.getConstIntValue(ctx) == 1)
				return ce.getConstLongValue(ctx);
			else
				return ri.getConstLongValue(ctx);
		}
		return 0l;
	}
	
	public void genOutputVal(int reg, Context ctx)
	{
		int constType, id;
		Instruction ceIns, riIns, afterwards;
		
		if ((constType = calcConstantType(ctx)) > 0)
		{
			switch (constType)
			{
				case StdTypes.T_INT:
					ctx.arch.genLoadConstVal(reg, getConstIntValue(ctx), getRegType(ctx));
					return;
				case StdTypes.T_LONG:
					ctx.arch.genLoadConstDoubleOrLongVal(reg, getConstLongValue(ctx), false);
					return;
				default:
					compErr(ctx, "unkown constant type in ExChoose.genOutputVal");
					return;
			}
		}
		//not a constant condition, generate code
		ceIns = ctx.arch.getUnlinkedInstruction();
		riIns = ctx.arch.getUnlinkedInstruction();
		afterwards = ctx.arch.getUnlinkedInstruction();
		id = ctx.arch.insertFlowHint(Architecture.F_BLOCKSTART, 0);
		ctx.arch.insertFlowHint(Architecture.F_CONDSTART, id);
		le.genOutputCondJmp(riIns, false, ceIns, ctx);
		ctx.arch.insertFlowHint(Architecture.F_TRUESTART, id);
		ctx.arch.appendInstruction(ceIns);
		ce.genOutputVal(reg, ctx);
		ctx.arch.genJmp(afterwards);
		ctx.arch.insertFlowHint(Architecture.F_ELSESTART, id);
		ctx.arch.appendInstruction(riIns);
		ri.genOutputVal(reg, ctx);
		ctx.arch.appendInstruction(afterwards);
		ctx.arch.insertFlowHint(Architecture.F_BLOCKEND, id);
	}
	
	public void genOutputCondJmp(Instruction jumpDest, boolean isTrue, Instruction elseDest, Context ctx)
	{
		int res;
		Instruction ceIns, riIns, afterwards;
		
		if (baseType != StdTypes.T_BOOL || arrDim > 0)
		{
			compErr(ctx, "ExChoose.genOutputCondJump needs boolean type");
			return;
		}
		//check for constant jump
		if (calcConstantType(ctx) == StdTypes.T_INT)
		{
			res = getConstIntValue(ctx);
			//if ((isTrue && res==1) || (!isTrue && res==0)) ctx.arch.genJmp(jumpDest);
			if (res == (isTrue ? 1 : 0))
				ctx.arch.genJmp(jumpDest);
			return;
		}
		//not a constant condition, generate code
		ceIns = ctx.arch.getUnlinkedInstruction();
		riIns = ctx.arch.getUnlinkedInstruction();
		afterwards = ctx.arch.getUnlinkedInstruction();
		le.genOutputCondJmp(riIns, false, ceIns, ctx);
		ctx.arch.appendInstruction(ceIns);
		ce.genOutputCondJmp(jumpDest, isTrue, elseDest, ctx);
		ctx.arch.genJmp(afterwards);
		ctx.arch.appendInstruction(riIns);
		ri.genOutputCondJmp(jumpDest, isTrue, elseDest, ctx);
		ctx.arch.appendInstruction(afterwards);
	}
	
	private boolean nullCopy(Expr to, Expr from, Context ctx)
	{
		int res;
		
		res = from.getRegType(ctx);
		if (res == StdTypes.T_PTR)
			to.baseType = StdTypes.T_NNPT;
		else if (res == StdTypes.T_DPTR)
			to.baseType = StdTypes.T_NDPT;
		else
		{
			printPos(ctx, "not a pointer type");
			return false;
		}
		return true;
	}
}
