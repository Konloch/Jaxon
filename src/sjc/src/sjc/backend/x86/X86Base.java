/* Copyright (C) 2006, 2007, 2008, 2009, 2010, 2011, 2014, 2015, 2016 Stefan Frenz
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

import sjc.backend.Architecture;
import sjc.backend.Instruction;
import sjc.compbase.*;
import sjc.memory.MemoryImage;
import sjc.osio.TextBuffer;
import sjc.osio.TextPrinter;

/**
 * X86Base: instructions for x86-architectures
 *
 * @author S. Frenz
 * @version 160201 added pointer naming for plain addresses in asmMode
 * version 151226 added more optimization for jump encoding
 * version 151108 adopted changed buildAssemblerText concept
 * version 151026 added support for assembler output
 * version 140420 fixed print of OUT instruction
 * version 110127 add I_CMPmemreg, I_CBW and I_CWD for 64 bit optimizer
 * version 101119 added I_MOVSXD*
 * version 101104 added some instruction types (used in optimizer)
 * version 101027 fixed print of CL-based shift instructions
 * version 101018 optimized finalizeInstructions
 * version 100922 added I_ANDmemreg and I_XORmemreg
 * version 100813 fixed jump destination check
 * version 100504 removed no longer used genNativeReturnMissing
 * version 100415 fixed register maintainance for calls with special regs
 * version 100115 adopted changed error reporting
 * version 100114 removed unused method
 * version 100113 added POPdummy in printCode
 * version 091109 added POPdummy
 * version 091103 removed unused variables/methods
 * version 091102 added call to source token printing
 * version 091004 fixed inlineVarOffset after memory interface change
 * version 091001 adopted changed memory interface
 * version 090626 added support for stack extreme check
 * version 090619 adopted changed Architecture
 * version 090430 added support for "return missing" hint
 * version 090219 got basic getReg functionality and adopted changed Architecture (genCopyInstContext<->getClssInstReg)
 * version 090208 removed error message for genClearMem
 * version 090207 added copyright notice and removed genLoadNullAddr/genPushNull
 * version 090206 added error string ERR_INVMODE_GENSTOREVARX
 * version 081231 made insJump and insPatched* public to support SRB architectures
 * version 081209 added support for method printing
 * version 081021 adopted changes in Architecture
 * version 080615 adopted changes in Architecture
 * version 080605 added support for language throwables
 * version 080525 adopted changed genCondJmp signature, removed genMarker
 * version 080518 moved createNewInstruction to LittleEndian
 * version 070915 added FWAIT
 * version 070913 added support for inlineVarOffset, genClearMem, changed order of IM_P0/IM_P1
 * version 070814 added support for float/double instructions and registers
 * version 070812 prevented hasFltDblError from printing message multiple times
 * version 070809 added support for float/double error
 * version 070610 optimized access to different jump offset sizes
 * version 070606 got common methods and variables of childs
 * version 070601 got externalizable Strings of childs
 * version 070513 added ADD/SUB/AND/XOR/OR-regmem
 * version 070114 added I_MOVZXregmem and I_CMPmemimm
 * version 070108 changed SHLreg1, added SHLregimm
 * version 061231 added ari-mem-ops
 * version 060703 initial version
 */

public abstract class X86Base extends Architecture
{
	//map for general purpose registers of all childs, used by tokens and in alloc/dealloc-functions
	protected final static int RegA = 0x0001; //EAX
	protected final static int RegD = 0x0002; //EDX
	protected final static int RegB = 0x0004; //EBX
	protected final static int RegC = 0x0008; //ECX
	//register maps for special registers
	protected final static int RegClss = 0x1000; //EDI
	protected final static int RegInst = 0x2000; //ESI
	protected final static int RegBase = 0x4000; //EBP
	
	//real registers, entries are 0rrr0bbb with
	//  rrr = register code
	//  bbb = bytes affected in the corresponding full register
	//        with 111 -> complete 32 bits, may be combined with REX to indicate 64 bit
	//             011 -> lower 16 bits
	//             010 -> upper 8 bits of lower 16 bits not used and not supported because of REX-regs
	//             001 -> lowest 8 bits
	protected final static int R_AL = 0x001, R_AX = 0x003, R_EAX = 0x007, RM_A = 0x000;
	protected final static int R_CL = 0x011, R_CX = 0x013, R_ECX = 0x017, RM_C = 0x010;
	protected final static int R_DL = 0x021, R_DX = 0x023, R_EDX = 0x027, RM_D = 0x020;
	protected final static int R_BL = 0x031, R_BX = 0x033, R_EBX = 0x037, RM_B = 0x030;
	protected final static int R_SP = 0x043, R_ESP = 0x047;
	protected final static int R_BP = 0x053, R_EBP = 0x057;
	protected final static int R_SI = 0x063, R_ESI = 0x067;
	protected final static int R_DI = 0x073, R_EDI = 0x077;
	protected final static int R_AH = 0x42;
	
	//helper for the instruction-coding below
	protected final static int IM_OP = 0xFF00;
	protected final static int IM_P0 = 0x00F0;
	protected final static int IM_P1 = 0x000F;
	protected final static int I_reg0 = 0x10;
	protected final static int I_mem0 = 0x20;
	protected final static int I_imm0 = 0x30;
	protected final static int I_iml0 = 0x40;
	protected final static int I_reg1 = 0x01;
	protected final static int I_mem1 = 0x02;
	protected final static int I_imm1 = 0x03;
	protected final static int I_iml1 = 0x04;
	protected final static int I_MOV = 0x0100;
	protected final static int I_MOVSX = 0x0200;
	protected final static int I_MOVZX = 0x0300;
	protected final static int I_LEA = 0x0400;
	protected final static int I_PUSH = 0x0500;
	protected final static int I_POP = 0x0600;
	protected final static int I_PUSHA = 0x0700;
	protected final static int I_POPA = 0x0800;
	protected final static int I_ADD = 0x0900;
	protected final static int I_ADC = 0x0A00;
	protected final static int I_SUB = 0x0B00;
	protected final static int I_SBB = 0x0C00;
	protected final static int I_AND = 0x0D00;
	protected final static int I_XOR = 0x0E00;
	protected final static int I_OR = 0x0F00;
	protected final static int I_TEST = 0x1000;
	protected final static int I_MUL = 0x1100;
	protected final static int I_IMUL = 0x1200;
	protected final static int I_DIV = 0x1300;
	protected final static int I_IDIV = 0x1400;
	protected final static int I_INC = 0x1500;
	protected final static int I_DEC = 0x1600;
	protected final static int I_NEG = 0x1700;
	protected final static int I_NOT = 0x1800;
	protected final static int I_CMP = 0x1900;
	protected final static int I_CALL = 0x1A00;
	protected final static int I_RET = 0x1B00;
	protected final static int I_IRET = 0x1C00;
	protected final static int I_XCHG = 0x1D00;
	protected final static int I_SHL = 0x1E00;
	protected final static int I_SHR = 0x1F00;
	protected final static int I_SHLD = 0x2000;
	protected final static int I_SHRD = 0x2100;
	protected final static int I_SAR = 0x2200;
	protected final static int I_ROL = 0x2300;
	protected final static int I_ROR = 0x2400;
	protected final static int I_RCR = 0x2500;
	protected final static int I_BSR = 0x2600;
	protected final static int I_CWDE = 0x2700;
	protected final static int I_CDQ = 0x2800;
	protected final static int I_CDQE = 0x2900;
	protected final static int I_CQO = 0x2A00;
	protected final static int I_OUT = 0x2B00;
	protected final static int I_IN = 0x2C00;
	protected final static int I_SAHF = 0x2D00;
	protected final static int I_PUSHF = 0x2E00;
	protected final static int I_POPF = 0x2F00;
	//floating point instructions
	protected final static int I_FLD = 0x3000;
	protected final static int I_FSTP = 0x3100;
	protected final static int I_FILD = 0x3200;
	protected final static int I_FISTP = 0x3300;
	protected final static int I_FISTTP = 0x3400;
	protected final static int I_FCHS = 0x3500;
	protected final static int I_FADDP = 0x3600;
	protected final static int I_FSUBP = 0x3700;
	protected final static int I_FMULP = 0x3800;
	protected final static int I_FDIVP = 0x3900;
	protected final static int I_FDUP = 0x3A00;
	protected final static int I_FXCH = 0x3B00;
	protected final static int I_FFREE = 0x3C00;
	protected final static int I_FINCSTP = 0x3D00;
	protected final static int I_FCOMPP = 0x3E00;
	protected final static int I_FCOMIP = 0x3F00;
	protected final static int I_FSTSW = 0x4000;
	protected final static int I_FNSTCW = 0x4100;
	protected final static int I_FLDCW = 0x4200;
	protected final static int I_FWAIT = 0x4300;
	//special 64 bit instructions
	protected final static int I_MOVSXD = 0x5000;
	protected final static int I_CBW = 0x5100;
	protected final static int I_CWD = 0x5200;
	
