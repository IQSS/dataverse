#!/usr/bin/perl 
use Text::SpamAssassin;
use JSON; 

# adjust the path of the .cf file below: 
$SPAM_DETECTION_RULES = "dataverse_spam_prefs.cf";

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

# command line options: 
while ( $ARGV[0] =~/^\-/ )
{
    $option = shift @ARGV; 
    # the only option recognized so far is "-v" ("verbose"):
    if ( $option eq "-v" )
    {
	$verbose = 1; 
    }
    else 
    {
	print STDERR "usage: ./dataverse-spam-validator.pl [-v] <dv_metadata.json>\n";
	exit 0; 
    }
}

$infile = shift @ARGV; 

unless ( -f $infile )
{
    die "usage: ./dataverse-spam-validator.pl [-v] <dv_metadata.json>\n";
}

open INF, $infile; 

while (<INF>)
{
    $jsoninput .= $_; 
}

close INF; 

# create json handling object: 
$json = JSON->new->allow_nonref;

# parse and process json input:
eval {
    $jsonref = $json->decode( $jsoninput );
};
if ($@) {
    print STDERR "failed to parse json input: " . $@ . "\n";
    exit 0; 
}

$dvname = $jsonref->{name};
$dvdescription = $jsonref->{description}; 


print "dataverse name: " . $dvname . "\n" if $verbose;
print "dataverse description: " . $dvdescription . "\n" if $verbose; 

# create Text::SpamAssassin object: 
 
my $sa = Text::SpamAssassin->new(
    sa_options => {
	userprefs_filename => 'dataverse_spam_prefs.cf',
    },
    );
 
# The description of the dataverse is the main body of text on which we run SpamAssassin: 
$sa->set_html($dvdescription);
# ... and we'll use the name of the dataverse as the subject header:
$sa->set_header("Subject", $dvname);
 
my $result = $sa->analyze;
print "result: $result->{verdict}\n" if $verbose;
print "score: $result->{score}\n" if $verbose; 
print "matched rules: $result->{rules}\n" if $verbose; 

if ( $result->{verdict} eq "SUSPICIOUS" )
{
    exit 1; 
}

exit 0; 


