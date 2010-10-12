#! /usr/bin/perl

package Toolkit;

# (C)CDF gnuplot plotter

use strict;
use warnings;
use Getopt::Long;

my ($outfile, $labelRe, $term, $range, $params, $comp, $help);

my $usage = '
usage:  -out <output file name> 
        [-comp]
        [-label <plot title extracting RE>]
        [-params <params for plots>]
        [-term <gnuplot output terminal>]
        [-range <x range of plot>]
        fileNames...
';

GetOptions("label=s" => \$labelRe, "params=s" => \$params,
	   "out=s" => \$outfile, "term=s" => \$term, 
	   "range=s" => \$range, "comp!" => \$comp, "help|?!" => \$help);

if ($help) {
    print '(Complementary) Cumulative Distribution plotter. Creates CDF plots
from timestamped hitcount reports using gnuplot. All given input files
are plotted to the same graph.';
    print "\n$usage";
    print '
options:
   
out    Output file name\'s prefix

comp   Do Complementary CDF instead of normal CDF

label  A regular expression which is used to parse labels for plots.
       Use capture groups to get the content(s).
       Default = \'([^_]*)_\' (everything up to the first underscore)

params Gnuplot plotting style parameters. Default = \'smooth unique\'

term   Name of the terminal used for gnuplot output. Use "na" for no terminal
       (only the gnuplot output file is created). Default = emf

range  Range of x-values in the resulting graph (e.g. 0:100). 
       Default = no range, i.e. automatic scaling by gnuplot
';
    exit();
}

if (not defined $outfile or not @ARGV) {
    print "Missing required parameter(s)\n";
    print $usage;
    exit();
}

$term = "emf" unless defined $term;
$params = "smooth unique" unless defined $params;
$labelRe = '([^_]*)_' unless defined $labelRe;

my $plotfile = "$outfile.gnuplot";

open(PLOT, ">$plotfile") or die "Can't open plot output file $plotfile : $!";
print PLOT "set logscale x\n";
if ($comp) {
  print PLOT "set ylabel \"1-P(X <= x)\"\n";
}
else {
  print PLOT "set ylabel \"P(X <= x)\"\n";
}
if (defined($range)) {
    print PLOT "set xrange [$range]\n";
}

if (not $term eq "na") {
  print PLOT "set terminal $term\n";
}
print PLOT "plot ";

my $round = 0;
while (my $infile = shift(@ARGV)) {
    if ($round > 0) {
      print PLOT ", ";
    }
    $round++;

    open(INFILE, "$infile") or die "Can't open $infile : $!";

    # strip 1-3 char extension (if any)
    if ($infile =~ m/.*\.\w{1,3}/) {
      ($infile) = $infile =~ m/(.*)\.\w{1,3}/;
    }

    my $cdffile = "$infile.cdf";
    
    my $totalSum = 0;
    my $hitcount;
    my $time;

    open(OUTFILE,">$cdffile") or die "Can't open output file $cdffile : $!";

    while (<INFILE>) {
      ($hitcount) = m/\d+.\d+ (\d+)/;
      $totalSum += $hitcount;
    }

    seek(INFILE,0,0);

    my $cumSum = 0;

    while (<INFILE>) {
      ($time, $hitcount) = m/(\d+.\d+) (\d+)/;
      if ($hitcount > 0) {
        $cumSum += $hitcount;
        my $finalValue;
          if ($comp) { 
            $finalValue = 1 - ($cumSum / $totalSum);
          }
          else {
            $finalValue = ($cumSum / $totalSum);
          }
        print OUTFILE "$time ",$finalValue,"\n";
      }
    }

    close(OUTFILE);
    close(INFILE);

    print PLOT "'$cdffile'";
    if (defined($labelRe)) { # extract label for legend
      my @labels = $infile =~ m/$labelRe/;
      die "Cant' extract label using \'$labelRe\' from $infile" unless @labels;
      print PLOT " title \"@labels\"";
    }
    print PLOT " $params";

}

print PLOT "\n";
close(PLOT);

if (not $term eq "na") {
  system("gnuplot $plotfile > $outfile") == 0 or die "Can't run gnuplot: $?";
}