	//floating point sizes (or to base opcode)
	protected final static int FPU32 = 0x01;
	protected final static int FPU64 = 0x05;
	
	//floating point register counting
	protected final static int FPUREGSTART = 0x80000000; //must be negative
	protected final static int FPUREGINC = 0x01000000; //not a mask but an inc/decrementer
	
	//entries are Xmmnn with
	//  X     = number of instruction
	//  mm/nn = mode of operation (m first mode - lower bits, n second mode - higher bits)
	//          with 00 -> no argument
	//               01 -> reg
	//               10 -> mem
	//               11 -> imm
	protected final static int I_MOVregreg = I_MOV | I_reg0 | I_reg1;
	protected final static int I_MOVregmem = I_MOV | I_reg0 | I_mem1;
	protected final static int I_MOVmemreg = I_MOV | I_mem0 | I_reg1;
	protected final static int I_MOVregimm = I_MOV | I_reg0 | I_imm1;
	protected final static int I_MOVregimmL = I_MOV | I_reg0 | I_iml1;
	protected final static int I_MOVmemimm = I_MOV | I_mem0 | I_imm1;
	protected final static int I_MOVSXregreg = I_MOVSX | I_reg0 | I_reg1;
	protected final static int I_MOVSXregmem = I_MOVSX | I_reg0 | I_mem1;
	protected final static int I_MOVZXregreg = I_MOVZX | I_reg0 | I_reg1;
	protected final static int I_MOVZXregmem = I_MOVZX | I_reg0 | I_mem1;
	protected final static int I_MOVSXDregreg = I_MOVSXD | I_reg0 | I_reg1; //only valid for 64 bit register reg0
	protected final static int I_MOVSXDregmem = I_MOVSXD | I_reg0 | I_mem1; //only valid for 64 bit register reg0
	protected final static int I_LEAregmem = I_LEA | I_reg0 | I_mem1;
	protected final static int I_PUSHreg = I_PUSH | I_reg0;
	protected final static int I_PUSHmem = I_PUSH | I_mem0;
	protected final static int I_PUSHimm = I_PUSH | I_imm0;
	protected final static int I_POPreg = I_POP | I_reg0;
	protected final static int I_CALLreg = I_CALL | I_reg0;
	protected final static int I_CALLmem = I_CALL | I_mem0;
	protected final static int I_CALLimm = I_CALL | I_imm0;
	protected final static int I_RETimm = I_RET | I_imm0;
	protected final static int I_ADDregimm = I_ADD | I_reg0 | I_imm1;
	protected final static int I_ADDregreg = I_ADD | I_reg0 | I_reg1;
	protected final static int I_ADDmemimm = I_ADD | I_mem0 | I_imm1;
	protected final static int I_ADDregmem = I_ADD | I_reg0 | I_mem1;
	protected final static int I_ADDmemreg = I_ADD | I_mem0 | I_reg1;
	protected final static int I_ADCregreg = I_ADC | I_reg0 | I_reg1;
	protected final static int I_SUBregimm = I_SUB | I_reg0 | I_imm1;
	protected final static int I_SUBregreg = I_SUB | I_reg0 | I_reg1;
	protected final static int I_SUBmemimm = I_SUB | I_mem0 | I_imm1;
	protected final static int I_SUBregmem = I_SUB | I_reg0 | I_mem1;
	protected final static int I_SUBmemreg = I_SUB | I_mem0 | I_reg1;
	protected final static int I_SBBregreg = I_SBB | I_reg0 | I_reg1;
	protected final static int I_SBBregimm = I_SBB | I_reg0 | I_imm1;
	protected final static int I_ANDregreg = I_AND | I_reg0 | I_reg1;
	protected final static int I_ANDregimm = I_AND | I_reg0 | I_imm1;
	protected final static int I_ANDmemimm = I_AND | I_mem0 | I_imm1;
	protected final static int I_ANDregmem = I_AND | I_reg0 | I_mem1;
	protected final static int I_ANDmemreg = I_AND | I_mem0 | I_reg1;
	protected final static int I_XORregreg = I_XOR | I_reg0 | I_reg1;
	protected final static int I_XORregimm = I_XOR | I_reg0 | I_imm1;
	protected final static int I_XORmemimm = I_XOR | I_mem0 | I_imm1;
	protected final static int I_XORregmem = I_XOR | I_reg0 | I_mem1;
	protected final static int I_XORmemreg = I_XOR | I_mem0 | I_reg1;
	protected final static int I_ORregreg = I_OR | I_reg0 | I_reg1;
	protected final static int I_ORregimm = I_OR | I_reg0 | I_imm1;
	protected final static int I_ORmemimm = I_OR | I_mem0 | I_imm1;
	protected final static int I_ORregmem = I_OR | I_reg0 | I_mem1;
	protected final static int I_ORmemreg = I_OR | I_mem0 | I_reg1;
	protected final static int I_TESTregreg = I_TEST | I_reg0 | I_reg1;
	protected final static int I_MULreg = I_MUL | I_reg0;
	protected final static int I_IMULreg = I_IMUL | I_reg0;
	protected final static int I_IMULregreg = I_IMUL | I_reg0 | I_reg1;
	protected final static int I_DIVreg = I_DIV | I_reg0;
	protected final static int I_IDIVreg = I_IDIV | I_reg0;
	protected final static int I_INCreg = I_INC | I_reg0;
	protected final static int I_INCmem = I_INC | I_mem0;
	protected final static int I_DECreg = I_DEC | I_reg0;
	protected final static int I_DECmem = I_DEC | I_mem0;
	protected final static int I_NEGreg = I_NEG | I_reg0;
	protected final static int I_NOTreg = I_NOT | I_reg0;
	protected final static int I_CMPregimm = I_CMP | I_reg0 | I_imm1;
	protected final static int I_CMPregreg = I_CMP | I_reg0 | I_reg1;
	protected final static int I_CMPregmem = I_CMP | I_reg0 | I_mem1;
	protected final static int I_CMPmemimm = I_CMP | I_mem0 | I_imm1;
	protected final static int I_CMPmemreg = I_CMP | I_mem0 | I_reg1;
	protected final static int I_XCHGregreg = I_XCHG | I_reg0 | I_reg1;
	protected final static int I_SHLreg1 = I_SHL | I_reg0 | IM_P1;  //immediate is fixed to 1 (16 bit mode)
	protected final static int I_SHLregimm = I_SHL | I_reg0 | I_imm1;
	protected final static int I_SHLmemimm = I_SHL | I_mem0 | I_imm1;
	protected final static int I_SHLregmem = I_SHL | I_reg0 | I_mem1;
	protected final static int I_SHLmemreg = I_SHL | I_mem0 | I_reg1;
	protected final static int I_SHLregreg = I_SHL | I_reg0 | I_reg1; //second register is fixed to cl
	protected final static int I_SHRregreg = I_SHR | I_reg0 | I_reg1; //second register is fixed to cl
	protected final static int I_SHRregimm = I_SHR | I_reg0 | I_imm1;
	protected final static int I_SHRmemimm = I_SHR | I_mem0 | I_imm1;
	protected final static int I_SHRregmem = I_SHR | I_reg0 | I_mem1;
	protected final static int I_SHRmemreg = I_SHR | I_mem0 | I_reg1;
	protected final static int I_SHLDregreg = I_SHLD | I_reg0 | I_reg1; //third register is fixed to cl
	protected final static int I_SHRDregreg = I_SHRD | I_reg0 | I_reg1; //third register is fixed to cl
	protected final static int I_SARregreg = I_SAR | I_reg0 | I_reg1; //second register is fixed to cl
	protected final static int I_SARregimm = I_SAR | I_reg0 | I_imm1;
	protected final static int I_SARmemimm = I_SAR | I_mem0 | I_imm1;
	protected final static int I_SARregmem = I_SAR | I_reg0 | I_mem1;
	protected final static int I_SARmemreg = I_SAR | I_mem0 | I_reg1;
	protected final static int I_ROLregimm = I_ROL | I_reg0 | I_imm1;
	protected final static int I_RORregimm = I_ROR | I_reg0 | I_imm1;
	protected final static int I_RCRregimm = I_RCR | I_reg0 | I_imm1;
	protected final static int I_BSRregreg = I_BSR | I_reg0 | I_reg1;
	protected final static int I_OUTreg = I_OUT | I_reg0;
	protected final static int I_INreg = I_IN | I_reg0;
	//floating point instructions
	protected final static int I_FLDmem = I_FLD | I_mem0;
	protected final static int I_FSTPmem = I_FSTP | I_mem0;
	protected final static int I_FILDmem = I_FILD | I_mem0;
	protected final static int I_FISTPmem = I_FISTP | I_mem0;
	protected final static int I_FISTTPmem = I_FISTTP | I_mem0;
	protected final static int I_FNSTCWmem = I_FNSTCW | I_mem0;
	protected final static int I_FLDCWmem = I_FLDCW | I_mem0;
	//special ones
	protected final static int I_JUMP = -10;
	protected final static int I_LEAarray = 0xFFF1;
	protected final static int I_MOVindexed = 0xFFF2;
	protected final static int I_BOUNDEXC = 0xFFF3;
	protected final static int I_RETMSEXC = 0xFFF4;
	protected final static int I_MARKER = 0xFFF5;
	protected final static int I_ADDpatched = 0xFFF6;
	protected final static int I_IHELPER = 0xFFF7;
	protected final static int I_PUSHip = 0xFFF8;
	protected final static int I_STEXreg = 0xFFF9;
	protected final static int I_POPdummy = 0xFFFA; //POPdummy is used only in IA32 and equals "POP ECX"
	
