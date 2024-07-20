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

package sjc.frontend.sjava;

import sjc.backend.Instruction;
import sjc.compbase.*;
import sjc.debug.CodePrinter;

/**
 * ExBin: binary expression
 *
 * @author S. Frenz
 * @version 120924 added support for code printer
 * version 110610 allowed struct compare to null
 * version 110509 added struct check in null type assignment
 * version 110307 fixed check for invalid types
 * version 101210 adopted changed Architecture
 * version 101102 made value output of boolean operation conforming to write once semantic
 * version 101028 added implicit conversions for assign+op if not in explicitConversion mode
 * version 101027 fixed implicit shift conversion check, adopted changed assign+op, added support for assign+bitshift
 * version 101024 fixed op in call to genUnaOp
 * version 101015 adopted changed Expr
 * version 100929 made use of implicit conversion code in TypeRef
 * version 100927 fixed unsignedness of chars
 * version 100512 adopted changed Modifier/Marks
 * version 100127 adopted renaming of *ariCall into binAriCall
 * version 100126 fixed implicit conversion for char types
 * version 091112 added support for explicitTypeConversion
 * version 091026 using new genBinOpConst* in Architecture
 * version 091005 adopted changed Expr with support for preferredType in resolving
 * version 091001 adopted changed memory interface
 * version 090724 adopted changed Expr
 * version 090719 adopted changed Expr
 * version 090619 adopted changed Architecture
 * version 090401 fixed register allocation in genOutputVal for reg==0 in assign with call
 * version 090219 fixed bug in genCondJmp for C_ISIMPL
 * version 090207 added copyright notice, changed genPushNull to genPushConstVal, optimized assign
 * version 090206 optimized assign structure to make use of changed Expr and Architecture
 * version 081023 optimized genOutputVal for assignments to addresses on stack
 * version 081021 adopted changed Architecture.genComp/genCondJmp
 * version 080727 fixed checkGenAriCall for parameterized assignments
 * version 080624 added object-test for arithmetic operators
 * version 080613 adopted hasEffect->effectType, optimized handling of constants
 * version 080525 adopted changed genCondJmp signature
 * version 080520 fixed different targets for ariCalls
 * version 080518 added check for constant expression before setting of dynamicAri-request, changed ariCall-op-par to byte
 * version 080517 optimized multiple-if towards single-switch, adopted changes of Architecture.ariLongCall to ariCall
 * version 080203 fixed genOutputVal for constant double expressions
 * version 080119 adopted changed signature of Expr.canGenAddr
 * version 080118 added support for type-checking in assign
 * version 070910 adopted changes in TypeRef.compareType
 * version 070909 optimized signature of Expr.resolve
 * version 070828 fixed implicit conversion for interface assignments
 * version 070813 fixed getConstLongVal
 * version 070812 added support for constant float/double results, basic checks for float/double
 * version 070809 adopted changed for float/double
 * version 070727 replaced exSubResolve by resolve as there is nothing done anymore
 * version 070628 adopted changes for "Expr extends TypeRef"
 * version 070625 renamed TypeRef.S_* to T_* to conform with StdTypes
 * version 070531 adopted removal of Architecture.genLoadFromMem
 * version 070526 better error message
 * version 070114 reduced access level where possible
 * version 070111 adapted change in printPos and compErr
 * version 070106 removed genOutputAddr
 * version 070101 adopted change in genCall
 * version 061229 removed access to firstInstr
 * version 061203 optimized calls to printPos and compErr
 * version 061202 adopted change of genCall
 * version 061102 added checks to prohibit instanceof-call for structs
 * version 060807 added support for call on arithmetic operation
 * version 060629 added support for call instead of ptr-assign
 * version 060628 added support for static compilation
 * version 060621 implicit conversion for object->interface
 * version 060607 initial version
 */

public class ExBin extends ExCheckType
{
	private final static String INVCALLGETCONST = "invalid call to getConst*Val";
	private final static String ARINOTOBJ = "arithmetic operators are not supported for object types";
	protected Expr le, ri;
	protected int op, rank;
	private boolean genAssignCall, genAriCall;
	private char ariCallOp;
	
	protected ExBin(int iop, int ira, int fid, int il, int ic)
	{
		super(fid, il, ic);
		op = iop;
		rank = ira;
	}
	
	public void printExpression(CodePrinter codePrnt)
	{
		codePrnt.exprBin(le, ri, op >>> 16, op & 0xFFFF, rank);
	}
	
