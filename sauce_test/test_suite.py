# This contain a list of individual test and will be run from Jenkins.

import unittest
import access_dvn
import test_dataverse
import test_dataset

# This is a list of testFileName.testClass
def suite():
    return unittest.TestSuite((\
        unittest.makeSuite(access_dvn.AccessDVN),
        unittest.makeSuite(test_dataverse.TestDataverseFunctions),
        unittest.makeSuite(test_dataset.TestDatasetFunctions),
        ))

if __name__ == "__main__":
    result = unittest.TextTestRunner(verbosity=2).run(suite())
#    sys.exit(not result.wasSuccessful())