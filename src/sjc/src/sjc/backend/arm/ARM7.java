/* Copyright (C) 2010, 2011, 2012, 2015 Stefan Frenz
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

package sjc.backend.arm;

import sjc.backend.Architecture;
import sjc.backend.Instruction;
import sjc.compbase.Marks;
import sjc.compbase.Mthd;
import sjc.compbase.Ops;
import sjc.compbase.StdTypes;

/**
 * ARM7: Architecture backend for ARM7 processors
 *
 * @author S. Frenz
 * @version 150914 removed unneccessary createNewInstruction
 * version 120509 beautyfied and fixed genLoadDerefAddr, fixed genThrowFrameBuild and print
 * version 120501 added support for negative entrySizes in genLoadDerefAddr
 * version 110211 added support for stack extreme test and arithmetic shift right
 * version 101222 setting newly inserted needsAlignedVrbls
 * version 101210 adopted changed Architecture
 * version 101207 added support for programmed exceptions
 * version 101111 added more functions, fixed some bugs
 * version 101101 adopted changed Architecture
 * version 101027 added more functions, fixed some bugs
 * version 101024 restructured instruction coding, added more functions
 * version 101021 added more functions
 * version 101020 added more functions
 * version 101018 added more functions, prepared "nice" instruction encoding
 * version 101016 re-ordered registers, added better load strategy
 * version 101015 added temporary load/store routines
 * version 100929 added basic register management
 * version 100805 initial version
 */

public class ARM7 extends Architecture
{
	public final static String ERR_INVREGTHR = "invalid register at start of genThrow*";
	
	private final static int RegBASE = 9;
	private final static int RegCLSS = 10;
	private final static int RegINST = 11;
	private final static int RegHLP = 12;
	private final static int RegSP = 13;
	private final static int RegLR = 14; //may be used as secondary helper register
	private final static int RegPC = 15;
	private final static int RegALL = 0x01FF; //mask for general purpose registers (0..8)
	
	//instruction types
	public final static int IT_FIX = 1;
	public final static int IT_LITERAL = 2;
	public final static int IT_BRANCH = 3;
	public final static int IT_CALLpatched = 4;
	public final static int IT_LDR_LIT = 5;
	public final static int IT_ADDpatched = 6;
	public final static int IT_HELPER = 7;
	public final static int IT_STEX = 8;
	//condition codes
	public final static int IC_EQ = 0x00000000; //equal
	public final static int IC_NE = 0x10000000; //not equal
	public final static int IC_CS = 0x20000000; //carry set / unsigned higher or same
	public final static int IC_CC = 0x30000000; //carry clear / unsigned lower
	public final static int IC_MI = 0x40000000; //minus / negative
	public final static int IC_PL = 0x50000000; //plus / positive or zero
	public final static int IC_VS = 0x60000000; //overflow
	public final static int IC_VC = 0x70000000; //no overflow
	public final static int IC_HI = 0x80000000; //unsigned higher
	public final static int IC_LS = 0x90000000; //unsigned lower or same
	public final static int IC_GE = 0xA0000000; //signed greater or equal
	public final static int IC_LT = 0xB0000000; //signed less
	public final static int IC_GT = 0xC0000000; //signed greater
	public final static int IC_LE = 0xD0000000; //signed less than or equal
	public final static int IC_AL = 0xE0000000; //always (unconditional)
	//instruction opcodes
	public final static int IOB_BRANCHwoLink = 0x0A000000;
	public final static int IOB_BRANCHwtLink = 0x0B000000;
	public final static int IOBT_STMFDu = 0x09200000;
	public final static int IOBT_LDMFDu = 0x08B00000;
	public final static int IOST_LDRi32 = 0x05900000;
	public final static int IOST_LDRu08 = 0x05D00000;
	public final static int IOST_STRi32 = 0x05800000;
	public final static int IOST_STRu08 = 0x05C00000;
	public final static int IOSR_LDRSB = 0x01D000D0;
	public final static int IOSR_LDRSH = 0x01D000F0;
	public final static int IOSR_LDRZH = 0x01D000B0;
	public final static int IOSR_STRH = 0x01C000B0;
	public final static int IOD_MOV = 0x01A00000;
	public final static int IOD_MVN = 0x01E00000;
	public final static int IOD_ADD = 0x00800000;
	public final static int IOD_ADC = 0x00A00000;
	public final static int IOD_SUB = 0x00400000;
	public final static int IOD_SBC = 0x00C00000;
	public final static int IOD_RSB = 0x00600000;
	public final static int IOD_RSC = 0x00E00000;
	public final static int IOD_AND = 0x00000000;
	public final static int IOD_OR = 0x01800000;
	public final static int IOD_XOR = 0x00200000;
	public final static int IOD_CMPupd = 0x01500000; //update flags is already set here
	public final static int IOM_MUL = 0x00000090;
	public final static int IOM_MULADD = 0x00200090;
	public final static int IOML_UMULL = 0x00800090;
	//shift-types for IOD_*
	public final static int IOD_M_LSLimm = 0x00000000;
	public final static int IOD_M_LSRimm = 0x00000020;
	public final static int IOD_M_ASRimm = 0x00000040;
	public final static int IOD_M_RORimm = 0x00000060;
	public final static int IOD_M_LSLreg = 0x00000010;
	public final static int IOD_M_LSRreg = 0x00000030;
	public final static int IOD_M_ASRreg = 0x00000050;
	public final static int IOD_M_RORreg = 0x00000070;
	//update flags modifier for IOD_*
	public final static int IOD_M_updFlags = 0x00100000;
	//we use STMFD / LDMFD to get a full descending stack (pointing to last full element)
	
	private int usedRegs, writtenRegs, nextAllocReg;
	private Mthd mthdContainer;
	private Instruction literals, lastLiteral;
	
	public ARM7()
	{
		relocBytes = 4;
		stackClearBits = 3;
		allocClearBits = 3;
		maxInstrCodeSize = 8; //reserve space for two instructions, needed by insPatchedAdd
		throwFrameSize = relocBytes * 7;
		throwFrameExcOff = relocBytes * 6;
		regClss = 1 << RegCLSS;
		regInst = 1 << RegINST;
		regBase = 1 << RegBASE;
		needsAlignedVrbls = true;
		binAriCall[StdTypes.T_BYTE] |= (1 << (Ops.A_DIV - Ops.MSKBSE)) | (1 << (Ops.A_MOD - Ops.MSKBSE));
		binAriCall[StdTypes.T_SHRT] |= (1 << (Ops.A_DIV - Ops.MSKBSE)) | (1 << (Ops.A_MOD - Ops.MSKBSE));
		binAriCall[StdTypes.T_INT] |= (1 << (Ops.A_DIV - Ops.MSKBSE)) | (1 << (Ops.A_MOD - Ops.MSKBSE));
		binAriCall[StdTypes.T_LONG] |= (1 << (Ops.A_DIV - Ops.MSKBSE)) | (1 << (Ops.A_MOD - Ops.MSKBSE));
		binAriCall[StdTypes.T_FLT] |= (1 << (Ops.A_MUL - Ops.MSKBSE)) | (1 << (Ops.A_DIV - Ops.MSKBSE)) | (1 << (Ops.A_MOD - Ops.MSKBSE)) | (1 << (Ops.A_PLUS - Ops.MSKBSE)) | (1 << (Ops.A_MINUS - Ops.MSKBSE));
		binAriCall[StdTypes.T_DBL] |= (1 << (Ops.A_MUL - Ops.MSKBSE)) | (1 << (Ops.A_DIV - Ops.MSKBSE)) | (1 << (Ops.A_MOD - Ops.MSKBSE)) | (1 << (Ops.A_PLUS - Ops.MSKBSE)) | (1 << (Ops.A_MINUS - Ops.MSKBSE));
		unaAriCall[StdTypes.T_FLT] |= (1 << (Ops.A_MINUS - Ops.MSKBSE));
		unaAriCall[StdTypes.T_DBL] |= (1 << (Ops.A_MINUS - Ops.MSKBSE));
	}
	
	public void putRef(Object loc, int offset, Object ptr, int ptrOff)
	{
		mem.putInt(loc, offset, mem.getAddrAsInt(ptr, ptrOff));
	}
	
	public void putCodeRef(Object loc, int offset, Object ptr, int ptrOff)
	{
		int destAddr = mem.getAddrAsInt(loc, offset);
		//encode a relative branch including opcode with link
		mem.putInt(loc, offset, IC_AL | IOB_BRANCHwtLink | (((mem.getAddrAsInt(ptr, ptrOff) - destAddr - 8) >>> 2) & 0xFFFFFF));
	}
	
