/* We're adding a new permission at bit 10 (512 ManageFilePermissions) that should be set for any role that has the permission
 in bit 9 (256 ManageDatasetPermissions).
That means that any roles with current bits 10-13 (512+1024+2048+4096 = 7680) needs to have the corresponding bits 11-14 set 
after the update, which can be accomplished by adding permissionbits & 7680 to the original value. (So a role with just bit 10 
set (512) would have 512&7680 (=512) added to it and become bit 11 (1024). The same logic also shifts any values in bits 11-13 
into bits 12-14.

In addition, any role wite permission for bit 9 set now should also have bit 10 set after the update 
(any current role with ManageDatasetPermissions gets ManageDatasetPermissions and ManageFilePermissions after the update).
To do this we just add an addition bit 10 (512) if permissionbits&256!=0

Finally, to make this idempotent, at least under the assumption that the standard admin role with all permissions 
(or some role with current permission bit 13 set), we check to make sure no role has bit 14+ set - max(permissionsbits) <8192. 
With the IF statement, running this script more than once will only cause an update on the first pass.

*/
DO
$do$
BEGIN
   IF (SELECT MAX(permissionbits) FROM dataverserole) < 8192 THEN
     UPDATE dataverserole SET permissionbits=permissionbits + (permissionbits & 7680)  WHERE (permissionbits & 256 =0);
     UPDATE dataverserole SET permissionbits=permissionbits + (permissionbits & 7680) +512 WHERE (permissionbits & 256 !=0);
   END IF;
END
$do$;
