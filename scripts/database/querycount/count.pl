#!/usr/bin/perl

my $cmdarg = shift @ARGV;
my $interactive = 1; 

if ( $cmdarg =~/^\-\-(.*)$/ )
{
    if ( $1 eq "non-interactive" )
    {
	$interactive = 0;
    }
    else
    {
	print STDERR "unknown option: " . $cmdarg . "\n";
	die "usage: ./count.pl [--non-interactive] <PGLOGFILE>\n";
    }

    $cmdarg = shift @ARGV; 
}

my $pglogfile = $cmdarg;

unless ( -f $pglogfile )
{
    die "usage: ./count.pl [--non-interactive] <PGLOGFILE>\n";
}

my $pglogfilesize = (stat($pglogfile))[7];

if ( $interactive )
{
    print "Running in INTERACTIVE MODE!\n";
    print "Current log size: ".$pglogfilesize." bytes.\n";

    print "Execute the application task that you are testing/measuring (an API call, a page load, etc.), then press any key. The script will then attempt to parse the increment portion of the PostgresQL log.\n";

    system "stty cbreak </dev/tty >/dev/tty 2>&1";
    my $key = getc(STDIN);
    system "stty -cbreak </dev/tty >/dev/tty 2>&1";
    print "\n";

    my $newsize = (stat($pglogfile))[7];
    my $diff = $newsize - $pglogfilesize;

    system "tail -c ".$diff." < ".$pglogfile." > tail";

    print "Increment: ".$diff." bytes.\n";

    system "./parse.pl < tail > tail.parsed";
}
else
{
    # non-interactive mode; parsing the entire log file.
    system "./parse.pl < $pglogfile > tail.parsed";
}

my $parse_errors = $?;

system "cat tail.parsed | sed 's/ where.*//' | sed 's/ WHERE.*//' | sed 's/ SET .*//' | sed 's/ VALUES .*//' | sort | uniq -c | sort -nr -k 1,2 > tail.counted";

if ( $parse_errors )
{
    print "Parser encountered errors and may have exited prematurely.\n";
    print "The total number of queries, and the specific query numbers below MAY BE INCOMPLETE!\n";
}
else
{
    print "Parsed and counted the queries. Total number:\n";
}

system "awk '{a+=\$1}END{print a}' < tail.counted";

print "\nQueries, counted and sorted: \n\n";

system "cat tail.counted";
