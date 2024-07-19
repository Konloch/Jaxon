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

package sjc.compbase;

import sjc.osio.TextPrinter;

/**
 * TypeRef: fully qualified definition of a type in any declaration
 *
 * @author S. Frenz
 * @version 120227 cleaned up "package sjc." typo
 * version 110624 adopted changed Context
 * version 101231 adopted changed Unit
 * version 100929 added standard code for base type conversion
 * version 100924 added isNullType
 * version 100429 fixed compareType in case of initialized interface null pointer
 * version 100426 removed denial of interface arrays
 * version 100420 added support for flash instance type compare
 * version 100409 renamed resolve to resolveType to avoid confusion
 * version 100312 reorganized typeSpecial to support flash objects
 * version 100126 added char to convertible basetypes
 * version 091123 removed no longer needed symHint
 * version 091112 added getBaseTypeConvertible
 * version 091026 adopted changed minimumAccessLevel return value
 * version 091021 moved minimumAccessLevel to Unit, adopted changed modifier declarations
 * version 091007 fixed inline array type compare
 * version 091005 removed unneeded methods
 * version 091001 adopted changed memory interface
 * version 090918 added support for package private
 * version 090218 added centralized access level check
 * version 090207 added copyright notice
 * version 080604 added checker for Throwable and Exception
 * version 080119 fixed setting classdesc-required-flag for qid-arrays
 * version 080105 adopted changed signature of Unit.isParent
 * version 071227 added support for inner units
 * version 071001 fixed isObjType
 * version 070910 clarified null-compare in compareType
 * version 070808 optimized getMinSize
 * version 070731 optimized resolve
 * version 070727 got is* from Expr
 * version 070702 fixed un-special handling for normal arrays of special classes
 * version 070628 adopted changes for "Expr extends TypeRef"
 * version 070625 renamed S_* to T_* to conform with StdTypes
 * version 070526 added support for inline arrays in instances
 * version 070111 adapted change in printPos and compErr
 * version 070106 replaced arrayInsideStruct by inlinedArraySize
 * version 061129 moved initialized TypeRef-object to Context
 * version 061111 added flag arrayInsideStruct, extended getPtrType to getRegType
 * version 061030 changed detection of indirectCall
 * version 060620 no implicit conversion of standard types
 * version 060607 initial version
 */

public class TypeRef extends Token
{
	//internal types
	public final static int T_RES = 0;
	public final static int T_BYTE = StdTypes.T_BYTE; //"byte"
	public final static int T_SHRT = StdTypes.T_SHRT; //"short"
	public final static int T_INT = StdTypes.T_INT;  //"int"
	public final static int T_LONG = StdTypes.T_LONG; //"long"
	public final static int T_FLT = StdTypes.T_FLT;  //"float"
	public final static int T_DBL = StdTypes.T_DBL;  //"double"
	public final static int T_CHAR = StdTypes.T_CHAR; //"char"
	public final static int T_BOOL = StdTypes.T_BOOL; //"boolean"
	public final static int T_NULL = StdTypes.T_NULL; //null-type, unresolved
	public final static int T_NNPT = StdTypes.T_NNPT; //null-type, pointer
	public final static int T_NDPT = StdTypes.T_NDPT; //null-type, double sized pointer
	public final static int T_VOID = StdTypes.T_VOID;
	public final static int T_QID = StdTypes.T_QID;
	
	//results of comparison
	public final static int C_NP = -1; //conversion not possible
	public final static int C_EQ = 0; //type equal
	public final static int C_TT = 1; //conversion possible, take this type
	public final static int C_OT = 2; //conversion possible, take type of compared expression
	
	//array specials
	public final static int S_NOSPECIAL = 0; //no inline array, no flash object
	public final static int S_STRUCTARRNOTSPEC = -1; //size of struct-array not specified
	public final static int S_STRUCTARRDONTCHECK = -2; //index of struct-array must not be checked
	public final static int S_INSTINLARR = -3; //normal instance-inline array
	public final static int S_FLASHREF = -4; //object is accessed via flash
	public final static int S_FLASHINLARR = -5; //object incl. inline array is accessed via flash
	
