#!/bin/env bash
# Log in as admin and print access token

RESP=$(curl -X POST 'http://localhost:8081/realms/hikeway-keycloak/protocol/openid-connect/token' -H 'content-type: application/x-www-form-urlencoded' --data client_id=backend-test --data grant_type=password --data username=admin --data password=admin -s)

if [[  $RESP =~ "error" ]]; then
    echo $RESP
    exit
fi

# We assume that access_token is the 1st field in the json
echo $(awk -F '"' '{print $4}' <<< $RESP)
