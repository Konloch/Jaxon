/* Copyright (C) 2007, 2008, 2009, 2010 Stefan Frenz
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

/**
 * ExConv: basix routines to support conversion and typecheck of objects
 *
 * @author S. Frenz
 * @version 101210 adopted changed Architecture
 * version 101015 adopted changed Expr
 * version 100429 added check for genIntfParents if interface arrays are converted
 * version 100426 made class non-abstract and methods public to support special conversions
 * version 100420 added check for flash types
 * version 091215 fixed reg-prepare for std-type-arrays
 * version 091021 adopted changed modifier declarations
 * version 091001 adopted changed memory interface
 * version 090619 adopted changed architecture
 * version 090207 added copyright notice and changed genPushNull to genPushConstVal
 * version 081203 fixed reference to array of base types
 * version 080604 made main functionallity of genIsType static to provide global functionallity
 * version 080121 relaxed handling of conversions from/to basic type arrays
 * version 070628 adopted changes for "Expr extends TypeRef"
 * version 070522 added support to check unit descriptor necessity
 * version 070505 adopted change in Architecture
 * version 070330 bugfixed array to object conversion
 * version 070114 reduced access level where possible
 * version 070113 changed runtime environment to support conversion to array
 * version 070111 initial version
 */

public class ExCheckType extends Expr
{
	public final static int C_ISINST = 1;
	public final static int C_ISIMPL = 2;
	public final static int C_ISARRAY = 3;
	
	protected int checkMode;
	protected UnitList runtimeClass;
	private UnitList importedClass;
	private TypeRef dstType;
	
	public ExCheckType(int fid, int il, int ic)
	{
		super(fid, il, ic);
	}
	
	//this class is only a helper for conversions, overwrite methods if used as real Expr
	public void genOutputVal(int reg, Context ctx)
	{
		compErr(ctx, "invalid call to ExCheckType.genOutputVal");
	}
	
	public boolean resolve(Unit unitContext, Mthd mthdContext, int resolveFlags, TypeRef preferredType, Context ctx)
	{
		compErr(ctx, "invalid call to ExCheckType.resolve");
		return false;
	}
	
	public boolean prepareObjectConversion(TypeRef srcType, TypeRef destType, Unit unitContext, Context ctx)
	{
		boolean dstArr;
		Unit dstUnit = null;
		
		dstArr = destType.arrDim > 0;
		if ((srcType.baseType > 0 && srcType.arrDim == 0) || (destType.baseType != TypeRef.T_QID && !dstArr))
		{
			printPos(ctx, "basic type not valid");
			return false;
		}
		if (destType.baseType == TypeRef.T_QID)
			dstUnit = destType.qid.unitDest;
		if (srcType.baseType < 0 && srcType.arrDim == 0 && srcType.qid == null)
		{
			printPos(ctx, "source-type will never match");
			return false;
		}
		if (srcType.isStructType() || srcType.isFlashType() || destType.isStructType() || destType.isFlashType())
		{
			printPos(ctx, "STRUCT and FLASH not typecheckable");
			return false;
		}
		if (dstArr)
		{
			ctx.rteSArray.modifier |= Modifier.MA_ACCSSD;
			checkMode = C_ISARRAY;
			if (dstUnit != null && (dstUnit.modifier & Modifier.M_INDIR) != 0 && !ctx.genIntfParents)
			{
				printPos(ctx, "interface array conversion without parents array option");
				ctx.out.println();
			}
		}
		else
		{
			dstUnit.modifier |= Modifier.MA_ACCSSD;
			if ((dstUnit.modifier & Modifier.M_INDIR) != 0)
			{
				checkMode = C_ISIMPL;
			}
			else
				checkMode = C_ISINST;
		}
		if (ctx.dynaMem)
		{
			if (dstUnit != null)
				importedClass = unitContext.getRefUnit(dstUnit, true);
			runtimeClass = unitContext.getRefUnit(ctx.rteDynamicRuntime, true);
		}
		dstType = destType;
		return true;
	}
	
