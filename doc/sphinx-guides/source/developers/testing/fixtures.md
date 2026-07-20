# Fixtures For Tests

Most Dataverse test fixtures are based on JSON files stored in the test resources of the codebase.

In addition, (as of Dataverse 6.11) you can use a generator utility to create dataset-centered fixtures programmatically.
This is most useful for local integration and performance tests but may be of use for unit tests as well.

```{contents} Contents:
:local:
:depth: 3
```

(fixture-generator)=
## Dataset Fixture Generator

The dataset fixture generator is a test utility for creating connected dataset entity graphs with configurable size and shape.
It is located in the core testing utilities at `edu.harvard.iq.dataverse.util.testing.fixtures` and `edu.harvard.iq.dataverse.util.testing.recipes`.

The fixture generator is useful when tests need one or more datasets with many files, tabular files, variables, and optional variable metadata, while still keeping the test setup readable.
It is primarily intended for integration and performance tests where hand-building entities would be too verbose, brittle, or too uniform to uncover ORM and serialization issues.

The generator creates an in-memory entity graph.
Persisting that graph to a database is optional and requires the usual JPA persistence rules to be respected (see below).


### Architecture

The fixture generator is built around three main concepts: a builder, recipes for it, and field populators.
This separation keeps entity graph shape, relationship wiring, and scalar field population independent of each other.

#### Fixture Builder

The builder creates the connected *entity graph* by consuming recipes. It is responsible for:

- Creating the entities
- Wiring relationships
- Keeping both sides of relationships in sync where needed
- Returning a `DatasetFixture` with convenient references to generated objects

#### Recipes

Recipes *describe* the *shape* of the fixture's entity graph and should not manually wire entity relationships:

- How many files should exist?
- Which files are tabular?
- How many variables should a tabular file contain?
- Should variable metadata be created?

**Available Recipes:**

Recipes are composable using a fluent API and work together.

```text
DatasetRecipe
  -> DatasetTypeRecipe
  -> VersionRecipe
       -> FileRecipe
            -> VariableSetRecipe
                 -> VariableMetadataRecipe
```

`DatasetRecipe`  
Top-level recipe for creating a dataset fixture. It combines a `DatasetTypeRecipe` and a `VersionRecipe`.

`DatasetTypeRecipe`  
Provides the dataset type assigned to the generated dataset.
Can create a dataset type from scalar values or wrap an existing instance.

Note: the recipe provides the type object but does not persist it.
Tests that persist generated fixtures must ensure the dataset type is managed before the dataset is flushed.

`VersionRecipe`  
Describes the current dataset version. At the moment, this mainly means providing one or more file recipes.

`FileRecipe`  
Describes file populations. A file recipe may create regular files or tabular files.

`VariableSetRecipe`  
Describes how many variables to create for tabular files. It supports uniform and skewed variable populations.

`VariableMetadataRecipe`  
Decides whether a `VariableMetadata` row should be created for a generated `(FileMetadata, DataVariable)` pair.
At most one metadata row is generated for each such pair.


#### Fixture Populator

The populator fills scalar and non-relationship *fields*, which are not primarily about graph shape.

It sets values such as:

- Identifiers
- Timestamps
- File labels
- Content types
- Checksums
- Variable names
- Required fields
- Null-sensitive collections

The default *minimal* populator is conservative.
It creates enough data for serialization and persistence tests, but it does not try to simulate fully realistic production metadata.



### Full Example

The following example creates a small but non-uniform dataset fixture. It's suitable
- for a smoke test of a serializer,
- for an integration test with assertions on the result,
- for a performance test with benchmarking speed of different implementations, and other scenarios.

```java
var recipe = DatasetRecipe.of(
    DatasetTypeRecipe.dataset(),
    VersionRecipe.of(
        FileRecipe.regular(20),
        FileRecipe.tabular(30, 
            VariableSetRecipe
                .byPredicate(VariableMetadataRecipe.byPredicate(ctx -> ctx.variableIndex() < 5))
                .when(ctx -> ctx.fileIndex() % 10 == 0, 1_000)
                .otherwise(25)
    ))
);

DatasetFixture fixture = DatasetFixtureBuilder.builder()
    .recipe(recipe)
    .populator(FixturePopulator.minimal())
    .build();

JsonArrayBuilder files = Json.createArrayBuilder();

for (FileMetadata fileMetadata : fixture.fileMetadatas()) {
    files.add(JsonPrinter.json(fileMetadata.getDataFile(), fileMetadata, true));
}

var json = files.build();
```

