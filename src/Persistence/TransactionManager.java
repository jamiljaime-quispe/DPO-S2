package Persistence;

/**
 * Starts, commits, and rolls back database transactions.
 */
public interface TransactionManager {

	/** Starts a database transaction. */
	void beginTransaction();

	/** Saves all changes made in the current transaction. */
	void commit();

	/** Reverts all changes made in the current transaction. */
	void rollback();
}
