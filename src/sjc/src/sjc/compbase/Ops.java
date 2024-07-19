/* Copyright (C) 2005, 2006, 2007, 2008, 2009, 2010, 2012 Stefan Frenz
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

package sjc.compbase;

/**
 * Ops: platform and language independent operators
 *
 * @author S. Frenz
 * @version 120228 cleaned up "import sjc." typo
 * version 101027 splitted assign+ari to support assign+shift
 * version 090207 added copyright notice
 * version 080524 re-ordered CMP-values to have inversion by bit-toggle
 * version 070812 removed printOp as it is not used and import of TextPrinter has to be avoided
 * version 061109 added C_BO to check unsigned "<"
 * version 060807 added A_BASE to allow bitmasks for arithemtic operations
 * version 060607 initial version
 */

public class Ops
{
	public final static int S_ASN = 20; //assignment (pure, see S_ASNARI and S_ASNBSH)
	public final static int S_PFX = 21; //prefix and postfix
	public final static int S_ARI = 22; //arithmetic operators
	public final static int S_BSH = 23; //bitshift operators
	public final static int S_LOG = 24; //logical operators
	public final static int S_CMP = 25; //compares
	public final static int S_ASNARI = 26; //assignment with implicit arithmentic operation
	public final static int S_ASNBSH = 27; //assignment with implicit bitshift operation
	
	//types for PFX                   
	public final static int P_INC = 0x11; //"++"
	public final static int P_DEC = 0x12; //"--"
	//base value for bitmasks detecting binop-rte-calls
	public final static int MSKBSE = 0x21; //lowest ari-value, must be below BSH, LOG and CMP
	//types for set ARI
	public final static int A_AND = 0x21; //"&"
	public final static int A_OR = 0x22; //"|"
	public final static int A_XOR = 0x23; //"^"
	public final static int A_CPL = 0x24; //"~" may be used as preOp, too
	public final static int A_PLUS = 0x25; //"+" may be used as preOp, too
	public final static int A_MINUS = 0x26; //"-" may be used as preOp, too
	public final static int A_MUL = 0x27; //"*"
	public final static int A_DIV = 0x28; //"/"
	public final static int A_MOD = 0x29; //"%"
	//types for set BSH
	public final static int B_SHL = 0x2A; //"<<"
	public final static int B_SHRL = 0x2B; //">>>"
	public final static int B_SHRA = 0x2C; //">>"
	//types for set LOG
	public final static int L_NOT = 0x2D; //"!"
	public final static int L_AND = 0x2E; //"&&"
	public final static int L_OR = 0x2F; //"||"
	//types for set CMP
	public final static int INVCBIT = 0x01; //bit to toggle for inversion of compare condition
	public final static int C_EQ = 0x30; //"=="
	public final static int C_NE = 0x31; //"!="
	public final static int C_LW = 0x32; //"<"
	public final static int C_GE = 0x33; //">="
	public final static int C_LE = 0x34; //"<="
	public final static int C_GT = 0x35; //">"
	public final static int C_BO = 0x36; //unsigned "<" i.e. below (only used for bound-check, no inversion)
	public final static int C_INOF = 0x38; //"instanceof" (no inversion)
}
