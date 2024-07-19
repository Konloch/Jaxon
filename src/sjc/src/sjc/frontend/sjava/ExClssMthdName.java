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
 * ExClssMthdName: placeholder for reference to a class/interface or method
 *
 * @author S. Frenz
 * @version 120925 added support for code printer
 * version 120228 cleaned up "import sjc." typo
 * version 101219 optimized type setting in MAGIC cases
 * version 101218 adopted changed Pack/Unit
 * version 101015 adopted changed Expr
 * version 100510 renamed unitType to destType
 * version 100504 getting type of target unit if possible to ease usage in instanceof
 * version 100409 adopted changed TypeRef
 * version 091021 adopted changed modifier declarations and added relation tracking
 * version 091005 adopted changed Expr
 * version 090724 adopted changed Expr
 * version 090718 adopted changed Expr
 * version 090207 added copyright notice
 * version 081203 fixed reference to array of base types
 * version 080707 adopted changed signature of Unit.searchUnitInView
 * version 080616 removed unneccessary overwriting of calcConstantType
 * version 071222 adopted changes in Unit
 * version 070909 optimized signature of Expr.resolve
 * version 070731 adopted change of QualID[] to QualIDList
 * version 070727 adopted change of Mthd.id from PureID to String
 * version 070727 replaced exSubResolve by resolve as there is nothing done anymore
 * version 070628 adopted changes for "Expr extends TypeRef"
 * version 070509 added support to remove not accessed method fields from descriptor
 * version 070114 reduced access level where possible
 * version 070111 adapted change in printPos and compErr
 * version 070106 removed genOutputAddr
 * version 061211 mark method as called to ensure code generation
 * version 061203 optimized calls to printPos and compErr
 * version 061129 static TypeRef object moved dynamically to Context
 * version 060628 added support for static compilation
 * version 060607 initial version
 */

public class ExClssMthdName extends Expr
{
	protected TypeRef destType; //used in instanceof
	protected String unitName, mthdName; //used in MAGIC-expressions
	protected Unit destUnit;
	protected Mthd destMthd;
	protected UnitList importedClass;
	private final boolean doImport; //shall we import the given unit? only false for MAGICs
	
	protected ExClssMthdName(int fid, int il, int ic)
	{
		super(fid, il, ic);
		doImport = true; //called from parser, unitType will be set there
	}
	
	protected ExClssMthdName(String iun, String imn, boolean idi, int fid, int il, int ic)
	{
		super(fid, il, ic);
		//called from MAGIC, set values
		unitName = iun;
		mthdName = imn;
		doImport = idi;
	}
	
	public void printExpression(CodePrinter codePrnt)
	{
		if (destType != null)
			codePrnt.exprClssName(destType);
		else
			codePrnt.magcClssMthdName(destUnit, destMthd);
	}
	
	public boolean resolve(Unit unitContext, Mthd mthdContext, int resolveFlags, TypeRef preferredType, Context ctx)
	{
		Mthd mthd;
		
		if (destType != null)
		{ //used by JParser
			if (!destType.resolveType(unitContext, ctx))
				return false;
			if (destType.qid != null)
				destUnit = destType.qid.unitDest;
			getTypeOf(destType);
		}
		else if (unitName != null)
		{ //used by MAGIC
			if (!resolveUnitName(unitContext, ctx))
				return false;
			//destUnit already filled
			baseType = T_VOID;
			if (mthdName != null)
			{ //caller is interested in method, not unit
				if (!resolveMthdName(ctx))
					return false;
				mthd = destMthd;
				while (mthd != null)
				{
					mthd.modifier |= Modifier.MA_ACCSSD | Modifier.M_NDDESC;
					mthd = mthd.ovldMthd;
				}
			}
		}
		else
		{
			printPos(ctx, "ExClassName.exSubResolve with obj==name==null");
			return false;
		}
		if (destUnit != null && doImport && ctx.dynaMem)
			importedClass = unitContext.getRefUnit(destUnit, true);
		if (ctx.relations != null)
			ctx.relations.addRelation(destUnit, null, null, unitContext, mthdContext);
		//everything resolved
		return true;
	}
	
	public void genOutputVal(int reg, Context ctx)
	{
		compErr(ctx, "class can not be output of expression");
	}
	
	private boolean resolveUnitName(Unit inUnit, Context ctx)
	{
		//no protection-checks as this is used for MAGIC where no protection exists
		if ((destUnit = inUnit.searchUnitInView(new StringList(unitName), false)) == null)
		{
			printPos(ctx, "could not resolve unit ");
			ctx.out.print(unitName);
			return false;
		}
		return true;
	}
	
	private boolean resolveMthdName(Context ctx)
	{
		Mthd chk = destUnit.mthds;
		
		while (chk != null)
		{
			if (chk.name.equals(mthdName))
			{
				if (destMthd != null)
				{ //second match found -> error
					printPos(ctx, "multiple/ambiguous matches for method ");
					ctx.out.print(mthdName);
					ctx.out.print(" in unit ");
					ctx.out.print(unitName);
					return false;
				}
				destMthd = chk;
			}
			chk = chk.nextMthd;
		}
		if (destMthd != null)
			return true; //only one match found
		printPos(ctx, "could not resolve method ");
		ctx.out.print(mthdName);
		ctx.out.print(" in unit ");
		ctx.out.print(unitName);
		return false;
	}
}
