/* Copyright (C) 2012, 2014, 2015 Stefan Frenz
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

package sjc.debug;

import sjc.compbase.*;
import sjc.osio.TextPrinter;

/**
 * CodePrinterJava: debug writer that outputs Java source code corresponding to the (somewhat modified) parse tree
 *
 * @author S. Frenz
 * @version 151026 fixed explicit type conversion
 * version 140507 added support for MM_REFTOFLASH
 * version 121113 inserted reset of nextExprIsLeftMost in exprEnc to avoid to many enclosures
 * version 121031 one char string printing changed to char printing, fixed logic shift right
 * version 121029 fixed MAGIC class-/method-name, reduced unneccessary enclosures, fixed MAGIC.mthdOff
 * version 121021 fixed binop with assignment, fixed static inner class modifier, added offset/count fot struct vars
 * version 121020 beautified code, fixed initialized arrays, unified naming
 * version 121017 added support for labels, fixed new for array types and inner classes
 * version 121016 added support for try-catch-finally
 * version 121014 added support for static vars
 * version 120925 added support for expressions
 * version 120923 initial version
 */

public class CodePrinterJava extends DebugWriter
{
	protected TextPrinter finalOut;
	private final Context ctx;
	private final JavaStmtExprPrinter codePrnt;
	private int indent;
	private boolean skipNextIndent, skipUnit, nextExprIsTopLevelVarInit, nextExprIsBaseTypeArrayInit;
	private Unit nextInner;
	private Mthd curMthd;
	private boolean alreadyShowedNoPOB;
	
	private class JavaStmtExprPrinter extends CodePrinter
	{
		public void reportError(Token token, String error)
		{
			ctx.out.print("code printer error");
			if (token != null)
			{
				ctx.out.print(" at ");
				token.printPos(ctx, error);
				ctx.out.println();
			}
			else
			{
				ctx.out.print(": ");
				ctx.out.println(error);
			}
		}
		
		private boolean skipNextStmtLineFeed, skipNextStmtSemicolon, skipNextBlockBrackets;
		private boolean nextExprIsLeftMost, nextCallIsSuperThisCall, skipNextBlockLabels;
		
		public void stmtAssert(Expr cond, Expr msg)
		{
			spaceIndent();
			finalOut.print("assert ");
			cond.printExpression(this);
			finalOut.print(" : ");
			msg.printExpression(this);
			finishStmt();
		}
		
		public void blockLabel(String name)
		{
			if (skipNextBlockLabels)
				return;
			finalOut.print(name);
			finalOut.print(":\n");
		}
		
		public boolean stmtBlockStart()
		{ //returns "report block end"
			skipNextBlockLabels = false;
			if (skipNextBlockBrackets)
			{
				skipNextBlockBrackets = false;
				return false;
			}
			spaceIndent();
			finalOut.println('{');
			indent++;
			return true;
		}
		
		public void stmtBlockEnd()
		{
			indent--;
			spaceIndent();
			finalOut.println('}');
		}
		
		public void stmtEmpty()
		{
			finishStmt();
		}
		
		public void stmtEndLoop(boolean contNotBreak, String labelToEnd)
		{
			spaceIndent();
			finalOut.print(contNotBreak ? "continue" : "break");
			if (labelToEnd != null)
			{
				finalOut.print(' ');
				finalOut.print(labelToEnd);
			}
			finishStmt();
		}
		
		public void stmtExpr(Expr ex)
		{
			spaceIndent();
			if (ex.isSuperThisCall(ctx))
				nextCallIsSuperThisCall = true;
			ex.printExpression(this);
			finishStmt();
		}
		
