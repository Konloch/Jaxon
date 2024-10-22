package java.lang;

/**
 * @author Konloch
 * @author S. Frenz
 * @since 7/21/2024
 */
public class Math
{
	public final static double PI = 3.141592653589793238;
	
	public static byte min(byte a, byte b)
	{
		return (a <= b) ? a : b;
	}
	
	public static short min(short a, short b)
	{
		return (a <= b) ? a : b;
	}
	
	public static char min(char a, char b)
	{
		return (a <= b) ? a : b;
	}
	
	public static int min(int a, int b)
	{
		return (a <= b) ? a : b;
	}
	
	public static long min(long a, long b)
	{
		return (a <= b) ? a : b;
	}
	
	public static double min(double a, double b)
	{
		return (a <= b) ? a : b;
	}
	
	public static float min(float a, float b)
	{
		return (a <= b) ? a : b;
	}
	
	public static byte max(byte a, byte b)
	{
		return (a >= b) ? a : b;
	}
	
	public static short max(short a, short b)
	{
		return (a >= b) ? a : b;
	}
	
	public static char max(char a, char b)
	{
		return (a >= b) ? a : b;
	}
	
	public static int max(int a, int b)
	{
		return (a >= b) ? a : b;
	}
	
	public static long max(long a, long b)
	{
		return (a >= b) ? a : b;
	}
	
	public static double max(double a, double b)
	{
		return (a >= b) ? a : b;
	}
	
	public static float max(float a, float b)
	{
		return (a >= b) ? a : b;
	}
	
	public static double tan(double nr)
	{
		MAGIC.inline(0xDD, 0x45);
		MAGIC.inlineOffset(1, nr); //fld qword [ebp+8]
		MAGIC.inline(0xD9, 0xF2);       //fptan (puts additional "1.0" onto the stack)
		MAGIC.inline(0xDD, 0x5D);
		MAGIC.inlineOffset(1, nr); //fstp qword [ebp+8] (remove 1.0)
		MAGIC.inline(0xDD, 0x5D);
		MAGIC.inlineOffset(1, nr); //fstp qword [ebp+8]
		return nr;
	}
	
	public static double sqrt(double nr)
	{
		MAGIC.inline(0xDD, 0x45);
		MAGIC.inlineOffset(1, nr); //fld qword [ebp+8]
		MAGIC.inline(0xD9, 0xFA);       //fsqrt
		MAGIC.inline(0xDD, 0x5D);
		MAGIC.inlineOffset(1, nr); //fstp qword [ebp+8]
		return nr;
	}
	
	public static double exp(double nr)
	{
		int dummy = 0; //holds FPU state
		MAGIC.ignore(dummy);
		MAGIC.inline(0xD9, 0x7D);
		MAGIC.inlineOffset(1, dummy, 2); //fnstcw word [ebp-2]
		MAGIC.inline(0xD9, 0x7D);
		MAGIC.inlineOffset(1, dummy, 0); //fnstcw word [ebp-4]
		MAGIC.inline(0x80, 0x4D);
		MAGIC.inlineOffset(1, dummy, 1);
		MAGIC.inline(0x0B); //or byte [ebp-3],0x0B
		MAGIC.inline(0xD9, 0x6D);
		MAGIC.inlineOffset(1, dummy); //fldcw word [ebp-4]
		MAGIC.inline(0xDD, 0x45);
		MAGIC.inlineOffset(1, nr); //fld qword [ebp+8]
		MAGIC.inline(0xD9, 0xEA);             //fldl2e
		MAGIC.inline(0xDE, 0xC9);             //fmulp st(1),st
		MAGIC.inline(0xD9, 0xC0);             //fld st(0)
		MAGIC.inline(0xD9, 0xFC);             //frndint
		MAGIC.inline(0xD9, 0xC9);             //fxch st(1)
		MAGIC.inline(0xD8, 0xE1);             //fsub st,st(1)
		MAGIC.inline(0xD9, 0xF0);             //f2xm1
		MAGIC.inline(0xD9, 0xE8);             //fld1
		MAGIC.inline(0xDE, 0xC1);             //faddp st(1),st
		MAGIC.inline(0xD9, 0xC9);             //fxch st(1)
		MAGIC.inline(0xD9, 0xE8);             //fld1
		MAGIC.inline(0xD9, 0xFD);             //fscale
		MAGIC.inline(0xDD, 0xD9);             //fstp st(1)
		MAGIC.inline(0xDE, 0xC9);             //fmulp st(1),st
		MAGIC.inline(0xD9, 0x6D);
		MAGIC.inlineOffset(1, dummy, 2); //fldcw word [ebp-2]
		MAGIC.inline(0xDD, 0x5D);
		MAGIC.inlineOffset(1, nr); //fstp qword [ebp+8]
		return nr;
	}
	
	public static double ln(double nr)
	{
		MAGIC.inline(0xD9, 0xE8);       //fld1
		MAGIC.inline(0xDD, 0x45);
		MAGIC.inlineOffset(1, nr); //fld qword [ebp+8]
		MAGIC.inline(0xD9, 0xF1);       //fyl2x
		MAGIC.inline(0xD9, 0xEA);       //fldl2e
		MAGIC.inline(0xDE, 0xF9);       //fdivp
		MAGIC.inline(0xDD, 0x5D);
		MAGIC.inlineOffset(1, nr); //fstp qword [ebp+8]
		return nr;
	}
	
	public static double pow(double nr, double exp)
	{
		return exp(exp * ln(nr));
	}
	
	public static double abs(double nr)
	{
		MAGIC.inline(0xDD, 0x45);
		MAGIC.inlineOffset(1, nr); //fld qword [ebp+8]
		MAGIC.inline(0xD9, 0xE1);       //fabs
		MAGIC.inline(0xDD, 0x5D);
		MAGIC.inlineOffset(1, nr); //fstp qword [ebp+8]
		return nr;
	}
}
