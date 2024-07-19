/* Copyright (C) 2007, 2008, 2009, 2010, 2012, 2015 Stefan Frenz
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
 * ExDeArray: array dereferenzation
 *
 * @author S. Frenz
 * @version 151019 fixed bound check for flash arrays
 * version 120925 added support for code printer
 * version 120228 cleaned up "import sjc." typo
 * version 101210 adopted changed Architecture
 * version 101101 adopted changed Architecture
 * version 101015 adopted changed Expr
 * version 100929 allowed char as base type for array index
 * version 100512 adopted changed Modifier/Marks
 * version 100504 adopted changed Expr
 * version 100428 fixed interface array special
 * version 100426 added support for interface arrays
 * version 100413 fixed support for enhanced for loops in indirScalar mode
 * version 100412 fixed support for enhanced for loops
 * version 100411 separated address and value generation to support enhanced for loops
 * version 100319 allowed unlimited indices for struct inline arrays
 * version 100312 adopted changed TypeRef and added support for flash objects
 * version 091112 added support for explicitTypeConversion
 * version 091021 adopted changed modifier declarations
 * version 091005 adopted changed Expr with support for preferredType in resolving
 * version 091001 adopted changed memory interface
 * version 090724 adopted changed Expr
 * version 090718 adopted changed Expr
 * version 090702 fixed invalid address register recycling in genOutputAddr
 * version 090619 adopted changed Architecture
 * version 090207 added copyright notice
 * version 090206 adopted changed Expr.genOutputAssignTo
 * version 081021 adopted changed Architecture.genComp/genCondJmp
 * version 080525 adopted changed genCondJmp signature
 * version 080122 optimized push-pop-sequence for type-checking in assign
 * version 080119 added support for type-checking in assign
 * version 070909 optimized signature of Expr.resolve
 * version 070727 replaced exSubResolve by resolve as there is nothing done anymore
 * version 070628 adopted changes for "Expr extends TypeRef"
 * version 070528 optimized unit import check
 * version 070526 added support for inline arrays in instances
 * version 070331 added support for additional indirect scalars in SArray
 * version 070303 added support for movable indirect scalars
 * version 070208 fixed object array offset
 * version 070114 fixed SSA-relevent register duplication, reduced access level where possible
 * version 070113 adopted change of genCheckNull to genCompPtrToNull
 * version 070111 adapted change in printPos and compErr
 * version 070106 initial version
 */

public class ExDeArray extends Expr
{
	protected Expr le, ind;
	private UnitList runtimeClass;
	private ExCheckType intfConv;
	
	protected ExDeArray(int fid, int il, int ic)
	{
		super(fid, il, ic);
	}
	
	public void printExpression(CodePrinter codePrnt)
	{
		codePrnt.exprDeArray(le, ind);
	}
	
	public boolean resolve(Unit unitContext, Mthd mthdContext, int resolveFlags, TypeRef preferredElemType, Context ctx)
	{
		TypeRef preferredObjType = null;
		
		if (preferredElemType != null)
		{
			preferredObjType = new TypeRef(fileID, line, col);
			preferredObjType.getTypeOf(preferredElemType);
			preferredObjType.arrDim++;
		}
		if (!le.resolve(unitContext, mthdContext, resolveFlags | RF_CHECKREAD, preferredObjType, ctx))
			return false;
		if (le.arrDim < 1)
		{
			printPos(ctx, "left side is not an array");
			return false;
		}
		if (ind != null)
		{ //ind==null only for internally inserted ExDeArrays
			if (!ind.resolve(unitContext, mthdContext, resolveFlags | RF_CHECKREAD, ctx.intType, ctx))
			{
				ctx.out.print(" in array-deref");
				return false;
			}
			if ((mthdContext.marker & Marks.K_EXPC) == 0)
				switch (ind.getBaseTypeConvertible())
				{
					case StdTypes.T_BYTE:
					case StdTypes.T_SHRT:
					case StdTypes.T_CHAR:
						if ((ind = ExEnc.getConvertedResolvedExpr(ind, ctx.intType, unitContext, ctx)) == null)
							return false;
				}
			if (!ind.isIntType())
			{
				printPos(ctx, "need int-type in array-deref");
				return false;
			}
		}
		getTypeOf(le); //get a copy, we will modify arrDim
		arrDim--;
		if (isIntfType())
		{
			intfConv = new ExCheckType(fileID, line, col);
			intfConv.prepareObjectConversion(ctx.objectType, this, unitContext, ctx);
		}
		//import runtimeClass if necessary
		if (ctx.dynaMem && (ctx.runtimeBound || ctx.runtimeNull || ctx.doArrayStoreCheck) && ind != null)
			runtimeClass = unitContext.getRefUnit(ctx.rteDynamicRuntime, true);
		//everything ok
		return true;
	}
	
