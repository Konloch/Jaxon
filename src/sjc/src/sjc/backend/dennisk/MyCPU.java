/* Copyright (C) 2015, 2016 Stefan Frenz
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
import sjc.compbase.Marks;
import sjc.compbase.Mthd;
import sjc.compbase.Ops;
import sjc.compbase.StdTypes;

/**
 * MyCPU: Architecture backend for MyCPU by Dennis Kuschel
 *
 * @author S. Frenz
 * @version 160522 fixed error messages
 * version 160324 removed unneccessary needsAlignedVrbls
 * version 150929 added support for exceptions
 * version 150928 added optimized genStore* and genBinOp*, fixed some details
 * version 150927 added interface support, fixed some important details
 * version 150926 fixed many details
 * version 150922 added new inlineCodeOffset, implemented some more functions
 * version 150921 fixed some details and implemented some more functions
 * version 150919 implemented some more functions
 * version 150917 fixed some details
 * version 150916 implemented some more functions
 * version 150915 added basic behavior
 * version 150831 initial version
 */
public class MyCPU extends Architecture
{
	//basic concept:
	//  register A and Y are just dummies
	//  register X holds stack base
	//  RAM-locations 0xC0-0xD9 contain 26 general purpose registers
	//  RAM-location  0xDA+0xDB contain temporary 16-bit-values
	//  RAM-location  0xDC+0xDD contain the current class
	//  RAM-location  0xDE+0xDF contain the current instance
	
	//special "register"
	public final static int RegBaseMark = 0xAFFECAFE; //this mask should never occur
	
	//definitions of memory mapped registers
	public final static int RegMax = 31;
	public final static int RegInstHi = 31;
	public final static int RegInstLo = 30;
	public final static int RegClssHi = 29;
	public final static int RegClssLo = 28;
	public final static int RegTmpHi = 27;
	public final static int RegTmpLo = 26;
	public final static int RegMaxGP = 25;
	
	//addresses of memory mapped registers
	public final static int ZPAddrBase = 0xC0;
	public final static int ZPAddrInstHi = 31 + ZPAddrBase;
	public final static int ZPAddrInstLo = 30 + ZPAddrBase;
	public final static int ZPAddrClssHi = 29 + ZPAddrBase;
	public final static int ZPAddrClssLo = 28 + ZPAddrBase;
	public final static int ZPAddrTmpHi = 27 + ZPAddrBase;
	public final static int ZPAddrTmpLo = 26 + ZPAddrBase;
	
	public final static int RegMaskINST = (1 << RegInstHi) | (1 << RegInstLo);
	public final static int RegMaskCLSS = (1 << RegClssHi) | (1 << RegClssLo);
	public final static int RegMaskGP = (1 << (RegMaxGP + 1)) - 1;
	
	public final static int PAR_MASK = 0x00FF0000;
	public final static int PAR_NONE = 0x00010000;
	public final static int PAR_BYTE = 0x00020000;
	public final static int PAR_WORD = 0x00030000;
	public final static int PAR_SECBYTE = 0x00100000;
	
	public final static int IT_NORM = 1;
	public final static int IT_EXTCALL = 2;
	public final static int IT_INTJMP = 3;
	public final static int IT_DEFICA = 4;
	public final static int IT_INLCODA = 5;
	
	public final static int I_CLC = 0x04 | PAR_NONE; //clear the carry flag
	public final static int I_SEC = 0x05 | PAR_NONE; //set the carry flag
	public final static int I_CLA = 0x2C | PAR_NONE; //clear Accu
	public final static int I_CLY = 0x2E | PAR_NONE; //clear Y-Register
	public final static int I_LDAimm = 0x30 | PAR_BYTE; //load Accu immediate
	public final static int I_LDXimm = 0x50 | PAR_BYTE; //load X-Register immediate
	public final static int I_LDYimm = 0x57 | PAR_BYTE; //load Y-Register immediate
	public final static int I_LDAzp = 0x31 | PAR_BYTE; //load content of RAM addressed by ZP (8bit) into Accu
	public final static int I_LDAizpy = 0x34 | PAR_BYTE; //load content of RAM indirectly addressed by 16bit pointer stored in zeropage plus Y into Accu
	public final static int I_LDAabsx = 0x35 | PAR_WORD; //load content of RAM addressed by abs (16bit) plus X into Accu
	public final static int I_STAzp = 0x41 | PAR_BYTE; //store Accu into RAM addressed by ZP (8bit)
	public final static int I_STAabsx = 0x45 | PAR_WORD; //store Accu into RAM addressed by abs (16bit) plus X
	public final static int I_STAizpy = 0x44 | PAR_BYTE; //store Accu into RAM indirectly addressed by 16bit pointer stored in zeropage plus Y
	public final static int I_LDXzp = 0x51 | PAR_BYTE; //load content of RAM addressed by ZP (8bit) into X-Register
	public final static int I_STXzp = 0x54 | PAR_BYTE; //store X-Register into RAM addressed by ZP (8bit)
	public final static int I_LDYzp = 0x58 | PAR_BYTE; //load content of RAM addressed by ZP (8bit) into Y-Register
	public final static int I_STYzp = 0x5B | PAR_BYTE; //store Y-Register into RAM addressed by ZP (8bit
	public final static int I_LPA = 0xEA | PAR_NONE; //load content of RAM addressed by X- and Y-Register into Accu and increment 16 bit pointer
	public final static int I_SPA = 0xFA | PAR_NONE; //store Accu into RAM addressed by X- and Y-Register and increment 16 bit pointer
	public final static int I_MOVzpimm = 0x48 | PAR_BYTE | PAR_SECBYTE; //load data immediate to zeropage
	public final static int I_MOVzpzp = 0x47 | PAR_BYTE | PAR_SECBYTE; //copy content of RAM addressed by ZP2 (8bit) into RAM addressed by ZP1 (8bit)
	public final static int I_PHA = 0x08 | PAR_NONE; //push Accu to stack
	public final static int I_PHX = 0x09 | PAR_NONE; //push X-Register to stack
	public final static int I_PUSHimm = 0xBA | PAR_BYTE; //push immediate 8 bit data to stack
	public final static int I_PUSHzp = 0x9A | PAR_BYTE; //push content of RAM addressed by ZP (8bit) to stack
	public final static int I_PUSAimm = 0xCA | PAR_WORD; //push immediate 16 bit data to stack
	public final static int I_PLA = 0x0C | PAR_NONE; //load Accu from stack
	public final static int I_PLX = 0x0D | PAR_NONE; //load X-Register from stack
	public final static int I_POPzp = 0xAA | PAR_BYTE; //load data from stack and write it into RAM addressed by ZP (8bit)
	public final static int I_INY = 0x8B | PAR_NONE; //increment Y-Register
	public final static int I_INCizpy = 0x9D | PAR_BYTE; //increment the content of RAM indirectly addressed by 16 bit pointer stored in zeropage plus Y
	public final static int I_DECizpy = 0x9F | PAR_BYTE; //decrement the content of RAM indirectly addressed by 16 bit pointer stored in zeropage plus Y
	public final static int I_DEC = 0x9B | PAR_NONE; //decrement Accu
	public final static int I_DEY = 0xBB | PAR_NONE; //decrement Y-Register
	public final static int I_TSX = 0x26 | PAR_NONE; //transfer Stackpointer into X-Register
	public final static int I_TAX = 0x20 | PAR_NONE; //transfer Accu into X-Register
	public final static int I_TAY = 0x22 | PAR_NONE; //transfer Accu into Y-Register
	public final static int I_TXA = 0x21 | PAR_NONE; //transfer X-Register into Accu
	public final static int I_TYA = 0x23 | PAR_NONE; //transfer Y-Register into Accu
	public final static int I_TXY = 0x24 | PAR_NONE; //transfer X-Register into Y-Register
	public final static int I_TYX = 0x25 | PAR_NONE; //transfer Y-Register into X-Register
	public final static int I_INS = 0xDA | PAR_BYTE; //increment stackpointer by value
	public final static int I_RTS = 0x1F | PAR_NONE; //return from subroutine
	public final static int I_ADCimm = 0x80 | PAR_BYTE; //add immediate data to Accu
	public final static int I_ADCzp = 0x81 | PAR_BYTE; //add content of RAM addressed by ZP (8bit) to Accu
	public final static int I_SBCimm = 0x90 | PAR_BYTE; //subtract immediate data from Accu
	public final static int I_SBCzp = 0x91 | PAR_BYTE; //subtract content of RAM addressed by ZP (8bit) from Accu
	public final static int I_ANDimm = 0xD0 | PAR_BYTE; //logical AND with Accu and immediate data
	public final static int I_ANDzp = 0xD1 | PAR_BYTE; //logical AND with Accu and content of RAM addressed by ZP (8bit)
	public final static int I_ORimm = 0xE0 | PAR_BYTE; //logical OR with Accu and immediate data
	public final static int I_ORzp = 0xE1 | PAR_BYTE; //logical OR with Accu and content of RAM addressed by ZP (8bit)
	public final static int I_EORimm = 0xF0 | PAR_BYTE; //logical AND with Accu and immediate data
	public final static int I_EORzp = 0xF1 | PAR_BYTE; //logical exclusive OR with Accu and content of RAM addressed by ZP (8bit)
	public final static int I_MULimm = 0xA0 | PAR_BYTE; //multiply Accu with immediate data
	public final static int I_MULzp = 0xA1 | PAR_BYTE; //multiply Accu with content of RAM addressed by ZP (8bit)
	public final static int I_SHL = 0xCB | PAR_NONE; //shift left Accu, highest bit will be stored in carry
	public final static int I_SHLzp = 0xAC | PAR_BYTE; //shift left the content of RAM addressed by ZP (8bit), highest bit will be stored in carry
	public final static int I_SHR = 0xDB | PAR_NONE; //shift right Accu, lowest bit will be stored in carry
	public final static int I_SHRzp = 0xBC | PAR_BYTE; //shift right the content of RAM addressed by ZP (8bit), lowest bit will be stored in carry
	public final static int I_ROL = 0xEB | PAR_NONE; //rotate left Accu, highest bit will be stored in carry
	public final static int I_ROLzp = 0xDC | PAR_BYTE; //rotate left the content of RAM addressed by ZP (8bit), highest bit will be stored in carry
	public final static int I_ROR = 0xFB | PAR_NONE; //rotate right Accu, lowest bit will be stored in carry
	public final static int I_RORzp = 0xEC | PAR_BYTE; //rotate right the content of RAM addressed by ZP (8bit), lowest bit will be stored in carry
	public final static int I_CMPimm = 0x70 | PAR_BYTE; //compare Accu with immediate data
	public final static int I_CMPzp = 0x71 | PAR_BYTE; //compare Accu with content of RAM addressed by ZP (8bit)
	public final static int I_CAY = 0x69 | PAR_NONE; //compare Accu with Y-Register
	public final static int I_BITzpimm = 0x5E | PAR_BYTE | PAR_SECBYTE; //logical AND with RAM (ZP = 8bit address) and immediate data
	public final static int I_JSRmem = 0x1B | PAR_WORD; //jump to indirectly addressed  subroutine, the pointer to the subroutine is stored in RAM
	public final static int I_JSRimm = 0x1A | PAR_WORD; //jump to direct addressed subroutine
	public final static int I_JMPimm = 0x10 | PAR_WORD; //jump to direct address
	public final static int I_JNCimm = 0x16 | PAR_WORD; //jump if carry clear
	public final static int I_JNVimm = 0x14 | PAR_WORD; //jump if sign clear
	public final static int I_JNZimm = 0x18 | PAR_WORD; //jump if zero clear
	public final static int I_JPCimm = 0x17 | PAR_WORD; //jump if carry set
	public final static int I_JPVimm = 0x15 | PAR_WORD; //jump if sign set
	public final static int I_JPZimm = 0x19 | PAR_WORD; //jump if zero set
	public final static int I_DYJPimm = 0x4A | PAR_WORD; //decrement Y and jump if Y is not zero
	
