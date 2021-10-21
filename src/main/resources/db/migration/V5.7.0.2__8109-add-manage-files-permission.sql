UPDATE dataverserole SET permissionbits=permissionbits + 8192 WHERE (permissionbits & 256 >0) AND (permissionbits & 8192 = 0);

