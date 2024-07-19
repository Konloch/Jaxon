/* Copyright (C) 2009, 2010, 2011, 2012 Patrick Schmidt, Stefan Frenz
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

package sjc.osio.sun;

import sjc.compbase.*;
import sjc.osio.TextPrinter;
import sjc.symbols.SymbolInformer;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.StringTokenizer;

/**
 * ReflectionSymbols: Class converting the full Sun-Java-object parsetree into SJC-objects
 *
 * @author P. Schmidt, S. Frenz
 * @version 120305 removed unused variable: converted
 * version 110605 added allowParents option
 * version 110512 added skipSJC option
 * version 110412 fixed names after migration to sjc package
 * version 110411 added check if unit not found, added support for renamed BootStrap-class
 * version 101226 adopted changed SymbolInformer
 * version 100916 added parameter for reduced symbol generation
 * version 100826 redesign
 * version 100412 made use of generic utility classes of jdk1.5
 * version 100215 replaced jdk1.6 methods by jdk1.4 correspondants
 * version 091124 added check for null-pointer for relation-information
 * version 091021 adopted changed modifier declarations
 * version 091020 fixed comment
 * version 091009 initial version
 */

public class ReflectionSymbols extends SymbolInformer
{
	
	public String getName()
	{
		return "ReflectionSymbols";
	}
	
	/**
	 * Class-names of "interest"
	 */
	private static final String CLSS_OUTPUTLOCATION = "sjc.memory.ImageContainer.OutputLocation";
	private static final String CLSS_OBJECT = "java.lang.Object";
	private static final String CLSS_STRING = "java.lang.String";
	private static final String DEFAULT_CLSS_BOOTSTRAP = "symbols.BootStrap";
	private static final String CLSS_MARKER = "Mthd";
	
	private String clssBootstrap = DEFAULT_CLSS_BOOTSTRAP;
	
	/**
	 * Instance types which should be excluded from conversion
	 */
	private static final String[] CLSS_EXCLUDE = {"sjc.frontend.sjava.StBlock"};
	
	/**
	 * Variable-names of "interest"
	 */
	private static final String VAR_ROOT = "root";
	private static final String VAR_MARKER = "marker";
	
	/**
	 * Listelement for the hashtable varToPos in the class Transformer
	 *
	 * @author patrick
	 */
	private class VrblOffset
	{
		
		/**
		 * The offset of the variable in the instance
		 */
		private final int offset;
		
		/**
		 * Flag determining whether this variable is indirect
		 */
		private final boolean isIndirect;
		
		/**
		 * Standard constructor setting the values
		 *
		 * @param off   the offset of the variable
		 * @param indir flag determining whether this variable is indirect
		 */
		public VrblOffset(int off, boolean indir)
		{
			offset = off;
			isIndirect = indir;
		}
	}
	
	/**
	 * List element for fixing up missing references
	 *
	 * @author patrick
	 */
	private class FixupElement
	{
		
		/**
		 * The Sun-Java-instance which was converted into a SJC-instance
		 */
		private final Object instance;
		
		/**
		 * The address in the image where to insert the reference to the
		 * corresponding SJC-instance
		 */
		private final Object location;
		
		/**
		 * The offset to the location where to insert the reference
		 */
		private final int offset;
		
		/**
		 * Standard constructor setting the instance fields
		 *
		 * @param instance the Sun-Java-instance which was transformed
		 * @param address  the address in the image where to insert the reference
		 */
		public FixupElement(Object instance, Object location, int offset)
		{
			this.instance = instance;
			this.location = location;
			this.offset = offset;
		}
	}
	
	/**
	 * Class converting a Sun-Java-instance into a SJC-instance
	 *
	 * @author patrick
	 */
	private class Converter
	{
		
		/**
		 * Constants for the primitive data types
		 */
		private static final String INT = "int";
		private static final String SHORT = "short";
		private static final String CHAR = "char";
		private static final String LONG = "long";
		private static final String BYTE = "byte";
		private static final String BOOLEAN = "boolean";
		