	public boolean resolve(Unit unitContext, Mthd mthdContext, int resolveFlags, TypeRef preferredType, Context ctx)
	{
		int cmpRes, opType, opPar, rcType;
		boolean change;
		Expr tmpEx;
		TypeRef cmpType;
		
		opType = op >>> 16;
		opPar = op & 0xFFFF;
		//check normal expressions
		resolveFlags |= RF_CHECKREAD;
		if (!le.resolve(unitContext, mthdContext, ((opType != Ops.S_ASN || opPar != 0) ? resolveFlags : (resolveFlags & ~RF_CHECKREAD)), null, ctx)
				|| !ri.resolve(unitContext, mthdContext, resolveFlags, (opType == Ops.S_ASN
				|| opType == Ops.S_CMP) ? le : null, ctx))
			return false;
		//try to get resulting type
		switch (opType)
		{ //resulting type depends on operator
			case Ops.S_ASN: //le has to be var and the resulting type has to match type of le
			case Ops.S_ASNARI:
			case Ops.S_ASNBSH: //mostly the same checks have to be done
				if (!le.canGenAddr(unitContext, false, resolveFlags, ctx))
				{
					printPos(ctx, "left side not valid as destination");
					return false;
				}
				if (ri.baseType == T_NULL)
				{ //assign null-type
					if (!assignNullType(ctx))
						return false;
				}
				else
				{
					if (opType != Ops.S_ASN)
					{
						if (opType == Ops.S_ASNARI)
							cmpType = le;
						else
							cmpType = ctx.intType;
						cmpRes = cmpType.compareType(ri, true, ctx); //get type-compare
						if (cmpRes == TypeRef.C_NP || cmpRes == TypeRef.C_OT)
						{
							if ((mthdContext.marker & Marks.K_EXPC) == 0)
							{
								if ((ri = ExEnc.getConvertedResolvedExpr(ri, cmpType, unitContext, ctx)) == null)
									return false;
							}
							else
							{
								if (opType == Ops.S_ASNBSH)
									printPos(ctx, "right type of shift has to be int");
								else
									noImplicitConvert(ctx);
								return false;
							}
						}
					}
					else
					{
						cmpRes = le.compareType(ri, true, ctx); //get type-compare
						if (cmpRes == TypeRef.C_NP || cmpRes == TypeRef.C_OT)
						{
							if ((mthdContext.marker & Marks.K_EXPC) == 0)
							{
								TypeRef destType = ri.getBaseTypeConvertionType(le, false);
								if (destType != null)
								{
									if ((ri = ExEnc.getConvertedResolvedExpr(ri, destType, unitContext, ctx)) == null)
										return false;
								}
								else
								{
									noImplicitConvert(ctx);
									return false;
								}
							}
							else
							{
								noImplicitConvert(ctx);
								return false;
							}
						}
						if (cmpRes == TypeRef.C_TT && le.isIntfType())
						{ //insert implicit conversion of object to interface
							if ((ri = ExEnc.getConvertedResolvedExpr(ri, le, unitContext, ctx)) == null)
								return false;
						}
					}
				}
				getTypeOf(le); //required for checkAriBasicsAndCall
				if ((le.isObjType() || le.isIntfType()) && (ctx.assignCall || (ctx.assignHeapCall && !le.isAddrOnStack())))
				{
					if (ctx.dynaMem)
						runtimeClass = unitContext.getRefUnit(ctx.rteDynamicRuntime, true);
					genAssignCall = true;
				}
				else
					switch (opType)
					{
						case Ops.S_ASNARI:
							if (baseType < 0)
							{
								printPos(ctx, ARINOTOBJ);
								return false;
							}
							if (!checkAriBasicsAndCall(opPar, unitContext, ctx))
								return false;
							break;
						case Ops.S_ASNBSH:
							if (baseType < 0)
							{
								printPos(ctx, ARINOTOBJ);
								return false;
							}
							if (le.baseType != T_BYTE && le.baseType != T_SHRT && le.baseType != T_CHAR && le.baseType != T_INT && le.baseType != T_LONG)
							{
								printPos(ctx, "left operand of shift has to be byte, short, char, int or long");
								return false;
							}
							break;
					}
				effectType = EF_NORM;
				break;
			case Ops.S_CMP: //if types are equal or convertable, the result is a boolean value
				if (opPar == Ops.C_INOF)
				{ //instanceof
					if (le.baseType != T_QID)
					{
						printPos(ctx, "only objects can be instanceof a class");
						return false;
					}
					if (!prepareObjectConversion(le, ri, unitContext, ctx))
						return false;
				}
				else
				{ //all other compares
					if (le.baseType == T_NULL)
					{
						if (ri.baseType == T_NULL)
						{ //compare constant null with constant null ?!?
							printPos(ctx, "warning: expensive constant expression");
							ctx.out.println();
							ri.baseType = le.baseType = T_NNPT;
						}
						else
						{
							//replace left and right side to have the "null" on the right one
							tmpEx = le;
							le = ri;
							ri = tmpEx;
						}
					}
					if (ri.baseType == T_NULL)
					{ //check null-type
						if (!assignNullType(ctx))
							return false;
					}
					else
					{
						if (le.compareType(ri, true, ctx) == TypeRef.C_NP)
						{
							if ((mthdContext.marker & Marks.K_EXPC) == 0)
							{
								if (!checkImplicitIntBaseTypeConversion(unitContext, ctx))
									return false;
								//else: success in implicit conversion
							}
							else
							{
								noImplicitConvert(ctx);
								return false;
							}
						}
						if (le.baseType < 0 || le.arrDim > 0 || ri.baseType < 0 || ri.arrDim > 0)
						{ //compare pointers, check type of compare
							if (opPar != Ops.C_EQ && opPar != Ops.C_NE)
							{
								printPos(ctx, "objects can only be compared as (not) equal");
								return false;
							}
						}
					}
				}
				if (le.calcConstantType(ctx) != 0 && ri.calcConstantType(ctx) == 0)
				{
					change = false;
					switch (opPar)
					{ //if left side is constant and right side is not, try to change sides
						case Ops.C_GE:
							opPar = Ops.C_LE;
							change = true;
							break;
						case Ops.C_GT:
							opPar = Ops.C_LW;
							change = true;
							break;
						case Ops.C_LE:
							opPar = Ops.C_GE;
							change = true;
							break;
						case Ops.C_LW:
							opPar = Ops.C_GT;
							change = true;
							break;
						case Ops.C_EQ:
						case Ops.C_NE:
							change = true;
							break;
					}
					if (change)
					{
						tmpEx = le;
						le = ri;
						ri = tmpEx;
						op = (Ops.S_CMP << 16) | opPar;
					}
				}
				baseType = TypeRef.T_BOOL;
				break;
			case Ops.S_ARI: //types have to be equal or convertable
				if ((mthdContext.marker & Marks.K_EXPC) == 0 && !checkImplicitIntBaseTypeConversion(unitContext, ctx))
					return false;
				switch (le.compareType(ri, true, ctx))
				{
					case TypeRef.C_NP:
						noImplicitConvert(ctx);
						return false;
					case TypeRef.C_EQ:
					case TypeRef.C_TT:
						getTypeOf(le);
						break;
					case TypeRef.C_OT:
						getTypeOf(ri);
						break;
					default:
						noImplicitConvert(ctx);
						return false;
				}
				if (baseType < 0)
				{
					printPos(ctx, ARINOTOBJ);
					return false;
				}
				if (!checkAriBasicsAndCall(opPar, unitContext, ctx))
					return false;
				if (le.calcConstantType(ctx) != 0 && ri.calcConstantType(ctx) == 0)
					switch (opPar)
					{ //if left side is constant and right side is not, try to change sides
						case Ops.A_AND:
						case Ops.A_OR:
						case Ops.A_XOR:
						case Ops.A_MUL:
						case Ops.A_PLUS:
							tmpEx = le;
							le = ri;
							ri = tmpEx;
							break;
					}
				break;
			case Ops.S_BSH:
				if ((mthdContext.marker & Marks.K_EXPC) == 0 && ri.arrDim == 0 && (ri.baseType == T_BYTE || ri.baseType == T_SHRT || ri.baseType == T_CHAR))
				{
					if ((ri = ExEnc.getConvertedResolvedExpr(ri, ctx.intType, unitContext, ctx)) == null)
						return false;
				}
				if (!ri.isIntType())
				{
					printPos(ctx, "right operand of shift has to be int");
					return false;
				}
				if (le.arrDim != 0 || (le.baseType != T_BYTE && le.baseType != T_SHRT && le.baseType != T_CHAR && le.baseType != T_INT && le.baseType != T_LONG))
				{
					printPos(ctx, "left operand of shift has to be byte, short, char, int or long");
					return false;
				}
				if ((mthdContext.marker & Marks.K_EXPC) == 0 && le.baseType != T_INT && le.baseType != T_LONG)
				{
					if ((le = ExEnc.getConvertedResolvedExpr(le, ctx.intType, unitContext, ctx)) == null)
						return false;
				}
				getTypeOf(le);
				break;
			case Ops.S_LOG: //logical operators need boolean values as operands and have boolean result
				if (!le.isBoolType() || !ri.isBoolType())
				{
					noImplicitConvert(ctx);
					return false;
				}
				baseType = T_BOOL;
				break;
			default:
				ctx.out.println("### ExBin.exSubResolve doesn't check all types");
				ctx.err = true;
				return false;
		}
		//if division, check if right parameter is not constant zero
		if (opType == Ops.S_ASN || opType == Ops.S_ARI)
		{
			opPar = op & 0xFFFF;
			rcType = ri.calcConstantType(ctx);
			if ((opPar == Ops.A_DIV || opPar == Ops.A_MOD) && ((rcType == T_INT && ri.getConstIntValue(ctx) == 0) || (rcType == T_LONG && ri.getConstLongValue(ctx) == 0l)))
			{
				printPos(ctx, "division by constant zero");
				return false;
			}
		}
		//everything OK
		return true;
	}
	
