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

import sjc.backend.Architecture;
import sjc.backend.Instruction;
import sjc.compbase.*;
import sjc.debug.CodePrinter;

/**
 * StIf: if-statement
 *
 * @author S. Frenz
 * @version 120923 added support for code printer
 * version 101015 adopted changed Expr
 * version 100925 immediate stop if trueBlock is not resolvable
 * version 091111 added support for instance final-var-init
 * version 091109 rewriting of var's written state checks
 * version 091102 adopted changed Stmt
 * version 091021 adopted changed modifier declarations
 * version 091005 adopted changed Expr
 * version 090724 adopted changed Expr
 * version 090719 adopted changed Expr, added support for unwrittenVars
 * version 090508 adopted changes in Stmt
 * version 090507 added flow analysis information
 * version 090207 added copyright notice
 * version 080508 added flow hints
 * version 070909 optimized signature of Expr.resolve
 * version 070908 optimized signature of Stmt.resolve
 * version 070114 reduced access level where possible
 * version 070111 adapted change in printPos and compErr
 * version 061229 removed access to firstInstr
 * version 061203 optimized calls to printPos and compErr
 * version 060607 initial version
 */

public class StIf extends Stmt
{
	protected Expr cond;
	protected Stmt trStmt, faStmt;
	
	protected StIf(int fid, int il, int ic)
	{
		super(fid, il, ic);
	}
	
	public void printToken(CodePrinter prnt)
	{
		prnt.stmtIf(cond, trStmt, faStmt);
	}
	
	protected int innerResolve(int flowCode, Unit unitContext, Mthd mthdContext, Context ctx)
	{
		int trRes = FA_NO_FLOWCHANGE, faRes = FA_NO_FLOWCHANGE;
		VrblStateList preState, trState;
		int flowTrue = flowCode, flowFalse = flowCode, flowFilter = -1;
		
		if (!cond.resolve(unitContext, mthdContext, getExprFromStmtFlowCode(flowCode) | Expr.RF_CHECKREAD, null, ctx))
			return FA_ERROR;
		if (!cond.isBoolType())
		{
			printPos(ctx, "need boolean type in if-condition");
			return FA_ERROR;
		}
		if (cond.calcConstantType(ctx) == StdTypes.T_INT)
		{
			if (cond.getConstIntValue(ctx) == 1)
				flowFalse |= FA_DEAD_CODE;
			else
				flowTrue |= FA_DEAD_CODE;
			if ((flowCode & FA_DEAD_CODE) == 0)
				flowFilter = ~FA_DEAD_CODE; //filter dead code
		}
		
		preState = ctx.copyVrblListState(mthdContext.vars, mthdContext.checkInitVars);
		if ((trRes = trStmt.resolve(flowTrue, unitContext, mthdContext, ctx)) == FA_ERROR)
			return FA_ERROR;
		if (faStmt != null)
		{
			trState = ctx.copyVrblListState(mthdContext.vars, mthdContext.checkInitVars);
			ctx.setVrblListState(mthdContext.vars, mthdContext.checkInitVars, preState);
			if ((faRes = faStmt.resolve(flowFalse, unitContext, mthdContext, ctx)) == FA_ERROR)
				return FA_ERROR;
			if (((trRes | faRes) & FA_NEXT_IS_UNREACHABLE) != 0)
			{ //at least one of tr/fa has next_unreachable set
				if ((trRes & FA_NEXT_IS_UNREACHABLE) != 0)
				{
					if ((faRes & FA_NEXT_IS_UNREACHABLE) != 0)
						ctx.setVrblListState(mthdContext.vars, mthdContext.checkInitVars, preState);
					//else: nothing to do if only faStmt is relevant
				}
				else
					ctx.setVrblListState(mthdContext.vars, mthdContext.checkInitVars, trState);
			}
			else
				ctx.setVrblStateCombined(mthdContext.vars, mthdContext.checkInitVars, trState);
			ctx.recycleVrblStatelist(trState);
		}
		else
		{ //there is no faStmt
			if ((trRes & FA_NEXT_IS_UNREACHABLE) != 0)
				ctx.setVrblListState(mthdContext.vars, mthdContext.checkInitVars, preState);
			else
				ctx.setVrblStatePotential(mthdContext.vars, mthdContext.checkInitVars, preState);
		}
		ctx.recycleVrblStatelist(preState);
		
		return ((trRes & faRes & FA_NEXT_IS_UNREACHABLE) //flags both branches must have
				| ((trRes | faRes) & ~FA_NEXT_IS_UNREACHABLE)) //all other flags one branch may have
				& flowFilter; //filter dead code if entry hadn't set this flag
	}
	
	protected void innerGenOutput(Context ctx)
	{
		Instruction trIns, faIns = null, afterwards;
		int id;
		
		//check if condition is constant
		if (cond.calcConstantType(ctx) == StdTypes.T_INT)
		{
			if (cond.getConstIntValue(ctx) == 1)
				trStmt.genOutput(ctx);
			else
			{
				if (faStmt != null)
					faStmt.genOutput(ctx);
				//else: nothing to do
			}
			return;
		}
		//not a constant condition, generate code
		trIns = ctx.arch.getUnlinkedInstruction();
		afterwards = ctx.arch.getUnlinkedInstruction();
		id = ctx.arch.insertFlowHint(Architecture.F_BLOCKSTART, 0);
		ctx.arch.insertFlowHint(Architecture.F_CONDSTART, id);
		if (faStmt != null)
		{
			faIns = ctx.arch.getUnlinkedInstruction();
			cond.genOutputCondJmp(faIns, false, trIns, ctx);
		}
		else
			cond.genOutputCondJmp(afterwards, false, trIns, ctx);
		ctx.arch.insertFlowHint(Architecture.F_TRUESTART, id);
		ctx.arch.appendInstruction(trIns);
		trStmt.genOutput(ctx);
		if (faStmt != null)
		{
			ctx.arch.genJmp(afterwards);
			ctx.arch.insertFlowHint(Architecture.F_ELSESTART, id);
			ctx.arch.appendInstruction(faIns);
			faStmt.genOutput(ctx);
		}
		ctx.arch.appendInstruction(afterwards);
		ctx.arch.insertFlowHint(Architecture.F_BLOCKEND, id);
	}
}