		/**
		 * Instance of the Java-Reflection-API class type
		 */
		protected Class<?> sunClass;
		
		/**
		 * The corresponding instance of unit in the parse tree representing the
		 * SJC-structure of this class
		 */
		protected Unit sjcClass;
		
		/**
		 * Hashtable providing a mapping between variable-name and its SJC-offset
		 */
		private final Hashtable<String, VrblOffset> varToPos;
		
		private boolean hasMarker;
		
		/**
		 * Standard constructor
		 *
		 * @param clss     the instance of the Java-Reflection-API class type
		 * @param sjcClass the corresponding instance of Unit in the parse tree
		 * @throws Exception in case of invalid parameters
		 */
		private Converter(Class<?> clss, Unit sjcClass, Unit javaLangObject)
		{
			Unit unit;
			Vrbl vars;
			sunClass = clss;
			varToPos = new Hashtable<String, VrblOffset>();
			this.sjcClass = sjcClass;
			unit = sjcClass;
			//determine all offsets of this object including superclass up to Object
			while (unit != null && unit != javaLangObject)
			{
				vars = unit.vars;
				while (vars != null)
				{
					if (unit.name.equals(CLSS_MARKER) && vars.name.equals(VAR_MARKER))
						hasMarker = true;
					if ((vars.modifier & Modifier.M_STAT) == 0)
						varToPos.put(vars.name, new VrblOffset(vars.relOff, vars.location == AccVar.L_INSTIDS));
					vars = vars.nextVrbl;
				}
				unit = (unit.extsID != null ? unit.extsID.unitDest : null);
			}
		}
		
		/**
		 * Method to determine the dimension of a Sun-java-Array by the given
		 * type-name
		 *
		 * @param javaArray the type of the array in Java-Reflection-style, that
		 *                  is ([){B, S, I, J, Z}
		 * @return the dimension of the array with the given type
		 */
		private int arrDim(String javaArray)
		{
			int len, result = 0;
			len = javaArray.length();
			for (int i = 0; i < len; i++)
				if (javaArray.charAt(i) == '[')
					result++;
			return result;
		}
		
		/**
		 * Method to obtain the array-type of a SunJava-array in SJC-fashion
		 *
		 * @param javaArray the type-name of the SunJava-array
		 * @return the type in SJC-fashion
		 */
		private int getArrayType(String javaArray)
		{
			switch (javaArray.charAt(javaArray.length() - 1))
			{
				case 'B':
					return StdTypes.T_BYTE;
				case 'S':
					return StdTypes.T_SHRT;
				case 'I':
					return StdTypes.T_INT;
				case 'C':
					return StdTypes.T_CHAR;
				case 'J':
					return StdTypes.T_LONG;
				case 'Z':
					return StdTypes.T_BOOL;
			}
			if (javaArray.indexOf("[L") != -1)
				return StdTypes.T_PTR;
			return -1;
		}
		
		/**
		 * Method to determine, whether this instance can be omitted from conversion
		 *
		 * @param instance the SunJava-instance
		 * @return true, if it can be omitted, false otherwise
		 */
		private boolean isExcludeable(Object instance)
		{
			if (instance == null)
				return true;
			if (fullBlown)
				return false;
			String className = instance.getClass().getCanonicalName();
			for (int i = 0; i < CLSS_EXCLUDE.length; i++)
				if (className.equals(CLSS_EXCLUDE[i]))
					return true;
			if (skipSJC)
			{
				if (instance instanceof Pack && isInSJCPack((Pack) instance))
					return true;
				return instance instanceof Unit && isInSJCPack(((Unit) instance).pack.packDest);
			}
			return false;
		}
		
