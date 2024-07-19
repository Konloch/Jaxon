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
 * ExNew: new-expression
 *
 * @author S. Frenz
 * @version 210408 fixed check to not-allow new STRUCT array
 * version 120925 added support for code printer
 * version 120404 adopted changed ExAbsCall, again fixed reference to imported unit, added register saving for explicit constructor call
 * version 120312 added resolve variant for named new
 * version 101231 adopted changed Unit
 * version 101210 adopted changed Architecture
 * version 101019 fixed in optimization of 101018
 * version 101018 fixed write-once semantic in new
 * version 101015 adopted changed Expr
 * version 100409 adopted changed TypeRef
 * version 100114 reorganized constant object handling, version 100114 adopted changed Architecture
 * version 091125 optimized reg-copy if result is never read
 * version 091026 adopted changed minimumAccessLevel return value
 * version 091021 adopted changed modifier declarations and added relation tracking
 * version 091013 adopted changed method signature of genStore*
 * version 091005 adopted changed Expr
 * version 091001 adopted changed memory interface
 * version 090724 adopted changed Expr
 * version 090718 adopted changed Expr
 * version 090619 adopted changed Architecture
 * version 090219 adopted changed Architecture
 * version 090218 fixed genOutputVal for reg==0 and made use of centralized access level check
 * version 090207 added copyright notice and optimized assign structure to make use of changed Architecture
 * version 080622 made constNew protected to allow Clss/Intf access to it
 * version 080616 added compile-time-allocation for trivially initialized objects
 * version 080614 adopted changed Unit.searchVariable
 * version 080613 adopted hasEffect->effectType
 * version 080202 added support for alternate newInstance-signature
 * version 071223 added support for inner new
 * version 070909 optimized signature of Expr.resolve
 * version 070727 replaced exSubResolve by resolve as there is nothing done anymore
 * version 070628 adopted changes for "Expr extends TypeRef"
 * version 070527 added support for allocation of objects with inline arrays
 * version 070513 changed order of freeregs/instcontext-saving for optimizations
 * version 070505 adopted change in Architecture
 * version 070303 added support for indirect movable scalars
 * version 070208 fixed bug in array-allocation
 * version 070114 reduced access level where possible
 * version 070111 adapted change in printPos and compErr
 * version 070108 optimized static runtime call
 * version 070106 removed genOutputAddr
 * version 070101 adopted change in genCall
 * version 061229 removed access to firstInstr
 * version 061203 optimized calls to printPos and compErr
 * version 061202 adopted change of genCall
 * version 061129 static TypeRef object moved dynamically to Context
 * version 061111 bugfix if moveable code
 * version 061102 added support for structs
 * version 061030 changed detection of indirectCall
 * version 060628 added support for static compilation
 * version 060616 fixed genOutputVal for explicit constructors
 * version 060607 initial version
 */

public class ExNew extends ExAbsCall
{
	protected boolean asArray, multArray, canBeDoneAtCompileTime;
	protected TypeRef obj;
	private Unit destTypeUnit;
	private UnitList importedClass, runtimeClass;
	protected ExConstNew constNew;
	private boolean callExplicitConstr;
	
	protected ExNew(int fid, int il, int ic)
	{
		super(fid, il, ic);
	}
	
	public void printExpression(CodePrinter codePrnt)
	{
		codePrnt.exprNew(this, asArray, multArray, callExplicitConstr, destTypeUnit, dest, par);
	}
	
	public boolean resolve(Unit unitContext, Mthd mthdContext, int resolveFlags, TypeRef preferredType, Context ctx)
	{
		return resolve(mthdContext, unitContext, mthdContext, resolveFlags, preferredType, ctx);
	}
	
