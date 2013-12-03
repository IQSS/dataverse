import time
import datetime
import unittest
from selenium import webdriver
from selenium.webdriver.common.keys import Keys

class TestDatasetFunctions(unittest.TestCase):
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
       	    # Test creating a basic dataset
            driver=self.driver
            driver.get("http://dvn-build.hmdc.harvard.edu")
            driver.find_element_by_id("shareForm:shareData_button").click()
            driver.find_element_by_xpath("//div[@id='shareForm:shareData_menu']/ul/li/a/span").click()
            varTitle = "Dataset: ", datetime.datetime.now().strftime("%m-%d-%H-%M-%S")
            driver.find_element_by_id("datasetForm:title").send_keys(varTitle)
            driver.find_element_by_xpath("//div[@id='datasetForm:j_idt34']/div[3]/span")
            driver.find_element_by_xpath("//div[@id='datasetForm:j_idt34_panel']/div[2]/ul/li")
            driver.find_element_by_id("datasetForm:author").send_keys("Auto Tester")
            datasetDate = driver.find_element_by_id("datasetForm:date").send_keys(datetime.datetime.now().strftime("%y-%m-%d"))
            datasetDistributor = driver.find_element_by_id("datasetForm:distributor").send_keys("IQSS")
            datasetDescription = driver.find_element_by_id("datasetForm:description").send_keys("This is a description")
            driver.find_element_by_id("datasetForm:save").click()
            time.sleep(5)
            datasetCitation = driver.find_element_by_id("datasetForm:citation").text
            #citationFields = varAuthor, ", ", varTitle, ", ", varDate, ", ", varDistributor
            print datasetCitation
            #self.assertEqual(citationFields, datasetCitation.text[:len(citationFields)], msg='Citation does not equal citation fields.')

        def tearDown(self):
            self.driver.quit()

if __name__ == '__main__':
    unittest.main()