	protected int bitSearch(int value, int hit, int prefere)
	{
		int i, j;
		
		//first: try to search in prefered ones
		prefere &= value;
		if (prefere != 0 && prefere != value)
			for (i = 0; i < 16; i++)
			{ //search here only if there is a difference
				j = 1 << i;
				if ((prefere & j) != 0)
				{
					if (--hit == 0)
						return j;
				}
			}
		//nothing prefered, try in all possible
		for (i = 0; i < 16; i++)
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
	
	protected int freeRegSearch(int mask, int type)
	{
		int ret, ret2;
		
		mask &= RegALL;
		if ((ret = bitSearch(mask, 1, 0)) == 0)
			return 0;
		if (type == StdTypes.T_LONG || type == StdTypes.T_DPTR)
		{
			if ((ret2 = bitSearch(mask & ~ret, 1, 0)) == 0)
				return 0;
			ret |= ret2;
		}
		return ret;
	}
	
	protected int storeReg(int regs)
	{
		regs &= usedRegs & writtenRegs;
		if (regs != 0)
			insBlockTransfer(IC_AL, IOBT_STMFDu, RegSP, regs);
		return regs;
	}
	
	protected void restoreReg(int regs)
	{
		usedRegs |= regs;
		writtenRegs |= regs;
		if (regs != 0)
			insBlockTransfer(IC_AL, IOBT_LDMFDu, RegSP, regs);
	}
	
	public int ensureFreeRegs(int ignoreReg1, int ignoreReg2, int keepReg1, int keepReg2)
	{
		int restore = storeReg(RegALL & ~(ignoreReg1 | ignoreReg2));
		usedRegs = (keepReg1 | keepReg2) & RegALL;
		return restore;
	}
	
	public int prepareFreeReg(int avoidReg1, int avoidReg2, int reUseReg, int type)
	{
		int toStore, ret;
		//first: try to reuse given regs
		reUseReg &= RegALL;
		if (reUseReg != 0)
		{
			if ((ret = freeRegSearch(reUseReg, type)) != 0)
			{
				usedRegs |= (nextAllocReg = ret);
				return 0; //nothing has to be freed, reuse given registers
			}
		}
		//second: try to alloc everywhere normally
		if ((ret = freeRegSearch((RegALL & ~usedRegs & ~(avoidReg1 | avoidReg2)) | reUseReg, type)) != 0)
		{
			usedRegs |= (nextAllocReg = ret);
			return 0; //nothing has to be freed, use newly allocated registers
		}
		//third: try to free a register
		if ((ret = freeRegSearch((RegALL & ~(avoidReg1 | avoidReg2)) | reUseReg, type)) != 0)
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
		usedRegs |= (keepRegs | restore) & RegALL;
	}
	
	protected int getReg(int nr, int reg, boolean firstWrite)
	{
		if (nr < 1 || nr > 2)
		{
			fatalError("invalid call to getReg");
			return -1;
		}
		reg = bitSearch(reg, nr, 0);
		if (firstWrite)
		{
			writtenRegs |= reg & RegALL;
			usedRegs |= reg & RegALL;
		}
		if (reg != 0)
		{
			return getBitPos(reg);
		}
		fatalError("register not found in getReg");
		return -1;
	}
	
	public void finalizeInstructions(Instruction first)
	{
		Instruction now;
		boolean redo, hasAddPatched = false;
		
		//fix jumps and stex (potentially multiple times)
		do
		{
			now = first;
			redo = false;
			while (now != null)
			{
				switch (now.type)
				{
					case IT_BRANCH:
						redo |= fixBranch(now);
						break;
					case IT_ADDpatched:
						redo |= fixAddPatched(now, false); //do not insert literals so far, may be they change or are not used
						now = now.next; //skip helper after ADDpatched
						hasAddPatched = true;
						break;
					case IT_STEX:
						fixStex(now);
						break;
				}
				now = now.next;
			}
		} while (redo);
		//handle literals (only once) and finalize patched add
		if (literals != null || hasAddPatched)
		{
			if (literals != null)
			{
				//append literals
				appendInstruction(literals);
				//enumerate them
				enumerateInstructions();
			}
			//handle corresponding ldr instructions
			now = first;
			while (now != null)
			{
				switch (now.type)
				{
					case IT_LDR_LIT:
						fixLoadLit(now);
						break;
					case IT_ADDpatched:
						fixAddPatched(now, true); //now insert literal if it is neccessary
						break;
				}
				now = now.next;
			}
			//dismiss literals, cleanup is done by instruction list cleanup
			lastLiteral = literals = null; //literals are no longer literals
		}
		//print disassembly if wanted
		if (ctx.printCode || (mthdContainer.marker & Marks.K_PRCD) != 0)
			printCode(first, "ARM-out");
	}
	
	public Mthd prepareMethodCoding(Mthd mthd)
	{
		Mthd lastMthd;
		
		if ((lastMthd = curMthd) != null)
			curInlineLevel++;
		else
			mthdContainer = mthd; //remember outest level method
		curMthd = mthd;
		insBlockTransfer(IC_AL, IOBT_STMFDu, RegSP, (1 << RegBASE) | (1 << RegLR)); //need to push before potential stack extreme check occurs, see genCheckStackExtreme
		return lastMthd;
	}
	
	public void codeProlog()
	{
		usedRegs = 0;
		if (curMthd.varSize > 0 || curMthd.parSize > 0)
			insDataReg(IC_AL, IOD_MOV, RegBASE, 0, RegSP);
		if (curMthd.varSize > 0)
		{
			insDataImm(IC_AL, IOD_MOV, RegHLP, 0, 0, 0);
			if (curMthd.varSize == 4)
				insBlockTransfer(IC_AL, IOBT_STMFDu, RegSP, 1 << RegHLP);
			else
			{
				if (curMthd.varSize == 8)
				{
					insBlockTransfer(IC_AL, IOBT_STMFDu, RegSP, 1 << RegHLP);
					insBlockTransfer(IC_AL, IOBT_STMFDu, RegSP, 1 << RegHLP);
				}
				else
				{
					insDataImm(IC_AL, IOD_MOV, 0, 0, curMthd.varSize >>> 2, 0);
					Instruction redo = getUnlinkedInstruction();
					appendInstruction(redo);
					insBlockTransfer(IC_AL, IOBT_STMFDu, RegSP, 1 << RegHLP);
					insDataImm(IC_AL, IOD_SUB | IOD_M_updFlags, 0, 0, 1, 0);
					insBranch(IC_NE, redo);
				}
			}
		}
	}
	
	public void codeEpilog(Mthd outline)
	{
		if (curMthd.varSize > 0)
			insDataImm(IC_AL, IOD_ADD, RegSP, RegSP, curMthd.varSize, 0);
		insBlockTransfer(IC_AL, IOBT_LDMFDu, RegSP, (1 << RegBASE) | (1 << RegLR));
		if (curMthd.parSize > 0)
		{
			if (curMthd.parSize > 0xFF)
				fatalError("too many parameters");
			insDataImm(IC_AL, IOD_ADD, RegSP, RegSP, curMthd.parSize, 0);
		}
		insDataReg(IC_AL, IOD_MOV, RegPC, 0, RegLR);
		if (curInlineLevel > 0)
		{ //end of method inlining
			curMthd = outline;
			return;
		}
		//normal end of method
		curMthd = null;
	}
	
	public void genLoadConstVal(int dst, int val, int type)
	{
		if ((dst = getReg(1, dst, true)) == -1)
			return;
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
			//nothing to do for others (T_INT and T_FLT)
		}
		insMoveConst(IC_AL, dst, val, 0);
	}
	
	public void genLoadConstDoubleOrLongVal(int dst, long val, boolean asDouble)
	{
		int dst1, dst2;
		if ((dst1 = getReg(1, dst, true)) == -1 || (dst2 = getReg(2, dst, true)) == -1)
			return;
		insMoveConst(IC_AL, dst1, (int) val, 0);
		insMoveConst(IC_AL, dst2, (int) (val >>> 32), 0);
	}
	
	public void genLoadVarAddr(int dst, int src, Object loc, int off)
	{
		int dstReg, srcReg;
		off = mem.getAddrAsInt(loc, off);
		if ((dstReg = getReg(1, dst, false)) == -1)
			return;
		if (src == 0)
			genLoadConstVal(dst, off, StdTypes.T_INT);
		else
		{
			if (src == 1 << RegBASE && off >= 0)
				off += 8; //skip old base pointer and old pc
			if ((srcReg = getReg(1, src, false)) == -1)
				return;
			if (off == 0)
			{
				if (src == dst)
					return; //nothing to do
				insDataReg(IC_AL, IOD_MOV, dstReg, 0, srcReg);
			}
			else
			{ //off!=0
				if (off > 0)
					insDataImm(IC_AL, IOD_ADD, dstReg, srcReg, off, 0);
				else
					insDataImm(IC_AL, IOD_SUB, dstReg, srcReg, -off, 0);
			}
		}
	}
	
