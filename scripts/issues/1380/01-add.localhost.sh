# Add the localhost group to the system.
curl -X POST -H"Content-Type:application/json" -d@../../api/data/ipGroup-localhost.json  localhost:8080/api/admin/groups/ip
