$ TinyRAM 0 2 64 5
_BASE = 0x100

jmp _BASE

$ org _BASE

; initialize
mov r1,1
mov r2,1
read r3,0 ; read in the factorial argument from the input tape

cnjmp _FOO

mov r3,10 ; if we fail to read in a value, default is 10

_FOO : 

mull r1,r1,r2
add r2,r2,1
cmpa r2,r3
cnjmp _FOO

answer r1

tape 0 , 0 , 1
tape 1 , 0 , 1
tape 2 , 0 , 2
tape 3 , 0 , 3
tape 4 , 0 , 4
tape 5 , 0 , 5
tape 6 , 0 , 6
tape 7 , 0 , 7
tape 8 , 0 , 8
tape 9 , 0 , 9
tape 10 , 0 , 10
tape 11 , 0 , 11
tape 12 , 0 , 12
tape 13 , 0 , 13
tape 14 , 0 , 14
tape 15 , 0 , 15
tape 16 , 0 , 16
tape 17 , 0 , 17
tape 18 , 0 , 18
tape 19 , 0 , 19
tape 20 , 0 , 20