This creates:
- 20 regular files
- 30 tabular files
  - some tabular files with 1,000 variables
  - other tabular files with 25 variables
  - variable metadata only for the first few variables in each tabular file

This helps exercise code paths that traverse files, file metadata, data tables, data variables, variable metadata.
All of this happends without the need to pre-produce an enormous fixture as a JSON file.
Its deterministic nature allows running the test anywhere without depending on seeded randomness, offering reliable and reproducible results.



### Basic Usage

#### Small Dataset

This example creates:

- one dataset
- one current version
- 10 tabular files
- 10 variables per tabular file
- 1 regular file

```java
var recipe = DatasetRecipe.of(
    DatasetTypeRecipe.dataset(),
    VersionRecipe.of(
        FileRecipe.tabular(10, VariableSetRecipe.uniform(10)),
        FileRecipe.regular(1)
    )
);

DatasetFixture fixture = DatasetFixtureBuilder.builder()
    .recipe(recipe)
    .populator(FixturePopulator.minimal())
    .build();

Dataset dataset = fixture.dataset();
DatasetVersion version = fixture.currentVersion();
```

#### Skewed Variable Populations

Skewed data is useful for performance testing because real datasets are rarely uniform.
Some files may have only a few variables, while others may be very large.

This example creates 500 tabular files:

- one dataset
- one current version
- 500 tabular files
  - every 100th file receives 100,000 variables
  - every 10th file receives 10,000 variables
  - all others receive 250 variables

```java
var variables = VariableSetRecipe.byPredicate()
    .when(ctx -> ctx.fileIndex() % 100 == 0, 100_000)
    .when(ctx -> ctx.fileIndex() % 10 == 0, 10_000)
    .otherwise(250);

var recipe = DatasetRecipe.of(
    DatasetTypeRecipe.dataset(),
    VersionRecipe.of(
        FileRecipe.tabular(500, variables)
    )
);

DatasetFixture fixture = DatasetFixtureBuilder.builder()
    .recipe(recipe)
    .build();
```

#### Adding Variable Metadata

Variable Metadata is optional and controlled by `VariableMetadataRecipe`.
The metadata recipe is evaluated for each generated `(FileMetadata, DataVariable)` pair.
This matters because `VariableMetadata` is versioned indirectly through `FileMetadata`.

*No variable metadata (default):*

```java
VariableSetRecipe.uniform(1_000)
- or -
VariableSetRecipe.uniform(1_000, VariableMetadataRecipe.noop())
```

*Metadata for every variable:*

```java
VariableSetRecipe.uniform(1_000, VariableMetadataRecipe.always())
```

*Metadata for selected variables:*

```java
VariableSetRecipe.uniform(1_000, VariableMetadataRecipe.byPredicate(ctx -> ctx.variableIndex() % 10 == 0))
```



### Persistence Usage

The generator creates an in-memory entity graph. Persisting that graph is optional and follows normal JPA rules.

When persisting a generated fixture to a database, remember that not all relationships cascade from `Dataset` to every object.
In particular, `DataFile` instances usually need to be persisted explicitly before persisting the dataset graph.
The `DatasetType` must also be managed, either by persisting the generated type or by looking up an existing one in the same persistence context.

A typical persistence sequence is:

```java
jpa.inTransactionVoid(em -> {
    em.persist(fixture.datasetType());
    for (DataFile dataFile : fixture.dataFiles()) {
        em.persist(dataFile);
    }
    em.persist(fixture.dataset());
});
```

The exact order may evolve as the fixture generator grows, may depend on the exact usage scenario, and
is influenced by the evolution of the entity classes themselves, but the important point is:  
**Shared/reference entities and non-cascaded entities must be managed (persisted) before the dataset graph is flushed**.



### Discussion and Limitations

#### Benefits

1. **Readable scenarios:** tests describe intent at a high level. 
   For example: `FileRecipe.tabular(500, VariableSetRecipe.uniform(1_000))` is easier to understand than manually creating thousands of entities.
2. **Composable graph shape:** different recipes can be combined to describe mixed datasets.
3. **Deterministic output:** the build context carries fixture-wide values such as sequence and timestamp, making generated data easier to debug and compare.
4. **Reduced boilerplate:** relationship wiring and null-sensitive defaults are centralized.
5. **Better performance testing:** skewed fixtures can expose ORM issues that uniform data may hide, such as N+1 query expansion over large variable collections.
6. **Serialization safety:** the minimal populator initializes fields and collections that serializers commonly traverse.

