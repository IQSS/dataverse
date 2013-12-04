# This is a test to create an account. 
import unittest, time
from selenium import webdriver


class CreateAccountSuite(unittest.TestCase):

    def setUp(self):
        desired_capabilities = webdriver.DesiredCapabilities.FIREFOX
        desired_capabilities['version'] = '24'
        desired_capabilities['platform'] = 'Linux'
        desired_capabilities['name'] = 'Create Account'

        self.driver = webdriver.Remote(
            desired_capabilities=desired_capabilities,
            command_executor="http://esodvn:325caef9-81dd-47a5-8b74-433057ce888f@ondemand.saucelabs.com:80/wd/hub"
        )
        self.driver.implicitly_wait(30)

    def test_sauce(self):
        driver=self.driver
        driver.get('http://dvn-build.hmdc.harvard.edu/dataverseuser.xhtml')
        driver.find_element_by_id("dataverseUserForm:editAccountButton_button").click()
        driver.find_element_by_link_text("Create Account").click()
        driver.find_element_by_id("dataverseUserForm:userName").click()
        driver.find_element_by_id("dataverseUserForm:userName").clear()
        driver.find_element_by_id("dataverseUserForm:userName").send_keys("a")


    def tearDown(self):
        print("Link to your job: https://saucelabs.com/jobs/%s" % self.driver.session_id)
        self.driver.quit()

if __name__ == '__main__':
    unittest.main()
