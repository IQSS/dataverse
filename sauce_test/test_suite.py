# This contain a list of individual test and will be run from Jenkins.

import unittest
import access_dvn

# This is a list of testFileName.testClass
def suite():
    return unittest.TestSuite((\
        unittest.makeSuite(access_dvn.AccessDVN),
        ))

if __name__ == "__main__":
    result = unittest.TextTestRunner(verbosity=2).run(suite())
#    sys.exit(not result.wasSuccessful())