The Admin api to make/remove a user as superuser (`POST http://$SERVER/api/admin/superuser/$identifier`
), which was previously toggle based, is now a idempotent or silent.
The api is now only to make a user as superuser.
Calling the api multiple times or on already superuser user has no effect.