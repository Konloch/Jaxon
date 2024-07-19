/* Copyright (C) 2005, 2006, 2007, 2008, 2009, 2010, 2011, 2012 Stefan Frenz
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

package sjc.frontend;

import sjc.compbase.*;
import sjc.osio.TextPrinter;
import sjc.osio.TextReader;

/**
 * SScanner: scanner for the SJava-language
 *
 * @author S. Frenz
 * @version 121017 allowed '$' for names
 * version 120227 cleaned up "package sjc." typo
 * version 110328 added check for int overflow
 * version 101203 added workaround for zero floating points with multiple zeros (avoid octal numbers here)
 * version 101110 added support for octal numbers
 * version 101027 added support for assign+shift, therefore introduced special assign+ari symbol-set
 * version 101021 added support for "assert"
 * version 100924 removed unneccessary check for triple-"-" and -"+"
 * version 100505 restructured getSym, adopted changed StringPool interface, optimized id/keyword scanning
 * version 091209 moved to frontend package and renamed to SScanner
 * version 091102 added support for sypos and lastSyposEnd
 * version 091018 optimized getSym(.)
 * version 090218 added support for "synchronized" and "volatile"
 * version 090207 added copyright notice
 * version 080925 better detection of invalid numbers
 * version 080603 added support for "throws"
 * version 080121 added support for "transient" modifier
 * version 080118 added support for "@interface" annotation declaration keyword and M_ANNO
 * version 071001 added support for "native" modifier
 * version 070823 changed detection of long-buffer-overflow for float/double
 * version 070808 added support for float and double
 * version 070114 reduced access level where possible
 * version 061126 added support for ?:-operator
 * version 060711 added support for '\0' character
 * version 060620 fixed getNumericExt
 * version 060607 initial version
 */

public class SScanner
{
	public final static int MAX_SYMBOL_LENGTH = 255;
	public final static int RES = 0;
	
	//symbols and symbol-sets
	public final static int S_ERR = -2; //error in symbol
	public final static int S_EOF = -1; //end of input-file reached
	public final static int S_ID = 1; //identifier
	public final static int S_NUM = 2; //numeric value (subtypes from S_TYP)
	public final static int S_SCT = 3; //string-constants
	public final static int S_MOD = 4; //modifiers
	public final static int S_FLC = 5; //flow-control in statement
	public final static int S_OKE = 6; //other keywords
	public final static int S_TYP = 7; //standard-types (subtypes also for S_NUM)
	public final static int S_DEL = 8; //delimiter
	public final static int S_ENC = 9; //enclosure-characters
	public final static int S_ASK = 10; //choose-operator "?"
	public final static int S_ASN = Ops.S_ASN; //assignment (pure, see S_ASNARI and S_ASNBSH)
	public final static int S_PFX = Ops.S_PFX; //prefix and postfix
	public final static int S_CMP = Ops.S_CMP; //compares
	public final static int S_ARI = Ops.S_ARI; //arithmetic operators
	public final static int S_BSH = Ops.S_BSH; //bitshift operators
	public final static int S_LOG = Ops.S_LOG; //logical operators
	public final static int S_ASNARI = Ops.S_ASNARI; //assignment with implicit arithmentic operation
	public final static int S_ASNBSH = Ops.S_ASNBSH; //assignment with implicit bitshift operation
	
