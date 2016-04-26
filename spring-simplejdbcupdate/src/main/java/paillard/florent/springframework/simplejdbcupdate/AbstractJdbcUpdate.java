/*
 * Copyright 2002-2011 the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package paillard.florent.springframework.simplejdbcupdate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.support.nativejdbc.NativeJdbcExtractor;
import org.springframework.util.Assert;

/**
 * Abstract class to provide base functionality for easy updates based on
 * configuration options and database metadata. This class provides the base SPI
 * for {@link SimpleJdbcUpdate}.
 * 
 * @author Thomas Risberg
 * @author Juergen Hoeller
 * @author Florent Paillard
 * @since 3.0.5
 */
public abstract class AbstractJdbcUpdate {

	/** Logger available to subclasses */
	protected final Log logger = LogFactory.getLog(getClass());

	/** Lower-level class used to execute SQL */
	private final JdbcTemplate jdbcTemplate;

	/** Context used to retrieve and manage database metadata */
	private final TableMetaDataContext tableMetaDataContext = new TableMetaDataContext();

	/** List of columns declared by user to be used in 'set' clause */
	private final List<String> declaredUpdatingColumns = new ArrayList<String>();

	/** List of columns effectively used in 'set' clause */
	private final List<String> reconciledUpdatingColumns = new ArrayList<String>();

	/** The names of the columns to be used in 'where' clause */
	private final Map<String, Operator> restrictingColumns = new HashMap<String, Operator>();

	/**
	 * Has this operation been compiled? Compilation means at least checking
	 * that a DataSource or JdbcTemplate has been provided, but subclasses may
	 * also implement their own custom validation.
	 */
	private boolean compiled = false;

	/** The generated string used for update statement */
	private String updateString;

	/** The SQL type information for the declared columns */
	private int[] columnTypes;

	/**
	 * Constructor for sublasses to delegate to for setting the DataSource.
	 */
	protected AbstractJdbcUpdate(DataSource dataSource) {
		this.jdbcTemplate = new JdbcTemplate(dataSource);
	}

	/**
	 * Constructor for sublasses to delegate to for setting the JdbcTemplate.
	 */
	protected AbstractJdbcUpdate(JdbcTemplate jdbcTemplate) {
		Assert.notNull(jdbcTemplate, "JdbcTemplate must not be null");
		this.jdbcTemplate = jdbcTemplate;
		setNativeJdbcExtractor(jdbcTemplate.getNativeJdbcExtractor());
	}

	// -------------------------------------------------------------------------
	// Methods dealing with configuaration properties
	// -------------------------------------------------------------------------

	/**
	 * Get the name of the table for this update
	 */
	public String getTableName() {
		return this.tableMetaDataContext.getTableName();
	}

	/**
	 * Set the name of the table for this update
	 */
	public void setTableName(String tableName) {
		checkIfConfigurationModificationIsAllowed();
		this.tableMetaDataContext.setTableName(tableName);
	}

	/**
	 * Get the name of the schema for this update
	 */
	public String getSchemaName() {
		return this.tableMetaDataContext.getSchemaName();
	}

	/**
	 * Set the name of the schema for this update
	 */
	public void setSchemaName(String schemaName) {
		checkIfConfigurationModificationIsAllowed();
		this.tableMetaDataContext.setSchemaName(schemaName);
	}

	/**
	 * Get the name of the catalog for this update
	 */
	public String getCatalogName() {
		return this.tableMetaDataContext.getCatalogName();
	}

	/**
	 * Set the name of the catalog for this update
	 */
	public void setCatalogName(String catalogName) {
		checkIfConfigurationModificationIsAllowed();
		this.tableMetaDataContext.setCatalogName(catalogName);
	}

	/**
	 * Get the names of the columns used
	 */
	public List<String> getDeclaredUpdatingColumns() {
		return Collections.unmodifiableList(this.declaredUpdatingColumns);
	}

	/**
	 * Set the names of the columns to be used
	 */
	public void setDeclaredUpdatingColumns(List<String> declaredNames) {
		checkIfConfigurationModificationIsAllowed();
		this.declaredUpdatingColumns.clear();
		this.declaredUpdatingColumns.addAll(declaredNames);
	}

	/**
	 * Get the names of 'where' columns
	 */
	public Set<String> getRestrictingColumns() {
		return Collections.unmodifiableSet(this.restrictingColumns.keySet());
	}

	/**
	 * Set the names of any primary keys
	 */
	public void setRestrictingColumns(List<String> whereNames) {
		Map<String, Operator> columns = new HashMap<String, Operator>();
		for (String columnName : whereNames) {
		    columns.put(columnName, Operator.EQUALS);
		}
		setRestrictingColumns(columns);
	}
	