	public void genLoadVarVal(int dstReg, int src, Object loc, int off, int type)
	{
		int srcReg, dst2Reg;
		if (src == 0)
		{ //absolute address
			srcReg = RegHLP;
			insMoveConst(IC_AL, RegHLP, mem.getAddrAsInt(loc, off), 0);
			off = 0;
		}
		else
		{ //relative to register
			if (src == 1 << RegBASE && off >= 0)
				off += 8; //go behind old base pointer and old pc on stack
			if ((srcReg = getReg(1, src, true)) == -1)
				return;
		}
		switch (type)
		{
			case StdTypes.T_BOOL:
				if ((dstReg = getReg(1, dstReg, true)) == -1)
					return;
				insSingleTransfer(IC_AL, IOST_LDRu08, dstReg, srcReg, off);
				return;
			case StdTypes.T_BYTE:
				if ((dstReg = getReg(1, dstReg, true)) == -1)
					return;
				insSingleReducedTransfer(IC_AL, IOSR_LDRSB, dstReg, srcReg, off);
				return;
			case StdTypes.T_SHRT:
				if ((dstReg = getReg(1, dstReg, true)) == -1)
					return;
				insSingleReducedTransfer(IC_AL, IOSR_LDRSH, dstReg, srcReg, off);
				return;
			case StdTypes.T_CHAR:
				if ((dstReg = getReg(1, dstReg, true)) == -1)
					return;
				insSingleReducedTransfer(IC_AL, IOSR_LDRZH, dstReg, srcReg, off);
				return;
			case StdTypes.T_LONG:
				if ((dst2Reg = getReg(2, dstReg, true)) == -1)
					return;
				insSingleTransfer(IC_AL, IOST_LDRi32, dst2Reg, srcReg, off + 4);
				//has to do the following, too
			case StdTypes.T_INT:
			case StdTypes.T_PTR:
				if ((dstReg = getReg(1, dstReg, true)) == -1)
					return;
				insSingleTransfer(IC_AL, IOST_LDRi32, dstReg, srcReg, off);
				return;
		}
		fatalError("unsupported case in genLoadVarVal");
	}
	
	public void genConvertVal(int dst, int src, int toType, int fromType)
	{
		int dstReg, dst2Reg, srcReg, src2Reg;
		switch (toType)
		{
			case StdTypes.T_LONG:
				switch (fromType)
				{
					case StdTypes.T_LONG:
						if ((dst2Reg = getReg(2, dst, true)) == -1 || (dstReg = getReg(2, dst, true)) == -1 || (src2Reg = getReg(2, src, false)) == -1 || (srcReg = getReg(1, src, false)) == -1)
							return;
						if (dstReg != srcReg)
							insDataReg(IC_AL, IOD_MOV, dstReg, 0, srcReg);
						if (dst2Reg != src2Reg)
							insDataReg(IC_AL, IOD_MOV, dst2Reg, 0, src2Reg);
						return;
					case StdTypes.T_BYTE:
					case StdTypes.T_SHRT:
					case StdTypes.T_CHAR:
					case StdTypes.T_INT:
						if ((dst2Reg = getReg(2, dst, true)) == -1 || (dstReg = getReg(2, dst, true)) == -1 || (srcReg = getReg(1, src, false)) == -1)
							return;
						if (srcReg != dstReg)
							insDataReg(IC_AL, IOD_MOV, dstReg, 0, srcReg);
						insDataReg(IC_AL, IOD_MOV, dst2Reg, 0, srcReg, 31, IOD_M_ASRimm);
						return;
				}
				break; //error
			case StdTypes.T_PTR:
				switch (fromType)
				{
					case StdTypes.T_INT:
					case StdTypes.T_PTR:
						if ((dstReg = getReg(1, dst, true)) == -1 || (srcReg = getReg(1, src, false)) == -1)
							return;
						if (dstReg != srcReg)
							insDataReg(IC_AL, IOD_MOV, dstReg, 0, srcReg);
						return;
				}
				break; //error
			case StdTypes.T_INT:
				switch (fromType)
				{
					case StdTypes.T_LONG:
					case StdTypes.T_INT:
					case StdTypes.T_PTR:
					case StdTypes.T_BYTE:
					case StdTypes.T_SHRT:
					case StdTypes.T_CHAR:
						if ((dstReg = getReg(1, dst, true)) == -1 || (srcReg = getReg(1, src, false)) == -1)
							return;
						if (dstReg != srcReg)
							insDataReg(IC_AL, IOD_MOV, dstReg, 0, srcReg);
						return;
				}
				break; //error
			case StdTypes.T_SHRT:
				switch (fromType)
				{
					case StdTypes.T_LONG:
					case StdTypes.T_BYTE:
					case StdTypes.T_SHRT:
						if ((dstReg = getReg(1, dst, true)) == -1 || (srcReg = getReg(1, src, false)) == -1)
							return;
						if (dstReg != srcReg)
							insDataReg(IC_AL, IOD_MOV, dstReg, 0, srcReg);
						return;
					case StdTypes.T_CHAR:
					case StdTypes.T_INT:
						if ((dstReg = getReg(1, dst, true)) == -1 || (srcReg = getReg(1, src, false)) == -1)
							return;
						insDataReg(IC_AL, IOD_MOV, dstReg, 0, srcReg, 16, IOD_M_LSLimm);
						insDataReg(IC_AL, IOD_MOV, dstReg, 0, dstReg, 16, IOD_M_ASRimm);
						return;
				}
				break; //error
			case StdTypes.T_CHAR:
				switch (fromType)
				{
					case StdTypes.T_CHAR:
						if ((dstReg = getReg(1, dst, true)) == -1 || (srcReg = getReg(1, src, false)) == -1)
							return;
						if (dstReg != srcReg)
							insDataReg(IC_AL, IOD_MOV, dstReg, 0, srcReg);
						return;
					case StdTypes.T_LONG:
					case StdTypes.T_BYTE:
					case StdTypes.T_SHRT:
					case StdTypes.T_INT:
						if ((dstReg = getReg(1, dst, true)) == -1 || (srcReg = getReg(1, src, false)) == -1)
							return;
						insDataReg(IC_AL, IOD_MOV, dstReg, 0, srcReg, 16, IOD_M_LSLimm);
						insDataReg(IC_AL, IOD_MOV, dstReg, 0, dstReg, 16, IOD_M_LSRimm);
						return;
				}
				break; //error
			case StdTypes.T_BYTE:
				switch (fromType)
				{
					case StdTypes.T_BYTE:
						if ((dstReg = getReg(1, dst, true)) == -1 || (srcReg = getReg(1, src, false)) == -1)
							return;
						if (dstReg != srcReg)
							insDataReg(IC_AL, IOD_MOV, dstReg, 0, srcReg);
						return;
					case StdTypes.T_LONG:
					case StdTypes.T_SHRT:
					case StdTypes.T_CHAR:
					case StdTypes.T_INT:
						if ((dstReg = getReg(1, dst, true)) == -1 || (srcReg = getReg(1, src, false)) == -1)
							return;
						insDataReg(IC_AL, IOD_MOV, dstReg, 0, srcReg, 24, IOD_M_LSLimm);
						insDataReg(IC_AL, IOD_MOV, dstReg, 0, dstReg, 24, IOD_M_ASRimm);
						return;
				}
				break; //error
		}
		fatalError("unsupported type in genConvertVal");
	}
	
	public void genDup(int dst, int src, int type)
	{
		int dstReg, srcReg;
		if ((dstReg = getReg(1, dst, true)) == -1 || (srcReg = getReg(1, src, false)) == -1)
			return;
		if (dstReg != srcReg)
			insDataReg(IC_AL, IOD_MOV, dstReg, 0, srcReg);
		switch (type)
		{
			case StdTypes.T_LONG:
			case StdTypes.T_DPTR:
				if ((dstReg = getReg(2, dst, true)) == -1 || (srcReg = getReg(2, src, false)) == -1)
					return;
				if (dstReg != srcReg)
					insDataReg(IC_AL, IOD_MOV, dstReg, 0, srcReg);
		}
	}
	
	public void genPushConstVal(int val, int type)
	{
		insMoveConst(IC_AL, RegHLP, val, 0);
		insBlockTransfer(IC_AL, IOBT_STMFDu, RegSP, 1 << RegHLP);
	}
	
	public void genPushConstDoubleOrLongVal(long val, boolean asDouble)
	{
		insMoveConst(IC_AL, RegHLP, (int) val, 0);
		insMoveConst(IC_AL, RegLR, (int) (val >>> 32), 0);
		insBlockTransfer(IC_AL, IOBT_STMFDu, RegSP, (1 << RegHLP) | (1 << RegLR));
	}
	
	public void genPush(int src, int type)
	{
		insBlockTransfer(IC_AL, IOBT_STMFDu, RegSP, src); //src is already a mask
	}
	
