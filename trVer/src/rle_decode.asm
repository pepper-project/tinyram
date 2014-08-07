$ TinyRAM 1 2 32 5

;$org 0

; register layout
; r0 is zero reg
load r1,r0,_LEN     ; output length
; r3 is input counter
; r5 is data
; r6 is len
; r7 is data from auxtape

_LOADNEXT:
load r5,r3,_DATA    ; r5 = _DATA[i]
load r6,r3,_DNXT    ; r6 = _DATA[i+1]
add r3,r3,2

_CHECKDATA:
load r7,r4,_auxTape ; load datum to check
xor r0,r7,r5        ; if r7 != r5
cnjmp [>_ERROR]     ;    error

add r4,r4,1         ; r4++
xor r0,r4,r1        ; if r4 == _LEN
cjmp [>_DONE]       ;    done

sub r6,r6,1         ; else if r6 < 1
cjmp [>_LOADNEXT]   ;    load next datum
jmp [>_CHECKDATA]   ; else keep checking

_DONE:
answer 0            ; correct
_ERROR:
answer 1            ; oops

$inTape  [_ERROR   + 1]
$auxTape [_inTape  + 1024]
_OUT  =  [_auxTape + 16384]

_LEN  =  _inTape
_DATA = [_inTape + 1]
_DNXT = [_inTape + 2]

