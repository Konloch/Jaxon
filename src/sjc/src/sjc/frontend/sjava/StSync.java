/* Copyright (C) 2009, 2010, 2011, 2012 Stefan Frenz
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
 * StSync: synchronized-statement
 *
 * @author S. Frenz
 * @version 121017 added code printer support for labels
 * version 120923 added support for code printer
 * version 110223 added "descriptor needed" if sync depends on class descriptor
 * version 110221 removed buggy cleanup optimization
 * version 101220 added support for noSyncCall
 * version 101210 adopted changed Architecture
 * version 101015 adopted changed Expr
 * version 100916 fixed class context loading in static compilation environment
 * version 100526 added normal cleanup check, fixed register saving on cleanup
 * version 100504 extension of StBreakable to support cleanup, simplified compErr-messages
 * version 100114 adopted changed Architecture
 * version 091102 adopted changed Stmt
 * version 091013 adopted changed method signature of genStore*
 * version 091005 adopted changed Expr
 * version 091001 adopted changed memory interface
 * version 090724 adopted changed Expr
 * version 090718 adopted changed Expr
 * version 090619 adopted changed Architecture
 * version 090616 changed "enter" into "leave" to conform with profiler parameters
 * version 090508 adopted changes in Stmt
 * version 090506 added flow analysis information
 * version 090218 initial version
 */

public class StSync extends StBreakable
{
	public final static int SYNC_NORM = 0; //normal sync block
	public final static int SYNC_INST = 1; //sync on instance
	public final static int SYNC_CLSS = 2; //sync on class context
	
	private final static String ERR_NOFREEREG = "no free reg at synchronized";
	
	protected Expr syncObj;
	protected StBlock syncBlock;
	private UnitList runtimeClass;
	private Unit mthdOwnerClass;
	private final int syncType;
	private int syncObjOffset;
	
	protected StSync(StBreakable io, int is, int fid, int il, int ic)
	{
		super(io, null, fid, il, ic);
		syncType = is;
	}
	
	public void printBreakableStatement(CodePrinter prnt)
	{
		prnt.stmtSync(syncObj, syncBlock);
	}
	
	protected int innerResolve(int flowCode, Unit unitContext, Mthd mthdContext, Context ctx)
	{
		//resolve syncBlock
		if (((flowCode = syncBlock.resolve(flowCode, unitContext, mthdContext, ctx)) & FA_ERROR) != 0)
			return FA_ERROR;
		//check and resolve syncObj
		switch (syncType)
		{
			case SYNC_NORM: //object to synchronize on is explicitly given
				if (!syncObj.resolve(unitContext, mthdContext, getExprFromStmtFlowCode(flowCode) | Expr.RF_CHECKREAD, null, ctx))
					return FA_ERROR;
				if (!syncObj.isObjType())
				{
					printPos(ctx, "need object to synchronize on");
					return FA_ERROR;
				}
				if (syncObj.baseType == StdTypes.T_NNPT)
				{
					printPos(ctx, "null is not a valid object to synchronize on");
					return FA_ERROR;
				}
				//allocate space for buffer variable if synchronization is not done on inst/clss
				mthdContext.varSize = -(syncObjOffset = (-mthdContext.varSize - ctx.arch.relocBytes) & ~ctx.arch.stackClearBits);
				break;
			case SYNC_INST: //implicit dynamic method, sync on instance in instance register, nothing to resolve
				break;
			case SYNC_CLSS: //implicit static method, mark class descriptor as needed and remember it
				(mthdOwnerClass = mthdContext.owner).modifier |= Modifier.MA_ACCSSD;
				break;
			default:
				compErr(ctx, "invalid syncType");
				return FA_ERROR;
		}
		//everything ok
		if (!ctx.noSyncCalls && ctx.dynaMem)
			runtimeClass = unitContext.getRefUnit(ctx.rteDynamicRuntime, true);
		return flowCode;
	}
	
	protected boolean isBreakContDest(boolean named, boolean contNotBreak)
	{
		return false; //never a break/continue-destination
	}
	
