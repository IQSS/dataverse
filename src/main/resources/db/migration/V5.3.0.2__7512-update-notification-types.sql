--In support of removing world map we are deleting notifications related to world map
--and updating notifications to reflect the current enumeration of types on UserNotification

delete from usernotification where type = 5 or type = 6;

update usernotification set type = type - 2 where type > 6;