package java.lang;

import os.interrupt.Interrupt;
import os.screen.Cursor;

public class BIOS {
    private final static int BIOS_MEMORY = 0x60000;
    private final static int BIOS_STKEND = BIOS_MEMORY + 0x1000;
    private final static int BIOS_STKBSE = BIOS_STKEND - 0x28;

    public final static int TEXT_MODE = 0x0003;
    public final static int GRAPHICS_MODE = 0x0013;

    public final static int MEMSEG_TYPE_FREE = 1;
    public final static int MEMSEG_TYPE_RESERVED = 2;
    public final static int MEMSEG_TYPE_ACPI_RECLAIMABLE = 3;
    public final static int MEMSEG_TYPE_ACPI_NVS_MEMORY = 4;


    public static void switchMode(int mode) {
        BIOS.regs.EAX = mode;
        BIOS.rint(0x10);
    }

    public static class BIOSRegs extends STRUCT {
        public short DS, ES, FS, FLAGS;
        public int EDI, ESI, EBP, ESP, EBX, EDX, ECX, EAX;
    }

    public final static BIOSRegs regs = (BIOSRegs) MAGIC.cast2Struct(BIOS_STKBSE);

    public final static short F_CARRY = 0x0001;
    public final static short F_PARITY = 0x0004;
    public final static short F_AUX = 0x0010;
    public final static short F_ZERO = 0x0040;
    public final static short F_SIGN = 0x0080;
    public final static short F_TRAP = 0x0100;
    public final static short F_INT = 0x0200;
    public final static short F_DIR = 0x0400;
    public final static short F_OVER = 0x0800;

    private static boolean initDone;

