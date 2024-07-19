/* Copyright (C) 2006, 2007, 2008, 2009, 2010 Stefan Frenz and Patrick Schmidt
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

package sjc.emulation.ssa;

import sjc.backend.ssa.SSADef;
import sjc.emulation.Emulator;
import sjc.emulation.MethodDisassembly;
import sjc.emulation.Mnemonic;
import sjc.emulation.cond.StackCond;
import sjc.real.Real;

/**
 * Class representing the SSA emulator
 *
 * @author Patrick Schmidt, Stefan Frenz
 * @version 101101 adopted changed I_DEREF
 * version 100927 fixed unsignedness of chars
 * version 100412 added killRegOnJmp2 to support two registers to be killed on jump
 * version 090717 added stack extreme check instruction I_STKCHK
 * version 090626 fixed base pointer relative addressing
 * version 090207 added copyright notice
 * version 081021 adopted changes in I/O-read/write and compare
 * version 080701 updated endless-loop-detection (added flowhints inhibit old algorithm)
 * version 080616 added support for language throwables
 * version 080508 added support for flow hints
 * version 080207 adopted changes in semantics of I_IVOF to hold all parameters
 * version 080203 added support for method inlining and floating point arithmetic
 * version 080122 better error message for invalidly killed registers
 * version 080104 extended insAlloc to support allocation of low-numbered regrange-defined registers
 * version 070917 pre-initialization of stack variables with "0"
 * version 070913 adopted changes in stack frame building
 * version 070721 changed comments
 * version 070501 fixed error message in getMnemonicList, fixed parameter in I_CALLim_p
 * version 070114 added output of parSize for calls
 * version 061203 bugfix in disassembly of assign
 * version 061202 adopted change of genCall
 * version 061109 added bound-condition for int (needed for struct-array-bounds)
 * version 060714 changed bound-semantics
 * version 060620 added kill-on-jump
 * version 060613 several bugfixes
 * version 060608 initial version
 */
public class SSAEmul extends Emulator
{
	
	private final static int INIT_STACK_VALUE = 0x9BFF8;
	
	private final Real real;
	
	/**
	 * Constructor for SSAEmul
	 *
	 * @param ir Real interface implementation for floating point support
	 */
	public SSAEmul(Real ir)
	{
		real = ir;
	}
	
	/**
	 * Method to decode a jump
	 *
	 * @param cond the given condition
	 * @return a String representation of the jump
	 */
	private static String getJump(int cond)
	{
		String result = "jump";
		switch (cond)
		{
			case SSADef.CC_AL:
				return result.concat(" always");
			case SSADef.CC_EQ:
				return result.concat(" ifequa");
			case SSADef.CC_GE:
				return result.concat(" ifeqgr");
			case SSADef.CC_GT:
				return result.concat(" ifgrea");
			case SSADef.CC_LE:
				return result.concat(" ifleeq");
			case SSADef.CC_LW:
				return result.concat(" ifless");
			case SSADef.CC_NE:
				return result.concat(" ifuneq");
			case SSADef.CC_BO:
				return result.concat(" ifbdok");
		}
		return result;
	}
	
	/**
	 * Method to decode the size of a register
	 *
	 * @param typeID the size of the register
	 * @return a String containing the type of the register
	 */
	private static String getType(int typeID)
	{
		switch (typeID)
		{
			case 1:
				return "byte ";
			case 2:
				return "short ";
			case 3:
				return "char ";
			case 4:
				return "int ";
			case 5:
				return "float ";
			case 8:
				return "long ";
			case 9:
				return "double ";
			case -1:
				return "ptr ";
			case -2:
				return "dPtr ";
			default:
				return "!!invTp!! ";
		}
	}
	
	/**
	 * Instruction pointer register
	 */
	private int currentIP;
	
	/**
	 * Hint to determine last real (i.e. not flow-hint) instruction
	 */
	private int endlessLoopHint;
	
	/**
	 * Array containing the special registers for the emulator
	 */
	private Register[] sReg;
	
	/**
	 * Further registers
	 */
	private Register[] regs;
	
	/**
	 * Start offset of current register window
	 */
	private int curRegStartOff;
	
	/**
	 * Current inline level
	 */
	private int curInlineLevel;
	
	/**
	 * Index for the last allocated register
	 */
	private int lastAllocRegIndex;
	
	private int killRegOnJmp1, killRegOnJmp2;
	
	/**
	 * Highest index in the regs-array that is used in the current method
	 */
	private int highestUsedRegEntry;
	
	/**
	 * Status register for comparison, true <=> two arguments are equal
	 */
	private boolean cmpLess;
	
	/**
	 * Status register for comparison, true <=> arg0 < arg1
	 */
	private boolean cmpEqual;
	
	/**
	 * Status register for bound check, true if inside
	 */
	private boolean cmpBound;
	
	/**
	 * Offset of local parameters in stack frame
	 */
	private int paramOffsetNormal, paramOffsetInline;
	
	/**
	 * Address of current throw frame to emulate doThrow
	 */
	private int globalThrowFrameVariable;
	
	/**
	 * @see Emulator#initArchitecture(int, int, int, int)
	 */
	public boolean initArchitecture(int cd, int si)
	{
		int cnt;
		if ((relocBytes != 4 && relocBytes != 8) || stackClearBits != (relocBytes - 1))
		{
			out.print("Invalid reloc length or mask for stack alignment");
			return false;
		}
		// setting basic information
		currentIP = si;
		// initializing special registers
		sReg = new Register[SSADef.R_GPRS];
		for (cnt = 0; cnt < sReg.length; cnt++)
		{
			sReg[cnt] = new Register();
			sReg[cnt].size = -1;
		}
		sReg[SSADef.R_STCK].ptr = INIT_STACK_VALUE;
		sReg[SSADef.R_CLSS].ptr = cd;
		regs = new Register[128];
		for (cnt = 0; cnt < regs.length; cnt++)
			regs[cnt] = new Register();
		// initialize instance variables
		paramOffsetNormal = 2 * ((relocBytes + stackClearBits) & ~stackClearBits);
		paramOffsetInline = (relocBytes + stackClearBits) & ~stackClearBits;
		// everything ok
		return true;
	}
	
	/**
	 * Method to obtain the register corresponding to the given number
	 *
	 * @param regNo the desired register number
	 * @return an instance of the corresponding register
	 */
	private Register getRegister(int regNo, int size, boolean forWrite)
	{
		Register r;
		if (regNo < 1)
		{
			out.print("Access to invalid register ");
			return null;
		}
		if (regNo < SSADef.R_GPRS)
		{
			r = sReg[regNo];
			//primary result register may contain any size, set to required one
			if (regNo == SSADef.R_PRIR)
				r.size = size;
		}
		// to keep physical index low, there is a sliding window
		else
		{
			if (curRegStartOff == 0)
			{
				out.print("Access to getRegister with invalid curRegStartOff ");
				return null;
			}
			if (regNo - curRegStartOff < 0)
			{
				out.print("invalid curRegStartOff ");
				return null;
			}
			r = regs[regNo - curRegStartOff];
		}
		// if this is the first write to a register after a call, the type is implicitly set
		if (r.size == 0 && forWrite)
			r.size = size;
			// otherwise sizes have to match, only exception is single pointer access of double pointer register
		else if (r.size != size && size != 0 && (size != -1 || r.size != -2))
		{
			out.print("Invalid size of register ");
			out.print(regNo);
			out.print(" (");
			out.print(r.size);
			out.print(" instead of ");
			out.print(size);
			out.print(") ");
			out.print(", curRegStartOff==");
			out.println(curRegStartOff);
			return null;
		}
		return r;
	}
	
	/**
	 * Method to process the marker instruction
	 * TODO fuer jede Methode enter - marker - nfreg - code - leave
	 * Wird vom Programmierer gesetzt. Gibt an, ob es sich bspw. um eine
	 * Interrupt-Methode handelt - bis jetzt ignorieren
	 *
	 * @param imI
	 * @return true if the operation succeeds, false otherwise
	 */
	private boolean insMarker(int imI)
	{
		return true;
	}
	
	/**
	 * Method to process the enter instruction
	 *
	 * @param imI  variable size, needed place on the stack for local variables
	 * @param para parameter size, to be removed at leave
	 * @return true if the operation succeeds, false otherwise
	 */
	private boolean insEnter(int imI, int para, boolean inline)
	{
		Register base, stck;
		if ((base = getRegister(SSADef.R_BASE, -1, false)) == null || (stck = getRegister(SSADef.R_STCK, -1, false)) == null)
		{
			out.println("in insEnter");
			return false;
		}
		insPush(SSADef.R_BASE, -1);
		base.ptr = stck.ptr;
		while (imI > 0)
			switch (stackClearBits)
			{ //allocate space and clear memory
				case 0:
					imI--;
					write8(false, --stck.ptr, (byte) 0);
					break;
				case 1:
					imI -= 2;
					write16(false, stck.ptr -= 2, (short) 0);
					break;
				case 3:
					imI -= 4;
					write32(false, stck.ptr -= 4, 0);
					break;
				case 7:
					imI -= 8;
					write64(false, stck.ptr -= 8, 0l);
					break;
				default:
					out.println("invalid stackClearBits in insEnter");
					return false;
			}
		if (!inline)
		{ //reset window functionality for register for real procedure call
			lastAllocRegIndex = 0;
			curRegStartOff = 0;
			curInlineLevel = 0;
		}
		else
			curInlineLevel++;
		return true;
	}
	
	/**
	 * Method to process the NFReg instruction.
	 * Contains number of next free register
	 *
	 * @param imI number of the next free register
	 * @return true if the operation succeeds, false otherwise
	 * TODO Sicherstellen, dass alle Register Platz finden
	 */
	private boolean insNFReg(int imI)
	{
		// TODO
		return true;
	}
	
	private void resetRegisters()
	{
		int cnt;
		for (cnt = 0; cnt < highestUsedRegEntry; cnt++)
			regs[cnt].size = 0;
		killRegOnJmp1 = killRegOnJmp2 = lastAllocRegIndex = curRegStartOff = curInlineLevel = highestUsedRegEntry = 0;
	}
	
	/**
	 * Method to process the leave instruction. Return and stack pointer
	 * adjustment.
	 * SP <- SP + imI
	 * IP <- [SP]
	 * SP <- SP + relocBytes + para
	 * (Ruecksprungadresse + uebergebene Parameter abraeumen)
	 *
	 * @param imI  variable size add to stack pointer
	 * @param para return size
	 * @return true if the operation succeeds, false otherwise
	 */
	private boolean insLeave(int imI, int para, boolean inline)
	{
		Register stck;
		if ((stck = getRegister(SSADef.R_STCK, -1, false)) == null)
		{
			out.println("in insLeave");
			return false;
		}
		stck.ptr += imI;
		insPop(SSADef.R_BASE, -1);
		if (inline)
		{
			stck.ptr += para; //remove parameters and fake address
			curInlineLevel--;
		}
		else
		{ //clean up completely and reset window functionality for register
			currentIP = read32(false, stck.ptr);
			stck.ptr += relocBytes + para;
			resetRegisters();
		}
		return true;
	}
	
	/**
	 * Method to process the load immediate int instruction.
	 *
	 * @param reg0 the destination register
	 * @param size size of register (has to match declared type, which is the size
	 *             of the already allocated register)
	 * @param para the value to load (byte, short, int)
	 * @return true if the operation succeeds, false otherwise
	 */
	private boolean insLoadImI(int reg0, int size, int para)
	{
		Register reg;
		if ((reg = getRegister(reg0, size, true)) == null)
		{
			out.println("in insLoadImI");
			return false;
		}
		switch (size)
		{
			case 1:
				reg.value8 = (byte) para;
				break;
			case 2:
			case 3:
				reg.value16 = (short) para;
				break;
			case 4:
			case 5:
				reg.value32 = para;
				break;
			default:
				out.println("Invalid size for insLoadImI");
				return false;
		}
		return true;
	}
	
	/**
	 * Method to process the load immediate long instruction
	 *
	 * @param reg0 the destination register (has to be long sized)
	 * @param imL  the long value to load
	 * @return true if the operation succeeds, false otherwise
	 */
	private boolean insLoadImL(int reg0, int size, long imL)
	{
		Register reg;
		if ((reg = getRegister(reg0, size, true)) == null)
		{
			out.println("in inLoadImL");
			return false;
		}
		reg.value64 = imL;
		return true;
	}
	
	private boolean insLoadImP(int reg0, int imP)
	{
		Register reg;
		if ((reg = getRegister(reg0, -1, true)) == null)
		{
			out.println("in insLoadImI");
			return false;
		}
		reg.ptr = imP;
		return true;
	}
	
