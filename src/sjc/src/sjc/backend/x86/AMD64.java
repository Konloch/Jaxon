/* Copyright (C) 2005, 2006, 2007, 2008, 2009, 2010, 2011 Stefan Frenz
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

package sjc.backend.x86;

import sjc.backend.Instruction;
import sjc.compbase.*;
import sjc.memory.MemoryImage;
import sjc.osio.TextPrinter;

/**
 * AMD64: architecture implementation for 64 bit long mode AMD64/EM64T processors
 *
 * @author S. Frenz
 * @version 110223 removed debug code
 * version 110222 fixed MOVSX* and MOVZX* instructions for 16 bit register
 * version 110219 fixed MOVSX* and MOVZX* instructions for 16 bit register
 * version 110218 fixed ADDmemimm instruction
 * version 110208 added some instructions used in optimizer
 * version 110127 added some instructions used in optimizer
 * version 110120 fixed MOVmemimm for upper regs
 * version 110115 fixed MOVregimmL for upper regs
 * version 101210 adopted changed Architecture
 * version 101125 added support for native callback epilog style
 * version 101124 fixed shift in memory instructions (used in optimier)
 * version 101119 added some encoding of instructions (used in optimizer)
 * version 101107 fixed codeAriRegImm for R8-based operations
 * version 101105 flipped prefix byte order
 * version 101104 added some encoding of instructions (used in optimizer)
 * version 101101 adopted changed Architecture
 * version 101027 fixed sign handling for logical bitshift of byte and short values
 * version 100927 fixed unsignedness of chars
 * version 100924 fixed access to parameter in interrupt method after inline method, fixed codeAriRegImm, fixed code for I_ANDmemimm
 * version 100922 made some private variables protected, added some instruction-types, fixed I_ORmemreg
 * version 100824 fixed coding of MOVSX for new amd64 registers
 * version 100428 fixed coding of NEG and NOT for new amd64 registers
 * version 100115 adopted changed error reporting and codeStart-movement
 * version 091105 fixed invalid stack size adjustment in epilog of inlined methods with vars+pars
 * version 091026 added optimized version of genBinOpConst*
 * version 091013 adopted changed method signature of genStore*
 * version 091005 fixed genStore* after memory interface changes
 * version 091004 removed getSpecialValue
 * version 091001 adopted changed memory interface
 * version 090717 adopted changed Architecture
 * version 090626 added support for stack extreme check
 * version 090619 adopted changed Architecture
 * version 090609 added check for null-pointers in putRef in "image relocated" mode
 * version 090430 added support for native "return missing" hint
 * version 090409 added implementation of newly inserted getSpecialValue
 * version 090408 added support for images starting above 2gb with architecture option "-T rXXX"
 * version 090219 adopted changed X86Base
 * version 090218 clarified genLoadConstDoubleOrLongVal
 * version 090208 removed genClearMem
 * version 090207 added copyright notice
 * version 090206 added support for optimized genStoreVar*l
 * version 081209 added support for method printing
 * version 081021 adopted changes in Architecture
 * version 080607 added support for language throwables
 * version 080316 added support for native calls with parameters in registers
 * version 080313 added support for native calls
 * version 080205 added support for noStackOptimization
 * version 080127 fixed bug in XCHG opcode
 * version 080122 fixed setting of usedRegs in getReg on firstWrite
 * version 080105 added genSavePrimary and genRestPrimary
 * version 071011 fixed invalid optimization of constant long-push
 * version 070917 pre-initialization of stack variables with "0"
 * version 070913 moved curVarOffParam to X86Base, added support for genClearMem
 * version 070910 added support for inling methods with unclear FPU-stack
 * version 070830 bugfix in long division
 * version 070816 added float/double-optimization with FCOMIP
 * version 070815 added support for float/double
 * version 070812 dummy-handling of float and double to enable self-compilation (still without float/double)
 * version 070809 adopted changes for float and double
 * version 070723 changed bound exception interrupt number from 0x21 to 0x05
 * version 070628 added allocClearBits
 * version 070615 removed no longer needed getRef
 * version 070610 optimized access to different jump offset sizes
 * version 070606 moved common methods and variables to X86Base
 * version 070601 optimized genBinOp, externalized Strings to X86Base
 * version 070531 adopted removal of Architecture.genLoadFromMem
 * version 070505 adopted naming of Clss to Unit, changed OutputObject to int
 * version 070501 optimized insPatched
 * version 070422 fixed param-offset in interrupt routines
 * version 070127 optimized information output
 * version 070114 removed never called genGetClassOfResult, reduced access level where possible
 * version 070113 adopted change of genCheckNull to genCompPtrToNull
 * version 070104 optimized register allocation
 * version 070101 fixed fixJump, adopted change in genCall
 * version 061231 removed coding of unused instruction, rolled out coding of ari-instructions
 * version 061229 removed access to firstInstr
 * version 061225 adopted change in codeProlog
 * version 061203 optimized assign/check and calls to printPos and compErr
 * version 061202 adopted change of genCall
 * version 061111 adopted change of Architecture.codeEpilog
 * version 061109 adapted Ops.C_BO
 * version 061107 fixed genLoadDerefAddr for odd values
 * version 061105 optimized genLoadVarAddr if offset==0 and src==dst
 * version 060628 added support for static compilation
 * version 060621 fixed genLoadConstVal
 * version 060616 inserted genCopyInstContext
 * version 060607 initial version
 */

public class AMD64 extends X86Base
{
	//additional general purpose registers, used by tokens and in alloc/dealloc-functions
	protected final static int Reg8 = 0x010;
	protected final static int Reg9 = 0x020;
	protected final static int Reg10 = 0x040;
	protected final static int Reg11 = 0x080;
	protected final static int Reg12 = 0x100;
	protected final static int Reg13 = 0x200;
	protected final static int Reg14 = 0x400;
	protected final static int Reg15 = 0x800; //reg14/15 are for temporary use only and therefore not in RegAll
	
	//additional registers, entries are 0rrr0bbb with
	//  rrr = register code
	//  bbb = bytes affected in the corresponding full register
	//        with 111 -> complete 32 bits, may be combined with REX to indicate 64 bit
	//             011 -> lower 16 bits
	//             010 -> upper 8 bits of lower 16 bits not used and not supported because of REX-regs
	//             001 -> lowest 8 bits
	protected final static int R_8B = 0x101, R_8W = 0x103, R_8D = 0x107, RM_8 = 0x100;
	protected final static int R_9B = 0x111, R_9W = 0x113, R_9D = 0x117, RM_9 = 0x110;
	protected final static int R_10B = 0x121, R_10W = 0x123, R_10D = 0x127, RM_10 = 0x120;
	protected final static int R_11B = 0x131, R_11W = 0x133, R_11D = 0x137, RM_11 = 0x130;
	protected final static int R_12B = 0x141, R_12W = 0x143, R_12D = 0x147, RM_12 = 0x140;
	protected final static int R_13B = 0x151, R_13W = 0x153, R_13D = 0x157, RM_13 = 0x150;
	protected final static int R_14B = 0x161, R_14W = 0x163, R_14D = 0x167, RM_14 = 0x160;
	protected final static int R_15B = 0x171, R_15W = 0x173, R_15D = 0x177, RM_15 = 0x170;
	protected final static int NRG = 0x100; //new registers R8..R15
	protected final static int REX = 0x200; //extend operand size from 32 to 64 bit
	
	//offsets of parameters
	protected final static int VAROFF_PARAM_INL = 8;
	protected final static int VAROFF_PARAM_NRM = 16;
	protected final static int VAROFF_PARAM_INT = 120;
	
	//configuration
	protected boolean noStackOptimization; //initialized to false
	
	//initialization
	public AMD64()
	{
		relocBytes = 8;
		allocClearBits = stackClearBits = 7;
		maxInstrCodeSize = 16;
		rAll = RegA | RegB | RegC | RegD | Reg8 | Reg9 | Reg10 | Reg11 | Reg12 | Reg13;
		rClss = REX | R_EDI;
		rInst = REX | R_ESI;
		rBase = REX | R_EBP;
		fullIPChangeBytes = 4;
		patchedAddPrefix = 0x48;
		mPtr = REX | RS_E;
	}
	
	public boolean setParameter(String parm, TextPrinter v)
	{
		if ("nsop".equals(parm))
		{
			v.println("always building complete stack frame");
			noStackOptimization = true;
		}
		else
		{
			v.println("invalid parameter for AMD64, possible parameters:");
			printParameter(v);
			return false;
		}
		return true;
	}
	
	public static void printParameter(TextPrinter v)
	{
		v.println("   nsop - always build complete stack frame");
	}
	
	public void init(MemoryImage imem, int ilev, Context ictx)
	{
		super.init(imem, ilev, ictx);
	}
	
	//references are treated as normal long values
	public void putRef(Object loc, int offset, Object ptr, int ptrOff)
	{
		mem.putLong(loc, offset, mem.getAddrAsLong(ptr, ptrOff));
	}
	
	public void putCodeRef(Object loc, int offset, Object ptr, int ptrOff)
	{
		long destPtr = mem.getAddrAsLong(ptr, ptrOff) - mem.getAddrAsLong(loc, offset) - 4l;
		long testBits = destPtr & 0xFFFFFFFF80000000l;
		if (testBits != 0l && testBits != 0xFFFFFFFF80000000l)
		{
			fatalError("putCodeRef with upper 33 bits not completely set or cleared");
		}
		else
			mem.putInt(loc, offset, (int) destPtr);
	}
	
	//register allocation and de-allocation
	private int getFreeReg(int avoidRegs, int type)
	{ //allow tmp-Regs, so we always find something
		int ret;
		if ((ret = freeRegSearch((rAll & ~usedRegs & ~avoidRegs) | Reg14 | Reg15, type)) == 0)
		{
			fatalError("internal error in AMD64.getFreeReg: nothing free");
			return 0;
		}
		usedRegs |= ret;
		return ret;
	}
	
	protected int freeRegSearch(int mask, int type)
	{
		int ret, ret2;
		
		if ((ret = bitSearch(mask, 1, 0)) == 0)
			return 0;
		if (type == StdTypes.T_DPTR)
		{
			if ((ret2 = bitSearch(mask & ~ret, 1, 0)) == 0)
				return 0;
			ret |= ret2;
		}
		return ret;
	}
	
	protected int storeReg(int regs)
	{
		int stored = 0;
		regs &= usedRegs & writtenRegs;
		if ((regs & RegA) != 0)
		{
			ins(I_PUSHreg, REX | R_EAX);
			stored |= RegA;
		}
		if ((regs & RegB) != 0)
		{
			ins(I_PUSHreg, REX | R_EBX);
			stored |= RegB;
		}
		if ((regs & RegC) != 0)
		{
			ins(I_PUSHreg, REX | R_ECX);
			stored |= RegC;
		}
		if ((regs & RegD) != 0)
		{
			ins(I_PUSHreg, REX | R_EDX);
			stored |= RegD;
		}
		if ((regs & Reg8) != 0)
		{
			ins(I_PUSHreg, REX | R_8D);
			stored |= Reg8;
		}
		if ((regs & Reg9) != 0)
		{
			ins(I_PUSHreg, REX | R_9D);
			stored |= Reg9;
		}
		if ((regs & Reg10) != 0)
		{
			ins(I_PUSHreg, REX | R_10D);
			stored |= Reg10;
		}
		if ((regs & Reg11) != 0)
		{
			ins(I_PUSHreg, REX | R_11D);
			stored |= Reg11;
		}
		if ((regs & Reg12) != 0)
		{
			ins(I_PUSHreg, REX | R_12D);
			stored |= Reg12;
		}
		if ((regs & Reg13) != 0)
		{
			ins(I_PUSHreg, REX | R_13D);
			stored |= Reg13;
		}
		if ((regs & Reg14) != 0)
		{
			ins(I_PUSHreg, REX | R_14D);
			stored |= Reg14;
		}
		if ((regs & Reg15) != 0)
		{
			ins(I_PUSHreg, REX | R_15D);
			stored |= Reg15;
		}
		return stored;
	}
	
	protected void restoreReg(int regs)
	{
		usedRegs |= regs;
		writtenRegs |= regs;
		if ((regs & Reg15) != 0)
			ins(I_POPreg, REX | R_15D);
		if ((regs & Reg14) != 0)
			ins(I_POPreg, REX | R_14D);
		if ((regs & Reg13) != 0)
			ins(I_POPreg, REX | R_13D);
		if ((regs & Reg12) != 0)
			ins(I_POPreg, REX | R_12D);
		if ((regs & Reg11) != 0)
			ins(I_POPreg, REX | R_11D);
		if ((regs & Reg10) != 0)
			ins(I_POPreg, REX | R_10D);
		if ((regs & Reg9) != 0)
			ins(I_POPreg, REX | R_9D);
		if ((regs & Reg8) != 0)
			ins(I_POPreg, REX | R_8D);
		if ((regs & RegD) != 0)
			ins(I_POPreg, REX | R_EDX);
		if ((regs & RegC) != 0)
			ins(I_POPreg, REX | R_ECX);
		if ((regs & RegB) != 0)
			ins(I_POPreg, REX | R_EBX);
		if ((regs & RegA) != 0)
			ins(I_POPreg, REX | R_EAX);
	}
	
