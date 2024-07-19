/* Copyright (C) 2006, 2007, 2008, 2009, 2010, 2012 Stefan Frenz
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
 * ExArrayInit: expression containing constant array initialization
 *
 * @author S. Frenz
 * @version 120925 added support for code printer
 * version 101015 adopted changed Expr
 * version 100504 using isCompInitObject instead of type check
 * version 100423 fixed preferredType check
 * version 100420 added support for flash array initialization
 * version 100409 adopted changed TypeRef
 * version 100312 added support for flash objects
 * version 100114 reorganized constant object handling
 * version 091210 adopted changed ExConstInitObj, added support for forcedType, separated putElements
 * version 091116 added support for empty array initialization
 * version 091112 added insertion-check to avoid multiple insertions during recompilation
 * version 091005 moved to compbase-package
 * version 090724 adopted changed Expr
 * version 090207 added copyright notice
 * version 080616 added filling of dependsOn, removed getCompInitConstObjectVrbl
 * version 080614 adopted move of arrays from Clss to JUnit
 * version 070918 added support for multidimensional arrays
 * version 070909 optimized signature of Expr.resolve, adopted changes in ExAccVrbl
 * version 070727 replaced exSubResolve by resolve as there is nothing done anymore
 * version 070628 adopted changes for "Expr extends TypeRef"
 * version 070511 removed invalid location assignment
 * version 070114 reduced access level where possible
 * version 070111 adapted change in printPos and compErr
 * version 070106 removed genOutputAddr
 * version 061203 optimized calls to printPos and compErr
 * version 061202 optimized static modes
 * version 061130 initial version
 */

public class ExArrayInit extends ExConstInitObj
{
	public int len;
	public FilledParam par;
	public TypeRef forcedType;
	
	public ExArrayInit(int fid, int il, int ic)
	{
		super(fid, il, ic);
	}
	
	public void printExpression(CodePrinter prnt)
	{
		prnt.exprArrayInit(this, par);
	}
	
	public boolean resolve(Unit unitContext, Mthd mthdContext, int resolveFlags, TypeRef preferredType, Context ctx)
	{
		FilledParam pa;
		TypeRef element = null;
		Expr ex;
		TypeRef preferredElemType = null;
		int cmpRes;
		
		if (forcedType != null)
		{
			if (forcedType.qid != null && forcedType.qid.unitDest == null && !forcedType.resolveType(unitContext, ctx))
				return false;
			preferredType = forcedType;
		}
		//enter in array-list of unit (has to be done before param-resolve to ensure correct order in list)
		dest.owner = unitContext;
		if (mthdContext != unitContext.initStat && mthdContext != unitContext.initDyna)
			dependsOn = mthdContext;
		if ((mthdContext.modifier & Modifier.M_STAT) == 0)
		{
			ensureClassContext = true;
			if (ctx.dynaMem)
				importedClass = unitContext.getRefUnit(unitContext, true);
		}
		if (!ctx.doNotInsertConstantObjectsDuringResolve && (resolveFlags & RF_DEAD_CODE) == 0)
		{
			nextConstInit = unitContext.constObjList;
			unitContext.constObjList = this;
		}
		//check parameter and enter type
		if (preferredType != null)
		{
			preferredElemType = new TypeRef(fileID, line, col);
			preferredElemType.getTypeOf(preferredType);
			if (--preferredElemType.arrDim < 0)
				preferredElemType = null;
		}
		pa = par;
		len = 0;
		while (pa != null)
		{
			len++;
			ex = pa.expr; //get expression
			pa = pa.nextParam; //switch to next expression to enable "continue" below
			if (!ex.resolve(unitContext, mthdContext, resolveFlags | RF_CHECKREAD, preferredElemType, ctx))
				return false;
			if (!ex.isCompInitConstObject(ctx))
			{
				switch (ex.calcConstantType(ctx))
				{
					case 0:
						if (ex.baseType == StdTypes.T_NULL)
							continue; //null-type valid, do not copy type
						ex.printPos(ctx, "array initialization needs constant value, array or String");
						return false;
					case StdTypes.T_INT:
					case StdTypes.T_LONG:
						break;
					default:
						ex.compErr(ctx, "unknown or invalid constant type in ExArrayInit.exSubResolve");
						return false;
				}
			}
			if (element == null)
				element = ex;
			else if ((cmpRes = element.compareType(ex, true, ctx)) != TypeRef.C_EQ && cmpRes != TypeRef.C_TT)
			{
				if (element.isObjType())
					element = ctx.objectType;
				else
				{
					printPos(ctx, "array initialization needs identically typed values");
					return false;
				}
			}
		}
		if (len == 0 || element == null)
		{
			if (preferredType == null)
			{
				printPos(ctx, "unknown type of empty-/null-initialization of array");
				return false;
			}
			getTypeOf(preferredType);
		}
		else
		{
			getTypeOf(element);
			arrDim++;
		}
		if (preferredType != null && (preferredType.typeSpecial & TypeRef.S_FLASHREF) != 0)
			typeSpecial = TypeRef.S_FLASHREF;
		if (forcedType != null && (cmpRes = forcedType.compareType(this, true, ctx)) != TypeRef.C_EQ && cmpRes != TypeRef.C_TT)
		{
			printPos(ctx, "elements do not match required type");
			return false;
		}
		dest.type = this;
		constObject = true;
		if ((dest.type.typeSpecial & Modifier.MM_FLASH) != 0)
		{
			inFlash = true;
			ctx.needSecondGenConstObj = true;
		}
		return true;
	}
	
