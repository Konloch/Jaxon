/* Copyright (C) 2008, 2009 Stefan Frenz
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
import sjc.compbase.Token;
import sjc.compbase.UnitList;
import sjc.compbase.Vrbl;

/**
 * CatchBlock: helper for catch block in try-catch-statement
 *
 * @author S. Frenz
 * @version 090207 added copyright notice
 * version 080610 added valid-throwable flag
 * version 080603 initial version
 */

public class CatchBlock extends Token
{
	protected CatchBlock nextCatchDecl;
	protected Vrbl catchVar;
	protected Stmt stmts;
	protected Instruction stIns;
	protected UnitList importedClass;
	protected boolean isValid;
	
	protected CatchBlock(int fid, int il, int ic)
	{
		super(fid, il, ic);
	}
}