	protected int internalGetReg(int nr, int reg, int type, boolean firstWrite)
	{
		reg = bitSearch(reg, nr, 0);
		if (firstWrite)
		{
			writtenRegs |= reg;
			usedRegs |= reg;
		}
		switch (reg)
		{
			case RegA:
				reg = RM_A;
				break;
			case RegB:
				reg = RM_B;
				break;
			case RegC:
				reg = RM_C;
				break;
			case RegD:
				reg = RM_D;
				break;
			case Reg8:
				reg = RM_8;
				break;
			case Reg9:
				reg = RM_9;
				break;
			case Reg10:
				reg = RM_10;
				break;
			case Reg11:
				reg = RM_11;
				break;
			case Reg12:
				reg = RM_12;
				break;
			case Reg13:
				reg = RM_13;
				break;
			case Reg14:
				reg = RM_14;
				break;
			case Reg15:
				reg = RM_15;
				break;
			default:
				fatalError(ERR_INVREG_GETREG);
				return 0;
		}
		switch (type)
		{
			case StdTypes.T_DPTR:
				return reg | 0x7 | REX;
			case StdTypes.T_LONG:
			case StdTypes.T_PTR:
				if (nr != 1)
					return 0;
				return reg | 0x7 | REX;
			case StdTypes.T_INT:
				if (nr != 1)
					return 0;
				return reg | 0x7;
			case StdTypes.T_SHRT:
			case StdTypes.T_CHAR:
				if (nr != 1)
					return 0;
				return reg | 0x3;
			case StdTypes.T_BOOL:
			case StdTypes.T_BYTE:
				if (nr != 1)
					return 0;
				return reg | 0x1;
		}
		fatalError(ERR_INVTYPE_GETREG);
		return 0;
	}
	
	//here the real code-generation starts
	public Mthd prepareMethodCoding(Mthd mthd)
	{
		Mthd lastMthd;
		
		if ((lastMthd = curMthd) != null)
			curInlineLevel++;
		else
		{
			mthdContainer = mthd; //remember outest level method
			curFPUReg = FPUREGSTART; //reset only if not inlining
		}
		popFPUDone = writtenRegs = usedRegs = 0;
		curMthd = mthd;
		return lastMthd;
	}
	
	public void codeProlog()
	{
		int i;
		if ((curMthd.marker & Marks.K_INTR) == 0)
		{
			curVarOffParam = curInlineLevel > 0 ? VAROFF_PARAM_INL : VAROFF_PARAM_NRM;
			if (curMthd.varSize == 0 && curMthd.parSize == 0 && !noStackOptimization)
				return; //no intr, no parameters, no local vars -> no ebp
		}
		else
		{
			if (curInlineLevel > 0)
			{
				fatalError(ERR_INTNOINLINE);
				return;
			}
			if (curMthd.parSize != 0 && curMthd.parSize != 8)
			{
				fatalError(ERR_INVPARINT);
				return;
			}
			curVarOffParam = VAROFF_PARAM_INT;
			ins(I_PUSHreg, REX | R_EAX);
			ins(I_PUSHreg, REX | R_ECX);
			ins(I_PUSHreg, REX | R_EDX);
			ins(I_PUSHreg, REX | R_EBX);
			ins(I_PUSHreg, REX | R_ESI);
			ins(I_PUSHreg, REX | R_EDI);
			ins(I_PUSHreg, REX | R_8D);
			ins(I_PUSHreg, REX | R_9D);
			ins(I_PUSHreg, REX | R_10D);
			ins(I_PUSHreg, REX | R_11D);
			ins(I_PUSHreg, REX | R_12D);
			ins(I_PUSHreg, REX | R_13D);
			ins(I_PUSHreg, REX | R_14D);
			ins(I_PUSHreg, REX | R_15D);
		}
		ins(I_PUSHreg, REX | R_EBP);
		ins(I_MOVregreg, REX | R_EBP, REX | R_ESP);
		switch (curMthd.varSize)
		{
			case 0:
				break;
			case 8:
				ins(I_PUSHimm);
				break;
			default:
				ins(I_XORregreg, R_EBX, R_EBX);
				for (i = curMthd.varSize; i > 0; i -= 8)
					ins(I_PUSHreg, REX | R_EBX);
		}
	}
	
	public void codeEpilog(Mthd outline)
	{
		int var, par;
		
		if (curInlineLevel == 0 && curFPUReg != FPUREGSTART)
		{
			fatalError(ERR_FPUSTACK);
			return;
		}
		var = curMthd.varSize;
		par = curMthd.parSize;
		if (curInlineLevel > 0)
		{ //end of method inlining
			if (var != 0 || par != 0)
			{
				if (var == 0)
				{ //par!=0
					ins(I_POPreg, REX | R_EBP);
					ins(I_ADDregimm, REX | R_ESP, 0, 0, par);
				}
				else if (par == 0)
				{ //var!=0
					ins(I_ADDregimm, REX | R_ESP, 0, 0, var);
					ins(I_POPreg, REX | R_EBP);
				}
				else
				{ //neither var==0 nor par==0
					ins(I_MOVregmem, REX | R_EBP, REX | R_EBP);
					ins(I_ADDregimm, REX | R_ESP, 0, 0, var + par + 8);
				}
			}
			else if (noStackOptimization)
				ins(I_POPreg, REX | R_EBP); //no stack optimization - clean up stack frame
			if (--curInlineLevel == 0)
				curVarOffParam = (outline.marker & Marks.K_INTR) != 0 ? VAROFF_PARAM_INT : VAROFF_PARAM_NRM;
			curMthd = outline;
			return;
		}
		//normal method return
		if (var == 0 && par == 0 && (curMthd.marker & Marks.K_INTR) == 0 && !noStackOptimization)
		{
			ins(I_RETimm); //nothing pushed, don't pop
			curMthd = null;
			return;
		}
		if (var != 0)
			ins(I_ADDregimm, REX | R_ESP, 0, 0, var);
		ins(I_POPreg, REX | R_EBP);
		if (par > 32767)
		{
			fatalError(ERR_RETSIZE_CODEEPILOG);
			return;
		}
		if ((curMthd.marker & Marks.K_INTR) != 0)
		{
			ins(I_POPreg, REX | R_15D);
			ins(I_POPreg, REX | R_14D);
			ins(I_POPreg, REX | R_13D);
			ins(I_POPreg, REX | R_12D);
			ins(I_POPreg, REX | R_11D);
			ins(I_POPreg, REX | R_10D);
			ins(I_POPreg, REX | R_9D);
			ins(I_POPreg, REX | R_8D);
			ins(I_POPreg, REX | R_EDI);
			ins(I_POPreg, REX | R_ESI);
			ins(I_POPreg, REX | R_EBX);
			ins(I_POPreg, REX | R_EDX);
			ins(I_POPreg, REX | R_ECX);
			ins(I_POPreg, REX | R_EAX);
			if (par != 0)
				ins(I_ADDregimm, REX | R_ESP, 0, 0, 8);
			ins(I_IRET);
		}
		else
			ins(I_RETimm, 0, 0, 0, (curMthd.marker & Marks.K_NTCB) == 0 ? par : 0);
		curMthd = null;
	}
	
	//general purpose instructions
	public void genLoadConstVal(int dst, int val, int type)
	{
		if (type == StdTypes.T_FLT)
		{
			if (dst != curFPUReg)
			{
				errorFPU();
				return;
			}
			ins(I_PUSHimm, 0, 0, 0, val);
			ins(I_FLDmem, REX | R_ESP, 0, 0, 0, 0l, FPU32);
			ins(I_ADDregimm, REX | R_ESP, 0, 0, 8); //pseudo-pop
			return;
		}
		if ((dst = getReg(1, dst, StdTypes.T_INT, true)) == 0)
			return;
		if (val == 0)
		{
			ins(I_XORregreg, dst, dst);
			return;
		}
		switch (type)
		{
			case StdTypes.T_BYTE:
				val = (byte) val;
				break;
			case StdTypes.T_SHRT:
				val = (short) val;
				break;
			case StdTypes.T_CHAR:
				val = (char) val;
				break;
			//default: nothing to do for other types
		}
		ins(I_MOVregimm, dst, 0, 0, val);
	}
	
	public void genLoadConstDoubleOrLongVal(int dst, long val, boolean asDouble)
	{
		if (asDouble)
		{
			if (dst != curFPUReg)
			{
				errorFPU();
				return;
			}
			genPushConstDoubleOrLongVal(val, true);
			ins(I_FLDmem, REX | R_ESP, 0, 0, 0, 0l, FPU64);
			ins(I_ADDregimm, REX | R_ESP, 0, 0, 8); //pseudo-pop
		}
		else
		{
			if ((dst = getReg(1, dst, StdTypes.T_LONG, true)) == 0)
				return;
			if (val == 0l)
				ins(I_XORregreg, dst, dst);
			else if ((val & 0xFFFFFFFF00000000l) == 0l)
				ins(I_MOVregimm, dst & ~REX, 0, 0, (int) val);
			else
				ins(I_MOVregimmL, dst, 0, 0, 0, val, 0);
		}
	}
	
	public void genLoadVarAddr(int dst, int src, Object loc, int off)
	{
		int pos = mem.getAddrAsInt(loc, off);
		if ((dst = getReg(1, dst, StdTypes.T_PTR, true)) == 0)
			return;
		if (src == 0)
		{
			long val;
			if (((val = mem.getAddrAsLong(loc, off)) & 0xFFFFFFFF00000000l) != 0l)
				ins(I_MOVregimmL, dst, 0, 0, 0, val, 0);
			else
				ins(I_MOVregimm, dst & ~REX, 0, 0, (int) val);
			return;
		}
		if (src == regBase && pos >= 0)
			if (pos >= 0)
				pos += curVarOffParam;
		if ((src = getReg(1, src, StdTypes.T_PTR, true)) == 0)
			return;
		if (dst == src && pos == 0)
			return;
		ins(I_LEAregmem, dst, src, pos);
	}
	
	public void genLoadVarVal(int dstR, int src, Object loc, int off, int type)
	{
		int dst, dst2, pos = mem.getAddrAsInt(loc, off);
		if (src == regBase && pos >= 0)
			if (pos >= 0)
				pos += curVarOffParam;
		if (src != 0 && (src = getReg(1, src, StdTypes.T_PTR, false)) == 0)
			return;
		if (src == 0)
		{
			//TODO
			if ((mem.getAddrAsLong(loc, off) & 0xFFFFFFFF00000000l) != 0l)
			{
				fatalError("genLoadVarVal with upper address bits not cleared is not yet supported");
				return;
			}
		}
		if (type == StdTypes.T_FLT || type == StdTypes.T_DBL)
		{
			if (dstR != curFPUReg)
			{
				errorFPU();
				return;
			}
			ins(I_FLDmem, src, 0, pos, 0, 0l, type == StdTypes.T_FLT ? FPU32 : FPU64);
			return;
		}
		if ((dst = getReg(1, dstR, StdTypes.T_INT, true)) == 0)
			return;
		switch (type)
		{
			case StdTypes.T_BOOL:
			case StdTypes.T_BYTE:
				ins(I_MOVSXregmem, dst, src, pos, 0, 0l, 1);
				return;
			case StdTypes.T_SHRT:
				ins(I_MOVSXregmem, dst, src, pos, 0, 0l, 2);
				return;
			case StdTypes.T_CHAR:
				ins(I_MOVZXregmem, dst, src, pos, 0, 0l, 2);
				return;
			case StdTypes.T_INT:
				ins(I_MOVregmem, dst, src, pos);
				return;
			case StdTypes.T_LONG:
			case StdTypes.T_PTR:
				ins(I_MOVregmem, REX | dst, src, pos);
				return;
			case StdTypes.T_DPTR:
				if ((dst2 = getReg(2, dstR, StdTypes.T_DPTR, true)) == 0)
					return;
				if ((dst |= REX) != (src |= REX))
				{ //first move dst, then dst2
					ins(I_MOVregmem, dst, src, pos);
					ins(I_MOVregmem, dst2, src, pos + 8);
				}
				else
				{
					ins(I_MOVregmem, dst2, src, pos + 8);
					ins(I_MOVregmem, dst, src, pos);
				}
				return;
			default:
				fatalError(ERR_INVTYPE_GENLOADVARVAL);
				return;
		}
	}
	
