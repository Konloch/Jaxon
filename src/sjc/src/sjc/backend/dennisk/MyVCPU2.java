/* Copyright (C) 2016, 2017, 2018, 2019 Stefan Frenz
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
package sjc.backend.dennisk;

import sjc.backend.Architecture;
import sjc.backend.Instruction;
import sjc.compbase.*;
import sjc.osio.TextBuffer;
import sjc.osio.TextPrinter;

/**
 * MyVCPU2: Architecture backend for vCPU2 on MyCPU by Dennis Kuschel
 *
 * @author S. Frenz, B. Hofmann
 * @version 190322 fixed ex-/implicit conversion
 * version 190307 (Benedikt) added genCheckBounds
 * version 181018 fixed long register allocation and STDror instruction
 * version 181016 fixed push null, fixed pop order
 * version 170101 some more working
 * version 161222 some more working
 * version 161129 some more working
 * version 161117 replaced inc/dec by add/sub where possible
 * version 161003 added support for referenced memory locations
 * version 161002 some more working
 * version 160922 some more working
 * version 160917 some more working
 * version 160909 some more working
 * version 160818 some more working
 * version 160804 fixed dest.isDest for jumps to be set
 * version 160526 beautified register concept, some more working
 * version 160522 some more working
 * version 160328 some more working
 * version 160224 initial version
 */
public class MyVCPU2 extends Architecture
{
	//basic concept:
	//  register r0 to r7 are used as 16 bit registers, two consecutive registers are used as 32 bit register
	//           p0 contains the current class         (R08+09)
	//           p1 contains the current instance      (R10+11)
	//           p2 is a temporary pointer             (R12+13)
	//           sp contains the stack pointer         (R14+15)
	
	//special "register"
	public final static int RegBaseMark = 0xAFFECAFE; //this mask should never occur
	
	//definitions of memory mapped registers
	public final static int RegMax = 15;
	public final static int RegMaxGP = 7;
	
	public final static int R_D0_LO = 0;
	public final static int R_D0_HI = 1;
	public final static int R_D1_LO = 2;
	public final static int R_D1_HI = 3;
	public final static int R_D2_LO = 4;
	public final static int R_D2_HI = 5;
	public final static int R_D3_LO = 6;
	public final static int R_D3_HI = 7;
	public final static int R_CLSS_LO = 8; //must be directly before inst to allow simplified push/pop
	public final static int R_CLSS_HI = 9;
	public final static int R_INST_LO = 10; //must be directly after clss to allow simplified push/pop
	public final static int R_INST_HI = 11;
	public final static int R_TMP_LO = 12;
	public final static int R_TMP_HI = 13;
	public final static int R_STACK_LO = 14;
	public final static int R_STACK_HI = 15;
	
	public final static int R_D0_32 = R_D0_LO;
	public final static int R_D1_32 = R_D1_LO;
	public final static int R_D2_32 = R_D2_LO;
	public final static int R_D3_32 = R_D3_LO;
	public final static int R_CLSS_32 = R_CLSS_LO;
	public final static int R_INST_32 = R_INST_LO;
	public final static int R_TMP_32 = R_TMP_LO;
	public final static int R_STACK_32 = R_STACK_LO;
	
	public final static int RegClss = (1 << R_CLSS_LO) | (1 << R_CLSS_HI);
	public final static int RegInst = (1 << R_INST_LO) | (1 << R_INST_HI);
	public final static int RegMaskGP = (1 << (RegMaxGP + 1)) - 1;
	
	public final static int PAR_P1M = 0x000F0000;
	public final static int PAR_REG1 = 0x00010000; //register
	public final static int PAR_IMM1 = 0x00020000; //immediate
	public final static int PAR_ADR1 = 0x00030000; //address in register
	public final static int PAR_AOF1 = 0x00040000; //address in register plus offset
	public final static int PAR_AAD1 = 0x00050000; //dereferenced immediate address
	public final static int PAR_RMA1 = 0x000D0000; //load of immediate address (not dereferenced)
	public final static int PAR_JDT1 = 0x000E0000; //jump destination
	public final static int PAR_CDT1 = 0x000F0000; //call destination
	public final static int PAR_P2M = 0x00F00000;
	public final static int PAR_REG2 = 0x00100000;
	public final static int PAR_IMM2 = 0x00200000;
	public final static int PAR_ADR2 = 0x00300000;
	public final static int PAR_AOF2 = 0x00400000;
	public final static int PAR_AAD2 = 0x00500000;
	public final static int PAR_RMA2 = 0x00D00000;
	public final static int PAR_JDT2 = 0x00E00000;
	public final static int PAR_CDT2 = 0x00F00000;
	public final static int SIZE_MASK = 0xF0000000;
	public final static int SIZE_SHIFT = 28;
	public final static int SIZE_1 = 0x10000000;
	public final static int SIZE_2 = 0x20000000;
	public final static int SIZE_4 = 0x40000000;
	
