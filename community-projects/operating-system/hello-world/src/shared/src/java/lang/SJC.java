package java.lang;

@SJC.IgnoreUnit
public @interface SJC
{
	int enterCodeAddr() default 0;
	
	int offset() default 0;
	
	int count() default 0;
	
	@interface Ref {}
	
	@interface Flash {}
	
	@interface Interrupt {}
	
	@interface Inline {}
	
	@interface NoInline {}
	
	@interface Head {}
	
	@interface Debug {}
	
	@interface Profile {}
	
	@interface NoProfile {}
	
	@interface StackExtreme {}
	
	@interface NoStackExtreme {}
	
	@interface PrintCode {}
	
	@interface NoOptimization {}
	
	@interface GenCode {}
	
	@interface GenDesc {}
	
	@interface SourceLines {}
	
	@interface ExplicitConversion {}
	
	@interface WinDLL {}
	
	@interface IgnoreUnit {}
	
	@interface InlineArrayVar {}
	
	@interface InlineArrayCount {}
}
