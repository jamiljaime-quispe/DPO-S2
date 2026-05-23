package Persistence;

import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Manages the JDBC connection to the MySQL database.
 * Provides a single lazily-initialised connection reused across all DAO calls.
 */
public class DatabaseManager {
	static {
		try {
			Class.forName("com.mysql.cj.jdbc.Driver");
		} catch (ClassNotFoundException e) {
			throw new ExceptionInInitializerError(
					new ClassNotFoundException(
							"MySQL JDBC driver not on classpath. Add mysql-connector-j under lib/ (see pom.xml).",
							e));
		}
	}

	private final String url;
	private final String user;
	private final String password;
	private java.sql.Connection connection;

	/**
	 * Constructs a new DatabaseManager and builds the JDBC connection URL.
	 *
	 * @param ip       the database host IP or hostname
	 * @param port     the database port
	 * @param dbName   the schema/database name
	 * @param user     the database username
	 * @param password the database password
	 */
	public DatabaseManager(String ip, int port, String dbName, String user, String password) {
		this.url = "jdbc:mysql://" + ip + ":" + port + "/" + dbName + "?useSSL=false&allowPublicKeyRetrieval=true";
		this.user = user;
		this.password = password;
	}

	/**
	 * Returns the active database connection, opening a new one if needed.
	 *
	 * @return a live JDBC Connection
	 * @throws RuntimeException if the connection cannot be established
	 */
	public java.sql.Connection getConnection() {
		try {
			if (connection == null || connection.isClosed()) {
				connection = DriverManager.getConnection(url, user, password);
			}
		} catch (SQLException e) {
			throw new RuntimeException("Failed to connect to database: " + e.getMessage(), e);
		}
		return connection;
	}

	/** Closes the database connection if it is open. */
	public void disconnect() {
		try {
			if (connection != null && !connection.isClosed()) {
				connection.close();
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	/** Starts a database transaction by disabling auto-commit. */
	public void beginTransaction() {
		try {
			getConnection().setAutoCommit(false);
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	/** Commits the current transaction and re-enables auto-commit. */
	public void commit() {
		try {
			getConnection().commit();
			getConnection().setAutoCommit(true);
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	/** Rolls back the current transaction and re-enables auto-commit. */
	public void rollback() {
		try {
			getConnection().rollback();
			getConnection().setAutoCommit(true);
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}
}
