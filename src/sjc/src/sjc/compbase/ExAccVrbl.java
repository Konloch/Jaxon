/* Copyright (C) 2006, 2007, 2008, 2009, 2010, 2011, 2012, 2014, 2018 Stefan Frenz
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
 * ExAccVrbl: expression referring to a memory-variable
 *
 * @author S. Frenz
 * @version 181016 optimized and fixed genOutputAddr with outer variable
 * version 140507 added support for RefToFlash
 * version 140124 added more support for local / param variables pointing to flash memory
 * version 120504 completed support for struct references
 * version 120502 added support to assign struct references
 * version 110624 made outerLevel and outerAccessStart public to enable access in ExDeRef, added support for outerLevel to generate this
 * version 101015 adopted changed Expr
 * version 100813 added dest-null-check
 * version 100504 adopted changed Expr, reduced compErr-messages
 * version 100312 adopted changed TypeRef and added support for flash values
 * version 100114 adopted changed Architecture
 * version 100114 reorganized constant object handling
 * version 091013 adopted changed method signature of genStore*
 * version 091001 adopted changed memory interface
 * version 090916 fixed indirect scalar addressing
 * version 090724 adopted changed Expr and added support for insideLoop-check for final variables
 * version 090718 added support for non-static final variables
 * version 090625 fixed indirect scalar addressing
 * version 090619 adopted changed Architecture
 * version 090219 adopted changed Architecture
 * version 090207 added copyright notice and inserted check for reg==0 in genOutputAssignTo
 * version 090206 added isAddrOnStack and genOutputAssignTo
 * version 080614 simplified int accMode to boolean useResu, adopted changed Unit.searchVariable
 * version 080523 better error message for invalid genOutputAddr
 * version 080331 added support for L_STRUCTREF
 * version 080119 adopted changed signature of Expr.canGenAddr
 * version 072224 added support for inner classes
 * version 070909 removed not needed isSuper and destUnit
 * version 070823 added support for constant double
 * version 070628 adopted changes for "Expr extends TypeRef"
 * version 070527 optimized inline array access, added support for instance inline arrays
 * version 070511 added support for constant objects
 * version 070505 adopted change in Architecture
 * version 070331 fixed comment
 * version 070303 added support for indirect movable scalars
 * version 070114 reduced access level where possible
 * version 070111 adapted change in printPos and compErr
 * version 070107 fixed inline array detection
 * version 070106 removed array stuff
 * version 070101 adopted change in genCall
 * version 061229 removed access to firstInstr
 * version 061228 added bound check skipping
 * version 061203 optimized calls to printPos and compErr
 * version 061202 adopted change of genCall
 * version 061130 initial version
 */

public abstract class ExAccVrbl extends Expr
{
	public AccVar dest;
	public boolean isThis, useResu; //isThis=useResu==false initialized
	public boolean constObject; //constType==false initialized
	public boolean ensureClassContext; //ensure correct class context for static variables
	public UnitList importedClass; //used for super
	public int constType; //constType==0 initialized
	public int outerLevel; //used for access of outer variables, initialized to 0
	public Unit outerAccessStart; //inner unit where to start access to outer variable
	
	private final static String ERR_INVMODE = "ExAccVrbl.genOutput* with invalid accMode";
	
	private int[] outerObj, outerRestore;
	
	public ExAccVrbl(int fid, int il, int ic)
	{
		super(fid, il, ic);
	}
	
	public int calcConstantType(Context ctx)
	{
		return constType;
	}
	
	public boolean isCompInitConstObject(Context ctx)
	{
		return constObject;
	}
	
	public AccVar getDestVar()
	{
		return dest;
	}
	
	public boolean isAddrOnStack()
	{
		return dest != null && (dest.location == Vrbl.L_LOCAL || dest.location == Vrbl.L_PARAM);
	}
	
