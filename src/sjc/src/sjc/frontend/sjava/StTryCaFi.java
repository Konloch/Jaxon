/* Copyright (C) 2008, 2009, 2010, 2012 Stefan Frenz
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
 * StTryCaFi: try-catch-finally block
 *
 * @author S. Frenz
 * @version 121020 fixed method hierarchy
 * version 121016 added support for code printer
 * version 121014 added dummy for code printer
 * version 100526 fixed register saving on cleanup
 * version 100504 extension of StBreakable to support cleanup code generation
 * version 100409 adopted changed TypeRef
 * version 091111 added support for instance final-var-init
 * version 091109 rewriting of var's written state checks
 * version 091021 adopted changed modifier declarations
 * version 091013 adopted changed method signature of genStore*
 * version 091001 adopted changed memory interface
 * version 090724 added support for detailed flow analysis
 * version 090619 adopted changed Architecture
 * version 090508 adopted changes in Stmt
 * version 090507 added flow analysis information
 * version 090208 using genStoreVarConstVal instead of genClearMem
 * version 090207 added copyright notice
 * version 081021 adopted changed Architecture.genComp/genCondJmp
 * version 080616 fixed kill-hints, added support for ctx.noThrowFrames
 * version 080615 added catch-reachable-checking, adopted changed Architecture.throwFrame*
 * version 080610 added throwable-checking
 * version 080603 initial version
 */

public class StTryCaFi extends StBlock
{
	public final static String REGNOTFREE = "registers not clear";
	public final static String UNREACHABLE = "unreachable catch of ";
	
	protected Stmt tryBlock, finallyBlock;
	protected CatchBlock catchBlocks;
	private UnitList runtimeClass;
	private int excFrameOffset;
	
	//for description of throw frame, see Architecture
	
	protected StTryCaFi(StBreakable io, StringList ila, int fid, int il, int ic)
	{
		super(io, ila, fid, il, ic);
	}
	
	public void printBreakableStatement(CodePrinter prnt)
	{
		CatchBlock c = catchBlocks;
		prnt.stmtTryStart(tryBlock);
		while (c != null)
		{
			prnt.stmtTryCatch(c.catchVar, c.stmts);
			c = c.nextCatchDecl;
		}
		if (finallyBlock != null)
			prnt.stmtTryFinally(finallyBlock);
	}
	