	private boolean checkImplicitIntBaseTypeConversion(Unit unitContext, Context ctx)
	{
		TypeRef dest = ri.getBaseTypeConvertionType(le.getBaseTypeConvertionType(ctx.intType, true), true);
		if (dest != null)
		{
			if (le.baseType != dest.baseType && (le = ExEnc.getConvertedResolvedExpr(le, dest, unitContext, ctx)) == null)
				return false;
			return ri.baseType == dest.baseType || (ri = ExEnc.getConvertedResolvedExpr(ri, dest, unitContext, ctx)) != null;
		}
		if (le.baseType == ri.baseType)
			return true;
		noImplicitConvert(ctx);
		return false;
	}
	
	private boolean checkAriBasicsAndCall(int opPar, Unit unitContext, Context ctx)
	{
		switch (baseType)
		{
			case T_FLT:
			case T_DBL:
				switch (opPar)
				{
					case Ops.A_PLUS:
					case Ops.A_MINUS:
					case Ops.A_MUL:
					case Ops.A_DIV:
						break;
					default:
						printPos(ctx, "not a valid float/double operation");
						return false;
				}
		}
		if (calcConstantType(ctx) == 0 && (ctx.arch.binAriCall[baseType] & (1 << (opPar - Ops.MSKBSE))) != 0)
		{
			if (ctx.dynaMem)
				runtimeClass = unitContext.getRefUnit(ctx.rteDynamicAri, true);
			genAriCall = true;
			ctx.arch.binAriCall[baseType] |= 0x80000000; //set sign-bit to request service
			switch (opPar)
			{
				case Ops.A_PLUS:
					ariCallOp = '+';
					break;
				case Ops.A_MINUS:
					ariCallOp = '-';
					break;
				case Ops.A_MUL:
					ariCallOp = '*';
					break;
				case Ops.A_DIV:
					ariCallOp = '/';
					break;
				case Ops.A_MOD:
					ariCallOp = '%';
					break;
				default:
					compErr(ctx, "ExBin.exSubResolve with call-request for unchecked operation");
					return false;
			}
		}
		//else: operation will be done in native code, no call
		return true;
	}
	
