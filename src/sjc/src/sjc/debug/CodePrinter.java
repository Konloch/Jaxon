/* Copyright (C) 2012 Stefan Frenz
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

/**
 * CodePrinter: print supported statements and expressions
 *
 * @author S. Frenz
 * @version 121029 added support for multiple init-/lupd-statements in for-loop
 * version 121020 added "report block end" return value to stmtBlockStart
 * version 121014 added support for assert statement
 * version 120924 added support for expressions
 * version 120923 initial version
 */

public abstract class CodePrinter
{
	//error messages
	public abstract void reportError(Token token, String error);
	
	//statements
	public abstract void stmtAssert(Expr cond, Expr msg);
	
	public abstract void blockLabel(String name);
	
	public abstract boolean stmtBlockStart(); //returns "report block end"
	
	public abstract void stmtBlockEnd();
	
	public abstract void stmtEmpty();
	
	public abstract void stmtEndLoop(boolean contNotBreak, String labelToEnd);
	
	public abstract void stmtExpr(Expr ex);
	
	public abstract void stmtFor(TokenAbstrPrintable init, TokenAbstrPrintable[] furtherInit, TokenAbstrPrintable lupd, TokenAbstrPrintable[] furtherLupd, Expr cond, TokenAbstrPrintable loStmt);
	
	public abstract void stmtForEnh(TokenAbstrPrintable var, Expr iter, TokenAbstrPrintable loStmt);
	
	public abstract void stmtIf(Expr cond, TokenAbstrPrintable trStmt, TokenAbstrPrintable faStmt);
	
	public abstract void stmtReturn(Expr retVal);
	
	public abstract void stmtReturnMissing();
	
	public abstract void stmtSwitchStart(Expr cond);
	
	public abstract void stmtSwitchCase(Expr cond);
	
	public abstract void stmtSwitchEnd();
	
	public abstract void stmtSync(Expr syncObj, TokenAbstrPrintable syncBlock);
	
	public abstract void stmtThrow(Expr throwVal);
	
	public abstract void stmtTryStart(TokenAbstrPrintable tryBlock);
	
	public abstract void stmtTryCatch(Vrbl catchVar, TokenAbstrPrintable catchBlock);
	
	public abstract void stmtTryFinally(TokenAbstrPrintable finallyBlock);
	
	public abstract void stmtVrbl(Vrbl varList, int varCount);
	
	public abstract void stmtWhile(Expr cond, boolean inclusiveWhile, TokenAbstrPrintable loStmt);
	
	//expressions
	public abstract void exprArrayInit(TypeRef type, FilledParam par);
	
	public abstract void exprBin(Expr le, Expr ri, int opType, int opPar, int rank);
	
	public abstract void exprCall(Mthd dest, FilledParam par);
	
	public abstract void exprNew(TypeRef type, boolean asArray, boolean multArray, boolean callExplicitConstr, Unit destTypeUnit, Mthd dest, FilledParam par);
	
	public abstract void exprChoose(Expr le, Expr ce, Expr ri);
	
	public abstract void exprClssName(TypeRef destType);
	
	public abstract void exprConstStruct(ExConstStruct constStruct);
	
	public abstract void exprDeArray(Expr le, Expr ind);
	
	public abstract void exprEnc(TypeRef convertTo, Expr ex);
	
	public abstract void exprPrePst(Expr ex, int opPar, boolean pre);
	
	public abstract void exprString(String value);
	
	public abstract void exprSuper(Expr ri);
	
	public abstract void exprUna(Expr ex, int opType, int opPar);
	
	public abstract void exprVar(AccVar dest, String id, boolean isThis);
	
	public abstract void exprVal(TypeRef type, int intValue, long longValue);
	
	public abstract void exprDeref(Expr le, Expr ri, boolean leftStatic);
	
	//magic expressions
	public abstract void magcVar(AccVar dest);
	
	public abstract void magcCall(String id, FilledParam par);
	
	public abstract void magcClssMthdName(Unit unit, Mthd mthd);
}
