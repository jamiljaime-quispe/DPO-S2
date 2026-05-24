package Business.Services;

import Business.Entities.Config;
import Persistence.ConfigDAO;

import java.util.HashMap;
import java.util.Map;

/**
 * Provides access to application configuration values loaded from config.json.
 * <p>
 * The service keeps the business rule in one place before any data is saved, loaded, or shown. This helps
 * the rest of the project call the same logic consistently.
 * </p>
 */
public class ConfigService {
	private final Config config;

	/**
	 * Constructs a new ConfigService. Gets configuration values through the persistence layer.
	 * <p>
	 * The constructor receives the objects or values this class needs and stores them before the rest of the
	 * methods are used.
	 * </p>
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

	/**
	 * Handles validate config.
	 * <p>
	 * This method keeps the business decision in the service layer before anything is sent back to the screen.
	 * </p>
	 */
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

	/**
	 * Checks whether blank.
	 * <p>
	 * This method keeps the business decision in the service layer before anything is sent back to the screen.
	 * </p>
	 *
	 * @param value value used by this operation
	 * @return true when the condition is met, false otherwise
	 */
	private boolean isBlank(String value) {
		return value == null || value.trim().isEmpty();
	}

	/**
	 * Gets the database connection parameters.
	 * <p>
	 * The getter keeps the field private while still giving the rest of the project a clear way to read it.
	 * </p>
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
	 * <p>
	 * The getter keeps the field private while still giving the rest of the project a clear way to read it.
	 * </p>
	 *
	 * @return the admin password
	 */
	public String getAdminPassword() {
		return getAdminPasswordValue();
	}


	/**
	 * Gets the full Config object.
	 * <p>
	 * The getter keeps the field private while still giving the rest of the project a clear way to read it.
	 * </p>
	 *
	 * @return the loaded Config
	 */
	public Config getConfig() {
		return config;
	}

	/**
	 * Loads config.
	 * <p>
	 * This method obtains the needed data through the persistence interfaces and returns it in a form the
	 * controller can use.
	 * </p>
	 *
	 * @param configDAO config DAO used to read or write the needed data
	 * @return the loaded config
	 */
	private Config loadConfig(ConfigDAO configDAO) {
		return configDAO.loadConfig();
	}

	/**
	 * Gets the configured database IP.
	 * <p>
	 * The getter keeps the field private while still giving the rest of the project a clear way to read it.
	 * </p>
	 *
	 * @return the current db ip
	 */
	private String getDbIP() {
		return config.getDbIP();
	}

	/**
	 * Gets the configured database port.
	 * <p>
	 * The getter keeps the field private while still giving the rest of the project a clear way to read it.
	 * </p>
	 *
	 * @return the current db port
	 */
	private int getDbPort() {
		return config.getDbPort();
	}

	/**
	 * Gets the configured database name.
	 * <p>
	 * The getter keeps the field private while still giving the rest of the project a clear way to read it.
	 * </p>
	 *
	 * @return the current db name
	 */
	private String getDbName() {
		return config.getDbName();
	}

	/**
	 * Gets the configured database user.
	 * <p>
	 * The getter keeps the field private while still giving the rest of the project a clear way to read it.
	 * </p>
	 *
	 * @return the current db user
	 */
	private String getDbUser() {
		return config.getDbUser();
	}

	/**
	 * Gets the configured database password.
	 * <p>
	 * The getter keeps the field private while still giving the rest of the project a clear way to read it.
	 * </p>
	 *
	 * @return the current db password
	 */
	private String getDbPassword() {
		return config.getDbPassword();
	}

	/**
	 * Sets the configured database password.
	 * <p>
	 * The setter keeps the field change inside this object instead of letting other classes touch the field
	 * directly.
	 * </p>
	 *
	 * @param password password entered by the user
	 */
	private void setDbPassword(String password) {
		config.setDbPassword(password);
	}

	/**
	 * Gets the configured admin password.
	 * <p>
	 * The getter keeps the field private while still giving the rest of the project a clear way to read it.
	 * </p>
	 *
	 * @return the current admin password value
	 */
	private String getAdminPasswordValue() {
		return config.getAdminPassword();
	}

	/**
	 * Gets the configured simulation delay.
	 * <p>
	 * The getter keeps the field private while still giving the rest of the project a clear way to read it.
	 * </p>
	 *
	 * @return the current simulated vehicle delay
	 */
	private int getSimulatedVehicleDelay() {
		return config.getSimulatedVehicleDelay();
	}

	/**
	 * Sets the configured simulation delay.
	 * <p>
	 * The setter keeps the field change inside this object instead of letting other classes touch the field
	 * directly.
	 * </p>
	 *
	 * @param delay delay used by this operation
	 */
	private void setSimulatedVehicleDelay(int delay) {
		config.setSimulatedVehicleDelay(delay);
	}
}