	public void genConvertVal(int dst, int src, int toType, int fromType)
	{
		if (toType == StdTypes.T_FLT || toType == StdTypes.T_DBL)
		{
			if (dst != curFPUReg)
			{
				errorFPU();
				return;
			}
			switch (fromType)
			{
				case StdTypes.T_BYTE:
				case StdTypes.T_SHRT:
				case StdTypes.T_CHAR:
				case StdTypes.T_INT:
					if ((src = getReg(1, src, StdTypes.T_INT, false)) == 0)
						return;
					ins(I_PUSHreg, src);
					ins(I_FILDmem, REX | R_ESP, 0, 0, 0, 0l, FPU32);
					ins(I_POPreg, src);
					return;
				case StdTypes.T_LONG:
					if ((src = getReg(1, src, StdTypes.T_LONG, false)) == 0)
						return;
					ins(I_PUSHreg, src);
					ins(I_FILDmem, REX | R_ESP, 0, 0, 0, 0l, FPU64);
					ins(I_POPreg, src); //shorter than the line above
					return;
				case StdTypes.T_FLT:
				case StdTypes.T_DBL:
					return; //nothing to do
			}
			fatalError("error in AMD64.genConvertVal to floating point");
			return;
		}
		//toType is not floating point
		if (fromType == StdTypes.T_FLT || fromType == StdTypes.T_DBL)
		{
			if (src != curFPUReg)
			{
				errorFPU();
				return;
			}
			switch (toType)
			{
				case StdTypes.T_BYTE:
				case StdTypes.T_SHRT:
				case StdTypes.T_CHAR:
				case StdTypes.T_INT:
					if ((dst = getReg(1, dst, toType, false)) == 0)
						return;
					ins(I_PUSHreg, dst | RS_E);
					ins(I_FISTTPmem, REX | R_ESP, 0, 0, 0, 0l, FPU32);
					ins(I_POPreg, dst | RS_E);
					if (toType != StdTypes.T_INT)
						ins(I_MOVSXregreg, dst | RS_E, dst);
					dupFPUIns = null;
					popFPUDone++;
					return;
				case StdTypes.T_LONG:
					if ((dst = getReg(1, dst, StdTypes.T_LONG, false)) == 0)
						return;
					ins(I_PUSHreg, dst);
					ins(I_FISTTPmem, REX | R_ESP, 0, 0, 0, 0l, FPU64);
					ins(I_POPreg, dst);
					dupFPUIns = null;
					popFPUDone++;
					return;
			}
			fatalError("error in AMD64.genConvertVal from floating point");
			return;
		}
		//no floating point involved
		if (toType == StdTypes.T_LONG || toType == StdTypes.T_PTR)
		{ //to long/ptr is always the same
			if ((dst = getReg(1, dst, StdTypes.T_LONG, true)) == 0)
				return; //next line: pointers do not need sign extension
			if (fromType == StdTypes.T_LONG || fromType == StdTypes.T_PTR || toType == StdTypes.T_PTR)
			{
				if ((src = getReg(1, src, StdTypes.T_LONG, false)) == 0)
					return;
				if (dst == src) /*nothing to do*/
					return;
				ins(I_MOVregreg, dst, src);
				return;
			}
			//all others are int-values already, extend sign to complete upper register
			if ((src = getReg(1, src, StdTypes.T_INT, false)) == 0)
				return;
			if (dst == (REX | R_EAX))
			{
				if (src != R_EAX)
					ins(I_MOVregreg, R_EAX, src);
				ins(I_CDQE);
			}
			else
			{
				if ((usedRegs & RegA) != 0)
				{ //RegA is in use
					ins(I_XCHGregreg, REX | R_EAX, REX | src);
					ins(I_CDQE);
					ins(I_XCHGregreg, REX | R_EAX, REX | src);
					if (dst != (REX | src))
						ins(I_MOVregreg, dst, REX | src);
				}
				else
				{ //silently destroy RegA
					ins(I_MOVregreg, R_EAX, src);
					ins(I_CDQE);
					ins(I_MOVregreg, dst, REX | R_EAX);
				}
			}
			return;
		}
		//toType!=T_LONG, use only requested part of source
		if ((dst = getReg(1, dst, StdTypes.T_INT, true)) == 0 || (src = getReg(1, src, toType, false)) == 0)
			return;
		switch (fromType)
		{
			case StdTypes.T_BYTE:
				switch (toType)
				{
					case StdTypes.T_BYTE:
					case StdTypes.T_SHRT:
					case StdTypes.T_CHAR:
					case StdTypes.T_INT:
						//nothing to do for byte->byte/short/char/int
						if (dst != (src | RS_E))
							ins(I_MOVregreg, dst, src);
						//else: nothing to do
						return;
				}
				break;
			case StdTypes.T_SHRT:
			case StdTypes.T_CHAR:
				switch (toType)
				{
					case StdTypes.T_BYTE:
						ins(I_MOVSXregreg, dst, src);
						return;
					case StdTypes.T_SHRT:
					case StdTypes.T_CHAR:
					case StdTypes.T_INT:
						//nothing to do for short/char->short/char/int
						if (dst != (src | RS_E))
							ins(I_MOVregreg, dst, src);
						//else: nothing to do
						return;
				}
				break;
			case StdTypes.T_INT:
			case StdTypes.T_LONG:
				switch (toType)
				{
					case StdTypes.T_BYTE:
					case StdTypes.T_SHRT:
						ins(I_MOVSXregreg, dst, src);
						return;
					case StdTypes.T_CHAR:
						ins(I_MOVZXregreg, dst, src);
						return;
					case StdTypes.T_INT:
						//nothing to do for int->int
						if (dst != (src | RS_E))
							ins(I_MOVregreg, dst, src);
						//else: nothing to do
						return;
				}
				break;
		}
		fatalError(ERR_UNSTYPE_GENCONVERTVAL);
	}
	
	public void genDup(int dstR, int srcR, int type)
	{
		int dst, src;
		if (dstR == srcR) /*nothing to do*/
			return;
		if ((dst = getReg(1, dstR, type, true)) == 0 || (src = getReg(1, srcR, type, false)) == 0)
			return;
		switch (type)
		{
			case StdTypes.T_BOOL:
			case StdTypes.T_BYTE:
			case StdTypes.T_SHRT:
			case StdTypes.T_CHAR:
			case StdTypes.T_INT:
			case StdTypes.T_LONG:
			case StdTypes.T_PTR:
				ins(I_MOVregreg, dst, src);
				return;
			case StdTypes.T_DPTR:
				ins(I_MOVregreg, dst, src);
				if ((dst = getReg(2, dstR, StdTypes.T_DPTR, true)) == 0 || (src = getReg(2, srcR, StdTypes.T_DPTR, false)) == 0)
					return;
				ins(I_MOVregreg, dst, src);
				return;
		}
		fatalError(ERR_UNSTYPE_GENDUP);
	}
	
	public void genPushConstVal(int val, int type)
	{ //type is not of interest here
		ins(I_PUSHimm, 0, 0, 0, val);
	}
	
	public void genPushConstDoubleOrLongVal(long val, boolean asDouble)
	{
		int iVal = (int) val, reg, regR;
		if (val == (long) iVal)
		{
			ins(I_PUSHimm, 0, 0, 0, iVal); //automatic sign-extension
			return;
		}
		//this is a real 64-bit value, use register as temp-store
		regR = getFreeReg(0, StdTypes.T_LONG);
		if ((reg = getReg(1, regR, StdTypes.T_LONG, false)) == 0)
			return;
		ins(I_MOVregimmL, reg, 0, 0, 0, val, 0);
		ins(I_PUSHreg, reg);
		deallocReg(regR);
	}
	
	public void genPush(int srcR, int type)
	{
		int src;
		boolean isDouble = false;
		
		switch (type)
		{
			case StdTypes.T_DBL:
				isDouble = true; //no break;
			case StdTypes.T_FLT:
				if (curFPUReg != srcR)
				{
					errorFPU();
					return;
				}
				ins(I_PUSHreg, R_EAX);
				ins(I_FSTPmem, REX | R_ESP, 0, 0, 0, 0l, isDouble ? FPU64 : FPU32);
				dupFPUIns = null;
				popFPUDone++;
				return;
			case StdTypes.T_NULL:
				fatalError(ERR_UNRPUSH_GENPUSH);
				return;
			case StdTypes.T_DPTR:
				if ((src = getReg(2, srcR, StdTypes.T_DPTR, false)) == 0)
					return;
				ins(I_PUSHreg, src);
				//no return, has to do the following, too
		}
		if (type == StdTypes.T_BOOL || type == StdTypes.T_BYTE || type == StdTypes.T_SHRT || type == StdTypes.T_CHAR)
			type = StdTypes.T_INT;
		if ((src = getReg(1, srcR, type, false)) == 0)
			return;
		ins(I_PUSHreg, src);
	}
	
	public void genPop(int dstR, int type)
	{
		int dst, dummyType;
		boolean isDouble = false;
		
		if (type == StdTypes.T_FLT || (isDouble = (type == StdTypes.T_DBL)) == true)
		{
			if (curFPUReg != dstR)
			{
				errorFPU();
				return;
			}
			ins(I_FLDmem, REX | R_ESP, 0, 0, 0, 0l, isDouble ? FPU64 : FPU32);
			ins(I_ADDregimm, REX | R_ESP, 0, 0, 8);
			return;
		}
		if (type == StdTypes.T_BOOL || type == StdTypes.T_BYTE || type == StdTypes.T_SHRT || type == StdTypes.T_CHAR)
			dummyType = StdTypes.T_INT;
		else
			dummyType = type;
		if ((dst = getReg(1, dstR, dummyType, true)) == 0)
			return;
		ins(I_POPreg, dst);
		if (type == StdTypes.T_DPTR)
		{
			if ((dst = getReg(2, dstR, StdTypes.T_DPTR, true)) == 0)
				return;
			ins(I_POPreg, dst);
		}
	}
	
	public void genAssign(int dst, int srcR, int type)
	{
		int src;
		boolean isDouble = false;
		
		if (type == StdTypes.T_FLT || (isDouble = (type == StdTypes.T_DBL)) == true)
		{
			if (curFPUReg != srcR)
			{
				errorFPU();
				return;
			}
			if ((dst = getReg(1, dst, StdTypes.T_PTR, false)) == 0)
				return;
			dupFPUIns = ins(I_FDUP);
			ins(I_FSTPmem, dst, 0, 0, 0, 0l, isDouble ? FPU64 : FPU32);
			return;
		}
		if ((dst = getReg(1, dst, StdTypes.T_PTR, false)) == 0 || (src = getReg(1, srcR, type, false)) == 0)
			return;
		ins(I_MOVmemreg, dst, src);
		if (type == StdTypes.T_DPTR)
		{
			if ((src = getReg(2, srcR, StdTypes.T_DPTR, false)) == 0)
				return;
			ins(I_MOVmemreg, dst, src, 8);
		}
	}
	
	public void genStoreVarVal(int objReg, Object loc, int off, int src, int type)
	{
		int srcR, pos = mem.getAddrAsInt(loc, off);
		if (type == StdTypes.T_FLT || type == StdTypes.T_DBL)
		{
			super.genStoreVarVal(objReg, loc, off, src, type);
			return;
		}
		if ((srcR = getReg(1, src, type, false)) == 0)
			return;
		if (objReg == 0)
		{
			//TODO
			if ((mem.getAddrAsLong(loc, off) & 0xFFFFFFFF00000000l) != 0l)
			{
				fatalError("genStoreVarVal with upper address bits not cleared is not yet supported");
				return;
			}
		}
		if (objReg == regBase && pos >= 0)
			pos += curVarOffParam;
		if (objReg != 0 && (objReg = getReg(1, objReg, StdTypes.T_PTR, false)) == 0)
			return;
		ins(I_MOVmemreg, objReg, srcR, pos);
		if (type == StdTypes.T_DPTR)
		{
			if ((srcR = getReg(2, src, type, false)) == 0)
				return;
			ins(I_MOVmemreg, objReg, srcR, pos + 8);
		}
	}
	
	public void genStoreVarConstVal(int objReg, Object loc, int off, int val, int type)
	{
		int pos = mem.getAddrAsInt(loc, off);
		if (objReg == regBase && pos >= 0)
			pos += curVarOffParam;
		if (objReg == 0)
		{
			//TODO
			if ((mem.getAddrAsLong(loc, off) & 0xFFFFFFFF00000000l) != 0l)
			{
				fatalError("genStoreVarConstVal with upper address bits not cleared is not yet supported");
				return;
			}
		}
		if (objReg != 0 && (objReg = getReg(1, objReg, StdTypes.T_PTR, false)) == 0)
			return;
		switch (type)
		{
			case StdTypes.T_BYTE:
			case StdTypes.T_BOOL:
				ins(I_MOVmemimm, objReg, 0, pos, val, 0l, 1);
				break;
			case StdTypes.T_SHRT:
			case StdTypes.T_CHAR:
				ins(I_MOVmemimm, objReg, 0, pos, val, 0l, 2);
				break;
			case StdTypes.T_PTR:
				ins(I_MOVmemimm, objReg, 0, pos + 4, 0, 0l, 4); //no break, has to do the following, too
			case StdTypes.T_INT:
			case StdTypes.T_FLT:
				ins(I_MOVmemimm, objReg, 0, pos, val, 0l, 4);
				break;
			default:
				fatalError(ERR_INVMODE_GENSTOREVARX);
				return;
		}
	}
	
