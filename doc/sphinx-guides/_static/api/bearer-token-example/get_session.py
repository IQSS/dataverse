import contextlib
import selenium.webdriver as webdriver
import selenium.webdriver.support.ui as ui
import re
import json
import requests

with contextlib.closing(webdriver.Firefox()) as driver:
    driver.get("http://localhost:8080/oidc/login?target=API&oidcp=oidc-mpconfig")
    wait = ui.WebDriverWait(driver, 100)  # timeout after 100 seconds
    wait.until(lambda driver: "accessToken" in driver.page_source)
    driver.get("view-source:http://localhost:8080/api/v1/oidc/session")
    result = wait.until(
        lambda driver: (
            driver.page_source if "accessToken" in driver.page_source else False
        )
    )
    m = re.search("<pre>(.+?)</pre>", result)
    if m:
        found = m.group(1)
    session = json.loads(found)

    token = session["data"]["accessToken"]
    endpoint = "http://localhost:8080/api/v1/users/:me"
    headers = {"Authorization": "Bearer " + token}

    print()
    print(requests.get(endpoint, headers=headers).json())
