R Data Format
+++++++++++++++++++++++++++++

Support for R (.RData) files has been introduced in DVN 3.5.

.. contents:: |toctitle|
    :local:

Overview.
===========


R has been increasingly popular in the research/academic community,
owing to the fact that it is free and open-source (unlike SPSS and
STATA). Consequently, there is an increasing amount of data available
exclusively in R format.  

Data Formatting Requirements.
==============================

The data must be formatted as an R dataframe (data.frame()). If an
.RData file contains multiple dataframes, only the 1st one will be
ingested (this may change in the future).

Data Types, compared to other supported formats (Stat, SPSS)
=============================================================

Integers, Doubles, Character strings
------------------------------------

The handling of these types is intuitive and straightforward. The
resulting tab file columns, summary statistics and UNF signatures
should be identical to those produced by ingesting the same vectors
from SPSS and Stata.

**Things that are unique to R:** 

R explicitly supports Missing Values for all of the types above;
Missing Values encoded in R vectors will be recognized and preserved
in TAB files, counted in the generated summary statistics
and data analysis. Please note however that the Dataverse Software notation for 
a missing value, as stored in a TAB file, is an empty string, an not "NA" as in R. 

In addition to Missing Values, R recognizes "Not a Value" (NaN) and
positive and negative infinity for floating point variables. These
are now properly supported by the Dataverse Software.

Also note, that unlike Stata, that does recognize "float" and "double"
as distinct data types, all floating point values in R are in fact
doubles. 

R Factors 
---------

These are ingested as "Categorical Values" in the Dataverse installation. 

One thing to keep in mind: in both Stata and SPSS, the actual value of
a categorical variable can be both character and numeric. In R, all
factor values are strings, even if they are string representations of
numbers. So the values of the resulting categoricals in the Dataverse installation will always be of string type too.

Another thing to note is that R factors have no builtin support for
SPSS or STATA-like descriptive labels. This is in fact potentially
confusing, as they also use the word "label", in R parlance. However,
in the context of a factor in R, it still refers to the "payload", or
the data content of its value. For example, if you create a factor
with the "labels" of *democrat*, *republican* and *undecided*, these
strings become the actual values of the resulting vector. Once
ingested in the Dataverse installation, these values will be stored in the
tab-delimited file. The Dataverse Software DataVariable object representing the vector will be of type "Character" and have 3 VariableCategory objects
with the *democrat*, etc. for **both** the CategoryValue and
CategoryLabel.  (In one of the future releases, we are planning to
make it possible for the user to edit the CategoryLabel, using it for
its intended purpose - as a descriptive, human-readable text text
note).

| To properly handle R vectors that are *ordered factors* the Dataverse Software (starting with DVN 3.6) supports the concept of an "Ordered Categorical" - a categorical value where an explicit order is assigned to the list of value labels.

Boolean values
---------------------

R Boolean (logical) values are supported. 


Limitations of R, as compared to SPSS and STATA. 
------------------------------------------------

Most noticeably, R lacks a standard mechanism for defining descriptive
labels for the data frame variables.  In the Dataverse Software, similarly to
both Stata and SPSS, variables have distinct names and labels; with
the latter reserved for longer, descriptive text.
With variables ingested from R data frames the variable name will be
used for both the "name" and the "label".

*Optional R packages exist for providing descriptive variable labels;
in one of the future versions support may be added for such a
mechanism. It would of course work only for R files that were
created with such optional packages*.

Similarly, R categorical values (factors) lack descriptive labels too.
**Note:** This is potentially confusing, since R factors do
actually have "labels".  This is a matter of terminology - an R
factor's label is in fact the same thing as the "value" of a
categorical variable in SPSS or Stata and the Dataverse Software; it contains the actual meaningful data for the given observation. It is NOT a field reserved
for explanatory, human-readable text, such as the case with the
SPSS/Stata "label". 

Ingesting an R factor with the level labels "MALE" and "FEMALE" will
produce a categorical variable with "MALE" and "FEMALE" in the
values and labels both.


Time values in R
================

