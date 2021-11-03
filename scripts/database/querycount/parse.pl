#!/usr/bin/perl 
$count = 0;  # total count of queries encountered
$parsed = 0; # count of queries parsed

while (<>) 
{
    chop;

    # a freaky multi-line notation for "SELECT EXISTS" (very rare; something administrative?)
    if ( /execute [^:]*: (select exists.*)$/i )
    {
	$query_multiline = $1;
	$count++;
    }
    elsif ( /execute [^:]*: (select .*)$/i || /execute [^:]*: (insert .*)$/i || /execute [^:]*: (update .*)$/i || /execute [^:]*: (delete .*)$/i )
    {
	$query = $1; 
	$count++; 

	if ($query =~/\$1/)
	{
	    # saving the query, will substitute parameters
	    #print STDERR "saving query: " . $query . "\n";

	}
	else 
	{
	    print $query . "\n";
	    $parsed++;
	    $query = "";
	}
    }
    elsif (/^.*[A-Z][A-Z][A-Z]\s.*DETAIL:  parameters: (.*)$/i)
    {
	#print STDERR "detail line encountered.\n";

	if ( $query_multiline )
	{
	    $query = $query_multiline;
	    $query_multiline = "";
	}
	
	unless ($query)
	{
	    die "DETAIL statement encountered (" . $_ . ", no query\n";
	}

	$params = $1; 

	@params_ = split (', \$', $params); 

	for $p (@params_)
	{
	    $p =~s/^ *//; 
	    $p =~s/ *$//; 
	    $p =~s/ *=/=/g;
	    $p =~s/= */=/g; 

#	    print STDERR $p . "\n";

	    ($name, $value) = split ("=", $p, 2); 

	    $name =~s/^\$//g; 
	    $value=~s/[()]+//g; 

	    $query =~s/\$$name/$value/ge;
	}

	print $query . "\n"; 
	$parsed++; 
	$query = "";
    }
    elsif ( $query_multiline )
    {
	$query_multiline .= $_; 
    }
}
print STDERR "total queries encountered: $count\n";
print STDERR "total queries parsed: $parsed\n";
