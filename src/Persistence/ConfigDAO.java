package Persistence;

import Business.Entities.Config;

/**
 * Data access interface for application configuration. This interface keeps the promise clear so another
 * class can use it without depending on a specific implementation.
 * <p>
 * The interface lets the business layer ask for stored data without depending on the class that talks
 * directly to the database.
 * </p>
 */
public interface ConfigDAO {

	/**
	 * Loads config.
	 * <p>
	 * This helper keeps the step named and separate, which makes the larger operation easier to read and
	 * follow.
	 * </p>
	 *
	 * @return loaded configuration values
	 */
	Config loadConfig();
}