	public boolean resolve(Mthd enclosingMthd, Unit unitContext, Mthd mthdContext, int resolveFlags, TypeRef preferredType, Context ctx)
	{
		FilledParam pa;
		int accessLevel;
		
		//resolve type to instantiate
		if (!obj.resolveType(unitContext, ctx))
			return false;
		//resolve parameter and constructor
		//initialized: implContextSwitch=false; //always call dynamically
		//initialized: callExplicitConstr=false; //is set below if explicit constructor used
		if (asArray)
		{ //create new array
			canBeDoneAtCompileTime = mthdContext == unitContext.initStat;
			//resolve parameter
			pa = par;
			while (pa != null)
			{
				if (pa.expr == null)
					break; //no more parameters, just for type-setting
				//not empty parameter, resolve it
				if (!pa.expr.resolve(unitContext, mthdContext, resolveFlags | RF_CHECKREAD, ctx.intType, ctx))
					return false;
				if (!pa.expr.isIntType())
				{
					printPos(ctx, "expression to size an array has to be of type int");
					return false;
				}
				if (pa.expr.calcConstantType(ctx) != StdTypes.T_INT)
					canBeDoneAtCompileTime = false; //not a constant dimension
				pa = pa.nextParam;
				if (pa != null)
					multArray = true;
			}
			//check type of array
			if (obj.baseType == T_QID)
			{
				destTypeUnit = obj.qid.unitDest;
				if ((destTypeUnit.modifier & Modifier.M_STRUCT) != 0)
				{
					printPos(ctx, "can not create STRUCT-array");
					return false;
				}
			}
			if (canBeDoneAtCompileTime && constNew == null && !(constNew = new ExConstNew(fileID, line, col)).fillConstNew(unitContext, destTypeUnit, obj, par, resolveFlags, ctx))
				return false; //create constNew-container
		}
		else
		{ //no array, must be a new Object
			if (obj.baseType != T_QID)
			{
				printPos(ctx, "new can not be called for basic type with no array");
				return false;
			}
			destTypeUnit = obj.qid.unitDest;
			if ((destTypeUnit.modifier & Modifier.M_STRUCT) != 0)
			{
				printPos(ctx, "can not create new struct");
				return false;
			}
			//check access
			if ((accessLevel = unitContext.minimumAccessLevel(destTypeUnit, destTypeUnit.name, destTypeUnit, destTypeUnit.modifier, true, ctx)) == Modifier.M_ERROR)
				return false;
			destTypeUnit.modifier |= accessLevel;
			if (destTypeUnit.outerUnit != null && (destTypeUnit.modifier & Modifier.M_STAT) == 0)
			{
				//enclosing instance needed
				if ((enclosingMthd.modifier & Modifier.M_STAT) != 0)
				{
					printPos(ctx, "enclosing instance required");
					return false;
				}
			}
			//resolve constructor and possibly existing parameters
			if (!resolveInMthd(obj.qid.getLastPID(), true, destTypeUnit, destTypeUnit.initDyna, unitContext, mthdContext, resolveFlags, false, ctx))
				return false;
			if ((destTypeUnit.modifier & Modifier.M_INDIR) != 0)
			{
				printPos(ctx, "new array not supported for interfaces");
				return false;
			}
			//call constructor of class only if neccessary
			if (dest != destTypeUnit.initDyna)
				callExplicitConstr = true;
			//check if object may be allocated at compile-time
			if ((canBeDoneAtCompileTime = (mthdContext == unitContext.initStat && !callExplicitConstr)) && constNew == null && !(constNew = new ExConstNew(fileID, line, col)).fillConstNew(unitContext, destTypeUnit, obj, null, resolveFlags, ctx))
				return false; //create constNew-container
		}
		if (destTypeUnit != null)
		{
			if (!asArray)
			{
				if ((destTypeUnit.modifier & Modifier.M_ABSTR) != 0)
				{
					printPos(ctx, "new can not be called for abstract class");
					return false;
				}
				if (destTypeUnit.inlArr != null && !destTypeUnit.explConstr)
				{
					printPos(ctx, "new can not be called without explicit constructor for inline-array class ");
					ctx.out.print(destTypeUnit.name);
					return false;
				}
			}
			if (ctx.dynaMem && (destTypeUnit.modifier & Modifier.M_STRUCT) == 0)
				importedClass = unitContext.getRefUnit(destTypeUnit, true);
			if (ctx.relations != null)
				ctx.relations.addRelation(destTypeUnit, null, null, unitContext, mthdContext);
		}
		if (ctx.dynaMem)
			runtimeClass = unitContext.getRefUnit(ctx.rteDynamicRuntime, true);
		//get type of new object
		getTypeOf(obj);
		//everything resolved
		effectType = EF_NORM; //creating a new object always has an effect
		return true;
	}
	