This warrants a dedicated section of its own, because of some unique
ways in which time values are handled in R.

R makes an effort to treat a time value as a real time instance. This
is in contrast with either SPSS or Stata, where time value
representations such as "Sep-23-2013 14:57:21" are allowed; note that
in the absence of an explicitly defined time zone, this value cannot
be mapped to an exact point in real time.  R handles times in the
"Unix-style" way: the value is converted to the
"seconds-since-the-Epoch" Greenwich time (GMT or UTC) and the
resulting numeric value is stored in the data file; time zone
adjustments are made in real time as needed.

Things still get ambiguous and confusing when R **displays** this time
value: unless the time zone was explicitly defined, R will adjust the
value to the current time zone. The resulting behavior is often
counter-intuitive: if you create a time value, for example:

``timevalue<-as.POSIXct("03/19/2013 12:57:00", format = "%m/%d/%Y %H:%M:%OS");``

on a computer configured for the San Francisco time zone, the value
will be differently displayed on computers in different time zones;
for example, as "12:57 PST" while still on the West Coast, but as
"15:57 EST" in Boston.

If it is important that the values are always displayed the same way,
regardless of the current time zones, it is recommended that the time
zone is explicitly defined. For example: 

``attr(timevalue,"tzone")<-"PST"``

or 

``timevalue<-as.POSIXct("03/19/2013 12:57:00", format = "%m/%d/%Y %H:%M:%OS", tz="PST");``

Now the value will always be displayed as "15:57 PST", regardless of
the time zone that is current for the OS ... **BUT ONLY** if the OS
where R is installed actually understands the time zone "PST", which
is not by any means guaranteed! Otherwise, it will **quietly adjust**
the stored GMT value to **the current time zone**, yet it will still
display it with the "PST" tag attached!** One way to rephrase this is
that R does a fairly decent job **storing** time values in a
non-ambiguous, platform-independent manner - but gives you no guarantee that 
the values will be displayed in any way that is predictable or intuitive. 

In practical terms, it is recommended to use the long/descriptive
forms of time zones, as they are more likely to be properly recognized
on most computers. For example, "Japan" instead of "JST".  Another possible
solution is to explicitly use GMT or UTC (since it is very likely to be
properly recognized on any system), or the "UTC+<OFFSET>" notation. Still, none of the above
**guarantees** proper, non-ambiguous handling of time values in R data
sets. The fact that R **quietly** modifies time values when it doesn't
recognize the supplied timezone attribute, yet still appends it to the
**changed** time value does make it quite difficult. (These issues are
discussed in depth on R-related forums, and no attempt is made to
summarize it all in any depth here; this is just to made you aware of
this being a potentially complex issue!)

An important thing to keep in mind, in connection with the Dataverse Software ingest of R files, is that it will **reject** an R data file with any time
values that have time zones that we can't recognize. This is done in
order to avoid (some) of the potential issues outlined above.

It is also recommended that any vectors containing time values
ingested into the Dataverse installation are reviewed, and the resulting entries in the
TAB files are compared against the original values in the R data
frame, to make sure they have been ingested as expected. 

Another **potential issue** here is the **UNF**. The way the UNF
algorithm works, the same date/time values with and without the
timezone (e.g. "12:45" vs. "12:45 EST") **produce different
UNFs**. Considering that time values in Stata/SPSS do not have time
zones, but ALL time values in R do (yes, they all do - if the timezone
wasn't defined explicitly, it implicitly becomes a time value in the
"UTC" zone!), this means that it is **impossible** to have 2 time
value vectors, in Stata/SPSS and R, that produce the same UNF.

**A pro tip:** if it is important to produce SPSS/Stata and R versions of
the same data set that result in the same UNF when ingested, you may
define the time variables as **strings** in the R data frame, and use
the "YYYY-MM-DD HH:mm:ss" formatting notation. This is the formatting used by the UNF
algorithm to normalize time values, so doing the above will result in
the same UNF as the vector of the same time values in Stata.

Note: date values (dates only, without time) should be handled the
exact same way as those in SPSS and Stata, and should produce the same
UNFs.
