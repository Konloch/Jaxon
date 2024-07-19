/* Copyright (C) 2005, 2006, 2007, 2008, 2009, 2010, 2012, 2014 Stefan Frenz
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
 * AccVar: platform and language independent variable that may be accessed in any way
 *
 * @author S. Frenz
 * @version 140507 added support for MM_REFTOFLASH
 * version 120227 cleaned up "package sjc." typo
 * version 100504 added getInitExpr
 * version 100428 removed structAsReference case
 * version 100419 added checks for Flash-modifier
 * version 100114 reorganized constant object handling
 * version 091021 adopted changed modifier declarations
 * version 091001 adopted changed memory interface
 * version 090918 added support for package private
 * version 090718 added support for non-static final variables
 * version 090207 added copyright notice
 * version 080816 changed access to M_STRUCT via Modifier instead of Unit
 * version 080331 added L_STRUCTREF
 * version 071222 added checkNameAgainstUnits
 * version 070903 added checkNameAgainst*
 * version 070731 renamed id to name
 * version 070727 changed type of id from PureID to String
 * version 070628 adopted changes for "Expr extends TypeRef"
 * version 070526 added support for instance inline arrays
 * version 070303 added support for indirect movable scalars
 * version 070111 adapted change in printPos and compErr
 * version 070106 moved inlinedArraySize to TypeRef
 * version 061203 optimized calls to printPos and compErr
 * version 061109 added inlinedArraySize
 * version 061101 added support of variables in struct
 * version 061030 changed detection of indirectCall
 * version 060607 initial version
 */

public abstract class AccVar extends Token
{
	//valid locations
	public final static int L_UNIT = -3; //declared variable, needs checks
	public final static int L_CONSTTR = -2; //trying to resolve this constant at the moment
	public final static int L_CONSTDC = -1; //declared constant, needs checks and resolving
	public final static int L_NOTRDY = 0;
	public final static int L_CONST = 1; //resolved constant
	public final static int L_CLSSSCL = 2; //scalar inside class
	public final static int L_CLSSREL = 3; //reloc inside class
	public final static int L_INSTSCL = 4; //scalar inside instance
	public final static int L_INSTIDS = 5; //indirect accessed scalar inside instance
	public final static int L_INSTREL = 6; //reloc inside instance
	public final static int L_LOCAL = 7; //local variable of method
	public final static int L_PARAM = 8; //parameter of method
	public final static int L_STRUCT = 9; //variable used in struct
	public final static int L_STRUCTREF = 10; //variable used in struct point to another struct
	public final static int L_INLARR = 11; //variable is name of inline-array in instance
	
	//default relOff
	public final static int INV_RELOFF = 0x7FFFFFFF;
	
	//internal error strings
	private final static String NAMEOFVRBL = "name of variable ";
	private final static String ALREADYUSED = " already used";
	
	public String name;
	public TypeRef type;
	public Unit owner;
	//required fields for interface-checks, overloading and code-generation
	public int minSize, relOff, location; //varPos is offset for scalars and relocs
	public int modifier; //modifier of variable
	//note: for inline-arrays varPos contains offset of length field,
	//   the real array offset is defined through instScalarSize of containing unit
	
	public AccVar(int fid, int il, int ic)
	{
		super(fid, il, ic);
		relOff = INV_RELOFF;
		location = L_NOTRDY;
	}
	
	public boolean enterSize(int loc, Context ctx)
	{
		int mod;
		
		//check size of variable
		if (type.baseType != TypeRef.T_QID && type.arrDim == 0)
		{ //standard-type
			if ((minSize = TypeRef.getMinSize(type.baseType)) == 0)
			{
				compErr(ctx, "var ");
				ctx.out.print(name);
				ctx.out.print(" has illegal type");
				return false;
			}
			if ((modifier & (Modifier.MM_FLASH | Modifier.MM_REFTOFLASH)) != 0)
			{
				printPos(ctx, "flash-modifier invalid for base type var ");
				ctx.out.print(name);
				return false;
			}
		}
		else
		{ //reference, check size
			if ((modifier & (Modifier.MM_FLASH | Modifier.MM_REFTOFLASH)) != 0)
				type.typeSpecial = TypeRef.S_FLASHREF;
			if (type.arrDim > 0)
				minSize = -1; //array
			else
			{ //qualified identifier
				if (type.qid == null || type.qid.unitDest == null)
				{
					compErr(ctx, "qid of var ");
					ctx.out.print(name);
					ctx.out.print(" is null or not resolved");
					return false;
				}
				if (((mod = type.qid.unitDest.modifier) & Modifier.M_INDIR) != 0)
					minSize = -2;
				else if ((mod & Modifier.M_STRUCT) == 0)
					minSize = -1;
				else
					minSize = ctx.arch.relocBytes; //destination is struct and should not be treated as ref
			}
		}
		//if location in variable is known already, it is set here (otherwise method is overwritten)
		location = loc;
		//everything OK
		return true;
	}
	
	public Expr getInitExpr(Context ctx)
	{
		compErr(ctx, "invalid call to AccVar.getInitExpr");
		return null;
	}
	
	public ExConstInitObj getConstInitObj(Context ctx)
	{
		compErr(ctx, "invalid call to AccVar.getConstInitObj");
		return null;
	}
	
	public boolean checkNameAgainstVrbl(Vrbl ov, Context ctx)
	{
		while (ov != this && ov != null)
		{
			if (name.equals(ov.name))
			{
				printPos(ctx, NAMEOFVRBL);
				ctx.out.print(name);
				ctx.out.print(ALREADYUSED);
				return false;
			}
			ov = ov.nextVrbl;
		}
		//everything OK
		return true;
	}
	
	public boolean checkNameAgainstUnits(Unit ou, Context ctx)
	{
		while (ou != null)
		{
			if (name.equals(ou.name))
			{
				printPos(ctx, NAMEOFVRBL);
				ctx.out.print(name);
				ctx.out.print(ALREADYUSED);
				return false;
			}
			ou = ou.nextUnit;
		}
		//everything ok
		return true;
	}
	
	public boolean checkNameAgainstParam(Param op, Context ctx)
	{
		while (op != this && op != null)
		{
			if (name.equals(op.name))
			{
				printPos(ctx, NAMEOFVRBL);
				ctx.out.print(name);
				ctx.out.print(ALREADYUSED);
				return false;
			}
			op = op.nextParam;
		}
		//everything OK
		return true;
	}
}
