$ TinyRAM 1 2 32 5

;$org 0

; register layout
; r0 is zero reg
load r1,r0,_LEN     ; pattern length
load r2,r0,_ALEN    ; alphabet length
; r3 is counter variable
; r4 is temporary variable
sub r5,r1,1         ; r5 = _LEN - 1
; r6 is temporary variable

sub r3,r2,1         ; r3 = r2 - 1
_INITLIST:
store r1,r3,_OUT    ; _OUT[r3] = _LEN
sub r3,r3,1         ; if --r3 >= 0
cnjmp [>_INITLIST]  ;   goto _INITLIST

add r3,r0,r0        ; i = 0
_FINDLAST:
load r4,r3,_PAT     ; r4 = _PAT[i]
sub r6,r5,r3        ; r6 = _LEN - 1 - i
store r6,r4,_OUT    ; _OUT[_PAT[i]] = r6
add r3,r3,1         ; r3 += 1
sub r0,r3,r5        ; if r3 < _LEN - 1
cjmp [>_FINDLAST]   ;   goto _FINDLAST

add r3,r0,r0        ; i = 0
_COMPARE:
load r4,r3,_OUT     ; r4 = _OUT[r3]
load r6,r3,_auxTape ; r6 = _auxTape[r3]
xor r0,r4,r6        ; if r4 != r6
cnjmp [>_ERROR]     ;   oops
add r3,r3,1         ; r3 += 1
sub r0,r3,r2        ; if r3 < r2
cjmp [>_COMPARE]    ;   continue
answer 0            ; else correct

_ERROR:
answer 1            ; oops

$inTape [_ERROR + 1]
$auxTape [_inTape + 4096]
_OUT = [_auxTape + 4096]

_LEN = _inTape
_ALEN = [_inTape + 1]
_PAT = [_inTape + 2]