	public boolean canGenAddr(Unit unitContext, boolean allowSpecialWriteAccess, int resolveFlags, Context ctx)
	{
		if (ctx.dynaMem && ctx.doArrayStoreCheck && runtimeClass == null)
			runtimeClass = unitContext.getRefUnit(ctx.rteDynamicRuntime, true);
		return true;
	}
	
	public void genOutputAddr(int reg, Context ctx)
	{
		genOutputAddr(0, reg, 0, ctx);
	}
	
	protected void genOutputAddrFirstStopElem(int firstAddrReg, int stopAddrReg, Context ctx)
	{
		genOutputAddr(0, stopAddrReg, firstAddrReg, ctx);
	}
	
	protected int getElemSize(Context ctx)
	{
		int regType;
		if (le.typeSpecial > 0)
		{ //valid struct array special
			if (baseType > 0)
				return TypeRef.getMinSize(baseType);
			else if (baseType == TypeRef.T_QID)
				return qid.unitDest.instScalarTableSize;
		}
		//normal element or invalid struct array special (will stop the compiler in genOutputAddr)
		return (regType = getRegType(ctx)) == StdTypes.T_PTR || regType == StdTypes.T_DPTR ? -ctx.arch.relocBytes : TypeRef.getMinSize(regType);
	}
	
	public int getAssignType(Context ctx)
	{
		return (baseType < 0 || arrDim > 0) ? StdTypes.T_PTR : baseType;
	}
	