	//types for set MOD, bitfields (used in Parser)
	public final static int M_PUB = Modifier.M_PUB;   //"public"
	public final static int M_PROT = Modifier.M_PROT;  //"public"
	public final static int M_PRIV = Modifier.M_PRIV;  //"private"
	public final static int M_FIN = Modifier.M_FIN;   //"final"
	public final static int M_STAT = Modifier.M_STAT;  //"static"
	public final static int M_ABSTR = Modifier.M_ABSTR; //"abstract"
	public final static int M_NAT = Modifier.M_NAT;   //"native"
	public final static int M_TRANS = Modifier.M_TRANS; //"transient"
	public final static int M_ANNO = Modifier.M_ANNO;  //annotation "@"
	public final static int M_SYNC = Modifier.M_SYNC;  //"synchronized"
	public final static int M_VOLAT = Modifier.M_VOLAT; //"volatile"
	//types for set TYP
	public final static int T_BYTE = StdTypes.T_BYTE; //"byte"
	public final static int T_SHRT = StdTypes.T_SHRT; //"short"
	public final static int T_INT = StdTypes.T_INT;  //"int"
	public final static int T_LONG = StdTypes.T_LONG; //"long"
	public final static int T_FLT = StdTypes.T_FLT;  //"float"
	public final static int T_DBL = StdTypes.T_DBL;  //"double"
	public final static int T_CHAR = StdTypes.T_CHAR; //"char"
	public final static int T_BOOL = StdTypes.T_BOOL; //"boolean"
	public final static int T_NULL = StdTypes.T_NULL; //null-type
	//types for set FLC
	public final static int F_IF = 1; //"if"
	public final static int F_ELSE = 2; //"else"
	public final static int F_FOR = 3; //"for"
	public final static int F_WHILE = 4; //"while"
	public final static int F_DO = 5; //"do"
	public final static int F_SWTCH = 6; //"switch"
	public final static int F_CASE = 7; //"case"
	public final static int F_DFLT = 8; //"default"
	public final static int F_RET = 9; //"return"
	public final static int F_BRK = 10; //"break"
	public final static int F_CNT = 11; //"continue"
	public final static int F_TRY = 12; //"try"
	public final static int F_CATCH = 13; //"catch"
	public final static int F_FIN = 14; //"finally"
	public final static int F_THROW = 15; //"throw"
	public final static int F_THRWS = 16; //"throws"
	public final static int F_ASSRT = 17; //"assert"
	//type for set OKE
	public final static int O_NEW = 1; //"new"
	public final static int O_VOID = 2; //"void"
	public final static int O_PACK = 3; //"package"
	public final static int O_IMPT = 4; //"import"
	public final static int O_CLSS = 5; //"class"
	public final static int O_INTF = 6; //"interface"
	public final static int O_EXTS = 7; //"extends"
	public final static int O_IMPL = 8; //"implements"
	public final static int O_ANDC = 9; //annotation "@interface"
	//types for set DEL
	public final static int D_SEM = 1; //";"
	public final static int D_COL = 2; //":"
	public final static int D_COM = 3; //","
	public final static int D_DOT = 4; //"."
	//types for set CMP
	public final static int C_LW = Ops.C_LW; //"<"
	public final static int C_LE = Ops.C_LE; //"<="
	public final static int C_EQ = Ops.C_EQ; //"=="
	public final static int C_GE = Ops.C_GE; //">="
	public final static int C_GT = Ops.C_GT; //">"
	public final static int C_NE = Ops.C_NE; //"!="
	public final static int C_INOF = Ops.C_INOF; //"instanceof"
	//types for set ARI
	public final static int A_AND = Ops.A_AND;   //"&"
	public final static int A_OR = Ops.A_OR;    //"|"
	public final static int A_XOR = Ops.A_XOR;   //"^"
	public final static int A_CPL = Ops.A_CPL;   //"~"
	public final static int A_PLUS = Ops.A_PLUS;  //"+"
	public final static int A_MINUS = Ops.A_MINUS; //"-"
	public final static int A_MUL = Ops.A_MUL;   //"*" (also used in import-statement as wildcard)
	public final static int A_DIV = Ops.A_DIV;   //"/"
	public final static int A_MOD = Ops.A_MOD;   //"%"
	//types for PFX                   
	public final static int P_INC = Ops.P_INC; //"++"
	public final static int P_DEC = Ops.P_DEC; //"--"
	//types for set BSH
	public final static int B_SHL = Ops.B_SHL;  //"<<"
	public final static int B_SHRL = Ops.B_SHRL; //">>>"
	public final static int B_SHRA = Ops.B_SHRA; //">>"
	//types for set LOG
	public final static int L_NOT = Ops.L_NOT; //"!"
	public final static int L_AND = Ops.L_AND; //"&&"
	public final static int L_OR = Ops.L_OR;  //"||"
	//types for set ENC
	public final static int E_RO = 1; //"(" round open
	public final static int E_RC = 2; //")" round close
	public final static int E_BO = 3; //"{" bracket open
	public final static int E_BC = 4; //"}" bracket close
	public final static int E_SO = 5; //"[" squared bracket open
	public final static int E_SC = 6; //"]" squared bracket close
	public final static int E_SOC = 7; //"[]" squared brackets open and close
	
	//internally used categories of characters
	private final static int CCT_OTHER = 0;
	private final static int CCT_CTRL = 1; //' ' and below
	private final static int CCT_ALPHAEXT = 2; //'A'..'Z', 'a'..'z', '_', '$'
	private final static int CCT_BASEOP1 = 3; //'!', '"', '#', '%', '&', '''
	private final static int CCT_BASEOP2 = 4; //'(', ')', '*', '+', ',', '-', '.', '/'
	private final static int CCT_BASEOP3 = 5; //':', ';', '<', '=', '>', '?'
	
	public SScanSym nxtSym, lahSym; //next and lookahead symbol
	public int endOfLastSymbol; //position of end of last symbol in file
	private Context ctx; //calling java-compiler
	private TextReader r; //Input-Reader
	private int curFID; //current file-id
	private TextPrinter v; //for outputs
	private final StringPool sp; //Pool for strings
	private SScanSym l2aSym; //look-2-ahead symbol for internal recognition of identifier
	private final char[] chrBuf;
	private int bufLen;
	
	public SScanner()
	{
		sp = new StringPool();
		chrBuf = new char[MAX_SYMBOL_LENGTH + 1];
		nxtSym = new SScanSym();
		lahSym = new SScanSym();
		l2aSym = new SScanSym();
	}
	
	public void init(TextReader ir, int fid, Context ic)
	{
		r = ir;
		curFID = fid;
		ctx = ic;
		v = ctx.out;
		nxtSym.type = RES;
		nxtSym.par = RES;
		lahSym.type = RES;
		lahSym.par = RES;
		l2aSym.type = RES;
		l2aSym.par = RES;
		next(); //initialize internal look-2-ahead
		next(); //initialize lookahead
		next(); //initialize next
	}
	
