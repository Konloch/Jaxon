/* Copyright (C) 2005, 2006, 2007, 2008, 2009 Stefan Frenz
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

package sjc.compbase;

import sjc.osio.TextPrinter;

/**
 * QualID: qualified identifier (list of strings)
 *
 * @author S. Frenz
 * @version 090207 added copyright notice
 * version 080707 adopted changed signature of Unit.searchUnitInView
 * version 080603 got resolveAsUnit from TypeRef
 * version 070714 printing resolved QID if possible
 * version 060607 initial version
 */

public class QualID extends Token
{
	public final static int Q_PACKAGE = 1;
	public final static int Q_IMPORTPACK = 2;
	public final static int Q_IMPORTUNIT = 3;
	public final static int Q_UNIT = 4;
	
	public StringList name;
	public int type;
	public Pack packDest; //used for PACKAGE and IMPORTPACK
	public Unit unitDest; //used for UNIT and IMPORTUNIT
	
	public QualID(StringList is, int it, int fid, int il, int ic)
	{
		super(fid, il, ic);
		name = is;
		type = it;
	}
	
	public String getLastPID()
	{
		StringList check;
		
		if (name == null)
			return null;
		check = name;
		while (check.next != null)
			check = check.next;
		return check.str;
	}
	
	public void printFullQID(TextPrinter v)
	{
		StringList s;
		
		if (packDest != null)
			packDest.printFullName(v);
		else if (unitDest != null)
		{
			if (unitDest.pack != null)
			{
				unitDest.pack.printFullQID(v);
				v.print('.');
			}
			v.print(unitDest.name);
		}
		else if (name != null)
		{
			v.print((s = name).str);
			while (s.next != null)
			{
				v.print('.');
				s = s.next;
				v.print(s.str);
			}
			if (type == Q_IMPORTPACK)
				v.print(".*");
		}
		else
			v.print("root");
	}
	
	public boolean resolveAsUnit(Unit inUnit, Context ctx)
	{
		if ((unitDest = inUnit.searchUnitInView(name, false)) == null && (unitDest = ctx.defUnits.searchUnit(name)) == null && (unitDest = ctx.root.searchUnit(name)) == null)
		{
			printPos(ctx, "could not resolve type ");
			printFullQID(ctx.out);
			return false;
		}
		return true;
	}
}
