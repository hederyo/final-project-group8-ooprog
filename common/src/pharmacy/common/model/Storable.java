package pharmacy.common.model;

/**
 * Interface for all entities that can be persisted to pipe-delimited text files.
 *
 * Demonstrates: Abstraction (defines consistent ID and serialization methods).
 */
public interface Storable {

    /**
     * Returns the unique identifier of the entity.
     *
     * @return the unique record ID
     */
    String getId();

    /**
     * Serializes the entity into a single pipe-delimited line for file storage.
     *
     * @return formatted record string
     */
    String toFileLine();
}