	   /**
     * Set the names of any where columns
     */
    public void setRestrictingColumns(Map<String, Operator> whereNames) {
        checkIfConfigurationModificationIsAllowed();
        this.restrictingColumns.clear();
        this.restrictingColumns.putAll(whereNames);
    }

	/**
	 * Specify whether the parameter metadata for the call should be used. The
	 * default is true.
	 */
	public void setAccessTableColumnMetaData(boolean accessTableColumnMetaData) {
		this.tableMetaDataContext.setAccessTableColumnMetaData(accessTableColumnMetaData);
	}

	/**
	 * Specify whether the default for including synonyms should be changed. The
	 * default is false.
	 */
	public void setOverrideIncludeSynonymsDefault(boolean override) {
		this.tableMetaDataContext.setOverrideIncludeSynonymsDefault(override);
	}

	/**
	 * Set the {@link NativeJdbcExtractor} to use to retrieve the native
	 * connection if necessary
	 */
	public void setNativeJdbcExtractor(NativeJdbcExtractor nativeJdbcExtractor) {
		this.tableMetaDataContext.setNativeJdbcExtractor(nativeJdbcExtractor);
	}

	/**
	 * Get the update string to be used
	 */
	protected String getUpdateString() {
		return this.updateString;
	}

	/**
	 * Get the array of {@link java.sql.Types} to be used in 'set' clause
	 */
	protected int[] getColumnTypes() {
		return this.columnTypes;
	}

	/**
	 * Get the {@link JdbcTemplate} that is configured to be used
	 */
	protected JdbcTemplate getJdbcTemplate() {
		return this.jdbcTemplate;
	}

	// -------------------------------------------------------------------------
	// Methods handling compilation issues
	// -------------------------------------------------------------------------

	/**
	 * Compile this JdbcUpdate using provided parameters and meta data plus
	 * other settings. This finalizes the configuration for this object and
	 * subsequent attempts to compile are ignored. This will be implicitly
	 * called the first time an un-compiled update is executed.
	 * 
	 * @throws org.springframework.dao.InvalidDataAccessApiUsageException
	 *             if the object hasn't been correctly initialized, for example
	 *             if no DataSource has been provided
	 */
	public synchronized final void compile() throws InvalidDataAccessApiUsageException {
		if (!isCompiled()) {
			if (getTableName() == null) {
				throw new InvalidDataAccessApiUsageException("Table name is required");
			}

			try {
				this.jdbcTemplate.afterPropertiesSet();
			}
			catch (IllegalArgumentException ex) {
				throw new InvalidDataAccessApiUsageException(ex.getMessage());
			}

			compileInternal();
			this.compiled = true;

			if (logger.isDebugEnabled()) {
				logger.debug("JdbcUpdate for table [" + getTableName() + "] compiled");
			}
		}
	}

	/**
	 * Method to perform the actual compilation. Subclasses can override this
	 * template method to perform their own compilation. Invoked after this base
	 * class's compilation is complete.
	 */
	protected void compileInternal() {

		tableMetaDataContext.processMetaData(getJdbcTemplate().getDataSource());
		reconcileUpdatingColumns();

		updateString = createUpdateString();

		List<String> columns = new ArrayList<String>();
		columns.addAll(reconciledUpdatingColumns);
		columns.addAll(restrictingColumns.keySet());

		columnTypes = tableMetaDataContext.createColumnTypes(columns);

		if (logger.isDebugEnabled()) {
			logger.debug("Compiled JdbcUpdate. Update string is [" + getUpdateString() + "]");
		}

		onCompileInternal();
	}

	/**
	 * Hook method that subclasses may override to react to compilation. This
	 * implementation does nothing.
	 */
	protected void onCompileInternal() {
	}

	/**
	 * Is this operation "compiled"?
	 * 
	 * @return whether this operation is compiled, and ready to use.
	 */
	public boolean isCompiled() {
		return this.compiled;
	}

	/**
	 * Check whether this operation has been compiled already; lazily compile it
	 * if not already compiled.
	 * <p>
	 * Automatically called by <code>validateParameters</code>.
	 */
	protected void checkCompiled() {
		if (!isCompiled()) {
			logger.debug("JdbcUpdate not compiled before execution - invoking compile");
			compile();
		}
	}

	/**
	 * Method to check whether we are allowd to make any configuration changes
	 * at this time. If the class has been compiled, then no further changes to
	 * the configuration are allowed.
	 */
	protected void checkIfConfigurationModificationIsAllowed() {
		if (isCompiled()) {
			throw new InvalidDataAccessApiUsageException("Configuration can't be altered once the class has been compiled or used.");
		}
	}