		public void stmtFor(TokenAbstrPrintable init, TokenAbstrPrintable[] furtherInit, TokenAbstrPrintable lupd, TokenAbstrPrintable[] furtherLupd, Expr cond, TokenAbstrPrintable loStmt)
		{
			spaceIndent();
			finalOut.print("for (");
			if (init != null)
			{
				skipNextIndent = skipNextStmtLineFeed = skipNextStmtSemicolon = true;
				init.printToken(this);
				if (furtherInit != null)
					for (TokenAbstrPrintable p : furtherInit)
					{
						finalOut.print(", ");
						skipNextIndent = skipNextStmtLineFeed = skipNextStmtSemicolon = true;
						p.printToken(this);
					}
			}
			finalOut.print(';');
			finalOut.print(' ');
			if (cond != null)
				cond.printExpression(this);
			finalOut.print("; ");
			if (lupd != null)
			{
				skipNextIndent = skipNextStmtLineFeed = skipNextStmtSemicolon = true;
				lupd.printToken(this);
				if (furtherLupd != null)
					for (TokenAbstrPrintable p : furtherLupd)
					{
						finalOut.print(", ");
						skipNextIndent = skipNextStmtLineFeed = skipNextStmtSemicolon = true;
						p.printToken(this);
					}
			}
			finalOut.print(") ");
			skipNextIndent = true;
			loStmt.printToken(this);
		}
		
		public void stmtForEnh(TokenAbstrPrintable var, Expr iter, TokenAbstrPrintable loStmt)
		{
			spaceIndent();
			finalOut.print("for (");
			skipNextIndent = skipNextStmtLineFeed = skipNextStmtSemicolon = true;
			var.printToken(this);
			finalOut.print(": ");
			iter.printExpression(this);
			finalOut.print(") ");
			skipNextIndent = true;
			loStmt.printToken(this);
		}
		
		public void stmtIf(Expr cond, TokenAbstrPrintable trStmt, TokenAbstrPrintable faStmt)
		{
			spaceIndent();
			finalOut.print("if (");
			cond.printExpression(this);
			finalOut.print(") ");
			skipNextIndent = true;
			trStmt.printToken(this);
			if (faStmt != null)
			{
				spaceIndent();
				finalOut.print("else ");
				skipNextIndent = true;
				faStmt.printToken(this);
			}
		}
		
		public void stmtReturn(Expr retVal)
		{
			spaceIndent();
			finalOut.print("return");
			if (retVal != null)
			{
				finalOut.print(' ');
				retVal.printExpression(this);
			}
			finishStmt();
		}
		
		public void stmtReturnMissing()
		{
			//does not have an equivalent in java, just skip
		}
		
		public void stmtSwitchStart(Expr cond)
		{
			spaceIndent();
			finalOut.print("switch (");
			cond.printExpression(this);
			finalOut.println(") {");
			indent += 2;
		}
		
		public void stmtSwitchCase(Expr cond)
		{
			indent--;
			spaceIndent();
			if (cond == null)
				finalOut.print("default");
			else
			{
				finalOut.print("case ");
				cond.printExpression(this);
			}
			finalOut.println(':');
			indent++;
		}
		
		public void stmtSwitchEnd()
		{
			indent -= 2;
			spaceIndent();
			finalOut.println('}');
		}
		
		public void stmtSync(Expr syncObj, TokenAbstrPrintable syncBlock)
		{
			if (syncObj != null)
			{ //normal sync block
				spaceIndent();
				finalOut.print("synchronized (");
				syncObj.printExpression(this);
				finalOut.print(')');
				skipNextIndent = true;
			}
			else
				skipNextBlockBrackets = true; //complete method block in synchronized
			syncBlock.printToken(this);
		}
		
		public void stmtThrow(Expr throwVal)
		{
			spaceIndent();
			finalOut.print("throw ");
			throwVal.printExpression(this);
			finishStmt();
		}
		
		public void stmtTryStart(TokenAbstrPrintable tryBlock)
		{
			spaceIndent();
			finalOut.print("try ");
			skipNextIndent = true;
			tryBlock.printToken(this);
		}
		
		public void stmtTryCatch(Vrbl catchVar, TokenAbstrPrintable catchBlock)
		{
			spaceIndent();
			finalOut.print("catch (");
			printType(catchVar.type, true);
			finalOut.print(' ');
			finalOut.print(catchVar.name);
			finalOut.print(") ");
			skipNextIndent = true;
			catchBlock.printToken(this);
		}
		
		public void stmtTryFinally(TokenAbstrPrintable finallyBlock)
		{
			spaceIndent();
			finalOut.print("finally ");
			skipNextIndent = skipNextBlockLabels = true;
			finallyBlock.printToken(this);
		}
		
