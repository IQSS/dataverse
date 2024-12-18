Bug Fix:
OAI Client harvesting now uses the correct granularity while re-run a partial harvest (using the `from` parameter). The correct granularity comes from the `Identify` verb request.