	private boolean assignNullType(Context ctx)
	{
		if (le.isStructType())
		{
			if (op >>> 16 != Ops.S_CMP)
			{
				printPos(ctx, "null and struct are not compatible");
				return false;
			}
			else
				ri.baseType = T_NNPT;
			return true;
		}
		switch (le.getRegType(ctx))
		{
			case StdTypes.T_PTR:
				ri.baseType = T_NNPT;
				return true;
			case StdTypes.T_DPTR:
				ri.baseType = T_NDPT;
				return true;
		}
		printPos(ctx, "unresolvable pointer for null");
		return false;
	}
	
	public int calcConstantType(Context ctx)
	{
		int opType;
		
		if (arrDim > 0)
			return 0;
		opType = op >>> 16;
		if (opType != Ops.S_ARI && opType != Ops.S_BSH && opType != Ops.S_LOG && opType != Ops.S_CMP)
			return 0;
		if (le.calcConstantType(ctx) == 0 || ri.calcConstantType(ctx) == 0)
			return 0;
		switch (baseType)
		{
			case T_BYTE:
			case T_SHRT:
			case T_INT:
			case T_CHAR:
			case T_BOOL:
			case T_FLT:
				return T_INT;
			case T_LONG:
			case T_DBL:
				return T_LONG;
		}
		return 0;
	}
	
	public int getConstIntValue(Context ctx)
	{
		int l, r;
		long ll, lr;
		int opType, par;
		
		if (le.baseType != ri.baseType)
		{
			compErr(ctx, INVCALLGETCONST);
			return 0;
		}
		opType = op >>> 16;
		par = op & 0xFFFF;
		if (opType == Ops.S_CMP)
		{
			if (le.calcConstantType(ctx) == T_INT && ri.calcConstantType(ctx) == T_INT)
			{
				l = le.getConstIntValue(ctx);
				r = ri.getConstIntValue(ctx);
				if (le.baseType == StdTypes.T_FLT)
					return ctx.arch.real.compareConstFloat(l, r, par);
				switch (par)
				{
					case Ops.C_LW:
						if (l < r)
							return 1;
						else
							return 0;
					case Ops.C_LE:
						if (l <= r)
							return 1;
						else
							return 0;
					case Ops.C_EQ:
						if (l == r)
							return 1;
						else
							return 0;
					case Ops.C_GE:
						if (l >= r)
							return 1;
						else
							return 0;
					case Ops.C_GT:
						if (l > r)
							return 1;
						else
							return 0;
					case Ops.C_NE:
						if (l != r)
							return 1;
						else
							return 0;
				}
			}
			if (le.calcConstantType(ctx) == T_LONG && ri.calcConstantType(ctx) == T_LONG)
			{
				ll = le.getConstLongValue(ctx);
				lr = ri.getConstLongValue(ctx);
				if (le.baseType == StdTypes.T_DBL)
					return ctx.arch.real.compareConstDouble(ll, lr, par);
				switch (par)
				{
					case Ops.C_LW:
						if (ll < lr)
							return 1;
						else
							return 0;
					case Ops.C_LE:
						if (ll <= lr)
							return 1;
						else
							return 0;
					case Ops.C_EQ:
						if (ll == lr)
							return 1;
						else
							return 0;
					case Ops.C_GE:
						if (ll >= lr)
							return 1;
						else
							return 0;
					case Ops.C_GT:
						if (ll > lr)
							return 1;
						else
							return 0;
					case Ops.C_NE:
						if (ll != lr)
							return 1;
						else
							return 0;
				}
			}
			compErr(ctx, INVCALLGETCONST);
			return 0;
		}
		if (le.calcConstantType(ctx) != T_INT || ri.calcConstantType(ctx) != T_INT)
		{
			compErr(ctx, INVCALLGETCONST);
			return 0;
		}
		l = le.getConstIntValue(ctx);
		r = ri.getConstIntValue(ctx);
		if (baseType == T_FLT)
			return ctx.arch.real.binOpFloat(l, r, par);
		switch (opType)
		{
			case Ops.S_ARI:
				switch (par)
				{
					case Ops.A_AND:
						return l & r;
					case Ops.A_OR:
						return l | r;
					case Ops.A_XOR:
						return r ^ r;
					case Ops.A_PLUS:
						return checkBits(l + r);
					case Ops.A_MINUS:
						return checkBits(l - r);
					case Ops.A_MUL:
						return checkBits(l * r);
					case Ops.A_DIV:
						if (r == 0)
						{
							compErr(ctx, "division by zero");
							ctx.out.println();
							return 0;
						}
						else
							return checkBits(l / r);
					case Ops.A_MOD:
						return checkBits(l % r);
				}
				break;
			case Ops.S_BSH:
				switch (par)
				{
					case Ops.B_SHL:
						return checkBits(l << r);
					case Ops.B_SHRL:
						return l >>> r;
					case Ops.B_SHRA:
						return l >> r;
				}
				break;
			case Ops.S_LOG:
				switch (par)
				{
					case Ops.L_AND:
						if (l != 0 && r != 0)
							return 1;
						else
							return 0;
					case Ops.L_OR:
						if (l != 0 || r != 0)
							return 1;
						else
							return 0;
				}
				break;
		}
		compErr(ctx, INVCALLGETCONST);
		return 0;
	}
	