	public void genStoreVarConstDoubleOrLongVal(int objReg, Object loc, int off, long val, boolean asDouble)
	{
		genStoreVarConstVal(objReg, loc, off + 4, (int) (val >>> 32), StdTypes.T_INT);
		genStoreVarConstVal(objReg, loc, off, (int) val, StdTypes.T_INT);
	}
	
	public void genBinOp(int dstR, int src1, int src2, int op, int type)
	{ //may destroy src1/2
		int opType = op >>> 16, opPar = op & 0xFFFF, dst;
		int usedMask, saveRegs, tmp;
		boolean xchg;
		
		if (dstR != src1 || dstR == src2)
		{
			fatalError(ERR_UNSCASE_GENBINOP);
			return;
		}
		switch (type)
		{
			case StdTypes.T_FLT:
			case StdTypes.T_DBL:
				if (src2 != curFPUReg || dstR != curFPUReg - FPUREGINC)
				{
					errorFPU();
					return;
				}
				dupFPUIns = null;
				popFPUDone++;
				switch (opPar)
				{
					case Ops.A_PLUS:
						ins(I_FADDP);
						return;
					case Ops.A_MINUS:
						ins(I_FSUBP);
						return;
					case Ops.A_MUL:
						ins(I_FMULP);
						return;
					case Ops.A_DIV:
						ins(I_FDIVP);
						return;
				}
				fatalError(ERR_UNSOP_GENBINOP);
				return;
			case StdTypes.T_BOOL:
				if (opType != Ops.S_ARI || !(opPar == Ops.A_AND || opPar == Ops.A_OR || opPar == Ops.A_XOR))
				{
					fatalError(ERR_UNSOP_GENBINOP);
					return;
				}
				//has to do the following, too!
			case StdTypes.T_BYTE:
			case StdTypes.T_SHRT:
			case StdTypes.T_CHAR:
			case StdTypes.T_INT:
				if ((dst = getReg(1, dstR, StdTypes.T_INT, false)) == 0 || (src1 = getReg(1, src1, StdTypes.T_INT, false)) == 0 || (src2 = getReg(1, src2, StdTypes.T_INT, false)) == 0)
					return;
				switch (opType)
				{
					case Ops.S_ARI:
						switch (opPar)
						{
							case Ops.A_AND:
								ins(I_ANDregreg, dst, src2);
								break;
							case Ops.A_XOR:
								ins(I_XORregreg, dst, src2);
								break;
							case Ops.A_OR:
								ins(I_ORregreg, dst, src2);
								break;
							case Ops.A_PLUS:
								ins(I_ADDregreg, dst, src2);
								break;
							case Ops.A_MINUS:
								ins(I_SUBregreg, dst, src2);
								break;
							case Ops.A_MUL:
								if (dst != R_EAX && src2 != R_EAX)
									saveRegs = RegA;
								else
									saveRegs = 0;
								if (dst != R_EDX && src2 != R_EDX)
									saveRegs |= RegD;
								usedMask = storeReg(saveRegs);
								if (dst != R_EAX)
								{
									if (src2 != R_EAX)
										ins(I_MOVregreg, R_EAX, dst);
									else
										src2 = dst; //we don't need the original src2 anymore
								}
								//else: dstsrc1R==RegA && src2R!=RegA, nothing needed
								ins(I_IMULreg, src2);
								if (dst != R_EAX)
									ins(I_MOVregreg, dst, R_EAX);
								restoreReg(usedMask);
								break;
							case Ops.A_DIV:
							case Ops.A_MOD:
								if (dst != R_EAX)
								{ //dstsrc1!=R_EAX
									if (src2 == R_EAX)
									{ //dstsrc1!=R_EAX && src2==R_EAX
										if (dst == R_EDX)
										{ //dstsrc1==R_EDX && src2==R_EAX
											usedMask = storeReg(RegB);
											ins(I_MOVregreg, R_EAX, dst);
											ins(I_MOVregreg, R_EBX, src2);
											src2 = R_EBX;
										}
										else
										{ //dstsrc1!=R_EAX && dstsrc1!=R_EDX && src2==R_EAX
											usedMask = storeReg(RegD);
											ins(I_XCHGregreg, dst, src2);
											src2 = dst;
										}
									}
									else
									{ //dstsrc1!=R_EAX && src2!=R_EAX
										if (dst == R_EDX)
										{ //dstsrc1==R_EDX && src2!=R_EAX (&& src2!=R_EDX)
											usedMask = storeReg(RegA);
											ins(I_MOVregreg, R_EAX, dst);
										}
										else
										{ //dstsrc1!=R_EAX && dstsrc1!=R_EDX && src2!=R_EAX
											if (src2 == R_EDX)
											{ //dstsrc1!=R_EAX && dstsrc1!=R_EDX && src2==R_EDX
												usedMask = storeReg(RegA);
												ins(I_MOVregreg, R_EAX, dst);
												ins(I_MOVregreg, dst, src2);
												src2 = dst;
											}
											else
											{ //dstsrc1!=R_EAX && dstsrc1!=R_EDX && src2!=R_EAX && src2!=R_EDX
												//==> dstsrc1 and src2 reside in R_EBX and R_ECX (somehow)
												usedMask = storeReg(RegA | RegD);
												ins(I_MOVregreg, R_EAX, dst);
											}
										}
									}
								}
								else if (src2 == R_EDX)
								{ //dstsrc1==R_EAX && src2==R_EDX
									usedMask = storeReg(RegB);
									ins(I_MOVregreg, R_EBX, src2);
									src2 = R_EBX;
								}
								else
									usedMask = storeReg(RegD); //dstsrc1==R_EAX && src2!=R_EDX, save to be destroyed R_EDX
								ins(I_CDQ, 0);
								ins(I_IDIVreg, src2);
								if (opPar == Ops.A_DIV)
								{
									if (dst != R_EAX)
										ins(I_MOVregreg, dst, R_EAX);
								}
								else
								{ //Ops.A_MOD
									if (dst != R_EDX)
										ins(I_MOVregreg, dst, R_EDX);
								}
								restoreReg(usedMask);
								break;
							default:
								fatalError(ERR_UNSOP_GENBINOP);
								return;
						}
						switch (type)
						{
							case StdTypes.T_BYTE:
							case StdTypes.T_SHRT:
								tmp = getReg(1, dstR, type, false);
								ins(I_MOVSXregreg, dst, tmp);
								break;
							case StdTypes.T_CHAR:
								tmp = getReg(1, dstR, type, false);
								ins(I_MOVZXregreg, dst, tmp);
								break;
						}
						return;
					case Ops.S_BSH:
						if (dst == R_ECX)
						{ //src2!=R_ECX
							ins(I_XCHGregreg, R_ECX, src2);
							tmp = src2;
							src2 = dst;
							dst = tmp;
							usedMask = 0;
							xchg = true;
						}
						else
						{
							xchg = false;
							if (src2 != R_ECX)
							{
								usedMask = storeReg(RegC);
								ins(I_MOVregreg, R_ECX, src2);
							}
							else
								usedMask = 0;
						}
						switch (opPar)
						{
							case Ops.B_SHL:
								ins(I_SHLregreg, dst); //second regiser is fixed to cl
								break;
							case Ops.B_SHRL:
								ins(I_SHRregreg, getReg(1, dstR, type, false)); //second register is fixed to cl, get read operand size
								break;
							case Ops.B_SHRA:
								ins(I_SARregreg, dst); //second register is fixed to cl
								break;
							default:
								fatalError(ERR_UNSOP_GENBINOP);
								return;
						}
						if (xchg)
							ins(I_XCHGregreg, dst, src2);
						restoreReg(usedMask);
						if (type != StdTypes.T_INT && type != StdTypes.T_BOOL)
						{
							tmp = getReg(1, dstR, type, false);
							ins(I_MOVSXregreg, dst, tmp);
						}
						return;
				}
				fatalError(ERR_UNSOP_GENBINOP);
				return;
			case StdTypes.T_LONG:
				if ((dst = getReg(1, dstR, StdTypes.T_LONG, false)) == 0 || (src2 = getReg(1, src2, StdTypes.T_LONG, false)) == 0)
					return;
				switch (opType)
				{
					case Ops.S_ARI:
						switch (opPar)
						{
							case Ops.A_AND:
								ins(I_ANDregreg, dst, src2);
								return;
							case Ops.A_XOR:
								ins(I_XORregreg, dst, src2);
								return;
							case Ops.A_OR:
								ins(I_ORregreg, dst, src2);
								return;
							case Ops.A_PLUS:
								ins(I_ADDregreg, dst, src2);
								return;
							case Ops.A_MINUS:
								ins(I_SUBregreg, dst, src2);
								return;
							case Ops.A_MUL:
								if (dst != (REX | R_EAX) && src2 != (REX | R_EAX))
									saveRegs = RegA;
								else
									saveRegs = 0;
								if (dst != (REX | R_EDX) && src2 != (REX | R_EDX))
									saveRegs |= RegD;
								usedMask = storeReg(saveRegs);
								if (dst != (REX | R_EAX))
								{
									if (src2 != (REX | R_EAX))
										ins(I_MOVregreg, REX | R_EAX, dst);
									else
										src2 = dst; //we don't need the original src2 anymore
								}
								//else: dstsrc1==REX|R_EAX && src2!=REX|R_EAX, nothing needed
								ins(I_IMULreg, src2);
								if (dst != (REX | R_EAX))
									ins(I_MOVregreg, dst, REX | R_EAX);
								restoreReg(usedMask);
								return;
							case Ops.A_DIV:
							case Ops.A_MOD:
								if (dst != (REX | R_EAX))
								{ //dstsrc1!=REX|R_EAX
									if (src2 == (REX | R_EAX))
									{ //dstsrc1!=REX|R_EAX && src2==REX|R_EAX
										if (dst == (REX | R_EDX))
										{ //dstsrc1==REX|R_EDX && src2==REX|R_EAX
											usedMask = storeReg(RegB);
											ins(I_MOVregreg, REX | R_EBX, src2);
											ins(I_MOVregreg, REX | R_EAX, dst);
											src2 = REX | R_EBX;
										}
										else
										{ //dstsrc1!=REX|R_EAX && dstsrc1!=REX|R_EDX && src2==REX|R_EAX
											usedMask = storeReg(RegD);
											ins(I_XCHGregreg, dst, src2);
											src2 = dst;
										}
									}
									else
									{ //dstsrc1!=REX|R_EAX && src2!=REX|R_EAX
										if (dst == (REX | R_EDX))
										{ //dstsrc1==REX|R_EDX && src2!=REX|R_EAX (&& src2!=R_EDX)
											usedMask = storeReg(RegA);
											ins(I_MOVregreg, REX | R_EAX, dst);
										}
										else
										{ //dstsrc1!=REX|R_EAX && dstsrc1!=REX|R_EDX && src2!=REX|R_EAX
											if (src2 == (REX | R_EDX))
											{ //dstsrc1!=REX|R_EAX && dstsrc1!=REX|R_EDX && src2==REX|R_EDX
												usedMask = storeReg(RegA);
												ins(I_MOVregreg, REX | R_EAX, dst);
												ins(I_MOVregreg, dst, src2);
												src2 = dst;
											}
											else
											{ //dstsrc1!=REX|R_EAX && dstsrc1!=REX|R_EDX && src2!=REX|R_EAX && src2!=R_REX|EDX
												//==> dstsrc1 and src2 reside not in REX|R_EAX and REX|R_EDX (somewhere)
												usedMask = storeReg(RegA | RegD);
												ins(I_MOVregreg, REX | R_EAX, dst);
											}
										}
									}
								}
								else if (src2 == (REX | R_EDX))
								{ //dstsrc1==REX|R_EAX && src2==REX|R_EDX
									usedMask = storeReg(RegB);
									ins(I_MOVregreg, REX | R_EBX, src2);
									src2 = REX | R_EBX;
								}
								else
									usedMask = storeReg(RegD); //dstsrc1==R_EAX && src2!=R_EDX, save to be destroyed R_EDX
								ins(I_CQO, 0);
								ins(I_IDIVreg, src2);
								if (opPar == Ops.A_DIV)
								{
									if (dst != (REX | R_EAX))
										ins(I_MOVregreg, dst, REX | R_EAX);
								}
								else
								{ //COps.A_MOD
									if (dst != (REX | R_EDX))
										ins(I_MOVregreg, dst, REX | R_EDX);
								}
								restoreReg(usedMask);
								return;
						}
						fatalError(ERR_UNSOP_GENBINOP);
						return;
					case Ops.S_BSH:
						if (dst == (REX | R_ECX))
						{ //src2!=REX|R_ECX
							ins(I_XCHGregreg, REX | R_ECX, src2);
							tmp = src2;
							src2 = dst;
							dst = tmp;
							usedMask = 0;
							xchg = true;
						}
						else
						{
							xchg = false;
							if (src2 != (REX | R_ECX))
							{
								usedMask = storeReg(RegC);
								ins(I_MOVregreg, REX | R_ECX, src2);
							}
							else
								usedMask = 0;
						}
						switch (opPar)
						{
							case Ops.B_SHL:
								ins(I_SHLregreg, dst); //second regiser is fixed to cl
								break;
							case Ops.B_SHRL:
								ins(I_SHRregreg, dst); //second register is fixed to cl
								break;
							case Ops.B_SHRA:
								ins(I_SARregreg, dst); //second register is fixed to cl
								break;
							default:
								fatalError(ERR_UNSOP_GENBINOP);
								return;
						}
						if (xchg)
							ins(I_XCHGregreg, dst, src2);
						return;
				}
				fatalError(ERR_UNSOP_GENBINOP);
				return;
		}
		fatalError(ERR_UNSTYPE_GENBINOP);
	}
	
