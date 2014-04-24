#!/bin/bash

# deleting an unreleased dataset, with a bunch of unreleased files. 
# seems to be working like a charm - ? 
#     -- Leonid

curl -X DELETE http://localhost:8080/api/datasets/43?key=pete

