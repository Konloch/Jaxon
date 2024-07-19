/* Copyright (C) 2006, 2007, 2008, 2009, 2010, 2011, 2014, 2015, 2016 Stefan Frenz and Patrick Schmidt
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

package sjc.backend.atmel;

import sjc.backend.Architecture;
import sjc.backend.Instruction;
import sjc.compbase.Marks;
import sjc.compbase.Mthd;
import sjc.compbase.Ops;
import sjc.compbase.StdTypes;
import sjc.osio.TextPrinter;

/**
 * ATmega: Architecture backend for Atmel's ATmega processors
 *
 * @author S. Frenz, P. Schmidt
 * @version 160522 (Stefan) removed unneeded semicolon
 * version 151019 (Stefan) fixed several bugs incl. genLoadDerefAddr, genCheckBounds
 * version 140124 (Stefan) fixed two bugs in genMoveFromPrimary
 * version 110222 (Stefan) fixed bitSearch
 * version 101210 (Stefan) adopted changed Architecture, fixed stack extreme check
 * version 101101 (Stefan) adopted changed Architecture
 * version 100928 (Stefan) fixed unsignedness of char
 * version 100619 (Stefan) special handling of rjmp instructions in header section
 * version 100616 (Stefan) fixed dynamic call
 * version 100609 (Stefan) added support for negative elemSize in genLoadDerefAddr
 * version 100415 (Stefan) fixed register maintainance for calls with special regs
 * version 100312 (Stefan) added support for flash objects
 * version 100127 (Stefan) adopted renaming of aricall into binAriCall, added support for unaAriCall
 * version 100126 (Stefan) added support for float and double data types
 * version 100115 (Stefan) adopted changed error reporting and codeStart-movement
 * version 100114 (Stefan) removed unused method
 * version 091103 (Stefan) removed unused variables/methods
 * version 091102 (Stefan) added call to source token printing
 * version 091013 (Stefan) adopted changed method signature of genStore*
 * version 091004 (Stefan) fixed inlineVarOffset after memory interface change
 * version 091001 (Stefan) adopted changed memory interface
 * version 090718 (Stefan) fixed prepareMethodeCoding
 * version 090717 (Stefan) adopted changed Architecture
 * version 090703 (Stefan) fixed genStoreVar* for RegY with pos>=64
 * version 090629 (Stefan) added real implementation for genCheckStackExtreme
 * version 090626 (Stefan) added dummy-implementation for genCheckStackExtreme
 * version 090619 (Stefan) adopted changed Architecture
 * version 090430 (Stefan) moved error-reporting genNativeBoundException to Architecture
 * version 090219 (Stefan) adopted changed Architecture (genCopyInstContext<->getClssInstReg)
 * version 090208 (Stefan) removed genClearMem, fixed genStoreVar*
 * version 090207 (Stefan) added copyright notice, implemented genStoreVar* for stack variables and removed genLoadNullAddr/genPushNull
 * version 090116 (Stefan) optimized genConvertVal, optimized comparison by changing jump condition for unsupported jumps
 * version 090112 (Stefan) changed printCode for I_CBI and I_SBI to show bit position instead of bit mask
 * version 081229 (Stefan) added I_JUMP which was removed from Architecture
 * version 081123 (Stefan) sized all immediate parameters correctly
 * version 081106 (Stefan) added I_ANDI, I_ORI, I_CBI, I_SBI for ATmegaOpti-supply
 * version 081104 (Stefan) added I_LDS for optimized genLoadVarVal and I_STS for ATmegaOpti-supply
 * version 081022 (Stefan) printCode-functionallity for complete method
 * version 081021 (Stefan) adopted changed genReadIO and genWriteIO, genComp* and genCondJmp, added binop-hint for multi-instruction operations
 * version 081019 (Stefan) added I_SEC, better print for I_MAGC
 * version 081016 (Stefan) added support for ctx.printCode
 * version 081015 (Stefan) optimized register allocation, added print, made deallocReg and register variables protected to allow overwriting in ATmegaOpti
 * version 081008 (Stefan) inserted cli before stack-pointer manipulation
 * version 080919 (Stefan) fixed buggy getReg-check in genLoadVarVal
 * version 080917 (Stefan) fixed inc/dec-order in genAccess after change in 080916
 * version 080916 (Stefan) fixed high-low-order of write access to multi-byte values
 * version 080731 (Stefan) optimized stack frame for inlined methods with parSize==varSize==0, fixed genMoveFromPrimary
 * version 080629 (Stefan) added warning for odd sized instructions
 * version 080622 (Stefan) added support for method inlining
 * version 080615 (Stefan) adopted changes in Architecture, optimized throwFrame, added parSize as iPar1 to I_CALL* for ATmegaOpti, optimized codeEpilog and insMOVpair
 * version 080613 (Stefan) optimized codeProlog/codeEpilog/genCall*
 * version 080612 (Stefan) fixed genLoadVarAddr/Val for mode M_RESU, changed genCompPtrToNull to be non-invasive, optimized reg-pair-moving
 * version 080611 (Stefan) added support for language throwables
 * version 080525 (Stefan) adopted changed genCondJmp signature, optimized stack cleanup after call and at method end, optimized compare and adopted jumps (now signed)
 * version 080523 (Stefan) optimized byte-mul (use unsigned mul because lower 8 bit of result are sign independent)
 * version 080522 (Stefan) fixed insJump for not supported ">" condition, fixed genClearMem, changed register allocation
 * version 080520 (Stefan) fixed genBinOp-byte-mul, optimized neg for byte
 * version 080518 (Stefan) setting of ariCall, added support for mul for bytes, moved createNewInstruction to LittleEndian, added short to getRegCount
 * version 080122 (Stefan) fixed setting of usedRegs in getReg on firstWrite
 * version 080105 (Stefan) added genSavePrimary and genRestPrimary
 * version 070913 (Stefan) added inlineVarOffset, genClearMem
 * version 070725 (Stefan) changed ins(.) to support ATmegaOpti
 * version 070711 (Patrick) bugfix in genLoadVarVal
 * version 070706 (Stefan) fixed genPop, replaced genNativeBoundException
 * version 070706 (Patrick) bugfix in genComp with workaround
 * version 070704 (Stefan) fixed fixJump and optimized some loops
 * version 070701 (Stefan) optimized pro-/epilog for better stack relative addressing, fixed genPush
 * version 070628 (Stefan) added allocClearBits
 * version 070622 (Stefan) optimized genBinOp, fixed RegCLSS
 * version 070621 (Patrick) bugfix in genBinOp, optimization in genBinOp
 * version 070620 (Patrick) using register Y for "ebp", moved class context to R2:R1, absolute jump for native bound exception
 * version 070618 (Patrick) bugfixes, using I_ADDIW where possible, stack cleanup
 * version 070617 (Patrick+Stefan) several bugfixes
 * version 070615 (Stefan) removed no longer needed getRef
 * version 070615 (Patrick) bugfixes, restructured insJump and fixJump
 * version 070614 (Patrick) internal free register management removed
 * version 070612 (Patrick) restructured insJump and fixJump and register allocation
 * version 070608 (Patrick) restructured registers for class and instance context
 * version 070605 (Patrick) restructured jump, ADDIWregimm implemented
 * version 070531 (Stefan) adopted removal of Architecture.genLoadFromMem
 * version 070505 (Stefan) changed naming of Clss to Unit, changed OutputObject to int
 * version 070501 (Stefan) fixed insPatched-related methods
 * version 070427 (Patrick) filled many methods
 * version 070127 (Stefan) optimized access to err-flag, register count detection with method
 * version 070126 (Patrick) filled many methods
 * version 070114 (Stefan) removed never called genGetClassOfResult, reduced access level where possible
 * version 070113 (Stefan) adopted change of genCheckNull to genCompPtrToNull
 * version 070101 (Stefan) adopted change in genCall
 * version 061225 (Stefan) adopted change in codeProlog
 * version 061222 (Stefan) fixed endianess and updated putCodeRef
 * version 061217 (Stefan) initial version
 */

public class ATmega extends Architecture
{
	protected final static int IM_OP = 0xFF00;
	protected final static int IM_P0 = 0x000F;
	protected final static int IM_P1 = 0x00F0;
	protected final static int I_reg0 = 0x01;
	protected final static int I_mem0 = 0x02;
	protected final static int I_imm0 = 0x03;
	protected final static int I_reg1 = 0x10;
	protected final static int I_mem1 = 0x20;
	protected final static int I_imm1 = 0x30;
	protected final static int I_LDI = 0x0100;
	protected final static int I_MOV = 0x0200;
	protected final static int I_MOVW = 0x0300;
	protected final static int I_PUSH = 0x0400;
	protected final static int I_POP = 0x0500;
	protected final static int I_AND = 0x0600;
	protected final static int I_ANDI = 0x0700;
	protected final static int I_OR = 0x0800;
	protected final static int I_ORI = 0x0900;
	protected final static int I_EOR = 0x0A00;
	protected final static int I_INC = 0x0B00;
	protected final static int I_ADC = 0x0C00;
	protected final static int I_SUBI = 0x0D00;
	protected final static int I_SBCI = 0x0E00;
	protected final static int I_SBC = 0x0F00;
	protected final static int I_LDZ = 0x1000;
	protected final static int I_STZ_INC = 0x1100;
	protected final static int I_LDZ_INC = 0x1200;
	protected final static int I_STX_DEC = 0x1300;
	protected final static int I_LDX_INC = 0x1400;
	protected final static int I_IN = 0x1500;
	protected final static int I_OUT = 0x1600;
	protected final static int I_CP = 0x1700;
	protected final static int I_CPC = 0x1800;
	protected final static int I_CPI = 0x1900;
	protected final static int I_COM = 0x1A00;
	protected final static int I_ADD = 0x1B00;
	protected final static int I_SUB = 0x1C00;
	protected final static int I_ICALL = 0x1D00;
	protected final static int I_ADDIW = 0x1E00;
	protected final static int I_MUL = 0x1F00;
	protected final static int I_STZ_DEC = 0x2000;
	protected final static int I_LSR = 0x2100;
	protected final static int I_ROR = 0x2200;
	protected final static int I_LDZ_DEC = 0x2300;
	protected final static int I_CALL = 0x2400;
	protected final static int I_LSL = 0x2500;
	protected final static int I_ROL = 0x2600;
	protected final static int I_RET = 0x2700;
	protected final static int I_RETI = 0x2800;
	protected final static int I_NEG = 0x2900;
	protected final static int I_CLC = 0x2A00;
	protected final static int I_SEC = 0x2B00;
	protected final static int I_DEC = 0x2C00;
	protected final static int I_ASR = 0x2D00;
	protected final static int I_LPM_INC = 0x2E00;
	protected final static int I_LPM = 0x2F00;
	protected final static int I_LDY_DISP = 0x3000;
	protected final static int I_STY_DISP = 0x3100;
	protected final static int I_PUSHip = 0x3100;
	protected final static int I_CLI = 0x3200;
	protected final static int I_LDS = 0x3300;
	protected final static int I_STS = 0x3400;
	protected final static int I_CBI = 0x3500;
	protected final static int I_SBI = 0x3600;
	protected final static int I_SBRC = 0x3700;
	
	protected final static int I_LDIregimm = I_LDI | I_reg0 | I_imm1;
	protected final static int I_MOVregreg = I_MOV | I_reg0 | I_reg1;
	protected final static int I_MOVWregreg = I_MOVW | I_reg0 | I_reg1;
	protected final static int I_PUSHreg = I_PUSH | I_reg0;
	protected final static int I_POPreg = I_POP | I_reg0;
	protected final static int I_ANDregreg = I_AND | I_reg0 | I_reg1;
	protected final static int I_ANDIregimm = I_ANDI | I_reg0 | I_imm1;
	protected final static int I_ORregreg = I_OR | I_reg0 | I_reg1;
	protected final static int I_ORIregimm = I_ORI | I_reg0 | I_imm1;
	protected final static int I_EORregreg = I_EOR | I_reg0 | I_reg1;
	protected final static int I_INCreg = I_INC | I_reg0;
	protected final static int I_ADCregreg = I_ADC | I_reg0 | I_reg1;
	protected final static int I_SUBIregimm = I_SUBI | I_reg0 | I_imm1;
	protected final static int I_SBCIregimm = I_SBCI | I_reg0 | I_imm1;
	protected final static int I_SBCregreg = I_SBC | I_reg0 | I_reg1;
	protected final static int I_LDZreg = I_LDZ | I_reg0;
	protected final static int I_STZ_INCreg = I_STZ_INC | I_reg0;
	protected final static int I_LDZ_INCreg = I_LDZ_INC | I_reg0;
	protected final static int I_STX_DECreg = I_STX_DEC | I_reg0;
	protected final static int I_LDX_INCreg = I_LDX_INC | I_reg0;
	protected final static int I_INregimm = I_IN | I_reg0 | I_imm1;
	protected final static int I_OUTimmreg = I_OUT | I_imm0 | I_reg1;
	protected final static int I_CPregreg = I_CP | I_reg0 | I_reg1;
	protected final static int I_CPCregreg = I_CPC | I_reg0 | I_reg1;
	protected final static int I_CPIregimm = I_CPI | I_reg0 | I_imm1;
	protected final static int I_COMreg = I_COM | I_reg0;
	protected final static int I_ADDregreg = I_ADD | I_reg0 | I_reg1;
	protected final static int I_SUBregreg = I_SUB | I_reg0 | I_reg1;
	protected final static int I_ADDIWregimm = I_ADDIW | I_reg0 | I_imm1;
	protected final static int I_MULregreg = I_MUL | I_reg0 | I_reg1;
	protected final static int I_STZ_DECreg = I_STZ_DEC | I_reg0;
	protected final static int I_LSRreg = I_LSR | I_reg0;
	protected final static int I_RORreg = I_ROR | I_reg0;
	protected final static int I_LDZ_DECreg = I_LDZ_DEC | I_reg0;
	protected final static int I_LSLreg = I_LSL | I_reg0;
	protected final static int I_ROLreg = I_ROL | I_reg0;
	protected final static int I_NEGreg = I_NEG | I_reg0;
	protected final static int I_DECreg = I_DEC | I_reg0;
	protected final static int I_ASRreg = I_ASR | I_reg0;
	protected final static int I_LPM_INCreg = I_LPM_INC | I_reg0;
	protected final static int I_LPMreg = I_LPM | I_reg0;
	protected final static int I_LDY_DISPregimm = I_LDY_DISP | I_reg0 | I_imm1;
	protected final static int I_STY_DISPregimm = I_STY_DISP | I_reg0 | I_imm1;
	protected final static int I_LDSregimm = I_LDS | I_reg0 | I_imm1;
	protected final static int I_STSimmreg = I_STS | I_imm0 | I_reg1;
	protected final static int I_CBIimm = I_CBI | I_imm0; //imm0 is coded addr&val
	protected final static int I_SBIimm = I_SBI | I_imm0; //imm0 is coded addr&val
	protected final static int I_SBRCregimm = I_SBRC | I_reg0 | I_imm1;
	