	public void genBinOpConstRi(int dstR, int src1R, int val, int op, int type)
	{
		int opType = op >>> 16, opPar = op & 0xFFFF, dst, tmp;
		if (dstR != src1R)
		{
			fatalError(ERR_UNSCASE_GENBINOP);
			return;
		}
		switch (type)
		{
			case StdTypes.T_BOOL:
				if (opType != Ops.S_ARI || !(opPar == Ops.A_AND || opPar == Ops.A_OR || opPar == Ops.A_XOR))
				{
					fatalError(ERR_UNSOP_GENBINOP);
					return;
				}
				//has to do the following, too!
			case StdTypes.T_BYTE:
			case StdTypes.T_SHRT:
			case StdTypes.T_CHAR:
			case StdTypes.T_INT:
				if ((dst = getReg(1, dstR, StdTypes.T_INT, false)) == 0)
					return;
				switch (opType)
				{
					case Ops.S_ARI:
						switch (opPar)
						{
							case Ops.A_AND:
								ins(I_ANDregimm, dst, 0, 0, val);
								break;
							case Ops.A_XOR:
								ins(I_XORregimm, dst, 0, 0, val);
								break;
							case Ops.A_OR:
								ins(I_ORregimm, dst, 0, 0, val);
								break;
							case Ops.A_PLUS:
								ins(I_ADDregimm, dst, 0, 0, val);
								break;
							case Ops.A_MINUS:
								ins(I_SUBregimm, dst, 0, 0, val);
								break;
							default:
								super.genBinOpConstRi(dstR, src1R, val, op, type);
								return;
						}
						break;
					case Ops.S_BSH:
						switch (opPar)
						{
							case Ops.B_SHL:
								ins(I_SHLregimm, dst, 0, 0, val);
								break;
							case Ops.B_SHRL:
								ins(I_SHRregimm, getReg(1, dstR, type, false), 0, 0, val);
								break;
							case Ops.B_SHRA:
								ins(I_SARregimm, dst, 0, 0, val);
								break;
							default:
								super.genBinOpConstRi(dstR, src1R, val, op, type);
								return;
						}
				}
				if (type != StdTypes.T_INT && type != StdTypes.T_BOOL)
				{
					tmp = getReg(1, dstR, type, false);
					ins(I_MOVSXregreg, dst, tmp);
				}
				return;
		}
		super.genBinOpConstRi(dstR, src1R, val, op, type);
	}
	
	public void genUnaOp(int dstR, int srcR, int op, int type)
	{
		int dst;
		if (dstR != srcR)
		{
			fatalError(ERR_UNSCASE_GENUNAOP);
			return;
		}
		switch (type)
		{
			case StdTypes.T_FLT:
			case StdTypes.T_DBL:
				if (srcR != curFPUReg)
				{
					errorFPU();
					return;
				}
				switch (op & 0xFFFF)
				{
					case Ops.A_MINUS:
						ins(I_FCHS);
						return;
					case Ops.A_PLUS:
						/*nothing to do*/
						return;
				}
				break;
			case StdTypes.T_LONG:
				if ((dst = getReg(1, dstR, StdTypes.T_LONG, false)) == 0)
					return;
				switch (op & 0xFFFF)
				{
					case Ops.A_CPL:
						ins(I_NOTreg, dst);
						return;
					case Ops.A_MINUS:
						ins(I_NEGreg, dst);
						return;
					case Ops.A_PLUS:
						/*nothing to do*/
						return;
				}
				break;
			default:
				genDefaultUnaOp(dstR, srcR, op, type);
				return;
		}
		fatalError(ERR_UNSOP_GENUNAOP);
	}
	
	public void genIncMem(int dst, int type)
	{
		if ((dst = getReg(1, dst, StdTypes.T_PTR, false)) == 0)
			return;
		switch (type)
		{
			case StdTypes.T_BYTE:
				ins(I_INCmem, dst, 0, 0, 0, 0l, 1);
				return;
			case StdTypes.T_SHRT:
			case StdTypes.T_CHAR:
				ins(I_INCmem, dst, 0, 0, 0, 0l, 2);
				return;
			case StdTypes.T_INT:
				ins(I_INCmem, dst, 0, 0, 0, 0l, 4);
				return;
			case StdTypes.T_LONG:
				ins(I_INCmem, dst, 0, 0, 0, 0l, 8);
				return;
		}
		fatalError(ERR_UNSTYPE_GENINCDECMEM);
	}
	
	public void genDecMem(int dst, int type)
	{
		if ((dst = getReg(1, dst, StdTypes.T_PTR, false)) == 0)
			return;
		switch (type)
		{
			case StdTypes.T_BYTE:
				ins(I_DECmem, dst, 0, 0, 0, 0l, 1);
				return;
			case StdTypes.T_SHRT:
			case StdTypes.T_CHAR:
				ins(I_DECmem, dst, 0, 0, 0, 0l, 2);
				return;
			case StdTypes.T_INT:
				ins(I_DECmem, dst, 0, 0, 0, 0l, 4);
				return;
			case StdTypes.T_LONG:
				ins(I_DECmem, dst, 0, 0, 0, 0l, 8);
				return;
		}
		fatalError(ERR_UNSTYPE_GENINCDECMEM);
	}
	
	public void genLoadConstUnitContext(int dst, Object unitLoc)
	{
		long unitAddr = mem.getAddrAsLong(unitLoc, 0);
		if ((dst = getReg(1, dst, StdTypes.T_PTR, true)) == 0)
			return;
		if ((unitAddr & 0xFFFFFFFF00000000l) == 0l)
			ins(I_MOVregimm, dst & ~REX, 0, 0, (int) unitAddr);
		else
			ins(I_MOVregimmL, dst, 0, 0, 0, unitAddr, 0);
	}
	
	public void genCall(int off, int clssReg, int parSize)
	{
		if ((clssReg = getReg(1, clssReg, StdTypes.T_PTR, false)) == 0)
			return;
		if (ctx.codeStart == 0)
			ins(I_CALLmem, clssReg, 0, off);
		else
		{
			ins(I_MOVregmem, REX | R_EAX, clssReg, off);
			ins(I_LEAregmem, REX | R_EAX, REX | R_EAX, ctx.codeStart);
			ins(I_CALLreg, REX | R_EAX);
		}
	}
	
	public void genCallIndexed(int intfReg, int off, int parSize)
	{
		if ((intfReg = getReg(2, intfReg, StdTypes.T_DPTR, false)) == 0)
			return;
		ins(I_MOVregmem, R_EAX, intfReg, off); //load int
		ins(I_CDQE); //extend to long
		ins(I_MOVindexed, REX | R_EAX, REX | R_EDI); //mov rax,[rdi+rax]
		if (ctx.codeStart != 0)
			ins(I_LEAregmem, REX | R_EAX, REX | R_EAX, ctx.codeStart);
		ins(I_CALLreg, REX | R_EAX);
	}
	
	public void genCallConst(Mthd mthd, int parSize)
	{
		insPatchedCall(mthd, parSize);
	}
	
	public int genComp(int src1, int src2, int type, int cond)
	{
		if (type == StdTypes.T_FLT || type == StdTypes.T_DBL)
		{
			if (src2 != curFPUReg || src1 != curFPUReg - FPUREGINC)
			{
				errorFPU();
				return 0;
			}
			dupFPUIns = null;
			popFPUDone++;
			ins(I_FXCH);
			ins(I_FCOMIP);
			nextJumpsUnsigned = true;
			return cond;
		}
		nextJumpsUnsigned = false;
		switch (type)
		{
			case StdTypes.T_BOOL:
			case StdTypes.T_BYTE:
			case StdTypes.T_SHRT:
			case StdTypes.T_CHAR:
			case StdTypes.T_INT:
				type = StdTypes.T_INT;
				break;
			case StdTypes.T_LONG:
			case StdTypes.T_PTR:
				type = StdTypes.T_LONG;
				break;
			default:
				fatalError(ERR_UNSTYPE_GENCOMP);
				return 0;
		}
		if ((src1 = getReg(1, src1, type, false)) == 0 || (src2 = getReg(1, src2, type, false)) == 0)
			return 0;
		ins(I_CMPregreg, src1, src2);
		return cond;
	}
	
	public int genCompValToConstVal(int src, int val, int type, int cond)
	{
		int src2, restore;
		if (type == StdTypes.T_FLT)
		{ //no direct compare available
			restore = prepareFreeReg(src, 0, 0, type);
			src2 = allocReg();
			genLoadConstVal(src2, val, type);
			cond = genComp(src, src2, type, cond);
			deallocRestoreReg(src2, 0, restore);
			return cond;
		}
		nextJumpsUnsigned = false;
		if ((src = getReg(1, src, StdTypes.T_INT, false)) == 0)
			return 0;
		if (val == 0)
		{
			ins(I_ORregreg, src, src);
			return cond;
		}
		switch (type)
		{
			case StdTypes.T_BYTE:
				val = (byte) val;
				break;
			case StdTypes.T_SHRT:
				val = (short) val;
				break;
			case StdTypes.T_CHAR:
				val = (char) val;
				break;
			//default: nothing to do for other types
		}
		ins(I_CMPregimm, src, 0, 0, val);
		return cond;
	}
	
	public int genCompValToConstDoubleOrLongVal(int srcR, long val, boolean asDouble, int cond)
	{
		int src, tmpR, tmp, src2, restore;
		if (asDouble)
		{ //no direct compare available
			restore = prepareFreeReg(srcR, 0, 0, StdTypes.T_DBL);
			src2 = allocReg();
			genLoadConstDoubleOrLongVal(src2, val, true);
			cond = genComp(srcR, src2, StdTypes.T_DBL, cond);
			deallocRestoreReg(src2, 0, restore);
			return cond;
		}
		nextJumpsUnsigned = false;
		if ((src = getReg(1, srcR, StdTypes.T_LONG, false)) == 0)
			return 0;
		if (val == 0l)
			ins(I_ORregreg, src, src);
		else
		{
			tmpR = getFreeReg(srcR, StdTypes.T_LONG);
			if ((tmp = getReg(1, tmpR, StdTypes.T_LONG, false)) == 0)
				return 0;
			ins(I_MOVregimmL, tmp, 0, 0, 0, val, 0);
			ins(I_CMPregreg, src, tmp);
			deallocReg(tmpR);
		}
		return cond;
	}
	
	public void genLoadDerefAddr(int destReg, int objReg, int ind, int baseOffset, int entrySize)
	{
		int tmp, tmpRst, indReg;
		if ((destReg = getReg(1, destReg, StdTypes.T_PTR, true)) == 0 || (objReg = getReg(1, objReg, StdTypes.T_PTR, false)) == 0 || (indReg = getReg(1, ind, StdTypes.T_INT, false)) == 0)
			return;
		if (entrySize < 0)
		{
			ins(I_NEGreg, REX | indReg);
			entrySize = -entrySize;
		}
		switch (entrySize)
		{
			case 1:
			case 2:
			case 4:
			case 8:
				if (destReg != objReg)
					ins(I_MOVregreg, destReg, objReg);
				ins(I_LEAarray, destReg, indReg, baseOffset, 0, 0l, entrySize);
				break;
			default:
				tmpRst = prepareFreeReg(ind, 0, 0, StdTypes.T_INT);
				tmp = allocReg();
				genLoadConstVal(tmp, entrySize, StdTypes.T_INT);
				genBinOp(ind, ind, tmp, (Ops.S_ARI << 16) | Ops.A_MUL, StdTypes.T_INT);
				deallocRestoreReg(tmp, 0, tmpRst);
				if (destReg != objReg)
					ins(I_MOVregreg, destReg, objReg);
				if (baseOffset != 0)
					ins(I_ADDregimm, destReg, 0, 0, baseOffset, 0l, 0);
				ins(I_ADDregreg, destReg, indReg, 0, 0, 0l, 0);
		}
	}
	