	/**
	 * Method to process the load np instruction - i.e. to load a null pointer
	 *
	 * @param reg0 destination register
	 * @param size -1 for pointer, -2 for double pointer, has to match declared
	 *             type
	 * @return true if the operation succeeds, false otherwise
	 */
	private boolean insLoadNP(int reg0, int size)
	{
		Register reg;
		if ((reg = getRegister(reg0, size, true)) == null)
		{
			out.println("in inLoadNP");
			return false;
		}
		reg.ptr = 0;
		reg.upperPtr = 0;
		return true;
	}
	
	/**
	 * Method to process the load addr instruction
	 * reg0 <- reg1 + rela
	 *
	 * @param reg0 the destination register - has to be pointer
	 * @param reg1 a base register - has to be a pointer
	 * @param rela offset relative to reg1
	 * @return true if the operation succeeds, false otherwise
	 */
	private boolean insLoadAddr(int reg0, int reg1, int rela)
	{
		Register r0, r1;
		if ((r0 = getRegister(reg0, -1, true)) == null || (r1 = getRegister(reg1, -1, false)) == null)
		{
			out.println("in insLoadAddr");
			return false;
		}
		// because of return address and additional push&pop in insEnter/-Leave
		// the parameters are further away than specified in rela
		if (reg1 == SSADef.R_BASE && rela >= 0)
		{
			rela += (curInlineLevel > 0 ? paramOffsetInline : paramOffsetNormal);
		}
		r0.ptr = r1.ptr + rela;
		return true;
	}
	
	/**
	 * Method to process the load val instruction
	 * reg0 <- [reg1].size
	 *
	 * @param reg0 the destination register, has to be sized size
	 * @param reg1 the register containing the source address (has to be a
	 *             pointer)
	 * @param size the size of reg0
	 * @return true if the operation succeeds, false otherwise
	 */
	private boolean insLoadVal(int reg0, int reg1, int size)
	{
		Register r0, r1;
		int addr;
		if ((r0 = getRegister(reg0, size, true)) == null || (r1 = getRegister(reg1, -1, false)) == null)
		{
			out.println("in insLoadVal");
			out.println("Registers are ");
			out.print(reg0);
			out.print(" and ");
			out.println(reg1);
			return false;
		}
		addr = r1.ptr;
		// because of return address and additional push&pop in insEnter/-Leave
		// the parameters are further away than specified in rela
		if (reg1 == SSADef.R_BASE)
			addr += (curInlineLevel > 0 ? paramOffsetInline : paramOffsetNormal);
		switch (size)
		{
			case 1:
				r0.value8 = read8(false, addr);
				break;
			case 2:
			case 3:
				r0.value16 = read16(false, addr);
				break;
			case 4:
			case 5:
				r0.value32 = read32(false, addr);
				break;
			case 8:
			case 9:
				r0.value64 = read64(false, addr);
				break;
			case -2:
				r0.upperPtr = read32(false, addr + relocBytes);
				// no break
			case -1:
				r0.ptr = read32(false, addr);
				break;
			default:
				out.println("Invalid size for insLoadVal");
				return false;
		}
		return true;
	}
	
	/**
	 * Method to process the conv instruction. Convert the value of register 1 of
	 * type para to the type size and store it in register 0
	 *
	 * @param reg0 the destination register
	 * @param reg1 the source register
	 * @param size the destination size
	 * @param para the source size
	 * @return true if the operation succeeds, false otherwise
	 */
	private boolean insConv(int reg0, int reg1, int size, int para)
	{
		Register r0, r1;
		if ((r0 = getRegister(reg0, size, true)) == null || (r1 = getRegister(reg1, para, false)) == null)
		{
			out.println("in insConv");
			return false;
		}
		// determine all conversion possibilities
		switch (r0.size)
		{
			case 1:
				switch (r1.size)
				{
					case 1:
						r0.value8 = r1.value8;
						break;
					case 2:
					case 3:
						r0.value8 = (byte) r1.value16;
						break;
					case 4:
						r0.value8 = (byte) r1.value32;
						break;
					case 5:
						r0.value8 = (byte) real.getLongOfFloat(r1.value32);
						break;
					case 8:
						r0.value8 = (byte) r1.value64;
						break;
					case 9:
						r0.value8 = (byte) real.getLongOfDouble(r1.value64);
						break;
					case -1:
					case -2:
						r0.value8 = (byte) r1.ptr;
						break;
					default:
						out.println("Invalid source size for byte destination insConv");
						return false;
				}
				break;
			case 2:
			case 3:
				switch (r1.size)
				{
					case 1:
						r0.value16 = r1.value8;
						break;
					case 2:
					case 3:
						r0.value16 = r1.value16;
						break;
					case 4:
						r0.value16 = (short) r1.value32;
						break;
					case 5:
						r0.value16 = (short) real.getLongOfFloat(r1.value32);
						break;
					case 8:
						r0.value16 = (short) r1.value64;
						break;
					case 9:
						r0.value16 = (short) real.getLongOfDouble(r1.value64);
						break;
					case -1:
					case -2:
						r0.value16 = (short) r1.ptr;
						break;
					default:
						out.println("Invalid source size for short/char destination insConv");
						return false;
				}
				break;
			case 4:
				switch (r1.size)
				{
					case 1:
						r0.value32 = r1.value8;
						break;
					case 2:
						r0.value32 = r1.value16;
						break;
					case 3:
						r0.value32 = (char) r1.value16;
						break;
					case 4:
						r0.value32 = r1.value32;
						break;
					case 5:
						r0.value32 = (int) real.getLongOfFloat(r1.value32);
						break;
					case 8:
						r0.value32 = (int) r1.value64;
						break;
					case 9:
						r0.value32 = (int) real.getLongOfDouble(r1.value64);
						break;
					case -1:
					case -2:
						r0.value32 = r1.ptr;
						break;
					default:
						out.println("Invalid source size for int destination insConv");
						return false;
				}
				break;
			case 5:
				switch (r1.size)
				{
					case 1:
						r0.value32 = real.buildFloat(r1.value8, 0);
						break;
					case 2:
						r0.value32 = real.buildFloat(r1.value16, 0);
						break;
					case 3:
						r0.value32 = real.buildFloat((char) r1.value16, 0);
						break;
					case 4:
						r0.value32 = real.buildFloat(r1.value32, 0);
						break;
					case 5:
						r0.value32 = r1.value32;
						break;
					case 8:
						r0.value32 = real.buildFloat(r1.value64, 0);
						break;
					case 9:
						r0.value32 = real.buildFloatFromDouble(r1.value64);
						break;
					default:
						out.println("Invalid source size for float destination insConv");
						return false;
				}
				break;
			case 8:
				switch (r1.size)
				{
					case 1:
						r0.value64 = r1.value8;
						break;
					case 2:
						r0.value64 = r1.value16;
						break;
					case 3:
						r0.value64 = (char) r1.value16;
						break;
					case 4:
						r0.value64 = r1.value32;
						break;
					case 5:
						r0.value64 = real.getLongOfFloat(r1.value32);
						break;
					case 8:
						r0.value64 = r1.value64;
						break;
					case 9:
						r0.value64 = real.getLongOfDouble(r1.value64);
						break;
					case -1:
					case -2:
						r0.value64 = (long) r1.ptr & 0xFFFFFFFFl;
						break;
					default:
						out.println("Invalid source size for long destination insConv");
						return false;
				}
				break;
			case 9:
				switch (r1.size)
				{
					case 1:
						r0.value64 = real.buildDouble(r1.value8, 0);
						break;
					case 2:
						r0.value64 = real.buildDouble(r1.value16, 0);
						break;
					case 3:
						r0.value64 = real.buildDouble((char) r1.value16, 0);
						break;
					case 4:
						r0.value64 = real.buildDouble(r1.value32, 0);
						break;
					case 5:
						r0.value64 = real.buildDoubleFromFloat(r1.value32);
						break;
					case 8:
						r0.value64 = real.buildDouble(r1.value64, 0);
						break;
					case 9:
						r0.value64 = r1.value64;
						break;
					default:
						out.println("Invalid source size for double destination insConv");
						return false;
				}
				break;
			case -2:
				if (r1.size == -2)
				{
					r0.ptr = r1.ptr;
					r0.upperPtr = r1.upperPtr;
					break;
				}
				out.println("Invalid source size for dPtr destination insConv");
				return false;
			case -1:
				switch (r1.size)
				{
					case 1:
						r0.ptr = (int) r1.value8 & 0xFF;
						break;
					case 2:
					case 3:
						r0.ptr = (int) r1.value16 & 0xFFFF;
						break;
					case 4:
						r0.ptr = r1.value32;
						break;
					case 8:
						r0.ptr = (int) r1.value64;
						break;
					case -1:
					case -2:
						r0.ptr = r1.ptr;
						break;
					default:
						out.println("Invalid source size for ptr destination insConv");
						return false;
				}
				break;
			default:
				out.println("Invalid destination size for insConv");
				return false;
		}
		return true;
	}
	
	/**
	 * Method to process the copy instruction
	 *
	 * @param reg0 the destination register
	 * @param reg1 the source register
	 * @param size the size of both registers to check
	 * @return true if the operation succeeds, false otherwise
	 */
	private boolean insCopy(int reg0, int reg1, int size)
	{
		Register r0, r1;
		if ((r0 = getRegister(reg0, size, true)) == null || (r1 = getRegister(reg1, size, false)) == null)
		{
			out.print("in insCopy to ");
			out.print(reg0);
			out.print(" from ");
			out.println(reg1);
			return false;
		}
		switch (size)
		{
			case 1:
				r0.value8 = r1.value8;
				break;
			case 2:
			case 3:
				r0.value16 = r1.value16;
				break;
			case 4:
			case 5:
				r0.value32 = r1.value32;
				break;
			case 8:
			case 9:
				r0.value64 = r1.value64;
				break;
			case -2:
				r0.upperPtr = r1.upperPtr;
				// no break
			case -1:
				r0.ptr = r1.ptr;
				break;
			default:
				out.println("Invalid size for insCopy");
				return false;
		}
		return true;
	}
	
	/**
	 * Method to process the push immediate int instruction
	 * SP <- (SP - size) & ~stackClearBits
	 * [SP] <- imI
	 *
	 * @param size the size of the value to store on the stack
	 * @param imI  the value
	 * @return true if the operation succeeds, false otherwise
	 */
	private boolean insPushImI(int size, int imI)
	{
		Register stck;
		if ((stck = getRegister(SSADef.R_STCK, -1, false)) == null)
		{
			out.println("in insPushImI");
			return false;
		}
		if (size == 5)
			stck.ptr = (stck.ptr - 4) & ~stackClearBits;
		else
			stck.ptr = (stck.ptr - size) & ~stackClearBits;
		switch (size)
		{
			case 1:
				write8(false, stck.ptr, (byte) imI);
				break;
			case 2:
			case 3:
				write16(false, stck.ptr, (short) imI);
				break;
			case 4:
			case 5:
				write32(false, stck.ptr, imI);
				break;
			default:
				out.println("Invalid size for insPushImI");
				return false;
		}
		return true;
	}
	
	/**
	 * Method to process the push immediate long instruction
	 * SP <- (SP - 8) & ~stackClearBits
	 * [SP] <- imL
	 *
	 * @param imL the long value to store on the stack
	 * @return true if the operation succeeds, false otherwise
	 */
	private boolean insPushImL(long imL)
	{
		Register stck;
		if ((stck = getRegister(SSADef.R_STCK, -1, false)) == null)
		{
			out.println("in insPushImL");
			return false;
		}
		stck.ptr = (stck.ptr - 8) & ~stackClearBits;
		write64(false, stck.ptr, imL);
		return true;
	}
	
	/**
	 * Method to process the push np instruction - i. e. to push a null pointer on
	 * the stack
	 * SP <- (SP - relocBytes) & ~stackClearBits
	 * [SP] <- 0
	 *
	 * @return true if the operation succeeds, false otherwise
	 */
	private boolean insPushNP()
	{
		Register stck;
		if ((stck = getRegister(SSADef.R_STCK, -1, false)) == null)
		{
			out.println("in insPushNP");
			return false;
		}
		stck.ptr = (stck.ptr - relocBytes) & ~stackClearBits;
		write32(false, stck.ptr, 0);
		return true;
	}
	