		public void stmtVrbl(Vrbl varList, int varCount)
		{
			Vrbl vl;
			spaceIndent();
			printType(varList.type, true);
			finalOut.print(' ');
			vl = varList;
			while (true)
			{
				finalOut.print(vl.name);
				if (vl.init != null)
				{
					finalOut.print(" = ");
					nextExprIsTopLevelVarInit = true;
					if (vl.type.qid == null)
						nextExprIsBaseTypeArrayInit = true;
					vl.init.printExpression(this);
					nextExprIsBaseTypeArrayInit = nextExprIsTopLevelVarInit = false;
				}
				if (--varCount <= 0)
					break;
				finalOut.print(", ");
				vl = vl.nextVrbl;
			}
			finishStmt();
		}
		
		public void stmtWhile(Expr cond, boolean inclusiveWhile, TokenAbstrPrintable loStmt)
		{
			spaceIndent();
			if (inclusiveWhile)
			{
				finalOut.print("do {");
				indent++;
				loStmt.printToken(this);
				indent--;
				spaceIndent();
				finalOut.print("} while (");
				cond.printExpression(this);
				finalOut.println(");");
			}
			else
			{
				finalOut.print("while (");
				cond.printExpression(this);
				finalOut.print(") ");
				skipNextIndent = true;
				loStmt.printToken(this);
			}
		}
		
		public void exprArrayInit(TypeRef type, FilledParam par)
		{
			if (!nextExprIsTopLevelVarInit && !nextExprIsBaseTypeArrayInit)
			{
				finalOut.print("new ");
				printType(type, true);
			}
			nextExprIsTopLevelVarInit = false;
			finalOut.print('{');
			if (par != null)
				while (true)
				{
					par.expr.printExpression(this);
					if (par.nextParam == null)
						break;
					finalOut.print(", ");
					par = par.nextParam;
				}
			finalOut.print('}');
		}
		
		public void exprBin(Expr le, Expr ri, int opType, int opPar, int rank)
		{
			le.printExpression(this);
			finalOut.print(" ");
			switch (opPar)
			{
				case Ops.A_AND:
					finalOut.print('&');
					break;
				case Ops.A_DIV:
					finalOut.print('/');
					break;
				case Ops.A_MINUS:
					finalOut.print('-');
					break;
				case Ops.A_MOD:
					finalOut.print('%');
					break;
				case Ops.A_MUL:
					finalOut.print('*');
					break;
				case Ops.A_OR:
					finalOut.print('|');
					break;
				case Ops.A_PLUS:
					finalOut.print('+');
					break;
				case Ops.A_XOR:
					finalOut.print('^');
					break;
				case Ops.B_SHL:
					finalOut.print("<<");
					break;
				case Ops.B_SHRA:
					finalOut.print(">>");
					break;
				case Ops.B_SHRL:
					finalOut.print(">>>");
					break;
				case Ops.C_EQ:
					finalOut.print("==");
					break;
				case Ops.C_GE:
					finalOut.print(">=");
					break;
				case Ops.C_GT:
					finalOut.print('>');
					break;
				case Ops.C_INOF:
					finalOut.print("instanceof");
					break;
				case Ops.C_LE:
					finalOut.print("<=");
					break;
				case Ops.C_LW:
					finalOut.print('<');
					break;
				case Ops.C_NE:
					finalOut.print("!=");
					break;
				case Ops.L_AND:
					finalOut.print("&&");
					break;
				case Ops.L_OR:
					finalOut.print("||");
					break;
				default:
					if (opType != Ops.S_ASN)
					{
						reportError(le, "unknown binOp");
						return;
					}
			}
			if (opType == Ops.S_ASN || opType == Ops.S_ASNARI || opType == Ops.S_ASNBSH)
				finalOut.print("= ");
			else
				finalOut.print(' ');
			ri.printExpression(this);
		}
		