	public void genMoveToPrimary(int srcR, int type)
	{
		int reg;
		switch (type)
		{
			case StdTypes.T_FLT:
			case StdTypes.T_DBL:
				if (srcR != curFPUReg)
					errorFPU();
				dupFPUIns = null;
				popFPUDone++; //do not destroy result
				return; //leave result on top of stack
			case StdTypes.T_LONG:
			case StdTypes.T_PTR:
				if ((reg = getReg(1, srcR, StdTypes.T_LONG, false)) == 0)
					return;
				if (reg != (REX | R_EAX))
					ins(I_MOVregreg, REX | R_EAX, reg);
				//else: nothing to do
				return;
			case StdTypes.T_DPTR:
				if ((reg = getReg(1, srcR, StdTypes.T_DPTR, false)) == 0)
					return;
				if (reg != (REX | R_EAX))
					ins(I_MOVregreg, REX | R_EAX, reg);
				if ((reg = getReg(2, srcR, StdTypes.T_DPTR, false)) == 0)
					return;
				if (reg != (REX | R_EDX))
					ins(I_MOVregreg, REX | R_EDX, reg);
				return;
		}
		//default
		if ((reg = getReg(1, srcR, StdTypes.T_INT, false)) == 0)
			return;
		if (reg != R_EAX)
			ins(I_MOVregreg, R_EAX, reg);
		//else: nothing to do
	}
	
	public void genMoveFromPrimary(int dstR, int type)
	{
		int reg;
		switch (type)
		{
			case StdTypes.T_FLT:
			case StdTypes.T_DBL:
				if (dstR != curFPUReg)
					errorFPU();
				return; //result already on top of stack
			case StdTypes.T_LONG:
			case StdTypes.T_PTR:
				if ((reg = getReg(1, dstR, StdTypes.T_LONG, true)) == 0)
					return;
				if (reg != (REX | R_EAX))
					ins(I_MOVregreg, reg, REX | R_EAX);
				//else: nothin to do
				return;
			case StdTypes.T_DPTR:
				if ((reg = getReg(2, dstR, StdTypes.T_DPTR, true)) == 0)
					return;
				if (reg != (REX | R_EDX))
					ins(I_MOVregreg, REX | R_EDX, reg);
				if ((reg = getReg(1, dstR, StdTypes.T_DPTR, true)) == 0)
					return;
				if (reg != (REX | R_EAX))
					ins(I_MOVregreg, REX | R_EAX, reg);
				return;
		}
		//default
		if ((reg = getReg(1, dstR, StdTypes.T_INT, true)) == 0)
			return;
		if (reg != R_EAX)
			ins(I_MOVregreg, reg, R_EAX);
		//else: nothin to do
	}
	
	public void genMoveIntfMapFromPrimary(int dst)
	{
		if ((dst = getReg(2, dst, StdTypes.T_DPTR, true)) == 0)
			return;
		ins(I_MOVregreg, dst, REX | R_EAX); //reg can not be REX|R_EAX, because it is the second one in dst
	}
	
	public void genSavePrimary(int type)
	{
		if (type == StdTypes.T_FLT || type == StdTypes.T_DBL)
		{
			return; //result already on top of FPU-stack
		}
		switch (type)
		{
			case StdTypes.T_PTR:
			case StdTypes.T_LONG:
				ins(I_PUSHreg, REX | R_EAX);
				return;
			case StdTypes.T_DPTR:
				ins(I_PUSHreg, REX | R_EAX);
				ins(I_PUSHreg, REX | R_EDX);
				return;
		}
		ins(I_PUSHreg, R_EAX);
	}
	
	public void genRestPrimary(int type)
	{
		if (type == StdTypes.T_FLT || type == StdTypes.T_DBL)
		{
			return; //result already on top of FPU-stack
		}
		switch (type)
		{
			case StdTypes.T_DPTR:
				ins(I_POPreg, REX | R_EDX);
				//no break/return, has to do the following too!
			case StdTypes.T_PTR:
			case StdTypes.T_LONG:
				ins(I_POPreg, REX | R_EAX);
				return;
		}
		ins(I_POPreg, R_EAX);
	}
	
	public void genCallNative(Object loc, int off, boolean relative, int parSize, boolean noCleanUp)
	{ //valid only for linux 64 bit
		long destAddr;
		if (relative)
			ins(I_MOVregmem, REX | R_EBX, REX | R_EDI, off);
		if (parSize > 0)
		{
			int val = parSize > 48 ? 48 : parSize;
			switch (val)
			{
				case 48:
					ins(I_MOVregmem, REX | R_9D, REX | R_ESP, 40);
				case 40:
					ins(I_MOVregmem, REX | R_8D, REX | R_ESP, 32);
				case 32:
					ins(I_MOVregmem, REX | R_ECX, REX | R_ESP, 24);
				case 24:
					ins(I_MOVregmem, REX | R_EDX, REX | R_ESP, 16);
				case 16:
					ins(I_MOVregmem, REX | R_ESI, REX | R_ESP, 8);
				case 8:
					ins(I_MOVregmem, REX | R_EDI, REX | R_ESP);
					break;
				default:
					fatalError("invalid parSize for native call");
					return;
			}
			ins(I_ADDregimm, REX | R_ESP, 0, 0, val);
			parSize -= val;
		}
		if (relative)
			ins(I_CALLreg, REX | R_EBX);
		else
		{
			if (((destAddr = mem.getAddrAsLong(loc, off)) & 0xFFFFFFFF00000000l) == 0l)
				ins(I_CALLmem, 0, 0, (int) destAddr);
			else
			{
				ins(I_MOVregimmL, REX | R_EBX, 0, 0, 0, destAddr, 0);
				ins(I_CALLreg, REX | R_EBX);
			}
		}
		if (!noCleanUp && parSize > 0)
			ins(I_ADDregimm, REX | R_ESP, 0, 0, parSize);
	}
	
	public void genReserveNativeStack(int size)
	{
		if (size > 0)
			ins(I_SUBregimm, REX | R_ESP, 0, 0, size);
	}
	
	public void genStoreNativeParameter(int offset, int src, int type)
	{
		int reg;
		
		if (type == StdTypes.T_FLT || type == StdTypes.T_DBL)
		{
			if (curFPUReg != src)
			{
				errorFPU();
				return;
			}
			dupFPUIns = ins(I_FDUP);
			ins(I_PUSHreg, R_EAX);
			ins(I_FSTPmem, REX | R_ESP, 0, 0, 0, 0l, type == StdTypes.T_DBL ? FPU64 : FPU32);
			return;
		}
		if ((reg = getReg(1, src, type, false)) == 0)
			return;
		ins(I_MOVmemreg, REX | R_ESP, reg, offset);
		if (type == StdTypes.T_DPTR)
		{
			if ((reg = getReg(2, src, StdTypes.T_DPTR, false)) == 0)
				return;
			ins(I_MOVmemreg, REX | R_ESP, reg, offset + 8);
		}
	}
	
	protected String getRegName(int reg)
	{
		switch (reg)
		{
			case REX | R_EAX:
				return "RAX";
			case REX | R_EBP:
				return "RBP";
			case REX | R_EBX:
				return "RBX";
			case REX | R_ECX:
				return "RCX";
			case REX | R_EDI:
				return "RDI";
			case REX | R_EDX:
				return "RDX";
			case REX | R_ESI:
				return "RSI";
			case REX | R_ESP:
				return "RSP";
			case R_8B:
				return "R8B";
			case R_8W:
				return "R8W";
			case R_8D:
				return "R8D";
			case REX | R_8D:
				return "R8";
			case R_9B:
				return "R9B";
			case R_9W:
				return "R9W";
			case R_9D:
				return "R9D";
			case REX | R_9D:
				return "R9";
			case R_10B:
				return "R10B";
			case R_10W:
				return "R10W";
			case R_10D:
				return "R10D";
			case REX | R_10D:
				return "R10";
			case R_11B:
				return "R11B";
			case R_11W:
				return "R11W";
			case R_11D:
				return "R11D";
			case REX | R_11D:
				return "R11";
			case R_12B:
				return "R12B";
			case R_12W:
				return "R12W";
			case R_12D:
				return "R12D";
			case REX | R_12D:
				return "R12";
			case R_13B:
				return "R13B";
			case R_13W:
				return "R13W";
			case R_13D:
				return "R13D";
			case REX | R_13D:
				return "R13";
			case R_14B:
				return "R14B";
			case R_14W:
				return "R14W";
			case R_14D:
				return "R14D";
			case REX | R_14D:
				return "R14";
			case R_15B:
				return "R15B";
			case R_15W:
				return "R15W";
			case R_15D:
				return "R15D";
			case REX | R_15D:
				return "R15";
		}
		return super.getRegName(reg);
	}
	
	//here the internal coding of the instructions takes place
	protected Instruction ins(int type, int reg0, int reg1, int disp, int imm)
	{
		return ins(type, reg0, reg1, disp, imm, 0l, 0);
	}
	
	protected void internalFixStackExtremeAdd(Instruction me, int stackCells)
	{
		boolean sizeprefix = false;
		int wordflag = 0, tmp = me.reg0 & 0x0F; //register 0 gives operation size (exception: MOVSXregmem, INC, DEC)
		if (tmp == RS_E)
			wordflag = 1; //EAX, EBX, ...
		else if (tmp == RS_X)
		{ //AX, BX, ...
			wordflag = 1;
			sizeprefix = true;
		}
		boolean rex0 = (me.reg0 & REX) != 0;
		boolean nrg0 = (me.reg0 & NRG) != 0;
		me.size = 0;
		me.type = I_ADDregimm;
		codeAriRegImm(me, 0x04, sizeprefix, wordflag, me.reg0 & ~(NRG | REX), rex0, nrg0, me.iPar2 = stackCells << 3);
	}
	