	//helper for the register coding
	protected final static int RS_L = 0x1; //AL, BL, ...
	protected final static int RS_H = 0x2; //AH, BH, ...
	protected final static int RS_X = 0x3; //AX, BX, ...
	protected final static int RS_E = 0x7; //EAX, EBX, ...
	
	//error strings
	protected final static String ERR_UNRESREG = "unsolvable register request";
	protected final static String ERR_INVREG_GETREG = "invalid or no register found in getReg";
	protected final static String ERR_INVTYPE_GETREG = "invalid type in getReg";
	protected final static String ERR_INTNOINLINE = "can not inline interrupt method";
	protected final static String ERR_INVPARINT = "invalid parameter-size for interupt-method";
	protected final static String ERR_FPUSTACK = "FPU stack not valid at end of method";
	protected final static String ERR_RETSIZE_CODEEPILOG = "codeEpilog with too large retSize";
	protected final static String ERR_INVMODE_GENSTOREVARX = "genStoreVarX with invalid mode/type";
	protected final static String ERR_INVTYPE_GENLOADVARVAL = "unsupported type for genLoadVarVal";
	protected final static String ERR_UNSTYPE_GENCONVERTVAL = "unsupported from/toType for genConvertVal";
	protected final static String ERR_UNSTYPE_GENDUP = "unsupported type for genDup";
	protected final static String ERR_UNRPUSH_GENPUSH = "unresolved null-type in genPush";
	protected final static String ERR_UNSTYPE_GENASSIGN = "unsupported type for genAssign";
	protected final static String ERR_UNSCASE_GENBINOP = "unsupported case with dst!=src1 or src1==src2 for genBinOp";
	protected final static String ERR_UNSOP_GENBINOP = "unsupported operation for genBinOp";
	protected final static String ERR_UNSTYPE_GENBINOP = "unsupported type for genBinOp";
	protected final static String ERR_UNSCASE_GENUNAOP = "unsupported case with dst!=src for genUnaOp";
	protected final static String ERR_UNSOP_GENUNAOP = "unsupported operation for genUnaOp";
	protected final static String ERR_UNSTYPE_GENUNAOP = "unsupported type for genUnaOp";
	protected final static String ERR_UNSTYPE_GENINCDECMEM = "unsupported type for genInc/DecMem";
	protected final static String ERR_UNSTYPE_GENCOMP = "unsupported type for genComp";
	protected final static String ERR_UNSTYPE_GENWRITEIO = "unsupported type for genWriteIO";
	protected final static String ERR_UNSTYPE_GENREADIO = "unsupported type for genReadIO";
	protected final static String ERR_JUMPDESTINIT_FIXJUMP = "jump to uninitialized destination";
	protected final static String ERR_JUMPDESTNOTFOUND_FIXJUMP = "jump destination before not found";
	protected final static String ERR_CONDJUMP_FIXJUMP = "conditional jump with unsupported condition";
	protected final static String ERR_INVINS_CODE = "invalid instruction type for code(.)";
	protected final static String ERR_INVGLOBADDRREG = "invalid register for address";
	
	//special jumps for internal use
	protected final static int SC_BE = 101; //below or equal
	protected final static int SC_AE = 102; //above or equal
	protected final static int SC_AB = 103; //above
	protected final static int SC_US = 104; //unsigned
	
	//common variables
	protected Mthd mthdContainer;
	protected int rAll, usedRegs, writtenRegs, nextAllocReg;
	protected int rClss, rInst, rBase; //register for class, instance and stack frame
	protected int fullIPChangeBytes, patchedAddPrefix, mPtr; //size of unconditional full sized jump
	protected int curFPUReg, popFPUDone;
	protected int curVarOffParam;
	protected Instruction dupFPUIns;
	protected boolean nextJumpsUnsigned;
	
	//internally used methods
	protected abstract int freeRegSearch(int mask, int type);
	
	protected abstract int storeReg(int regs);
	
	protected abstract void restoreReg(int regs);
	
	protected abstract int internalGetReg(int i, int regs, int type, boolean firstWrite);
	
	protected abstract void internalFixStackExtremeAdd(Instruction me, int stackCells);
	
	protected abstract Instruction ins(int type, int reg0, int reg1, int disp, int imm);
	
	//common methods
	protected Instruction ins(int type)
	{
		return ins(type, 0, 0, 0, 0);
	}
	
	protected Instruction ins(int type, int reg0)
	{
		return ins(type, reg0, 0, 0, 0);
	}
	
	protected Instruction ins(int type, int reg0, int reg1)
	{
		return ins(type, reg0, reg1, 0, 0);
	}
	
	protected Instruction ins(int type, int reg0, int reg1, int disp)
	{
		return ins(type, reg0, reg1, disp, 0);
	}
	
	//X86Base variables
	private boolean alreadyPrintedFltDblError;
	private TextBuffer asmTmpTextBuffer;
	
