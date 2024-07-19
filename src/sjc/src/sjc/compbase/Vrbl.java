/* Copyright (C) 2005, 2006, 2007, 2008, 2009, 2010, 2012, 2015, 2016 Stefan Frenz
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
 * Vrbl: platform and language independent declaration of a variable
 *
 * @author S. Frenz
 * @version 160324 fixed flash final variable check
 * version 151018 added check for flash variables to be final
 * version 120504 fixed size of struct references
 * version 120501 added special struct reference case with non-recursive-resolving
 * version 101015 adopted changed Expr
 * version 100920 sending insertZeroHint only on explicit initialization
 * version 100823 added support for insertZeroHint
 * version 100504 added support for getInitExpr
 * version 100401 added getAnnotation
 * version 100114 reorganized constant object handling
 * version 091123 removed no longer needed symHint
 * version 091021 adopted changed modifier declarations
 * version 091013 adopted changed method signature of genStore*
 * version 091005 added support for preferredType in init-resolving
 * version 091001 adopted changed memory interface
 * version 090718 added support for non-static final variables, optimized genInitCode, adopted changed Expr
 * version 090619 adopted changed Architecture
 * version 090507 added checks for struct variables minSize to support correct offset calculation of struct-arrays
 * version 090209 fixed usage of genStoreVarConstVal if type is T_LONG or T_DBL
 * version 090208 using genStoreVarConstVal instead of genClearMem
 * version 090207 added copyright notice
 * version 080802 changed register allocation strategy in genInitCode to reduce register copy after method calls
 * version 080614 removed copy
 * version 070917 added optimization to skip zero-initialization
 * version 070913 added support for genInitCode with init==null
 * version 070909 optimized signature of Expr.resolve
 * version 070903 removed checkNameAndType
 * version 070731 adopted change of renaming id to name
 * version 070727 added symHint
 * version 070628 adopted changes for "Expr extends TypeRef"
 * version 070526 added flag for length of inline-arrays
 * version 070331 added initialization of movable indirect scalars, fixed location assignment
 * version 070127 added generic genInitCode
 * version 070114 added flags to optimize visibility level
 * version 070111 adapted change in printPos and compErr
 * version 070106 removed access to inlinedArraySize
 * version 061203 optimized calls to printPos and compErr
 * version 061101 added support for structs
 * version 060607 initial version
 */

public class Vrbl extends AccVar
{
	public final static String NOREGFREE = "registers not free in Vrbl.genInitCode";
	
	//required fields for resolving
	public Vrbl nextVrbl;
	public Expr init;
	
	public Vrbl(String ii, int im, int fid, int il, int ic)
	{
		super(fid, il, ic);
		name = ii;
		modifier = im;
	}
	
	public FilledAnno getAnnotation()
	{ //overwritten by VrblAnno
		return null;
	}
	
	public boolean resolveConstant(Unit owner, Context ctx)
	{
		if (location == L_CONST)
			return true;
		if (location != L_CONSTDC)
		{
			printPos(ctx, "cyclic initialization for variable ");
			ctx.out.print(name);
			return false; //cyclic declaration
		}
		location = L_CONSTTR;
		if (init != null)
		{
			if (!init.resolve(owner, owner.initStat, Expr.RF_CHECKREAD, type, ctx))
			{
				ctx.out.print(" in constant initialization for variable ");
				ctx.out.print(name);
				return false;
			}
			if (init.calcConstantType(ctx) == 0 && !init.isCompInitConstObject(ctx))
			{
				init.printPos(ctx, "not constant initialization for variable ");
				ctx.out.print(name);
				return false;
			}
			if (init.compareType(type, false, ctx) != TypeRef.C_EQ)
			{
				init.printPos(ctx, "constant initialization needs to have exactly the same type of variable ");
				ctx.out.print(name);
				return false;
			}
		}
		location = L_CONST;
		return true;
	}
	
	public Expr getInitExpr(Context ctx)
	{
		return init;
	}
	
	public ExConstInitObj getConstInitObj(Context ctx)
	{
		return init != null && init.isCompInitConstObject(ctx) ? init.getConstInitObj(ctx) : null;
	}
	
