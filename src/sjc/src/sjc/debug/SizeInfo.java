/* Copyright (C) 2011, 2012 Stefan Frenz
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

/**
 * SizeInfo: symbol information writer containing size information
 *
 * @author S. Frenz
 * @version 120326 added closing of printer
 * version 110616 initial version
 */

public class SizeInfo extends DebugWriter
{
	private TextPrinter finalOut;
	private final Context ctx;
	private Unit currentUnit;
	private int currentUnitMthdSize;
	
	public SizeInfo(String filename, Context ic)
	{
		if ((finalOut = ic.osio.getNewFilePrinter(filename)) == null)
		{
			ic.out.println("current system does not support writing to text files");
			finalOut = ic.out;
		}
		ctx = ic;
	}
	
	public void startImageInfo(boolean isDecompressor)
	{
	}
	
	public void finalizeImageInfo()
	{
		finalOut.close();
	}
	
	public void globalMemoryInfo(int baseAddress, int memBlockLen)
	{
	}
	
	public void globalMethodInfo(int mthdCodeSize, int mthdCount)
	{
	}
	
	public void globalStringInfo(int stringCount, int stringChars, int stringMemBytes)
	{
	}
	
	public void globalRAMInfo(Object ramInitLoc, int ramSize, int constMemorySize)
	{
	}
	
	public void globalSymbolInfo(int symGenSize)
	{
	}
	
	public void startUnit(String type, Unit unit)
	{
		currentUnit = unit;
		currentUnitMthdSize = 0;
		if (unit.pack != null)
			unit.pack.printFullQID(finalOut);
		finalOut.print(';');
		unit.printNameWithOuter(finalOut);
		finalOut.print(";$DESC;");
		finalOut.println(unit.clssScalarTableSize + unit.statScalarTableSize + (unit.clssRelocTableEntries + unit.statRelocTableEntries) * ctx.arch.relocBytes);
	}
	
	public void markUnitAsNotUsed()
	{
	}
	
	public void hasUnitOutputLocation(Object outputLocation)
	{
	}
	
	public void hasUnitFields(int clssRelocTableEntries, int clssScalarTableSize, int statRelocTableEntries, int statScalarTableSize, int instRelocTableEntries, int instScalarTableSize, int instIndirScalarTableSize)
	{
	}
	
	public void startVariableList()
	{
	}
	
	public void hasVariable(Vrbl var)
	{
	}
	
	public void endVariableList()
	{
	}
	
	public void startMethodList()
	{
	}
	
	public void hasMethod(Mthd mthd, boolean indir)
	{
		if (mthd.owner != currentUnit)
		{
			mthd.compErr(ctx, "method owner is not equal to containing method in SizeInfo DebugWriter");
			return;
		}
		if (currentUnit.pack != null)
			currentUnit.pack.printFullQID(finalOut);
		finalOut.print(';');
		currentUnit.printNameWithOuter(finalOut);
		finalOut.print(';');
		mthd.printNamePar(finalOut);
		finalOut.print(';');
		finalOut.println(mthd.codeSize);
		currentUnitMthdSize += mthd.codeSize;
	}
	
	public void endMethodList()
	{
	}
	
	public void startStatObjList()
	{
	}
	
	public void hasStatObj(int rela, Object loc, String value, boolean inFlash)
	{
	}
	
	public void endStatObjList()
	{
	}
	
	public void startImportedUnitList()
	{
	}
	
	public void hasImportedUnit(UnitList ul)
	{
	}
	
	public void endImportedUnitList()
	{
	}
	
	public void startInterfaceMapList()
	{
	}
	
	public void hasInterfaceMap(IndirUnitMapList intf)
	{
	}
	
	public void endInterfaceMapList()
	{
	}
	
	public void endUnit()
	{
		if (currentUnit.pack != null)
			currentUnit.pack.printFullQID(finalOut);
		finalOut.print(';');
		currentUnit.printNameWithOuter(finalOut);
		finalOut.print(";$CODE;");
		finalOut.println(currentUnitMthdSize);
		currentUnit = null;
	}
}
