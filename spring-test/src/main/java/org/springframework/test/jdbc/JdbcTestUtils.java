/*
 * Copyright 2002-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.test.jdbc;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.SqlParameterValue;
import org.springframework.util.StringUtils;

/**
 * {@code JdbcTestUtils} is a collection of JDBC related utility functions intended to simplify
 * standard database testing scenarios.
 *
 * @author Thomas Risberg
 * @author Sam Brannen
 * @author Juergen Hoeller
 * @author Phillip Webb
 * @author Chris Baldwin
 * @since 2.5.4
 * @see org.springframework.jdbc.core.JdbcTemplate
 * @see org.springframework.jdbc.datasource.init.ScriptUtils
 * @see org.springframework.jdbc.datasource.init.ResourceDatabasePopulator
 * @see org.springframework.jdbc.datasource.init.DatabasePopulatorUtils
 */
public abstract class JdbcTestUtils {

  private static final Log logger = LogFactory.getLog(JdbcTestUtils.class);

  /**
   * Count the rows in the given table.
   *
   * @param jdbcTemplate the JdbcTemplate with which to perform JDBC operations
   * @param tableName name of the table to count rows in
   * @return the number of rows in the table
   */
  public static int countRowsInTable(JdbcTemplate jdbcTemplate, String tableName) {
    Integer result =
        jdbcTemplate.queryForObject("SELECT COUNT(0) FROM " + tableName, Integer.class);
    return (result != null ? result : 0);
  }

  /**
   * Count the rows in the given table, using the provided {@code WHERE} clause.
   *
   * <p>If the provided {@code WHERE} clause contains text, it will be prefixed with {@code " WHERE
   * "} and then appended to the generated {@code SELECT} statement. For example, if the provided
   * table name is {@code "person"} and the provided where clause is {@code "name = 'Bob' and age >
   * 25"}, the resulting SQL statement to execute will be {@code "SELECT COUNT(0) FROM person WHERE
   * name = 'Bob' and age > 25"}.
   *
   * @param jdbcTemplate the JdbcTemplate with which to perform JDBC operations
   * @param tableName the name of the table to count rows in
   * @param whereClause the {@code WHERE} clause to append to the query
   * @return the number of rows in the table that match the provided {@code WHERE} clause
   */
  public static int countRowsInTableWhere(
      JdbcTemplate jdbcTemplate, String tableName, String whereClause) {
    String sql = "SELECT COUNT(0) FROM " + tableName;
    if (StringUtils.hasText(whereClause)) {
      sql += " WHERE " + whereClause;
    }
    Integer result = jdbcTemplate.queryForObject(sql, Integer.class);
    return (result != null ? result : 0);
  }

  /**
   * Delete all rows from the specified tables.
   *
   * @param jdbcTemplate the JdbcTemplate with which to perform JDBC operations
   * @param tableNames the names of the tables to delete from
   * @return the total number of rows deleted from all specified tables
   */
  public static int deleteFromTables(JdbcTemplate jdbcTemplate, String... tableNames) {
    int totalRowCount = 0;
    for (String tableName : tableNames) {
      int rowCount = jdbcTemplate.update("DELETE FROM " + tableName);
      totalRowCount += rowCount;
      if (logger.isInfoEnabled()) {
        logger.info("Deleted " + rowCount + " rows from table " + tableName);
      }
    }
    return totalRowCount;
  }

  /**
   * 使用提供的{@code WHERE}子句从给定表中删除行。
   *
   * <p>如果提供的{@code WHERE}子句包含文本，则将以{@code " WHERE "}作为前缀，然后附加到生成的{@code
   * DELETE}语句中。例如，如果提供的表名是{@code "person"}，提供的where子句是{@code "name = 'Bob' and age >
   * 25"}，那么执行的结果SQL语句将是{@code "DELETE FROM person where name = 'Bob' and age > 25"}。
   *
   * <p>作为硬编码值的替代方法，{@code "?}占位符可以在{@code WHERE}子句中使用，绑定到给定的参数。@param: jdbcTemplate:
   * jdbcTemplate:执行JDBC操作的jdbcTemplate: @param: tableName:表名:删除行:@param: whereClause: {@code:
   * WHERE}子句:附加到查询;@param: args:参数:绑定到查询(留给PreparedStatement去猜测相应的SQL类型);也可能包含{@link
   * SqlParameterValue}对象，这些对象不仅指示参数值，还指示SQL类型和可选的比例。
   *
   * @return the number of rows deleted from the table
   */
  public static int deleteFromTableWhere(
      JdbcTemplate jdbcTemplate, String tableName, String whereClause, Object... args) {

    String sql = "DELETE FROM " + tableName;
    if (StringUtils.hasText(whereClause)) {
      sql += " WHERE " + whereClause;
    }
    int rowCount = (args.length > 0 ? jdbcTemplate.update(sql, args) : jdbcTemplate.update(sql));
    if (logger.isInfoEnabled()) {
      logger.info("Deleted " + rowCount + " rows from table " + tableName);
    }
    return rowCount;
  }

  /**
   * Drop the specified tables.
   *
   * @param jdbcTemplate the JdbcTemplate with which to perform JDBC operations
   * @param tableNames the names of the tables to drop
   */
  public static void dropTables(JdbcTemplate jdbcTemplate, String... tableNames) {
    for (String tableName : tableNames) {
      jdbcTemplate.execute("DROP TABLE " + tableName);
      if (logger.isInfoEnabled()) {
        logger.info("Dropped table " + tableName);
      }
    }
  }
}
