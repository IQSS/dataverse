#!/bin/bash
echo Setting up dataverses for deletion, as described in https://redmine.hmdc.harvard.edu/issues/3543

curl -H"Content-type:application/json" -d @dv-peteDeleteTop.json http://localhost:8080/api/dataverses/peteTop?key=pete
curl -H"Content-type:application/json" -d @dv-peteDelete1.json http://localhost:8080/api/dataverses/peteDeleteTop?key=pete
curl -H"Content-type:application/json" -d @dv-peteDelete2.json http://localhost:8080/api/dataverses/peteDeleteTop?key=pete
curl -H"Content-type:application/json" -d @dv-peteDelete3.json http://localhost:8080/api/dataverses/peteDeleteTop?key=pete
