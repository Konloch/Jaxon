/* Copyright (C) 2005, 2006, 2007, 2008, 2009, 2010, 2015, 2016 Stefan Frenz
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

package sjc.backend;

import sjc.backend.arm.ARM7;
import sjc.backend.atmel.ATmega;
import sjc.backend.atmel.ATmegaOpti;
import sjc.backend.dennisk.MyCPU;
import sjc.backend.dennisk.MyVCPU2;
import sjc.backend.ssa.SSA;
import sjc.backend.ssaopt.SSAopt;
import sjc.backend.ssaopt.SSAopt2amd;
import sjc.backend.x86.AMD64;
import sjc.backend.x86.IA32;
import sjc.backend.x86.IA32Opti;
import sjc.backend.x86.IA32RM;
import sjc.osio.TextPrinter;

/**
 * ArchFactory: creation of architecture dependend backend
 *
 * @author S. Frenz
 * @version 160324 added MyVCPU2
 * version 150831 added MyCPU
 * version 100923 added SSAopt2amd
 * version 100805 added ARM7
 * version 100622 removed filtering message
 * version 100619 added printParameter for ATmega
 * version 100126 removed ConstFilterArch
 * version 100115 added ConstFilterArch in combination with ia32
 * version 090717 removed SRB
 * version 090207 added copyright notice and removed SSAToNative
 * version 081226 added SRB
 * version 080615 fixed description for atmegaopt
 * version 080210 removed SSAOpti
 * version 080204 added SSAToAMD64, added parameter output of AMD64
 * version 080123 adopted changed SSAToNative structure
 * version 071002 added SSAToIA32
 * version 070815 added parameter output of IA32
 * version 070725 added ATmegaOpti
 * version 061217 removed SSANative and added ATmega
 * version 061203 added SSANative
 * version 060720 added IA32Opti
 * version 060607 initial version
 */

public class ArchFactory
{
	public static void printKnownArchitectures(TextPrinter v)
	{
		v.println(" primary:");
		v.println("  ia32      - 80386 Protected Mode 32 bit (default)");
		IA32.printParameter(v);
		v.println("  ia32opt   - 80386 Protected Mode 32 bit optimized");
		v.println("  ia32rm    - 80386 Real Mode 16 bit");
		v.println("  amd64     - AMD64 Long Mode 64 bit");
		AMD64.printParameter(v);
		v.println("  ssa2amd64 - SSA optimized AMD64 (experimental)");
		v.println("  ssa32     - Pseudo-SSA 32 bit");
		v.println("  ssa64     - Pseudo-SSA 64 bit");
		v.println("  atmega    - Atmel ATmega");
		ATmega.printParameter(v);
		v.println("  atmegaopt - optimized Atmel ATmega");
		v.println("  arm7      - ARM7 (experimental)");
		v.println("  mycpu     - MyCPU (very exp.)");
		v.println("  myvcpu2   - MyVCPU2 (alpha)");
	}
	
	public static Architecture getArchitecture(String name, Architecture currentArchitecture, TextPrinter v)
	{
		if (currentArchitecture != null)
		{
			v.println("unknown filter, trying primary architectures...");
		}
		if (name == null || name.equals("ia32"))
			return new IA32();
		if (name.equals("ia32opt"))
			return new IA32Opti();
		if (name.equals("ia32rm"))
			return new IA32RM();
		if (name.equals("amd64"))
			return new AMD64();
		if (name.equals("ssa2amd64"))
			return new SSAopt(new SSAopt2amd(), 8);
		if (name.equals("ssa32"))
			return new SSA(4);
		if (name.equals("ssa64"))
			return new SSA(8);
		if (name.equals("atmega"))
			return new ATmega();
		if (name.equals("atmegaopt"))
			return new ATmegaOpti();
		if (name.equals("arm7"))
			return new ARM7();
		if (name.equals("mycpu"))
			return new MyCPU();
		if (name.equals("myvcpu2"))
			return new MyVCPU2();
		v.println("unknown architecture");
		return null;
	}
}
