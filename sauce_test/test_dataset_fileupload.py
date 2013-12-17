from selenium import webdriver
import time, unittest, config

def is_alert_present(wd):
    try:
        wd.switch_to_alert().text
        return True
    except:
        return False

class test_dataset_fileupload(unittest.TestCase):
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
    
    def test_test_dataset_fileupload(self):
        success = True
        wd = self.wd
        wd.get(config.accessURL)
        # Log in as test user
        wd.find_element_by_link_text("Log In").click()
        time.sleep(1)
        wd.find_element_by_id("loginForm:userName").click()
        wd.find_element_by_id("loginForm:userName").clear()
        wd.find_element_by_id("loginForm:userName").send_keys("tester")
        wd.find_element_by_id("loginForm:password").click()
        wd.find_element_by_id("loginForm:password").clear()
        wd.find_element_by_id("loginForm:password").send_keys("tester")
        wd.find_element_by_id("loginForm:login").click()
        time.sleep(1)
        # Choose to upload dataset
        wd.find_element_by_id("shareForm:shareData_button").click()
        wd.find_element_by_link_text("Add Dataset").click()
        time.sleep(1)
        # Enter dataset info
        wd.find_element_by_id("datasetForm:title").click()
        wd.find_element_by_id("datasetForm:title").clear()
        wd.find_element_by_id("datasetForm:title").send_keys("Test Drag and Drop")
        wd.find_element_by_id("datasetForm:author").click()
        wd.find_element_by_id("datasetForm:author").clear()
        wd.find_element_by_id("datasetForm:author").send_keys("tester")
        wd.find_element_by_id("datasetForm:date").click()
        wd.find_element_by_id("datasetForm:date").clear()
        wd.find_element_by_id("datasetForm:date").send_keys("2013")
        wd.find_element_by_id("datasetForm:distributor").click()
        wd.find_element_by_id("datasetForm:distributor").clear()
        wd.find_element_by_id("datasetForm:distributor").send_keys("test")
        wd.find_element_by_id("datasetForm:description").click()
        wd.find_element_by_id("datasetForm:description").clear()
        wd.find_element_by_id("datasetForm:description").send_keys("This is a test.")
        # Upload file
        wd.find_element_by_id("datasetForm:tabView:fileUpload_input").send_keys("/Users/kcondon/Downloads/50by1000.dta")
        time.sleep(3)
        if wd.find_element_by_id("datasetForm:tabView:filesTable:0:fileName").get_attribute("value") != "50by1000.dta":
            success = False
            print("Could not find file name in upload table.")
        wd.find_element_by_id("datasetForm:save").click()
        time.sleep(3)
        if not ("Test Drag and Drop" in wd.find_element_by_tag_name("html").text):
            success = False
            print("Could not find dataset title on page.")
        if not ("tester, \"Test Drag and Drop\", 2013, test, http://dx.doi.org/10.1234/dataverse/" in wd.find_element_by_tag_name("html").text):
            success = False
            print("Could not verify data citation.")
        wd.find_element_by_link_text("Log Out").click()
        self.assertTrue(success)
    
    def tearDown(self):
        if not (config.local):
            print("Link to your job: https://saucelabs.com/jobs/%s" % self.wd.session_id)        
        self.wd.quit()

if __name__ == '__main__':
    unittest.main()