	public boolean generateObject(Context ctx, boolean doFlash)
	{
		int arrOff;
		Object arr, extType;
		boolean objElements;
		
		if (inFlash != doFlash)
			return true; //don't do it if not in correct phase
		if (qid != null)
		{
			objElements = true;
			if (qid.unitDest == null)
			{
				compErr(ctx, "invalid unitDest for array initialization");
				return false;
			}
			extType = qid.unitDest.outputLocation;
		}
		else
		{
			objElements = arrDim > 1;
			extType = null;
		}
		if ((outputLocation = arr = ctx.mem.allocateArray(len, arrDim, objElements ? -1 : TypeRef.getMinSize(baseType), baseType, extType)) == null)
		{
			ctx.out.println("error allocating memory while creating constant array for initialization");
			return false;
		}
		//enter values
		if (objElements)
			arrOff = -(ctx.rteSArray.instRelocTableEntries + 1) * ctx.arch.relocBytes;
		else
		{
			if (ctx.indirScalars)
			{
				arr = ctx.mem.getIndirScalarObject(arr);
				arrOff = ctx.rteSArray.instIndirScalarTableSize;
			}
			else
				arrOff = ctx.rteSArray.instScalarTableSize;
		}
		putElements(arr, arrOff, objElements, ctx);
		return true;
	}
	
	public String getDebugValue()
	{
		return "const-array";
	}
	
	public boolean putElements(Object arr, int arrOff, boolean objElements, Context ctx)
	{
		FilledParam pa = par;
		int off;
		ExConstInitObj coi;
		
		for (off = 0; off < len; off++)
		{
			if (pa == null)
			{
				ctx.out.println("internal error in parameter-count during array initialization");
				return false;
			}
			if (objElements)
			{
				if (pa.expr.baseType != StdTypes.T_NULL)
				{
					if (!pa.expr.isCompInitConstObject(ctx))
					{
						compErr(ctx, "not constant object in array initialization");
						return false;
					}
					coi = pa.expr.getConstInitObj(ctx);
					ctx.arch.putRef(arr, arrOff - off * ctx.arch.relocBytes, coi.outputLocation, coi.getOutputLocationOffset(ctx));
				}
			}
			else
				switch (baseType)
				{
					case StdTypes.T_BOOL:
					case StdTypes.T_BYTE:
						ctx.mem.putByte(arr, arrOff + off, (byte) pa.expr.getConstIntValue(ctx));
						break;
					case StdTypes.T_SHRT:
					case StdTypes.T_CHAR:
						ctx.mem.putShort(arr, arrOff + (off << 1), (short) pa.expr.getConstIntValue(ctx));
						break;
					case StdTypes.T_INT:
					case StdTypes.T_FLT:
						ctx.mem.putInt(arr, arrOff + (off << 2), pa.expr.getConstIntValue(ctx));
						break;
					case StdTypes.T_LONG:
					case StdTypes.T_DBL:
						ctx.mem.putLong(arr, arrOff + (off << 3), pa.expr.getConstLongValue(ctx));
						break;
					default:
						compErr(ctx, "invalid base type for constant array");
						return false;
				}
			pa = pa.nextParam;
		}
		return true;
	}
}