	/**
	 * Method to process the push instruction
	 * adjust SP
	 * [SP] <- reg0.size
	 *
	 * @param reg0 the register to push
	 * @param size the size of the register
	 * @return true if the operation succeeds, false otherwise
	 */
	private boolean insPush(int reg0, int size)
	{
		Register stck, r0;
		if ((stck = getRegister(SSADef.R_STCK, -1, false)) == null || (r0 = getRegister(reg0, size, false)) == null)
		{
			out.println("in insPush");
			return false;
		}
		// reserve place on the stack
		if (size < 0) //ptr or dptr
			stck.ptr = (stck.ptr + relocBytes * size) & ~stackClearBits;
		else if (size == 5 || size == 9) //flt or dbl
			stck.ptr = (stck.ptr - (size - 1)) & ~stackClearBits;
		else //all others
			stck.ptr = (stck.ptr - size) & ~stackClearBits;
		// write the value to [SP]
		switch (size)
		{
			case 1:
				write8(false, stck.ptr, r0.value8);
				break;
			case 2:
			case 3:
				write16(false, stck.ptr, r0.value16);
				break;
			case 4:
			case 5:
				write32(false, stck.ptr, r0.value32);
				break;
			case 8:
			case 9:
				write64(false, stck.ptr, r0.value64);
				break;
			case -2:
				write32(false, stck.ptr + relocBytes, r0.upperPtr);
				//no break
			case -1:
				write32(false, stck.ptr, r0.ptr);
				break;
			default:
				out.println("Invalid size for insPush");
				return false;
		}
		return true;
	}
	
	/**
	 * Method to process the pop instruction
	 * reg0 <- [SP].size
	 * adjust SP
	 *
	 * @param reg0 the destination register
	 * @param size the size of register 0
	 * @return true if the operation succeeds, false otherwise
	 */
	private boolean insPop(int reg0, int size)
	{
		Register r0, stck;
		if ((r0 = getRegister(reg0, size, true)) == null || (stck = getRegister(SSADef.R_STCK, -1, false)) == null)
		{
			out.println("in insPop");
			return false;
		}
		switch (size)
		{
			case 1:
				r0.value8 = read8(false, stck.ptr);
				break;
			case 2:
			case 3:
				r0.value16 = read16(false, stck.ptr);
				break;
			case 4:
			case 5:
				r0.value32 = read32(false, stck.ptr);
				break;
			case 8:
			case 9:
				r0.value64 = read64(false, stck.ptr);
				break;
			case -2:
				r0.upperPtr = read32(false, stck.ptr);
				// no break
			case -1:
				r0.ptr = read32(false, stck.ptr);
				break;
			default:
				out.println("Invalid size for insPop");
				return false;
		}
		// set new pointer position
		if (size < 0) //ptr or dptr
			stck.ptr = (stck.ptr - relocBytes * size + stackClearBits) & ~stackClearBits;
		else if (size == 5 || size == 9) //flt or dbl
			stck.ptr = (stck.ptr + (size - 1) + stackClearBits) & ~stackClearBits;
		else //all others
			stck.ptr = (stck.ptr + size + stackClearBits) & ~stackClearBits;
		return true;
	}
	
	/**
	 * Save register before call
	 *
	 * @param reg0 register to save
	 * @param size size of the register to save
	 * @param para if 0 then ignore, if 1 push reg0 on the stack
	 * @return
	 */
	private boolean insSave(int reg0, int size, int para)
	{
		if (para == 0)
		{
			if (getRegister(reg0, size, false) == null)
			{
				out.println("in insSave");
				return false;
			}
			return true;
		}
		if (para == 1)
			return insPush(reg0, size);
		out.println("Unknown parameter for insSave");
		return false;
	}
	
	/**
	 * Restore register after call
	 *
	 * @param reg0 register to restore
	 * @param size size of the register to restore
	 * @param para if 0 then allocate register, if 1 pop reg0 from the stack
	 * @return
	 */
	private boolean insRest(int reg0, int size, int para)
	{
		if (para == 0)
			return insAllocReg(reg0, size, true);
		if (para == 1)
			return insPop(reg0, size);
		out.println("Unknown parameter for insRest");
		return false;
	}
	
	/**
	 * Method to process the assign instruction
	 * [reg0] <- reg1.size
	 *
	 * @param reg0 the destination register, has to be pointer
	 * @param reg1 the source register, has to be sized size
	 * @param size the size
	 * @return true if the operation succeeds, false otherwise
	 */
	private boolean insAssign(int reg0, int reg1, int size)
	{
		Register r0, r1;
		if ((r0 = getRegister(reg0, -1, false)) == null || (r1 = getRegister(reg1, size, false)) == null)
		{
			out.println("in insAssign");
			return false;
		}
		switch (size)
		{
			case 1: //byte and boolean
				write8(false, r0.ptr, r1.value8);
				break;
			case 2:
			case 3: //short and char
				write16(false, r0.ptr, r1.value16);
				break;
			case 4:
			case 5: //int and float
				write32(false, r0.ptr, r1.value32);
				break;
			case 8:
			case 9: //long and double
				write64(false, r0.ptr, r1.value64);
				break;
			case -2: //dptr
				write32(false, r0.ptr + relocBytes, r1.upperPtr);
				// no break
			case -1: //ptr
				write32(false, r0.ptr, r1.ptr);
				break;
			default:
				out.println("Invalid size for insAssign");
				return false;
		}
		return true;
	}
	
	/**
	 * Method to process the and instruction
	 * reg0 <- reg1 and reg2
	 *
	 * @param reg0 the destination register
	 * @param reg1 the first operand
	 * @param reg2 the second operand
	 * @param size the size of all registers
	 * @return true if the operation succeeds, false otherwise
	 */
	private boolean insAnd(int reg0, int reg1, int reg2, int size)
	{
		Register r0, r1, r2;
		if ((r0 = getRegister(reg0, size, true)) == null || (r1 = getRegister(reg1, size, false)) == null || (r2 = getRegister(reg2, size, false)) == null)
		{
			out.println("in insAnd");
			return false;
		}
		switch (size)
		{
			case 1:
				r0.value8 = (byte) ((int) r1.value8 & (int) r2.value8);
				break;
			case 2:
			case 3:
				r0.value16 = (short) ((int) r1.value16 & (int) r2.value16);
				break;
			case 4:
				r0.value32 = r1.value32 & r2.value32;
				break;
			case 8:
				r0.value64 = r1.value64 & r2.value64;
				break;
			default:
				out.println("Invalid type for insAnd");
				return false;
		}
		return true;
	}
	
	/**
	 * Method to process the xor instruction
	 * reg0 <- reg1 xor reg2
	 *
	 * @param reg0 the destination register
	 * @param reg1 the first operand
	 * @param reg2 the second operand
	 * @param size the size of all registers
	 * @return true if the operation succeeds, false otherwise
	 */
	private boolean insXor(int reg0, int reg1, int reg2, int size)
	{
		Register r0, r1, r2;
		if ((r0 = getRegister(reg0, size, true)) == null || (r1 = getRegister(reg1, size, false)) == null || (r2 = getRegister(reg2, size, false)) == null)
		{
			out.println("in insXor");
			return false;
		}
		switch (size)
		{
			case 1:
				r0.value8 = (byte) ((int) r1.value8 ^ (int) r2.value8);
				break;
			case 2:
			case 3:
				r0.value16 = (short) ((int) r1.value16 ^ (int) r2.value16);
				break;
			case 4:
				r0.value32 = r1.value32 ^ r2.value32;
				break;
			case 8:
				r0.value64 = r1.value64 ^ r2.value64;
				break;
			default:
				out.println("Invalid type for insXor");
				return false;
		}
		return true;
	}
	
	/**
	 * Method to process the or instruction
	 * reg0 <- reg1 or reg2
	 *
	 * @param reg0 the destination register
	 * @param reg1 the first operand
	 * @param reg2 the second operand
	 * @param size the size of all registers
	 * @return true if the operation succeeds, false otherwise
	 */
	private boolean insOr(int reg0, int reg1, int reg2, int size)
	{
		Register r0, r1, r2;
		if ((r0 = getRegister(reg0, size, true)) == null || (r1 = getRegister(reg1, size, false)) == null || (r2 = getRegister(reg2, size, false)) == null)
		{
			out.println("in insOr");
			return false;
		}
		switch (size)
		{
			case 1:
				r0.value8 = (byte) ((int) r1.value8 | (int) r2.value8);
				break;
			case 2:
			case 3:
				r0.value16 = (short) ((int) r1.value16 | (int) r2.value16);
				break;
			case 4:
				r0.value32 = r1.value32 | r2.value32;
				break;
			case 8:
				r0.value64 = r1.value64 | r2.value64;
				break;
			default:
				out.println("Invalid type for insOr");
				return false;
		}
		return true;
	}
	
	/**
	 * Method to process the add instruction
	 * reg0 <- reg1 add reg2
	 *
	 * @param reg0 the destination register
	 * @param reg1 the first operand
	 * @param reg2 the second operand
	 * @param size the size of all registers
	 * @return true if the operation succeeds, false otherwise
	 */
	private boolean insAdd(int reg0, int reg1, int reg2, int size)
	{
		Register r0, r1, r2;
		if ((r0 = getRegister(reg0, size, true)) == null || (r1 = getRegister(reg1, size, false)) == null || (r2 = getRegister(reg2, size, false)) == null)
		{
			out.println("in insAdd");
			return false;
		}
		switch (size)
		{
			case 1:
				r0.value8 = (byte) ((int) r1.value8 + (int) r2.value8);
				break;
			case 2:
			case 3:
				r0.value16 = (short) ((int) r1.value16 + (int) r2.value16);
				break;
			case 4:
				r0.value32 = r1.value32 + r2.value32;
				break;
			case 5:
				r0.value32 = real.binOpFloat(r1.value32, r2.value32, Real.A_PLUS);
				break;
			case 8:
				r0.value64 = r1.value64 + r2.value64;
				break;
			case 9:
				r0.value64 = real.binOpDouble(r1.value64, r2.value64, Real.A_PLUS);
				break;
			default:
				out.println("Invalid type for insAdd");
				return false;
		}
		return true;
	}
	
	/**
	 * Method to process the sub instruction
	 * reg0 <- reg1 sub reg2
	 *
	 * @param reg0 the destination register
	 * @param reg1 the first operand
	 * @param reg2 the second operand
	 * @param size the size of all registers
	 * @return true if the operation succeeds, false otherwise
	 */
	private boolean insSub(int reg0, int reg1, int reg2, int size)
	{
		Register r0, r1, r2;
		if ((r0 = getRegister(reg0, size, true)) == null || (r1 = getRegister(reg1, size, false)) == null || (r2 = getRegister(reg2, size, false)) == null)
		{
			out.println("in insSub");
			return false;
		}
		switch (size)
		{
			case 1:
				r0.value8 = (byte) ((int) r1.value8 - (int) r2.value8);
				break;
			case 2:
			case 3:
				r0.value16 = (short) ((int) r1.value16 - (int) r2.value16);
				break;
			case 4:
				r0.value32 = r1.value32 - r2.value32;
				break;
			case 5:
				r0.value32 = real.binOpFloat(r1.value32, r2.value32, Real.A_MINUS);
				break;
			case 8:
				r0.value64 = r1.value64 - r2.value64;
				break;
			case 9:
				r0.value64 = real.binOpDouble(r1.value64, r2.value64, Real.A_MINUS);
				break;
			default:
				out.println("Invalid type for insSub");
				return false;
		}
		return true;
	}
	
	/**
	 * Method to process the mul instruction
	 * reg0 <- reg1 mul reg2
	 *
	 * @param reg0 the destination register
	 * @param reg1 the first operand
	 * @param reg2 the second operand
	 * @param size the size of all registers
	 * @return true if the operation succeeds, false otherwise
	 */
	private boolean insMul(int reg0, int reg1, int reg2, int size)
	{
		Register r0, r1, r2;
		if ((r0 = getRegister(reg0, size, true)) == null || (r1 = getRegister(reg1, size, false)) == null || (r2 = getRegister(reg2, size, false)) == null)
		{
			out.println("in insMul");
			return false;
		}
		switch (size)
		{
			case 1:
				r0.value8 = (byte) ((int) r1.value8 * (int) r2.value8);
				break;
			case 2:
			case 3:
				r0.value16 = (short) ((int) r1.value16 * (int) r2.value16);
				break;
			case 4:
				r0.value32 = r1.value32 * r2.value32;
				break;
			case 5:
				r0.value32 = real.binOpFloat(r1.value32, r2.value32, Real.A_MUL);
				break;
			case 8:
				r0.value64 = r1.value64 * r2.value64;
				break;
			case 9:
				r0.value64 = real.binOpDouble(r1.value64, r2.value64, Real.A_MUL);
				break;
			default:
				out.println("Invalid type for insMul");
				return false;
		}
		return true;
	}
	
