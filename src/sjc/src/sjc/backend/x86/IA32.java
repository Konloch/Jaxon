/* Copyright (C) 2005, 2006, 2007, 2008, 2009, 2010, 2015, 2024 Stefan Frenz
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
import sjc.compbase.Marks;
import sjc.compbase.Mthd;
import sjc.compbase.Ops;
import sjc.compbase.StdTypes;
import sjc.osio.TextPrinter;

/**
 * IA32: architecture implementation for 32 bit protected mode IA32 processors
 *
 * @author S. Frenz
 * @version 240408 fixed float-to-long conversion register bug
 * version 151108 fixed optimized encoding of I_MOVmemreg for EAX-register
 * version 101210 adopted changed Architecture
 * version 101125 added support for native callback epilog style
 * version 101101 adopted changed Architecture
 * version 101027 fixed sign handling for logical bitshift of byte and short values
 * version 100927 fixed unsignedness of chars
 * version 100924 fixed access to parameter in interrupt method after inline method
 * version 100127 adopted renaming of ariCall into binAriCall
 * version 100115 adopted changed error reporting and codeStart-movement
 * version 091109 using POPdummy after codeEpilog-optimization to allow IA32Opti to distinguish
 * version 091105 optimized codeEpilog
 * version 091026 added optimized version of genBinOpConst*
 * version 091013 adopted changed method signature of genStore*
 * version 091001 adopted changed memory interface
 * version 090717 adopted changed Architecture
 * version 090626 added support for stack extreme check
 * version 090619 adopted changed Architecture
 * version 090430 added support for native "return missing" hint
 * version 090219 adopted changed X86Base
 * version 090218 clarified genLoadConstDoubleOrLongVal and made getReg protected again (now supporting inherited access)
 * version 090215 made getReg public to enable SRBT_IA32 overwriting it
 * version 090208 removed genClearMem
 * version 090207 added copyright notice and optimized genStoreVarConstDoubleOrLongVal
 * version 090206 added support for optimized genStoreVar*
 * version 081209 added support for method printing
 * version 081021 adopted changes in Architecture
 * version 080612 added parameter rtlc as replacement for global -f
 * version 080605 added support for language throwables
 * version 080525 adopted changed genCondJmp signature
 * version 080205 added support for noStackOptimization
 * version 080122 fixed setting of usedRegs in getReg on firstWrite
 * version 080105 added genSavePrimary and genRestPrimary
 * version 071102 fixed constant long compare
 * version 071011 fixed float parameter for native method calls
 * version 071001 added support for native method calls
 * version 070917 pre-initialization of stack variables with "0"
 * version 070915 added FWAIT in 487-mode
 * version 070913 moved curVarOffParam to X86Base, added support for genClearMem
 * version 070910 fixed bug in conversion float/double->int, added support for inling methods with unclear FPU-stack
 * version 070829 optimized genLoadVarVal
 * version 070816 added float/double-optimization with FCOMIP
 * version 070815 added float/double-conversion without FISTTP
 * version 070814 moved float/double basics to X86Base
 * version 070812 added float/double compares
 * version 070809 added support for float and double
 * version 070723 changed bound exception interrupt number from 0x21 to 0x05
 * version 070705 fixed genCompValToConstLongVal
 * version 070628 added allocClearBits
 * version 070615 removed no longer needed getRef
 * version 070610 optimized access to different jump offset sizes
 * version 070606 moved common methods and variables to X86Base
 * version 070601 changed boolean register allocation to use 8-bit-register, optimized genBinOp, externalized Strings to X86Base
 * version 070531 adopted removal of Architecture.genLoadFromMem
 * version 070513 fixed insPatchedCall, added opcodes for ADD/SUB/AND/XOR/OR-regmem
 * version 070505 changed naming of Clss to Unit, changed OutputObject to int
 * version 070501 optimized insPatched
 * version 070422 fixed param-offset in interrupt routines
 * version 070127 optimized information output
 * version 070114 removed never called methods, reduced access level, added several opcodes
 * version 070113 adopted change of genCheckNull to genCompPtrToNull
 * version 070108 fixed genCall-parSize, added SHLregimm
 * version 070104 optimized register allocation
 * version 070101 fixed fixJump, adopted change in genCall
 * version 061231 added opcodes for AND/XOR/OR-regimm and -memimm
 * version 061229 removed access to firstInstr
 * version 061228 optimized codeEpilog
 * version 061225 support for inline code generation
 * version 061203 optimized assign/check and calls to printPos and compErr
 * version 061202 adopted change of genCall
 * version 061111 adopted change of Architecture.codeEpilog
 * version 061109 adapted Ops.C_BO
 * version 061107 fixed genLoadDerefAddr for odd values
 * version 061105 optimized genLoadVarAddr if offset==0 and src==dst
 * version 060723 integration of structure of IA32Opti to enable extension
 * version 060628 added support for static compilation
 * version 060621 fixed genLoadConstVal
 * version 060620 bugfixes in long-handling
 * version 060616 inserted genCopyInstContext
 * version 060608 fixed debug-output
 * version 060607 initial version
 */

public class IA32 extends X86Base
{
	//offsets of parameters
	protected final static int VAROFF_PARAM_INL = 4;
	protected final static int VAROFF_PARAM_NRM = 8;
	protected final static int VAROFF_PARAM_INT = 36;
	
	//configuration
	protected boolean useOnly487, useFISTTP, noStackOptimization; //initialized to false
	
	//initialization
	public IA32()
	{
		relocBytes = 4;
		allocClearBits = stackClearBits = 3;
		maxInstrCodeSize = 10;
		rAll = RegA | RegB | RegC | RegD;
		rClss = R_EDI;
		rInst = R_ESI;
		rBase = R_EBP;
		fullIPChangeBytes = 4;
		mPtr = RS_E;
	}
	
	public boolean setParameter(String parm, TextPrinter v)
	{
		if ("sse3".equals(parm))
		{
			v.println("using instructions FCOMPIP and FISTTP available since P4-SSE3");
			useFISTTP = true;
		}
		else if ("i487".equals(parm))
		{
			v.println("avoiding FCOMPIP and inserting FWAIT for i487");
			useOnly487 = true;
		}
		else if ("nsop".equals(parm))
		{
			v.println("always building complete stack frame");
			noStackOptimization = true;
		}
		else if ("rtlc".equals(parm))
		{
			v.println("using runtime call for long-div and -mod");
			binAriCall[StdTypes.T_LONG] |= (1 << (Ops.A_DIV - Ops.MSKBSE)) | (1 << (Ops.A_MOD - Ops.MSKBSE));
		}
		else
		{
			v.println("invalid parameter for IA32, possible parameters:");
			printParameter(v);
			return false;
		}
		return true;
	}
	
	public static void printParameter(TextPrinter v)
	{
		v.println("   i487 - disable FCOMIP-instruction (since PPro) and insert FWAIT for i487");
		v.println("   sse3 - enable FCOMIP- and FISTTP-instruction (since P4-SSE3)");
		v.println("   nsop - always build complete stack frame");
		v.println("   rtlc - long-div and -mod through rte");
	}
	
	//references are treated as normal integers
	public void putRef(Object loc, int offset, Object ptr, int ptrOff)
	{
		mem.putInt(loc, offset, mem.getAddrAsInt(ptr, ptrOff));
	}
	
	public void putCodeRef(Object loc, int offset, Object ptr, int ptrOff)
	{
		mem.putInt(loc, offset, mem.getAddrAsInt(ptr, ptrOff) - mem.getAddrAsInt(loc, offset) - 4);
	}
	
	//register allocation and de-allocation
	protected int freeRegSearch(int mask, int type)
	{
		int ret, ret2;
		
		if ((ret = bitSearch(mask, 1, RegA | RegB | RegC)) == 0)
			return 0;
		if (type == StdTypes.T_LONG || type == StdTypes.T_DPTR)
		{
			if ((ret2 = bitSearch(mask & ~ret, 1, RegD)) == 0)
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
			ins(I_PUSHreg, R_EAX);
			stored |= RegA;
		}
		if ((regs & RegB) != 0)
		{
			ins(I_PUSHreg, R_EBX);
			stored |= RegB;
		}
		if ((regs & RegC) != 0)
		{
			ins(I_PUSHreg, R_ECX);
			stored |= RegC;
		}
		if ((regs & RegD) != 0)
		{
			ins(I_PUSHreg, R_EDX);
			stored |= RegD;
		}
		return stored;
	}
	
	protected void restoreReg(int regs)
	{
		usedRegs |= regs;
		writtenRegs |= regs;
		if ((regs & RegD) != 0)
			ins(I_POPreg, R_EDX);
		if ((regs & RegC) != 0)
			ins(I_POPreg, R_ECX);
		if ((regs & RegB) != 0)
			ins(I_POPreg, R_EBX);
		if ((regs & RegA) != 0)
			ins(I_POPreg, R_EAX);
	}
	
