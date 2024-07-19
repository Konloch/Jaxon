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

package sjc.backend.ssa;

/**
 * SSAdef: definition of registers, parameters and instruction opcodes for SSA
 *
 * @author S. Frenz
 * @version 110127 added I_ASSIGNcmplx and I_LOADcmplx instructions used by SSA-optimizer
 * version 101101 added third register to I_DEREF
 * version 090717 added I_STKCHK
 * version 090207 added copyright notice and cleaned up sugar
 * version 081021 modified I_CMP, I_IN and I_OUT matching changes in Architecture
 * version 080616 added neccessary instructions to support language throwables
 * version 080508 added I_FLOWHINT to support insertFlowHint
 * version 080221 checked sugar changes, committed changes to compiler tree
 * version 080220 various "sugar" changes (A. Venner)
 * version 080203 added I_ENTERINL and I_LEAVEINL to support method inlining
 * version 070912 added new instruction I_IVOF to support inlineVarOffset
 * version 070114 added parSize in genCall
 * version 061202 adopted change of genCall
 * version 060628 added support for static compilation
 * version 060620 added kill-on-jump
 * version 060609 comment bugfix and inserted jump conditions
 * version 060607 initial version
 */
public class SSADef
{
	//parameter bit definitions
	//do not mix size, rela, im_i in a single instruction
	public final static int IP_reg0 = 0x0100;
	public final static int IP_reg1 = 0x0200;
	public final static int IP_reg2 = 0x0400;
	public final static int IP_size = 0x0800;
	public final static int IP_im_i = 0x1000;
	public final static int IP_para = 0x2000;
	public final static int IP_im_l = 0x4000;
	public final static int IP_rela = IP_im_i;
	public final static int IP_iPar1 = IP_size | IP_im_i;
	
	//instruction bit mask definitions
	public final static int IPOPTMASK = 0x7F00; //get the flags of an instruction-type
	public final static int IPOPTSHFT = 8; //bits to shift the options
	public final static int IPJMPCOND = 0x08; //treat as IP_size, shifted already
	public final static int IPJMPDEST = 0x80; //recycled bit of IP_rela, shifted already
	public final static int ICODEMASK = 0xFF; //this is the mask to get the instruction type
	