	private int checkBits(int v)
	{
		switch (baseType)
		{
			case T_BYTE:
			case T_SHRT:
			case T_CHAR:
				return v;
		}
		return v;
	}
	
	public long getConstLongValue(Context ctx)
	{
		long ll, lr;
		int r, opType, par;
		
		if (le.calcConstantType(ctx) != T_LONG)
		{
			compErr(ctx, INVCALLGETCONST);
			return 0l;
		}
		ll = le.getConstLongValue(ctx);
		opType = op >>> 16;
		par = op & 0xFFFF;
		if (opType == Ops.S_BSH)
		{
			if (le.baseType != T_LONG || ri.baseType != T_INT || ri.calcConstantType(ctx) != T_INT)
			{
				compErr(ctx, INVCALLGETCONST);
				return 0l;
			}
			r = ri.getConstIntValue(ctx);
			switch (par)
			{
				case Ops.B_SHL:
					return ll << r;
				case Ops.B_SHRL:
					return ll >>> r;
				case Ops.B_SHRA:
					return ll >> r;
			}
		}
		else
		{
			if (le.baseType != ri.baseType || ri.calcConstantType(ctx) != T_LONG)
			{
				compErr(ctx, INVCALLGETCONST);
				return 0l;
			}
			lr = ri.getConstLongValue(ctx);
			if (baseType == T_DBL)
				return ctx.arch.real.binOpDouble(ll, lr, par);
			if (opType == Ops.S_ARI)
			{
				switch (par)
				{
					case Ops.A_AND:
						return ll & lr;
					case Ops.A_OR:
						return ll | lr;
					case Ops.A_XOR:
						return lr ^ lr;
					case Ops.A_PLUS:
						return ll + lr;
					case Ops.A_MINUS:
						return ll - lr;
					case Ops.A_MUL:
						return ll * lr;
					case Ops.A_DIV:
						if (lr == 0l)
						{
							printPos(ctx, "division by zero");
							ctx.out.println();
						}
						else
							return ll / lr;
					case Ops.A_MOD:
						return ll % lr;
				}
			}
		}
		compErr(ctx, INVCALLGETCONST);
		return 0l;
	}
	
	private void noImplicitConvert(Context ctx)
	{
		printPos(ctx, "cannot implicitly convert ");
		le.printType(ctx.out);
		ctx.out.print("!=");
		ri.printType(ctx.out);
	}
	
