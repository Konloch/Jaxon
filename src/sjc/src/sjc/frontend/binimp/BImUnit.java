/* Copyright (C) 2005, 2006, 2007, 2008, 2009, 2012 Stefan Frenz
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

package sjc.frontend.binimp;

import sjc.compbase.Context;
import sjc.compbase.UnitDummy;
import sjc.debug.DebugWriter;

/**
 * BImUnit: placeholder for a single class containing references to imported binary data
 *
 * @author S. Frenz
 * @version 121020 added support for getSourceType
 * version 091215 adopted changed Unit
 * version 091209 adopted changed UnitDummy and ExConstInitObj
 * version 091005 reimplemented writeDebug
 * version 091001 adopted changed memory interface
 * version 090207 added copyright notice
 * version 070927 added class descriptor address to debug out
 * version 070918 cleaned up debug out
 * version 070802 fixed not overwriting resolveInterface (handled by UnitDummy)
 * version 070801 made to child of UnitDummy
 * version 070731 adopted renaming of id to name
 * version 070727 adopted changed type of id from PureID to String
 * version 070714 checking genAllUnitDesc
 * version 070711 added final flag in modifier
 * version 070705 fixed address calculation in embConstRAM mode
 * version 070704 added support for embConstRAM
 * version 070628 adopted changes for "Expr extends TypeRef"
 * version 070615 adopted removal of Architecture.getRef
 * version 070511 added genConstObj, class descriptor creation only in dynaMem mode
 * version 070504 added genDescriptor
 * version 070331 added support for additional indirect scalars in SArray
 * version 070303 added support for movable indirect scalars
 * version 070114 reduced access level where possible
 * version 070111 adapted change in printPos and compErr
 * version 061203 optimized calls to printPos and compErr
 * version 061202 optimized static modes
 * version 061128 added support for embedded mode
 * version 061031 removed method indirectCalls which is no longer needed
 * version 060607 initial version
 */

public class BImUnit extends UnitDummy
{
	public String getSourceType()
	{
		return "bim";
	}
	
	public void writeDebug(Context ctx, DebugWriter dbw)
	{
		dbw.startUnit("unit for binary imported data", this);
		dbw.hasUnitOutputLocation(outputLocation);
		writeVarsAndConstObjDebug(ctx, dbw);
		dbw.endUnit();
	}
}
