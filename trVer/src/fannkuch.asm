$ TinyRAM 1 2 32 5

; Fannkuch benchmark
; This program takes one of the permutations of [1, 2, ..m] and finds,
; for 100 lexicographically ordered permutations starting with the input
; one, the maximum pancake flips needed to make any permutation have
; a 1 in its first position.
; Input length must be less than 13.

;$org 0

; register layout
; r0 is the zero reg
; r1 is the M, the number of elements
; r2 is the # of permutations left to try
; r3 is the global max# of flips
; r4 is the local max# of flips
; r5 is local iterator variable
; r6 is local var
; r7 is local var
; r8 is local var
; r9 is local var

load r1,r0,_NELMS   ; #elems
load r2,r0,_NITER   ; 100 iterations
;add r3,r0,r0       ; all registers start equal to 0
;add r4,r0,r0       ; all registers start equal to 0

_FLIPSTART:
sub r5,r1,1         ; memcpy counter, r5 = M-1

_LISTCPY:
load r6,r5,_ELMS    ; make a copy of the input list
store r6,r5,_WORK   ; at _WORK
sub r5,r5,1         ; if --r5 >= 0
cnjmp [>_LISTCPY]   ;   goto _LISTCPY

add r4,r0,r0        ; local #flips counter
_PANCAKES:
load r6,r0,_WORK    ; r6 = _WORK[0]
sub r6,r6,1         ; r6--
sub r0,r0,r6        ; if 0 >= r6
cnjmp [>_CAKE_END]  ; goto _CAKE_END

add r7,r0,r0        ; r7 = 0
_CAKE_FLIP:
load r8,r6,_WORK    ; r8 = _WORK[r6]
load r9,r7,_WORK    ; r9 = _WORK[r7]
store r8,r7,_WORK   ; _WORK[r7] = r8
store r9,r6,_WORK   ; _WORK[r6] = r9
sub r6,r6,1         ; r6--
add r7,r7,1         ; r7++
sub r0,r7,r6        ; if r7 < r6
cjmp [>_CAKE_FLIP]  ; not done yet
add r4,r4,1         ; else increment local counter
jmp [>_PANCAKES]    ; and keep flipping

_CAKE_END:
sub r0,r3,r4        ; if r3 >= r4
cnjmp [>_NEXT_PERM] ;   next permutation
add r3,r4,r0        ; else r3 = r4

_NEXT_PERM:
sub r5,r1,2         ; r5 = M - 2

; find max idx such that _ELMS[idx] < _ELMS[idx+1]
_FIND_PIV:
load r8,r5,_ELMS    ; r8 = _ELMS[r5]
load r9,r5,_ELMN    ; r9 = _ELMS[r5 + 1]
sub r0,r8,r9        ; if r8 < r9
cjmp [>_GOT_PIV]    ;   found a pivot
sub r5,r5,1         ; if --r5 >= 0
cnjmp [>_FIND_PIV]  ;   keep going
jmp [>_FLIPEND]     ; else we did not find a greater perm

; r5 is idx
; r8 is _ELMS[idx]

_GOT_PIV:
; find max u such that _ELMS[idx] < _ELMS[u]
sub r6,r1,1         ; r6 = M - 1
_FIND_SWAP:
load r4,r6,_ELMS    ; r4 = _ELMS[r6]
sub r0,r8,r4        ; if r8 < r4
cjmp [>_GOT_U]      ;   found it
sub r6,r6,1         ; else
jmp [>_FIND_SWAP]   ;   continue
; NOTE no bounds check here: pivot loop guarantees this one terminates

; r6 is u
; r4 is _ELMS[u]
                    
_GOT_U:
; swap _ELMS[idx] and _ELMS[u]
store r8,r6,_ELMS   ; _ELMS[u] = _ELMS[idx]
store r4,r5,_ELMS   ; _ELMS[idx] = _ELMS[u]

; swap elements from idx+1 to list end
add r6,r5,1         ; r6 = idx + 1
sub r7,r1,1         ; r7 = M - 1
_SWAP_ELMS:
load r8,r6,_ELMS    ; r8 = _ELMS[r6]
load r9,r7,_ELMS    ; r9 = _ELMS[r7]
store r8,r7,_ELMS   ; _ELMS[r7] = r8
store r9,r6,_ELMS   ; _ELMS[r6] = r9
add r6,r6,1         ; r6++
sub r7,r7,1         ; r7--
sub r0,r6,r7        ; if r6 < r7
cjmp [>_SWAP_ELMS]  ;   not done yet

_FLIPEND:
sub r2,r2,1         ; if --r2 >= 0
cnjmp [>_FLIPSTART] ;   goto _FLIPSTART

load r4,r0,_auxTape ; else read answer from _auxTape

sub r5,r4,r3        ; r5 = r4-r3  -- if r4 < r3, then r5 wraps and is huge
sub r0,r5,1         ; if r5 >= 1  -- otherwise if r4 > r3 then r5 is 1 or more
cnjmp [>_ERROR]     ;   wrong

; we could use xor here, but the savings from eliminating
; xor from the instruction set is much greater than the cost
; of one additional instruction being executed.
;  - eliminating xor saves ~76 instructions per transition
;  - we only need ~25 transitions to make up for the cost
;    of the extra program step

answer 0            ; else correct

_ERROR:
answer 1

$ inTape [_ERROR + 1]
$ auxTape [_inTape + 32]

_WORK = [_auxTape + 32] ; working memory
_NELMS = _inTape        ; number of elements in the input list
_NITER =[_inTape + 1]
_ELMS = [_inTape + 2]   ; input list
_ELMN = [_ELMS + 1]     ; input list + 1 (for _FIND_PIV)

