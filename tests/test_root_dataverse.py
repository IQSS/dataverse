from selenium import webdriver
import time, unittest, config

def is_alert_present(wd):
    try:
        wd.switch_to_alert().text
        return True
    except:
        return False

class test_root_dataverse(unittest.TestCase):
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
    
    def test_test_root_dataverse(self):
        success = True
        wd = self.wd
        wd.get(config.accessURL)
        if not ("Create Root Dataverse" in wd.find_element_by_tag_name("html").text):
            print("Root dataverse exists. Exiting.")
            return
        wd.find_element_by_id("dataverseForm:name").click()
        wd.find_element_by_id("dataverseForm:name").clear()
        wd.find_element_by_id("dataverseForm:name").send_keys("root dv")
        wd.find_element_by_id("dataverseForm:alias").click()
        wd.find_element_by_id("dataverseForm:alias").clear()
        wd.find_element_by_id("dataverseForm:alias").send_keys("rootdv")
        wd.find_element_by_id("dataverseForm:contactEmail").click()
        wd.find_element_by_id("dataverseForm:contactEmail").clear()
        wd.find_element_by_id("dataverseForm:contactEmail").send_keys("kcondon@hmdc.harvard.edu")
        wd.find_element_by_id("dataverseForm:affiliation").click()
        wd.find_element_by_id("dataverseForm:affiliation").clear()
        wd.find_element_by_id("dataverseForm:affiliation").send_keys("IQSS")
        wd.find_element_by_id("dataverseForm:description").click()
        wd.find_element_by_id("dataverseForm:description").clear()
        wd.find_element_by_id("dataverseForm:description").send_keys("This is a test")
        wd.find_element_by_id("dataverseForm:save").click()
        time.sleep(1)
        if not ("root dv" in wd.find_element_by_tag_name("html").text):
            success = False
            print("verify root dv name failed")
        self.assertTrue(success)
    
    def tearDown(self):
        if not (config.local):
            print("Link to your job: https://saucelabs.com/jobs/%s" % self.wd.session_id)        
        self.wd.quit()

if __name__ == '__main__':
    unittest.main()
