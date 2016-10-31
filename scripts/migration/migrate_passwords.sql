update builtinuser 
set passwordencryptionversion = 0,
encryptedpassword= _dvn3_vdcuser.encryptedpassword
from _dvn3_vdcuser
where _dvn3_vdcuser.username=builtinuser.username;