	public boolean isCompInitConstObject(Context ctx)
	{
		return canBeDoneAtCompileTime;
	}
	
	public ExConstInitObj getConstInitObj(Context ctx)
	{
		return canBeDoneAtCompileTime ? constNew : null;
	}
	
	public void genOutputVal(int reg, Context ctx)
	{
		genOutputVal(reg, ctx.arch.regInst, ctx);
	}
	
	public void genOutputVal(int reg, int outerClassReg, Context ctx)
	{
		int specialRestore = 0;
		boolean doSpecialRestore = false;
		int restore, restore2, restoreCall = 0, level, tmpPtr;
		Vrbl outer;
		FilledParam p;
		
		if (reg == 0)
		{ //allocate register if it is not given by outer expression
			specialRestore = ctx.arch.prepareFreeReg(0, 0, 0, StdTypes.T_PTR);
			reg = ctx.arch.allocReg();
			doSpecialRestore = true;
		}
		if (asArray)
		{ //create array
			if (ctx.dynaMem)
				ctx.arch.genSaveUnitContext(); //save current class context for call
			restore = ctx.arch.ensureFreeRegs(reg, 0, 0, 0); //save registers
			genOutputSinglePar(par.expr, ctx); //generate output for the single/first parameter
			ctx.arch.genPushConstVal(obj.arrDim, StdTypes.T_INT); //array-dimension of result
			if (obj.baseType != TypeRef.T_QID)
			{ //basic type
				ctx.arch.genPushConstVal(TypeRef.getMinSize(obj.baseType), StdTypes.T_INT); //entrysize
				ctx.arch.genPushConstVal(obj.baseType, StdTypes.T_INT); //type
				ctx.arch.genPushConstVal(0, StdTypes.T_PTR); //no extType
			}
			else
			{ //object
				ctx.arch.genPushConstVal(-1, StdTypes.T_INT); //normal object-reference
				ctx.arch.genPushConstVal(0, StdTypes.T_INT); //stdType==0
				restore2 = ctx.arch.prepareFreeReg(0, 0, reg, StdTypes.T_PTR);
				tmpPtr = ctx.arch.allocReg();
				if ((obj.qid.unitDest.modifier & Modifier.M_STRUCT) != 0)
					ctx.arch.genPushConstVal(0, StdTypes.T_PTR); //push null if array of struct
				else if (ctx.dynaMem)
					ctx.arch.genLoadUnitContext(tmpPtr, importedClass.relOff); //load address of destination type
				else
					ctx.arch.genLoadConstUnitContext(tmpPtr, destTypeUnit.outputLocation); //load address of destination type
				ctx.arch.genPush(tmpPtr, StdTypes.T_PTR); //push that address
				ctx.arch.deallocRestoreReg(tmpPtr, 0, restore2);
			}
			if (ctx.dynaMem)
			{
				ctx.arch.genLoadUnitContext(ctx.arch.regClss, runtimeClass.relOff); //load class context of runtime system
				ctx.arch.genCall(ctx.rteDRNewArrayMd.relOff, ctx.arch.regClss, ctx.rteDRNewArrayMd.parSize); //call runtime system
			}
			else
				ctx.arch.genCallConst(ctx.rteDRNewArrayMd, ctx.rteDRNewArrayMd.parSize); //call runtime system
			ctx.arch.genMoveFromPrimary(reg, StdTypes.T_PTR); //load result in desired register
			ctx.arch.deallocRestoreReg(0, reg, restore); //restore saved registers
			if (ctx.dynaMem)
				ctx.arch.genRestUnitContext(); //save current class context
			if (multArray)
			{
				p = par.nextParam;
				level = 1;
				while (p != null)
				{ //generate call to rteDRNewMultArray for each parameter==level
					level++;
					if (ctx.dynaMem)
						ctx.arch.genSaveUnitContext(); //save current instance context for call
					restore = ctx.arch.ensureFreeRegs(0, 0, reg, 0);
					ctx.arch.genPush(reg, StdTypes.T_PTR); //pointer to root of array is in reg
					ctx.arch.genPushConstVal(1, StdTypes.T_INT); //push level of root
					ctx.arch.genPushConstVal(level, StdTypes.T_INT); //push current level as destination
					genOutputSinglePar(p.expr, ctx); //generate output for length of current level
					ctx.arch.genPushConstVal(obj.arrDim, StdTypes.T_INT); //array-dimension of result
					if (obj.baseType != TypeRef.T_QID)
					{ //basic type
						ctx.arch.genPushConstVal(TypeRef.getMinSize(obj.baseType), StdTypes.T_INT); //entrysize
						ctx.arch.genPushConstVal(obj.baseType, StdTypes.T_INT); //type
						ctx.arch.genPushConstVal(0, StdTypes.T_PTR); //no extType
					}
					else
					{ //object
						ctx.arch.genPushConstVal(-1, StdTypes.T_INT); //normal object-reference
						ctx.arch.genPushConstVal(0, StdTypes.T_INT); //stdType==0
						restore2 = ctx.arch.prepareFreeReg(0, 0, reg, StdTypes.T_PTR);
						tmpPtr = ctx.arch.allocReg();
						if (ctx.dynaMem)
							ctx.arch.genLoadUnitContext(tmpPtr, importedClass.relOff); //load address of destination type
						else
							ctx.arch.genLoadConstUnitContext(tmpPtr, destTypeUnit.outputLocation); //load address of destination type
						ctx.arch.genPush(tmpPtr, StdTypes.T_PTR); //push that address
						ctx.arch.deallocRestoreReg(tmpPtr, 0, restore2);
					}
					if (ctx.dynaMem)
					{
						ctx.arch.genLoadUnitContext(ctx.arch.regClss, runtimeClass.relOff); //load class context of runtime system
						ctx.arch.genCall(ctx.rteDRNewMultArrayMd.relOff, ctx.arch.regClss, ctx.rteDRNewMultArrayMd.parSize); //call runtime system
					}
					else
						ctx.arch.genCallConst(ctx.rteDRNewMultArrayMd, ctx.rteDRNewMultArrayMd.parSize); //call runtime system
					ctx.arch.genMoveFromPrimary(reg, StdTypes.T_PTR); //load result in desired register
					ctx.arch.deallocRestoreReg(0, 0, restore); //restore saved registers
					if (ctx.dynaMem)
						ctx.arch.genRestUnitContext(); //save current instance context
					p = p.nextParam;
				}
			}
		}
		else if (callExplicitConstr && (dest.modifier & Modifier.M_EXINIT) != 0)
		{ //explicit allocation, no object allocation here
			restore = ctx.arch.ensureFreeRegs(reg, 0, 0, 0); //save registers
			ctx.arch.genSaveInstContext(); //save current instance context for constructor
			genOutputPar(ctx); //generate parameters for explicit call
			genOutputCall(reg, 0, ctx); //call explicit constructor (inst-reg invalid!)
			ctx.arch.genRestInstContext(); //restore saved instance context
			ctx.arch.deallocRestoreReg(0, reg, restore); //restore saved registers
		}
		else
		{ //constructor for normal object
			if (callExplicitConstr)
			{
				restoreCall = ctx.arch.ensureFreeRegs(reg, 0, outerClassReg, 0); //save registers
				ctx.arch.genSaveInstContext(); //save current instance context for constructor
				genOutputPar(ctx); //generate parameters for explicit call
			}
			else if (ctx.dynaMem)
				ctx.arch.genSaveUnitContext(); //save current class context for new-call
			restore = ctx.arch.ensureFreeRegs(reg, 0, outerClassReg, 0); //save registers
			if (!ctx.alternateObjNew)
			{
				ctx.arch.genPushConstVal(destTypeUnit.instScalarTableSize, StdTypes.T_INT); //push scalarsize of destination object
				if (ctx.indirScalars)
					ctx.arch.genPushConstVal(destTypeUnit.instIndirScalarTableSize, StdTypes.T_INT); //push indirect scalarsize of destination object
				ctx.arch.genPushConstVal(destTypeUnit.instRelocTableEntries, StdTypes.T_INT); //push relocentries of destination object
			}
			restore2 = ctx.arch.prepareFreeReg(0, 0, reg, StdTypes.T_PTR);
			if (restore2 != 0)
			{
				compErr(ctx, "no free reg to push address in ExNew");
				return;
			}
			tmpPtr = ctx.arch.allocReg();
			if (ctx.dynaMem)
				ctx.arch.genLoadUnitContext(tmpPtr, importedClass.relOff); //load address of destination type indirectly
			else
				ctx.arch.genLoadConstUnitContext(tmpPtr, destTypeUnit.outputLocation); //load address of destination type directly
			ctx.arch.genPush(tmpPtr, StdTypes.T_PTR); //push destination type address
			ctx.arch.deallocRestoreReg(tmpPtr, 0, restore2);
			if (ctx.dynaMem)
			{
				ctx.arch.genLoadUnitContext(ctx.arch.regClss, runtimeClass.relOff); //load class context of runtime system
				ctx.arch.genCall(ctx.rteDRNewInstMd.relOff, ctx.arch.regClss, ctx.rteDRNewInstMd.parSize); //call runtime system
			}
			else
				ctx.arch.genCallConst(ctx.rteDRNewInstMd, ctx.rteDRNewInstMd.parSize); //call runtime system
			ctx.arch.genMoveFromPrimary(reg, StdTypes.T_PTR); //load result in desired register
			ctx.arch.deallocRestoreReg(0, reg, restore); //restore saved registers
			if (destTypeUnit.outerUnit != null && (destTypeUnit.modifier & Modifier.M_STAT) == 0)
			{ //remember current instance as outer instance
				if ((outer = destTypeUnit.searchVariable(Unit.OUTERVARNAME, ctx)) == null)
				{
					compErr(ctx, "could not find outer variable");
					return;
				}
				ctx.arch.genStoreVarVal(reg, null, outer.relOff, outerClassReg, StdTypes.T_PTR); //store in outer variable
			}
			if (callExplicitConstr)
			{ //generate explicit call, parameters are already on stack
				ctx.arch.genLoadInstContext(reg); //load instance and context
				genOutputCall(0, 0, ctx); //generate call, no output
				if (!doSpecialRestore)
					ctx.arch.genDup(reg, ctx.arch.regInst, StdTypes.T_PTR); //copy instance pointer to result
				ctx.arch.genRestInstContext(); //restore current instance context
				ctx.arch.deallocRestoreReg(0, reg, restoreCall); //restore saved registers
			}
			else if (ctx.dynaMem)
				ctx.arch.genRestUnitContext(); //restore current class context
		}
		if (doSpecialRestore)
			ctx.arch.deallocRestoreReg(reg, 0, specialRestore); //restore register environment if required
	}
}