	public void genPop(int dst, int type)
	{
		insBlockTransfer(IC_AL, IOBT_LDMFDu, RegSP, dst); //dst is already a mask
	}
	
	public void genAssign(int dst, int srcReg, int type)
	{
		int src, src2;
		if ((dst = getReg(1, dst, false)) == -1 || (src = getReg(1, srcReg, false)) == -1)
			return;
		switch (type)
		{
			case StdTypes.T_BOOL:
			case StdTypes.T_BYTE:
				insSingleTransfer(IC_AL, IOST_STRu08, src, dst, 0);
				return;
			case StdTypes.T_CHAR:
			case StdTypes.T_SHRT:
				insSingleReducedTransfer(IC_AL, IOSR_STRH, src, dst, 0);
				return;
			case StdTypes.T_LONG:
				if ((src2 = getReg(2, srcReg, false)) == -1)
					return;
				insSingleTransfer(IC_AL, IOST_STRi32, src2, dst, 4);
				//has to do INT, too
			case StdTypes.T_INT:
			case StdTypes.T_PTR:
				insSingleTransfer(IC_AL, IOST_STRi32, src, dst, 0);
				return;
		}
		fatalError("unsupported type in genAssign ");
	}
	
	public void genBinOp(int dst, int src1, int src2, int op, int type)
	{
		Instruction t1, t2;
		int dstHi, src1Hi, src2Hi = 0;
		switch (type)
		{
			case StdTypes.T_BOOL:
			case StdTypes.T_BYTE:
			case StdTypes.T_SHRT:
			case StdTypes.T_CHAR:
			case StdTypes.T_INT:
				if ((dst = getReg(1, dst, true)) == -1 || (src1 = getReg(1, src1, false)) == -1 || (src2 = getReg(1, src2, false)) == -1)
					return;
				switch (op)
				{
					case (Ops.S_ARI << 16) | Ops.A_PLUS:
						insDataReg(IC_AL, IOD_ADD, dst, src1, src2);
						break;
					case (Ops.S_ARI << 16) | Ops.A_MINUS:
						insDataReg(IC_AL, IOD_SUB, dst, src1, src2);
						break;
					case (Ops.S_ARI << 16) | Ops.A_MUL:
						if (src2 == dst)
						{
							insDataReg(IC_AL, IOD_MOV, RegHLP, 0, src2);
							src2 = RegHLP;
						}
						insMul(IC_AL, IOM_MUL, dst, src1, src2, 0);
						break;
					case (Ops.S_ARI << 16) | Ops.A_AND:
						insDataReg(IC_AL, IOD_AND, dst, src1, src2);
						break;
					case (Ops.S_ARI << 16) | Ops.A_OR:
						insDataReg(IC_AL, IOD_OR, dst, src1, src2);
						break;
					case (Ops.S_ARI << 16) | Ops.A_XOR:
						insDataReg(IC_AL, IOD_XOR, dst, src1, src2);
						break;
					case (Ops.S_BSH << 16) | Ops.B_SHL:
						insDataReg(IC_AL, IOD_MOV, dst, 0, src1, src2, IOD_M_LSLreg);
						break;
					case (Ops.S_BSH << 16) | Ops.B_SHRL:
						insDataReg(IC_AL, IOD_MOV, dst, 0, src1, src2, IOD_M_LSRreg);
						break;
					case (Ops.S_BSH << 16) | Ops.B_SHRA:
						insDataReg(IC_AL, IOD_MOV, dst, 0, src1, src2, IOD_M_ASRreg);
						break;
					default:
						fatalError("unsupported integer op");
						return;
				}
				switch (type)
				{ //check sign-bits
					case StdTypes.T_BYTE:
						insDataReg(IC_AL, IOD_MOV, dst, 0, dst, 24, IOD_M_LSLimm);
						insDataReg(IC_AL, IOD_MOV, dst, 0, dst, 24, IOD_M_ASRimm);
						break;
					case StdTypes.T_SHRT:
						insDataReg(IC_AL, IOD_MOV, dst, 0, dst, 16, IOD_M_LSLimm);
						insDataReg(IC_AL, IOD_MOV, dst, 0, dst, 16, IOD_M_ASRimm);
						break;
					case StdTypes.T_CHAR:
						insDataReg(IC_AL, IOD_MOV, dst, 0, dst, 16, IOD_M_LSLimm);
						insDataReg(IC_AL, IOD_MOV, dst, 0, dst, 16, IOD_M_LSRimm);
						break;
				}
				return;
			case StdTypes.T_LONG:
				if ((dstHi = getReg(2, dst, true)) == -1 || (dst = getReg(1, dst, true)) == -1 || (src1Hi = getReg(2, src1, false)) == -1 || (src1 = getReg(1, src1, false)) == -1)
					return;
				if ((op >>> 16) != Ops.S_BSH && (src2Hi = getReg(2, src2, false)) == -1)
					return;
				if ((src2 = getReg(1, src2, false)) == -1)
					return;
				switch (op)
				{
					case (Ops.S_ARI << 16) | Ops.A_PLUS:
						insDataReg(IC_AL, IOD_ADD | IOD_M_updFlags, dst, src1, src2);
						insDataReg(IC_AL, IOD_ADC, dstHi, src1Hi, src2Hi);
						break;
					case (Ops.S_ARI << 16) | Ops.A_MINUS:
						insDataReg(IC_AL, IOD_SUB | IOD_M_updFlags, dst, src1, src2);
						insDataReg(IC_AL, IOD_SBC, dstHi, src1Hi, src2Hi);
						break;
					case (Ops.S_ARI << 16) | Ops.A_MUL:
						insMul(IC_AL, IOM_MUL, RegHLP, src1, src2Hi, 0);
						insMul(IC_AL, IOM_MULADD, RegHLP, src1Hi, src2, RegHLP);
						insMulLong(IC_AL, IOML_UMULL, dstHi, dst, src1, src2);
						insDataReg(IC_AL, IOD_ADD, dstHi, dstHi, RegHLP);
						break;
					case (Ops.S_ARI << 16) | Ops.A_AND:
						insDataReg(IC_AL, IOD_AND, dst, src1, src2);
						insDataReg(IC_AL, IOD_AND, dstHi, src1Hi, src2Hi);
						break;
					case (Ops.S_ARI << 16) | Ops.A_OR:
						insDataReg(IC_AL, IOD_OR, dst, src1, src2);
						insDataReg(IC_AL, IOD_OR, dstHi, src1Hi, src2Hi);
						break;
					case (Ops.S_ARI << 16) | Ops.A_XOR:
						insDataReg(IC_AL, IOD_XOR, dst, src1, src2);
						insDataReg(IC_AL, IOD_XOR, dstHi, src1Hi, src2Hi);
						break;
					case (Ops.S_BSH << 16) | Ops.B_SHL:
						t1 = getUnlinkedInstruction();
						t2 = getUnlinkedInstruction();
						insDataImm(IC_AL, IOD_CMPupd, 0, src2, 32, 0);
						insDataReg(IC_CS, IOD_MOV, dstHi, 0, src1);
						insDataImm(IC_CS, IOD_MOV, dst, 0, 0, 0);
						insBranch(IC_EQ, t2);
						insBranch(IC_HI, t1);
						//less than 32 bits to shift, nothing done so far
						insDataReg(IC_AL, IOD_MOV, dstHi, 0, src1Hi, src2, IOD_M_LSLreg);
						insDataReg(IC_AL, IOD_MOV, RegHLP, 0, src1);
						insDataReg(IC_AL, IOD_MOV, dst, 0, src1, src2, IOD_M_LSLreg);
						insDataImm(IC_AL, IOD_RSB, src2, src2, 32, 0);
						insDataReg(IC_AL, IOD_OR, dstHi, dstHi, RegHLP, src2, IOD_M_LSRreg);
						insBranch(IC_AL, t2);
						//more than 32 bits to shift, lower 32 bits already cleared
						appendInstruction(t1);
						insDataImm(IC_AL, IOD_SUB, src2, src2, 32, 0);
						insDataReg(IC_AL, IOD_MOV, dstHi, 0, dstHi, src2, IOD_M_LSLreg);
						//done
						appendInstruction(t2);
						break;
					case (Ops.S_BSH << 16) | Ops.B_SHRL:
						t1 = getUnlinkedInstruction();
						t2 = getUnlinkedInstruction();
						insDataImm(IC_AL, IOD_CMPupd, 0, src2, 32, 0);
						insDataReg(IC_CS, IOD_MOV, dst, 0, src1Hi);
						insDataImm(IC_CS, IOD_MOV, dstHi, 0, 0, 0);
						insBranch(IC_EQ, t2);
						insBranch(IC_HI, t1);
						//less than 32 bits to shift, nothing done so far
						insDataReg(IC_AL, IOD_MOV, dst, 0, src1, src2, IOD_M_LSRreg);
						insDataReg(IC_AL, IOD_MOV, RegHLP, 0, src1Hi);
						insDataReg(IC_AL, IOD_MOV, dstHi, 0, src1Hi, src2, IOD_M_LSRreg);
						insDataImm(IC_AL, IOD_RSB, src2, src2, 32, 0);
						insDataReg(IC_AL, IOD_OR, dst, dst, RegHLP, src2, IOD_M_LSLreg);
						insBranch(IC_AL, t2);
						//more than 32 bits to shift, upper 32 bits already cleared
						appendInstruction(t1);
						insDataImm(IC_AL, IOD_SUB, src2, src2, 32, 0);
						insDataReg(IC_AL, IOD_MOV, dst, 0, dst, src2, IOD_M_LSRreg);
						//done
						appendInstruction(t2);
						break;
					case (Ops.S_BSH << 16) | Ops.B_SHRA:
						t1 = getUnlinkedInstruction();
						t2 = getUnlinkedInstruction();
						insDataImm(IC_AL, IOD_CMPupd, 0, src2, 32, 0);
						insDataReg(IC_CS, IOD_MOV, dst, 0, src1Hi);
						insDataImm(IC_CS, IOD_MOV, dstHi, 0, 0, 0);
						insBranch(IC_EQ, t2);
						insBranch(IC_HI, t1);
						//less than 32 bits to shift, nothing done so far
						insDataReg(IC_AL, IOD_MOV, dst, 0, src1, src2, IOD_M_LSRreg);
						insDataReg(IC_AL, IOD_MOV, RegHLP, 0, src1Hi);
						insDataReg(IC_AL, IOD_MOV, dstHi, 0, src1Hi, src2, IOD_M_ASRreg);
						insDataImm(IC_AL, IOD_RSB, src2, src2, 32, 0);
						insDataReg(IC_AL, IOD_OR, dst, dst, RegHLP, src2, IOD_M_LSLreg);
						insBranch(IC_AL, t2);
						//more than 32 bits to shift, upper 32 bits already cleared
						appendInstruction(t1);
						insDataImm(IC_AL, IOD_SUB, src2, src2, 32, 0);
						insDataReg(IC_AL, IOD_MOV | IOD_M_updFlags, dst, 0, dst, src2, IOD_M_ASRreg);
						insDataImm(IC_MI, IOD_SUB, dstHi, dstHi, 1, 0);
						//done
						appendInstruction(t2);
						break;
					default:
						fatalError("unsupported long op");
						return;
				}
				return;
		}
		fatalError("unsupported type or op in genBinOp");
	}
	
