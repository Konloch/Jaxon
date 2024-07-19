/* Copyright (C) 2008, 2009, 2010, 2015 Stefan Frenz
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
 * ExConstNew: container for "new" that can be done at compile-time, created by ExNew
 *
 * @author S. Frenz
 * @version 151108 added allocation debug hint
 * version 101015 adopted changed Expr
 * version 100328 fixed check for second run of generateObject
 * version 100312 added support for flash objects
 * version 100114 reorganized constant object handling
 * version 091209 adopted changed ExConstInitObj
 * version 091112 added insertion-check to avoid multiple insertions during recompilation
 * version 091005 moved to compbase-package
 * version 090724 adopted changed Expr
 * version 090718 adopted changed Expr
 * version 090207 added copyright notice
 * version 080706 fixed type setting of target
 * version 080616 initial version
 */

public class ExConstNew extends ExConstInitObj
{
	public Unit destTypeUnit;
	public FilledParam arrSizes;
	
	public ExConstNew(int fid, int il, int ic)
	{
		super(fid, il, ic);
	}
	
	public boolean resolve(Unit unitContext, Mthd mthdContext, int resolveFlags, TypeRef preferredType, Context ctx)
	{
		//should never be called as ExConstNew is called only by ExNew on demand
		compErr(ctx, "invalid call to ExConstNew.resolve");
		return false;
	}
	
	public boolean fillConstNew(Unit unitContext, Unit dtu, TypeRef targetType, FilledParam arrS, int resolveFlags, Context ctx)
	{
		//enter in array-list of unit (has to be done before param-resolve to ensure correct order in list)
		dest.owner = unitContext;
		dest.type = targetType;
		destTypeUnit = dtu;
		if (!ctx.doNotInsertConstantObjectsDuringResolve && (resolveFlags & RF_DEAD_CODE) == 0)
		{
			nextConstInit = unitContext.constObjList;
			unitContext.constObjList = this;
		}
		arrSizes = arrS;
		getTypeOf(targetType);
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
		Object extType;
		boolean objElements;
		
		if (inFlash != doFlash)
			return true;
		if (arrDim == 0)
		{
			if ((outputLocation = ctx.mem.allocate(destTypeUnit.instScalarTableSize, destTypeUnit.instIndirScalarTableSize, destTypeUnit.instRelocTableEntries, destTypeUnit.outputLocation)) == null)
			{
				ctx.out.println("error allocating memory while creating pre-allocated object");
				return false;
			}
			ctx.mem.allocationDebugHint(this);
		}
		else
		{
			if (destTypeUnit == null)
			{
				extType = null;
				objElements = false;
			}
			else
			{
				extType = destTypeUnit.outputLocation;
				objElements = true;
			}
			if ((outputLocation = ctx.allocateMultiArray(arrDim, arrSizes, objElements ? -1 : TypeRef.getMinSize(baseType), baseType, extType)) == null)
			{
				ctx.out.println("error allocating memory while creating pre-allocated array");
				return false;
			}
		}
		return true;
	}
	
	public String getDebugValue()
	{
		return "const-new";
	}
}