	// -------------------------------------------------------------------------
	// Methods handling execution
	// -------------------------------------------------------------------------

	/**
	 * Method that provides execution of the update using the passed in
	 * {@link SqlParameterSource}
	 * 
	 * @param parameterSource
	 *            parameter names and values to be used in update
	 * @param pkValues
	 *            List containing PK column values
	 * @return number of rows affected
	 */
	protected int doExecute(Map<String, Object> updatingValues, Map<String, Object> restrictingValues) {
		checkCompiled();
		List<Object> values = new ArrayList<Object>();
		values.addAll(matchInParameterValuesWithUpdateColumns(updatingValues, reconciledUpdatingColumns));
		values.addAll(matchInParameterValuesWithUpdateColumns(restrictingValues, new ArrayList<String>(restrictingColumns.keySet())));
		return executeUpdateInternal(values);
	}

	/**
	 * Method that provides execution of the update using the passed in Map of
	 * parameters
	 * 
	 * @param args
	 *            Map with parameter names and values to be used in update
	 * @param pkValues
	 *            List containing PK column values
	 * @return number of rows affected
	 */
	protected int doExecute(SqlParameterSource updatingValues, SqlParameterSource restrictingValues) {
		checkCompiled();
		List<Object> values = new ArrayList<Object>();
		values.addAll(matchInParameterValuesWithUpdateColumns(updatingValues, reconciledUpdatingColumns));
		values.addAll(matchInParameterValuesWithUpdateColumns(restrictingValues, new ArrayList<String>(restrictingColumns.keySet())));
		return executeUpdateInternal(values);
	}

	/**
	 * Method to execute the update
	 */
	private int executeUpdateInternal(List<Object> values) {
		if (logger.isDebugEnabled()) {
			logger.debug("The following parameters are used for update " + getUpdateString() + " with: " + values);
		}
		int updateCount = jdbcTemplate.update(updateString, values.toArray(), columnTypes);
		return updateCount;
	}

	/**
	 * Match the provided in parameter values with regitered parameters and
	 * parameters defined via metedata processing.
	 * 
	 * @param args
	 *            the parameter values provided in a Map
	 * @return Map with parameter names and values
	 */
	protected List<Object> matchInParameterValuesWithUpdateColumns(Map<String, Object> args, List<String> columns) {
		return tableMetaDataContext.sortAndTypeInParameter(args, columns);
	}

	/**
	 * Match the provided in parameter values with regitered parameters and
	 * parameters defined via metedata processing.
	 * 
	 * @param parameterSource
	 *            the parameter vakues provided as a {@link SqlParameterSource}
	 * @return Map with parameter names and values
	 */
	protected List<Object> matchInParameterValuesWithUpdateColumns(SqlParameterSource parameterSource, List<String> columns) {
		return tableMetaDataContext.sortAndTypeInParameter(parameterSource, columns);
	}

	/**
	 * Build the update string based on configuration and metadata information
	 * 
	 * @return the update string to be used
	 */
	protected String createUpdateString() {
		StringBuilder updateStatement = new StringBuilder();
		updateStatement.append("UPDATE ");
		if (this.getSchemaName() != null) {
			updateStatement.append(this.getSchemaName());
			updateStatement.append(".");
		}
		updateStatement.append(this.getTableName());
		updateStatement.append(" SET ");
		int columnCount = 0;
		for (String columnName : reconciledUpdatingColumns) {
			columnCount++;
			if (columnCount > 1) {
				updateStatement.append(", ");
			}
			updateStatement.append(columnName);
			updateStatement.append(" = ? ");
		}
		if (restrictingColumns.size() > 0) {
			updateStatement.append(" WHERE ");
			columnCount = 0;
			for (Map.Entry<String, Operator> column : restrictingColumns.entrySet()) {
				columnCount++;
				if (columnCount > 1) {
					updateStatement.append(" AND ");
				}
				updateStatement.append(column.getKey());
				updateStatement.append(" ");
				updateStatement.append(column.getValue().toString());
				updateStatement.append(" ? ");
			}
		}
		return updateStatement.toString();
	}

	private void reconcileUpdatingColumns() {
		if (declaredUpdatingColumns.size() > 0) {
			reconciledUpdatingColumns.clear();
			reconciledUpdatingColumns.addAll(declaredUpdatingColumns);
		}
		else {
			reconciledUpdatingColumns.clear();
			reconciledUpdatingColumns.addAll(tableMetaDataContext.createColumns());
		}
	}

}
