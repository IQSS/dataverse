#!/usr/bin/perl 

while (<>) 
{
    chop; 
    if ( /execute <unnamed>: (select .*)$/i || /execute <unnamed>: (insert .*)$/i || /execute <unnamed>: (update .*)$/i)
    {
	$select_q = $1; 

	if ($select_q =~/\$1/)
	{
	    # saving the query, will substitute parameters
	    #print STDERR "saving query: " . $select_q . "\n";

	}
	else 
	{
	    print $select_q . "\n";
	    $select_q = "";
	}
    }
    elsif (/^.*[A-Z][A-Z][A-Z] >DETAIL:  parameters: (.*)$/i)
    {
#	print STDERR "EDT detail line encountered.\n";
	unless ($select_q)
	{
	    die "EDT DETAIL encountered (" . $_ . ", no select_q\n";
	}

	$params = $1; 

	@params_ = split (",", $params); 

	for $p (@params_)
	{
	    $p =~s/^ *//; 
	    $p =~s/ *$//; 
	    $p =~s/ *=/=/g;
	    $p =~s/= */=/g; 

#	    print STDERR $p . "\n";

	    ($name,$value) = split ("=", $p); 

	    $name =~s/^\$//g; 

#	    print STDERR "name: $name, value: $value\n";


	    $select_q =~s/\$$name/$value/ge;
	}

	print $select_q . "\n"; 
	$select_q = "";
    }
}
