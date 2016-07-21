package org.talend.components.snowflake.connection;

import java.util.List;

public class Column {
 
	public Column(){}
	
	private String name;
	private String dType;
 
	private List<ColumnExtension> extensions;

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @return the dType
	 */
	public String getdType() {
		return dType;
	}

	/**
	 * @param dType the dType to set
	 */
	public void setdType(String dType) {
		this.dType = dType;
	}

	/**
	 * @return the extensions
	 */
	public List<ColumnExtension> getExtensions() {
		return extensions;
	}

	/**
	 * @param extensions the extensions to set
	 */
	public void setExtensions(List<ColumnExtension> extensions) {
		this.extensions = extensions;
	}
 
	
	

}