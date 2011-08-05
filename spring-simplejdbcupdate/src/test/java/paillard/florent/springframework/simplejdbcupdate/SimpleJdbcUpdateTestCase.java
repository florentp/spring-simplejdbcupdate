package paillard.florent.springframework.simplejdbcupdate;

import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.junit.Test;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

public class SimpleJdbcUpdateTestCase {

	@Test
	public void testSqlParameterSource() {
		DataSource dataSource = new DriverManagerDataSource("jdbc:hsqldb:mem:testdb");
		SimpleJdbcTemplate createTableTemplate = new SimpleJdbcTemplate(dataSource);
		createTableTemplate.update("create table dummy_table (key_1 VARCHAR(50), key_2 INT, a_string VARCHAR(50), an_int INTEGER, a_bool BIT) ");

		SimpleJdbcUpdate simpleJdbcUpdate = new SimpleJdbcUpdate(dataSource)
				.withTableName("dummy_table")
				.updatingColumns("a_string", "an_int", "a_bool")
				.restrictingColumns("key_1", "key_2");

		SqlParameterSource mapSqlParameterSource1 = new MapSqlParameterSource()
				.addValue("a_string", "Hello")
				.addValue("an_int", 42)
				.addValue("a_bool", true);

		SqlParameterSource mapSqlParameterSource2 = new MapSqlParameterSource()
				.addValue("key_1", "Pwet")
				.addValue("key_2", 3);

		simpleJdbcUpdate.execute(mapSqlParameterSource1, mapSqlParameterSource2);
	}

	@Test
	public void testMap() {

		DataSource dataSource = new DriverManagerDataSource("jdbc:hsqldb:mem:testdb");

		SimpleJdbcUpdate simpleJdbcUpdate = new SimpleJdbcUpdate(dataSource)
				.withTableName("dummy_table")
				.updatingColumns("a_string", "an_int", "a_bool")
				.restrictingColumns("key_1", "key_2");

		Map<String, Object> map1 = new HashMap<String, Object>();
		map1.put("a_string", "Hello");
		map1.put("an_int", 42);
		map1.put("a_bool", true);

		Map<String, Object> map2 = new HashMap<String, Object>();
		map2.put("key_1", "Pwet");
		map2.put("key_2", 3);

		simpleJdbcUpdate.execute(map1, map2);
	}

}
