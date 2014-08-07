#!/usr/bin/perl -w

use strict;

if (scalar(@ARGV) < 2) {
    print "Usage: $0 <length> <alphabet_length>\n";
    exit(-1);
}

my @out;
for(my $i=0; $i<$ARGV[0]; $i++) {
    $out[$i] = int(rand($ARGV[1]));
}

print STDERR "\$iter 0\ntape 0, $ARGV[0]\ntape 0, $ARGV[1]\ntape 0, ", join("\ntape 0, ", @out), "\n";
print "$ARGV[0] $ARGV[1] ", join(' ', @out), "\n";