    //-------------------------------------------------------- call BIOS-IRQ ------------------------------------
    public static void rint(int inter) {
        int addr = BIOS_MEMORY + 8;

        if (!initDone) { //initialize 16 bit code
            MAGIC.wMem8(addr++, (byte) 0x66);
            MAGIC.wMem8(addr++, (byte) 0xBB);
            MAGIC.wMem32(addr, BIOS_MEMORY); //mov ebx,0x60000(BIOS_MEMORY)
            addr += 4;

            MAGIC.wMem8(addr++, (byte) 0x67);
            MAGIC.wMem8(addr++, (byte) 0x66);
            MAGIC.wMem8(addr++, (byte) 0x89);
            MAGIC.wMem8(addr++, (byte) 0x23); //mov [ebx],esp

            MAGIC.wMem8(addr++, (byte) 0x0F);
            MAGIC.wMem8(addr++, (byte) 0x20);
            MAGIC.wMem8(addr++, (byte) 0xC0); //mov eax,cr0

            MAGIC.wMem8(addr++, (byte) 0x67);
            MAGIC.wMem8(addr++, (byte) 0x66);
            MAGIC.wMem8(addr++, (byte) 0x89);
            MAGIC.wMem8(addr++, (byte) 0x43);
            MAGIC.wMem8(addr++, (byte) 0x04); //mov [ebx+4],eax

            MAGIC.wMem8(addr++, (byte) 0x66);
            MAGIC.wMem8(addr++, (byte) 0x25);
            MAGIC.wMem8(addr++, (byte) 0xFE);
            MAGIC.wMem8(addr++, (byte) 0xFF);
            MAGIC.wMem8(addr++, (byte) 0xFE);
            MAGIC.wMem8(addr++, (byte) 0x7F); //and eax, 0x7FFEFFFE

            MAGIC.wMem8(addr++, (byte) 0x0F);
            MAGIC.wMem8(addr++, (byte) 0x22);
            MAGIC.wMem8(addr++, (byte) 0xC0); //mov cr0, eax

            MAGIC.wMem8(addr++, (byte) 0xEA);
            MAGIC.wMem8(addr++, (byte) 0x28);
            MAGIC.wMem8(addr++, (byte) 0x00);
            MAGIC.wMem16(addr, (short) (BIOS_MEMORY >>> 4)); //jmp 0x6000(BIOS_MEMORY>>>4):0028
            addr += 2;

            MAGIC.wMem8(addr++, (byte) 0xBA);
            MAGIC.wMem16(addr, (short) (BIOS_MEMORY >>> 4)); //mov dx,0x6000(BIOS_MEMORY>>>4)
            addr += 2;

            MAGIC.wMem8(addr++, (byte) 0x8E);
            MAGIC.wMem8(addr++, (byte) 0xD2); //mov ss,dx

            MAGIC.wMem8(addr++, (byte) 0x8E);
            MAGIC.wMem8(addr++, (byte) 0xEA); //mov gs,dx

            MAGIC.wMem8(addr++, (byte) 0x66);
            MAGIC.wMem8(addr++, (byte) 0xBC);
            MAGIC.wMem32(addr, BIOS_STKBSE - BIOS_MEMORY); //mov esp,0x2000(BIOS_MEMORY-BIOS_STKBSE)
            addr += 4;

            MAGIC.wMem8(addr++, (byte) 0x1F); //pop ds
            MAGIC.wMem8(addr++, (byte) 0x07); //pop es

            MAGIC.wMem8(addr++, (byte) 0x0f);
            MAGIC.wMem8(addr++, (byte) 0xa1); //pop fs

            MAGIC.wMem8(addr++, (byte) 0x58); //pop ax -> we have to pop something for symmetry

            MAGIC.wMem8(addr++, (byte) 0x66);
            MAGIC.wMem8(addr++, (byte) 0x61); //popad

            MAGIC.wMem8(addr++, (byte) 0xCD);
            MAGIC.wMem8(addr++, (byte) 0); //cd inter

            MAGIC.wMem8(addr++, (byte) 0xFA); //cli (some buggy BIOS versions enable interrupts)

            MAGIC.wMem8(addr++, (byte) 0x66);
            MAGIC.wMem8(addr++, (byte) 0x60); //pushad

            MAGIC.wMem8(addr++, (byte) 0x9C); //pushf

            MAGIC.wMem8(addr++, (byte) 0x0f);
            MAGIC.wMem8(addr++, (byte) 0xa0); //push fs

            MAGIC.wMem8(addr++, (byte) 0x06); //push es
            MAGIC.wMem8(addr++, (byte) 0x1E); //push ds

            MAGIC.wMem8(addr++, (byte) 0x2E);
            MAGIC.wMem8(addr++, (byte) 0x66);
            MAGIC.wMem8(addr++, (byte) 0xA1);
            MAGIC.wMem8(addr++, (byte) 0x04);
            MAGIC.wMem8(addr++, (byte) 0x00); //mov eax,[cs:0x0004]

            MAGIC.wMem8(addr++, (byte) 0x0F);
            MAGIC.wMem8(addr++, (byte) 0x22);
            MAGIC.wMem8(addr++, (byte) 0xC0); //mov cr0, eax

            MAGIC.wMem8(addr++, (byte) 0xEA);
            MAGIC.wMem8(addr++, (byte) 0x53);
            MAGIC.wMem8(addr++, (byte) 0x00);
            MAGIC.wMem8(addr++, (byte) 0x18);
            MAGIC.wMem8(addr++, (byte) 0x00); //jmp 0x0018:0053

            MAGIC.wMem8(addr++, (byte) 0xBA);
            MAGIC.wMem8(addr++, (byte) 0x10);
            MAGIC.wMem8(addr++, (byte) 0x00); //mov dx,0x0010

            MAGIC.wMem8(addr++, (byte) 0x8E);
            MAGIC.wMem8(addr++, (byte) 0xDA); //mov ds,dx

            MAGIC.wMem8(addr++, (byte) 0x8E);
            MAGIC.wMem8(addr++, (byte) 0xC2); //mov es,dx

            MAGIC.wMem8(addr++, (byte) 0x8E);
            MAGIC.wMem8(addr++, (byte) 0xE2); //mov fs,dx

            MAGIC.wMem8(addr++, (byte) 0x8E);
            MAGIC.wMem8(addr++, (byte) 0xEA); //mov gs,dx

            MAGIC.wMem8(addr++, (byte) 0x8E);
            MAGIC.wMem8(addr++, (byte) 0xD2); //mov ss,dx

            MAGIC.wMem8(addr++, (byte) 0x66);
            MAGIC.wMem8(addr++, (byte) 0xB8);
            MAGIC.wMem32(addr, BIOS_MEMORY); //mov eax, BIOS_MEMORY
            addr += 4;

            MAGIC.wMem8(addr++, (byte) 0x66);
            MAGIC.wMem8(addr++, (byte) 0x67);
            MAGIC.wMem8(addr++, (byte) 0x8B);
            MAGIC.wMem8(addr++, (byte) 0x20); //mov esp, [eax]

            MAGIC.wMem8(addr++, (byte) 0x66);
            MAGIC.wMem8(addr, (byte) 0xCB); //far ret

            initDone = true;
        }

        //real function after initialization
        MAGIC.wMem8(BIOS_MEMORY + 61, (byte) inter); //set interrupt number
        MAGIC.inline(0x9C); //pushf
        MAGIC.inline(0xFA); //cli

        //load idt with real mode interrupt table
        Interrupt.lidtRM();

        //call 16 bit code
        MAGIC.inline(0x56); //push e/rsi
        MAGIC.inline(0x57); //push e/rdi
        if (MAGIC.ptrSize == 4) {
            MAGIC.inline(0x9A);
            MAGIC.inline32(0x00000008);
            MAGIC.inline16(0x0018); //call far 18:00000008
        } else {
            //manually code return address (code after "retf")
            MAGIC.inline(0xE8, 0x00, 0x00, 0x00, 0x00); //call rel 0
            MAGIC.inline(0x83, 0x04, 0x24, 0x0F);       //add dword [rsp],byte 0x0F
            MAGIC.inline(0xC6, 0x44, 0x24, 0x04, 0x08); //mov byte [rsp+4],0x08
            //manually code destination address 18:00000008
            MAGIC.inline(0x6A, 0x18);                   //push byte 0x18
            MAGIC.inline(0x6A, 0x08);                   //push byte 0x08
            //use return far as call
            MAGIC.inline(0x48, 0xCB);                   //retf
        }
        MAGIC.inline(0x5F); //pop e/rdi
        MAGIC.inline(0x5E); //pop e/rsi

        //load idt with protected/long mode interrupt table
        Interrupt.initPic();

        MAGIC.inline(0x9D); //popf
    }

