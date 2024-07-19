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

package sjc.output;

import sjc.memory.ImageContainer;
import sjc.osio.BinWriter;
import sjc.osio.OsIO;
import sjc.osio.TextPrinter;

/**
 * RawOut: just write the created image
 *
 * @author S. Frenz
 * @version 120227 cleaned up "package sjc." typo
 * version 110609 added option to change output filename
 * version 090303 adopted changed osio package structure
 * version 090207 added copyright notice
 * version 070105 removed prefixing
 * version 060818 adapted changed interface
 * version 060607 initial version
 */

public class RawOut extends OutputFormat
{
	private final static String DEF_FNAME = "raw_out.bin";
	
	private String fname;
	private BinWriter bin;
	private TextPrinter out;
	
	public static void printValidParameters(TextPrinter v)
	{
		v.println("   FILE - write to FILE instead of ");
	}
	
	public boolean setParameter(String parm, TextPrinter v)
	{
		if (fname != null)
		{
			v.println("filename already set for RawOut");
			return false;
		}
		fname = parm;
		return true;
	}
	
	public boolean checkParameter(OsIO iO, TextPrinter errOut)
	{
		//nothing to check, just remember
		bin = iO.getNewBinWriter();
		out = errOut;
		return true;
	}
	
	public boolean writeOutput(ImageContainer img, ImageContainer cimg, int cilen)
	{
		boolean res;
		
		if (fname == null)
			fname = DEF_FNAME;
		
		if (!bin.open(fname))
		{
			out.print("Error opening output-file ");
			out.println(fname);
			return false;
		}
		res = img.appendImage(bin);
		bin.close();
		return res;
	}
}
