; File needs to be copied to include folder!
; yasm file.s -o file.bim -a x86

section .text
bits 32

var_offset_from  EQU 16
var_offset_to    EQU 12
var_offset_cnt   EQU 8


memcopy32:
    push    esi
    push    edi             ; Save edi (addr) onto the stack
    cld                     ; Clear the direction flag (forward string operation)

    mov     esi, [ebp + var_offset_from] ; Load the source address
    mov     edi, [ebp + var_offset_to]   ; Load the destination address
    mov     ecx, [ebp + var_offset_cnt]  ; Load the count

    rep     movsd           ; Move the data

    pop     edi             ; Restore original edi value
    pop     esi