	/**
	 * Method to process the div instruction
	 * reg0 <- reg1 div reg2
	 *
	 * @param reg0 the destination register
	 * @param reg1 the first operand
	 * @param reg2 the second operand
	 * @param size the size of all registers
	 * @return true if the operation succeeds, false otherwise
	 */
	private boolean insDiv(int reg0, int reg1, int reg2, int size)
	{
		Register r0, r1, r2;
		if ((r0 = getRegister(reg0, size, true)) == null || (r1 = getRegister(reg1, size, false)) == null || (r2 = getRegister(reg2, size, false)) == null)
		{
			out.println("in insDiv");
			return false;
		}
		switch (size)
		{
			case 1:
				r0.value8 = (byte) ((int) r1.value8 / (int) r2.value8);
				break;
			case 2:
			case 3:
				r0.value16 = (short) ((int) r1.value16 / (int) r2.value16);
				break;
			case 4:
				r0.value32 = r1.value32 / r2.value32;
				break;
			case 5:
				r0.value32 = real.binOpFloat(r1.value32, r2.value32, Real.A_DIV);
				break;
			case 8:
				r0.value64 = r1.value64 / r2.value64;
				break;
			case 9:
				r0.value64 = real.binOpDouble(r1.value64, r2.value64, Real.A_DIV);
				break;
			default:
				out.println("Invalid type for insDiv");
				return false;
		}
		return true;
	}
	
	/**
	 * Method to process the mod instruction
	 * reg0 <- reg1 mod reg2
	 *
	 * @param reg0 the destination register
	 * @param reg1 the first operand
	 * @param reg2 the second operand
	 * @param size the size of all registers
	 * @return true if the operation succeeds, false otherwise
	 */
	private boolean insMod(int reg0, int reg1, int reg2, int size)
	{
		Register r0, r1, r2;
		if ((r0 = getRegister(reg0, size, true)) == null || (r1 = getRegister(reg1, size, false)) == null || (r2 = getRegister(reg2, size, false)) == null)
		{
			out.println("in insMod");
			return false;
		}
		switch (size)
		{
			case 1:
				r0.value8 = (byte) ((int) r1.value8 % (int) r2.value8);
				break;
			case 2:
			case 3:
				r0.value16 = (short) ((int) r1.value16 % (int) r2.value16);
				break;
			case 4:
				r0.value32 = r1.value32 % r2.value32;
				break;
			case 8:
				r0.value64 = r1.value64 % r2.value64;
				break;
			default:
				out.println("Invalid type for insMod");
				return false;
		}
		return true;
	}
	
	/**
	 * Method to process the ShL instruction
	 * reg0 <- reg1.size << reg2
	 *
	 * @param reg0 the destination register
	 * @param reg1 the source value to shift
	 * @param reg2 the bit count to shift (must be integer)
	 * @param size size of reg0 and reg1
	 * @return true if the operation succeeds, false otherwise
	 */
	private boolean insShL(int reg0, int reg1, int reg2, int size)
	{
		Register r0, r1, r2;
		if ((r0 = getRegister(reg0, size, true)) == null || (r1 = getRegister(reg1, size, false)) == null || (r2 = getRegister(reg2, 4, false)) == null)
		{
			out.println("in insShL");
			return false;
		}
		switch (size)
		{
			case 1:
				r0.value8 = (byte) (((int) r1.value8) << r2.value32);
				break;
			case 2:
			case 3:
				r0.value16 = (short) (((int) r1.value16) << r2.value32);
				break;
			case 4:
				r0.value32 = r1.value32 << r2.value32;
				break;
			case 8:
				r0.value64 = r1.value64 << r2.value32;
				break;
			default:
				out.println("Invalid type for insShL");
				return false;
		}
		return true;
	}
	
	/**
	 * Method to process the ShRL instruction. Logical shift right (zero extend)
	 * reg0 <- reg1.size >>> reg2
	 *
	 * @param reg0 the destination register
	 * @param reg1 the first operand
	 * @param reg2 the second operand
	 * @param size the size of all registers
	 * @return true if the operation succeeds, false otherwise
	 */
	private boolean insShRL(int reg0, int reg1, int reg2, int size)
	{
		Register r0, r1, r2;
		if ((r0 = getRegister(reg0, size, true)) == null || (r1 = getRegister(reg1, size, false)) == null || (r2 = getRegister(reg2, 4, false)) == null)
		{
			out.println("in insShRL");
			return false;
		}
		switch (size)
		{
			case 1:
				r0.value8 = (byte) (((int) r1.value8) >>> r2.value32);
				break;
			case 2:
			case 3:
				r0.value16 = (short) (((int) r1.value16) >>> r2.value32);
				break;
			case 4:
				r0.value32 = r1.value32 >>> r2.value32;
				break;
			case 8:
				r0.value64 = r1.value64 >>> r2.value32;
				break;
			default:
				out.println("Invalid type for insShRL");
				return false;
		}
		return true;
	}
	
	/**
	 * Method to process the ShRA instruction. Arithmetical shift right (sign
	 * extend)
	 * reg0 <- reg1.size >> reg2
	 *
	 * @param reg0 the destination register
	 * @param reg1 the first operand
	 * @param reg2 the second operand
	 * @param size the size of all registers
	 * @return true if the operation succeeds, false otherwise
	 */
	private boolean insShRA(int reg0, int reg1, int reg2, int size)
	{
		Register r0, r1, r2;
		if ((r0 = getRegister(reg0, size, true)) == null || (r1 = getRegister(reg1, size, false)) == null || (r2 = getRegister(reg2, 4, false)) == null)
		{
			out.println("in insShRA");
			return false;
		}
		switch (size)
		{
			case 1:
				r0.value8 = (byte) (((int) r1.value8) >> r2.value32);
				break;
			case 2:
			case 3:
				r0.value16 = (short) (((int) r1.value16) >> r2.value32);
				break;
			case 4:
				r0.value32 = r1.value32 >> r2.value32;
				break;
			case 8:
				r0.value64 = r1.value64 >> r2.value32;
				break;
			default:
				out.println("Invalid type for insShRA");
				return false;
		}
		return true;
	}
	
	/**
	 * Method to process the Not instruction
	 * reg0 <- ~reg1.size
	 *
	 * @param reg0 the destination register
	 * @param reg1 the source register
	 * @param size the size of both registers
	 * @return true if the operation succeeds, false otherwise
	 */
	private boolean insNot(int reg0, int reg1, int size)
	{
		Register r0, r1;
		if ((r0 = getRegister(reg0, size, true)) == null || (r1 = getRegister(reg1, size, false)) == null)
		{
			out.println("in insNot");
			return false;
		}
		switch (size)
		{
			case 1:
				r0.value8 = (byte) ~r1.value8;
				break;
			case 2:
			case 3:
				r0.value16 = (short) ~r1.value16;
				break;
			case 4:
				r0.value32 = ~r1.value32;
				break;
			case 8:
				r0.value64 = ~r0.value64;
				break;
			default:
				out.println("Invalid type for insNot");
				return false;
		}
		return true;
	}
	
	/**
	 * Method to process the Neg instruction
	 * reg0 <- -reg1.size
	 *
	 * @param reg0 the destination register
	 * @param reg1 the source register
	 * @param size the size of both registers
	 * @return true if the operation succeeds, false otherwise
	 */
	private boolean insNeg(int reg0, int reg1, int size)
	{
		Register r0, r1;
		if ((r0 = getRegister(reg0, size, true)) == null || (r1 = getRegister(reg1, size, false)) == null)
		{
			out.println("in insNeg");
			return false;
		}
		switch (size)
		{
			case 1:
				r0.value8 = (byte) -r1.value8;
				break;
			case 2:
			case 3:
				r0.value16 = (short) -r1.value16;
				break;
			case 4:
				r0.value32 = -r1.value32;
				break;
			case 5:
				r0.value32 = real.negateFloat(r1.value32);
				break;
			case 8:
				r0.value64 = -r0.value64;
				break;
			case 9:
				r0.value64 = real.negateDouble(r1.value64);
				break;
			default:
				out.println("Invalid type for insNeg");
				return false;
		}
		return true;
	}
	
	/**
	 * Method to process the BinV instruction. Binary inversion
	 * reg0 <- !reg1
	 * size has to be one
	 *
	 * @param reg0 the destination register
	 * @param reg1 the source register
	 * @return true if the operation succeeds, false otherwise
	 */
	private boolean insBinV(int reg0, int reg1)
	{
		Register r0, r1;
		if ((r0 = getRegister(reg0, 1, true)) == null || (r1 = getRegister(reg1, 1, false)) == null)
		{
			out.println("in insNot");
			return false;
		}
		if ((int) r1.value8 == 0)
			r0.value8 = (byte) 1;
		else
			r0.value8 = (byte) 0;
		return true;
	}
	
	/**
	 * Method to process the inc mem instruction
	 * [reg0] <- 1 + [reg0].size
	 *
	 * @param reg0 the memory destination (reg0 has to be pointer)
	 * @param size the size of the memory location to increment
	 * @return true if the operation succeeds, false otherwise
	 */
	private boolean insIncMem(int reg0, int size)
	{
		Register r0;
		if ((r0 = getRegister(reg0, -1, false)) == null)
		{
			out.println("in insIncMem");
			return false;
		}
		switch (size)
		{
			case 1:
				write8(false, r0.ptr, (byte) ((int) read8(false, r0.ptr) + 1));
				break;
			case 2:
			case 3:
				write16(false, r0.ptr, (short) ((int) read16(false, r0.ptr) + 1));
				break;
			case 4:
				write32(false, r0.ptr, read32(false, r0.ptr) + 1);
				break;
			case 8:
				write64(false, r0.ptr, read64(false, r0.ptr) + 1l);
				break;
			default:
				out.println("Invalid size for insIncMem");
				return false;
		}
		return true;
	}
	
	/**
	 * Method to process the dec mem instruction
	 * [reg0] <- 1 - [reg0].size
	 *
	 * @param reg0 the memory destination (reg0 has to be pointer)
	 * @param size the size of the memory location to decrement
	 * @return true if the operation succeeds, false otherwise
	 */
	private boolean insDecMem(int reg0, int size)
	{
		Register r0;
		if ((r0 = getRegister(reg0, -1, false)) == null)
		{
			out.println("in insIncMem");
			return false;
		}
		switch (size)
		{
			case 1:
				write8(false, r0.ptr, (byte) ((int) read8(false, r0.ptr) - 1));
				break;
			case 2:
			case 3:
				write16(false, r0.ptr, (short) ((int) read16(false, r0.ptr) - 1));
				break;
			case 4:
				write32(false, r0.ptr, read32(false, r0.ptr) - 1);
				break;
			case 8:
				write64(false, r0.ptr, read64(false, r0.ptr) - 1l);
				break;
			default:
				out.println("Invalid size for insDecMem");
				return false;
		}
		return true;
	}
	
	/**
	 * Method to process the call instruction. Call method starting at
	 * SP <- (SP - relocBytes) & ~stackClearBits
	 * [SP] <- IP
	 * IP <- [SReg[1] + rela]
	 *
	 * @param rela the relative offset to SReg[1]
	 * @return true if the operation succeeds, false otherwise
	 */
	private boolean insCall(int reg0, int rela)
	{
		Register stck, cReg;
		if ((stck = getRegister(SSADef.R_STCK, -1, false)) == null || (cReg = getRegister(reg0, -1, false)) == null)
		{
			out.println("in insCall");
			return false;
		}
		stck.ptr = (stck.ptr - relocBytes) & ~stackClearBits;
		write32(false, stck.ptr, currentIP);
		currentIP = read32(false, cReg.ptr + rela) + codeStart;
		resetRegisters();
		return true;
	}
	
	private boolean insCallImP(int imP)
	{
		Register stck;
		if ((stck = getRegister(SSADef.R_STCK, -1, false)) == null)
		{
			out.println("in insCallImP");
			return false;
		}
		stck.ptr = (stck.ptr - relocBytes) & ~stackClearBits;
		write32(false, stck.ptr, currentIP);
		currentIP = imP;
		resetRegisters();
		return true;
	}
	
