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
 * ExDeRef: dereferenzation inside an expression
 *
 * @author S. Frenz
 * @version 120925 added support for code printer
 * version 120404 replaced RF_IN_SUPER by extra parameter, fixed code formatting
 * version 120402 added support for RF_IN_SUPER to enable special visibility checks for super-calls
 * version 120312 fixed name.this deref
 * version 110624 added support for name.this deref
 * version 101218 adopted changed Pack/Unit
 * version 101210 adopted changed Architecture
 * version 101102 fixed T_DESC mapping to T_PTR
 * version 101015 adopted changed Expr
 * version 100504 adopted changed Expr
 * version 100423 simplified security check of MAGIC access
 * version 100421 added security check of MAGIC access by asking osio
 * version 100420 added support deref of flash arrays
 * version 100401 removed MARKER-special
 * version 100311 adopted changed TypeRef
 * version 100114 reorganized constant object handling
 * version 100113 adopted changed MAGIC.resolve
 * version 091021 adopted changed modifier declarations and removed relation tracking
 * version 091009 added support for relation tracking
 * version 091005 adopted changed Expr
 * version 091001 adopted changed memory interface
 * version 090718 adopted changed Expr
 * version 090716 added check for call of abstract super method which is not allowed
 * version 090619 adopted changed Architecture
 * version 090616 adopted changed ExVar
 * version 090207 added copyright notice
 * version 090206 added specialized genOutputAssignTo
 * version 081021 adopted changed Architecture.genComp/genCondJmp
 * version 080614 adopted changed ExAccVrbl, adopted merging of T_CLSS/T_INTF in StdTypes
 * version 080613 adopted hasEffect->effectType
 * version 080525 adopted changed genCondJmp signature
 * version 080305 added object-/class-check if right side is call
 * version 080220 adopted changed signature of ExVar.varResolve
 * version 080119 adopted changed signature of Expr.canGenAddr
 * version 071227 fixed detection of invalid static access
 * version 071222 added support for deref-new (needed for inner units)
 * version 070909 optimized signature of Expr.resolve, cleaned up resolve
 * version 070829 added support for constObjects
 * version 070727 replaced exSubResolve by resolve as there is nothing done anymore
 * version 070722 fixed runtimeClass-setting for array access
 * version 070628 adopted changes for "Expr extends TypeRef"
 * version 070527 added detection of invalid dereferencation of inline arrays
 * version 070114 reduced access level where possible
 * version 070113 adopted change of genCheckNull to genCompPtrToNull
 * version 070111 adapted change in printPos and compErr
 * version 070106 cleaned up
 * version 070101 adopted change in genCall
 * version 061229 removed access to firstInstr
 * version 061222 bugfixed config for MAGIC and added MARKER-genOutputVal
 * version 061203 optimized calls to printPos and compErr
 * version 061202 adopted change of genCall, bugfix in code generation of super
 * version 061129 added checks for MAGIC.genOutputAddr
 * version 061112 removed not necessary loading of class in genOutputAddr
 * version 061027 optimized check for special names
 * version 060803 added check for ownership of static variables
 * version 060714 added null check to support runtime handler
 * version 060705 changed deref-execution order (now Java-standard)
 * version 060703 optimized register usage for static compilation
 * version 060628 added support for static compilation
 * version 060619 bugfix for register-allocation in genCondJmp
 * version 060607 initial version
 */

public class ExDeRef extends Expr
{
	private final static String ERRQID = "ExDeref with invalid le.qid";
	private final static String ERREXT = " in deref";
	
	protected Expr le, ri;
	private UnitList leImportedClassIndexed;
	private boolean isSuper, noGenLeftSide, leftStatic;
	private UnitList runtimeClass; //used for exception-call if not native
	private Magic magic;
	
	protected ExDeRef(int fid, int il, int ic)
	{
		super(fid, il, ic);
		isSuper = false; //super needs a very special handling
	}
	