	public final static int VAROFF_PARAM_NRM = 3;
	public final static int VAROFF_PARAM_INL = 1;
	
	public final static int STACKMEM_OFF = 0x0101;
	
	protected final static String ERR_INVGLOBADDRREG = "invalid register for address";
	
	private int usedRegs, writtenRegs, nextAllocReg, curVarOffParam;
	
	//initialization
	public MyCPU()
	{
		relocBytes = 2;
		stackClearBits = 0;
		allocClearBits = 1;
		maxInstrCodeSize = 3;
		throwFrameSize = 12;
		throwFrameExcOff = 10;
		regClss = RegMaskCLSS;
		regInst = RegMaskINST;
		regBase = RegBaseMark;
		binAriCall[StdTypes.T_BYTE] |= (1 << (Ops.A_DIV - Ops.MSKBSE)) | (1 << (Ops.A_MOD - Ops.MSKBSE));
		binAriCall[StdTypes.T_SHRT] |= (1 << Ops.A_MUL - Ops.MSKBSE) | (1 << (Ops.A_DIV - Ops.MSKBSE)) | (1 << (Ops.A_MOD - Ops.MSKBSE));
		binAriCall[StdTypes.T_INT] |= (1 << Ops.A_MUL - Ops.MSKBSE) | (1 << (Ops.A_DIV - Ops.MSKBSE)) | (1 << (Ops.A_MOD - Ops.MSKBSE));
		binAriCall[StdTypes.T_LONG] |= (1 << Ops.A_MUL - Ops.MSKBSE) | (1 << (Ops.A_DIV - Ops.MSKBSE)) | (1 << (Ops.A_MOD - Ops.MSKBSE));
		binAriCall[StdTypes.T_FLT] |= (1 << (Ops.A_MUL - Ops.MSKBSE)) | (1 << (Ops.A_DIV - Ops.MSKBSE)) | (1 << (Ops.A_MOD - Ops.MSKBSE)) | (1 << (Ops.A_PLUS - Ops.MSKBSE)) | (1 << (Ops.A_MINUS - Ops.MSKBSE));
		binAriCall[StdTypes.T_DBL] |= (1 << (Ops.A_MUL - Ops.MSKBSE)) | (1 << (Ops.A_DIV - Ops.MSKBSE)) | (1 << (Ops.A_MOD - Ops.MSKBSE)) | (1 << (Ops.A_PLUS - Ops.MSKBSE)) | (1 << (Ops.A_MINUS - Ops.MSKBSE));
		unaAriCall[StdTypes.T_FLT] |= (1 << (Ops.A_MINUS - Ops.MSKBSE));
		unaAriCall[StdTypes.T_DBL] |= (1 << (Ops.A_MINUS - Ops.MSKBSE));
	}
	
	//general memory and register management
	public void putRef(Object loc, int offset, Object ptr, int ptrOff)
	{
		mem.putShort(loc, offset, (short) mem.getAddrAsInt(ptr, ptrOff));
	}
	
	public void putCodeRef(Object loc, int offset, Object ptr, int ptrOff)
	{
		mem.putShort(loc, offset, (short) mem.getAddrAsInt(ptr, ptrOff));
	}
	