	public boolean next()
	{
		SScanSym dumSym;
		
		endOfLastSymbol = nxtSym.syposE;
		if (nxtSym.type == S_ERR || nxtSym.type == S_EOF)
			return false;
		if (l2aSym.type == S_EOF)
		{
			if (lahSym.type == S_EOF)
			{
				nxtSym.type = S_EOF;
				return false;
			}
			dumSym = nxtSym;
			nxtSym = lahSym;
			lahSym = dumSym;
			lahSym.type = S_EOF;
			return true;
		}
		//exchange actual / next symbol if no error
		dumSym = nxtSym;
		nxtSym = lahSym;
		lahSym = l2aSym;
		l2aSym = dumSym;
		//get the symbol
		if (!getSym())
			return false;
		//check if square brackets open+close
		if (lahSym.type == S_ENC && lahSym.par == E_SO && l2aSym.type == S_ENC && l2aSym.par == E_SC)
		{
			lahSym.par = E_SOC;
			getSym();
			l2aSym.syposE = r.pos;
			return true;
		}
		//check if "@interface"
		if (lahSym.type == S_MOD && lahSym.par == M_ANNO && l2aSym.type == S_OKE && l2aSym.par == O_INTF)
		{
			lahSym.type = S_OKE;
			lahSym.par = O_ANDC;
			getSym();
			l2aSym.syposE = r.pos;
			return true;
		}
		//normal symbol
		l2aSym.syposE = r.pos;
		return true;
	}
	