		/**
		 * Method to determine, whether this package is *below* the sjc package
		 *
		 * @param p the package
		 * @return true, if the package is below the sjc package, false otherwise
		 */
		private boolean isInSJCPack(Pack p)
		{
			if (p == null || p.name.equals("sjc"))
				return false;
			while (p.outer != null && p.outer.name != null)
				p = p.outer;
			return p != null && p.name != null && p.name.equals("sjc");
		}
		
		/**
		 * Method to create a SJC-array
		 *
		 * @param arrayRef the Sun-Java-array to be converted
		 * @return a memory-interface-corresponding output location of the sjc-array
		 */
		private Object createSJCArray(Object arrayRef)
		{
			if (sjcClass.inlArr != null)
			{
				ctx.out.println("inline-arrays currently not supported in symbol generator");
				ctx.err = true;
				return null;
			}
			int arrayLength = Array.getLength(arrayRef), arrayType;
			String arrayTypeName = arrayRef.getClass().getName();
			Object outputLocation, indirOutputLocation;
			//handle 1-dimensional arrays
			if (arrDim(arrayTypeName) != 1)
			{
				ctx.out.println("multi-dimensional arrays currently not supported");
				ctx.err = true;
				return null;
			}
			switch (arrayType = getArrayType(arrayTypeName))
			{
				case StdTypes.T_BYTE:
					outputLocation = ctx.mem.allocateArray(arrayLength, 1, 1, arrayType, null);
					//indirect address needed in case of indir-objects
					indirOutputLocation = ctx.mem.getIndirScalarObject(outputLocation);
					//set data
					for (int i = 0; i < arrayLength; i++)
					{
						if (ctx.dynaMem)
							ctx.mem.putByte(indirOutputLocation, sarray.instIndirScalarTableSize + i, Array.getByte(arrayRef, i));
						else
							ctx.mem.putByte(outputLocation, sarray.instScalarTableSize + i, Array.getByte(arrayRef, i));
					}
					break;
				case StdTypes.T_SHRT:
				case StdTypes.T_CHAR:
					outputLocation = ctx.mem.allocateArray(arrayLength, 1, 2, arrayType, null);
					//indirect address needed in case of indir-objects
					indirOutputLocation = ctx.mem.getIndirScalarObject(outputLocation);
					//set data
					for (int i = 0; i < arrayLength; i++)
					{
						if (ctx.dynaMem)
							ctx.mem.putShort(indirOutputLocation, sarray.instIndirScalarTableSize + (i << 1), Array.getShort(arrayRef, i));
						else
							ctx.mem.putShort(outputLocation, sarray.instScalarTableSize + (i << 1), Array.getShort(arrayRef, i));
					}
					break;
				case StdTypes.T_INT:
					outputLocation = ctx.mem.allocateArray(arrayLength, 1, 4, arrayType, null);
					//indirect address needed in case of indir-objects
					indirOutputLocation = ctx.mem.getIndirScalarObject(outputLocation);
					//set data
					for (int i = 0; i < arrayLength; i++)
					{
						if (ctx.dynaMem)
							ctx.mem.putInt(indirOutputLocation, sarray.instIndirScalarTableSize + (i << 2), Array.getInt(arrayRef, i));
						else
							ctx.mem.putInt(outputLocation, sarray.instScalarTableSize + (i << 2), Array.getInt(arrayRef, i));
					}
					break;
				case StdTypes.T_LONG:
					outputLocation = ctx.mem.allocateArray(arrayLength, 1, 8, arrayType, null);
					//indirect address needed in case of indir-objects
					indirOutputLocation = ctx.mem.getIndirScalarObject(outputLocation);
					//set data
					for (int i = 0; i < arrayLength; i++)
					{
						if (ctx.dynaMem)
							ctx.mem.putLong(indirOutputLocation, sarray.instIndirScalarTableSize + (i << 3), Array.getLong(arrayRef, i));
						else
							ctx.mem.putLong(outputLocation, sarray.instScalarTableSize + (i << 3), Array.getLong(arrayRef, i));
					}
					break;
				case StdTypes.T_BOOL:
					outputLocation = ctx.mem.allocateArray(arrayLength, 1, 1, arrayType, null);
					//indirect address needed in case of indir-objects
					indirOutputLocation = ctx.mem.getIndirScalarObject(outputLocation);
					//set data
					for (int i = 0; i < arrayLength; i++)
					{
						if (ctx.dynaMem)
							ctx.mem.putByte(indirOutputLocation, sarray.instIndirScalarTableSize + i, Array.getBoolean(arrayRef, i) ? (byte) 1 : (byte) 0);
						else
							ctx.mem.putByte(outputLocation, sarray.instScalarTableSize + i, Array.getBoolean(arrayRef, i) ? (byte) 1 : (byte) 0);
					}
					break;
				case StdTypes.T_PTR:
					Object arrayItemLocation, arrElement;
					Unit sjcType;
					//get the type of the array
					arrayTypeName = arrayTypeName.substring(arrayTypeName.indexOf("[L") + 2, arrayTypeName.length() - 1);
					if ((sjcType = findUnitByName(arrayTypeName)) == null)
					{
						ctx.out.println("couldn't find corresponding Unit for array type");
						ctx.err = true;
						return null;
					}
					outputLocation = ctx.mem.allocateArray(arrayLength, 1, -1, 0, sjcType.outputLocation);
					for (int i = 0; i < arrayLength; i++)
					{
						arrElement = Array.get(arrayRef, i);
						if (arrElement != null)
						{
							//look whether this instance has already been created
							if ((arrayItemLocation = createdInstances.get(arrElement)) != null)
								ctx.arch.putRef(outputLocation, -((sarray.instRelocTableEntries + i + 1) * ctx.arch.relocBytes), arrayItemLocation, 0);
								//check for strings
							else if (arrElement instanceof String)
							{
								Object strAddr = ctx.allocateString((String) arrElement);
								createdInstances.put(arrElement, strAddr);
							}
							else if (isExcludeable(arrElement))
								ctx.arch.putRef(outputLocation, -((sarray.instRelocTableEntries + i + 1) * ctx.arch.relocBytes), null, 0);
							else if (arrElement.getClass().getCanonicalName().equals(CLSS_OUTPUTLOCATION))
								ctx.arch.putRef(outputLocation, -((sarray.instRelocTableEntries + i + 1) * ctx.arch.relocBytes), arrElement, 0);
							else
							{
								if (!toDo.contains(arrElement))
									toDo.add(arrElement);
								fixup.add(new FixupElement(arrElement, outputLocation, -((sarray.instRelocTableEntries + i + 1) * ctx.arch.relocBytes)));
							}
						}
					}
					break;
				default:
					ctx.out.println("unknown primitve type in array conversion");
					ctx.err = true;
					return null;
			}
			if (outputLocation != null)
				createdInstances.put(arrayRef, outputLocation);
			else
			{
				ctx.out.println("allocation of memory for array failed");
				ctx.err = true;
				return null;
			}
			return outputLocation;
		}
		
