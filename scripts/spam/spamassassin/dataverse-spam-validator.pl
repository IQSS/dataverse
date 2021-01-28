#!/usr/bin/perl -w
use Text::SpamAssassin;
use JSON; 
use strict; 
use DBI; 


my $DV_SA_INSTALL_DIRECTORY = $ENV{'DV_SA_INSTALL_DIRECTORY'}; 
$DV_SA_INSTALL_DIRECTORY = "." unless $DV_SA_INSTALL_DIRECTORY; 
my $SPAM_DETECTION_RULES = $DV_SA_INSTALL_DIRECTORY . "/dataverse_spam_prefs.cf";


my $port = 5432;
my $host = "localhost";
my $username = "dvnapp";
my $password = 'secret';
my $database = "spamdb";

unless ( -f $SPAM_DETECTION_RULES )
{
    print STDERR "Could not find the rules file " . $SPAM_DETECTION_RULES 
	. "; make sure the path is correct";
    exit 0;  
    # Note that normally we would use "die" here - but we don't want to 
    # return a non-zero exit code; as in, we don't want an improperly installed
    # or misconfigured script to result in false positives, potentially telling 
    # users that their dataverses are being rejected on account of being spam"; 
}

my $USAGE = "usage: ./dataverse-spam-validator.pl [-v] <(dataverse|dataset)> <dv_metadata.json>\n";

my $verbose; 

# command line options: 
while ( $ARGV[0] =~/^\-/ )
{
    my $option = shift @ARGV; 
    # the only option recognized so far is "-v" ("verbose"):
    if ( $option eq "-v" )
    {
	$verbose = 1; 
    }
    else 
    {
	print STDERR $USAGE; 
	exit 0; 
    }
}

my $dtype = shift @ARGV; 

unless ( $dtype =~/^data(verse|set)$/ )
{
    print STDERR $USAGE; 
    exit 0; 
}

my $infile = shift @ARGV; 

unless ( -f $infile )
{
    print STDERR $USAGE;
    exit 0; 
}

open INF, $infile; 
my $jsoninput = ""; 

while (<INF>)
{
    $jsoninput .= $_; 
}

close INF; 

# create json handling object: 
my $json = JSON->new->allow_nonref;

# parse and process json input:
# (catch any parsing exception and exit cleanly; 
# again, we don't want any false positives.

my $test_subject = ""; 
my $test_textbody = ""; 

my $dv_alias = ""; # for dataverses only;
my $ds_identifier = "";

eval {
    my $jsonref = $json->decode( $jsoninput );

    my $dvdescription = "";
    if ( $dtype eq "dataverse" )
    {
	my $dvname = $jsonref->{name};
	$dvdescription = $jsonref->{description}; 
	$dv_alias = $jsonref->{alias}; 

	print "dataverse name: " . $dvname . "\n" if $verbose;
	print "dataverse alias: " . $dv_alias . "\n" if $verbose;
	print "dataverse description: " . $dvdescription . "\n" if $verbose; 

	# The description of the dataverse is the main body of text on which we run SpamAssassin: 
	$test_textbody = $dvdescription; 
	# ... and we'll use the name of the dataverse as the subject header:
	$test_subject = $dvname; 
    }
    else # dataset
    {
	my $dstitle = "";
	my $dsdescription = ""; 


	$ds_identifier = $jsonref->{protocol} . ":" . $jsonref->{authority} . "/" . $jsonref->{identifier}; 
	print "identifier: " . $ds_identifier . "\n" if $verbose;

	# check the white list: 

	my $whitelisted = &check_dataset_whitelist($ds_identifier, $DV_SA_INSTALL_DIRECTORY . "/dataset_whitelist.txt");

	if ($whitelisted)
	{
	    exit 0;
	}

	my $metadatafields = $jsonref->{datasetVersion}->{metadataBlocks}->{citation}->{fields}; 

	for my $field (@$metadatafields)
	{
	    my $ftype = $field->{typeName}; 

	    if ( $ftype eq "title" )
	    {
		$dstitle = $field->{value};
		print "title: " . $dstitle . "\n" if $verbose;
	    }
	    elsif ( $ftype eq "dsDescription" )
	    {
		# dsDescription is a compound field:
		# (and multiple values are allowed!)
		my $subfields = $field->{value}; 
	    
		for my $sfield (@$subfields) 
		{
		    # dsDescriptionValue is a required subfield... 
		    # so we can expect it to be populated:
		    $dsdescription .= $sfield->{dsDescriptionValue}->{value};
		    print "description: " . $dsdescription . "\n" if $verbose;
		}
	    }
	    # Any other dataset metadata fields we want to subject to this check? 
	}
	$test_textbody = $dsdescription; 
	$test_subject = $dstitle; 
    }
};
# catch any exceptions:
if ($@) {
    print STDERR "failed to parse json input: " . $@ . "\n";
    exit 0; 
}

# create Text::SpamAssassin object: 
 
my $sa = Text::SpamAssassin->new(
    sa_options => {
	userprefs_filename => $SPAM_DETECTION_RULES,
    },
    );
 
$sa->set_text($test_subject . " " . $test_textbody);
$sa->set_header("Subject", $test_subject);
 
# ... and run the check:

my $result = $sa->analyze;
print "result: $result->{verdict}\n" if $verbose;
print "score: $result->{score}\n" if $verbose; 
print "matched rules: $result->{rules}\n" if $verbose; 

system ("cp $infile $DV_SA_INSTALL_DIRECTORY/saved/"); 

if ( $result->{verdict} eq "SUSPICIOUS" )
{
    # let's create a spamdb entry: 
    # (skipped if the db credentials are not defined)
    
    if ( $database )
    {
	my $dbh = DBI->connect("DBI:Pg:dbname=$database;host=$host;port=$port",$username,$password); 

	my $createquery = ""; 
	my $sdalias; # only for dataverses
	my $sdtitle = $dbh->quote($test_subject);
	my $sdidentifier; # only for datasets
	my $sdscore = $result->{score}; 

	# constructing a formatted time stamp is a bit of a project:
	my @lt = localtime; 
	my $sddate = $dbh->quote((1900 + $lt[5]) . "-" . ($lt[4] + 1) . "-" . $lt[3] . " " . $lt[2] . ":" . $lt[1] . ":" . $lt[0]);
    
	if ( $dtype eq "dataverse" )
	{
	    $sdalias = $dbh->quote($dv_alias);
	    $createquery = qq{INSERT INTO positivetestresult (alias, title, score, testdate) VALUES ($sdalias, $sdtitle, $sdscore, $sddate);};
	}
	else 
	{
	    $sdidentifier = $dbh->quote($ds_identifier); 
	    $createquery = qq{INSERT INTO positivetestresult (identifier, title, score, testdate) VALUES ($sdidentifier, $sdtitle, $sdscore, $sddate);};
	}

	my $sth; 

	eval {
	    $sth = $dbh->prepare($createquery); 
	    $sth->execute();
	};
	if ($@) {
	    print STDERR "failed to save the db entry: " . $@ . "\n";
	    exit 0; 
	}

	$sth->finish; 
	$dbh->disconnect; 
    }

    exit 1; 
}

exit 0; 


sub check_dataset_whitelist {
    my ($dsid, $wlfile) = @_;

    open WLF, $wlfile || return 0;

    while (<WLF>)
    {
	chop;
	if ($_ eq $dsid)
	{
	    print "dataset $dsid whitelisted\n" if $verbose;
	    close WLF;
	    return 1;
	}
    }
    close WLF;
    return 0;
}
