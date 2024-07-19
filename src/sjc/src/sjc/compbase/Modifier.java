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
 * Modifier: platform and language independent modifiers for units and methods
 *
 * @author S. Frenz
 * @version 140507 added MM_REFTOFLASH
 * version 120227 cleaned up "package sjc." typo
 * version 100512 moved MC_EXPCONV, M_MSWIN to Marks, reordered flags
 * version 100312 added M_FLASH
 * version 091112 added MC_EXPCONV
 * version 091026 added M_ERROR
 * version 091012 added comment, reordered MA_* modifier
 * version 090918 added M_PACP
 * version 090718 added MF_ISWRITTEN and MF_MAYBEWRITTEN
 * version 090218 added synchronized and volatile modifier flag
 * version 090207 added copyright notice
 * version 080706 added MA_INTFMD
 * version 080121 added transient modifier flag
 * version 080118 added annotation modifier flag
 * version 071001 added M_NATIVE and M_MSWIN
 * version 070527 added more specials
 * version 070521 added more specials
 * version 070509 added more specials
 * version 070114 added more specials
 * version 061228 added more specials
 * version 061211 added more specials
 * version 061030 added specials
 * version 060607 initial version
 */

public class Modifier
{
	//modifier order is important for visibility check, assumption: pub<prot<pacp<priv
	public final static int M_PUB = 0x00000001; //"public"
	public final static int M_PROT = 0x00000002; //"protected"
	public final static int M_PACP = 0x00000004; //package private (without modifier in java)
	public final static int M_PRIV = 0x00000008; //"private"
	public final static int M_FIN = 0x00000010; //"final"
	public final static int M_STAT = 0x00000020; //"static"
	public final static int M_ABSTR = 0x00000040; //"abstract"
	public final static int M_NAT = 0x00000080; //"native"
	public final static int M_TRANS = 0x00000200; //"transient"
	public final static int M_ANNO = 0x00000400; //"@" for annotation modifier
	public final static int M_SYNC = 0x00000800; //"synchronized"
	public final static int M_VOLAT = 0x00001000; //"volatile"
	//following: unit / variable specials
	public final static int M_INDIR = 0x00002000; //special mark for units: indirect call (i.e. interfaces)
	public final static int M_STRUCT = 0x00004000; //special mark for units: struct
	public final static int M_ARRLEN = 0x00008000; //special mark for vrbls and units: length of inline array
	public final static int MM_FLASH = 0x00010000; //special mark for vrbls: keep in flash
	public final static int MM_REFTOFLASH = 0x00020000; //special mark for vrbls: variable refers to object in flash
	//following: method specials
	public final static int M_EXINIT = 0x00040000; //special mark for mthds: contains explicit init
	public final static int M_OVERLD = 0x00080000; //special mark for mthds: is overloaded
	public final static int M_HSCALL = 0x00100000; //special mark for mthds: method contains call
	public final static int M_NDDESC = 0x00200000; //special mark for mthds: need descriptor entry beside normal mode
	public final static int M_NDCODE = 0x00400000; //special mark for mthds: generate code
	//following: access specials
	public final static int MA_ACCSSD = 0x00800000; //is accessed
	public final static int MA_PUB = 0x01000000; //access through "public" way
	public final static int MA_PROT = 0x02000000; //access through "protected" way
	public final static int MA_PACP = 0x04000000; //access through package private way
	public final static int MA_PRIV = 0x08000000; //access through "private" way
	public final static int MA_INTFMD = 0x10000000; //access through interface map
	//following: flow analysis
	public final static int MF_ISWRITTEN = 0x20000000; //variable is written
	public final static int MF_MAYBEWRITTEN = 0x40000000; //variable may be written
	//error flag
	public final static int M_ERROR = 0xFFFFFFFF; //modifier is invalid
}