		/**
		 * Method translating the given Sun-Java-instance into a SJC-instance in a
		 * given memory image
		 *
		 * @param instance the instance to be translated
		 * @return true, if the conversion was successful, false otherwise
		 */
		public boolean createSJCInstance(Object instance)
		{
			Field[] fields;
			Class<?> clss = instance.getClass();
			String fieldName, fieldType;
			int i, offset;
			Object location, targetAddr, indirAddr;
			Object listAccess, value;
			VrblOffset variable;
			//check whether this instance has already been transformed
			if (createdInstances.get(instance) != null)
				return true;
			if (instance.getClass() != sunClass)
			{
				ctx.out.println("instance type and transformer type do not match");
				return false;
			}
			//allocate empty object
			location = ctx.mem.allocate(sjcClass.instScalarTableSize, sjcClass.instIndirScalarTableSize, sjcClass.instRelocTableEntries, sjcClass.outputLocation);
			//add the sjc instance to the created instances hashtable
			if (location == null)
			{
				ctx.out.println("not enough memory for conversion");
				ctx.err = true;
				return false;
			}
			createdInstances.put(instance, location);
			indirAddr = ctx.mem.getIndirScalarObject(location);
			//read out all elements of the java-instance up to the root object
			while (!clss.getCanonicalName().equals(CLSS_OBJECT))
			{
				fields = clss.getDeclaredFields();
				for (i = 0; i < fields.length; i++)
				{
					//examine just instance fields
					if ((fields[i].getModifiers() & java.lang.reflect.Modifier.STATIC) == 0)
					{
						fields[i].setAccessible(true);
						fieldName = fields[i].getName();
						fieldType = fields[i].getType().getCanonicalName();
						//other variable managed in normal way
						if ((variable = varToPos.get(fieldName)) != null)
						{
							//for each value, determine the offset of the SJC-instance
							offset = variable.offset;
							try
							{
								//handle base-types immediately
								if (fields[i].getType().isPrimitive())
								{
									//distinguish between indirect and direct scalars
									targetAddr = (ctx.indirScalars && variable.isIndirect) ? indirAddr : location;
									if (fieldType.equals(BYTE))
										ctx.mem.putByte(targetAddr, offset, fields[i].getByte(instance));
									else if (fieldType.equals(SHORT))
										ctx.mem.putShort(targetAddr, offset, fields[i].getShort(instance));
									else if (fieldType.equals(CHAR))
										ctx.mem.putShort(targetAddr, offset, (short) fields[i].getChar(instance));
									else if (fieldType.equals(INT))
									{
										int intValue = fields[i].getInt(instance);
										//changing marker-value to reduce amount of instances
										if (hasMarker && fieldName.equals(VAR_MARKER))
										{
											intValue &= ~Marks.K_FINL;
											intValue |= Marks.K_NINL;
										}
										ctx.mem.putInt(targetAddr, offset, intValue);
									}
									else if (fieldType.equals(LONG))
										ctx.mem.putLong(targetAddr, offset, fields[i].getLong(instance));
									else if (fieldType.equals(BOOLEAN))
										ctx.mem.putByte(targetAddr, offset, fields[i].getBoolean(instance) ? (byte) 1 : (byte) 0);
									else
										ctx.out.println("! unknown primitive type");
								}
								//reference handling
								else if ((value = fields[i].get(instance)) != null)
								{
									//try to obtain the sjc-instance, if already created
									if ((listAccess = createdInstances.get(value)) != null)
										ctx.arch.putRef(location, offset, listAccess, 0);
										//special handling for OutputLocation-instances
									else if (value.getClass().getCanonicalName().equals(CLSS_OUTPUTLOCATION))
									{
										ctx.arch.putRef(location, offset, value, 0);
									}
									//omit excludeable instances
									else if (isExcludeable(value))
										ctx.arch.putRef(location, offset, null, 0);
										//array creation
									else if (fields[i].getType().isArray())
									{
										if ((targetAddr = createSJCArray(value)) == null)
										{
											ctx.out.println("error during array creation");
											ctx.err = true;
											return false;
										}
										ctx.arch.putRef(location, offset, targetAddr, 0);
									}
									//String creation
									else if (fieldType.equals(CLSS_STRING))
									{
										targetAddr = ctx.allocateString((String) value);
										createdInstances.put(value, targetAddr);
										ctx.arch.putRef(location, offset, targetAddr, 0);
									}
									//otherwise add this instance to the toDo-list
									else
									{
										if (!toDo.contains(value))
											toDo.add(value);
										fixup.add(new FixupElement(value, location, offset));
									}
								}
							}
							catch (IllegalAccessException iax)
							{
								ctx.out.println("IllegalAccessException in Transformer.createSJCInstances");
								ctx.err = true;
								return false;
							}
						}
						else
						{
							if (!allowParents)
							{ //if parents are allowed, silently skip non-existing fields
								ctx.out.print("couldn't find variable with name ");
								ctx.out.print(fieldName);
								ctx.out.print(" in unit ");
								ctx.out.println(sjcClass.name);
								ctx.err = true;
								return false;
							}
						}
					}
				}
				clss = clss.getSuperclass();
			}
			//take care of the remaining references
			FixupElement elem;
			Iterator<FixupElement> it = fixup.iterator();
			while (it.hasNext())
			{
				elem = it.next();
				if (elem.instance == instance)
				{
					ctx.arch.putRef(elem.location, elem.offset, location, 0);
					fixup.remove(elem);
					it = fixup.iterator();
				}
			}
			return true;
		}
	}
	
