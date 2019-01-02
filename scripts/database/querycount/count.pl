#!/usr/bin/perl

my $pglogfile = shift @ARGV;

unless ( -f $pglogfile )
{
    die "usage: ./count.pl <PGLOGFILE>\n";
}

my $pglogfilesize = (stat($pglogfile))[7];
print "Current size: ".$pglogfilesize." bytes.\n";
print "Press any key when ready.\n";

system "stty cbreak </dev/tty >/dev/tty 2>&1";
my $key = getc(STDIN);
system "stty -cbreak </dev/tty >/dev/tty 2>&1";
print "\n";

my $newsize = (stat($pglogfile))[7];
my $diff = $newsize - $pglogfilesize;

system "tail -c ".$diff." < ".$pglogfile." > tail";

print "Increment: ".$diff." bytes.\n";

system "./parse.pl < tail > tail.parsed";

system "cat tail.parsed | sed 's/ where.*//' | sed 's/ WHERE.*//' | sort | uniq -c | sort -nr -k 1,2 > tail.counted";


print "Parsed and counted the queries. Total number:\n";

system "awk '{a+=\$1}END{print a}' < tail.counted";

print "\nQueries, counted and sorted: \n\n";

system "cat tail.counted";
