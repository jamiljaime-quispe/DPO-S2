package Persistence.IMPL;

import Business.Entities.Config;
import Persistence.ConfigDAO;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

/**
 * File-based implementation that reads configuration values from config.json.
 */
public class ConfigDAOImpl implements ConfigDAO {
	private static final String CONFIG_FILE_NAME = "config.json";

	/** Loads config.json from the project root. */
	@Override
	public Config loadConfig() {
		File configFile = new File(CONFIG_FILE_NAME);
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
			Config config = new Config();
			config.setDbPort(parseIntValue(json, "dbPort"));
			config.setDbIP(parseStringValue(json, "dbIP"));
			config.setDbName(parseStringValue(json, "dbName"));
			config.setDbUser(parseStringValue(json, "dbUser"));
			config.setDbPassword(parseStringValue(json, "dbPassword"));
			config.setAdminPassword(parseStringValue(json, "adminPassword"));
			config.setSimulatedVehicleDelay(parseIntValue(json, "simulatedVehicleDelay"));
			return config;
		} catch (IOException e) {
			throw new IllegalStateException("Could not read config.json: " + e.getMessage(), e);
		}
	}

	/** Reads a text value from the config file content. */
	private String parseStringValue(String json, String key) {
		int keyIdx = json.indexOf("\"" + key + "\"");
		if (keyIdx == -1) return null;
		int colonIdx = json.indexOf(':', keyIdx);
		if (colonIdx == -1) return null;

		String rest = json.substring(colonIdx + 1).trim();
		if (!rest.startsWith("\"")) return null;

		int start = 1;
		int end = rest.indexOf('"', start);
		if (end == -1) return null;

		return rest.substring(start, end);
	}

	/** Reads a number value from the config file content. */
	private int parseIntValue(String json, String key) {
		int keyIdx = json.indexOf("\"" + key + "\"");
		if (keyIdx == -1) return 0;
		int colonIdx = json.indexOf(':', keyIdx);
		if (colonIdx == -1) return 0;

		String rest = json.substring(colonIdx + 1).trim();
		StringBuilder digits = new StringBuilder();
		for (int i = 0; i < rest.length(); i++) {
			char c = rest.charAt(i);
			if (Character.isDigit(c)) {
				digits.append(c);
			} else if (digits.length() > 0) {
				break;
			}
		}

		if (digits.length() == 0) return 0;
		return Integer.parseInt(digits.toString());
	}
}
