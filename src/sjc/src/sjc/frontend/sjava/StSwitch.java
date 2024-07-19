/* Copyright (C) 2005, 2006, 2007, 2008, 2009, 2010, 2012, 2015 Stefan Frenz
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
 * StSwitch: switch-case-statement
 *
 * @author S. Frenz
 * @version 150926 added test for unique case constants
 * version 121017 added code printer support for labels
 * version 120923 added support for code printer
 * version 101015 adopted changed Expr
 * version 100616 fixed unreachable warning
 * version 100504 adopted changed StBreakable
 * version 091111 added support for instance final-var-init
 * version 091109 added variable-write analysis
 * version 091102 adopted changed Stmt
 * version 091005 adopted changed Expr
 * version 090809 adopted changed Expr, added support for detailed flow analysis
 * version 090718 adopted changed Expr
 * version 090508 adopted changes in Stmt
 * version 090506 added flow analysis information
 * version 090207 added copyright notice
 * version 081021 adopted changed Architecture.genComp/genCondJmp
 * version 080604 better internal error message
 * version 080525 adopted changed genCondJmp signature
 * version 080508 added flow hints
 * version 071108 fixed check for exact type match for constant case-expression
 * version 070913 added support for spread local variable declaration
 * version 070909 optimized signature of Expr.resolve, adopted changes in StBreakable
 * version 070908 optimized signature of Stmt.resolve
 * version 070809 adopted changed for float/double
 * version 070628 adopted changes for "Expr extends TypeRef"
 * version 070625 using StdTypes instead of TypeRef for standard types
 * version 070114 reduced access level where possible
 * version 070111 adapted change in printPos and compErr
 * version 061231 optimized genOutput
 * version 061229 removed access to firstInstr
 * version 061203 optimized calls to printPos and compErr
 * version 060620 added kill-on-jump
 * version 060607 initial version
 */

public class StSwitch extends StBreakable
{
	protected Expr cond;
	protected Stmt stmts, def;
	protected CondStmt caseConds;
	private int constType;
	
	protected StSwitch(StBreakable io, StringList ila, int fid, int il, int ic)
	{
		super(io, ila, fid, il, ic);
	}
	
	public void printBreakableStatement(CodePrinter prnt)
	{
		Stmt stmt = stmts;
		CondStmt cc = caseConds;
		prnt.stmtSwitchStart(cond);
		while (stmt != null)
		{
			if (cc != null && stmt == cc.stmt)
			{
				prnt.stmtSwitchCase(cc.cond);
				cc = cc.nextCondStmt;
				stmt = stmt.nextStmt; //skip StEmpty which is inserted for this particular case
			}
			else if (stmt == def)
			{
				prnt.stmtSwitchCase(null);
				stmt = stmt.nextStmt; //skip StEmpty which is inserted for default
			}
			else
			{
				stmt.printToken(prnt);
				stmt = stmt.nextStmt;
			}
		}
		prnt.stmtSwitchEnd();
	}
	
