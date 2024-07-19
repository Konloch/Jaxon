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
 * StForEnh: enhanced for-loop of jdk1.5
 *
 * @author S. Frenz
 * @version 121017 added code printer support for labels
 * version 120923 added support for code printer
 * version 101015 adopted changed Expr
 * version 100922 added F_LOOPEND
 * version 100813 removed unneeded variables
 * version 100413 added source hint for condition test
 * version 100412 adopted changed ExDeArray
 * version 100411 initial version
 */

public class StForEnh extends StLoop
{
	protected StVrbl var; //declaration of
	protected Expr iter;
	private ExDeArray iterDeArray;
	private int valOffset, iterPtrOffset, stopPtrOffset;
	
	protected StForEnh(StBreakable io, StringList ila, int fid, int il, int ic)
	{
		super(io, ila, fid, il, ic);
	}
	
	public void printBreakableStatement(CodePrinter prnt)
	{
		prnt.stmtForEnh(var, iter, loStmt);
	}
	
	protected int innerResolve(int flowCode, Unit unitContext, Mthd mthdContext, Context ctx)
	{
		Vrbl oldVarState;
		int oldLoopState = flowCode & FA_INSIDE_LOOP;
		
		oldVarState = mthdContext.vars;
		stopPtrOffset = -(mthdContext.varSize += (ctx.arch.relocBytes + ctx.arch.stackClearBits) & ~ctx.arch.stackClearBits);
		iterPtrOffset = -(mthdContext.varSize += (ctx.arch.relocBytes + ctx.arch.stackClearBits) & ~ctx.arch.stackClearBits);
		if ((var.resolve(0, unitContext, mthdContext, ctx) & FA_ERROR) != 0)
			return FA_ERROR; //var-decl may not change flow
		valOffset = var.varList.relOff;
		var.varList.modifier |= Modifier.MF_ISWRITTEN; //treat variable are written
		iterDeArray = new ExDeArray(fileID, line, col);
		iterDeArray.le = iter;
		if (!iterDeArray.resolve(unitContext, mthdContext, getExprFromStmtFlowCode(flowCode) | Expr.RF_CHECKREAD, var.varList.type, ctx))
			return FA_ERROR;
		if (iterDeArray.compareType(var.varList.type, true, ctx) != TypeRef.C_EQ)
		{
			printPos(ctx, "types do not match in enhanced for loop");
			return FA_ERROR;
		}
		
		flowCode |= FA_INSIDE_LOOP;
		if (((flowCode = loStmt.resolve(flowCode, unitContext, mthdContext, ctx)) & FA_ERROR) != 0)
			return FA_ERROR;
		
		mthdContext.vars = oldVarState;
		return (flowCode & ~FA_INSIDE_LOOP) | oldLoopState;
	}
	
	protected void innerGenOutput(Context ctx)
	{
		Instruction loopDest, condDest;
		int idFlow, idCond, elemSize;
		int restore1, addr1, restore2, addr2, valRegType, restoreVal, valReg;
		
		elemSize = iterDeArray.getElemSize(ctx);
		valRegType = var.varList.type.getRegType(ctx);
		
		loopDest = ctx.arch.getUnlinkedInstruction();
		condDest = ctx.arch.getUnlinkedInstruction();
		contDest = ctx.arch.getUnlinkedInstruction();
		breakDest = ctx.arch.getUnlinkedInstruction();
		idFlow = ctx.arch.insertFlowHint(Architecture.F_BLOCKSTART, 0);
		
		restore1 = ctx.arch.prepareFreeReg(0, 0, 0, StdTypes.T_PTR);
		addr1 = ctx.arch.allocReg();
		restore2 = ctx.arch.prepareFreeReg(addr1, 0, 0, StdTypes.T_PTR);
		addr2 = ctx.arch.allocReg();
		iterDeArray.genOutputAddrFirstStopElem(addr1, addr2, ctx);
		ctx.arch.genStoreVarVal(ctx.arch.regBase, null, iterPtrOffset, addr1, StdTypes.T_PTR);
		ctx.arch.genStoreVarVal(ctx.arch.regBase, null, stopPtrOffset, addr2, StdTypes.T_PTR);
		ctx.arch.deallocRestoreReg(addr2, 0, restore2);
		ctx.arch.deallocRestoreReg(addr1, 0, restore1);
		ctx.arch.genJmp(condDest);
		
		ctx.arch.insertFlowHint(Architecture.F_LOOPSTART, idFlow);
		ctx.arch.appendInstruction(loopDest);
		loStmt.genOutput(ctx);
		
		ctx.arch.appendInstruction(contDest);
		restore1 = ctx.arch.prepareFreeReg(0, 0, 0, StdTypes.T_PTR);
		addr1 = ctx.arch.allocReg();
		ctx.arch.genLoadVarVal(addr1, ctx.arch.regBase, null, iterPtrOffset, StdTypes.T_PTR);
		restore2 = ctx.arch.prepareFreeReg(0, 0, addr1, StdTypes.T_PTR);
		addr2 = ctx.arch.allocReg();
		ctx.arch.genLoadVarAddr(addr2, addr1, null, elemSize);
		ctx.arch.genStoreVarVal(ctx.arch.regBase, null, iterPtrOffset, addr2, StdTypes.T_PTR);
		ctx.arch.deallocRestoreReg(addr2, addr1, restore2);
		ctx.arch.deallocRestoreReg(addr1, 0, restore1);
		
		ctx.arch.insertFlowHint(Architecture.F_CONDSTART, idFlow);
		ctx.arch.appendInstruction(condDest);
		ctx.arch.insertSourceHint(this);
		restoreVal = ctx.arch.prepareFreeReg(0, 0, 0, valRegType);
		valReg = ctx.arch.allocReg();
		restore1 = ctx.arch.prepareFreeReg(0, 0, valReg, StdTypes.T_PTR);
		addr1 = ctx.arch.allocReg();
		ctx.arch.genLoadVarVal(addr1, ctx.arch.regBase, null, iterPtrOffset, StdTypes.T_PTR);
		restore2 = ctx.arch.prepareFreeReg(addr1, 0, 0, StdTypes.T_PTR);
		addr2 = ctx.arch.allocReg();
		ctx.arch.genLoadVarVal(addr2, ctx.arch.regBase, null, stopPtrOffset, StdTypes.T_PTR);
		idCond = ctx.arch.genComp(addr1, addr2, StdTypes.T_PTR, Ops.C_EQ);
		ctx.arch.deallocRestoreReg(addr2, 0, restore2);
		ctx.arch.insertKillHint(addr1);
		ctx.arch.insertKillHint(valReg);
		ctx.arch.genCondJmp(breakDest, idCond);
		iterDeArray.genOutputValOfElemAddr(valReg, addr1, ctx);
		ctx.arch.deallocRestoreReg(addr1, valReg, restore1);
		ctx.arch.genStoreVarVal(ctx.arch.regBase, null, valOffset, valReg, valRegType);
		ctx.arch.deallocRestoreReg(valReg, 0, restoreVal);
		ctx.arch.genJmp(loopDest);
		
		ctx.arch.insertFlowHint(Architecture.F_LOOPEND, idFlow);
		ctx.arch.appendInstruction(breakDest);
		ctx.arch.insertFlowHint(Architecture.F_BLOCKEND, idFlow);
		contDest = breakDest = null;
	}
}
