package com.demo.server.epmigration.project.persistence

import org.apache.ibatis.type.BaseTypeHandler
import org.apache.ibatis.type.JdbcType
import org.apache.ibatis.type.MappedJdbcTypes
import org.apache.ibatis.type.MappedTypes
import java.sql.CallableStatement
import java.sql.PreparedStatement
import java.sql.ResultSet

@MappedTypes(List::class)
@MappedJdbcTypes(JdbcType.ARRAY)
class StringListTypeHandler : BaseTypeHandler<List<String>>() {
    override fun setNonNullParameter(ps: PreparedStatement, i: Int, parameter: List<String>, jdbcType: JdbcType?) {
        ps.setArray(i, ps.connection.createArrayOf("text", parameter.toTypedArray()))
    }

    override fun getNullableResult(rs: ResultSet, columnName: String): List<String>? {
        return rs.getArray(columnName)?.toStringList()
    }

    override fun getNullableResult(rs: ResultSet, columnIndex: Int): List<String>? {
        return rs.getArray(columnIndex)?.toStringList()
    }

    override fun getNullableResult(cs: CallableStatement, columnIndex: Int): List<String>? {
        return cs.getArray(columnIndex)?.toStringList()
    }

    private fun java.sql.Array.toStringList(): List<String> {
        return (array as Array<*>).map { it.toString() }
    }
}