	/**
	 * Hashtable tranlating between classname and the corresponding transformer
	 * instance
	 */
	private final Hashtable<String, Converter> transformers;
	
	/**
	 * Hashtable translating between Sun-Java-instance and the address of its
	 * corresponding sjc-instance in the image
	 */
	private final Hashtable<Object, Object> createdInstances;
	
	/**
	 * Collection containing the locations of references in the image which need
	 * to be updated and instances which still have to be converted
	 */
	private final LinkedList<FixupElement> fixup;
	private final LinkedList<Object> toDo;
	
	/**
	 * Unit of SJC-array for faster access
	 */
	private Unit sarray;
	
	/**
	 * Flag determining whether the SymbolGenerator converts the whole parse tree
	 * providing recompilation of classes contained in the image or whether the
	 * symbol generation stops at instances of frontend.sjava.StBlock
	 */
	private boolean fullBlown = false;
	
	/**
	 * Flag determining whether the SymbolGenerator stops conversion on packages
	 * and units below the sjc package
	 */
	private boolean skipSJC = false;
	
	/**
	 * Flag determining whether to be converted objects may be converted using
	 * their parents
	 */
	private boolean allowParents = false;
	
	/**
	 * Standard constructor initializing all Collections
	 */
	public ReflectionSymbols()
	{
		transformers = new Hashtable<String, Converter>();
		createdInstances = new Hashtable<Object, Object>();
		toDo = new LinkedList<Object>();
		fixup = new LinkedList<FixupElement>();
	}
	