	public void init(MemoryImage imem, int ilev, Context ictx)
	{
		super.init(imem, ilev, ictx);
		//all x86-architectures use throw-frame as shown in Architecture
		throwFrameSize = relocBytes * 7;
		throwFrameExcOff = relocBytes * 6;
		regClss = RegClss;
		regInst = RegInst;
		regBase = RegBase;
	}
	
	public String checkBuildAssembler(Context preInitCtx)
	{
		asmTmpTextBuffer = new TextBuffer();
		return "x86";
	}
	
	protected void attachMethodAssemblerText(Mthd generatingMthd, Instruction first)
	{
		printCode(asmTmpTextBuffer, first, null, true);
		generatingMthd.asmCode = asmTmpTextBuffer.toString();
		asmTmpTextBuffer.reset();
	}
	
	public int ensureFreeRegs(int ignoreReg1, int ignoreReg2, int keepReg1, int keepReg2)
	{
		int restore = storeReg(rAll & ~(ignoreReg1 | ignoreReg2));
		usedRegs = (keepReg1 | keepReg2) & rAll;
		return restore;
	}
	
	public int prepareFreeReg(int avoidReg1, int avoidReg2, int reUseReg, int type)
	{
		int toStore, ret;
		
		//before all other tests: check for floating point
		if (type == StdTypes.T_FLT || type == StdTypes.T_DBL)
		{
			if (reUseReg < 0)
			{ //only fpu-registers
				if (reUseReg != curFPUReg)
					errorFPU();
				else
					nextAllocReg = curFPUReg;
			}
			else
				nextAllocReg = (curFPUReg += FPUREGINC);
			return 0;
		}
		//mask out fpu- and special registers
		reUseReg &= rAll;
		if (avoidReg1 < 0)
			avoidReg1 = 0;
		if (avoidReg2 < 0)
			avoidReg2 = 0;
		if (reUseReg < 0)
			reUseReg = 0;
		//first: try to reuse given regs
		if (reUseReg != 0)
		{
			if ((ret = freeRegSearch(reUseReg, type)) != 0)
			{
				usedRegs |= (nextAllocReg = ret);
				return 0; //nothing has to be freed, reuse given registers
			}
		}
		//second: try to alloc normally
		if ((ret = freeRegSearch((rAll & ~usedRegs & ~(avoidReg1 | avoidReg2)) | reUseReg, type)) != 0)
		{
			usedRegs |= (nextAllocReg = ret);
			return 0; //nothing has to be freed, use newly allocated registers
		}
		//third: try to free a register
		if ((ret = freeRegSearch((rAll & ~(avoidReg1 | avoidReg2)) | reUseReg, type)) != 0)
		{
			toStore = storeReg(ret);
			deallocReg(toStore);
			usedRegs |= (nextAllocReg = ret);
			return toStore;
		}
		//no possibility found to free registers
		fatalError(ERR_UNRESREG);
		return 0;
	}
	
	public int allocReg()
	{
		int ret = nextAllocReg;
		nextAllocReg = 0;
		return ret;
	}
	
	public void deallocRestoreReg(int deallocRegs, int keepRegs, int restore)
	{
		if (deallocRegs < 0)
		{ //FPU-register
			if (deallocRegs != keepRegs)
			{
				if (deallocRegs != curFPUReg)
					errorFPU();
				else if (popFPUDone == 0)
				{
					if (dupFPUIns == null)
					{
						ins(I_FFREE);
						ins(I_FINCSTP);
					}
					else
						kill(dupFPUIns);
				}
				else
					popFPUDone--;
				curFPUReg -= FPUREGINC;
			}
			dupFPUIns = null;
			return;
		}
		deallocReg(deallocRegs & ~(keepRegs | restore));
		restoreReg(restore);
		usedRegs |= (keepRegs | restore) & rAll;
	}
	
	protected int getReg(int nr, int reg, int type, boolean firstWrite)
	{
		if (reg < 0 || nr < 1 || nr > 2)
		{
			fatalError("invalid call to getReg");
			return 0;
		}
		if (nr == 1)
			switch (reg)
			{
				case RegClss:
					return rClss;
				case RegInst:
					return rInst;
				case RegBase:
					return rBase;
			}
		return internalGetReg(nr, reg, type, firstWrite);
	}
	
	protected void deallocReg(int regs)
	{
		usedRegs &= ~regs;
		writtenRegs &= ~regs;
	}
	
