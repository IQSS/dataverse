A new alternative implementation of Globus polling during upload data transfers has been added in this release. This experimental framework does not rely on the instance staying up continuously for the duration of the transfer and saves the state information about Globus upload requests in the database. See the `globus-use-experimental-async-framework` feature flag in the Configuration guide.