	protected Instruction ins(int type, int reg0, int reg1, int disp, int imm, long immL, int par)
	{
		Instruction i;
		int tmp;
		int wordflag = 0, prefix;
		boolean sizeprefix = false, rex0, rex1, nrg0, nrg1;
		
		//get a new instruction and insert ist
		i = getUnlinkedInstruction();
		appendInstruction(i);
		//get parameters and remember them
		i.type = type;
		i.reg0 = reg0;
		i.reg1 = reg1;
		i.iPar1 = disp;
		i.iPar2 = imm;
		i.lPar = immL;
		i.iPar3 = par;
		//check rex => 64 bit instead of 32 bit
		rex0 = (reg0 & REX) != 0;
		rex1 = (reg1 & REX) != 0;
		reg0 &= ~REX;
		reg1 &= ~REX;
		//check new registers
		nrg0 = (reg0 & NRG) != 0;
		nrg1 = (reg1 & NRG) != 0;
		reg0 &= ~NRG;
		reg1 &= ~NRG;
		//wordflag and sizeprefix for most instructions (check only, do not code)
		if ((type & IM_P0) == I_reg0 || (type & IM_P1) == I_reg1)
		{
			if ((type & IM_P0) == I_reg0)
				tmp = reg0 & 0x0F; //register 0 gives operation size (exception: MOVSXregmem, INC, DEC)
			else
				tmp = reg1 & 0x0F; //register 1 gives operation size
			if (tmp == RS_E)
				wordflag = 1; //EAX, EBX, ...
			else if (tmp == RS_X)
			{ //AX, BX, ...
				wordflag = 1;
				sizeprefix = true;
			}
			//else: AL, BL, ... have wordflag==0 and sizeprefix==false
		}
		//code instruction
		switch (type)
		{
			//standard instructions
			case I_MOVregreg:
				putPrefix(i, sizeprefix, rex0, nrg0, nrg1);
				i.putByte(0x8A | wordflag);
				putRegReg(i, reg0, reg1);
				break;
			case I_MOVregmem:
				putPrefix(i, sizeprefix, rex0, nrg0, nrg1);
				i.putByte(0x8A | wordflag);
				putMem(i, reg0, reg1, disp);
				break;
			case I_MOVmemreg:
				putPrefix(i, sizeprefix, rex1, nrg1, nrg0);
				i.putByte(0x88 | wordflag);
				putMem(i, reg1, reg0, disp); //toggle reg0/reg1 as needed for memreg-ops
				break;
			case I_MOVmemimm:
				putPrefix(i, par == 2, par == 8, false, nrg0);
				i.putByte(0xC6 | (par == 1 ? 0 : 1));
				putMem(i, 0, reg0, disp);
				if (par == 4 || par == 8)
					i.putInt(imm);
				else if (par == 2)
					i.putShort(imm);
				else
					i.putByte(imm);
				break;
			case I_MOVregimm:
				putPrefix(i, sizeprefix, rex0, false, nrg0);
				i.putByte(0xB0 | (wordflag << 3) | (reg0 >>> 4));
				putImm(i, reg0, imm);
				break;
			case I_MOVregimmL:
				putPrefix(i, false, true, false, nrg0);
				i.putByte(0xB0 | (wordflag << 3) | (reg0 >>> 4));
				i.putLong(immL);
				break;
			case I_MOVSXregreg:
				putPrefix(i, sizeprefix, rex0, nrg0, nrg1); //also handles "to 16 bit instead of 32"
				i.putByte(0x0F);
				if ((reg1 & 0x0F) == RS_X)
					i.putByte(0xBF); //from AX, BX, ...
				else
					i.putByte(0xBE); //from AL, BL, ...
				putRegReg(i, reg0, reg1);
				break;
			case I_MOVSXregmem:
				putPrefix(i, sizeprefix, rex0, nrg0, nrg1); //also handles "to 16 bit instead of 32"
				i.putByte(0x0F);
				if (par == 2)
					i.putByte(0xBF); //from 16 bit source
				else
					i.putByte(0xBE); //from 8 bit source
				putMem(i, reg0, reg1, disp);
				break;
			case I_MOVZXregreg:
				putPrefix(i, sizeprefix, rex0, nrg0, nrg1); //also handles "to 16 bit instead of 32"
				i.putByte(0x0F);
				if ((reg1 & 0x0F) == RS_X)
					i.putByte(0xB7); //from AX, BX, ...
				else
					i.putByte(0xB6); //from AL, BL, ...
				putRegReg(i, reg0, reg1);
				break;
			case I_MOVZXregmem:
				putPrefix(i, sizeprefix, rex0, nrg0, nrg1); //also handles "to 16 bit instead of 32"
				i.putByte(0x0F);
				if (par == 2)
					i.putByte(0xB7); //from 16 bit source
				else
					i.putByte(0xB6); //from 8 bit source
				putMem(i, reg0, reg1, disp);
				break;
			case I_LEAregmem:
				if (!rex1)
					i.putByte(0x67);
				putPrefix(i, sizeprefix, rex0, nrg0, nrg1);
				i.putByte(0x8D);
				putMem(i, reg0, reg1, disp);
				break;
			case I_PUSHreg:
				putPrefix(i, sizeprefix, false, false, nrg0);
				i.putByte(0x50 | (reg0 >>> 4));
				break;
			case I_PUSHimm:
				if (imm >= -128 && imm < 128)
				{ //8 bit immediate
					i.putByte(0x6A);
					i.putByte(imm);
				}
				else
				{ //32 bit immediate
					i.putByte(0x68);
					i.putInt(imm);
				}
				break;
			case I_POPreg:
				putPrefix(i, sizeprefix, false, false, nrg0);
				i.putByte(0x58 | (reg0 >>> 4));
				break;
			case I_CALLreg:
				putPrefix(i, false, false, nrg0, false);
				i.putByte(0xFF);
				i.putByte(0xD0 | (reg0 >>> 4));
				break;
			case I_CALLmem:
				putPrefix(i, false, false, nrg0, false);
				i.putByte(0xFF);
				putMem(i, 0x20, reg0, disp);
				break;
			case I_PUSHip:
				i.putByte(0xE8);
				i.putInt(0);
				break;
			case I_RETimm:
				if (imm == 0)
					i.putByte(0xC3);
				else
				{
					i.putByte(0xC2);
					i.putShort(imm);
				}
				break;
			case I_IRET:
				i.putByte(0x48);
				i.putByte(0xCF);
				break;
			case I_ADDregreg:
				putPrefix(i, sizeprefix, rex0, nrg0, nrg1);
				i.putByte(0x02 | wordflag);
				putRegReg(i, reg0, reg1);
				break;
			case I_STEXreg:
			case I_ADDregimm:
				codeAriRegImm(i, 0x04, sizeprefix, wordflag, reg0, rex0, nrg0, imm);
				break;
			case I_ADDregmem:
				putPrefix(i, sizeprefix, rex0, nrg0, nrg1);
				i.putByte(0x02 | wordflag);
				putMem(i, reg0, reg1, disp);
				break;
			case I_ADDmemreg: //only used in optimizer
				putPrefix(i, sizeprefix, rex1, nrg1, nrg0);
				i.putByte(0x00 | wordflag);
				putMem(i, reg1, reg0, disp);
				break;
			case I_ADDmemimm: //only used in optimizer
				putPrefix(i, false, par == 8, false, nrg0);
				codeAriMemImm(i, 0x00, reg0, disp, imm, par);
				break;
			case I_SUBregreg:
				putPrefix(i, sizeprefix, rex0, nrg0, nrg1);
				i.putByte(0x2A | wordflag);
				putRegReg(i, reg0, reg1);
				break;
			case I_SUBregimm:
				codeAriRegImm(i, 0x2C, sizeprefix, wordflag, reg0, rex0, nrg0, imm);
				break;
			case I_SUBregmem:
				putPrefix(i, sizeprefix, rex0, nrg0, nrg1);
				i.putByte(0x2A | wordflag);
				putMem(i, reg0, reg1, disp);
				break;
			case I_SUBmemreg:
				putPrefix(i, sizeprefix, rex1, nrg1, nrg0);
				i.putByte(0x28 | wordflag);
				putMem(i, reg1, reg0, disp);
				break;
			case I_SUBmemimm: //only used in optimizer
				putPrefix(i, false, par == 8, false, nrg0);
				codeAriMemImm(i, 0x50, reg0, disp, imm, par);
				break;
			case I_ANDregreg:
				putPrefix(i, sizeprefix, rex0, nrg0, nrg1);
				i.putByte(0x22 | wordflag);
				putRegReg(i, reg0, reg1);
				break;
			case I_ANDregimm:
				codeAriRegImm(i, 0x24, sizeprefix, wordflag, reg0, rex0, nrg0, imm);
				break;
			case I_ANDmemimm:
				putPrefix(i, false, par == 8, false, nrg0);
				codeAriMemImm(i, 0x48, reg0, disp, imm, par);
				break;
			case I_ANDmemreg: //only used in optimizer
				putPrefix(i, sizeprefix, rex1, nrg1, nrg0);
				i.putByte(0x20 | wordflag);
				putMem(i, reg1, reg0, disp);
				break;
			case I_ANDregmem: //only used in optimizer
				putPrefix(i, sizeprefix, rex0, nrg0, nrg1);
				i.putByte(0x22 | wordflag);
				putMem(i, reg0, reg1, disp);
				break;
			case I_XORregreg:
				putPrefix(i, sizeprefix, rex0, nrg0, nrg1);
				i.putByte(0x32 | wordflag);
				putRegReg(i, reg0, reg1);
				break;
			case I_XORregimm:
				codeAriRegImm(i, 0x34, sizeprefix, wordflag, reg0, rex0, nrg0, imm);
				break;
			case I_XORregmem: //only used in optimizer
				putPrefix(i, sizeprefix, rex0, nrg0, nrg1);
				i.putByte(0x32 | wordflag);
				putMem(i, reg0, reg1, disp);
				break;
			case I_XORmemreg: //only used in optimizer
				putPrefix(i, sizeprefix, rex1, nrg1, nrg0);
				i.putByte(0x30 | wordflag);
				putMem(i, reg1, reg0, disp);
				break;
			case I_XORmemimm: //only used in optimizer
				putPrefix(i, false, par == 8, false, nrg0);
				codeAriMemImm(i, 0x60, reg0, disp, imm, par);
				break;
			case I_ORregreg:
				putPrefix(i, sizeprefix, rex0, nrg0, nrg1);
				i.putByte(0x0A | wordflag);
				putRegReg(i, reg0, reg1);
				break;
			case I_ORregimm:
				codeAriRegImm(i, 0x0C, sizeprefix, wordflag, reg0, rex0, nrg0, imm);
				break;
			case I_ORmemreg:
				putPrefix(i, sizeprefix, rex1, nrg1, nrg0);
				i.putByte(0x08 | wordflag);
				putMem(i, reg1, reg0, disp);
				break;
			case I_ORregmem: //only used in optimizer
				putPrefix(i, sizeprefix, rex0, nrg0, nrg1);
				i.putByte(0x0A | wordflag);
				putMem(i, reg0, reg1, disp);
				break;
			case I_IMULreg:
				putPrefix(i, sizeprefix, rex0, false, nrg0);
				i.putByte(0xF6 | wordflag);
				i.putByte(0xE8 | (reg0 >>> 4));
				break;
			case I_IDIVreg:
				putPrefix(i, sizeprefix, rex0, false, nrg0);
				i.putByte(0xF6 | wordflag);
				i.putByte(0xF8 | (reg0 >>> 4));
				break;
			case I_INCmem:
				prefix = 0;
				if (par > 1)
				{
					wordflag = 1;
					if (par == 8)
						prefix = 0x48;
					else if (par == 2)
						i.putByte(0x66);
				}
				else
					wordflag = 0;
				if (nrg0)
					prefix |= 0x41;
				if (prefix != 0)
					i.putByte(prefix);
				i.putByte(0xFE | wordflag);
				putMem(i, 0x00, reg0, disp); //"inc reg0" is like "dummy 0, reg0"
				break;
			case I_DECmem:
				prefix = 0;
				if (par > 1)
				{
					wordflag = 1;
					if (par == 8)
						prefix = 0x48;
					else if (par == 2)
						i.putByte(0x66);
				}
				else
					wordflag = 0;
				if (nrg0)
					prefix |= 0x41;
				if (prefix != 0)
					i.putByte(prefix);
				i.putByte(0xFE | wordflag);
				putMem(i, 0x10, reg0, disp); //"dec reg0" is like "dummy 1, reg0"
				break;
			case I_NEGreg:
				putPrefix(i, sizeprefix, rex0, false, nrg0);
				i.putByte(0xF6 | wordflag);
				i.putByte(0xD8 | (reg0 >>> 4));
				break;
			case I_NOTreg:
				putPrefix(i, sizeprefix, rex0, false, nrg0);
				i.putByte(0xF6 | wordflag);
				i.putByte(0xD0 | (reg0 >>> 4));
				break;
			case I_CMPregimm:
				codeAriRegImm(i, 0x3C, sizeprefix, wordflag, reg0, rex0, nrg0, imm);
				break;
			case I_CMPregreg:
				putPrefix(i, sizeprefix, rex0, nrg0, nrg1);
				i.putByte(0x3A | wordflag);
				putRegReg(i, reg0, reg1);
				break;
			case I_CMPregmem:
				putPrefix(i, sizeprefix, rex0, nrg0, nrg1);
				i.putByte(0x3A | wordflag);
				putMem(i, reg0, reg1, disp);
				break;
			case I_CMPmemreg: //only used in optimizer
				putPrefix(i, sizeprefix, rex1, nrg1, nrg0);
				i.putByte(0x38 | wordflag);
				putMem(i, reg1, reg0, disp);
				break;
			case I_CMPmemimm: //only used in optimizer
				putPrefix(i, false, par == 8, false, nrg0);
				codeAriMemImm(i, 0x70, reg0, disp, imm, par);
				break;
			case I_XCHGregreg:
				putPrefix(i, sizeprefix, rex0, nrg0, nrg1);
				if (!nrg0 && !nrg1 && (reg0 == R_EAX || reg0 == R_AX))
				{
					i.putByte(0x90 | (reg1 >>> 4));
				}
				else if (!nrg0 && !nrg1 && (reg1 == R_EAX || reg1 == R_AX))
				{
					i.putByte(0x90 | (reg0 >>> 4));
				}
				else
				{
					i.putByte(0x86 | wordflag);
					putRegReg(i, reg0, reg1);
				}
				break;
			case I_SHLregreg:
				putPrefix(i, sizeprefix, rex0, false, nrg0);
				i.putByte(0xD2 | wordflag);
				i.putByte(0xE0 | (reg0 >>> 4));
				break;
			case I_SHLregimm:
				codeShiftRegImm(i, 0xE0, sizeprefix, wordflag, reg0, rex0, nrg0, imm);
				break;
			case I_SHLregmem:
				putPrefix(i, sizeprefix, rex0, nrg0, nrg1);
				i.putByte(0xD2 | wordflag);
				putMem(i, reg0, reg1, disp);
				break;
			case I_SHLmemreg:
				prefix = 0;
				if (par > 1)
				{
					wordflag = 1;
					if (par == 8)
						prefix = 0x48;
					else if (par == 2)
						i.putByte(0x66);
				}
				else
					wordflag = 0;
				if (nrg0)
					prefix |= 0x41;
				if (prefix != 0)
					i.putByte(prefix);
				i.putByte(0xD2 | wordflag);
				putMem(i, 0x40, reg0, disp);
				break;
			case I_SHRregreg:
				putPrefix(i, sizeprefix, rex0, false, nrg0);
				i.putByte(0xD2 | wordflag);
				i.putByte(0xE8 | (reg0 >>> 4));
				break;
			case I_SHRregmem:
				putPrefix(i, sizeprefix, rex0, nrg0, nrg1);
				i.putByte(0xD2 | wordflag);
				putMem(i, reg0, reg1, disp);
				break;
			case I_SHRmemreg:
				prefix = 0;
				if (par > 1)
				{
					wordflag = 1;
					if (par == 8)
						prefix = 0x48;
					else if (par == 2)
						i.putByte(0x66);
				}
				else
					wordflag = 0;
				if (nrg0)
					prefix |= 0x41;
				if (prefix != 0)
					i.putByte(prefix);
				i.putByte(0xD2 | wordflag);
				putMem(i, 0x50, reg0, disp);
				break;
			case I_SHRregimm:
				codeShiftRegImm(i, 0xE8, sizeprefix, wordflag, reg0, rex0, nrg0, imm);
				break;
			case I_SARregreg:
				putPrefix(i, sizeprefix, rex0, false, nrg0);
				i.putByte(0xD2 | wordflag);
				i.putByte(0xF8 | (reg0 >>> 4));
				break;
			case I_SARregimm:
				codeShiftRegImm(i, 0xF8, sizeprefix, wordflag, reg0, rex0, nrg0, imm);
				break;
			case I_SARregmem:
				putPrefix(i, sizeprefix, rex0, nrg0, nrg1);
				i.putByte(0xD2 | wordflag);
				putMem(i, reg0, reg1, disp);
				break;
			case I_SARmemreg:
				prefix = 0;
				if (par > 1)
				{
					wordflag = 1;
					if (par == 8)
						prefix = 0x48;
					else if (par == 2)
						i.putByte(0x66);
				}
				else
					wordflag = 0;
				if (nrg0)
					prefix |= 0x41;
				if (prefix != 0)
					i.putByte(prefix);
				i.putByte(0xD2 | wordflag);
				putMem(i, 0x70, reg0, disp);
				break;
			case I_CBW: //only used in optimizer
				i.putByte(0x66);
				i.putByte(0x98);
				break;
			case I_CWD: //only used in optimizer
				i.putByte(0x66);
				i.putByte(0x99);
				break;
			case I_CDQ:
				i.putByte(0x99);
				break;
			case I_CQO:
				i.putByte(0x48);
				i.putByte(0x99);
				break;
			case I_CDQE:
				i.putByte(0x48);
				i.putByte(0x98);
				break;
			case I_OUTreg:
				if (sizeprefix)
					i.putByte(0x66);
				i.putByte(0xEE | wordflag);
				break;
			case I_INreg:
				if (sizeprefix)
					i.putByte(0x66);
				i.putByte(0xEC | wordflag);
				break;
			case I_PUSHF:
				i.putByte(0x9C);
				break;
			case I_POPF:
				i.putByte(0x9D);
				break;
			//floating point instructions
			case I_FLDmem:
				i.putByte(0xD8 | par);
				putMem(i, 0x00, reg0, disp);
				break;
			case I_FSTPmem:
				i.putByte(0xD8 | par);
				putMem(i, 0x30, reg0, disp);
				break;
			case I_FILDmem:
				switch (par)
				{
					case FPU32:
						i.putByte(0xDB);
						putMem(i, 0, reg0, disp);
						break;
					case FPU64:
						i.putByte(0xDF);
						putMem(i, 0x50, reg0, disp);
						break;
				}
				break;
			case I_FISTTPmem:
				i.putByte(par == FPU32 ? 0xDB : 0xDD);
				putMem(i, 0x10, reg0, disp);
				break;
			case I_FCHS:
				i.putByte(0xD9);
				i.putByte(0xE0);
				break;
			case I_FADDP:
				i.putByte(0xDE);
				i.putByte(0xC1);
				break;
			case I_FSUBP:
				i.putByte(0xDE);
				i.putByte(0xE9);
				break;
			case I_FMULP:
				i.putByte(0xDE);
				i.putByte(0xC9);
				break;
			case I_FDIVP:
				i.putByte(0xDE);
				i.putByte(0xF9);
				break;
			case I_FDUP: //fld st0 to duplicate the stack top
				i.putByte(0xD9);
				i.putByte(0xC0);
				break;
			case I_FXCH:
				i.putByte(0xD9);
				i.putByte(0xC9);
				break;
			case I_FFREE:
				i.putByte(0xDD);
				i.putByte(0xC0);
				break;
			case I_FINCSTP:
				i.putByte(0xD9);
				i.putByte(0xF7);
				break;
			case I_FCOMIP:
				i.putByte(0xDF);
				i.putByte(0xF1);
				break;
			//special 64 bit instructions
			case I_MOVSXDregreg:
				putPrefix(i, sizeprefix, rex0, nrg0, nrg1);
				i.putByte(0x63);
				putRegReg(i, reg0, reg1);
				break;
			case I_MOVSXDregmem:
				putPrefix(i, sizeprefix, rex0, nrg0, nrg1);
				i.putByte(0x63);
				putMem(i, reg0, reg1, disp);
				break;
			//special instructions
			case I_LEAarray: //will result in lea reg0,[reg0+reg1*par+disp] (only for pointers!)
				prefix = 0x48;
				if (nrg0)
					prefix |= 0x45;
				if (nrg1)
					prefix |= 0x42;
				i.putByte(prefix);
				i.putByte(0x8D);
				putRegMemInx(i, reg0, reg0, reg1, par, disp);
				break;
			case I_MOVindexed: //will result in mov reg0,[reg0+reg1] (only for pointers!)
				prefix = 0x48;
				if (nrg0)
					prefix |= 0x46;
				if (nrg1)
					prefix |= 0x41;
				i.putByte(prefix);
				i.putByte(0x8B);
				putRegMemInx(i, reg0, reg0, reg1, 0, 0);
				break;
			case I_BOUNDEXC:
				i.putByte(0xCD); //INT 0x05
				i.putByte(0x05);
				break;
			case I_RETMSEXC:
				i.putByte(0xCD); //INT 0x1F, marked as "do not use"
				i.putByte(0x1F);
				break;
			case I_MARKER:
				i.putByte(0x90);
				break;
			default:
				fatalError(ERR_INVINS_CODE);
				return null;
		}
		//everything ok
		return i;
	}
	
