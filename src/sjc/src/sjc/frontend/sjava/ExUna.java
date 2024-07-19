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

import sjc.backend.Instruction;
import sjc.compbase.*;
import sjc.debug.CodePrinter;

/**
 * ExUna: unary expression
 *
 * @author S. Frenz
 * @version 120925 added support for code printer
 * version 110328 fixed load of constant double value
 * version 101210 adopted changed Architecture
 * version 101015 adopted changed Expr
 * version 100127 added optional runtime call depending of type and operator
 * version 091005 adopted changed Expr
 * version 090724 adopted changed Expr
 * version 090718 adopted changed Expr
 * version 090207 added copyright notice
 * version 070909 optimized signature of Expr.resolve
 * version 070808 added support for float and double
 * version 070727 replaced exSubResolve by resolve as there is nothing done anymore and optimized it
 * version 070628 adopted changes for "Expr extends TypeRef"
 * version 070114 reduced access level where possible
 * version 070111 adapted change in printPos and compErr
 * version 070106 removed genOutputAddr
 * version 061229 removed access to firstInstr
 * version 061203 optimized calls to printPos and compErr
 * version 060607 initial version
 */

public class ExUna extends Expr
{
	protected Expr ex;
	private final int op;
	private char ariCallOp;
	private boolean genAriCall;
	private UnitList runtimeClass;
	
	protected ExUna(int iop, int fid, int il, int ic)
	{
		super(fid, il, ic);
		op = iop;
	}
	
	public void printExpression(CodePrinter codePrnt)
	{
		codePrnt.exprUna(ex, op >>> 16, op & 0xFFFF);
	}
	
	public boolean resolve(Unit unitContext, Mthd mthdContext, int resolveFlags, TypeRef preferredType, Context ctx)
	{
		int opType, opPar;
		
		//check normal expressions
		if (!ex.resolve(unitContext, mthdContext, resolveFlags | RF_CHECKREAD, preferredType, ctx))
			return false;
		//get resulting type
		getTypeOf(ex);
		if (arrDim > 0)
		{
			printPos(ctx, "unary operation not allowed for arrays");
			return false;
		}
		//check type of operation
		opType = op >>> 16;
		opPar = op & 0xFFFF;
		switch (opType)
		{
			case Ops.S_ARI:
				if (baseType == T_FLT || baseType == T_DBL)
				{
					if (opPar != Ops.A_PLUS && opPar != Ops.A_MINUS)
					{
						printPos(ctx, "invalid arithmetic unary operation for floating point number");
						return false;
					}
				}
				else if ((baseType != T_BYTE && baseType != T_SHRT && baseType != T_INT && baseType != T_LONG) || (opPar != Ops.A_PLUS && opPar != Ops.A_MINUS && opPar != Ops.A_CPL))
				{
					printPos(ctx, "invalid number or invalid arithmetic operation");
					return false;
				}
				break;
			case Ops.S_LOG:
				if (baseType != T_BOOL || opPar != Ops.L_NOT)
				{
					printPos(ctx, "logical operation only allowed for booleans, only negation allowed");
					return false;
				}
				break;
			default:
				printPos(ctx, "unknown unary operation");
				return false;
		}
		//check if call is requested
		if (calcConstantType(ctx) == 0 && (ctx.arch.unaAriCall[baseType] & (1 << (opPar - Ops.MSKBSE))) != 0)
		{
			if (ctx.dynaMem)
				runtimeClass = unitContext.getRefUnit(ctx.rteDynamicAri, true);
			genAriCall = true;
			ctx.arch.unaAriCall[baseType] |= 0x80000000; //set sign-bit to request service
			switch (opPar)
			{
				case Ops.A_PLUS:
					ariCallOp = '+';
					break;
				case Ops.A_MINUS:
					ariCallOp = '-';
					break;
				case Ops.A_CPL:
					ariCallOp = '~';
					break;
				case Ops.L_NOT:
					ariCallOp = '!';
					break;
				default:
					compErr(ctx, "ExUna.exSubResolve with call-request for unchecked operation");
					return false;
			}
		}
		//everything OK
		return true;
	}
	
	public int calcConstantType(Context ctx)
	{
		return ex.calcConstantType(ctx);
	}
	