	//bit mask for normal instructions:     type   reg0      reg1      reg2      iPar1     iPar2     lPar
	public final static int I_MARKER = 0x01 | IP_im_i;                     //marker inside the current method
	public final static int I_ENTER = 0x02 | IP_im_i | IP_para;           //enter method
	public final static int I_ENTERINL = 0x03 | IP_im_i | IP_para;           //enter inline method
	public final static int I_NFREG = 0x04 | IP_im_i;                     //hint containing the next free register
	public final static int I_LEAVE = 0x05 | IP_im_i | IP_para;           //leave method
	public final static int I_LEAVEINL = 0x06 | IP_im_i | IP_para;           //leave inline method
	public final static int I_LOADim_i = 0x07 | IP_reg0 | IP_size | IP_para;           //load 8/16/32 bit immediate value
	public final static int I_LOADim_l = 0x08 | IP_reg0 | IP_size | IP_im_l; //load 64 bit immediate value
	public final static int I_LOADim_p = 0x09 | IP_reg0 | IP_im_i;                     //load 32 bit immediate pointer
	public final static int I_LOADnp = 0x0A | IP_reg0 | IP_size;                     //load null pointer
	public final static int I_LOADaddr = 0x0B | IP_reg0 | IP_reg1 | IP_rela;                     //load pointer from memory with optional offset
	public final static int I_LOADval = 0x0C | IP_reg0 | IP_reg1 | IP_size;                     //load value from memory
	public final static int I_CONV = 0x0D | IP_reg0 | IP_reg1 | IP_size | IP_para;           //convert reg1/para to size and store in reg0
	public final static int I_COPY = 0x0E | IP_reg0 | IP_reg1 | IP_size;                     //copy register
	public final static int I_PUSHim_i = 0x0F | IP_size | IP_para;           //push 8/16/32 bit immediate
	public final static int I_PUSHim_l = 0x10 | IP_im_l; //push 64 bit immediate
	public final static int I_PUSHnp = 0x11;                                                             //push null pointer
	public final static int I_PUSH = 0x12 | IP_reg0 | IP_size;                     //push register
	public final static int I_POP = 0x13 | IP_reg0 | IP_size;                     //pop to register
	public final static int I_SAVE = 0x14 | IP_reg0 | IP_size | IP_para;           //save register for later use (para==0 if register is not written yet)
	public final static int I_REST = 0x15 | IP_reg0 | IP_size | IP_para;           //restore register (para==0 if register to be restore was not written yet)
	public final static int I_ASSIGN = 0x16 | IP_reg0 | IP_reg1 | IP_size;                     //write reg1 to memory location pointed to by reg0
	public final static int I_AND = 0x17 | IP_reg0 | IP_reg1 | IP_reg2 | IP_size;                     //binary "and" operation
	public final static int I_XOR = 0x18 | IP_reg0 | IP_reg1 | IP_reg2 | IP_size;                     //binary "xor" operation
	public final static int I_OR = 0x19 | IP_reg0 | IP_reg1 | IP_reg2 | IP_size;                     //binary "or" operation
	public final static int I_ADD = 0x1A | IP_reg0 | IP_reg1 | IP_reg2 | IP_size;                     //binary "add" operation
	public final static int I_SUB = 0x1B | IP_reg0 | IP_reg1 | IP_reg2 | IP_size;                     //binary "subtract" operation
	public final static int I_MUL = 0x1C | IP_reg0 | IP_reg1 | IP_reg2 | IP_size;                     //binary "multiply" operation
	public final static int I_DIV = 0x1D | IP_reg0 | IP_reg1 | IP_reg2 | IP_size;                     //binary "divide" operation
	public final static int I_MOD = 0x1E | IP_reg0 | IP_reg1 | IP_reg2 | IP_size;                     //binary "modulo" operation
	public final static int I_SHL = 0x1F | IP_reg0 | IP_reg1 | IP_reg2 | IP_size;                     //binary "shift left" operation
	public final static int I_SHRL = 0x20 | IP_reg0 | IP_reg1 | IP_reg2 | IP_size;                     //binary "shift right logically" operation
	public final static int I_SHRA = 0x21 | IP_reg0 | IP_reg1 | IP_reg2 | IP_size;                     //binary "shift right arithmetically" operation
	public final static int I_NOT = 0x22 | IP_reg0 | IP_reg1 | IP_size;                     //unary "not" operation
	public final static int I_NEG = 0x23 | IP_reg0 | IP_reg1 | IP_size;                     //unary "negate" operation
	public final static int I_BINV = 0x24 | IP_reg0 | IP_reg1;                                         //unary "bool invert" operation
	public final static int I_INCmem = 0x25 | IP_reg0 | IP_size;                     //increment memory value pointed to by reg0
	public final static int I_DECmem = 0x26 | IP_reg0 | IP_size;                     //decrement memory value pointed to by reg0
	public final static int I_CALL = 0x27 | IP_reg0 | IP_rela | IP_para;           //call method directly through register
	public final static int I_CALLind = 0x28 | IP_reg0 | IP_rela | IP_para;           //call method indirectly through register/memory
	public final static int I_CALLim_p = 0x29 | IP_im_i | IP_para;           //call method at constant address
	public final static int I_CMP = 0x2A | IP_reg0 | IP_reg1 | IP_size | IP_para;           //compare value of registers and set condition flags
	public final static int I_OUT = 0x2B | IP_reg0 | IP_reg1 | IP_size | IP_para;           //write reg1 to special memory location pointed to by reg0 (may be handled differently depending on target architecture)
	public final static int I_IN = 0x2C | IP_reg0 | IP_reg1 | IP_size | IP_para;           //read reg0 from special memory location pointerd to by reg1 (may be handled differently depending on target architecture)
	public final static int I_BOUND = 0x2D | IP_reg0 | IP_reg1 | IP_rela;                     //do bound check
	public final static int I_DEREF = 0x2E | IP_reg0 | IP_reg1 | IP_reg2 | IP_rela | IP_para;           //calculate address of value inside array (base-pointer, offset, element number, element size)
	public final static int I_MOVEMAP = 0x2F | IP_reg0;                                                   //move interface map from primary result register to upper part of double pointer
	public final static int I_ALLOCREG = 0x30 | IP_reg0 | IP_size;                     //allocate register
	public final static int I_KILLREG = 0x31 | IP_reg0;                                                   //de-allocate/kill a register
	public final static int I_KILLOJMP = 0x32 | IP_reg0;                                                   //automatically de-allocate/kill a register on next jump (for conditional jumps: only if jump is done)
	public final static int I_REGRANGE = 0x33 | IP_reg0 | IP_reg1;                                         //hint containing the used register range (lower/upper bound)
	public final static int I_EXCEPT = 0x34 | IP_im_i;                     //execute a native exception of type im_i (see below)
	public final static int I_IVOF = 0x35 | IP_im_i | IP_para;           //inline offset of variable (may be handled differently depending on target architecture)
	public final static int I_FLOWHINT = 0x36 | IP_im_i | IP_para;           //flow control hint (see Architecture.F_*)
	public final static int I_TFBUILD = 0x37 | IP_reg0 | IP_rela | IP_para;           //build and set up a try-catch-finally-block
	public final static int I_TFUPD1 = 0x38 | IP_rela | IP_para;           //update an already existing try-catch-finally-block (part 1), always followed by I_TFUPD2
	public final static int I_TFUPD2 = 0x39 | IP_rela | IP_para;           //update an already existing try-catch-finally-block (part 2), always following I_TFUPD1
	public final static int I_TFRESET = 0x3A | IP_reg0 | IP_rela;                     //reset a valid try-catch-finally-block to it's original state
	public final static int I_STKCHK = 0x3B | IP_reg0;                                                   //insert special stack extreme check instructions, followed by CC_BO conditional jump
	public final static int I_LOADcmplx = 0x40 | IP_reg0 | IP_reg1 | IP_size | IP_para;           //do a complex load (used by optimizer)
	public final static int I_ASSIGNcmplx = 0x41 | IP_reg0 | IP_reg1 | IP_size | IP_para;           //do a complex assign (used by optimizer)
	
