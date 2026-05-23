package Business.Services;

import Business.Entities.Config;
import Persistence.ConfigDAO;

import java.util.HashMap;
import java.util.Map;

/**
 * Provides access to application configuration values loaded from config.json.
 */
public class ConfigService {
	private final Config config;

	/**
	 * Constructs a new ConfigService.
	 * Gets configuration values through the persistence layer.
	 *
	 * @param configDAO the configuration DAO
	 */
	public ConfigService(ConfigDAO configDAO) {
		if (configDAO == null) {
			throw new IllegalArgumentException("ConfigDAO is required.");
		}
		this.config = configDAO.loadConfig();
		validateConfig();
	}

	/** Checks that all required configuration values are present. */
	private void validateConfig() {
		if (isBlank(config.getDbIP())) {
			throw new IllegalStateException("config.json is missing dbIP.");
		}
		if (config.getDbPort() <= 0) {
			throw new IllegalStateException("config.json is missing dbPort.");
		}
		if (isBlank(config.getDbName())) {
			throw new IllegalStateException("config.json is missing dbName.");
		}
		if (isBlank(config.getDbUser())) {
			throw new IllegalStateException("config.json is missing dbUser.");
		}
		if (config.getDbPassword() == null) {
			config.setDbPassword("");
		}
		if (isBlank(config.getAdminPassword())) {
			throw new IllegalStateException("config.json is missing adminPassword.");
		}
		if (config.getSimulatedVehicleDelay() <= 0) {
			config.setSimulatedVehicleDelay(5);
		}
	}

	/** Checks whether a text value is null or empty after trimming. */
	private boolean isBlank(String value) {
		return value == null || value.trim().isEmpty();
	}

	/**
	 * Gets the database connection parameters.
	 *
	 * @return a map with keys: ip, port, name, user, password
	 */
	public Map<String, String> getDatabaseConfig() {
		Map<String, String> dbConfig = new HashMap<>();
		dbConfig.put("ip", config.getDbIP());
		dbConfig.put("port", String.valueOf(config.getDbPort()));
		dbConfig.put("name", config.getDbName());
		dbConfig.put("user", config.getDbUser());
		dbConfig.put("password", config.getDbPassword());
		return dbConfig;
	}

	/**
	 * Gets the admin account password from the configuration.
	 *
	 * @return the admin password
	 */
	public String getAdminPassword() {
		return config.getAdminPassword();
	}

	/**
	 * Gets the simulation delay in seconds.
	 *
	 * @return the maximum delay between simulated vehicle events
	 */
	public int getSimulationDelay() {
		return config.getSimulatedVehicleDelay();
	}

	/**
	 * Gets the full Config object.
	 *
	 * @return the loaded Config
	 */
	public Config getConfig() {
		return config;
	}
}