	public void genUnaOp(int dst, int src, int op, int type)
	{
		int dstHi, srcHi;
		switch (type)
		{
			case StdTypes.T_BOOL:
				if ((dst = getReg(1, dst, true)) == -1 || (src = getReg(1, src, false)) == -1)
					return;
				switch (op)
				{
					case (Ops.S_LOG << 16) | Ops.L_NOT:
						insDataImm(IC_AL, IOD_XOR, dst, src, 1, 0);
						return;
				}
				break;
			case StdTypes.T_BYTE:
			case StdTypes.T_SHRT:
			case StdTypes.T_CHAR:
			case StdTypes.T_INT:
				if ((dst = getReg(1, dst, true)) == -1 || (src = getReg(1, src, false)) == -1)
					return;
				switch (op)
				{
					case (Ops.S_ARI << 16) | Ops.A_CPL:
						insDataReg(IC_AL, IOD_MVN, dst, 0, src);
						return;
					case (Ops.S_ARI << 16) | Ops.A_MINUS:
						insDataImm(IC_AL, IOD_RSB, dst, src, 0, 0);
						return;
					case (Ops.S_ARI << 16) | Ops.A_PLUS:
						if (dst != src)
							insDataReg(IC_AL, IOD_MOV, dst, 0, src);
						return;
				}
				break;
			case StdTypes.T_LONG:
				if ((dstHi = getReg(2, dst, true)) == -1 || (srcHi = getReg(2, src, false)) == -1 || (dst = getReg(1, dst, true)) == -1 || (src = getReg(1, src, false)) == -1)
					return;
				switch (op)
				{
					case (Ops.S_ARI << 16) | Ops.A_CPL:
						insDataReg(IC_AL, IOD_MVN, dst, 0, src);
						insDataReg(IC_AL, IOD_MVN, dstHi, 0, srcHi);
						return;
					case (Ops.S_ARI << 16) | Ops.A_MINUS:
						insDataImm(IC_AL, IOD_RSB | IOD_M_updFlags, dst, src, 0, 0);
						insDataImm(IC_AL, IOD_RSC, dstHi, srcHi, 0, 0);
						return;
					case (Ops.S_ARI << 16) | Ops.A_PLUS:
						if (dst != src)
							insDataReg(IC_AL, IOD_MOV, dst, 0, src);
						if (dstHi != srcHi)
							insDataReg(IC_AL, IOD_MOV, dstHi, 0, srcHi);
						return;
				}
				break;
		}
		fatalError("unsupported type or op in genUnaOp");
	}
	
	public void genIncMem(int dst, int type)
	{
		if ((dst = getReg(1, dst, false)) == -1)
			return;
		switch (type)
		{
			case StdTypes.T_BYTE:
				insSingleTransfer(IC_AL, IOST_LDRu08, RegHLP, dst, 0);
				insDataImm(IC_AL, IOD_ADD, RegHLP, RegHLP, 1, 0);
				insSingleTransfer(IC_AL, IOST_STRu08, RegHLP, dst, 0);
				return;
			case StdTypes.T_SHRT:
			case StdTypes.T_CHAR:
				insSingleReducedTransfer(IC_AL, IOSR_LDRZH, RegHLP, dst, 0);
				insDataImm(IC_AL, IOD_ADD, RegHLP, RegHLP, 1, 0);
				insSingleReducedTransfer(IC_AL, IOSR_STRH, RegHLP, dst, 0);
				return;
			case StdTypes.T_INT:
				insSingleTransfer(IC_AL, IOST_LDRi32, RegHLP, dst, 0);
				insDataImm(IC_AL, IOD_ADD, RegHLP, RegHLP, 1, 0);
				insSingleTransfer(IC_AL, IOST_STRi32, RegHLP, dst, 0);
				return;
			case StdTypes.T_LONG:
				insSingleTransfer(IC_AL, IOST_LDRi32, RegHLP, dst, 0);
				insDataImm(IC_AL, IOD_ADD | IOD_M_updFlags, RegHLP, RegHLP, 1, 0);
				insSingleTransfer(IC_AL, IOST_STRi32, RegHLP, dst, 0);
				insSingleTransfer(IC_AL, IOST_LDRi32, RegHLP, dst, 4);
				insDataImm(IC_AL, IOD_ADC, RegHLP, RegHLP, 0, 0);
				insSingleTransfer(IC_AL, IOST_STRi32, RegHLP, dst, 4);
				return;
		}
		fatalError("unsupported type in genIncMem");
	}
	
	public void genDecMem(int dst, int type)
	{
		if ((dst = getReg(1, dst, false)) == -1)
			return;
		switch (type)
		{
			case StdTypes.T_BYTE:
				insSingleTransfer(IC_AL, IOST_LDRu08, RegHLP, dst, 0);
				insDataImm(IC_AL, IOD_SUB, RegHLP, RegHLP, 1, 0);
				insSingleTransfer(IC_AL, IOST_STRu08, RegHLP, dst, 0);
				return;
			case StdTypes.T_SHRT:
			case StdTypes.T_CHAR:
				insSingleReducedTransfer(IC_AL, IOSR_LDRZH, RegHLP, dst, 0);
				insDataImm(IC_AL, IOD_SUB, RegHLP, RegHLP, 1, 0);
				insSingleReducedTransfer(IC_AL, IOSR_STRH, RegHLP, dst, 0);
				return;
			case StdTypes.T_INT:
				insSingleTransfer(IC_AL, IOST_LDRi32, RegHLP, dst, 0);
				insDataImm(IC_AL, IOD_SUB, RegHLP, RegHLP, 1, 0);
				insSingleTransfer(IC_AL, IOST_STRi32, RegHLP, dst, 0);
				return;
			case StdTypes.T_LONG:
				insSingleTransfer(IC_AL, IOST_LDRi32, RegHLP, dst, 0);
				insDataImm(IC_AL, IOD_SUB | IOD_M_updFlags, RegHLP, RegHLP, 1, 0);
				insSingleTransfer(IC_AL, IOST_STRi32, RegHLP, dst, 0);
				insSingleTransfer(IC_AL, IOST_LDRi32, RegHLP, dst, 4);
				insDataImm(IC_AL, IOD_SBC, RegHLP, RegHLP, 0, 0);
				insSingleTransfer(IC_AL, IOST_STRi32, RegHLP, dst, 4);
				return;
		}
		fatalError("unsupported type in genDecMem");
	}
	