	private void genOutputAddr(int lePtr, int addrReg, int elem0Reg, Context ctx)
	{ //if elem0Reg!=0, put addr stop-element in addrReg and addr of first element in elem0Reg
		int obj, objRestore = 0, pos, restore, index, size, lenPos = ctx.rteSArrayLength, condHnd;
		int indirBaseObj = 0, indirRestore = 0;
		Unit container = ctx.rteSArray;
		AccVar inlDecl;
		Instruction excCheckDone;
		
		if (lePtr == 0)
		{ //left side is not needed after outputAddr
			objRestore = ctx.arch.prepareFreeReg(0, 0, elem0Reg == 0 ? addrReg : 0, StdTypes.T_PTR);
			obj = ctx.arch.allocReg();
		}
		else
			obj = lePtr; //remember pointer to array for caller
		le.genOutputVal(obj, ctx);
		switch (le.typeSpecial)
		{
			case TypeRef.S_FLASHINLARR: //flash instance inline array
			case TypeRef.S_INSTINLARR: //instance inline array
				if ((inlDecl = le.getDestVar()) == null)
				{
					compErr(ctx, "could not resolve variable of inline array");
					return;
				}
				lenPos = inlDecl.relOff;
				container = inlDecl.owner;
				//no break
			case TypeRef.S_FLASHREF: //flash instance array (both normal and inline)
			case TypeRef.S_NOSPECIAL: //instance array (both normal and inline)
				if (ctx.runtimeNull)
				{
					excCheckDone = ctx.arch.getUnlinkedInstruction();
					condHnd = ctx.arch.genCompPtrToNull(obj, Ops.C_NE);
					ctx.arch.genCondJmp(excCheckDone, condHnd);
					if (ctx.dynaMem)
					{
						ctx.arch.genSaveUnitContext();
						ctx.arch.genLoadUnitContext(ctx.arch.regClss, runtimeClass.relOff);
						ctx.arch.genCall(ctx.rteDRNullExcMd.relOff, ctx.arch.regClss, ctx.rteDRNullExcMd.parSize);
						ctx.arch.genRestUnitContext();
					}
					else
						ctx.arch.genCallConst(ctx.rteDRNullExcMd, ctx.rteDRNullExcMd.parSize);
					ctx.arch.appendInstruction(excCheckDone);
				}
				size = getElemSize(ctx);
				restore = ctx.arch.prepareFreeReg(obj, addrReg, 0, StdTypes.T_INT);
				index = ctx.arch.allocReg();
				if (elem0Reg == 0)
				{ //normal de-array
					ind.genOutputVal(index, ctx);
					if (ctx.doBoundCheck)
					{
						excCheckDone = ctx.arch.getUnlinkedInstruction();
						if (le.typeSpecial == TypeRef.S_FLASHINLARR || le.typeSpecial == TypeRef.S_FLASHREF)
							ctx.arch.nextValInFlash();
						ctx.arch.genCheckBounds(obj, index, lenPos, excCheckDone);
						if (ctx.runtimeBound)
						{
							if (ctx.dynaMem)
							{
								ctx.arch.genSaveUnitContext();
								ctx.arch.genPush(obj, StdTypes.T_PTR);
								ctx.arch.genPush(index, StdTypes.T_INT);
								ctx.arch.genLoadUnitContext(ctx.arch.regClss, runtimeClass.relOff);
								ctx.arch.genCall(ctx.rteDRBoundExcMd.relOff, ctx.arch.regClss, ctx.rteDRBoundExcMd.parSize);
								ctx.arch.genRestUnitContext();
							}
							else
							{
								ctx.arch.genPush(obj, StdTypes.T_PTR);
								ctx.arch.genPush(index, StdTypes.T_INT);
								ctx.arch.genCallConst(ctx.rteDRBoundExcMd, ctx.rteDRBoundExcMd.parSize);
							}
						}
						else
							ctx.arch.genNativeBoundException();
						ctx.arch.appendInstruction(excCheckDone);
					}
				}
				else
					ctx.arch.genLoadVarVal(index, obj, null, lenPos, StdTypes.T_INT); //calculate address of stop element
				if (size > 0)
				{ //scalars
					if (ctx.indirScalars)
					{ //movable indirect scalars
						indirBaseObj = obj;
						indirRestore = ctx.arch.prepareFreeReg(index, obj, addrReg, StdTypes.T_PTR);
						obj = ctx.arch.allocReg();
						ctx.arch.genLoadVarVal(obj, indirBaseObj, null, ctx.indirScalarAddrOff, StdTypes.T_PTR);
						pos = container.instIndirScalarTableSize;
					}
					else
						pos = container.instScalarTableSize; //normal scalars
				}
				else
					pos = -(container.instRelocTableEntries + 1) * ctx.arch.relocBytes; //relocs
				ctx.arch.genLoadDerefAddr(addrReg, obj, index, pos, size);
				if (indirBaseObj != 0)
				{ //clean up indir
					ctx.arch.deallocRestoreReg(indirBaseObj, addrReg, indirRestore);
					obj = indirBaseObj;
				}
				if (elem0Reg != 0)
				{ //also get address of first element
					if (ctx.indirScalars && size > 0)
						ctx.arch.genLoadVarVal(elem0Reg, obj, null, ctx.indirScalarAddrOff, StdTypes.T_PTR);
					else
						ctx.arch.genLoadVarAddr(elem0Reg, obj, null, pos);
				}
				ctx.arch.deallocRestoreReg(index, 0, restore);
				break;
			default: //struct inline array with positive limited index
				if (le.typeSpecial < 0)
				{
					compErr(ctx, "ExDeArray.genOutputAddr with unknown typeSpecial");
					return;
				}
				//no break
			case TypeRef.S_STRUCTARRDONTCHECK: //struct inline array with unlimited index
			case TypeRef.S_STRUCTARRNOTSPEC: //struct inline array with unlimited index
				if (baseType > 0)
					size = TypeRef.getMinSize(baseType);
				else if (baseType == TypeRef.T_QID)
					size = qid.unitDest.instScalarTableSize;
				else
				{
					compErr(ctx, "ExDeArray.genOutputAddr with invalid struct-deref");
					return;
				}
				if (elem0Reg == 0 && ind.calcConstantType(ctx) == StdTypes.T_INT)
				{ //only relevant in normal mode
					pos = ind.getConstIntValue(ctx);
					if (le.typeSpecial > 0 && (pos < 0 || pos >= le.typeSpecial))
					{
						compErr(ctx, "constant index exceeds bounds of inlined array");
						return;
					}
					pos *= size;
					ctx.arch.genLoadVarAddr(addrReg, obj, null, pos);
				}
				else
				{
					restore = ctx.arch.prepareFreeReg(obj, addrReg, 0, StdTypes.T_INT);
					index = ctx.arch.allocReg();
					if (elem0Reg == 0)
					{ //normal de-array
						ind.genOutputVal(index, ctx);
						if (ctx.doBoundCheck && le.typeSpecial > 0)
						{ //insert check for array index
							excCheckDone = ctx.arch.getUnlinkedInstruction();
							condHnd = ctx.arch.genCompValToConstVal(index, le.typeSpecial, StdTypes.T_INT, Ops.C_BO);
							ctx.arch.genCondJmp(excCheckDone, condHnd);
							if (ctx.runtimeBound)
							{
								if (ctx.dynaMem)
								{
									ctx.arch.genSaveUnitContext();
									ctx.arch.genPush(obj, StdTypes.T_PTR);
									ctx.arch.genPush(index, StdTypes.T_INT);
									ctx.arch.genLoadUnitContext(ctx.arch.regClss, runtimeClass.relOff);
									ctx.arch.genCall(ctx.rteDRBoundExcMd.relOff, ctx.arch.regClss, ctx.rteDRBoundExcMd.parSize);
									ctx.arch.genRestUnitContext();
								}
								else
								{
									ctx.arch.genPush(obj, StdTypes.T_PTR);
									ctx.arch.genPush(index, StdTypes.T_INT);
									ctx.arch.genCallConst(ctx.rteDRBoundExcMd, ctx.rteDRBoundExcMd.parSize);
								}
							}
							else
								ctx.arch.genNativeBoundException();
							ctx.arch.appendInstruction(excCheckDone);
						}
					}
					else
					{ //first and stop element
						if (le.typeSpecial <= 0)
						{
							le.printPos(ctx, "can not generate last element of STRUCT-array with undefined length");
							ctx.out.println();
							ctx.err = true;
							//no return because clean up of register is required
						}
						ctx.arch.genLoadConstVal(index, le.typeSpecial, StdTypes.T_INT);
					}
					if (elem0Reg != 0)
						ctx.arch.genDup(elem0Reg, obj, StdTypes.T_PTR); //also get address of first element
					ctx.arch.genLoadDerefAddr(addrReg, obj, index, 0, size);
					ctx.arch.deallocRestoreReg(index, 0, restore);
				}
				break;
		}
		if (lePtr == 0)
			ctx.arch.deallocRestoreReg(obj, addrReg, objRestore);
	}
	
