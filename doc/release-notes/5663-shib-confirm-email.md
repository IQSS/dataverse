For Shib users we now set the emailconfirmed timestamp on login. (The guides say we do this already but are wrong. It was only being set on account creation.)

For Shib users, I also prevent "check for your welcome email to verify your address" from being shown in the in-app welcome/new account notification.

I put in a check to make sure Shib users never get a "verify your email address" email notification.

Finally, I removed the hasNoStaleVerificationTokens check from the hasVerifiedEmail method. We've never worried about if there are stale verification tokens in the database or not and this check was preventing "Verified" from being shown, even when the user has a timestamp (the timestamp being the way we know if an email is verified or not).
