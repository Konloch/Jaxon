/* Copyright (C) 2015, 2016 Stefan Frenz
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

import sjc.compbase.Context;
import sjc.compbase.Mthd;
import sjc.memory.MemoryImage;
import sjc.memory.MemoryObjectDebugInfo;

/**
 * AsmOutX86: symbol information writer for x86 assembler output
 *
 * @author S. Frenz
 * @version 161117 adopted changed AsmOut
 * version 160328 added calculation of scalar method code size, now extending AsmOut for code sharing reasons
 * version 160201 fixed scalar size for objects of unknown type, now explicitly extending unicode strings
 * version 160109 assemble-ready reloc/scalar/string distinctive output
 * version 160102 added support for structured unit output
 * version 151108 initial version
 */

public class AsmOutX86 extends AsmOut
{
	public AsmOutX86(String filename, Context ictx)
	{
		super(filename, ictx);
	}
	
	protected void printHeader(MemoryImage mem)
	{
		out.print("BITS ");
		out.println(ctx.arch.relocBytes * 8);
		out.print("ORG 0x");
		out.printHexFix(mem.getBaseAddress(), 8);
		out.println();
		out.println();
	}
	
	protected void setMthdFlag(boolean nextIsMthd)
	{
		//nothing to do
	}
	
	protected void printMthdHead(MemoryObjectDebugInfo now, Mthd mthd)
	{
		//nothing to do
	}
	
	protected void printMthdTail(MemoryObjectDebugInfo now, Mthd mthd)
	{
		out.print("    TIMES ");
		out.print(now.scalarSize);
		out.print("-$+");
		printRelocLabel(now.pointer);
		out.println(" db 0");
		printRelocLabel(now.pointer);
		out.println("end:");
	}
}