	public void genOutputVal(int reg, Context ctx)
	{
		Instruction trueDest, falseDest, after;
		int opType = op >>> 16, opPar = op & 0xFFFF, riConstType = 0;
		int addr, reg1, reg2, restore1, restore2, restore3, restoreReg = 0;
		boolean deallocReg = false;
		
		//check if expression is constant
		switch (calcConstantType(ctx))
		{
			case 0:
				break; //not constant
			case T_INT:
				ctx.arch.genLoadConstVal(reg, getConstIntValue(ctx), getRegType(ctx));
				return;
			case T_LONG:
				ctx.arch.genLoadConstDoubleOrLongVal(reg, getConstLongValue(ctx), getRegType(ctx) == StdTypes.T_DBL);
				return;
			default:
				compErr(ctx, "unkown constant type in ExBin.genOutputVal");
				return;
		}
		//check if right side is constant
		switch (ri.calcConstantType(ctx))
		{
			case T_INT:
				riConstType = T_INT;
				break;
			case T_LONG:
				riConstType = T_LONG;
				break;
		}
		//evaluate
		switch (opType)
		{
			case Ops.S_ASN:
				if (!genAssignCall)
					le.genOutputAssignTo(reg, ri, ctx);
				else
				{
					if (reg == 0)
					{
						restoreReg = ctx.arch.prepareFreeReg(0, 0, 0, isIntfType() ? StdTypes.T_DPTR : StdTypes.T_PTR);
						reg = ctx.arch.allocReg();
						deallocReg = true;
					}
					restore1 = ctx.arch.prepareFreeReg(reg, 0, 0, StdTypes.T_PTR);
					addr = ctx.arch.allocReg();
					le.genOutputPrepareAssignTo(addr, reg, ri, ctx);
					restore2 = ctx.arch.ensureFreeRegs(addr, 0, reg, addr);
					if (ctx.dynaMem)
						ctx.arch.genSaveUnitContext();
					if (isIntfType())
					{
						ctx.arch.genPushConstVal(1, T_BOOL); //true: interface
						ctx.arch.genPush(addr, StdTypes.T_PTR);
						ctx.arch.genPush(reg, StdTypes.T_DPTR);
					}
					else
					{
						ctx.arch.genPushConstVal(0, T_BOOL); //false: normal object
						ctx.arch.genPush(addr, StdTypes.T_PTR);
						ctx.arch.genPushConstVal(0, StdTypes.T_PTR);
						ctx.arch.genPush(reg, StdTypes.T_PTR);
					}
					if (ctx.dynaMem)
					{
						ctx.arch.genLoadUnitContext(ctx.arch.regClss, runtimeClass.relOff);
						ctx.arch.genCall(ctx.rteDRAssignMd.relOff, ctx.arch.regClss, ctx.rteDRAssignMd.parSize);
						ctx.arch.genRestUnitContext();
					}
					else
						ctx.arch.genCallConst(ctx.rteDRAssignMd, ctx.rteDRAssignMd.parSize);
					ctx.arch.deallocRestoreReg(0, reg, restore2);
					ctx.arch.deallocRestoreReg(addr, 0, restore1);
					if (deallocReg)
						ctx.arch.deallocRestoreReg(reg, 0, restoreReg);
				}
				return;
			case Ops.S_ASNARI:
				if (chkAriErr(ctx))
					return;
				if (reg == 0)
				{
					restoreReg = ctx.arch.prepareFreeReg(0, 0, 0, baseType);
					reg = ctx.arch.allocReg();
					deallocReg = true;
				}
				restore1 = ctx.arch.prepareFreeReg(reg, 0, 0, StdTypes.T_PTR);
				addr = ctx.arch.allocReg();
				le.genOutputAddr(addr, ctx);
				restore2 = ctx.arch.prepareFreeReg(addr, 0, reg, baseType);
				reg1 = ctx.arch.allocReg();
				ctx.arch.genLoadVarVal(reg1, addr, null, 0, baseType);
				if (riConstType == 0 || genAriCall)
				{
					restore3 = ctx.arch.prepareFreeReg(reg, reg1, 0, baseType);
					reg2 = ctx.arch.allocReg();
					ri.genOutputVal(reg2, ctx);
					if (genAriCall)
						genOutAriCall(reg, reg1, reg2, ctx);
					else
						ctx.arch.genBinOp(reg, reg1, reg2, (Ops.S_ARI << 16) | opPar, baseType);
					ctx.arch.deallocRestoreReg(reg2, 0, restore3);
				}
				else
				{
					if (riConstType == T_INT)
						ctx.arch.genBinOpConstRi(reg, reg1, ri.getConstIntValue(ctx), (Ops.S_ARI << 16) | opPar, baseType);
					else
						ctx.arch.genBinOpConstDoubleOrLongRi(reg, reg1, ri.getConstLongValue(ctx), (Ops.S_ARI << 16) | opPar, baseType == T_DBL);
				}
				ctx.arch.deallocRestoreReg(reg1, reg, restore2);
				ctx.arch.genAssign(addr, reg, baseType);
				ctx.arch.deallocRestoreReg(addr, 0, restore1);
				if (deallocReg)
					ctx.arch.deallocRestoreReg(reg, 0, restoreReg);
				return;
			case Ops.S_ASNBSH:
				if (baseType < 0 || arrDim > 0)
				{
					compErr(ctx, "bitshift operations not allowed for pointers");
					return;
				}
				if (reg == 0)
				{
					restoreReg = ctx.arch.prepareFreeReg(0, 0, 0, baseType);
					reg = ctx.arch.allocReg();
					deallocReg = true;
				}
				restore1 = ctx.arch.prepareFreeReg(reg, 0, 0, StdTypes.T_PTR);
				addr = ctx.arch.allocReg();
				le.genOutputAddr(addr, ctx);
				restore2 = ctx.arch.prepareFreeReg(addr, 0, reg, baseType);
				reg1 = ctx.arch.allocReg();
				ctx.arch.genLoadVarVal(reg1, addr, null, 0, baseType);
				if (riConstType == 0)
				{
					restore3 = ctx.arch.prepareFreeReg(reg, reg1, 0, StdTypes.T_INT);
					reg2 = ctx.arch.allocReg();
					ri.genOutputVal(reg2, ctx);
					ctx.arch.genBinOp(reg, reg1, reg2, (Ops.S_BSH << 16) | opPar, baseType);
					ctx.arch.deallocRestoreReg(reg2, 0, restore3);
				}
				else
				{
					if (riConstType == T_INT)
						ctx.arch.genBinOpConstRi(reg, reg1, ri.getConstIntValue(ctx), (Ops.S_BSH << 16) | opPar, baseType);
					else
						compErr(ctx, "bitshift with constant right but not int type");
				}
				ctx.arch.deallocRestoreReg(reg1, reg, restore2);
				ctx.arch.genAssign(addr, reg, baseType);
				ctx.arch.deallocRestoreReg(addr, 0, restore1);
				if (deallocReg)
					ctx.arch.deallocRestoreReg(reg, 0, restoreReg);
				return;
			case Ops.S_CMP:
				if (opPar == Ops.C_INOF && checkMode != C_ISIMPL)
				{
					restore1 = ctx.arch.prepareFreeReg(0, 0, 0, le.getRegType(ctx));
					addr = ctx.arch.allocReg();
					le.genOutputVal(addr, ctx);
					genIsType(reg, addr, false, ctx);
					ctx.arch.deallocRestoreReg(addr, 0, restore1);
					return;
				}
				break;
			case Ops.S_ARI:
				if (chkAriErr(ctx))
					return;
				restore1 = ctx.arch.prepareFreeReg(0, 0, reg, baseType);
				reg1 = ctx.arch.allocReg();
				le.genOutputVal(reg1, ctx);
				if (riConstType == 0 || genAriCall)
				{
					restore2 = ctx.arch.prepareFreeReg(reg1, 0, 0, baseType);
					reg2 = ctx.arch.allocReg();
					ri.genOutputVal(reg2, ctx);
					if (genAriCall)
						genOutAriCall(reg, reg1, reg2, ctx);
					else
						ctx.arch.genBinOp(reg, reg1, reg2, (Ops.S_ARI << 16) | opPar, baseType);
					ctx.arch.deallocRestoreReg(reg2, 0, restore2);
				}
				else
				{
					if (riConstType == T_INT)
						ctx.arch.genBinOpConstRi(reg, reg1, ri.getConstIntValue(ctx), (Ops.S_ARI << 16) | opPar, baseType);
					else
						ctx.arch.genBinOpConstDoubleOrLongRi(reg, reg1, ri.getConstLongValue(ctx), (Ops.S_ARI << 16) | opPar, baseType == T_DBL);
				}
				ctx.arch.deallocRestoreReg(reg1, reg, restore1);
				return;
			case Ops.S_BSH: //no type-check, already done by exSubResolve
				restore1 = ctx.arch.prepareFreeReg(0, 0, reg, baseType);
				reg1 = ctx.arch.allocReg();
				le.genOutputVal(reg1, ctx);
				if (riConstType != T_INT)
				{
					restore2 = ctx.arch.prepareFreeReg(reg1, 0, 0, T_INT);
					reg2 = ctx.arch.allocReg();
					ri.genOutputVal(reg2, ctx);
					ctx.arch.genBinOp(reg, reg1, reg2, op, baseType);
					ctx.arch.deallocRestoreReg(reg2, 0, restore2);
				}
				else
					ctx.arch.genBinOpConstRi(reg, reg1, ri.getConstIntValue(ctx), op, baseType);
				ctx.arch.deallocRestoreReg(reg1, reg, restore1);
				return;
		}
		//if we came to this point, there is no standard-operation possible, try boolean result
		if (baseType == T_BOOL)
		{
			//normal compare with boolean result
			trueDest = ctx.arch.getUnlinkedInstruction();
			falseDest = ctx.arch.getUnlinkedInstruction();
			after = ctx.arch.getUnlinkedInstruction();
			genOutputCondJmp(falseDest, false, trueDest, ctx); //jump to falseDest if false
			ctx.arch.appendInstruction(trueDest);
			ctx.arch.genLoadConstVal(reg, 1, T_BOOL); //load "true"
			ctx.arch.genJmp(after);
			ctx.arch.appendInstruction(falseDest);
			ctx.arch.genLoadConstVal(reg, 0, T_BOOL); //load "false"
			ctx.arch.appendInstruction(after);
			return;
		}
		//if we came to this point, there is no standard-operation and no boolean result possible
		compErr(ctx, "missing code-generation for operator in ExBin.genOutputVal");
	}
	
