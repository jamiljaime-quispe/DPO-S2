package Persistence;

import java.sql.DriverManager;
import java.sql.SQLException;

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

	public DatabaseManager(String ip, int port, String dbName, String user, String password) {
		this.url = "jdbc:mysql://" + ip + ":" + port + "/" + dbName + "?useSSL=false&allowPublicKeyRetrieval=true";
		this.user = user;
		this.password = password;
	}

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

	public void disconnect() {
		try {
			if (connection != null && !connection.isClosed()) {
				connection.close();
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public void beginTransaction() {
		try {
			getConnection().setAutoCommit(false);
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	public void commit() {
		try {
			getConnection().commit();
			getConnection().setAutoCommit(true);
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	public void rollback() {
		try {
			getConnection().rollback();
			getConnection().setAutoCommit(true);
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}
}