	protected int innerResolve(int flowCode, Unit unitContext, Mthd mthdContext, Context ctx)
	{
		JMthd mthd;
		TryCaFiContainer myContainer, dummyContainer = null;
		CatchBlock curCatch, cmpCatch;
		Vrbl var;
		int singleRes, globalRes;
		boolean allBlocksHaveNextUnreachable = false;
		VrblStateList preState;
		
		//enter ourself in try-container of mthdContext, get exception frame offset
		if (!(mthdContext instanceof JMthd))
		{
			printPos(ctx, "try statement in not-java method is not supported");
			return FA_ERROR;
		}
		mthd = (JMthd) mthdContext;
		if (mthd.freeTryFrames != null)
		{
			myContainer = mthd.freeTryFrames;
			excFrameOffset = myContainer.excFrameOffset;
			dummyContainer = myContainer.nextTryCaFiBlock;
		}
		else
		{
			myContainer = new TryCaFiContainer();
			myContainer.excFrameOffset = excFrameOffset = -(mthdContext.varSize += ctx.arch.throwFrameSize);
		}
		myContainer.stTryCaFi = this;
		myContainer.nextTryCaFiBlock = mthd.curTryFrame;
		mthd.curTryFrame = myContainer;
		mthd.freeTryFrames = dummyContainer;
		//check catch-types, required for throwable-checking of blocks
		curCatch = catchBlocks;
		while (curCatch != null)
		{
			if (!(var = curCatch.catchVar).type.resolveType(unitContext, ctx) || !var.checkNameAgainstParam(mthdContext.param, ctx) || !var.checkNameAgainstVrbl(mthdContext.vars, ctx) || !var.enterSize(Vrbl.L_LOCAL, ctx))
				return FA_ERROR;
			if (!var.type.isThrowableType(ctx))
			{
				printPos(ctx, "catch needs throwable type");
				return FA_ERROR;
			}
			cmpCatch = catchBlocks;
			while (cmpCatch != curCatch)
			{
				if (cmpCatch.catchVar.type.qid.unitDest.isParent(var.type.qid.unitDest, ctx))
				{
					curCatch.printPos(ctx, UNREACHABLE);
					ctx.out.print(curCatch.catchVar.type.qid.unitDest.name);
					ctx.out.print(" (already catched)");
					return FA_ERROR;
				}
				cmpCatch = cmpCatch.nextCatchDecl;
			}
			if (!var.type.isCheckedExceptionType(ctx))
				curCatch.isValid = true; //not checked exceptions are always valid
			curCatch.catchVar.type.qid.unitDest.modifier |= Modifier.MA_ACCSSD;
			if (ctx.dynaMem)
				curCatch.importedClass = unitContext.getRefUnit(var.type.qid.unitDest, true);
			var.relOff = excFrameOffset + ctx.arch.throwFrameExcOff;
			var.nextVrbl = mthdContext.vars;
			curCatch = curCatch.nextCatchDecl;
		}
		//check blocks
		preState = ctx.copyVrblListState(mthdContext.vars, mthdContext.checkInitVars);
		globalRes = (singleRes = tryBlock.resolve(flowCode, unitContext, mthdContext, ctx)) & FA_ERROR;
		if ((singleRes & FA_NEXT_IS_UNREACHABLE) != 0)
			allBlocksHaveNextUnreachable = true;
		curCatch = catchBlocks;
		while (curCatch != null)
		{
			var = (mthdContext.vars = curCatch.catchVar).nextVrbl;
			globalRes |= (singleRes = curCatch.stmts.resolve(flowCode, unitContext, mthdContext, ctx)) & FA_ERROR;
			if ((singleRes & FA_HAS_ENDBLOCK) != 0 || (singleRes & FA_NEXT_IS_UNREACHABLE) == 0)
				allBlocksHaveNextUnreachable = false;
			mthdContext.vars = var;
			curCatch = curCatch.nextCatchDecl;
		}
		if (finallyBlock != null)
		{
			globalRes |= (singleRes = finallyBlock.resolve(flowCode, unitContext, mthdContext, ctx)) & FA_ERROR;
			if ((singleRes & FA_NEXT_IS_UNREACHABLE) != 0)
			{
				finallyBlock.flowWarn(ctx, "finally block does not end normally");
				globalRes |= FA_NEXT_IS_UNREACHABLE;
			}
		}
		if (allBlocksHaveNextUnreachable)
			globalRes |= FA_NEXT_IS_UNREACHABLE;
		ctx.setVrblListState(mthdContext.vars, mthdContext.checkInitVars, preState);
		ctx.recycleVrblStatelist(preState);
		//restore exception container of mthdContext
		dummyContainer = myContainer.nextTryCaFiBlock;
		myContainer.nextTryCaFiBlock = mthd.freeTryFrames;
		mthd.freeTryFrames = myContainer;
		mthd.curTryFrame = dummyContainer;
		//check if all catch-blocks are valid
		curCatch = catchBlocks;
		while (curCatch != null)
		{
			if (!curCatch.isValid)
			{
				curCatch.printPos(ctx, UNREACHABLE);
				ctx.out.print(curCatch.catchVar.type.qid.unitDest.name);
				ctx.out.print(" (never thrown)");
				return FA_ERROR;
			}
			curCatch = curCatch.nextCatchDecl;
		}
		//everything ok, check imports
		if (ctx.dynaMem)
			runtimeClass = unitContext.getRefUnit(ctx.rteDynamicRuntime, true);
		//everything done
		return globalRes;
	}
	
