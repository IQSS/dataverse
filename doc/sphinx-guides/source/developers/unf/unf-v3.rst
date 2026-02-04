.. _unf-v3:

UNF Version 3
===========================

.. contents:: |toctitle|
	:local:

Version 3 of the UNF algorithm was used by the Dataverse Network software prior to version 2.0, and was implemented in R code. This algorithm was used on digital objects containing vectors of numbers, vectors of character strings, data sets comprising such vectors, and studies comprising one or more such data sets.

The UNF V3 algorithm applied to the content of a data set or study is as follows:

1. Round each element in a numeric vector to k significant digits using the IEEE 754 round towards zero rounding mode. The default value of k is seven, the maximum expressible in single-precision floating point calculations. UNF calculation for vectors of character strings is identical, except that you truncate to k characters and the default value of k is 128.

2. Convert each vector element to a character string in exponential notation, omitting noninformational zeros. If an element is missing, represent it as a string of three null characters. If an element is an IEEE 754, nonfinite, floating-point special value, represent it as the signed, lowercase, IEEE minimal printable equivalent (that is, +inf, -inf, or +nan).

Each character string comprises the following:

• A sign character.

• A single leading digit.

• A decimal point.

• Up to k-1 digits following the decimal, consisting of the remaining k-1 digits of the number, omitting trailing zeros.

• A lowercase letter "e."

• A sign character.

• The digits of the exponent, omitting trailing zeros.

For example, the number pi at five digits is represented as -3.1415e+, and the number 300 is represented as the string +3.e+2.

1. Terminate character strings representing nonmissing values with a POSIX end-of-line character.

2. Encode each character string with `Unicode bit encoding <https://www.unicode.org/versions/Unicode4.0.0/>`_. Versions 3 through 4 use UTF-32BE; Version 4.1 uses UTF-8.

3. Combine the vector of character strings into a single sequence, with each character string separated by a POSIX end-of-line character and a null byte.

4. Compute a hash on the resulting sequence using the standard MD5 hashing algorithm for Version 3 and using `SHA256 <https://csrc.nist.gov/publications/fips/fips180-2/fips180-2withchangenotice.pdf>`_ for Version 4. The resulting hash is `base64 <https://www.ietf.org/rfc/rfc3548.txt>`_ encoded to support readability.

5. Calculate the UNF for each lower-level data object, using a consistent UNF version and level of precision across the individual UNFs being combined.

6. Sort the base64 representation of UNFs in POSIX locale sort order.

7. Apply the UNF algorithm to the resulting vector of character strings using k at least as large as the length of the underlying character string.

8. Combine UNFs from multiple variables to form a single UNF for an entire data frame, and then combine UNFs for a set of data frames to form a single UNF that represents an entire research study.

Learn more: 
Software for computing UNFs is available in an R Module, which includes a Windows standalone tool and code for Stata and SAS languages. Also see the following for more details: Micah Altman and Gary King. 2007. "A Proposed Standard for the Scholarly Citation of Quantitative Data," D-Lib Magazine, Vol. 13, No. 3/4 (March). (Abstract: `HTML <https://gking.harvard.edu/files/abs/cite-abs.shtml>`_ | Article: `PDF <https://gking.harvard.edu/files/cite.pdf>`_)
