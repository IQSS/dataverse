FROM osixia/openldap:1.5.0

# Add custom LDIF users file
COPY 50-users.ldif /container/service/slapd/assets/config/bootstrap/ldif/custom/50-users.ldif
RUN chown 911:911 /container/service/slapd/assets/config/bootstrap/ldif/custom/50-users.ldif
