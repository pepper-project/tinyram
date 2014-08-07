#!/usr/bin/perl

use strict;
use warnings;
use bigint;

#
# prototype transition verification function
# 
# given a program P, tapes TIn and TAux, and a transcript of execution
# E, we verify that the execution happened as expected.
#

my @P;      # program ${$P[$addr]}[0] is word0, ${$P[$addr]}[1] is word1
my @tapes;  # tape: tape is @{${$tape[$iter]}[$tapeNum]}
my $version;
my $outform;
my $wordsize;
my $instBits = 5;
my $nRegBits;
my $instMask;
my $uImmMask;
my $reg1Mask;
my $reg2Mask;
my $instShift;
my $uImmShift;
my $reg1Shift;
my $reg2Shift;
my $valueMask;

sub readProgram {
    my $fname = shift @_;       # filename

    my $file;
    open($file,'<'.$fname);
    while (<$file>) {
        chomp (my $line = $_);
        my @fields = split(' ',$line);
        if (5 == $#fields && $fields[0] eq '$' && $fields[1] =~ /tinyram/i) {
        # process tinyram directive
            $version = $fields[2];
            $outform = $fields[3];
            $wordsize = $fields[4];
            $nRegBits = $fields[5];
        } elsif (3 == $#fields && $fields[0] =~ /tape/i) {
        # process tape data
            my $tapeIter = oct($fields[1]);
            my $tapeNum = oct($fields[2]);
            my $tapeVal = oct($fields[3]);      # NOTE: if we exceed 64 bits this will break!
            push(@{${$tapes[$tapeIter]}[$tapeNum]},$tapeVal);
        } elsif (2 == $#fields) {
        # process instruction
            my $address = oct($fields[0]);
            my $word0 = oct($fields[1]);
            my $word1 = oct($fields[2]);
            @{$P[$address]} = ($word0,$word1);
        } else {
            warn("Unreadable data in input.");
        }
    }

    close($file);
}

sub dumpProgram {
    printf("\$ TinyRAM %d %d %d %d\n",$version,$outform,$wordsize,$nRegBits);

    for (my $i=0; $i<=$#P; ++$i) {
        if (defined($P[$i])) { printf("0x%x 0x%x 0x%x\n",$i,${$P[$i]}[0],${$P[$i]}[1]); }
    }

    for (my $i=0; $i<=$#tapes; ++$i) {
        if (defined($tapes[$i])) {
            my @tapeIter = @{$tapes[$i]};
            for (my $j=0; $j<=$#tapeIter; ++$j) {
                if (defined($tapeIter[$j])) {
                    foreach my $d (@{$tapeIter[$j]}) {
                        printf("TAPE 0x%x 0x%x 0x%x\n",$i,$j,$d);
                    }
                }
            }
        }
    }
}

sub transcriptName { return sprintf("%s/out_%06x",$ARGV[1],$_[0]); }