	/**
	 * Method to process the call ind instruction
	 * SP <- (SP - relocBytes) & ~stackClearBits
	 * [SP] <- IP
	 * IP <- [SReg[1] + [reg0.upperPtr + rela]]
	 *
	 * @param reg0 register with double pointer to the map
	 * @param rela the relative offset in map
	 * @return true if the operation succeeds, false otherwise
	 */
	private boolean insCallInd(int reg0, int rela)
	{
		Register r0, stck, sreg1;
		if ((r0 = getRegister(reg0, -2, false)) == null || (stck = getRegister(SSADef.R_STCK, -1, false)) == null || (sreg1 = getRegister(SSADef.R_CLSS, -1, false)) == null)
		{
			out.println("in insCallInd");
			return false;
		}
		stck.ptr = (stck.ptr - relocBytes) & ~stackClearBits;
		write32(false, stck.ptr, currentIP);
		currentIP = read32(false, sreg1.ptr + read32(false, r0.upperPtr + rela)) + codeStart;
		resetRegisters();
		return true;
	}
	
	/**
	 * Method to process the cmp instruction. Sets the jump conditions
	 *
	 * @param reg0 the first source operand
	 * @param reg1 the second source operand
	 * @param size the size of the operands
	 * @param cond the condition for the next jump
	 * @return true if the operation succeeds, false otherwise
	 */
	private boolean insCmp(int reg0, int reg1, int size, int para)
	{
		Register r0, r1;
		if ((r0 = getRegister(reg0, size, false)) == null || (r1 = getRegister(reg1, size, false)) == null)
		{
			out.println("in insCmp");
			return false;
		}
		switch (size)
		{
			case 1:
				cmpEqual = r0.value8 == r1.value8;
				cmpLess = r0.value8 < r1.value8;
				break;
			case 2:
				cmpEqual = r0.value16 == r1.value16;
				cmpLess = r0.value16 < r1.value16;
				break;
			case 3:
				cmpEqual = r0.value16 == r1.value16;
				cmpLess = (int) ((char) r0.value16) < (int) ((char) r1.value16);
				break;
			case 4:
				cmpEqual = r0.value32 == r1.value32;
				cmpLess = r0.value32 < r1.value32;
				cmpBound = (r0.value32 >= 0 && r0.value32 < r1.value32); //only valid for int (bound-check)
				break;
			case 5:
				cmpEqual = real.compareConstFloat(r0.value32, r1.value32, Real.C_EQ) == 1;
				cmpLess = real.compareConstFloat(r0.value32, r1.value32, Real.C_LW) == 1;
				break;
			case 8:
				cmpEqual = r0.value64 == r1.value64;
				cmpLess = r0.value64 < r1.value64;
				break;
			case 9:
				cmpEqual = real.compareConstDouble(r0.value64, r1.value64, Real.C_EQ) == 1;
				cmpLess = real.compareConstDouble(r0.value64, r1.value64, Real.C_LW) == 1;
				break;
			case -2:
			case -1:
				cmpEqual = r0.ptr == r1.ptr; //pointers may only be check to be equal or not
				break;
			default:
				out.println("Invalid size for insCmp");
				return false;
		}
		return true;
	}
	
	/**
	 * Method to process the out instruction. IO memory write
	 *
	 * @param reg0   pointer
	 * @param reg1   the value to write
	 * @param size   the size of reg1
	 * @param memLoc the memory location
	 * @return true if the operation succeeds, false otherwise
	 */
	private boolean insOut(int reg0, int reg1, int size, int memLoc)
	{
		Register r0, r1;
		if ((r0 = getRegister(reg0, -1, false)) == null || (r1 = getRegister(reg1, size, false)) == null)
		{
			out.println("in insOut");
			return false;
		}
		if (memLoc != 0)
		{
			out.println("Invalid memory location for insOut");
			return false;
		}
		switch (size)
		{
			case 1:
				write8(true, r0.ptr, r1.value8);
				break;
			case 2:
			case 3:
				write16(true, r0.ptr, r1.value16);
				break;
			case 4:
				write32(true, r0.ptr, r1.value32);
				break;
			case 8:
				write64(true, r0.ptr, r1.value64);
				break;
			default:
				out.println("Invalid size for insOut");
				return false;
		}
		return true;
	}
	
	/**
	 * Method to process the in instruction. IO memory read
	 *
	 * @param reg0
	 * @param reg1
	 * @param size
	 * @param memLoc
	 * @return true if the operation succeeds, false otherwise
	 */
	private boolean insIn(int reg0, int reg1, int size, int memLoc)
	{
		Register r0, r1;
		if ((r0 = getRegister(reg0, size, true)) == null || (r1 = getRegister(reg1, -1, false)) == null)
		{
			out.println("in insIn");
			return false;
		}
		if (memLoc != 0)
		{
			out.println("Invalid memory location for insIn");
			return false;
		}
		switch (size)
		{
			case 1:
				r0.value8 = read8(true, r1.ptr);
				break;
			case 2:
			case 3:
				r0.value16 = read16(true, r1.ptr);
				break;
			case 4:
				r0.value32 = read32(true, r1.ptr);
				break;
			case 8:
				r0.value64 = read64(true, r1.ptr);
				break;
			default:
				out.println("Invalid size for insIn");
				return false;
		}
		return true;
	}
	
	/**
	 * Method to process the jump instruction
	 * if (para)
	 * IP <- IP + rela
	 *
	 * @param cond jump condition (set by last cmp)
	 * @param rela the offset relative to current IP
	 * @return true if the operation succeeds, false otherwise
	 */
	private boolean insJump(int cond, int rela)
	{
		boolean jump = false;
		switch (cond)
		{
			case SSADef.CC_AL:
				if (listener != null && endlessLoopHint == currentIP)
					listener.endlessLoopDetected();
				jump = true;
				break;
			case SSADef.CC_EQ:
				jump = cmpEqual;
				break;
			case SSADef.CC_GE:
				jump = !cmpLess;
				break;
			case SSADef.CC_GT:
				jump = !(cmpLess || cmpEqual);
				break;
			case SSADef.CC_LE:
				jump = cmpLess || cmpEqual;
				break;
			case SSADef.CC_LW:
				jump = cmpLess;
				break;
			case SSADef.CC_NE:
				jump = !cmpEqual;
				break;
			case SSADef.CC_BO:
				jump = cmpBound;
				break;
			default:
				out.println("Invalid parameter for insJump");
				return false;
		}
		if (jump)
		{
			if (killRegOnJmp1 != 0)
			{
				if (!insKillReg(killRegOnJmp1))
					return false;
				killRegOnJmp1 = 0;
			}
			if (killRegOnJmp2 != 0)
			{
				if (!insKillReg(killRegOnJmp2))
					return false;
				killRegOnJmp2 = 0;
			}
			if (curRegStartOff == 0)
				lastAllocRegIndex = 0;
			currentIP += rela;
		}
		return true;
	}
	
	/**
	 * Method to process the bound instruction
	 * if (reg1<0 || reg1>=[reg0+rela])
	 * throw exception index out of bounds
	 *
	 * @param reg0 contains the address of the array
	 * @param reg1 contains the desired index
	 * @param rela the offset of the length entry in the array object
	 * @return true if the operation succeeds, false otherwise
	 */
	private boolean insBound(int reg0, int reg1, int rela)
	{
		Register r0, r1;
		if ((r0 = getRegister(reg0, -1, false)) == null || (r1 = getRegister(reg1, 4, false)) == null)
		{
			out.println("in insBound");
			return false;
		}
		cmpBound = (r1.value32 >= 0 && r1.value32 < read32(false, r0.ptr + rela));
		return true;
	}
	
	/**
	 * Method to process the deref instruction. Only the address of the desired
	 * element is calculated
	 * reg0 <- reg1+rela+reg2*para
	 *
	 * @param reg0 pointer to the array (must be pointer sized)
	 * @param reg1 index register (index of the desired element)
	 * @param rela base offset
	 * @param para size of array entries
	 * @return true if the operation succeeds, false otherwise
	 */
	private boolean insDeref(int reg0, int reg1, int reg2, int rela, int para)
	{
		Register r0, r1, r2;
		if ((r0 = getRegister(reg0, -1, true)) == null || (r1 = getRegister(reg1, -1, false)) == null || (r2 = getRegister(reg2, 4, false)) == null)
		{
			out.println("in insDeref");
			return false;
		}
		r0.ptr = r1.ptr + rela + r2.value32 * para;
		return true;
	}
	
	/**
	 * Method to process the movemap instruction
	 * reg0.upperPtr <- SReg[5] (SReg[5] has to be pointer)
	 *
	 * @param reg0 destination register must be double pointer sized
	 * @return true if the operation succeeds, false otherwise
	 */
	private boolean insMovemap(int reg0)
	{
		Register r0, sreg5;
		
		if ((r0 = getRegister(reg0, -2, true)) == null || (sreg5 = getRegister(SSADef.R_PRIR, -1, false)) == null)
		{
			out.println("in insMovemap");
			return false;
		}
		r0.upperPtr = sreg5.ptr;
		return true;
	}
	
	/**
	 * Method to process the allocreg instruction. Allocate the register
	 *
	 * @param reg0 register to allocate
	 * @param size the size of the register
	 * @return true if the operation succeeds, false otherwise
	 */
	private boolean insAllocReg(int reg0, int size, boolean restore)
	{
		Register[] temp;
		int cnt;
		if (curRegStartOff == 0)
			curRegStartOff = reg0;
		else if (reg0 < curRegStartOff)
		{
			out.print("Compiler error in allocation strategy (reuse), reg would be ");
			out.println(reg0);
			out.print("curRegStartOff: ");
			out.println(curRegStartOff);
			return false;
		}
		else if (reg0 - curRegStartOff >= regs.length)
		{
			cnt = regs.length;
			while (reg0 - curRegStartOff >= cnt)
				cnt *= 2;
			temp = new Register[cnt];
			for (cnt = 0; cnt < regs.length; cnt++)
				temp[cnt] = regs[cnt];
			for (; cnt < temp.length; cnt++)
				temp[cnt] = new Register();
			regs = temp;
		}
		// register enumeration has to be incremental if not restoring a defined register
		if (!restore && lastAllocRegIndex != 0 && lastAllocRegIndex >= reg0)
		{
			out.print("Compiler error in allocation strategy (index), reg would be ");
			out.println(reg0);
			out.print("curRegStartOff: ");
			out.println(curRegStartOff);
			return false;
		}
		// access with physical offset
		cnt = reg0 - curRegStartOff;
		regs[cnt].size = size;
		if (cnt > highestUsedRegEntry)
			highestUsedRegEntry = cnt;
		// set the last allocated index
		lastAllocRegIndex = reg0;
		return true;
	}
	
	/**
	 * Method to process the kill reg instruction. Clean up register
	 *
	 * @param reg0 the register to clean
	 * @return true if the operation succeeds, false otherwise
	 */
	private boolean insKillReg(int reg0)
	{
		int cnt;
		Register r;
		if ((r = getRegister(reg0, 0, false)) == null)
		{
			out.println("in insKillReg");
			return false;
		}
		r.size = 0;
		if (reg0 == killRegOnJmp1)
			killRegOnJmp1 = 0;
		if (reg0 == killRegOnJmp2)
			killRegOnJmp2 = 0;
		if (reg0 == curRegStartOff)
		{
			for (cnt = curRegStartOff; cnt <= lastAllocRegIndex; cnt++)
			{
				if (getRegister(cnt, 0, false).size != 0)
				{
					out.print("register ");
					out.print(reg0);
					out.print(" is killed invalidly instead of");
					out.println(cnt);
					return false;
				}
			}
			curRegStartOff = 0;
		}
		return true;
	}
	
	private boolean insKillOJmp(int reg0)
	{
		if (killRegOnJmp1 != 0)
		{
			if (killRegOnJmp2 != 0)
			{
				out.println("Compiler error in using KillOJmp");
				return false;
			}
			killRegOnJmp2 = reg0;
		}
		else
			killRegOnJmp1 = reg0;
		return true;
	}
	
	/**
	 * Method to set the current start-offset in register array after call
	 *
	 * @param reg0 first currently used register (the new start-offset)
	 * @param reg1 last currently used register
	 * @return true if the operation succeeds, false otherwise
	 */
	private boolean insRegrange(int reg0, int reg1)
	{
		//    int r;
		//set start of current window
		curRegStartOff = reg0;
		//would be paranoid: reset size of still-in-use registers
		//    if (reg0>0) for (r=reg0; r<=reg1; r++) getRegister(r, 0, false).size=0;
		return true;
	}
	
	/**
	 * Method to simulate a native exception
	 *
	 * @param nr exception code
	 * @return false always (do not proceed in execution)
	 */
	private boolean insException(int nr)
	{
		switch (nr)
		{
			case 1:
				out.println("Index out of bounds");
				break;
			default:
				out.println("Unknown exception");
		}
		return false;
	}
	
