package com.drelabs.datacheck

import androidx.room.Query
import com.drelabs.datacheck.data.db.UsageLogDao
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Structural pin for ISSUE-008 (F-01): the totalsSince query must guard
 * both SUM columns with IFNULL(..., 0).
 *
 * This is a STRUCTURAL test, not a behavioral one. It reflectively reads
 * the @Query annotation SQL from the UsageLogDao interface and asserts on
 * the SQL text. Room's row mapping (NULL aggregate row over an empty match
 * mapped into non-null TotalsRow Longs) cannot be executed in a JVM unit
 * test without an in-memory database (Robolectric/room-testing), which the
 * no-new-dependencies constraint forbids. So this pin proves the SQL
 * contains the NULL guard by construction; it does NOT prove Room maps the
 * result correctly. Room's annotation processor validates the SQL syntax
 * and table/column references at compile time in CI.
 */
class UsageLogDaoSqlGuardTest {

    @Test
    fun `totalsSince guards both SUM columns with IFNULL`() {
        val method = UsageLogDao::class.java.methods.single { it.name == "totalsSince" }
        val query = method.getAnnotation(Query::class.java)
        assertNotNull("totalsSince must carry a @Query annotation", query)

        val sql = query!!.value
        val compact = sql.lowercase().replace(Regex("\\s+"), "")

        assertTrue(
            "total column must be IFNULL(SUM(rx + tx), 0) AS total, was: $sql",
            compact.contains("ifnull(sum(rx+tx),0)astotal"),
        )
        assertTrue(
            "fgTotal column must be IFNULL(SUM(fgRx + fgTx), 0) AS fgTotal, was: $sql",
            compact.contains("ifnull(sum(fgrx+fgtx),0)asfgtotal"),
        )
        assertTrue(
            "query must keep targeting usage with tickStart >= :sinceMs filter, was: $sql",
            compact.contains("fromusagewheretickstart>=:sincems"),
        )
    }
}
