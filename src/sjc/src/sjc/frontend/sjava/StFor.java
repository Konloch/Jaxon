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
 * StFor: for-loop
 *
 * @author S. Frenz
 * @version 121029 added support for multiple init-/lupd-statements in call to code printer
 * version 121017 added code printer support for labels
 * version 120923 added support for code printer
 * version 101015 adopted changed Expr
 * version 100924 fixed loop flow check
 * version 100922 added F_LOOPEND
 * version 100826 added null-check for loop-update-statement in nested endless loops
 * version 100823 optimized innerGenOutput, added F_CONDEND flowhint
 * version 100411 extension of new parent StLoop
 * version 091111 added support for instance final-var-init
 * version 091109 rewriting of var's written state checks
 * version 091103 added source hint for condition calculation and loop update
 * version 091102 adopted changed Stmt
 * version 091021 adopted changed modifier declarations
 * version 091005 adopted changed Expr
 * version 090809 fixed flow analysis for cond==null
 * version 090724 added flow detailed analysis
 * version 090817 adopted changed Expr
 * version 090508 adopted changes in Stmt
 * version 090506 added flow analysis information
 * version 090207 added copyright notice
 * version 080610 removed conversion to JMthd as it is no longer required
 * version 080508 added flow hints
 * version 070913 made init and lupd of type Stmt, added support for spread local variable declaration
 * version 070909 optimized signature of Expr.resolve, fixed position of contDest, adopted changes in StBreakable
 * version 070908 optimized signature of Stmt.resolve
 * version 070114 reduced access level where possible
 * version 070111 adapted change in printPos and compErr
 * version 061229 removed access to firstInstr
 * version 061203 optimized calls to printPos and compErr
 * version 061128 added support for expression lists
 * version 060607 initial version
 */

public class StFor extends StLoop
{
	protected Stmt init, lupd;
	protected Expr cond;
	private Stmt[] furtherInit, furtherLupd;
	
	protected StFor(StBreakable io, StringList ila, int fid, int il, int ic)
	{
		super(io, ila, fid, il, ic);
	}
	
	public void printBreakableStatement(CodePrinter prnt)
	{
		if (furtherInit == null && init != null && init.nextStmt != null)
			furtherInit = getStmtArray(init.nextStmt);
		if (furtherLupd == null && lupd != null && lupd.nextStmt != null)
			furtherLupd = getStmtArray(lupd.nextStmt);
		prnt.stmtFor(init, furtherInit, lupd, furtherLupd, cond, loStmt);
	}
	