	//variables required for resolving
	public int baseType, arrDim;
	public int typeSpecial; //required to check struct and inline arrays and flash-objects, initialized to 0 "everything normal"
	public QualID qid; //used if type is non-standard
	
	public TypeRef(int fid, int il, int ic)
	{
		super(fid, il, ic);
	}
	
	public void printType(TextPrinter v)
	{
		int i;
		
		switch (baseType)
		{
			case T_RES:
				v.print("resType");
				return;
			case T_VOID:
				v.print("void");
				return;
			case T_QID:
				qid.printFullQID(v);
				break;
			default:
				StdTypes.printStdType(baseType, v);
		}
		switch (typeSpecial)
		{
			case S_STRUCTARRDONTCHECK:
			case S_STRUCTARRNOTSPEC:
				v.print("(S)");
				break;
			case S_INSTINLARR:
				v.print("(I)");
				break;
			case S_FLASHREF:
				v.print("(F)");
				break;
			case S_FLASHINLARR:
				v.print("(FI)");
				break;
		}
		for (i = 0; i < arrDim; i++)
			v.print("[]");
	}
	
	public TypeRef copy()
	{
		TypeRef n;
		
		n = new TypeRef(fileID, line, col);
		n.getTypeOf(this);
		return n;
	}
	
	public boolean resolveType(Unit inUnit, Context ctx)
	{
		if (baseType != T_QID)
			return true;
		if (!qid.resolveAsUnit(inUnit, ctx))
			return false;
		if (arrDim > 0)
			qid.unitDest.modifier |= Modifier.MA_ACCSSD; //array of qid requires class descriptor to be generated
		return inUnit.minimumAccessLevel(qid, qid.unitDest.name, qid.unitDest, qid.unitDest.modifier, true, ctx) != Modifier.M_ERROR;
	}
	
	public int compareType(TypeRef cmp, boolean searchParent, Context ctx)
	{
		boolean meNull;
		
		//check uninitialized null
		if (baseType == T_NULL)
		{
			if (cmp.baseType < 0 || cmp.arrDim > 0)
				return C_EQ;
			return C_NP;
		}
		if (cmp.baseType == T_NULL)
		{
			if (baseType < 0 || arrDim > 0)
				return C_EQ;
			return C_NP;
		}
		//check initialized null
		if ((meNull = (baseType == T_NNPT || baseType == T_NDPT)) || cmp.baseType == T_NNPT || cmp.baseType == T_NDPT)
		{
			return getRegType(ctx) != cmp.getRegType(ctx) ? C_NP : meNull ? C_OT : C_TT;
		}
		//check additional array conditions
		if (arrDim != 0 || cmp.arrDim != 0)
		{
			//check for typespecials
			if ((typeSpecial != S_NOSPECIAL || cmp.typeSpecial != S_NOSPECIAL) && (typeSpecial != S_FLASHREF || typeSpecial != cmp.typeSpecial))
				return C_NP;
			//ok, deeper check
			if (arrDim != cmp.arrDim)
			{
				if (cmp.baseType == T_QID && (cmp.qid.unitDest == ctx.langRoot || cmp.qid.unitDest == ctx.rteSArray))
					return C_OT;
				if (baseType == T_QID && (qid.unitDest == ctx.langRoot || qid.unitDest == ctx.rteSArray))
					return C_TT;
				return C_NP;
			}
		}
		//check normally
		if (baseType <= 0 && baseType != cmp.baseType)
			return C_NP; //will never match
		if (baseType == T_VOID)
			return C_EQ; //void==void always
		//if (inlinedArraySize!=0 || cmp.inlinedArraySize!=0) return C_NP; //inlined array pointer not existent
		if (baseType != T_QID)
		{ //check standard-conversion
			if (baseType == cmp.baseType)
				return C_EQ; //same standard-type
			return C_NP; //do not try to convert, implicit conversion is handled by caller
		}
		//check object-conversion
		if (qid.unitDest == cmp.qid.unitDest)
			return C_EQ;
		if (!searchParent)
			return C_NP;
		if (qid.unitDest.isParent(cmp.qid.unitDest, ctx))
			return C_TT;
		if (cmp.qid.unitDest.isParent(qid.unitDest, ctx))
			return C_OT;
		return C_NP;
	}
	
