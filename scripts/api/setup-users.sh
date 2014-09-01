#!/bin/bash -f
curl -H "Content-type:application/json" -X POST -d @data/userPete.json "http://localhost:8080/api/users?password=pete"
echo
curl -H "Content-type:application/json" -X POST -d @data/userUma.json "http://localhost:8080/api/users?password=uma"
echo
curl -H "Content-type:application/json" -X POST -d @data/userGabbi.json "http://localhost:8080/api/users?password=gabbi"
echo
curl -H "Content-type:application/json" -X POST -d @data/userCathy.json "http://localhost:8080/api/users?password=cathy"
echo
curl -H "Content-type:application/json" -X POST -d @data/userNick.json "http://localhost:8080/api/users?password=nick"
