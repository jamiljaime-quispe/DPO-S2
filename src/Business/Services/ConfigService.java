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
		this.config = loadConfig(configDAO);
		validateConfig();
	}

	/** Checks that all required configuration values are present. */
	private void validateConfig() {
		if (isBlank(getDbIP())) {
			throw new IllegalStateException("config.json is missing dbIP.");
		}
		if (getDbPort() <= 0) {
			throw new IllegalStateException("config.json is missing dbPort.");
		}
		if (isBlank(getDbName())) {
			throw new IllegalStateException("config.json is missing dbName.");
		}
		if (isBlank(getDbUser())) {
			throw new IllegalStateException("config.json is missing dbUser.");
		}
		if (getDbPassword() == null) {
			setDbPassword("");
		}
		if (isBlank(getAdminPasswordValue())) {
			throw new IllegalStateException("config.json is missing adminPassword.");
		}
		if (getSimulatedVehicleDelay() <= 0) {
			setSimulatedVehicleDelay(5);
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
		dbConfig.put("ip", getDbIP());
		dbConfig.put("port", String.valueOf(getDbPort()));
		dbConfig.put("name", getDbName());
		dbConfig.put("user", getDbUser());
		dbConfig.put("password", getDbPassword());
		return dbConfig;
	}

	/**
	 * Gets the admin account password from the configuration.
	 *
	 * @return the admin password
	 */
	public String getAdminPassword() {
		return getAdminPasswordValue();
	}

	/**
	 * Gets the simulation delay in seconds.
	 *
	 * @return the maximum delay between simulated vehicle events
	 */
	public int getSimulationDelay() {
		return getSimulatedVehicleDelay();
	}

	/**
	 * Gets the full Config object.
	 *
	 * @return the loaded Config
	 */
	public Config getConfig() {
		return config;
	}

	/** Loads configuration through the persistence layer. */
	private Config loadConfig(ConfigDAO configDAO) {
		return configDAO.loadConfig();
	}

	/** Gets the configured database IP. */
	private String getDbIP() {
		return config.getDbIP();
	}

	/** Gets the configured database port. */
	private int getDbPort() {
		return config.getDbPort();
	}

	/** Gets the configured database name. */
	private String getDbName() {
		return config.getDbName();
	}

	/** Gets the configured database user. */
	private String getDbUser() {
		return config.getDbUser();
	}

	/** Gets the configured database password. */
	private String getDbPassword() {
		return config.getDbPassword();
	}

	/** Sets the configured database password. */
	private void setDbPassword(String password) {
		config.setDbPassword(password);
	}

	/** Gets the configured admin password. */
	private String getAdminPasswordValue() {
		return config.getAdminPassword();
	}

	/** Gets the configured simulation delay. */
	private int getSimulatedVehicleDelay() {
		return config.getSimulatedVehicleDelay();
	}

	/** Sets the configured simulation delay. */
	private void setSimulatedVehicleDelay(int delay) {
		config.setSimulatedVehicleDelay(delay);
	}
}
