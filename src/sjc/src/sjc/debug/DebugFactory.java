/* Copyright (C) 2008, 2009, 2011, 2012, 2013, 2015, 2016 Stefan Frenz
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
import sjc.osio.TextPrinter;

/**
 * DebugFactory: creation of debug-writers
 *
 * @author S. Frenz
 * @version 160324 added support for AsmOutVCpu2
 * version 151108 added support for AssemblerDebugWriter
 * version 130328 added Dwarf
 * version 120923 added CodePrinterJava
 * version 110616 added sizeInfo
 * version 091105 added codeInfo
 * version 091020 added RelationInfo
 * version 090207 added copyright notice
 * version 080820 added filename check
 * version 080717 added MthdInfo
 * version 080712 made factory for SymInfo and GccInfo
 * version 080701 initial version
 */

public class DebugFactory
{
	public static void printKnownDebuggers(TextPrinter v)
	{
		v.println(" gcc      - writer for gcc output");
		v.println(" rel      - relation info writer");
		v.println(" dwrf     - dwarf/elf debug info");
		v.println(" mthd     - method info writer");
		v.println(" code     - code info writer");
		v.println(" size     - size info writer");
		v.println(" java     - java source out");
		v.println(" sym      - syminfo writer");
		v.println(" x86asm   - x86 debug lister");
		v.println(" vcpu2asm - vCPU2 debug lister");
	}
	
	public static DebugWriter getDebugWriter(String name, String filename, Context ctx)
	{
		if (filename == null || filename.equals(""))
		{
			ctx.out.println("invalid filename for debug info writer");
			return null;
		}
		if (name == null || name.equals("sym"))
			return new SymInfo(filename, ctx);
		if (name.equals("rel"))
			return new RelationInfo(filename, ctx);
		if (name.equals("dwrf"))
			return new Dwarf(filename, ctx);
		if (name.equals("mthd"))
			return new MthdInfo(filename, ctx);
		if (name.equals("code"))
			return new CodeInfo(filename, ctx);
		if (name.equals("size"))
			return new SizeInfo(filename, ctx);
		if (name.equals("java"))
			return new CodePrinterJava(filename, ctx);
		if (name.equals("gcc"))
			return new GccInfo(filename, ctx);
		return null;
	}
	
	public static DebugLister getDebugLister(String name, String filename, Context ctx)
	{
		if (name == null || filename == null || filename.equals(""))
		{
			ctx.out.println("invalid name or filename for debug info lister");
			return null;
		}
		if (name.equals("x86asm"))
			return new AsmOutX86(filename, ctx);
		if (name.equals("vcpu2asm"))
			return new AsmOutVCPU2(filename, ctx);
		return null;
	}
	
	public static DebugLister getDefaultAsmDebugLister(String archID, String filename, Context ctx)
	{
		if (archID == null || filename == null || filename.equals(""))
		{
			ctx.out.println("invalid archID or filename for default debug info lister");
			return null;
		}
		if (archID.equals("x86"))
			return new AsmOutX86(filename, ctx);
		if (archID.equals("vCPU2"))
			return new AsmOutVCPU2(filename, ctx);
		return null;
	}
}
