$ TinyRAM 1 2 32 5

;$org 0
; not necessary

; memory layout
; 0             : code
; _DATA         : list 1
; _DATA + SIZE  : list 2

; register layout
; r0 is zero reg
; r1-r7 are local variables
; r8 is SIZE
; r9 is span
; r10 is i
; r11 is bPtr
; r12 is mPtr
; r13 is ePtr
; r14 is lPtr
; r15 is rPtr
; r16 is src (_DATA + 0 or _DATA + SIZE)
; r17 is dst (_DATA + SIZE or _DATA + 0)
; r18 is 2*span

; load SIZE param from input tape, set up some parameters
load r8,r0,_inTape  ; SIZE
add r16,r0,_DATA    ; _DATA + 0 is initial src
add r17,r8,_DATA    ; _DATA + SIZE is initial dst
add r9,r0,1         ; initial span is 1
add r18,r9,r9       ; 2*span

;; load data from input tape
;;add r2,r0,r0       ; not necessary as long as you don't write r2 above here
;_READIN:            ; assume SIZE is 2^k, k=[1..)
;read r1,0           ; next datum
;store r1,r2,_DATA   ; store it
;add r2,r2,1         ; r2++
;xor r0,r2,r8        ; if r2 != size
;cnjmp [>_READIN]    ;   goto READIN

; now, the sorting loop
_SPANLOOP:
add r14,r11,r0      ; lPtr = bPtr
add r12,r11,r9      ; mPtr = bPtr + span
add r15,r12,r0      ; rPtr = mPtr
add r13,r12,r9      ; ePtr = mPtr + span
add r10,r14,r0      ; i = lPtr
load r1,r16,r14     ; r1 = src[lPtr]
load r2,r16,r15     ; r2 = src[rPtr]
_COMPLOOP:
sub r0,r14,r12      ; if lPtr >= mPtr
cnjmp [>_RMERGE]    ;   goto _RMERGE
sub r0,r15,r13      ; if rPtr >= ePtr
cnjmp [>_LMERGE]    ;   goto _LMERGE
sub r0,r1,r2        ; if src[lPtr] >= src[rPtr]
cnjmp [>_RMERGE]    ;   goto _RMERGE
_LMERGE:
add r3,r1,r0        ; tmp = src[lPtr]
add r14,r14,1       ; lPtr++
load r1,r16,r14     ; r1 = src[lPtr]
jmp [>_MERGE]       ; goto _MERGE
_RMERGE:
add r3,r2,r0        ; tmp = src[rPtr]
add r15,r15,1       ; rPtr++
load r2,r16,r15     ; r2 = src[rPtr]
_MERGE:
store r3,r17,r10    ; dst[i] = tmp
add r10,r10,1       ; i++
xor r0,r10,r13      ; if i != ePtr
cnjmp [>_COMPLOOP]  ;   goto _COMPLOOP
add r11,r11,r18     ; bPtr += 2*span
xor r0,r11,r8       ; if bPtr != SIZE
cnjmp [>_SPANLOOP]  ;   goto _SPANLOOP
add r11,r0,r0       ; bPtr = 0  - restarting with bigger span
add r9,r9,r9        ; span *= 2
add r18,r18,r18     ; 2span *= 2
add r1,r16,r0       ; tmp = src - swap src and dst pointers
add r16,r17,r0      ; src = dst
add r17,r1,r0       ; dst = tmp
xor r0,r9,r8        ; if span != SIZE
cnjmp [>_SPANLOOP]  ;   goto _SPANLOOP

; check the result against the aux tape
add r2,r0,r0        ; offset from src (we just swapped src and dst, so src now points to previous iteration's output)
_TESTLOOP:
load r1,r2,_auxTape ; load value from AUX
load r3,r2,r16      ; read value from memory
xor r0,r1,r3        ; if r1 != r3
cnjmp [>_ERROR]     ;   goto _ERROR
add r2,r2,1         ; r2++
xor r0,r2,r8        ; if r2 != SIZE
cnjmp [>_TESTLOOP]  ;   goto _TESTLOOP

answer 0            ; all was OK

_ERROR:
answer 1            ; all was not OK

$ inTape [_ERROR + 1]
$ auxTape [_inTape + 1024]
_DATA = [_inTape + 1] ; first item in inTape is length; rest is data segment

