package java.lang;

/**
 * x86 instruction set opcodes
 *
 * @author Konloch
 * @since 7/18/2024
 */
public class x86
{
	//Push instructions
	public static final int PUSH_IMMEDIATE_BYTE = 0x6A;
	public static final int PUSH_IMMEDIATE_WORD = 0x68;
	public static final int PUSH_REGISTER_EAX = 0x50;
	
	//Load Effective Address instructions
	public static final int LOAD_EFFECTIVE_ADDRESS = 0x8D;
	
	//Call instructions
	public static final int CALL_NEAR = 0xFF;
	public static final int CALL_FAR = 0xFF;
	
	//ModRM instructions
	public static final int MODRM_REGISTER = 0x45;
	
	//Immediate instructions
	public static final int IMMEDIATE_BYTE = 0x6A;
	public static final int IMMEDIATE_WORD = 0x68;
	public static final int IMMEDIATE_DWORD = 0xB8;

	//Register instructions
	public static final int PUSH_REGISTER = 0x57;
	public static final int POP_REGISTER = 0x5F;
	public static final int MOVE_REGISTER_TO_REGISTER = 0x89;
	public static final int MOVE_MEMORY_TO_REGISTER = 0x8B;
	public static final int MOVE_IMMEDIATE_TO_REGISTER = 0xBB;
	
	//System call instructions
	public static final int SYSCALL = 0xCD;
	
	//Other instructions
	public static final int CLEAR_DIRECTION_FLAG = 0xFC;
	public static final int REPETITIVE_STORE_STRING = 0xF3;
}
