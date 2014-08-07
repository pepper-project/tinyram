
$ TinyRAM 1 2 32 5

;$org 0

; register layout
; r0 is zero reg
load r1,r0,_N       ; r1 is N, vector dimension
load r2,r0,_K       ; r2 is K, # nonzero elements
                    ; _DATA is beginning of vector
add r3,r1,_DATA     ; r3 is beginning of elms
add r4,r3,r2        ; r4 is beginning of inds
add r5,r4,r2        ; r5 is beginning of ptrs
add r6,r5,1         ; r6 is ptrs+1
;add r7,r0,r0       ; r6 is pointer

_ROWLOOP:
load r8,r7,r5       ; r8 = ptrs[i]
load r9,r7,r6       ; r9 = ptrs[i+1]
add r10,r0,r0       ; _OUT[i] = 0
_COLLOOP:
sub r0,r8,r9        ; if r8 >= r9
cnjmp [>_NEXTROW]   ;   done with this row
load r11,r8,r3      ; r11 = elms[j]
load r12,r8,r4      ; r12 = inds[j]
load r12,r12,_DATA  ; r12 = vector[inds[j]]
mull r11,r11,r12    ; r11 = elms[j] * vector[inds[j]]
add r10,r10,r11     ; r10 = _OUT[i] + elms[j] * vector[inds[j]]
add r8,r8,1         ; r8++
jmp [>_COLLOOP]     ;   continue

_NEXTROW:
load r11,r7,_auxTape; r11 = _auxTape[i]
xor r0,r10,r11      ; if _OUT[i] != _auxTape[i]
cnjmp [>_ERROR]     ;   failure

add r7,r7,1         ; i++
sub r0,r7,r1        ; if i < N
cjmp [>_ROWLOOP]    ;   continue

answer 0            ; correct!
_ERROR:
answer 1            ; oops

$inTape [_ERROR + 1]
$auxTape [_inTape + 16384]
_OUT = [_auxTape + 4096]

_N    =  _inTape
_K    = [_inTape + 1]
_DATA = [_inTape + 2]