		public void exprCall(Mthd dest, FilledParam par)
		{
			if (dest.owner == null)
			{
				reportError(dest, "owner of dest is null");
				return;
			}
			if (nextCallIsSuperThisCall)
			{
				nextCallIsSuperThisCall = false;
				if (curMthd.owner == dest.owner)
					finalOut.print("this");
				else if (dest.owner.isParent(curMthd.owner, ctx))
					finalOut.print("super");
				else
					reportError(curMthd, "unknown type hierarchy");
			}
			else
			{
				if ((dest.modifier & Modifier.M_STAT) != 0 || dest.name == null)
				{
					if (dest.owner.pack != null)
					{
						dest.owner.pack.printFullQID(finalOut);
						finalOut.print('.');
					}
					dest.owner.printNameWithOuter(finalOut);
					if (dest.name != null)
						finalOut.print('.');
				}
				if (dest.name != null)
					finalOut.print(dest.name);
			}
			finalOut.print('(');
			if (par != null)
				while (true)
				{
					par.expr.printExpression(this);
					if (par.nextParam == null)
						break;
					finalOut.print(", ");
					par = par.nextParam;
				}
			finalOut.print(')');
		}
		
		public void exprChoose(Expr le, Expr ce, Expr ri)
		{
			le.printExpression(this);
			finalOut.print(" ? ");
			ce.printExpression(this);
			finalOut.print(" : ");
			ri.printExpression(this);
		}
		
		public void exprClssName(TypeRef destType)
		{
			printType(destType, true);
		}
		
		public void exprConstStruct(ExConstStruct constStruct)
		{
			finalOut.print('(');
			printType(constStruct, true);
			finalOut.print(")MAGIC.cast2Struct(0x");
			finalOut.printHexFix(ctx.mem.getAddrAsInt(constStruct.outputLocation, 0), 8);
			finalOut.print(')');
		}
		
		public void exprDeArray(Expr le, Expr ind)
		{
			le.printExpression(this);
			finalOut.print('[');
			ind.printExpression(this);
			finalOut.print(']');
		}
		
		public void exprDeref(Expr le, Expr ri, boolean leftStatic)
		{
			if (!leftStatic)
			{
				nextExprIsLeftMost = true;
				le.printExpression(this);
				nextExprIsLeftMost = false;
				finalOut.print('.');
			}
			ri.printExpression(this);
		}
		
		public void exprEnc(TypeRef convertTo, Expr ex)
		{
			if (convertTo != null)
			{
				finalOut.print('(');
				printType(convertTo, true);
				finalOut.print(')');
				ex.printExpression(this);
			}
			else
			{
				nextExprIsLeftMost = false;
				finalOut.print('(');
				ex.printExpression(this);
				finalOut.print(')');
			}
		}
		
		public void exprNew(TypeRef type, boolean asArray, boolean multArray, boolean callExplicitConstr, Unit destTypeUnit, Mthd dest, FilledParam par)
		{
			int dim;
			if (nextExprIsLeftMost)
				finalOut.print('(');
			finalOut.print("new ");
			if (!asArray)
			{
				printType(type, false);
				finalOut.print('(');
				if (par != null)
					while (true)
					{
						par.expr.printExpression(this);
						if (par.nextParam == null)
							break;
						finalOut.print(", ");
						par = par.nextParam;
					}
				finalOut.print(')');
			}
			else
			{
				printType(type, false);
				dim = type.arrDim;
				while (dim > 0)
				{
					finalOut.print('[');
					if (par != null)
					{
						par.expr.printExpression(this);
						par = par.nextParam;
					}
					finalOut.print(']');
					dim--;
				}
			}
			if (nextExprIsLeftMost)
				finalOut.print(')');
		}
		
		public void exprPrePst(Expr ex, int opPar, boolean pre)
		{
			if (pre)
				switch (opPar)
				{
					case Ops.P_DEC:
						finalOut.print("--");
						break;
					case Ops.P_INC:
						finalOut.print("++");
						break;
					default:
						reportError(ex, "unknown preOp");
						return;
				}
			ex.printExpression(this);
			if (!pre)
				switch (opPar)
				{
					case Ops.P_DEC:
						finalOut.print("--");
						break;
					case Ops.P_INC:
						finalOut.print("++");
						break;
					default:
						reportError(ex, "unknown pstOp");
						return;
				}
		}
		
		public void exprString(String value)
		{
			finalOut.print('\"');
			for (int i = 0; i < value.length(); i++)
				printTextChar(value.charAt(i));
			finalOut.print('\"');
		}
		