	protected int bitSearch(int value, int hit, int prefere)
	{
		int i, j;
		
		//first: try to search in prefered ones
		prefere &= value;
		if (prefere != 0 && prefere != value)
			for (i = 0; i < 32; i++)
			{ //search here only if there is a difference
				j = 1 << i;
				if ((prefere & j) != 0)
				{
					if (--hit == 0)
						return j;
				}
			}
		//nothing prefered, try in all possible
		for (i = 0; i < 32; i++)
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
				switch (now.type)
				{
					case I_JUMP:
						redo |= fixJump(now);
						break;
					case I_ADDpatched:
						redo |= fixAdd(now);
						now = now.next; //skip I_IHELPER
						break;
					case I_STEXreg:
						redo |= fixStackExtremeAdd(now);
						break;
				}
				now = now.next;
			}
		} while (redo);
		//print disassembly if wanted
		if (ctx.printCode || (mthdContainer.marker & Marks.K_PRCD) != 0)
			printCode(ctx.out, first, "X86", false);
	}
	
	public void genSaveUnitContext()
	{
		ins(I_PUSHreg, rClss);
	}
	
	public void genRestUnitContext()
	{
		ins(I_POPreg, rClss);
	}
	
	public void genLoadUnitContext(int dst, int off)
	{
		if ((dst = getReg(1, dst, StdTypes.T_PTR, true)) == 0)
			return;
		else
			ins(I_MOVregmem, dst, rClss, off);
	}
	
	public void genLoadInstContext(int src)
	{
		if ((src = getReg(1, src, StdTypes.T_PTR, false)) == 0)
			return;
		ins(I_MOVregreg, rInst, src);
		ins(I_MOVregmem, rClss, src, -relocBytes);
	}
	
	public void genSaveInstContext()
	{
		ins(I_PUSHreg, rInst);
		ins(I_PUSHreg, rClss);
	}
	
	public void genRestInstContext()
	{
		ins(I_POPreg, rClss);
		ins(I_POPreg, rInst);
	}
	
	protected void genDefaultUnaOp(int dstR, int srcR, int op, int type)
	{
		int opPar = op & 0xFFFF, dst;
		if (dstR != srcR)
		{
			fatalError(ERR_UNSCASE_GENUNAOP);
			return;
		}
		switch (type)
		{
			case StdTypes.T_BOOL:
				if (opPar == Ops.L_NOT)
				{ //do not use I_NOT, would change 0x01 to 0xFE instead of 0x00
					if ((dst = getReg(1, dstR, StdTypes.T_BYTE, false)) == 0)
						return;
					ins(I_XORregimm, dst, 0, 0, 1);
					return;
				}
				fatalError(ERR_UNSOP_GENUNAOP);
				return;
			case StdTypes.T_BYTE:
			case StdTypes.T_SHRT:
			case StdTypes.T_CHAR:
			case StdTypes.T_INT:
				if ((dst = getReg(1, dstR, type, false)) == 0)
					return;
				switch (opPar)
				{
					case Ops.A_CPL:
						ins(I_NOTreg, dst);
						return;
					case Ops.A_MINUS:
						ins(I_NEGreg, dst);
						return;
					case Ops.A_PLUS:  /*nothing to do*/
						return;
				}
				fatalError(ERR_UNSOP_GENUNAOP);
				return;
			//LONG is handled in child
		}
		fatalError(ERR_UNSTYPE_GENUNAOP);
	}
	
	public void genJmp(Instruction dest)
	{
		insJump(dest, 0);
	}
	
	public void genCondJmp(Instruction dest, int cond)
	{
		insJump(dest, cond);
	}
	
	public void genWriteIO(int addrR, int valR, int type, int memLoc)
	{
		int addr, val, typedRegA, restore1 = 0, restore2 = 0;
		if ((type != StdTypes.T_BYTE && type != StdTypes.T_SHRT && type != StdTypes.T_CHAR && type != StdTypes.T_INT) || memLoc != 0)
		{
			curMthd.printPos(ctx, ERR_UNSTYPE_GENWRITEIO);
			fatalError(null);
			return;
		}
		if ((addr = getReg(1, addrR, StdTypes.T_SHRT, false)) == 0 || (val = getReg(1, valR, type, false)) == 0)
			return;
		typedRegA = getReg(1, RegA, type, false);
		if (addrR == RegD)
		{
			if (valR != RegA)
			{
				restore1 = storeReg(RegA);
				ins(I_MOVregreg, typedRegA, val);
			}
			//else: nothing to prepare
		}
		else
		{ //dstR!=RegD
			if (valR == RegA)
			{
				restore1 = storeReg(RegD);
				ins(I_MOVregreg, R_DX, addr);
			}
			else
			{ //dstR!=RegD && srcR!=RegA
				if (addrR == RegA)
				{ //dstR==RegA
					if (valR == RegD)
					{ //dstR==RegA && srcR==RegD
						ins(I_XCHGregreg, R_EAX, R_EDX);
					}
					else
					{ //dstR==RegA && src!=RegD
						restore1 = storeReg(RegD);
						ins(I_MOVregreg, R_DX, addr);
						ins(I_MOVregreg, typedRegA, val);
					}
				}
				else
				{
					restore1 = storeReg(RegD);
					restore2 = storeReg(RegA);
					ins(I_MOVregreg, R_DX, addr);
					ins(I_MOVregreg, typedRegA, val);
				}
			}
		}
		ins(I_OUTreg, typedRegA);
		restoreReg(restore2);
		restoreReg(restore1);
	}
	
	public void genReadIO(int valR, int addrR, int type, int memLoc)
	{
		int addr, val, typedRegA, restore1 = 0, restore2 = 0;
		if ((type != StdTypes.T_BYTE && type != StdTypes.T_SHRT && type != StdTypes.T_CHAR && type != StdTypes.T_INT) || memLoc != 0)
		{
			curMthd.printPos(ctx, ERR_UNSTYPE_GENREADIO);
			fatalError(null);
			return;
		}
		if ((addr = getReg(1, addrR, StdTypes.T_SHRT, false)) == 0 || (val = getReg(1, valR, StdTypes.T_INT, true)) == 0)
			return;
		typedRegA = getReg(1, RegA, type, true);
		if (addrR == RegD)
		{
			if (valR != RegA)
				restore1 = storeReg(RegA);
			//else: nothing to prepare
		}
		else
		{ //dstR!=RegD
			if (valR == RegA)
			{
				restore1 = storeReg(RegD);
				ins(I_MOVregreg, R_DX, addr);
			}
			else
			{ //dstR!=RegD && srcR!=RegA
				restore1 = storeReg(RegD);
				restore2 = storeReg(RegA);
				ins(I_MOVregreg, R_DX, addr);
			}
		}
		ins(I_INreg, typedRegA);
		if (type == StdTypes.T_BYTE || type == StdTypes.T_SHRT)
		{
			ins(I_MOVSXregreg, val, typedRegA);
		}
		else if (valR != RegA)
			ins(I_MOVregreg, val, R_EAX);
		restoreReg(restore2);
		restoreReg(restore1);
	}
	
	public void genCheckBounds(int addr, int off, int checkToOffset, Instruction onSuccess)
	{
		if ((addr = getReg(1, addr, StdTypes.T_PTR, false)) == 0 || (off = getReg(1, off, StdTypes.T_INT, false)) == 0)
			return;
		ins(I_CMPregmem, off, addr, checkToOffset);
		insJump(onSuccess, Ops.C_BO);
	}
	
	public void genCheckStackExtreme(int maxValueReg, Instruction onSuccess)
	{
		if (maxValueReg != RegA)
		{
			fatalError("invalid maxValueReg for genCheckStackExtreme");
			return;
		}
		ins(I_STEXreg, mPtr | R_AX, 0, 0, 0x1234); //avoid byte-extension for placeholder
		ins(I_CMPregreg, mPtr | R_AX, mPtr | R_SP);
		insJump(onSuccess, Ops.C_BO);
	}
	
	public void genNativeBoundException()
	{
		ins(I_BOUNDEXC);
	}
	
	public int genCompPtrToNull(int reg, int cond)
	{
		if ((reg = getReg(1, reg, StdTypes.T_PTR, false)) == 0)
			return cond;
		ins(I_ORregreg, reg, reg);
		return cond;
	}
	
	public void inlineVarOffset(int byteCount, int objReg, Object loc, int offset, int additionalOffset)
	{
		Instruction i = null;
		int pos = mem.getAddrAsInt(loc, offset);
		
		if (objReg == regBase && pos >= 0)
			pos += curVarOffParam;
		pos += additionalOffset;
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
	
	public void genThrowFrameBuild(int globalAddrReg, Instruction dest, int throwBlockOffset)
	{
		Instruction relaDummy;
		int rA = mPtr | R_AX, rB = mPtr | R_BX, rC = mPtr | R_CX;
		int rS = mPtr | R_SP, rP = mPtr | R_BP;
		
		if (globalAddrReg != RegA || usedRegs != RegA)
		{ //globalAddrReg is the only allocated register
			fatalError(ERR_INVGLOBADDRREG);
			return;
		}
		//fill throw frame: destination
		ins(I_PUSHip);
		appendInstruction(relaDummy = getUnlinkedInstruction());
		ins(I_POPreg, rB);
		insPatchedAdd(0, relaDummy, dest);
		ins(I_MOVmemreg, rP, rB, throwBlockOffset + relocBytes);
		//fill throw frame: current state registers
		ins(I_MOVmemreg, rP, rClss, throwBlockOffset + relocBytes * 2);
		ins(I_MOVmemreg, rP, rInst, throwBlockOffset + relocBytes * 3);
		ins(I_MOVmemreg, rP, rP, throwBlockOffset + relocBytes * 4);
		ins(I_MOVmemreg, rP, rS, throwBlockOffset + relocBytes * 5);
		//fill throw frame: reset exception variable
		ins(I_XORregreg, rB, rB);
		ins(I_MOVmemreg, rP, rB, throwBlockOffset + relocBytes * 6);
		//get global frame pointer, save in current frame, set current frame as global
		ins(I_MOVregmem, rB, rA);
		ins(I_LEAregmem, rC, rP, throwBlockOffset);
		ins(I_MOVmemreg, rC, rB);
		ins(I_MOVmemreg, rA, rC);
	}
	
	public void genThrowFrameUpdate(Instruction oldDest, Instruction newDest, int throwBlockOffset)
	{
		insPatchedAdd(throwBlockOffset + relocBytes, oldDest, newDest);
	}
	
	public void genThrowFrameReset(int globalAddrReg, int throwBlockOffset)
	{
		int rA = mPtr | R_AX, rB = mPtr | R_BX, rP = mPtr | R_BP;
		if (globalAddrReg != RegA || usedRegs != RegA)
		{ //globalAddrReg is the only allocated register
			fatalError(ERR_INVGLOBADDRREG);
			return;
		}
		ins(I_MOVregmem, rB, rP, throwBlockOffset);
		ins(I_MOVmemreg, rA, rB);
	}
	
	//common internal methods
	public void insJump(Instruction dest, int cond)
	{
		Instruction me = getUnlinkedInstruction();
		appendInstruction(me);
		//size jump for maximum possible length of jump, will be optimized later if possible
		
		me.size = fullIPChangeBytes + (cond != 0 ? 2 : 1);
		me.type = I_JUMP;
		if (nextJumpsUnsigned)
			switch (cond)
			{
				case Ops.C_LW:
					cond = Ops.C_BO;
					break;
				case Ops.C_LE:
					cond = SC_BE;
					break;
				case Ops.C_GE:
					cond = SC_AE;
					break;
				case Ops.C_GT:
					cond = SC_AB;
					break;
			}
		me.iPar1 = cond;
		me.jDest = dest;
		dest.isDest = true;
	}
	
	public void insPatchedCall(Mthd refMthd, int par)
	{
		Instruction i;
		
		//get a new instruction and insert it
		appendInstruction(i = getUnlinkedInstruction());
		i.type = I_CALLimm;
		i.refMthd = refMthd;
		//code instruction
		i.putByte(0xE8);
		i.size += fullIPChangeBytes;
		addToCodeRefFixupList(i, 1);
	}
	
	//add dest-rela to mode==0: (e/r)bx; mode!=0: [(e/r)bp+mode]
	public void insPatchedAdd(int mode, Instruction i1, Instruction i2)
	{
		Instruction i;
		
		//get a new instruction and insert it
		appendInstruction(i = getUnlinkedInstruction());
		i.type = I_ADDpatched;
		i.iPar1 = mode;
		i.jDest = i1;
		i1.isDest = true;
		//do no yet code instruction, just enter worst case size
		i.size = fullIPChangeBytes + 2;
		if (mode != 0)
			i.size++;
		if (patchedAddPrefix != 0)
			i.size++;
		//add dummy-instruction for i2
		appendInstruction(i = getUnlinkedInstruction());
		i.type = I_IHELPER;
		i.jDest = i2;
		i2.isDest = true;
	}
	
	protected boolean fixJump(Instruction me)
	{ //returns true if size of instruction has changed -> redo for already done jumps
		int relative = 0, bbbb, i, pos;
		Instruction dummy, dest;
		
		if ((dest = me.jDest) == null)
		{
			fatalError(ERR_JUMPDESTINIT_FIXJUMP);
			return false;
		}
		//get offset
		if (me.instrNr >= dest.instrNr)
		{ //get destination before us
			relative -= (dummy = me).size;
			while (dummy != dest)
			{
				dummy = dummy.prev;
				if (dummy == null)
				{
					fatalError(ERR_JUMPDESTNOTFOUND_FIXJUMP);
					return false;
				}
				relative -= dummy.size;
			}
		}
		else
		{ //get destination behind us
			if (isUntilNextRealIns(dummy = me.next, dest))
			{ //do not jump to next instruction
				me.type = I_NONE;
				me.size = 0;
				return true;
			}
			while (dummy != dest)
			{
				relative += dummy.size;
				dummy = dummy.next;
				if (dummy == null)
				{
					fatalError(ERR_JUMPDESTNOTFOUND_FIXJUMP);
					return false;
				}
			}
		}
		
		if (me.iPar1 == 0)
		{ //jump unconditionally
			if (relative >= -128 && relative <= 127)
			{ //always a short-jump
				me.code[0] = (byte) 0xEB;
				me.code[1] = (byte) relative;
				if (me.size == 2)
					return false;
				me.size = 2;
				return true;
			}
			if (relative >= -131 && relative <= -129 && me.size == 5)
			{ //re-encoding far jump as short jump allows short jump
				relative += 3; //distance will be 3 bytes nearer and then fit in short jump
				me.code[0] = (byte) 0xEB;
				me.code[1] = (byte) relative;
				me.size = 2;
				return true;
			}
			me.code[0] = (byte) 0xE9;
			pos = 1;
		}
		else
		{
			switch (me.iPar1)
			{ //contains condition
				case SC_BE:
					bbbb = 0x6;
					break; //jump if below or equal
				case SC_AE:
					bbbb = 0x3;
					break; //jump if above or equal
				case SC_AB:
					bbbb = 0x7;
					break; //jump if above
				case SC_US:
					bbbb = 0x9;
					break; //jump if unsigned
				case Ops.C_LW:
					bbbb = 0xC;
					break; //jump if less
				case Ops.C_LE:
					bbbb = 0xE;
					break; //jump if less or equal
				case Ops.C_EQ:
					bbbb = 0x4;
					break; //jump if equal
				case Ops.C_GE:
					bbbb = 0xD;
					break; //jump if greater or equal (==not less)
				case Ops.C_GT:
					bbbb = 0xF;
					break; //jump if greater (==not less or equal)
				case Ops.C_NE:
					bbbb = 0x5;
					break; //jump if not equal
				case Ops.C_BO:
					bbbb = 0x2;
					break; //jump if carry (==below ==not above or equal)
				default:
					fatalError(ERR_CONDJUMP_FIXJUMP);
					return false;
			}
			if (relative >= -128 && relative <= 127)
			{ //always a short-jump
				me.code[0] = (byte) (0x70 | bbbb);
				me.code[1] = (byte) relative;
				if (me.size == 2)
					return false;
				me.size = 2;
				return true;
			}
			if (relative >= -131 && relative <= -129 && me.size == 5)
			{ //re-encoding far jump as short jump allows short jump
				relative += 3; //distance will be 3 bytes nearer and then fit in short jump
				me.code[0] = (byte) (0x70 | bbbb);
				me.code[1] = (byte) relative;
				me.size = 2;
				return true;
			}
			me.code[0] = (byte) 0x0F;
			me.code[1] = (byte) (0x80 | bbbb);
			pos = 2;
		}
		me.code[pos++] = (byte) relative;
		for (i = 1; i < fullIPChangeBytes; i++)
			me.code[pos++] = (byte) (relative = relative >>> 8);
		return false;
	}
	
	protected boolean fixAdd(Instruction me)
	{
		int oldSize = me.size, relative = 0, i, byteAdd = 0;
		Instruction from, to;
		
		from = me.jDest;
		to = me.next.jDest;
		if (from.instrNr >= to.instrNr)
		{ //invalid: to before from
			fatalError(ERR_JUMPDESTNOTFOUND_FIXJUMP);
			return false;
		}
		//get destination behind us
		while (from != to)
		{
			relative += from.size;
			from = from.next;
			if (from == null)
			{
				fatalError(ERR_JUMPDESTNOTFOUND_FIXJUMP);
				return false;
			}
		}
		
		me.size = 0; //reset instruction size
		if (patchedAddPrefix != 0)
			me.putByte(patchedAddPrefix);
		if (relative <= 127)
			byteAdd = 2; //used as bit and flag
		me.putByte(0x81 + byteAdd);
		if (me.iPar1 == 0)
		{ //add to (e/r)bx
			me.putByte(0xC3);
		}
		else
		{ //add to [(e/r)bp]
			me.putByte(0x45);
			me.putByte(me.iPar1);
		}
		me.putByte(relative);
		if (byteAdd == 0)
			for (i = 1; i < fullIPChangeBytes; i++)
				me.putByte(relative = relative >>> 8);
		return me.size != oldSize;
	}
	
	protected boolean fixStackExtremeAdd(Instruction me)
	{
		int oldSize = me.size, maxStackUsage = 0, curStackUsage = 0, calls = 0;
		Instruction check = me;
		while (check != null)
		{
			switch (check.type)
			{
				case I_PUSHA:
					curStackUsage += 8;
					break;
				case I_PUSHF:
				case I_PUSHimm:
				case I_PUSHip:
				case I_PUSHmem:
				case I_PUSHreg:
					curStackUsage++;
					break;
				case I_POPA:
					curStackUsage -= 8;
					break;
				case I_POPF:
				case I_POPreg:
					curStackUsage--;
					break;
				case I_CALLimm:
				case I_CALLmem:
				case I_CALLreg:
					calls = 1;
			}
			if (curStackUsage > maxStackUsage)
				maxStackUsage = curStackUsage;
			check = check.next;
		}
		internalFixStackExtremeAdd(me, curStackUsage + calls);
		return oldSize != me.size;
	}
	
	protected void hasFltDblWarning()
	{
		if (!alreadyPrintedFltDblError)
		{
			ctx.out.println("warning: no support for float/double (skipping instructions)");
			alreadyPrintedFltDblError = true;
		}
	}
	
	protected void errorFPU()
	{
		fatalError("error in FPU-register (de)allocation");
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
			if (now.type != I_NONE && now.type != I_IHELPER)
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
	
	protected String getRegName(int reg)
	{
		switch (reg)
		{
			case R_AH:
				return "AH";
			case R_AL:
				return "AL";
			case R_AX:
				return "AX";
			case R_EAX:
				return "EAX";
			case R_BL:
				return "BL";
			case R_BX:
				return "BX";
			case R_BP:
				return "BP";
			case R_CL:
				return "CL";
			case R_CX:
				return "CX";
			case R_DI:
				return "DI";
			case R_DL:
				return "DL";
			case R_DX:
				return "DX";
			case R_EBP:
				return "EBP";
			case R_EBX:
				return "EBX";
			case R_ECX:
				return "ECX";
			case R_EDI:
				return "EDI";
			case R_EDX:
				return "EDX";
			case R_ESI:
				return "ESI";
			case R_ESP:
				return "ESP";
			case R_SI:
				return "SI";
			case R_SP:
				return "SP";
		}
		return "INVREG";
	}
	
	protected void printReg(TextPrinter v, int reg)
	{
		v.print(getRegName(reg));
	}
	
	protected void printMem(TextPrinter v, int reg, int disp, boolean asmMode)
	{
		v.print('[');
		if (asmMode && reg == 0 && disp >= ctx.mem.getBaseAddress() && disp < ctx.mem.getBaseAddress() + ctx.getMaximumImageSize())
		{
			v.print("$_A_");
			v.printHexFix(disp, fullIPChangeBytes << 1);
		}
		else
		{
			if (reg != 0)
			{
				printReg(v, reg);
				if (disp > 0)
					v.print('+');
				else if (disp < 0)
				{
					v.print('-');
					disp = -disp;
				}
			}
			if (disp != 0)
			{
				v.print("0x");
				v.printHexFix(disp, fullIPChangeBytes << 1);
			}
		}
		v.print("]");
	}
	
	protected void printSize(TextPrinter v, int par)
	{
		switch (par)
		{
			case 1:
				v.print("byte ");
				break;
			case 2:
				v.print("word ");
				break;
			case 4:
				v.print("dword ");
				break;
			case 8:
				v.print("qword ");
				break;
			default:
				v.print("invSz ");
		}
	}
	
	protected void printJumpDest(TextPrinter v, Instruction dest)
	{
		ctx.printUniqueMethodName(v, mthdContainer);
		v.print("$i");
		v.print(dest.instrNr);
	}
	
	private int print(TextPrinter v, Instruction i, boolean asmMode)
	{
		int cnt, reg1, tmp;
		boolean printMemSize = false, mathImm = false, thirdRegCL = false;
		
		if (i.type == I_NONE)
			return 0;
		if (asmMode)
			v.print("  ");
		else
		{
			v.print(i.instrNr);
			v.print(": ");
		}
		switch (i.type)
		{
			case I_MAGC:
				v.print(asmMode ? "db" : "MAGIC");
				for (cnt = 0; cnt < i.size; cnt++)
				{
					if (cnt > 0)
						v.print(',');
					v.print(" 0x");
					v.printHexFix(i.code[cnt], 2);
				}
				return cnt;
			case I_JUMP:
				switch (i.iPar1)
				{ //contains condition
					case 0:
						v.print("JMP");
						break; //jump unconditionally
					case SC_BE:
						v.print("JBE");
						break; //jump if below or equal
					case SC_AE:
						v.print("JAE");
						break; //jump if above or equal
					case SC_AB:
						v.print("JA");
						break; //jump if above
					case SC_US:
						v.print("JNS");
						break; //jump if unsigned
					case Ops.C_LW:
						v.print("JL");
						break; //jump if less
					case Ops.C_LE:
						v.print("JLE");
						break; //jump if less or equal
					case Ops.C_EQ:
						v.print("JE");
						break; //jump if equal
					case Ops.C_GE:
						v.print("JGE");
						break; //jump if greater or equal (==not less)
					case Ops.C_GT:
						v.print("JG");
						break; //jump if greater (==not less or equal)
					case Ops.C_NE:
						v.print("JNE");
						break; //jump if not equal
					case Ops.C_BO:
						v.print("JB");
						break; //jump if carry (==below ==not above or equal)
					default:
						v.println(ERR_CONDJUMP_FIXJUMP);
				}
				if (asmMode)
				{
					v.print(' ');
					if (i.size < 1 + relocBytes)
						v.print("short ");
					printJumpDest(v, i.jDest);
				}
				else
				{
					v.print(" to i");
					v.print(i.jDest.instrNr);
				}
				break;
			case I_CALLimm:
				v.print("CALL ");
				if (asmMode)
					ctx.printUniqueMethodName(v, i.refMthd);
				else
				{
					i.refMthd.owner.printNameWithOuter(v);
					v.print('.');
					i.refMthd.printNamePar(v);
				}
				break;
			case I_LEAarray: //will result in lea reg0,[reg0+reg1*par+disp] (only for pointers!)
				v.print("LEA ");
				printReg(v, i.reg0);
				v.print(",[");
				printReg(v, i.reg0);
				v.print('+');
				v.print(getRegName(i.reg1));
				if (i.iPar3 != 0)
				{
					v.print("*");
					v.print(i.iPar3);
				}
				if (i.iPar1 != 0)
				{
					if (i.iPar1 > 0)
					{
						v.print("+0x");
						tmp = i.iPar1;
					}
					else
					{
						v.print("-0x");
						tmp = -i.iPar1;
					}
					v.printHexFix(tmp, fullIPChangeBytes << 1);
				}
				v.print(']');
				break;
			case I_MOVindexed: //will result in mov reg0,[reg0+reg1] (only for pointers!)
				v.print("MOV ");
				printReg(v, i.reg0);
				v.print(",[");
				printReg(v, i.reg0);
				v.print('+');
				printReg(v, i.reg1);
				v.print(']');
				break;
			case I_INreg:
				v.print("IN ");
				printReg(v, i.reg0);
				v.print(",DX");
				break;
			case I_BOUNDEXC:
				v.print("INT 0x05");
				break;
			case I_RETMSEXC:
				v.print("INT 0x1F");
				break;
			case I_MARKER:
				v.print("NOP");
				if (asmMode)
					v.print(" ;marker");
				break;
			case I_STEXreg:
				v.print("STEX");
				break;
			case I_POPdummy:
				if (asmMode)
					v.print("POP ECX ;pop dummy"); //POPdummy is used only in IA32 and equals "POP ECX"
				else
					v.print("POPdummy");
				break;
			case I_ADDpatched:
				v.print("ADD ");
				if (i.iPar1 == 0)
					printReg(v, getReg(1, RegB, StdTypes.T_PTR, false)); //add to (e/r)bx
				else
				{
					printSize(v, relocBytes);
					v.print('[');
					printReg(v, rBase); //add to [(e/r)bp]
					v.print(']');
				}
				v.print(",(");
				printJumpDest(v, i.next.jDest);
				v.print('-');
				printJumpDest(v, i.jDest);
				v.print(')');
				break;
			case I_PUSHip:
				if (asmMode)
				{
					v.print("CALL ");
					printJumpDest(v, i.next);
					i.next.isDest = true;
				}
				else
					v.print("PUSHIP");
				break;
			default:
				reg1 = i.reg1; //may be replaced for fixed second operand instructions
				switch (i.type & IM_OP)
				{
					case I_MOV:
						v.print("MOV ");
						if ((i.type & IM_P0) == I_mem0 && (i.type & IM_P1) == I_imm1)
							printMemSize = true;
						break;
					case I_MOVSX:
						v.print("MOVSX ");
						if ((i.type & IM_P0) == I_mem0 || (i.type & IM_P1) == I_mem1)
							printMemSize = true;
						break;
					case I_MOVZX:
						v.print("MOVZX ");
						if ((i.type & IM_P0) == I_mem0 || (i.type & IM_P1) == I_mem1)
							printMemSize = true;
						break;
					case I_LEA:
						v.print("LEA ");
						break;
					case I_PUSH:
						v.print("PUSH ");
						mathImm = true;
						break;
					case I_POP:
						v.print("POP ");
						break;
					case I_PUSHA:
						v.print("PUSHA");
						break;
					case I_POPA:
						v.print("POPA");
						break;
					case I_PUSHF:
						v.print("PUSHF");
						break;
					case I_POPF:
						v.print("POPF");
						break;
					case I_CALL:
						v.print("CALL ");
						break;
					case I_RET:
						v.print("RET ");
						if (i.iPar2 == 0)
							return 1;
						break;
					case I_IRET:
						v.print("IRET");
						break;
					case I_ADD:
						v.print("ADD ");
						mathImm = true;
						if ((i.type & IM_P0) == I_mem0 && (i.type & IM_P1) == I_imm1)
							printMemSize = true;
						break;
					case I_ADC:
						v.print("ADC ");
						mathImm = true;
						if ((i.type & IM_P0) == I_mem0 && (i.type & IM_P1) == I_imm1)
							printMemSize = true;
						break;
					case I_SUB:
						v.print("SUB ");
						mathImm = true;
						if ((i.type & IM_P0) == I_mem0 && (i.type & IM_P1) == I_imm1)
							printMemSize = true;
						break;
					case I_SBB:
						v.print("SBB ");
						mathImm = true;
						if ((i.type & IM_P0) == I_mem0 && (i.type & IM_P1) == I_imm1)
							printMemSize = true;
						break;
					case I_AND:
						v.print("AND ");
						mathImm = true;
						if ((i.type & IM_P0) == I_mem0 && (i.type & IM_P1) == I_imm1)
							printMemSize = true;
						break;
					case I_XOR:
						v.print("XOR ");
						mathImm = true;
						if ((i.type & IM_P0) == I_mem0 && (i.type & IM_P1) == I_imm1)
							printMemSize = true;
						break;
					case I_OR:
						v.print("OR ");
						mathImm = true;
						if ((i.type & IM_P0) == I_mem0 && (i.type & IM_P1) == I_imm1)
							printMemSize = true;
						break;
					case I_TEST:
						v.print("TEST ");
						break;
					case I_MUL:
						v.print("MUL ");
						mathImm = true;
						break;
					case I_IMUL:
						v.print("IMUL ");
						mathImm = true;
						break;
					case I_DIV:
						v.print("DIV ");
						mathImm = true;
						break;
					case I_IDIV:
						v.print("IDIV ");
						mathImm = true;
						break;
					case I_INC:
						v.print("INC ");
						printMemSize = true;
						break;
					case I_DEC:
						v.print("DEC ");
						printMemSize = true;
						break;
					case I_NEG:
						v.print("NEG ");
						break;
					case I_NOT:
						v.print("NOT ");
						break;
					case I_CMP:
						v.print("CMP ");
						mathImm = true;
						break;
					case I_XCHG:
						v.print("XCHG ");
						break;
					case I_SHL:
						v.print("SHL ");
						reg1 = R_CL;
						break;
					case I_SHR:
						v.print("SHR ");
						reg1 = R_CL;
						break;
					case I_SHLD:
						v.print("SHLD ");
						thirdRegCL = true;
						break;
					case I_SHRD:
						v.print("SHRD ");
						thirdRegCL = true;
						break;
					case I_SAR:
						v.print("SAR ");
						reg1 = R_CL;
						break;
					case I_ROL:
						v.print("ROL ");
						reg1 = R_CL;
						break;
					case I_ROR:
						v.print("ROR ");
						reg1 = R_CL;
						break;
					case I_RCR:
						v.print("RCR ");
						reg1 = R_CL;
						break;
					case I_BSR:
						v.print("BSR ");
						break;
					case I_CDQ:
						v.print("CDQ");
						break;
					case I_CWDE:
						v.print("CWDE");
						break;
					case I_CDQE:
						v.print("CDQE");
						break;
					case I_CQO:
						v.print("CQO");
						break;
					case I_OUT:
						v.print("OUT DX,");
						break;
					case I_SAHF:
						v.print("SAHF");
						break;
					//floating point instructions
					case I_FLD:
						v.print("FLD ");
						break;
					case I_FSTP:
						v.print("FSTP ");
						break;
					case I_FILD:
						v.print("FILD ");
						break;
					case I_FISTP:
						v.print("FISTP ");
						break;
					case I_FISTTP:
						v.print("FISTTP ");
						break;
					case I_FCHS:
						v.print("FCHS");
						break;
					case I_FADDP:
						v.print("FADDP");
						break;
					case I_FSUBP:
						v.print("FSUBP");
						break;
					case I_FMULP:
						v.print("FMULP");
						break;
					case I_FDIVP:
						v.print("FDIVP");
						break;
					case I_FDUP:
						v.print("FDUP");
						break;
					case I_FXCH:
						v.print("FXCH");
						break;
					case I_FFREE:
						v.print("FFREE");
						break;
					case I_FINCSTP:
						v.print("FINCSTP");
						break;
					case I_FCOMPP:
						v.print("FCOMPP");
						break;
					case I_FCOMIP:
						v.print("FCOMIP");
						break;
					case I_FSTSW:
						v.print("FSTSW");
						break;
					case I_FNSTCW:
						v.print("FNSTCW ");
						break;
					case I_FLDCW:
						v.print("FLDCW ");
						break;
					case I_FWAIT:
						v.print("FWAIT");
						break;
					default:
						v.print("unknown instruction 0x");
						v.printHexFix(i.type, 8);
						return 1;
				}
				switch (i.type & IM_P0)
				{
					case I_reg0:
						printReg(v, i.reg0);
						break;
					case I_mem0:
						if (printMemSize)
							printSize(v, i.iPar3);
						printMem(v, i.reg0, i.iPar1, asmMode);
						break;
					case I_imm0:
						if (mathImm && i.iPar2 < 0)
						{
							v.print('-');
							tmp = -i.iPar2;
						}
						else
							tmp = i.iPar2;
						v.print("0x");
						v.printHexFix(tmp, fullIPChangeBytes << 1);
						break;
					case I_iml0:
						v.print("0x");
						v.printHexFix((int) (i.lPar >>> 32), 8);
						v.printHexFix((int) i.lPar, 8);
						break;
				}
				if ((i.type & IM_P1) != 0)
				{
					v.print(',');
					switch (i.type & IM_P1)
					{
						case I_reg1:
							printReg(v, reg1);
							break;
						case I_mem1:
							if (printMemSize)
								printSize(v, i.iPar3);
							printMem(v, reg1, i.iPar1, asmMode);
							break;
						case I_imm1:
							if (mathImm && i.iPar2 < 0)
							{
								v.print('-');
								tmp = -i.iPar2;
							}
							else
								tmp = i.iPar2;
							v.print("0x");
							v.printHexFix(tmp, fullIPChangeBytes << 1);
							break;
						case I_iml1:
							v.print("0x");
							v.printHexFix((int) (i.lPar >>> 32), 8);
							v.printHexFix((int) i.lPar, 8);
							break;
					}
				}
				if (thirdRegCL)
				{
					v.print(',');
					v.print(getRegName(R_CL));
				}
		}
		return 1;
	}
}