	public boolean canGenAddr(Unit unitContext, boolean allowSpecialWriteAccess, int resolveFlags, Context ctx)
	{
		if (isThis || constType > 0 || constObject || dest == null || (dest.location == AccVar.L_STRUCT && dest.type.typeSpecial != TypeRef.S_NOSPECIAL) || (!allowSpecialWriteAccess && (dest.modifier & Modifier.M_FIN) != 0 && ((resolveFlags & RF_INSIDE_LOOP) != 0 || (dest.modifier & (Modifier.MF_ISWRITTEN | Modifier.MF_MAYBEWRITTEN)) != 0)))
			return false;
		dest.modifier |= Modifier.MF_ISWRITTEN;
		return true;
	}
	
	public void genOutputAddr(int reg, Context ctx)
	{
		genOutputAddr(reg, 0, ctx);
	}
	
	public void genOutputAddr(int reg, int obj, Context ctx)
	{
		int pos, oldObj1 = 0, addrRestore1 = 0, oldObj2 = 0, addrRestore2 = 0;
		boolean addr2Used = false;
		Object objBase = null;
		
		if (isThis || constType > 0 || constObject || (dest.location != AccVar.L_PARAM && dest.location != AccVar.L_LOCAL && dest.type.typeSpecial != TypeRef.S_NOSPECIAL && (dest.modifier & Modifier.MM_REFTOFLASH) == 0))
		{
			compErr(ctx, "can not generate output address in ExAccVrbl.genOutputAddr");
			ctx.out.println();
			return;
		}
		switch (dest.location)
		{
			case AccVar.L_STRUCT:
			case AccVar.L_STRUCTREF: //struct variable is a very special case
				//reg must contain the value of the struct (i.e. base address)
				//so the offset has to be calculated and returned
				ctx.arch.genLoadVarAddr(reg, obj, null, dest.relOff);
				return;
			case AccVar.L_CONST:
			case AccVar.L_CONSTDC:
			case AccVar.L_CONSTTR:
				printPos(ctx, "address of constant variable not available");
				ctx.out.println();
				ctx.err = true;
				return;
		}
		pos = dest.relOff;
		if (ensureClassContext)
		{ //access to variable possibly needs change of class context
			switch (dest.location)
			{
				case AccVar.L_CLSSSCL:
				case AccVar.L_CLSSREL:
					if (ctx.dynaMem)
						obj = ctx.arch.regClss;
					break;
				default:
					compErr(ctx, "ExAccVrbl.genOutputAddr with invalid var-type in ensureClassContext");
					return;
			}
			if (ctx.dynaMem)
			{
				addrRestore1 = ctx.arch.prepareFreeReg(reg, 0, oldObj1 = obj, StdTypes.T_PTR);
				obj = ctx.arch.allocReg();
				ctx.arch.genLoadUnitContext(obj, importedClass.relOff);
			}
			else
			{
				obj = 0;
				objBase = !ctx.embedded ? dest.owner.outputLocation : ctx.ramLoc;
			}
		}
		else
		{
			switch (dest.location)
			{ //normal access
				case AccVar.L_LOCAL:
				case AccVar.L_PARAM:
					if (useResu)
					{
						compErr(ctx, ERR_INVMODE);
						return;
					}
					obj = ctx.arch.regBase;
					break;
				case AccVar.L_CLSSSCL:
				case AccVar.L_CLSSREL:
					if (ctx.dynaMem)
					{
						if (!useResu)
							obj = ctx.arch.regClss;
					}
					else
					{
						objBase = !ctx.embedded ? dest.owner.outputLocation : ctx.ramLoc;
						obj = 0;
					}
					break;
				case AccVar.L_INSTSCL:
				case AccVar.L_INSTREL:
					if (!useResu)
						obj = ctx.arch.regInst;
					break;
				case AccVar.L_INSTIDS:
					if (!useResu)
						obj = ctx.arch.regInst;
					if (outerLevel > 0)
						obj = genDeOuter(reg, obj, ctx);
					addrRestore2 = ctx.arch.prepareFreeReg(reg, oldObj2 = obj, 0, StdTypes.T_PTR);
					obj = ctx.arch.allocReg();
					ctx.arch.genLoadVarVal(obj, oldObj2, null, ctx.indirScalarAddrOff, StdTypes.T_PTR);
					ctx.arch.genLoadVarAddr(reg, obj, null, pos);
					if (outerLevel > 0)
						cleanUpDeOuter(reg, ctx);
					ctx.arch.deallocRestoreReg(obj, oldObj2, addrRestore2);
					if (ensureClassContext && ctx.dynaMem)
						ctx.arch.deallocRestoreReg(obj, oldObj1, addrRestore1);
					return;
				default:
					compErr(ctx, "ExAccVrbl.genOutputAddr with invalid var-type");
					return;
			}
		}
		if (outerLevel > 0)
			obj = genDeOuter(reg, obj, ctx);
		ctx.arch.genLoadVarAddr(reg, obj, objBase, pos); //load the address
		if (outerLevel > 0)
			cleanUpDeOuter(reg, ctx);
		if (addr2Used)
			ctx.arch.deallocRestoreReg(obj, oldObj2, addrRestore2);
		if (ensureClassContext && ctx.dynaMem)
			ctx.arch.deallocRestoreReg(obj, oldObj1, addrRestore1);
	}
	
