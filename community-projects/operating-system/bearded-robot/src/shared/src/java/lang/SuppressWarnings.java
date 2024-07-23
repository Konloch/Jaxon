package java.lang;

/**
 * Specifies that the named warnings in the annotated element are to be suppressed.
 * should be suppressed.
 *
 * <p>
 * This interface is provided for compatibility reasons. For example
 * to simplify the use of code analysis functions in an IDE.
 * </p>
 */
@SJC.IgnoreUnit
public @interface SuppressWarnings
{
	String[] value();
}
