/* Copyright (C) 2016 Stefan Frenz
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
 * AsmOutVCpu2: symbol information writer for vCpu2 assembler output
 *
 * @author S. Frenz
 * @version 161222 added p1/p2 push before jump to main
 * version 161129 added codeStart check
 * version 161118 fixed segment keywords
 * version 161117 inserted code/data segment statements
 * version 160909 replaced header by include statement
 * version 160818 added jump to start method
 * version 160526 added assembler header for vCpu2
 * version 160328 added calculation of scalar method code size, now extending AsmOut for code sharing reasons
 * version 160324 initial version
 */

public class AsmOutVCPU2 extends AsmOut
{
	private boolean inCodeSeg = false;
	
	public AsmOutVCPU2(String filename, Context ictx)
	{
		super(filename, ictx);
	}
	
	protected void printHeader(MemoryImage mem)
	{
		if (ctx.codeStart != 0)
		{
			out.println("#error \"invalid codeStart (JMthd must be empty and extend STRUCT)\"");
			ctx.out.println("invalid codeStart (JMthd must be empty and extend STRUCT)");
			ctx.err = true;
			return;
		}
		out.println("#include <vcpu/compiler/sjc.hsm>");
		out.println("  push r13,r10");
		out.print("  jump ");
		ctx.printUniqueMethodName(out, ctx.startMthd);
		out.println();
		out.println();
		out.println("data_segment");
	}
	
	protected void setMthdFlag(boolean nextIsMthd)
	{
		if (nextIsMthd)
		{
			if (!inCodeSeg)
			{
				out.println("code_segment");
				inCodeSeg = true;
			}
		}
		else if (inCodeSeg)
		{
			out.println("data_segment");
			inCodeSeg = false;
		}
	}
	
	protected void printMthdHead(MemoryObjectDebugInfo now, Mthd mthd)
	{
		printRelocLabel(now.pointer);
		out.println(":");
	}
	
	protected void printMthdTail(MemoryObjectDebugInfo now, Mthd mthd)
	{
		//nothing to do
	}
}