	public void genOutputVal(int reg, Context ctx)
	{
		genOutputVal(reg, 0, ctx);
	}
	
	public void genOutputVal(int reg, int obj, Context ctx)
	{
		int regType, pos, restore, oldObj;
		ExConstInitObj coi;
		
		if (isThis)
		{
			if (outerLevel > 0)
			{
				obj = genDeOuter(reg, ctx.arch.regInst, ctx);
				ctx.arch.genDup(reg, obj, StdTypes.T_PTR);
				cleanUpDeOuter(reg, ctx);
			}
			else
				ctx.arch.genDup(reg, ctx.arch.regInst, StdTypes.T_PTR);
			return;
		}
		if (constType > 0)
		{
			switch (constType)
			{
				case StdTypes.T_INT:
					ctx.arch.genLoadConstVal(reg, getConstIntValue(ctx), getRegType(ctx));
					return;
				case StdTypes.T_LONG:
					ctx.arch.genLoadConstDoubleOrLongVal(reg, getConstLongValue(ctx), baseType == T_DBL);
					return;
				default:
					compErr(ctx, "unkown constant type in ExAccVrbl.genOutputVal");
					return;
			}
		}
		if (constObject && (!ctx.dynaMem || isStructType()))
		{ //in dynaMem-mode outputLocation may only be used for constant structs
			coi = dest.getConstInitObj(ctx);
			if (coi == null)
			{
				compErr(ctx, "invalid coi");
				return;
			}
			ctx.arch.genLoadVarAddr(reg, 0, coi.outputLocation, coi.getOutputLocationOffset(ctx));
			return;
		}
		if (baseType < 0 && dest == null)
		{
			compErr(ctx, "ExAccVrbl.genOutputVal called for type<0 && dest==null");
			return;
		}
		if (dest.location == AccVar.L_STRUCT || dest.location == AccVar.L_STRUCTREF)
		{ //this is a very special case
			//reg must contain the value of the struct (i.e. base address)
			//so the offset has to be calculated and returned
			if (dest.location != AccVar.L_STRUCTREF && (dest.type.arrDim != 0 || dest.type.baseType < 0))
				ctx.arch.genLoadVarAddr(reg, obj, null, dest.relOff);
			else
				ctx.arch.genLoadVarVal(reg, obj, null, dest.relOff, getRegType(ctx));
			return;
		}
		regType = getRegType(ctx);
		if ((pos = dest.relOff) == AccVar.INV_RELOFF)
		{
			dest.compErr(ctx, "variable has invalid offset");
			return;
		}
		if (ensureClassContext)
		{ //access to variable possibly needs change of class context
			switch (dest.location)
			{
				case AccVar.L_CLSSSCL:
				case AccVar.L_CLSSREL:
					break;
				default:
					compErr(ctx, "ExAccVrbl.genOutputVal with invalid var-type in ensureClassContext");
					return;
			}
			if (ctx.dynaMem)
			{
				restore = ctx.arch.prepareFreeReg(reg, 0, oldObj = obj, StdTypes.T_PTR);
				obj = ctx.arch.allocReg();
				ctx.arch.genLoadUnitContext(obj, importedClass.relOff);
				if ((typeSpecial == S_FLASHREF || typeSpecial == S_FLASHINLARR) && (dest.modifier & Modifier.MM_REFTOFLASH) == 0)
					ctx.arch.nextValInFlash();
				ctx.arch.genLoadVarVal(reg, obj, null, pos, regType);
				ctx.arch.deallocRestoreReg(obj, oldObj, restore);
			}
			else
			{
				if ((typeSpecial == S_FLASHREF || typeSpecial == S_FLASHINLARR) && (dest.modifier & Modifier.MM_REFTOFLASH) == 0)
					ctx.arch.nextValInFlash();
				ctx.arch.genLoadVarVal(reg, 0, !ctx.embedded ? dest.owner.outputLocation : ctx.ramLoc, pos, regType);
			}
		}
		else
		{
			switch (dest.location)
			{
				case AccVar.L_LOCAL:
				case AccVar.L_PARAM:
					if (useResu)
					{
						compErr(ctx, ERR_INVMODE);
						return;
					}
					ctx.arch.genLoadVarVal(reg, ctx.arch.regBase, null, dest.relOff, regType);
					break;
				case AccVar.L_CLSSSCL:
				case AccVar.L_CLSSREL:
					if ((typeSpecial == S_FLASHREF || typeSpecial == S_FLASHINLARR) && (dest.modifier & Modifier.MM_REFTOFLASH) == 0)
						ctx.arch.nextValInFlash();
					if (ctx.dynaMem)
						ctx.arch.genLoadVarVal(reg, useResu ? obj : ctx.arch.regClss, null, dest.relOff, regType);
					else
						ctx.arch.genLoadVarVal(reg, 0, !ctx.embedded ? dest.owner.outputLocation : ctx.ramLoc, dest.relOff, regType);
					break;
				case AccVar.L_INSTSCL:
				case AccVar.L_INSTREL:
					if (!useResu)
						obj = ctx.arch.regInst;
					if (outerLevel > 0)
						obj = genDeOuter(reg, obj, ctx);
					if ((typeSpecial == S_FLASHREF || typeSpecial == S_FLASHINLARR) && (dest.modifier & Modifier.MM_REFTOFLASH) == 0)
						ctx.arch.nextValInFlash();
					ctx.arch.genLoadVarVal(reg, obj, null, dest.relOff, regType);
					if (outerLevel > 0)
						cleanUpDeOuter(reg, ctx);
					break;
				case AccVar.L_INSTIDS:
					if (!useResu)
						obj = ctx.arch.regInst;
					if (outerLevel > 0)
						obj = genDeOuter(reg, obj, ctx);
					restore = ctx.arch.prepareFreeReg(oldObj = obj, reg, 0, StdTypes.T_PTR);
					obj = ctx.arch.allocReg();
					ctx.arch.genLoadVarVal(obj, oldObj, null, ctx.indirScalarAddrOff, StdTypes.T_PTR);
					ctx.arch.genLoadVarVal(reg, obj, null, dest.relOff, regType);
					ctx.arch.deallocRestoreReg(obj, oldObj, restore);
					if (outerLevel > 0)
						cleanUpDeOuter(reg, ctx);
					break;
				case AccVar.L_INLARR:
					if (!useResu)
						obj = ctx.arch.regInst;
					if (outerLevel > 0)
					{
						obj = genDeOuter(reg, obj, ctx);
						ctx.arch.genDup(reg, obj, StdTypes.T_PTR);
						cleanUpDeOuter(reg, ctx);
					}
					else if (useResu)
						ctx.arch.genDup(reg, obj, StdTypes.T_PTR);
					else
						ctx.arch.genDup(reg, ctx.arch.regInst, StdTypes.T_PTR);
					break;
				default:
					compErr(ctx, "ExAccVrbl.genOutputVal with invalid var-type");
					return;
			}
		}
		//everything ok
	}
	
