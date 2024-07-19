package kernel.linux;

public class XTextProperty extends STRUCT
{
	int stringAddr;    /* same as Property routines */
	int encoding;    /* prop type */
	int format;        /* prop data format: 8, 16, or 32 */
	int nitems;        /* number of data items in value */
}