package java.lang;

import kernel.Kernel;

public class Math
{
	public static final float E = 2.71828182845f;
	public static final float PI = 3.1415926535f;
	public static final float TWO_PI = 2 * PI;
	
	public static int Pow(int base, int exp)
	{
		int result = 1;
		for (int i = 0; i < exp; i++)
		{
			result *= base;
		}
		return result;
	}
	
	public static int Abs(int n)
	{
		return n < 0 ? -n : n;
	}
	
	public static double Abs(double n)
	{
		return n < 0 ? -n : n;
	}
	
	public static long Abs(long n)
	{
		return n < 0 ? -n : n;
	}
	
	public static int Min(int a, int b)
	{
		return a < b ? a : b;
	}
	
	public static int Max(int a, int b)
	{
		return a > b ? a : b;
	}
	
	public static double Max(double a, double b)
	{
		return a > b ? a : b;
	}
	
	public static int Clamp(int n, int min, int max)
	{
		return n < min ? min : n > max ? max : n;
	}
	
	public static int Compress(int n, int min, int max, int newMin, int newMax)
	{
		return (n - min) * (newMax - newMin) / (max - min) + newMin;
	}
	
	// Returns -1 if n is negative, 1 if n is positive, and 0 if n is zero
	public static int Sign(int n)
	{
		if (n < 0)
		{
			return -1;
		}
		else if (n > 0)
		{
			return 1;
		}
		else
		{
			return 0;
		}
	}
	
	/**
	 * Returns the product of the arguments,
	 * throwing an exception if the result overflows an {@code int}.
	 *
	 * @param x the first value
	 * @param y the second value
	 * @return the result
	 */
	public static int MultiplyExact(int x, int y)
	{
		long r = (long) x * (long) y;
		if ((int) r != r)
		{
			Kernel.panic("integer overflow");
		}
		return (int) r;
	}
	
	public static double Sin_Slow(double n)
	{
		final double my_pi = 3.14159265358979323;
		n = Fmod(n, 2 * my_pi);
		if (n < 0)
		{
			n = 2 * my_pi - n;
		}
		int sign = 1;
		if (n > my_pi)
		{
			n -= my_pi;
			sign = -1;
		}
		double result = n;
		double coefficent = 3;
		for (int i = 0; i < 10; i++)
		{
			double pow = power(n, coefficent);
			double frac = factorial(coefficent);
			if (i % 2 == 0)
			{
				result = result - (pow / frac);
			}
			else
			{
				result = result + (pow / frac);
			}
			coefficent = coefficent + 2;
		}
		
		return sign * n;
	}
	
	@SJC.Inline
	public static double Cos_Slow(double n)
	{
		return Sin_Slow(n + 1.57079632679489662);
	}
	
	public static double Fmod(double a, double b)
	{
		double frac = a / b;
		int floor = frac > 0 ? (int) frac : (int) (frac - 0.9999999999999);
		return (a - b * floor);
	}
	
	public static double factorial(double n)
	{
		if (n == 0)
		{
			return 1.0;
		}
		return n * (factorial(n - 1));
	}
	
	public static double power(double n, double power)
	{
		double result = n;
		for (int i = 1; i < power; i++)
		{
			result = n * result;
		}
		return result;
	}
	
	@SJC.Inline
	public static double sqrt(double nr)
	{
		// fld qword [ebp+8]
		MAGIC.inline(0xDD, 0x45);
		MAGIC.inlineOffset(1, nr);
		// fsqrt
		MAGIC.inline(0xD9, 0xFA);
		// fstp qword [ebp+8]
		MAGIC.inline(0xDD, 0x5D);
		MAGIC.inlineOffset(1, nr);
		return nr;
	}
	
	@SJC.Inline
	public static float Sqrt(float nr)
	{
		// fld dword [ebp+8]
		MAGIC.inline(0xD9, 0x45);
		MAGIC.inlineOffset(1, nr);
		// fsqrt
		MAGIC.inline(0xD9, 0xFA);
		// fstp dword [ebp+8]
		MAGIC.inline(0xD9, 0x5D);
		MAGIC.inlineOffset(1, nr);
		return nr;
	}
	