	public TypeRef getBaseTypeConvertionType(TypeRef dest, boolean viceversa)
	{
		if (dest == null)
			return null;
		int meBaseType = getBaseTypeConvertible(), destBaseType = dest.getBaseTypeConvertible();
		if (meBaseType != 0 && destBaseType != 0)
			switch (meBaseType)
			{
				case StdTypes.T_BYTE:
					switch (destBaseType)
					{
						case StdTypes.T_BYTE:
						case StdTypes.T_SHRT:
						case StdTypes.T_CHAR:
						case StdTypes.T_INT:
						case StdTypes.T_LONG:
						case StdTypes.T_FLT:
						case StdTypes.T_DBL:
							return dest;
					}
					break;
				case StdTypes.T_SHRT:
					switch (destBaseType)
					{
						case StdTypes.T_BYTE:
							return viceversa ? this : null;
						case StdTypes.T_SHRT:
						case StdTypes.T_INT:
						case StdTypes.T_LONG:
						case StdTypes.T_FLT:
						case StdTypes.T_DBL:
							return dest;
					}
					break;
				case StdTypes.T_CHAR:
					switch (destBaseType)
					{
						case StdTypes.T_BYTE:
							return viceversa ? this : null;
						case StdTypes.T_CHAR:
						case StdTypes.T_INT:
						case StdTypes.T_LONG:
						case StdTypes.T_FLT:
						case StdTypes.T_DBL:
							return dest;
					}
					break;
				case StdTypes.T_INT:
					switch (destBaseType)
					{
						case StdTypes.T_BYTE:
						case StdTypes.T_SHRT:
						case StdTypes.T_CHAR:
							return viceversa ? this : null;
						case StdTypes.T_INT:
						case StdTypes.T_LONG:
						case StdTypes.T_FLT:
						case StdTypes.T_DBL:
							return dest;
					}
					break;
				case StdTypes.T_LONG:
					switch (destBaseType)
					{
						case StdTypes.T_BYTE:
						case StdTypes.T_SHRT:
						case StdTypes.T_CHAR:
						case StdTypes.T_INT:
							return viceversa ? this : null;
						case StdTypes.T_LONG:
						case StdTypes.T_FLT:
						case StdTypes.T_DBL:
							return dest;
					}
					break;
				case StdTypes.T_FLT:
					switch (destBaseType)
					{
						case StdTypes.T_BYTE:
						case StdTypes.T_SHRT:
						case StdTypes.T_CHAR:
						case StdTypes.T_INT:
						case StdTypes.T_LONG:
							return viceversa ? this : null;
						case StdTypes.T_FLT:
						case StdTypes.T_DBL:
							return dest;
					}
					break;
				case StdTypes.T_DBL:
					switch (destBaseType)
					{
						case StdTypes.T_BYTE:
						case StdTypes.T_SHRT:
						case StdTypes.T_CHAR:
						case StdTypes.T_INT:
						case StdTypes.T_LONG:
						case StdTypes.T_FLT:
							return viceversa ? this : null;
						case StdTypes.T_DBL:
							return dest;
					}
					break;
			}
		return null;
	}
	
