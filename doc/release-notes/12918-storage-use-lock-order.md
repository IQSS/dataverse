### Concurrent uploads no longer deadlock on storage use

Every saved file adds its size to the recorded storage use of its dataset and of every collection above it, up to the root. On installations with fewer than about 25,000 datasets and collections, PostgreSQL reads the whole storage use table for that update and locks the rows in the order in which it finds them, which changes with every update. Two uploads into the same collection could therefore lock the same rows in opposite order and deadlock. PostgreSQL then cancelled one of them, and that upload failed.

With a few uploads a minute this rarely happens, and a retry usually works. When many people upload at the same time, it adds up quickly. In a load test on Dataverse 6.12 with 64 users depositing at once, three out of four deposits failed and uploads nearly came to a stop.

The update now always locks the rows in the same order, by their id, so two uploads can no longer block each other this way. In the same load test, every deposit succeeded. Storage quotas keep working as before.

No upgrade steps are needed.
