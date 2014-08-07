$ TinyRAM 1 2 32 5

;$org 0

; memory layout
; 0         : code
; _NDEEP    : # of indirections
; _DATA     : pointer data

; register layout
; r0 is zero reg
load r1,r0,_NDEEP   ; how many indirections?
; add r2,r0,r0      ; pointer; all regs are initialized to 0

_CHASE:
load r2,r2,_DATA    ; r2 = _DATA[r2]
sub r1,r1,1         ; if --r1 >= 0
cnjmp [>_CHASE]     ;   keep chasing

load r3,r0,_auxTape ; answer from auxTape
xor r0,r2,r3        ; if r2 != r3
cnjmp [>_ERROR]     ; error
answer 0            ; else correct

_ERROR:
answer 1            ; oops

$inTape [_ERROR + 1]
$auxTape [_inTape + 16384]

_NDEEP = _inTape
_DATA = [_inTape + 1]