	public void printExpression(CodePrinter codePrnt)
	{
		if (isSuper)
			codePrnt.exprSuper(ri);
		else if (le.baseType == StdTypes.T_MAGC)
			magic.printExpression(ri, codePrnt);
		else
			codePrnt.exprDeref(le, ri, leftStatic);
	}
	
	public boolean resolve(Unit unitContext, Mthd mthdContext, int resolveFlags, TypeRef preferredType, Context ctx)
	{
		ExVar asVar = null;
		ExCall asCall;
		ExNew asNew = null;
		Pack inPack = null;
		Unit inUnit = null;
		Mthd inMthd = null;
		CtxBasedConfig config;
		
		//try to resolve left side
		if (le instanceof ExVar)
		{ //this is the left-out-identifier, only this one has full search-space
			asVar = (ExVar) le;
			if (asVar.id.equals(SJava.KEY_MAGIC))
			{
				if (!ctx.isCrossCompilation() && !ctx.osio.checkMagicAccess())
				{
					le.printPos(ctx, "MAGIC access forbidden by operating system");
					return false;
				}
				le.baseType = StdTypes.T_MAGC;
			}
			else if (asVar.id.equals(SJava.KEY_THIS))
			{
				if (!asVar.varResolveThis(unitContext, mthdContext, ctx))
				{
					ctx.out.print(" in this-deref");
					return false;
				}
			}
			else if (asVar.id.equals(SJava.KEY_SUPER))
			{
				if (!asVar.varResolveSuper(unitContext, mthdContext, ctx))
				{
					ctx.out.print(" in super-deref");
					return false;
				}
				if (!(ri instanceof ExCall))
				{
					printPos(ctx, "super only supported for calls");
					return false;
				}
				isSuper = true;
				if (ctx.dynaMem)
					leImportedClassIndexed = asVar.importedClass;
			}
			else if (!asVar.varResolve(null, unitContext, mthdContext, unitContext, mthdContext, resolveFlags | RF_CHECKREAD, false, null, ctx))
				return false;
		}
		else
		{ //resolve normally
			if (!le.resolve(unitContext, mthdContext, resolveFlags | RF_CHECKREAD, null, ctx))
			{
				ctx.out.print(ERREXT);
				return false;
			}
		}
		//left operand resolved, try to resolve right identifier in context of left "expression"
		if (le.arrDim > 0)
		{
			if (le.typeSpecial != TypeRef.S_FLASHREF && le.typeSpecial != TypeRef.S_NOSPECIAL)
			{ //STRUCT and inline arrays are forbidden
				printPos(ctx, "inlined arrays can not be dereferenciated");
				return false;
			}
			//normal or flash array deref
			inUnit = ctx.rteSArray;
			inMthd = ctx.rteSArray.initDyna;
		}
		else
			switch (le.baseType)
			{
				case StdTypes.T_MAGC:
					config = ctx.config;
					while (!(config instanceof Magic))
					{
						if (config == null)
						{
							compErr(ctx, "ExDeref could not find Magic object");
							return false;
						}
						config = config.nextConfig;
					}
					if ((ri = (magic = (Magic) config).resolve(ri, unitContext, mthdContext, resolveFlags, preferredType, ctx)) == null)
						return false;
					getTypeOf(ri);
					effectType = ri.effectType;
					return true;
				case StdTypes.T_DESC:
					//left side is a class or interface, we don't have an object
					if (le.qid == null || le.qid.unitDest == null)
					{
						compErr(ctx, ERRQID);
						return false;
					}
					leftStatic = true;
					inUnit = le.qid.unitDest;
					inMthd = inUnit.initStat;
					if (ctx.dynaMem)
					{
						if ((leImportedClassIndexed = unitContext.getRefUnit(inUnit, true)) == null)
						{
							ctx.out.print(" in class-deref");
							return false;
						}
					}
					break;
				case StdTypes.T_PACK:
					//left side names a package, nothing to do for left side
					if (le.qid == null || le.qid.packDest == null)
					{
						compErr(ctx, ERRQID);
						return false;
					}
					leftStatic = true;
					inPack = le.qid.packDest;
					break;
				default:
					if (le.baseType == TypeRef.T_QID)
					{ //this is an instance of an object
						if (le.qid == null || le.qid.unitDest == null)
						{
							compErr(ctx, ERRQID);
							return false;
						}
						inUnit = le.qid.unitDest;
						inMthd = inUnit.initDyna;
					}
					else
					{
						printPos(ctx, "cannot dereference left item of type ");
						le.printType(ctx.out);
						return false;
					}
			}
		
		if (ri instanceof ExCall)
		{
			if (inUnit == null)
			{
				printPos(ctx, "call needs object or unit on left side");
				return false;
			}
			asCall = (ExCall) ri;
			if (!asCall.mthdResolve(inUnit, inMthd, unitContext, mthdContext, resolveFlags, isSuper, ctx))
			{
				ctx.out.print(ERREXT);
				return false;
			}
			if ((asCall.dest.modifier & Modifier.M_STAT) != 0)
			{
				leftStatic = true;
				if (((inMthd.modifier & Modifier.M_STAT) == 0 || inUnit != asCall.dest.owner))
				{
					printPos(ctx, "static method should be called through real owner class, discarding left side");
					ctx.out.println();
					asVar = new ExVar(asCall.dest.owner.name, le.fileID, le.line, le.col);
					asVar.qid = asCall.dest.owner.getQIDTo();
					le = asVar;
					le.baseType = StdTypes.T_DESC;
					if (ctx.dynaMem)
					{
						if ((leImportedClassIndexed = unitContext.getRefUnit(asVar.qid.unitDest, true)) == null)
						{
							ctx.out.print(" in class-deref");
							return false;
						}
					}
				}
			}
			if (isSuper && (asCall.dest.modifier & Modifier.M_ABSTR) != 0)
			{
				printPos(ctx, "can not call abstract method of super class");
				return false;
			}
		}
		else if (ri instanceof ExVar)
		{
			asVar = (ExVar) ri;
			if (asVar.id.equals(SJava.KEY_THIS))
			{ //special case for (inner) class referencing (outer) instance
				if (le.baseType != StdTypes.T_DESC)
				{
					printPos(ctx, "this in deref must have unit name on the left");
					return false;
				}
				if ((mthdContext.modifier & Modifier.M_STAT) != 0)
				{
					printPos(ctx, "can not access this in static method");
					return false;
				}
				//search referenced outer class
				asVar.outerLevel = 0;
				asVar.outerAccessStart = unitContext;
				Unit search = unitContext;
				while (search != null && search != inUnit)
				{
					if ((search.modifier & Modifier.M_STAT) != 0)
					{
						printPos(ctx, "static inner class must not reference outer instance");
						return false;
					}
					asVar.outerLevel++;
					search = search.outerUnit;
				}
				if (search == null)
				{
					printPos(ctx, "invalid class for this");
					return false;
				}
				asVar.qid = inUnit.getQIDTo();
				asVar.baseType = StdTypes.T_QID;
				asVar.isThis = true;
			}
			else
			{ //there is a name to resolve
				if (!asVar.varResolve(inPack, inUnit, inMthd, unitContext, mthdContext, resolveFlags, true, preferredType, ctx))
				{
					ctx.out.print(ERREXT);
					return false;
				}
				if (inPack != null)
					leftStatic = noGenLeftSide = true;
				else if (asVar.dest instanceof Vrbl && (asVar.dest.location == AccVar.L_CLSSSCL || asVar.dest.location == AccVar.L_CLSSREL))
				{
					if (inUnit != asVar.dest.owner)
					{
						printPos(ctx, "static variable should be accessed through real owner class, discarding left side");
						ctx.out.println();
						if (!asVar.ensureClassContext)
						{
							compErr(ctx, "!ensureClassContext in access to static variable through object");
							return false;
						}
						noGenLeftSide = true;
					}
				}
				//else: left side returns something to be generated at runtime
				if (asVar.dest != null && (asVar.dest.modifier & Modifier.M_STAT) != 0)
					leftStatic = true;
			}
			//check special case for flash array
			if (inUnit == ctx.rteSArray && le.typeSpecial == TypeRef.S_FLASHREF)
			{
				asVar.typeSpecial = TypeRef.S_FLASHREF;
			}
		}
		else if (ri instanceof ExNew)
		{
			if (le.baseType != T_QID)
			{
				printPos(ctx, "new in deref needs instance on left side");
				return false;
			}
			asNew = (ExNew) ri;
			if (!asNew.resolve(le.qid.unitDest.initDyna, unitContext, mthdContext, resolveFlags, preferredType, ctx))
			{
				ctx.out.print(ERREXT);
				return false;
			}
		}
		else
		{
			compErr(ctx, "unknown ExDeref.ri element type");
			return false;
		}
		if (ctx.runtimeNull && ctx.dynaMem)
			runtimeClass = unitContext.getRefUnit(ctx.rteDynamicRuntime, true);
		//result of this deref is type of our right-out-expression
		getTypeOf(ri);
		effectType = ri.effectType;
		return true;
	}
	