	protected final static int I_JUMP = -10;
	protected final static int I_CALLpatched = -11;
	protected final static int I_ADDpatched = -12;
	protected final static int I_IHELPER = -13;
	protected final static int I_BSHOPHINT = -14;
	protected final static int I_STEXreg = -15;
	
	protected final static String ERR_INVGLOBADDRREG = "invalid register for address";
	
	protected static final int RegAll = 0x07FF07FF;   //0000 0111 1111 1111 0000 0111 1111 1111
	//  protected final static int RegX=0x0C000000; //no special handling, allocated normally
	protected final static int RegY = 0x30000000; //stack frame pointer
	//  protected final static int RegZ=0xC0000000; //general purpose temp register (main use: ptr)
	protected final static int RegCLSS = 0x0000C000; //current class context, see CLSL and CLSH
	protected final static int RegINST = 0x00003000; //current instance context, see INSTL and INSTH
	protected final static int R_XL = 26, R_XH = 27;
	protected final static int R_YL = 28, R_YH = 29;
	protected final static int R_ZL = 30, R_ZH = 31;
	protected final static int R_CLSL = 14, R_CLSH = 15;
	protected final static int R_INSTL = 12, R_INSTH = 13;
	protected final static int R_PRIM_START = 16;
	protected final static int R_HLP = 27; //general purpse temp register
	protected final static int R_ZERO = 11; //register has to be cleared in boot section
	
	//offsets of parameters
	protected final static int VAROFF_PARAM_INL = 3;
	protected final static int VAROFF_PARAM_NRM = 5;
	
	protected int usedRegs, writtenRegs, nextAllocReg, stackLevel;
	protected Mthd mthdContainer; //remember current method container, not available in finalizeInstructions
	private int curVarOffParam, headerLength;
	private String outestMthdName;
	private boolean nextValInFlash, headerWithRJMP;
	
	public ATmega()
	{
		relocBytes = 2;
		stackClearBits = 0;
		allocClearBits = 1;
		maxInstrCodeSize = 8;
		throwFrameSize = 2 * 6; //for description of throw-frame see genThrowFrameBuild
		throwFrameExcOff = 2 * 5;
		regClss = RegCLSS;
		regInst = RegINST;
		regBase = RegY;
		binAriCall[StdTypes.T_BYTE] |= (1 << (Ops.A_DIV - Ops.MSKBSE)) | (1 << (Ops.A_MOD - Ops.MSKBSE));
		binAriCall[StdTypes.T_SHRT] |= (1 << (Ops.A_MUL - Ops.MSKBSE)) | (1 << (Ops.A_DIV - Ops.MSKBSE)) | (1 << (Ops.A_MOD - Ops.MSKBSE));
		binAriCall[StdTypes.T_INT] |= (1 << (Ops.A_MUL - Ops.MSKBSE)) | (1 << (Ops.A_DIV - Ops.MSKBSE)) | (1 << (Ops.A_MOD - Ops.MSKBSE));
		binAriCall[StdTypes.T_LONG] |= (1 << (Ops.A_MUL - Ops.MSKBSE)) | (1 << (Ops.A_DIV - Ops.MSKBSE)) | (1 << (Ops.A_MOD - Ops.MSKBSE));
		binAriCall[StdTypes.T_FLT] |= (1 << (Ops.A_MUL - Ops.MSKBSE)) | (1 << (Ops.A_DIV - Ops.MSKBSE)) | (1 << (Ops.A_MOD - Ops.MSKBSE)) | (1 << (Ops.A_PLUS - Ops.MSKBSE)) | (1 << (Ops.A_MINUS - Ops.MSKBSE));
		binAriCall[StdTypes.T_DBL] |= (1 << (Ops.A_MUL - Ops.MSKBSE)) | (1 << (Ops.A_DIV - Ops.MSKBSE)) | (1 << (Ops.A_MOD - Ops.MSKBSE)) | (1 << (Ops.A_PLUS - Ops.MSKBSE)) | (1 << (Ops.A_MINUS - Ops.MSKBSE));
		unaAriCall[StdTypes.T_FLT] |= (1 << (Ops.A_MINUS - Ops.MSKBSE));
		unaAriCall[StdTypes.T_DBL] |= (1 << (Ops.A_MINUS - Ops.MSKBSE));
	}
	
	public static void printParameter(TextPrinter v)
	{
		v.println("   rjih - use rjmp in header");
	}
	
	public boolean setParameter(String parm, TextPrinter v)
	{
		if ("rjih".equals(parm))
		{
			v.println("using rjmp in header");
			headerWithRJMP = true;
		}
		else
		{
			v.println("invalid parameter for ATmega, possible parameters:");
			printParameter(v);
			return false;
		}
		return true;
	}
	
	public void setHeaderLength(int length)
	{
		headerLength = length;
	}
	
	public void putRef(Object loc, int offset, Object ptr, int ptrOff)
	{
		mem.putShort(loc, offset, (short) mem.getAddrAsInt(ptr, ptrOff));
	}
	
	public void putCodeRef(Object loc, int offset, Object ptr, int ptrOff)
	{
		int destAddr;
		if (headerWithRJMP && (destAddr = mem.getAddrAsInt(loc, offset)) < headerLength)
		{
			//this is a very special case: code a rjmp inclusive opcode
			int value = ((mem.getAddrAsInt(ptr, ptrOff) - destAddr - 2) >>> 1) & 0xFFF;
			mem.putShort(loc, offset, (short) (0xC000 | value));
		}
		else
			mem.putShort(loc, offset, (short) (mem.getAddrAsInt(ptr, ptrOff) >>> 1));
	}
	
	public int prepareFreeReg(int avoidReg1, int avoidReg2, int reUseReg, int type)
	{
		int toStore, ret, mask, i;
		//first: try to reuse given regs
		reUseReg &= RegAll;
		if (reUseReg != 0)
		{
			if ((ret = freeRegSearch(reUseReg, type)) != 0)
			{
				usedRegs |= ret;
				nextAllocReg = ret;
				return 0; //nothing has to be freed, reuse given registers
			}
		}
		//second: try to alloc at even register numbers if type requires more than one register
		if (getByteCount(type) > 1)
		{
			mask = (RegAll & ~usedRegs & ~(avoidReg1 | avoidReg2)) | reUseReg;
			for (i = 0; i < 32; i += 2)
				if ((mask & (3 << i)) != (3 << i))
					mask &= ~(3 << i);
			if ((ret = freeRegSearch(mask, type)) != 0)
			{
				usedRegs |= ret;
				nextAllocReg = ret;
				return 0; //nothing has to be freed, use newly allocated registers
			}
		}
		//third: try to alloc everywhere normally
		if ((ret = freeRegSearch((RegAll & ~usedRegs & ~(avoidReg1 | avoidReg2)) | reUseReg, type)) != 0)
		{
			usedRegs |= ret;
			nextAllocReg = ret;
			return 0; //nothing has to be freed, use newly allocated registers
		}
		//fourth: try to free a register
		if ((ret = freeRegSearch((RegAll & ~(avoidReg1 | avoidReg2)) | reUseReg, type)) != 0)
		{
			toStore = storeReg(ret);
			deallocReg(toStore);
			usedRegs |= ret;
			nextAllocReg = ret;
			return toStore;
		}
		//no possibility found to free registers
		fatalError("unsolvable register request");
		return 0;
	}
	
	public int allocReg()
	{
		return nextAllocReg;
	}
	
	protected void deallocReg(int regs)
	{
		usedRegs &= ~regs;
		writtenRegs &= ~regs;
	}
	
	private int freeRegSearch(int mask, int type)
	{
		int ret = 0, temp, regCount;
		regCount = getByteCount(type);
		while (regCount-- > 0)
		{
			if ((temp = bitSearch(mask, 1)) == 0)
				return 0;
			ret |= temp;
			mask &= ~temp;
		}
		return ret;
	}
	
	private int bitSearch(int value, int hit)
	{
		int i = 16, j;
		do
		{
			j = 1 << i;
			if ((value & j) != 0)
			{
				if (--hit == 0)
					return j;
			}
			i = (i + 1) & 0x1F;
		} while (i != 16);
		//nothing found, sorry
		return 0;
	}
	
	private int storeReg(int regs)
	{
		int stored = 0, i;
		regs &= usedRegs & writtenRegs;
		for (i = 0; i < 32; i++)
		{
			if ((regs & (1 << i)) != 0)
			{
				ins(I_PUSHreg, i, 0, 0);
				stored |= (1 << i);
				stackLevel++;
			}
		}
		return stored;
	}
	
