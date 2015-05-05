# This contain a list of individual test and will be run from Jenkins.

import unittest
import test_access
import test_create_test_account
import test_root_dataverse
import test_account
import test_dataverse
import test_dataset
import test_dataset_fileupload

# This is a list of testFileName.testClass
def suite():
    return unittest.TestSuite((\
        unittest.makeSuite(test_access.test_access),    	
        unittest.makeSuite(test_create_test_account.test_create_test_account),
        unittest.makeSuite(test_root_dataverse.test_root_dataverse),
        unittest.makeSuite(test_account.test_account),        
        unittest.makeSuite(test_dataverse.test_dataverse),
        unittest.makeSuite(test_dataset.test_dataset),
        unittest.makeSuite(test_dataset_fileupload.test_dataset_fileupload),        
        ))

if __name__ == '__main__':
    result = unittest.TextTestRunner(verbosity=2).run(suite())