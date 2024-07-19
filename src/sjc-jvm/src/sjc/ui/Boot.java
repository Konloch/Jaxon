/* Copyright (C) 2006, 2007, 2008, 2009, 2010, 2012, 2016 Stefan Frenz
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

package sjc.ui;

import sjc.memory.BootableImage;
import sjc.osio.TextPrinter;
import sjc.osio.sun.SunOS;
import sjc.output.BootOut;

/**
 * Boot: make a raw_out-file bootable
 *
 * @author S. Frenz
 * @version 160216 added unit- and code-address initialization
 * version 120227 cleaned up "package sjc." typo
 * version 100422 adopted changes SunOS
 * version 091111 added error handling
 * version 090730 removed not needed methods
 * version 090303 adopted changed osio package structure
 * version 090207 added copyright notice
 * version 080712 adopted changed compile-interface
 * version 060818 adapted changed interface of OutputFormat
 * version 060817 added version history
 */

public class Boot
{
	private static class RawContainer extends BootableImage
	{
		public boolean init(byte[] data)
		{
			if ((memBlock = data) == null)
				return false; //get binary data
			if ((memBlockLen = memBlock.length) != readInt(4))
				return false; //get and check length
			if ((readInt(28) & 0xFF0000FF) != 0x550000AA)
				return false; //check endianess
			baseAddress = readInt(0); //get image base address
			startUnit = readInt(8); //get start unit address
			startCode = readInt(12); //get start of code
			return true;
		}
		
		private int readInt(int off)
		{
			return ((int) memBlock[off] & 0xFF) | (((int) memBlock[off + 1] & 0xFF) << 8) | (((int) memBlock[off + 2] & 0xFF) << 16) | (((int) memBlock[off + 3] & 0xFF) << 24);
		}
	}
	
	public static void main(String[] args)
	{
		RawContainer img;
		BootOut bo;
		SunOS os = new SunOS(System.out);
		TextPrinter v = os.getNewFilePrinter(null);
		
		if (!(img = new RawContainer()).init(os.readFile("raw_out.bin")))
		{
			v.println("error initializing from raw image");
			return;
		}
		bo = new BootOut();
		boolean success = true;
		if (args != null)
			for (int i = 0; i < args.length; i++)
				success &= bo.setParameter(args[i], v);
		if (success && bo.checkParameter(os, v) && bo.writeOutput(img, null, 0))
			v.println("Image created");
		else
			v.println("image not created");
	}
}