	public int getConstIntValue(Context ctx)
	{
		int v;
		int opType, par;
		
		v = ex.getConstIntValue(ctx);
		opType = op >>> 16;
		par = op & 0xFFFF;
		switch (opType)
		{
			case Ops.S_ARI:
				switch (par)
				{
					case Ops.A_PLUS:
						return v;
					case Ops.A_MINUS:
						return baseType == T_FLT ? ctx.arch.real.negateFloat(v) : -v;
					case Ops.A_CPL:
						return ~v;
				}
				break;
			case Ops.S_LOG:
				switch (par)
				{
					case Ops.L_NOT:
						return v ^ 1;
				}
		}
		ctx.out.println("### internal compiler error in ExUna.getConstIntValue");
		return 0;
	}
	
	public long getConstLongValue(Context ctx)
	{
		long v;
		int opType, par;
		
		v = ex.getConstLongValue(ctx);
		opType = op >>> 16;
		par = op & 0xFFFF;
		switch (opType)
		{
			case Ops.S_ARI:
				switch (par)
				{
					case Ops.A_PLUS:
						return v;
					case Ops.A_MINUS:
						return baseType == T_DBL ? ctx.arch.real.negateDouble(v) : -v;
					case Ops.A_CPL:
						return ~v;
				}
				break;
		}
		ctx.out.println("### internal compiler error in ExUna.getConstLongValue");
		return 0l;
	}
	
	public void genOutputVal(int reg, Context ctx)
	{
		int opType = op >>> 16, opPar = op & 0xFFFF, constType;
		int preVal, restore;
		
		if ((constType = calcConstantType(ctx)) > 0)
		{
			switch (constType)
			{
				case T_INT:
					ctx.arch.genLoadConstVal(reg, getConstIntValue(ctx), getRegType(ctx));
					return;
				case T_LONG:
					ctx.arch.genLoadConstDoubleOrLongVal(reg, getConstLongValue(ctx), baseType == T_DBL);
					return;
				default:
					compErr(ctx, "unkown constant type in ExUna.genOutputVal");
					return;
			}
		}
		restore = ctx.arch.prepareFreeReg(0, 0, reg, baseType);
		preVal = ctx.arch.allocReg();
		ex.genOutputVal(preVal, ctx);
		if (genAriCall)
			genOutAriCall(reg, preVal, ctx);
		else
			switch (opType)
			{
				case Ops.S_ARI:
					switch (opPar)
					{
						case Ops.A_PLUS:
						case Ops.A_MINUS:
						case Ops.A_CPL:
							ctx.arch.genUnaOp(reg, preVal, op, baseType);
							break;
						default:
							compErr(ctx, "internal error in arithmetic ExUna.genOutputVal");
							return;
					}
					break;
				case Ops.S_LOG:
					switch (opPar)
					{
						case Ops.L_NOT:
							ctx.arch.genUnaOp(reg, preVal, op, T_BOOL);
							break;
						default:
							compErr(ctx, "internal error in logical ExUna.genOutputVal");
							return;
					}
					break;
				default:
					compErr(ctx, "internal error in operation of ExUna.genOutputVal");
					return;
			}
		ctx.arch.deallocRestoreReg(preVal, reg, restore);
	}
	
	private void genOutAriCall(int reg, int reg1, Context ctx)
	{
		int restore;
		Mthd target = ctx.rteDABinAriCallMds[baseType];
		
		restore = ctx.arch.ensureFreeRegs(reg, 0, reg1, 0);
		if (ctx.dynaMem)
			ctx.arch.genSaveUnitContext();
		ctx.arch.genPush(reg1, baseType);
		ctx.arch.genPushConstVal(ariCallOp, T_BYTE);
		if (ctx.dynaMem)
		{
			ctx.arch.genLoadUnitContext(ctx.arch.regClss, runtimeClass.relOff);
			ctx.arch.genCall(target.relOff, ctx.arch.regClss, target.parSize);
			ctx.arch.genRestUnitContext();
		}
		else
			ctx.arch.genCallConst(target, target.parSize);
		ctx.arch.genMoveFromPrimary(reg, baseType);
		ctx.arch.deallocRestoreReg(0, reg, restore);
	}
	
	public void genOutputCondJmp(Instruction jumpDest, boolean isTrue, Instruction elseDest, Context ctx)
	{
		if (baseType != T_BOOL)
		{
			compErr(ctx, "ExUna.genOutputCondJump needs boolean type");
			return;
		}
		if (op != ((Ops.S_LOG << 16) | Ops.L_NOT))
		{
			compErr(ctx, "ExUna.genOutputCondJump with invalid operator");
			return;
		}
		ex.genOutputCondJmp(jumpDest, !isTrue, elseDest, ctx);
	}
}
