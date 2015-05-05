from selenium import webdriver
import time, unittest, config

def is_alert_present(wd):
    try:
        wd.switch_to_alert().text
        return True
    except:
        return False

class test_account(unittest.TestCase):
    def setUp(self):
        if (config.local):
            self.wd = webdriver.Firefox()
        else:
            desired_capabilities = webdriver.DesiredCapabilities.FIREFOX
            desired_capabilities['version'] = '24'
            desired_capabilities['platform'] = 'Linux'
            desired_capabilities['name'] = 'test_access'
            self.wd = webdriver.Remote(
                desired_capabilities=desired_capabilities,
                command_executor="http://esodvn:325caef9-81dd-47a5-8b74-433057ce888f@ondemand.saucelabs.com:80/wd/hub"
            )
 
        self.wd.implicitly_wait(60)
    
    def test_test_account(self):
        success = True
        varName = "Tester", time.time()
        wd = self.wd
        wd.get(config.accessURL)
        wd.find_element_by_link_text("Create Account").click()
        wd.find_element_by_id("dataverseUserForm:userName").click()
        wd.find_element_by_id("dataverseUserForm:userName").clear()
        wd.find_element_by_id("dataverseUserForm:userName").send_keys("varName")
        wd.find_element_by_id("dataverseUserForm:inputPassword").click()
        wd.find_element_by_id("dataverseUserForm:inputPassword").clear()
        wd.find_element_by_id("dataverseUserForm:inputPassword").send_keys("tester")
        wd.find_element_by_id("dataverseUserForm:retypePassword").click()
        wd.find_element_by_id("dataverseUserForm:retypePassword").clear()
        wd.find_element_by_id("dataverseUserForm:retypePassword").send_keys("tester")
        wd.find_element_by_id("dataverseUserForm:firstName").click()
        wd.find_element_by_id("dataverseUserForm:firstName").clear()
        wd.find_element_by_id("dataverseUserForm:firstName").send_keys("test")
        wd.find_element_by_id("dataverseUserForm:lastName").click()
        wd.find_element_by_id("dataverseUserForm:lastName").clear()
        wd.find_element_by_id("dataverseUserForm:lastName").send_keys("user")
        wd.find_element_by_id("dataverseUserForm:email").click()
        wd.find_element_by_id("dataverseUserForm:email").clear()
        wd.find_element_by_id("dataverseUserForm:email").send_keys("kcondon@hmdc.harvard.edu")
        wd.find_element_by_id("dataverseUserForm:institution").click()
        wd.find_element_by_id("dataverseUserForm:institution").clear()
        wd.find_element_by_id("dataverseUserForm:institution").send_keys("IQSS")
        wd.find_element_by_xpath("//div[@id='dataverseUserForm:j_idt45']/div[3]").click()
        wd.find_element_by_xpath("//div[@class='ui-selectonemenu-items-wrapper']//li[.='Staff']").click()
        wd.find_element_by_id("dataverseUserForm:phone").click()
        wd.find_element_by_id("dataverseUserForm:phone").clear()
        wd.find_element_by_id("dataverseUserForm:phone").send_keys("1-222-333-4444")
        wd.find_element_by_id("dataverseUserForm:save").click()       


    def tearDown(self):
        if not (config.local):
            print("Link to your job: https://saucelabs.com/jobs/%s" % self.wd.session_id)        
        self.wd.quit()

if __name__ == '__main__':
    unittest.main()
