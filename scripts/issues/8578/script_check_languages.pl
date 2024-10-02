#!/usr/bin/perl
use utf8;


$USAGE = "usage: <this_script.pl> -check[codes|names] citation_languages_10762.tsv citation_languages_develop.tsv\n";

$mode = shift @ARGV;
$citation_languages_file_new = shift @ARGV;
$citation_languages_file_develop = shift @ARGV;

unless ( $mode eq "-checkcodes" || $mode eq "-checknames" )
{
    print STDERR $USAGE;
    exit 1;
}
    

unless ( $citation_languages_file_new
	 && $citation_languages_file_develop
	 && -f $citation_languages_file_new
	 && -f $citation_languages_file_develop )
{
    print STDERR $USAGE; 
    exit 1; 
}

print "Parsing the new languages list...\n";

open FINP, $citation_languages_file_new; 

while ( $_ = <FINP>)
{
    chop;
    
    @_ = split ("\t", $_);

    if ($_[1] ne "language")
    {
	die "illegal entry: $_\n";
    }

    $mainname = $_[2];
    if ( $mainname !~/^[\p{L}\p{M}].*[\p{L}\p{M} \)']$/ )
    {
	#print STDERR "warning: (potentially?) illegal language name (main): " . $mainname . "\n";
    }

    if ( $mainname ne "Not applicable" && $_[3] !~/^[a-z][a-z][a-z]$/ )
    {
	die "Error: Illegal entry: no 3-letter identifier code in the 3rd column: " . $_ . "\n";
    }
    else
    {
	$identifier = $_[3];
	$LANGUAGE_NAMES{$identifier} = $mainname;
    }
    
    unless ($_[4] >= 0)
    {
	die "Error: illegal entry: (no CVV ref. number) " . $_ . "\n";
    }

    $threelettercode = $_[5];

    unless ( $mainname eq "Not applicable" || $threelettercode =~/^[a-z][a-z][a-z]$/ )
    {
        die "Error: No 3-letter code in the 5th column: " . join ("***", @_) . "\n";
    }
    else
    {
	$hash = {};
	$hash->{$threelettercode} = 1; 
    
    }

    if ( $identifier ne $threelettercode )
    {
	print STDERR "warning: the identifier in the 3rd column is different from the first \"alternate\" in the 5th: " . $_ . "\n";
    }
    

    for ( $c = 6; $c <= $#_; $c++ )
    {
	$alternate = $_[$c];
	$hash->{$alternate} = 1 if $alternate ne "";
    }

    $LANGUAGE_ALTERNATES{$identifier} = $hash; 
    $num_new++;
}

close FINP;
print $num_new . " language entries processed. All entries well-formed.\n";


## Now let's go through the previously supported languages and compare how they are treated in the new CVV:

print "Processing the previously supported list:\n";

open FINP, $citation_languages_file_develop;

while ($_ = <FINP>)
{
    chop;
    
    @_ = split ("\t", $_);

    if ($_[1] ne "language")
    {
	die "illegal entry: $_\n";
    }

    $mainname = $_[2];

    if ($mainname ne "Not applicable")
    {
	if ($_[3] !~/^[a-z][a-z][a-z]$/) 
	{
	    die "Error: Illegal entry: no 3-letter identifier code in the 3rd column: " . $_ . "\n";
	}
	else
	{
	    $identifier = $_[3];

	    unless ($LANGUAGE_NAMES{$identifier})
	    {
		die "Previously supported language " . $identifier . " (" . $mainname . ") is no longer on the list.\n";
	    }

	    if ( $mode eq "-checknames" )
	    {
		if ($mainname ne $LANGUAGE_NAMES{$identifier})
		{
		    print "Language name different for id " . $identifier . ": old: " . $mainname . ", new: " . $LANGUAGE_NAMES{$identifier} . "\n";
		}
	    }
	}
    }
    
    unless ($_[4] >= 0)
    {
	die "Error: illegal entry: (no CVV ref. number) " . $_ . "\n";
    }

    $threelettercode = $_[5];

    if ( $mainname ne "Not applicable" )
    {
	unless ( $threelettercode =~/^[a-z][a-z][a-z]$/ )
	{
	    die "Error: No 3-letter code in the 5th column: " . join ("***", @_) . "\n";
	}
	else
	{
	    if ( $mode eq "-checkcodes" )
	    {
		unless ($LANGUAGE_ALTERNATES{$identifier}->{$threelettercode})
		{
		    print "identifier: " . $identifier . ", main 3-letter code \"" . $threelettercode . "\" is not found in the new list!\n";
		}
	    }
	}
    }

    if ( $mainname ne "Not applicable" && $identifier ne $threelettercode )
    {
	die "warning: the identifier in the 3rd column is different from the first \"alternate\" in the 5th: " . $_ . "\n";
    }
    
    if ( $mode eq "-checkcodes" )
    {
	for ( $c = 6; $c <= $#_; $c++ )
	{
	    $alternate = $_[$c];

	    if ( $alternate ne "" )
	    {
		unless ($LANGUAGE_ALTERNATES{$identifier}->{$alternate})
		{
		    print "identifier: " . $identifier . ", alternate value " . $alternate . " is missing in the new list!\n";
		}	    
	    }
	}
    }

    $num_develop++; 
}

print $num_develop . " develop branch entries processed.\n";