	protected boolean handlesThrowable(Unit thrown, Context ctx)
	{
		CatchBlock curCatch = catchBlocks;
		
		while (curCatch != null)
		{
			if (curCatch.catchVar.type.qid.unitDest.isParent(thrown, ctx))
			{
				curCatch.isValid = true; //mark current catch-block valid
				return true;
			}
			curCatch = curCatch.nextCatchDecl;
		}
		return false;
	}
	
	protected void genOutput(Context ctx)
	{
		Instruction catchListStart, finallyStart, afterAll;
		CatchBlock curCatch;
		int regExc, regRes, id, condHnd;
		
		if (ctx.noThrowFrames)
		{ //code-optimized skipping of catch-blocks
			breakDest = ctx.arch.getUnlinkedInstruction();
			tryBlock.genOutput(ctx);
			ctx.arch.appendInstruction(breakDest);
			if (finallyBlock != null)
			{
				breakDest = ctx.arch.getUnlinkedInstruction();
				finallyBlock.genOutput(ctx);
				ctx.arch.appendInstruction(breakDest);
			}
		}
		else
		{ //normal and Java-conforming code
			id = ctx.arch.insertFlowHint(Architecture.F_BLOCKSTART, 0);
			ctx.arch.insertFlowHint(Architecture.F_TRYSTART, id);
			catchListStart = ctx.arch.getUnlinkedInstruction();
			breakDest = finallyStart = ctx.arch.getUnlinkedInstruction();
			afterAll = ctx.arch.getUnlinkedInstruction();
			if ((regRes = buildGlobalAddress(ctx)) == 0)
				return;
			ctx.arch.genThrowFrameBuild(regRes, catchListStart, excFrameOffset);
			ctx.arch.deallocRestoreReg(regRes, 0, 0);
			tryBlock.genOutput(ctx);
			if (catchBlocks != null)
			{
				ctx.arch.genJmp(finallyStart);
				ctx.arch.insertFlowHint(Architecture.F_CONDSTART, id);
				ctx.arch.appendInstruction(catchListStart);
				curCatch = catchBlocks;
				while (curCatch != null)
				{
					curCatch.stIns = ctx.arch.getUnlinkedInstruction();
					if (ctx.arch.prepareFreeReg(0, 0, 0, StdTypes.T_BOOL) != 0)
					{
						compErr(ctx, REGNOTFREE);
						return;
					}
					regRes = ctx.arch.allocReg();
					if (ctx.arch.prepareFreeReg(0, 0, regRes, StdTypes.T_PTR) != 0)
					{
						compErr(ctx, REGNOTFREE);
						return;
					}
					regExc = ctx.arch.allocReg();
					ctx.arch.genLoadVarVal(regExc, ctx.arch.regBase, null, excFrameOffset + ctx.arch.throwFrameExcOff, StdTypes.T_PTR);
					ExCheckType.genIsType(regRes, regExc, false, curCatch.catchVar.type, ExCheckType.C_ISINST, runtimeClass, curCatch.importedClass, ctx);
					ctx.arch.deallocRestoreReg(regExc, regRes, 0);
					condHnd = ctx.arch.genCompValToConstVal(regRes, 0, StdTypes.T_BOOL, Ops.C_NE);
					ctx.arch.deallocRestoreReg(regRes, 0, 0);
					ctx.arch.genCondJmp(curCatch.stIns, condHnd);
					curCatch = curCatch.nextCatchDecl;
				}
				if (finallyBlock != null)
					ctx.arch.genJmp(finallyStart);
				else
				{
					if (ctx.arch.prepareFreeReg(0, 0, 0, StdTypes.T_PTR) != 0)
					{
						compErr(ctx, REGNOTFREE);
						return;
					}
					regExc = ctx.arch.allocReg();
					ctx.arch.genLoadVarVal(regExc, ctx.arch.regBase, null, excFrameOffset, StdTypes.T_PTR);
					StThrow.genThrow(regExc, runtimeClass, ctx);
					ctx.arch.deallocRestoreReg(regExc, 0, 0);
				}
				curCatch = catchBlocks;
				while (curCatch != null)
				{
					ctx.arch.insertFlowHint(Architecture.F_CAFISTART, id);
					ctx.arch.appendInstruction(curCatch.stIns);
					curCatch.stIns = null;
					if (finallyBlock == null)
					{
						if ((regRes = buildGlobalAddress(ctx)) == 0)
							return;
						ctx.arch.genThrowFrameReset(regRes, excFrameOffset);
						ctx.arch.deallocRestoreReg(regRes, 0, 0);
					}
					else
						ctx.arch.genThrowFrameUpdate(catchListStart, finallyStart, excFrameOffset);
					curCatch.stmts.genOutput(ctx);
					if (finallyBlock != null)
					{
						ctx.arch.genStoreVarConstVal(ctx.arch.regBase, null, excFrameOffset + ctx.arch.throwFrameExcOff, 0, StdTypes.T_PTR);
						ctx.arch.genJmp(finallyStart);
					}
					else
						ctx.arch.genJmp(afterAll);
					curCatch = curCatch.nextCatchDecl;
				}
			}
			else
				ctx.arch.appendInstruction(catchListStart); //dummy-insert, ease conditions above
			ctx.arch.appendInstruction(finallyStart);
			breakDest = afterAll;
			if (finallyBlock != null)
			{
				ctx.arch.insertFlowHint(Architecture.F_CAFISTART, id);
				if ((regRes = buildGlobalAddress(ctx)) == 0)
					return;
				ctx.arch.genThrowFrameReset(regRes, excFrameOffset);
				ctx.arch.deallocRestoreReg(regRes, 0, 0);
				finallyBlock.genOutput(ctx);
				if (ctx.arch.prepareFreeReg(0, 0, 0, StdTypes.T_PTR) != 0)
				{
					compErr(ctx, REGNOTFREE);
					return;
				}
				regExc = ctx.arch.allocReg();
				ctx.arch.genLoadVarVal(regExc, ctx.arch.regBase, null, excFrameOffset + ctx.arch.throwFrameExcOff, StdTypes.T_PTR);
				condHnd = ctx.arch.genCompPtrToNull(regExc, Ops.C_EQ);
				ctx.arch.insertKillHint(regExc);
				ctx.arch.genCondJmp(afterAll, condHnd);
				StThrow.genThrow(regExc, runtimeClass, ctx);
				ctx.arch.deallocRestoreReg(regExc, 0, 0);
			}
			ctx.arch.appendInstruction(afterAll);
			ctx.arch.insertFlowHint(Architecture.F_BLOCKEND, id);
		}
		breakDest = null;
	}
	
