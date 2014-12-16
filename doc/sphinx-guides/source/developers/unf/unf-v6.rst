UNF Version 6
================================

*(this document is a draft!)*

The document is primarily intended for those who are interested in implementing their own UNF Version 6 calculator. We would like to encourage multiple parallel implementations, since that would be a great (the only, really) way to cross-validate UNF signatures calculated for specific sets of data.

Algorithm Description
========================================

UNF v5, on which v6 is based, was originally described in Dr. Micah Altman's paper "A Fingerprint Method for Verification of Scientific Data", Springer Verlag, 2008. The reader is encouraged to consult it for the explanation of the theory behind UNF. However, various changes and clarifications concerning the specifics of normalization have been made to the algorithm since the publication. These crucial details were only documented in the author's unpublished edits of the article and in private correspondence. With this document, a serious effort has been made to produce a complete step-by-step description of the entire process. It should be fully sufficient for the purposes of implementing the algorithm.

I. UNF of a Data Vector
=========================================

For each individual vector in a data frame, calculate its UNF signature as follows:

Ia. Normalize each vector element as follows:
-----------------------------------------------------------------------

**1. For a vector of numeric elements:**
Round each vector element to k significant digits using the IEEE 754 "round towards nearest, ties to even" rounding mode. The default value of k is 7.

(See an Important :ref:`Note <note1>` on the use of :ref:`default and optional <note1>` values and methods!)

Convert each vector element into a character string in exponential notation, as follows:

A sign character.
A single leading non-zero digit.
A decimal point.
Up to k-1 remaining digits following the decimal, no trailing zeros.
A lowercase letter "e".
A sign character.
The digits of the exponent, omitting trailing zeros.

*Special cases:*

Zero representation (an exception to the "leading non-zero digit" rule, above):
+0.e+ for positive zero.
-0.e+ for negative zero.
(see the :ref:`Note <note2>` below on :ref:`negative zero <note2>`)

Infinity and NaN ("Not a Number") values:
If an element is an IEEE 754, non-finite, special floating-point value, represent it as the signed, lowercase, IEEE minimal printable equivalent, that is, +inf, -inf, or +nan. No attempt is made to differentiate between various types of NaNs allowed under IEEE 754.

*Examples:*

The number 1 is represented as +1.e+
The number pi at five digits is represented as +3.1415e+
The number -300 is represented as -3.e+2
The number 0.00073 is represented as +7.3e-4
Positive infinity is represented as +inf
The number 1.23456789, normalized with the default rounding to 7 digits of precision, is represented as +1.234568e+

**2. For a vector of character strings:**
Encode each character string with Unicode bit encoding. In UNF Version 6 UTF-8 is used. Truncate each string to l characters; the default value of l is 128. No further normalization is performed.

**3. Vectors of Boolean values**
Should be treated as numeric vectors of 0s and 1s.

**4. Bit fields.**
Normalize bit fields by converting to big-endian form, truncating all leading empty bits, aligning to a byte boundary by re-padding with leading zero bits, and base64 encoding to form a character string representation.

**5. Normalize dates, times and intervals as follows:**

*5a. Dates.*
Convert calendar dates to a character string of the form YYYY-MM-DD, zero padded. Partial dates in the form YYYY or YYYY-MM are permitted

*5b. Time.*
Time representation is based on an ISO 8601 format hh:mm:ss.fffff. hh, mm and ss are 2 digit, zero-padded numbers. fffff represents fractions of a second, it must contain no trailing (non-significant) zeroes, and must be omitted if valued at zero. No other fractional representations, such as fractional minutes or hours, are permitted. If the time zone of the observation is known, convert the time value to the UTC time zone and append a ”Z” to the time representation. (In other words, no time zones other than UTC are allowed in the final normalized representation).

(see the :ref:`Note <note3>` at the end of this document for a discussion on :ref:`potential issues when calculating UNFs of time values <note3>`).

*5c. Combined Date and Time values.*
Format elements that comprise a combined date and time by concatenating the (full) date representation, a single letter “T”, and the time representation. Partial date representations are **prohibited** in combined date and time values.

*5d. Intervals.*
Represent intervals by using two date-time values, formatted as defined previously, and separated by a slash ("/").

*Durations*, that were mentioned in the old UNF v5 document are NOT in fact implemented and have been dropped from the spec.

*Examples:*

2:29 pm on Jun 10, 2012 is encoded as "2012-06-10T14:29:00".

Fri Aug 22 12:51:05 EDT 2014 is encoded as "2014-08-22T16:51:05Z"
(The UTC offset of Eastern Daylight Time is -4:00).

**6. Missing values**
Missing values, of all of the above types, are encoded as 3 null bytes: \000\000\000.

Ib. Calculate the UNF of the vector as follows:
---------------------------------------------------------------------------

Terminate each character string representing a NON-MISSING value with a POSIX end-of-line character and a null byte (\000). Do not terminate missing value representations (3 null bytes \000\000\000). Concatenate all the individual character strings, and compute the SHA256 hash of the combined string. Truncate the resulting hash to 128 bits (128 being the default, with other values possible - see the note below). Encode the resulting string in base64, for readability. Prepend the encoded hash string with the signature header UNF:6: (with 6 being the current version).

*Example:*

Vector (numeric): {1.23456789, <MISSING VALUE>, 0}
Normalized elements (k=7,default): "+1.234568e+", "\000\000\000", "+0.e+"
Combined string: "+1.234568e+\n\000\000\000\000+0.e+\n\000"
SHA256 hash truncated to the default 128 bits: Do5dfAoOOFt4FSj0JcByEw==
Printable UNF: UNF:6:Do5dfAoOOFt4FSj0JcByEw==

II. Combining multiple UNFs to create UNFs of higher-level objects.
==============================================================================================

