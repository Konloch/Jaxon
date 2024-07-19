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
 * StBlock: block of statements
 *
 * @author S. Frenz
 * @version 121020 added support for "report block end" option in printBreakableStatement
 * version 121017 added code printer support for labels
 * version 120923 added support for code printer
 * version 100504 adopted changed StBreakable, got this-/super-call checks from JMthd
 * version 091111 added support for instance final-var-init
 * version 091109 added support for abbreviation
 * version 091103 overwrote genOutput to avoid source hinting for blocks
 * version 091102 adopted changed Stmt
 * version 090724 adopted changed Expr
 * version 090718 added break on first erroneous statement, added support for unwrittenVars
 * version 090616 adopted changed ExCall
 * version 090508 adopted changes in Stmt
 * version 090507 added flow analysis
 * version 090505 added basic flow analysis
 * version 090207 added copyright notice
 * version 080610 removed conversion to JMthd as it is no longer required
 * version 070913 added support for spread local variable declaration
 * version 070909 optimized signature of Expr.resolve, made StBlock breakable
 * version 070908 optimized signature of Stmt.resolve
 * version 070905 added support for this(.)-calls
 * version 070903 adopted changes in AccVar.checkNameAgainst*, now also checking locals against parameter
 * version 070812 added support for double
 * version 070701 added hint for architecture when local variables pushed
 * version 070628 adopted changes for "Expr extends TypeRef"
 * version 070527 added possibility to save instance context for inline array constructors
 * version 070127 moved complex initialization of variables to Vrbl.genInitCode
 * version 070114 reduced access level where possible
 * version 070111 adapted change in printPos and compErr
 * version 061203 optimized calls to printPos and compErr
 * version 061128 added comments
 * version 061027 optimized check for special names
 * version 060621 inserted missing type-test for initialized variables
 * version 060607 initial version
 */

public class StBlock extends StBreakable
{
	protected Stmt stmts;
	
	protected StBlock(StBreakable io, StringList ila, int fid, int il, int ic)
	{
		super(io, ila, fid, il, ic);
	}
	
	public void printBreakableStatement(CodePrinter prnt)
	{
		Stmt stmt = stmts;
		boolean reportEnd = prnt.stmtBlockStart();
		while (stmt != null)
		{
			stmt.printToken(prnt);
			stmt = stmt.nextStmt;
		}
		if (reportEnd)
			prnt.stmtBlockEnd();
	}
	
	protected int innerResolve(int flowEntryCode, Unit unitContext, Mthd mthdContext, Context ctx)
	{
		return innerResolve(flowEntryCode, false, unitContext, mthdContext, ctx); //call is not from mthd
	}
	
	protected int innerResolve(int flowCode, boolean isOutestInConstr, Unit unitContext, Mthd mthdContext, Context ctx)
	{
		Stmt stmt;
		ExCall call;
		Vrbl oldVars;
		VrblStateList sure = null;
		StExpr news;
		
		//remember block variable state
		oldVars = mthdContext.vars;
		//check constructor specials
		if (isOutestInConstr)
		{ //in constructors, first statement has to be call to super
			if (stmts == null || !stmts.isSuperThisCallStmt(ctx))
			{ //no statement or not super/this-statement
				//insert call to super
				news = new StExpr(fileID, line, col);
				call = new ExCall(fileID, line, col);
				news.ex = call;
				call.id = SJava.KEY_SUPER;
				//ok, insert the call into the stmts-list
				news.nextStmt = stmts;
				stmts = news;
			}
			if (!stmts.resolveSuperThisCallStmt(unitContext, mthdContext, ctx))
				return FA_ERROR;
			//super- or this-call is resolved, start normal resolving at second statement
			stmt = stmts.nextStmt;
		}
		else
			stmt = stmts;
		//check statements
		while (stmt != null)
		{
			if ((flowCode = stmt.resolve(flowCode, unitContext, mthdContext, ctx)) == FA_ERROR)
				return flowCode; //break on error
			if ((flowAnalysisBuffer & FA_HAS_SHORTCUT) != 0)
			{
				flowAnalysisBuffer &= ~FA_HAS_SHORTCUT;
				if (sure == null)
					sure = ctx.copyVrblListState(mthdContext.vars, mthdContext.checkInitVars);
			}
			stmt = stmt.nextStmt;
		}
		//reset abbreviated variable-initializations
		if (sure != null)
		{
			ctx.setVrblStatePotential(mthdContext.vars, mthdContext.checkInitVars, sure);
			ctx.recycleVrblStatelist(sure);
		}
		//check if all variables are set if constructor
		if (isOutestInConstr && mthdContext.checkInitVars != null && (flowCode & FA_NEXT_IS_UNREACHABLE) == 0 && !mthdContext.checkVarWriteState(mthdContext, ctx))
			return FA_ERROR;
		//update flow code
		if ((flowAnalysisBuffer & FA_HAS_ENDBLOCK) == 0 && (flowCode & FA_NEXT_IS_UNREACHABLE) != 0)
			flowCode |= FA_NEXT_IS_UNREACHABLE;
		//everything done, restore block variable state
		mthdContext.vars = oldVars;
		return flowCode;
	}
	
	protected boolean isBreakContDest(boolean named, boolean contNotBreak)
	{
		return !contNotBreak && named; //only breakable (not continuable) and only if named
	}
	
	protected void genOutput(Context ctx)
	{
		genOutput(ctx, false);
	}
	
	protected void innerGenOutput(Context ctx)
	{
		genOutput(ctx, false);
	}
	
	protected void genOutput(Context ctx, boolean saveInstContext)
	{
		Stmt stmt;
		
		//save instance context if required
		if (saveInstContext)
			ctx.arch.genSaveInstContext();
		//generate output for statements
		breakDest = ctx.arch.getUnlinkedInstruction();
		stmt = stmts;
		while (stmt != null)
		{
			stmt.genOutput(ctx);
			stmt = stmt.nextStmt;
		}
		ctx.arch.appendInstruction(breakDest);
		breakDest = null;
	}
}