	public final static int I_RET = 0x01 | SIZE_2;                       //return from subroutine
	public final static int I_CLRC = 0x08 | SIZE_2;                       //clear carry flag
	public final static int I_SETC = 0x09 | SIZE_2;                       //set carry flag
	public final static int I_JUMPabs = 0x10 | PAR_JDT1 | SIZE_4;            //jump to address
	public final static int I_CALLabs = 0x11 | PAR_CDT1 | SIZE_4;            //call with absolute address
	public final static int I_JPZra = 0x13 | PAR_REG1 | PAR_JDT2 | SIZE_4; //jump to address if register is zero
	public final static int I_JPNZra = 0x14 | PAR_REG1 | PAR_JDT2 | SIZE_4; //jump to address if register is not zero
	public final static int I_SEXTri = 0x15 | PAR_REG1 | PAR_IMM2 | SIZE_4; //sign-extend 32 bit value from 8-/16-bit value
	public final static int I_TSTri = 0x16 | PAR_REG1 | PAR_IMM2 | SIZE_4; //test register with mask
	public final static int I_LDPra = 0x17 | PAR_REG1 | PAR_RMA2 | SIZE_4; //load pointer to memory
	public final static int I_JPZabs = 0x18 | PAR_JDT1 | SIZE_4;            //jump to address if "equal" (ZF=1)
	public final static int I_JPNZabs = 0x19 | PAR_JDT1 | SIZE_4;            //jump to address if "not equal" (ZF=0)
	public final static int I_JPGRabs = 0x1C | PAR_JDT1 | SIZE_4;            //jump to address if "greater" (CF=1 && ZF=0)
	public final static int I_JPLEabs = 0x1D | PAR_JDT1 | SIZE_4;            //jump to address if "less" (CF=0 && ZF=1)
	public final static int I_JPEGabs = 0x1E | PAR_JDT1 | SIZE_4;            //jump to address if "equal or greater" (CF=1 || ZF=1)
	public final static int I_JPELabs = 0x1F | PAR_JDT1 | SIZE_4;            //jump to address if "equal or less" (CF=0 || ZF=1)
	public final static int I_CALLr = 0x21 | PAR_REG1 | SIZE_2;            //call with address in register
	public final static int I_NOTr = 0x22 | PAR_REG1 | SIZE_2;            //one's-complement register
	public final static int I_TSTDr = 0x29 | PAR_REG1 | SIZE_2;            //test if 32 bit register is zero
	public final static int I_ASRDr = 0x2B | PAR_REG1 | SIZE_2;            //arithmetical shift right 32 bit register by one
	public final static int I_LDBra = 0x30 | PAR_REG1 | PAR_AAD2 | SIZE_4; //load byte value from absolute memory
	public final static int I_LDWra = 0x31 | PAR_REG1 | PAR_AAD2 | SIZE_4; //load short value from absolute memory
	public final static int I_LDDra = 0x34 | PAR_REG1 | PAR_AAD2 | SIZE_4; //load int value from absolute memory
	public final static int I_SFTLri = 0x38 | PAR_REG1 | PAR_IMM2 | SIZE_2; //shift left register by given immediate
	public final static int I_SFTRri = 0x39 | PAR_REG1 | PAR_IMM2 | SIZE_2; //shift right register by given immediate
	public final static int I_ROTLri = 0x3A | PAR_REG1 | PAR_IMM2 | SIZE_2; //rotate left register by given immediate
	public final static int I_ROTRri = 0x3B | PAR_REG1 | PAR_IMM2 | SIZE_2; //rotate right register by given immediate
	public final static int I_INCri = 0x3C | PAR_REG1 | PAR_IMM2 | SIZE_2; //increase register by given immediate (inc: 1..15)
	public final static int I_DECri = 0x3D | PAR_REG1 | PAR_IMM2 | SIZE_2; //decrease register by given immediate (dec: 1..15)
	public final static int I_INCDri = 0x3E | PAR_REG1 | PAR_IMM2 | SIZE_2; //increase 32-bit register by given immediate (inc: 1..15)
	public final static int I_DECDri = 0x3F | PAR_REG1 | PAR_IMM2 | SIZE_2; //decrease 32-bit register by given immediate (dec: 1..15)
	public final static int I_MOVrr = 0x40 | PAR_REG1 | PAR_REG2 | SIZE_2; //copy register
	public final static int I_PUSHrr = 0x43 | PAR_REG1 | PAR_REG2 | SIZE_2; //push content of register range
	public final static int I_POPrr = 0x44 | PAR_REG1 | PAR_REG2 | SIZE_2; //pop content of register range
	public final static int I_MOVDrr = 0x45 | PAR_REG1 | PAR_REG2 | SIZE_2; //copy two registers to another two registers (32 bit)
	public final static int I_MULDrr = 0x4A | PAR_REG1 | PAR_REG2 | SIZE_2; //binary 32 bit multiplication
	public final static int I_DIVDrr = 0x4C | PAR_REG1 | PAR_REG2 | SIZE_2; //binary 32 bit division
	public final static int I_MODDrr = 0x4E | PAR_REG1 | PAR_REG2 | SIZE_2; //binary 32 bit modulo
	public final static int I_ANDrr = 0x50 | PAR_REG1 | PAR_REG2 | SIZE_2; //binary and of two registers
	public final static int I_ORrr = 0x51 | PAR_REG1 | PAR_REG2 | SIZE_2; //binary or of two registers
	public final static int I_XORrr = 0x52 | PAR_REG1 | PAR_REG2 | SIZE_2; //binary xor of two registers
	public final static int I_ADCrr = 0x53 | PAR_REG1 | PAR_REG2 | SIZE_2; //binary add with carry of two registers
	public final static int I_SBCrr = 0x54 | PAR_REG1 | PAR_REG2 | SIZE_2; //binary subtract with carry of two registers
	public final static int I_CMPUrr = 0x55 | PAR_REG1 | PAR_REG2 | SIZE_2; //compare two unsigned registers
	public final static int I_CMPSrr = 0x56 | PAR_REG1 | PAR_REG2 | SIZE_2; //compare two signed registers
	public final static int I_CMPUDrr = 0x5D | PAR_REG1 | PAR_REG2 | SIZE_2; //compare two 32 bit unsigned registers
	public final static int I_CMPSDrr = 0x5E | PAR_REG1 | PAR_REG2 | SIZE_2; //compare two 32 bit signed registers
	public final static int I_ADDDrr = 0x4D | PAR_REG1 | PAR_REG2 | SIZE_2; //add two 32 bit registers without carry
	public final static int I_ANDDrr = 0x58 | PAR_REG1 | PAR_REG2 | SIZE_2; //and two 32 bit registers
	public final static int I_ORDrr = 0x59 | PAR_REG1 | PAR_REG2 | SIZE_2; //or two 32 bit registers
	public final static int I_XORDrr = 0x5A | PAR_REG1 | PAR_REG2 | SIZE_2; //xor two 32 bit registers
	public final static int I_SBCDrr = 0x5C | PAR_REG1 | PAR_REG2 | SIZE_2; //subtract two 32 bit registers with carry
	public final static int I_LDBrr = 0x60 | PAR_REG1 | PAR_ADR2 | SIZE_2; //load byte value from reg-memory
	public final static int I_LDWrr = 0x61 | PAR_REG1 | PAR_ADR2 | SIZE_2; //load short value from reg-memory
	public final static int I_STBrr = 0x62 | PAR_REG1 | PAR_ADR2 | SIZE_2; //store byte value to reg-memory
	public final static int I_STWrr = 0x63 | PAR_REG1 | PAR_ADR2 | SIZE_2; //store short value to reg-memory
	public final static int I_LDDrr = 0x64 | PAR_REG1 | PAR_ADR2 | SIZE_2; //load int value from reg-memory
	public final static int I_STDrr = 0x65 | PAR_REG1 | PAR_ADR2 | SIZE_2; //store int value to reg-memory
	public final static int I_LDBrro = 0x68 | PAR_REG1 | PAR_AOF2 | SIZE_4; //load byte value from reg-off-memory
	public final static int I_LDWrro = 0x69 | PAR_REG1 | PAR_AOF2 | SIZE_4; //load short value from reg-off-memory
	public final static int I_LDDrro = 0x6C | PAR_REG1 | PAR_AOF2 | SIZE_4; //load int value from reg-off-memory
	public final static int I_STDror = 0x6D | PAR_REG1 | PAR_AOF2 | SIZE_4; //store int value to reg-off-memory
	public final static int I_ADDDri = 0x6E | PAR_REG1 | PAR_IMM2 | SIZE_4; //add immediate to 32 bit register
	public final static int I_SUBDri = 0x6F | PAR_REG1 | PAR_IMM2 | SIZE_4; //subtract immediate from 32 bit register
	public final static int I_ANDri = 0x70 | PAR_REG1 | PAR_IMM2 | SIZE_4; //and immediate to register
	public final static int I_ORri = 0x71 | PAR_REG1 | PAR_IMM2 | SIZE_4; //or immediate to register
	public final static int I_XORri = 0x72 | PAR_REG1 | PAR_IMM2 | SIZE_4; //xor immediate to register
	public final static int I_ADCri = 0x73 | PAR_REG1 | PAR_IMM2 | SIZE_4; //add immediate to register with carry
	public final static int I_SBCri = 0x74 | PAR_REG1 | PAR_IMM2 | SIZE_4; //subtract immediate from register with carry
	public final static int I_CMPUri = 0x75 | PAR_REG1 | PAR_IMM2 | SIZE_4; //compare immediate to unsigned register
	public final static int I_CMPSri = 0x76 | PAR_REG1 | PAR_IMM2 | SIZE_4; //compare immediate to signed register
	public final static int I_LDri = 0x77 | PAR_REG1 | PAR_IMM2 | SIZE_4; //load immediate value to register
	public final static int I_ADDri = 0x78 | PAR_REG1 | PAR_IMM2 | SIZE_4; //add immediate to register
	public final static int I_SUBri = 0x79 | PAR_REG1 | PAR_IMM2 | SIZE_4; //subtract immediate from register
	
	protected final static String ERR_INVGLOBADDRREG = "invalid register for address";
	protected final static String ERR_INVREGUSE = "invalid register use";
	
	private Mthd mthdContainer;
	private int usedRegs, writtenRegs, nextAllocReg, curVarOffParam;
	private TextBuffer asmTmpTextBuffer;
	
	//initialization
	public MyVCPU2()
	{
		supportsAsmTextInline = true;
		relocBytes = 4;
		stackClearBits = 1;
		allocClearBits = 3;
		maxInstrCodeSize = 4;
		throwFrameSize = 12;
		throwFrameExcOff = 10;
		regClss = RegClss;
		regInst = RegInst;
		regBase = RegBaseMark;
		binAriCall[StdTypes.T_BYTE] |= (1 << (Ops.A_MOD - Ops.MSKBSE));
		binAriCall[StdTypes.T_SHRT] |= (1 << (Ops.A_MOD - Ops.MSKBSE));
		//binAriCall[StdTypes.T_INT]|=;
		binAriCall[StdTypes.T_LONG] |= (1 << Ops.A_MUL - Ops.MSKBSE) | (1 << (Ops.A_DIV - Ops.MSKBSE)) | (1 << (Ops.A_MOD - Ops.MSKBSE));
		binAriCall[StdTypes.T_FLT] |= (1 << (Ops.A_MUL - Ops.MSKBSE)) | (1 << (Ops.A_DIV - Ops.MSKBSE)) | (1 << (Ops.A_MOD - Ops.MSKBSE)) | (1 << (Ops.A_PLUS - Ops.MSKBSE)) | (1 << (Ops.A_MINUS - Ops.MSKBSE));
		binAriCall[StdTypes.T_DBL] |= (1 << (Ops.A_MUL - Ops.MSKBSE)) | (1 << (Ops.A_DIV - Ops.MSKBSE)) | (1 << (Ops.A_MOD - Ops.MSKBSE)) | (1 << (Ops.A_PLUS - Ops.MSKBSE)) | (1 << (Ops.A_MINUS - Ops.MSKBSE));
		unaAriCall[StdTypes.T_FLT] |= (1 << (Ops.A_MINUS - Ops.MSKBSE));
		unaAriCall[StdTypes.T_DBL] |= (1 << (Ops.A_MINUS - Ops.MSKBSE));
	}
	
	public boolean mayInline()
	{ //MyVCPU2 does not support inlining
		return false;
	}
	
	public String checkBuildAssembler(Context preInitCtx)
	{
		asmTmpTextBuffer = new TextBuffer();
		return "vCPU2";
	}
	
	protected void attachMethodAssemblerText(Mthd generatingMthd, Instruction first)
	{
		printCode(asmTmpTextBuffer, first, null, true);
		generatingMthd.asmCode = asmTmpTextBuffer.toString();
		asmTmpTextBuffer.reset();
	}
	
	//general memory and register management
	public void putRef(Object loc, int offset, Object ptr, int ptrOff)
	{
		mem.putInt(loc, offset, mem.getAddrAsInt(ptr, ptrOff));
	}
	
	public void putCodeRef(Object loc, int offset, Object ptr, int ptrOff)
	{
		mem.putInt(loc, offset, mem.getAddrAsInt(ptr, ptrOff));
	}
	
	private int bitSearch(int value, int hit)
	{
		int i, j;
		for (i = 0; i <= RegMax; i++)
		{
			j = 1 << i;
			if ((value & j) != 0)
			{
				if (--hit == 0)
					return j;
			}
		}
		//nothing found, sorry
		return 0;
	}
	
