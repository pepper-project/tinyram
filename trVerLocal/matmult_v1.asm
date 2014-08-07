$ TinyRAM 1 2 32 5

;$org 0
; not necessary

; memory layout
; 0                  : code
; _DATA              : matrix A
; _DATA + SIZE^2     : matrix B
; _DATA + 2 * SIZE^2 : output matrix

; register layout
; r0 is zero reg
; r1-r6 are local variables
; r7 is output offset from _DATA
; r8 is SIZE
; r9 is i
; r10 is j
;   -- NOTE -- k is not explicitly kept, instead we increment the A and B addresses
; r11 is SIZE^2
; r12 is 2*SIZE^2

; load parameter from input tape
load r8,r0,_inTape  ; SIZE
mull r11,r8,r8      ; SIZE^2, offset of matrix B from DATA
mull r12,r11,2      ; 2*SIZE^2, offset of matrix C from DATA

;; load data from input tape
;;add r2,r0,r0       ; not necessary as long as you don't touch r2 above this point
;_READIN:            ; we assume that SIZE is never 0
;read r1,0           ; read data
;store r1,r2,_DATA   ; store in memory
;add r2,r2,1         ; r2++
;xor r0,r2,r12       ; if r2 != 2*SIZE^2
;cnjmp [>_READIN]    ;   goto _READIN

; do matrix multiplication
add r7,r12,r0        ; output offset from _DATA
_LOOPJ:
add r1,r0,r0        ; c_ij = 0
mull r2,r9,r8       ; idxA = i*SIZE
add r3,r10,r11      ; idxB = j + SIZE^2
add r6,r9,1         ; idxAMax = (1+i)...
mull r6,r6,r8       ;           ...*SIZE
_LOOPK:
load r4,r2,_DATA    ; load A[idxA]
load r5,r3,_DATA    ; load B[idxB]
mull r4,r4,r5       ; tmp = A[idxA]*B[idxB]
add r1,r1,r4        ; c_ij += tmp
add r2,r2,1         ; idxA++
add r3,r3,r8        ; idxB += SIZE
xor r0,r2,r6        ; if idxA != idxAMax
cnjmp [>_LOOPK]     ;   goto _LOOPK
store r1,r7,_DATA   ; out[idxOut] = c_ij
add r7,r7,1         ; idxOut++
add r10,r10,1       ; j++
xor r0,r10,r8       ; if j != SIZE
cnjmp [>_LOOPJ]     ;   goto _LOOPJ
add r10,r0,r0       ; j = 0
add r9,r9,1         ; i++
xor r0,r9,r8        ; if i != SIZE
cnjmp [>_LOOPJ]     ;   goto _LOOPJ

; check result against aux tape
add r2,r0,r0        ; counter
add r5,r12,_DATA    ; base output address
_TESTLOOP:
load r1,r2,_auxTape ; read expected value from _auxTape
load r3,r2,r5       ; load value from memory
xor r0,r1,r3        ; if r1 != r3
cnjmp [>_ERROR]     ;   goto _ERROR
add r2,r2,1         ; r2++
xor r0,r2,r11       ; if r2 != SIZE^2
cnjmp [>_TESTLOOP]  ;   goto _TESTLOOP

answer 0            ; all OK

_ERROR:
answer 1            ; something wrong!

$ inTape [_ERROR + 1]
$ auxTape [_inTape + 1024]
_DATA = [_inTape + 1] ; first item in inTape is length, rest is data segment

