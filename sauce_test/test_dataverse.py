import time
import datetime
import unittest
from selenium import webdriver
from selenium.webdriver.common.keys import Keys

class TestDataverseFunctions(unittest.TestCase):
        def setUp(self):
            desired_capabilities = webdriver.DesiredCapabilities.FIREFOX
            desired_capabilities['version'] = '24'
            desired_capabilities['platform'] = 'Linux'
            desired_capabilities['name'] = 'Testing Selenium 2 in Python at Sauce'

            self.driver = webdriver.Remote(
                desired_capabilities=desired_capabilities,
                command_executor="http://esodvn:325caef9-81dd-47a5-8b74-433057ce888f@ondemand.saucelabs.com:80/wd/hub"
            )
            self.driver.implicitly_wait(30)

        def test_create(self):
       	    # Test creating a basic dataverse
            driver=self.driver
            driver.get("http://dvn-build.hmdc.harvard.edu")
            driver.find_element_by_id("shareForm:shareData_button").click()
            driver.find_element_by_xpath("//div[@id='shareForm:shareData_menu']/ul/li[3]/a/span").click()
            varDvAlias=datetime.datetime.now().strftime("%m-%d-%H-%M-%S")
            varDvName="Selenium Test: ", varDvAlias
            driver.find_element_by_id("dataverseForm:name").send_keys(varDvName)
            driver.find_element_by_id("dataverseForm:alias").send_keys(varDvAlias)
            driver.find_element_by_id("dataverseForm:contactEmail").send_keys("kcondon@hmdc.harvard.edu")
            driver.find_element_by_id("dataverseForm:affiliation").send_keys("DVN QA")
            driver.find_element_by_id("dataverseForm:description").send_keys("This is just a test")
            driver.find_element_by_id("dataverseForm:save").click()
            driver.find_element_by_id("dataverseForm:j_idt23.ui-panel:nameOutput")
            self.assertEqual(1,1, "Not equal!")

        def tearDown(self):
            self.driver.quit()            

if __name__ == '__main__':
    unittest.main()