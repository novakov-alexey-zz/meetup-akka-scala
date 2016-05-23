package meetup.akka.dal

import java.sql.{CallableStatement, PreparedStatement, ResultSet, Timestamp}
import java.time.{LocalDateTime, ZoneId, ZonedDateTime}
import java.util.GregorianCalendar

import org.apache.ibatis.`type`.{BaseTypeHandler, JdbcType}


class LocalDateTimeTypeHandler extends BaseTypeHandler[LocalDateTime] {
  override def getNullableResult(rs: ResultSet, columnName: String): LocalDateTime = {
    val ts = rs.getTimestamp(columnName)
    if (ts != null) {
      return LocalDateTime.ofInstant(ts.toInstant, ZoneId.systemDefault)
    }
    null
  }

  override def getNullableResult(rs: ResultSet, columnIndex: Int): LocalDateTime = {
    val ts = rs.getTimestamp(columnIndex)
    if (ts != null) {
      return LocalDateTime.ofInstant(ts.toInstant, ZoneId.systemDefault)
    }
    null
  }

  override def getNullableResult(cs: CallableStatement, columnIndex: Int): LocalDateTime = {
    val ts = cs.getTimestamp(columnIndex)
    if (ts != null) {
      return LocalDateTime.ofInstant(ts.toInstant, ZoneId.systemDefault)
    }
    null
  }

  override def setNonNullParameter(ps: PreparedStatement, i: Int, parameter: LocalDateTime, jdbcType: JdbcType): Unit = {
    ps.setTimestamp(i, Timestamp.valueOf(parameter), GregorianCalendar.from(ZonedDateTime.of(parameter, ZoneId.systemDefault)))
  }
}
