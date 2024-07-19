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

package sjc.compbase;

import sjc.backend.Instruction;
import sjc.debug.CodePrinter;

/**
 * Expr: platform and language independent expression
 *
 * @author S. Frenz
 * @version 120925 added support for code printer
 * version 120404 removed RF_IN_SUPER as it is no longer used
 * version 120402 added RF_IN_SUPER to support required visibility checks
 * version 120227 cleaned up "package sjc." typo
 * version 101015 changed resolve signature to support multiple resolve flags
 * version 100504 added getRightMostExpr, getLeftOfRightMostExpr, isSuperThisCall, resolveSuperThisCall, getDestVrbl
 * version 100426 added getAssignType to support interface arrays
 * version 100409 adopted changed TypeRef, removal of resolveType-remapping
 * version 100408 added remapping of TypeRef.resolve as Expr.resolveType
 * version 100401 fixed parameter names of canGenAddr
 * version 100114 reorganized constant object handling
 * version 091005 added support for preferredType in resolving
 * version 090724 added flag to support loop dependant resolving
 * version 090718 added support for non-static final variables, check for constant zero expressions and check for value generation
 * version 090207 added copyright notice and inserted check for reg==0 in genOutputAssignTo
 * version 090206 generalized genOutputPrepareAssignRefTo to genOutputPrepareAssignTo
 * version 081023 optimized genOutputPrepareAssignRefTo
 * version 081021 adopted changed Architecture.genComp/genCondJmp
 * version 080613 changed hasEffect to effectType
 * version 080525 adopted changed Architecture.genCondJmp signature
 * version 080119 changed signature of canGenAddr
 * version 080118 added support for type-checking in assign
 * version 070918 simplified error reporting statements
 * version 070909 optimized signature of resolve
 * version 070829 moved magicType to java subpackage where it belongs
 * version 070727 replaced exSubResolve by resolve as there is nothing done anymore, move is* to TypeRef
 * version 070628 adopted changes for "Expr extends TypeRef"
 * version 070625 renamed TypeRef.S_* to T_* to conform with StdTypes
 * version 070114 reduced access level where possible
 * version 070111 adapted change in printPos and compErr
 * version 070106 made genOutputAddr non-abstract, added canGenAddr, renamed isAddrOnStack
 * version 061229 removed access to firstInstr
 * version 061203 optimized calls to printPos and compErr
 * version 061202 removed unused methods
 * version 061129 static TypeRef object moved dynamically to Context
 * version 061111 minor changes
 * version 061102 added isStructType()
 * version 061030 changed detection of indirectCall
 * version 060607 initial version
 */

public abstract class Expr extends TypeRef
{
	//negative values of effect type flags (EF_*) may be used by frontends
	public final static int EF_NONE = 0; //default: no effect
	public final static int EF_NORM = 1; //just do it
	
	//resolve type flags (RF_*)
	public final static int RF_NONE = 0x0; //default: no special
	public final static int RF_CHECKREAD = 0x1; //expression has to be readable
	public final static int RF_INSIDE_LOOP = 0x2; //expression is inside a loop
	public final static int RF_DEAD_CODE = 0x4; //expression is in a constantly dead code block
	
	public int effectType; //exSubResolve will change this if it has any effect
	
	public Expr(int fid, int il, int ic)
	{
		super(fid, il, ic);
	}
	
	public abstract boolean resolve(Unit unitContext, Mthd mthdContext, int resolveFlags, TypeRef preferredType, Context ctx); //has to set hasEffect
	
	public abstract void genOutputVal(int reg, Context ctx);
	
	//basic implementations to have a default or to report an error if not overwritten
	public void printExpression(CodePrinter prnt)
	{
		prnt.reportError(this, "expression does not support code printing");
	}
	
	public void genOutputAddr(int reg, Context ctx)
	{
		compErr(ctx, "invalid call to Expr.genOutputAddr");
	}
	
	public boolean canGenAddr(Unit unitContext, boolean allowSpecialWriteAccess, int resolveFlags, Context ctx)
	{ //by default, address is not available
		return false;
	}
	