IIa. Combine UNFs from multiple variables to form the UNF for an entire data frame as follows:
------------------------------------------------------------------------------------------------------------------------------

Sort the printable representations of individual UNFs in POSIX locale sort order.

Apply the UNF algorithm to the resulting vector of character strings, with the character string truncation parameter l at least as large as the length of each individual UNF string. (i.e., do not truncate the UNFs of individual variables!)

Do note the sorting part, above, it is important! In a vector of observations, the order is important; changing the order of observations changes the UNF. A data frame, however, is considered an unordered set of individual vectors. I.e., re-arranging the order in which data variable columns occur in an R or Stata file should not affect the UNF. Hence the UNFs of individual variables are sorted, before the combined UNF of the data frame is calculated.

IIb. Similarly, combine the UNFs for a set of data frames to form a single UNF that represents an entire research study ("dataset").
------------------------------------------------------------------------------------------------------------------------------------------------

Using a consistent UNF version and level of precision across an entire dataset is recommended when calculating the UNFs of individual data objects.

**Footnotes:**

.. _note1:

Note: On default and non-default parameter values:
Here and throughout the rest of this document, phrases like "The default value of k is 7" suggest that it is possible to use non-default values, such as a different number of digits of precision, in this case. This has been a source of some confusion in the past. UNF relies on data normalization to produce "data fingerprints" that are meaningful and descriptive. So how do you generate reproducible and verifiable signatures if any flexibility is allowed in the normalization algorithm? The answer, as specified in the original UNF paper: any non-default parameters used are embedded in the header portion of the UNF!

For example, the UNF of a floating point (Double) vector with a single value of 1.23456789, calculated with the default 7 digits of precision, is UNF:6:vcKELUSS4s4k1snF4OTB9A==. Calculated with the 9 digits of precision, the printable UNF is UNF:6:N9:IKw+l4ywdwsJeDze8dplJA==. With the parameter value embedded in the signature, it can be recalculated and verified unambiguously.

Other optional parameters supported (in any comma-separated combination and ordering):

Xnnn - where nnn is the number of characters for truncation of character strings;
Hnnn - where nnn is the number of bits to which the SHA256 hash should be truncated.
Allowed values are {128,192,196,256} with 128 being the default. 
R1 - **truncate** numeric values to k digits, instead of rounding, as previously described.

`Dr. Micah Altman's classic UNF v5 paper <http://www.researchgate.net/publication/200043172_A_Fingerprint_Method_for_Scientific_Data_Verification>`_ mentions another optional parameter T###, for specifying rounding of date and time values (implemented as stripping the values of entire components - fractional seconds, seconds, minutes, hours... etc., progressively) - but it doesn't specify its syntax. It is left as an exercise for a curious reader to contact the author and work out the details, if so desired. (Not implemented in UNF Version 6 by the Dataverse Project).

It should be noted that the Dataverse application never calculates UNFs with any non-default parameters. And we are not aware of anyone else actually doing so. If you are considering creating your own implementation of the UNF, it may be worth trying to create a simplified, defaults-only version first. Such an implementation would be sufficient to independently verify Dataverse-produced UNFs, among other things.

.. _note2:

Note: Negative Zero
IEEE 754 zero is signed. I.e., there are 2 zeros, positive and negative. As implemented in most programming languages, floating point types can have negative zero values. It is the responsibility of the implementer, to properly identify the sign of a floating point zero value. Which can be a bit tricky; for example, in Java programming language, when performing arithmetic comparison on values of the primitive type double, the following evaluates to TRUE:
0.0d == -0.0d
However, the comparison methods provided by the wrapper class Double recognize -0.0 and 0.0 as different values, and 0.0 to be greater than -0.0. So all of the following expressions evaluate to FALSE:

new Double(0.0d).equals(new Double(-0.0d))
Double.compare(-0.0d, 0.0d) >= 0
new Double(-0.0d).compareTo(new Double(0.0d)) >= 0

.. _note3:

Note: UNFs of time values in real-life statistical packages
The following is not by itself an implementation concern. But it is something you may need to consider when calculating UNFs of time values from real-world data.

The fact that the same time value with and without the time zone specified produces different UNFs presents an interesting issue when converting data between different formats. For example, in STATA none of the available time types support time zones. In R, on the other hand, ALL time values are stored with a time zone. While it is possible to create an R time value from a character representation with no time zone - for example:

timevar<-as.POSIXct("03/19/2013 18:20:00", format = "%m/%d/%Y %H:%M:%OS");

it still results in R assuming the time is in the current time zone, and storing the UTC equivalent of that time. In fact R always stores its time values in UTC; specific time zones can be defined, as attributes, in which case the values will be adjusted accordingly for display. Otherwise the display representation will be readjusted each time the vector is viewed, according to the time zone **current to the viewer**. Meaning that the human readable representation of the same stored time value will be different when viewed on systems in different time zones. With that in mind, it appears that the only way to calculate a meaningful UNF of a time value from an R data frame is to use the stored UTC time - resulting in the "Z" in the normalized string. And that further means that it is impossible to convert a data frame with time values from STATA to R, or the other way around, and have the same UNF preserved.

We do not consider this a problem with the algorithm. These differences between the two approaches to handling time values, in R and STATA, should in fact be considered as **significant**. Enough so to conclude that the format conversion actually changes the data **semantically**. Which, in turn, justifies a different UNF.

If for whatever reason it is important to produce an R version of a STATA file while preserving the UNF, it can still be done. One way to achieve that would be to convert the original time vector to a String vector in R, in the format identical to that used in the UNF normalization algorithm, e.g., "yy-mm-ddThh:mm:ss". One would not be able to use this resulting R vector in any time-based calculations without extra type conversion. But the data frame would produce the same UNF.
