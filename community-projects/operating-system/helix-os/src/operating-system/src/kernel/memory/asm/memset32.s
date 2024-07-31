; File needs to be copied to include folder!
; yasm file.s -o file.bim -a x86

section .text
bits 32

var_offset_addr  EQU 16
var_offset_cnt   EQU 12
var_offset_val   EQU 8


memset32:
    push    esi
    push    edi             ; Save edi (addr) onto the stack
    cld                     ; Clear the direction flag (forward string operation)
    
    mov     edi, dword [ebp + var_offset_addr]  ; Move addr (edi) into edi
    mov     eax, dword [ebp + var_offset_val]   ; Move val (edx) into eax
    mov     ecx, dword [ebp + var_offset_cnt]   ; Move cnt (esi) into ecx
    
    rep stosd               ; rep stosd (Store dword string)

    pop     edi             ; Restore original edi value
    pop    esi