	public void genSaveUnitContext()
	{
		insBlockTransfer(IC_AL, IOBT_STMFDu, RegSP, (1 << RegCLSS));
	}
	
	public void genRestUnitContext()
	{
		insBlockTransfer(IC_AL, IOBT_LDMFDu, RegSP, (1 << RegCLSS));
	}
	
	public void genLoadUnitContext(int dst, int off)
	{
		if ((dst = getReg(1, dst, true)) == -1)
			return;
		insSingleTransfer(IC_AL, IOST_LDRi32, dst, RegCLSS, off);
	}
	
	public void genLoadConstUnitContext(int dst, Object unitLoc)
	{
		if ((dst = getReg(1, dst, true)) == -1)
			return;
		insMoveConst(IC_AL, dst, mem.getAddrAsInt(unitLoc, 0), 0);
	}
	
	public void genSaveInstContext()
	{
		insBlockTransfer(IC_AL, IOBT_STMFDu, RegSP, (1 << RegINST) | (1 << RegCLSS));
	}
	
	public void genRestInstContext()
	{
		insBlockTransfer(IC_AL, IOBT_LDMFDu, RegSP, (1 << RegINST) | (1 << RegCLSS));
	}
	
	public void genLoadInstContext(int src)
	{
		if ((src = getReg(1, src, false)) == -1)
			return;
		insDataReg(IC_AL, IOD_MOV, RegINST, 0, src);
		insSingleTransfer(IC_AL, IOST_LDRi32, RegCLSS, RegINST, -4);
	}
	
	public void genCall(int off, int clssReg, int parSize)
	{
		fatalError("unsupported function genCall");
	}
	
	public void genCallIndexed(int intfReg, int off, int parSize)
	{
		fatalError("unsupported function genCallIndexed");
	}
	
	public void genCallConst(Mthd obj, int parSize)
	{
		insPatchedCall(obj, parSize);
	}
	
	public void genJmp(Instruction dest)
	{
		insBranch(IC_AL, dest);
	}
	
	public void genCondJmp(Instruction dest, int condHnd)
	{
		int cond;
		switch (condHnd)
		{
			case Ops.C_EQ:
				cond = IC_EQ;
				break; //"=="
			case Ops.C_NE:
				cond = IC_NE;
				break; //"!="
			case Ops.C_LW:
				cond = IC_LT;
				break; //"<"
			case Ops.C_GE:
				cond = IC_GE;
				break; //">="
			case Ops.C_LE:
				cond = IC_LE;
				break; //"<="
			case Ops.C_GT:
				cond = IC_GT;
				break; //">"
			case Ops.C_BO:
				cond = IC_CC;
				break; //unsigned "<"
			default:
				fatalError("unsupported cond in genCondJmp");
				return;
		}
		insBranch(cond, dest);
	}
	
	public int genComp(int src1, int src2, int type, int cond)
	{
		int src1Hi, src2Hi;
		switch (type)
		{
			case StdTypes.T_BOOL:
			case StdTypes.T_BYTE:
			case StdTypes.T_SHRT:
			case StdTypes.T_CHAR:
			case StdTypes.T_INT:
			case StdTypes.T_PTR:
				if ((src1 = getReg(1, src1, false)) == -1 || (src2 = getReg(1, src2, false)) == -1)
					return 0;
				insDataReg(IC_AL, IOD_CMPupd, 0, src1, src2);
				return cond;
			case StdTypes.T_LONG:
				if ((src1Hi = getReg(2, src1, false)) == -1 || (src2Hi = getReg(2, src2, false)) == -1 || (src1 = getReg(1, src1, false)) == -1 || (src2 = getReg(1, src2, false)) == -1)
					return 0;
				insDataReg(IC_AL, IOD_CMPupd, 0, src1Hi, src2Hi);
				insDataReg(IC_EQ, IOD_MOV, RegHLP, 0, src1, 1, IOD_M_LSRimm);
				insDataReg(IC_EQ, IOD_CMPupd, 0, RegHLP, src2, 1, IOD_M_LSRimm);
				insDataReg(IC_EQ, IOD_CMPupd, 0, src1, src2);
				return cond;
		}
		fatalError("unsupported type in genComp");
		return 0;
	}
	
	public int genCompValToConstVal(int src, int val, int type, int cond)
	{
		if (type == StdTypes.T_FLT)
		{
			fatalError("float not supported");
			return 0;
		}
		if ((src = getReg(1, src, false)) == -1)
			return 0;
		insMoveConst(IC_AL, RegHLP, val, 0);
		insDataReg(IC_AL, IOD_CMPupd, 0, src, RegHLP);
		return cond;
	}
	
	public int genCompValToConstDoubleOrLongVal(int src, long val, boolean asDouble, int cond)
	{
		int srcHi, srcLo;
		if (asDouble)
		{
			fatalError("double not supported");
			return 0;
		}
		if ((srcHi = getReg(2, src, false)) == -1 || (srcLo = getReg(1, src, false)) == -1)
			return 0;
		insMoveConst(IC_AL, RegLR, (int) (val >>> 32), 0);
		insDataReg(IC_AL, IOD_CMPupd, 0, srcHi, RegLR);
		insMoveConst(IC_EQ, RegLR, (int) val, 0);
		insDataReg(IC_EQ, IOD_MOV, RegHLP, 0, srcLo, 1, IOD_M_LSRimm);
		insDataReg(IC_EQ, IOD_CMPupd, 0, RegHLP, RegLR, 1, IOD_M_LSRimm);
		insDataReg(IC_EQ, IOD_CMPupd, 0, srcLo, RegLR);
		return cond;
	}
	
	public int genCompPtrToNull(int reg, int cond)
	{
		if ((reg = getReg(1, reg, false)) == -1)
			return 0;
		insDataImm(IC_AL, IOD_CMPupd, 0, reg, 0, 0);
		return cond;
	}
	
	public void genWriteIO(int dst, int src, int type, int memLoc)
	{
		fatalError("unsupported function genWriteIO");
	}
	
	public void genReadIO(int dst, int src, int type, int memLoc)
	{
		fatalError("unsupported function genReadIO");
	}
	
	public void genMoveToPrimary(int srcR, int type)
	{
		int reg;
		if ((reg = getReg(1, srcR, false)) == -1)
			return;
		if (reg != 0)
			insDataReg(IC_AL, IOD_MOV, 0, 0, reg);
		if (type == StdTypes.T_LONG || type == StdTypes.T_DPTR)
		{
			if ((reg = getReg(2, srcR, false)) == -1)
				return;
			if (reg != 1)
				insDataReg(IC_AL, IOD_MOV, 1, 0, reg);
		}
	}
	
	public void genMoveFromPrimary(int dstR, int type)
	{
		int reg;
		if (type == StdTypes.T_LONG || type == StdTypes.T_DPTR)
		{
			if ((reg = getReg(2, dstR, false)) == -1)
				return;
			if (reg != 1)
				insDataReg(IC_AL, IOD_MOV, reg, 0, 1);
		}
		if ((reg = getReg(1, dstR, false)) == -1)
			return;
		if (reg != 0)
			insDataReg(IC_AL, IOD_MOV, reg, 0, 0);
	}
	
	public void genMoveIntfMapFromPrimary(int dst)
	{
		fatalError("unsupported function genMoveIntfMapFromPrimary");
	}
	
	public void genSavePrimary(int type)
	{
		fatalError("unsupported function genSavePrimary");
	}
	
	public void genRestPrimary(int type)
	{
		fatalError("unsupported function genRestPrimary");
	}
	
	public void genCheckBounds(int addr, int off, int checkToOffset, Instruction onSuccess)
	{
		if ((addr = getReg(1, addr, false)) == -1 || (off = getReg(1, off, false)) == -1)
			return;
		insSingleTransfer(IC_AL, IOST_LDRi32, RegHLP, addr, checkToOffset);
		insDataReg(IC_AL, IOD_CMPupd, 0, off, RegHLP);
		insBranch(IC_CC, onSuccess);
	}
	
	public void genCheckStackExtreme(int maxValueReg, Instruction onSuccess)
	{
		if (maxValueReg != 1)
		{
			fatalError("invalid maxValueReg for genCheckStackExtreme");
			return;
		}
		Instruction i = getUnlinkedInstruction();
		appendInstruction(i);
		i.type = IT_STEX;
		i.size = 4;
		insDataReg(IC_AL, IOD_CMPupd, 0, 0, RegSP);
		insBranch(IC_CC, onSuccess);
	}
	