	/**
	 * Method to build the throw frame
	 *
	 * @param reg0 register containing globalAddr
	 * @param tbo  offset of throw-frame relative to stack frame pointer
	 * @param dest relative target address
	 * @return true if the operation succeeds, false otherwise
	 */
	private boolean insThrowFrameBuild(int reg0, int tfo, int destRela)
	{
		int tf;
		Register r0;
		if ((r0 = getRegister(reg0, 0, false)) == null)
		{
			out.println("in insThrowFrameBuild");
			return false;
		}
		//build throw frame and enter current throw frame address in global throw frame variable
		tf = sReg[SSADef.R_BASE].ptr + tfo;
		write32(false, tf, read32(false, r0.ptr)); //build chain of throw frames
		write32(false, tf + relocBytes, currentIP + destRela);
		write32(false, tf + relocBytes * 2, sReg[SSADef.R_CLSS].ptr);
		write32(false, tf + relocBytes * 3, sReg[SSADef.R_INST].ptr);
		write32(false, tf + relocBytes * 4, sReg[SSADef.R_BASE].ptr);
		write32(false, tf + relocBytes * 5, sReg[SSADef.R_STCK].ptr);
		write32(false, tf + relocBytes * 6, 0); //cleanup current throwable thrown
		write32(false, r0.ptr, globalThrowFrameVariable = tf); //set global throw frame address
		return true;
	}
	
	/**
	 * Method to update the throw frame, called twice for each update
	 *
	 * @param tbo  offset of throw-frame relative to stack frame pointer
	 * @param dest relative target address
	 * @return true if the operation succeeds, false otherwise
	 */
	private boolean insThrowFrameUpdate(boolean secondCall, int tfo, int destRela)
	{
		//first call is just a helper for some native architectures
		if (secondCall)
			write32(false, sReg[SSADef.R_BASE].ptr + tfo + relocBytes, currentIP + destRela);
		return true;
	}
	
	/**
	 * Method to reset the throw frame
	 *
	 * @param reg0 register containing globalAddr
	 * @param tbo  offset of throw-frame relative to stack frame pointer
	 * @return true if the operation succeeds, false otherwise
	 */
	private boolean insThrowFrameReset(int reg0, int tbo)
	{
		Register r0;
		if ((r0 = getRegister(reg0, 0, false)) == null)
		{
			out.println("in insThrowFrameReset");
			return false;
		}
		//copy global throw frame address from current throw frame to global address variable
		write32(false, r0.ptr, globalThrowFrameVariable = read32(false, sReg[SSADef.R_BASE].ptr + tbo));
		return true;
	}
	
	/**
	 * Method to check stack pointer, set cmpBound flag for following CC_BO conditional jump
	 *
	 * @param reg0 register containing maximum valid stack value
	 * @return true if the operation succeeds, false otherwise
	 */
	private boolean insStackExtremeCheck(int reg0)
	{
		Register r0;
		if ((r0 = getRegister(reg0, 0, false)) == null)
		{
			out.println("in insStackExtremeCheck");
			return false;
		}
		cmpBound = (r0.value32 >= sReg[SSADef.R_STCK].ptr); //do not check stack current method's requirements in emulator
		return true;
	}
	
	/**
	 * Method to emulate the doThrow method usually implemented in the runtime system
	 *
	 * @return true if the operation succeeds, false otherwise
	 */
	private boolean insDoThrow()
	{
		//set current throwable thrown in throw frame
		write32(false, globalThrowFrameVariable + relocBytes * 6, read32(false, sReg[SSADef.R_BASE].ptr + paramOffsetNormal));
		//initialize sRegs
		currentIP = read32(false, globalThrowFrameVariable + relocBytes);
		out.print("throwable jumps to 0x");
		out.printHexFix(currentIP, 8);
		out.println();
		sReg[SSADef.R_CLSS].ptr = read32(false, globalThrowFrameVariable + 8);
		sReg[SSADef.R_INST].ptr = read32(false, globalThrowFrameVariable + 12);
		sReg[SSADef.R_BASE].ptr = read32(false, globalThrowFrameVariable + 16);
		sReg[SSADef.R_STCK].ptr = read32(false, globalThrowFrameVariable + 20);
		//disable register checks
		killRegOnJmp1 = killRegOnJmp2 = 0;
		resetRegisters();
		return true;
	}
	
	/**
	 * Method to read a byte from where the instruction pointer currently points
	 * at
	 *
	 * @return the byte at that position
	 */
	private int readByte()
	{
		int result = (int) read8(false, currentIP) & 0xFF;
		currentIP++;
		return result;
	}
	
	/**
	 * Method to read an int from where the instruction pointer currently points
	 * at
	 *
	 * @return the int lying at that position
	 */
	private int readInt()
	{
		int result = read32(false, currentIP);
		currentIP += 4;
		return result;
	}
	
	/**
	 * Method to read a long from where the instruction pointer currently points
	 * at
	 *
	 * @return the long lying at that position
	 */
	private long readLong()
	{
		long result = read64(false, currentIP);
		currentIP += 8;
		return result;
	}
	
	/**
	 * Method performing a single step with the given program
	 *
	 * @param into true, if the emulator should step into methods, false to step
	 *             over
	 */
	public boolean step(boolean into)
	{
		int param = 0, opcode = 0, reg0 = 0, reg1 = 0, reg2 = 0, iPar1 = 0, iPar2 = 0;
		boolean success = true;
		long lPar = 0l;
		//get type and parameter
		param = readByte();
		opcode = readByte();
		//check for jump-destination
		if ((param & SSADef.IPJMPDEST) != 0)
		{
			param &= ~SSADef.IPJMPDEST;
      /*for (i=SSADef.R_GPRS; i<usage.length; i++) {
        if ((usage[i]&(R_ALLOCED|R_KILLED))==R_ALLOCED) printErr("jumpDest: register in use", i);
      }*/
		}
		if (opcode == (0xFF & SSADef.I_INLINE))
		{ //MAGIC.inline
			out.print("Skipping inline code ");
			while (param-- > 0)
			{
				out.print(readByte() & 0xFF); //skip bytes
				out.print(' ');
			}
			out.println();
			endlessLoopHint = currentIP;
		}
		else
		{
			if (opcode == (0xFF & SSADef.I_JUMP))
			{
				iPar1 = readByte();
				iPar2 = readInt();
				param = currentIP; //remember currentIP for endlessLoopHint
				success = insJump(iPar1, iPar2);
				endlessLoopHint = param; //assignment has to be done after insJump, but currentIP might have changed
			}
			else
			{
				param = param << SSADef.IPOPTSHFT;
				// fetch parameters
				if ((param & SSADef.IP_reg0) != 0)
					reg0 = readInt();
				if ((param & SSADef.IP_reg1) != 0)
					reg1 = readInt();
				if ((param & SSADef.IP_reg2) != 0)
					reg2 = readInt();
				if ((param & SSADef.IP_size) != 0)
					iPar1 = (byte) readByte();
				else if ((param & SSADef.IP_im_i) != 0)
					iPar1 = readInt();
				if ((param & SSADef.IP_para) != 0)
					iPar2 = readInt();
				if ((param & SSADef.IP_im_l) != 0)
					lPar = readLong();
				// process instruction
				if (opcode == (0xFF & SSADef.I_FLOWHINT))
				{
					//special handling for flow hint: don't set realInstructionHint
					if (iPar2 == SSADef.F_DOTHROW)
						success = insDoThrow(); //emulate doThrow-method
					else
						success = true; //nothing to do
				}
				else
				{
					switch (opcode)
					{
						case 0xFF & SSADef.I_MARKER:
							success = insMarker(iPar1);
							break;
						case 0xFF & SSADef.I_ENTER:
							success = insEnter(iPar1, iPar2, false);
							break;
						case 0xFF & SSADef.I_ENTERINL:
							success = insEnter(iPar1, iPar2, true);
							break;
						case 0xFF & SSADef.I_NFREG:
							success = insNFReg(iPar1);
							break;
						case 0xFF & SSADef.I_LEAVE:
							success = insLeave(iPar1, iPar2, false);
							// check for step over condition
							if (stepOverC != null && stepOverC.hit(this))
							{
								listener.breakPointOccurred(stepOverC);
								stepOverC = null;
							}
							break;
						case 0xFF & SSADef.I_LEAVEINL:
							success = insLeave(iPar1, iPar2, true);
							break;
						case 0xFF & SSADef.I_LOADim_i:
							success = insLoadImI(reg0, iPar1, iPar2);
							break;
						case 0xFF & SSADef.I_LOADim_l:
							success = insLoadImL(reg0, iPar1, lPar);
							break;
						case 0xFF & SSADef.I_LOADim_p:
							success = insLoadImP(reg0, iPar1);
							break;
						case 0xFF & SSADef.I_LOADnp:
							success = insLoadNP(reg0, iPar1);
							break;
						case 0xFF & SSADef.I_LOADaddr:
							success = insLoadAddr(reg0, reg1, iPar1);
							break;
						case 0xFF & SSADef.I_LOADval:
							success = insLoadVal(reg0, reg1, iPar1);
							break;
						case 0xFF & SSADef.I_CONV:
							success = insConv(reg0, reg1, iPar1, iPar2);
							break;
						case 0xFF & SSADef.I_COPY:
							success = insCopy(reg0, reg1, iPar1);
							break;
						case 0xFF & SSADef.I_PUSHim_i:
							success = insPushImI(iPar1, iPar2);
							break;
						case 0xFF & SSADef.I_PUSHim_l:
							success = insPushImL(lPar);
							break;
						case 0xFF & SSADef.I_PUSHnp:
							success = insPushNP();
							break;
						case 0xFF & SSADef.I_PUSH:
							success = insPush(reg0, iPar1);
							break;
						case 0xFF & SSADef.I_POP:
							success = insPop(reg0, iPar1);
							break;
						case 0xFF & SSADef.I_SAVE:
							success = insSave(reg0, iPar1, iPar2);
							break;
						case 0xFF & SSADef.I_REST:
							success = insRest(reg0, iPar1, iPar2);
							break;
						case 0xFF & SSADef.I_ASSIGN:
							success = insAssign(reg0, reg1, iPar1);
							break;
						case 0xFF & SSADef.I_AND:
							success = insAnd(reg0, reg1, reg2, iPar1);
							break;
						case 0xFF & SSADef.I_XOR:
							success = insXor(reg0, reg1, reg2, iPar1);
							break;
						case 0xFF & SSADef.I_OR:
							success = insOr(reg0, reg1, reg2, iPar1);
							break;
						case 0xFF & SSADef.I_ADD:
							success = insAdd(reg0, reg1, reg2, iPar1);
							break;
						case 0xFF & SSADef.I_SUB:
							success = insSub(reg0, reg1, reg2, iPar1);
							break;
						case 0xFF & SSADef.I_MUL:
							success = insMul(reg0, reg1, reg2, iPar1);
							break;
						case 0xFF & SSADef.I_DIV:
							success = insDiv(reg0, reg1, reg2, iPar1);
							break;
						case 0xFF & SSADef.I_MOD:
							success = insMod(reg0, reg1, reg2, iPar1);
							break;
						case 0xFF & SSADef.I_SHL:
							success = insShL(reg0, reg1, reg2, iPar1);
							break;
						case 0xFF & SSADef.I_SHRL:
							success = insShRL(reg0, reg1, reg2, iPar1);
							break;
						case 0xFF & SSADef.I_SHRA:
							success = insShRA(reg0, reg1, reg2, iPar1);
							break;
						case 0xFF & SSADef.I_NOT:
							success = insNot(reg0, reg1, iPar1);
							break;
						case 0xFF & SSADef.I_NEG:
							success = insNeg(reg0, reg1, iPar1);
							break;
						case 0xFF & SSADef.I_BINV:
							success = insBinV(reg0, reg1);
							break;
						case 0xFF & SSADef.I_INCmem:
							success = insIncMem(reg0, iPar1);
							break;
						case 0xFF & SSADef.I_DECmem:
							success = insDecMem(reg0, iPar1);
							break;
						case 0xFF & SSADef.I_CALL:
							if (!into)
							{
								// create a breakpoint for the return from the call
								stepOverC = new StackCond(sReg[SSADef.R_STCK].ptr);
								// register the condition
								listener.proceedAfterStep();
							}
							success = insCall(reg0, iPar1);
							break;
						case 0xFF & SSADef.I_CALLind:
							if (!into)
							{
								// create a breakpoint for the return from the call
								stepOverC = new StackCond(sReg[SSADef.R_STCK].ptr);
								// register the condition
								listener.proceedAfterStep();
							}
							success = insCallInd(reg0, iPar1);
							break;
						case 0xFF & SSADef.I_CALLim_p:
							if (!into)
							{
								// create a breakpoint for the return from the call
								stepOverC = new StackCond(sReg[SSADef.R_STCK].ptr);
								// register the condition
								listener.proceedAfterStep();
							}
							success = insCallImP(iPar1);
							break;
						case 0xFF & SSADef.I_CMP:
							success = insCmp(reg0, reg1, iPar1, iPar2);
							break;
						case 0xFF & SSADef.I_OUT:
							success = insOut(reg0, reg1, iPar1, iPar2);
							break;
						case 0xFF & SSADef.I_IN:
							success = insIn(reg0, reg1, iPar1, iPar2);
							break;
						case 0xFF & SSADef.I_BOUND:
							success = insBound(reg0, reg1, iPar1);
							break;
						case 0xFF & SSADef.I_DEREF:
							success = insDeref(reg0, reg1, reg2, iPar1, iPar2);
							break;
						case 0xFF & SSADef.I_MOVEMAP:
							success = insMovemap(reg0);
							break;
						case 0xFF & SSADef.I_ALLOCREG:
							success = insAllocReg(reg0, iPar1, false);
							break;
						case 0xFF & SSADef.I_KILLREG:
							success = insKillReg(reg0);
							break;
						case 0xFF & SSADef.I_KILLOJMP:
							success = insKillOJmp(reg0);
							break;
						case 0xFF & SSADef.I_REGRANGE:
							success = insRegrange(reg0, reg1);
							break;
						case 0xFF & SSADef.I_EXCEPT:
							success = insException(iPar1);
							break;
						case 0xFF & SSADef.I_IVOF:
							out.println("Skipping inline var offset");
							break;
						case 0xFF & SSADef.I_TFBUILD:
							success = insThrowFrameBuild(reg0, iPar1, iPar2);
							break;
						case 0xFF & SSADef.I_TFUPD1:
							success = insThrowFrameUpdate(false, iPar1, iPar2);
							break;
						case 0xFF & SSADef.I_TFUPD2:
							success = insThrowFrameUpdate(true, iPar1, iPar2);
							break;
						case 0xFF & SSADef.I_TFRESET:
							success = insThrowFrameReset(reg0, iPar1);
							break;
						case 0xFF & SSADef.I_STKCHK:
							success = insStackExtremeCheck(reg0);
							break;
						default:
							out.print("unknown opcode: ");
							out.println(opcode);
							success = false;
					}
					endlessLoopHint = currentIP;
				}
			}
			if (!success)
			{
				out.println("Error during opcode emulation");
				out.print("Opcode was ");
				out.print(opcode);
				out.print(", next instruction would be at ");
				out.println(toHexString(currentIP));
				return false;
			}
		}
		// check for standard break conditions
		if (firstBreakPointC != null)
			breakCondCheck();
		return true;
	}
	
