package paillard.florent.springframework.simplejdbcupdate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.jdbc.core.SqlTypeValue;
import org.springframework.jdbc.core.metadata.TableMetaDataProvider;
import org.springframework.jdbc.core.metadata.TableMetaDataProviderFactory;
import org.springframework.jdbc.core.metadata.TableParameterMetaData;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.core.namedparam.SqlParameterSourceUtils;
import org.springframework.jdbc.support.JdbcUtils;
import org.springframework.jdbc.support.nativejdbc.NativeJdbcExtractor;

class TableMetaDataContext {

	/** Logger available to subclasses */
	protected final Log logger = LogFactory.getLog(getClass());

	/** name of procedure to call **/
	private String tableName;

	/** name of catalog for call **/
	private String catalogName;

	/** name of schema for call **/
	private String schemaName;

	/** should we access insert parameter meta data info or not */
	private boolean accessTableColumnMetaData = true;

	/** should we override default for including synonyms for meta data lookups */
	private boolean overrideIncludeSynonymsDefault = false;

	/** the provider of table meta data */
	private TableMetaDataProvider metaDataProvider;

	/** NativeJdbcExtractor to be used to retrieve the native connection */
	private NativeJdbcExtractor nativeJdbcExtractor;

	/**
	 * Set the name of the table for this context.
	 */
	public void setTableName(String tableName) {
		this.tableName = tableName;
	}

	/**
	 * Get the name of the table for this context.
	 */
	public String getTableName() {
		return this.tableName;
	}

	/**
	 * Set the name of the catalog for this context.
	 */
	public void setCatalogName(String catalogName) {
		this.catalogName = catalogName;
	}

	/**
	 * Get the name of the catalog for this context.
	 */
	public String getCatalogName() {
		return this.catalogName;
	}

	/**
	 * Set the name of the schema for this context.
	 */
	public void setSchemaName(String schemaName) {
		this.schemaName = schemaName;
	}

	/**
	 * Get the name of the schema for this context.
	 */
	public String getSchemaName() {
		return this.schemaName;
	}

	/**
	 * Specify whether we should access table column meta data.
	 */
	public void setAccessTableColumnMetaData(boolean accessTableColumnMetaData) {
		this.accessTableColumnMetaData = accessTableColumnMetaData;
	}

	/**
	 * Are we accessing table meta data?
	 */
	public boolean isAccessTableColumnMetaData() {
		return this.accessTableColumnMetaData;
	}

	/**
	 * Specify whether we should override default for accessing synonyms.
	 */
	public void setOverrideIncludeSynonymsDefault(boolean override) {
		this.overrideIncludeSynonymsDefault = override;
	}

	/**
	 * Are we overriding include synonyms default?
	 */
	public boolean isOverrideIncludeSynonymsDefault() {
		return this.overrideIncludeSynonymsDefault;
	}

	/**
	 * Does this database support the JDBC 3.0 feature of retrieving generated
	 * keys {@link java.sql.DatabaseMetaData#supportsGetGeneratedKeys()}?
	 */
	public boolean isGetGeneratedKeysSupported() {
		return this.metaDataProvider.isGetGeneratedKeysSupported();
	}

	/**
	 * Does this database support simple query to retrieve generated keys when
	 * the JDBC 3.0 feature is not supported.
	 * {@link java.sql.DatabaseMetaData#supportsGetGeneratedKeys()}?
	 */
	public boolean isGetGeneratedKeysSimulated() {
		return this.metaDataProvider.isGetGeneratedKeysSimulated();
	}

	/**
	 * Does this database support simple query to retrieve generated keys when
	 * the JDBC 3.0 feature is not supported.
	 * {@link java.sql.DatabaseMetaData#supportsGetGeneratedKeys()}?
	 */
	public String getSimulationQueryForGetGeneratedKey(String tableName, String keyColumnName) {
		return this.metaDataProvider.getSimpleQueryForGetGeneratedKey(tableName, keyColumnName);
	}

	/**
	 * Is a column name String array for retrieving generated keys supported?
	 * {@link java.sql.Connection#createStruct(String, Object[])}?
	 */
	public boolean isGeneratedKeysColumnNameArraySupported() {
		return this.metaDataProvider.isGeneratedKeysColumnNameArraySupported();
	}

	/**
	 * Set {@link NativeJdbcExtractor} to be used to retrieve the native
	 * connection.
	 */
	public void setNativeJdbcExtractor(NativeJdbcExtractor nativeJdbcExtractor) {
		this.nativeJdbcExtractor = nativeJdbcExtractor;
	}

	/**
	 * Process the current meta data with the provided configuration options.
	 * 
	 * @param dataSource
	 *            the DataSource being used
	 * @param declaredColumns
	 *            any columns that are declared
	 * @param generatedKeyNames
	 *            name of generated keys
	 */
	public void processMetaData(DataSource dataSource) {
		// TODO: comment to explain this HORRIBLE thing!
		org.springframework.jdbc.core.metadata.TableMetaDataContext underlyingTableMetaDataContext = new org.springframework.jdbc.core.metadata.TableMetaDataContext();
		underlyingTableMetaDataContext.setAccessTableColumnMetaData(accessTableColumnMetaData);
		underlyingTableMetaDataContext.setCatalogName(catalogName);
		underlyingTableMetaDataContext.setNativeJdbcExtractor(nativeJdbcExtractor);
		underlyingTableMetaDataContext.setOverrideIncludeSynonymsDefault(overrideIncludeSynonymsDefault);
		underlyingTableMetaDataContext.setSchemaName(schemaName);
		underlyingTableMetaDataContext.setTableName(tableName);

		this.metaDataProvider = TableMetaDataProviderFactory.createMetaDataProvider(dataSource, underlyingTableMetaDataContext, nativeJdbcExtractor);
	}