    public static class MemorySegment {
        public final long baseAddress;
        public final long length;
        public final int type;

        MemorySegment(long baseAddress, long length, int type) {
            this.baseAddress = baseAddress;
            this.length = length;
            this.type = type;
        }
    }


    public static class MemGenerator {
        private int ebx = 0;
        private boolean done = false;
        private final int storeAt;

        public MemGenerator(int storeAt) {
            this.storeAt = storeAt;
        }

        public MemorySegment next() {
            if (done)
                return null;

            BIOS.regs.EAX = 0x0000E820;
            BIOS.regs.EDX = 0x534D4150; // SMAP

            BIOS.regs.EBX = ebx;

            // store result in
            BIOS.regs.EDI = storeAt;
            // buffer size >= 20 bytes
            BIOS.regs.ECX = 20;

            BIOS.rint(0x15);

            ebx = BIOS.regs.EBX;

            // is done?
            if (BIOS.regs.EAX != 0x534D4150 || BIOS.regs.EBX == 0) {
                done = true;
                return null;
            }

            // error?
            if ((BIOS.regs.FLAGS & BIOS.F_CARRY) != 0) {
                done = true;
                Cursor.staticCursor.print("Error: Carry flag was set");
                return null;
            }

            return new MemorySegment(
                    MAGIC.rMem64(storeAt),
                    MAGIC.rMem64(storeAt + 8),
                    MAGIC.rMem32(storeAt + 16)
            );
        }
    }


    /**
     * StaticMemoryGenerator
     *
     * For object generation in DynamicRuntime, we can't create a new instance of MemoryGenerator
     * 'cause we'd need the ability to generate objects for that. duh.
     */
    public static class SMG {
        private static int ebx = 0;
        private static boolean done = false;
        private static boolean error = false;
        private static int storeAt = 0x8000;

        public static long baseAddress = -1;
        public static long length = -1;
        public static int type = -1;

        public static void reset(int storeAt) {
            ebx = 0;
            BIOS.SMG.storeAt = storeAt;
            done = false;
            error = false;
        }

        @SJC.Inline
        public static boolean isDone() {
            return done;
        }

        @SJC.Inline
        public static boolean hasError() {
            return error;
        }

        @SJC.Inline
        public static boolean isTypeFree() {
            return type == MEMSEG_TYPE_FREE;
        }

        public static boolean next() {
            if (done)
                return false;

            BIOS.regs.EAX = 0x0000E820;
            BIOS.regs.EDX = 0x534D4150; // SMAP

            BIOS.regs.EBX = ebx;

            // store result in
            BIOS.regs.EDI = storeAt;
            // buffer size >= 20 bytes
            BIOS.regs.ECX = 20;

            BIOS.rint(0x15);
            ebx = BIOS.regs.EBX;

            // is done?
            if (BIOS.regs.EAX != 0x534D4150 || BIOS.regs.EBX == 0) {
                done = true;
                return false;
            }

            // error?
            if ((BIOS.regs.FLAGS & BIOS.F_CARRY) != 0) {
                done = true;
                error = true;
                MAGIC.inline(0xCC);
                return false;
            }

            baseAddress = MAGIC.rMem64(storeAt);
            length = MAGIC.rMem64(storeAt + 8);
            type = MAGIC.rMem32(storeAt + 16);

            return true;
        }
    }
}
