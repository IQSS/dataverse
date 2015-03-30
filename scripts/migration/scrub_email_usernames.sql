-- first, find users with e-mails as usernames
select id, username, email from vdcuser where username like '%@%'
--and username != email;

-- then find which those which would create duplicates after truncating
-- (verify that adding 1 would be OK; if not, you may need to update some individually)
select u1.id, u1.username, u2.id, u2.username  from vdcuser u1, vdcuser u2
where u1.id != u2.id
and u1.username like '%@%'
and split_part (u1.username, '@', 1) = u2.username

-- for those usernames, truncate and add 1, so no duplicates
update vdcuser set username = split_part (username, '@', 1) ||'1'
where id in (
select u1.id  from vdcuser u1, vdcuser u2
where u1.id != u2.id
and u1.username like '%@%'
and split_part (u1.username, '@', 1) = u2.username
)

--  now truncate the rest
update vdcuser set username = split_part (username, '@', 1) where username like '%@%'

-- confirm no duplicates
select id, username, email from vdcuser where username in (
select username from vdcuser
group by username having count(*) > 1
)