	/**
	 * @see SymbolInformer#generateSymbols(UnitList, Context)
	 */
	public boolean generateSymbols(UnitList newUnits, Context ctx)
	{
		Converter converter;
		int instanceCount = 0;
		Unit targetUnit, javaLangObject;
		Vrbl targetVar;
		String className;
		Object currentInstance;
		resetLists();
		this.ctx = ctx;
		sarray = ctx.rteSArray;
		//obtain java.lang.Object
		if ((javaLangObject = findUnitByName(CLSS_OBJECT)) == null)
		{
			ctx.out.println("couldn't find java.lang.Object in SJC-parse-tree");
			ctx.err = true;
			return false;
		}
		//add root object
		toDo.add(ctx.root);
		//if relation data needed, add relations, too
		//    if (doRelation) {
		//      if (ctx.relations==null || ctx.relations.index==null)
		//        ctx.out.println("no relation information set - skipping (parameter \"-q\" provided in compilation?)");
		//      else toDo.add(ctx.relations.index);
		//    }
		while (!toDo.isEmpty())
		{
			instanceCount++;
			currentInstance = toDo.removeFirst();
			className = currentInstance.getClass().getCanonicalName();
			if ((converter = transformers.get(className)) == null)
			{
				if ((targetUnit = findUnitByName(className)) == null)
				{
					boolean found = false;
					if (allowParents)
					{
						Class<?> c = currentInstance.getClass();
						while (true)
						{
							c = c.getSuperclass();
							if (c == null)
								break;
							if ((targetUnit = findUnitByName(c.getCanonicalName())) != null)
							{
								found = true;
								break;
							}
						}
					}
					if (!found)
					{
						ctx.out.print("couldn't find corresponding SJC-unit for SunJava class ");
						ctx.out.println(className);
						ctx.err = true;
						return false;
					}
				}
				converter = new Converter(currentInstance.getClass(), targetUnit, javaLangObject);
				transformers.put(className, converter);
			}
			if (!converter.createSJCInstance(currentInstance))
			{
				ctx.out.println("error while converting SunJava instance into SJC-instance");
				ctx.err = true;
				return false;
			}
		}
		//check for missing fixups
		if (fixup.size() != 0)
		{
			ctx.out.println("There exist pending fixups (" + fixup.size() + ")");
			ctx.err = true;
			return false;
		}
		//tell statistics
		ctx.out.print(instanceCount);
		ctx.out.println(" instances traversed...");
		ctx.out.print("created ");
		ctx.out.print(transformers.size());
		ctx.out.println(" transformer...");
		//enter address of parse-tree in BootStrap.root
		if ((targetUnit = findUnitByName(clssBootstrap)) == null)
		{
			ctx.out.print("unit ");
			ctx.out.print(clssBootstrap);
			ctx.out.println(" required for ReflectionSymbols (non-abstract, non-indir, non-struct)");
			ctx.err = true;
			return false;
		}
		if ((targetVar = targetUnit.searchVariable(VAR_ROOT, ctx)) == null || (targetVar.modifier & (Modifier.M_FIN | Modifier.M_STAT | Modifier.M_STRUCT)) != Modifier.M_STAT || targetVar.type.baseType != TypeRef.T_QID || targetVar.type.arrDim != 0)
		{
			ctx.out.println("variable \"static Pack root\" needed in symbols.BootStrap");
			ctx.err = true;
			return false;
		}
		ctx.arch.putRef(targetUnit.outputLocation, targetVar.relOff, createdInstances.get(ctx.root), 0);
		return true;
	}
	
