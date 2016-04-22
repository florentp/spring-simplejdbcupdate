/*
 * Copyright 2011 the original author or authors.
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

import static junit.framework.Assert.*;

import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

public class SimpleJdbcUpdateTestCase {
	private static final String INSERT_SQL =
			"INSERT INTO dummy_table(key_1, key_2, a_string, an_int, a_bool)\n"
			+ "VALUES(?, ?, ?, ?, ?)";

	private static final String SELECT_SQL =
			"SELECT key_1, key_2, a_string, an_int, a_bool\n"
			+ "FROM dummy_table\n"
			+ "WHERE key_1 = ? AND key_2 = ?";

	private DataSource dataSource;
	private JdbcTemplate jdbcTemplate;

	@Before
	public void setUpDataSource() {
		dataSource = new DriverManagerDataSource("jdbc:hsqldb:mem:testdb");
		jdbcTemplate = new JdbcTemplate(dataSource);
		jdbcTemplate.update("CREATE TABLE IF NOT EXISTS dummy_table (key_1 VARCHAR(50), key_2 INT, a_string VARCHAR(50), an_int INTEGER, a_bool BIT) ");
	}

	@Test
	public void testSqlParameterSource() {
		String key1 = "Pwet";
		int key2 = 3;
		jdbcTemplate.update(INSERT_SQL, key1, key2, "Hi", 40, false);

		SimpleJdbcUpdate simpleJdbcUpdate = new SimpleJdbcUpdate(dataSource)
				.withTableName("dummy_table")
				.updatingColumns("a_string", "an_int", "a_bool")
				.restrictingColumns("key_1", "key_2");

		SqlParameterSource mapSqlParameterSource1 = new MapSqlParameterSource()
				.addValue("a_string", "Hello")
				.addValue("an_int", 42)
				.addValue("a_bool", true);

		SqlParameterSource mapSqlParameterSource2 = new MapSqlParameterSource()
				.addValue("key_1", key1)
				.addValue("key_2", key2);

		int affected = simpleJdbcUpdate.execute(mapSqlParameterSource1, mapSqlParameterSource2);
		assertEquals(1, affected);

		Map<String, Object> row = jdbcTemplate.queryForMap(SELECT_SQL, key1, key2);
		assertEquals(key1, row.get("key_1"));
		assertEquals(key2, row.get("key_2"));
		assertEquals("Hello", row.get("a_string"));
		assertEquals(42, row.get("an_int"));
		assertEquals(true, row.get("a_bool"));
	}

	@Test
	public void testMap() {
		String key1 = "Pwet";
		int key2 = 4;
		jdbcTemplate.update(INSERT_SQL, key1, key2, "Hi", 40, false);

		SimpleJdbcUpdate simpleJdbcUpdate = new SimpleJdbcUpdate(dataSource)
				.withTableName("dummy_table")
				.updatingColumns("a_string", "an_int", "a_bool")
				.restrictingColumns("key_1", "key_2");

		Map<String, Object> map1 = new HashMap<String, Object>();
		map1.put("a_string", "Hello");
		map1.put("an_int", 42);
		map1.put("a_bool", true);

		Map<String, Object> map2 = new HashMap<String, Object>();
		map2.put("key_1", key1);
		map2.put("key_2", key2);

		int affected = simpleJdbcUpdate.execute(map1, map2);
		assertEquals(1, affected);

		Map<String, Object> expectedRow = new HashMap<String, Object>(map1);
		expectedRow.putAll(map2);
		Map<String, Object> row = jdbcTemplate.queryForMap(SELECT_SQL, key1, key2);
		assertEquals(expectedRow, row);
	}

	@Test
	public void testWhereOperator() {
		String key1 = "Pwet";
		int key2 = 1;
		jdbcTemplate.update(INSERT_SQL, key1, key2, "Hi", 40, false);

		Map<String, Operator> where = new HashMap<String, Operator>();
		where.put("key_1", Operator.EQUALS);
		where.put("key_2", Operator.LESS_THAN);

		SimpleJdbcUpdate simpleJdbcUpdate = new SimpleJdbcUpdate(dataSource)
				.withTableName("dummy_table")
				.updatingColumns("a_string", "an_int", "a_bool")
				.restrictingColumns(where);

		Map<String, Object> map1 = new HashMap<String, Object>();
		map1.put("a_string", "Hello");
		map1.put("an_int", 42);
		map1.put("a_bool", true);

		Map<String, Object> map2 = new HashMap<String, Object>();
		map2.put("key_1", key1);
		map2.put("key_2", 2);

		int affected = simpleJdbcUpdate.execute(map1, map2);
		assertEquals(1, affected);

		Map<String, Object> expectedRow = new HashMap<String, Object>(map1);
		expectedRow.put("key_1", key1);
		expectedRow.put("key_2", key2);
		Map<String, Object> row = jdbcTemplate.queryForMap(SELECT_SQL, key1, key2);
		assertEquals(expectedRow, row);
	}
}
