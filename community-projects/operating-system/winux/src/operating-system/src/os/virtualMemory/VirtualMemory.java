package os.virtualMemory;

import rte.ImageHelper;

public class VirtualMemory {
    private static final int pagedirBaseAdress = 0x12000;

    public static int initVirtualMemory() {
        int pageTables = ImageHelper.align4kBAddress(ImageHelper.getImageEnd());
        int ptr;
        int i;

        for(i = 0, ptr = pagedirBaseAdress; i < 1024; i++, ptr += 4) {
            MAGIC.wMem32(ptr, ((i << 12) + pageTables) | 0x03);
            ptr += 4;
        }

        // set first page in first table to 0
        MAGIC.wMem32(pageTables, 0);

        // everything else
        for(i = 1, ptr = pageTables + 4; i < 1024 * 1024 - 1; i++, ptr += 4) {
            MAGIC.wMem32(ptr, (i << 12) | 3);
        }

        // set last page in last table to 0
        MAGIC.wMem32(ptr, 0);

        setCR3(pagedirBaseAdress);
        enableVirtualMemory();

        return ptr + 4;
    }


    public static void setCR3(int pagedirStart) {
        MAGIC.inline(0x8B, 0x45);
        MAGIC.inlineOffset(1, pagedirStart); //mov eax,[ebp+8]
        MAGIC.inline(0x0F, 0x22, 0xD8); //mov cr3,eax
    }

    public static void enableVirtualMemory() {
        MAGIC.inline(0x0F, 0x20, 0xC0); //mov eax,cr0
        MAGIC.inline(0x0D, 0x00, 0x00, 0x01, 0x80); //or eax,0x80010000
        MAGIC.inline(0x0F, 0x22, 0xC0); //mov cr0,eax
    }

    public static int getCR2() {
        int cr2 = 0;
        MAGIC.inline(0x0F, 0x20, 0xD0); //mov e/rax,cr2
        MAGIC.inline(0x89, 0x45);
        MAGIC.inlineOffset(1, cr2); //mov [ebp-4],eax
        return cr2;
    }
}
