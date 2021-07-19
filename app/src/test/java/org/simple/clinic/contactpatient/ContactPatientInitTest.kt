package org.simple.clinic.contactpatient

import com.spotify.mobius.test.FirstMatchers.hasEffects
import com.spotify.mobius.test.FirstMatchers.hasModel
import com.spotify.mobius.test.FirstMatchers.hasNoEffects
import com.spotify.mobius.test.InitSpec
import com.spotify.mobius.test.InitSpec.assertThatFirst
import org.junit.Test
import org.simple.clinic.TestData
import org.simple.clinic.facility.FacilityConfig
import org.simple.clinic.overdue.AppointmentConfig
import org.simple.clinic.overdue.TimeToAppointment
import org.simple.clinic.overdue.TimeToAppointment.Days
import org.simple.clinic.overdue.TimeToAppointment.Weeks
import org.simple.clinic.util.TestUserClock
import java.time.LocalDate
import java.time.Period
import java.util.Optional
import java.util.UUID

class ContactPatientInitTest {

  private val patientUuid = UUID.fromString("34556bef-6221-4ffb-a5b7-4e7f30d584c1")
  private val timeToAppointments = listOf(
      Days(1),
      Weeks(1),
      Weeks(2)
  )
  private val userClock = TestUserClock(LocalDate.parse("2018-01-01"))

  private val spec = InitSpec(ContactPatientInit())

  @Test
  fun `when the screen is created, load the patient profile and the latest appointment`() {
    val defaultModel = defaultModel()

    spec
        .whenInit(defaultModel)
        .then(assertThatFirst(
            hasModel(defaultModel.contactPatientInfoLoading()),
            hasEffects(LoadPatientProfile(patientUuid), LoadLatestOverdueAppointment(patientUuid), LoadCurrentFacility)
        ))
  }

  @Test
  fun `when the screen is restored, do not load the patient profile, latest appointment and current facility if they are already loaded`() {
    val facility = TestData.facility(
        uuid = UUID.fromString("251deca2-d219-4863-80fc-e7d48cb22b1b"),
        name = "PHC Obvious",
        facilityConfig = FacilityConfig(
            diabetesManagementEnabled = true,
            teleconsultationEnabled = false
        )
    )

    val model = defaultModel()
        .patientProfileLoaded(TestData.patientProfile(patientUuid = patientUuid))
        .overdueAppointmentLoaded(Optional.empty())
        .currentFacilityLoaded(currentFacility = facility)

    spec
        .whenInit(model)
        .then(assertThatFirst(
            hasModel(model.contactPatientInfoLoaded()),
            hasNoEffects()
        ))
  }

  private fun defaultModel(
      phoneMaskFeatureEnabled: Boolean = false,
      timeToAppointments: List<TimeToAppointment> = this.timeToAppointments,
      mode: UiMode = UiMode.CallPatient,
      overdueListChangesFeatureEnabled: Boolean = false
  ): ContactPatientModel {
    val appointmentConfig = AppointmentConfig(
        appointmentDuePeriodForDefaulters = Period.ZERO,
        scheduleAppointmentsIn = emptyList(),
        defaultTimeToAppointment = Days(0),
        periodForIncludingOverdueAppointments = Period.ZERO,
        remindAppointmentsIn = timeToAppointments
    )

    return ContactPatientModel.create(
        patientUuid = patientUuid,
        appointmentConfig = appointmentConfig,
        userClock = userClock,
        mode = mode,
        secureCallFeatureEnabled = phoneMaskFeatureEnabled,
        overdueListChangesFeatureEnabled = overdueListChangesFeatureEnabled
    )
  }
}