		public void exprSuper(Expr ri)
		{
			finalOut.print("super.");
			ri.printExpression(this);
		}
		
		public void exprUna(Expr ex, int opType, int opPar)
		{
			switch (opPar)
			{
				case Ops.A_PLUS:
					finalOut.print('+');
					break;
				case Ops.A_MINUS:
					finalOut.print('-');
					break;
				case Ops.A_CPL:
					finalOut.print('~');
					break;
				case Ops.L_NOT:
					finalOut.print('!');
					break;
				default:
					reportError(ex, "unknown unaOp");
					return;
			}
			ex.printExpression(this);
		}
		
		public void exprVal(TypeRef type, int intValue, long longValue)
		{
			switch (type.baseType)
			{
				case StdTypes.T_BOOL:
					finalOut.print(intValue != 0 ? "true" : "false");
					break;
				case StdTypes.T_CHAR:
					finalOut.print('\'');
					printTextChar(intValue);
					finalOut.print('\'');
					break;
				case StdTypes.T_BYTE:
					finalOut.print("(byte)0x");
					finalOut.printHexFix(intValue, 2);
					break;
				case StdTypes.T_SHRT:
					finalOut.print("(short)0x");
					finalOut.printHexFix(intValue, 4);
					break;
				case StdTypes.T_INT:
					if (intValue > -1000 && intValue < 1000)
						finalOut.print(intValue);
					else
					{
						finalOut.print("0x");
						finalOut.printHexFix(intValue, 8);
					}
					break;
				case StdTypes.T_LONG:
					finalOut.print(longValue);
					finalOut.print('l');
					break;
				case StdTypes.T_NULL:
				case StdTypes.T_NNPT:
				case StdTypes.T_NDPT:
					finalOut.print("null");
					break;
				default:
					reportError(type, "not yet supported type for exprVal");
					return;
			}
		}
		
		public void exprVar(AccVar dest, String id, boolean isThis)
		{
			if (isThis)
				finalOut.print("this");
			else
			{
				if (dest.owner != null)
				{
					if ((dest.modifier & Modifier.M_STAT) != 0 || dest.name == null)
					{
						printUnitName(dest.owner, true);
						finalOut.print('.');
					}
					finalOut.print(dest.name != null ? dest.name : id);
				}
				else
					finalOut.print(id);
			}
		}
		
		public void magcVar(AccVar dest)
		{
			finalOut.print("MAGIC.");
			finalOut.print(dest.name);
		}
		
		public void magcCall(String id, FilledParam par)
		{
			finalOut.print("MAGIC.");
			finalOut.print(id);
			finalOut.print('(');
			if (par != null)
				while (true)
				{
					par.expr.printExpression(this);
					if (par.nextParam == null)
						break;
					finalOut.print(", ");
					par = par.nextParam;
				}
			finalOut.print(')');
		}
		
		public void magcClssMthdName(Unit unit, Mthd mthd)
		{
			if (mthd == null)
			{
				finalOut.print('"');
				finalOut.print(unit.name);
				finalOut.print('"');
			}
			else
			{
				finalOut.print('"');
				finalOut.print(mthd.owner.name);
				finalOut.print('"');
				finalOut.print(", ");
				finalOut.print('"');
				finalOut.print(mthd.name);
				finalOut.print('"');
			}
		}
		
		private void printTextChar(int value)
		{
			switch (value)
			{
				case '\b':
					finalOut.print("\\b");
					break;
				case '\n':
					finalOut.print("\\n");
					break;
				case '\r':
					finalOut.print("\\r");
					break;
				case '\t':
					finalOut.print("\\t");
					break;
				case '"':
					finalOut.print("\\\"");
					break;
				case '\'':
					finalOut.print("\\'");
					break;
				default:
					if (value >= 32 && value <= 126)
						finalOut.print((char) value);
					else
					{
						finalOut.print("\\u");
						finalOut.printHexFix(value, 4);
					}
			}
		}
		
		private void finishStmt()
		{
			if (!skipNextStmtSemicolon)
				finalOut.print(';');
			if (!skipNextStmtLineFeed)
				finalOut.println();
			skipNextIndent = skipNextStmtLineFeed = skipNextStmtSemicolon = false;
		}
	}
	
