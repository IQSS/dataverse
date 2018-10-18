ALTER TABLE usernotification
ADD requestor_id BIGINT;

INSERT INTO setting(
           name, content)
   VALUES (':UploadMethods', 'native/http');