	public void genIsType(int dst, int src, boolean asCast, Context ctx)
	{
		genIsType(dst, src, asCast, dstType, checkMode, runtimeClass, importedClass, ctx);
	}
	
	public static void genIsType(int dst, int src, boolean asCast, TypeRef dstType, int checkMode, UnitList runtimeClass, UnitList importedClass, Context ctx)
	{
		int callRestore, unitRestore = 0, unitReg = 0;
		
		//pointer to an object is in src, we have to put pointer to casted object or interface in dst
		callRestore = ctx.arch.ensureFreeRegs(dst, src, 0, 0); //these registers are handled by hand
		if (dstType.qid != null)
		{
			unitRestore = ctx.arch.prepareFreeReg(0, 0, src, StdTypes.T_PTR);
			unitReg = ctx.arch.allocReg();
		}
		if (asCast)
			ctx.arch.genPush(src, StdTypes.T_PTR);
		if (ctx.dynaMem)
			ctx.arch.genSaveUnitContext();
		ctx.arch.genPush(src, StdTypes.T_PTR);
		if (checkMode == C_ISARRAY)
			ctx.arch.genPushConstVal(dstType.baseType, StdTypes.T_INT);
		if (dstType.qid != null)
		{ //
			if (ctx.dynaMem)
				ctx.arch.genLoadUnitContext(unitReg, importedClass.relOff);
			else
				ctx.arch.genLoadConstUnitContext(unitReg, dstType.qid.unitDest.outputLocation);
			ctx.arch.genPush(unitReg, StdTypes.T_PTR);
		}
		else
			ctx.arch.genPushConstVal(0, StdTypes.T_PTR); //may only occur if checkMode==C_ISARRAY
		if (checkMode == C_ISARRAY)
			ctx.arch.genPushConstVal(dstType.arrDim, StdTypes.T_INT);
		ctx.arch.genPushConstVal(asCast ? 1 : 0, StdTypes.T_BOOL);
		if (ctx.dynaMem)
		{
			ctx.arch.genLoadUnitContext(ctx.arch.regClss, runtimeClass.relOff);
			switch (checkMode)
			{
				case C_ISINST:
					ctx.arch.genCall(ctx.rteDRIsInstMd.relOff, ctx.arch.regClss, ctx.rteDRIsInstMd.parSize);
					break;
				case C_ISIMPL:
					ctx.arch.genCall(ctx.rteDRIsImplMd.relOff, ctx.arch.regClss, ctx.rteDRIsImplMd.parSize);
					break;
				case C_ISARRAY:
					ctx.arch.genCall(ctx.rteDRIsArrayMd.relOff, ctx.arch.regClss, ctx.rteDRIsArrayMd.parSize);
					break;
			}
			ctx.arch.genRestUnitContext();
		}
		else
		{
			switch (checkMode)
			{
				case C_ISINST:
					ctx.arch.genCallConst(ctx.rteDRIsInstMd, ctx.rteDRIsInstMd.parSize);
					break;
				case C_ISIMPL:
					ctx.arch.genCallConst(ctx.rteDRIsImplMd, ctx.rteDRIsImplMd.parSize);
					break;
				case C_ISARRAY:
					ctx.arch.genCallConst(ctx.rteDRIsArrayMd, ctx.rteDRIsArrayMd.parSize);
					break;
			}
		}
		if (checkMode == C_ISIMPL)
		{ //interface
			if (asCast)
				ctx.arch.genMoveIntfMapFromPrimary(dst); //move map from result to secodary reg for use
			else
				ctx.arch.genMoveFromPrimary(dst, StdTypes.T_PTR); //move map from result to primary reg for check
		}
		else if (!asCast)
			ctx.arch.genMoveFromPrimary(dst, StdTypes.T_BOOL); //if no cast, result not of interest
		if (asCast)
			ctx.arch.genPop(dst, StdTypes.T_PTR);
		if (dstType.qid != null)
			ctx.arch.deallocRestoreReg(unitReg, src, unitRestore);
		ctx.arch.deallocRestoreReg(0, 0, callRestore);
	}
}