	public void genOutputAssignTo(int newValueReg, Expr newValue, Context ctx)
	{
		genOutputAssignTo(0, newValueReg, newValue, ctx);
	}
	
	public void genOutputAssignTo(int obj, int newValueReg, Expr newValue, Context ctx)
	{
		int pos, oldObj1 = 0, addrRestore1 = 0, oldObj2 = 0, addrRestore2 = 0;
		int regType, newValueCalcConstType = 0, newValueRestore = 0;
		boolean addr2Used = false, deallocNewValueReg = false;
		Object loc = null;
		
		if (isThis || constType > 0 || constObject || (dest.location != AccVar.L_PARAM && dest.location != AccVar.L_LOCAL && dest.type.typeSpecial != TypeRef.S_NOSPECIAL && (dest.modifier & Modifier.MM_REFTOFLASH) == 0))
		{
			compErr(ctx, "can not generate output address in ExAccVrbl.genOutputAssignTo");
			ctx.out.println();
			return;
		}
		regType = getRegType(ctx);
		switch (dest.location)
		{
			case AccVar.L_STRUCT:
			case AccVar.L_STRUCTREF: //struct variable is a very special case
				//reg must contain the value of the struct (i.e. base address)
				//so the offset has to be calculated and returned
				if (newValueReg == 0 && (newValueCalcConstType = newValue.calcConstantType(ctx)) != 0)
				{ //result is never read and constant, so assign directly
					switch (newValueCalcConstType)
					{
						case StdTypes.T_INT:
							ctx.arch.genStoreVarConstVal(obj, null, dest.relOff, newValue.getConstIntValue(ctx), regType);
							break;
						case StdTypes.T_LONG:
							ctx.arch.genStoreVarConstDoubleOrLongVal(obj, null, dest.relOff, newValue.getConstLongValue(ctx), regType == StdTypes.T_DBL);
							break;
						default:
							compErr(ctx, "invalid constant type in ExAccVrbl.genOutputAssignTo");
					}
				}
				else
				{ //generate result normally and assign
					if (newValueReg == 0)
					{
						newValueRestore = ctx.arch.prepareFreeReg(obj, 0, 0, regType);
						newValueReg = ctx.arch.allocReg();
						deallocNewValueReg = true;
					}
					newValue.genOutputVal(newValueReg, ctx);
					ctx.arch.genStoreVarVal(obj, null, dest.relOff, newValueReg, regType);
					if (deallocNewValueReg)
						ctx.arch.deallocRestoreReg(newValueReg, 0, newValueRestore);
				}
				return;
			case AccVar.L_CONST:
			case AccVar.L_CONSTDC:
			case AccVar.L_CONSTTR:
				printPos(ctx, "address of constant variable not available");
				ctx.out.println();
				ctx.err = true;
				return;
		}
		pos = dest.relOff;
		if (ensureClassContext)
		{ //access to variable possibly needs change of class context
			switch (dest.location)
			{
				case AccVar.L_CLSSSCL:
				case AccVar.L_CLSSREL:
					break;
				default:
					compErr(ctx, "ExAccVrbl.genOutputAssignTo with invalid var-type in ensureClassContext");
					return;
			}
			if (ctx.dynaMem)
			{
				addrRestore1 = ctx.arch.prepareFreeReg(newValueReg, 0, oldObj1 = obj, StdTypes.T_PTR);
				obj = ctx.arch.allocReg();
				ctx.arch.genLoadUnitContext(obj, importedClass.relOff);
			}
			else
			{
				obj = 0;
				loc = !ctx.embedded ? dest.owner.outputLocation : ctx.ramLoc;
			}
		}
		else
		{
			switch (dest.location)
			{ //normal access
				case AccVar.L_LOCAL:
				case AccVar.L_PARAM:
					if (useResu)
					{
						compErr(ctx, ERR_INVMODE);
						return;
					}
					obj = ctx.arch.regBase;
					break;
				case AccVar.L_CLSSSCL:
				case AccVar.L_CLSSREL:
					if (ctx.dynaMem)
					{
						if (!useResu)
							obj = ctx.arch.regClss;
					}
					else
					{
						loc = !ctx.embedded ? dest.owner.outputLocation : ctx.ramLoc;
					}
					break;
				case AccVar.L_INSTSCL:
				case AccVar.L_INSTREL:
				case AccVar.L_INSTIDS:
					if (!useResu)
						obj = ctx.arch.regInst;
					break;
				default:
					compErr(ctx, "ExAccVrbl.genOutputAssignTo with invalid var-type: ");
					return;
			}
		}
		if (outerLevel > 0)
			obj = genDeOuter(0, obj, ctx);
		if (dest.location == AccVar.L_INSTIDS)
		{
			addr2Used = true;
			oldObj2 = obj;
			addrRestore2 = ctx.arch.prepareFreeReg(newValueReg, obj, 0, StdTypes.T_PTR);
			obj = ctx.arch.allocReg();
			ctx.arch.genLoadVarVal(obj, oldObj2, null, ctx.indirScalarAddrOff, StdTypes.T_PTR);
		}
		if (newValueReg == 0 && (newValueCalcConstType = newValue.calcConstantType(ctx)) != 0)
		{ //result is never read and constant, so assign directly
			switch (newValueCalcConstType)
			{
				case StdTypes.T_INT:
					ctx.arch.genStoreVarConstVal(obj, loc, pos, newValue.getConstIntValue(ctx), regType);
					break;
				case StdTypes.T_LONG:
					ctx.arch.genStoreVarConstDoubleOrLongVal(obj, loc, pos, newValue.getConstLongValue(ctx), regType == StdTypes.T_DBL);
					break;
				default:
					compErr(ctx, "invalid constant type in ExAccVrbl.genOutputAssignTo");
			}
		}
		else
		{ //generate result normally and assign
			if (newValueReg == 0)
			{
				newValueRestore = ctx.arch.prepareFreeReg(obj, 0, 0, regType);
				newValueReg = ctx.arch.allocReg();
				deallocNewValueReg = true;
			}
			newValue.genOutputVal(newValueReg, ctx);
			ctx.arch.genStoreVarVal(obj, loc, pos, newValueReg, regType); //store the value
			if (deallocNewValueReg)
				ctx.arch.deallocRestoreReg(newValueReg, 0, newValueRestore);
		}
		if (outerLevel > 0)
			cleanUpDeOuter(0, ctx);
		if (addr2Used)
			ctx.arch.deallocRestoreReg(obj, oldObj2, addrRestore2);
		if (ensureClassContext && ctx.dynaMem)
			ctx.arch.deallocRestoreReg(obj, oldObj1, addrRestore1);
	}
	
	private int genDeOuter(int reuseReg, int obj, Context ctx)
	{
		int i;
		Vrbl outer;
		Unit currentUnit;
		
		if (outerObj == null)
		{
			outerObj = new int[outerLevel];
			outerRestore = new int[outerLevel];
		}
		currentUnit = outerAccessStart;
		for (i = 0; i < outerLevel; i++)
		{
			outerRestore[i] = ctx.arch.prepareFreeReg(obj, 0, reuseReg, StdTypes.T_PTR);
			outerObj[i] = ctx.arch.allocReg();
			if (currentUnit == null || (outer = currentUnit.searchVariable(Unit.OUTERVARNAME, ctx)) == null)
			{
				compErr(ctx, "could not find outer variable");
				return 0;
			}
			ctx.arch.genLoadVarVal(outerObj[i], obj, null, outer.relOff, StdTypes.T_PTR);
			obj = outerObj[i];
			currentUnit = currentUnit.outerUnit;
		}
		return obj;
	}
	
	private void cleanUpDeOuter(int reg, Context ctx)
	{
		int i;
		for (i = outerLevel - 1; i >= 0; i--)
			ctx.arch.deallocRestoreReg(outerObj[i], reg, outerRestore[i]);
	}
}