#### Tradeoffs

1. **More concepts to learn:** developers need to understand builders, recipes, populators, and resulting fixture objects vs. a static factory.
2. **Not a full production object factory:** the minimal populator creates safe test data, not necessarily realistic production data.
3. **Persistence still requires care:** some entities must be persisted explicitly because the production model does not cascade every relationship.
4. **Hardcoded defaults:** the minimal populator uses deterministic placeholder values, tests that need realistic metadata should provide a custom populator.

#### Limitations

1. **Minimalistic:** The current fixture generator is intentionally minimal.
2. **Single dataset version only:** the fixture currently models one current dataset version and does not generate multiple versions.
3. **No version evolution recipes:** there is no support yet for deriving later versions from earlier versions, modeling change over time.
4. **Limited dataset metadata:** dataset fields and metadata blocks are not generated in detail.
5. **Simple dataset type handling:** a `DatasetType` can be generated or supplied, but persistence of shared types is still the responsibility of the test.
6. **No persistence manager:** the fixture system builds graphs, but it does not yet provide a dedicated persister that knows the correct persistence order.
7. **One table per tabular file:** tabular files currently get one `DataTable`. The domain model can allow more, but the fixture generator does not expose that yet.
8. **One variable group per tabular file:** each non-empty tabular file currently gets one `VarGroup` containing all variables, there is no `VarGroupRecipe` yet.
9. **Limited variable metadata content:** variable metadata can be present or absent, but the minimal populator only fills basic scalar values.
10. **No category or statistics recipes:** the fixture generator does not yet provide recipes for variable categories, summary statistics, invalid ranges, or category metadata.

#### Unsupported Usage Scenarios

The following scenarios are not yet directly expressible:

- multiple dataset versions sharing the same `DataFile` objects
- metadata-only changes between versions
- version-specific `VariableMetadata` changes across versions
- files added or removed between versions
- multiple `DataTable` objects per file
- different variable group distributions per file
- weighted random or seeded random file populations
- Zipf-like or heavy-tail distributions as first-class recipes
- realistic dataset field metadata
- fixture graphs that mimic a fully published dataset lifecycle



### Extending The Fixture Generator

When extending the fixture generator, first decide which responsibility your change belongs to.

#### Add Recipes For Graph Shaping

Use a new recipe when the test needs to describe what shape should be created.

Examples:

- number of var groups
- number of data tables per file
- whether categories should exist
- how many variables receive summary statistics
- how versions evolve over time

Recipe changes usually belong in the `edu.harvard.iq.dataverse.util.testing.recipes` package.

#### Add Populator Behavior For Scalar Values

Use a new or custom populator when entities should be filled differently, but the graph shape is the same.
Extend the populator interface if new types of scalar data are required.

Examples:

- more realistic file names
- different content types
- richer variable labels
- custom checksums
- realistic variable metadata text

Populator changes usually belong in the `edu.harvard.iq.dataverse.util.testing.fixtures` package.

#### Change Builder For Wiring

Change the builder when new relationships must be created or maintained.

Examples:

- adding support for `VariableCategory`
- wiring category metadata
- creating multiple data tables per file
- linking version-evolved file metadata back to shared data files

Builder changes should be kept small and split into helper methods where possible.

#### Recommended Extension Path

A practical roadmap for further evolution is:

1. Add a `VarGroupRecipe` to control group count and membership.
2. Add category and summary statistic recipes for variable-level enrichment.
3. Add a fixture persister that knows the correct persistence order.
4. Add version evolution recipes for multi-version datasets.
5. Add richer dataset metadata generation.
6. Add (seeded!) random distribution recipes if a deterministic skew is not enough.
7. Add fuzzy testing by generating fixtures with targeted chaos.

#### Guidelines For Contributions

1. Keep recipes declarative: recipes should describe shape, not manually wire entity relationships.
2. Keep populators focused: populators should fill fields, not decide how many entities exist.
3. Keep builders responsible for wiring: relationship consistency belongs in the builder.
4. Prefer deterministic generation: deterministic data makes performance tests easier to reproduce and debug.
5. Avoid hiding persistence requirements: if an entity must be persisted before another, document it clearly or add a dedicated persister.
6. Start minimal: add the smallest recipe or populator extension needed for the scenario. Avoid making the DSL generic before there is a concrete test need.