	//special instructions
	public final static int I_JUMP = 0xFE; //jump, next byte contains jump condition (see below)
	public final static int I_INLINE = 0xFF; //magic instruction for target architecture, next byte contains amount of magic code bytes
	
	//registers
	public final static int R_CLSS = 1; //current class context (if required)
	public final static int R_INST = 2; //current instance context (if existing)
	public final static int R_BASE = 3; //base pointer to current method's stack frame
	public final static int R_STCK = 4; //current stack pointer
	public final static int R_PRIR = 5; //primary result register (can be written everywhere and read after a call)
	public final static int R_GPRS = 6; //first register number for general purpose registers
	
	//jump conditions
	public final static int CC_AL = 0; //jump always
	public final static int CC_LW = 1; //jump if lower (signed)
	public final static int CC_LE = 2; //jump if lower of equal (signed)
	public final static int CC_EQ = 3; //jump if equal
	public final static int CC_GE = 4; //jump if greater and equal (signed)
	public final static int CC_GT = 5; //jump if greater (signed)
	public final static int CC_NE = 6; //jump if not equal
	public final static int CC_BO = 7; //jump if below (unsigned)
	
	//native exceptions
	public final static int E_BOUND = 1; //bound exception
	
	//special flow hints
	public final static int F_DOTHROW = -1; //current method is runtime environment method called if an exception is thrown (required to emulate exceptions)
}
