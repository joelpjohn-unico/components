package org.talend.components.snowflake.connection;

import java.sql.Connection;

/**
 * This is a wrapper for the Snowflake JDBC Connection
 * @author user
 *
 */
public class SnowflakeNativeConnection {
	private Connection connection;

	public SnowflakeNativeConnection() {
	}

	public SnowflakeNativeConnection(Connection conn) {
		this.connection = conn;
	}
	
	public Connection getConnection() {
		return this.connection;
	}
	
	public void setConnection(Connection conn) {
		this.connection = conn;
	}
}