	public boolean enterSize(int loc, Context ctx)
	{
		//at first, do default things
		if (!super.enterSize(loc, ctx))
			return false;
		//do special checks for variables inside a struct
		if ((modifier & Modifier.M_STRUCT) != 0)
		{
			if (type.arrDim > 0)
			{
				if (type.arrDim > 1)
				{
					printPos(ctx, "struct-variables must not have array dimension >1");
					return false;
				}
				if (type.baseType != TypeRef.T_QID)
					minSize = TypeRef.getMinSize(type.baseType);
			}
			if ((modifier & (Modifier.M_STAT | Modifier.M_FIN)) != 0)
			{
				printPos(ctx, "struct-variables can not be \"final\" or \"static\"");
				return false;
			}
			if (type.baseType == TypeRef.T_QID)
			{
				if ((type.qid.unitDest.modifier & Modifier.M_STRUCT) == 0)
				{
					printPos(ctx, "struct-variable must not reference non-struct-objects");
					return false;
				}
				if (location == AccVar.L_STRUCTREF)
					minSize = ctx.arch.relocBytes;
				else
				{
					if (!type.qid.unitDest.resolveInterface(ctx))
						return false; //parent must be ok for inlining
					minSize = type.qid.unitDest.instScalarTableSize; //enter size of one element if array
				}
			}
			//check if location is to be set
			if (loc == L_UNIT)
				location = L_STRUCT;
		}
		//otherwise check if variable is declared in a unit and location has to be set
		else if (loc == L_UNIT)
		{
			if ((modifier & (Modifier.M_FIN | Modifier.M_STAT)) == (Modifier.M_FIN | Modifier.M_STAT))
				location = L_CONSTDC;
			else if (minSize < 0)
			{
				if ((modifier & Modifier.M_STAT) != 0)
					location = L_CLSSREL;
				else
					location = L_INSTREL;
			}
			else
			{
				if ((modifier & Modifier.M_STAT) != 0)
					location = L_CLSSSCL;
				else if (ctx.indirScalars)
					location = L_INSTIDS;
				else
					location = L_INSTSCL;
			}
		}
		//do special checks for variables in flash
		if ((modifier & (Modifier.MM_FLASH | Modifier.M_FIN)) == Modifier.MM_FLASH)
		{
			printPos(ctx, "flash variable must be final");
			return false;
		}
		//everything ok
		return true;
	}
	
	public void genInitCode(boolean forceInit, Context ctx)
	{
		int addr = 0, restore = 0, regType, reg;
		Expr toInit;
		int value;
		boolean asDouble = false;
		
		//check if initialization is really required
		if ((toInit = init) != null && toInit.isConstZero())
			toInit = null; //constant zero
		//get type and position of initialization
		regType = type.getRegType(ctx);
		switch (location)
		{
			case L_INSTREL:
				reg = ctx.arch.regInst;
				break;
			case L_INSTSCL:
				reg = ctx.arch.regInst;
				break;
			case L_INSTIDS: //very special case: scalar variable addressed indirectly
				restore = ctx.arch.prepareFreeReg(0, 0, 0, StdTypes.T_PTR);
				reg = addr = ctx.arch.allocReg();
				ctx.arch.genLoadVarVal(addr, ctx.arch.regInst, null, ctx.indirScalarAddrOff, StdTypes.T_PTR);
				break;
			case L_LOCAL:
				reg = ctx.arch.regBase;
				break;
			default:
				compErr(ctx, "can not initialize variable beside instance or local");
				return;
		}
		//no initialization needed for zero values, in all cases report to backend
		if (toInit == null && !forceInit)
		{
			if (init != null)
				ctx.arch.insertZeroHint(reg, relOff, regType);
			return;
		}
		//do initialization
		if (toInit != null)
			switch (toInit.calcConstantType(ctx))
			{ //value-init
				case StdTypes.T_INT: //constant 8/16/32 bit value
					ctx.arch.genStoreVarConstVal(reg, null, relOff, toInit.getConstIntValue(ctx), regType);
					break;
				case StdTypes.T_LONG: //constant 64 bit value
					ctx.arch.genStoreVarConstDoubleOrLongVal(reg, null, relOff, toInit.getConstLongValue(ctx), regType == StdTypes.T_DBL);
					break;
				default: //not a constant value
					if (ctx.arch.prepareFreeReg(0, 0, 0, regType) != 0)
					{
						compErr(ctx, NOREGFREE);
						return;
					}
					value = ctx.arch.allocReg(); //address of variable does not change, value is generatable already
					toInit.genOutputVal(value, ctx);
					ctx.arch.genStoreVarVal(reg, null, relOff, value, regType);
					ctx.arch.deallocRestoreReg(value, 0, 0);
			}
		else
		{ //clear memory
			switch (regType)
			{
				case StdTypes.T_DBL:
					asDouble = true; //no break
				case StdTypes.T_LONG:
					ctx.arch.genStoreVarConstDoubleOrLongVal(reg, null, relOff, 0l, asDouble);
					break;
				default:
					ctx.arch.genStoreVarConstVal(reg, null, relOff, 0, regType);
			}
		}
		//clean up indirect scalar if required
		if (location == L_INSTIDS)
			ctx.arch.deallocRestoreReg(addr, 0, restore);
	}
}