	private void genOutAriCall(int reg, int reg1, int reg2, Context ctx)
	{
		int restore;
		Mthd target = ctx.rteDABinAriCallMds[baseType];
		
		restore = ctx.arch.ensureFreeRegs(reg, 0, reg1, reg2);
		if (ctx.dynaMem)
			ctx.arch.genSaveUnitContext();
		ctx.arch.genPush(reg1, baseType);
		ctx.arch.genPush(reg2, baseType);
		ctx.arch.genPushConstVal(ariCallOp, T_BYTE);
		if (ctx.dynaMem)
		{
			ctx.arch.genLoadUnitContext(ctx.arch.regClss, runtimeClass.relOff);
			ctx.arch.genCall(target.relOff, ctx.arch.regClss, target.parSize);
			ctx.arch.genRestUnitContext();
		}
		else
			ctx.arch.genCallConst(target, target.parSize);
		ctx.arch.genMoveFromPrimary(reg, baseType);
		ctx.arch.deallocRestoreReg(0, reg, restore);
	}
	
	private boolean chkAriErr(Context ctx)
	{
		if (baseType < 0 || arrDim > 0)
		{
			compErr(ctx, "arithmetic operations not allowed for pointers");
			return true;
		}
		if (le.baseType != ri.baseType)
		{
			compErr(ctx, "implicit conversions not supported");
			return true;
		}
		return false;
	}
	