	public CodePrinterJava(String filename, Context ic)
	{
		if ((finalOut = ic.osio.getNewFilePrinter(filename)) == null)
		{
			ic.out.println("current system does not support writing to text files");
			finalOut = ic.out;
		}
		ctx = ic;
		codePrnt = new JavaStmtExprPrinter();
	}
	
	public void startImageInfo(boolean isDecompressor)
	{
	}
	
	public void finalizeImageInfo()
	{
		finalOut.close();
	}
	
	public void globalMemoryInfo(int baseAddress, int memBlockLen)
	{
	}
	
	public void globalMethodInfo(int mthdCodeSize, int mthdCount)
	{
	}
	
	public void globalStringInfo(int stringCount, int stringChars, int stringMemBytes)
	{
	}
	
	public void globalRAMInfo(Object ramInitLoc, int ramSize, int constMemorySize)
	{
	}
	
	public void globalSymbolInfo(int symGenSize)
	{
	}
	
	public void startUnit(String type, Unit unit)
	{
		QualIDList ql;
		Unit inner;
		
		if ((unit.marker & Marks.K_IGNU) != 0 || (unit.outerUnit != null && nextInner != unit))
		{ //inner units are handled in context of their outer unit, do not handle again
			skipUnit = true;
			return;
		}
		if ("pob".equals(unit.getSourceType()))
		{ //java does not support initialized objects without injection framework
			if (!alreadyShowedNoPOB)
				ctx.out.println("code printer warning: prepared objects not supported");
			alreadyShowedNoPOB = skipUnit = true;
			return;
		}
		if (unit.outerUnit == null && unit.pack != null)
		{
			finalOut.print("package ");
			unit.pack.printFullQID(finalOut);
			finalOut.println(";");
		}
		printMarker(unit.marker);
		if ((unit.modifier & Modifier.MM_FLASH) != 0)
		{
			spaceIndent();
			finalOut.println("@SJC.Flash");
		}
		if ((unit.modifier & Modifier.MM_REFTOFLASH) != 0)
		{
			spaceIndent();
			finalOut.println("@SJC.RefToFlash");
		}
		spaceIndent();
		printVisibilityModifier(unit.modifier);
		if ((unit.modifier & Modifier.M_FIN) != 0)
			finalOut.print("final ");
		if ((unit.modifier & Modifier.M_STAT) != 0 && unit.outerUnit != null)
			finalOut.print("static ");
		if ((unit.modifier & Modifier.M_INDIR) != 0)
			finalOut.print("interface ");
		else
		{
			if ((unit.modifier & Modifier.M_ABSTR) != 0)
				finalOut.print("abstract ");
			finalOut.print("class ");
		}
		finalOut.print(unit.name);
		if (unit.extsID != null && unit.extsID.unitDest != ctx.langRoot)
		{
			finalOut.print(" extends ");
			printUnitName(unit.extsID.unitDest, true);
		}
		if (unit.extsImplIDList != null)
		{
			finalOut.print((unit.modifier & Modifier.M_INDIR) != 0 ? " extends " : " implements ");
			ql = unit.extsImplIDList;
			while (true)
			{
				printUnitName(ql.qid.unitDest, true);
				if (ql.nextQualID == null)
					break;
				finalOut.print(", ");
				ql = ql.nextQualID;
			}
		}
		finalOut.println(" {");
		indent++;
		inner = unit.innerUnits;
		while (inner != null)
		{
			nextInner = inner;
			inner.writeDebug(ctx, this);
			inner = inner.nextUnit;
		}
	}
	
	private void printUnitName(Unit unit, boolean withPackage)
	{
		if (withPackage && unit.pack != null)
		{
			unit.pack.printFullQID(finalOut);
			finalOut.print('.');
		}
		printOuter(unit);
		if (unit.outerUnit != null)
			finalOut.print('.');
		finalOut.print(unit.name);
	}
	
	private void printOuter(Unit unit)
	{
		if (unit.outerUnit != null)
		{
			if (unit.outerUnit.outerUnit != null)
				printOuter(unit.outerUnit);
			finalOut.print(unit.outerUnit.name);
		}
	}
	