	protected int internalGetReg(int nr, int reg, int type, boolean firstWrite)
	{ //never called for float or double
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
			default:
				fatalError(ERR_INVREG_GETREG);
				return 0;
		}
		switch (type)
		{
			case StdTypes.T_LONG:
			case StdTypes.T_DPTR:
				return reg | 0x7;
			case StdTypes.T_INT:
			case StdTypes.T_PTR:
				if (nr != 1)
					break;
				return reg | 0x7;
			case StdTypes.T_SHRT:
			case StdTypes.T_CHAR:
				if (nr != 1)
					break;
				return reg | 0x3;
			case StdTypes.T_BOOL:
			case StdTypes.T_BYTE:
				if (nr != 1)
					break;
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
			if (curMthd.parSize != 0 && curMthd.parSize != 4)
			{
				fatalError(ERR_INVPARINT);
				return;
			}
			curVarOffParam = VAROFF_PARAM_INT;
			ins(I_PUSHA);
		}
		ins(I_PUSHreg, R_EBP);
		ins(I_MOVregreg, R_EBP, R_ESP);
		switch (curMthd.varSize)
		{
			case 0:
				break;
			case 4:
				ins(I_PUSHimm);
				break;
			default:
				ins(I_XORregreg, R_EBX, R_EBX);
				for (i = curMthd.varSize; i > 0; i -= 4)
					ins(I_PUSHreg, R_EBX);
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
					ins(I_POPreg, R_EBP);
					ins(I_ADDregimm, R_ESP, 0, 0, par);
				}
				else if (par == 0)
				{ //var!=0
					ins(I_ADDregimm, R_ESP, 0, 0, var);
					ins(I_POPreg, R_EBP);
				}
				else
				{ //neither var==0 nor par==0
					ins(I_MOVregmem, R_EBP, R_EBP);
					ins(I_ADDregimm, R_ESP, 0, 0, var + par + 4);
				}
			}
			else if (noStackOptimization)
				ins(I_POPreg, R_EBP); //no stack optimization - clean up stack frame
			if (--curInlineLevel == 0)
				curVarOffParam = (outline.marker & Marks.K_INTR) != 0 ? VAROFF_PARAM_INT : VAROFF_PARAM_NRM;
			curMthd = outline;
			return;
		}
		//normal end of method
		if (var == 0 && par == 0 && (curMthd.marker & Marks.K_INTR) == 0 && !noStackOptimization)
		{
			ins(I_RETimm); //nothing pushed, don't pop
			curMthd = null;
			return;
		}
		if (var != 0)
		{
			if (var <= 8)
			{
				ins(I_POPdummy);
				if (var == 8)
					ins(I_POPdummy);
			}
			else
				ins(I_ADDregimm, R_ESP, 0, 0, var);
		}
		ins(I_POPreg, R_EBP);
		if (par > 32767)
		{
			fatalError(ERR_RETSIZE_CODEEPILOG);
			return;
		}
		if ((curMthd.marker & Marks.K_INTR) != 0)
		{
			ins(I_POPA);
			if (par != 0)
				ins(I_ADDregimm, R_ESP, 0, 0, 4);
			ins(I_IRET);
		}
		else
			ins(I_RETimm, 0, 0, 0, (curMthd.marker & Marks.K_NTCB) == 0 ? par : 0);
		curMthd = null;
	}
	
	//general purpose instructions
	public void genLoadConstVal(int dst, int val, int type)
	{ //type not of interest here
		if (type == StdTypes.T_FLT)
		{
			if (dst != curFPUReg)
			{
				errorFPU();
				return;
			}
			ins(I_PUSHimm, 0, 0, 0, val);
			ins(I_FLDmem, R_ESP, 0, 0, 0, FPU32);
			ins(I_ADDregimm, R_ESP, 0, 0, 4); //pseudo-pop
		}
		else
		{
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
					//default: nothing to do for other types
			}
			ins(I_MOVregimm, dst, 0, 0, val);
		}
	}
	
	public void genLoadConstDoubleOrLongVal(int dst, long val, boolean asDouble)
	{
		int reg1, reg2;
		if (asDouble)
		{
			if (dst != curFPUReg)
			{
				errorFPU();
				return;
			}
			genPushConstDoubleOrLongVal(val, true);
			ins(I_FLDmem, R_ESP, 0, 0, 0, FPU64);
			ins(I_ADDregimm, R_ESP, 0, 0, 8); //pseudo-pop
		}
		else
		{
			if ((reg1 = getReg(1, dst, StdTypes.T_LONG, true)) == 0 || (reg2 = getReg(2, dst, StdTypes.T_LONG, true)) == 0)
				return;
			ins(I_MOVregimm, reg1, 0, 0, (int) val);
			ins(I_MOVregimm, reg2, 0, 0, (int) (val >>> 32));
		}
	}
	
	public void genLoadVarAddr(int dst, int src, Object loc, int off)
	{
		int pos = mem.getAddrAsInt(loc, off);
		if ((dst = getReg(1, dst, StdTypes.T_PTR, true)) == 0)
			return;
		if (src == 0)
		{
			ins(I_MOVregimm, dst, 0, 0, pos);
			return;
		}
		if (src == regBase && pos >= 0)
			pos += curVarOffParam;
		if ((src = getReg(1, src, StdTypes.T_PTR, false)) == 0)
			return;
		if (pos == 0 && src == dst) /*nothing to do*/
			return;
		ins(I_LEAregmem, dst, src, pos);
	}
	
	public void genLoadVarVal(int dstR, int src, Object loc, int off, int type)
	{
		int dst, dstL, pos = mem.getAddrAsInt(loc, off);
		if (src == regBase && pos >= 0)
			pos += curVarOffParam;
		if (src != 0 && (src = getReg(1, src, StdTypes.T_PTR, false)) == 0)
			return;
		if (type == StdTypes.T_FLT || type == StdTypes.T_DBL)
		{
			if (dstR != curFPUReg)
			{
				errorFPU();
				return;
			}
			ins(I_FLDmem, src, 0, pos, 0, type == StdTypes.T_FLT ? FPU32 : FPU64);
			return;
		}
		if ((dst = getReg(1, dstR, StdTypes.T_INT, true)) == 0)
			return;
		switch (type)
		{
			case StdTypes.T_BOOL:
				ins(I_MOVregmem, dst & ~(RS_E & ~RS_L), src, pos);
				return;
			case StdTypes.T_BYTE:
				ins(I_MOVSXregmem, dst, src, pos, 0, 1);
				return;
			case StdTypes.T_SHRT:
				ins(I_MOVSXregmem, dst, src, pos, 0, 2);
				return;
			case StdTypes.T_CHAR:
				ins(I_MOVZXregmem, dst, src, pos, 0, 2);
				return;
			case StdTypes.T_INT:
			case StdTypes.T_PTR:
				ins(I_MOVregmem, dst, src, pos);
				return;
			case StdTypes.T_LONG:
			case StdTypes.T_DPTR:
				if ((dstL = getReg(2, dstR, type, true)) == 0)
					return;
				if (dst != src)
				{ //first move dst, then dstL
					ins(I_MOVregmem, dst, src, pos);
					ins(I_MOVregmem, dstL, src, pos + 4);
				}
				else
				{
					ins(I_MOVregmem, dstL, src, pos + 4);
					ins(I_MOVregmem, dst, src, pos);
				}
				return;
		}
		fatalError(ERR_INVTYPE_GENLOADVARVAL);
	}
	
	public void genConvertVal(int dstR, int srcR, int toType, int fromType)
	{
		int dst, dstL, src, srcL;
		
		if (toType == StdTypes.T_FLT || toType == StdTypes.T_DBL)
		{
			if (dstR != curFPUReg)
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
					if ((src = getReg(1, srcR, StdTypes.T_INT, false)) == 0)
						return;
					ins(I_PUSHreg, src);
					ins(I_FILDmem, R_ESP, 0, 0, 0, FPU32);
					if (useOnly487)
						ins(I_FWAIT);
					ins(I_POPreg, src);
					return;
				case StdTypes.T_LONG:
					if ((src = getReg(1, srcR, StdTypes.T_LONG, false)) == 0 || (srcL = getReg(2, srcR, StdTypes.T_LONG, false)) == 0)
						return;
					ins(I_PUSHreg, srcL);
					ins(I_PUSHreg, src);
					ins(I_FILDmem, R_ESP, 0, 0, 0, FPU64);
					if (useOnly487)
						ins(I_FWAIT);
					ins(I_POPreg, src);
					ins(I_POPreg, srcL);
					return;
				case StdTypes.T_FLT:
				case StdTypes.T_DBL:
					return; //nothing to do
			}
			fatalError("error in IA32.genConvertVal to floating point");
			return;
		}
		//toType is not floating point
		if (fromType == StdTypes.T_FLT || fromType == StdTypes.T_DBL)
		{
			if (srcR != curFPUReg)
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
					if ((dst = getReg(1, dstR, toType, false)) == 0)
						return;
					ins(I_PUSHreg, dst | RS_E);
					if (useFISTTP)
						ins(I_FISTTPmem, R_ESP, 0, 0, 0, FPU32); //introduced with P4-SSE3
					else
					{
						ins(I_FNSTCWmem, R_ESP);                 //get FCW
						if (useOnly487)
							ins(I_FWAIT);
						ins(I_POPreg, dst | RS_E);                 //get value from stack
						ins(I_PUSHreg, dst | RS_E);                //save value to stack again
						ins(I_PUSHreg, dst | RS_E);                //save value to stack restore allocated space
						ins(I_ORregimm, (dst & ~RS_E) | RS_X, 0, 0, 0x0C00); //set truncate mode
						ins(I_PUSHreg, dst | RS_E);                //save value to stack
						ins(I_FLDCWmem, R_ESP);                  //set control word
						if (useOnly487)
							ins(I_FWAIT);
						ins(I_POPreg, dst | RS_E);                 //remove changed FCW
						ins(I_FISTPmem, R_ESP, 0, 4, 0, FPU32);  //store as INT onto stack, overwrite upper FCW
						ins(I_FLDCWmem, R_ESP);                  //load old control word
						if (useOnly487)
							ins(I_FWAIT);
						ins(I_POPreg, dst | RS_E);                 //remove original FCW
					}
					ins(I_POPreg, dst | RS_E);
					if (toType != StdTypes.T_INT)
						ins(I_MOVSXregreg, dst | RS_E, dst);
					dupFPUIns = null;
					popFPUDone++;
					return;
				case StdTypes.T_LONG:
					if ((dst = getReg(1, dstR, StdTypes.T_LONG, false)) == 0 || (dstL = getReg(2, dstR, StdTypes.T_LONG, false)) == 0)
						return;
					ins(I_PUSHreg, dstL);
					ins(I_PUSHreg, dst);
					if (useFISTTP)
						ins(I_FISTTPmem, R_ESP, 0, 0, 0, FPU64); //introduced with P4-SSE3
					else
					{
						ins(I_FNSTCWmem, R_ESP);                 //get FCW
						if (useOnly487)
							ins(I_FWAIT);
						ins(I_POPreg, dst | RS_E);                 //get value from stack
						ins(I_PUSHreg, dst | RS_E);                //save value to stack again
						ins(I_PUSHreg, dst | RS_E);                //save value to stack restore allocated space
						ins(I_ORregimm, (dst & ~RS_E) | RS_X, 0, 0, 0x0C00); //set truncate mode
						ins(I_PUSHreg, dst | RS_E);                //save value to stack
						ins(I_FLDCWmem, R_ESP);                  //set control word
						if (useOnly487)
							ins(I_FWAIT);
						ins(I_POPreg, dst | RS_E);                 //remove changed FCW
						ins(I_FISTPmem, R_ESP, 0, 4, 0, FPU64);  //store as INT onto stack, overwrite upper FCW
						ins(I_FLDCWmem, R_ESP);                  //load old control word
						if (useOnly487)
							ins(I_FWAIT);
						ins(I_POPreg, dst | RS_E);                 //remove original FCW
					}
					ins(I_POPreg, dst);
					ins(I_POPreg, dstL);
					dupFPUIns = null;
					popFPUDone++;
					return;
			}
			fatalError("error in IA32.genConvertVal from floating point");
			return;
		}
		//destination is always 32-bit-value, use only requested part of source
		if ((dst = getReg(1, dstR, StdTypes.T_INT, true)) == 0 || (src = getReg(1, srcR, toType, false)) == 0)
			return;
		//check special conversions
		if (toType == StdTypes.T_PTR || fromType == StdTypes.T_PTR)
		{
			if (fromType != StdTypes.T_INT && toType != StdTypes.T_INT)
			{
				fatalError(ERR_UNSTYPE_GENCONVERTVAL);
				return;
			}
			if (dst != src)
				ins(I_MOVregreg, dst, src);
			return;
		}
		//normal conversions
		if (toType == StdTypes.T_LONG)
		{ //to long is always the same
			if ((dstL = getReg(2, dstR, toType, true)) == 0)
				return;
			if (fromType == StdTypes.T_LONG)
			{
				if ((srcL = getReg(2, srcR, toType, false)) == 0)
					return;
				if (dstR == srcR) /*nothing to do for long->long*/
					return;
				if (dst == srcL)
				{
					ins(I_MOVregreg, dstL, srcL);
					ins(I_MOVregreg, dst, src);
					return;
				}
				ins(I_MOVregreg, dst, src);
				ins(I_MOVregreg, dstL, srcL);
				return;
			}
			//all others are int-values already, extend sign to complete upper register
			if ((src = getReg(1, srcR, StdTypes.T_INT, false)) == 0)
				return;
			if (src != dst)
				ins(I_MOVregreg, dst, src);
			if (dst == R_EAX && dstL == R_EDX)
				ins(I_CDQ);
			else
			{
				ins(I_MOVregreg, dstL, src);
				ins(I_SARregimm, dstL, 0, 0, 31);
			}
			return;
		}
		//toType!=T_LONG
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
					case StdTypes.T_LONG:
						//nothing to do for int/long->int
						if (dst != (src | RS_E))
							ins(I_MOVregreg, dst, src);
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
		if ((dst = getReg(1, dstR, StdTypes.T_INT, true)) == 0 || (src = getReg(1, srcR, StdTypes.T_INT, false)) == 0)
			return;
		switch (type)
		{
			case StdTypes.T_BOOL:
			case StdTypes.T_BYTE:
			case StdTypes.T_SHRT:
			case StdTypes.T_CHAR:
			case StdTypes.T_INT:
			case StdTypes.T_PTR:
				ins(I_MOVregreg, dst, src);
				return;
			case StdTypes.T_LONG:
			case StdTypes.T_DPTR:
				ins(I_MOVregreg, dst, src);
				if ((dst = getReg(2, dstR, StdTypes.T_LONG, true)) == 0 || (src = getReg(2, srcR, StdTypes.T_LONG, false)) == 0)
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
	{ //type is not of interest here
		ins(I_PUSHimm, 0, 0, 0, (int) (val >>> 32));
		ins(I_PUSHimm, 0, 0, 0, (int) val);
	}
	
	public void genPush(int srcR, int type)
	{
		int src;
		boolean isDouble = false;
		
		switch (type)
		{
			case StdTypes.T_NULL:
				fatalError(ERR_UNRPUSH_GENPUSH);
				return;
			case StdTypes.T_DBL:
				isDouble = true; //no break;
			case StdTypes.T_FLT:
				if (curFPUReg != srcR)
				{
					errorFPU();
					return;
				}
				//ins(I_SUBregimm, R_ESP, 0, 0, isDouble ? 8 : 4);
				ins(I_PUSHreg, R_EAX); //faster than the line above
				if (isDouble)
					ins(I_PUSHreg, R_EAX);
				ins(I_FSTPmem, R_ESP, 0, 0, 0, isDouble ? FPU64 : FPU32);
				if (useOnly487)
					ins(I_FWAIT);
				dupFPUIns = null;
				popFPUDone++;
				return;
			case StdTypes.T_LONG:
			case StdTypes.T_DPTR:
				if ((src = getReg(2, srcR, StdTypes.T_LONG, false)) == 0)
					return;
				ins(I_PUSHreg, src);
				//no return - has to do the following, too
		}
		//done by all except float/double
		if ((src = getReg(1, srcR, StdTypes.T_INT, false)) == 0)
			return;
		ins(I_PUSHreg, src);
	}
	
	public void genPop(int dstR, int type)
	{
		int dst;
		boolean isDouble = false;
		
		if (type == StdTypes.T_FLT || (isDouble = (type == StdTypes.T_DBL)) == true)
		{
			if (curFPUReg != dstR)
			{
				errorFPU();
				return;
			}
			ins(I_FLDmem, R_ESP, 0, 0, 0, isDouble ? FPU64 : FPU32);
			if (useOnly487)
				ins(I_FWAIT);
			ins(I_ADDregimm, R_ESP, 0, 0, isDouble ? 8 : 4);
			return;
		}
		if ((dst = getReg(1, dstR, StdTypes.T_INT, true)) == 0)
			return;
		ins(I_POPreg, dst);
		if (type == StdTypes.T_LONG || type == StdTypes.T_DPTR)
		{
			if ((dst = getReg(2, dstR, StdTypes.T_LONG, true)) == 0)
				return;
			ins(I_POPreg, dst);
		}
	}
	
	public void genAssign(int dst, int srcR, int type)
	{
		int src;
		boolean isDouble = false;
		
		if (type == StdTypes.T_FLT || (isDouble = (type == StdTypes.T_DBL)))
		{
			if (curFPUReg != srcR)
			{
				errorFPU();
				return;
			}
			if ((dst = getReg(1, dst, StdTypes.T_PTR, false)) == 0)
				return;
			dupFPUIns = ins(I_FDUP);
			ins(I_FSTPmem, dst, 0, 0, 0, isDouble ? FPU64 : FPU32);
			if (useOnly487)
				ins(I_FWAIT);
			return;
		}
		if ((dst = getReg(1, dst, StdTypes.T_PTR, false)) == 0 || (src = getReg(1, srcR, type, false)) == 0)
			return;
		switch (type)
		{
			case StdTypes.T_BOOL:
			case StdTypes.T_BYTE:
			case StdTypes.T_SHRT:
			case StdTypes.T_CHAR:
			case StdTypes.T_INT:
			case StdTypes.T_PTR:
				ins(I_MOVmemreg, dst, src);
				return;
			case StdTypes.T_LONG:
			case StdTypes.T_DPTR:
				ins(I_MOVmemreg, dst, src);
				if ((src = getReg(2, srcR, StdTypes.T_LONG, false)) == 0)
					return;
				ins(I_MOVmemreg, dst, src, 4);
				return;
		}
		fatalError(ERR_UNSTYPE_GENASSIGN);
	}
	
	public void genStoreVarVal(int objReg, Object loc, int off, int src, int type)
	{
		int srcR;
		if (type == StdTypes.T_FLT || type == StdTypes.T_DBL)
		{
			super.genStoreVarVal(objReg, loc, off, src, type);
			return;
		}
		int pos = mem.getAddrAsInt(loc, off);
		if ((srcR = getReg(1, src, type, false)) == 0)
			return;
		if (objReg == regBase && pos >= 0)
			pos += curVarOffParam;
		if (objReg != 0 && (objReg = getReg(1, objReg, StdTypes.T_PTR, false)) == 0)
			return;
		ins(I_MOVmemreg, objReg, srcR, pos);
		switch (type)
		{
			case StdTypes.T_LONG:
			case StdTypes.T_DPTR:
				if ((srcR = getReg(2, src, type, false)) == 0)
					return;
				ins(I_MOVmemreg, objReg, srcR, pos + 4);
		}
	}
	
	public void genStoreVarConstVal(int objReg, Object loc, int off, int val, int type)
	{
		int pos = mem.getAddrAsInt(loc, off);
		if (objReg == regBase && pos >= 0)
			pos += curVarOffParam;
		if (objReg != 0 && (objReg = getReg(1, objReg, StdTypes.T_PTR, false)) == 0)
			return;
		switch (type)
		{
			case StdTypes.T_BYTE:
			case StdTypes.T_BOOL:
				ins(I_MOVmemimm, objReg, 0, pos, val, 1);
				break;
			case StdTypes.T_SHRT:
			case StdTypes.T_CHAR:
				ins(I_MOVmemimm, objReg, 0, pos, val, 2);
				break;
			case StdTypes.T_INT:
			case StdTypes.T_FLT:
			case StdTypes.T_PTR:
				ins(I_MOVmemimm, objReg, 0, pos, val, 4);
				break;
			default:
				fatalError(ERR_INVMODE_GENSTOREVARX);
		}
	}
	
	public void genStoreVarConstDoubleOrLongVal(int objReg, Object loc, int off, long val, boolean asDouble)
	{
		genStoreVarConstVal(objReg, loc, off + 4, (int) (val >>> 32), StdTypes.T_INT);
		genStoreVarConstVal(objReg, loc, off, (int) val, StdTypes.T_INT);
	}
	
	private void longDeshuffle(int src1Hi, int src1Lo, int src2Hi, int src2Lo)
	{
		//move first operand to EDX:EAX and second to ECX:EBX
		//  with special case src2Hi==0, then use ECX as single second
		//there are only the registers EAX, EBX, ECX and EDX used, so shuffle them as wanted
		//this is brute force - xchg would be nicer, shorter, faster... ;-)
		if (src1Hi != R_EDX || src1Lo != R_EAX)
		{
			ins(I_PUSHreg, src1Hi);
			ins(I_PUSHreg, src1Lo);
		}
		if (src2Hi != 0)
		{ //normal case
			ins(I_PUSHreg, src2Hi);
			if (src2Lo != R_EBX)
			{
				ins(I_MOVregreg, R_EBX, src2Lo);
			}
			ins(I_POPreg, R_ECX);
		}
		else
		{ //special case without src2Hi and where src2Lo has to go to ECX for shift
			if (src2Lo != R_ECX)
				ins(I_MOVregreg, R_ECX, src2Lo);
		}
		if (src1Hi != R_EDX || src1Lo != R_EAX)
		{
			ins(I_POPreg, R_EAX);
			ins(I_POPreg, R_EDX);
		}
	}
	
	private void longMoveRes(int dstHi, int dstLo)
	{
		if (dstHi != R_EDX)
			ins(I_MOVregreg, dstHi, R_EDX); //hi can not be R_EAX, so move this one first
		if (dstLo != R_EAX)
			ins(I_MOVregreg, dstLo, R_EAX);
	}
	
	public void genBinOp(int dstR, int src1R, int src2R, int op, int type)
	{ //may destroy src2R
		Instruction dummy1, dummy2, dummy3, neg1, udiv, done;
		int opType = op >>> 16, opPar = op & 0xFFFF;
		int dst, src2, dstL, src2L, saveRegs, usedMask, tmp;
		boolean xchg;
		
		if (dstR != src1R || dstR == src2R)
		{
			fatalError(ERR_UNSCASE_GENBINOP);
			return;
		}
		if (type == StdTypes.T_FLT || type == StdTypes.T_DBL)
		{
			if (src2R != curFPUReg || dstR != curFPUReg - FPUREGINC)
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
		}
		//no floating point operation
		if ((dst = getReg(1, dstR, StdTypes.T_INT, false)) == 0 || (src2 = getReg(1, src2R, StdTypes.T_INT, false)) == 0)
			return;
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
								//else: dstsrc1==R_EAX && src2!=R_EAX, nothing needed
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
											ins(I_MOVregreg, R_EBX, src2);
											ins(I_MOVregreg, R_EAX, dst);
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
								ins(I_CDQ);
								ins(I_IDIVreg, src2);
								if (opPar == Ops.A_DIV)
								{
									if (dst != R_EAX)
										ins(I_MOVregreg, dst, R_EAX);
								}
								else
								{ //COps.A_MOD
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
								ins(I_SHLregreg, dst);
								break; //second regiser is fixed to cl
							case Ops.B_SHRL:
								ins(I_SHRregreg, getReg(1, dstR, type, false));
								break; //second regiser is fixed to cl, get real operand size
							case Ops.B_SHRA:
								ins(I_SARregreg, dst);
								break; //second regiser is fixed to cl
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
				if ((dstL = getReg(2, dstR, StdTypes.T_LONG, false)) == 0)
					return;
				switch (opType)
				{
					case Ops.S_ARI:
						if ((src2L = getReg(2, src2R, StdTypes.T_LONG, false)) == 0)
							return;
						switch (opPar)
						{
							case Ops.A_AND:
								ins(I_ANDregreg, dst, src2);
								ins(I_ANDregreg, dstL, src2L);
								return;
							case Ops.A_XOR:
								ins(I_XORregreg, dst, src2);
								ins(I_XORregreg, dstL, src2L);
								return;
							case Ops.A_OR:
								ins(I_ORregreg, dst, src2);
								ins(I_ORregreg, dstL, src2L);
								return;
							case Ops.A_PLUS:
								ins(I_ADDregreg, dst, src2);
								ins(I_ADCregreg, dstL, src2L);
								return;
							case Ops.A_MINUS:
								ins(I_SUBregreg, dst, src2);
								ins(I_SBBregreg, dstL, src2L);
								return;
							case Ops.A_MUL:
								longDeshuffle(dstL, dst, src2L, src2);
								ins(I_PUSHreg, R_EAX); //save lo1
								ins(I_PUSHreg, R_EDX); //save hi1
								ins(I_MULreg, R_ECX); //unsigned mul lo1*hi2
								ins(I_MOVregreg, R_ECX, R_EAX); //remember low value of result
								ins(I_POPreg, R_EAX); //restore hi1
								ins(I_MULreg, R_EBX); //unsigned mul hi1*lo2
								ins(I_ADDregreg, R_ECX, R_EAX); //add low result
								ins(I_POPreg, R_EAX); //restore lo1
								ins(I_MULreg, R_EBX); //unsigned mul lo1*lo2
								ins(I_ADDregreg, R_EDX, R_ECX); //add hi result
								longMoveRes(dstL, dst);
								return;
							case Ops.A_DIV: //found this strange code in the internet - didn't find a contra-example
							case Ops.A_MOD: //mostly the same code for div/mod
								nextJumpsUnsigned = false;
								longDeshuffle(dstL, dst, src2L, src2);
								ins(I_PUSHreg, R_ESI);
								ins(I_XORregreg, R_ESI, R_ESI);
								neg1 = getUnlinkedInstruction(); //will stay NOP, just as jump-destination
								udiv = getUnlinkedInstruction(); //will stay NOP, just as jump-destination
								ins(I_ORregreg, R_EDX, R_EDX);
								insJump(neg1, SC_US); //jump if op1 not signed
								ins(I_INCreg, R_ESI); //result has to be negged for mod, further check for div necessary
								ins(I_NEGreg, R_EDX);
								ins(I_NEGreg, R_EAX);
								ins(I_SBBregimm, R_EDX);
								appendInstruction(neg1);
								ins(I_ORregreg, R_ECX, R_ECX);
								insJump(udiv, SC_US); //jump if op2 not signed
								if (opPar == Ops.A_DIV)
									ins(I_XORregimm, R_ESI, 0, 0, 1); //neg has to be reversed for div
								ins(I_NEGreg, R_ECX);
								ins(I_NEGreg, R_EBX);
								ins(I_SBBregimm, R_ECX);
								appendInstruction(udiv); //here the real divide/modulo starts
								ins(I_TESTregreg, R_ECX, R_ECX);
								dummy1 = getUnlinkedInstruction(); //will stay NOP, just as jump-destination
								dummy2 = getUnlinkedInstruction(); //will stay NOP, just as jump-destination
								dummy3 = getUnlinkedInstruction(); //will stay NOP, just as jump-destination
								insJump(dummy2, Ops.C_NE);
								ins(I_CMPregreg, R_EDX, R_EBX);
								insJump(dummy1, Ops.C_BO);
								ins(I_MOVregreg, R_ECX, R_EAX);
								ins(I_MOVregreg, R_EAX, R_EDX);
								ins(I_XORregreg, R_EDX, R_EDX);
								ins(I_DIVreg, R_EBX);
								ins(I_XCHGregreg, R_EAX, R_ECX);
								appendInstruction(dummy1);
								ins(I_DIVreg, R_EBX);
								ins(I_MOVregreg, R_EBX, R_EDX);
								ins(I_MOVregreg, R_EDX, R_ECX);
								ins(I_XORregreg, R_ECX, R_ECX);
								insJump(dummy3, 0);
								appendInstruction(dummy2);
								ins(I_PUSHreg, R_ESI);
								ins(I_PUSHreg, R_EDI);
								ins(I_PUSHreg, R_EDX);
								ins(I_PUSHreg, R_EAX);
								ins(I_MOVregreg, R_ESI, R_EBX);
								ins(I_MOVregreg, R_EDI, R_ECX);
								ins(I_SHRregimm, R_EDX, 0, 0, 1);
								ins(I_RCRregimm, R_EAX, 0, 0, 1);
								ins(I_RORregimm, R_EDI, 0, 0, 1);
								ins(I_RCRregimm, R_EBX, 0, 0, 1);
								ins(I_BSRregreg, R_ECX, R_ECX);
								ins(I_SHRDregreg, R_EBX, R_EDI);
								ins(I_SHRDregreg, R_EAX, R_EDX);
								ins(I_SHRregreg, R_EDX); //second register is fixed to CL
								ins(I_ROLregimm, R_EDI, 0, 0, 1);
								ins(I_DIVreg, R_EBX);
								ins(I_POPreg, R_EBX);
								ins(I_MOVregreg, R_ECX, R_EAX);
								ins(I_IMULregreg, R_EDI, R_EAX);
								ins(I_MULreg, R_ESI);
								ins(I_ADDregreg, R_EDX, R_EDI);
								ins(I_SUBregreg, R_EBX, R_EAX);
								ins(I_MOVregreg, R_EAX, R_ECX);
								ins(I_POPreg, R_ECX);
								ins(I_SBBregreg, R_ECX, R_EDX);
								ins(I_SBBregreg, R_EDX, R_EDX);
								if (opPar == Ops.A_MOD)
								{
									ins(I_ANDregreg, R_ESI, R_EDX);
									ins(I_ANDregreg, R_EDI, R_EDX);
									ins(I_ADDregreg, R_EBX, R_ESI);
									ins(I_ADCregreg, R_ECX, R_EDI);
								}
								else
								{
									ins(I_ADDregreg, R_EAX, R_EDX);
									ins(I_XORregreg, R_EDX, R_EDX);
								}
								ins(I_POPreg, R_EDI);
								ins(I_POPreg, R_ESI);
								appendInstruction(dummy3);
								if (opPar == Ops.A_MOD)
								{
									ins(I_MOVregreg, R_EAX, R_EBX);
									ins(I_MOVregreg, R_EDX, R_ECX);
								}
								ins(I_ORregreg, R_ESI, R_ESI);
								done = getUnlinkedInstruction(); //will stay NOP, just as jump-destination
								insJump(done, Ops.C_EQ);
								ins(I_NEGreg, R_EDX);
								ins(I_NEGreg, R_EAX);
								ins(I_SBBregimm, R_EDX);
								appendInstruction(done);
								ins(I_POPreg, R_ESI);
								longMoveRes(dstL, dst);
								return;
						}
						fatalError(ERR_UNSOP_GENBINOP);
						return;
					case Ops.S_BSH:
						usedMask = dstR | src2R;
						saveRegs = storeReg(rAll & ~usedMask);
						switch (opPar)
						{
							case Ops.B_SHL:
								nextJumpsUnsigned = false;
								longDeshuffle(dstL, dst, 0, src2);
								dummy1 = getUnlinkedInstruction(); //will stay NOP, just as jump-destination
								dummy2 = getUnlinkedInstruction(); //will stay NOP, just as jump-destination
								dummy3 = getUnlinkedInstruction(); //will stay NOP, just as jump-destination
								ins(I_CMPregimm, R_CL, 0, 0, 0x20); //shld only works with cl<32
								insJump(dummy2, Ops.C_EQ); //exact 32 shifts
								insJump(dummy1, SC_AE); //if more than 31 shifts
								ins(I_SHLDregreg, R_EDX, R_EAX); //third register is fixed to cl
								ins(I_SHLregreg, R_EAX); //second regiser is fixed to cl
								insJump(dummy3, 0);
								appendInstruction(dummy1);
								ins(I_MOVregreg, R_EDX, R_EAX);
								ins(I_XORregreg, R_EAX, R_EAX);
								ins(I_SUBregimm, R_CL, 0, 0, 0x20);
								ins(I_SHLregreg, R_EDX); //second register is fixed to cl
								insJump(dummy3, 0);
								appendInstruction(dummy2);
								ins(I_MOVregreg, R_EDX, R_EAX);
								ins(I_XORregreg, R_EAX, R_EAX);
								appendInstruction(dummy3);
								longMoveRes(dstL, dst);
								restoreReg(saveRegs);
								return;
							case Ops.B_SHRL:
								nextJumpsUnsigned = false;
								longDeshuffle(dstL, dst, 0, src2);
								dummy1 = getUnlinkedInstruction(); //will stay NOP, just as jump-destination
								dummy2 = getUnlinkedInstruction(); //will stay NOP, just as jump-destination
								dummy3 = getUnlinkedInstruction(); //will stay NOP, just as jump-destination
								ins(I_CMPregimm, R_CL, 0, 0, 0x20); //shld only works with cl<32
								insJump(dummy2, Ops.C_EQ); //exact 32 shifts
								insJump(dummy1, SC_AE); //if more than 31 shifts
								ins(I_SHRDregreg, R_EAX, R_EDX); //third register is fixed to cl
								ins(I_SHRregreg, R_EDX); //second register is fixed to cl
								insJump(dummy3, 0);
								appendInstruction(dummy1);
								ins(I_MOVregreg, R_EAX, R_EDX);
								ins(I_XORregreg, R_EDX, R_EDX);
								ins(I_SUBregimm, R_CL, 0, 0, 0x20);
								ins(I_SHRregreg, R_EAX); //second register is fixed to cl
								insJump(dummy3, 0);
								appendInstruction(dummy2);
								ins(I_MOVregreg, R_EAX, R_EDX);
								ins(I_XORregreg, R_EDX, R_EDX);
								appendInstruction(dummy3);
								longMoveRes(dstL, dst);
								restoreReg(saveRegs);
								return;
							case Ops.B_SHRA:
								nextJumpsUnsigned = false;
								longDeshuffle(dstL, dst, 0, src2);
								dummy1 = getUnlinkedInstruction(); //will stay NOP, just as jump-destination
								dummy2 = getUnlinkedInstruction(); //will stay NOP, just as jump-destination
								dummy3 = getUnlinkedInstruction(); //will stay NOP, just as jump-destination
								ins(I_CMPregimm, R_CL, 0, 0, 0x20); //shld only works with cl<32
								insJump(dummy2, Ops.C_EQ); //exact 32 shifts
								insJump(dummy1, SC_AE); //if more than 31 shifts
								ins(I_SHRDregreg, R_EAX, R_EDX); //third register is fixed to cl
								ins(I_SARregreg, R_EDX); //second register is fixed to cl
								insJump(dummy3, 0);
								appendInstruction(dummy1);
								ins(I_MOVregreg, R_EAX, R_EDX);
								ins(I_SARregimm, R_EDX, 0, 0, 31);
								ins(I_SUBregimm, R_CL, 0, 0, 0x20);
								ins(I_SARregreg, R_EAX); //second register is fixed to cl
								insJump(dummy3, 0);
								appendInstruction(dummy2);
								ins(I_MOVregreg, R_EAX, R_EDX);
								ins(I_SARregimm, R_EDX, 0, 0, 31);
								appendInstruction(dummy3);
								longMoveRes(dstL, dst);
								restoreReg(saveRegs);
								return;
						}
						fatalError(ERR_UNSOP_GENBINOP);
						return;
				}
				fatalError(ERR_UNSOP_GENBINOP);
				return;
		}
		fatalError(ERR_UNSTYPE_GENBINOP);
	}
	
	public void genBinOpConstRi(int dstR, int src1R, int val, int op, int type)
	{
		int opType = op >>> 16, opPar = op & 0xFFFF, dst, dstL, tmp;
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
								break; //use real operand size
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
			case StdTypes.T_LONG:
				switch (opType)
				{
					case Ops.S_BSH:
						if (val != 32)
							break; //optimize only if a complete register may be shifted
						if ((dst = getReg(1, dstR, StdTypes.T_LONG, false)) == 0 || (dstL = getReg(2, dstR, StdTypes.T_LONG, false)) == 0)
							return;
						switch (opPar)
						{
							case Ops.B_SHL:
								ins(I_MOVregreg, dstL, dst);
								ins(I_XORregreg, dst, dst);
								return;
							case Ops.B_SHRL:
								ins(I_MOVregreg, dst, dstL);
								ins(I_XORregreg, dstL, dstL);
								return;
						}
				}
		}
		super.genBinOpConstRi(dstR, src1R, val, op, type);
	}
	
	public void genUnaOp(int dstR, int srcR, int op, int type)
	{
		int dst, dst2;
		if (dstR != srcR)
		{
			fatalError(ERR_UNSCASE_GENUNAOP);
			return;
		}
		if (type == StdTypes.T_FLT || type == StdTypes.T_DBL)
		{
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
		}
		else
		{
			if (type != StdTypes.T_LONG)
			{
				genDefaultUnaOp(dstR, srcR, op, type != StdTypes.T_BOOL ? type = StdTypes.T_INT : type);
				return;
			}
			if ((dst = getReg(1, dstR, StdTypes.T_LONG, false)) == 0)
				return;
			if ((dst2 = getReg(2, dstR, StdTypes.T_LONG, false)) == 0)
				return;
			switch (op & 0xFFFF)
			{
				case Ops.A_CPL:
					ins(I_NOTreg, dst);
					ins(I_NOTreg, dst2);
					return;
				case Ops.A_MINUS:
					ins(I_NEGreg, dst2);
					ins(I_NEGreg, dst);
					ins(I_SBBregimm, dst2);
					return;
				case Ops.A_PLUS:
					/*nothing to do*/
					return;
			}
		}
		fatalError(ERR_UNSOP_GENUNAOP);
		return;
	}
	
	public void genIncMem(int dst, int type)
	{
		Instruction after;
		if ((dst = getReg(1, dst, StdTypes.T_PTR, false)) == 0)
			return;
		switch (type)
		{
			case StdTypes.T_BYTE:
				ins(I_INCmem, dst, 0, 0, 0, 1);
				return;
			case StdTypes.T_SHRT:
			case StdTypes.T_CHAR:
				ins(I_INCmem, dst, 0, 0, 0, 2);
				return;
			case StdTypes.T_INT:
				ins(I_INCmem, dst, 0, 0, 0, 4);
				return;
			case StdTypes.T_LONG:
				ins(I_ADDmemimm, dst, 0, 0, 1, 4);
				after = getUnlinkedInstruction(); //will stay NOP, just as jump-destination
				nextJumpsUnsigned = false;
				insJump(after, SC_AE); //do not execute the inc on the upper dword, if no carry
				ins(I_INCmem, dst, 0, 4, 0, 4); //handle overflow with inc of upper dword
				appendInstruction(after);
				return;
		}
		fatalError(ERR_UNSTYPE_GENINCDECMEM);
	}
	
	public void genDecMem(int dst, int type)
	{
		Instruction after;
		if ((dst = getReg(1, dst, StdTypes.T_PTR, false)) == 0)
			return;
		switch (type)
		{
			case StdTypes.T_BYTE:
				ins(I_DECmem, dst, 0, 0, 0, 1);
				return;
			case StdTypes.T_SHRT:
			case StdTypes.T_CHAR:
				ins(I_DECmem, dst, 0, 0, 0, 2);
				return;
			case StdTypes.T_INT:
				ins(I_DECmem, dst, 0, 0, 0, 4);
				return;
			case StdTypes.T_LONG:
				ins(I_SUBmemimm, dst, 0, 0, 1, 4);
				after = getUnlinkedInstruction(); //will stay NOP, just as jump-destination
				nextJumpsUnsigned = false;
				insJump(after, SC_AE); //do not execute the dec on the upper dword, if no overflow
				ins(I_DECmem, dst, 0, 4, 0, 4); //handle overflow with dec of upper dword
				appendInstruction(after);
				return;
		}
		fatalError(ERR_UNSTYPE_GENINCDECMEM);
	}
	
	public void genLoadConstUnitContext(int dst, Object unitLoc)
	{
		if ((dst = getReg(1, dst, StdTypes.T_PTR, true)) == 0)
			return;
		ins(I_MOVregimm, dst, 0, 0, mem.getAddrAsInt(unitLoc, 0));
	}
	
	public void genCall(int off, int clssReg, int parSize)
	{
		if ((clssReg = getReg(1, clssReg, StdTypes.T_PTR, false)) == 0)
			return;
		if (ctx.codeStart == 0)
			ins(I_CALLmem, clssReg, 0, off, 0, parSize);
		else
		{
			ins(I_MOVregmem, R_EAX, clssReg, off);
			ins(I_LEAregmem, R_EAX, R_EAX, ctx.codeStart);
			ins(I_CALLreg, R_EAX, 0, 0, 0, parSize);
		}
	}
	
	public void genCallIndexed(int intfReg, int off, int parSize)
	{
		if ((intfReg = getReg(2, intfReg, StdTypes.T_DPTR, false)) == 0)
			return;
		ins(I_MOVregmem, R_EAX, intfReg, off);
		ins(I_MOVindexed, R_EAX, R_EDI); //mov eax,[edi+eax]
		if (ctx.codeStart != 0)
			ins(I_LEAregmem, R_EAX, R_EAX, ctx.codeStart);
		ins(I_CALLreg, R_EAX, 0, 0, 0, parSize);
	}
	
	public void genCallConst(Mthd mthd, int parSize)
	{
		insPatchedCall(mthd, parSize);
	}
	
	public int genComp(int src1R, int src2R, int type, int cond)
	{
		Instruction dummy;
		int src1, src2, src1L, src2L;
		if (type == StdTypes.T_FLT || type == StdTypes.T_DBL)
		{
			if (src2R != curFPUReg || src1R != curFPUReg - FPUREGINC)
			{
				errorFPU();
				return 0;
			}
			dupFPUIns = null;
			popFPUDone++;
			ins(I_FXCH);
			if (useOnly487)
			{
				popFPUDone++; //will pop both register
				if ((usedRegs & RegA) != 0)
					ins(I_PUSHreg, R_EAX);
				ins(I_FCOMPP);
				ins(I_FSTSW);
				ins(I_FWAIT);
				ins(I_SAHF);
				if ((usedRegs & RegA) != 0)
					ins(I_POPreg, R_EAX);
			}
			else
				ins(I_FCOMIP);
			nextJumpsUnsigned = true;
			return cond;
		}
		nextJumpsUnsigned = false;
		if ((src1 = getReg(1, src1R, StdTypes.T_INT, false)) == 0 || (src2 = getReg(1, src2R, StdTypes.T_INT, false)) == 0)
			return 0;
		switch (type)
		{
			case StdTypes.T_BOOL:
				ins(I_CMPregreg, src1 & ~(RS_E & ~RS_L), src2 & ~(RS_E & ~RS_L));
				return cond;
			case StdTypes.T_BYTE:
			case StdTypes.T_SHRT:
			case StdTypes.T_CHAR:
			case StdTypes.T_INT:
			case StdTypes.T_PTR:
				ins(I_CMPregreg, src1, src2);
				return cond;
			case StdTypes.T_LONG:
				if ((src1L = getReg(2, src1R, StdTypes.T_LONG, false)) == 0 || (src2L = getReg(2, src2R, StdTypes.T_LONG, false)) == 0)
					return 0;
				ins(I_CMPregreg, src1L, src2L); //compare bits 63..32
				dummy = getUnlinkedInstruction(); //will stay NOP, just as jump-destination
				insJump(dummy, Ops.C_NE);
				ins(I_PUSHreg, src1);
				ins(I_PUSHreg, src2);
				ins(I_SHRregimm, src1, 0, 0, 1);
				ins(I_SHRregimm, src2, 0, 0, 1);
				ins(I_CMPregreg, src1, src2); //unsigned compare of bits 31..1
				ins(I_POPreg, src2);
				ins(I_POPreg, src1);
				insJump(dummy, Ops.C_NE);
				ins(I_CMPregreg, src1, src2); //compare of bits 1
				appendInstruction(dummy);
				return cond;
		}
		fatalError(ERR_UNSTYPE_GENCOMP);
		return 0;
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
		switch (type)
		{
			case StdTypes.T_BOOL:
				src &= ~(RS_E & ~RS_L);
				break;
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
		if (val == 0)
		{
			ins(I_ORregreg, src, src);
			return cond;
		}
		ins(I_CMPregimm, src, 0, 0, val);
		return cond;
	}
	
	public int genCompValToConstDoubleOrLongVal(int src, long val, boolean asDouble, int cond)
	{
		Instruction dummy;
		int reg1, reg2, src2, restore;
		if (asDouble)
		{ //no direct compare available
			restore = prepareFreeReg(src, 0, 0, StdTypes.T_DBL);
			src2 = allocReg();
			genLoadConstDoubleOrLongVal(src2, val, true);
			cond = genComp(src, src2, StdTypes.T_DBL, cond);
			deallocRestoreReg(src2, 0, restore);
			return cond;
		}
		nextJumpsUnsigned = false;
		if ((reg1 = getReg(1, src, StdTypes.T_LONG, false)) == 0 || (reg2 = getReg(2, src, StdTypes.T_LONG, false)) == 0)
			return 0;
		ins(I_CMPregimm, reg2, 0, 0, (int) (val >>> 32));
		dummy = getUnlinkedInstruction(); //will stay NOP, just as jump-destination
		insJump(dummy, Ops.C_NE);
		ins(I_PUSHreg, reg1);
		ins(I_SHRregimm, reg1, 0, 0, 1);
		ins(I_CMPregimm, reg1, 0, 0, (int) (val >>> 1) & 0x7FFFFFFF); //unsigned compare of bits 31..1
		ins(I_POPreg, reg1);
		insJump(dummy, Ops.C_NE);
		ins(I_CMPregimm, reg1, 0, 0, (int) val); //compare of bits 1
		appendInstruction(dummy);
		return cond;
	}
	
	public void genLoadDerefAddr(int destReg, int objReg, int ind, int baseOffset, int entrySize)
	{
		int tmp, tmpRst, indReg;
		if ((destReg = getReg(1, destReg, StdTypes.T_PTR, true)) == 0 || (objReg = getReg(1, objReg, StdTypes.T_PTR, false)) == 0 || (indReg = getReg(1, ind, StdTypes.T_INT, false)) == 0)
			return;
		if (entrySize < 0)
		{
			ins(I_NEGreg, indReg);
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
				ins(I_LEAarray, destReg, indReg, baseOffset, 0, entrySize);
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
					ins(I_ADDregimm, destReg, 0, 0, baseOffset);
				ins(I_ADDregreg, destReg, indReg);
		}
	}
	
	public void genMoveToPrimary(int srcR, int type)
	{
		int reg;
		if (type == StdTypes.T_FLT || type == StdTypes.T_DBL)
		{
			if (srcR != curFPUReg)
				errorFPU();
			dupFPUIns = null;
			popFPUDone++; //do not destroy result
			return; //leave result on top of stack
		}
		if ((reg = getReg(1, srcR, StdTypes.T_INT, false)) == 0)
			return;
		if (reg != R_EAX)
			ins(I_MOVregreg, R_EAX, reg);
		if (type == StdTypes.T_LONG || type == StdTypes.T_DPTR)
		{
			if ((reg = getReg(2, srcR, StdTypes.T_LONG, false)) == 0)
				return;
			if (reg != R_EDX)
				ins(I_MOVregreg, R_EDX, reg);
		}
	}
	
	public void genMoveFromPrimary(int dstR, int type)
	{
		int reg;
		if (type == StdTypes.T_FLT || type == StdTypes.T_DBL)
		{
			if (dstR != curFPUReg)
				errorFPU();
			return; //result already on top of stack
		}
		if (type == StdTypes.T_LONG || type == StdTypes.T_DPTR)
		{
			if ((reg = getReg(2, dstR, StdTypes.T_LONG, true)) == 0)
				return;
			if (reg != R_EDX)
				ins(I_MOVregreg, reg, R_EDX);
		}
		if ((reg = getReg(1, dstR, StdTypes.T_INT, true)) == 0)
			return;
		if (reg != R_EAX)
			ins(I_MOVregreg, reg, R_EAX);
	}
	
	public void genMoveIntfMapFromPrimary(int dst)
	{
		if ((dst = getReg(2, dst, StdTypes.T_DPTR, true)) == 0)
			return;
		ins(I_MOVregreg, dst, R_EAX); //reg can not be EAX, because it is the second one in dst
	}
	
	public void genSavePrimary(int type)
	{
		if (type == StdTypes.T_FLT || type == StdTypes.T_DBL)
		{
			return; //result already on top of FPU-stack
		}
		ins(I_PUSHreg, R_EAX);
		if (type == StdTypes.T_LONG || type == StdTypes.T_DPTR)
			ins(I_PUSHreg, R_EDX);
	}
	
	public void genRestPrimary(int type)
	{
		if (type == StdTypes.T_FLT || type == StdTypes.T_DBL)
		{
			return; //result already on top of FPU-stack
		}
		if (type == StdTypes.T_LONG || type == StdTypes.T_DPTR)
			ins(I_POPreg, R_EDX);
		ins(I_POPreg, R_EAX);
	}
	
	public void genCallNative(Object loc, int off, boolean relative, int parSize, boolean noCleanUp)
	{
		if (relative)
		{
			ins(I_CALLmem, R_EDI, 0, off, 0, parSize);
		}
		else
			ins(I_CALLmem, 0, 0, mem.getAddrAsInt(loc, off), 0, parSize);
		if (!noCleanUp && parSize > 0)
			ins(I_ADDregimm, R_ESP, 0, 0, parSize);
	}
	
	public void genReserveNativeStack(int size)
	{
		if (size > 0)
			ins(I_SUBregimm, R_ESP, 0, 0, size);
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
			ins(I_FSTPmem, R_ESP, 0, offset, 0, type == StdTypes.T_DBL ? FPU64 : FPU32);
			if (useOnly487)
				ins(I_FWAIT);
			return;
		}
		if ((reg = getReg(1, src, StdTypes.T_INT, false)) == 0)
			return;
		ins(I_MOVmemreg, R_ESP, reg, offset);
		if (type == StdTypes.T_LONG || type == StdTypes.T_DPTR)
		{
			if ((reg = getReg(2, src, StdTypes.T_LONG, false)) == 0)
				return;
			ins(I_MOVmemreg, R_ESP, reg, offset + 4);
		}
	}
	
	//here the internal coding of the instructions takes place
	protected Instruction ins(int type, int reg0, int reg1, int disp, int imm)
	{
		return ins(type, reg0, reg1, disp, imm, 0);
	}
	
	protected Instruction ins(int type, int reg0, int reg1, int disp, int imm, int par)
	{
		Instruction i;
		
		//get a new instruction and insert it
		i = getUnlinkedInstruction();
		i.type = type;
		appendInstruction(i);
		code(i, type, reg0, reg1, disp, imm, par);
		return i;
	}
	
	protected void internalFixStackExtremeAdd(Instruction me, int stackCells)
	{
		me.size = 0;
		code(me, me.type = I_ADDregimm, me.reg0, 0, 0, stackCells << 2, 0);
	}
	
	protected void code(Instruction i, int type, int reg0, int reg1, int disp, int imm, int par)
	{
		int tmp, wordflag = 0;
		boolean sizeprefix = false;
		
		//if instruction has a refObj, it is already coded
		if (i.refMthd != null)
			return;
		//get parameters and remember them
		if (type <= 0)
			return; //not a "real" instruction that has to be coded
		i.reg0 = reg0;
		i.reg1 = reg1;
		i.iPar1 = disp;
		i.iPar2 = imm;
		i.iPar3 = par;
		//check wordflag and sizeprefix for most instructions (check only, do not code)
		if ((type & IM_P0) == I_reg0 || (type & IM_P1) == I_reg1)
		{
			if ((type & IM_P0) == I_reg0)
				tmp = reg0 & 0x0F; //register 0 gives operation size (exception: MOVSXregmem, ADDmemimm, SUBmemimm, INC, DEC)
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
				if (sizeprefix)
					i.putByte(0x66);
				i.putByte(0x8A | wordflag);
				putRegReg(i, reg0, reg1);
				return;
			case I_MOVregmem:
				if (sizeprefix)
					i.putByte(0x66);
				if (reg1 == 0 && (reg0 == R_EAX || reg0 == R_AX || reg0 == R_AL))
				{
					i.putByte(0xA0 | wordflag);
					i.putInt(disp);
				}
				else
				{
					i.putByte(0x8A | wordflag);
					putMem(i, reg0, reg1, disp);
				}
				return;
			case I_MOVmemreg:
				if (sizeprefix)
					i.putByte(0x66);
				if (reg0 == 0 && (reg1 == R_EAX || reg1 == R_AX || reg1 == R_AL))
				{
					i.putByte(0xA2 | wordflag);
					i.putInt(disp);
				}
				else
				{
					i.putByte(0x88 | wordflag);
					putMem(i, reg1, reg0, disp); //toggle reg0/reg1 as needed for memreg-ops
				}
				return;
			case I_MOVregimm:
				if (sizeprefix)
					i.putByte(0x66);
				i.putByte(0xB0 | (wordflag << 3) | (reg0 >>> 4));
				putImm(i, reg0, imm);
				return;
			case I_MOVmemimm:
				if (par == 2)
					i.putByte(0x66);
				i.putByte(0xC6 | (par == 1 ? 0 : 1));
				putMem(i, 0, reg0, disp);
				if (par == 4)
					i.putInt(imm);
				else if (par == 2)
					i.putShort(imm);
				else
					i.putByte(imm);
				return;
			case I_MOVSXregreg:
				if ((reg0 & 0x0F) == RS_X)
					i.putByte(0x66); //to 16 bit instead of 32
				i.putByte(0x0F);
				if ((reg1 & 0x0F) == RS_X)
					i.putByte(0xBF); //from AX, BX, ...
				else
					i.putByte(0xBE); //from AL, BL, ...
				putRegReg(i, reg0, reg1);
				return;
			case I_MOVSXregmem:
				if ((reg0 & 0x0F) == RS_X)
					i.putByte(0x66); //to 16 bit instead of 32
				i.putByte(0x0F);
				if (par == 2)
					i.putByte(0xBF); //from 16 bit source
				else
					i.putByte(0xBE); //from 8 bit source
				putMem(i, reg0, reg1, disp);
				return;
			case I_MOVZXregreg:
				if ((reg0 & 0x0F) == RS_X)
					i.putByte(0x66); //to 16 bit instead of 32
				i.putByte(0x0F);
				if ((reg1 & 0x0F) == RS_X)
					i.putByte(0xB7); //from AX, BX, ...
				else
					i.putByte(0xB6); //from AL, BL, ...
				putRegReg(i, reg0, reg1);
				return;
			case I_MOVZXregmem:
				if ((reg0 & 0x0F) == RS_X)
					i.putByte(0x66); //to 16 bit instead of 32
				i.putByte(0x0F);
				if (par == 2)
					i.putByte(0xB7); //from 16 bit source
				else
					i.putByte(0xB6); //from 8 bit source
				putMem(i, reg0, reg1, disp);
				return;
			case I_LEAregmem:
				i.putByte(0x8D);
				putMem(i, reg0, reg1, disp);
				return;
			case I_PUSHreg:
				//no prefix: only 32 bit supported
				i.putByte(0x50 | (reg0 >>> 4));
				return;
			case I_PUSHmem:
				i.putByte(0xFF);
				putMem(i, 0x60, reg0, disp);
				return;
			case I_PUSHimm:
				if (imm >= -128 && imm < 128)
				{ //8 bits immediate
					i.putByte(0x6A);
					i.putByte(imm);
				}
				else
				{ //32 bit immediate
					i.putByte(0x68);
					i.putInt(imm);
				}
				return;
			case I_POPdummy:
				//pop ecx as this register is never used as return value
				i.putByte(0x58 | (R_ECX >>> 4));
				return;
			case I_POPreg:
				//no prefix: only 32 bit supported
				i.putByte(0x58 | (reg0 >>> 4));
				return;
			case I_PUSHA:
				i.putByte(0x60);
				return;
			case I_POPA:
				i.putByte(0x61);
				return;
			case I_CALLreg:
				i.putByte(0xFF);
				i.putByte(0xD0 | (reg0 >>> 4));
				return;
			case I_CALLmem:
				i.putByte(0xFF);
				putMem(i, 0x20, reg0, disp);
				return;
			case I_PUSHip:
				i.putByte(0xE8);
				i.putInt(0);
				return;
			case I_RETimm:
				if (imm == 0)
					i.putByte(0xC3);
				else
				{
					i.putByte(0xC2);
					i.putShort(imm);
				}
				return;
			case I_IRET:
				i.putByte(0xCF);
				return;
			case I_STEXreg:
			case I_ADDregimm:
				codeAriRegImm(i, 0x04, sizeprefix, wordflag, reg0, imm);
				return;
			case I_ADDregreg:
				if (sizeprefix)
					i.putByte(0x66);
				i.putByte(0x02 | wordflag);
				putRegReg(i, reg0, reg1);
				return;
			case I_ADDmemimm:
				codeAriMemImm(i, 0x00, reg0, disp, imm, par);
				return;
			case I_ADDregmem:
				if (sizeprefix)
					i.putByte(0x66);
				i.putByte(0x02 | wordflag);
				putMem(i, reg0, reg1, disp);
				return;
			case I_ADCregreg:
				if (sizeprefix)
					i.putByte(0x66);
				i.putByte(0x12 | wordflag);
				putRegReg(i, reg0, reg1);
				return;
			case I_SUBregreg:
				if (sizeprefix)
					i.putByte(0x66);
				i.putByte(0x2A | wordflag);
				putRegReg(i, reg0, reg1);
				return;
			case I_SUBregimm:
				codeAriRegImm(i, 0x2C, sizeprefix, wordflag, reg0, imm);
				return;
			case I_SUBmemimm:
				codeAriMemImm(i, 0x50, reg0, disp, imm, par);
				return;
			case I_SUBregmem:
				if (sizeprefix)
					i.putByte(0x66);
				i.putByte(0x2A | wordflag);
				putMem(i, reg0, reg1, disp);
				return;
			case I_SBBregreg:
				if (sizeprefix)
					i.putByte(0x66);
				i.putByte(0x1A | wordflag);
				putRegReg(i, reg0, reg1);
				return;
			case I_SBBregimm:
				codeAriRegImm(i, 0x1C, sizeprefix, wordflag, reg0, imm);
				return;
			case I_ANDregreg:
				if (sizeprefix)
					i.putByte(0x66);
				i.putByte(0x22 | wordflag);
				putRegReg(i, reg0, reg1);
				return;
			case I_ANDregimm:
				codeAriRegImm(i, 0x24, sizeprefix, wordflag, reg0, imm);
				return;
			case I_ANDmemimm:
				codeAriMemImm(i, 0x48, reg0, disp, imm, par);
				return;
			case I_ANDregmem:
				if (sizeprefix)
					i.putByte(0x66);
				i.putByte(0x22 | wordflag);
				putMem(i, reg0, reg1, disp);
				return;
			case I_XORregreg:
				if (sizeprefix)
					i.putByte(0x66);
				i.putByte(0x32 | wordflag);
				putRegReg(i, reg0, reg1);
				return;
			case I_XORregimm:
				codeAriRegImm(i, 0x34, sizeprefix, wordflag, reg0, imm);
				return;
			case I_XORmemimm:
				codeAriMemImm(i, 0x60, reg0, disp, imm, par);
				return;
			case I_XORregmem:
				if (sizeprefix)
					i.putByte(0x66);
				i.putByte(0x32 | wordflag);
				putMem(i, reg0, reg1, disp);
				return;
			case I_ORregreg:
				if (sizeprefix)
					i.putByte(0x66);
				i.putByte(0x0A | wordflag);
				putRegReg(i, reg0, reg1);
				return;
			case I_ORregimm:
				codeAriRegImm(i, 0x0C, sizeprefix, wordflag, reg0, imm);
				return;
			case I_ORmemimm:
				codeAriMemImm(i, 0x18, reg0, disp, imm, par);
				return;
			case I_ORregmem:
				if (sizeprefix)
					i.putByte(0x66);
				i.putByte(0x0A | wordflag);
				putMem(i, reg0, reg1, disp);
				return;
			case I_TESTregreg:
				if (sizeprefix)
					i.putByte(0x66);
				i.putByte(0x84 | wordflag);
				putRegReg(i, reg0, reg1);
				return;
			case I_MULreg:
				if (sizeprefix)
					i.putByte(0x66);
				i.putByte(0xF6 | wordflag);
				i.putByte(0xE0 | (reg0 >>> 4));
				return;
			case I_IMULreg:
				if (sizeprefix)
					i.putByte(0x66);
				i.putByte(0xF6 | wordflag);
				i.putByte(0xE8 | (reg0 >>> 4));
				return;
			case I_IMULregreg:
				if (sizeprefix)
					i.putByte(0x66);
				i.putByte(0x0F);
				i.putByte(0xAF);
				putRegReg(i, reg0, reg1);
				return;
			case I_DIVreg:
				if (sizeprefix)
					i.putByte(0x66);
				i.putByte(0xF6 | wordflag);
				i.putByte(0xF0 | (reg0 >>> 4));
				return;
			case I_IDIVreg:
				if (sizeprefix)
					i.putByte(0x66);
				i.putByte(0xF6 | wordflag);
				i.putByte(0xF8 | (reg0 >>> 4));
				return;
			case I_INCreg:
				if (sizeprefix)
					i.putByte(0x66);
				i.putByte(0x40 | (reg0 >>> 4));
				return;
			case I_INCmem:
				if (par > 1)
				{
					wordflag = 1;
					if (par == 2)
						i.putByte(0x66);
				}
				else
					wordflag = 0;
				i.putByte(0xFE | wordflag);
				putMem(i, 0x00, reg0, disp); //"inc reg0" is like "dummy 0, reg0"
				return;
			case I_DECreg:
				if (sizeprefix)
					i.putByte(0x66);
				i.putByte(0x48 | (reg0 >>> 4));
				return;
			case I_DECmem:
				if (par > 1)
				{
					wordflag = 1;
					if (par == 2)
						i.putByte(0x66);
				}
				else
					wordflag = 0;
				i.putByte(0xFE | wordflag);
				putMem(i, 0x10, reg0, disp); //"dec reg0" is like "dummy 1, reg0"
				return;
			case I_NEGreg:
				if (sizeprefix)
					i.putByte(0x66);
				i.putByte(0xF6 | wordflag);
				i.putByte(0xD8 | (reg0 >>> 4));
				return;
			case I_NOTreg:
				if (sizeprefix)
					i.putByte(0x66);
				i.putByte(0xF6 | wordflag);
				i.putByte(0xD0 | (reg0 >>> 4));
				return;
			case I_CMPregimm:
				codeAriRegImm(i, 0x3C, sizeprefix, wordflag, reg0, imm);
				return;
			case I_CMPregreg:
				if (sizeprefix)
					i.putByte(0x66);
				i.putByte(0x3A | wordflag);
				putRegReg(i, reg0, reg1);
				return;
			case I_CMPregmem:
				if (sizeprefix)
					i.putByte(0x66);
				i.putByte(0x3A | wordflag);
				putMem(i, reg0, reg1, disp);
				return;
			case I_CMPmemimm:
				codeAriMemImm(i, 0x70, reg0, disp, imm, par);
				return;
			case I_XCHGregreg:
				if (sizeprefix)
					i.putByte(0x66);
				if (reg0 == R_EAX || reg0 == R_AX)
				{
					i.putByte(0x90 | (reg1 >>> 4));
				}
				else if (reg1 == R_EAX || reg1 == R_AX)
				{
					i.putByte(0x90 | (reg0 >>> 4));
				}
				else
				{
					i.putByte(0x86 | wordflag);
					putRegReg(i, reg0, reg1);
				}
				return;
			case I_SHLregreg:
				if (sizeprefix)
					i.putByte(0x66);
				i.putByte(0xD2 | wordflag);
				i.putByte(0xE0 | (reg0 >>> 4));
				return;
			case I_SHLregimm:
				codeShiftRegImm(i, 0xE0, sizeprefix, wordflag, reg0, imm);
				return;
			case I_SHLmemimm:
				codeShiftMemImm(i, 0x40, reg0, disp, imm, par);
				return;
			case I_SHRregreg:
				if (sizeprefix)
					i.putByte(0x66);
				i.putByte(0xD2 | wordflag);
				i.putByte(0xE8 | (reg0 >>> 4));
				return;
			case I_SHRregimm:
				codeShiftRegImm(i, 0xE8, sizeprefix, wordflag, reg0, imm);
				return;
			case I_SHRmemimm:
				codeShiftMemImm(i, 0x50, reg0, disp, imm, par);
				return;
			case I_SHLDregreg:
				if (sizeprefix)
					i.putByte(0x66);
				i.putByte(0x0F);
				i.putByte(0xA5);
				putRegReg(i, reg1, reg0); //shld has exchanged order of operands
				return;
			case I_SHRDregreg:
				if (sizeprefix)
					i.putByte(0x66);
				i.putByte(0x0F);
				i.putByte(0xAD);
				putRegReg(i, reg1, reg0); //shrd has exchanged order of operands
				return;
			case I_SARregreg:
				if (sizeprefix)
					i.putByte(0x66);
				i.putByte(0xD2 | wordflag);
				i.putByte(0xF8 | (reg0 >>> 4));
				return;
			case I_SARregimm:
				codeShiftRegImm(i, 0xF8, sizeprefix, wordflag, reg0, imm);
				return;
			case I_SARmemimm:
				codeShiftMemImm(i, 0x78, reg0, disp, imm, par);
				return;
			case I_ROLregimm:
				codeShiftRegImm(i, 0xC0, sizeprefix, wordflag, reg0, imm);
				return;
			case I_RORregimm:
				codeShiftRegImm(i, 0xC8, sizeprefix, wordflag, reg0, imm);
				return;
			case I_RCRregimm:
				codeShiftRegImm(i, 0xD8, sizeprefix, wordflag, reg0, imm);
				return;
			case I_BSRregreg:
				if (sizeprefix)
					i.putByte(0x66);
				i.putByte(0x0F);
				i.putByte(0xBD);
				putRegReg(i, reg0, reg1);
				return;
			case I_CDQ:
				i.putByte(0x99);
				return;
			case I_OUTreg:
				if (sizeprefix)
					i.putByte(0x66);
				i.putByte(0xEE | wordflag);
				return;
			case I_INreg:
				if (sizeprefix)
					i.putByte(0x66);
				i.putByte(0xEC | wordflag);
				return;
			case I_SAHF:
				i.putByte(0x9E);
				return;
			//floating point instructions
			case I_FLDmem:
				i.putByte(0xD8 | par);
				putMem(i, 0x00, reg0, disp);
				return;
			case I_FSTPmem:
				i.putByte(0xD8 | par);
				putMem(i, 0x30, reg0, disp);
				return;
			case I_FILDmem:
				switch (par)
				{
					case FPU32:
						i.putByte(0xDB);
						putMem(i, 0, reg0, disp);
						return;
					case FPU64:
						i.putByte(0xDF);
						putMem(i, 0x50, reg0, disp);
						return;
				}
				break;
			case I_FISTPmem:
				i.putByte(0xDB | par);
				putMem(i, par == FPU32 ? 0x30 : 0x70, reg0, disp);
				return;
			case I_FISTTPmem:
				i.putByte(par == FPU32 ? 0xDB : 0xDD);
				putMem(i, 0x10, reg0, disp);
				return;
			case I_FCHS:
				i.putByte(0xD9);
				i.putByte(0xE0);
				return;
			case I_FADDP:
				i.putByte(0xDE);
				i.putByte(0xC1);
				return;
			case I_FSUBP:
				i.putByte(0xDE);
				i.putByte(0xE9);
				return;
			case I_FMULP:
				i.putByte(0xDE);
				i.putByte(0xC9);
				return;
			case I_FDIVP:
				i.putByte(0xDE);
				i.putByte(0xF9);
				return;
			case I_FDUP: //fld st0 to duplicate the stack top
				i.putByte(0xD9);
				i.putByte(0xC0);
				return;
			case I_FXCH:
				i.putByte(0xD9);
				i.putByte(0xC9);
				return;
			case I_FFREE:
				i.putByte(0xDD);
				i.putByte(0xC0);
				return;
			case I_FINCSTP:
				i.putByte(0xD9);
				i.putByte(0xF7);
				return;
			case I_FCOMPP:
				i.putByte(0xDE);
				i.putByte(0xD9);
				return;
			case I_FCOMIP:
				i.putByte(0xDF);
				i.putByte(0xF1);
				return;
			case I_FSTSW:
				i.putByte(0xDF);
				i.putByte(0xE0);
				return;
			case I_FNSTCWmem:
				i.putByte(0xD9);
				putMem(i, 0x70, reg0, disp);
				return;
			case I_FLDCWmem:
				i.putByte(0xD9);
				putMem(i, 0x50, reg0, disp);
				return;
			case I_FWAIT:
				i.putByte(0x9B);
				return;
			//special instructions
			case I_LEAarray: //will result in lea reg0,[reg0+reg1*par+disp] (only for pointers!)
				i.putByte(0x8D);
				putRegMemInx(i, reg0, reg0, reg1, par, disp);
				return;
			case I_MOVindexed: //will result in mov reg0,[reg0+reg1] (only for pointers!)
				i.putByte(0x8B);
				putRegMemInx(i, reg0, reg0, reg1, 0, 0);
				return;
			case I_BOUNDEXC:
				i.putByte(0xCD); //INT 0x05
				i.putByte(0x05);
				return;
			case I_RETMSEXC:
				i.putByte(0xCD); //INT 0x1F, marked as "do not use"
				i.putByte(0x1F);
				return;
			case I_MARKER:
				i.putByte(0x90);
				return;
		}
		fatalError(ERR_INVINS_CODE);
	}
	
	private void codeAriRegImm(Instruction i, int code, boolean sizeprefix, int wordflag, int reg0, int imm)
	{
		int sizeExt = 0;
		
		if (sizeprefix)
			i.putByte(0x66);
		if (wordflag != 0 && imm >= -128 && imm < 128)
			sizeExt = 2;
		//else sizeExt=0; already initialized
		if (sizeExt == 0 && (reg0 == R_EAX || reg0 == R_AX || reg0 == R_AL))
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
	
	private void codeShiftRegImm(Instruction i, int code, boolean sizeprefix, int wordflag, int reg0, int imm)
	{
		if (sizeprefix)
			i.putByte(0x66);
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
	
	private void codeShiftMemImm(Instruction i, int code, int reg0, int disp, int imm, int par)
	{
		int wordflag = 0;
		
		if (par == 2)
		{
			i.putByte(0x66);
			wordflag = 1;
		}
		else if (par == 4)
			wordflag = 1;
		if (imm == 1)
		{
			i.putByte(0xD0 | wordflag);
			putMem(i, code, reg0, disp);
		}
		else
		{
			i.putByte(0xC0 | wordflag);
			putMem(i, code, reg0, disp);
			i.putByte(imm);
		}
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
			i.putByte(0x05 | ((reg0 >>> 4) << 3));
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
				fatalError("IA32.putImm with invalid reg0");
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
				fatalError("IA32.putRegMemInx with invalid scale");
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
