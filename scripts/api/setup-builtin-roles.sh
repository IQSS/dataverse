SERVER=http://localhost:8080/api

# Setup the authentication providers
echo "Setting up admin role"
curl -H "Content-type:application/json" -d @data/role-admin.json http://localhost:8080/api/s/roles/

echo "Setting up file downloader role"
curl -H "Content-type:application/json" -d @data/role-filedownloader.json http://localhost:8080/api/s/roles/