	public int calcConstantType(Context ctx)
	{
		return ri.calcConstantType(ctx);
	}
	
	public int getConstIntValue(Context ctx)
	{
		return ri.getConstIntValue(ctx);
	}
	
	public long getConstLongValue(Context ctx)
	{
		return ri.getConstLongValue(ctx);
	}
	
	public boolean isCompInitConstObject(Context ctx)
	{
		return ri.isCompInitConstObject(ctx);
	}
	
	public ExConstInitObj getConstInitObj(Context ctx)
	{
		return isCompInitConstObject(ctx) ? ri.getConstInitObj(ctx) : null;
	}
	
	public Expr getRightMostExpr()
	{
		return ri.getRightMostExpr();
	}
	
	public Expr getLeftOfRightMostExpr()
	{
		return le.getRightMostExpr();
	}
	
	public AccVar getDestVar()
	{
		return ri.getDestVar();
	}
	
	public void genOutputAddr(int reg, Context ctx)
	{
		ExVar asVar;
		int extReg, restore2, condHnd;
		Instruction excCheckDone;
		
		if (le.baseType == StdTypes.T_MAGC)
		{ //left side is "MAGIC", this is an error
			compErr(ctx, "ExDeref.genOutputAddr not valid for MAGIC");
		}
		else if (noGenLeftSide)
			ri.genOutputAddr(reg, ctx);
		else if (calcConstantType(ctx) != 0)
			compErr(ctx, "ExDeref.genOutputAddr not valid for constant value");
		else if (ri instanceof ExCall)
			compErr(ctx, "ExDeref.genOutputAddr not valid for call"); //super is supported only for calls => print same message
		else if (ri instanceof ExVar)
		{
			asVar = (ExVar) ri;
			if (le.baseType == StdTypes.T_DESC)
			{ //left side just a reference to a unit
				asVar.useResu = true;
				if (ctx.dynaMem)
				{
					restore2 = ctx.arch.prepareFreeReg(0, 0, reg, le.getRegType(ctx));
					extReg = ctx.arch.allocReg();
					ctx.arch.genLoadUnitContext(extReg, leImportedClassIndexed.relOff);
					asVar.genOutputAddr(reg, extReg, ctx);
					ctx.arch.deallocRestoreReg(extReg, reg, restore2);
				}
				else
					asVar.genOutputAddr(reg, ctx);
			}
			else
			{ //left side is something that returns an object
				asVar.useResu = true;
				restore2 = ctx.arch.prepareFreeReg(0, 0, reg, le.getRegType(ctx));
				extReg = ctx.arch.allocReg();
				le.genOutputVal(extReg, ctx);
				if (ctx.runtimeNull)
				{
					excCheckDone = ctx.arch.getUnlinkedInstruction();
					condHnd = ctx.arch.genCompPtrToNull(extReg, Ops.C_NE);
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
				asVar.genOutputAddr(reg, extReg, ctx);
				ctx.arch.deallocRestoreReg(extReg, reg, restore2);
			}
		}
		else
			compErr(ctx, "ExDeref.genOutputAddr with invalid type of ri");
	}
	
	public boolean canGenAddr(Unit unitContext, boolean allowSpecialWriteAccess, int resolveFlags, Context ctx)
	{
		return ri.canGenAddr(unitContext, allowSpecialWriteAccess, resolveFlags, ctx);
	}
	
	public void genOutputVal(int reg, Context ctx)
	{
		ExCall asCall;
		ExVar asVar;
		ExNew asNew;
		int leRegType, extReg, restore, restore2, type, condHnd;
		Instruction excCheckDone;
		
		if (le.baseType == StdTypes.T_MAGC)
			magic.genOutput(reg, ri, ctx); //left side is "MAGIC", special handling
		else if (noGenLeftSide || ri.calcConstantType(ctx) != 0)
			ri.genOutputVal(reg, ctx);
		else
		{
			if (ri instanceof ExCall)
			{
				asCall = (ExCall) ri;
				restore = ctx.arch.ensureFreeRegs(reg, 0, 0, 0);
				if (isSuper)
				{
					asVar = (ExVar) le;
					if (ctx.dynaMem)
					{
						restore2 = ctx.arch.prepareFreeReg(0, 0, reg, StdTypes.T_PTR);
						extReg = ctx.arch.allocReg();
						asCall.genOutputPar(ctx);
						ctx.arch.genLoadUnitContext(extReg, leImportedClassIndexed.relOff);
						ctx.arch.genCall(asCall.dest.relOff, extReg, asCall.dest.parSize);
						ctx.arch.deallocRestoreReg(extReg, reg, restore2);
					}
					else
					{
						asCall.genOutputPar(ctx);
						ctx.arch.genCallConst(asCall.dest, asCall.dest.parSize);
					}
					if (reg != 0)
					{
						if ((type = getRegType(ctx)) != 0)
							ctx.arch.genMoveFromPrimary(reg, type);
					}
				}
				else
				{
					if (le.baseType == StdTypes.T_DESC)
					{ //left side is a reference to a unit
						if (ctx.dynaMem)
						{
							ctx.arch.genSaveUnitContext();
							asCall.genOutputPar(ctx);
							ctx.arch.genLoadUnitContext(ctx.arch.regClss, leImportedClassIndexed.relOff);
						}
						else
							asCall.genOutputPar(ctx);
						//no else: call is statically bound and everything inside is statically resolved, no class context needed
						asCall.genOutputCall(reg, 0, ctx);
						if (ctx.dynaMem)
							ctx.arch.genRestUnitContext();
					}
					else
					{ //left side is something that returns an object
						restore2 = ctx.arch.prepareFreeReg(0, 0, reg, le.getRegType(ctx));
						extReg = ctx.arch.allocReg();
						ctx.arch.genSaveInstContext();
						le.genOutputVal(extReg, ctx);
						if (ctx.runtimeNull)
						{
							excCheckDone = ctx.arch.getUnlinkedInstruction();
							condHnd = ctx.arch.genCompPtrToNull(extReg, Ops.C_NE);
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
						asCall.genOutputPar(ctx);
						ctx.arch.genLoadInstContext(extReg);
						asCall.genOutputCall(reg, extReg, ctx);
						ctx.arch.genRestInstContext();
						ctx.arch.deallocRestoreReg(extReg, reg, restore2);
					}
				}
				ctx.arch.deallocRestoreReg(0, reg, restore);
			}
			else if (ri instanceof ExVar)
			{
				asVar = (ExVar) ri;
				if (le.baseType == StdTypes.T_DESC && !ctx.dynaMem)
				{
					//variable access is statically bound, no class context needed
					asVar.genOutputVal(reg, 0, ctx);
				}
				else
				{
					//variable access is dynamically bound, access via class context or result
					if (le.baseType == StdTypes.T_DESC)
						leRegType = StdTypes.T_PTR;
					else
						leRegType = le.getRegType(ctx);
					restore2 = ctx.arch.prepareFreeReg(0, 0, reg, leRegType);
					extReg = ctx.arch.allocReg();
					if (le.baseType == StdTypes.T_DESC)
					{ //left side is a reference to a unit
						ctx.arch.genLoadUnitContext(extReg, leImportedClassIndexed.relOff);
						asVar.useResu = true;
					}
					else
					{ //left side is something that returns an object
						le.genOutputVal(extReg, ctx);
						asVar.useResu = true;
						if (ctx.runtimeNull)
						{
							excCheckDone = ctx.arch.getUnlinkedInstruction();
							condHnd = ctx.arch.genCompPtrToNull(extReg, Ops.C_NE);
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
					}
					asVar.genOutputVal(reg, extReg, ctx);
					ctx.arch.deallocRestoreReg(extReg, reg, restore2);
				}
			}
			else if (ri instanceof ExNew)
			{
				asNew = (ExNew) ri;
				restore2 = ctx.arch.prepareFreeReg(reg, 0, 0, StdTypes.T_PTR);
				extReg = ctx.arch.allocReg();
				le.genOutputVal(extReg, ctx);
				asNew.genOutputVal(reg, extReg, ctx);
				ctx.arch.deallocRestoreReg(extReg, reg, restore2);
			}
			else
			{
				compErr(ctx, "ExDeref.genOutputVal with invalid type of ri");
			}
		}
	}
	
	public void genOutputAssignTo(int newValueReg, Expr newValue, Context ctx)
	{
		ExVar asVar;
		int leRegType, extReg, restore2, condHnd;
		Instruction excCheckDone;
		
		if (le.baseType == StdTypes.T_MAGC)
			super.genOutputAssignTo(newValueReg, newValue, ctx);
		else if (noGenLeftSide || ri.calcConstantType(ctx) != 0)
			ri.genOutputVal(newValueReg, ctx);
		else if (ri instanceof ExVar)
		{
			asVar = (ExVar) ri;
			if (le.baseType == StdTypes.T_DESC && !ctx.dynaMem)
			{
				//variable access is statically bound, no class context needed
				asVar.genOutputAssignTo(0, newValueReg, newValue, ctx);
			}
			else
			{
				if (le.baseType == StdTypes.T_DESC)
					leRegType = StdTypes.T_PTR;
				else
					leRegType = le.getRegType(ctx);
				//variable access is dynamically bound, access via class context or result
				restore2 = ctx.arch.prepareFreeReg(newValueReg, 0, 0, leRegType);
				extReg = ctx.arch.allocReg();
				if (le.baseType == StdTypes.T_DESC)
				{ //left side is a reference to a unit
					ctx.arch.genLoadUnitContext(extReg, leImportedClassIndexed.relOff);
					asVar.useResu = true;
				}
				else
				{ //left side is something that returns an object
					le.genOutputVal(extReg, ctx);
					asVar.useResu = true;
					if (ctx.runtimeNull)
					{
						excCheckDone = ctx.arch.getUnlinkedInstruction();
						condHnd = ctx.arch.genCompPtrToNull(extReg, Ops.C_NE);
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
				}
				asVar.genOutputAssignTo(extReg, newValueReg, newValue, ctx);
				ctx.arch.deallocRestoreReg(extReg, 0, restore2);
			}
		}
		else
			super.genOutputAssignTo(newValueReg, newValue, ctx);
	}
}
