About
=====

Spring Framework provides two SimpleJdbc classes (`SimpleJdbcInsert` and `SimpleJdbcCall`) simplifying the insert operations and stored procedure calls through JDBC where no ORM framework is used. Spring issue [SPR-4691](https://jira.spring.io/browse/SPR-4691) points out the need of a SimpleJdbcUpdate class simplifying the update operations too.

This project is an implementation of the _missing_ `SimpleJdbcUpdate` class.

Requirements
============

Java 1.5+ and Spring 3.0.5+ are required.

If you're using a prior Spring version, feel free to contact me.

Download and install
====================

Download the latest version from the `Downloads` button on the [project home page](https://github.com/florentp/spring-simplejdbcupdate)

You can also clone this repository and compile the binary yourself (a `mvn package` command should be enough).

Then, you simply need to add the jar file to your classpath.

Usages
======

Main configuration and simple usage samples
-------------------------------------------

Let say, we have a table `my_table` with 4 fields `field_1`, `field_2`, `field_3` and `field_4`. The primary key of the table is composed of the first to columns.

Like `SimpleJdbcInsert`, the `SimpleJdbcUpdate` is often a private class member. It's initialized through a `DataSource` or `JdbcTemplate` injection and used within other the methods of the class.

    public class MyDao {
        private SimpleJdbcUpdate simpleJdbcUpdate;
    
        public void setDataSource(DataSource dataSource) {
            simpleJdbcUpdate = new SimpleJdbcUpdate(dataSource);
        }
    
        public void updateMyObject(String arg1, int arg2, String arg3, boolean arg4) {
            // call to simpleJdbcUpdate.execute(...);
        }
    }

With no configuration (like on the previous code snippet), the `SimpleJdbcUpdate` can't work. You must, at least, supplied the table name to update using the `withTableName`.

We should also supply  the _restricting_ columns. The restricting columns will be the columns that will be used in the `WHERE` clause of the `UPDATE` query. In many cases, the restricting columns are the ones composing the primary key of your table. It may be  an _id_ column (which may be auto-incremented) or multiple columns.

        public void setDataSource(DataSource dataSource) {
            simpleJdbcUpdate = new SimpleJdbcUpdate(dataSource)
                    .withTableName("my_table")
                    .restrictingColumns("field_1", "field_2");
        }

To execute the update statement, you must supply 2 arguments to the `execute` method. Those arguments may be of type `SqlParameterSource` or `Map<Object, String>`. Both of them must be of the same type.

The first one contains the values to update in the table and the second one, the values for the restricting columns to use in the `WHERE` clause.

        public void updateMyObject(String arg1, int arg2, Float arg3, boolean arg4) {
            SqlParameterSource updatingParameterSource = new MapSqlParameterSource
                .addValue("field_1", arg1)
                .addValue("field_2", arg2)
                .addValue("field_3", arg3)
                .addValue("field_4", arg4);

            SqlParameterSource restrictingParameterSource = new MapSqlParameterSource
                .addValue("field_1", arg1)
                .addValue("field_2", arg2);

            simpleJdbcUpdate.execute(updatingParameterSource, restrictingParameterSource);
        }

The previous `execute` method call will build (on first call only for the given simpleJdbcUpdate object) the following SQL query:

    UPDATE my_table SET field_1 = ?, field_2 = ?, field_3 = ?, field_4 = ? WHERE field_1 = ?, field_2 = ?

Then, on each call of this method, the statement is executed against the database using the values you provided for the call.

Advanced configuration
----------------------

### Customizing the `SET` clause (fields to update)

By default, the built-in query updates every field of the table. You can change this by calling the `updatingColumns` method. It takes the list of fields to update (in the `SET` clause of the `UPDATE` command).

        public void setDataSource(DataSource dataSource) {
            simpleJdbcUpdate = new SimpleJdbcUpdate(dataSource)
                    .withTableName("my_table")
                    .updatingColumns("field_3", "field_4")
                    .restrictingColumns("field_1", "field_2");
        }

The query built after the previous example will be:

    UPDATE my_table SET field_3 = ?, field_4 = ? WHERE field_1 = ?, field_2 = ?

Then, you'll only have to supply values for `field_3` and `field_4` as first parameter of the `execute` method.

### Using restricting columns with operators

By default `restrictingColumns` assume an `=` operator for restricting columns.  An overloaded version of this method, takes a map of column names to operators.
        
        Map<String, Operator> where = new HashMap<String,Operator>();
        where.put("field_1", Operator.GREATER_THAN);
        simpleJdbcUpdate = new SimpleJdbcUpdate(dataSource)
                    .withTableName("my_table")
                    .updatingColumns("field_3", "field_4")
                    .restrictingColumns(where);

### Using no restricting columns

If you don't call the `restrictingColumns`, the `WHERE` clause will not be present in the query and every row will be updated each time the execute `method` is called.

### Other `with*` methods

Like for the `SimpleJdbcInsert`, you can call the `withCatalogName`, `withSchemaName` and `withoutColumnMetaDataAccess` methods prior any call to the `execute` method.

License
=======

This entire project is released under the [Apache License 2.0](http://www.apache.org/licenses/LICENSE-2.0)
