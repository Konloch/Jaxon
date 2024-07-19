/* Copyright (C) 2006, 2007, 2008, 2009, 2010, 2012 Stefan Frenz
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
import sjc.compbase.StringList;
import sjc.debug.CodePrinter;

/**
 * StBreakable: base type of construct that may be broken or continued
 *
 * @author S. Frenz
 * @version 121017 added code printer support for labels
 * version 100504 added genOutputCleanup and isBreakContDest
 * version 090506 added flowAnalysisBuffer
 * version 090207 added copyright notice
 * version 070909 added support for labels
 * version 070114 reduced access level where possible
 * version 061229 initial version
 */

public abstract class StBreakable extends Stmt
{
	protected Instruction breakDest, contDest;
	protected StBreakable outer;
	protected StringList labels;
	protected int flowAnalysisBuffer;
	
	protected StBreakable(StBreakable io, StringList ila, int fid, int il, int ic)
	{
		super(fid, il, ic);
		outer = io;
		labels = ila;
	}
	
	protected abstract void printBreakableStatement(CodePrinter prnt);
	
	public void printToken(CodePrinter prnt)
	{
		StringList l = labels;
		while (l != null)
		{
			prnt.blockLabel(l.str);
			l = l.next;
		}
		printBreakableStatement(prnt);
	}
	
	protected abstract boolean isBreakContDest(boolean named, boolean contNotBreak);
	
	protected void innerGenOutputCleanup(Context ctx)
	{
		//default: nothing to clean up
	}
	
	protected void genOutputCleanup(StBreakable stopBefore, Context ctx)
	{
		if (stopBefore == this)
			return;
		innerGenOutputCleanup(ctx);
		if (outer != null)
			outer.genOutputCleanup(stopBefore, ctx);
	}
}
