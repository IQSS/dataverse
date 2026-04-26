package edu.harvard.iq.dataverse.util.testing.fixtures;

/**
 * Immutable build context shared across a single fixture build.
 *
 * <p>This object exists so populators and helpers do not have to depend on
 * builder-internal types. As more cross-cutting build information is needed
 * (for example version index, deterministic seed, or builder configuration),
 * it can be added here without changing populator method signatures.</p>
 *
 * @param sequence deterministic sequence number for the fixture instance
 */
public record BuildContext(
        long sequence
) {
}