	public void genOutputPrepareAssignTo(int destReg, int newValueReg, Expr newValueEx, Context ctx)
	{
		int lePtr, restore1, restore2;
		
		if (ctx.doArrayStoreCheck && (newValueEx.isObjType() || newValueEx.isIntfType()))
		{
			restore1 = ctx.arch.prepareFreeReg(destReg, newValueReg, 0, StdTypes.T_PTR);
			lePtr = ctx.arch.allocReg();
			genOutputAddr(lePtr, destReg, 0, ctx);
			newValueEx.genOutputVal(newValueReg, ctx);
			restore2 = ctx.arch.ensureFreeRegs(lePtr, newValueReg, 0, 0);
			ctx.arch.genPush(newValueReg, StdTypes.T_PTR);
			if (ctx.dynaMem)
				ctx.arch.genSaveUnitContext();
			ctx.arch.genPush(lePtr, StdTypes.T_PTR);
			ctx.arch.genPush(newValueReg, StdTypes.T_PTR);
			if (ctx.dynaMem)
			{
				ctx.arch.genLoadUnitContext(ctx.arch.regClss, runtimeClass.relOff);
				ctx.arch.genCall(ctx.rteDRCheckArrayStoreMd.relOff, ctx.arch.regClss, ctx.rteDRCheckArrayStoreMd.parSize);
				ctx.arch.genRestUnitContext();
			}
			else
				ctx.arch.genCallConst(ctx.rteDRCheckArrayStoreMd, ctx.rteDRCheckArrayStoreMd.parSize);
			ctx.arch.genPop(newValueReg, StdTypes.T_PTR);
			ctx.arch.deallocRestoreReg(0, 0, restore2);
			ctx.arch.deallocRestoreReg(lePtr, 0, restore1);
		}
		else
			super.genOutputPrepareAssignTo(destReg, newValueReg, newValueEx, ctx);
	}
	
	public void genOutputVal(int reg, Context ctx)
	{
		int restore, addr;
		
		restore = ctx.arch.prepareFreeReg(0, 0, reg, StdTypes.T_PTR);
		addr = ctx.arch.allocReg();
		genOutputAddr(0, addr, 0, ctx);
		genOutputValOfElemAddr(reg, addr, ctx);
		ctx.arch.deallocRestoreReg(addr, reg, restore);
	}
	
	protected void genOutputValOfElemAddr(int reg, int addrReg, Context ctx)
	{
		if (qid != null && (qid.unitDest.modifier & Modifier.M_STRUCT) != 0)
		{ //struct de-array
			ctx.arch.genDup(reg, addrReg, StdTypes.T_PTR);
		}
		else if (intfConv != null)
		{ //interface de-array
			ctx.arch.genLoadVarVal(reg, addrReg, null, 0, StdTypes.T_PTR);
			intfConv.genIsType(reg, reg, true, ctx);
		}
		else
		{ //normal de-array
			if (le.typeSpecial == TypeRef.S_FLASHINLARR || le.typeSpecial == TypeRef.S_FLASHREF)
				ctx.arch.nextValInFlash();
			ctx.arch.genLoadVarVal(reg, addrReg, null, 0, getRegType(ctx));
		}
	}
}
