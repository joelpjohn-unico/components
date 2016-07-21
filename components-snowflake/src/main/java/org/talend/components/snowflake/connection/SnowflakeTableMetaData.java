package org.talend.components.snowflake.connection;

import java.util.List;

public class SnowflakeTableMetaData {
	
	private String tableName;
	private List<Column> column;

	public SnowflakeTableMetaData() {}

	/**
	 * @return the tableName
	 */
	public String getTableName() {
		return tableName;
	}

	/**
	 * @param tableName the tableName to set
	 */
	public void setTableName(String tableName) {
		this.tableName = tableName;
	}

	/**
	 * @return the column
	 */
	public List<Column> getColumn() {
		return column;
	}

	/**
	 * @param column the column to set
	 */
	public void setColumn(List<Column> column) {
		this.column = column;
	}
	
	
}