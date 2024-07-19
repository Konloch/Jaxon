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

package sjc.compbase;

import sjc.debug.CodePrinter;

/**
 * ExStr: constant string expression
 *
 * @author S. Frenz
 * @version 120925 added support for code printer
 * version 101015 adopted changed Expr
 * version 100312 added support for flash objects
 * version 100114 reorganized constant object handling
 * version 091209 adopted changed ExConstInitObj
 * version 091112 added insertion-check to avoid multiple insertions during recompilation
 * version 091021 adopted changed modifier declarations
 * version 091005 moved to compbase-package
 * version 090724 adopted changed Expr
 * version 090718 adopted changed Expr
 * version 090207 added copyright notice
 * version 080616 added filling of dependsOn, removed getCompInitConstObjectVrbl
 * version 080614 adopted move of arrays from Clss to JUnit
 * version 070918 made child of newly added ExConstInitObj
 * version 070909 optimized signature of Expr.resolve, adopted changed ExAccVrbl
 * version 070727 replaced exSubResolve by resolve as there is nothing done anymore
 * version 070628 adopted changes for "Expr extends TypeRef"
 * version 070511 removed invalid location assignment
 * version 070114 reduced access level where possible
 * version 070111 adapted change in printPos and compErr
 * version 070106 removed genOutputAddr
 * version 061203 optimized calls to printPos and compErr
 * version 061202 optimized static modes
 * version 061129 static TypeRef object moved dynamically to Context
 * version 060607 initial version
 */

public class ExStr extends ExConstInitObj
{
	public String value;
	
	public ExStr(String is, int fid, int il, int ic)
	{
		super(fid, il, ic);
		value = is;
	}
	
	public void printExpression(CodePrinter prnt)
	{
		prnt.exprString(value);
	}
	
	public boolean resolve(Unit unitContext, Mthd mthdContext, int resolveFlags, TypeRef preferredType, Context ctx)
	{
		dest.owner = unitContext;
		dest.minSize = -1;
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
		//set type and dest of parent
		getTypeOf(ctx.stringType);
		dest.type = this;
		constObject = true;
		if ((ctx.langString.modifier & Modifier.MM_FLASH) != 0)
		{
			typeSpecial = Modifier.MM_FLASH;
			inFlash = true;
			ctx.needSecondGenConstObj = true;
		}
		return true;
	}
	
	public boolean generateObject(Context ctx, boolean doFlash)
	{
		int memoryHint;
		
		if (inFlash != doFlash)
			return true;
		memoryHint = ctx.mem.getCurrentAllocAmountHint();
		ctx.stringCount++;
		ctx.stringChars += value.length();
		if ((outputLocation = ctx.allocateString(value)) == null)
			return false;
		ctx.stringMemBytes += ctx.mem.getCurrentAllocAmountHint() - memoryHint;
		return true;
	}
	
	public String getDebugValue()
	{
		return value;
	}
}
