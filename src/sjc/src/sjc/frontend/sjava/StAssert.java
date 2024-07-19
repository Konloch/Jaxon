/* Copyright (C) 2010, 2012 Stefan Frenz
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
 * StAssert: assert-statement
 *
 * @author S. Frenz
 * @version 120923 added support for code printer
 * version 101210 adopted changed Architecture
 * version 101021 initial version
 */

public class StAssert extends Stmt
{
	protected Expr cond, msg;
	private UnitList runtimeClass;
	private boolean skipEncode;
	
	protected StAssert(int fid, int il, int ic)
	{
		super(fid, il, ic);
	}
	
	public void printToken(CodePrinter prnt)
	{
		prnt.stmtAssert(cond, msg);
	}
	
	protected int innerResolve(int flowCode, Unit unitContext, Mthd mthdContext, Context ctx)
	{
		int myFlow = flowCode;
		
		if ((mthdContext.marker & Marks.K_ASRT) == 0 && !ctx.globalAssert)
		{
			myFlow |= FA_DEAD_CODE;
			skipEncode = true;
		}
		if (!cond.resolve(unitContext, mthdContext, getExprFromStmtFlowCode(myFlow) | Expr.RF_CHECKREAD, null, ctx))
			return FA_ERROR;
		if (!cond.isBoolType())
		{
			printPos(ctx, "need boolean type in assert-condition");
			return FA_ERROR;
		}
		if (cond.calcConstantType(ctx) == StdTypes.T_INT && cond.getConstIntValue(ctx) == 0)
		{
			myFlow |= FA_DEAD_CODE;
			skipEncode = true;
		}
		if (msg != null)
		{
			if (!msg.resolve(unitContext, mthdContext, getExprFromStmtFlowCode(myFlow) | Expr.RF_CHECKREAD, null, ctx))
				return FA_ERROR;
			if (msg.compareType(ctx.stringType, false, ctx) != TypeRef.C_EQ)
			{
				printPos(ctx, "need string type in assert-message");
				return FA_ERROR;
			}
		}
		if (!skipEncode)
		{
			ctx.assertUsed = true;
			if (ctx.dynaMem)
				runtimeClass = unitContext.getRefUnit(ctx.rteDynamicRuntime, true);
		}
		return flowCode;
	}
	
	protected void innerGenOutput(Context ctx)
	{
		Instruction afterwards, doCall = null;
		int id = 0, restore, msgReg;
		
		if (skipEncode)
			return; //assert disabled or constant true condition
		afterwards = ctx.arch.getUnlinkedInstruction();
		//check if condition is constant
		if (cond.calcConstantType(ctx) != StdTypes.T_INT)
		{ //condidional assert
			id = ctx.arch.insertFlowHint(Architecture.F_BLOCKSTART, 0);
			doCall = ctx.arch.getUnlinkedInstruction();
			ctx.arch.insertFlowHint(Architecture.F_CONDSTART, id);
			cond.genOutputCondJmp(afterwards, true, doCall, ctx);
			ctx.arch.appendInstruction(doCall);
		}
		//to be done for constant false assert and conditional assert: call
		restore = ctx.arch.ensureFreeRegs(0, 0, 0, 0);
		if (ctx.dynaMem)
			ctx.arch.genSaveUnitContext();
		if (msg == null)
			ctx.arch.genPushConstVal(0, StdTypes.T_PTR);
		else
		{
			if (ctx.arch.prepareFreeReg(0, 0, 0, StdTypes.T_PTR) != 0)
			{ //should be 0 always
				compErr(ctx, "no free reg at assert");
				return;
			}
			msgReg = ctx.arch.allocReg();
			msg.genOutputVal(msgReg, ctx);
			ctx.arch.genPush(msgReg, StdTypes.T_PTR); //push non-constant sync obj
			ctx.arch.deallocRestoreReg(msgReg, 0, 0);
		}
		if (ctx.dynaMem)
		{
			ctx.arch.genLoadUnitContext(ctx.arch.regClss, runtimeClass.relOff);
			ctx.arch.genCall(ctx.rteAssertFailedMd.relOff, ctx.arch.regClss, ctx.rteAssertFailedMd.parSize);
			ctx.arch.genRestUnitContext();
		}
		else
			ctx.arch.genCallConst(ctx.rteAssertFailedMd, ctx.rteAssertFailedMd.parSize);
		ctx.arch.deallocRestoreReg(0, 0, restore);
		ctx.arch.appendInstruction(afterwards);
		if (doCall != null)
			ctx.arch.insertFlowHint(Architecture.F_BLOCKEND, id);
	}
}
