package Business.Entities;

/**
 * Holds application configuration loaded from config.json.
 * All values are populated by ConfigService at startup.
 */
public class Config {
	private int dbPort;
	private String dbIP;
	private String dbName;
	private String dbUser;
	private String dbPassword;
	private String adminPassword;
	private int simulatedVehicleDelay;

	/**
	 * Constructs a new Config with default (empty) values.
	 */
	public Config() {}

	/**
	 * Gets the database port.
	 *
	 * @return the database port number
	 */
	public int getDbPort() { return dbPort; }

	/**
	 * Gets the database IP address.
	 *
	 * @return the database IP address
	 */
	public String getDbIP() { return dbIP; }

	/**
	 * Gets the database name.
	 *
	 * @return the database name
	 */
	public String getDbName() { return dbName; }

	/**
	 * Gets the database username.
	 *
	 * @return the database username
	 */
	public String getDbUser() { return dbUser; }

	/**
	 * Gets the database password.
	 *
	 * @return the database password
	 */
	public String getDbPassword() { return dbPassword; }

	/**
	 * Gets the admin account password.
	 *
	 * @return the admin password
	 */
	public String getAdminPassword() { return adminPassword; }

	/**
	 * Gets the maximum delay in seconds between simulated vehicle events.
	 *
	 * @return the simulated vehicle delay in seconds
	 */
	public int getSimulatedVehicleDelay() { return simulatedVehicleDelay; }

	/**
	 * Sets the database port.
	 *
	 * @param dbPort the new database port number
	 */
	public void setDbPort(int dbPort) { this.dbPort = dbPort; }

	/**
	 * Sets the database IP address.
	 *
	 * @param dbIP the new database IP address
	 */
	public void setDbIP(String dbIP) { this.dbIP = dbIP; }

	/**
	 * Sets the database name.
	 *
	 * @param dbName the new database name
	 */
	public void setDbName(String dbName) { this.dbName = dbName; }

	/**
	 * Sets the database username.
	 *
	 * @param dbUser the new database username
	 */
	public void setDbUser(String dbUser) { this.dbUser = dbUser; }

	/**
	 * Sets the database password.
	 *
	 * @param dbPassword the new database password
	 */
	public void setDbPassword(String dbPassword) { this.dbPassword = dbPassword; }

	/**
	 * Sets the admin account password.
	 *
	 * @param adminPassword the new admin password
	 */
	public void setAdminPassword(String adminPassword) { this.adminPassword = adminPassword; }

	/**
	 * Sets the maximum delay in seconds between simulated vehicle events.
	 *
	 * @param simulatedVehicleDelay the new delay in seconds
	 */
	public void setSimulatedVehicleDelay(int simulatedVehicleDelay) { this.simulatedVehicleDelay = simulatedVehicleDelay; }
}
