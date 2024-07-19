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
 * ExCall: implementation of method-calls
 *
 * @author S. Frenz
 * @version 120925 added support for code printer
 * version 120404 replaced RF_IN_SUPER by special parameter
 * version 120402 added RF_IN_SUPER flag in resolving of super (special visibility checks required)
 * version 110624 adopted changed Context
 * version 101015 adopted changed Expr
 * version 100504 removed unneccessary Clss type cast, adopted changed Expr
 * version 091021 adopted changed modifier declarations
 * version 091020 adopted changes of RelationManager & bugfix
 * version 091009 added support for relation tracking
 * version 091005 adopted changed Expr
 * version 090724 adopted changed Expr
 * version 090718 adopted changed Expr
 * version 090616 changed type of id from PureID to String
 * version 090207 added copyright notice
 * version 080613 adopted hasEffect->effectType
 * version 071227 clearified resolving, added support for inner units
 * version 070909 optimized signature of Expr.resolve
 * version 070905 added support for this(.)-calls
 * version 070829 got magicType from Expr
 * version 070727 replaced exSubResolve by resolve as there is nothing done anymore
 * version 070628 adopted changes for "Expr extends TypeRef"
 * version 070114 reduced access level where possible
 * version 070111 adapted change in printPos and compErr
 * version 070106 removed genOutputAddr
 * version 061229 removed access to firstInstr
 * version 061228 setting mark in method on call
 * version 061203 optimized calls to printPos and compErr
 * version 061129 static TypeRef object moved dynamically to Context
 * version 060607 initial version
 */

public class ExCall extends ExAbsCall
{
	protected String id;
	protected int magicType; //==0 if not a magic call
	private boolean noCall; //only used for dummy-super()-calls
	
	protected ExCall(int fid, int il, int ic)
	{
		super(fid, il, ic);
	}
	
	public void printExpression(CodePrinter codePrnt)
	{
		codePrnt.exprCall(dest, par);
	}
	
	public boolean resolve(Unit unitContext, Mthd mthdContext, int resolveFlags, TypeRef preferredType, Context ctx)
	{
		return mthdResolve(unitContext, mthdContext, unitContext, mthdContext, resolveFlags, false, ctx);
	}
	
	public boolean mthdResolve(Unit inUnit, Mthd inMthd, Unit unitContext, Mthd mthdContext, int resolveFlags, boolean partOfSuper, Context ctx)
	{
		if (!resolveInMthd(id, false, inUnit, inMthd, unitContext, mthdContext, resolveFlags, partOfSuper, ctx))
			return false;
		if (ctx.relations != null && id != null)
			ctx.relations.addRelation(inUnit, dest, null, unitContext, mthdContext);
		//check if implicit context switch implContextSwitch
		implContextSwitch = ((dest.modifier & Modifier.M_STAT) != 0) && (dest.owner != unitContext || (mthdContext.modifier & Modifier.M_STAT) == 0);
		//get return-type
		getTypeOf(dest.retType);
		effectType = EF_NORM;
		mthdContext.modifier |= Modifier.M_HSCALL;
		return true;
	}
	
	public boolean isSuperThisCall(Context ctx)
	{
		return id.equals(SJava.KEY_SUPER) || id.equals(SJava.KEY_THIS);
	}
	
	public boolean resolveSuperThisCall(Unit inUnit, Mthd inMthd, Context ctx)
	{
		Unit calledConstrUnit;
		boolean callSuper;
		
		if ((!(callSuper = id.equals(SJava.KEY_SUPER)) && !id.equals(SJava.KEY_THIS)))
		{
			return super.resolveSuperThisCall(inUnit, inMthd, ctx);
		}
		if (!inMthd.isConstructor)
		{
			printPos(ctx, "call to this-/super-constructor only allowed in constructor");
			return false;
		}
		baseType = TypeRef.T_VOID;
		if (callSuper)
		{
			if (inUnit == ctx.langRoot)
			{ //no parent existing
				effectType = EF_NORM;
				noCall = true;
				return true;
			}
			calledConstrUnit = inUnit.extsID.unitDest;
		}
		else
			calledConstrUnit = inUnit;
		//resolve call
		if (!resolveInMthd(calledConstrUnit.name, true, calledConstrUnit, calledConstrUnit.initDyna, inUnit, inMthd, RF_NONE, true, ctx))
			return false;
		if (dest == calledConstrUnit.initDyna)
			noCall = true; //implicit constructor, no effect
		else
		{
			if (callSuper)
				implContextSwitch = true; //always switch to class of parent if call to super
			else if (dest == inMthd)
			{ //check recursion
				printPos(ctx, "recursive constructor call");
				return false;
			}
		}
		//check if "this" is used as parameter which is not valid
		//TODO
		//everything done
		effectType = EF_NORM;
		return true;
	}
	
	public void genOutputVal(int reg, Context ctx)
	{ //not called from ExDeref
		int restore;
		
		if (noCall)
			return; //nothing to do, just return
		//prepare call
		restore = ctx.arch.ensureFreeRegs(reg, 0, 0, 0);
		//check if call needs context switch
		if (outerLevel > 0)
			ctx.arch.genSaveInstContext();
		else if (implContextSwitch && ctx.dynaMem)
			ctx.arch.genSaveUnitContext();
		//create output for each parameter
		genOutputPar(ctx);
		//generate call (no special object existing)
		genOutputCall(reg, 0, ctx);
		//check if call has needed context switch
		if (outerLevel > 0)
			ctx.arch.genRestInstContext();
		else if (implContextSwitch && ctx.dynaMem)
			ctx.arch.genRestUnitContext();
		//restore registers
		ctx.arch.deallocRestoreReg(0, reg, restore);
	}
}
