/* Copyright (C) 2007, 2008, 2009, 2010 Stefan Frenz
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

package sjc.symbols;

import sjc.compbase.*;
import sjc.osio.TextPrinter;

/**
 * SymbolInformer: interface to generate in-system symbols
 *
 * @author S. Frenz
 * @version 101226 added getName
 * version 100411 beautified warning message (annotation- instead of DEFINE-hint)
 * version 091121 added support for SymbolInformer chaining
 * version 091001 adopted changed memory interface
 * version 090207 added copyright notice
 * version 080701 updated comment
 * version 080614 adopted changed Unit.searchVariable
 * version 080118 added NOPARMREQ and checkVrbl
 * version 080115 changed signature of generateSymbols
 * version 080114 removed checkParameters
 * version 070729 removed prepareUnits
 * version 070711 initial version
 */

public abstract class SymbolInformer
{
	public final static String NOPARMREQ = "   --- no parameter required";
	
	public SymbolInformer nextInformer;
	
	protected Context ctx; //has to be initialized at the latest before use of checkVrbl
	
	public abstract String getName();
	
	public abstract boolean setParameter(String parm, TextPrinter v);
	
	public abstract boolean generateSymbols(UnitList newUnits, Context ctx);
	
	protected int checkVrbl(Unit inUnit, String name)
	{
		Vrbl var;
		int res;
		
		if ((var = inUnit.searchVariable(name, ctx)) == null)
		{
			ctx.out.print("missing variable ");
			ctx.out.print(name);
			ctx.out.print(" in unit ");
			ctx.out.println(inUnit.name);
			return AccVar.INV_RELOFF;
		}
		if ((res = var.relOff) == AccVar.INV_RELOFF)
		{
			ctx.out.print("invalid variable ");
			ctx.out.print(name);
			ctx.out.print(" in unit ");
			ctx.out.println(inUnit.name);
			return AccVar.INV_RELOFF;
		}
		if (var.location == Vrbl.L_INSTIDS)
		{
			ctx.out.print("variable ");
			ctx.out.print(name);
			ctx.out.print(" needs to be in header (use @SJC.Head) in unit ");
			ctx.out.println(inUnit.name);
			return AccVar.INV_RELOFF;
		}
		return res;
	}
}