	protected int getZPAddrOfReg(int reg)
	{
		return ZPAddrBase + reg;
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
	
	private int getBitPos(int reg)
	{
		int i, j;
		for (i = 0; i < 32; i++)
		{ //also search "special registers"
			j = 1 << i;
			if ((reg & j) != 0)
				return i;
		}
		return -1;
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
	
	protected int storeReg(int regs)
	{
		regs &= usedRegs & writtenRegs;
		for (int i = 0; i <= RegMaxGP; i++)
			if ((regs & (1 << i)) != 0)
				ins(I_PUSHzp, getZPAddrOfReg(i));
		return regs;
	}
	
	protected void restoreReg(int regs)
	{
		usedRegs |= regs;
		writtenRegs |= regs;
		for (int i = RegMaxGP; i >= 0; i--)
			if ((regs & (1 << i)) != 0)
				ins(I_POPzp, getZPAddrOfReg(i));
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
	
	protected int getRegZPAddr(int nr, int reg, int type, boolean firstWrite)
	{
		if (nr < 1 || nr > 8)
		{
			fatalError("invalid call to getReg");
			return -1;
		}
		if (reg == regBase)
		{
			fatalError("regBase not detected before getRegZPAddr");
			return -1;
		}
		reg = bitSearch(reg, nr);
		if (reg == 0)
		{
			fatalError("register not found in getRegZPAddr");
			return -1;
		}
		if (firstWrite)
		{
			writtenRegs |= reg & RegMaskGP;
			usedRegs |= reg & RegMaskGP;
		}
		return getZPAddrOfReg(getBitPos(reg));
	}
	
	//method start, end and finalization
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
			curVarOffParam = VAROFF_PARAM_NRM;
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
		if (curMthd.parSize + curMthd.varSize > 253)
		{
			fatalError("method has more than 253 bytes var+par on stack");
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
			fatalError("interrupt methods not supported yet in codeProlog");
		}
		ins(I_PHX);
		ins(I_TSX); //stack pointer in BSE
		if (curMthd.varSize > 0)
		{
			ins(I_CLA);
			if (curMthd.varSize <= 4)
				for (i = 0; i < curMthd.varSize; i++)
					ins(I_PHA);
			else
			{
				if (curMthd.varSize > 255)
				{
					fatalError("maximum of 255 bytes for local variables allowed");
					return;
				}
				ins(I_LDYimm, curMthd.varSize);
				appendInstruction(loopDest = getUnlinkedInstruction());
				ins(I_PHA);
				insPatchedJmp(I_DYJPimm, loopDest);
			}
		}
	}
	
	public void codeEpilog(Mthd outline)
	{
		if (outline != null && curMthd.parSize == 0 && curMthd.varSize == 0)
		{ //optimize inline methods without parameters and variables
			--curInlineLevel;
			curMthd = outline;
			return;
		}
		if (curMthd.varSize != 0)
		{
			ins(I_INS, curMthd.varSize);
		}
		ins(I_PLX);
		if ((curMthd.marker & Marks.K_INTR) != 0)
		{
			fatalError("interrupt methods not supported yet in codeEpilog");
			curMthd = null;
		}
		else if (outline != null)
		{
			if (--curInlineLevel == 0)
				curVarOffParam = VAROFF_PARAM_NRM;
			--curInlineLevel;
			curMthd = outline;
		}
		else
		{
			ins(I_RTS);
			curMthd = null;
		}
	}
	
	public void finalizeInstructions(Instruction first)
	{
		//nothing to do, all instructions are ready to be copied, only jumps/magics need to be fixed after address is known
	}
	
	public void finalizeInstructionAddresses(Mthd generatingMthd, Instruction first, Object loc, int offset)
	{
		int baseInlineCodeAddress = -1;
		Instruction now = first;
		int base = mem.getAddrAsInt(loc, offset);
		if ((generatingMthd.marker & Marks.K_DEBG) != 0)
		{
			ctx.out.print(" ---> base code address of debugged method ");
			generatingMthd.printNamePar(ctx.out);
			ctx.out.print(" is 0x");
			ctx.out.printHexFix(base, 4);
			ctx.out.println();
		}
		//fix jumps
		while (now != null)
		{
			switch (now.type)
			{
				case IT_INTJMP:
					now.replaceShort(1, base + getRelativeAddressOfInstruction(first, now.jDest));
					break;
				case IT_DEFICA:
					baseInlineCodeAddress = base + getRelativeAddressOfInstruction(first, now);
					break;
				case IT_INLCODA:
					if (baseInlineCodeAddress == -1)
						fatalError("inline code address needs reference");
					else
						now.replaceShort(0, baseInlineCodeAddress + now.iPar1);
					break;
			}
			now = now.next;
		}
	}
	
	private int getRelativeAddressOfInstruction(Instruction first, Instruction dest)
	{
		Instruction now = first;
		int offset = 0;
		while (now != null)
		{
			if (now == dest)
				return offset;
			offset += now.size;
			now = now.next;
		}
		fatalError("could not find relative address of instruction");
		return 0;
	}
	
	//basic architecture implementation
	public void genLoadConstVal(int dst, int val, int type)
	{
		int reg, count, i;
		if (dst == regBase)
		{
			fatalError("invalid use of BaseReg in genLoadVarConstVal");
			return;
		}
		if ((count = getByteCount(type)) == -1)
			return;
		for (i = 0; i < count; i++)
		{
			if ((reg = getRegZPAddr(i + 1, dst, type, true)) == -1)
				return;
			ins(I_MOVzpimm, reg, (val >>> (i << 3)) & 0xFF);
		}
	}
	
	public void genLoadConstDoubleOrLongVal(int dst, long val, boolean asDouble)
	{
		int reg, i, tempVal;
		if (dst == regBase)
		{
			fatalError("invalid use of BaseReg in genLoadConstDoubleOrLongVal");
			return;
		}
		for (i = 0; i < 8; i++)
		{
			if ((reg = getRegZPAddr(i + 1, dst, StdTypes.T_LONG, true)) == -1)
				return;
			tempVal = (int) (val >>> (i << 3)) & 0xFF; //determine value to load
			if (tempVal == 0)
				ins(I_CLA);
			else
				ins(I_LDAimm, tempVal);
			ins(I_STAzp, reg);
		}
	}
	
	public void genLoadVarAddr(int dst, int src, Object loc, int off)
	{
		int pos = mem.getAddrAsInt(loc, off);
		int dst1, dst2;
		if (dst == regBase)
		{
			fatalError("invalid use of BaseReg in genLoadVarAddr");
			return;
		}
		if (pos == 0 && src == dst)
			return;
		if ((dst1 = getRegZPAddr(1, dst, StdTypes.T_PTR, true)) == -1 || (dst2 = getRegZPAddr(2, dst, StdTypes.T_PTR, true)) == -1)
			return;
		if (src == regBase)
		{
			if (pos >= 0)
				pos += curVarOffParam;
			pos += STACKMEM_OFF;
			ins(I_TXA);
			ins(I_CLC);
			ins(I_ADCimm, pos & 0xFF);
			ins(I_STAzp, dst1);
			ins(I_CLA);
			ins(I_ADCimm, (pos >>> 8) & 0xFF);
			ins(I_STAzp, dst2);
		}
		else if (src != 0)
		{
			int src1, src2;
			if ((src1 = getRegZPAddr(1, src, StdTypes.T_PTR, true)) == -1 || (src2 = getRegZPAddr(2, src, StdTypes.T_PTR, true)) == -1)
				return;
			ins(I_LDAzp, src1);
			if (pos != 0)
			{
				ins(I_CLC);
				if ((pos & 0xFF) != 0)
					ins(I_ADCimm, pos & 0xFF);
			}
			ins(I_STAzp, dst1);
			ins(I_LDAzp, src2);
			if (pos != 0)
				ins(I_ADCimm, (pos >>> 8) & 0xFF);
			ins(I_STAzp, dst2);
		}
		else
		{
			ins(I_LDAimm, pos & 0xFF);
			ins(I_STAzp, dst1);
			ins(I_LDAimm, (pos >>> 8) & 0xFF);
			ins(I_STAzp, dst2);
		}
	}
	
	public void genLoadVarVal(int dst, int src, Object loc, int off, int type)
	{
		int dstReg, count, i = 0, pos = mem.getAddrAsInt(loc, off), src1, src2;
		count = getByteCount(type);
		if (src == regBase)
		{
			if (pos >= 0)
				pos += curVarOffParam;
			pos += STACKMEM_OFF;
			while (i < count)
			{
				ins(I_LDAabsx, pos + i);
				if ((dstReg = getRegZPAddr(++i, dst, type, true)) == -1)
					return;
				ins(I_STAzp, dstReg);
			}
			return;
		}
		ins(I_PHX);
		if (src != 0)
		{
			if ((src1 = getRegZPAddr(1, src, StdTypes.T_PTR, true)) == -1 || (src2 = getRegZPAddr(2, src, StdTypes.T_PTR, true)) == -1)
				return;
			if (pos != 0)
			{
				ins(I_CLC);
				ins(I_LDAzp, src1);
				ins(I_ADCimm, pos & 0xFF);
				ins(I_TAX);
				ins(I_LDAzp, src2);
				ins(I_ADCimm, (pos >>> 8) & 0xFF);
				ins(I_TAY);
			}
			else
			{
				ins(I_LDXzp, src1);
				ins(I_LDYzp, src2);
			}
		}
		else
		{
			ins(I_LDXimm, pos & 0xFF);
			ins(I_LDYimm, (pos >>> 8) & 0xFF);
		}
		while (i < count)
		{
			if ((dstReg = getRegZPAddr(++i, dst, type, true)) == -1)
				return;
			ins(I_LPA);
			ins(I_STAzp, dstReg);
		}
		ins(I_PLX);
	}
	
	public void genConvertVal(int dst, int src, int toType, int fromType)
	{
		int countFrom, countTo, regDst, regSrc = 0, i = 1, temp;
		Instruction dest;
		countFrom = getByteCount(fromType);
		countTo = getByteCount(toType);
		temp = countFrom >= countTo ? countTo : countFrom; //determine minimum value
		for (; i <= temp; i++)
		{ //take care of lower bytes, regSrc is highest valid register
			if ((regDst = getRegZPAddr(i, dst, toType, true)) == -1 || (regSrc = getRegZPAddr(i, src, fromType, false)) == -1)
				return;
			if (regDst != regSrc)
				ins(I_MOVzpzp, regDst, regSrc);
		}
		if (countTo > countFrom)
		{ //conversion was an expansion
			ins(I_CLA);
			if (fromType != StdTypes.T_CHAR)
			{ //not char -> sign extension
				ins(I_BITzpimm, regSrc, 0x80);
				insPatchedJmp(I_JPZimm, dest = getUnlinkedInstruction());
				ins(I_DEC);
				appendInstruction(dest);
			}
			//else //char -> zero extension
			for (; i <= countTo; i++)
			{
				if ((regDst = getRegZPAddr(i, dst, toType, true)) == -1)
					return;
				ins(I_STAzp, regDst);
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
			if ((dstReg = getRegZPAddr(i, dst, type, true)) == -1 || (srcReg = getRegZPAddr(i, src, type, false)) == -1)
				return;
			if (dstReg != srcReg)
			{
				ins(I_MOVzpzp, dstReg, srcReg);
			}
		}
	}
	
	public void genPushConstVal(int val, int type)
	{
		int count, shortVal;
		count = getByteCount(type);
		if (count == 1)
			ins(I_PUSHimm, val & 0xFF);
		else
		{
			if ((count & 1) != 0)
			{
				fatalError("odd regCount>=2 in genPushConstVal");
				return;
			}
			while (count > 0)
			{
				count -= 2;
				shortVal = (val >>> (count << 3)) & 0xFFFF;
				ins(I_PUSAimm, shortVal);
			}
		}
	}
	
	public void genPushConstDoubleOrLongVal(long val, boolean asDouble)
	{
		int count = 8, shortVal;
		while (count > 0)
		{
			count -= 2;
			shortVal = ((int) (val >>> (count << 3))) & 0xFFFF;
			ins(I_PUSAimm, shortVal);
		}
	}
	
	public void genPush(int src, int type)
	{
		int count, reg;
		count = getByteCount(type);
		while (count > 0)
		{
			if ((reg = getRegZPAddr(count--, src, type, false)) == -1)
				return;
			ins(I_PUSHzp, reg);
		}
	}
	
	public void genPop(int dst, int type)
	{
		int count, reg, i = 0;
		count = getByteCount(type);
		while (i < count)
		{
			if ((reg = getRegZPAddr(++i, dst, type, true)) == -1)
				return;
			ins(I_POPzp, reg);
		}
	}
	
	public void genAssign(int dst, int src, int type)
	{
		int i, regCount, reg1, reg2, srcReg;
		regCount = getByteCount(type);
		if ((reg1 = getRegZPAddr(1, dst, StdTypes.T_PTR, false)) == -1 || (reg2 = getRegZPAddr(2, dst, StdTypes.T_PTR, false)) == -1)
			return;
		ins(I_PHX);
		ins(I_LDXzp, reg1);
		ins(I_LDYzp, reg2);
		for (i = 1; i <= regCount; i++)
		{
			if ((srcReg = getRegZPAddr(i, src, type, false)) == -1)
				return;
			ins(I_LDAzp, srcReg);
			ins(I_SPA);
		}
		ins(I_PLX);
	}
	
	public void genBinOp(int dst, int src1, int src2, int op, int type)
	{
		int opType = op >>> 16, opPar = op & 0xFFFF;
		int dstR, srcR1, srcR2, i, count;
		Instruction end, redo;
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
					if ((dstR = getRegZPAddr(1, dst, StdTypes.T_BYTE, true)) == -1 || (srcR1 = getRegZPAddr(1, src1, StdTypes.T_BYTE, false)) == -1 || (srcR2 = getRegZPAddr(1, src2, StdTypes.T_BYTE, false)) == -1)
						return;
					ins(I_PHX);
					ins(I_LDAzp, srcR1);
					ins(I_MULzp, srcR2);
					ins(I_STAzp, dstR);
					ins(I_PLX);
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
							if ((dstR = getRegZPAddr(i, dst, type, true)) == -1 || (srcR1 = getRegZPAddr(i, src1, type, false)) == -1 || (srcR2 = getRegZPAddr(i, src2, type, false)) == -1)
								return;
							ins(I_LDAzp, srcR1);
							switch (opPar)
							{
								case Ops.A_AND:
									ins(I_ANDzp, srcR2);
									break;
								case Ops.A_OR:
									ins(I_ORzp, srcR2);
									break;
								case Ops.A_XOR:
									ins(I_EORzp, srcR2);
									break;
								case Ops.A_MINUS:
									if (i == 1)
										ins(I_SEC);
									ins(I_SBCzp, srcR2);
									break;
								case Ops.A_PLUS:
									if (i == 1)
										ins(I_CLC);
									ins(I_ADCzp, srcR2);
									break;
								default:
									fatalError("unsupported ari-operation for genBinOp");
									return;
							}
							ins(I_STAzp, dstR);
						}
						return;
					case Ops.S_BSH:
						end = getUnlinkedInstruction();
						if ((srcR2 = getRegZPAddr(1, src2, type, false)) == -1)
							return;
						ins(I_LDYzp, srcR2);
						insPatchedJmp(I_JPZimm, end);
						redo = getUnlinkedInstruction();
						appendInstruction(redo);
						if ((dstR = getRegZPAddr(opPar == Ops.B_SHL ? 1 : count, dst, type, true)) == -1 || (srcR1 = getRegZPAddr(opPar == Ops.B_SHL ? 1 : count, src1, type, false)) == -1)
							return;
						switch (opPar)
						{
							case Ops.B_SHL:
								if (srcR1 != dstR)
								{
									ins(I_LDAzp, srcR1);
									ins(I_SHL);
									ins(I_STAzp, dstR);
								}
								else
									ins(I_SHLzp, dstR);
								i = 2;
								break;
							case Ops.B_SHRL:
								if (srcR1 != dstR)
								{
									ins(I_LDAzp, srcR1);
									ins(I_SHR);
									ins(I_STAzp, dstR);
								}
								else
									ins(I_SHRzp, dstR);
								i = count - 1;
								break;
							case Ops.B_SHRA:
								ins(I_LDAzp, srcR1);
								ins(I_SHL);
								ins(I_LDAzp, srcR1);
								ins(I_ROR);
								ins(I_STAzp, dstR);
								i = count - 1;
								break;
							default:
								fatalError("unsupported bsh-operation for genBinOp");
								return;
						}
						while (--count > 0)
						{ //higher bytes - respect carry
							if ((dstR = getRegZPAddr(i, dst, type, true)) == -1 || (srcR1 = getRegZPAddr(i, src1, type, false)) == -1)
								return;
							if (opPar == Ops.B_SHL)
							{
								if (dstR != srcR1)
								{
									ins(I_LDAzp, srcR1);
									ins(I_ROL);
									ins(I_STAzp, dstR);
								}
								else
									ins(I_ROLzp, dstR);
								i++;
							}
							else
							{
								if (dstR != srcR1)
								{
									ins(I_LDAzp, srcR1);
									ins(I_ROR);
									ins(I_STAzp, dstR);
								}
								else
									ins(I_RORzp, dstR);
								i--;
							}
						}
						insPatchedJmp(I_DYJPimm, redo); //decrease counter and jump to redo
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
	
	public void genUnaOp(int dst, int src, int op, int type)
	{
		int opPar = op & 0xFFFF, dstR, srcR, count, i = 1;
		if (type == StdTypes.T_FLT || type == StdTypes.T_DBL)
		{
			fatalError("unary operator not yet supported for float and double");
			return;
		}
		if ((dstR = getRegZPAddr(1, dst, type, true)) == -1 || (srcR = getRegZPAddr(1, src, type, false)) == -1)
			return;
		if (type == StdTypes.T_BOOL)
		{
			if (opPar == Ops.L_NOT)
			{
				ins(I_LDAzp, srcR);
				ins(I_EORimm, 1);
				ins(I_STAzp, dstR);
				return;
			}
			else
			{
				fatalError("unsupported bool-operation for genUnaOp");
				return;
			}
		}
		else if (opPar == Ops.A_MINUS)
		{
			ins(I_CLA);
			ins(I_SEC);
			ins(I_SBCzp, srcR);
			ins(I_STAzp, dstR);
			count = getByteCount(type);
			for (i = 2; i <= count; i++)
			{
				if ((dstR = getRegZPAddr(i, dst, type, true)) == -1 || (srcR = getRegZPAddr(i, src, type, false)) == -1)
					return;
				ins(I_CLA);
				ins(I_SBCzp, srcR);
				ins(I_STAzp, dstR);
			}
			return;
		}
		else if (opPar == Ops.A_CPL)
		{
			count = getByteCount(type);
			for (; i <= count; i++)
			{
				if ((dstR = getRegZPAddr(i, dst, type, true)) == -1 || (srcR = getRegZPAddr(i, src, type, false)) == -1)
					return;
				ins(I_LDAzp, srcR);
				ins(I_EORimm, 0xFF);
				ins(I_STAzp, dstR);
			}
			return;
		}
		if (opPar != Ops.A_PLUS)
		{ //do nothing for Ops.A_PLUS
			fatalError("unsupported operation for genUnaOp");
		}
	}
	
	public void genIncMem(int dst, int type)
	{
		int i, regCount, reg1, reg2;
		regCount = getByteCount(type);
		if ((reg1 = getRegZPAddr(1, dst, StdTypes.T_PTR, false)) == -1 || (reg2 = getRegZPAddr(2, dst, StdTypes.T_PTR, false)) == -1)
			return;
		ins(I_CLY);
		if (reg2 != reg1 + 1)
		{
			ins(I_MOVzpzp, RegTmpLo, reg1);
			ins(I_MOVzpzp, RegTmpHi, reg2);
			reg1 = RegTmpLo;
			reg2 = RegTmpHi;
		}
		if (regCount == 1)
		{
			ins(I_INCizpy, reg1);
			return;
		}
		ins(I_CLC);
		ins(I_LDAizpy, reg1);
		ins(I_ADCimm, 1);
		ins(I_STAizpy, reg1);
		for (i = 1; i < regCount; i++)
		{
			ins(I_INY);
			ins(I_LDAizpy, reg1);
			ins(I_ADCimm, 0);
			ins(I_STAizpy, reg1);
		}
	}
	
	public void genDecMem(int dst, int type)
	{
		int i, regCount, reg1, reg2;
		regCount = getByteCount(type);
		if ((reg1 = getRegZPAddr(1, dst, StdTypes.T_PTR, false)) == -1 || (reg2 = getRegZPAddr(2, dst, StdTypes.T_PTR, false)) == -1)
			return;
		ins(I_CLY);
		if (reg2 != reg1 + 1)
		{
			ins(I_MOVzpzp, RegTmpLo, reg1);
			ins(I_MOVzpzp, RegTmpHi, reg2);
			reg1 = RegTmpLo;
			reg2 = RegTmpHi;
		}
		if (regCount == 1)
		{
			ins(I_DECizpy, reg1);
			return;
		}
		ins(I_SEC);
		ins(I_LDAizpy, reg1);
		ins(I_SBCimm, 1);
		ins(I_STAizpy, reg1);
		for (i = 1; i < regCount; i++)
		{
			ins(I_INY);
			ins(I_LDAizpy, reg1);
			ins(I_SBCimm, 0);
			ins(I_STAizpy, reg1);
		}
	}
	
	public void genSaveUnitContext()
	{
		ins(I_PUSHzp, ZPAddrClssHi);
		ins(I_PUSHzp, ZPAddrClssLo);
	}
	
	public void genRestUnitContext()
	{
		ins(I_POPzp, ZPAddrClssLo);
		ins(I_POPzp, ZPAddrClssHi);
	}
	
	public void genLoadUnitContext(int dst, int off)
	{
		fatalError("genLoadUnitContext not implemented yet");
	}
	
	public void genLoadConstUnitContext(int dst, Object unitLoc)
	{
		genLoadConstVal(dst, mem.getAddrAsInt(unitLoc, 0), StdTypes.T_SHRT);
	}
	
	public void genSaveInstContext()
	{
		ins(I_PUSHzp, ZPAddrInstHi);
		ins(I_PUSHzp, ZPAddrInstLo);
	}
	
	public void genRestInstContext()
	{
		ins(I_POPzp, ZPAddrInstLo);
		ins(I_POPzp, ZPAddrInstHi);
	}
	
	public void genLoadInstContext(int src)
	{
		int src1, src2;
		if ((src1 = getRegZPAddr(1, src, StdTypes.T_PTR, false)) == -1 || (src2 = getRegZPAddr(2, src, StdTypes.T_PTR, false)) == -1)
			return;
		ins(I_MOVzpzp, ZPAddrInstLo, src1);
		ins(I_MOVzpzp, ZPAddrInstHi, src2);
		ins(I_PHX);
		ins(I_LDAzp, src1);
		ins(I_STAzp, ZPAddrInstLo);
		ins(I_CLC);
		ins(I_ADCimm, 0xFE);
		ins(I_TAX);
		ins(I_LDAzp, src2);
		ins(I_STAzp, ZPAddrInstHi);
		ins(I_ADCimm, 0xFF);
		ins(I_TAY);
		ins(I_LPA);
		ins(I_STAzp, ZPAddrClssLo);
		ins(I_LPA);
		ins(I_STAzp, ZPAddrClssHi);
		ins(I_PLX);
	}
	
	public void genCall(int off, int clssReg, int parSize)
	{
		int cr1, cr2;
		if ((cr1 = getRegZPAddr(1, clssReg, StdTypes.T_PTR, false)) == -1 || (cr2 = getRegZPAddr(2, clssReg, StdTypes.T_PTR, false)) == -1)
			return;
		ins(I_PHX);
		ins(I_CLC);
		ins(I_LDAzp, cr1);
		ins(I_ADCimm, off & 0xFF);
		ins(I_TAX);
		ins(I_LDAzp, cr2);
		ins(I_ADCimm, off >>> 8);
		ins(I_TAY);
		if (ctx.codeStart == 0)
		{
			ins(I_LPA);
			ins(I_STAzp, ZPAddrTmpLo);
			ins(I_LPA);
			ins(I_STAzp, ZPAddrTmpHi);
			ins(I_PLX);
			ins(I_JSRmem, ZPAddrTmpLo);
		}
		else
		{
			ins(I_CLC);
			ins(I_LPA);
			ins(I_ADCimm, ctx.codeStart & 0xFF);
			ins(I_STAzp, ZPAddrTmpLo);
			ins(I_LPA);
			ins(I_ADCimm, (ctx.codeStart >>> 8) & 0xFF);
			ins(I_STAzp, ZPAddrTmpHi);
			ins(I_PLX);
			ins(I_JSRmem, ZPAddrTmpLo);
		}
		insCleanStackAfterCall(parSize);
	}
	
	public void genCallIndexed(int intfReg, int off, int parSize)
	{
		int cr1, cr2;
		if ((cr1 = getRegZPAddr(3, intfReg, StdTypes.T_DPTR, false)) == -1 || (cr2 = getRegZPAddr(4, intfReg, StdTypes.T_DPTR, false)) == -1)
			return;
		ins(I_PHX);
		ins(I_CLC);
		ins(I_LDAzp, cr1);
		ins(I_ADCimm, off & 0xFF);
		ins(I_TAX);
		ins(I_LDAzp, cr2);
		ins(I_ADCimm, off >>> 8);
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
		if (ctx.codeStart == 0)
		{
			ins(I_LPA);
			ins(I_STAzp, ZPAddrTmpLo);
			ins(I_LPA);
			ins(I_STAzp, ZPAddrTmpHi);
			ins(I_PLX);
			ins(I_JSRmem, ZPAddrTmpLo);
		}
		else
		{
			ins(I_CLC);
			ins(I_LPA);
			ins(I_ADCimm, ctx.codeStart & 0xFF);
			ins(I_STAzp, ZPAddrTmpLo);
			ins(I_LPA);
			ins(I_ADCimm, (ctx.codeStart >>> 8) & 0xFF);
			ins(I_STAzp, ZPAddrTmpHi);
			ins(I_PLX);
			ins(I_JSRmem, ZPAddrTmpLo);
		}
		insCleanStackAfterCall(parSize);
	}
	
	public void genCallConst(Mthd obj, int parSize)
	{
		insPatchedCall(obj, parSize);
		insCleanStackAfterCall(parSize);
	}
	
	public void genJmp(Instruction dest)
	{
		insPatchedJmp(I_JMPimm, dest);
	}
	
	public void genCondJmp(Instruction dest, int cond)
	{
		Instruction here;
		switch (cond)
		{
			case Ops.C_EQ: //"=="
				insPatchedJmp(I_JPZimm, dest);
				break;
			case Ops.C_NE: //"!="
				insPatchedJmp(I_JNZimm, dest);
				break;
			case Ops.C_LW: //"<"
			case Ops.C_BO: //unsigned "<" i.e. below (sign "not"-handled in genComp)
				insPatchedJmp(I_JNCimm, dest);
				break;
			case Ops.C_GE: //">="
				insPatchedJmp(I_JPCimm, dest);
				break;
			case Ops.C_LE: //"<="
				insPatchedJmp(I_JNCimm, dest);
				insPatchedJmp(I_JPZimm, dest);
				break;
			case Ops.C_GT: //">"
				here = getUnlinkedInstruction();
				insPatchedJmp(I_JPZimm, here);
				insPatchedJmp(I_JPCimm, dest);
				appendInstruction(here);
				break;
			default:
				fatalError("unsupported jump in genCondJmp");
		}
	}
	
	public int genComp(int src1, int src2, int type, int cond)
	{
		int i, max, r1, r2;
		Instruction zeroKeeper;
		max = getByteCount(type);
		if (cond != Ops.C_EQ && cond != Ops.C_NE && cond != Ops.C_BO && type != StdTypes.T_CHAR)
		{
			if ((r1 = getRegZPAddr(max, src1, type, false)) == -1 || (r2 = getRegZPAddr(max, src2, type, false)) == -1)
				return 0;
			ins(I_LDAzp, r1);
			ins(I_EORimm, 0x80);
			ins(I_STAzp, ZPAddrTmpLo);
			ins(I_LDAzp, r2);
			ins(I_EORimm, 0x80);
			ins(I_STAzp, ZPAddrTmpHi);
		}
		if (max > 1)
			ins(I_CLY);
		for (i = 1; i <= max; i++)
		{
			if (i == max && cond != Ops.C_EQ && cond != Ops.C_NE && cond != Ops.C_BO && type != StdTypes.T_CHAR)
			{
				r1 = ZPAddrTmpLo;
				r2 = ZPAddrTmpHi;
			}
			else if ((r1 = getRegZPAddr(i, src1, type, false)) == -1 || (r2 = getRegZPAddr(i, src2, type, false)) == -1)
				return 0;
			ins(I_LDAzp, r1);
			ins(i == 1 ? I_CMPzp : I_SBCzp, r2);
			if (max > 1)
			{
				zeroKeeper = getUnlinkedInstruction();
				insPatchedJmp(I_JPZimm, zeroKeeper);
				ins(I_INY);
				appendInstruction(zeroKeeper);
			}
		}
		if (max > 1)
			ins(I_TYA);
		return cond;
	}
	
	public int genCompValToConstVal(int src, int val, int type, int cond)
	{
		int i, max, r;
		Instruction zeroKeeper;
		max = getByteCount(type);
		if (cond != Ops.C_EQ && cond != Ops.C_NE && cond != Ops.C_BO)
		{
			if ((r = getRegZPAddr(max, src, type, false)) == -1)
				return 0;
			ins(I_LDAzp, r);
			ins(I_EORimm, 0x80);
			ins(I_STAzp, ZPAddrTmpLo);
		}
		if (max > 1)
			ins(I_CLY);
		for (i = 1; i <= max; i++)
		{
			if ((r = getRegZPAddr(i, src, type, false)) == -1)
				return 0;
			if (i == max && cond != Ops.C_EQ && cond != Ops.C_NE && cond != Ops.C_BO)
			{
				ins(I_LDAzp, ZPAddrTmpLo);
				ins(i == 1 ? I_CMPimm : I_SBCimm, ((val >>> ((i - 1) << 3)) & 0xFF) ^ 0x80);
			}
			else
			{
				ins(I_LDAzp, r);
				ins(i == 1 ? I_CMPimm : I_SBCimm, (val >>> ((i - 1) << 3)) & 0xFF);
			}
			if (max > 1)
			{
				zeroKeeper = getUnlinkedInstruction();
				insPatchedJmp(I_JPZimm, zeroKeeper);
				ins(I_INY);
				appendInstruction(zeroKeeper);
			}
		}
		if (max > 1)
			ins(I_TYA);
		return cond;
	}
	
	public int genCompValToConstDoubleOrLongVal(int src, long val, boolean asDouble, int cond)
	{
		int i, r;
		Instruction zeroKeeper;
		if (cond != Ops.C_EQ && cond != Ops.C_NE && cond != Ops.C_BO)
		{
			if ((r = getRegZPAddr(8, src, StdTypes.T_LONG, false)) == -1)
				return 0;
			ins(I_LDAzp, r);
			ins(I_EORimm, 0x80);
			ins(I_STAzp, ZPAddrTmpLo);
		}
		ins(I_CLY);
		for (i = 1; i <= 8; i++)
		{
			if ((r = getRegZPAddr(i, src, StdTypes.T_LONG, false)) == -1)
				return 0;
			if (i == 1 && cond != Ops.C_EQ && cond != Ops.C_NE && cond != Ops.C_BO)
			{
				ins(I_LDAzp, ZPAddrTmpLo);
				ins(i == 1 ? I_CMPimm : I_SBCimm, ((int) ((val >>> ((i - 1) << 3))) & 0xFF) ^ 0x80);
			}
			else
			{
				ins(I_LDAzp, r);
				ins(i == 1 ? I_CMPimm : I_SBCimm, ((int) (val >>> ((i - 1) << 3))) & 0xFF);
			}
			zeroKeeper = getUnlinkedInstruction();
			insPatchedJmp(I_JPZimm, zeroKeeper);
			ins(I_INY);
			appendInstruction(zeroKeeper);
		}
		ins(I_TYA);
		return cond;
	}
	
	public int genCompPtrToNull(int reg, int cond)
	{
		int cr1, cr2;
		if ((cr1 = getRegZPAddr(1, reg, StdTypes.T_PTR, false)) == -1 || (cr2 = getRegZPAddr(2, reg, StdTypes.T_PTR, false)) == -1)
			return 0;
		ins(I_LDAzp, cr1);
		ins(I_ORzp, cr2);
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
		int tempReg, primReg, i = 1, count;
		for (count = getByteCount(type); i <= count; i++)
		{
			if ((tempReg = getRegZPAddr(i, src, type, false)) == -1)
				return;
			primReg = getZPAddrOfReg(i - 1);
			if (tempReg != primReg)
				ins(I_MOVzpzp, primReg, tempReg);
		}
	}
	
	public void genMoveFromPrimary(int dst, int type)
	{
		int tempReg, primReg, i = getByteCount(type);
		while (i > 0)
		{
			if ((tempReg = getRegZPAddr(i, dst, type, true)) == -1)
				return;
			writtenRegs |= (1 << tempReg);
			primReg = getZPAddrOfReg(--i);
			if (primReg != tempReg)
				ins(I_MOVzpzp, tempReg, primReg);
		}
	}
	
	public void genMoveIntfMapFromPrimary(int dst)
	{
		int r1, r2;
		if ((r1 = getRegZPAddr(3, dst, StdTypes.T_DPTR, true)) == 0 || (r2 = getRegZPAddr(4, dst, StdTypes.T_DPTR, true)) == 0)
			return;
		ins(I_MOVzpzp, r1, getZPAddrOfReg(0)); //interface map double pointer
		ins(I_MOVzpzp, r2, getZPAddrOfReg(1)); //  is never in r0/r1, so always move
	}
	
	public void genSavePrimary(int type)
	{
		int primReg, count;
		count = getByteCount(type);
		for (primReg = 0; count > 0; primReg++, count--)
			ins(I_PUSHzp, getZPAddrOfReg(primReg));
	}
	
	public void genRestPrimary(int type)
	{
		int primReg, count;
		count = getByteCount(type);
		for (primReg = count - 1; count > 0; primReg--, count--)
			ins(I_POPzp, getZPAddrOfReg(primReg));
	}
	
	public void genCheckBounds(int addrReg, int offReg, int checkToOffset, Instruction onSuccess)
	{
		int addrR1, addrR2, offR1, offR2; //although offReg is of type INT, we use only lower 2 bytes
		if ((addrR1 = getRegZPAddr(1, addrReg, StdTypes.T_PTR, false)) == -1 || (addrR2 = getRegZPAddr(2, addrReg, StdTypes.T_PTR, false)) == -1 || (offR1 = getRegZPAddr(1, offReg, StdTypes.T_PTR, false)) == -1 || (offR2 = getRegZPAddr(2, offReg, StdTypes.T_PTR, false)) == -1)
			return;
		ins(I_PHX);
		ins(I_CLC);
		ins(I_LDAzp, addrR1);
		ins(I_ADCimm, checkToOffset & 0xFF);
		ins(I_TAX);
		ins(I_LDAzp, addrR2);
		ins(I_ADCimm, (checkToOffset >>> 8) & 0xFF);
		ins(I_TAY);
		ins(I_LPA);
		ins(I_STAzp, ZPAddrTmpLo);
		ins(I_LPA);
		ins(I_STAzp, ZPAddrTmpHi);
		ins(I_PLX);
		ins(I_LDAzp, offR1);
		ins(I_CMPzp, ZPAddrTmpLo);
		ins(I_LDAzp, offR2);
		ins(I_SBCzp, ZPAddrTmpHi);
		insPatchedJmp(I_JNCimm, onSuccess); //if (off<limit) jump everything is ok
	}
	
	public void genCheckStackExtreme(int maxValueReg, Instruction onSuccess)
	{
		fatalError("stack extreme check not supported");
	}
	
	public void genLoadDerefAddr(int destReg, int objReg, int indReg, int baseOffset, int entrySize)
	{
		int dstR1, dstR2, objR1, objR2, indR1, indR2;
		if ((dstR1 = getRegZPAddr(1, destReg, StdTypes.T_SHRT, true)) == -1 || (dstR2 = getRegZPAddr(2, destReg, StdTypes.T_SHRT, true)) == -1 || (objR1 = getRegZPAddr(1, objReg, StdTypes.T_SHRT, false)) == -1 || (objR2 = getRegZPAddr(2, objReg, StdTypes.T_SHRT, false)) == -1 || (indR1 = getRegZPAddr(1, indReg, StdTypes.T_SHRT, false)) == -1 || (indR2 = getRegZPAddr(2, indReg, StdTypes.T_SHRT, false)) == -1)
			return;
		if (entrySize < 0)
		{
			ins(I_CLA);
			ins(I_SEC);
			ins(I_SBCzp, indR1);
			ins(I_STAzp, ZPAddrTmpLo);
			ins(I_CLA);
			ins(I_SBCzp, indR2);
			ins(I_STAzp, ZPAddrTmpHi);
			entrySize = -entrySize;
		}
		else
		{
			ins(I_MOVzpzp, ZPAddrTmpLo, indR1);
			ins(I_MOVzpzp, ZPAddrTmpHi, indR2);
		}
		switch (entrySize)
		{
			case 1:
			case 2:
			case 4:
			case 8:
				while (entrySize > 1)
				{
					ins(I_SHLzp, ZPAddrTmpLo);
					ins(I_ROLzp, ZPAddrTmpHi);
					entrySize = entrySize >>> 1;
				}
				break;
			default:
				fatalError("not supported entrySize in genLoadDerefAddr");
				return;
		}
		if (baseOffset != 0)
		{
			ins(I_CLC);
			ins(I_LDAimm, baseOffset & 0xFF);
			ins(I_ADCzp, ZPAddrTmpLo);
			ins(I_STAzp, ZPAddrTmpLo);
			ins(I_LDAimm, (baseOffset >>> 8) & 0xFF);
			ins(I_ADCzp, ZPAddrTmpHi);
			ins(I_STAzp, ZPAddrTmpHi);
		}
		ins(I_CLC);
		ins(I_LDAzp, objR1);
		ins(I_ADCzp, ZPAddrTmpLo);
		ins(I_STAzp, dstR1);
		ins(I_LDAzp, objR2);
		ins(I_ADCzp, ZPAddrTmpHi);
		ins(I_STAzp, dstR2);
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
		throwBlockOffset += STACKMEM_OFF;
		//get global frame pointer, save in current frame, set current frame as global
		ins(I_CLY); //load current global pointer and store throwBlockOffset+0/1
		ins(I_LDAizpy, ZPAddrBase);
		ins(I_STAabsx, throwBlockOffset);
		ins(I_INY);
		ins(I_LDAizpy, ZPAddrBase);
		ins(I_STAabsx, throwBlockOffset + 1);
		ins(I_TXA); //calculate current throw frame address, store in global pointer
		ins(I_CLY);
		ins(I_CLC);
		ins(I_ADCimm, throwBlockOffset & 0xFF);
		ins(I_STAizpy, ZPAddrBase);
		ins(I_CLA);
		ins(I_ADCimm, (throwBlockOffset >>> 8) & 0xFF);
		ins(I_INY);
		ins(I_STAizpy, ZPAddrBase);
		//insert destination code address
		insPatchedJmp(I_PUSAimm, dest);
		ins(I_PLA);
		ins(I_STAabsx, throwBlockOffset + 2);
		ins(I_PLA);
		ins(I_STAabsx, throwBlockOffset + 3);
		//fill unit- and instance context, stack and base pointer
		ins(I_LDAzp, ZPAddrClssLo);
		ins(I_STAabsx, throwBlockOffset + 4);
		ins(I_LDAzp, ZPAddrClssHi);
		ins(I_STAabsx, throwBlockOffset + 5);
		ins(I_LDAzp, ZPAddrInstLo);
		ins(I_STAabsx, throwBlockOffset + 6);
		ins(I_LDAzp, ZPAddrInstHi);
		ins(I_STAabsx, throwBlockOffset + 7);
		ins(I_TXY);
		ins(I_TSX);
		ins(I_TXA);
		ins(I_TYX);
		ins(I_STAabsx, throwBlockOffset + 8);
		ins(I_TXA);
		ins(I_STAabsx, throwBlockOffset + 9);
		//clear exception thrown
		ins(I_CLA);
		ins(I_STAabsx, throwBlockOffset + 10);
		ins(I_STAabsx, throwBlockOffset + 11);
	}
	
	public void genThrowFrameUpdate(Instruction oldDest, Instruction newDest, int throwBlockOffset)
	{
		if (throwBlockOffset >= 0)
		{ //throwBlockOffset is in local vars
			fatalError(ERR_INVGLOBADDRREG);
			return;
		}
		throwBlockOffset += STACKMEM_OFF;
		insPatchedJmp(I_PUSAimm, newDest);
		ins(I_PLA);
		ins(I_STAabsx, throwBlockOffset + 2);
		ins(I_PLA);
		ins(I_STAabsx, throwBlockOffset + 3);
	}
	
	public void genThrowFrameReset(int globalAddrReg, int throwBlockOffset)
	{
		if (globalAddrReg != 0x0003 || usedRegs != 0x0003 || throwBlockOffset >= 0)
		{ //globalAddrReg is the only allocated register, throwBlockOffset is in local vars
			fatalError(ERR_INVGLOBADDRREG);
			return;
		}
		//load previous pointer from current frame and store in global variable
		throwBlockOffset += STACKMEM_OFF;
		ins(I_CLY);
		ins(I_LDAabsx, throwBlockOffset);
		ins(I_STAizpy, ZPAddrBase);
		ins(I_INY);
		ins(I_LDAabsx, throwBlockOffset + 1);
		ins(I_STAizpy, ZPAddrBase);
	}
	
	public void inlineVarOffset(int inlineMode, int objReg, Object loc, int offset, int baseValue)
	{
		fatalError("inlining of variable offsets is not supported");
	}
	
	public void inlineCodeAddress(boolean defineHere, int addOffset)
	{
		Instruction i;
		appendInstruction(i = getUnlinkedInstruction());
		if (defineHere)
			i.type = IT_DEFICA;
		else
		{
			i.type = IT_INLCODA;
			i.iPar1 = addOffset;
			i.putShort(0); //dummy address
		}
	}
	
	//optimized storing and calculation
	public void genStoreVarVal(int objReg, Object loc, int off, int valReg, int type)
	{
		int valR, objR, count, i;
		count = getByteCount(type);
		int pos = mem.getAddrAsInt(loc, off);
		if (objReg == regBase)
		{
			if (pos >= 0)
				pos += curVarOffParam;
			pos += STACKMEM_OFF;
			for (i = 0; i < count; i++)
			{
				if ((valR = getRegZPAddr(i + 1, valReg, type, false)) == -1)
					return;
				ins(I_LDAzp, valR);
				ins(I_STAabsx, pos + i);
			}
		}
		else
		{
			ins(I_PHX);
			if (objReg == 0)
			{
				ins(I_LDXimm, pos & 0xFF);
				ins(I_LDYimm, pos >>> 8);
			}
			else
			{
				ins(I_CLC);
				if ((objR = getRegZPAddr(1, objReg, StdTypes.T_PTR, false)) == -1)
					return;
				ins(I_LDAzp, objR);
				ins(I_ADCimm, pos & 0xFF);
				ins(I_TAX);
				if ((objR = getRegZPAddr(2, objReg, StdTypes.T_PTR, false)) == -1)
					return;
				ins(I_LDAzp, objR);
				ins(I_ADCimm, pos >>> 8);
				ins(I_TAY);
			}
			for (i = 0; i < count; i++)
			{
				if ((valR = getRegZPAddr(i + 1, valReg, type, false)) == -1)
					return;
				ins(I_LDAzp, valR);
				ins(I_SPA);
			}
			ins(I_PLX);
		}
	}
	
	public void genStoreVarConstVal(int objReg, Object loc, int off, int val, int type)
	{
		int objR, count, i;
		count = getByteCount(type);
		int pos = mem.getAddrAsInt(loc, off);
		if (objReg == regBase)
		{
			if (pos >= 0)
				pos += curVarOffParam;
			pos += STACKMEM_OFF;
			for (i = 0; i < count; i++)
			{
				ins(I_LDAimm, (val >>> (i << 3)) & 0xFF);
				ins(I_STAabsx, pos + i);
			}
		}
		else
		{
			ins(I_PHX);
			if (objReg == 0)
			{
				ins(I_LDXimm, pos & 0xFF);
				ins(I_LDYimm, pos >>> 8);
			}
			else
			{
				ins(I_CLC);
				if ((objR = getRegZPAddr(1, objReg, StdTypes.T_PTR, false)) == -1)
					return;
				ins(I_LDAzp, objR);
				ins(I_ADCimm, pos & 0xFF);
				ins(I_TAX);
				if ((objR = getRegZPAddr(2, objReg, StdTypes.T_PTR, false)) == -1)
					return;
				ins(I_LDAzp, objR);
				ins(I_ADCimm, pos >>> 8);
				ins(I_TAY);
			}
			for (i = 0; i < count; i++)
			{
				ins(I_LDAimm, (val >>> (i << 3)) & 0xFF);
				ins(I_SPA);
			}
			ins(I_PLX);
		}
	}
	
	public void genStoreVarConstDoubleOrLongVal(int objReg, Object loc, int off, long val, boolean asDouble)
	{
		int objR, i;
		int pos = mem.getAddrAsInt(loc, off);
		if (objReg == regBase)
		{
			if (pos >= 0)
				pos += curVarOffParam;
			pos += STACKMEM_OFF;
			for (i = 0; i < 8; i++)
			{
				ins(I_LDAimm, ((int) (val >>> (i << 3))) & 0xFF);
				ins(I_STAabsx, pos + i);
			}
		}
		else
		{
			ins(I_PHX);
			if (objReg == 0)
			{
				ins(I_LDXimm, pos & 0xFF);
				ins(I_LDYimm, pos >>> 8);
			}
			else
			{
				ins(I_CLC);
				if ((objR = getRegZPAddr(1, objReg, StdTypes.T_PTR, false)) == -1)
					return;
				ins(I_LDAzp, objR);
				ins(I_ADCimm, pos & 0xFF);
				ins(I_TAX);
				if ((objR = getRegZPAddr(2, objReg, StdTypes.T_PTR, false)) == -1)
					return;
				ins(I_LDAzp, objR);
				ins(I_ADCimm, pos >>> 8);
				ins(I_TAY);
			}
			for (i = 0; i < 8; i++)
			{
				ins(I_LDAimm, ((int) (val >>> (i << 3))) & 0xFF);
				ins(I_SPA);
			}
			ins(I_PLX);
		}
	}
	
	public void genBinOpConstRi(int dst, int src1, int val, int op, int type)
	{ //may destroy src1
		int opType = op >>> 16, opPar = op & 0xFFFF;
		int dstR, srcR1, i, count, tmpVal;
		Instruction redo;
		count = getByteCount(type);
		switch (type)
		{
			case StdTypes.T_BOOL:
				if (opType != Ops.S_ARI || !(opPar == Ops.A_AND || opPar == Ops.A_OR || opPar == Ops.A_XOR))
				{
					fatalError("unsupported operation for bool-genBinOpConstRi");
					return;
				}
				//has to do the following, too!
			case StdTypes.T_BYTE:
				if (opPar == Ops.A_MUL)
				{ //special case: mul for byte is supported
					if ((dstR = getRegZPAddr(1, dst, StdTypes.T_BYTE, true)) == -1 || (srcR1 = getRegZPAddr(1, src1, StdTypes.T_BYTE, false)) == -1)
						return;
					ins(I_PHX);
					ins(I_LDAzp, srcR1);
					ins(I_MULimm, val & 0xFF);
					ins(I_STAzp, dstR);
					ins(I_PLX);
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
						for (i = 0; i < count; i++)
						{
							if ((dstR = getRegZPAddr(i + 1, dst, type, true)) == -1 || (srcR1 = getRegZPAddr(i + 1, src1, type, false)) == -1)
								return;
							tmpVal = (val >>> (i << 3)) & 0xFF;
							ins(I_LDAzp, srcR1);
							switch (opPar)
							{
								case Ops.A_AND:
									ins(I_ANDimm, tmpVal);
									break;
								case Ops.A_OR:
									ins(I_ORimm, tmpVal);
									break;
								case Ops.A_XOR:
									ins(I_EORimm, tmpVal);
									break;
								case Ops.A_MINUS:
									if (i == 0)
										ins(I_SEC);
									ins(I_SBCimm, tmpVal);
									break;
								case Ops.A_PLUS:
									if (i == 0)
										ins(I_CLC);
									ins(I_ADCimm, tmpVal);
									break;
								default:
									fatalError("unsupported ari-operation for genBinOpConstRi");
									return;
							}
							ins(I_STAzp, dstR);
						}
						return;
					case Ops.S_BSH:
						if (val == 0)
							return; //do not shift
						ins(I_LDYimm, val);
						redo = getUnlinkedInstruction();
						appendInstruction(redo);
						if ((dstR = getRegZPAddr(opPar == Ops.B_SHL ? 1 : count, dst, StdTypes.T_BYTE, true)) == -1 || (srcR1 = getRegZPAddr(opPar == Ops.B_SHL ? 1 : count, src1, StdTypes.T_BYTE, false)) == -1)
							return;
						switch (opPar)
						{
							case Ops.B_SHL:
								if (srcR1 != dstR)
								{
									ins(I_LDAzp, srcR1);
									ins(I_SHL);
									ins(I_STAzp, dstR);
								}
								else
									ins(I_SHLzp, dstR);
								i = 2;
								break;
							case Ops.B_SHRL:
								if (srcR1 != dstR)
								{
									ins(I_LDAzp, srcR1);
									ins(I_SHR);
									ins(I_STAzp, dstR);
								}
								else
									ins(I_SHRzp, dstR);
								i = count - 1;
								break;
							case Ops.B_SHRA:
								ins(I_LDAzp, srcR1);
								ins(I_SHL);
								ins(I_LDAzp, srcR1);
								ins(I_ROR);
								ins(I_STAzp, dstR);
								i = count - 1;
								break;
							default:
								fatalError("unsupported bsh-operation for genBinOpConstRi");
								return;
						}
						while (--count > 0)
						{ //higher bytes - respect carry
							if ((dstR = getRegZPAddr(i, dst, type, true)) == -1 || (srcR1 = getRegZPAddr(i, src1, type, false)) == -1)
								return;
							if (opPar == Ops.B_SHL)
							{
								if (dstR != srcR1)
								{
									ins(I_LDAzp, srcR1);
									ins(I_ROL);
									ins(I_STAzp, dstR);
								}
								else
									ins(I_ROLzp, dstR);
								i++;
							}
							else
							{
								if (dstR != srcR1)
								{
									ins(I_LDAzp, srcR1);
									ins(I_ROR);
									ins(I_STAzp, dstR);
								}
								else
									ins(I_RORzp, dstR);
								i--;
							}
						}
						insPatchedJmp(I_DYJPimm, redo); //decrease counter and jump to redo
						return;
					default:
						fatalError("unsupported operation-type for genBinOpConstRi");
						return;
				}
			default:
				fatalError("unsupported operand-type for genBinOpConstRi");
		}
	}
	
	public void genBinOpConstDoubleOrLongRi(int dst, int src1, long val, int op, boolean asDouble)
	{
		int opType = op >>> 16, opPar = op & 0xFFFF;
		int dstR, srcR1, i, tmpVal, count;
		Instruction redo;
		if (asDouble)
			super.genBinOpConstDoubleOrLongRi(dst, src1, val, opPar, asDouble);
		switch (opType)
		{
			case Ops.S_ARI:
				for (i = 0; i < 8; i++)
				{
					if ((dstR = getRegZPAddr(i + 1, dst, StdTypes.T_LONG, true)) == -1 || (srcR1 = getRegZPAddr(i + 1, src1, StdTypes.T_LONG, false)) == -1)
						return;
					tmpVal = ((int) (val >>> (i << 3))) & 0xFF;
					ins(I_LDAzp, srcR1);
					switch (opPar)
					{
						case Ops.A_AND:
							ins(I_ANDimm, tmpVal);
							break;
						case Ops.A_OR:
							ins(I_ORimm, tmpVal);
							break;
						case Ops.A_XOR:
							ins(I_EORimm, tmpVal);
							break;
						case Ops.A_MINUS:
							if (i == 0)
								ins(I_SEC);
							ins(I_SBCimm, tmpVal);
							break;
						case Ops.A_PLUS:
							if (i == 0)
								ins(I_CLC);
							ins(I_ADCimm, tmpVal);
							break;
						default:
							fatalError("unsupported ari-operation for genBinOpConstDoubleOrLongRi");
							return;
					}
					ins(I_STAzp, dstR);
				}
				return;
			case Ops.S_BSH:
				if (val == 0)
					return; //do not shift
				ins(I_LDYimm, (int) val & 0xFF);
				redo = getUnlinkedInstruction();
				appendInstruction(redo);
				if ((dstR = getRegZPAddr(opPar == Ops.B_SHL ? 1 : 8, dst, StdTypes.T_LONG, true)) == -1 || (srcR1 = getRegZPAddr(opPar == Ops.B_SHL ? 1 : 8, src1, StdTypes.T_LONG, false)) == -1)
					return;
				switch (opPar)
				{
					case Ops.B_SHL:
						if (srcR1 != dstR)
						{
							ins(I_LDAzp, srcR1);
							ins(I_SHL);
							ins(I_STAzp, dstR);
						}
						else
							ins(I_SHLzp, dstR);
						i = 2;
						break;
					case Ops.B_SHRL:
						if (srcR1 != dstR)
						{
							ins(I_LDAzp, srcR1);
							ins(I_SHR);
							ins(I_STAzp, dstR);
						}
						else
							ins(I_SHRzp, dstR);
						i = 7;
						break;
					case Ops.B_SHRA:
						ins(I_LDAzp, srcR1);
						ins(I_SHL);
						ins(I_LDAzp, srcR1);
						ins(I_ROR);
						ins(I_STAzp, dstR);
						i = 7;
						break;
					default:
						fatalError("unsupported bsh-operation for genBinOpConstDoubleOrLongRi");
						return;
				}
				count = 8;
				while (--count > 0)
				{ //higher bytes - respect carry
					if ((dstR = getRegZPAddr(i, dst, StdTypes.T_LONG, true)) == -1 || (srcR1 = getRegZPAddr(i, src1, StdTypes.T_LONG, false)) == -1)
						return;
					if (opPar == Ops.B_SHL)
					{
						if (dstR != srcR1)
						{
							ins(I_LDAzp, srcR1);
							ins(I_ROL);
							ins(I_STAzp, dstR);
						}
						else
							ins(I_ROLzp, dstR);
						i++;
					}
					else
					{
						if (dstR != srcR1)
						{
							ins(I_LDAzp, srcR1);
							ins(I_ROR);
							ins(I_STAzp, dstR);
						}
						else
							ins(I_RORzp, dstR);
						i--;
					}
				}
				insPatchedJmp(I_DYJPimm, redo); //decrease counter and jump to redo
				return;
			default:
				fatalError("unsupported operation-type for genBinOpConstDoubleOrLongRi");
				return;
		}
	}
	
	//internal instruction coding
	private void ins(int op)
	{
		if ((op & PAR_MASK) != PAR_NONE)
		{
			fatalError("invalid call to ins(op)");
			return;
		}
		Instruction i;
		appendInstruction(i = getUnlinkedInstruction());
		i.type = IT_NORM;
		i.putByte(op);
	}
	
	private void ins(int op, int par)
	{
		Instruction i;
		appendInstruction(i = getUnlinkedInstruction());
		i.type = IT_NORM;
		i.putByte(op);
		if ((op & PAR_MASK) == PAR_BYTE)
		{
			i.putByte(par);
			return;
		}
		if ((op & PAR_MASK) == PAR_WORD)
		{
			i.putShort(par);
			return;
		}
		fatalError("invalid call to ins(op,par)");
	}
	
	private void ins(int op, int par1, int par2)
	{
		if ((op & PAR_MASK) != (PAR_BYTE | PAR_SECBYTE))
		{
			fatalError("invalid call to ins(op,par,par)");
			return;
		}
		Instruction i;
		appendInstruction(i = getUnlinkedInstruction());
		i.type = IT_NORM;
		i.putByte(op);
		i.putByte(par1);
		i.putByte(par2);
	}
	
	private Instruction insPatchedCall(Mthd refMthd, int parSize)
	{
		Instruction i;
		appendInstruction(i = getUnlinkedInstruction());
		i.type = IT_EXTCALL;
		i.refMthd = refMthd;
		i.putByte(I_JSRimm);
		i.putShort(0); //dummy address
		addToCodeRefFixupList(i, 1);
		return i;
	}
	
	private Instruction insPatchedJmp(int op, Instruction dest)
	{
		Instruction i;
		appendInstruction(i = getUnlinkedInstruction());
		i.type = IT_INTJMP;
		i.jDest = dest;
		i.putByte(op);
		i.putShort(0); //dummy address
		return i;
	}
	
	private void insCleanStackAfterCall(int parSize)
	{
		if (parSize != 0)
		{ //clean parameters from stack
			ins(I_INS, parSize);
		}
	}
}