	protected int innerResolve(int flowCode, Unit unitContext, Mthd mthdContext, Context ctx)
	{
		Stmt jel;
		Vrbl oldVarState;
		int oldLoopState = flowCode & FA_INSIDE_LOOP;
		VrblStateList preState = null;
		
		oldVarState = mthdContext.vars;
		jel = init;
		while (jel != null)
		{
			if ((jel.resolve(0, unitContext, mthdContext, ctx) & FA_ERROR) != 0)
				return FA_ERROR; //init-stmts may not change flow
			jel = jel.nextStmt;
		}
		if (cond != null)
		{
			if (!cond.resolve(unitContext, mthdContext, getExprFromStmtFlowCode(flowCode) | Expr.RF_CHECKREAD | Expr.RF_INSIDE_LOOP, null, ctx))
				return FA_ERROR;
			if (!cond.isBoolType())
			{
				printPos(ctx, "need boolean type in for-condition");
				return FA_ERROR;
			}
		}
		if (cond != null && cond.calcConstantType(ctx) == StdTypes.T_INT && cond.getConstIntValue(ctx) == 0)
			loStmt.flowWarn(ctx, ERR_UNREACHABLE_CODE);
		
		if (cond != null && (cond.calcConstantType(ctx) != StdTypes.T_INT || cond.getConstIntValue(ctx) != 1))
			preState = ctx.copyVrblListState(mthdContext.vars, mthdContext.checkInitVars);
		
		flowCode |= FA_INSIDE_LOOP;
		if (((flowCode = loStmt.resolve(flowCode, unitContext, mthdContext, ctx)) & FA_ERROR) != 0)
		{
			if (preState != null)
				ctx.recycleVrblStatelist(preState);
			return FA_ERROR;
		}
		if ((flowCode & FA_NEXT_IS_UNREACHABLE) != 0 && (flowAnalysisBuffer & FA_HAS_CONTINUE) == 0 && lupd != null)
			lupd.flowWarn(ctx, ERR_UNREACHABLE_CODE);
		
		jel = lupd;
		while (jel != null)
		{
			if ((jel.resolve(0, unitContext, mthdContext, ctx) & FA_ERROR) != 0)
			{
				if (preState != null)
					ctx.recycleVrblStatelist(preState);
				return FA_ERROR; //update-stmts may not change flow
			}
			jel = jel.nextStmt;
		}
		
		if (preState != null)
		{
			ctx.setVrblListState(mthdContext.vars, mthdContext.checkInitVars, preState);
			ctx.recycleVrblStatelist(preState);
		}
		
		if ((cond == null || (cond.calcConstantType(ctx) == StdTypes.T_INT && cond.getConstIntValue(ctx) == 1)) && (flowAnalysisBuffer & FA_HAS_ENDBLOCK) == 0)
		{ //endless loop without break
			flowCode |= FA_NEXT_IS_UNREACHABLE;
		}
		if ((flowAnalysisBuffer & FA_HAS_ENDBLOCK) != 0)
			flowCode &= ~FA_NEXT_IS_UNREACHABLE; //break existing
		mthdContext.vars = oldVarState;
		return (flowCode & ~FA_INSIDE_LOOP) | oldLoopState;
	}
	
	protected void innerGenOutput(Context ctx)
	{
		Stmt jel;
		Instruction loopDest, condDest;
		int id;
		
		loopDest = ctx.arch.getUnlinkedInstruction();
		condDest = ctx.arch.getUnlinkedInstruction();
		contDest = ctx.arch.getUnlinkedInstruction();
		breakDest = ctx.arch.getUnlinkedInstruction();
		id = ctx.arch.insertFlowHint(Architecture.F_BLOCKSTART, 0);
		jel = init;
		while (jel != null)
		{
			jel.genOutput(ctx);
			jel = jel.nextStmt;
		}
		ctx.arch.insertFlowHint(Architecture.F_LOOPSTART, id);
		ctx.arch.insertFlowHint(Architecture.F_CONDSTART, id);
		ctx.arch.appendInstruction(condDest);
		if (cond != null)
		{
			ctx.arch.insertSourceHint(cond);
			cond.genOutputCondJmp(breakDest, false, loopDest, ctx);
		}
		ctx.arch.insertFlowHint(Architecture.F_CONDEND, id);
		ctx.arch.appendInstruction(loopDest);
		loStmt.genOutput(ctx);
		ctx.arch.appendInstruction(contDest);
		jel = lupd;
		while (jel != null)
		{
			jel.genOutput(ctx);
			jel = jel.nextStmt;
		}
		ctx.arch.genJmp(condDest);
		ctx.arch.insertFlowHint(Architecture.F_LOOPEND, id);
		ctx.arch.appendInstruction(breakDest);
		ctx.arch.insertFlowHint(Architecture.F_BLOCKEND, id);
		contDest = breakDest = null;
	}
	
	private static Stmt[] getStmtArray(Stmt list)
	{
		Stmt[] arr = null;
		Stmt tmp = list;
		int cnt = 0;
		while (tmp != null)
		{
			cnt++;
			tmp = tmp.nextStmt;
		}
		arr = new Stmt[cnt];
		cnt = 0;
		while (list != null)
		{
			arr[cnt++] = list;
			list = list.nextStmt;
		}
		return arr;
	}
}