	private void restoreReg(int regs)
	{
		int i;
		usedRegs |= regs;
		writtenRegs |= regs;
		for (i = 31; i >= 0; i--)
		{
			if ((regs & (1 << i)) != 0)
			{
				ins(I_POPreg, i, 0, 0);
				stackLevel--;
			}
		}
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
	
	protected int getReg(int nr, int reg, int type, boolean firstWrite)
	{
		if (nr < 1 || nr > 8)
		{
			fatalError("invalid call to getReg");
			return -1;
		}
		reg = bitSearch(reg, nr);
		if (firstWrite)
		{
			writtenRegs |= reg;
			usedRegs |= reg;
		}
		if (reg != 0)
		{
			return getBitPos(reg);
		}
		fatalError("register not found in getReg");
		return -1;
	}
	
	protected int getByteCount(int type)
	{
		switch (type)
		{
			case StdTypes.T_BOOL:
			case StdTypes.T_BYTE:
				return 1;
			case StdTypes.T_CHAR:
			case StdTypes.T_PTR:
			case StdTypes.T_SHRT:
				return 2;
			case StdTypes.T_INT:
			case StdTypes.T_DPTR:
			case StdTypes.T_FLT:
				return 4;
			case StdTypes.T_LONG:
			case StdTypes.T_DBL:
				return 8;
		}
		fatalError("invalid type in getRegCount");
		return -1;
	}
	
	public void deallocRestoreReg(int deallocRegs, int keepRegs, int restore)
	{
		deallocReg(deallocRegs & ~(keepRegs | restore));
		restoreReg(restore);
		usedRegs |= (keepRegs | restore) & RegAll;
	}
	
	public int ensureFreeRegs(int ignoreReg1, int ignoreReg2, int keepReg1, int keepReg2)
	{
		int restore = storeReg(RegAll & ~(ignoreReg1 | ignoreReg2));
		usedRegs = (keepReg1 | keepReg2) & RegAll;
		return restore;
	}
	
	public Mthd prepareMethodCoding(Mthd mthd)
	{
		Mthd lastMthd;
		
		writtenRegs = usedRegs = 0;
		if ((lastMthd = curMthd) != null)
		{
			curVarOffParam = VAROFF_PARAM_INL;
			curInlineLevel++;
		}
		else
		{
			mthdContainer = mthd;
			curVarOffParam = VAROFF_PARAM_NRM;
			stackLevel = 0;
			outestMthdName = mthd.name;
		}
		curMthd = mthd;
		return lastMthd;
	}
	
	public void codeProlog()
	{
		Instruction loopDest;
		int i;
		if (curInlineLevel > 0 && curMthd.parSize == 0 && curMthd.varSize == 0)
		{ //optimize inline methods without parameters and variables
			return;
		}
		if ((curMthd.marker & Marks.K_INTR) != 0)
		{
			if (curInlineLevel > 0)
			{
				fatalError("interrupt method can not be inlined");
				return;
			}
			if (curMthd.parSize != 0)
			{
				fatalError("interrupt method can not have parameters");
				return;
			}
			for (i = 0; i <= 31; i++)
				ins(I_PUSHreg, i, 0, 0);
			ins(I_INregimm, R_HLP, 0, 0x3F); //in R_HLP,SREG (SREG must be stored)
			ins(I_PUSHreg, R_HLP, 0, 0);
		}
		ins(I_PUSHreg, R_YH, 0, 0); //save old BSE
		ins(I_PUSHreg, R_YL, 0, 0);
		if (curMthd.varSize > 0)
		{
			if (curMthd.varSize <= 4)
				for (i = 0; i < curMthd.varSize; i++)
					ins(I_PUSHreg, R_ZERO, 0, 0);
			else
			{
				if (curMthd.varSize > 255)
				{
					fatalError("maximum of 255 bytes for local variables allowed");
					return;
				}
				ins(I_LDIregimm, R_HLP, 0, curMthd.varSize);
				appendInstruction(loopDest = getUnlinkedInstruction());
				ins(I_PUSHreg, R_ZERO, 0, 0);
				ins(I_DECreg, R_HLP, 0, 0);
				insJump(loopDest, Ops.C_NE);
			}
		}
		ins(I_INregimm, R_YL, 0, 0x3D); //stack pointer in BSE
		ins(I_INregimm, R_YH, 0, 0x3E);
	}
	
	public void codeEpilog(Mthd outline)
	{
		int i;
		
		if (outline != null && curMthd.parSize == 0 && curMthd.varSize == 0)
		{ //optimize inline methods without parameters and variables
			if (--curInlineLevel == 0)
				curVarOffParam = VAROFF_PARAM_NRM;
			curMthd = outline;
			return;
		}
		if (curMthd.varSize != 0)
		{
			if (curMthd.varSize > 3)
			{
				if (curMthd.varSize < 64)
					ins(I_ADDIWregimm, R_YL, 0, curMthd.varSize); //add var-count to Z-register
				else
				{
					ins(I_SUBIregimm, R_YL, 0, (-curMthd.varSize) & 0xFF);
					ins(I_SBCIregimm, R_YH, 0, ((-curMthd.varSize) >>> 8) & 0xFF);
				}
				ins(I_INregimm, R_HLP, 0, 0x3F); //get SREG
				ins(I_CLI, 0, 0, 0); //clear interrupts for stack pointer manipulation
				ins(I_OUTimmreg, R_YH, 0, 0x3E); //set high byte of stack pointer
				ins(I_OUTimmreg, R_HLP, 0, 0x3F); //set SREG to (perhaps) reenable interrupts after the next instruction
				ins(I_OUTimmreg, R_YL, 0, 0x3D); //set low byte of stack pointer
			}
			else
				for (i = 0; i < curMthd.varSize; i++)
					ins(I_POPreg, R_HLP, 0, 0);
		}
		ins(I_POPreg, R_YL, 0, 0);
		ins(I_POPreg, R_YH, 0, 0);
		if ((curMthd.marker & Marks.K_INTR) != 0)
		{
			ins(I_POPreg, R_HLP, 0, 0);
			ins(I_OUTimmreg, R_HLP, 0, 0x3F); //out SREG,R_HLP (SREG must be restored)
			for (i = 31; i >= 0; i--)
				ins(I_POPreg, i, 0, 0);
			ins(I_RETI, 0, 0, 0);
			curMthd = null;
		}
		else if (outline != null)
		{
			if (--curInlineLevel == 0)
				curVarOffParam = VAROFF_PARAM_NRM;
			insCleanStackAfterCall(curMthd.parSize);
			curMthd = outline;
		}
		else
		{
			ins(I_RET, 0, 0, 0);
			curMthd = null;
			if (stackLevel != 0)
			{
				fatalError("stackLevel!=0 in ATmega.codeEpilog");
			}
		}
	}
	
	public void finalizeInstructions(Instruction first)
	{
		Instruction now;
		boolean redo;
		//fix jumps
		do
		{
			now = first;
			redo = false;
			while (now != null)
			{
				if (now.type == I_JUMP)
					redo |= fixJump(now);
				else if (now.type == I_ADDpatched)
				{
					redo |= fixAdd(now);
					now = now.next; //skip I_IHELPER
				}
				else if (now.type == I_STEXreg)
				{
					redo |= fixStackExtremeAdd(now);
				}
				if ((now.size & 1) != 0)
				{
					ctx.out.print("warning: odd size of instruction in method ");
					ctx.out.println(outestMthdName);
				}
				now = now.next;
			}
		} while (redo);
		//print disassembly if wanted
		if (ctx.printCode || (mthdContainer.marker & Marks.K_PRCD) != 0)
			printCode(first, "ATmega-out");
	}
	
	public void genAssign(int dst, int src, int type)
	{
		int i, regCount, reg1, reg2;
		regCount = getByteCount(type);
		if ((reg1 = getReg(1, dst, StdTypes.T_PTR, false)) == -1 || (reg2 = getReg(2, dst, StdTypes.T_PTR, false)) == -1)
			return;
		insMOVpair(R_ZH, R_ZL, reg2, reg1);
		if (regCount == 1)
		{
			if ((reg1 = getReg(1, src, type, false)) == -1)
				return;
			ins(I_STZ_INCreg, reg1, 0, 0);
		}
		else
		{
			ins(I_ADDIWregimm, R_ZL, 0, regCount); //write access to memory has to be in high-low-order
			for (i = regCount; i > 0; i--)
			{
				if ((reg1 = getReg(i, src, type, false)) == -1)
					return;
				ins(I_STZ_DECreg, reg1, 0, 0);
			}
		}
	}
	
	public void genBinOp(int dst, int src1, int src2, int op, int type)
	{
		int opType = op >>> 16, opPar = op & 0xFFFF;
		int dstR, srcR, stored, i, count;
		Instruction dummy, end;
		if (dst != src1)
		{
			fatalError("unsupported case with dst!=src1 for genBinOp");
			return;
		}
		count = getByteCount(type);
		switch (type)
		{
			case StdTypes.T_BOOL:
				if (opType != Ops.S_ARI || !(opPar == Ops.A_AND || opPar == Ops.A_OR || opPar == Ops.A_XOR))
				{
					fatalError("unsupported operation for bool-genBinOp");
					return;
				}
				//has to do the following, too!
			case StdTypes.T_BYTE:
				if (opPar == Ops.A_MUL)
				{ //special case: mul for byte is supported
					if ((dstR = getReg(1, dst, StdTypes.T_BYTE, false)) == -1 || (srcR = getReg(1, src2, StdTypes.T_BYTE, false)) == -1)
						return;
					if (dstR != 0)
						stored = storeReg(3);
					else
						stored = storeReg(2);
					ins(I_MULregreg, dstR, srcR, 0); //requires in-registers >=16, output in r1:r0
					if (dstR != 0)
						ins(I_MOVregreg, dstR, 0, 0);
					restoreReg(stored);
					return;
				}
				//has to do the following, too!
			case StdTypes.T_SHRT:
			case StdTypes.T_CHAR:
			case StdTypes.T_INT:
			case StdTypes.T_LONG:
				switch (opType)
				{
					case Ops.S_ARI:
						for (i = 1; i <= count; i++)
						{
							if ((dstR = getReg(i, dst, type, false)) == -1 || (srcR = getReg(i, src2, type, false)) == -1)
								return;
							switch (opPar)
							{
								case Ops.A_AND:
									ins(I_ANDregreg, dstR, srcR, 0);
									break;
								case Ops.A_OR:
									ins(I_ORregreg, dstR, srcR, 0);
									break;
								case Ops.A_XOR:
									ins(I_EORregreg, dstR, srcR, 0);
									break;
								case Ops.A_MINUS: //no minus with carry->invert and add
									if (i == 1)
										ins(I_SUBregreg, dstR, srcR, 0);
									else
										ins(I_SBCregreg, dstR, srcR, 0);
									break;
								case Ops.A_PLUS:
									if (i == 1)
										ins(I_ADDregreg, dstR, srcR, 0);
									else
										ins(I_ADCregreg, dstR, srcR, 0);
									break;
								default:
									fatalError("unsupported ari-operation for genBinOp");
									return;
							}
						}
						return;
					case Ops.S_BSH:
						end = getUnlinkedInstruction();
						if ((srcR = getReg(1, src2, StdTypes.T_INT, false)) == -1)
							return;
						insBSHOpHint(opPar, type, dst, srcR, end); //attention: dst is register mask, srcR is 8 register number!
						ins(I_CPregreg, srcR, R_ZERO, 0); //compare second operand
						insJump(end, Ops.C_EQ);
						dummy = getUnlinkedInstruction();
						appendInstruction(dummy); //redo jump destination
						if ((dstR = getReg(opPar == Ops.B_SHL ? 1 : count, dst, type, false)) == -1)
							return; //first run - omit carry
						switch (opPar)
						{
							case Ops.B_SHL:
								ins(I_LSLreg, dstR, 0, 0);
								i = 2;
								break;
							case Ops.B_SHRL:
								ins(I_LSRreg, dstR, 0, 0);
								i = count - 1;
								break;
							case Ops.B_SHRA:
								ins(I_ASRreg, dstR, 0, 0);
								i = count - 1;
								break;
							default:
								fatalError("unsupported bsh-operation for genBinOp");
								return;
						}
						while (--count > 0)
						{ //higher bytes - respect carry
							if ((dstR = getReg(i, dst, type, false)) == -1)
								return;
							if (opPar == Ops.B_SHL)
							{
								ins(I_ROLreg, dstR, 0, 0);
								i++;
							}
							else
							{
								ins(I_RORreg, dstR, 0, 0);
								i--;
							}
						}
						ins(I_DECreg, srcR, 0, 0); //decrease counter
						insJump(dummy, Ops.C_NE); //BRNE //if necessary jump to redo
						appendInstruction(end);
						return;
					default:
						fatalError("unsupported operation-type for genBinOp");
						return;
				}
			default:
				fatalError("unsupported operand-type for genBinOp");
		}
	}
	
	public void genCall(int off, int clssReg, int parSize)
	{
		int i, j;
		if ((i = getReg(1, clssReg, StdTypes.T_PTR, false)) == -1 || (j = getReg(2, clssReg, StdTypes.T_PTR, false)) == -1)
			return;
		insMOVpair(R_ZH, R_ZL, j, i);
		if (off >= 0 && off < 64)
			ins(I_ADDIWregimm, R_ZL, 0, off);
		else
		{
			ins(I_SUBIregimm, R_ZL, 0, (-off) & 0xFF);
			ins(I_SBCIregimm, R_ZH, 0, ((-off) >>> 8) & 0xFF); //Z pointing to clssReg+off
		}
		ins(I_LPM_INCreg, R_HLP, 0, 0);
		ins(I_LPMreg, R_ZH, 0, 0);
		ins(I_MOVregreg, R_ZL, R_HLP, 0); //Z contains [clssReg+off]
		if (ctx.codeStart != 0)
		{
			if (ctx.codeStart < 64)
				ins(I_ADDIWregimm, R_ZL, 0, ctx.codeStart);
			else
			{
				ins(I_SUBIregimm, R_ZL, 0, (-ctx.codeStart) & 0xFF); //Z == [clssReg+off]+codestart
				ins(I_SBCIregimm, R_ZH, 0, ((-ctx.codeStart) >>> 8) & 0xFF);
			}
		}
		ins(I_LSRreg, R_ZH, 0, 0); //correct address for code-access
		ins(I_RORreg, R_ZL, 0, 0); //RegZ>>>1
		ins(I_ICALL, 0, 0, parSize);
		insCleanStackAfterCall(parSize);
	}
	
	public void genCallConst(Mthd obj, int parSize)
	{
		insPatchedCall(obj, parSize);
		insCleanStackAfterCall(parSize);
	}
	
	public void genCallIndexed(int intfReg, int off, int parSize)
	{
		int i, j;
		if ((i = getReg(1, intfReg, StdTypes.T_DPTR, false)) == -1)
			return;
		if ((j = getReg(2, intfReg, StdTypes.T_DPTR, false)) == -1)
			return;
		insMOVpair(R_ZH, R_ZL, j, i);
		if (off > 0 && off < 64)
			ins(I_ADDIWregimm, R_ZL, 0, off);
		else if (off != 0)
		{
			ins(I_SUBIregimm, R_ZL, 0, (-off) & 0xFF); // add lower byte
			ins(I_SBCIregimm, R_ZH, 0, ((-off) >>> 8) & 0xFF); //Z==intfreg+off
		}
		ins(I_LPM_INCreg, R_HLP, 0, 0); //load program memory!
		ins(I_LPMreg, R_ZH, 0, 0);
		ins(I_MOVregreg, R_ZL, R_HLP, 0); //Z == [intfReg+off]
		ins(I_ADDregreg, R_ZL, R_CLSL, 0);
		ins(I_ADCregreg, R_ZH, R_CLSH, 0); //Z+=clssDsc
		ins(I_LPM_INCreg, R_HLP, 0, 0);
		ins(I_LPMreg, R_ZH, 0, 0); //load program memory!
		ins(I_MOVregreg, R_ZL, R_HLP, 0); //Z=[Z]
		if (ctx.codeStart != 0)
		{
			if (ctx.codeStart < 64)
				ins(I_ADDIWregimm, R_ZL, 0, ctx.codeStart);
			else
			{
				ins(I_SUBIregimm, R_ZL, 0, (-ctx.codeStart) & 0xFF); //Z+=codeStart
				ins(I_SBCIregimm, R_ZH, 0, ((-ctx.codeStart) >>> 8) & 0xFF);
			}
		}
		ins(I_ICALL, 0, 0, parSize); //call [Z]
		insCleanStackAfterCall(parSize);
	}
	
	public void genCheckBounds(int addrReg, int offReg, int checkToOffset, Instruction onSuccess)
	{
		int i, j;
		if ((i = getReg(1, addrReg, StdTypes.T_PTR, false)) == -1 || (j = getReg(2, addrReg, StdTypes.T_PTR, false)) == -1)
			return;
		insMOVpair(R_ZH, R_ZL, j, i);
		if (checkToOffset > 0 && checkToOffset < 64)
			ins(I_ADDIWregimm, R_ZL, 0, checkToOffset);
		else if (checkToOffset != 0)
		{
			ins(I_LDIregimm, R_HLP, 0, checkToOffset & 0xFF); //add offset of reference value
			ins(I_ADDregreg, R_ZL, R_HLP, 0);
			ins(I_LDIregimm, R_HLP, 0, (checkToOffset >>> 8) & 0xFF);
			ins(I_ADCregreg, R_ZH, R_HLP, 0); //Z contains addrReg+checkToOffset+2
		}
		ins(nextValInFlash ? I_LPM_INCreg : I_LDZ_INCreg, R_HLP, 0, 0); //read low byte with post-increment
		ins(I_CPregreg, getReg(1, offReg, StdTypes.T_INT, false), R_HLP, 0); //compare low byte
		ins(nextValInFlash ? I_LPM_INCreg : I_LDZ_INCreg, R_HLP, 0, 0); //read high byte
		ins(I_CPCregreg, getReg(2, offReg, StdTypes.T_INT, false), R_HLP, 0); //compare high byte
		insJump(onSuccess, Ops.C_BO);
		nextValInFlash = false;
	}
	
	public void genCheckStackExtreme(int maxValueReg, Instruction onSuccess)
	{
		if (maxValueReg != 0x00030000)
		{
			fatalError("invalid maxValueReg for genCheckStackExtreme");
			return;
		}
		ins(I_STEXreg, 16, 0, 0); //placeholder for stack extreme add (first instruction)
		ins(I_STEXreg, 17, 0, 0); //placeholder for stack extreme add (second instruction)
		ins(I_INregimm, 18, 0, 0x3D); //stack pointer in r18/r19
		ins(I_INregimm, 19, 0, 0x3E);
		ins(I_CPregreg, 16, 18, 0); //compare increased memory pointer with stack pointer
		ins(I_CPCregreg, 17, 19, 0);
		insJump(onSuccess, Ops.C_BO); //if not below or equal, jump to success mark
	}
	
	public int genCompPtrToNull(int reg, int cond)
	{
		int tmp;
		if ((tmp = getReg(1, reg, StdTypes.T_PTR, false)) == -1 || ((reg = getReg(2, reg, StdTypes.T_PTR, false))) == -1)
			return 0;
		ins(I_CPregreg, tmp, R_ZERO, 0);
		ins(I_CPCregreg, reg, R_ZERO, 0);
		return cond;
	}
	
	public int genComp(int src1, int src2, int type, int cond)
	{
		int i, max, r1, r2;
		boolean withCarry = false;
		
		switch (cond)
		{ //replace unsupported jumps by flipping registers and changing condition
			case Ops.C_GT: //unsupported jump #1 GT->LW
				cond = Ops.C_LW;
				i = src1;
				src1 = src2;
				src2 = i;
				break;
			case Ops.C_LE: //unsupported jump #2 LE->GE
				cond = Ops.C_GE;
				i = src1;
				src1 = src2;
				src2 = i;
				break;
		}
		
		max = getByteCount(type);
		for (i = 1; i <= max; i++)
		{
			if ((r1 = getReg(i, src1, type, false)) == -1 || (r2 = getReg(i, src2, type, false)) == -1)
				return 0;
			ins(withCarry ? I_CPCregreg : I_CPregreg, r1, r2, 0);
			withCarry = true;
		}
		return cond;
	}
	
	public int genCompValToConstVal(int src, int val, int type, int cond)
	{
		int i, max, v, r1, r2;
		boolean withCarry = false;
		max = getByteCount(type);
		for (i = 1; i <= max; i++)
		{
			if ((r1 = getReg(i, src, type, false)) == -1)
				return 0;
			if ((v = val >>> ((i - 1) << 3)) != 0)
				ins(I_LDIregimm, r2 = R_HLP, 0, v & 0xFF);
			else
				r2 = R_ZERO;
			ins(withCarry ? I_CPCregreg : I_CPregreg, r1, r2, 0);
			withCarry = true;
		}
		return cond;
	}
	
	public int genCompValToConstDoubleOrLongVal(int src, long val, boolean asDouble, int cond)
	{
		boolean withCarry = false;
		int i, v, r1, r2;
		for (i = 1; i <= 8; i++)
		{
			if ((r1 = getReg(i, src, StdTypes.T_LONG, false)) == -1)
				return 0;
			if ((v = (int) (val >>> ((i - 1) << 3))) != 0)
				ins(I_LDIregimm, r2 = R_HLP, 0, v & 0xFF);
			else
				r2 = R_ZERO;
			ins(withCarry ? I_CPCregreg : I_CPregreg, r1, r2, 0);
			withCarry = true;
		}
		return cond;
	}
	
	public void genConvertVal(int dst, int src, int toType, int fromType)
	{
		int countFrom, countTo, regDst, regSrc = 0, i = 1, temp;
		Instruction dest;
		countFrom = getByteCount(fromType);
		countTo = getByteCount(toType);
		temp = countFrom >= countTo ? countTo : countFrom; //determine minimum value
		for (; i <= temp; i++)
		{ //take care of lower bytes
			if ((regDst = getReg(i, dst, toType, true)) == -1 || (regSrc = getReg(i, src, fromType, false)) == -1)
				return;
			if (regDst != regSrc)
				ins(I_MOVregreg, regDst, regSrc, 0);
		}
		if (countTo > countFrom)
		{ //conversion was an expansion
			if (fromType != StdTypes.T_CHAR)
			{ //not char -> sign extension
				ins(I_EORregreg, R_HLP, R_HLP, 0);
				ins(I_SBRCregimm, regSrc, 0, 7, dest = getUnlinkedInstruction());
				ins(I_COMreg, R_HLP, 0, 0);
				appendInstruction(dest);
				for (; i <= countTo; i++)
				{
					if ((regDst = getReg(i, dst, toType, true)) == -1)
						return;
					ins(I_MOVregreg, regDst, R_HLP, 0);
				}
			}
			else
			{ //char -> zero extension
				for (; i <= countTo; i++)
				{
					if ((regDst = getReg(i, dst, toType, true)) == -1)
						return;
					ins(I_MOVregreg, regDst, R_ZERO, 0);
				}
			}
		}
	}
	
	public void genDup(int dst, int src, int type)
	{
		int srcReg, dstReg, regCount, i;
		if (dst == src)
			return;
		regCount = getByteCount(type);
		for (i = 1; i <= regCount; i++)
		{
			if ((dstReg = getReg(i, dst, type, true)) == -1 || (srcReg = getReg(i, src, type, false)) == -1)
				return;
			if (dstReg != srcReg)
				ins(I_MOVregreg, dstReg, srcReg, 0);
		}
	}
	
	public void genDecMem(int dst, int type)
	{
		int count, i, j;
		count = getByteCount(type);
		if ((i = getReg(1, dst, StdTypes.T_PTR, false)) == -1 || (j = getReg(2, dst, StdTypes.T_PTR, false)) == -1)
			return;
		insMOVpair(R_ZH, R_ZL, j, i);
		ins(I_LDZreg, R_HLP, 0, 0); //first byte
		ins(I_SUBIregimm, R_HLP, 0, 0x01); //sub 1
		ins(I_STZ_INCreg, R_HLP, 0, 0);
		for (i = 2; i <= count; i++)
		{
			ins(I_LDZreg, R_HLP, 0, 0);
			ins(I_SBCregreg, R_HLP, R_ZERO, 0); //sub reg0==0 with carry
			ins(I_STZ_INCreg, R_HLP, 0, 0);
		}
	}
	
	public void genIncMem(int dst, int type)
	{
		int count, i, j;
		count = getByteCount(type);
		if ((i = getReg(1, dst, StdTypes.T_PTR, false)) == -1 || (j = getReg(2, dst, StdTypes.T_PTR, false)) == -1)
			return;
		insMOVpair(R_ZH, R_ZL, j, i);
		ins(I_LDZreg, R_HLP, 0, 0); //first byte
		ins(I_SUBIregimm, R_HLP, 0, 0xFF); //sub -1
		ins(I_STZ_INCreg, R_HLP, 0, 0);
		for (i = 2; i <= count; i++)
		{
			ins(I_LDZreg, R_HLP, 0, 0);
			ins(I_SBCIregimm, R_HLP, 0, 0xFF); //sub 0xFF with carry
			ins(I_STZ_INCreg, R_HLP, 0, 0);
		}
	}
	
	public void genJmp(Instruction dest)
	{
		insJump(dest, 0);
	}
	
	public void genCondJmp(Instruction dest, int cond)
	{
		insJump(dest, cond);
	}
	
	public void genLoadUnitContext(int dst, int off)
	{
		fatalError("genLoadUnitContext not implemented yet");
		//TODO existing implementation of genLoadUnitContext does not work - and not yet needed at the moment
    /*
    ins(I_MOVregreg, R_HLP, R_CLSL, 0); //load CLSL
    if (off!=0) ins(I_SUBIregimm, R_HLP, 0, -off); //add offset
    if (replace) ins(I_MOVregreg, R_CLSL, R_HLP, 0);
    else ins(I_MOVregreg, getReg(1, dst, StdTypes.T_PTR, false), R_HLP, 0);
    ins(I_MOVregreg, R_HLP, R_CLSH, 0); //load CLSH
    if (off!=0) ins(I_SBCIregimm, R_HLP, 0, 0xFF); //carry untouched by MOV => carry from SUBI
    if (replace) ins(I_MOVregreg, R_CLSH, R_HLP, 0);
    else ins(I_MOVregreg, getReg(2, dst, StdTypes.T_PTR, false), R_HLP, 0);
    */
	}
	
	public void genLoadConstUnitContext(int dst, Object unitLoc)
	{
		int reg, unitAddr = mem.getAddrAsInt(unitLoc, 0);
		if ((reg = getReg(1, dst, StdTypes.T_PTR, false)) == -1)
			return;
		if (reg < 16)
		{
			ins(I_LDIregimm, R_HLP, 0, unitAddr & 0xFF);
			ins(I_MOVregreg, reg, R_HLP, 0);
		}
		else
			ins(I_LDIregimm, reg, 0, unitAddr & 0xFF);
		if ((reg = getReg(2, dst, StdTypes.T_PTR, false)) == -1)
			return;
		if (reg < 16)
		{
			ins(I_LDIregimm, R_HLP, 0, (unitAddr >>> 8) & 0xFF);
			ins(I_MOVregreg, reg, R_HLP, 0);
		}
		else
			ins(I_LDIregimm, reg, 0, (unitAddr >>> 8) & 0xFF);
	}
	
	public void genLoadConstDoubleOrLongVal(int dst, long val, boolean asDouble)
	{
		int reg, i, tempVal;
		for (i = 0; i < 8; i++)
		{
			if ((reg = getReg(i + 1, dst, StdTypes.T_LONG, true)) == -1)
				return;
			tempVal = (int) (val >>> (i << 3)) & 0xFF; //determine value to load
			if (tempVal == 0)
				ins(I_EORregreg, reg, reg, 0);
			else if (reg < 16)
			{ //ldi only for regs 16..31
				ins(I_LDIregimm, R_HLP, 0, tempVal);
				ins(I_MOVregreg, reg, R_HLP, 0);
			}
			else
				ins(I_LDIregimm, reg, 0, tempVal);
		}
	}
	
	public void genLoadConstVal(int dst, int val, int type)
	{
		int reg, count, i, tempVal;
		if ((count = getByteCount(type)) == -1)
			return;
		for (i = 0; i < count; i++)
		{
			if ((reg = getReg(i + 1, dst, type, true)) == -1)
				return;
			tempVal = (val >>> (i << 3)) & 0xFF;
			if (tempVal == 0)
				ins(I_EORregreg, reg, reg, 0);
			else if (reg < 16)
			{ //ldi only for regs 16..31
				ins(I_LDIregimm, R_HLP, 0, tempVal);
				ins(I_MOVregreg, reg, R_HLP, 0);
			}
			else
				ins(I_LDIregimm, reg, 0, tempVal);
		}
	}
	
	public void genLoadDerefAddr(int destReg, int objReg, int indReg, int baseOffset, int entrySize)
	{
		int i, j;
		if ((i = getReg(1, indReg, StdTypes.T_SHRT, false)) == -1 || (j = getReg(2, indReg, StdTypes.T_SHRT, false)) == -1)
			return;
		if (entrySize < 0)
		{
			ins(I_EORregreg, R_ZH, R_ZH, 0);
			ins(I_EORregreg, R_ZL, R_ZL, 0);
			ins(I_SUBregreg, R_ZL, i, 0);
			ins(I_SBCregreg, R_ZH, j, 0);
			entrySize = -entrySize;
		}
		else
			insMOVpair(R_ZH, R_ZL, j, i);
		switch (entrySize)
		{
			case 1:
			case 2:
			case 4:
			case 8:
				while (entrySize > 1)
				{
					ins(I_LSLreg, R_ZL, 0, 0);
					ins(I_ROLreg, R_ZH, 0, 0);
					entrySize = entrySize >>> 1;
				} //Z contains indReg*entrySize (entrySize e {1, 2, 4, 8})
				break;
			default:
				fatalError("not supported case in genLoadDerefAddr");
				return;
		}
		if (baseOffset > 0 && baseOffset < 64)
			ins(I_ADDIWregimm, R_ZL, 0, baseOffset);
		else if (baseOffset != 0)
		{
			ins(I_SUBIregimm, R_ZL, 0, (-baseOffset) & 0xFF);
			ins(I_SBCIregimm, R_ZH, 0, ((-baseOffset) >>> 8) & 0xFF);
		} //Z contains indReg*entrySize+baseOffset
		if ((i = getReg(1, destReg, StdTypes.T_PTR, false)) == -1 || (j = getReg(2, destReg, StdTypes.T_PTR, false)) == -1)
			return;
		if (objReg != destReg)
			insMOVpair(j, i, getReg(2, objReg, StdTypes.T_PTR, false), getReg(1, objReg, StdTypes.T_PTR, false));
		ins(I_ADDregreg, i, R_ZL, 0);
		ins(I_ADCregreg, j, R_ZH, 0);
		//addrReg+=indReg*entrySize+baseOffset
	}
	
	public void genLoadInstContext(int src)
	{
		int i, j, offset;
		//load instance context
		if ((i = getReg(1, src, StdTypes.T_PTR, false)) == -1 || (j = getReg(2, src, StdTypes.T_PTR, false)) == -1)
			return;
		insMOVpair(R_INSTH, R_INSTL, j, i);
		//set class context
		offset = getByteCount(StdTypes.T_PTR);
		insMOVpair(R_ZH, R_ZL, R_INSTH, R_INSTL); //Z==instance-ptr
		ins(I_SUBIregimm, R_ZL, 0, offset & 0xFF); //subtract offset to class descriptor
		ins(I_SBCIregimm, R_ZH, 0, (offset >>> 8) & 0xFF);
		ins(I_LDZ_INCreg, R_CLSL, 0, 0); //load class descriptor to RegCLSS
		ins(I_LDZ_INCreg, R_CLSH, 0, 0);
	}
	
	public void genLoadVarAddr(int dst, int src, Object loc, int off)
	{
		int dstReg1, dstReg2, srcReg1, srcReg2, tempReg1, tempReg2, pos;
		if ((dstReg1 = getReg(1, dst, StdTypes.T_PTR, true)) == -1)
			return;
		if ((dstReg2 = getReg(2, dst, StdTypes.T_PTR, true)) == -1)
			return;
		if (dstReg1 < 16)
			tempReg1 = R_ZL; //dst maybe has to be loaded with immediate
		else
			tempReg1 = dstReg1;
		if (dstReg2 < 16)
			tempReg2 = R_ZH;
		else
			tempReg2 = dstReg2;
		pos = mem.getAddrAsInt(loc, off);
		if (src == regBase)
		{
			if (pos >= 0)
				pos += curVarOffParam; //step over old stack pointer, return address for method parameters and saved R_BSE
			else
				pos++;
			pos += curMthd.varSize; //BSE points not to first parameter but last local variable
		}
		if (pos == 0 && src == dst)
			return;
		if (src != 0)
		{
			srcReg1 = getReg(1, src, StdTypes.T_PTR, false); //already checked
			srcReg2 = getReg(2, src, StdTypes.T_PTR, false); //already checked
			if (tempReg1 != srcReg1 && tempReg2 != srcReg2)
				insMOVpair(tempReg2, tempReg1, srcReg2, srcReg1);
			else
			{
				if (tempReg1 != srcReg1)
					ins(I_MOVregreg, tempReg1, srcReg1, 0);
				if (tempReg2 != srcReg2)
					ins(I_MOVregreg, tempReg2, srcReg2, 0);
			}
			if (pos != 0)
			{
				ins(I_SUBIregimm, tempReg1, 0, (-pos) & 0xFF);
				ins(I_SBCIregimm, tempReg2, 0, ((-pos) >>> 8) & 0xFF);
			}
		}
		else
		{
			ins(I_LDIregimm, tempReg1, 0, pos & 0xFF);
			ins(I_LDIregimm, tempReg2, 0, (pos >>> 8) & 0xFF);
		}
		if (dstReg2 != tempReg2 && dstReg1 != tempReg1)
			insMOVpair(dstReg2, dstReg1, tempReg2, tempReg1);
		else
		{
			if (dstReg2 != tempReg2)
				ins(I_MOVregreg, dstReg2, tempReg2, 0);
			if (dstReg1 != tempReg1)
				ins(I_MOVregreg, dstReg1, tempReg1, 0);
		}
	}
	
	public void genLoadVarVal(int dst, int src, Object loc, int off, int type)
	{
		int dstReg, count, i = 0, pos = mem.getAddrAsInt(loc, off);
		count = getByteCount(type);
		if (src == regBase)
		{
			if (pos >= 0)
				pos += curVarOffParam; //step over old stack pointer, return address for method parameters and saved R_BSE
			else
				pos++;
			pos += curMthd.varSize; //BSE points not to first parameter but last local variable
		}
		else if (!nextValInFlash && src == 0 && count <= 2)
		{ //special handling for known constant addresses if byte count is below or equal to 2
			if ((dstReg = getReg(1, dst, type, true)) == -1)
				return;
			ins(I_LDSregimm, dstReg, 0, pos);
			if (count == 2)
			{
				if ((dstReg = getReg(2, dst, type, true)) == -1)
					return;
				ins(I_LDSregimm, dstReg, 0, pos + 1);
			}
			return;
		}
		if (src != 0)
		{
			if (src == RegY && pos >= 0 && (pos + count) < 64)
			{ //use displacement for stack access if possible
				if (nextValInFlash)
				{
					fatalError("src==RegY with nextVarValFromFlash");
					return;
				}
				while (i < count)
				{
					if ((dstReg = getReg(i + 1, dst, type, true)) == -1)
						return;
					ins(I_LDY_DISPregimm, dstReg, 0, pos + i++);
				}
				return;
			} //otherwise use Z register for access
			insMOVpair(R_ZH, R_ZL, getReg(2, src, StdTypes.T_PTR, false), getReg(1, src, StdTypes.T_PTR, false));
			if (pos != 0)
			{
				ins(I_SUBIregimm, R_ZL, 0, (-pos) & 0xFF);
				ins(I_SBCIregimm, R_ZH, 0, ((-pos) >>> 8) & 0xFF);
			}
		}
		else
		{
			ins(I_LDIregimm, R_ZL, 0, pos & 0xFF);
			ins(I_LDIregimm, R_ZH, 0, (pos >>> 8) & 0xFF);
		}
		while (i < count)
		{
			if ((dstReg = getReg(++i, dst, type, true)) == -1)
				return;
			ins(nextValInFlash ? I_LPM_INCreg : I_LDZ_INCreg, dstReg, 0, 0);
		}
		nextValInFlash = false;
	}
	
	public void genMoveFromPrimary(int dst, int type)
	{
		int tempReg, primReg, i = getByteCount(type);
		while (i > 0)
		{
			if ((tempReg = getReg(i, dst, type, false)) == -1)
				return;
			writtenRegs |= (1 << tempReg);
			primReg = R_PRIM_START + --i;
			if (primReg == tempReg)
				continue;
			ins(I_MOVregreg, tempReg, primReg, 0);
		}
	}
	
	public void genMoveIntfMapFromPrimary(int dst)
	{
		int i, j;
		if ((i = getReg(3, dst, StdTypes.T_DPTR, false)) == -1 || (j = getReg(4, dst, StdTypes.T_DPTR, false)) == -1)
			return;
		if (i != R_PRIM_START && j != R_PRIM_START + 1)
			insMOVpair(j, i, R_PRIM_START + 1, R_PRIM_START);
		else
		{
			if (i != R_PRIM_START)
				ins(I_MOVregreg, i, R_PRIM_START, 0);
			if (j != R_PRIM_START + 1)
				ins(I_MOVregreg, j, R_PRIM_START + 1, 0);
		}
	}
	
	public void genMoveToPrimary(int src, int type)
	{
		int tempReg, primReg, i = 1, count;
		for (count = getByteCount(type); i <= count; i++)
		{
			if ((tempReg = getReg(i, src, type, false)) == -1)
				return;
			primReg = R_PRIM_START + i - 1;
			if (tempReg != primReg)
				ins(I_MOVregreg, primReg, tempReg, 0);
		}
	}
	
	public void genSavePrimary(int type)
	{
		int primReg, count;
		stackLevel += count = getByteCount(type);
		for (primReg = R_PRIM_START; count > 0; primReg++, count--)
		{
			ins(I_PUSHreg, primReg, 0, 0);
		}
	}
	
	public void genRestPrimary(int type)
	{
		int primReg, count;
		stackLevel -= count = getByteCount(type);
		for (primReg = R_PRIM_START + count - 1; count > 0; primReg--, count--)
		{
			ins(I_POPreg, primReg, 0, 0);
		}
	}
	
	public void genPop(int dst, int type)
	{
		int i = 0, m;
		stackLevel -= m = getByteCount(type);
		while (i < m)
			ins(I_POPreg, getReg(++i, dst, type, true), 0, 0);
	}
	
	public void genPush(int src, int type)
	{
		int i;
		stackLevel += i = getByteCount(type);
		while (i > 0)
			ins(I_PUSHreg, getReg(i--, src, type, false), 0, 0);
	}
	
	public void genPushConstDoubleOrLongVal(long val, boolean asDouble)
	{
		int i = 8, byteVal;
		stackLevel += 8;
		for (; i > 0; i--)
		{
			if ((byteVal = (int) (val >>> ((i - 1) << 3)) & 0xFF) == 0)
				ins(I_PUSHreg, R_ZERO, 0, 0);
			else
			{
				ins(I_LDIregimm, R_HLP, 0, byteVal);
				ins(I_PUSHreg, R_HLP, 0, 0);
			}
		}
	}
	
	public void genPushConstVal(int val, int type)
	{
		int count, i, byteVal;
		if ((count = getByteCount(type)) == -1)
			return;
		for (stackLevel += i = count; i > 0; i--)
		{
			if ((byteVal = (val >>> ((i - 1) << 3)) & 0xFF) == 0)
				ins(I_PUSHreg, R_ZERO, 0, 0);
			else
			{
				ins(I_LDIregimm, R_HLP, 0, byteVal);
				ins(I_PUSHreg, R_HLP, 0, 0);
			}
		}
	}
	
	public void genRestUnitContext()
	{
		stackLevel -= 2;
		ins(I_POPreg, R_CLSL, 0, 0);
		ins(I_POPreg, R_CLSH, 0, 0);
	}
	
	public void genRestInstContext()
	{
		stackLevel -= 2;
		ins(I_POPreg, R_INSTL, 0, 0);
		ins(I_POPreg, R_INSTH, 0, 0);
	}
	
	public void genSaveUnitContext()
	{
		stackLevel += 2;
		ins(I_PUSHreg, R_CLSH, 0, 0);
		ins(I_PUSHreg, R_CLSL, 0, 0);
	}
	
	public void genSaveInstContext()
	{
		stackLevel += 2;
		ins(I_PUSHreg, R_INSTH, 0, 0);
		ins(I_PUSHreg, R_INSTL, 0, 0);
	}
	
	public void genStoreVarConstDoubleOrLongVal(int objReg, Object loc, int off, long val, boolean asDouble)
	{
		if (objReg != RegY)
		{
			super.genStoreVarConstDoubleOrLongVal(objReg, loc, off, val, asDouble);
			return;
		}
		genStoreVarConstVal(RegY, loc, off + 4, (int) (val >>> 32), StdTypes.T_INT);
		genStoreVarConstVal(RegY, loc, off, (int) val, StdTypes.T_INT);
	}
	
	public void genStoreVarConstVal(int objReg, Object loc, int off, int val, int type)
	{
		int cnt, tempVal, pos;
		if ((cnt = getByteCount(type)) == -1)
			return;
		pos = mem.getAddrAsInt(loc, off);
		if (objReg != RegY || pos + cnt + curMthd.varSize + (pos >= 0 ? curVarOffParam : 1) >= 64)
		{
			super.genStoreVarConstVal(objReg, loc, off, val, type);
			return;
		}
		if (pos >= 0)
			pos += curVarOffParam; //step over old stack pointer, return address for method parameters and saved R_BSE
		else
			pos++;
		pos += curMthd.varSize; //BSE points not to first parameter but last local variable
		while (--cnt >= 0)
		{
			tempVal = (val >>> (cnt << 3)) & 0xFF;
			if (tempVal == 0)
				ins(I_STY_DISPregimm, R_ZERO, 0, pos + cnt);
			else
			{
				ins(I_LDIregimm, R_HLP, 0, tempVal);
				ins(I_STY_DISPregimm, R_HLP, 0, pos + cnt);
			}
		}
	}
	
	public void genStoreVarVal(int objReg, Object loc, int off, int valReg, int type)
	{
		int cnt, reg, pos;
		if ((cnt = getByteCount(type)) == -1)
			return;
		pos = mem.getAddrAsInt(loc, off);
		if (objReg != RegY || pos + cnt + curMthd.varSize + (pos >= 0 ? curVarOffParam : 1) >= 64)
		{
			super.genStoreVarVal(objReg, loc, off, valReg, type);
			return;
		}
		if (pos >= 0)
			pos += curVarOffParam; //step over old stack pointer, return address for method parameters and saved R_BSE
		else
			pos++;
		pos += curMthd.varSize; //BSE points not to first parameter but last local variable
		while (cnt > 0)
		{
			if ((reg = getReg(cnt--, valReg, type, false)) == -1)
				return;
			ins(I_STY_DISPregimm, reg, 0, pos + cnt);
		}
	}
	
	public void genUnaOp(int dst, int src, int op, int type)
	{
		int opPar = op & 0xFFFF, reg, count, i = 1;
		boolean minus;
		if (dst != src)
		{
			fatalError("unsupported case with dst!=src for genUnaOp");
			return;
		}
		if (type == StdTypes.T_BOOL)
		{
			if (opPar == Ops.L_NOT)
			{ //I_EOR doesn't work with imm => get register
				if ((reg = getReg(1, dst, StdTypes.T_BYTE, false)) == -1)
					return;
				ins(I_LDIregimm, R_HLP, 0, 1); //and load with 1
				ins(I_EORregreg, reg, R_HLP, 0);
				return;
			}
			else
			{
				fatalError("unsupported bool-operation for genUnaOp");
				return;
			}
		}
		else if ((minus = (opPar == Ops.A_MINUS)) && type == StdTypes.T_BYTE)
		{
			if ((reg = getReg(1, dst, StdTypes.T_BYTE, false)) == -1)
				return;
			ins(I_NEGreg, reg, 0, 0); //two's complement
		}
		else if (minus || opPar == Ops.A_CPL)
		{
			if (type == StdTypes.T_FLT || type == StdTypes.T_DBL)
			{
				fatalError("unary operator not yet supported for float and double in ATmega-backend");
				return;
			}
			count = getByteCount(type);
			for (; i <= count; i++)
			{
				if ((reg = getReg(i, dst, type, false)) == -1)
					return;
				ins(I_COMreg, reg, 0, 0); //one's complement
			}
			if (minus)
			{ //add 1 to complete value in case of two's complement
				for (i = 1; i <= count; i++)
				{
					if ((reg = getReg(i, dst, type, false)) == -1)
						return;
					ins(I_ADCregreg, reg, R_ZERO, 0); //carry is set by last I_COMreg
				}
			}
			return;
		}
		if (opPar != Ops.A_PLUS)
		{
			fatalError("unsupported operation for genUnaOp");
		}
		//do nothing for Ops.A_PLUS
	}
	
	public void genReadIO(int dst, int src, int type, int memLoc)
	{
		int cnt = 1, count, tempReg, i, j;
		if ((i = getReg(1, src, StdTypes.T_PTR, false)) == -1 || (j = getReg(2, src, StdTypes.T_PTR, false)) == -1)
			return;
		insMOVpair(R_ZH, R_ZL, j, i);
		count = getByteCount(type);
		switch (memLoc)
		{
			case 0: //default == I/O
				//instruction IN only with immediate parameter => detour over LD instruction
				ins(I_ADDIWregimm, R_ZL, 0, 20); //correct address for I/O-access
				while (cnt <= count)
				{
					if ((tempReg = getReg(cnt++, dst, type, true)) == -1)
						return;
					ins(I_LDZ_INCreg, tempReg, 0, 0); //read with post-increment
				}
				break;
			case 1: //flash
				while (cnt <= count)
				{
					if ((tempReg = getReg(cnt++, dst, type, true)) == -1)
						return;
					ins(I_LPM_INCreg, tempReg, 0, 0);
				}
				break;
			default:
				curMthd.printPos(ctx, INVALID_IO_LOC);
				fatalError(null);
				return;
		}
	}
	
	public void genWriteIO(int dst, int src, int type, int memLoc)
	{
		int cnt, tempReg, i, j;
		if ((i = getReg(1, dst, StdTypes.T_PTR, false)) == -1 || (j = getReg(2, dst, StdTypes.T_PTR, false)) == -1)
			return;
		insMOVpair(R_ZH, R_ZL, j, i);
		switch (memLoc)
		{
			case 0: //default == I/O
				//instruction OUT only with immediate parameter => detour over ST instruction
				ins(I_ADDIWregimm, R_ZL, 0, 20); //correct address for I/O-access
				if ((cnt = getByteCount(type)) != 1) //set Z to MSB (write Hi->Lo)
					ins(I_ADDIWregimm, R_ZL, 0, cnt);
				while (cnt >= 1)
				{
					if ((tempReg = getReg(cnt--, src, type, false)) == -1)
						return;
					ins(I_STZ_DECreg, tempReg, 0, 0); //store with pre-decrement
				}
			case 1:
			default:
				curMthd.printPos(ctx, INVALID_IO_LOC);
				fatalError(null);
				return;
		}
	}
	
	public void nextValInFlash()
	{
		nextValInFlash = true;
	}
	
	public void inlineVarOffset(int inlineMode, int objReg, Object loc, int offset, int baseValue)
	{
		Instruction i = null;
		int byteCount = 0, pos = mem.getAddrAsInt(loc, offset);
		
		if (objReg == RegY)
		{
			if (pos >= 0)
				pos += curVarOffParam; //step over old stack pointer, return address for method parameters and saved R_BSE
			else
				pos++;
			pos += curMthd.varSize; //BSE points not to first parameter but last local variable
		}
		switch (inlineMode)
		{
			case 1: //used for LDD rd,[X/Y/Z]+q+1
				pos++;
				//no break, has to do the following too!
			case 0: //used for LDD rd,[X/Y/Z]+q
				pos = baseValue | (pos & 0x07) | ((pos & 0x18) << 7) | ((pos & 0x20) << 8);
				byteCount = 2;
				break;
			default:
				fatalError("inlineVarOffset with invalid inlineMode");
				return;
		}
		while (byteCount-- > 0)
		{
			if (i == null)
			{
				appendInstruction(i = getUnlinkedInstruction());
				i.type = I_MAGC;
			}
			i.putByte(pos & 0xFF);
			pos = pos >>> 8;
			if (i.size + 1 == maxInstrCodeSize)
				i = null;
		}
	}
	
	//space reserved on stack requiring only 6*relocBytes:
	//      opimization stack frame pointer == stack pointer is possible because
	//      (1) all methods (even without locals and parameters) update regY
	//      (2) at start of statements, regY equals stack pointer
	//  excOffset + 5*relocBytes: current exception thrown
	//  excOffset + 4*relocBytes: stack frame == stack pointer for current try-block
	//  excOffset + 3*relocBytes: instance context of current try-block
	//  excOffset + 2*relocBytes: unit context of current try-block
	//  excOffset + 1*relocBytes: code-byte to jump to if exception is thrown
	//  excOffset               : pointer to last excStackFrame
	public void genThrowFrameBuild(int globalAddrReg, Instruction dest, int throwBlockOffset)
	{
		Instruction relaDummy;
		
		if (globalAddrReg != ((1 << 16) | (1 << 17)) || usedRegs != ((1 << 16) | (1 << 17)))
		{ //globalAddrReg is the only allocated register
			fatalError(ERR_INVGLOBADDRREG);
			return;
		}
		throwBlockOffset += curMthd.varSize + 1; //BSE points not to first parameter but last local variable
		//prepare pointers: X to global var, Z to throw-frame
		insMOVpair(R_XH, R_XL, 17, 16);
		insMOVpair(R_ZH, R_ZL, R_YH, R_YL);
		if (throwBlockOffset < 64)
			ins(I_ADDIWregimm, R_ZL, 0, throwBlockOffset);
		else
		{
			ins(I_SUBIregimm, R_ZL, 0, (-throwBlockOffset) & 0xFF);
			ins(I_SBCIregimm, R_ZH, 0, ((-throwBlockOffset) >>> 8) & 0xFF);
		}
		//get global frame pointer, save in current frame, set current frame as global
		ins(I_LDX_INCreg, 18, 0, 0); //load pointer at [X+] into 18/19
		ins(I_LDX_INCreg, 19, 0, 0);
		ins(I_STX_DECreg, 31, 0, 0); //store current Z at [-X]
		ins(I_STX_DECreg, 30, 0, 0);
		ins(I_STZ_INCreg, 18, 0, 0); //store 18/19 at [Z+]
		ins(I_STZ_INCreg, 19, 0, 0);
		//fill throw frame: destination
		ins(I_PUSHip, 0, 0, 0);
		appendInstruction(relaDummy = getUnlinkedInstruction());
		ins(I_POPreg, R_XH, 0, 0);
		ins(I_POPreg, R_XL, 0, 0);
		insPatchedAdd(relaDummy, dest);
		ins(I_STZ_INCreg, R_XL, 0, 0);
		ins(I_STZ_INCreg, R_XH, 0, 0);
		//fill throw frame: current state registers (stack==RegY)
		ins(I_STZ_INCreg, R_CLSL, 0, 0);
		ins(I_STZ_INCreg, R_CLSH, 0, 0);
		ins(I_STZ_INCreg, R_INSTL, 0, 0);
		ins(I_STZ_INCreg, R_INSTH, 0, 0);
		ins(I_STZ_INCreg, R_YL, 0, 0);
		ins(I_STZ_INCreg, R_YH, 0, 0);
		//fill throw frame: reset exception variable
		ins(I_STZ_INCreg, R_ZERO, 0, 0);
		ins(I_STZ_INCreg, R_ZERO, 0, 0);
	}
	
	public void genThrowFrameUpdate(Instruction oldDest, Instruction newDest, int throwBlockOffset)
	{
		if (usedRegs != 0)
		{ //no allocated register
			fatalError(ERR_INVGLOBADDRREG);
			return;
		}
		throwBlockOffset += curMthd.varSize + 1; //BSE points not to first parameter but last local variable
		if (throwBlockOffset < 64 - 1 - 2)
		{
			ins(I_LDY_DISPregimm, R_XL, 0, throwBlockOffset + 2);
			ins(I_LDY_DISPregimm, R_XH, 0, throwBlockOffset + 2 + 1);
			insPatchedAdd(oldDest, newDest);
			ins(I_STY_DISPregimm, R_XL, 0, throwBlockOffset + 2);
			ins(I_STY_DISPregimm, R_XH, 0, throwBlockOffset + 2 + 1);
		}
		else
		{
			insMOVpair(R_ZH, R_ZL, R_YH, R_YL);
			ins(I_SUBIregimm, R_ZL, 0, (-(throwBlockOffset + 2)) & 0xFF);
			ins(I_SBCIregimm, R_ZH, 0, ((-(throwBlockOffset + 2)) >>> 8) & 0xFF);
			ins(I_LDZ_INCreg, R_XL, 0, 0);
			ins(I_LDZ_INCreg, R_XH, 0, 0);
			insPatchedAdd(oldDest, newDest);
			ins(I_STZ_DECreg, R_XH, 0, 0);
			ins(I_STZ_DECreg, R_XL, 0, 0);
		}
	}
	
	public void genThrowFrameReset(int globalAddrReg, int throwBlockOffset)
	{
		if (globalAddrReg != ((1 << 16) | (1 << 17)) || usedRegs != ((1 << 16) | (1 << 17)))
		{ //globalAddrReg is the only allocated register
			fatalError(ERR_INVGLOBADDRREG);
			return;
		}
		throwBlockOffset += curMthd.varSize + 1; //BSE points not to first parameter but last local variable
		if (throwBlockOffset < 64 - 1)
		{
			ins(I_LDY_DISPregimm, 18, 0, throwBlockOffset);
			ins(I_LDY_DISPregimm, 19, 0, throwBlockOffset + 1);
		}
		else
		{
			insMOVpair(R_ZH, R_ZL, R_YH, R_YL);
			ins(I_SUBIregimm, R_ZL, 0, (-throwBlockOffset) & 0xFF);
			ins(I_SBCIregimm, R_ZH, 0, ((-throwBlockOffset) >>> 8) & 0xFF);
			ins(I_LDZ_INCreg, 18, 0, 0);
			ins(I_LDZ_INCreg, 19, 0, 0);
		}
		insMOVpair(R_ZH, R_ZL, 17, 16);
		ins(I_STZ_INCreg, 18, 0, 0);
		ins(I_STZ_INCreg, 19, 0, 0);
	}
	
	//------------------------------------------------------------------
	
	private void insMOVpair(int dh, int dl, int sh, int sl)
	{
		if (dh == sh && dl == sl)
			return;
		if (sl + 1 == sh && (sl & 1) == 0 && dl + 1 == dh && (dl & 1) == 0)
			ins(I_MOVWregreg, dl, sl, 0);
		else
		{
			if (dl != sl)
				ins(I_MOVregreg, dl, sl, 0);
			if (dh != sh)
				ins(I_MOVregreg, dh, sh, 0);
		}
	}
	
	private void insCleanStackAfterCall(int parSize)
	{
		int i;
		
		if (parSize != 0)
		{ //clean parameters from stack
			stackLevel -= parSize;
			if (parSize > 2 && stackLevel == 0)
			{
				ins(I_INregimm, R_HLP, 0, 0x3F); //get SREG
				ins(I_CLI, 0, 0, 0); //clear interrupts for stack pointer manipulation
				ins(I_OUTimmreg, R_YH, 0, 0x3E); //set high byte of stack pointer
				ins(I_OUTimmreg, R_HLP, 0, 0x3F); //set SREG to (perhaps) reenable interrupts after the next instruction
				ins(I_OUTimmreg, R_YL, 0, 0x3D); //set low byte of stack pointer
			}
			else if (parSize > 5)
			{
				ins(I_INregimm, R_ZL, 0, 0x3D);
				ins(I_INregimm, R_ZH, 0, 0x3E);
				if (parSize < 64)
					ins(I_ADDIWregimm, R_ZL, 0, parSize);
				else
				{
					ins(I_SUBIregimm, R_ZL, 0, (-parSize) & 0xFF);
					ins(I_SBCIregimm, R_ZH, 0, ((-parSize) >>> 8) & 0xFF);
				}
				ins(I_INregimm, R_HLP, 0, 0x3F); //get SREG
				ins(I_CLI, 0, 0, 0); //clear interrupts for stack pointer manipulation
				ins(I_OUTimmreg, R_ZH, 0, 0x3E); //set high byte of stack pointer
				ins(I_OUTimmreg, R_HLP, 0, 0x3F); //set SREG to (perhaps) reenable interrupts after the next instruction
				ins(I_OUTimmreg, R_ZL, 0, 0x3D); //set low byte of stack pointer
			}
			else
				for (i = 0; i < parSize; i++)
					ins(I_POPreg, R_HLP, 0, 0);
		}
	}
	
	protected Instruction insPatchedCall(Mthd refMthd, int parSize)
	{
		Instruction i;
		
		i = getUnlinkedInstruction();
		appendInstruction(i);
		i.type = I_CALLpatched;
		i.refMthd = refMthd;
		i.iPar1 = parSize;
		i.putInt(0x0000940E);
		addToCodeRefFixupList(i, 2);
		return i;
	}
	
	//add dest-rela to R_XL/R_XH
	protected void insPatchedAdd(Instruction i1, Instruction i2)
	{
		Instruction i;
		
		//get a new instruction and insert it
		appendInstruction(i = getUnlinkedInstruction());
		i.type = I_ADDpatched;
		i.jDest = i1;
		//do no yet code instruction, just enter worst case size
		i.size = 8;
		//add dummy-instruction for i2
		appendInstruction(i = getUnlinkedInstruction());
		i.type = I_IHELPER;
		i.jDest = i2;
	}
	
	protected void insJump(Instruction dest, int cond)
	{
		Instruction me;
		Instruction helper;
		appendInstruction(me = getUnlinkedInstruction());
		me.size = 2;
		me.type = I_JUMP;
		switch (cond)
		{ //special cases cond==0 and ==Ops.C_BO: no jump helper
			case 0:
			case Ops.C_BO:
				me.iPar1 = cond;
				me.jDest = dest;
				return;
		}
		//to enable conditional jump farer than 64 bytes -> detour over jump construction
		//with helper jumps
		appendInstruction(helper = getUnlinkedInstruction());
		helper.size = 2;
		helper.type = I_JUMP;
		switch (cond)
		{
			//create jump combination for supported jumps:
			//branch with inverse condition after unconditional jump==helper.next
			case Ops.C_LW:
			case Ops.C_EQ:
			case Ops.C_NE:
			case Ops.C_GE:
				me.iPar1 = cond ^ Ops.INVCBIT;
				helper.iPar1 = 0; //unconditional jump
				helper.jDest = dest;
				appendInstruction(helper = getUnlinkedInstruction()); //insert nop to jump to
				me.jDest = helper;
				return;
			case Ops.C_GT: //unsupported jump #1
				me.iPar1 = Ops.C_LW;
				helper.iPar1 = Ops.C_EQ;
				appendInstruction(helper = getUnlinkedInstruction());
				helper.type = I_JUMP;
				helper.iPar1 = 0;
				helper.size = 2;
				helper.jDest = dest;
				appendInstruction(helper = getUnlinkedInstruction());
				me.next.jDest = me.jDest = helper;
				return;
			case Ops.C_LE: //unsupported jump #2
				me.iPar1 = Ops.C_EQ;
				helper.iPar1 = Ops.C_GE;
				appendInstruction(helper = getUnlinkedInstruction());
				helper.type = I_JUMP;
				helper.iPar1 = 0;
				helper.size = 2;
				helper.jDest = dest;
				me.jDest = helper;
				appendInstruction(helper = getUnlinkedInstruction());
				me.next.jDest = helper;
				return;
		}
		ctx.out.println("unsupported jump condition in insJump");
	}
	
	protected int getJumpRel(Instruction from, Instruction to)
	{
		Instruction helper;
		int relative = 0;
		
		if (from == null || to == null)
		{
			//helper jumps do not have destinations
			fatalError(" jump to uninitialized destination");
			return 0;
		}
		//determine offset
		if (to.instrNr <= from.instrNr)
		{
			relative -= (helper = from).size;
			while (helper != to)
			{
				helper = helper.prev;
				if (helper == null)
				{
					fatalError("jump destination before not found");
					return 0;
				}
				relative -= helper.size;
			}
		}
		else
		{
			helper = from.next;
			while (helper != to)
			{
				relative += helper.size;
				helper = helper.next;
				if (helper == null)
				{
					fatalError("jump destination after not found");
					return 0;
				}
			}
		}
		return relative >> 1; //code memory is word addressed, skip jump if relative 0
	}
	
	protected boolean codeJump(Instruction jump, int relative)
	{
		int opcode = 0;
		
		if (jump.iPar1 == 0)
		{
			if (relative < -2048 || relative > 2047)
			{
				fatalError("unconditional jump destination too far away");
				return false;
			}
			relative &= 0xFFF;
			opcode = 0xC000;
		}
		else
		{
			if (relative < -64 || relative > 63)
			{
				fatalError("conditional jump destination too far away");
				return false;
			}
			relative = (relative & 0x7F) << 3;
			switch (jump.iPar1)
			{
				case Ops.C_GE: //jump if greater or equal (signed)
					opcode = 0xF404;
					break;
				case Ops.C_LW: //jump if lower (signed)
					opcode = 0xF004;
					break;
				case Ops.C_BO: //jump if below (unsigned)
					opcode = 0xF000;
					break;
				case Ops.C_NE: //jump if not equal
					opcode = 0xF401;
					break;
				case Ops.C_EQ: //jump if equal
					opcode = 0xF001;
					break;
				default:
					fatalError("conditional jump with unsupported condition");
					return false;
			}
		}
		jump.replaceShort(0, opcode | relative);
		return false;
	}
	
	protected boolean fixJump(Instruction jump)
	{
		int relative;
		
		if ((relative = getJumpRel(jump, jump.jDest)) == 0)
		{ //do not jump to next instruction
			jump.type = I_NONE;
			jump.size = 0;
			return true;
		}
		return codeJump(jump, relative);
	}
	
	protected boolean fixAdd(Instruction me)
	{
		int oldSize = me.size, relative;
		
		relative = getJumpRel(me.jDest, me.next.jDest);
		me.size = 0; //reset instruction size
		if (relative <= 63)
		{
			me.putShort(0x9610 | (relative & 0xF) | ((relative & 0x30) << 2)); //ADDIWregimm X
		}
		else
		{
			//TODO: this is possible without LDI through I_SUBI and I_SBCI
			me.putShort(0xE000 | ((16 - 16) << 4) | (relative & 0xF) | ((relative & 0xF0) << 4)); //LDIregimm
			relative = relative >>> 8;
			me.putShort(0xE000 | ((17 - 16) << 4) | (relative & 0xF) | ((relative & 0xF0) << 4)); //LDIregimm
			me.putShort(0x0C00 | (R_XL << 4) | (16 & 0xF) | ((16 & 0x10) << 5)); //ADDregreg
			me.putShort(0x1C00 | (R_XH << 4) | (17 & 0xF) | ((17 & 0x10) << 5)); //ADCregreg
		}
		return me.size != oldSize;
	}
	
	protected boolean fixStackExtremeAdd(Instruction me)
	{
		int oldSize = me.size + me.next.size, curStackUsage = mthdContainer.varSize, maxStackUsage = 0;
		boolean hasCall = false;
		Instruction check = me;
		if (me.next == null || me.next.type != I_STEXreg)
		{
			fatalError("invalid next instruction for fixStackExtremeAdd");
			return false;
		}
		while (check != null)
		{
			switch (check.type)
			{
				case I_PUSHip:
					curStackUsage += 2;
					break;
				case I_PUSHreg:
					curStackUsage++;
					break;
				case I_POPreg:
					if (check.reg0 != R_HLP)
						curStackUsage--; //helper is only popped for call, which is handled there, and variables at end of the method (not relevant)
					break;
				case I_CALLpatched:
				case I_ICALL:
					hasCall = true;
					curStackUsage -= me.iPar1;
					break;
			}
			if (curStackUsage > maxStackUsage)
				maxStackUsage = curStackUsage;
			check = check.next;
		}
		me.size = 0;
		me.next.size = 0;
		me.type = I_SUBIregimm; //change type, reg0 is already set
		me.next.type = I_SBCIregimm; //change type, reg0 is already set
		if (hasCall)
			maxStackUsage += 2;
		me.iPar1 = (-maxStackUsage) & 0xFF;
		me.next.iPar1 = ((-maxStackUsage) >>> 8) & 0xFF;
		code(me);
		code(me.next);
		return oldSize != me.size + me.next.size;
	}
	
	protected void insBSHOpHint(int opPar, int type, int dst, int src2, Instruction afterwards)
	{
		Instruction i;
		
		appendInstruction(i = getUnlinkedInstruction());
		i.type = I_BSHOPHINT;
		i.reg0 = dst; //attention: mask not register!
		i.reg1 = src2; //normal register number
		i.iPar1 = opPar;
		//i.iPar2 is used in ATmegaOpti, do not use here
		i.iPar3 = type;
		i.jDest = afterwards;
	}
	
	
	protected void ins(int type, int reg0, int reg1, int imm)
	{
		Instruction i;
		
		appendInstruction(i = getUnlinkedInstruction());
		i.type = type;
		i.reg0 = reg0;
		i.reg1 = reg1;
		i.iPar1 = imm;
		code(i);
	}
	
	protected void ins(int type, int reg0, int reg1, int imm, Instruction dest)
	{
		Instruction i;
		
		appendInstruction(i = getUnlinkedInstruction());
		i.type = type;
		i.reg0 = reg0;
		i.reg1 = reg1;
		i.iPar1 = imm;
		i.jDest = dest;
		code(i);
	}
	
	protected void code(Instruction i)
	{
		int reg0, reg1, imm;
		
		reg0 = i.reg0;
		reg1 = i.reg1;
		imm = i.iPar1;
		if (reg0 > 31 || reg1 > 31)
		{
			fatalError("invalid register number in code");
			return;
		}
		switch (i.type)
		{
			case I_LDIregimm:
				i.putShort(0xE000 | ((reg0 - 16) << 4) | (imm & 0xF) | ((imm & 0xF0) << 4));
				return;
			case I_MOVregreg:
				i.putShort(0x2C00 | (reg0 << 4) | (reg1 & 0xF) | ((reg1 & 0x10) << 5));
				return;
			case I_MOVWregreg:
				i.putShort(0x0100 | ((reg0 >>> 1) << 4) | (reg1 >>> 1));
				return;
			case I_PUSHreg:
				i.putShort(0x920F | (reg0 << 4));
				return;
			case I_POPreg:
				i.putShort(0x900F | (reg0 << 4));
				return;
			case I_ANDregreg:
				i.putShort(0x2000 | (reg0 << 4) | (reg1 & 0xF) | ((reg1 & 0x10) << 5));
				return;
			case I_ANDIregimm:
				i.putShort(0x7000 | ((reg0 - 16) << 4) | (imm & 0xF) | ((imm & 0xF0) << 4));
				return;
			case I_ORregreg:
				i.putShort(0x2800 | (reg0 << 4) | (reg1 & 0xF) | ((reg1 & 0x10) << 5));
				return;
			case I_ORIregimm:
				i.putShort(0x6000 | ((reg0 - 16) << 4) | (imm & 0xF) | ((imm & 0xF0) << 4));
				return;
			case I_EORregreg:
				i.putShort(0x2400 | (reg0 << 4) | (reg1 & 0xF) | ((reg1 & 0x10) << 5));
				return;
			case I_INCreg:
				i.putShort(0x9403 | (reg0 << 4));
				return;
			case I_ADCregreg:
				i.putShort(0x1C00 | (reg0 << 4) | (reg1 & 0xF) | ((reg1 & 0x10) << 5));
				return;
			case I_SUBIregimm:
				i.putShort(0x5000 | ((reg0 - 16) << 4) | ((imm & 0xF0) << 4) | (imm & 0xF));
				return;
			case I_SBCIregimm:
				i.putShort(0x4000 | ((reg0 - 16) << 4) | ((imm & 0xF0) << 4) | (imm & 0xF));
				return;
			case I_LDZreg:
				i.putShort(0x8000 | (reg0 << 4));
				return;
			case I_STZ_INCreg:
				i.putShort(0x9201 | (reg0 << 4));
				return;
			case I_SBCregreg:
				i.putShort(0x0800 | (reg0 << 4) | (reg1 & 0xF) | ((reg1 & 0x10) << 5));
				return;
			case I_LDZ_INCreg:
				i.putShort(0x9001 | (reg0 << 4));
				return;
			case I_INregimm:
				i.putShort(0xB000 | (imm & 0xF) | ((imm & 0x30) << 5) | (reg0 << 4));
				return;
			case I_OUTimmreg:
				i.putShort(0xB800 | (imm & 0xF) | ((imm & 0x30) << 5) | (reg0 << 4));
				return;
			case I_CPregreg:
				i.putShort(0x1400 | (reg0 << 4) | (reg1 & 0xF) | ((reg1 & 0x10) << 5));
				return;
			case I_CPCregreg:
				i.putShort(0x0400 | (reg0 << 4) | (reg1 & 0xF) | ((reg1 & 0x10) << 5));
				return;
			case I_CPIregimm:
				i.putShort(0x3000 | ((reg0 - 16) << 4) | ((imm & 0xF0) << 4) | (imm & 0xF));
				return;
			case I_COMreg:
				i.putShort(0x9400 | (reg0 << 4));
				return;
			case I_ADDregreg:
				i.putShort(0x0C00 | (reg0 << 4) | (reg1 & 0xF) | ((reg1 & 0x10) << 5));
				return;
			case I_SUBregreg:
				i.putShort(0x1800 | (reg0 << 4) | (reg1 & 0xF) | ((reg1 & 0x10) << 5));
				return;
			case I_ICALL:
				i.putShort(0x9509);
				return;
			case I_ADDIWregimm:
				if ((imm & ~0x3F) != 0)
				{
					fatalError("invalid immediate for ADDIWregimm");
					return;
				}
				switch (reg0)
				{
					case 24:
						i.putShort(0x9600 | (imm & 0xF) | ((imm & 0x30) << 2));
						return;
					case 26:
						i.putShort(0x9610 | (imm & 0xF) | ((imm & 0x30) << 2));
						return;
					case 28:
						i.putShort(0x9620 | (imm & 0xF) | ((imm & 0x30) << 2));
						return;
					case 30:
						i.putShort(0x9630 | (imm & 0xF) | ((imm & 0x30) << 2));
						return;
				}
				fatalError("invalid operand for ADDIWregimm");
				return;
			case I_MULregreg:
				i.putShort(0x9C00 | (reg0 << 4) | (reg1 & 0xF) | ((reg1 & 0x10) << 5));
				return;
			case I_STZ_DECreg:
				i.putShort(0x9202 | (reg0 << 4));
				return;
			case I_LSRreg:
				i.putShort(0x9406 | (reg0 << 4));
				return;
			case I_RORreg:
				i.putShort(0x9407 | (reg0 << 4));
				return;
			case I_LDZ_DECreg:
				i.putShort(0x9002 | (reg0 << 4));
				return;
			case I_STX_DECreg:
				i.putShort(0x920E | (reg0 << 4));
				return;
			case I_LDX_INCreg:
				i.putShort(0x900D | (reg0 << 4));
				return;
			case I_ROLreg: //same as ADC reg0, reg0
				i.putShort(0x1C00 | (reg0 << 4) | (reg0 & 0xF) | ((reg0 & 0x10) << 5));
				return;
			case I_LSLreg: //same as ADD reg0, reg0
				i.putShort(0x0C00 | (reg0 << 4) | (reg0 & 0xF) | ((reg0 & 0x10) << 5));
				return;
			case I_RET:
				i.putShort(0x9508);
				return;
			case I_RETI:
				i.putShort(0x9518);
				return;
			case I_NEGreg:
				i.putShort(0x9401 | (reg0 << 4));
				return;
			case I_CLC:
				i.putShort(0x9488);
				return;
			case I_SEC:
				i.putShort(0x9408);
				return;
			case I_DECreg:
				i.putShort(0x940A | (reg0 << 4));
				return;
			case I_ASRreg:
				i.putShort(0x9405 | (reg0 << 4));
				return;
			case I_LPM_INCreg:
				i.putShort(0x9005 | (reg0 << 4));
				return;
			case I_LPMreg:
				i.putShort(0x9004 | (reg0 << 4));
				return;
			case I_LDY_DISPregimm:
				i.putShort(0x8008 | (reg0 << 4) | (imm & 0x07) | ((imm & 0x18) << 7) | ((imm & 0x20) << 8));
				return;
			case I_STY_DISPregimm:
				i.putShort(0x8208 | (reg0 << 4) | (imm & 0x07) | ((imm & 0x18) << 7) | ((imm & 0x20) << 8));
				return;
			case I_LDSregimm:
				i.putShort(0x9000 | (reg0 << 4));
				i.putShort(imm);
				return;
			case I_STSimmreg:
				i.putShort(0x9200 | (reg0 << 4));
				i.putShort(imm);
				return;
			case I_CBIimm:
				i.putShort(0x9800 | imm);
				return;
			case I_SBIimm:
				i.putShort(0x9A00 | imm);
				return;
			case I_SBRCregimm:
				i.putShort(0xFC00 | (reg0 << 4) | imm);
				return;
			case I_PUSHip:
				i.putShort(0xD000);
				return;
			case I_CLI:
				i.putShort(0x94F8);
				return;
			case I_STEXreg:
				i.putShort(0); //placeholder for one instruction
				return;
		}
		fatalError("invalid instruction type in code");
	}
	
	protected int print(Instruction i)
	{ //returns number of real instructions
		int cnt;
		if (i.type == I_NONE)
			return 0;
		ctx.out.print(i.instrNr);
		ctx.out.print(": ");
		switch (i.type)
		{
			case I_LDIregimm:
				ctx.out.print("LDI r");
				ctx.out.print(i.reg0);
				ctx.out.print(",0x");
				ctx.out.printHexFix(i.iPar1, 2);
				break;
			case I_MOVregreg:
				ctx.out.print("MOV r");
				ctx.out.print(i.reg0);
				ctx.out.print(",r");
				ctx.out.print(i.reg1);
				break;
			case I_MOVWregreg:
				ctx.out.print("MOVW r");
				ctx.out.print(i.reg0);
				ctx.out.print(",r");
				ctx.out.print(i.reg1);
				break;
			case I_PUSHreg:
				ctx.out.print("PUSH r");
				ctx.out.print(i.reg0);
				break;
			case I_POPreg:
				ctx.out.print("POP r");
				ctx.out.print(i.reg0);
				break;
			case I_ANDregreg:
				ctx.out.print("AND r");
				ctx.out.print(i.reg0);
				ctx.out.print(",r");
				ctx.out.print(i.reg1);
				break;
			case I_ANDIregimm:
				ctx.out.print("ANDI r");
				ctx.out.print(i.reg0);
				ctx.out.print(",0x");
				ctx.out.printHexFix(i.iPar1, 2);
				break;
			case I_ORregreg:
				ctx.out.print("OR r");
				ctx.out.print(i.reg0);
				ctx.out.print(",r");
				ctx.out.print(i.reg1);
				break;
			case I_ORIregimm:
				ctx.out.print("ORI r");
				ctx.out.print(i.reg0);
				ctx.out.print(",0x");
				ctx.out.printHexFix(i.iPar1, 2);
				break;
			case I_ADCregreg:
				ctx.out.print("ADC r");
				ctx.out.print(i.reg0);
				ctx.out.print(",r");
				ctx.out.print(i.reg1);
				break;
			case I_SBCregreg:
				ctx.out.print("SBC r");
				ctx.out.print(i.reg0);
				ctx.out.print(",r");
				ctx.out.print(i.reg1);
				break;
			case I_ADDregreg:
				ctx.out.print("ADD r");
				ctx.out.print(i.reg0);
				ctx.out.print(",r");
				ctx.out.print(i.reg1);
				break;
			case I_SUBregreg:
				ctx.out.print("SUB r");
				ctx.out.print(i.reg0);
				ctx.out.print(",r");
				ctx.out.print(i.reg1);
				break;
			case I_EORregreg:
				ctx.out.print("EOR r");
				ctx.out.print(i.reg0);
				ctx.out.print(",r");
				ctx.out.print(i.reg1);
				break;
			case I_SUBIregimm:
				ctx.out.print("SUBI r");
				ctx.out.print(i.reg0);
				ctx.out.print(",0x");
				ctx.out.printHexFix(i.iPar1, 2);
				break;
			case I_SBCIregimm:
				ctx.out.print("SBCI r");
				ctx.out.print(i.reg0);
				ctx.out.print(",0x");
				ctx.out.printHexFix(i.iPar1, 2);
				break;
			case I_COMreg:
				ctx.out.print("COM r");
				ctx.out.print(i.reg0);
				break;
			case I_LSRreg:
				ctx.out.print("LSR r");
				ctx.out.print(i.reg0);
				break;
			case I_RORreg:
				ctx.out.print("ROR r");
				ctx.out.print(i.reg0);
				break;
			case I_ROLreg:
				ctx.out.print("ROL r");
				ctx.out.print(i.reg0);
				break;
			case I_LSLreg:
				ctx.out.print("LSL r");
				ctx.out.print(i.reg0);
				break;
			case I_NEGreg:
				ctx.out.print("NEG r");
				ctx.out.print(i.reg0);
				break;
			case I_ASRreg:
				ctx.out.print("ASR r");
				ctx.out.print(i.reg0);
				break;
			case I_INCreg:
				ctx.out.print("INC r");
				ctx.out.print(i.reg0);
				break;
			case I_DECreg:
				ctx.out.print("DEC r");
				ctx.out.print(i.reg0);
				break;
			case I_LDZreg:
				ctx.out.print("LD r");
				ctx.out.print(i.reg0);
				ctx.out.print(",Z");
				break;
			case I_STZ_INCreg:
				ctx.out.print("ST Z+,r");
				ctx.out.print(i.reg0);
				break;
			case I_STZ_DECreg:
				ctx.out.print("ST -Z,r");
				ctx.out.print(i.reg0);
				break;
			case I_LDZ_INCreg:
				ctx.out.print("LD r");
				ctx.out.print(i.reg0);
				ctx.out.print(",Z+");
				break;
			case I_LDZ_DECreg:
				ctx.out.print("LD r");
				ctx.out.print(i.reg0);
				ctx.out.print(",-Z");
				break;
			case I_LPM_INCreg:
				ctx.out.print("LPM r");
				ctx.out.print(i.reg0);
				ctx.out.print(",Z+");
				break;
			case I_INregimm:
				ctx.out.print("IN r");
				ctx.out.print(i.reg0);
				ctx.out.print(",0x");
				ctx.out.printHexFix(i.iPar1, 2);
				break;
			case I_OUTimmreg:
				ctx.out.print("OUT 0x");
				ctx.out.printHexFix(i.iPar1, 2);
				ctx.out.print(",r");
				ctx.out.print(i.reg0);
				break;
			case I_CPIregimm:
				ctx.out.print("CPI r");
				ctx.out.print(i.reg0);
				ctx.out.print(",0x");
				ctx.out.printHexFix(i.iPar1, 2);
				break;
			case I_CPregreg:
				ctx.out.print("CP r");
				ctx.out.print(i.reg0);
				ctx.out.print(",r");
				ctx.out.print(i.reg1);
				break;
			case I_CPCregreg:
				ctx.out.print("CPC r");
				ctx.out.print(i.reg0);
				ctx.out.print(",r");
				ctx.out.print(i.reg1);
				break;
			case I_ICALL:
				ctx.out.print("ICALL");
				break;
			case I_ADDIWregimm:
				ctx.out.print("ADDIW r");
				ctx.out.print(i.reg0);
				ctx.out.print(",0x");
				ctx.out.printHexFix(i.iPar1, 2);
				break;
			case I_MULregreg:
				ctx.out.print("MUL r");
				ctx.out.print(i.reg0);
				ctx.out.print(",r");
				ctx.out.print(i.reg1);
				break;
			case I_STX_DECreg:
				ctx.out.print("ST -X,r");
				ctx.out.print(i.reg0);
				break;
			case I_LDX_INCreg:
				ctx.out.print("LD r");
				ctx.out.print(i.reg0);
				ctx.out.print(",X+");
				break;
			case I_RET:
				ctx.out.print("RET");
				break;
			case I_RETI:
				ctx.out.print("RETI");
				break;
			case I_CLC:
				ctx.out.print("CLC");
				break;
			case I_SEC:
				ctx.out.print("SEC");
				break;
			case I_LPMreg:
				ctx.out.print("LPM r");
				ctx.out.print(i.reg0);
				ctx.out.print(",Z");
				break;
			case I_LDY_DISPregimm:
				ctx.out.print("LD r");
				ctx.out.print(i.reg0);
				ctx.out.print(",Y+");
				ctx.out.print(i.iPar1);
				break;
			case I_STY_DISPregimm:
				ctx.out.print("ST Y+");
				ctx.out.print(i.iPar1);
				ctx.out.print(",r");
				ctx.out.print(i.reg0);
				break;
			case I_LDSregimm:
				ctx.out.print("LDS r");
				ctx.out.print(i.reg0);
				ctx.out.print(",0x");
				ctx.out.printHexFix(i.iPar1, 4);
				break;
			case I_STSimmreg:
				ctx.out.print("STS 0x");
				ctx.out.printHexFix(i.iPar1, 4);
				ctx.out.print(",r");
				ctx.out.print(i.reg0);
				break;
			case I_CBIimm:
				ctx.out.print("CBI 0x");
				ctx.out.printHexFix(i.iPar1 >>> 3, 2);
				ctx.out.print(",");
				ctx.out.print(i.iPar1 & 0x7);
				break;
			case I_SBIimm:
				ctx.out.print("SBI 0x");
				ctx.out.printHexFix(i.iPar1 >>> 3, 2);
				ctx.out.print(",");
				ctx.out.print(i.iPar1 & 0x7);
				break;
			case I_SBRCregimm:
				ctx.out.print("SBRC r");
				ctx.out.print(i.reg0);
				ctx.out.print(",");
				ctx.out.print(i.iPar1);
				break;
			case I_PUSHip:
				ctx.out.print("PUSHIP");
				break;
			case I_CLI:
				ctx.out.print("CLI");
				break;
			case I_CALLpatched:
				ctx.out.print("CALLpatched to ");
				i.refMthd.owner.printNameWithOuter(ctx.out);
				ctx.out.print('.');
				i.refMthd.printNamePar(ctx.out);
				break;
			case I_ADDpatched:
				ctx.out.print("ADDpatched i");
				ctx.out.print(i.jDest.instrNr);
				break;
			case I_IHELPER:
				ctx.out.print("--ihelper i");
				ctx.out.print(i.jDest.instrNr);
				return 0; //not a real instruction
			case I_BSHOPHINT:
				ctx.out.print("--binophint ");
				switch (i.iPar1)
				{
					case Ops.B_SHL:
						ctx.out.print("shl");
						break;
					case Ops.B_SHRL:
						ctx.out.print("shrl");
						break;
					case Ops.B_SHRA:
						ctx.out.print("shra");
						break;
				}
				ctx.out.print(" m");
				ctx.out.printHexFix(i.reg0, 8);
				ctx.out.print("/r");
				ctx.out.print(i.reg1);
				ctx.out.print(" until i");
				ctx.out.print(i.jDest.instrNr);
				return 0; //not a real instruction
			case I_STEXreg:
				ctx.out.print("STEX r");
				ctx.out.print(i.reg0);
				break;
			case I_JUMP:
				switch (i.iPar1)
				{
					case 0:
						ctx.out.print("JMP");
						break; //unconditionally
					case Ops.C_GE:
						ctx.out.print("JGE");
						break; //jump if greater or equal (signed)
					case Ops.C_LW:
						ctx.out.print("JLW");
						break; //jump if lower (signed)
					case Ops.C_BO:
						ctx.out.print("JBO");
						break; //jump if below (unsigned)
					case Ops.C_NE:
						ctx.out.print("JNE");
						break; //jump if not equal
					case Ops.C_EQ:
						ctx.out.print("JEQ");
						break; //jump if equal
				}
				ctx.out.print(" to i");
				ctx.out.print(i.jDest.instrNr);
				break;
			case I_MAGC:
				ctx.out.print("MAGIC");
				if ((i.size & 1) != 0)
					ctx.out.print(" INVSIZE!");
				for (cnt = 0; cnt < i.size; cnt += 2)
				{
					ctx.out.print(" 0x");
					ctx.out.printHexFix(i.code[cnt + 1], 2);
					ctx.out.printHexFix(i.code[cnt], 2);
				}
				return i.size / 2;
			default:
				ctx.out.print("unknown instruction: ");
				ctx.out.print(i.type);
		}
		return 1; //default: one instruction
	}
	
	protected void printCode(Instruction first, String comment)
	{
		Instruction now;
		int insCnt;
		mthdContainer.owner.printNameWithOuter(ctx.out);
		ctx.out.print('.');
		mthdContainer.printNamePar(ctx.out);
		ctx.out.print(": //");
		ctx.out.println(comment);
		now = first;
		insCnt = 0;
		while (now != null)
		{
			if (now.token != null)
				ctx.printSourceHint(now.token);
			if (now.type != I_NONE)
			{
				insCnt += print(now);
				ctx.out.println();
			}
			now = now.next;
		}
		ctx.out.print("//instruction count ");
		ctx.out.print(comment);
		ctx.out.print(": ");
		ctx.out.println(insCnt);
	}
}
