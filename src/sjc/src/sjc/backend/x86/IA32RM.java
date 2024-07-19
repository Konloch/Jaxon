/* Copyright (C) 2006, 2007, 2008, 2009, 2010, 2011 Stefan Frenz
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

/**
 * IA32RM: architecture implementation for 32 bit protected mode IA32 processors
 *
 * @author S. Frenz
 * @version 110609 fixed entrySize check in genLoadDerefAddr
 * version 101210 adopted changed Architecture
 * version 101101 adopted changed Architecture
 * version 101027 fixed sign handling for logical bitshift of byte and short values
 * version 100927 fixed unsignedness of chars
 * version 100115 adopted changed error reporting and codeStart-movement
 * version 091001 adopted changed memory interface
 * version 090717 adopted changed Architecture
 * version 090626 added support for stack extreme check
 * version 090619 adopted changed Architecture
 * version 090430 added support for native "return missing" hint
 * version 090219 adopted changed X86Base
 * version 090208 removed genClearMem
 * version 081209 added support for method printing
 * version 081021 adopted changes in Architecture
 * version 080608 added support for language throwables
 * version 080525 adopted changed genCondJmp signature
 * version 080122 fixed setting of usedRegs in getReg on firstWrite
 * version 080105 added genSavePrimary and genRestPrimary
 * version 070917 pre-initialization of stack variables with "0"
 * version 070913 moved curVarOffParam to X86Base, added support for genClearMem
 * version 070814 adopted renaming of hasFltDblWarning
 * version 070809 adopted changes for float/double
 * version 070809 adopted changes for float/double
 * version 070705 fixed genCompValToConstLongVal
 * version 070628 added allocClearBits
 * version 070615 removed no longer needed getRef
 * version 070610 optimized access to different jump offset sizes
 * version 070606 moved common methods and variables to X86Base
 * version 070601 optimized genBinOp, externalized Strings to X86Base
 * version 070531 adopted removal of Architecture.genLoadFromMem
 * version 070505 changed naming of Clss to Unit, changed OutputObject to int
 * version 070501 optimized insPatched
 * version 070423 fixed interrupt pro- and epilog
 * version 070127 optimized access to err-flag
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
 * version 060630 initial version
 */

public class IA32RM extends X86Base
{
	//offsets of parameters
	private final static int VAROFF_PARAM_INL = 2;
	private final static int VAROFF_PARAM_NRM = 4;
	
	//initialization
	public IA32RM()
	{
		relocBytes = 2;
		allocClearBits = stackClearBits = 1;
		maxInstrCodeSize = 10;
		rAll = RegA | RegB | RegC | RegD;
		rClss = R_DI;
		rInst = R_SI;
		rBase = R_BP;
		fullIPChangeBytes = 2;
		mPtr = RS_X;
	}
	
	//references are treated as normal short values
	public void putRef(Object loc, int offset, Object ptr, int ptrOff)
	{
		mem.putShort(loc, offset, (short) mem.getAddrAsInt(ptr, ptrOff));
	}
	
	public void putCodeRef(Object loc, int offset, Object ptr, int ptrOff)
	{
		mem.putShort(loc, offset, (short) (mem.getAddrAsInt(ptr, ptrOff) - mem.getAddrAsInt(loc, offset) - 2));
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
			default:
				fatalError(ERR_INVREG_GETREG);
				return 0;
		}
		switch (type)
		{
			case StdTypes.T_LONG:
				return reg | 0x7;
			case StdTypes.T_INT:
				if (nr != 1)
					return 0;
				return reg | 0x7;
			case StdTypes.T_DPTR:
				return reg | 0x3;
			case StdTypes.T_SHRT:
			case StdTypes.T_CHAR:
			case StdTypes.T_PTR:
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
			mthdContainer = mthd; //remember outest level method
		usedRegs = 0;
		curMthd = mthd;
		return lastMthd;
	}
	
	public void codeProlog()
	{
		int i;
		
		if ((curMthd.marker & Marks.K_INTR) == 0)
		{
			curVarOffParam = curInlineLevel > 0 ? VAROFF_PARAM_INL : VAROFF_PARAM_NRM;
			if (curMthd.varSize == 0 && curMthd.parSize == 0)
				return; //no intr, no parameters, no local vars -> no ebp
		}
		else
		{
			if (curInlineLevel > 0)
			{
				fatalError(ERR_INTNOINLINE);
				return;
			}
			if (curMthd.parSize != 0)
			{
				fatalError(ERR_INVPARINT);
				return;
			}
			curVarOffParam = 0; //no parameter existing
			ins(I_PUSHreg, R_EAX);
			ins(I_PUSHreg, R_ECX);
			ins(I_PUSHreg, R_EDX);
			ins(I_PUSHreg, R_EBX);
			ins(I_PUSHreg, R_SI);
			ins(I_PUSHreg, R_DI);
		}
		ins(I_PUSHreg, R_BP);
		ins(I_MOVregreg, R_BP, R_SP);
		switch (curMthd.varSize)
		{
			case 0:
				break;
			case 2:
				ins(I_PUSHimm);
				break;
			default:
				ins(I_XORregreg, R_BX, R_BX);
				for (i = curMthd.varSize; i > 0; i -= 2)
					ins(I_PUSHreg, R_BX);
		}
	}
	
