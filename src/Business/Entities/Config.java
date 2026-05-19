package Business.Entities;

/**
 * Holds application configuration loaded from config.json.
 */
public class Config {
	private int dbPort;
	private String dbIP;
	private String dbName;
	private String dbUser;
	private String dbPassword;
	private String adminPassword;
	private int simulatedVehicleDelay;

	public Config() {}

	/**
	 * @return database port
	 */
	public int getDbPort() { return dbPort; }

	/**
	 * @return database IP address
	 */
	public String getDbIP() { return dbIP; }

	/**
	 * @return database name
	 */
	public String getDbName() { return dbName; }

	/**
	 * @return database username
	 */
	public String getDbUser() { return dbUser; }

	/**
	 * @return database password
	 */
	public String getDbPassword() { return dbPassword; }

	/**
	 * @return admin account password
	 */
	public String getAdminPassword() { return adminPassword; }

	/**
	 * @return max delay in seconds between simulated vehicle events
	 */
	public int getSimulatedVehicleDelay() { return simulatedVehicleDelay; }

	public void setDbPort(int dbPort) { this.dbPort = dbPort; }
	public void setDbIP(String dbIP) { this.dbIP = dbIP; }
	public void setDbName(String dbName) { this.dbName = dbName; }
	public void setDbUser(String dbUser) { this.dbUser = dbUser; }
	public void setDbPassword(String dbPassword) { this.dbPassword = dbPassword; }
	public void setAdminPassword(String adminPassword) { this.adminPassword = adminPassword; }
	public void setSimulatedVehicleDelay(int simulatedVehicleDelay) { this.simulatedVehicleDelay = simulatedVehicleDelay; }
}