	/**
	 * @see Emulator#getMnemonicList(int)
	 */
	public MethodDisassembly getMnemonicList(int startIP)
	{
		Mnemonic mn = null;
		Mnemonic lastIns = null;
		MethodDisassembly result = null;
		int param = 0, opcode = 0, reg0 = 0, reg1 = 0, reg2 = 0, iPar1 = 0, iPar2 = 0;
		int tempIP, saveIP;
		boolean success = true, isJumpDest;
		long lPar = 0l;
		String pars;
		saveIP = currentIP;
		currentIP = startIP;
		do
		{
			isJumpDest = false;
			//get type and parameter
			tempIP = currentIP;
			param = readByte();
			opcode = readByte();
			//check for jump-destination
			if ((param & SSADef.IPJMPDEST) != 0)
			{
				param &= ~SSADef.IPJMPDEST;
				isJumpDest = true;
			}
			if (opcode == (0xFF & SSADef.I_INLINE))
			{ //MAGIC.inline
				pars = "";
				while (param-- > 0)
				{
					pars = pars.concat(toDecString(readByte() & 0xFF)).concat(" ");
				}
				mn = new Mnemonic(tempIP, "inline ", pars, isJumpDest);
			}
			else
			{
				if (opcode == (0xFF & SSADef.I_JUMP))
				{
					iPar1 = readByte();
					iPar2 = readInt();
					pars = toHexString(iPar2 + currentIP); //destination is relative to tempIP+jumpLen==currentIP
					mn = new Mnemonic(tempIP, getJump(iPar1), pars, isJumpDest);
				}
				else
				{
					param = param << SSADef.IPOPTSHFT;
					// fetch parameters
					if ((param & SSADef.IP_reg0) != 0)
						reg0 = readInt();
					if ((param & SSADef.IP_reg1) != 0)
						reg1 = readInt();
					if ((param & SSADef.IP_reg2) != 0)
						reg2 = readInt();
					if ((param & SSADef.IP_size) != 0)
						iPar1 = (byte) readByte(); //do sign extension
					else if ((param & SSADef.IP_im_i) != 0)
						iPar1 = readInt();
					if ((param & SSADef.IP_para) != 0)
						iPar2 = readInt();
					if ((param & SSADef.IP_im_l) != 0)
						lPar = readLong();
					// process instruction
					switch (opcode)
					{
						case 0xFF & SSADef.I_MARKER:
							pars = toDecString(iPar1);
							mn = new Mnemonic(tempIP, "marker   ", pars, isJumpDest);
							break;
						case 0xFF & SSADef.I_ENTER:
							pars = toDecString(iPar1).concat(" ").concat(toDecString(iPar2));
							mn = new Mnemonic(tempIP, "enter    ", pars, isJumpDest);
							break;
						case 0xFF & SSADef.I_ENTERINL:
							pars = toDecString(iPar1).concat(" ").concat(toDecString(iPar2));
							mn = new Mnemonic(tempIP, "enterinl ", pars, isJumpDest);
							break;
						case 0xFF & SSADef.I_NFREG:
							pars = toDecString(iPar1);
							mn = new Mnemonic(tempIP, "nfreg    ", pars, isJumpDest);
							break;
						case 0xFF & SSADef.I_LEAVE:
							pars = toDecString(iPar1).concat(" ").concat(toDecString(iPar2));
							mn = new Mnemonic(tempIP, "leave    ", pars, isJumpDest);
							break;
						case 0xFF & SSADef.I_LEAVEINL:
							pars = toDecString(iPar1).concat(" ").concat(toDecString(iPar2));
							mn = new Mnemonic(tempIP, "leaveinl ", pars, isJumpDest);
							break;
						case 0xFF & SSADef.I_LOADim_i:
							pars = getType(iPar1).concat("r").concat(toDecString(reg0)).concat(",").concat(toDecString(iPar2));
							mn = new Mnemonic(tempIP, "loadi    ", pars, isJumpDest);
							break;
						case 0xFF & SSADef.I_LOADim_l:
							pars = getType(iPar1).concat("r").concat(toDecString(reg0)).concat(",").concat(toLongHexString(lPar));
							mn = new Mnemonic(tempIP, "loadl    ", pars, isJumpDest);
							break;
						case 0xFF & SSADef.I_LOADim_p:
							pars = toHexString(iPar1);
							mn = new Mnemonic(tempIP, "loadimp  ", pars, isJumpDest);
							break;
						case 0xFF & SSADef.I_LOADnp:
							pars = getType(iPar1).concat("r").concat(toDecString(reg0));
							mn = new Mnemonic(tempIP, "loadn    ", pars, isJumpDest);
							break;
						case 0xFF & SSADef.I_LOADaddr:
							pars = "r".concat(toDecString(reg0)).concat(",[r").concat(toDecString(reg1)).concat("+".concat(toHexString(iPar1)).concat("]"));
							mn = new Mnemonic(tempIP, "loada    ", pars, isJumpDest);
							break;
						case 0xFF & SSADef.I_LOADval:
							pars = getType(iPar1).concat("r").concat(toDecString(reg0)).concat(",[r").concat(toDecString(reg1)).concat("]");
							mn = new Mnemonic(tempIP, "loadv    ", pars, isJumpDest);
							break;
						case 0xFF & SSADef.I_CONV:
							pars = getType(iPar1).concat("r").concat(toDecString(reg0)).concat(" from ").concat(getType(iPar2)).concat("r").concat(toDecString(reg1));
							mn = new Mnemonic(tempIP, "conv     ", pars, isJumpDest);
							break;
						case 0xFF & SSADef.I_COPY:
							pars = getType(iPar1).concat("r").concat(toDecString(reg0)).concat(",r").concat(toDecString(reg1));
							mn = new Mnemonic(tempIP, "copy     ", pars, isJumpDest);
							break;
						case 0xFF & SSADef.I_PUSHim_i:
							pars = getType(iPar1).concat(" ").concat(toDecString(iPar2));
							mn = new Mnemonic(tempIP, "pushi    ", pars, isJumpDest);
							break;
						case 0xFF & SSADef.I_PUSHim_l:
							pars = toLongHexString(lPar);
							mn = new Mnemonic(tempIP, "pushl    ", pars, isJumpDest);
							break;
						case 0xFF & SSADef.I_PUSHnp:
							mn = new Mnemonic(tempIP, "pushn    ", "", isJumpDest);
							break;
						case 0xFF & SSADef.I_PUSH:
							pars = getType(iPar1).concat("r").concat(toDecString(reg0));
							mn = new Mnemonic(tempIP, "push     ", pars, isJumpDest);
							break;
						case 0xFF & SSADef.I_POP:
							pars = getType(iPar1).concat("r").concat(toDecString(reg0));
							mn = new Mnemonic(tempIP, "pop      ", pars, isJumpDest);
							break;
						case 0xFF & SSADef.I_SAVE:
							pars = getType(iPar1).concat("r").concat(toDecString(reg0)).concat(",").concat(toDecString(iPar2));
							mn = new Mnemonic(tempIP, "save     ", pars, isJumpDest);
							break;
						case 0xFF & SSADef.I_REST:
							pars = getType(iPar1).concat("r").concat(toDecString(reg0)).concat(",").concat(toDecString(iPar2));
							mn = new Mnemonic(tempIP, "rest     ", pars, isJumpDest);
							break;
						case 0xFF & SSADef.I_ASSIGN:
							pars = getType(iPar1).concat("[r").concat(toDecString(reg0)).concat("],r").concat(toDecString(reg1));
							mn = new Mnemonic(tempIP, "assign   ", pars, isJumpDest);
							break;
						case 0xFF & SSADef.I_AND:
							pars = getType(iPar1).concat("r").concat(toDecString(reg0)).concat(",r").concat(toDecString(reg1)).concat(",r").concat(toDecString(reg2));
							mn = new Mnemonic(tempIP, "and      ", pars, isJumpDest);
							break;
						case 0xFF & SSADef.I_XOR:
							pars = getType(iPar1).concat("r").concat(toDecString(reg0)).concat(",r").concat(toDecString(reg1)).concat(",r").concat(toDecString(reg2));
							mn = new Mnemonic(tempIP, "xor      ", pars, isJumpDest);
							break;
						case 0xFF & SSADef.I_OR:
							pars = getType(iPar1).concat("r").concat(toDecString(reg0)).concat(",r").concat(toDecString(reg1)).concat(",r").concat(toDecString(reg2));
							mn = new Mnemonic(tempIP, "or       ", pars, isJumpDest);
							break;
						case 0xFF & SSADef.I_ADD:
							pars = getType(iPar1).concat("r").concat(toDecString(reg0)).concat(",r").concat(toDecString(reg1)).concat(",r").concat(toDecString(reg2));
							mn = new Mnemonic(tempIP, "add      ", pars, isJumpDest);
							break;
						case 0xFF & SSADef.I_SUB:
							pars = getType(iPar1).concat("r").concat(toDecString(reg0)).concat(",r").concat(toDecString(reg1)).concat(",r").concat(toDecString(reg2));
							mn = new Mnemonic(tempIP, "sub      ", pars, isJumpDest);
							break;
						case 0xFF & SSADef.I_MUL:
							pars = getType(iPar1).concat("r").concat(toDecString(reg0)).concat(",r").concat(toDecString(reg1)).concat(",r").concat(toDecString(reg2));
							mn = new Mnemonic(tempIP, "mul      ", pars, isJumpDest);
							break;
						case 0xFF & SSADef.I_DIV:
							pars = getType(iPar1).concat("r").concat(toDecString(reg0)).concat(",r").concat(toDecString(reg1)).concat(",r").concat(toDecString(reg2));
							mn = new Mnemonic(tempIP, "div      ", pars, isJumpDest);
							break;
						case 0xFF & SSADef.I_MOD:
							pars = getType(iPar1).concat("r").concat(toDecString(reg0)).concat(",r").concat(toDecString(reg1)).concat(",r").concat(toDecString(reg2));
							mn = new Mnemonic(tempIP, "mod      ", pars, isJumpDest);
							break;
						case 0xFF & SSADef.I_SHL:
							pars = getType(iPar1).concat("r").concat(toDecString(reg0)).concat(",r").concat(toDecString(reg1)).concat(",r").concat(toDecString(reg2));
							mn = new Mnemonic(tempIP, "shl      ", pars, isJumpDest);
							break;
						case 0xFF & SSADef.I_SHRL:
							pars = getType(iPar1).concat("r").concat(toDecString(reg0)).concat(",r").concat(toDecString(reg1)).concat(",r").concat(toDecString(reg2));
							mn = new Mnemonic(tempIP, "shrl     ", pars, isJumpDest);
							break;
						case 0xFF & SSADef.I_SHRA:
							pars = getType(iPar1).concat("r").concat(toDecString(reg0)).concat(",r").concat(toDecString(reg1)).concat(",r").concat(toDecString(reg2));
							mn = new Mnemonic(tempIP, "shra     ", pars, isJumpDest);
							break;
						case 0xFF & SSADef.I_NOT:
							pars = getType(iPar1).concat("r").concat(toDecString(reg0)).concat(",r").concat(toDecString(reg1));
							mn = new Mnemonic(tempIP, "not      ", pars, isJumpDest);
							break;
						case 0xFF & SSADef.I_NEG:
							pars = getType(iPar1).concat("r").concat(toDecString(reg0)).concat(",r").concat(toDecString(reg1));
							mn = new Mnemonic(tempIP, "neg      ", null, isJumpDest);
							break;
						case 0xFF & SSADef.I_BINV:
							pars = "r".concat(toDecString(reg0)).concat(",r").concat(toDecString(reg1));
							mn = new Mnemonic(tempIP, "binv     ", pars, isJumpDest);
							break;
						case 0xFF & SSADef.I_INCmem:
							pars = getType(iPar1).concat("[r").concat(toDecString(reg0)).concat("]");
							mn = new Mnemonic(tempIP, "incm     ", pars, isJumpDest);
							break;
						case 0xFF & SSADef.I_DECmem:
							pars = getType(iPar1).concat("[r").concat(toDecString(reg0)).concat("]");
							mn = new Mnemonic(tempIP, "decm     ", pars, isJumpDest);
							break;
						case 0xFF & SSADef.I_CALL:
							pars = toHexString(iPar1).concat(",parSize==").concat(toDecString(iPar2));
							mn = new Mnemonic(tempIP, "calld ", pars, isJumpDest);
							break;
						case 0xFF & SSADef.I_CALLind:
							pars = "r".concat(toDecString(reg0)).concat(",").concat(toDecString(iPar1)).concat(",parSize==").concat(toDecString(iPar2));
							mn = new Mnemonic(tempIP, "calli ", pars, isJumpDest);
							break;
						case 0xFF & SSADef.I_CALLim_p:
							pars = toHexString(iPar1).concat(",parSize==").concat(toDecString(iPar2));
							mn = new Mnemonic(tempIP, "callc ", pars, isJumpDest);
							break;
						case 0xFF & SSADef.I_CMP:
							pars = getType(iPar1).concat("r").concat(toDecString(reg0)).concat(",r").concat(toDecString(reg1)).concat(",par==").concat(toDecString(iPar2));
							mn = new Mnemonic(tempIP, "cmp      ", pars, isJumpDest);
							break;
						case 0xFF & SSADef.I_OUT:
							pars = getType(iPar1).concat("r").concat(toDecString(reg0)).concat(",r").concat(toDecString(reg1)).concat(",par==").concat(toDecString(iPar2));
							mn = new Mnemonic(tempIP, "out      ", pars, isJumpDest);
							break;
						case 0xFF & SSADef.I_IN:
							pars = getType(iPar1).concat("r").concat(toDecString(reg0)).concat(",r").concat(toDecString(reg1)).concat(",par==").concat(toDecString(iPar2));
							mn = new Mnemonic(tempIP, "in       ", pars, isJumpDest);
							break;
						case 0xFF & SSADef.I_BOUND:
							pars = "r".concat(toDecString(reg1)).concat(",[r").concat(toDecString(reg0)).concat("+").concat(toHexString(iPar1)).concat("]");
							mn = new Mnemonic(tempIP, "bound    ", pars, isJumpDest);
							break;
						case 0xFF & SSADef.I_DEREF:
							pars = "r".concat(toDecString(reg0)).concat(",[r").concat(toDecString(reg1)).concat("+r").concat(toDecString(reg2)).concat("*(").concat(toDecString(iPar2)).concat(")+").concat(toHexString(iPar1)).concat("]");
							mn = new Mnemonic(tempIP, "deref    ", pars, isJumpDest);
							break;
						case 0xFF & SSADef.I_MOVEMAP:
							pars = "r".concat(toDecString(reg0));
							mn = new Mnemonic(tempIP, "movemap  ", pars, isJumpDest);
							break;
						case 0xFF & SSADef.I_ALLOCREG:
							pars = getType(iPar1).concat("r").concat(toDecString(reg0));
							mn = new Mnemonic(tempIP, "alloc    ", pars, isJumpDest);
							break;
						case 0xFF & SSADef.I_KILLREG:
							pars = "r".concat(toDecString(reg0));
							mn = new Mnemonic(tempIP, "kill     ", pars, isJumpDest);
							break;
						case 0xFF & SSADef.I_KILLOJMP:
							pars = "r".concat(toDecString(reg0));
							mn = new Mnemonic(tempIP, "killojmp ", pars, isJumpDest);
							break;
						case 0xFF & SSADef.I_REGRANGE:
							pars = "r".concat(toDecString(reg0)).concat(",r").concat(toDecString(reg1));
							mn = new Mnemonic(tempIP, "regrange ", pars, isJumpDest);
							break;
						case 0xFF & SSADef.I_EXCEPT:
							pars = toDecString(iPar1);
							mn = new Mnemonic(tempIP, "except   ", pars, isJumpDest);
							break;
						case 0xFF & SSADef.I_IVOF:
							pars = toDecString((int) lPar).concat(",").concat(toDecString((int) (lPar >>> 32))).concat(",").concat(toDecString(iPar1)).concat(",").concat(toDecString(iPar2));
							mn = new Mnemonic(tempIP, "inlvof   ", pars, isJumpDest);
							break;
						case 0xFF & SSADef.I_FLOWHINT:
							pars = toDecString(iPar1).concat(",").concat(toDecString(iPar2));
							mn = new Mnemonic(tempIP, "flowhint ", pars, isJumpDest);
							break;
						case 0xFF & SSADef.I_TFBUILD:
							pars = "[r".concat(toDecString(reg0)).concat("],").concat(toDecString(iPar1)).concat(",").concat(toDecString(iPar2));
							mn = new Mnemonic(tempIP, "tfbuild  ", pars, isJumpDest);
							break;
						case 0xFF & SSADef.I_TFUPD1:
							pars = toDecString(iPar1).concat(",").concat(toDecString(iPar2));
							mn = new Mnemonic(tempIP, "tfupd1   ", pars, isJumpDest);
							break;
						case 0xFF & SSADef.I_TFUPD2:
							pars = toDecString(iPar1).concat(",").concat(toDecString(iPar2));
							mn = new Mnemonic(tempIP, "tfupd2   ", pars, isJumpDest);
							break;
						case 0xFF & SSADef.I_TFRESET:
							pars = "[r".concat(toDecString(reg0)).concat("],").concat(toDecString(iPar1));
							mn = new Mnemonic(tempIP, "tfreset  ", pars, isJumpDest);
							break;
						case 0xFF & SSADef.I_STKCHK:
							pars = "[r".concat(toDecString(reg0)).concat("]");
							mn = new Mnemonic(tempIP, "stlchk   ", pars, isJumpDest);
							break;
						default:
							out.print("unknown opcode: ");
							out.print(opcode);
							out.print(", next instruction would be at ");
							out.println(toHexString(currentIP));
							success = false;
					}
				}
				if (!success)
				{
					out.print("Error during opcode disassembly starting at ");
					out.println(toHexString(startIP));
					out.print("Opcode was ");
					out.println(opcode);
					return null;
				}
			}
			if (result == null)
			{
				result = new MethodDisassembly(mn, null);
				lastIns = mn;
			}
			else
			{
				lastIns.next = mn;
				lastIns = mn;
				// update the lastIP of this method
				result.lastIP = mn.startIP;
			}
		} while (opcode != (0xFF & SSADef.I_LEAVE));
		currentIP = saveIP;
		return result;
	}
	
