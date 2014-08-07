#!/usr/bin/perl -w
# vim: syntax=perl

use strict;

if (scalar(@ARGV) < 1) {
    print "Usage: $0 <ndeep>\n";
    exit(-1);
}

my $N = shift @ARGV;

print '$ iter 0' . "\n";
print "tape 0 , $N\n";

for (my $i=0; $i<$N; $i++) {
    print "tape 0, " . ($i+1) % $N . "\n";
}

if ($N > 0) {
    print "tape 1, 1\n";
} else {
    print "tape 1, 0\n";
}
