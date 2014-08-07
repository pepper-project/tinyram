$ TinyRAM 1 2 32 5

;$org 0

; register layout
; r0 is zero reg
load r1,r0,_NLEN    ; needle length
load r2,r0,_HLEN    ; haystack length
add r3,r1,_PAT      ; haystack location in memory
sub r4,r1,1         ; NLEN - 1
sub r5,r2,r4        ; index in haystack at which match is impossible
sub r6,r0,1         ; r6 = -1

store r6,r0,_TAB    ; _TAB[0] = -1
add r7,r0,1         ; tpos = 1
store r0,r7,_TAB    ; _TAB[1] = 0
add r7,r7,1         ; tpos = 2
;add r8,r0,r0       ; cand = 0

_MKTAB:
sub r0,r7,r1        ; if tpos >= nlen
cnjmp [>_SEARCH]    ;   goto _SEARCH

sub r9,r7,1         ; r9 = tpos - 1
load r9,r9,_PAT     ; r9 = needle[tpos-1]
load r10,r8,_PAT    ; r10 = needle[cand]
xor r0,r9,r10       ; if r9 != r10
cnjmp [>_NCAND]     ; check candidate
add r8,r8,1         ; else cand++
store r8,r7,_TAB    ; _TAB[tpos] = cand
add r7,r7,1         ; tpos++
jmp [>_MKTAB]       ;   continue

_NCAND:
sub r0,r8,1         ; if cand < 1
cjmp [>_ZTAB]       ;   goto _ZTAB
load r8,r8,_TAB     ; else cand = _TAB[cand]
jmp [>_MKTAB]       ;   continue

_ZTAB:
store r0,r7,_TAB    ; _TAB[tpos] = 0
add r7,r7,1         ; tpos++
jmp [>_MKTAB]       ;   continue

_SEARCH:

add r7,r0,r0        ; m = 0
add r8,r0,r0        ; i = 0

_SNEXT:
sub r0,r7,r5        ; if m >= (len(haystack) - len(needle) + 1)
cnjmp [>_NOTFOUND]  ;    not found

add r9,r7,r8        ; r9 = m + i
load r9,r9,r3       ; r9 = haystack[m + i]
load r10,r8,_PAT    ; r10 = needle[i]
xor r0,r9,r10       ; if r9 != r10
cnjmp [>_CKTAB]     ;   goto _CKTAB
xor r0,r8,r4        ; if i == last
cjmp [>_FOUND]      ;   success!
add r8,r8,1         ; else i++
jmp [>_SNEXT]       ;   continue

_CKTAB:
load r9,r8,_TAB     ; r9 = _TAB[i]
xor r0,r9,r6        ; if r9 == -1
cjmp [>_MINC]       ;   goto _MINC
xor r0,r9,r0        ; if r9 == 0
cjmp [>_MINC]       ;   goto _MINC
load r8,r8,_TAB     ; i = _TAB[i]
load r9,r8,_TAB     ; r9 = _TAB[i]
add r7,r7,r8        ; m = m + i
sub r7,r7,r9        ;           - _TAB[i]
jmp [>_SNEXT]       ;   continue

_MINC:
add r8,r0,r0        ; i = 0
add r7,r7,1         ; m++
jmp [>_SNEXT]

_NOTFOUND:
add r7,r2,r0        ; m = len(haystack) -- indicates not found
_FOUND:

load r9,r0,_auxTape ; load answer
xor r0,r9,r7        ; if r9 != r7
cnjmp [>_ERROR]     ;   oops
answer 0            ; correct!
_ERROR:
answer 1            ; oops

$inTape [_ERROR + 1]
$auxTape [_inTape + 4096]
_TAB = [_auxTape + 4096]

_NLEN =  _inTape
_HLEN = [_inTape + 1]
_PAT  = [_inTape + 2]

