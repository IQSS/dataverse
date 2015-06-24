from locust import HttpLocust, TaskSet
from dataverse_tasks import homepage, random_dataset_page
import requests
requests.packages.urllib3.disable_warnings()


class BrowseAndDownloadBehavior(TaskSet):
    tasks = {
                homepage: 50,
                random_dataset_page: 50,
    }


class WebsiteUser(HttpLocust):
    task_set = BrowseAndDownloadBehavior
    min_wait = 5000     # min pause before new task
    max_wait = 20000    # max pause before new task

"""
locust -f basic_test_01.py
http://127.0.0.1:8089/
"""
