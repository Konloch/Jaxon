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

import sjc.backend.Instruction;
import sjc.compbase.Context;
import sjc.compbase.Mthd;
import sjc.compbase.StringList;
import sjc.compbase.Unit;
import sjc.debug.CodePrinter;

/**
 * StEndLoop: statement to end a loop (break/continue)
 *
 * @author S. Frenz
 * @version 120923 added support for code printer
 * version 100512 fixed typo
 * version 100504 adopted changed StBreakable
 * version 091109 added support for FA_HAS_SHORTCUT
 * version 091102 adopted changed Stmt
 * version 090508 adopted changes in Stmt
 * version 090507 added flow analysis information
 * version 090207 added copyright notice
 * version 070911 fixed unnamed continue in switch-statements
 * version 070909 added support for labels
 * version 070908 optimized signature of Stmt.resolve
 * version 070114 reduced access level where possible
 * version 070111 adapted change in printPos and compErr
 * version 061231 added check for not null dest
 * version 061229 removed access to firstInstr
 * version 061203 optimized calls to printPos and compErr
 * version 060607 initial version
 */

public class StEndLoop extends Stmt
{
	protected boolean contNotBreak;
	protected String labelToEnd;
	private final StBreakable outer;
	private StBreakable toEnd;
	
	protected StEndLoop(StBreakable io, String it, int fid, int il, int ic)
	{
		super(fid, il, ic);
		outer = io;
		labelToEnd = it;
	}
	
	public void printToken(CodePrinter prnt)
	{
		prnt.stmtEndLoop(contNotBreak, labelToEnd);
	}
	
	protected int innerResolve(int flowCode, Unit unitContext, Mthd mthdContext, Context ctx)
	{
		StBreakable o;
		StringList ol;
		boolean found = false;
		
		o = outer;
		if (labelToEnd == null)
		{ //no label given
			while (o != null && !o.isBreakContDest(false, contNotBreak))
				o = o.outer;
			if (o == null)
			{
				compErr(ctx, "break/cont-dest not found");
				return FA_ERROR;
			}
			toEnd = o;
		}
		else
		{ //search label
			LOOP:
			while (o != null)
			{
				ol = o.labels;
				while (ol != null)
				{
					if (labelToEnd.equals(ol.str))
					{
						toEnd = o;
						found = true;
						break LOOP;
					}
					ol = ol.next;
				}
				o = o.outer;
			}
			if (!found)
			{
				printPos(ctx, "label not found");
				return FA_ERROR;
			}
		}
		if (toEnd.isBreakContDest(true, contNotBreak))
		{ //check target context
			toEnd.flowAnalysisBuffer |= contNotBreak ? FA_HAS_CONTINUE : FA_HAS_ENDBLOCK;
			//search for block to be abbreviated
			if (toEnd instanceof StBlock || toEnd instanceof StSwitch)
				toEnd.flowAnalysisBuffer |= FA_HAS_SHORTCUT;
			else
			{ //toEnd is a loop, search next inner block to mark as abbreviated
				o = outer;
				while (o != null && o.outer != toEnd)
					o = o.outer;
				if (o instanceof StBlock)
					o.flowAnalysisBuffer |= FA_HAS_SHORTCUT;
			}
			//everything ok
			return flowCode | FA_NEXT_IS_UNREACHABLE;
		}
		//no valid outer block found
		printPos(ctx, "statement invalid in this context");
		return FA_ERROR;
	}
	
	protected void innerGenOutput(Context ctx)
	{
		Instruction dest;
		
		if ((dest = contNotBreak ? toEnd.contDest : toEnd.breakDest) == null)
		{
			compErr(ctx, "StEndLoop with dest==null");
			return;
		}
		outer.genOutputCleanup(toEnd, ctx);
		ctx.arch.genJmp(dest);
	}
}