	public void codeEpilog(Mthd outline)
	{
		int var, par;
		
		var = curMthd.varSize;
		par = curMthd.parSize;
		if (curInlineLevel > 0)
		{ //end of method inlining
			if (var == 0 && par == 0)
			{ /*nothing to do*/ }
			else if (var == 0)
			{ //par!=0
				ins(I_POPreg, R_BP);
				ins(I_ADDregimm, R_SP, 0, 0, par);
			}
			else if (par == 0)
			{ //var!=0
				ins(I_ADDregimm, R_SP, 0, 0, var);
				ins(I_POPreg, R_BP);
			}
			else
			{ //neither var==0 nor par==0
				ins(I_MOVregmem, R_BP, R_BP);
				ins(I_ADDregimm, R_SP, 0, 0, var + par + 2);
			}
			if (--curInlineLevel == 0)
				curVarOffParam = VAROFF_PARAM_NRM;
			curMthd = outline;
			return;
		}
		//normal end of method
		if (curMthd.varSize == 0 && curMthd.parSize == 0 && (curMthd.marker & Marks.K_INTR) == 0)
		{
			ins(I_RETimm); //nothing pushed, don't pop
			curMthd = null;
			return;
		}
		if (var != 0)
		{
			ins(I_ADDregimm, R_SP, 0, 0, var);
			ins(I_POPreg, R_BP);
		}
		else
			ins(I_POPreg, R_BP);
		if (par > 32767)
		{
			fatalError(ERR_RETSIZE_CODEEPILOG);
			return;
		}
		if ((curMthd.marker & Marks.K_INTR) != 0)
		{
			ins(I_POPreg, R_DI);
			ins(I_POPreg, R_SI);
			ins(I_POPreg, R_EBX);
			ins(I_POPreg, R_EDX);
			ins(I_POPreg, R_ECX);
			ins(I_POPreg, R_EAX);
			ins(I_IRET);
		}
		else
			ins(I_RETimm, 0, 0, 0, par);
		curMthd = null;
	}
	
	//general purpose instructions
	public void genLoadConstVal(int dst, int val, int type)
	{
		if (type == StdTypes.T_FLT)
		{
			hasFltDblWarning();
			return;
		}
		if ((dst = getReg(1, dst, type, true)) == 0)
			return;
		if (val == 0)
			ins(I_XORregreg, dst, dst);
		else
			ins(I_MOVregimm, dst, 0, 0, val);
	}
	
	public void genLoadConstDoubleOrLongVal(int dst, long val, boolean asDouble)
	{
		int reg1, reg2;
		if (asDouble)
		{
			hasFltDblWarning();
			return;
		}
		if ((reg1 = getReg(1, dst, StdTypes.T_LONG, true)) == 0 || (reg2 = getReg(2, dst, StdTypes.T_LONG, true)) == 0)
			return;
		ins(I_MOVregimm, reg1, 0, 0, (int) val);
		ins(I_MOVregimm, reg2, 0, 0, (int) (val >>> 32));
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
		ins(I_LEAregmem, dst, src, pos);
	}
	
	public void genLoadVarVal(int dstR, int src, Object loc, int off, int type)
	{
		int dst, dstL, pos = mem.getAddrAsInt(loc, off);
		if ((dst = getReg(1, dstR, type, true)) == 0)
			return;
		if (src == regBase && pos >= 0)
			pos += curVarOffParam;
		if (src != 0 && (src = getReg(1, src, StdTypes.T_PTR, false)) == 0)
			return;
		switch (type)
		{
			case StdTypes.T_BOOL:
			case StdTypes.T_BYTE:
			case StdTypes.T_SHRT:
			case StdTypes.T_CHAR:
			case StdTypes.T_PTR:
			case StdTypes.T_INT:
				ins(I_MOVregmem, dst, src, pos);
				return;
			case StdTypes.T_DPTR:
				if ((dstL = getReg(2, dstR, StdTypes.T_DPTR, true)) == 0)
					return;
				if ((dst | RS_E) != (src | RS_E))
				{ //first move dst, then dstL
					ins(I_MOVregmem, dst, src, pos);
					ins(I_MOVregmem, dstL, src, pos + 2);
					return;
				}
				//first move dstL, then dst
				ins(I_MOVregmem, dstL, src, pos + 2);
				ins(I_MOVregmem, dst, src, pos);
				return;
			case StdTypes.T_LONG:
				if ((dstL = getReg(2, dstR, StdTypes.T_LONG, true)) == 0)
					return;
				if ((dst | RS_E) != (src | RS_E))
				{ //first move dst, then dstL
					ins(I_MOVregmem, dst, src, pos);
					ins(I_MOVregmem, dstL, src, pos + 4);
					return;
				}
				//first move dstL, then dst
				ins(I_MOVregmem, dstL, src, pos + 4);
				ins(I_MOVregmem, dst, src, pos);
				return;
		}
		fatalError(ERR_INVTYPE_GENLOADVARVAL);
	}
	
