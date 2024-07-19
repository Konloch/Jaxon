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

package sjc.frontend.prepobj;

import sjc.compbase.Context;
import sjc.compbase.UnitDummy;
import sjc.debug.DebugWriter;

/**
 * PObjUnit: placeholder for a single class containing references to compile-time prepared objects
 *
 * @author S. Frenz
 * @version 121020 added support for getSourceType
 * version 091215 adopted changed Unit
 * version 091209 initial version
 */

public class PObjUnit extends UnitDummy
{
	public String getSourceType()
	{
		return "pob";
	}
	
	public void writeDebug(Context ctx, DebugWriter dbw)
	{
		dbw.startUnit("unit for compile-time prepared objects", this);
		dbw.hasUnitOutputLocation(outputLocation);
		writeVarsAndConstObjDebug(ctx, dbw);
		dbw.endUnit();
	}
}
