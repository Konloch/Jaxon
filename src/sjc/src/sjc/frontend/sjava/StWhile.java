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
 * StWhile: while- and do-while-loop
 *
 * @author S. Frenz
 * @version 121017 added code printer support for labels
 * version 120923 added support for code printer
 * version 101015 adopted changed Expr
 * version 100922 added F_LOOPEND
 * version 100505 fixed flow state if condition is not constant
 * version 100411 extension of new parent StLoop
 * version 091111 added support for instance final-var-init
 * version 091109 rewriting of var's written state checks
 * version 091103 added source hint for condition calculation
 * version 091102 adopted changed Stmt
 * version 091021 adopted changed modifier declarations
 * version 091005 adopted changed Expr
 * version 090810 fixed variable flow analysis for inclusive-while
 * version 090724 added detailed flow analysis
 * version 090718 adopted changed Expr, simplified exclusive-while flag
 * version 090508 adopted changes in Stmt
 * version 090507 added flow analysis information
 * version 090207 added copyright notice
 * version 080508 added flow hints
 * version 070909 optimized signature of Expr.resolve, adopted changes in StBreakable
 * version 070908 optimized signature of Stmt.resolve
 * version 070114 reduced access level where possible
 * version 070111 adapted change in printPos and compErr
 * version 061229 removed access to firstInstr
 * version 061203 optimized calls to printPos and compErr
 * version 060607 initial version
 */

public class StWhile extends StLoop
{
	protected Expr cond;
	protected boolean inclusiveWhile;
	
	protected StWhile(StBreakable io, StringList ila, int fid, int il, int ic)
	{
		super(io, ila, fid, il, ic);
	}
	
	public void printBreakableStatement(CodePrinter prnt)
	{
		prnt.stmtWhile(cond, inclusiveWhile, loStmt);
	}
	
	protected int innerResolve(int flowCode, Unit unitContext, Mthd mthdContext, Context ctx)
	{
		int oldLoopState = flowCode & FA_INSIDE_LOOP;
		VrblStateList preState = null;
		
		flowCode |= FA_INSIDE_LOOP;
		if (inclusiveWhile)
		{ //inclusive-while is executed at least once
			flowCode = loStmt.resolve(flowCode, unitContext, mthdContext, ctx);
			if (!cond.resolve(unitContext, mthdContext, getExprFromStmtFlowCode(flowCode) | (Expr.RF_CHECKREAD | Expr.RF_INSIDE_LOOP), null, ctx))
				return FA_ERROR;
		}
		else
		{
			if (!cond.resolve(unitContext, mthdContext, getExprFromStmtFlowCode(flowCode) | (Expr.RF_CHECKREAD | Expr.RF_INSIDE_LOOP), null, ctx))
				return FA_ERROR;
			if (cond.calcConstantType(ctx) != StdTypes.T_INT || cond.getConstIntValue(ctx) != 1)
				preState = ctx.copyVrblListState(mthdContext.vars, mthdContext.checkInitVars);
			flowCode = loStmt.resolve(flowCode, unitContext, mthdContext, ctx);
			if (preState != null)
			{
				ctx.setVrblListState(mthdContext.vars, mthdContext.checkInitVars, preState);
				ctx.recycleVrblStatelist(preState);
			}
		}
		
		if (!cond.isBoolType())
		{
			printPos(ctx, "need boolean type in while-condition");
			return FA_ERROR;
		}
		if ((flowAnalysisBuffer & FA_HAS_ENDBLOCK) != 0)
			flowCode &= ~FA_NEXT_IS_UNREACHABLE; //break existing
		else if (cond.calcConstantType(ctx) == StdTypes.T_INT)
		{
			if (cond.getConstIntValue(ctx) == 1)
				flowCode |= FA_NEXT_IS_UNREACHABLE; //endless loop without break
		}
		else
			flowCode &= ~FA_NEXT_IS_UNREACHABLE; //condition existing
		return (flowCode & ~FA_INSIDE_LOOP) | oldLoopState;
	}
	
	protected void innerGenOutput(Context ctx)
	{
		Instruction loopDest;
		int id;
		
		loopDest = ctx.arch.getUnlinkedInstruction();
		breakDest = ctx.arch.getUnlinkedInstruction();
		contDest = ctx.arch.getUnlinkedInstruction();
		id = ctx.arch.insertFlowHint(Architecture.F_BLOCKSTART, 0);
		if (!inclusiveWhile)
		{
			if (cond.calcConstantType(ctx) == StdTypes.T_INT)
			{
				//condition is constant, reduce output
				if (cond.getConstIntValue(ctx) == 0)
					return; //loop will never be executed
				//else: constant true, no check, just start the loop
			}
			else
				ctx.arch.genJmp(contDest); //not constant, first check condition
		}
		ctx.arch.insertFlowHint(Architecture.F_LOOPSTART, id);
		ctx.arch.appendInstruction(loopDest);
		loStmt.genOutput(ctx);
		ctx.arch.insertFlowHint(Architecture.F_CONDSTART, id);
		ctx.arch.appendInstruction(contDest);
		ctx.arch.insertSourceHint(cond);
		cond.genOutputCondJmp(loopDest, true, breakDest, ctx);
		ctx.arch.insertFlowHint(Architecture.F_LOOPEND, id);
		ctx.arch.appendInstruction(breakDest);
		ctx.arch.insertFlowHint(Architecture.F_BLOCKEND, id);
		breakDest = contDest = null;
	}
}