	public static double exp(double nr)
	{
		int dummy = 0; // holds FPU state
		MAGIC.ignore(dummy);
		MAGIC.inline(0xD9, 0x7D);
		MAGIC.inlineOffset(1, dummy, 2); // fnstcw word [ebp-2]
		MAGIC.inline(0xD9, 0x7D);
		MAGIC.inlineOffset(1, dummy, 0); // fnstcw word [ebp-4]
		MAGIC.inline(0x80, 0x4D);
		MAGIC.inlineOffset(1, dummy, 1);
		MAGIC.inline(0x0B); // or byte [ebp-3],0x0B
		MAGIC.inline(0xD9, 0x6D);
		MAGIC.inlineOffset(1, dummy); // fldcw word [ebp-4]
		MAGIC.inline(0xDD, 0x45);
		MAGIC.inlineOffset(1, nr); // fld qword [ebp+8]
		MAGIC.inline(0xD9, 0xEA); // fldl2e
		MAGIC.inline(0xDE, 0xC9); // fmulp st(1),st
		MAGIC.inline(0xD9, 0xC0); // fld st(0)
		MAGIC.inline(0xD9, 0xFC); // frndint
		MAGIC.inline(0xD9, 0xC9); // fxch st(1)
		MAGIC.inline(0xD8, 0xE1); // fsub st,st(1)
		MAGIC.inline(0xD9, 0xF0); // f2xm1
		MAGIC.inline(0xD9, 0xE8); // fld1
		MAGIC.inline(0xDE, 0xC1); // faddp st(1),st
		MAGIC.inline(0xD9, 0xC9); // fxch st(1)
		MAGIC.inline(0xD9, 0xE8); // fld1
		MAGIC.inline(0xD9, 0xFD); // fscale
		MAGIC.inline(0xDD, 0xD9); // fstp st(1)
		MAGIC.inline(0xDE, 0xC9); // fmulp st(1),st
		MAGIC.inline(0xD9, 0x6D);
		MAGIC.inlineOffset(1, dummy, 2); // fldcw word [ebp-2]
		MAGIC.inline(0xDD, 0x5D);
		MAGIC.inlineOffset(1, nr); // fstp qword [ebp+8]
		return nr;
	}
	
	public static double ln(double nr)
	{
		MAGIC.inline(0xD9, 0xE8); // fld1
		MAGIC.inline(0xDD, 0x45);
		MAGIC.inlineOffset(1, nr); // fld qword [ebp+8]
		MAGIC.inline(0xD9, 0xF1); // fyl2x
		MAGIC.inline(0xD9, 0xEA); // fldl2e
		MAGIC.inline(0xDE, 0xF9); // fdivp
		MAGIC.inline(0xDD, 0x5D);
		MAGIC.inlineOffset(1, nr); // fstp qword [ebp+8]
		return nr;
	}
	
	@SJC.Inline
	public static double pow(double nr, double exp)
	{
		return exp(exp * ln(nr));
	}
	
	@SJC.Inline
	public static int floor(float nr)
	{
		return (int) nr;
	}
	
	@SJC.Inline
	public static double Sin(double nr)
	{
		// fld qword [ebp+8]
		MAGIC.inline(0xDD, 0x45);
		MAGIC.inlineOffset(1, nr);
		// fsin
		MAGIC.inline(0xD9, 0xFE);
		// fstp qword [ebp+8]
		MAGIC.inline(0xDD, 0x5D);
		MAGIC.inlineOffset(1, nr);
		return nr;
	}
	
	@SJC.Inline
	public static float Sin(float nr)
	{
		// fld dword [ebp+8]
		MAGIC.inline(0xD9, 0x45);
		MAGIC.inlineOffset(1, nr);
		// fsin
		MAGIC.inline(0xD9, 0xFE);
		// fstp dword [ebp+8]
		MAGIC.inline(0xD9, 0x5D);
		MAGIC.inlineOffset(1, nr);
		return nr;
	}
	
	@SJC.Inline
	public static double Cos(double nr)
	{
		// fld qword [ebp+8]
		MAGIC.inline(0xDD, 0x45);
		MAGIC.inlineOffset(1, nr);
		// fcos
		MAGIC.inline(0xD9, 0xFF);
		// fstp qword [ebp+8]
		MAGIC.inline(0xDD, 0x5D);
		MAGIC.inlineOffset(1, nr);
		return nr;
	}
	
	@SJC.Inline
	public static float Cos(float nr)
	{
		// fld dword [ebp+8]
		MAGIC.inline(0xD9, 0x45);
		MAGIC.inlineOffset(1, nr);
		// fcos
		MAGIC.inline(0xD9, 0xFF);
		// fstp dword [ebp+8]
		MAGIC.inline(0xD9, 0x5D);
		MAGIC.inlineOffset(1, nr);
		return nr;
	}
}
