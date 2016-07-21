package org.talend.components.snowflake.connection;

public class ColumnExtension {
 
	private boolean isNullable;
	private boolean isPrimaryKey;
	private boolean isUnique;
	private int precision;
	private int scale;
	private int length;
	private String defColValue;

	public ColumnExtension() {}
	
	/**
	 * @return the isNullable
	 */
	public boolean isNullable() {
		return isNullable;
	}
	/**
	 * @param isNullable the isNullable to set
	 */
	public void setNullable(boolean isNullable) {
		this.isNullable = isNullable;
	}
	/**
	 * @return the isPrimaryKey
	 */
	public boolean isPrimaryKey() {
		return isPrimaryKey;
	}
	/**
	 * @param isPrimaryKey the isPrimaryKey to set
	 */
	public void setPrimaryKey(boolean isPrimaryKey) {
		this.isPrimaryKey = isPrimaryKey;
	}
	/**
	 * @return the isUnique
	 */
	public boolean isUnique() {
		return isUnique;
	}
	/**
	 * @param isUnique the isUnique to set
	 */
	public void setUnique(boolean isUnique) {
		this.isUnique = isUnique;
	}
	/**
	 * @return the precision
	 */
	public int getPrecision() {
		return precision;
	}
	/**
	 * @param precision the precision to set
	 */
	public void setPrecision(int precision) {
		this.precision = precision;
	}
	/**
	 * @return the scale
	 */
	public int getScale() {
		return scale;
	}
	/**
	 * @param scale the scale to set
	 */
	public void setScale(int scale) {
		this.scale = scale;
	}
	/**
	 * @return the length
	 */
	public int getLength() {
		return length;
	}
	/**
	 * @param length the length to set
	 */
	public void setLength(int length) {
		this.length = length;
	}

	/**
	 * @return the defColValue
	 */
	public String getDefColValue() {
		return defColValue;
	}

	/**
	 * @param defColValue the defColValue to set
	 */
	public void setDefColValue(String defColValue) {
		this.defColValue = defColValue;
	}
 
	

}