	public void genConvertVal(int dstR, int srcR, int toType, int fromType)
	{
		int dst, dstL, src, srcL;
		//use only requested part of source
		if ((dst = getReg(1, dstR, toType, true)) == 0 || (src = getReg(1, srcR, fromType, false)) == 0)
			return;
		//check special conversions
		if (toType == StdTypes.T_PTR || fromType == StdTypes.T_PTR)
		{
			if (fromType != StdTypes.T_INT && toType != StdTypes.T_INT && fromType != StdTypes.T_SHRT && toType != StdTypes.T_SHRT)
			{
				fatalError(ERR_UNSTYPE_GENCONVERTVAL);
				return;
			}
			if (toType == StdTypes.T_INT)
			{
				ins(I_MOVZXregreg, dst, src);
				return;
			}
			if (dst != src)
				ins(I_MOVregreg, dst, src);
			//else: nothing to do
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
			if ((src = getReg(1, srcR, fromType, false)) == 0)
				return;
			if (fromType != StdTypes.T_INT)
			{ //convert 8/16-bit to 32 bit
				ins(I_MOVSXregreg, dst, src);
				src |= RS_E; //use as E* register
			}
			else if (src != dst)
				ins(I_MOVregreg, dst, src);
			if (dst == R_EAX && dstL == R_EDX)
				ins(I_CWDE);
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
						//nothing to do for byte->byte
						if (dst != (src | RS_E))
							ins(I_MOVregreg, dst, src);
						//else: nothing to do
						return;
					case StdTypes.T_SHRT:
					case StdTypes.T_INT:
						ins(I_MOVSXregreg, dst, src);
						return;
					case StdTypes.T_CHAR:
						ins(I_MOVZXregreg, dst, src);
						return;
				}
				break;
			case StdTypes.T_SHRT:
			case StdTypes.T_CHAR:
				switch (toType)
				{
					case StdTypes.T_BYTE:
					case StdTypes.T_SHRT:
					case StdTypes.T_CHAR:
						//nothing to do for short/char->byte/short/char
						if (dst != (src | RS_E))
							ins(I_MOVregreg, dst, src);
						//else: nothing to do
						return;
					case StdTypes.T_INT:
						ins(I_MOVSXregreg, dst, src);
						return;
				}
				break;
			case StdTypes.T_INT:
			case StdTypes.T_LONG:
				switch (toType)
				{
					case StdTypes.T_BYTE:
					case StdTypes.T_SHRT:
					case StdTypes.T_CHAR:
					case StdTypes.T_INT:
					case StdTypes.T_LONG:
						//nothing to do for int/long->byte/short/char/int
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
				if ((dst = getReg(2, dstR, type, true)) == 0 || (src = getReg(2, srcR, type, false)) == 0)
					return;
				ins(I_MOVregreg, dst, src);
				return;
		}
		fatalError(ERR_UNSTYPE_GENDUP);
	}
	
	public void genPushConstVal(int val, int type)
	{
		ins(I_PUSHimm, 0, 0, 0, val, type);
	}
	
	public void genPushConstDoubleOrLongVal(long val, boolean asDouble)
	{
		ins(I_PUSHimm, 0, 0, 0, (int) (val >>> 32), StdTypes.T_INT);
		ins(I_PUSHimm, 0, 0, 0, (int) val, StdTypes.T_INT);
	}
	
	public void genPush(int srcR, int type)
	{
		int src;
		if (type == StdTypes.T_NULL)
		{
			fatalError(ERR_UNRPUSH_GENPUSH);
			return;
		}
		if (type == StdTypes.T_LONG || type == StdTypes.T_DPTR)
		{
			if ((src = getReg(2, srcR, type, false)) == 0)
				return;
			ins(I_PUSHreg, src);
		}
		if ((src = getReg(1, srcR, type, false)) == 0)
			return;
		ins(I_PUSHreg, src);
	}
	
	public void genPop(int dstR, int type)
	{
		int dst;
		if ((dst = getReg(1, dstR, type, true)) == 0)
			return;
		ins(I_POPreg, dst);
		if (type == StdTypes.T_LONG || type == StdTypes.T_DPTR)
		{
			if ((dst = getReg(2, dstR, type, true)) == 0)
				return;
			ins(I_POPreg, dst);
		}
	}
	