	private boolean getSym()
	{
		l2aSym.type = RES;
		l2aSym.strBuf = null;
		//evaluate next symbol
		while (r.nextChar != '\0')
		{
			l2aSym.syline = r.line;
			l2aSym.sycol = r.col;
			l2aSym.sypos = r.pos;
			if (isNum(r.nextChar) || (r.nextChar == '.' && isNum(r.lookAhead)))
			{
				if (r.nextChar == '0' && (r.lookAhead == 'x' || r.lookAhead == 'X'))
				{
					r.readChar();
					r.readChar();
					if (!isAlphaNum(r.nextChar))
					{
						scannerError("invalid number");
						return false;
					}
					readNumValue(16);
					return true;
				}
				readNumValue(r.nextChar == '0' && isNum(r.lookAhead) ? 8 : 10);
				return true;
			}
			switch (getCharCat(r.nextChar))
			{
				case CCT_CTRL: //control
					switch (r.nextChar)
					{
						//separators
						case ' ':
						case '\t':
						case '\r':
						case '\n':
							r.readChar();
							continue;
					}
					break;
				case CCT_ALPHAEXT: //identifier or keyword
					bufLen = 0;
					do
					{
						chrBuf[bufLen++] = r.nextChar;
						if (bufLen >= MAX_SYMBOL_LENGTH)
						{
							scannerError("too many characters for buffer");
							return false;
						}
						r.readChar();
					} while (isAlphaNum(r.nextChar) || r.nextChar == '_' || r.nextChar == '$');
					if (!checkKeyword(chrBuf, bufLen))
					{ //checkKeyword will set type and par
						l2aSym.strBuf = sp.getString(chrBuf, bufLen);
						l2aSym.type = S_ID;
					}
					return true;
				case CCT_BASEOP1: //'!', '"', '#', '%', '&', '''
					switch (r.nextChar)
					{
						case '"':
							r.readChar();
							bufLen = 0;
							while (r.nextChar != '\0' && r.nextChar != '"')
							{
								chrBuf[bufLen] = readChValue();
								if (l2aSym.type == S_ERR)
									return false; //there were an error in readChValue
								bufLen++;
								if (bufLen > MAX_SYMBOL_LENGTH)
								{
									scannerError("too many characters for buffer");
									return false;
								}
							}
							if (r.nextChar != '"')
							{
								scannerError("not closed string-constant");
								return false;
							}
							r.readChar();
							l2aSym.strBuf = sp.getString(chrBuf, bufLen);
							l2aSym.type = S_SCT;
							l2aSym.par = RES;
							return true;
						case '\'':
							r.readChar();
							l2aSym.intBuf = readChValue();
							if (l2aSym.type == S_ERR)
								return false; //there were an error in readChValue
							if (r.nextChar != '\'')
							{
								scannerError("not closed character-constant");
								return false;
							}
							r.readChar();
							l2aSym.type = S_NUM;
							l2aSym.par = T_CHAR;
							return true;
						case '!':
							r.readChar();
							if (r.nextChar == '=')
							{ // "!=": unequal
								r.readChar();
								l2aSym.type = S_CMP;
								l2aSym.par = C_NE;
								return true;
							}
							// "!": logical not
							l2aSym.type = S_LOG;
							l2aSym.par = L_NOT;
							return true;
						case '&':
							r.readChar();
							if (r.nextChar == '&')
							{ // "&&": logical and
								r.readChar();
								l2aSym.type = S_LOG;
								l2aSym.par = L_AND;
								return true;
							}
							if (r.nextChar == '=')
							{ // "&=": arithmetic and + assign
								r.readChar();
								l2aSym.type = S_ASNARI;
								l2aSym.par = A_AND;
								return true;
							}
							// "&": arithmetic bitwise and
							l2aSym.type = S_ARI;
							l2aSym.par = A_AND;
							return true;
						case '%':
							r.readChar();
							if (r.nextChar == '=')
							{ // "%=": modulo + assign
								r.readChar();
								l2aSym.type = S_ASNARI;
								l2aSym.par = A_MOD;
								return true;
							}
							// "%": modulo
							l2aSym.type = S_ARI;
							l2aSym.par = A_MOD;
							return true;
					}
					break;
				case CCT_BASEOP2: //'(', ')', '*', '+', ',', '-', '.', '/'
					switch (r.nextChar)
					{
						case '(':
							r.readChar();
							l2aSym.type = S_ENC;
							l2aSym.par = E_RO;
							return true;
						case ')':
							r.readChar();
							l2aSym.type = S_ENC;
							l2aSym.par = E_RC;
							return true;
						case '.':
							r.readChar();
							l2aSym.type = S_DEL;
							l2aSym.par = D_DOT;
							return true;
						case ',':
							r.readChar();
							l2aSym.type = S_DEL;
							l2aSym.par = D_COM;
							return true;
						case '*':
							r.readChar();
							if (r.nextChar == '=')
							{ // "*=": multply + assign
								r.readChar();
								l2aSym.type = S_ASNARI;
								l2aSym.par = A_MUL;
								return true;
							}
							l2aSym.type = S_ARI;
							l2aSym.par = A_MUL;
							return true;
						case '+':
							r.readChar();
							if (r.nextChar == '+')
							{ // "++": prae- or postfix increase
								r.readChar();
								l2aSym.type = S_PFX;
								l2aSym.par = P_INC;
								return true;
							}
							if (r.nextChar == '=')
							{ // "+=": plus + assign
								r.readChar();
								l2aSym.type = S_ASNARI;
								l2aSym.par = A_PLUS;
								return true;
							}
							// "+": plus
							l2aSym.type = S_ARI;
							l2aSym.par = A_PLUS;
							return true;
						case '-':
							r.readChar();
							if (r.nextChar == '-')
							{ // "--": prae- or postfix decrease
								r.readChar();
								l2aSym.type = S_PFX;
								l2aSym.par = P_DEC;
								return true;
							}
							if (r.nextChar == '=')
							{ // "-=": minus + assign
								r.readChar();
								l2aSym.type = S_ASNARI;
								l2aSym.par = A_MINUS;
								return true;
							}
							// "-": minus
							l2aSym.type = S_ARI;
							l2aSym.par = A_MINUS;
							return true;
						case '/':
							r.readChar();
							if (r.nextChar == '/')
							{ // "//": comment rest of line
								while (r.nextChar != '\0' && r.nextChar != '\r' && r.nextChar != '\n')
								{
									r.readChar();
								}
								continue;
							}
							else if (r.nextChar == '*')
							{ // "/*": start of comment
								r.readChar();
								while (r.nextChar != '\0')
								{
									if (r.nextChar == '*' && r.lookAhead == '/')
										break; //leave while-loop
									r.readChar();
								}
								if (r.nextChar != '*' || r.lookAhead != '/')
								{
									scannerError("missing end of comment");
									return false;
								}
								r.readChar();
								r.readChar();
								continue;
							}
							if (r.nextChar == '=')
							{ // "/=": divide + assign
								r.readChar();
								l2aSym.type = S_ASNARI;
								l2aSym.par = A_DIV;
								return true;
							}
							// "/": divide
							l2aSym.type = S_ARI;
							l2aSym.par = A_DIV;
							return true;
					}
					break;
				case CCT_BASEOP3: //':', ';', '<', '=', '>', '?'
					switch (r.nextChar)
					{
						case ';':
							r.readChar();
							l2aSym.type = S_DEL;
							l2aSym.par = D_SEM;
							return true;
						case ':':
							r.readChar();
							l2aSym.type = S_DEL;
							l2aSym.par = D_COL;
							return true;
						case '?':
							r.readChar();
							l2aSym.type = S_ASK;
							l2aSym.par = RES;
							return true;
						case '=':
							r.readChar();
							if (r.nextChar == '=')
							{ // "==": equal to
								r.readChar();
								l2aSym.type = S_CMP;
								l2aSym.par = C_EQ;
								return true;
							}
							// "=": assignment
							l2aSym.type = S_ASN;
							l2aSym.par = RES;
							return true;
						case '>':
							r.readChar();
							if (r.nextChar == '=')
							{ // ">=": greater than or equal to
								r.readChar();
								l2aSym.type = S_CMP;
								l2aSym.par = C_GE;
								return true;
							}
							if (r.nextChar == '>')
							{ // ">>" so far
								r.readChar();
								if (r.nextChar == '>')
								{ // ">>>": shift right without sign
									r.readChar();
									if (r.nextChar == '=')
									{ // ">>>=: shift right without sign with assignment
										r.readChar();
										l2aSym.type = S_ASNBSH;
										l2aSym.par = B_SHRL;
										return true;
									}
									l2aSym.type = S_BSH;
									l2aSym.par = B_SHRL;
									return true;
								}
								if (r.nextChar == '=')
								{ // ">>=: shift right with assignment
									r.readChar();
									l2aSym.type = S_ASNBSH;
									l2aSym.par = B_SHRA;
									return true;
								}
								// ">>": shift right
								l2aSym.type = S_BSH;
								l2aSym.par = B_SHRA;
								return true;
							}
							// ">": greater than
							l2aSym.type = S_CMP;
							l2aSym.par = C_GT;
							return true;
						case '<':
							r.readChar();
							if (r.nextChar == '=')
							{ // "<=": lower than or equal to
								r.readChar();
								l2aSym.type = S_CMP;
								l2aSym.par = C_LE;
								return true;
							}
							if (r.nextChar == '<')
							{ // "<<" so far
								r.readChar();
								if (r.nextChar == '=')
								{ // "<<=": shift left with assignment
									r.readChar();
									l2aSym.type = S_ASNBSH;
									l2aSym.par = B_SHL;
									return true;
								}
								// "<<": shift left
								l2aSym.type = S_BSH;
								l2aSym.par = B_SHL;
								return true;
							}
							// "<": lower than
							l2aSym.type = S_CMP;
							l2aSym.par = C_LW;
							return true;
					}
					break;
			}
			//not done yet
			switch (r.nextChar)
			{
				case '{':
					r.readChar();
					l2aSym.type = S_ENC;
					l2aSym.par = E_BO;
					return true;
				case '}':
					r.readChar();
					l2aSym.type = S_ENC;
					l2aSym.par = E_BC;
					return true;
				case '[':
					r.readChar();
					l2aSym.type = S_ENC;
					l2aSym.par = E_SO;
					return true;
				case ']':
					r.readChar();
					l2aSym.type = S_ENC;
					l2aSym.par = E_SC;
					return true;
				case '^':
					r.readChar();
					if (r.nextChar == '=')
					{ // "^=": xor + assign
						r.readChar();
						l2aSym.type = S_ASNARI;
						l2aSym.par = A_XOR;
						return true;
					}
					l2aSym.type = S_ARI;
					l2aSym.par = A_XOR;
					return true;
				case '~':
					r.readChar();
					l2aSym.type = S_ARI;
					l2aSym.par = A_CPL;
					return true;
				case '|':
					r.readChar();
					if (r.nextChar == '|')
					{ // "||": logical or
						r.readChar();
						l2aSym.type = S_LOG;
						l2aSym.par = L_OR;
						return true;
					}
					if (r.nextChar == '=')
					{ // "|=": arithmetic or + assign
						r.readChar();
						l2aSym.type = S_ASNARI;
						l2aSym.par = A_OR;
						return true;
					}
					// "|": arithmetic bitwise or
					l2aSym.type = S_ARI;
					l2aSym.par = A_OR;
					return true;
				case '@':
					r.readChar();
					l2aSym.type = S_MOD;
					l2aSym.par = M_ANNO;
					return true;
			}
			//invalid character in input
			scannerError("invalid character in input");
			return false;
		}
		//no valid next symbol
		l2aSym.type = S_EOF;
		l2aSym.par = RES;
		return false;
	}
	