	public int getRegType(Context ctx)
	{
		//reserved or void? -> no pointer
		if (baseType == T_RES || baseType == T_VOID)
			return 0;
		//array? -> normal pointer
		if (arrDim > 0)
			return StdTypes.T_PTR;
		//standard-type? -> pointer only if resolved null-type
		if (baseType != T_QID)
		{
			if (baseType == T_NNPT)
				return StdTypes.T_PTR;
			if (baseType == T_NDPT)
				return StdTypes.T_DPTR;
			if (baseType == T_NULL)
			{
				compErr(ctx, "unresolved null-type in TypeRef.getRegType");
				return 0;
			}
			return baseType;
		}
		//get size of QID-type
		if (qid.unitDest == null)
		{
			compErr(ctx, "unresolved type in TypeRef.getRegType");
			return 0;
		}
		if ((qid.unitDest.modifier & Modifier.M_INDIR) != 0)
			return StdTypes.T_DPTR;
		return StdTypes.T_PTR;
	}
	
	public void getTypeOf(TypeRef from)
	{
		if (from == null)
			baseType = T_VOID; //void for constructors
		else
		{
			baseType = from.baseType;
			qid = from.qid;
			arrDim = from.arrDim;
			typeSpecial = from.typeSpecial;
		}
	}
	
	public char getSig()
	{
		if (baseType == T_VOID)
			return 'v';
		if (arrDim != 0)
			return 'a';
		switch (baseType)
		{
			case T_BOOL:
				return 'b';
			case T_BYTE:
			case T_SHRT:
			case T_CHAR:
			case T_INT:
				return 'i';
			case T_LONG:
				return 'l';
			case T_FLT:
				return 'f';
			case T_DBL:
				return 'd';
		}
		return 'o';
	}
	
	public static int getMinSize(int std)
	{
		switch (std)
		{
			case T_BYTE:
			case T_BOOL:
				return 1;
			case T_SHRT:
			case T_CHAR:
				return 2;
			case T_INT:
			case T_FLT:
				return 4;
			case T_LONG:
			case T_DBL:
				return 8;
		}
		return 0; //this is an error
	}
	
	public int getBaseTypeConvertible()
	{
		if (arrDim == 0)
			switch (baseType)
			{
				case T_BYTE:
				case T_SHRT:
				case T_CHAR:
				case T_INT:
				case T_LONG:
				case T_FLT:
				case T_DBL:
					return baseType;
			}
		return 0;
	}
	
	public boolean isBoolType()
	{
		return baseType == T_BOOL && arrDim == 0;
	}
	
	public boolean isShortType()
	{
		return baseType == T_SHRT && arrDim == 0;
	}
	
	public boolean isIntType()
	{
		return baseType == T_INT && arrDim == 0;
	}
	
	public boolean isLongType()
	{
		return baseType == T_LONG && arrDim == 0;
	}
	
	public boolean isNullType()
	{
		return (baseType == T_NULL || baseType == T_NNPT || baseType == T_NDPT);
	}
	
	public boolean isObjType()
	{
		return baseType == T_NNPT || arrDim != 0 || (baseType != T_VOID && (qid != null && ((qid.unitDest.modifier & (Modifier.M_STRUCT | Modifier.M_INDIR)) == 0)));
	}
	
	public boolean isIntfType()
	{
		return baseType == T_NDPT || (arrDim == 0 && qid != null && (qid.unitDest.modifier & Modifier.M_INDIR) != 0);
	}
	
	public boolean isStructType()
	{
		return (qid != null && (qid.unitDest.modifier & Modifier.M_STRUCT) != 0);
	}
	
	public boolean isFlashType()
	{
		return (qid != null && (qid.unitDest.modifier & Modifier.MM_FLASH) != 0);
	}
	
	public boolean isThrowableType(Context ctx)
	{
		return (qid != null && arrDim == 0 && ctx.excThrowable != null && ctx.excThrowable.isParent(qid.unitDest, ctx));
	}
	
	public boolean isCheckedExceptionType(Context ctx)
	{
		return (qid != null && arrDim == 0 && ctx.excChecked != null && ctx.excChecked.isParent(qid.unitDest, ctx));
	}
}
