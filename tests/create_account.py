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
        driver.find_element_by_id("dataverseUserForm:userName").send_keys("user1")
        driver.find_element_by_id("dataverseUserForm:inputPassword").click()
        driver.find_element_by_id("dataverseUserForm:inputPassword").clear()
        driver.find_element_by_id("dataverseUserForm:inputPassword").send_keys("u")
        driver.find_element_by_id("dataverseUserForm:retypePassword").click()
        driver.find_element_by_id("dataverseUserForm:retypePassword").clear()
        driver.find_element_by_id("dataverseUserForm:retypePassword").send_keys("u")
        driver.find_element_by_id("dataverseUserForm:firstName").click()
        driver.find_element_by_id("dataverseUserForm:firstName").clear()
        driver.find_element_by_id("dataverseUserForm:firstName").send_keys("user")
        driver.find_element_by_id("dataverseUserForm:lastName").click()
        driver.find_element_by_id("dataverseUserForm:lastName").clear()
        driver.find_element_by_id("dataverseUserForm:lastName").send_keys("zero")
        driver.find_element_by_id("dataverseUserForm:email").click()
        driver.find_element_by_id("dataverseUserForm:email").clear()
        driver.find_element_by_id("dataverseUserForm:email").send_keys("u@u.edu")
        driver.find_element_by_id("dataverseUserForm:institution").click()
        driver.find_element_by_id("dataverseUserForm:institution").clear()
        driver.find_element_by_id("dataverseUserForm:institution").send_keys("IQSS")
        driver.find_element_by_id("dataverseUserForm:j_idt45_focus").click()
        driver.find_element_by_id("dataverseUserForm:j_idt45_focus").send_keys("\\9")
        driver.find_element_by_css_selector("span.ui-icon.ui-icon-triangle-1-s").click()
        driver.find_element_by_xpath("//div[@class='ui-selectonemenu-items-wrapper']//li[.='Student']").click()
        driver.find_element_by_id("dataverseUserForm:phone").click()
        driver.find_element_by_id("dataverseUserForm:phone").click()
        driver.find_element_by_id("dataverseUserForm:phone").clear()
        driver.find_element_by_id("dataverseUserForm:phone").send_keys("888-888-8888")
        driver.find_element_by_id("dataverseUserForm:save").click()


    def tearDown(self):
        print("Link to your job: https://saucelabs.com/jobs/%s" % self.driver.session_id)
        self.driver.quit()

if __name__ == '__main__':
    unittest.main()