	public void genLoadDerefAddr(int destReg, int objReg, int indReg, int baseOffset, int entrySize)
	{
		int op, shift;
		if ((destReg = getReg(1, destReg, true)) == -1 || (objReg = getReg(1, objReg, false)) == -1 || (indReg = getReg(1, indReg, false)) == -1)
			return;
		if (entrySize < 0)
		{
			entrySize = -entrySize;
			op = IOD_SUB;
		}
		else
			op = IOD_ADD;
		switch (entrySize)
		{
			case 1:
				shift = 0;
				break;
			case 2:
				shift = 1;
				break;
			case 4:
				shift = 2;
				break;
			case 8:
				shift = 3;
				break;
			default:
				fatalError("unsupported entrySize in genLoadDerefAddr");
				return;
		}
		insDataReg(IC_AL, op, destReg, objReg, indReg, shift, IOD_M_LSLimm);
		if (baseOffset != 0)
		{
			if (baseOffset >= 1 && baseOffset <= 255)
				insDataImm(IC_AL, IOD_ADD, destReg, destReg, baseOffset, 0);
			else if (baseOffset <= -1 && baseOffset >= -255)
				insDataImm(IC_AL, IOD_SUB, destReg, destReg, -baseOffset, 0);
			else
				fatalError("unsupported baseOffset in genLoadDerefAddr");
		}
	}
	
	//using default stack as described in architecture using 7*relocOnStackBytes:
	//  excOffset + 6*relocOnStackBytes: current throwable thrown
	//  excOffset + 5*relocOnStackBytes: stack pointer for current try-block
	//  excOffset + 4*relocOnStackBytes: stack frame pointer for current try-block
	//  excOffset + 3*relocOnStackBytes: instance context of current try-block
	//  excOffset + 2*relocOnStackBytes: unit context of current try-block
	//  excOffset + 1*relocOnStackBytes: code-byte to jump to if exception is thrown
	//  excOffset                      : pointer to last excStackFrame
	public void genThrowFrameBuild(int globalAddrReg, Instruction dest, int throwBlockOffset)
	{
		if (globalAddrReg != 1 || usedRegs != 1)
		{ //globalAddrReg is the only allocated register, it is r0
			fatalError(ERR_INVREGTHR);
			return;
		}
		//reset exception variable
		insMoveConst(IC_AL, 1, 0, 0);
		insSingleTransfer(IC_AL, IOST_STRi32, 1, RegBASE, throwBlockOffset + 6 * 4);
		//fill in current register values
		insSingleTransfer(IC_AL, IOST_STRi32, RegSP, RegBASE, throwBlockOffset + 5 * 4);
		insSingleTransfer(IC_AL, IOST_STRi32, RegBASE, RegBASE, throwBlockOffset + 4 * 4);
		insSingleTransfer(IC_AL, IOST_STRi32, RegINST, RegBASE, throwBlockOffset + 3 * 4);
		insSingleTransfer(IC_AL, IOST_STRi32, RegCLSS, RegBASE, throwBlockOffset + 2 * 4);
		//calculate destination address and store it
		insDataReg(IC_AL, IOD_MOV, 1, 0, RegPC);
		Instruction from = getUnlinkedInstruction();
		appendInstruction(from);
		insPatchedAdd(from, dest);
		insSingleTransfer(IC_AL, IOST_STRi32, 1, RegBASE, throwBlockOffset + 1 * 4);
		//get global frame pointer, save in current frame, set current frame as new global one
		insSingleTransfer(IC_AL, IOST_LDRi32, 1, 0, 0);
		insSingleTransfer(IC_AL, IOST_STRi32, 1, RegBASE, throwBlockOffset + 0 * 4);
		if (throwBlockOffset < 0 && throwBlockOffset > -(256 << 2) && (throwBlockOffset & 3) == 0)
			insDataImm(IC_AL, IOD_SUB, 1, RegBASE, -(throwBlockOffset >> 2), 30);
		else
			fatalError("unsupported throwBlockOffset");
		insSingleTransfer(IC_AL, IOST_STRi32, 1, 0, 0);
	}
	
	public void genThrowFrameUpdate(Instruction oldDest, Instruction newDest, int throwBlockOffset)
	{
		insSingleTransfer(IC_AL, IOST_LDRi32, 0, RegBASE, throwBlockOffset + 1 * 4);
		insPatchedAdd(oldDest, newDest);
		insSingleTransfer(IC_AL, IOST_STRi32, 0, RegBASE, throwBlockOffset + 1 * 4);
	}
	
	public void genThrowFrameReset(int globalAddrReg, int throwBlockOffset)
	{
		if (globalAddrReg != 1 || usedRegs != 1)
		{ //globalAddrReg is the only allocated register, it is r0
			fatalError(ERR_INVREGTHR);
			return;
		}
		//copy pointer to last excStackFrame to global addr
		insSingleTransfer(IC_AL, IOST_LDRi32, 1, RegBASE, throwBlockOffset);
		insSingleTransfer(IC_AL, IOST_STRi32, 1, 0, 0);
	}
	
	public void inlineVarOffset(int inlineMode, int objReg, Object loc, int offset, int baseValue)
	{
		fatalError("unsupported function inlineVarOffset");
	}
	
	public void genNativeBoundException()
	{
		insMoveConst(IC_AL, RegPC, 0, 0); //reset cpu
	}
	
	// ---------- internal methods ----------
	
	protected int rotateLeft(int value, int bits)
	{
		return (value << bits) | (value >>> (32 - bits));
	}
	
	protected int getEvenByteRol(int val)
	{
		for (int i = 0; i < 32; i += 2)
			if ((rotateLeft(val, i) & 0xFFFFFF00) == 0)
				return i;
		return -1;
	}
	
	protected Instruction ins(int code)
	{
		Instruction i = getUnlinkedInstruction();
		appendInstruction(i);
		i.type = IT_FIX;
		i.putInt(code);
		return i;
	}
	
	protected void insDataReg(int cond, int op, int dstReg, int src1Reg, int src2Reg)
	{
		insDataReg(cond, op, dstReg, src1Reg, src2Reg, 0, 0);
	}
	
	protected void insDataReg(int cond, int op, int dstReg, int src1Reg, int src2Reg, int shiftRegOrImm, int shiftType)
	{
		//works with op being IOD_*, src1 should be 0 for IOD_MOV and IOD_MVN
		int shiftPos = 7;
		if ((shiftType & 0x00000010) != 0)
			shiftPos = 8; //shift is not an immediate value but a register, has to be shifted one bit further
		ins(cond | op | (dstReg << 12) | (src1Reg << 16) | (shiftRegOrImm << shiftPos) | shiftType | src2Reg);
	}
	
	protected void insDataImm(int cond, int op, int dstReg, int srcReg, int val, int rotateLeftEven)
	{
		//works with op being IOD_*, src should be 0 for IOD_MOV and IOD_MVN
		//rotate must have lowest bit cleared and must be in the range 0..30 (values 0, 2, 4, 6, ..., 30 allowed)
		//value must have hightest 24 bits cleared (values 0, 1, 2, ... 255 allowed)
		if ((val & 0xFFFFFF00) != 0)
			fatalError("value for insDataImm has to be 0..255");
		else
			ins(cond | 0x02000000 | op | (srcReg << 16) | (dstReg << 12) | (rotateLeftEven << (8 - 1)) | val);
	}
	
	protected void insSingleTransfer(int cond, int op, int valReg, int memReg, int memOff)
	{
		//works with op being IOST_*
		encodeSingleTransfer(ins(0), cond, op, valReg, memReg, memOff);
	}
	
	protected void insSingleReducedTransfer(int cond, int op, int valReg, int memReg, int memOff)
	{
		//works with op being IOSR_*
		if (memOff < 0)
		{
			op &= ~0x00800000; //clear flag "add" to get "sub offset"
			memOff = -memOff; //negate memory offset
		}
		if ((memOff & 0xFFFFFF00) != 0)
		{
			fatalError("memory offset too far away");
			return;
		}
		ins(cond | op | (memReg << 16) | (valReg << 12) | ((memOff & 0xF0) << (8 - 4)) | (memOff & 0xF));
	}
	
	protected void insBlockTransfer(int cond, int op, int ptrReg, int regMask)
	{
		//works with op being IOBT_*
		Instruction i = ins(cond | op | (ptrReg << 16) | regMask);
		if (ptrReg == RegSP)
		{
			int regsAffected = 0;
			for (int m = 0x0001; m != 0x10000; m = m << 1)
				if ((regMask & m) != 0)
					regsAffected++;
			if (op == IOBT_LDMFDu)
				regsAffected = -regsAffected;
			i.iPar3 = regsAffected;
		}
	}
	
	protected void insMul(int cond, int op, int dst, int src1, int src2, int addReg)
	{
		//works with op being IOM_*, addReg should be 0 for IOM_MULADD
		ins(cond | op | (dst << 16) | (addReg << 12) | (src1 << 8) | src2);
	}
	
