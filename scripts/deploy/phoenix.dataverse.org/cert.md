Note that `-sha256` is used but the important thing is making sure SHA-1 is not selected when uploading the CSR to https://cert-manager.com/customer/InCommon

    openssl genrsa -out phoenix.dataverse.org.key 2048

    openssl req -new -sha256 -key phoenix.dataverse.org.key -out phoenix.dataverse.org.csr

    Country Name (2 letter code) [XX]:US
    State or Province Name (full name) []:Massachusetts
    Locality Name (eg, city) [Default City]:Cambridge
    Organization Name (eg, company) [Default Company Ltd]:Harvard College
    Organizational Unit Name (eg, section) []:IQSS
    Common Name (eg, your name or your server's hostname) []:phoenix.dataverse.org
    Email Address []:support@dataverse.org
