/* Copyright (C) 2009, 2010 Patrick Schmidt, Stefan Frenz
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

package sjc.debug;

import sjc.compbase.*;
import sjc.osio.TextPrinter;
import sjc.relations.RelationElement;

/**
 * RelationInfo: Relation information writer
 *
 * @author P. Schmidt, S. Frenz
 * @version 100826 redesign
 * version 100331 adopted changed DebugWriter
 * version 091022 changed order of output in finalizeImage
 * version 091020 initial version
 */

public class RelationInfo extends DebugWriter
{
	private final Context ctx;
	private final TextPrinter out;
	
	public RelationInfo(String filename, Context ic)
	{
		out = (ctx = ic).osio.getNewFilePrinter(filename);
	}
	
	public void finalizeImageInfo()
	{
		if (ctx.relations == null)
		{
			out.println("no relation information available (see compiler options)");
			return;
		}
		out.println("usedPackage;usedUnit;usedMthd;usedVrbl;usingPackage;usingUnit;usingMthd");
		printRelations(ctx.root);
		out.close();
	}
	
	public void endImportedUnitList()
	{
	}
	
	public void endInterfaceMapList()
	{
	}
	
	public void endMethodList()
	{
	}
	
	public void endStatObjList()
	{
	}
	
	public void endUnit()
	{
	}
	
	public void endVariableList()
	{
	}
	
	public void globalMemoryInfo(int baseAddress, int memBlockLen)
	{
	}
	
	public void globalMethodInfo(int mthdCodeSize, int mthdCount)
	{
	}
	
	public void globalRAMInfo(Object ramInitLoc, int ramSize, int constMemorySize)
	{
	}
	
	public void globalStringInfo(int stringCount, int stringChars, int stringMemBytes)
	{
	}
	
	public void globalSymbolInfo(int symGenSize)
	{
	}
	
	public void hasImportedUnit(UnitList unit)
	{
	}
	
	public void hasInterfaceMap(IndirUnitMapList intf)
	{
	}
	
	public void hasMethod(Mthd mthd, boolean indir)
	{
	}
	
	public void hasStatObj(int rela, Object loc, String value, boolean inFlash)
	{
	}
	
	public void hasUnitFields(int clssRelocTableEntries, int clssScalarTableSize, int statRelocTableEntries, int statScalarTableSize, int instRelocTableEntries, int instScalarTableSize, int instIndirScalarTableSize)
	{
	}
	
	public void hasUnitOutputLocation(Object outputLocation)
	{
	}
	
	public void hasVariable(Vrbl var)
	{
	}
	
	public void markUnitAsNotUsed()
	{
	}
	
	public void startImageInfo(boolean isDecompressor)
	{
	}
	
	public void startImportedUnitList()
	{
	}
	
	public void startInterfaceMapList()
	{
	}
	
	public void startMethodList()
	{
	}
	
	public void startStatObjList()
	{
	}
	
	public void startUnit(String unitType, Unit unit)
	{
	}
	
	public void startVariableList()
	{
	}
	
	private void printRelations(Pack pckg)
	{
		Unit units;
		RelationElement outer, inner;
		if (pckg == null)
			return;
		units = pckg.units;
		while (units != null)
		{
			outer = units.myRelations;
			while (outer != null)
			{
				inner = outer.referencedBy;
				while (inner != null)
				{
					if (outer.unit.pack != null && outer.unit.pack.packDest != null)
						outer.unit.pack.packDest.printFullName(out);
					out.print(';');
					outer.unit.printNameWithOuter(out);
					out.print(';');
					if (outer.mthd != null)
						outer.mthd.printSig(out);
					out.print(';');
					if (outer.var != null)
						out.print(outer.var.name);
					out.print(';');
					if (inner.unit.pack != null)
						inner.unit.pack.packDest.printFullName(out);
					out.print(';');
					inner.unit.printNameWithOuter(out);
					out.print(';');
					if (inner.mthd != null)
						inner.mthd.printSig(out);
					out.println();
					inner = inner.next;
				}
				outer = outer.next;
			}
			units = units.nextUnit;
		}
		printRelations(pckg.subPacks);
		printRelations(pckg.nextPack);
	}
	
}
