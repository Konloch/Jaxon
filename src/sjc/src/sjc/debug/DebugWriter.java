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

package sjc.debug;

import sjc.compbase.*;

/**
 * DebugWriter: interface for debug-writers
 *
 * @author S. Frenz
 * @version 100331 added inFlash parameter for objects
 * version 091001 adopted changed memory interface
 * version 090207 added copyright notice
 * version 080712 enabled chaining
 * version 080706 full support for all kinds of information
 * version 080701 initial version
 */

public abstract class DebugWriter
{
	public DebugWriter nextWriter; //chaining of DebugWriters
	
	//start/end image debug information
	public abstract void startImageInfo(boolean isDecompressor);
	
	public abstract void finalizeImageInfo();
	
	//global information
	public abstract void globalMemoryInfo(int baseAddress, int memBlockLen);
	
	public abstract void globalMethodInfo(int mthdCodeSize, int mthdCount);
	
	public abstract void globalStringInfo(int stringCount, int stringChars, int stringMemBytes);
	
	public abstract void globalRAMInfo(Object ramInitLoc, int ramSize, int constMemorySize);
	
	public abstract void globalSymbolInfo(int symGenSize);
	
	//unit handling
	public abstract void startUnit(String unitType, Unit unit);
	
	public abstract void markUnitAsNotUsed();
	
	public abstract void hasUnitOutputLocation(Object outputLocation);
	
	public abstract void hasUnitFields(int clssRelocTableEntries, int clssScalarTableSize, int statRelocTableEntries, int statScalarTableSize, int instRelocTableEntries, int instScalarTableSize, int instIndirScalarTableSize);
	
	public abstract void startVariableList();
	
	public abstract void hasVariable(Vrbl var);
	
	public abstract void endVariableList();
	
	public abstract void startMethodList();
	
	public abstract void hasMethod(Mthd mthd, boolean indir);
	
	public abstract void endMethodList();
	
	public abstract void startStatObjList();
	
	public abstract void hasStatObj(int rela, Object loc, String value, boolean inFlash);
	
	public abstract void endStatObjList();
	
	public abstract void startImportedUnitList();
	
	public abstract void hasImportedUnit(UnitList unit);
	
	public abstract void endImportedUnitList();
	
	public abstract void startInterfaceMapList();
	
	public abstract void hasInterfaceMap(IndirUnitMapList intf);
	
	public abstract void endInterfaceMapList();
	
	public abstract void endUnit();
}