	protected int innerResolve(int flowCode, Unit unitContext, Mthd mthdContext, Context ctx)
	{
		Stmt stmt;
		CondStmt cases, cmp;
		Vrbl oldVarState;
		int singleRes = FA_NO_FLOWCHANGE, complRes = FA_NO_FLOWCHANGE;
		VrblStateList preState, sure = null;
		
		//remember variable state
		oldVarState = mthdContext.vars;
		//resolve condition
		if (!cond.resolve(unitContext, mthdContext, getExprFromStmtFlowCode(flowCode) | Expr.RF_CHECKREAD, null, ctx))
		{
			ctx.out.print(" in switch-condition");
			return FA_ERROR;
		}
		if (cond.arrDim > 0)
		{
			printPos(ctx, "switch needs basic type");
			return FA_ERROR;
		}
		switch (cond.baseType)
		{
			case StdTypes.T_BYTE:
			case StdTypes.T_SHRT:
			case StdTypes.T_CHAR:
			case StdTypes.T_INT:
				constType = StdTypes.T_INT;
				break;
			case StdTypes.T_LONG:
				constType = StdTypes.T_LONG;
				break;
			default:
				printPos(ctx, "type of condition must be byte, short, char, int or long");
				return FA_ERROR;
		}
		//resolve cases and statements
		stmt = stmts;
		cases = caseConds;
		preState = ctx.copyVrblListState(mthdContext.vars, mthdContext.checkInitVars);
		while (stmt != null)
		{
			//handle case conditions if this is their place
			if (def == stmt || (cases != null && cases.stmt == stmt))
			{ //only one of def==stmt and cases.stmt==stmt may be true
				//merge current state into complRes, reset buffer and singleRes as a new case-block starts
				complRes |= flowAnalysisBuffer & FA_HAS_ENDBLOCK;
				singleRes = flowAnalysisBuffer = flowCode;
				//mark variables as potentially written
				ctx.setVrblStatePotential(mthdContext.vars, mthdContext.checkInitVars, preState);
				//resolve condition if case-statement
				if (def != stmt)
				{ //if def!=stmt, then cases!=null && cases.stmt==stmt
					//resolve expression
					if (!cases.cond.resolve(unitContext, mthdContext, getExprFromStmtFlowCode(flowCode) | Expr.RF_CHECKREAD, cond, ctx))
					{
						ctx.out.print(" in case-condition");
						ctx.recycleVrblStatelist(preState);
						ctx.recycleVrblStatelist(sure);
						return FA_ERROR;
					}
					if (cases.cond.calcConstantType(ctx) != constType || cases.cond.baseType != cond.baseType)
					{
						printPos(ctx, "condition is not constant or has wrong type (must match switch expression)");
						ctx.recycleVrblStatelist(preState);
						ctx.recycleVrblStatelist(sure);
						return FA_ERROR;
					}
					//set case-check to next case
					cases = cases.nextCondStmt;
				}
			}
			//handle statement
			if (((singleRes = stmt.resolve(singleRes, unitContext, mthdContext, ctx)) & FA_ERROR) != 0)
			{
				ctx.out.print(" in switch-statement");
				ctx.recycleVrblStatelist(preState);
				ctx.recycleVrblStatelist(sure);
				return FA_ERROR;
			}
			if ((flowAnalysisBuffer & FA_HAS_SHORTCUT) != 0 || ((singleRes & FA_NEXT_IS_UNREACHABLE) == 0 && stmt.nextStmt == null))
			{
				flowAnalysisBuffer &= ~FA_HAS_SHORTCUT;
				if (def != null)
				{
					if (sure == null)
						sure = ctx.copyVrblListState(mthdContext.vars, mthdContext.checkInitVars);
					else
						ctx.mergeVrblListState(sure, mthdContext.vars, mthdContext.checkInitVars);
				}
			}
			if ((singleRes & FA_NEXT_IS_UNREACHABLE) != 0)
			{
				ctx.setVrblListState(mthdContext.vars, mthdContext.checkInitVars, preState);
			}
			//next statement
			stmt = stmt.nextStmt;
		}
		//last block was potentially not merged into complRes
		complRes |= flowAnalysisBuffer & FA_HAS_ENDBLOCK;
		if ((complRes & FA_HAS_ENDBLOCK) == 0 && (singleRes & FA_NEXT_IS_UNREACHABLE) != 0 && def != null)
			flowCode |= FA_NEXT_IS_UNREACHABLE;
		if ((complRes & FA_ERROR) != 0)
			flowCode |= FA_ERROR;
		//handle variable writing
		ctx.setVrblListState(mthdContext.vars, mthdContext.checkInitVars, preState);
		if (sure != null)
			ctx.setVrblStateSure(mthdContext.vars, mthdContext.checkInitVars, sure);
		ctx.recycleVrblStatelist(preState);
		ctx.recycleVrblStatelist(sure);
		//check if case constants are unique
		cases = caseConds;
		while (cases != null)
		{
			cmp = cases.nextCondStmt;
			while (cmp != null)
			{
				if (constType == StdTypes.T_INT ? cases.cond.getConstIntValue(ctx) == cmp.cond.getConstIntValue(ctx) : cases.cond.getConstLongValue(ctx) == cmp.cond.getConstLongValue(ctx))
				{
					printPos(ctx, "case constant already used");
					return FA_ERROR;
				}
				cmp = cmp.nextCondStmt;
			}
			cases = cases.nextCondStmt;
		}
		//restore variable state
		mthdContext.vars = oldVarState;
		return flowCode;
	}
	
	protected boolean isBreakContDest(boolean named, boolean contNotBreak)
	{
		return !contNotBreak; //only breakable, named irrelevant
	}
	
	protected void innerGenOutput(Context ctx)
	{
		Stmt stmt;
		CondStmt condstmt;
		int reg, id, condHnd;
		Instruction defIns = null;
		
		breakDest = ctx.arch.getUnlinkedInstruction();
		id = ctx.arch.insertFlowHint(Architecture.F_BLOCKSTART, 0);
		ctx.arch.insertFlowHint(Architecture.F_CONDSTART, id);
		if (ctx.arch.prepareFreeReg(0, 0, 0, cond.baseType) != 0)
		{ //should be 0 always
			compErr(ctx, "no free reg at beginning of switch");
			return;
		}
		reg = ctx.arch.allocReg();
		cond.genOutputVal(reg, ctx);
		ctx.arch.insertKillHint(reg);
		condstmt = caseConds;
		while (condstmt != null)
		{
			if (constType == StdTypes.T_INT)
				condHnd = ctx.arch.genCompValToConstVal(reg, condstmt.cond.getConstIntValue(ctx), cond.baseType, Ops.C_EQ);
			else
				condHnd = ctx.arch.genCompValToConstDoubleOrLongVal(reg, condstmt.cond.getConstLongValue(ctx), false, Ops.C_EQ);
			ctx.arch.genCondJmp(condstmt.stIns = ctx.arch.getUnlinkedInstruction(), condHnd);
			condstmt = condstmt.nextCondStmt;
		}
		ctx.arch.deallocRestoreReg(reg, 0, 0);
		if (def != null)
		{
			defIns = ctx.arch.getUnlinkedInstruction();
			ctx.arch.genJmp(defIns);
		}
		else
			ctx.arch.genJmp(breakDest);
		stmt = stmts;
		condstmt = caseConds;
		ctx.arch.insertFlowHint(Architecture.F_SWSBSTART, id);
		while (stmt != null)
		{
			while (condstmt != null && condstmt.stmt == stmt)
			{
				ctx.arch.appendInstruction(condstmt.stIns);
				condstmt.stIns = null;
				condstmt = condstmt.nextCondStmt;
			}
			if (def == stmt)
				ctx.arch.appendInstruction(defIns);
			stmt.genOutput(ctx);
			stmt = stmt.nextStmt;
		}
		ctx.arch.appendInstruction(breakDest);
		ctx.arch.insertFlowHint(Architecture.F_BLOCKEND, id);
		breakDest = null;
	}
}
