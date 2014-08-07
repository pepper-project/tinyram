$ TinyRAM 1 2 32 5

;$org 0

; memory layout
; 0         : code
; _NDEEP    : # of indirections
; _DATA     : pointer data

; register layout
; r0 is zero reg
load r1,r0,_LEN     ; pattern length
load r2,r0,_ALEN    ; alphabet length
; r3 is counter variable
; r4 is temporary variable
sub r5,r1,1         ; r5 = _LEN - 1
; r6 is temporary variable
load r7,r0,_HLEN    ; haystack length
add r8,r1,_PAT      ; haystack location in memory

sub r3,r2,1         ; r3 = r2 - 1
_INITLIST:
store r1,r3,_TAB    ; _TAB[r3] = _LEN
sub r3,r3,1         ; if --r3 >= 0
cnjmp [>_INITLIST]  ;   goto _INITLIST

add r3,r0,r0        ; i = 0
_FINDLAST:
load r4,r3,_PAT     ; r4 = _PAT[i]
sub r6,r5,r3        ; r6 = _LEN - 1 - i
store r6,r4,_TAB    ; _TAB[_PAT[i]] = r6
add r3,r3,1         ; r3 += 1
sub r0,r3,r5        ; if r3 < _LEN - 1
cjmp [>_FINDLAST]   ;   goto _FINDLAST

; now we have the occurs table at _TAB
add r4,r8,r0        ; hptr = haystack
add r6,r7,r0        ; hlen = haystack length
_FINDLOOP:
sub r0,r6,r1        ; if hlen < length
cjmp [>_NOTFOUND]   ;   not found
add r3,r5,r0        ; i = last
_COMPARE:
load r9,r4,r3       ; r9 = hptr[i]
load r10,r3,_PAT    ; r10 = _PAT[i]
xor r0,r9,r10       ; if r9 != r10
cnjmp [>_SKIP]      ;   skip ahead
sub r3,r3,1         ; else if --r3 < 0
cjmp [>_FOUND]      ;   found a match
jmp [>_COMPARE]     ; else continue

_SKIP:
load r9,r4,r5       ; r9 = hptr[last]
load r9,r9,_TAB     ; skip = _TAB[hptr[last]]
add r4,r4,r9        ; hptr += skip
sub r6,r6,r9        ; hlen -= skip
cjmp [>_NOTFOUND]   ; if hlen < 0, not found
jmp [>_FINDLOOP]    ; else continue searching

_FOUND:
sub r7,r4,r8        ; if we found it, r7 = hptr - haystack
_NOTFOUND:          ; else haystack_length (not found)

load r4,r0,_auxTape ; load answer
xor r0,r4,r7        ; if r4 != r7
cnjmp [>_ERROR]     ;   oops
answer 0            ; else correct!

_ERROR:
answer 1            ; oops

$inTape [_ERROR + 1]
$auxTape [_inTape + 4096]
_TAB = [_auxTape + 4096]

_LEN  =  _inTape
_ALEN = [_inTape + 1]
_HLEN = [_inTape + 2]
_PAT  = [_inTape + 3]

$iter 0
tape 0, 5
tape 0, 10
tape 0, 15
tape 0, 1
tape 0, 2
tape 0, 3
tape 0, 4
tape 0, 5
tape 0, 6
tape 0, 7
tape 0, 8
tape 0, 9
tape 0, 5
tape 0, 1
tape 0, 2
tape 0, 6
tape 0, 4
tape 0, 5
tape 0, 1
tape 0, 2
tape 0, 3
tape 0, 4
tape 0, 5