	private int doubleBitSearch(int value)
	{
		int i, j;
		for (i = 0; i < RegMaxGP - 1; i++)
		{
			j = 3 << i;
			if ((value & j) == j)
				return j;
		}
		return 0;
	}
	
	private int getBitPos(int reg)
	{
		int i, j;
		for (i = 0; i < 32; i++)
		{
			j = 1 << i;
			if ((reg & j) != 0)
				return i;
		}
		return -1;
	}
	
	private int freeRegSearch(int mask, int type)
	{
		int reg, regd;
		if ((type == StdTypes.T_BYTE) || (type == StdTypes.T_BOOL) || (type == StdTypes.T_SHRT) || (type == StdTypes.T_CHAR))
		{ //need one register
			return bitSearch(mask, 1);
		}
		//need two or four registers
		reg = doubleBitSearch(mask);
		if ((type == StdTypes.T_LONG) || (type == StdTypes.T_DBL))
		{ //need four registers
			if ((regd = doubleBitSearch(mask & ~reg)) == 0)
				return 0;
			reg |= regd;
		}
		return reg;
	}
	
	protected int storeReg(int regs)
	{
		regs &= usedRegs & writtenRegs;
		for (int i = 0; i <= RegMaxGP; i++)
			if ((regs & (1 << i)) != 0)
			{
				int start = i;
				while (++i <= RegMaxGP && (regs & (1 << i)) != 0)
					;
				insPush(start, i - 1);
			}
		return regs;
	}
	
	protected void restoreReg(int regs)
	{
		usedRegs |= regs;
		writtenRegs |= regs;
		for (int i = RegMaxGP; i >= 0; i--)
			if ((regs & (1 << i)) != 0)
			{
				int end = i;
				while (--i >= 0 && (regs & (1 << i)) != 0)
					;
				insPop(i + 1, end);
			}
	}
	
	public int ensureFreeRegs(int ignoreReg1, int ignoreReg2, int keepReg1, int keepReg2)
	{
		int restore = storeReg(RegMaskGP & ~(ignoreReg1 | ignoreReg2));
		usedRegs = (keepReg1 | keepReg2) & RegMaskGP;
		return restore;
	}
	
	public int prepareFreeReg(int avoidReg1, int avoidReg2, int reUseReg, int type)
	{
		int toStore, ret;
		//basereg can not be allocated
		if (avoidReg1 == regBase)
			avoidReg1 = 0;
		if (avoidReg2 == regBase)
			avoidReg2 = 0;
		if (reUseReg == regBase)
			reUseReg = 0;
		//first: try to reuse given regs
		reUseReg &= RegMaskGP;
		if (reUseReg != 0)
		{
			if ((ret = freeRegSearch(reUseReg, type)) != 0)
			{
				usedRegs |= (nextAllocReg = ret);
				return 0; //nothing has to be freed, reuse given registers
			}
		}
		//second: try to alloc everywhere normally
		if ((ret = freeRegSearch((RegMaskGP & ~usedRegs & ~(avoidReg1 | avoidReg2)) | reUseReg, type)) != 0)
		{
			usedRegs |= (nextAllocReg = ret);
			return 0; //nothing has to be freed, use newly allocated registers
		}
		//third: try to free a register
		if ((ret = freeRegSearch((RegMaskGP & ~(avoidReg1 | avoidReg2)) | reUseReg, type)) != 0)
		{
			toStore = storeReg(ret);
			deallocReg(toStore);
			usedRegs |= (nextAllocReg = ret);
			return toStore;
		}
		//no possibility found to free registers
		fatalError("unsolvable register request");
		return 0;
	}
	
	protected void deallocReg(int regs)
	{
		usedRegs &= ~regs;
		writtenRegs &= ~regs;
	}
	
	public int allocReg()
	{
		return nextAllocReg;
	}
	
	public void deallocRestoreReg(int deallocRegs, int keepRegs, int restore)
	{
		deallocReg(deallocRegs & ~(keepRegs | restore));
		restoreReg(restore);
		usedRegs |= (keepRegs | restore) & RegMaskGP;
	}
	
	private int getReg(int pubReg, int word, boolean forWrite)
	{
		if (pubReg == regBase)
		{
			fatalError("invalid use of BaseReg");
			return -1;
		}
		int reg = bitSearch(pubReg, word);
		if (reg != 0)
		{
			if (forWrite)
			{
				writtenRegs |= reg;
				usedRegs |= reg;
			}
			return getBitPos(reg);
		}
		fatalError("register not found");
		return -1;
	}
	
	private boolean checkConsecReg(int r1, int r2)
	{
		if (r1 + 1 == r2)
			return true;
		fatalError("32 bit value is not in two consecutive registers");
		return false;
	}
	
	private int getRegCount(int type)
	{
		switch (type)
		{
			case StdTypes.T_BOOL:
			case StdTypes.T_BYTE:
			case StdTypes.T_SHRT:
			case StdTypes.T_CHAR:
				return 1;
			case StdTypes.T_INT:
			case StdTypes.T_FLT:
			case StdTypes.T_PTR:
				return 2;
			case StdTypes.T_LONG:
			case StdTypes.T_DBL:
			case StdTypes.T_DPTR:
				return 4;
		}
		fatalError("invalid reg count type");
		return 0;
	}
	
	//method start, end and finalization
	public Mthd prepareMethodCoding(Mthd mthd)
	{
		writtenRegs = usedRegs = 0;
		curVarOffParam = 0;
		mthdContainer = curMthd = mthd;
		return null;
	}
	
	public void codeProlog()
	{
		Instruction loopDest;
		int i;
		if (curMthd.parSize + curMthd.varSize > 253)
		{
			fatalError("method has more than 253 bytes var+par on stack");
			return;
		}
		if ((curMthd.marker & Marks.K_INTR) != 0)
		{
			if (curMthd.parSize != 0)
			{
				fatalError("interrupt method can not have parameters");
				return;
			}
			fatalError("interrupt methods not supported yet in codeProlog");
		}
		if (curMthd.varSize > 0)
		{
			ins(I_XORrr, R_TMP_LO, R_TMP_LO);
			if (curMthd.varSize <= 8)
				for (i = 0; i < curMthd.varSize; i += 2)
					ins(I_PUSHrr, R_TMP_LO, R_TMP_LO);
			else
			{
				ins(I_LDri, R_TMP_HI, curMthd.varSize / 2);
				appendInstruction(loopDest = getUnlinkedInstruction());
				ins(I_PUSHrr, R_TMP_LO, R_TMP_LO);
				ins(I_DECri, R_TMP_HI, 1);
				insJmp(I_JPNZra, R_TMP_HI, loopDest);
			}
			curVarOffParam += (curMthd.varSize + 1) & ~1;
		}
	}
	
	public void codeEpilog(Mthd outline)
	{
		int stackRed = curMthd.varSize + curMthd.parSize;
		if (stackRed != 0)
		{
			if (stackRed <= 0xFFFF)
				ins(I_ADDDri, R_STACK_32, stackRed);
			else
			{
				ins(I_LDri, R_TMP_LO, stackRed & 0xFFFF);
				ins(I_LDri, R_TMP_HI, stackRed >>> 16);
				ins(I_ADDDrr, R_STACK_32, R_TMP_32);
			}
		}
		if ((curMthd.marker & Marks.K_INTR) != 0)
		{
			fatalError("interrupt methods not supported yet in codeEpilog");
			curMthd = null;
		}
		else
		{
			ins(I_RET);
			curMthd = null;
		}
	}
	
	public void finalizeInstructions(Instruction first)
	{
		//nothing to do, all instructions are ready to be copied
		//print disassembly if wanted
		if (ctx.printCode || (mthdContainer.marker & Marks.K_PRCD) != 0)
			printCode(ctx.out, first, "MyVCPU2", false);
	}
	