	public int calcConstantType(Context ctx)
	{ //results 0 for not constant or StdTypes.T_INT or T_LONG (maybe later also T_FLT or T_DBL)
		return 0;
	}
	
	public int getConstIntValue(Context ctx)
	{
		compErr(ctx, "invalid call to Expr.getConstIntValue");
		return 0;
	}
	
	public long getConstLongValue(Context ctx)
	{
		compErr(ctx, "invalid call to Expr.getConstLongValue");
		return 0l;
	}
	
	public boolean isCompInitConstObject(Context ctx)
	{
		return false;
	}
	
	public ExConstInitObj getConstInitObj(Context ctx)
	{
		compErr(ctx, "invalid call to Expr.getConstInitObj");
		return null;
	}
	
	public boolean isAddrOnStack()
	{ //only expressions with addr on stack have to overwrite
		return false;
	}
	
	public boolean isConstZero()
	{
		return false;
	}
	
	public Expr getRightMostExpr()
	{
		return this;
	}
	
	public Expr getLeftOfRightMostExpr()
	{
		return this;
	}
	
	public AccVar getDestVar()
	{
		return null;
	}
	
	public boolean isSuperThisCall(Context ctx)
	{
		return false;
	}
	
	public boolean resolveSuperThisCall(Unit inUnit, Mthd inMthd, Context ctx)
	{
		return false;
	}
	
	public void genOutput(Context ctx)
	{
		if (effectType == EF_NONE)
		{
			printPos(ctx, "expression without effect");
			return;
		}
		genOutputVal(0, ctx);
	}
	
	public void genOutputPrepareAssignTo(int destReg, int newValueReg, Expr newValue, Context ctx)
	{
		if (isAddrOnStack())
		{ //if addr is on stack, delay address calculation
			newValue.genOutputVal(newValueReg, ctx);
			genOutputAddr(destReg, ctx);
		}
		else
		{ //if addr or value may be changed, do not delay address calculation
			genOutputAddr(destReg, ctx);
			newValue.genOutputVal(newValueReg, ctx);
		}
	}
	
	public int getAssignType(Context ctx)
	{
		return (baseType < 0 || arrDim > 0) ? (isIntfType() ? StdTypes.T_DPTR : StdTypes.T_PTR) : baseType;
	}
	
	public void genOutputAssignTo(int newValueReg, Expr newValue, Context ctx)
	{
		int addr, restore;
		int regRestore = 0;
		boolean deallocReg = false;
		if (newValueReg == 0)
		{
			regRestore = ctx.arch.prepareFreeReg(0, 0, 0, getRegType(ctx));
			newValueReg = ctx.arch.allocReg();
			deallocReg = true;
		}
		restore = ctx.arch.prepareFreeReg(newValueReg, 0, 0, StdTypes.T_PTR);
		addr = ctx.arch.allocReg();
		genOutputPrepareAssignTo(addr, newValueReg, newValue, ctx);
		ctx.arch.genAssign(addr, newValueReg, getAssignType(ctx));
		ctx.arch.deallocRestoreReg(addr, 0, restore);
		if (deallocReg)
			ctx.arch.deallocRestoreReg(newValueReg, 0, regRestore);
	}
	
	public void genOutputCondJmp(Instruction jumpDest, boolean isTrue, Instruction nextToken, Context ctx)
	{
		int reg, restore, condHnd;
		
		if (!isBoolType())
		{
			compErr(ctx, "Expr.genOutputCondJmp with not boolean type called");
			return;
		}
		restore = ctx.arch.prepareFreeReg(0, 0, 0, T_BOOL);
		reg = ctx.arch.allocReg();
		genOutputVal(reg, ctx);
		condHnd = ctx.arch.genCompValToConstVal(reg, 0, T_BOOL, isTrue ? Ops.C_NE : Ops.C_EQ);
		ctx.arch.deallocRestoreReg(reg, 0, restore);
		ctx.arch.genCondJmp(jumpDest, condHnd);
	}
}
