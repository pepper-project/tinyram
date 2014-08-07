$ TinyRAM 1 2 32 5

;$org 0

; register layout
; r0 is zero reg
load r1,r0,_LEN     ; input length
; r2 is loop variable i
; r3 is loop variable j
; r4 is data
; r5 is dcount
; r6 is temporary
; r7 is outp/midp

load r4,r2,_DATA    ; read in first datum
;add r5,r0,r0       ; dcount = 0 (initial reg value)
;add r7,r0,r0       ; midp = 0 (initial reg value)
add r2,r0,1         ; i = 1

; compress data
_COMPRESS:
load r6,r2,_DATA    ; r6 = _DATA[i]
xor r0,r6,r4        ; if r6 != r4
cnjmp [>_GOT_DIFF]  ;   goto GOT_DIFF

add r5,r5,1         ; else increment count
add r2,r2,1         ; i++
sub r0,r2,r1        ; if i < SIZE
cjmp [>_COMPRESS]   ;   continue compressing
jmp [>_COMP_DONE]   ; else done with compression

_GOT_DIFF:
store r4,r7,_MID    ; _MID[midp] = data
store r5,r7,_MNXT   ; _MID[midp + 1] = count
add r7,r7,2         ; midp += 2
add r4,r6,r0        ; data = _DATA[i]
add r5,r0,r0        ; dcount = 0

add r2,r2,1         ; i++
sub r0,r2,r1        ; if i < SIZE
cjmp [>_COMPRESS]   ;   continue compessing
                    ; else we're done

_COMP_DONE:
store r4,r7,_MID
store r5,r7,_MNXT

add r7,r0,r0        ; outp = 0
add r2,r0,r0        ; i = 0

_DECOMPRESS:
load r4,r2,_MID     ; data = _MID[i]
load r5,r2,_MNXT    ; dcount = _MID[i + 1]
add r2,r2,2         ; i += 2

_WRITE_LOOP:
store r4,r7,_OUT    ; _OUT[outp] = data
add r7,r7,1         ; outp++
sub r5,r5,1         ; if r5-- >= 1
cnjmp [>_WRITE_LOOP]; continue

sub r0,r7,r1        ; if outp < SIZE
cjmp [>_DECOMPRESS] ; continue decompressing

answer 0            ; if we got here, correct

_ERROR:
answer 1            ; we never get here

$inTape  [_ERROR  + 1]
$auxTape [_inTape + 4096]    ; no aux tape

_LEN  =  _inTape
_DATA = [_inTape + 1]
_DNXT = [_DATA + 1]

_MID  =  _auxTape
_MNXT = [_MID + 1]

_OUT  = [_MID + 4096]

; how long does this program need to run?
; longest is when every datum is different
$iter 0
tape 0, 128
tape 0, 0
tape 0, 1
tape 0, 2
tape 0, 3
tape 0, 4
tape 0, 5
tape 0, 6
tape 0, 7
tape 0, 0
tape 0, 1
tape 0, 2
tape 0, 3
tape 0, 4
tape 0, 5
tape 0, 6
tape 0, 7
tape 0, 0
tape 0, 1
tape 0, 2
tape 0, 3
tape 0, 4
tape 0, 5
tape 0, 6
tape 0, 7
tape 0, 0
tape 0, 1
tape 0, 2
tape 0, 3
tape 0, 4
tape 0, 5
tape 0, 6
tape 0, 7
tape 0, 0
tape 0, 1
tape 0, 2
tape 0, 3
tape 0, 4
tape 0, 5
tape 0, 6
tape 0, 7
tape 0, 0
tape 0, 1
tape 0, 2
tape 0, 3
tape 0, 4
tape 0, 5
tape 0, 6
tape 0, 7
tape 0, 0
tape 0, 1
tape 0, 2
tape 0, 3
tape 0, 4
tape 0, 5
tape 0, 6
tape 0, 7
tape 0, 0
tape 0, 1
tape 0, 2
tape 0, 3
tape 0, 4
tape 0, 5
tape 0, 6
tape 0, 7
tape 0, 0
tape 0, 1
tape 0, 2
tape 0, 3
tape 0, 4
tape 0, 5
tape 0, 6
tape 0, 7
tape 0, 0
tape 0, 1
tape 0, 2
tape 0, 3
tape 0, 4
tape 0, 5
tape 0, 6
tape 0, 7
tape 0, 0
tape 0, 1
tape 0, 2
tape 0, 3
tape 0, 4
tape 0, 5
tape 0, 6
tape 0, 7
tape 0, 0
tape 0, 1
tape 0, 2
tape 0, 3
tape 0, 4
tape 0, 5
tape 0, 6
tape 0, 7
tape 0, 0
tape 0, 1
tape 0, 2
tape 0, 3
tape 0, 4
tape 0, 5
tape 0, 6
tape 0, 7
tape 0, 0
tape 0, 1
tape 0, 2
tape 0, 3
tape 0, 4
tape 0, 5
tape 0, 6
tape 0, 7
tape 0, 0
tape 0, 1
tape 0, 2
tape 0, 3
tape 0, 4
tape 0, 5
tape 0, 6
tape 0, 7
tape 0, 0
tape 0, 1
tape 0, 2
tape 0, 3
tape 0, 4
tape 0, 5
tape 0, 6
tape 0, 7
