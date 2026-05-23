package Persistence;

import Business.Entities.Config;

/**
 * Data access interface for application configuration.
 */
public interface ConfigDAO {

	/**
	 * Loads the application configuration.
	 *
	 * @return loaded configuration values
	 */
	Config loadConfig();
}
