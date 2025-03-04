package org.simple.clinic.overdue.callresult

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import io.reactivex.Observable
import org.simple.clinic.overdue.Appointment
import org.simple.clinic.overdue.AppointmentCancelReason
import org.simple.clinic.patient.SyncStatus
import org.simple.clinic.storage.Timestamps
import org.simple.clinic.user.User
import org.simple.clinic.util.UtcClock
import java.util.UUID

@Entity(tableName = "CallResult")
data class CallResult(

    @PrimaryKey
    val id: UUID,

    val userId: UUID,

    val appointmentId: UUID,

    val removeReason: AppointmentCancelReason?,

    val outcome: Outcome,

    @Embedded
    val timestamps: Timestamps,

    val syncStatus: SyncStatus
) {

  companion object {
    fun agreedToVisit(
        id: UUID,
        appointment: Appointment,
        user: User,
        clock: UtcClock,
        syncStatus: SyncStatus
    ) = CallResult(
        id = id,
        userId = user.uuid,
        appointmentId = appointment.uuid,
        removeReason = null,
        outcome = Outcome.AgreedToVisit,
        timestamps = Timestamps.create(clock),
        syncStatus = syncStatus
    )

    fun remindToCallLater(
        id: UUID,
        appointment: Appointment,
        user: User,
        clock: UtcClock,
        syncStatus: SyncStatus
    ) = CallResult(
        id = id,
        userId = user.uuid,
        appointmentId = appointment.uuid,
        removeReason = null,
        outcome = Outcome.RemindToCallLater,
        timestamps = Timestamps.create(clock),
        syncStatus = syncStatus
    )

    fun removed(
        id: UUID,
        removeReason: AppointmentCancelReason,
        appointment: Appointment,
        user: User,
        clock: UtcClock,
        syncStatus: SyncStatus
    ) = CallResult(
        id = id,
        userId = user.uuid,
        appointmentId = appointment.uuid,
        removeReason = removeReason,
        outcome = Outcome.RemovedFromOverdueList,
        timestamps = Timestamps.create(clock),
        syncStatus = syncStatus
    )
  }

  @Dao
  interface RoomDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun save(callResults: List<CallResult>)

    @Query("""
        UPDATE CallResult
        SET syncStatus = :newStatus
        WHERE syncStatus = :oldStatus
    """)
    fun updateSyncStatus(
        oldStatus: SyncStatus,
        newStatus: SyncStatus
    )

    @Query("""
      UPDATE CallResult
      SET syncStatus = :newStatus
      WHERE id IN (:callResultIds)
    """)
    fun updateSyncStatusForIds(
        callResultIds: List<UUID>,
        newStatus: SyncStatus
    )

    @Query("""
      SELECT COUNT(id)
      FROM CallResult
    """)
    fun recordCount(): Observable<Int>

    @Query("""
      SELECT COUNT(id)
      FROM CallResult
      WHERE syncStatus = :syncStatus
    """)
    fun countWithStatus(syncStatus: SyncStatus): Observable<Int>

    @Query("""
      SELECT *
      FROM CallResult
      WHERE syncStatus = :syncStatus
      LIMIT :limit OFFSET :offset
    """)
    fun recordsWithSyncStatusBatched(
        syncStatus: SyncStatus,
        limit: Int,
        offset: Int
    ): List<CallResult>

    @Query("""
      DELETE FROM CallResult
    """)
    fun clear()

    @Query("""
      SELECT *
      FROM CallResult
      WHERE id = :id
    """)
    fun getOne(
        id: UUID
    ): CallResult?

    @Query("""
      DELETE
      FROM CallResult
      WHERE
        deletedAt IS NOT NULL
        AND syncStatus == 'DONE'
    """)
    fun purgeDeleted()
  }
}
