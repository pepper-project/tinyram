#!/usr/bin/perl -w
# vim: syntax=perl

use strict;

if (scalar(@ARGV) < 4) {
    print "Usage: $0 <n_dim> <n_points> <n_medoids> <n_pam_iters>\n";
    exit(-1);
}

my $maxdist = 64;

my $D = shift @ARGV;
my $M = shift @ARGV;
my $K = shift @ARGV;
my $L = shift @ARGV;

if ($M <= $K) {
    print "Error: M must be greater than K, but got (M,K) = ($M, $K)\n";
    exit(-1);
}

print '$ iter 0' . "\n";
print "tape 0 , $D\n";
print "tape 0 , $M\n";
print "tape 0 , $K\n";
print "tape 0 , $L\n";

for (my $i=0; $i<$M; $i++) {
    for (my $j=0; $j<$D; $j++) {
        print "tape 0 , " . int(rand($maxdist)) . "\n";
    }
}
