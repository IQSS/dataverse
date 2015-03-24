SERVER=http://localhost:8080/api

# Setup the builtin roles
echo "Setting up admin role"
curl -H "Content-type:application/json" -d @data/role-admin.json http://localhost:8080/api/admin/roles/
echo

echo "Setting up file downloader role"
curl -H "Content-type:application/json" -d @data/role-filedownloader.json http://localhost:8080/api/admin/roles/
echo

echo "Setting up full contributor role"
curl -H "Content-type:application/json" -d @data/role-fullContributor.json http://localhost:8080/api/admin/roles/
echo

echo "Setting up dv contributor role"
curl -H "Content-type:application/json" -d @data/role-dvContributor.json http://localhost:8080/api/admin/roles/
echo

echo "Setting up ds contributor role"
curl -H "Content-type:application/json" -d @data/role-dsContributor.json http://localhost:8080/api/admin/roles/
echo

echo "Setting up editor role"
curl -H "Content-type:application/json" -d @data/role-editor.json http://localhost:8080/api/admin/roles/
echo

echo "Setting up curator role"
curl -H "Content-type:application/json" -d @data/role-curator.json http://localhost:8080/api/admin/roles/
echo

echo "Setting up member role"
curl -H "Content-type:application/json" -d @data/role-member.json http://localhost:8080/api/admin/roles/
echo
