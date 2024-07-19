/* Copyright (C) 2008, 2009, 2010, 2012, 2013 Stefan Frenz
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
import sjc.osio.TextBuffer;
import sjc.osio.TextPrinter;

/**
 * MthdInfo: symbol information writer containing method information
 *
 * @author S. Frenz
 * @version 130329 fixed codeStart
 * version 120227 cleaned up "package sjc." typo
 * version 100928 fixed access to Context.mem
 * version 100331 adopted changed DebugWriter
 * version 100115 adopted codeStart-movement
 * version 091021 adopted changed modifier declarations
 * version 091005 removed unneeded methods
 * version 091001 adopted changed memory interface
 * version 090303 adopted changed osio package structure
 * version 090207 added copyright notice
 * version 080820 added fallback if creation of file printer fails
 * version 080718 added plain-name
 * version 080717 initial version
 */

public class MthdInfo extends DebugWriter
{
	protected TextPrinter finalOut;
	protected TextBuffer myBuffer;
	protected MthdList firstMthd, lastMthd;
	private String currentUnit;
	private boolean unitIsIndir;
	private final Context ctx;
	
	protected static class MthdList
	{
		protected String unit, nameWithPar, namePlain;
		protected int addr, codeStart, codeSize;
		protected MthdList nextMthd;
	}
	
	public MthdInfo(String filename, Context ic)
	{
		if ((finalOut = ic.osio.getNewFilePrinter(filename)) == null)
		{
			ic.out.println("current system does not support writing to text files");
			finalOut = ic.out;
		}
		ctx = ic;
		myBuffer = new TextBuffer();
	}
	
	public void startImageInfo(boolean isDecompressor)
	{
	}
	
	public void finalizeImageInfo()
	{
		MthdList mthd = firstMthd;
		
		finalOut.println(";addr(hex):codeStart(hex):codeSize(dec):name");
		while (mthd != null)
		{
			finalOut.printHexFix(mthd.addr, 8);
			finalOut.print(':');
			finalOut.printHexFix(mthd.codeStart, 8);
			finalOut.print(':');
			finalOut.print(mthd.codeSize);
			finalOut.print(':');
			finalOut.print(mthd.unit);
			finalOut.print('_');
			finalOut.println(mthd.nameWithPar);
			mthd = mthd.nextMthd;
		}
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
		if ((unit.modifier & Modifier.M_INDIR) != 0)
			unitIsIndir = true;
		else
		{
			unitIsIndir = false;
			myBuffer.reset();
			if (unit.pack != null)
			{
				unit.pack.printFullQID(myBuffer);
				myBuffer.print('.');
			}
			unit.printNameWithOuter(myBuffer);
			myBuffer.replace('.', '_');
			currentUnit = myBuffer.toString();
		}
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
		MthdList search;
		
		if (!unitIsIndir && mthd.outputLocation != null)
		{
			//create new method information object
			MthdList now = new MthdList();
			now.unit = currentUnit;
			myBuffer.reset();
			mthd.printNamePar(myBuffer);
			//replace array dimension
			myBuffer.replace('[', '_');
			myBuffer.replace(']', '_');
			//replace brackets
			myBuffer.replace('(', '$');
			myBuffer.replace(')', '$');
			//replace separator
			myBuffer.replace(',', '$');
			//replace dereferencator
			myBuffer.replace('.', '_');
			now.nameWithPar = myBuffer.toString();
			now.namePlain = mthd.name;
			now.codeStart = (now.addr = ctx.mem.getAddrAsInt(mthd.outputLocation, 0)) + ctx.codeStart;
			now.codeSize = mthd.codeSize; //equals 0 if method is redirected
			//insert newly created object in sorted list
			if (lastMthd == null)
				firstMthd = lastMthd = now; //no object so far
			else
			{
				if (now.addr > lastMthd.addr)
				{ //append (normal case)
					lastMthd.nextMthd = now;
					lastMthd = now;
				}
				else if (lastMthd == firstMthd || now.addr < firstMthd.addr)
				{ //insert at start of list
					now.nextMthd = firstMthd;
					firstMthd = now;
				}
				else
				{ //search for correct place
					search = firstMthd;
					while (now.addr > search.nextMthd.addr)
						search = search.nextMthd; //should never result in a null-pointer
					now.nextMthd = search.nextMthd;
					search.nextMthd = now;
				}
			}
		}
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
	}
}