	protected void innerGenOutput(Context ctx)
	{
		int regSyncObj = 0;
		if (ctx.noSyncCalls)
			return;
		//evaluate expression and call runtime for "enter"
		switch (syncType)
		{
			case SYNC_NORM:
				if (ctx.arch.prepareFreeReg(0, 0, 0, StdTypes.T_PTR) != 0)
				{ //should be 0 always
					compErr(ctx, ERR_NOFREEREG);
					return;
				}
				regSyncObj = ctx.arch.allocReg();
				syncObj.genOutputVal(regSyncObj, ctx);
				ctx.arch.genStoreVarVal(ctx.arch.regBase, null, syncObjOffset, regSyncObj, StdTypes.T_PTR);
				genOutputDoSyncCall(regSyncObj, 0, ctx);
				ctx.arch.deallocRestoreReg(regSyncObj, 0, 0);
				break;
			case SYNC_INST:
				genOutputDoSyncCall(ctx.arch.regInst, 0, ctx);
				break;
			case SYNC_CLSS:
				if (ctx.dynaMem)
					genOutputDoSyncCall(ctx.arch.regClss, 0, ctx); //unit context existing
				else
				{ //unit context must be loaded
					if (ctx.arch.prepareFreeReg(0, 0, 0, StdTypes.T_PTR) != 0)
					{ //should be 0 always
						compErr(ctx, ERR_NOFREEREG);
						return;
					}
					regSyncObj = ctx.arch.allocReg();
					ctx.arch.genLoadConstUnitContext(regSyncObj, mthdOwnerClass.outputLocation);
					genOutputDoSyncCall(regSyncObj, 0, ctx);
					ctx.arch.deallocRestoreReg(regSyncObj, 0, 0);
				}
				break;
		}
		//generate code for synchronized block
		syncBlock.genOutput(ctx);
		//clean up
		innerGenOutputCleanup(ctx);
	}
	
	protected void innerGenOutputCleanup(Context ctx)
	{
		int regSyncObj, restore;
		if (ctx.noSyncCalls)
			return;
		//get evaluated expression and call runtime for "leave"
		restore = ctx.arch.ensureFreeRegs(0, 0, 0, 0);
		switch (syncType)
		{
			case SYNC_NORM:
				if (ctx.arch.prepareFreeReg(0, 0, 0, StdTypes.T_PTR) != 0)
				{ //should be 0 always
					compErr(ctx, ERR_NOFREEREG);
					return;
				}
				regSyncObj = ctx.arch.allocReg();
				ctx.arch.genLoadVarVal(regSyncObj, ctx.arch.regBase, null, syncObjOffset, StdTypes.T_PTR);
				genOutputDoSyncCall(regSyncObj, 1, ctx);
				ctx.arch.deallocRestoreReg(regSyncObj, 0, 0);
				break;
			case SYNC_INST:
				genOutputDoSyncCall(ctx.arch.regInst, 1, ctx);
				break;
			case SYNC_CLSS:
				if (ctx.dynaMem)
					genOutputDoSyncCall(ctx.arch.regClss, 1, ctx); //unit context existing
				else
				{ //unit context must be loaded
					if (ctx.arch.prepareFreeReg(0, 0, 0, StdTypes.T_PTR) != 0)
					{ //should be 0 always
						compErr(ctx, ERR_NOFREEREG);
						return;
					}
					regSyncObj = ctx.arch.allocReg();
					ctx.arch.genLoadConstUnitContext(regSyncObj, mthdOwnerClass.outputLocation);
					genOutputDoSyncCall(regSyncObj, 1, ctx);
					ctx.arch.deallocRestoreReg(regSyncObj, 0, 0);
				}
				break;
		}
		ctx.arch.deallocRestoreReg(0, 0, restore);
	}
	
	private void genOutputDoSyncCall(int regSyncObj, int leave, Context ctx)
	{
		if (ctx.dynaMem)
			ctx.arch.genSaveUnitContext();
		ctx.arch.genPush(regSyncObj, StdTypes.T_PTR); //push non-constant sync obj
		ctx.arch.genPushConstVal(leave, StdTypes.T_BOOL); //push "leave"
		if (ctx.dynaMem)
		{
			ctx.arch.genLoadUnitContext(ctx.arch.regClss, runtimeClass.relOff);
			ctx.arch.genCall(ctx.rteDoSyncMd.relOff, ctx.arch.regClss, ctx.rteDoSyncMd.parSize);
			ctx.arch.genRestUnitContext();
		}
		else
			ctx.arch.genCallConst(ctx.rteDoSyncMd, ctx.rteDoSyncMd.parSize);
	}
}