	private void printType(TypeRef type, boolean withArrayDim)
	{
		int i;
		
		if (type.baseType == TypeRef.T_VOID)
			finalOut.print("void");
		else if (type.qid != null)
			printUnitName(type.qid.unitDest, true);
		else
			StdTypes.printStdType(type.baseType, finalOut);
		if (withArrayDim)
			for (i = 0; i < type.arrDim; i++)
				finalOut.print("[]");
	}
	
	public void markUnitAsNotUsed()
	{
	}
	
	public void hasUnitOutputLocation(Object outputLocation)
	{
	}
	
	public void hasUnitFields(int clssRelocTableEntries, int clssScalarTableSize, int statRelocTableEntries, int statScalarTableSize, int instRelocTableEntries, int instScalarTableSize, int instIndirScalarTableSize)
	{
	}
	
	public void startVariableList()
	{
	}
	
	public void hasVariable(Vrbl var)
	{
		if (skipUnit)
			return;
		if (var.name.equals(Unit.OUTERVARNAME))
			return;
		if (var.owner.inlArr == var)
		{
			spaceIndent();
			finalOut.println("@SJC.InlineArrayVar");
		}
		else if ((var.modifier & Modifier.M_ARRLEN) != 0)
		{
			spaceIndent();
			finalOut.println("@SJC.InlineArrayCount");
		}
		else if ((var.owner.modifier & Modifier.M_STRUCT) != 0)
		{
			spaceIndent();
			finalOut.print("@SJC(offset=");
			finalOut.print(var.relOff);
			if (var.type.arrDim > 0 && var.type.typeSpecial != TypeRef.S_STRUCTARRNOTSPEC)
			{
				finalOut.print(",count=");
				if (var.type.typeSpecial == TypeRef.S_STRUCTARRDONTCHECK)
					finalOut.print('0');
				else
					finalOut.print(var.type.typeSpecial);
			}
			finalOut.print(")");
		}
		if ((var.modifier & Modifier.MM_FLASH) != 0)
		{
			spaceIndent();
			finalOut.println("@SJC.Flash");
		}
		if ((var.modifier & Modifier.MM_REFTOFLASH) != 0)
		{
			spaceIndent();
			finalOut.println("@SJC.RefToFlash");
		}
		spaceIndent();
		printVisibilityModifier(var.modifier);
		if ((var.modifier & Modifier.M_STAT) != 0)
			finalOut.print("static ");
		if ((var.modifier & Modifier.M_FIN) != 0)
			finalOut.print("final ");
		printType(var.type, true);
		finalOut.print(' ');
		finalOut.print(var.name);
		if (var.init != null)
		{
			finalOut.print(" = ");
			nextExprIsTopLevelVarInit = true;
			if (var.type.qid == null)
				nextExprIsBaseTypeArrayInit = true;
			var.init.printExpression(codePrnt);
			nextExprIsBaseTypeArrayInit = nextExprIsTopLevelVarInit = false;
		}
		finalOut.println(';');
	}
	
	public void endVariableList()
	{
	}
	
	public void startMethodList()
	{
	}
	
	public void hasMethod(Mthd mthd, boolean indir)
	{
		Param pl;
		
		if (skipUnit)
			return;
		printMarker(mthd.marker);
		spaceIndent();
		printVisibilityModifier(mthd.modifier);
		if (indir && (mthd.owner.modifier & Modifier.M_INDIR) == 0)
			finalOut.print("abstract ");
		else if ((mthd.modifier & Modifier.M_STAT) != 0)
			finalOut.print("static ");
		if ((mthd.modifier & Modifier.M_SYNC) != 0)
			finalOut.print("synchronized ");
		if (mthd.retType != null)
		{
			printType(mthd.retType, true);
			finalOut.print(' ');
		}
		finalOut.print(mthd.name);
		finalOut.print('(');
		if (mthd.param != null)
		{
			pl = mthd.param;
			while (true)
			{
				printType(pl.type, true);
				finalOut.print(' ');
				finalOut.print(pl.name);
				if (pl.nextParam == null)
					break;
				finalOut.print(", ");
				pl = pl.nextParam;
			}
		}
		finalOut.print(')');
		if (indir)
			finalOut.println(';');
		else
		{
			finalOut.print(' ');
			skipNextIndent = true;
			curMthd = mthd;
			mthd.printCode(codePrnt);
			curMthd = null;
		}
	}
	