	private void codeAriRegImm(Instruction i, int code, boolean sizeprefix, int wordflag, int reg0, boolean rex0, boolean nrg0, int imm)
	{
		int sizeExt = 0;
		
		putPrefix(i, sizeprefix, rex0, false, nrg0);
		if (wordflag != 0 && imm >= -128 && imm < 128)
			sizeExt = 2;
		//else sizeExt=0; already initialized
		if (sizeExt == 0 && (reg0 == R_EAX || reg0 == R_AX || reg0 == R_AL) && !nrg0)
		{
			i.putByte(code | wordflag);
			putImm(i, reg0, imm);
		}
		else
		{
			i.putByte(0x80 | sizeExt | wordflag);
			i.putByte((0xBC + code) | (reg0 >>> 4));
			if (sizeExt != 0)
				i.putByte(imm);
			else
				putImm(i, reg0, imm);
		}
	}
	
	private void codeAriMemImm(Instruction i, int code, int reg0, int disp, int imm, int par)
	{
		if (par > 1)
		{ //word or dword
			if (par == 2)
				i.putByte(0x66);
			if (imm >= -128 && imm < 127)
			{
				i.putByte(0x83);
				putMem(i, code, reg0, disp);
				i.putByte(imm);
			}
			else
			{
				i.putByte(0x81);
				putMem(i, code, reg0, disp);
				if (par == 2)
					i.putShort(imm);
				else
					i.putInt(imm);
			}
		}
		else
		{ //byte
			i.putByte(0x80);
			putMem(i, code, reg0, disp);
			i.putByte(imm);
		}
	}
	
	private void codeShiftRegImm(Instruction i, int code, boolean sizeprefix, int wordflag, int reg0, boolean rex0, boolean nrg0, int imm)
	{
		putPrefix(i, sizeprefix, rex0, nrg0, false);
		if (imm == 1)
		{
			i.putByte(0xD0 | wordflag);
			i.putByte(code | (reg0 >>> 4));
		}
		else
		{
			i.putByte(0xC0 | wordflag);
			i.putByte(code | (reg0 >>> 4));
			i.putByte(imm);
		}
	}
	
	private void putPrefix(Instruction i, boolean sizeprefix, boolean rex, boolean nrg0, boolean nrg1)
	{
		int prefix = 0;
		if (rex)
			prefix = 0x48;
		if (nrg1)
			prefix |= 0x41;
		if (nrg0)
			prefix |= 0x44;
		if (sizeprefix)
			i.putByte(0x66);
		if (prefix != 0)
			i.putByte(prefix);
	}
	
	private void putRegReg(Instruction i, int reg0, int reg1)
	{
		i.putByte(0xC0 | ((reg0 >>> 4) << 3) | (reg1 >>> 4));
	}
	
	private void putMem(Instruction i, int reg0, int reg1, int disp)
	{
		int reg1ex;
		
		if (reg1 == 0)
		{ //absolute
			i.putByte(0x04 | ((reg0 >>> 4) << 3)); //escape to two bytes, as
			i.putByte(0x25); //single byte absolute is relative to RIP in long mode
			i.putInt(disp);
		}
		else
		{ //relative to register
			reg1ex = reg1 | 0x7; //map to 32-bit e*-register
			if (reg1ex == R_ESP)
			{ //2-byte-address-encoding for esp
				putRegMemInx(i, reg0, reg1, R_ESP, 1, disp);
			}
			else if (disp == 0 && reg1ex != R_EBP)
			{ //no displacement
				i.putByte(((reg0 >>> 4) << 3) | (reg1 >>> 4));
			}
			else if (disp >= -128 && disp < 128)
			{ //8 bit displacement
				i.putByte(0x40 | ((reg0 >>> 4) << 3) | (reg1 >>> 4));
				i.putByte(disp);
			}
			else
			{ //32 bit displacement
				i.putByte(0x80 | ((reg0 >>> 4) << 3) | (reg1 >>> 4));
				i.putInt(disp);
			}
		}
	}
	
	private void putImm(Instruction i, int reg0, int imm)
	{
		switch (reg0 & 0xF)
		{
			case 0x1:
			case 0x2:
				i.putByte(imm);
				break;
			case 0x3:
				i.putShort(imm);
				break;
			case 0x7:
				i.putInt(imm);
				break;
			default:
				fatalError("AMD64.putImm with invalid reg0");
				return;
		}
	}
	
	private void putRegMemInx(Instruction i, int reg0, int reg1, int reg2, int scale, int disp)
	{
		int scaleBits = 0, reg1ex;
		
		switch (scale)
		{
			case 0:
			case 1:
				break; //ok, scaleBits==0
			case 2:
				scaleBits = 0x40;
				break; //ok, scaleBits for *2
			case 4:
				scaleBits = 0x80;
				break; //ok, scaleBits for *4
			case 8:
				scaleBits = 0xC0;
				break; //ok, scaleBits for *8
			default:
				fatalError("AMD64.putRegMemInx with invalid scale");
				return;
		}
		if (reg1 == 0)
		{ //absolute
			i.putByte(0x04 | ((reg0 >>> 4) << 3)); //escape to two bytes
			i.putByte(0x05 | scaleBits | ((reg2 >>> 4) << 3));
			i.putInt(disp);
		}
		else
		{ //relative to register
			reg1ex = reg1 | 0x7; //map to 32-bit e*-register
			if (disp == 0 && reg1ex != R_EBP)
			{ //no displacement
				i.putByte(0x04 | ((reg0 >>> 4) << 3)); //escape to two bytes
				i.putByte(scaleBits | ((reg2 >>> 4) << 3) | (reg1 >>> 4));
			}
			else if (disp >= -128 && disp < 128)
			{ //8 bit displacement
				i.putByte(0x44 | ((reg0 >>> 4) << 3)); //escape to two bytes
				i.putByte(scaleBits | ((reg2 >>> 4) << 3) | (reg1 >>> 4));
				i.putByte(disp);
			}
			else
			{ //32 bit displacement
				i.putByte(0x84 | ((reg0 >>> 4) << 3)); //escape to two bytes
				i.putByte(scaleBits | ((reg2 >>> 4) << 3) | (reg1 >>> 4));
				i.putInt(disp);
			}
		}
	}
}
