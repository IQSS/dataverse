-- A script for creating, through a database stored procedure, sequential
-- 8 character identifiers from a base36 representation of current timestamp.

CREATE OR REPLACE FUNCTION base36_encode(
  IN digits bigint, IN min_width int = 0)
RETURNS varchar AS $$
DECLARE
    chars char[];
    ret varchar;
    val bigint;
BEGIN
    chars := ARRAY[
      '0','1','2','3','4','5','6','7','8','9',
      'a','b','c','d','e','f','g','h','i','j',
      'k','l','m','n','o','p','q','r','s','t',
      'u','v','w','x','y','z'];
    val := digits;
    ret := '';
    IF val < 0 THEN
        val := val * -1;
    END IF;
    WHILE val != 0 LOOP
        ret := chars[(val % 36)+1] || ret;
        val := val / 36;
    END LOOP;

    IF min_width > 0 AND char_length(ret) < min_width THEN
        ret := lpad(ret, min_width, '0');
    END IF;

    RETURN ret;
END;
$$ LANGUAGE plpgsql IMMUTABLE;


CREATE OR REPLACE FUNCTION generateIdentifierFromStoredProcedure()
RETURNS varchar AS $$
DECLARE
    curr_time_msec bigint;
    identifier varchar;
BEGIN
    curr_time_msec := extract(epoch from now())*1000;
    identifier := base36_encode(curr_time_msec);
    RETURN identifier;
END;
$$ LANGUAGE plpgsql IMMUTABLE;