	/**
	 * @see Emulator#getCurrentIP()
	 */
	public int getCurrentIP()
	{
		return currentIP;
	}
	
	/**
	 * @see Emulator#getStartOfMethod()
	 */
	public int getStartOfMethod(int currIP)
	{
		int saveIP, param, opcode, iPar1 = 0, res = 0;
		boolean leaveFound = false, goOn = true;
		
		saveIP = currentIP;
		currentIP = currIP;
		do
		{
			//get type and parameter
			param = readByte();
			opcode = readByte();
			if (opcode == (0xFF & SSADef.I_INLINE))
			{ //MAGIC.inline
				while (param-- > 0)
					readByte();
			}
			else if (opcode == (0xFF & SSADef.I_JUMP))
			{ //jump
				readByte();
				readInt();
			}
			else
			{ //normal instruction
				param = (param & ~SSADef.IPJMPDEST) << SSADef.IPOPTSHFT;
				// fetch parameters
				if ((param & SSADef.IP_reg0) != 0)
					currentIP += 4;
				if ((param & SSADef.IP_reg1) != 0)
					currentIP += 4;
				if ((param & SSADef.IP_reg2) != 0)
					currentIP += 4;
				if ((param & SSADef.IP_size) != 0)
					currentIP++;
				else if ((param & SSADef.IP_im_i) != 0)
					iPar1 = readInt();
				if ((param & SSADef.IP_para) != 0)
					currentIP += 4;
				if ((param & SSADef.IP_im_l) != 0)
					currentIP += 8;
				// process instruction
				if (opcode == (0xFF & SSADef.I_LEAVE))
					leaveFound = true;
				else if (leaveFound)
				{
					goOn = false;
					if (opcode == (0xFF & SSADef.I_MARKER)) //marker found
						res = currentIP - iPar1;
					else
						res = -1; //MARKER not found after LEAVE, signal error
				}
			}
		} while (goOn);
		currentIP = saveIP;
		return res;
	}
	
	/**
	 * @see Emulator#getEndOfMethod()
	 */
	public int getEndOfMethod(int currIP)
	{
		int saveIP, param, opcode, res;
		
		saveIP = currentIP;
		currentIP = currIP;
		while (true)
		{
			res = currentIP; //save current IP if the instruction is leave
			//get type and parameter
			param = readByte();
			opcode = readByte();
			if (opcode == (0xFF & SSADef.I_INLINE))
			{ //MAGIC.inline
				while (param-- > 0)
					readByte();
			}
			else if (opcode == (0xFF & SSADef.I_JUMP))
			{ //jump
				readByte();
				readInt();
			}
			else
			{ //normal instruction
				param = (param & ~SSADef.IPJMPDEST) << SSADef.IPOPTSHFT;
				// fetch parameters
				if ((param & SSADef.IP_reg0) != 0)
					currentIP += 4;
				if ((param & SSADef.IP_reg1) != 0)
					currentIP += 4;
				if ((param & SSADef.IP_reg2) != 0)
					currentIP += 4;
				if ((param & SSADef.IP_size) != 0)
					currentIP++;
				else if ((param & SSADef.IP_im_i) != 0)
					currentIP += 4;
				if ((param & SSADef.IP_para) != 0)
					currentIP += 4;
				if ((param & SSADef.IP_im_l) != 0)
					currentIP += 8;
				// process instruction
				if (opcode == (0xFF & SSADef.I_LEAVE))
				{
					currentIP = saveIP;
					return res; //set before reading the instruction
				}
			}
		}
	}
	
	/**
	 * @see Emulator#getCurrentSP()
	 */
	public int getCurrentSP()
	{
		return sReg[SSADef.R_STCK].ptr;
	}
}
