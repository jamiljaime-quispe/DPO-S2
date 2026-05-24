package Persistence;

/**
 * Starts, commits, and rolls back database transactions. This interface keeps the promise clear so another
 * class can use it without depending on a specific implementation.
 * <p>
 * The interface lets the business layer ask for stored data without depending on the class that talks
 * directly to the database.
 * </p>
 */
public interface TransactionManager {

	/**
	 * Starts a database transaction. The operation is kept together so the stored data remains consistent if
	 * something goes wrong halfway through.
	 * <p>
	 * This helper keeps the step named and separate, which makes the larger operation easier to read and
	 * follow.
	 * </p>
	 */
	void beginTransaction();

	/**
	 * Saves all changes made in the current transaction. The operation is kept together so the stored data
	 * remains consistent if something goes wrong halfway through.
	 * <p>
	 * This helper keeps the step named and separate, which makes the larger operation easier to read and
	 * follow.
	 * </p>
	 */
	void commit();

	/**
	 * Reverts all changes made in the current transaction. The operation is kept together so the stored data
	 * remains consistent if something goes wrong halfway through.
	 * <p>
	 * This helper keeps the step named and separate, which makes the larger operation easier to read and
	 * follow.
	 * </p>
	 */
	void rollback();
}