	/**
	 * @see SymbolInformer#setParameter(String, TextPrinter)
	 */
	public boolean setParameter(String parm, TextPrinter v)
	{
		if (parm.equals("full"))
			fullBlown = true;
		else if (parm.equals("skipSJC"))
			skipSJC = true;
		else if (parm.equals("allowParents"))
			allowParents = true;
		else
		{
			if (parm == null || parm.length() < 1)
			{
				v.println("invalid BootStrap class");
				return false;
			}
			v.print("setting BootStrap class to ");
			v.println(clssBootstrap = parm);
		}
		return true;
	}
	
	/**
	 * Method to reset the state of the SymbolInformer
	 */
	private void resetLists()
	{
		createdInstances.clear();
		transformers.clear();
		fixup.clear();
		toDo.clear();
	}
	
	/**
	 * Method search a unit with the given full-qualified name
	 *
	 * @param fullQualName the canonical name of the class to find
	 * @return the SJC-unit corresponding to the given name or null, if there is
	 * no such class
	 */
	private Unit findUnitByName(String fullQualName)
	{
		StringTokenizer tokenizer = new StringTokenizer(fullQualName, ".");
		Pack pack = ctx.root;
		Unit u = null;
		//step down in the SJC type hierarchy
		while (tokenizer.hasMoreTokens())
		{
			String name = tokenizer.nextToken();
			if (u != null)
			{ //already found outer unit, search within
				Unit inner = u.innerUnits;
				while (inner != null)
				{
					if (inner.name.equals(name))
						break;
					inner = inner.nextUnit;
				}
				if (inner == null)
					return null; //did not find inner unit
				u = inner;
			}
			else if ((u = pack.searchUnit(name)) == null && (pack = pack.searchSubPackage(name)) == null)
				return null; //did not find unit or pack within pack
		}
		return u;
	}
}
