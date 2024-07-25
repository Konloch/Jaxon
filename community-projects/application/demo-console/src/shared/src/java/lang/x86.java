package java.lang;

/**
 * x86 instruction set opcodes
 *
 * @author Konloch
 * @since 7/18/2024
 */
public class x86
{
	// Opcodes
	public static final int NOP = 0x90;
	public static final int JMP = 0xEB;
	public static final int CALL = 0xE8;
	public static final int PUSH = 0xFF;
	public static final int RET = 0xC3;
	
	// ModRM bytes
	public static final int MODRM_RM = 0x40;
	public static final int REG_CALL = 0x10;
	public static final int REG_OPCODE_PUSH = 0x30;
	
	// Registers
	public static final int REG_EAX = 0x00;
	public static final int REG_ECX = 0x01;
	public static final int REG_EDX = 0x02;
	public static final int REG_EBX = 0x03;
	public static final int REG_ESP = 0x04;
	public static final int REG_EBP = 0x05;
	public static final int REG_ESI = 0x06;
	public static final int REG_EDI = 0x07;
	
	// Local variables (negative offsets from EBP)
	public static final int EBP_MINUS_0 = 0x00;    // [ebp-0]  (often not used)
	public static final int EBP_MINUS_2 = 0xFE;    // [ebp-2]
	public static final int EBP_MINUS_4 = 0xFC;    // [ebp-4]
	public static final int EBP_MINUS_6 = 0xFA;    // [ebp-6]
	public static final int EBP_MINUS_8 = 0xF8;    // [ebp-8]
	public static final int EBP_MINUS_10 = 0xF6;   // [ebp-10]
	public static final int EBP_MINUS_12 = 0xF4;   // [ebp-12]
	public static final int EBP_MINUS_14 = 0xF2;   // [ebp-14]
	public static final int EBP_MINUS_16 = 0xF0;   // [ebp-16]
	public static final int EBP_MINUS_18 = 0xEE;   // [ebp-18]
	public static final int EBP_MINUS_20 = 0xEC;   // [ebp-20]
	public static final int EBP_MINUS_22 = 0xEA;   // [ebp-22]
	public static final int EBP_MINUS_24 = 0xE8;   // [ebp-24]
	public static final int EBP_MINUS_26 = 0xE6;   // [ebp-26]
	public static final int EBP_MINUS_28 = 0xE4;   // [ebp-28]
	public static final int EBP_MINUS_30 = 0xE2;   // [ebp-30]
	public static final int EBP_MINUS_32 = 0xE0;   // [ebp-32]
	
	// Function parameters (positive offsets from EBP)
	public static final int EBP_PLUS_0 = 0x00;     // [ebp+0] (not typically used for parameters)
	public static final int EBP_PLUS_2 = 0x02;     // [ebp+2] (not typically used for parameters)
	public static final int EBP_PLUS_4 = 0x04;     // [ebp+4] (return address)
	public static final int EBP_PLUS_6 = 0x06;     // [ebp+6] (not typically used for parameters)
	public static final int EBP_PLUS_8 = 0x08;     // [ebp+8] (first parameter)
	public static final int EBP_PLUS_10 = 0x0A;    // [ebp+10] (not typically used for parameters)
	public static final int EBP_PLUS_12 = 0x0C;    // [ebp+12] (second parameter)
	public static final int EBP_PLUS_14 = 0x0E;    // [ebp+14] (not typically used for parameters)
	public static final int EBP_PLUS_16 = 0x10;    // [ebp+16] (third parameter)
	public static final int EBP_PLUS_18 = 0x12;    // [ebp+18] (not typically used for parameters)
	public static final int EBP_PLUS_20 = 0x14;    // [ebp+20] (fourth parameter)
	public static final int EBP_PLUS_22 = 0x16;    // [ebp+22] (not typically used for parameters)
	public static final int EBP_PLUS_24 = 0x18;    // [ebp+24] (fifth parameter)
	public static final int EBP_PLUS_26 = 0x1A;    // [ebp+26] (not typically used for parameters)
	public static final int EBP_PLUS_28 = 0x1C;    // [ebp+28] (sixth parameter)
	public static final int EBP_PLUS_30 = 0x1E;    // [ebp+30] (not typically used for parameters)
	public static final int EBP_PLUS_32 = 0x20;    // [ebp+32] (seventh parameter)
	
	// Opcode extensions
	public static final int OPCD_NONE = 0x00;
	public static final int OPCD_RM = 0x01;
	public static final int OPCD_REG = 0x02;
	public static final int OPCD_IMM = 0x03;
	
	
	//Push instructions
	public static final int PUSH_IMMEDIATE_BYTE = 0x6A;
	public static final int PUSH_IMMEDIATE_WORD = 0x68;
	public static final int PUSH_IMMEDIATE_DWORD = 0xFF;
	public static final int PUSH_REGISTER_EAX = 0x50;
	
	//Load Effective Address instructions
	public static final int LOAD_EFFECTIVE_ADDRESS = 0x8D;
	
	//Call instructions
	public static final int CALL_NEAR = 0xFF;
	public static final int CALL_FAR = 0xFF;
	
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
