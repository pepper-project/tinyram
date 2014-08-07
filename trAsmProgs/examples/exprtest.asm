$ TinyRAM 0 0 32 5
$ org [0x3A - 2]
add r1,r1,r2
; this is a comment

_FOO : 
add r2,r1,r2
not r3,15138

_BAR : sub r2,r2,0xFFFFFFFF

jmp [_BAR - 2]

mull r2,r2,r2

_BAZ = 0o77

_FOO = [ _BAZ + 0b11000000 ]

sub r2,r2,[ 0xFF ^ [ 0b10110000 | [ 0b100 >> 2 ] ] ]
not r2,[[[[0x100 - 1] & _BAR] ^ [0xde % 0xad]] << 12]
answer [~_FOO]
answer [!_FOO]
answer 0
answer r0

#asdf 0

answer 0

$ iter 0
tape 0, 1
tape 0, 2
tape 0, 3
tape 0, 4
tape 1, 1
tape 1, 4
tape 1, 9
tape 1, 16

_QUUX:
$ iter 1
tape 0, 1
tape 0, 2
tape 0, 3
tape 0, 4
tape 1, 1
tape 1, 4
tape 1, 9
tape 1, 16

answer _QUUX
