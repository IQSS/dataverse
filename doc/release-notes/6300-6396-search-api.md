## Major Use Cases

Newly-supported use cases in this release include:

- Search API users will see additional fields in the JSON output #6300 #6396

## Notes for Tool Developers and Integrators

### Search API

The boolean parameter `query_entities` has been removed from the Search API.

The former "true" behavior of "whether entities are queried via direct database calls (for developer use)" is now always true.