	private void scannerError(String msg)
	{
		ctx.printPos(curFID, nxtSym.syline, nxtSym.sycol);
		v.print(": scanner-error: ");
		v.println(msg);
		l2aSym.type = S_ERR;
	}
	
	/*
  Char  Dec | Char  Dec | Char Dec
  --------------------------------
  (sp)   32 | @      64 | `     96
  !      33 | A      65 | a     97
  "      34 | B      66 | b     98
  #      35 | C      67 | c     99
  $      36 | D      68 | d    100
  %      37 | E      69 | e    101
  &      38 | F      70 | f    102
  '      39 | G      71 | g    103
  (      40 | H      72 | h    104
  )      41 | I      73 | i    105
  *      42 | J      74 | j    106
  +      43 | K      75 | k    107
  ,      44 | L      76 | l    108
  -      45 | M      77 | m    109
  .      46 | N      78 | n    110
  /      47 | O      79 | o    111
  0      48 | P      80 | p    112
  1      49 | Q      81 | q    113
  2      50 | R      82 | r    114
  3      51 | S      83 | s    115
  4      52 | T      84 | t    116
  5      53 | U      85 | u    117
  6      54 | V      86 | v    118
  7      55 | W      87 | w    119
  8      56 | X      88 | x    120
  9      57 | Y      89 | y    121
  :      58 | Z      90 | z    122
  ;      59 | [      91 | {    123
  <      60 | \      92 | |    124
  =      61 | ]      93 | }    125
  >      62 | ^      94 | ~    126
  ?      63 | _      95 | (dl) 127
	private final static int CCT_OTHER    = 0;
	private final static int CCT_CTRL     = 1; //' ' and below
	private final static int CCT_ALPHAEXT = 2; //'A'..'Z', 'a'..'z', '_', '$'
	private final static int CCT_BASEOP1  = 3; //'!', '"', '#', '%', '&', '''
	private final static int CCT_BASEOP2  = 4; //'(', ')', '*', '+', ',', '-', '.', '/'
	private final static int CCT_BASEOP3  = 5; //':', ';', '<', '=', '>', '?'
	 */
	private int getCharCat(char c)
	{
		if ((c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || c == '_' || c == '$')
			return CCT_ALPHAEXT;
		if (c <= ' ')
			return CCT_CTRL;
		if (c <= '/')
		{
			if (c <= '\'')
				return CCT_BASEOP1;
			return CCT_BASEOP2;
		}
		if (c >= ':')
			return CCT_BASEOP3;
		return CCT_OTHER;
	}
	
	private boolean isAlphaExt(char c)
	{
		if (c >= 'a' && c <= 'z')
			return true;
		if (c >= 'A' && c <= 'Z')
			return true;
		return c == '_' || c == '$';
	}
	
	private boolean isNum(char c)
	{
		return c >= '0' && c <= '9';
	}
	
	private boolean isAlphaNum(char c)
	{
		if (c >= '0' && c <= '9')
			return true;
		if (c >= 'a' && c <= 'z')
			return true;
		return c >= 'A' && c <= 'Z';
	}
	
	private boolean checkKeyword(char[] buf, int len)
	{
		switch (len)
		{
			case 2:
				if (equalTo(buf, "if"))
				{
					l2aSym.type = S_FLC;
					l2aSym.par = F_IF;
					return true;
				}
				if (equalTo(buf, "do"))
				{
					l2aSym.type = S_FLC;
					l2aSym.par = F_DO;
					return true;
				}
				break;
			case 3:
				if (equalTo(buf, "int"))
				{
					l2aSym.type = S_TYP;
					l2aSym.par = T_INT;
					return true;
				}
				if (equalTo(buf, "new"))
				{
					l2aSym.type = S_OKE;
					l2aSym.par = O_NEW;
					return true;
				}
				if (equalTo(buf, "for"))
				{
					l2aSym.type = S_FLC;
					l2aSym.par = F_FOR;
					return true;
				}
				if (equalTo(buf, "try"))
				{
					l2aSym.type = S_FLC;
					l2aSym.par = F_TRY;
					return true;
				}
				break;
			case 4:
				if (equalTo(buf, "else"))
				{
					l2aSym.type = S_FLC;
					l2aSym.par = F_ELSE;
					return true;
				}
				if (equalTo(buf, "null"))
				{
					l2aSym.type = S_NUM;
					l2aSym.par = T_NULL;
					return true;
				}
				if (equalTo(buf, "true"))
				{
					l2aSym.type = S_NUM;
					l2aSym.par = T_BOOL;
					l2aSym.intBuf = 1;
					return true;
				}
				if (equalTo(buf, "byte"))
				{
					l2aSym.type = S_TYP;
					l2aSym.par = T_BYTE;
					return true;
				}
				if (equalTo(buf, "long"))
				{
					l2aSym.type = S_TYP;
					l2aSym.par = T_LONG;
					return true;
				}
				if (equalTo(buf, "char"))
				{
					l2aSym.type = S_TYP;
					l2aSym.par = T_CHAR;
					return true;
				}
				if (equalTo(buf, "case"))
				{
					l2aSym.type = S_FLC;
					l2aSym.par = F_CASE;
					return true;
				}
				if (equalTo(buf, "void"))
				{
					l2aSym.type = S_OKE;
					l2aSym.par = O_VOID;
					return true;
				}
				break;
			case 5:
				if (equalTo(buf, "false"))
				{
					l2aSym.type = S_NUM;
					l2aSym.par = T_BOOL;
					l2aSym.intBuf = 0;
					return true;
				}
				if (equalTo(buf, "while"))
				{
					l2aSym.type = S_FLC;
					l2aSym.par = F_WHILE;
					return true;
				}
				if (equalTo(buf, "short"))
				{
					l2aSym.type = S_TYP;
					l2aSym.par = T_SHRT;
					return true;
				}
				if (equalTo(buf, "final"))
				{
					l2aSym.type = S_MOD;
					l2aSym.par = M_FIN;
					return true;
				}
				if (equalTo(buf, "break"))
				{
					l2aSym.type = S_FLC;
					l2aSym.par = F_BRK;
					return true;
				}
				if (equalTo(buf, "class"))
				{
					l2aSym.type = S_OKE;
					l2aSym.par = O_CLSS;
					return true;
				}
				if (equalTo(buf, "float"))
				{
					l2aSym.type = S_TYP;
					l2aSym.par = T_FLT;
					return true;
				}
				if (equalTo(buf, "catch"))
				{
					l2aSym.type = S_FLC;
					l2aSym.par = F_CATCH;
					return true;
				}
				if (equalTo(buf, "throw"))
				{
					l2aSym.type = S_FLC;
					l2aSym.par = F_THROW;
					return true;
				}
				break;
			case 6:
				if (equalTo(buf, "public"))
				{
					l2aSym.type = S_MOD;
					l2aSym.par = M_PUB;
					return true;
				}
				if (equalTo(buf, "static"))
				{
					l2aSym.type = S_MOD;
					l2aSym.par = M_STAT;
					return true;
				}
				if (equalTo(buf, "native"))
				{
					l2aSym.type = S_MOD;
					l2aSym.par = M_NAT;
					return true;
				}
				if (equalTo(buf, "switch"))
				{
					l2aSym.type = S_FLC;
					l2aSym.par = F_SWTCH;
					return true;
				}
				if (equalTo(buf, "return"))
				{
					l2aSym.type = S_FLC;
					l2aSym.par = F_RET;
					return true;
				}
				if (equalTo(buf, "import"))
				{
					l2aSym.type = S_OKE;
					l2aSym.par = O_IMPT;
					return true;
				}
				if (equalTo(buf, "double"))
				{
					l2aSym.type = S_TYP;
					l2aSym.par = T_DBL;
					return true;
				}
				if (equalTo(buf, "throws"))
				{
					l2aSym.type = S_FLC;
					l2aSym.par = F_THRWS;
					return true;
				}
				if (equalTo(buf, "assert"))
				{
					l2aSym.type = S_FLC;
					l2aSym.par = F_ASSRT;
					return true;
				}
				break;
			case 7:
				if (equalTo(buf, "private"))
				{
					l2aSym.type = S_MOD;
					l2aSym.par = M_PRIV;
					return true;
				}
				if (equalTo(buf, "boolean"))
				{
					l2aSym.type = S_TYP;
					l2aSym.par = T_BOOL;
					return true;
				}
				if (equalTo(buf, "default"))
				{
					l2aSym.type = S_FLC;
					l2aSym.par = F_DFLT;
					return true;
				}
				if (equalTo(buf, "package"))
				{
					l2aSym.type = S_OKE;
					l2aSym.par = O_PACK;
					return true;
				}
				if (equalTo(buf, "extends"))
				{
					l2aSym.type = S_OKE;
					l2aSym.par = O_EXTS;
					return true;
				}
				if (equalTo(buf, "finally"))
				{
					l2aSym.type = S_FLC;
					l2aSym.par = F_FIN;
					return true;
				}
				break;
			case 8:
				if (equalTo(buf, "abstract"))
				{
					l2aSym.type = S_MOD;
					l2aSym.par = M_ABSTR;
					return true;
				}
				if (equalTo(buf, "continue"))
				{
					l2aSym.type = S_FLC;
					l2aSym.par = F_CNT;
					return true;
				}
				if (equalTo(buf, "volatile"))
				{
					l2aSym.type = S_MOD;
					l2aSym.par = M_VOLAT;
					return true;
				}
				break;
			case 9:
				if (equalTo(buf, "protected"))
				{
					l2aSym.type = S_MOD;
					l2aSym.par = M_PROT;
					return true;
				}
				if (equalTo(buf, "interface"))
				{
					l2aSym.type = S_OKE;
					l2aSym.par = O_INTF;
					return true;
				}
				if (equalTo(buf, "transient"))
				{
					l2aSym.type = S_MOD;
					l2aSym.par = M_TRANS;
					return true;
				}
				break;
			case 10:
				if (equalTo(buf, "instanceof"))
				{
					l2aSym.type = S_CMP;
					l2aSym.par = C_INOF;
					return true;
				}
				if (equalTo(buf, "implements"))
				{
					l2aSym.type = S_OKE;
					l2aSym.par = O_IMPL;
					return true;
				}
				break;
			case 12:
				if (equalTo(buf, "synchronized"))
				{
					l2aSym.type = S_MOD;
					l2aSym.par = M_SYNC;
					return true;
				}
				break;
		}
		return false;
	}
	
	private int getNumericExt(char c, int base)
	{ //valid values for base: 10..26
		if (c >= '0' && c <= '9')
			return c - '0';
		if (c >= 'A' && c <= 'A' + (char) (base - 10))
			return (c - 'A') + 10;
		if (c >= 'a' && c <= 'a' + (char) (base - 10))
			return (c - 'a') + 10;
		return -1;
	}
	
	private boolean readNumValue(int base)
	{
		boolean didRead = false, expNeg = false, useFloat = false;
		int nv, exp = 0, expTmp = 0;
		long ov = 0L, buf = 0L, lb = base;
		
		l2aSym.type = S_NUM;
		if (r.nextChar != '.')
		{
			while (r.nextChar != '\0' && (nv = getNumericExt(r.nextChar, base)) >= 0 && nv < base)
			{
				buf = buf * lb + (long) nv;
				if ((buf ^ 0x8000000000000000l) < (ov ^ 0x8000000000000000l))
				{
					scannerError("constant value too big for long-buffer");
					return false;
				}
				ov = buf;
				r.readChar();
				didRead = true;
			}
			if (!didRead)
			{
				scannerError("invalid character for value");
				return false;
			}
			//check for integer-types
			switch (r.nextChar)
			{
				case 'l':
				case 'L': //type to long
					r.readChar();
					l2aSym.par = T_LONG;
					l2aSym.longBuf = buf;
					return true;
				case 'f':
				case 'F': //convert to float, handled below
				case 'd':
				case 'D': //convert to double, handled below
				case 'e':
				case 'E':
				case '.': //floating-point, handled below
					break;
				default:
					if (isAlphaExt(r.nextChar))
					{ //invalid constant
						scannerError("invalid constant value");
						return false;
					}
					//default type is int, check range before conversion
					if ((base == 10 && (buf & 0xFFFFFFFF80000000l) != 0l && buf != 0x0000000080000000l) || (buf & 0xFFFFFFFF00000000l) != 0l)
					{
						scannerError("constant too big for int");
						return false;
					}
					l2aSym.par = T_INT;
					l2aSym.intBuf = (int) buf;
					return true;
			}
		}
		//continue with digits after dot if existing
		if (r.nextChar == '.')
		{
			if (base != 10)
			{ //float and double are always base 10, but may be we already read too much of them in another base
				if (buf >= 8l || buf < 0l)
				{ //used invalid base, which is not supported by SJC
					scannerError("floating-point-values need base 10");
					return false;
				}
				base = 10; //ok, did not loose anything so far, read on with base 10
			}
			r.readChar();
			nv = getNumericExt(r.nextChar, base);
			while (r.nextChar != '\0' && nv >= 0)
			{
				if ((buf >>> 4) > 0xCCCCCCCCCCCCCCl)
				{
					scannerError("constant value too big for long-buffer");
					return false;
				}
				buf = buf * 10l + (long) nv;
				exp--;
				r.readChar();
				nv = getNumericExt(r.nextChar, base);
			}
		}
		//continue with exponent if existing
		if (r.nextChar == 'e')
		{
			r.readChar();
			switch (r.nextChar)
			{
				case '-':
					expNeg = true; //no break
				case '+':
					r.readChar(); //accept both - and +
			}
			nv = getNumericExt(r.nextChar, base);
			while (r.nextChar != '\0' && nv >= 0)
			{
				if ((expTmp = expTmp * 10 + nv) > 330)
				{ //double is -308..308, add precision of double for safety
					scannerError("exponent too big");
					return false;
				}
				r.readChar();
				nv = getNumericExt(r.nextChar, base);
			}
			if (expNeg)
				exp -= expTmp;
			else
				exp += expTmp;
		}
		//check trailing character
		switch (r.nextChar)
		{
			case 'f':
			case 'F':
				useFloat = true; //no break
			case 'd':
			case 'D':
				r.readChar(); //accept all f, F, d, D
		}
		if (isAlphaExt(r.nextChar))
		{ //invalid constant
			scannerError("invalid constant value");
			return false;
		}
		//everything ok, set up floating point number
		if (useFloat)
		{
			l2aSym.par = T_FLT;
			l2aSym.intBuf = ctx.arch.real.buildFloat(buf, exp);
			return true;
		}
		l2aSym.par = T_DBL;
		l2aSym.longBuf = ctx.arch.real.buildDouble(buf, exp);
		return true;
	}
	
	private char readChValue()
	{
		int iv;
		char cv;
		
		cv = r.nextChar;
		if (cv < '\u0020')
		{
			scannerError("control-character inside symbol");
			return '#';
		}
		if (r.nextChar != '\\')
		{
			r.readChar();
			return cv;
		}
		///escape-sequence
		r.readChar();
		switch (r.nextChar)
		{
			case '"':
				r.readChar();
				return '"';
			case 'b':
				r.readChar();
				return '\b';
			case 'n':
				r.readChar();
				return '\n';
			case 'r':
				r.readChar();
				return '\r';
			case 't':
				r.readChar();
				return '\t';
			case '0':
				r.readChar();
				return '\0';
			case '\\':
				r.readChar();
				return '\\';
			case '\'':
				r.readChar();
				return '\'';
			case 'u':
				r.readChar();
				iv = getNumericExt(r.nextChar, 16) << 12;
				r.readChar();
				iv = iv | getNumericExt(r.nextChar, 16) << 8;
				r.readChar();
				iv = iv | getNumericExt(r.nextChar, 16) << 4;
				r.readChar();
				iv = iv | getNumericExt(r.nextChar, 16);
				r.readChar();
				if (iv == -1)
				{
					scannerError("invalid unicode-escape-sequence");
					return '#';
				}
				return (char) iv;
		}
		scannerError("unknown escape-sequence in character");
		return '#';
	}
	
	private boolean equalTo(char[] buf, String to)
	{
		int i;
		
		//length checked already, else: if (to.length()!=len) return false; //not the same length
		for (i = to.length() - 1; i >= 0; i--)
			if (buf[i] != to.charAt(i))
				return false; //different character
		return true;
	}
}
