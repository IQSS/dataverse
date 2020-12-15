#!/usr/bin/perl -w
use Text::SpamAssassin;
use JSON; 
use strict; 

my $DV_SA_INSTALL_DIRECTORY = $ENV{'DV_SA_INSTALL_DIRECTORY'}; 
$DV_SA_INSTALL_DIRECTORY = "." unless $DV_SA_INSTALL_DIRECTORY; 
my $SPAM_DETECTION_RULES = $DV_SA_INSTALL_DIRECTORY . "/dataverse_spam_prefs.cf";

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

eval {
    my $jsonref = $json->decode( $jsoninput );

    if ( $dtype eq "dataverse" )
    {
	my $dvname = $jsonref->{name};
	my $dvdescription = $jsonref->{description}; 

	print "dataverse name: " . $dvname . "\n" if $verbose;
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
 
$sa->set_text($test_textbody);
$sa->set_header("Subject", $test_subject);
 
# ... and run the check:

my $result = $sa->analyze;
print "result: $result->{verdict}\n" if $verbose;
print "score: $result->{score}\n" if $verbose; 
print "matched rules: $result->{rules}\n" if $verbose; 

if ( $result->{verdict} eq "SUSPICIOUS" )
{
    exit 1; 
}

exit 0; 


