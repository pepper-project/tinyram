$ TinyRAM 1 2 32 5

;$org 0

; memory layout
; 0           : code
; _DATA       : input
; _DATA + M*D : output

; register layout
; r0 is zero reg
; r1 is used in _GET_DISTANCE
; r2, r3 are local load/store helper vars
; r4 is used in _GET_POINT_COST
; r5 is used in _GET_COST
; r6 is inner loop counter
; r7 is outer loop counter
load r8,r0,_DATA_D      ; D
load r9,r0,_DATA_M      ; M
load r10,r0,_DATA_K     ; K
load r11,r0,_DATA_L     ; L
sub r11,r11,1           ; r11 = L - 1
; r12 is current cost
; r13 is working medoid
; r14 is working non-medoid
mull r15,r8,r9          ; D*M
add r15,r15,_DATA       ; r15 = output[0]
; r16-r19 unused
; r20 - return value from _GET_DISTANCE
; r21 - return address for _GET_DISTANCE
; r22 - arg0 for _GET_DISTANCE
; r23 - arg1 for _GET_DISTANCE
; r24 - return value from _GET_POINT_COST
; r25 - return value from _GET_COST
; r26 - return address for _GET_COST
; r27-r31 unused

;add r6,r0,r0            ; r6 = 0 - not necessary, first access of r6
add r1,r0,_DATA         ; r1 = _DATA
_INIT_LOOP:             ; do {
store r1,r6,_POINTS     ; points[r6] = r1
store r6,r1,_RPOINTS    ; rpoints[r1] = r6 - we use this later to reverse-lookup points
add r1,r1,r8            ; r1 += D
add r6,r6,1             ; r6 += 1
xor r0,r6,r9            ; while (r6
cnjmp [>_INIT_LOOP]     ;           != M)

; initialize
add r26,r0,_INIT_COST   ; return to _INIT_COST
jmp [>_GET_COST]        ; call GET_COST
_INIT_COST:
add r12,r25,r0          ; r12 is current cost
add r26,r0,_LOOP_COST   ; return from _GET_COST is now into the loop

_L_LOOP:                ; L, number of times to run swap phase
add r6,r0,r0            ; i = 0
_I_LOOP:                ; outer swap loop (r6)
add r7,r10,r0           ; j = K
_J_LOOP:                ; inner swap loop (r7)
load r13,r6,_POINTS     ; r13 = points[r6]
load r14,r7,_POINTS     ; r14 = points[r7]
store r14,r6,_POINTS    ; points[r6] = r14
store r13,r7,_POINTS    ; points[r7] = r13
jmp [>_GET_COST]        ; get new cost
_LOOP_COST:             ; back from _GET_COST
sub r0,r25,r12          ; if old is at least as good
cnjmp [>_NO_SWAP]       ;   don't do the swap
add r12,r25,r0          ; else update cost
jmp [>_DONE_SWAP]       ;   and we're done
_NO_SWAP:               ; undoing swap
store r13,r6,_POINTS    ;   restore point[0]
store r14,r7,_POINTS    ;   restore point[1]
_DONE_SWAP:             ; done with swapping (or not swapping)
add r7,r7,1             ; r7 += 1
xor r0,r7,r9            ; if r7 != M
cnjmp [>_J_LOOP]        ;   goto _J_LOOP
add r6,r6,1             ; r6 += 1
xor r0,r6,r10           ; if r6 != K
cnjmp [>_I_LOOP]        ;   goto _I_LOOP
sub r11,r11,1           ; if r11-- > 0
cnjmp [>_L_LOOP]        ;   goto _L_LOOP

; classify points
add r6,r0,r0            ; i = 0
_CMED_LOOP:
load r3,r6,_POINTS      ; r3 = points[r6]
load r3,r3,_RPOINTS     ; r3 = rpoints[points[r6]] - this is the point # in original order
store r3,r15,r6         ; medoid(n) is always in its own cluster
add r6,r6,1             ; i += 1
xor r0,r6,r10           ; if (i != K)
cnjmp [>_CMED_LOOP]     ;   goto _CMED_LOOP

; at this point, r6 (i) == k
_CI_LOOP:               ;
add r13,r0,r0           ; temp_output = 0
load r23,r0,_POINTS     ; r23 is medoid[0]
load r22,r6,_POINTS     ; r22 is point[r6], a non-medoid
add r21,r0,_CI_LOOP0    ; set return address for get_distance
jmp [>_GET_DISTANCE]    ; call _GET_DISTANCE
_CI_LOOP0:              ; back from _GET_DISTANCE
add r12,r20,r0          ; r12 is current distance
add r21,r0,_CI_LOOPN    ; set return address for remaining get_distance calls
add r7,r0,1             ; r7 = 1
_CJ_LOOP:               ;
load r23,r7,_POINTS     ; r23 is medoid[r7]
jmp [>_GET_DISTANCE]    ; call _GET_DISTANCE
_CI_LOOPN:              ; back from _GET_DISTANCE
sub r0,r20,r12          ; if old distance <= new distance
cnjmp [>_CJ_NOUPDATE]   ;   goto _CJ_NOUPDATE
add r12,r20,r0          ; else record new distance
add r13,r7,r0           ; and save this medoid in temp_output
_CJ_NOUPDATE:           ; done with (maybe) updating
add r7,r7,1             ; r7 += 1
xor r0,r7,r10           ; if r7 != K
cnjmp [>_CJ_LOOP]       ;   goto _CJ_LOOP
load r3,r13,_POINTS     ; load _POINT corresponding to temp_output
load r3,r3,_RPOINTS     ; reverse-lookup original index of this point
store r3,r15,r6         ; output[i] = original index of medoid
add r6,r6,1             ; r6 += 1
xor r0,r6,r9            ; if r6 != M
cnjmp [>_CI_LOOP]       ;   goto _CI_LOOP

; at this point we have classified all points. compare to the aux tape
add r6,r0,r0            ; r6 = 0
_TESTLOOP:
load r2,r6,_auxTape     ; load value from AUX
load r3,r6,r15          ; load value from memory
xor r0,r2,r3            ; if r2 != r3
cnjmp [>_ERROR]         ; goto _ERROR
add r6,r6,1             ; r6 += 1
xor r0,r6,r9            ; if r6 != M
cnjmp [>_TESTLOOP]      ;   goto _TEST_LOOP

_FINISHED:
answer 0                ; if we got here, we've completed execution successfully


; get_cost function call
; USES r5 as local counter variable
; r25 is retVal
; r26 is retAddr
; runs for (M-K)*( (K+1)*(10 + 7*D) + 5) + 3 cycles
_GET_COST:
add r25,r0,r0           ; r25 = 0
add r5,r10,r0           ; r5 = K
_GCLOOP:                ; do {
load r23,r5,_POINTS     ; r23 = points[r5]
jmp [>_GET_POINT_COST]  ; call _GET_POINT_COST
_GC_GPC_RET:            ; back from _GET_POINT_COST
add r25,r25,r24         ; r25 += r24
add r5,r5,1             ; r5++
xor r0,r5,r9            ; } while (r5
cnjmp [>_GCLOOP]        ;             != M)
jmp r26                 ; return;

; get_point_cost function call
; USES r4 as local counter variable
; r23 is point whose cost we care about                                 - NOT MODIFIED
; r24 is retVal                                                         - MODIFIED
; no retAddr - always returns to _GC_GPC_RET
; runs for 9 + 7*D + (10 + 7*D)*K =~ (K+1) * (10 + 7*D) cycles
_GET_POINT_COST:
load r22,r0,_POINTS     ; medoid 0
add r21,r0,_GPCMED0     ; return address
jmp [>_GET_DISTANCE]    ; call _GET_DISTANCE
_GPCMED0:               ; back from _GET_DISTANCE
add r24,r20,r0          ; r24 = r20
add r21,r0,_GPCMEDN     ; return address into loop
add r4,r0,1             ; r4 = 1
_GPCMEDLOOP:            ; do {
load r22,r4,_POINTS     ; r22 = point[r4]
jmp [>_GET_DISTANCE]    ; call _GET_DISTANCE
_GPCMEDN:               ; back from _GET_DISTANCE
sub r0,r20,r24          ; if (r20 >= r24)
cnjmp [>_NOUPDATE]      ;   goto _NOUPDATE
add r24,r20,r0          ; else r20 = r24
_NOUPDATE:              ;
add r4,r4,1             ; r4++
xor r0,r4,r10           ; } while (r4
cnjmp [>_GPCMEDLOOP]    ;             != K)
jmp [>_GC_GPC_RET]      ; return;


; get_distance function call
; USES r1-r3 as local variables
; r20 is retVal                                                         - MODIFIED
; r21 is retAddr                                                        - NOT MODIFIED
; r22 is arg0 for function, a pointer to the first dimension of elm0    - NOT MODIFIED
; r23 is arg1 for function, a pointer to the first dimension of elm1    - NOT MODIFIED
; runs for 3 + 7*D cycles
_GET_DISTANCE:
add r20,r0,r0       ; initialize distance
sub r1,r8,1         ; i = D - 1
_GDLOOP:            ; do {
load r2,r22,r1      ;     r2 = elm0[i]
load r3,r23,r1      ;     r3 = elm1[i]
sub r3,r3,r2        ;     r3 = elm1[i] - elm0[i]
mull r3,r3,r3       ;     r3 = distance ^ 2
add r20,r20,r3      ;     distance += r3
sub r1,r1,1         ;     i -= 1
cnjmp [>_GDLOOP]    ; } while ( i > 0 )
jmp r21             ; return

_ERROR:
answer 1

$ inTape [_ERROR + 1]
$ auxTape [_inTape + 4096]
_DATA_D  = _inTape
_DATA_M  = [_inTape + 1]
_DATA_K  = [_inTape + 2]
_DATA_L  = [_inTape + 3]
_DATA    = [_inTape + 4]
_POINTS  = [_auxTape + 4096]
_RPOINTS = [_POINTS + 4096]

; insert input tape here
; first entry is D, dimensionality
; second entry is M, number of datapoints
; data for the ith point is located at
; [_DATA + i * D]; there are D dims associated
