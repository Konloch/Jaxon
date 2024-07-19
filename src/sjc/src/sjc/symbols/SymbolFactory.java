/* Copyright (C) 2007, 2008, 2009 Stefan Frenz
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

package sjc.symbols;

import sjc.osio.TextPrinter;

/**
 * SymbolAdmin: handling of symbol informer
 *
 * @author S. Frenz
 * @version 091123 removed BootStrapSymbols
 * version 091009 added support for os-dependent ReflectionSymbols
 * version 090207 added copyright notice
 * version 080118 added MthdSymbols
 * version 070727 added BootStrapSymbols
 * version 070714 added RTESymbols
 * version 070713 initial version
 */

public class SymbolFactory
{
	public static SymbolInformer preparedReflectionSymbols;
	
	public static void printKnownOutputFormats(TextPrinter v)
	{
		v.println(" raw   - symbol information in a byte stream");
		RawSymbols.printValidParameters(v);
		v.println(" rte   - glue symbol information to rte instances");
		RTESymbols.printValidParameters(v);
		v.println(" mthd  - glue method information to rte instances");
		MthdSymbols.printValidParameters(v);
		v.print(" refl  - provide full parse tree (RTE-dependent, state: ");
		v.println(preparedReflectionSymbols != null ? "available)" : "not supported)");
	}
	
	public static SymbolInformer getSymbolGenerator(String name)
	{
		if (name.equals("raw"))
			return new RawSymbols();
		if (name.equals("rte"))
			return new RTESymbols();
		if (name.equals("mthd"))
			return new MthdSymbols();
		if (name.equals("refl"))
			return preparedReflectionSymbols;
		return null;
	}
}