	protected void innerGenOutputCleanup(Context ctx)
	{
		int restore;
		if (finallyBlock != null)
		{
			restore = ctx.arch.ensureFreeRegs(0, 0, 0, 0);
			finallyBlock.genOutput(ctx);
			ctx.arch.deallocRestoreReg(0, 0, restore);
		}
	}
	
	private int buildGlobalAddress(Context ctx)
	{
		int regRes, regUnit;
		if (ctx.arch.prepareFreeReg(0, 0, 0, StdTypes.T_PTR) != 0)
		{
			compErr(ctx, REGNOTFREE);
			return 0;
		}
		regRes = ctx.arch.allocReg();
		if (ctx.dynaMem)
		{
			if (ctx.arch.prepareFreeReg(0, 0, regRes, StdTypes.T_PTR) != 0)
			{
				compErr(ctx, REGNOTFREE);
				return 0;
			}
			regUnit = ctx.arch.allocReg();
			ctx.arch.genLoadUnitContext(regUnit, runtimeClass.relOff);
			ctx.arch.genLoadVarAddr(regRes, regUnit, null, ctx.rteThrowFrame);
			ctx.arch.deallocRestoreReg(regUnit, regRes, 0);
		}
		else
			ctx.arch.genLoadVarAddr(regRes, 0, !ctx.embedded ? ctx.rteDynamicRuntime.outputLocation : ctx.ramLoc, ctx.rteThrowFrame);
		return regRes;
	}
}
