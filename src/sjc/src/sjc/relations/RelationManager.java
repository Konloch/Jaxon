/* Copyright (C) 2009, 2010, 2011 Patrick Schmidt, Stefan Frenz
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

package sjc.relations;

import sjc.compbase.AccVar;
import sjc.compbase.Mthd;
import sjc.compbase.Unit;
import sjc.compbase.UnitList;

/**
 * RelationManager: keeps track of relations during compilation
 *
 * @author P. Schmidt, S. Frenz
 * @version 110413 removed not needed method isStructuralDependance
 * version 100826 redesign
 * version 091022 cleanup of interface, added filtering to add-methods
 * version 091009 initial version
 */

public class RelationManager
{
	
	public static final String ROOT_OBJECT = "Object";
	
	public UnitList lastUnits;
	
	public void addImport(Unit from, Unit to)
	{
		if (from == null || to == null || from.name.equals(ROOT_OBJECT))
			return;
		updateUnitList(from);
		addRelation(from, null, null, to, null, RelationElement.DEP_IMPORT);
	}
	
	public void addRelation(Unit from, Unit to)
	{
		if (from.name.equals(ROOT_OBJECT))
			return;
		updateUnitList(from);
		addRelation(from, null, null, to, null, RelationElement.DEP_STRUCTURAL);
	}
	
	public void addRelation(Unit usedUnit, Mthd usedMthd, AccVar usedVar, Unit usingUnit, Mthd usingMthd)
	{
		if (usedUnit == usingUnit || usedUnit.outerUnit == usingUnit)
			return;
		updateUnitList(usedUnit);
		addRelation(usedUnit, usedMthd, usedVar, usingUnit, usingMthd, RelationElement.DEP_STD);
	}
	
	private void updateUnitList(Unit unit)
	{
		UnitList temp;
		if (lastUnits == null)
			lastUnits = new UnitList(unit);
		else
		{
			temp = lastUnits;
			while (temp != null)
			{
				if (temp.unit == unit)
					break;
				temp = temp.next;
			}
			if (temp == null)
			{
				temp = new UnitList(unit);
				temp.next = lastUnits;
				lastUnits = temp;
			}
		}
	}
	
	private void addRelation(Unit usedUnit, Mthd usedMthd, AccVar usedVar, Unit usingUnit, Mthd usingMthd, int type)
	{
		RelationElement outer, inner;
		AccVar varToSet = usedVar != null && usedVar.name != null ? usedVar : null;
		Mthd mthdToSet = usingMthd != null && usingMthd.name != null ? usingMthd : null;
		//first entry for this unit
		if ((outer = usedUnit.myRelations) == null)
		{
			outer = new RelationElement();
			outer.unit = usedUnit;
			outer.mthd = usedMthd;
			outer.var = varToSet;
			outer.type = type;
			inner = new RelationElement();
			outer.referencedBy = inner;
			inner.unit = usingUnit;
			inner.mthd = mthdToSet;
			inner.type = type;
			usedUnit.myRelations = outer;
			return;
		}
		//there exists already an entry for this unit
		while (outer != null)
		{
			if (outer.mthd == usedMthd && outer.var == usedVar && outer.type == type)
			{
				inner = outer.referencedBy;
				while (inner != null)
				{
					if (inner.unit == usingUnit && inner.mthd == usingMthd)
						return;
					inner = inner.next;
				}
				break;
			}
			outer = outer.next;
		}
		if (outer == null)
		{
			outer = new RelationElement();
			outer.unit = usedUnit;//.name;
			outer.mthd = usedMthd;
			outer.var = varToSet;
			outer.next = usedUnit.myRelations;
			usedUnit.myRelations = outer;
			inner = outer.referencedBy = new RelationElement();
		}
		else
		{
			inner = new RelationElement();
			inner.next = outer.referencedBy;
			outer.referencedBy = inner;
		}
		inner.unit = usingUnit;//.name;
		inner.mthd = mthdToSet;
		outer.type = inner.type = type;
	}
}
