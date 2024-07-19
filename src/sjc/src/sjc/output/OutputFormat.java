/* Copyright (C) 2005, 2006, 2007, 2008, 2009, 2012 Stefan Frenz
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
import sjc.osio.OsIO;
import sjc.osio.TextPrinter;

/**
 * OutputFormat: basic interface to access an outputformat
 *
 * @author S. Frenz
 * @version 120227 cleaned up "package sjc." typo
 * version 090303 adopted changed osio package structure
 * version 090207 added copyright notice
 * version 070105 removed prefixing
 * version 060808 changed signature to support CommBase
 * version 060613 adapted changed osio-package
 * version 060607 initial version
 */

public abstract class OutputFormat
{
	public abstract boolean setParameter(String parm, TextPrinter v);
	
	public abstract boolean checkParameter(OsIO comm, TextPrinter v);
	
	public abstract boolean writeOutput(ImageContainer img, ImageContainer compressedImg, int compressedImgOrigLen);
}