	public void endMethodList()
	{
	}
	
	public void startStatObjList()
	{
	}
	
	public void hasStatObj(int rela, Object loc, String value, boolean inFlash)
	{
	}
	
	public void endStatObjList()
	{
	}
	
	public void startImportedUnitList()
	{
	}
	
	public void hasImportedUnit(UnitList ul)
	{
	}
	
	public void endImportedUnitList()
	{
	}
	
	public void startInterfaceMapList()
	{
	}
	
	public void hasInterfaceMap(IndirUnitMapList intf)
	{
	}
	
	public void endInterfaceMapList()
	{
	}
	
	public void endUnit()
	{
		if (skipUnit)
			skipUnit = false;
		else
		{
			indent--;
			spaceIndent();
			finalOut.println('}');
			finalOut.println();
		}
	}
	
	private void printMarker(int marker)
	{
		if ((marker & Marks.K_INTR) != 0)
		{
			spaceIndent();
			finalOut.println("@SJC.Interrupt");
		}
		if ((marker & Marks.K_DEBG) != 0)
		{
			spaceIndent();
			finalOut.println("@SJC.Debug");
		}
		if ((marker & Marks.K_FINL) != 0)
		{
			spaceIndent();
			finalOut.println("@SJC.Inline");
		}
		if ((marker & Marks.K_NINL) != 0)
		{
			spaceIndent();
			finalOut.println("@SJC.NoInline");
		}
		if ((marker & Marks.K_PROF) != 0)
		{
			spaceIndent();
			finalOut.println("@SJC.Profile");
		}
		if ((marker & Marks.K_NPRF) != 0)
		{
			spaceIndent();
			finalOut.println("@SJC.NoProfile");
		}
		if ((marker & Marks.K_SEPC) != 0)
		{
			spaceIndent();
			finalOut.println("@SJC.StackExtreme");
		}
		if ((marker & Marks.K_NSPC) != 0)
		{
			spaceIndent();
			finalOut.println("@SJC.NoStackExtreme");
		}
		if ((marker & Marks.K_PRCD) != 0)
		{
			spaceIndent();
			finalOut.println("@SJC.PrintCode");
		}
		if ((marker & Marks.K_NOPT) != 0)
		{
			spaceIndent();
			finalOut.println("@SJC.NoOptimization");
		}
		if ((marker & Marks.K_FCDG) != 0)
		{
			spaceIndent();
			finalOut.println("@SJC.GenCode");
		}
		if ((marker & Marks.K_SLHI) != 0)
		{
			spaceIndent();
			finalOut.println("@SJC.SourceLines");
		}
		if ((marker & Marks.K_EXPC) != 0)
		{
			spaceIndent();
			finalOut.println("@SJC.ExplicitConversion");
		}
		if ((marker & Marks.K_NWIN) != 0)
		{
			spaceIndent();
			finalOut.println("@SJC.WinDLL");
		}
		if ((marker & Marks.K_FOCD) != 0)
		{
			spaceIndent();
			finalOut.println("@SJC.GenDesc");
		}
		if ((marker & Marks.K_ASRT) != 0)
		{
			spaceIndent();
			finalOut.println("@SJC.CheckAssert");
		}
		if ((marker & Marks.K_NTCB) != 0)
		{
			spaceIndent();
			finalOut.println("@SJC.NativeCallback");
		}
	}
	
	private void printVisibilityModifier(int modifier)
	{
		if ((modifier & Modifier.M_PUB) != 0)
			finalOut.print("public ");
		else if ((modifier & Modifier.M_PROT) != 0)
			finalOut.print("protected ");
		else if ((modifier & Modifier.M_PRIV) != 0)
			finalOut.print("private ");
	}
	
	private void spaceIndent()
	{
		if (!skipNextIndent)
			for (int i = 0; i < indent; i++)
				finalOut.print("  ");
		skipNextIndent = false;
	}
}
