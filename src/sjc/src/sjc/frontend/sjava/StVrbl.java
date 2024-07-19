/* Copyright (C) 2007, 2008, 2009, 2010, 2012 Stefan Frenz
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
 * StVrbl: statement containing variable declaration
 *
 * @author S. Frenz
 * @version 120923 added support for code printer
 * version 101015 adopted changed Expr
 * version 100826 added code for in-system compilation
 * version 100512 adopted changed JMthd
 * version 100411 made varList protected to allow access from StForEnh
 * version 100409 adopted changed TypeRef
 * version 091112 added support for implicit conversion and explicitTypeConversion
 * version 091109 adopted changed written-checks
 * version 091102 adopted changed Stmt
 * version 091021 adopted changed modifier declarations
 * version 091005 adopted changed Expr with support for preferredType in resolving
 * version 091001 adopted changed memory interface
 * version 090724 adopted changed Expr
 * version 090718 added support for MF_WRITTEN flag, adopted changed Expr
 * version 090508 adopted changes in Stmt
 * version 090207 added copyright notice
 * version 080610 removed conversion to JMthd as it is no longer required
 * version 080106 added support for direct non-interface descendants in variable initialization
 * version 070920 fixed declaration with cross-reference in varList
 * version 070917 added support for optimized initialization (skip zero-init)
 * version 070913 initial version
 */

public class StVrbl extends Stmt
{
	protected Vrbl varList;
	private final boolean forceInit; //needed for re-initialization of loop-variables
	private int varCount; //last variable's nextPointer will be modified, remember how many vars belong here
	
	protected StVrbl(Vrbl iv, boolean ifi)
	{
		super(iv.fileID, iv.line, iv.col);
		varList = iv;
		forceInit = ifi;
	}
	
	public void printToken(CodePrinter prnt)
	{
		prnt.stmtVrbl(varList, varCount);
	}
	
	protected int innerResolve(int flowCode, Unit unitContext, Mthd mthdContext, Context ctx)
	{
		Vrbl var = varList, origMthdVrbl, last = null, origLastNextVrbl = null;
		int offset;
		
		if (varCount > 0)
		{ //if already resolved: reset resolving
			for (int i = 1; i < varCount; i++)
				var = var.nextVrbl;
			var.nextVrbl = null;
			var = varList;
			varCount = 0;
		}
		
		origMthdVrbl = mthdContext.vars;
		offset = -mthdContext.varSize;
		while (var != null)
		{
			if (last != null)
			{ //already resolved a variable
				origLastNextVrbl = last.nextVrbl; //remember original nextVrbl of last resolved variable
				last.nextVrbl = origMthdVrbl; //enter method vars in list of last resolved variable
				mthdContext.vars = last; //set up method vars
			}
			if (!var.type.resolveType(unitContext, ctx) || !var.checkNameAgainstVrbl(mthdContext.vars, ctx) || !var.checkNameAgainstParam(mthdContext.param, ctx) || !var.enterSize(Vrbl.L_LOCAL, ctx))
				return FA_ERROR;
			if (var.minSize < 0)
				offset += var.minSize * ctx.arch.relocBytes; //this is a reloc, use size given in ctx
			else
				offset -= var.minSize; //scalar, take minimum size
			offset &= ~ctx.arch.stackClearBits; //align inside stack
			var.relOff = offset;
			if (var.init != null)
			{
				if (!var.init.resolve(unitContext, mthdContext, getExprFromStmtFlowCode(flowCode) | Expr.RF_CHECKREAD, var.type, ctx))
				{
					ctx.out.print(" in var-init");
					return FA_ERROR;
				}
				if (!SJava.checkVarInitType(var, (mthdContext.marker & Marks.K_EXPC) != 0, unitContext, ctx))
					return FA_ERROR;
				var.modifier |= Modifier.MF_ISWRITTEN; //initialized variables are written
			}
			if (origLastNextVrbl != null)
				last.nextVrbl = origLastNextVrbl; //restore previously changed nextVrbl
			last = var; //remember current variable for next loop
			var = var.nextVrbl;
			varCount++;
		}
		//enter all variables and fix varSize
		last.nextVrbl = origMthdVrbl;
		mthdContext.vars = varList;
		mthdContext.varSize = -offset;
		return flowCode;
	}
	
	protected void innerGenOutput(Context ctx)
	{
		Vrbl var;
		int i;
		
		var = varList;
		for (i = 0; i < varCount; i++, var = var.nextVrbl)
			var.genInitCode(forceInit, ctx);
	}
}
