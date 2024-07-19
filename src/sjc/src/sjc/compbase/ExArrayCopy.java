/* Copyright (C) 2009, 2010, 2012 Stefan Frenz
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

import sjc.debug.CodePrinter;

/**
 * ExArrayCopy: runtime call to deliver a deep array copy of a constant array
 *
 * @author S. Frenz
 * @version 121014 added support for code printer
 * version 101210 adopted changed Architecture
 * version 101015 adopted changed Expr
 * version 100409 adopted changed TypeRef
 * version 100114 reorganized constant object handling
 * version 091210 added restrictive check of forcedType
 * version 091208 initial version
 */

public class ExArrayCopy extends Expr
{
	private UnitList runtimeClass;
	private final ExArrayInit array;
	private final TypeRef forcedType;
	private final boolean impl;
	
	public ExArrayCopy(ExArrayInit constArray, TypeRef type, boolean implicit)
	{
		super(constArray.fileID, constArray.line, constArray.col);
		array = constArray;
		forcedType = type;
		impl = implicit;
	}
	
	public void printExpression(CodePrinter prnt)
	{
		array.printExpression(prnt);
	}
	
	public boolean resolve(Unit unitContext, Mthd mthdContext, int resolveFlags, TypeRef preferredType, Context ctx)
	{
		int cmpRes;
		
		if (forcedType != null)
		{
			if (forcedType.qid != null && forcedType.qid.unitDest == null && !forcedType.resolveType(unitContext, ctx))
				return false;
			preferredType = forcedType;
		}
		if (!array.resolve(unitContext, mthdContext, resolveFlags, preferredType, ctx))
			return false;
		getTypeOf(array);
		if (forcedType != null && (cmpRes = forcedType.compareType(this, true, ctx)) != TypeRef.C_EQ && cmpRes != TypeRef.C_TT)
		{
			printPos(ctx, "array does not match required type");
			return false;
		}
		if (ctx.dynaMem)
			runtimeClass = unitContext.getRefUnit(ctx.rteDynamicRuntime, true);
		ctx.arrayDeepCopyUsed = true;
		if (impl && ctx.verbose)
		{
			printPos(ctx, "inserted implicit deepArrayCopy");
			ctx.out.println();
		}
		return true;
	}
	
	public void genOutputVal(int reg, Context ctx)
	{
		int callRestore, arrayRestore, regArray;
		callRestore = ctx.arch.ensureFreeRegs(reg, 0, 0, 0);
		arrayRestore = ctx.arch.prepareFreeReg(0, 0, reg, StdTypes.T_PTR);
		regArray = ctx.arch.allocReg();
		if (ctx.dynaMem)
		{
			ctx.arch.genLoadVarVal(regArray, ctx.arch.regClss, null, array.dest.relOff, StdTypes.T_PTR);
			ctx.arch.genSaveUnitContext();
		}
		else
			ctx.arch.genLoadVarAddr(regArray, 0, array.outputLocation, array.getOutputLocationOffset(ctx));
		ctx.arch.genPush(regArray, StdTypes.T_PTR);
		ctx.arch.deallocRestoreReg(regArray, reg, arrayRestore);
		if (ctx.dynaMem)
		{
			ctx.arch.genLoadUnitContext(ctx.arch.regClss, runtimeClass.relOff);
			ctx.arch.genCall(ctx.rteArrayDeepCopyMd.relOff, ctx.arch.regClss, ctx.rteArrayDeepCopyMd.parSize);
			ctx.arch.genRestUnitContext();
		}
		else
			ctx.arch.genCallConst(ctx.rteArrayDeepCopyMd, ctx.rteArrayDeepCopyMd.parSize);
		ctx.arch.genMoveFromPrimary(reg, StdTypes.T_PTR);
		ctx.arch.deallocRestoreReg(0, reg, callRestore);
	}
}