	public void genAssign(int dst, int srcR, int type)
	{
		int src;
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
			case StdTypes.T_DPTR:
				ins(I_MOVmemreg, dst, src);
				if ((src = getReg(2, srcR, StdTypes.T_DPTR, false)) == 0)
					return;
				ins(I_MOVmemreg, dst, src, 2);
				return;
			case StdTypes.T_LONG:
				ins(I_MOVmemreg, dst, src);
				if ((src = getReg(2, srcR, StdTypes.T_LONG, false)) == 0)
					return;
				ins(I_MOVmemreg, dst, src, 4);
				return;
		}
		fatalError(ERR_UNSTYPE_GENASSIGN);
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
		if ((dst = getReg(1, dstR, type, false)) == 0 || (src2 = getReg(1, src2R, type, false)) == 0)
			return;
		if (dstR != src1R || dst == src2)
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
								if ((dst & RS_E) != RS_E)
								{
									ins(I_MOVSX, dst | RS_E, dst);
									dst |= RS_E;
								}
								if ((src2 & RS_E) != RS_E)
								{
									ins(I_MOVSX, src2 | RS_E, src2);
									src2 |= RS_E;
								}
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
								if ((dst & RS_E) != RS_E)
								{
									ins(I_MOVSX, dst | RS_E, dst);
									dst |= RS_E;
								}
								if ((src2 & RS_E) != RS_E)
								{
									ins(I_MOVSX, src2 | RS_E, src2);
									src2 |= RS_E;
								}
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
								ins(I_CWDE);
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
								xchg = false;
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
				fatalError(ERR_UNSTYPE_GENBINOP);
				return;
			case StdTypes.T_LONG:
				if ((dstL = getReg(2, dstR, StdTypes.T_LONG, false)) == 0)
					return;
				switch (opType)
				{
					case Ops.S_ARI:
						src2L = getReg(2, src2R, StdTypes.T_LONG, false);
						if (src2L == 0)
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
							default:
								fatalError(ERR_UNSOP_GENBINOP);
								return;
						}
					case Ops.S_BSH:
						usedMask = dstR | src2R;
						saveRegs = storeReg(rAll & ~usedMask);
						switch (opPar)
						{
							case Ops.B_SHL:
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
	
	public void genUnaOp(int dstR, int srcR, int op, int type)
	{
		int dst, dst2;
		if (type != StdTypes.T_LONG)
		{
			genDefaultUnaOp(dstR, srcR, op, type);
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
		fatalError(ERR_UNSOP_GENUNAOP);
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
			ins(I_CALLmem, R_DI, 0, off);
		else
		{
			ins(I_MOVregmem, R_BX, R_DI, off);
			ins(I_LEAregmem, R_BX, R_BX, ctx.codeStart);
			ins(I_CALLreg, R_BX);
		}
	}
	
	public void genCallIndexed(int intfReg, int off, int parSize)
	{
		if ((intfReg = getReg(2, intfReg, StdTypes.T_DPTR, false)) == 0)
			return;
		ins(I_MOVregmem, R_BX, intfReg, off);
		ins(I_MOVindexed); //fixed to mov bx,[bx+di]
		if (ctx.codeStart != 0)
			ins(I_LEAregmem, R_BX, R_BX, ctx.codeStart);
		ins(I_CALLreg, R_BX);
	}
	
	public void genCallConst(Mthd mthd, int parSize)
	{
		insPatchedCall(mthd, parSize);
	}
	
	public int genComp(int src1R, int src2R, int type, int cond)
	{
		Instruction dummy;
		int src1, src2, src1L, src2L;
		if ((src1 = getReg(1, src1R, type, false)) == 0 || (src2 = getReg(1, src2R, type, false)) == 0)
			return 0;
		switch (type)
		{
			case StdTypes.T_BOOL:
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
	{ //type not of interest here
		if ((src = getReg(1, src, type, false)) == 0)
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
	
	public int genCompValToConstDoubleOrLongVal(int src, long val, boolean asDouble, int cond)
	{
		Instruction dummy;
		int reg1, reg2;
		if (asDouble)
		{
			hasFltDblWarning();
			return 0;
		}
		if ((reg1 = getReg(1, src, StdTypes.T_LONG, false)) == 0 || (reg2 = getReg(2, src, StdTypes.T_LONG, false)) == 0)
			return 0;
		ins(I_CMPregimm, reg2, 0, 0, (int) (val >>> 32));
		dummy = getUnlinkedInstruction(); //will stay NOP, just as jump-destination
		insJump(dummy, Ops.C_NE);
		ins(I_PUSHreg, reg1);
		ins(I_SHRregimm, reg1, 0, 0, 1);
		ins(I_CMPregreg, reg1, 0, 0, (int) (val >>> 1) & 0x7FFFFFFF); //unsigned compare of bits 31..1
		ins(I_POPreg, reg1);
		insJump(dummy, Ops.C_NE);
		ins(I_CMPregimm, reg1, 0, 0, (int) val); //compare of bits 1
		appendInstruction(dummy);
		return cond;
	}
	
	public void genLoadDerefAddr(int destReg, int objReg, int indReg, int baseOffset, int entrySize)
	{
		if ((destReg = getReg(1, destReg, StdTypes.T_PTR, true)) == 0 || (objReg = getReg(1, objReg, StdTypes.T_PTR, false)) == 0 || (indReg = getReg(1, indReg, StdTypes.T_INT, false)) == 0)
			return;
		if (entrySize < 0)
		{
			ins(I_NEGreg, indReg);
			entrySize = -entrySize;
		}
		while (entrySize > 1)
		{
			if (entrySize != 1 && (entrySize & 1) != 0)
			{
				fatalError("unsupported entrySize in genLoadDerefAddr");
				return;
			}
			ins(I_SHLreg1, indReg);
			entrySize = entrySize >>> 1;
		}
		if (destReg != objReg)
			ins(I_MOVregreg, destReg, objReg);
		ins(I_ADDregreg, destReg, indReg);
		ins(I_ADDregimm, destReg, 0, 0, baseOffset);
	}
	
	public void genMoveToPrimary(int srcR, int type)
	{
		int reg;
		if ((reg = getReg(1, srcR, type, false)) == 0)
			return;
		if (srcR != RegA)
			ins(I_MOVregreg, getReg(1, RegA, type, false), reg);
		if (type == StdTypes.T_LONG || type == StdTypes.T_DPTR)
		{
			if ((reg = getReg(2, srcR, type, false)) == 0)
				return;
			if (srcR != RegD)
				ins(I_MOVregreg, getReg(2, RegD, type, false), reg);
		}
	}
	
	public void genMoveFromPrimary(int dstR, int type)
	{
		int reg;
		if (type == StdTypes.T_LONG || type == StdTypes.T_DPTR)
		{
			if ((reg = getReg(2, dstR, type, true)) == 0)
				return;
			if (dstR != RegD)
				ins(I_MOVregreg, reg, getReg(2, RegD, type, false));
		}
		if ((reg = getReg(1, dstR, type, true)) == 0)
			return;
		if (dstR != RegA)
			ins(I_MOVregreg, reg, getReg(1, RegA, type, false));
	}
	
	public void genMoveIntfMapFromPrimary(int dst)
	{
		if ((dst = getReg(2, dst, StdTypes.T_DPTR, true)) == 0)
			return;
		ins(I_MOVregreg, dst, R_AX); //reg can not be AX, because it is the second one in dst
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
	
	//here the internal coding of the instructions takes place
	private void insMem(int type, boolean sizeprefix, int wordflag, int reg, int mem, int disp, int imm, int par)
	{
		Instruction i;
		int popReg = 0;
		
		if ((mem & RS_E) != RS_X)
		{
			fatalError("invalid mem operand for IA32RM.insMem(.)");
			return;
		}
		if (mem != R_BX && mem != R_BP && mem != R_SI && mem != R_DI)
		{
			if ((reg | RS_E) != R_EBX)
			{
				if ((usedRegs & RegB) != 0)
				{
					ins(I_PUSHreg, R_BX);
					popReg = R_BX;
				}
				ins(I_MOVregreg, R_BX, mem);
				mem = R_BX;
			}
			else
			{
				ins(I_PUSHreg, R_SI);
				ins(I_MOVregreg, R_SI, mem);
				mem = popReg = R_SI;
			}
		}
		//get a new instruction and insert it
		i = getUnlinkedInstruction();
		appendInstruction(i);
		i.type = type;
		//code instruction
		switch (type)
		{
			case I_MOVregmem:
				if (sizeprefix)
					i.putByte(0x66);
				i.putByte(0x8A | wordflag);
				putMem(i, reg, mem, disp);
				break;
			case I_MOVmemreg:
				if (sizeprefix)
					i.putByte(0x66);
				i.putByte(0x88 | wordflag);
				putMem(i, reg, mem, disp);
				break;
			case I_MOVmemimm:
				if (par == 4)
					i.putByte(0x66);
				i.putByte(0xC6 | (par == 1 ? 0 : 1));
				putMem(i, 0, mem, disp);
				if (par == 4)
					i.putInt(imm);
				else if (par == 2)
					i.putShort(imm);
				else
					i.putByte(imm);
				break;
			case I_MOVSXregmem:
				if ((reg & 0x0F) == RS_E)
					i.putByte(0x66); //to 32 bit instead of 16
				i.putByte(0x0F);
				if (par == 2)
					i.putByte(0xBF); //from 16 bit source
				else
					i.putByte(0xBE); //from 8 bit source
				putMem(i, reg, mem, disp);
				break;
			case I_LEAregmem:
				i.putByte(0x8D);
				putMem(i, reg, mem, disp);
				break;
			case I_ADDmemimm:
				if (par > 1)
				{ //add word or dword
					if (par == 4)
						i.putByte(0x66);
					if (imm >= -128 && imm < 127)
					{
						i.putByte(0x83);
						putMem(i, 0, mem, disp);
						i.putByte(imm);
					}
					else
					{
						i.putByte(0x81);
						putMem(i, 0, mem, disp);
						if (par == 2)
							i.putShort(imm);
						else
							i.putInt(imm);
					}
				}
				else
				{ //add byte
					i.putByte(0x80);
					putMem(i, 0, mem, disp);
					i.putByte(imm);
				}
				break;
			case I_SUBmemimm:
				if (par > 1)
				{ //sub word or dword
					if (par == 4)
						i.putByte(0x66);
					if (imm >= -128 && imm < 127)
					{
						i.putByte(0x83);
						putMem(i, 0x50, mem, disp); //"sub reg0" is like "dummy 0, reg0"
						i.putByte(imm);
					}
					else
					{
						i.putByte(0x81);
						putMem(i, 0x50, mem, disp); //"sub reg0" is like "dummy 0, reg0"
						if (par == 2)
							i.putShort(imm);
						else
							i.putInt(imm);
					}
				}
				else
				{ //sub byte
					i.putByte(0x80);
					putMem(i, 0x50, mem, disp); //"sub reg0" is like "dummy 0, reg0"
					i.putByte(imm);
				}
				break;
			case I_INCmem:
				if (par > 1)
				{
					wordflag = 1;
					if (par == 4)
						i.putByte(0x66);
				}
				else
					wordflag = 0;
				i.putByte(0xFE | wordflag);
				putMem(i, 0x00, mem, disp); //"inc reg0" is like "dummy 0, reg0"
				break;
			case I_DECmem:
				if (par > 1)
				{
					wordflag = 1;
					if (par == 4)
						i.putByte(0x66);
				}
				else
					wordflag = 0;
				i.putByte(0xFE | wordflag);
				putMem(i, 0x10, mem, disp); //"dec reg0" is like "dummy 1, reg0"
				break;
			case I_CMPregmem:
				if (sizeprefix)
					i.putByte(0x66);
				i.putByte(0x3A | wordflag);
				putMem(i, reg, mem, disp);
				break;
			default:
				fatalError(ERR_INVINS_CODE);
				return;
		}
		if (popReg != 0)
			ins(I_POPreg, popReg, 0, 0, 0, 0);
	}
	
	protected void internalFixStackExtremeAdd(Instruction me, int stackCells)
	{
		boolean sizeprefix = false;
		int wordflag = 0, tmp = me.reg0 & 0x0F; //register 0 gives operation size (exception: MOVSXregmem, INC, DEC)
		if (tmp == RS_E)
		{
			wordflag = 1; //EAX, EBX, ...
			sizeprefix = true;
		}
		else if (tmp == RS_X)
		{ //AX, BX, ...
			wordflag = 1;
		}
		me.size = 0;
		me.type = I_ADDregimm;
		codeAriRegImm(me, 0x04, sizeprefix, wordflag, me.reg0, me.iPar2 = stackCells << 1);
	}
	
	protected Instruction ins(int type, int reg0, int reg1, int disp, int imm)
	{
		return ins(type, reg0, reg1, disp, imm, 0);
	}
	
	private Instruction ins(int type, int reg0, int reg1, int disp, int imm, int par)
	{
		Instruction i;
		int tmp, wordflag = 0;
		boolean sizeprefix = false;
		
		//get a new instruction and insert it
		i = getUnlinkedInstruction();
		appendInstruction(i);
		i.type = type;
		//check wordflag and sizeprefix for most instructions (check only, do not code)
		if ((type & IM_P0) == I_reg0 || (type & IM_P1) == I_reg1)
		{
			if ((type & IM_P0) == I_reg0)
				tmp = reg0 & 0x0F; //register 0 gives operation size (exception: MOVSXregmem, ADDmemimm, SUBmemimm, INC, DEC)
			else
				tmp = reg1 & 0x0F; //register 1 gives operation size
			if (tmp == RS_E)
			{
				wordflag = 1; //EAX, EBX, ...
				sizeprefix = true;
			}
			else if (tmp == RS_X)
			{ //AX, BX, ...
				wordflag = 1;
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
				break;
			case I_MOVregmem:
				if (reg1 == 0 && (reg0 == R_EAX || reg0 == R_AX || reg0 == R_AL))
				{
					if (sizeprefix)
						i.putByte(0x66);
					i.putByte(0xA0 | wordflag);
					i.putShort(disp);
				}
				else
				{
					i.type = I_NONE; //make instruction NOP and insert perhaps multiple instructions
					insMem(I_MOVregmem, sizeprefix, wordflag, reg0, reg1, disp, 0, 0);
				}
				break;
			case I_MOVmemreg:
				if (reg1 == 0 && (reg0 == R_EAX || reg0 == R_AX || reg0 == R_AL))
				{
					if (sizeprefix)
						i.putByte(0x66);
					i.putByte(0xA2 | wordflag);
					i.putShort(disp);
				}
				else
				{
					i.type = I_NONE; //make instruction NOP and insert perhaps multiple instructions
					insMem(I_MOVmemreg, sizeprefix, wordflag, reg1, reg0, disp, 0, 0); //toggle reg0/reg1 as needed for memreg-ops
				}
				break;
			case I_MOVmemimm:
				i.type = I_NONE; //make instruction NOP and insert perhaps multiple instructions
				insMem(I_MOVmemimm, false, 0, 0, reg0, disp, imm, par); //toggle reg0/reg1 as needed for memimm-op
				break;
			case I_MOVregimm:
				if (sizeprefix)
					i.putByte(0x66);
				i.putByte(0xB0 | (wordflag << 3) | (reg0 >>> 4));
				putImm(i, reg0, imm);
				break;
			case I_MOVSXregreg:
				tmp = reg0 & 0x0F;
				if (tmp == RS_E)
					i.putByte(0x66); //to 32 bit instead of 16
				i.putByte(0x0F);
				if ((reg1 & 0x0F) == RS_X)
					i.putByte(0xBF); //from AX, BX, ...
				else
					i.putByte(0xBE); //from AL, BL, ...
				putRegReg(i, reg0, reg1);
				break;
			case I_MOVSXregmem:
				i.type = I_NONE; //make instruction NOP and insert perhaps multiple instructions
				insMem(I_MOVSXregmem, sizeprefix, wordflag, reg0, reg1, disp, 0, par);
				break;
			case I_MOVZXregreg:
				tmp = reg0 & 0x0F;
				if (tmp == RS_E)
					i.putByte(0x66); //to 32 bit instead of 16
				i.putByte(0x0F);
				if ((reg1 & 0x0F) == RS_X)
					i.putByte(0xB7); //from AX, BX, ...
				else
					i.putByte(0xB6); //from AL, BL, ...
				putRegReg(i, reg0, reg1);
				break;
			case I_LEAregmem:
				i.type = I_NONE; //make instruction NOP and insert perhaps multiple instructions
				insMem(I_LEAregmem, sizeprefix, wordflag, reg0, reg1, disp, 0, 0);
				break;
			case I_PUSHreg:
				if (sizeprefix)
					i.putByte(0x66);
				i.putByte(0x50 | (reg0 >>> 4));
				break;
			case I_PUSHimm:
				if (imm >= -128 && imm < 128)
				{ //8 bits immediate
					if (par == StdTypes.T_INT)
						i.putByte(0x66);
					i.putByte(0x6A);
					i.putByte(imm);
				}
				else if (par != StdTypes.T_INT)
				{ //16 bit immediate
					i.putByte(0x68);
					i.putShort(imm);
				}
				else
				{ //32 bit immediate
					i.putByte(0x66);
					i.putByte(0x68);
					i.putInt(imm);
				}
				break;
			case I_POPreg:
				if (sizeprefix)
					i.putByte(0x66);
				i.putByte(0x58 | (reg0 >>> 4));
				break;
			case I_CALLreg:
				i.putByte(0xFF);
				i.putByte(0xD0 | (reg0 >>> 4));
				break;
			case I_CALLmem:
				i.putByte(0xFF);
				putMem(i, 0x20, reg0, disp);
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
				i.putByte(0xCF);
				break;
			case I_ADDregreg:
				if (sizeprefix)
					i.putByte(0x66);
				i.putByte(0x02 | wordflag);
				putRegReg(i, reg0, reg1);
				break;
			case I_STEXreg:
			case I_ADDregimm:
				codeAriRegImm(i, 0x04, sizeprefix, wordflag, reg0, imm);
				break;
			case I_ADDmemimm:
				i.type = I_NONE; //make instruction NOP and insert perhaps multiple instructions
				insMem(I_ADDmemimm, sizeprefix, wordflag, 0, reg0, disp, imm, par);
				break;
			case I_ADCregreg:
				if (sizeprefix)
					i.putByte(0x66);
				i.putByte(0x12 | wordflag);
				putRegReg(i, reg0, reg1);
				break;
			case I_SUBregimm:
				codeAriRegImm(i, 0x2C, sizeprefix, wordflag, reg0, imm);
				break;
			case I_SUBregreg:
				if (sizeprefix)
					i.putByte(0x66);
				i.putByte(0x2A | wordflag);
				putRegReg(i, reg0, reg1);
				break;
			case I_SUBmemimm:
				i.type = I_NONE; //make instruction NOP and insert perhaps multiple instructions
				insMem(I_SUBmemimm, sizeprefix, wordflag, 0, reg0, disp, imm, par);
				break;
			case I_SBBregreg:
				if (sizeprefix)
					i.putByte(0x66);
				i.putByte(0x1A | wordflag);
				putRegReg(i, reg0, reg1);
				break;
			case I_SBBregimm:
				codeAriRegImm(i, 0x1C, sizeprefix, wordflag, reg0, imm);
				break;
			case I_ANDregreg:
				if (sizeprefix)
					i.putByte(0x66);
				i.putByte(0x22 | wordflag);
				putRegReg(i, reg0, reg1);
				break;
			case I_XORregreg:
				if (sizeprefix)
					i.putByte(0x66);
				i.putByte(0x32 | wordflag);
				putRegReg(i, reg0, reg1);
				break;
			case I_XORregimm:
				codeAriRegImm(i, 0x34, sizeprefix, wordflag, reg0, imm);
				break;
			case I_ORregreg:
				if (sizeprefix)
					i.putByte(0x66);
				i.putByte(0x0A | wordflag);
				putRegReg(i, reg0, reg1);
				break;
			case I_TESTregreg:
				if (sizeprefix)
					i.putByte(0x66);
				i.putByte(0x84 | wordflag);
				putRegReg(i, reg0, reg1);
				break;
			case I_MULreg:
				if (sizeprefix)
					i.putByte(0x66);
				i.putByte(0xF6 | wordflag);
				i.putByte(0xE0 | (reg0 >>> 4));
				break;
			case I_IMULreg:
				if (sizeprefix)
					i.putByte(0x66);
				i.putByte(0xF6 | wordflag);
				i.putByte(0xE8 | (reg0 >>> 4));
				break;
			case I_IMULregreg:
				if (sizeprefix)
					i.putByte(0x66);
				i.putByte(0x0F);
				i.putByte(0xAF);
				putRegReg(i, reg0, reg1);
				break;
			case I_DIVreg:
				if (sizeprefix)
					i.putByte(0x66);
				i.putByte(0xF6 | wordflag);
				i.putByte(0xF0 | (reg0 >>> 4));
				break;
			case I_IDIVreg:
				if (sizeprefix)
					i.putByte(0x66);
				i.putByte(0xF6 | wordflag);
				i.putByte(0xF8 | (reg0 >>> 4));
				break;
			case I_INCreg:
				if (sizeprefix)
					i.putByte(0x66);
				i.putByte(0x40 | (reg0 >>> 4));
				break;
			case I_INCmem:
				i.type = I_NONE; //make instruction NOP and insert perhaps multiple instructions
				insMem(I_INCmem, sizeprefix, wordflag, 0, reg0, disp, 0, par);
				break;
			case I_DECreg:
				if (sizeprefix)
					i.putByte(0x66);
				i.putByte(0x48 | (reg0 >>> 4));
				break;
			case I_DECmem:
				i.type = I_NONE; //make instruction NOP and insert perhaps multiple instructions
				insMem(I_DECmem, sizeprefix, wordflag, 0, reg0, disp, 0, par);
				break;
			case I_NEGreg:
				if (sizeprefix)
					i.putByte(0x66);
				i.putByte(0xF6 | wordflag);
				i.putByte(0xD8 | (reg0 >>> 4));
				break;
			case I_NOTreg:
				if (sizeprefix)
					i.putByte(0x66);
				i.putByte(0xF6 | wordflag);
				i.putByte(0xD0 | (reg0 >>> 4));
				break;
			case I_CMPregimm:
				codeAriRegImm(i, 0x3C, sizeprefix, wordflag, reg0, imm);
				break;
			case I_CMPregreg:
				if (sizeprefix)
					i.putByte(0x66);
				i.putByte(0x3A | wordflag);
				putRegReg(i, reg0, reg1);
				break;
			case I_CMPregmem:
				i.type = I_NONE; //make instruction NOP and insert perhaps multiple instructions
				insMem(I_CMPregmem, sizeprefix, wordflag, reg0, reg1, disp, 0, 0);
				break;
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
				break;
			case I_SHLregreg:
				if (sizeprefix)
					i.putByte(0x66);
				i.putByte(0xD2 | wordflag);
				i.putByte(0xE0 | (reg0 >>> 4));
				break;
			case I_SHLreg1:
				if (sizeprefix)
					i.putByte(0x66);
				i.putByte(0xD0 | wordflag);
				i.putByte(0xE0 | (reg0 >>> 4));
				break;
			case I_SHRregreg:
				if (sizeprefix)
					i.putByte(0x66);
				i.putByte(0xD2 | wordflag);
				i.putByte(0xE8 | (reg0 >>> 4));
				break;
			case I_SHRregimm:
				codeShiftRegImm(i, 0xE8, sizeprefix, wordflag, reg0, imm);
				break;
			case I_SHLDregreg:
				if (sizeprefix)
					i.putByte(0x66);
				i.putByte(0x0F);
				i.putByte(0xA5);
				putRegReg(i, reg1, reg0); //shld has exchanged order of operands
				break;
			case I_SHRDregreg:
				if (sizeprefix)
					i.putByte(0x66);
				i.putByte(0x0F);
				i.putByte(0xAD);
				putRegReg(i, reg1, reg0); //shrd has exchanged order of operands
				break;
			case I_SARregreg:
				if (sizeprefix)
					i.putByte(0x66);
				i.putByte(0xD2 | wordflag);
				i.putByte(0xF8 | (reg0 >>> 4));
				break;
			case I_SARregimm:
				codeShiftRegImm(i, 0xF8, sizeprefix, wordflag, reg0, imm);
				break;
			case I_ROLregimm:
				codeShiftRegImm(i, 0xC0, sizeprefix, wordflag, reg0, imm);
				break;
			case I_RORregimm:
				codeShiftRegImm(i, 0xC8, sizeprefix, wordflag, reg0, imm);
				break;
			case I_RCRregimm:
				codeShiftRegImm(i, 0xD8, sizeprefix, wordflag, reg0, imm);
				break;
			case I_BSRregreg:
				if (sizeprefix)
					i.putByte(0x66);
				i.putByte(0x0F);
				i.putByte(0xBD);
				putRegReg(i, reg0, reg1);
				break;
			case I_CWDE:
				i.putByte(0x66);
				i.putByte(0x99);
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
			//special instructions
			case I_MOVindexed:
				i.putByte(0x8B); //mov bx,[bx+di]
				i.putByte(0x19);
				break;
			case I_BOUNDEXC:
			case I_RETMSEXC:
				i.putByte(0xB8); //mov ax,0x4CFE: terminate program, errorcode=254 or 253
				i.putByte(type == I_BOUNDEXC ? 0xFE : 0xFD);
				i.putByte(0x4C);
				i.putByte(0xCD); //INT 0x21
				i.putByte(0x21);
				break;
			case I_MARKER:
				i.putByte(0x90);
				break;
			default:
				fatalError("invalid instruction type for ins(.)");
				return null;
		}
		//everything ok
		return i;
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
	
	private void putRegReg(Instruction i, int reg0, int reg1)
	{
		i.putByte(0xC0 | ((reg0 >>> 4) << 3) | (reg1 >>> 4));
	}
	
	private void putMem(Instruction i, int reg0, int reg1, int disp)
	{
		int reg1code = 0;
		
		if (reg1 == 0)
		{ //absolute
			i.putByte(0x05 | ((reg0 >>> 4) << 3));
			i.putInt(disp);
		}
		else
		{ //relative to register
			if ((reg1 & RS_E) != RS_X)
			{
				fatalError("IA32RM: invalid1 reg1 in putMem");
				return;
			}
			switch (reg1)
			{
				case R_SI:
					reg1code = 0x4;
					break;
				case R_DI:
					reg1code = 0x5;
					break;
				case R_BP:
					reg1code = 0x6;
					break;
				case R_BX:
					reg1code = 0x7;
					break;
				default:
					fatalError("IA32RM: invalid2 reg1 in putMem");
					return;
			}
			if (disp == 0 && reg1 != R_BP)
			{ //no displacement
				i.putByte(((reg0 >>> 4) << 3) | reg1code);
			}
			else if (disp >= -128 && disp < 128)
			{ //8 bit displacement
				i.putByte(0x40 | ((reg0 >>> 4) << 3) | reg1code);
				i.putByte(disp);
			}
			else
			{ //16 bit displacement
				i.putByte(0x80 | ((reg0 >>> 4) << 3) | reg1code);
				i.putShort(disp);
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
}
