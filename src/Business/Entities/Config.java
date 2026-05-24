package Business.Entities;

/**
 * Holds application configuration loaded from config.json. All values are populated by ConfigService at
 * startup.
 * <p>
 * The class stores project data in a clear object so the services, controllers, and persistence code can
 * pass the same information around safely.
 * </p>
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
	 * <p>
	 * The constructor receives the objects or values this class needs and stores them before the rest of the
	 * methods are used.
	 * </p>
	 */
	public Config() {}

	/**
	 * Gets the database port.
	 * <p>
	 * The getter keeps the field private while still giving the rest of the project a clear way to read it.
	 * </p>
	 *
	 * @return the database port number
	 */
	public int getDbPort() { return dbPort; }

	/**
	 * Gets the database IP address.
	 * <p>
	 * The getter keeps the field private while still giving the rest of the project a clear way to read it.
	 * </p>
	 *
	 * @return the database IP address
	 */
	public String getDbIP() { return dbIP; }

	/**
	 * Gets the database name.
	 * <p>
	 * The getter keeps the field private while still giving the rest of the project a clear way to read it.
	 * </p>
	 *
	 * @return the database name
	 */
	public String getDbName() { return dbName; }

	/**
	 * Gets the database username.
	 * <p>
	 * The getter keeps the field private while still giving the rest of the project a clear way to read it.
	 * </p>
	 *
	 * @return the database username
	 */
	public String getDbUser() { return dbUser; }

	/**
	 * Gets the database password.
	 * <p>
	 * The getter keeps the field private while still giving the rest of the project a clear way to read it.
	 * </p>
	 *
	 * @return the database password
	 */
	public String getDbPassword() { return dbPassword; }

	/**
	 * Gets the admin account password.
	 * <p>
	 * The getter keeps the field private while still giving the rest of the project a clear way to read it.
	 * </p>
	 *
	 * @return the admin password
	 */
	public String getAdminPassword() { return adminPassword; }

	/**
	 * Gets the maximum delay in seconds between simulated vehicle events.
	 * <p>
	 * The getter keeps the field private while still giving the rest of the project a clear way to read it.
	 * </p>
	 *
	 * @return the simulated vehicle delay in seconds
	 */
	public int getSimulatedVehicleDelay() { return simulatedVehicleDelay; }

	/**
	 * Sets the database port.
	 * <p>
	 * The setter keeps the field change inside this object instead of letting other classes touch the field
	 * directly.
	 * </p>
	 *
	 * @param dbPort the new database port number
	 */
	public void setDbPort(int dbPort) { this.dbPort = dbPort; }

	/**
	 * Sets the database IP address.
	 * <p>
	 * The setter keeps the field change inside this object instead of letting other classes touch the field
	 * directly.
	 * </p>
	 *
	 * @param dbIP the new database IP address
	 */
	public void setDbIP(String dbIP) { this.dbIP = dbIP; }

	/**
	 * Sets the database name.
	 * <p>
	 * The setter keeps the field change inside this object instead of letting other classes touch the field
	 * directly.
	 * </p>
	 *
	 * @param dbName the new database name
	 */
	public void setDbName(String dbName) { this.dbName = dbName; }

	/**
	 * Sets the database username.
	 * <p>
	 * The setter keeps the field change inside this object instead of letting other classes touch the field
	 * directly.
	 * </p>
	 *
	 * @param dbUser the new database username
	 */
	public void setDbUser(String dbUser) { this.dbUser = dbUser; }

	/**
	 * Sets the database password.
	 * <p>
	 * The setter keeps the field change inside this object instead of letting other classes touch the field
	 * directly.
	 * </p>
	 *
	 * @param dbPassword the new database password
	 */
	public void setDbPassword(String dbPassword) { this.dbPassword = dbPassword; }

	/**
	 * Sets the admin account password.
	 * <p>
	 * The setter keeps the field change inside this object instead of letting other classes touch the field
	 * directly.
	 * </p>
	 *
	 * @param adminPassword the new admin password
	 */
	public void setAdminPassword(String adminPassword) { this.adminPassword = adminPassword; }

	/**
	 * Sets the maximum delay in seconds between simulated vehicle events.
	 * <p>
	 * The setter keeps the field change inside this object instead of letting other classes touch the field
	 * directly.
	 * </p>
	 *
	 * @param simulatedVehicleDelay the new delay in seconds
	 */
	public void setSimulatedVehicleDelay(int simulatedVehicleDelay) { this.simulatedVehicleDelay = simulatedVehicleDelay; }
}