sub readTranscript {
    my $tapeIter = shift @_;
    my @transcript;

    my $file;
    open($file,&transcriptName($tapeIter));
    while (<$file>) {
        chomp (my $line = $_);
        my @fields = split(' ',$line);
        my %state;
        if ($#fields == 1 && $fields[0] =~ /value/i) {
            $state{'value'} = oct($fields[1]);
        } elsif ($#fields != (3 + 2**$nRegBits)) {
            warn("Bad transcript line; incorrect #fields.");
        } else {
            for (my $i=0; $i<=$#fields; ++$i) { $fields[$i] = oct($fields[$i]); }
            $state{'pc'} = $fields[0];
            $state{'regs'} = [ @fields[1..$#fields-3] ];
            $state{'inTapePosn'} = $fields[$#fields-2];
            $state{'auxTapePosn'} = $fields[$#fields-1];
            $state{'flag'} = $fields[$#fields];
        }
        push(@transcript,\%state);
    }

    close($file);
    return @transcript;
}

sub dumpTranscript {
    foreach my $sRef (@_) {
        my %state = %$sRef;
        if (defined($state{'value'})) {
            printf("VALUE 0x%x\n",$state{'value'});
        } else {
            printf('0x%x ',$state{'pc'});
            foreach my $reg (@{$state{'regs'}}) { printf('0x%x ',$reg); }
            printf('0x%x ',$state{'inTapePosn'});
            printf('0x%x ',$state{'auxTapePosn'});
            printf("0x%x\n",$state{'flag'});
        }
    }
}

sub verifyTransition {
    my %s0 = %{shift @_};
    my %s1 = %{shift @_};
    my @tapes = @{shift @_};
    my @t0 = defined($tapes[0]) ? @{$tapes[0]} : ();
    my @t1 = defined($tapes[1]) ? @{$tapes[1]} : ();

    my @s0R = defined($s0{'regs'}) ? @{$s0{'regs'}} : ();
    my @s1R = defined($s1{'regs'}) ? @{$s1{'regs'}} : ();

    my $w0 = ${$P[$s0{'pc'}]}[0];
    my $w1 = ${$P[$s0{'pc'}]}[1];

    my $inst = ($w0 & $instMask) >> $instShift;
    my $uImm = ($w0 & $uImmMask) >> $uImmShift;
    my $reg1 = ($w0 & $reg1Mask) >> $reg1Shift;
    my $reg2 = ($w0 & $reg2Mask) >> $reg2Shift;

    # verify registers other than reg1 did not change
    # when the instruction is ANSWER, @s1R is an empty list
    # thus this does not happen
    for (my $i=0; $i<=$#s1R; ++$i) {
        return(0) unless ($i == $reg1 || $s0R[$i] == $s1R[$i]);
    }

    # verify PC is correctly incremented unless it's a jump instruction
    # ACCEPT does not increment the PC and s1 is not a real state when $inst = 31
    if ($inst < 20 || $inst > 22 && $inst != 31) { return(0) if ($s1{'pc'} != $s0{'pc'} + 1); }

    # verify tape pointers didn't change unless it's a READ instruction
    # on ACCEPT we do not verify this because s1 is not a real state
    # note: this condition should be implemented differently in the real system; < is very inefficient
    if ($inst < 30) {
        (($s0{'inTapePosn'}==$s1{'inTapePosn'})&&($s0{'auxTapePosn'}==$s1{'auxTapePosn'})) || return(0);
    }

    if ($inst == 0) { # AND
        my $op2 = $uImm ? $w1 : $s0R[$w1];
        ($s1R[$reg1] == ($s0R[$reg2] & $op2)) || return(0);
        (($s1R[$reg1] != 0) ^ $s1{'flag'}) || return(0);
    } elsif ($inst == 1) { # OR
        my $op2 = $uImm ? $w1 : $s0R[$w1];
        ($s1R[$reg1] == ($s0R[$reg2] | $op2)) || return(0);
        (($s1R[$reg1] != 0) ^ $s1{'flag'}) || return(0);
    } elsif ($inst == 2) { # XOR
        my $op2 = $uImm ? $w1 : $s0R[$w1];
        ($s1R[$reg1] == ($s0R[$reg2] ^ $op2)) || return(0);
        (($s1R[$reg1] != 0) ^ $s1{'flag'}) || return(0);
    } elsif ($inst == 3) { # NOT
        my $op2 = $uImm ? $w1 : $s0R[$w1];
        ($s1R[$reg1] == (~$op2 & $valueMask)) || return(0);
        (($s1R[$reg1] != 0) ^ $s1{'flag'}) || return(0);
    } elsif ($inst == 4) { # ADD
        my $op2 = $uImm ? $w1 : $s0R[$w1];
        my $arith = $s0R[$reg2] + $op2;
        ($s1R[$reg1] == ($arith & $valueMask)) || return(0);
        (($arith <= $valueMask) ^ $s1{'flag'}) || return(0);
    } elsif ($inst == 5) { # SUB
        my $op2 = $uImm ? $w1 : $s0R[$w1];
        my $arith = $s0R[$reg2] - $op2;
        ($s1R[$reg1] == ($arith & $valueMask)) || return(0);
        (($arith >= 0) ^ $s1{'flag'}) || return(0);
    } elsif ($inst == 6) { # MULL
        my $op2 = $uImm ? $w1 : $s0R[$w1];
        my $arith = $s0R[$reg2] * $op2;
        ($s1R[$reg1] == ($arith & $valueMask)) || return(0);
        (($arith <= $valueMask) ^ $s1{'flag'}) || return(0);
    } elsif ($inst == 7) { # UMULH
        my $op2 = $uImm ? $w1 : $s0R[$w1];
        my $arith = $s0R[$reg2] * $op2;
        ($s1R[$reg1] == (($arith >> $wordsize) & $valueMask)) || return(0);
        (($s1R[$reg1] == 0) ^ $s1{'flag'}) || return(0);
    } elsif ($inst == 8) { # SMULH
        my $op2 = &signedValue($uImm ? $w1 : $s0R[$w1]);
        my $arith = &signedValue($s0R[$reg2]) * $op2;
        ($s1R[$reg1] == (($arith >> $wordsize) & $valueMask)) || return(0);
        ((($s1R[$reg1] == 0)||($s1R[$reg1] == $valueMask)) ^ $s1{'flag'}) || return(0);
    } elsif ($inst == 9) { # UDIV
        my $op2 = $uImm ? $w1 : $s0R[$w1];
        if ($op2 == 0) { (($s1R[$reg1]==0) && $s1{'flag'}) || return(0); }
        else {
            (($s1R[$reg1] == $s0R[$reg2] / $op2) && !$s1{'flag'}) || return(0);
        }
    } elsif ($inst == 10) { # UMOD
        my $op2 = $uImm ? $w1 : $s0R[$w1];
        if ($op2 == 0) { (($s1R[$reg1]==0) && $s1{'flag'}) || return(0); }
        else {
            (($s1R[$reg1] == $s0R[$reg2] % $op2) && !$s1{'flag'}) || return(0);
        }
    } elsif ($inst == 11) { # SHL
        my $op2 = $uImm ? $w1 : $s0R[$w1];
        my $arith = $s0R[$reg2] << $op2;
        ($s1R[$reg1] == ($arith & $valueMask)) || return(0);
        ($s1{'flag'} ^ !($s0R[$reg2] & 2**($wordsize-1))) || return(0);
    } elsif ($inst == 12) { # SHR
        my $op2 = $uImm ? $w1 : $s0R[$w1];
        my $arith = $s0R[$reg2] >> $op2;
        ($s1R[$reg1] == ($arith & $valueMask)) || return(0);
        ($s1{'flag'} ^ !($s0R[$reg2] & 1)) || return(0);
    } elsif ($inst == 13) { # CMPE
        my $op2 = $uImm ? $w1 : $s0R[$w1];
        my $cmp = $s0R[$reg2] == $op2;
        ($s1R[$reg1] == $s0R[$reg1]) || return(0);
        ($s1{'flag'} ^ !$cmp) || return(0);
    } elsif ($inst == 14) { # CMPA (unsigned >)
        my $op2 = $uImm ? $w1 : $s0R[$w1];
        my $cmp = $s0R[$reg2] > $op2;
        ($s1R[$reg1] == $s0R[$reg1]) || return(0);
        ($s1{'flag'} ^ !$cmp) || return(0);
    } elsif ($inst == 15) { # CMPAE (unsigned >=)
        my $op2 = $uImm ? $w1 : $s0R[$w1];
        my $cmp = $s0R[$reg2] >= $op2;
        ($s1R[$reg1] == $s0R[$reg1]) || return(0);
        ($s1{'flag'} ^ !$cmp) || return(0);
    } elsif ($inst == 16) { # CMPG (signed >)
        my $op2 = &signedValue($uImm ? $w1 : $s0R[$w1]);
        my $cmp = &signedValue($s0R[$reg2]) > $op2;
        ($s1R[$reg1] == $s0R[$reg1]) || return(0);
        ($s1{'flag'} ^ !$cmp) || return(0);
    } elsif ($inst == 17) { # CMPGE (signed >=)
        my $op2 = &signedValue($uImm ? $w1 : $s0R[$w1]);
        my $cmp = &signedValue($s0R[$reg2]) >= $op2;
        ($s1R[$reg1] == $s0R[$reg1]) || return(0);
        ($s1{'flag'} ^ !$cmp) || return(0);
    } elsif ($inst == 18) { # MOV
        my $op2 = $uImm ? $w1 : $s0R[$w1];
        ($s1R[$reg1] == $op2) || return(0);
        ($s1{'flag'} == $s0{'flag'}) || return(0);
    } elsif ($inst == 19) { # CMOV
        my $op2 = $s0{'flag'} ? ($uImm ? $w1 : $s0R[$w1]) : $s0R[$reg1];
        ($s1R[$reg1] == $op2) || return(0);
        ($s1{'flag'} == $s0{'flag'}) || return(0);
    } elsif ($inst == 20) { # JMP
        my $op2 = $uImm ? $w1 : $s0R[$w1];
        ($s1R[$reg1] == $s0R[$reg1]) || return(0);
        ($s1{'flag'} == $s0{'flag'}) || return(0);
        ($s1{'pc'} == $op2) || return(0);
    } elsif ($inst == 21) { #CJMP
        my $op2 = $s0{'flag'} ? ($uImm ? $w1 : $s0R[$w1]) : ($s0{'pc'}+1);
        ($s1R[$reg1] == $s0R[$reg1]) || return(0);
        ($s1{'flag'} == $s0{'flag'}) || return(0);
        ($s1{'pc'} == $op2) || return(0);
    } elsif ($inst == 22) { # CNJMP
        my $op2 = $s0{'flag'} ? ($s0{'pc'}+1) : ($uImm ? $w1 : $s0R[$w1]);
        ($s1R[$reg1] == $s0R[$reg1]) || return(0);
        ($s1{'flag'} == $s0{'flag'}) || return(0);
        ($s1{'pc'} == $op2) || return(0);
    } elsif ($inst == 28) { # STORE
        # we don't check the state of $reg1 because
        # we are not verifying memory consistency
        # pc and tape positions are checked above
        # we just need to check that the flag register didn't change
        ($s1R[$reg1] == $s0R[$reg1]) || return(0);
        ($s1{'flag'} == $s0{'flag'}) || return(0);
    } elsif ($inst == 29) { # LOAD
        # as in STORE, above
        ($s1{'flag'} == $s0{'flag'}) || return(0);
    } elsif ($inst == 30) { # READ
        my $op2 = $uImm ? $w1 : $s0R[$w1];
        if ($op2 == 0) {
            ($s1{'inTapePosn'} == $s0{'inTapePosn'}+1) || return(0);
            ($s1{'auxTapePosn'} == $s0{'auxTapePosn'}) || return(0);
            if ($s0{'inTapePosn'} > $#t0) {
                (($s1{'flag'}==1) && ($s1R[$reg1]==0)) || return(0);
            } else {
                (($s1{'flag'}==0) && ($s1R[$reg1]==$t0[$s0{'inTapePosn'}])) || return(0);
            }
        } elsif ($op2 == 1) {
            ($s1{'inTapePosn'} == $s0{'inTapePosn'}) || return(0);
            ($s1{'auxTapePosn'} == $s0{'auxTapePosn'}+1) || return(0);
            if ($s0{'auxTapePosn'} > $#t1) {
                (($s1{'flag'}==1) && ($s1R[$reg1]==0)) || return(0);
            } else {
                (($s1{'flag'}==0) && ($s1R[$reg1]==$t1[$s0{'auxTapePosn'}])) || return(0);
            }
        } else {
            ($s1{'inTapePosn'} == $s0{'inTapePosn'}) || return(0);
            ($s1{'auxTapePosn'} == $s0{'auxTapePosn'}) || return(0);
            (($s1{'flag'}==1) && ($s1R[$reg1]==0)) || return(0);
        }
    } elsif ($inst == 31) { # ANSWER
        my $op2 = $uImm ? $w1 : $s0R[$w1];
        ($s1{'value'} == $op2) || return(0);
    } else { # invalid instruction
        return(0);
    }

    return(1);
}

sub verifyTranscript {
    my $tapeIter = shift @_;

    return(-1) unless (-f &transcriptName($tapeIter));

    my @tapeDeck = $tapes[$tapeIter];
    my @transcript = &readTranscript($tapeIter);

    my @errStep;

    my $i;
    for ($i=0; $i<$#transcript; ++$i) {
        push(@errStep,$i) unless (&verifyTransition($transcript[$i],$transcript[$i+1],@tapeDeck));
    }
    # final step must be an accept, and must be followed by a VALUE in the transcript

    return ($#errStep+1, @errStep);
}

sub verifyTranscripts {
    for (my $i=0; $i<=$#tapes; ++$i) {  # for each tape deck, verify
        next unless defined($tapes[$i]);
        my @ver = &verifyTranscript($i);

        if ($ver[0] == 0) { printf("Transcript %d verified.\n",$i); }
        elsif ($ver[0] == -1) { printf(">>ERROR: No transcript corresponding to tape %d.\n",$i); }
        else { printf("%d erroneous steps found in %d: %s\n",shift @ver,$i,join(" ",@ver)); }
    }
}

sub signedValue {
    my $num = shift @_;
    if ($num >= 2**($wordsize-1)) { return ($num - 2**$wordsize); }
    else                          { return $num; }
}

if (0 > $#ARGV) {
    print STDERR "Usage: $0 <progfile> [<transcriptDir>]\n";
    exit(1);
}
$ARGV[1] ||= ".";

&readProgram($ARGV[0]);

# make the masks for the various fields
$instShift = $wordsize - $instBits;
$uImmShift = $wordsize - (1 + $instBits);
$reg1Shift = $wordsize - (1 + $instBits + $nRegBits);
$reg2Shift = $wordsize - (1 + $instBits + 2*$nRegBits);
$valueMask = oct("0b".('1'x$wordsize));
# valueMask looks funny but this ensures that it works without overflow up to wordsize=64

$instMask = (2**$instBits - 1) << $instShift;
$uImmMask =                 1  << $uImmShift;
$reg1Mask = (2**$nRegBits - 1) << $reg1Shift;
$reg2Mask = (2**$nRegBits - 1) << $reg2Shift;

&verifyTranscripts();

