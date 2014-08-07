#!/usr/bin/perl
# generate N random matrices of size S
# -s size
# -n num

use warnings 'all';
use strict;
use Getopt::Std;

my %opts;
getopts('s:n:fl0',\%opts);
$opts{'s'} ||= 3;
$opts{'n'} ||= 1;

sub matmult {
    my $size = shift @_;
    my $arrA = shift @_;
    my $arrB = shift @_;

    my @arrC;

    for (my $i=0;$i<$size;$i++) {
    for (my $j=0;$j<$size;$j++) {
        my $tmp = 0;
        for (my $k=0;$k<$size;$k++) {
            $tmp += $$arrA[$i*$size+$k] * $$arrB[$k*$size+$j];
        }
        $arrC[$i*$size+$j] = $tmp;
    }}

    return \@arrC;
}

sub printmat {
    my $arr = shift @_;
    my $tapeNo = shift @_;

    foreach my $elm (@$arr) {
        print "tape $tapeNo , $elm\n";
    }
}

sub randmat {
    my $size = shift @_;
    my @outarr;

    for (my $i=0;$i<$size;$i++) {
        push(@outarr, int(rand(1024)));
    }
    
    return \@outarr;
}

if ($opts{'l'}) {   # make list inputs for mergesort
    for (my $i=0;$i<$opts{'n'};$i++) {
        my $listA = &randmat($opts{'s'});
        my @sListA;

        if ($opts{'f'}) {
            my $sl = &randmat($opts{'s'});
            @sListA = @$sl;
        } else {
            @sListA = sort {int($a)<=>int($b)} @$listA;
        }

        print "\$ iter $i\ntape 0 , $opts{'s'}\n";
        &printmat($listA,0);
        unless ($opts{'0'}) {
            &printmat(\@sListA,1);
        }
    }
} else {    # make matrix inputs for matmult
    for (my $i=0;$i<$opts{'n'};$i++) {
        my $arrA = &randmat($opts{'s'}*$opts{'s'});
        my $arrB = &randmat($opts{'s'}*$opts{'s'});
        my $arrAB;
        if ($opts{'f'}) {
            $arrAB = &randmat($opts{'s'}*$opts{'s'});
        } else {
            $arrAB = &matmult($opts{'s'},$arrA,$arrB);
        }

        print "\$ iter $i\ntape 0 , $opts{'s'}\n";
        &printmat($arrA,0);
        &printmat($arrB,0);
        unless ($opts{'0'}) {
            &printmat($arrAB,1);
        }
    }
}