	protected void insMulLong(int cond, int op, int dstHi, int dstLo, int src1, int src2)
	{
		//works with op being IOM_*, addReg should be 0 for IOM_MULADD
		ins(cond | op | (dstHi << 16) | (dstLo << 12) | (src1 << 8) | src2);
	}
	
	protected void insMoveConst(int cond, int dstReg, int val, int maxDiff)
	{
		int bs;
		if ((bs = getEvenByteRol(val)) != -1)
			insDataImm(cond, IOD_MOV, dstReg, 0, rotateLeft(val, bs), bs); //value is rotated byte, encode immediate load
		else if ((bs = getEvenByteRol(~val)) != -1)
			insDataImm(cond, IOD_MVN, dstReg, 0, rotateLeft(~val, bs), bs); //value is rotated inverted byte, encode immediate load
		else
			insLoadLit(cond, dstReg, val, maxDiff);
	}
	
	protected void insBranch(int cond, Instruction dest)
	{
		Instruction me = getUnlinkedInstruction();
		appendInstruction(me);
		me.size = 4;
		me.type = IT_BRANCH;
		me.iPar1 = cond;
		me.jDest = dest;
	}
	
	protected Instruction insPatchedCall(Mthd refMthd, int parSize)
	{
		Instruction me = getUnlinkedInstruction();
		appendInstruction(me);
		me.size = 4;
		me.type = IT_CALLpatched;
		me.refMthd = refMthd;
		me.iPar1 = parSize;
		addToCodeRefFixupList(me, 0);
		return me;
	}
	
	protected void insLoadLit(int cond, int dstReg, int value, int acceptDiff)
	{
		Instruction lit;
		//TODO handle acceptDiff
		//first: try to find the same literal
		lit = literals;
		while (lit != null)
		{
			if (lit.iPar1 == value)
				break;
			lit = lit.next;
		}
		if (lit == null)
		{
			lit = getUnlinkedInstruction();
			if (lastLiteral == null)
				literals = lit;
			else
				lastLiteral.next = lit;
			lastLiteral = lit;
			lit.type = IT_LITERAL;
			lit.size = 4;
			lit.iPar1 = value;
			lit.replaceInt(0, value);
		}
		Instruction me = getUnlinkedInstruction();
		appendInstruction(me);
		me.type = IT_LDR_LIT;
		me.size = 4;
		me.reg0 = dstReg;
		me.iPar1 = cond;
		me.jDest = lit;
	}
	
	protected void insPatchedAdd(Instruction from, Instruction to)
	{
		//will add difference of "to"-"from" to r1
		Instruction me;
		appendInstruction(me = getUnlinkedInstruction());
		me.size = 8; //worst case for load and add instruction, may be reduced to a single add if destination is not too far away
		me.type = IT_ADDpatched;
		me.jDest = from;
		me.iPar1 = 0; //reset ping-pong flag
		appendInstruction(me = getUnlinkedInstruction());
		me.size = 0;
		me.type = IT_HELPER;
		me.jDest = to;
	}
	
	protected void encodeSingleTransfer(Instruction i, int cond, int op, int valReg, int memReg, int memOff)
	{
		//works with op being IOST_*, they are coded as "add offset" having bit 0x00800000 set
		if (memOff < 0)
		{
			op &= ~0x00800000; //clear flag "add" to get "sub offset"
			memOff = -memOff; //negate memory offset
		}
		if ((memOff & 0xFFFFF000) != 0)
		{
			fatalError("memory offset too far away");
			return;
		}
		i.replaceInt(0, cond | op | (memReg << 16) | (valReg << 12) | memOff);
	}
	
	protected boolean fixBranch(Instruction me)
	{
		int relative = 0;
		//get offset
		relative = getRelative(me, me.jDest);
		if (relative == 0)
		{ //do not code jump to next instruction
			me.size = 0;
			me.type = I_NONE;
			return true;
		}
		me.replaceInt(0, me.iPar1 | IOB_BRANCHwoLink | (((relative - 4) >> 2) & 0xFFFFFF));
		return false;
	}
	
	protected boolean fixAddPatched(Instruction me, boolean doRealEncode)
	{
		int oldSize = me.size, relative, tmp;
		relative = getRelative(me.jDest, me.next.jDest) - 4; //next instruction is IT_HELPER
		
		if ((tmp = getEvenByteRol(relative)) != -1 && me.iPar1 == 0)
		{ //shorten only if no ping-pong of size detected
			me.size = 4; //distance may be encoded as immediate add
			if (doRealEncode)
				me.replaceInt(0, IC_AL | 0x02000000 | IOD_ADD | (1 << 16) | (1 << 12) | (tmp << (8 - 1)) | rotateLeft(relative, tmp)); //value is rotated byte, encode immediate add
		}
		else
		{
			if (oldSize == 4)
				me.iPar1 = 1; //remember that we were better already, do not change back to single instruction to avoid endless loop
			me.size = 8; //distance needs literal
			if (doRealEncode)
			{
				Instruction lit;
				//first: try to find the same literal
				lit = literals;
				while (lit != null)
				{
					if (lit.iPar1 == relative)
						break;
					lit = lit.next;
				}
				if (lit == null)
				{
					lit = getUnlinkedInstruction();
					if (lastLiteral == null)
						literals = lit;
					else
						lastLiteral.next = lit;
					lastLiteral = lit;
					lit.type = IT_LITERAL;
					lit.size = 4;
					lit.iPar1 = relative;
					lit.replaceInt(0, relative);
					appendInstruction(lit);
				}
				tmp = getRelative(me, lit) - 4;
				encodeSingleTransfer(me, IC_AL, IOST_LDRi32, 2, RegPC, tmp); //load literal to r2
				me.replaceInt(4, IC_AL | IOD_ADD | (1 << 12) | (1 << 16) | 2);
			}
		}
		return oldSize != me.size;
	}
	
	protected void fixStex(Instruction me)
	{
		int curStackUsage = 2, maxStackUsage = 0; //there are already 2 values on the stack, see prepareMethodCoding
		Instruction check = me;
		me.type = IT_FIX;
		while (check != null)
		{
			if (check.iPar3 != 0)
			{
				curStackUsage += check.iPar3;
				if (curStackUsage > maxStackUsage)
					maxStackUsage = curStackUsage;
				else if (curStackUsage < 0)
				{
					fatalError("invalid value of curStackUsage for stack extreme check");
					return;
				}
			}
			check = check.next;
		}
		if (maxStackUsage > 255)
		{
			fatalError("unsupported local stack size for stack extreme check");
			return;
		}
		me.replaceInt(0, IC_AL | 0x02000000 | IOD_ADD | (0 << 16) | (0 << 12) | (30 << (8 - 1)) | maxStackUsage);
	}
	
	protected void fixLoadLit(Instruction me)
	{
		int relative = getRelative(me, me.jDest) - 4;
		encodeSingleTransfer(me, me.iPar1, IOST_LDRi32, me.reg0, RegPC, relative);
	}
	
	protected int getRelative(Instruction from, Instruction to)
	{
		int relative = 0;
		if (from == null || to == null)
		{
			fatalError("from or to is null");
			return 0;
		}
		if (from.instrNr >= to.instrNr)
		{ //get destination before us
			relative -= from.size;
			while (from != to)
			{
				from = from.prev;
				if (from == null)
				{
					fatalError("dest before not found");
					return 0;
				}
				relative -= from.size;
			}
		}
		else
		{ //get destination behind us
			from = from.next;
			while (from != to)
			{
				relative += from.size;
				from = from.next;
				if (from == null)
				{
					fatalError("dest behind not found");
					return 0;
				}
			}
		}
		return relative;
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
	
	protected int print(Instruction i)
	{ //returns number of real instructions
		int cnt;
		if (i.type == I_NONE)
			return 0;
		ctx.out.print(i.instrNr);
		ctx.out.print(": ");
		switch (i.type)
		{
			case IT_FIX:
			case IT_LITERAL:
			case I_MAGC:
			case IT_HELPER:
			case IT_STEX:
				ctx.out.print("CONST");
				for (cnt = 0; cnt < i.size; cnt++)
				{
					ctx.out.print(" 0x");
					ctx.out.printHexFix(i.code[cnt], 2);
				}
				return cnt >>> 2;
			case IT_BRANCH:
				ctx.out.print("branch to i");
				ctx.out.print(i.jDest.instrNr);
				break;
			case IT_LDR_LIT:
				ctx.out.print("ldr literal");
				break;
			case IT_CALLpatched:
				ctx.out.print("patched call to ");
				ctx.out.print(i.refMthd.name);
				break;
			case IT_ADDpatched:
				ctx.out.print("patched add to i");
				ctx.out.print(i.jDest.instrNr);
				break;
			default:
				ctx.out.print("UNKNOWN");
				break;
		}
		return 1;
	}
}
