package java.lang;

import rte.SClassDesc;

/**
 * @author Konloch
 * @since 7/17/2024
 */
public class Object
{
	public SClassDesc _r_type; //fixed by compiler: reloc-position 0
	
	/**
	 * This is meant to be left up to the developers to implement for each use-case.
	 *
	 * @return The hashcode representation of the object variables values
	 */
	public int hashCode()
	{
		return 0;
	}
	
	/**
	 * Equals comparision which should be implemented by the developers when you have variables that can be compared.
	 *
	 * @param o Any object to compare against
	 * @return True or false if the object equals this class, either by direct comparison or variable contents
	 */
	public boolean equals(Object o)
	{
		return this == o;
	}
	
	/**
	 * Return the contents of the object as a String
	 *
	 * @return The class-name along with any variables the developers decided to include
	 */
	public String toString()
	{
		return "Object";
	}
}