	/**
	 * Compare columns created from metadata with declared columns and return a
	 * reconciled list.
	 * 
	 * @param declaredColumns
	 *            declared column names
	 * @param generatedKeyNames
	 *            names of generated key columns
	 */
	protected List<String> reconcileColumnsToUse(List<String> declaredColumns, String[] generatedKeyNames) {
		if (declaredColumns.size() > 0) {
			return new ArrayList<String>(declaredColumns);
		}
		Set<String> keys = new HashSet<String>(generatedKeyNames.length);
		for (String key : generatedKeyNames) {
			keys.add(key.toUpperCase());
		}
		List<String> columns = new ArrayList<String>();
		for (TableParameterMetaData meta : metaDataProvider.getTableParameterMetaData()) {
			if (!keys.contains(meta.getParameterName().toUpperCase())) {
				columns.add(meta.getParameterName());
			}
		}
		return columns;
	}

	/**
	 * Match the provided column names and values with the list of columns used.
	 * 
	 * @param sqlParameterSource
	 *            the parameter names and values
	 * @param reconciledUpdatingColumns
	 */
	public List<Object> sortAndTypeInParameter(SqlParameterSource sqlParameterSource, List<String> reconciledUpdatingColumns) {
		List<Object> values = new ArrayList<Object>();
		// for parameter source lookups we need to provide caseinsensitive
		// lookup support since the
		// database metadata is not necessarily providing case sensitive column
		// names
		Map<?, ?> caseInsensitiveParameterNames = SqlParameterSourceUtils.extractCaseInsensitiveParameterNames(sqlParameterSource);
		for (String column : reconciledUpdatingColumns) {
			if (sqlParameterSource.hasValue(column)) {
				values.add(SqlParameterSourceUtils.getTypedValue(sqlParameterSource, column));
			}
			else {
				String lowerCaseName = column.toLowerCase();
				if (sqlParameterSource.hasValue(lowerCaseName)) {
					values.add(SqlParameterSourceUtils.getTypedValue(sqlParameterSource, lowerCaseName));
				}
				else {
					String propertyName = JdbcUtils.convertUnderscoreNameToPropertyName(column);
					if (sqlParameterSource.hasValue(propertyName)) {
						values.add(SqlParameterSourceUtils.getTypedValue(sqlParameterSource, propertyName));
					}
					else {
						if (caseInsensitiveParameterNames.containsKey(lowerCaseName)) {
							values.add(SqlParameterSourceUtils.getTypedValue(sqlParameterSource, (String) caseInsensitiveParameterNames.get(lowerCaseName)));
						}
						else {
							values.add(null);
						}
					}
				}
			}
		}
		return values;
	}

	/**
	 * Match the provided column names and values with the list of columns used.
	 * 
	 * @param inParameters
	 *            the parameter names and values
	 */
	public List<Object> sortAndTypeInParameter(Map<String, Object> inParameters, List<String> reconciledUpdatingColumns) {
		List<Object> values = new ArrayList<Object>();
		Map<String, Object> source = new HashMap<String, Object>();
		for (String key : inParameters.keySet()) {
			source.put(key.toLowerCase(), inParameters.get(key));
		}
		for (String column : reconciledUpdatingColumns) {
			values.add(source.get(column.toLowerCase()));
		}
		return values;
	}

	public List<String> createColumns() {
		List<TableParameterMetaData> tableParameterMetaDataList = this.metaDataProvider.getTableParameterMetaData();
		List<String> columnList = new ArrayList<String>(tableParameterMetaDataList.size());
		for (TableParameterMetaData tableParameterMetaData : tableParameterMetaDataList) {
			columnList.add(tableParameterMetaData.getParameterName());
		}
		return columnList;
	}

	/**
	 * Build the array of {@link java.sql.Types} based on configuration and
	 * metadata information
	 * 
	 * @return the array of types to be used
	 */
	public int[] createColumnTypes(List<String> columns) {
		int[] types = new int[columns.size()];
		List<TableParameterMetaData> parameters = this.metaDataProvider.getTableParameterMetaData();
		Map<String, TableParameterMetaData> parameterMap = new HashMap<String, TableParameterMetaData>(parameters.size());
		for (TableParameterMetaData tpmd : parameters) {
			parameterMap.put(tpmd.getParameterName().toUpperCase(), tpmd);
		}
		int typeIndx = 0;
		for (String column : columns) {
			if (column == null) {
				types[typeIndx] = SqlTypeValue.TYPE_UNKNOWN;
			}
			else {
				TableParameterMetaData tpmd = parameterMap.get(column.toUpperCase());
				if (tpmd != null) {
					types[typeIndx] = tpmd.getSqlType();
				}
				else {
					types[typeIndx] = SqlTypeValue.TYPE_UNKNOWN;
				}
			}
			typeIndx++;
		}
		return types;
	}

}