	public void genOutputCondJmp(Instruction jumpDest, boolean isTrue, Instruction elseDest, Context ctx)
	{
		int opType = op >>> 16, opPar = op & 0xFFFF, condHnd;
		int addr1, addr2, val1, val2, restore1, restore2;
		Instruction riIns;
		
		if (calcConstantType(ctx) == T_INT)
		{
			val1 = getConstIntValue(ctx);
			if ((val1 == 1 && isTrue) || (val1 == 0 && !isTrue))
				ctx.arch.genJmp(jumpDest);
			//else: don't jump
			return;
		}
		if (le.baseType < 0 || ri.baseType < 0 || le.arrDim > 0 || ri.arrDim > 0)
		{ //compare pointers or check instanceof
			//although pointers may be double sized, the compare has to look only at
			//  the first part of the pointer always
			restore1 = ctx.arch.prepareFreeReg(0, 0, 0, le.getRegType(ctx));
			addr1 = ctx.arch.allocReg();
			le.genOutputVal(addr1, ctx);
			if (opPar == Ops.C_INOF)
			{
				if (checkMode == C_ISIMPL)
				{
					restore2 = ctx.arch.prepareFreeReg(addr1, 0, 0, StdTypes.T_PTR);
					val1 = ctx.arch.allocReg();
					genIsType(val1, addr1, false, ctx);
					condHnd = ctx.arch.genCompPtrToNull(val1, isTrue ? Ops.C_NE : Ops.C_EQ); //inverted result!
				}
				else
				{
					restore2 = ctx.arch.prepareFreeReg(addr1, 0, 0, T_BOOL);
					val1 = ctx.arch.allocReg();
					genIsType(val1, addr1, false, ctx);
					condHnd = ctx.arch.genCompValToConstVal(val1, 1, T_BOOL, isTrue ? Ops.C_EQ : Ops.C_NE);
				}
				ctx.arch.deallocRestoreReg(val1, 0, restore2);
				ctx.arch.deallocRestoreReg(addr1, 0, restore1);
				ctx.arch.genCondJmp(jumpDest, condHnd);
			}
			else
			{
				if (ri.baseType != StdTypes.T_NNPT && ri.baseType != StdTypes.T_NDPT)
				{
					restore2 = ctx.arch.prepareFreeReg(addr1, 0, 0, ri.getRegType(ctx));
					addr2 = ctx.arch.allocReg();
					ri.genOutputVal(addr2, ctx);
					condHnd = ctx.arch.genComp(addr1, addr2, StdTypes.T_PTR, isTrue ? opPar : opPar ^ Ops.INVCBIT);
					ctx.arch.deallocRestoreReg(addr2, 0, restore2);
				}
				else
					condHnd = ctx.arch.genCompPtrToNull(addr1, isTrue ? opPar : opPar ^ Ops.INVCBIT);
				ctx.arch.deallocRestoreReg(addr1, 0, restore1);
				ctx.arch.genCondJmp(jumpDest, condHnd);
			}
			return;
		}
		//stdType!=0
		if (le.baseType != ri.baseType)
		{
			compErr(ctx, "ExBin.genOutputCondJmp for not identical operand-types");
			return;
		}
		switch (opType)
		{
			case Ops.S_LOG:
				if (!le.isBoolType() || !ri.isBoolType())
				{
					compErr(ctx, "ExBin.genOutputCondJmp with logical operator but not logical operands");
					return;
				}
				switch (opPar)
				{
					case Ops.L_AND:
						if (isTrue)
						{
							riIns = ctx.arch.getUnlinkedInstruction();
							le.genOutputCondJmp(elseDest, false, riIns, ctx);
							ctx.arch.appendInstruction(riIns);
							ri.genOutputCondJmp(jumpDest, true, elseDest, ctx);
						}
						else
						{
							riIns = ctx.arch.getUnlinkedInstruction();
							le.genOutputCondJmp(jumpDest, false, riIns, ctx);
							ctx.arch.appendInstruction(riIns);
							ri.genOutputCondJmp(jumpDest, false, elseDest, ctx);
						}
						break;
					case Ops.L_OR:
						if (isTrue)
						{
							riIns = ctx.arch.getUnlinkedInstruction();
							le.genOutputCondJmp(jumpDest, true, riIns, ctx);
							ctx.arch.appendInstruction(riIns);
							ri.genOutputCondJmp(jumpDest, true, elseDest, ctx);
						}
						else
						{
							riIns = ctx.arch.getUnlinkedInstruction();
							le.genOutputCondJmp(elseDest, true, riIns, ctx);
							ctx.arch.appendInstruction(riIns);
							ri.genOutputCondJmp(jumpDest, false, elseDest, ctx);
						}
						break;
					default:
						compErr(ctx, "ExBin.genOutputCondJmp with illegal operator");
						return;
				}
				return;
			case Ops.S_CMP:
				restore1 = ctx.arch.prepareFreeReg(0, 0, 0, le.baseType);
				val1 = ctx.arch.allocReg();
				le.genOutputVal(val1, ctx);
				switch (ri.calcConstantType(ctx))
				{
					case 0:
						restore2 = ctx.arch.prepareFreeReg(val1, 0, 0, ri.baseType);
						val2 = ctx.arch.allocReg();
						ri.genOutputVal(val2, ctx);
						condHnd = ctx.arch.genComp(val1, val2, le.baseType, isTrue ? opPar : opPar ^ Ops.INVCBIT);
						ctx.arch.deallocRestoreReg(val2, 0, restore2);
						break;
					case T_INT:
						condHnd = ctx.arch.genCompValToConstVal(val1, ri.getConstIntValue(ctx), le.baseType, isTrue ? opPar : opPar ^ Ops.INVCBIT);
						break;
					case T_LONG:
						condHnd = ctx.arch.genCompValToConstDoubleOrLongVal(val1, ri.getConstLongValue(ctx), le.baseType == T_DBL, isTrue ? opPar : opPar ^ Ops.INVCBIT);
						break;
					default:
						compErr(ctx, "invalid ExBin-type");
						return;
				}
				ctx.arch.deallocRestoreReg(val1, 0, restore1);
				ctx.arch.genCondJmp(jumpDest, condHnd);
				return;
		}
		//nothing done until now, try default via genOutputVal
		super.genOutputCondJmp(jumpDest, isTrue, elseDest, ctx);
	}
}
