SERVER=http://localhost:8080/api

# Setup the authentication providers
echo "Setting up internal user provider"
curl -H "Content-type:application/json" -d @data/authentication-providers/builtin.json http://localhost:8080/api/admin/authenticationProviders/

#echo "Setting up Echo providers"
#curl -H "Content-type:application/json" -d @data/authentication-providers/echo.json http://localhost:8080/api/admin/authenticationProviders/
#curl -H "Content-type:application/json" -d @data/authentication-providers/echo-dignified.json http://localhost:8080/api/admin/authenticationProviders/
