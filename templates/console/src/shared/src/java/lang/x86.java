package java.lang;

/**
 * @author Konloch
 * @since 7/18/2024
 */
public class x86
{
	//Push instructions
	public static final int PUSH_BYTE = 0x6A;
	public static final int PUSH_WORD = 0x68;
	public static final int PUSH_EAX = 0x50;
	
	//Load Effective Address instructions
	public static final int LEA_EAX = 0x8D;
	
	//Call instructions
	public static final int CALL_NEAR = 0xFF;
	public static final int CALL_FAR = 0xFF;
	
	//ModRM instructions
	public static final int MODRM_EAX = 0x45;
	
	//Immediate instructions
	public static final int IMMEDIATE_BYTE = 0x6A;
	public static final int IMMEDIATE_WORD = 0x68;
	public static final int IMMEDIATE_DWORD = 0xB8;

	//Register instructions
	public static final int MOV_EBP_EAX = 0x89;
	public static final int MOV_EBX_MEM = 0x8B;
	
	
	//Stack instructions
	public static final int PUSH_EDI = 0x57;
	public static final int POP_EDI = 0x5F;
	
	//System call instructions
	public static final int SYSCALL = 0xCD;
	
	//Other instructions
	public static final int CLD = 0xFC;
	public static final int REP_STOSD = 0xF3;
}
