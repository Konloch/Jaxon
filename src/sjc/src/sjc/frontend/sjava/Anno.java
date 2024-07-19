/* Copyright (C) 2008, 2009, 2010 Stefan Frenz
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
import sjc.debug.DebugWriter;

/**
 * Anno: placeholder for annotation declarations
 *
 * @author S. Frenz
 * @version 100312 removed not needed methods
 * version 091001 adopted changed memory interface
 * version 090207 added copyright notice
 * version 080702 adopted changed symInfo-debug-interface
 * version 080118 initial version
 */

public class Anno extends JUnit
{
	protected Anno(QualID ip, QualIDList ii, int im, int fid, int il, int ic)
	{
		super(fid, il, ic);
		pack = ip;
		impt = ii;
		modifier = im;
	}
	
	protected boolean checkDeclarations(Context ctx)
	{
		return true;
	}
	
	protected boolean checkInstVarInit(Vrbl var, Context ctx)
	{
		return true;
	}
	
	protected boolean resolveIntfExtsIpls(Context ctx)
	{
		return true;
	}
	
	protected boolean resolveMthdExtsIpls(Context ctx)
	{
		return true;
	}
	
	public boolean assignOffsets(boolean doClssOff, Context ctx)
	{
		return true;
	}
	
	public IndirUnitMapList enterInheritableReferences(Object objLoc, IndirUnitMapList lastIntf, Context ctx)
	{
		return null;
	}
	
	public boolean genDescriptor(Context ctx)
	{
		return true;
	}
	
	public boolean genOutput(Context ctx)
	{
		return true;
	}
	
	public UnitList getRefUnit(Unit refUnit, boolean insert)
	{
		return null;
	}
	
	public void writeDebug(Context ctx, DebugWriter v)
	{
	}
}
