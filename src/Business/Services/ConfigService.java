package Business.Services;

import Business.Entities.Config;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Provides access to application configuration values loaded from config.json.
 */
public class ConfigService {
	private final Config config;

	/**
	 * Constructs a new ConfigService.
	 * Reads config.json from the project root and populates the Config object.
	 *
	 * @param config the Config object to populate
	 */
	public ConfigService(Config config) {
		this.config = config;
		loadConfig();
	}

	/**
	 * Reads config.json and populates the Config object.
	 */
	private void loadConfig() {
		File configFile = new File("config.json");
		if (!configFile.exists()) {
			throw new IllegalStateException("Missing config.json in the project root.");
		}

		try (BufferedReader reader = new BufferedReader(new FileReader(configFile))) {
			StringBuilder sb = new StringBuilder();
			String line;
			while ((line = reader.readLine()) != null) {
				sb.append(line.trim());
			}
			String json = sb.toString();

			config.setDbPort(parseIntValue(json, "dbPort"));
			config.setDbIP(parseStringValue(json, "dbIP"));
			config.setDbName(parseStringValue(json, "dbName"));
			config.setDbUser(parseStringValue(json, "dbUser"));
			config.setDbPassword(parseStringValue(json, "dbPassword"));
			config.setAdminPassword(parseStringValue(json, "adminPassword"));
			config.setSimulatedVehicleDelay(parseIntValue(json, "simulatedVehicleDelay"));

			validateConfig();
		} catch (IOException e) {
			throw new IllegalStateException("Could not read config.json: " + e.getMessage(), e);
		}
	}

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

	private boolean isBlank(String value) {
		return value == null || value.trim().isEmpty();
	}

	/**
	 * Extracts a String value for the given key from a flat JSON string.
	 *
	 * @param json raw JSON text
	 * @param key field name to look up
	 * @return extracted string value, or null if not found
	 */
	private String parseStringValue(String json, String key) {
		int keyIdx = json.indexOf("\"" + key + "\"");
		if (keyIdx == -1) return null;
		int colonIdx = json.indexOf(':', keyIdx);
		if (colonIdx == -1) return null;
		String rest = json.substring(colonIdx + 1).trim();
		if (!rest.startsWith("\"")) return null;
		int start = 1;
		int end = rest.indexOf('"', start);
		return end == -1 ? null : rest.substring(start, end);
	}

	/**
	 * Extracts an int value for the given key from a flat JSON string.
	 *
	 * @param json raw JSON text
	 * @param key field name to look up
	 * @return extracted int value, or 0 if not found
	 */
	private int parseIntValue(String json, String key) {
		int keyIdx = json.indexOf("\"" + key + "\"");
		if (keyIdx == -1) return 0;
		int colonIdx = json.indexOf(':', keyIdx);
		if (colonIdx == -1) return 0;
		String rest = json.substring(colonIdx + 1).trim();
		StringBuilder digits = new StringBuilder();
		for (char c : rest.toCharArray()) {
			if (Character.isDigit(c)) digits.append(c);
			else if (digits.length() > 0) break;
		}
		return digits.length() > 0 ? Integer.parseInt(digits.toString()) : 0;
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