	//basic architecture implementation
	public void genLoadConstVal(int dst, int val, int type)
	{
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
		}
		int reg;
		if ((reg = getReg(dst, 1, true)) == -1)
			return;
		ins(I_LDri, reg, val & 0xFFFF);
		if (type == StdTypes.T_INT || type == StdTypes.T_FLT)
		{
			if ((reg = getReg(dst, 2, true)) == -1)
				return;
			ins(I_LDri, reg, val >>> 16);
		}
	}
	
	public void genLoadConstDoubleOrLongVal(int dst, long val, boolean asDouble)
	{
		int reg, i;
		for (i = 1; i <= 4; i++)
		{
			if ((reg = getReg(dst, i, true)) == -1)
				return;
			ins(I_LDri, reg, ((int) val) & 0xFFFF);
			val = val >>> 16;
		}
	}
	
	public void genLoadVarAddr(int dst, int src, Object loc, int off)
	{
		int dst1, dst2;
		if ((dst1 = getReg(dst, 1, true)) == -1 || (dst2 = getReg(dst, 2, true)) == -1 || !checkConsecReg(dst1, dst2))
			return;
		if (loc != null)
		{
			if (src != 0)
			{
				fatalError("invalid src/loc-combination in genLoadVarAddr");
				return;
			}
			int pos = mem.getAddrAsInt(loc, off);
			ins(I_LDPra, dst1, pos);
		}
		else if (src == 0)
			ins(I_LDPra, dst1, off);
		else
		{
			if (off == 0 && src == dst)
				return;
			if (src == regBase)
			{
				off += curVarOffParam;
				ins(I_MOVDrr, dst1, R_STACK_LO);
			}
			else if (dst != src)
			{
				int src1, src2;
				if ((src1 = getReg(src, 1, false)) == -1 || (src2 = getReg(src, 2, false)) == -1 || !checkConsecReg(src1, src2))
					return;
				ins(I_MOVDrr, dst1, src1);
			}
			if (off != 0)
			{
				if (off > 0 && off <= 65535)
					ins(I_ADDDri, dst1, off);
				else if (off < 0 && off >= -65535)
					ins(I_SUBDri, dst1, -off);
				else
				{
					ins(I_ADDri, dst1, off & 0xFFFF);
					ins(I_ADCri, dst2, off >>> 16);
				}
			}
		}
	}
	
	public void genLoadVarVal(int dst, int src, Object loc, int off, int type)
	{
		int dst1, dst2;
		int i_rr, i_rro, i_ra;
		int src1 = 0, src2 = 0;
		if ((dst1 = getReg(dst, 1, true)) == -1)
			return;
		switch (type)
		{
			case StdTypes.T_BYTE:
			case StdTypes.T_BOOL:
				i_rr = I_LDBrr;
				i_rro = I_LDBrro;
				i_ra = I_LDBra;
				break;
			case StdTypes.T_CHAR:
			case StdTypes.T_SHRT:
				i_rr = I_LDWrr;
				i_rro = I_LDWrro;
				i_ra = I_LDWra;
				break;
			default:
				if ((dst2 = getReg(dst, 2, true)) == -1 || !checkConsecReg(dst1, dst2))
					return;
				i_rr = I_LDDrr;
				i_rro = I_LDDrro;
				i_ra = I_LDDra;
		}
		if (src != 0)
		{
			if (loc != null)
			{
				fatalError("invalid src/loc-combination in genLoadVarVal");
				return;
			}
			if (src == regBase)
			{
				src1 = R_STACK_LO;
				off += curVarOffParam;
			}
			else if ((src1 = getReg(src, 1, false)) == -1 || (src2 = getReg(src, 2, false)) == -1 || !checkConsecReg(src1, src2))
				return;
			if (off == 0)
				ins(i_rr, dst1, src1);
			else if (off >= 0 && off <= 0xFFFF)
				ins(i_rro, dst1, src1, off);
			else
			{
				ins(I_MOVDrr, R_TMP_LO, src1);
				ins(I_ADDri, R_TMP_LO, off & 0xFFFF);
				ins(I_ADCri, R_TMP_HI, off >>> 16);
				ins(i_rr, dst1, R_TMP_LO);
			}
		}
		else
		{
			int pos = mem.getAddrAsInt(loc, off);
			if (pos >= 0 && pos < 0x100000)
				ins(i_ra, dst1, pos);
			else
			{
				ins(I_LDPra, R_TMP_32, pos);
				ins(i_rr, dst1, R_TMP_32);
			}
		}
	}
	
	public void genConvertVal(int dst, int src, int toType, int fromType)
	{
		int dst1, dst2, src1;
		Instruction jd, end;
		if (toType == fromType)
		{
			if (dst != src)
				fatalError(ERR_INVREGUSE);
			return;
		}
		switch (toType)
		{
			case StdTypes.T_BYTE:
				return;
			case StdTypes.T_SHRT:
			case StdTypes.T_CHAR:
				if ((src1 = getReg(src, 1, false)) == -1 || (dst1 = getReg(dst, 1, true)) == -1)
					return;
				if (src1 != dst1)
				{
					fatalError(ERR_INVREGUSE);
					return;
				}
				if (fromType == StdTypes.T_BYTE)
				{
					jd = getUnlinkedInstruction();
					end = getUnlinkedInstruction();
					ins(I_TSTri, src1, 0x80);
					insJmp(I_JPZabs, jd);
					ins(I_ORri, dst1, 0xFF00);
					insJmp(I_JUMPabs, end);
					appendInstruction(jd);
					ins(I_ANDri, dst1, 0x00FF);
					appendInstruction(end);
				}
				return;
			case StdTypes.T_PTR:
			case StdTypes.T_INT:
				if ((src1 = getReg(src, 1, false)) == -1 || (dst1 = getReg(dst, 1, true)) == -1 || (dst2 = getReg(dst, 2, true)) == -1 || !checkConsecReg(dst1, dst2))
					return;
				if (src1 != dst1)
				{
					fatalError(ERR_INVREGUSE);
					return;
				}
				if (fromType == StdTypes.T_BYTE)
				{
					ins(I_SEXTri, dst1, 0x0080);
					return;
				}
				if (fromType == StdTypes.T_SHRT)
				{
					ins(I_SEXTri, dst1, 0x8000);
					return;
				}
				if (fromType == StdTypes.T_CHAR)
				{
					ins(I_XORrr, dst2, dst2);
					return;
				}
				return;
		}
		fatalError("not yet supported genConvertVal toType");
		return;
	}
	
	public void genDup(int dst, int src, int type)
	{
		int srcReg, dstReg, cnt = getRegCount(type);
		if (dst == src)
			return;
		if ((dstReg = getReg(dst, 1, true)) == -1 || (srcReg = getReg(src, 1, false)) == -1)
			return;
		ins(I_MOVrr, dstReg, srcReg);
		if (cnt > 1)
		{
			if ((dstReg = getReg(dst, 2, true)) == -1 || (srcReg = getReg(src, 2, false)) == -1)
				return;
			ins(I_MOVrr, dstReg, srcReg);
			if (cnt == 4)
			{
				if ((dstReg = getReg(dst, 3, true)) == -1 || (srcReg = getReg(src, 3, false)) == -1)
					return;
				ins(I_MOVrr, dstReg, srcReg);
				if ((dstReg = getReg(dst, 4, true)) == -1 || (srcReg = getReg(src, 4, false)) == -1)
					return;
				ins(I_MOVrr, dstReg, srcReg);
			}
		}
	}
	
	public void genPushConstVal(int val, int type)
	{
		if (type == StdTypes.T_INT || type == StdTypes.T_FLT || type == StdTypes.T_PTR)
		{
			ins(I_LDri, R_TMP_LO, val & 0xFFFF);
			ins(I_LDri, R_TMP_HI, val >>> 16);
			insPush(R_TMP_HI, R_TMP_LO);
		}
		else
		{
			ins(I_LDri, R_TMP_LO, val & 0xFFFF);
			insPush(R_TMP_LO, R_TMP_LO);
		}
	}
	
	public void genPushConstDoubleOrLongVal(long val, boolean asDouble)
	{
		genPushConstVal((int) (val >>> 16), StdTypes.T_INT);
		genPushConstVal((int) val, StdTypes.T_INT);
	}
	
	public void genPush(int src, int type)
	{
		int src1Reg, src2Reg, cnt = getRegCount(type);
		if ((src1Reg = getReg(src, 1, false)) == -1)
			return;
		if (cnt == 4)
		{
			if ((src1Reg = getReg(src, 3, false)) == -1 || (src2Reg = getReg(src, 4, false)) == -1 || !checkConsecReg(src1Reg, src2Reg))
				return;
			insPush(src2Reg, src1Reg);
		}
		if (cnt == 1)
			insPush(src1Reg, src1Reg);
		else
		{
			if ((src2Reg = getReg(src, 2, false)) == -1)
				return;
			if (!checkConsecReg(src1Reg, src2Reg))
				return;
			insPush(src2Reg, src1Reg);
		}
	}
	
	public void genPop(int dst, int type)
	{
		int dst1Reg, dst2Reg, cnt = getRegCount(type);
		if ((dst1Reg = getReg(dst, 1, true)) == -1)
			return;
		if (cnt == 1)
			insPop(dst1Reg, dst1Reg);
		else
		{
			if ((dst2Reg = getReg(dst, 2, true)) == -1)
				return;
			if (!checkConsecReg(dst1Reg, dst2Reg))
				return;
			insPop(dst2Reg, dst1Reg);
			if (cnt == 4)
			{
				if ((dst1Reg = getReg(dst, 3, true)) == -1 || (dst2Reg = getReg(dst, 4, true)) == -1 || !checkConsecReg(dst1Reg, dst2Reg))
					return;
				insPop(dst2Reg, dst1Reg);
			}
		}
	}
	
	public void genAssign(int dst, int src, int type)
	{
		int dst1, dst2, src1, src2;
		if ((dst1 = getReg(dst, 1, true)) == -1 || (dst2 = getReg(dst, 2, true)) == -1 || !checkConsecReg(dst1, dst2) || (src1 = getReg(src, 1, true)) == -1)
			return;
		switch (type)
		{
			case StdTypes.T_BYTE:
			case StdTypes.T_BOOL:
				ins(I_STBrr, src1, dst1);
				break;
			case StdTypes.T_SHRT:
			case StdTypes.T_CHAR:
				ins(I_STWrr, src1, dst1);
				break;
			case StdTypes.T_INT:
			case StdTypes.T_FLT:
			case StdTypes.T_PTR:
				if ((src2 = getReg(src, 2, true)) == -1 || !checkConsecReg(src1, src2))
					return;
				ins(I_STDrr, src1, dst1);
				break;
			case StdTypes.T_LONG:
			case StdTypes.T_DBL:
			case StdTypes.T_DPTR:
				if ((src2 = getReg(src, 2, true)) == -1 || !checkConsecReg(src1, src2))
					return;
				ins(I_STDrr, src1, dst1);
				if ((src1 = getReg(src, 3, true)) == -1 || (src2 = getReg(src, 4, true)) == -1 || !checkConsecReg(src1, src2))
					return;
				ins(I_STDror, src1, dst1, 4);
				break;
			default:
				fatalError("invalid type in genAssign");
		}
	}
	
	public void genBinOp(int dst, int src1, int src2, int op, int type)
	{
		int opType = op >>> 16, opPar = op & 0xFFFF;
		int r1, r2;
		Instruction jst, jend;
		if (dst != src1 || dst == src2)
		{
			fatalError("unsupported register combination for genBinOp");
			return;
		}
		switch (type)
		{
			case StdTypes.T_BOOL:
				if ((r1 = getReg(src1, 1, false)) == -1 || (r2 = getReg(src2, 1, false)) == -1)
					return;
				switch (opType)
				{
					case Ops.S_ARI:
						switch (opPar)
						{
							case Ops.A_AND:
								ins(I_ANDrr, r1, r2);
								return;
							case Ops.A_OR:
								ins(I_ORrr, r1, r2);
								return;
							case Ops.A_XOR:
								ins(I_XORrr, r1, r2);
								return;
						}
				}
				fatalError("unsupported operation for bool-genBinOp");
				return;
			case StdTypes.T_BYTE:
			case StdTypes.T_SHRT:
			case StdTypes.T_CHAR:
				fatalError("byte/short not supported for genBinOp");
				return;
			case StdTypes.T_INT:
				if ((r1 = getReg(src1, 1, false)) == -1 || (r2 = getReg(src2, 1, false)) == -1 || !checkConsecReg(r1, getReg(src1, 2, false)) || !checkConsecReg(r2, getReg(src2, 2, false)))
					return;
				switch (opType)
				{
					case Ops.S_ARI:
						switch (opPar)
						{
							case Ops.A_AND:
								ins(I_ANDDrr, r1, r2);
								return;
							case Ops.A_OR:
								ins(I_ORDrr, r1, r2);
								return;
							case Ops.A_XOR:
								ins(I_XORDrr, r1, r2);
								return;
							case Ops.A_PLUS:
								ins(I_ADDDrr, r1, r2);
								return;
							case Ops.A_MINUS:
								ins(I_SETC);
								ins(I_SBCDrr, r1, r2);
								return;
							case Ops.A_MUL:
								ins(I_MULDrr, r1, r2);
								return;
							case Ops.A_DIV:
								ins(I_DIVDrr, r1, r2);
								return;
							case Ops.A_MOD:
								ins(I_MODDrr, r1, r2);
								return;
						}
					case Ops.S_BSH:
						jst = getUnlinkedInstruction();
						appendInstruction(jst);
						jend = getUnlinkedInstruction();
						insJmp(I_JPZra, r2, jend);
						switch (opPar)
						{
							case Ops.B_SHL:
								ins(I_SFTLri, r1, 1);
								ins(I_ROTLri, r1 + 1, 1);
								break;
							case Ops.B_SHRL:
								ins(I_SFTRri, r1 + 1, 1);
								ins(I_ROTRri, r1, 1);
								break;
							case Ops.B_SHRA:
								ins(I_ASRDr, r1);
								break;
							default:
								fatalError("unsupported operation for int-bsh-genBinOp");
								return;
						}
						ins(I_DECri, r2, 1);
						insJmp(I_JUMPabs, jst);
						appendInstruction(jend);
						return;
				}
				fatalError("unsupported operation for int-genBinOp");
				return;
			case StdTypes.T_LONG:
				fatalError("unsupported operation for long-genBinOp");
				return;
			default:
				fatalError("unsupported operand-type for genBinOp");
		}
	}
	
	public void genUnaOp(int dst, int src, int op, int type)
	{
		int opPar = op & 0xFFFF, reg;
		if (dst != src)
		{
			fatalError("unsupported register combination for genUnaOp");
			return;
		}
		if ((reg = getReg(dst, 1, false)) == -1)
			return;
		switch (type)
		{
			case StdTypes.T_BOOL:
				if (opPar == Ops.L_NOT)
				{
					ins(I_XORri, reg, 1);
					return;
				}
				fatalError("unsupported bool-operation for genUnaOp");
				return;
			case StdTypes.T_BYTE:
			case StdTypes.T_SHRT:
			case StdTypes.T_CHAR:
				fatalError("byte/short not supported for genBinOp");
				return;
			case StdTypes.T_INT:
				if (!checkConsecReg(reg, getReg(dst, 2, false)))
					return;
				switch (opPar)
				{
					case Ops.A_PLUS: //do nothing for Ops.A_PLUS
						return;
					case Ops.A_MINUS:
						ins(I_MOVDrr, R_TMP_32, reg);
						ins(I_LDri, reg, 0);
						ins(I_MOVrr, reg + 1, reg);
						ins(I_SETC);
						ins(I_SBCDrr, reg, R_TMP_32);
						return;
					case Ops.A_CPL:
						ins(I_NOTr, reg);
						ins(I_NOTr, reg + 1);
						return;
				}
				fatalError("unsupported operation for int-genBinOp");
				return;
			case StdTypes.T_LONG:
				fatalError("unsupported operation for long-genBinOp");
				return;
			default:
				fatalError("unsupported operand-type for genBinOp");
		}
	}
	
	public void genIncMem(int dst, int type)
	{
		int reg;
		if ((reg = getReg(dst, 1, false)) == -1 || !checkConsecReg(reg, getReg(dst, 2, false)))
			return;
		switch (type)
		{
			case StdTypes.T_BYTE:
				ins(I_LDBrr, R_TMP_LO, reg);
				ins(I_ADDri, R_TMP_LO, 1);
				ins(I_STBrr, R_TMP_LO, reg);
				return;
			case StdTypes.T_SHRT:
			case StdTypes.T_CHAR:
				ins(I_LDWrr, R_TMP_LO, reg);
				ins(I_ADDri, R_TMP_LO, 1);
				ins(I_STWrr, R_TMP_LO, reg);
				return;
			case StdTypes.T_INT:
				ins(I_LDDrr, R_TMP_32, reg);
				ins(I_ADDDri, R_TMP_32, 1);
				ins(I_STDrr, R_TMP_32, reg);
				return;
		}
		fatalError("unsupported operand-type for genIncMem");
	}
	
	public void genDecMem(int dst, int type)
	{
		int reg;
		if ((reg = getReg(dst, 1, false)) == -1 || !checkConsecReg(reg, getReg(dst, 2, false)))
			return;
		switch (type)
		{
			case StdTypes.T_BYTE:
				ins(I_LDBrr, R_TMP_LO, reg);
				ins(I_SUBri, R_TMP_LO, 1);
				ins(I_STBrr, reg, R_TMP_LO);
				return;
			case StdTypes.T_SHRT:
			case StdTypes.T_CHAR:
				ins(I_LDWrr, R_TMP_LO, reg);
				ins(I_SUBri, R_TMP_LO, 1);
				ins(I_STWrr, R_TMP_LO, reg);
				return;
			case StdTypes.T_INT:
				ins(I_LDDrr, R_TMP_32, reg);
				ins(I_SUBDri, R_TMP_32, 1);
				ins(I_STDrr, R_TMP_32, reg);
				return;
		}
		fatalError("unsupported operand-type for genDecMem");
	}
	
	public void genSaveUnitContext()
	{
		insPush(R_CLSS_HI, R_CLSS_LO);
	}
	
	public void genRestUnitContext()
	{
		insPop(R_CLSS_HI, R_CLSS_LO);
	}
	
	public void genLoadUnitContext(int dst, int off)
	{
		genLoadVarVal(dst, R_CLSS_32, null, off, StdTypes.T_PTR);
	}
	
	public void genLoadConstUnitContext(int dst, Object unitLoc)
	{
		genLoadConstVal(dst, mem.getAddrAsInt(unitLoc, 0), StdTypes.T_INT);
	}
	
	public void genSaveInstContext()
	{
		insPush(R_CLSS_LO, R_INST_HI); //inst must be directly behind clss
	}
	
	public void genRestInstContext()
	{
		insPop(R_CLSS_LO, R_INST_HI); //inst must be directly behind clss
	}
	
	public void genLoadInstContext(int src)
	{
		int srcR;
		if ((srcR = getReg(src, 1, false)) == -1 || !checkConsecReg(srcR, getReg(src, 2, false)))
			return;
		ins(I_MOVDrr, R_INST_32, srcR);
		ins(I_MOVDrr, R_TMP_32, R_INST_32);
		ins(I_SUBDri, R_TMP_32, 4);
		ins(I_LDDrr, R_CLSS_32, R_TMP_32);
	}
	
	public void genCall(int off, int clssReg, int parSize)
	{
		int cR;
		if ((cR = getReg(clssReg, 0, false)) == -1 || !checkConsecReg(cR, getReg(clssReg, 2, false)))
			return;
		if (off != 0)
			ins(I_LDDrro, R_TMP_32, cR, off);
		else
			ins(I_LDDrr, R_TMP_32, cR);
		ins(I_CALLr, R_TMP_32);
		curVarOffParam -= parSize;
	}
	
	public void genCallIndexed(int intfReg, int off, int parSize)
	{
    /*int cr1, cr2;
    if ((cr1=getRegZPAddr(3, intfReg, StdTypes.T_DPTR, false))==-1
        || (cr2=getRegZPAddr(4, intfReg, StdTypes.T_DPTR, false))==-1) return;
    ins(I_PHX);
    ins(I_CLC);
    ins(I_LDAzp, cr1);
    ins(I_ADCimm, off&0xFF);
    ins(I_TAX);
    ins(I_LDAzp, cr2);
    ins(I_ADCimm, off>>>8);
    ins(I_TAY);
    ins(I_LPA);
    ins(I_STAzp, ZPAddrTmpLo);
    ins(I_LPA);
    ins(I_STAzp, ZPAddrTmpHi);
    ins(I_CLC);
    ins(I_LDAzp, ZPAddrTmpLo);
    ins(I_ADCzp, ZPAddrClssLo);
    ins(I_TAX);
    ins(I_LDAzp, ZPAddrTmpHi);
    ins(I_ADCzp, ZPAddrClssHi);
    ins(I_TAY);
    if (ctx.codeStart==0) {
      ins(I_LPA);
      ins(I_STAzp, ZPAddrTmpLo);
      ins(I_LPA);
      ins(I_STAzp, ZPAddrTmpHi);
      ins(I_PLX);
      ins(I_JSRmem, ZPAddrTmpLo);
    }
    else {
      ins(I_CLC);
      ins(I_LPA);
      ins(I_ADCimm, ctx.codeStart&0xFF);
      ins(I_STAzp, ZPAddrTmpLo);
      ins(I_LPA);
      ins(I_ADCimm, (ctx.codeStart>>>8)&0xFF);
      ins(I_STAzp, ZPAddrTmpHi);
      ins(I_PLX);
      ins(I_JSRmem, ZPAddrTmpLo);
    }
    insCleanStackAfterCall(parSize);*/
	}
	
	public void genCallConst(Mthd obj, int parSize)
	{
		insPatchedCall(obj, parSize);
		curVarOffParam -= parSize;
	}
	
	public void genJmp(Instruction dest)
	{
		insJmp(I_JUMPabs, dest);
	}
	
	public void genCondJmp(Instruction dest, int cond)
	{
		//signed/unsigned is done in comparison
		switch (cond)
		{
			case Ops.C_EQ: //"=="
				insJmp(I_JPZabs, dest);
				break;
			case Ops.C_NE: //"!="
				insJmp(I_JPNZabs, dest);
				break;
			case Ops.C_LW: //"<"
			case Ops.C_BO: //unsigned "<"
				insJmp(I_JPLEabs, dest);
				break;
			case Ops.C_GE: //">="
				insJmp(I_JPEGabs, dest);
				break;
			case Ops.C_LE: //"<="
				insJmp(I_JPELabs, dest);
				break;
			case Ops.C_GT: //">"
				insJmp(I_JPGRabs, dest);
				break;
			default:
				fatalError("unsupported jump in genCondJmp");
		}
	}
	
	public int genComp(int src1, int src2, int type, int cond)
	{
		int src1R, src2R;
		if ((src1R = getReg(src1, 1, false)) == -1 || (src2R = getReg(src2, 1, false)) == -1)
			return 0;
		switch (getRegCount(type))
		{
			case 1: //byte, boolean, short, char
				if (cond == Ops.C_BO)
					ins(I_CMPUrr, src1R, src2R);
				else
					ins(I_CMPSrr, src1R, src2R);
				break;
			case 2: //int, ptr
				if (!checkConsecReg(src1R, getReg(src1, 2, false)) || !checkConsecReg(src2R, getReg(src2, 2, false)))
					return 0;
				if (cond == Ops.C_BO)
					ins(I_CMPUDrr, src1R, src2R);
				else
					ins(I_CMPSDrr, src1R, src2R);
				break;
			default:
				fatalError("unsupported type in genComp");
		}
		return cond;
	}
	
	public int genCompValToConstVal(int src, int val, int type, int cond)
	{
		int srcR;
		if ((srcR = getReg(src, 1, false)) == -1)
			return 0;
		switch (getRegCount(type))
		{
			case 1:
				if (cond == Ops.C_BO)
					ins(I_CMPUri, srcR, val & 0xFFFF);
				else
					ins(I_CMPSri, srcR, val & 0xFFFF);
				break;
			case 2:
				if (!checkConsecReg(srcR, getReg(src, 2, false)))
					return 0;
				ins(I_LDri, R_TMP_LO, val & 0xFFFF);
				ins(I_LDri, R_TMP_HI, val >>> 16);
				if (cond == Ops.C_BO)
					ins(I_CMPUDrr, srcR, R_TMP_32);
				else
					ins(I_CMPSDrr, srcR, R_TMP_32);
				break;
			default:
				fatalError("unsupported type in genCompToConstVal");
		}
		return cond;
	}
	
	public int genCompValToConstDoubleOrLongVal(int src, long val, boolean asDouble, int cond)
	{
		fatalError("genCompToConstDoubleOrLongVal not supported yet");
		return cond;
	}
	
	public int genCompPtrToNull(int src, int cond)
	{
		int reg;
		if ((reg = getReg(src, 1, false)) == -1 || !checkConsecReg(reg, getReg(src, 2, false)))
			return 0;
		ins(I_TSTDr, reg);
		return cond;
	}
	
	public void genWriteIO(int dst, int src, int type, int memLoc)
	{
		// TODO Auto-generated method stub
	}
	
	public void genReadIO(int dst, int src, int type, int memLoc)
	{
		// TODO Auto-generated method stub
	}
	
	public void genMoveToPrimary(int src, int type)
	{
		int srcR, i, count = getRegCount(type);
		for (i = 0; i < count; i++)
		{
			if ((srcR = getReg(src, i + 1, false)) == -1)
				return;
			if (srcR != i)
				ins(I_MOVrr, i, srcR);
		}
	}
	
	public void genMoveFromPrimary(int dst, int type)
	{
		int dstR, i, count = getRegCount(type);
		for (i = count - 1; i >= 0; i--)
		{
			if ((dstR = getReg(dst, i + 1, true)) == -1)
				return;
			if (dstR != i)
				ins(I_MOVrr, dstR, i);
		}
	}
	
	public void genMoveIntfMapFromPrimary(int dst)
	{
    /*int r1, r2;
    if ((r1=getRegZPAddr(3, dst, StdTypes.T_DPTR, true))==0
        || (r2=getRegZPAddr(4, dst, StdTypes.T_DPTR, true))==0) return;
    ins(I_MOVzpzp, r1, getZPAddrOfReg(0)); //interface map double pointer
    ins(I_MOVzpzp, r2, getZPAddrOfReg(1));*/ //  is never in r0/r1, so always move
	}
	
	public void genSavePrimary(int type)
	{
		ins(I_PUSHrr, getRegCount(type) - 1, 0);
	}
	
	public void genRestPrimary(int type)
	{
		ins(I_POPrr, getRegCount(type) - 1, 0);
	}
	
	public void genCheckBounds(int addrReg, int offReg, int checkToOffset, Instruction onSuccess)
	{
		//code from Benedikt
		int addr = -1, off = -1;
		if ((addr = getReg(addrReg, 1, true)) == -1 || (off = getReg(offReg, 1, false)) == -1)
			return;
		
		storeReg(addrReg);//save addrReg to Stack
		ins(I_ADDDri, addr, checkToOffset); //add Offset onto Address
		ins(I_LDDrr, addr, addr); //load size of the Array into AddressRegister
		ins(I_CMPUrr, addr, off);//compare Size of the Array with The desired Offset to be accessed
		restoreReg(addrReg);//recover addReg from Stack
		insJmp(I_JPGRabs, onSuccess);//Jump if CMPUrr was EQ or GR
	}
	
	public void genCheckStackExtreme(int maxValueReg, Instruction onSuccess)
	{
		fatalError("stack extreme check not supported");
	}
	
	public void genLoadDerefAddr(int destReg, int objReg, int indReg, int baseOffset, int entrySize)
	{
		int dstR, objR, indR;
		if ((dstR = getReg(destReg, 1, true)) == -1 || !checkConsecReg(dstR, getReg(destReg, 2, true)) || (objR = getReg(objReg, 1, false)) == -1 || !checkConsecReg(objR, getReg(objReg, 2, false)) || (indR = getReg(indReg, 1, false)) == -1 || !checkConsecReg(indR, getReg(indReg, 2, false)))
			return;
		if (dstR != objR)
			ins(I_MOVDrr, dstR, objR);
		if (entrySize < 0)
		{
			ins(I_LDri, R_TMP_LO, 0);
			ins(I_MOVrr, R_TMP_HI, R_TMP_LO);
			ins(I_SETC);
			ins(I_SBCDrr, R_TMP_32, indR);
			entrySize = -entrySize;
		}
		else
			ins(I_MOVDrr, R_TMP_32, indR);
		switch (entrySize)
		{
			case 1:
			case 2:
			case 4:
			case 8:
				while (entrySize > 1)
				{
					ins(I_SFTLri, R_TMP_LO, 1);
					ins(I_ROTLri, R_TMP_HI, 1);
					entrySize = entrySize >>> 1;
				}
				break;
			default:
				fatalError("not supported entrySize in genLoadDerefAddr");
				return;
		}
		if (baseOffset != 0)
		{
			if (baseOffset > 0 && baseOffset <= 0xFFFF)
				ins(I_ADDDri, R_TMP_32, baseOffset);
			else if (baseOffset > 0)
			{
				ins(I_ADDri, R_TMP_LO, baseOffset & 0xFFFF);
				ins(I_ADCri, R_TMP_HI, baseOffset >>> 16);
			}
			else
			{
				baseOffset = -baseOffset;
				if (baseOffset <= 0xFFFF)
					ins(I_SUBDri, R_TMP_32, baseOffset);
				else
				{
					ins(I_SUBri, R_TMP_LO, baseOffset & 0xFFFF);
					ins(I_SBCri, R_TMP_HI, baseOffset >>> 16);
				}
			}
		}
		ins(I_ADDDrr, dstR, R_TMP_32);
	}
	
	//throw frame:
	//  excOffset + 10: current exception thrown
	//  excOffset +  9: base pointer for current try-block
	//  excOffset +  8: stack pointer for current try-block
	//  excOffset +  6: instance context of current try-block
	//  excOffset +  4: unit context of current try-block
	//  excOffset +  2: code-byte to jump to if exception is thrown
	//  excOffset     : pointer to last excStackFrame
	public void genThrowFrameBuild(int globalAddrReg, Instruction dest, int throwBlockOffset)
	{
		if (globalAddrReg != 0x0003 || usedRegs != 0x0003 || throwBlockOffset >= 0)
		{ //globalAddrReg is the only allocated register, throwBlockOffset is in local vars
			fatalError(ERR_INVGLOBADDRREG);
			return;
		}
    /*throwBlockOffset+=STACKMEM_OFF;
    //get global frame pointer, save in current frame, set current frame as global
    ins(I_CLY); //load current global pointer and store throwBlockOffset+0/1
    ins(I_LDAizpy, ZPAddrBase);
    ins(I_STAabsx, throwBlockOffset);
    ins(I_INY);
    ins(I_LDAizpy, ZPAddrBase);
    ins(I_STAabsx, throwBlockOffset+1);
    ins(I_TXA); //calculate current throw frame address, store in global pointer
    ins(I_CLY);
    ins(I_CLC);
    ins(I_ADCimm, throwBlockOffset&0xFF);
    ins(I_STAizpy, ZPAddrBase);
    ins(I_CLA);
    ins(I_ADCimm, (throwBlockOffset>>>8)&0xFF);
    ins(I_INY);
    ins(I_STAizpy, ZPAddrBase);
    //insert destination code address
    insJmp(I_PUSAimm, dest);
    ins(I_PLA);
    ins(I_STAabsx, throwBlockOffset+2);
    ins(I_PLA);
    ins(I_STAabsx, throwBlockOffset+3);
    //fill unit- and instance context, stack and base pointer
    ins(I_LDAzp, ZPAddrClssLo);
    ins(I_STAabsx, throwBlockOffset+4);
    ins(I_LDAzp, ZPAddrClssHi);
    ins(I_STAabsx, throwBlockOffset+5);
    ins(I_LDAzp, ZPAddrInstLo);
    ins(I_STAabsx, throwBlockOffset+6);
    ins(I_LDAzp, ZPAddrInstHi);
    ins(I_STAabsx, throwBlockOffset+7);
    ins(I_TXY);
    ins(I_TSX);
    ins(I_TXA);
    ins(I_TYX);
    ins(I_STAabsx, throwBlockOffset+8);
    ins(I_TXA);
    ins(I_STAabsx, throwBlockOffset+9);
    //clear exception thrown
    ins(I_CLA);
    ins(I_STAabsx, throwBlockOffset+10);
    ins(I_STAabsx, throwBlockOffset+11);*/
	}
	
	public void genThrowFrameUpdate(Instruction oldDest, Instruction newDest, int throwBlockOffset)
	{
		if (throwBlockOffset >= 0)
		{ //throwBlockOffset is in local vars
			fatalError(ERR_INVGLOBADDRREG);
			return;
		}
    /*throwBlockOffset+=STACKMEM_OFF;
    insJmp(I_PUSAimm, newDest);
    ins(I_PLA);
    ins(I_STAabsx, throwBlockOffset+2);
    ins(I_PLA);
    ins(I_STAabsx, throwBlockOffset+3);*/
	}
	
	public void genThrowFrameReset(int globalAddrReg, int throwBlockOffset)
	{
		if (globalAddrReg != 0x0003 || usedRegs != 0x0003 || throwBlockOffset >= 0)
		{ //globalAddrReg is the only allocated register, throwBlockOffset is in local vars
			fatalError(ERR_INVGLOBADDRREG);
			return;
		}
		//load previous pointer from current frame and store in global variable
    /*throwBlockOffset+=STACKMEM_OFF;
    ins(I_CLY);
    ins(I_LDAabsx, throwBlockOffset);
    ins(I_STAizpy, ZPAddrBase);
    ins(I_INY);
    ins(I_LDAabsx, throwBlockOffset+1);
    ins(I_STAizpy, ZPAddrBase);*/
	}
	
	public void inlineVarOffset(int inlineMode, int objReg, Object loc, int offset, int baseValue)
	{
		fatalError("inlining of variable offsets is not supported");
	}
	
	public void inlineCodeAddress(boolean defineHere, int addOffset)
	{
    /*Instruction i;
    appendInstruction(i=getUnlinkedInstruction());
    if (defineHere) i.type=IT_DEFICA;
    else {
      i.type=IT_INLCODA;
      i.iPar1=addOffset;
      i.putShort(0); //dummy address
    }*/
	}
	
	//internal instruction coding
	private void ins(int op)
	{
		Instruction i;
		appendInstruction(i = getUnlinkedInstruction());
		i.type = op;
		i.putByte(op);
		i.size = (op & SIZE_MASK) >>> SIZE_SHIFT;
	}
	
	private void ins(int op, int par)
	{
		Instruction i;
		appendInstruction(i = getUnlinkedInstruction());
		i.type = op;
		switch (op & PAR_P1M)
		{
			case PAR_IMM1:
				i.iPar1 = par;
				break;
			case PAR_REG1:
				i.reg0 = par;
				break;
			default:
				fatalError("invalid call to ins(op,par)");
		}
		i.putByte(op);
		i.size = (op & SIZE_MASK) >>> SIZE_SHIFT;
	}
	
	private void ins(int op, int par1, int par2)
	{
		Instruction i;
		appendInstruction(i = getUnlinkedInstruction());
		i.type = op;
		switch (op & PAR_P1M)
		{
			case PAR_IMM1:
			case PAR_RMA1:
				i.iPar1 = par1;
				break;
			case PAR_REG1:
			case PAR_ADR1:
				i.reg0 = par1;
				break;
			default:
				fatalError("invalid call to ins(op1,par,par)");
		}
		switch (op & PAR_P2M)
		{
			case PAR_IMM2:
			case PAR_RMA2:
			case PAR_AAD2:
				i.iPar2 = par2;
				break;
			case PAR_REG2:
			case PAR_ADR2:
				i.reg1 = par2;
				break;
			default:
				fatalError("invalid call to ins(op2,par,par)");
		}
		i.putByte(op);
		i.size = (op & SIZE_MASK) >>> SIZE_SHIFT;
	}
	
	private void ins(int op, int par1, int par2, int off)
	{
		Instruction i;
		appendInstruction(i = getUnlinkedInstruction());
		i.type = op;
		switch (op & PAR_P1M)
		{
			case PAR_REG1:
				i.reg0 = par1;
				break;
			case PAR_AAD1:
				i.iPar1 = par1;
				break;
			case PAR_AOF1:
				i.reg0 = par1;
				i.iPar3 = off;
				break;
			default:
				fatalError("invalid call-1 to ins(op,par,par,off)");
		}
		switch (op & PAR_P2M)
		{
			case PAR_IMM2:
			case PAR_AAD2:
				i.iPar2 = par2;
				break;
			case PAR_AOF2:
				i.reg1 = par2;
				i.iPar3 = off;
				break;
			default:
				fatalError("invalid call-2 to ins(op,par,par,off)");
		}
		i.putByte(op);
		i.size = (op & SIZE_MASK) >>> SIZE_SHIFT;
	}
	
	private Instruction insPatchedCall(Mthd refMthd, int parSize)
	{
		Instruction i;
		appendInstruction(i = getUnlinkedInstruction());
		i.type = I_CALLabs;
		i.refMthd = refMthd;
		i.putByte(i.type);
		i.putByte(0);
		i.putShort(0);
		addToCodeRefFixupList(i, 1);
		return i;
	}
	
	private Instruction insJmp(int op, Instruction dest)
	{
		Instruction i;
		appendInstruction(i = getUnlinkedInstruction());
		i.type = op;
		i.putByte(op);
		i.size = (op & SIZE_MASK) >>> SIZE_SHIFT;
		i.jDest = dest;
		dest.isDest = true;
		return i;
	}
	
	private Instruction insJmp(int op, int reg, Instruction dest)
	{
		Instruction i;
		appendInstruction(i = getUnlinkedInstruction());
		i.type = op;
		i.jDest = dest;
		dest.isDest = true;
		i.reg0 = reg;
		i.putByte(op);
		i.size = (op & SIZE_MASK) >>> SIZE_SHIFT;
		return i;
	}
	
	private void insPush(int from, int to)
	{
		ins(I_PUSHrr, from, to);
		if (from <= to)
			curVarOffParam += 2 * (to - from + 1);
		else if (from == to + 1)
			curVarOffParam += 4;
		else
			fatalError("invalid from/to in insPush");
	}
	
	private void insPop(int from, int to)
	{
		ins(I_POPrr, from, to);
		if (from <= to)
			curVarOffParam -= 2 * (to - from + 1);
		else if (from == to + 1)
			curVarOffParam -= 4;
		else
			fatalError("invalid from/to in insPop");
	}
	
	protected void printCode(TextPrinter v, Instruction first, String comment, boolean asmMode)
	{
		Instruction now;
		int insCnt = 0;
		if (asmMode)
			ctx.printUniqueMethodName(v, mthdContainer);
		else
		{
			mthdContainer.owner.printNameWithOuter(v);
			v.print('.');
			mthdContainer.printNamePar(v);
		}
		v.print(':');
		if (comment != null)
		{
			v.print(" //");
			v.println(comment);
		}
		else
			v.println();
		now = first;
		while (now != null)
		{
			if (now.token != null)
			{
				if (asmMode)
					v.print("    ");
				ctx.printSourceHint(v, now.token, ";");
			}
			if (asmMode && now.isDest)
			{
				printJumpDest(v, now);
				v.println(':');
			}
			if (now.type != I_NONE)
			{
				insCnt += print(v, now, asmMode);
				v.println();
			}
			now = now.next;
		}
		if (!asmMode)
		{
			v.print("//instruction count");
			if (comment != null)
			{
				v.print(' ');
				v.print(comment);
			}
			v.print(": ");
			v.println(insCnt);
		}
	}
	
	private void printAddressLabel(TextPrinter v, int pointer)
	{
		if (pointer != 0)
		{
			v.print("$_A_");
			v.printHexFix(pointer, 8);
		}
		else
			v.print("0");
	}
	
	private void printJumpDest(TextPrinter v, Instruction dest)
	{
		ctx.printUniqueMethodName(v, mthdContainer);
		v.print("$i");
		v.print(dest.instrNr);
	}
	
	private void printCallDest(TextPrinter v, Mthd call)
	{
		ctx.printUniqueMethodName(v, call);
	}
	
	private int print(TextPrinter v, Instruction i, boolean asmMode)
	{
		if (i.type == I_NONE)
			return 0;
		v.print("  ");
		if (i.asmText != null)
		{
			v.print(i.asmText);
			return 1;
		}
		if (i.type == I_MAGC)
		{
			if (i.size == 0)
				return 0;
			v.print("db ");
			for (int c = 0; c < i.size; c++)
			{
				if (c > 0)
					v.print(',');
				v.print("0x");
				v.printHexFix(i.code[c], 2);
			}
			return 1;
		}
		switch (i.type)
		{
			case I_RET:
				v.print("RET");
				break;
			case I_CLRC:
				v.print("CLRC");
				break;
			case I_SETC:
				v.print("SETC");
				break;
			case I_JUMPabs:
				v.print("JUMP");
				break;
			case I_CALLabs:
			case I_CALLr:
				v.print("CALL");
				break;
			case I_LDPra:
				v.print("LDP");
				break;
			case I_JPZra:
				v.print("JPZ");
				break;
			case I_JPNZra:
				v.print("JPNZ");
				break;
			case I_JPZabs:
				v.print("JPZ");
				break;
			case I_JPNZabs:
				v.print("JPNZ");
				break;
			case I_JPGRabs:
				v.print("JPGR");
				break;
			case I_JPLEabs:
				v.print("JPLE");
				break;
			case I_JPEGabs:
				v.print("JPEG");
				break;
			case I_JPELabs:
				v.print("JPEL");
				break;
			case I_SEXTri:
				v.print("SEXT");
				break;
			case I_TSTri:
				v.print("TST");
				break;
			case I_NOTr:
				v.print("NOT");
				break;
			case I_TSTDr:
				v.print("TSTD");
				break;
			case I_ASRDr:
				v.print("ASRD");
				break;
			case I_SFTLri:
				v.print("SFTL");
				break;
			case I_SFTRri:
				v.print("SFTR");
				break;
			case I_ROTLri:
				v.print("ROTL");
				break;
			case I_ROTRri:
				v.print("ROTR");
				break;
			case I_INCri:
				v.print("INC");
				break;
			case I_DECri:
				v.print("DEC");
				break;
			case I_INCDri:
				v.print("INCD");
				break;
			case I_DECDri:
				v.print("DECD");
				break;
			case I_MOVrr:
				v.print("MOV");
				break;
			case I_PUSHrr:
				v.print("PUSH");
				break;
			case I_POPrr:
				v.print("POP");
				break;
			case I_MOVDrr:
				v.print("MOVD");
				break;
			case I_MULDrr:
				v.print("MULD");
				break;
			case I_DIVDrr:
				v.print("DIVD");
				break;
			case I_MODDrr:
				v.print("MODD");
				break;
			case I_ANDrr:
				v.print("AND");
				break;
			case I_ORrr:
				v.print("OR");
				break;
			case I_XORrr:
				v.print("XOR");
				break;
			case I_ADCrr:
				v.print("ADC");
				break;
			case I_SBCrr:
				v.print("SBC");
				break;
			case I_CMPUrr:
				v.print("CMPU");
				break;
			case I_CMPSrr:
				v.print("CMPS");
				break;
			case I_CMPUDrr:
				v.print("CMPUD");
				break;
			case I_CMPSDrr:
				v.print("CMPSD");
				break;
			case I_ADDDrr:
				v.print("ADDD");
				break;
			case I_ANDDrr:
				v.print("ANDD");
				break;
			case I_ORDrr:
				v.print("ORD");
				break;
			case I_XORDrr:
				v.print("XORD");
				break;
			case I_SBCDrr:
				v.print("SBCD");
				break;
			case I_LDBra:
			case I_LDBrr:
			case I_LDBrro:
				v.print("LDB");
				break;
			case I_LDWra:
			case I_LDWrr:
			case I_LDWrro:
				v.print("LDW");
				break;
			case I_LDDra:
			case I_LDDrr:
			case I_LDDrro:
				v.print("LDD");
				break;
			case I_STBrr:
				v.print("STB");
				break;
			case I_STWrr:
				v.print("STW");
				break;
			case I_STDrr:
			case I_STDror:
				v.print("STD");
				break;
			case I_ADDDri:
				v.print("ADDD");
				break;
			case I_SUBDri:
				v.print("SUBD");
				break;
			case I_ANDri:
				v.print("AND");
				break;
			case I_ORri:
				v.print("OR");
				break;
			case I_XORri:
				v.print("XOR");
				break;
			case I_SBCri:
				v.print("SBC");
				break;
			case I_CMPUri:
				v.print("CMPU");
				break;
			case I_CMPSri:
				v.print("CMPS");
				break;
			case I_ADCri:
				v.print("ADC");
				break;
			case I_LDri:
				v.print("LD");
				break;
			case I_ADDri:
				v.print("ADD");
				break;
			case I_SUBri:
				v.print("SUB");
				break;
			default:
				v.print("---unknown instruction");
				break;
		}
		if ((i.type & PAR_P1M) != 0)
		{
			v.print(' ');
			switch (i.type & PAR_P1M)
			{
				case PAR_REG1:
					v.print('r');
					v.print(i.reg0);
					break;
				case PAR_IMM1:
					v.print("#0x");
					v.printHexFix(i.iPar1, 4);
					break;
				case PAR_ADR1:
					v.print("(r");
					v.print(i.reg0);
					v.print(')');
					break;
				case PAR_AOF1:
					v.print("(r");
					v.print(i.reg0);
					v.print('+');
					v.print(i.iPar3);
					v.print(')');
					break;
				case PAR_AAD1:
					v.print('(');
					printAddressLabel(v, i.iPar1);
					v.print(')');
					break;
				case PAR_RMA1:
					v.print('#');
					printAddressLabel(v, i.iPar1);
					break;
				case PAR_JDT1:
					printJumpDest(v, i.jDest);
					break;
				case PAR_CDT1:
					printCallDest(v, i.refMthd);
					break;
			}
			if ((i.type & PAR_P2M) != 0)
			{
				v.print(',');
				switch (i.type & PAR_P2M)
				{
					case PAR_REG2:
						v.print('r');
						v.print(i.reg1);
						break;
					case PAR_IMM2:
						v.print("#0x");
						v.printHexFix(i.iPar2, 4);
						break;
					case PAR_ADR2:
						v.print("(r");
						v.print(i.reg1);
						v.print(')');
						break;
					case PAR_AOF2:
						v.print("(r");
						v.print(i.reg1);
						v.print('+');
						v.print(i.iPar3);
						v.print(')');
						break;
					case PAR_AAD2:
						v.print('(');
						printAddressLabel(v, i.iPar2);
						v.print(')');
						break;
					case PAR_RMA2:
						v.print('#');
						printAddressLabel(v, i.iPar2);
						break;
					case PAR_JDT2:
						printJumpDest(v, i.jDest);
						break;
					case PAR_CDT2:
						printCallDest(v, i.refMthd);
						break;
				}
			}
		}
		return 1;
	}
}
