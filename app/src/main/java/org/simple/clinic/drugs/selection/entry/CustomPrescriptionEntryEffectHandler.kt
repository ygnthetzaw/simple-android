package org.simple.clinic.drugs.selection.entry

import com.spotify.mobius.rx2.RxMobius
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import io.reactivex.Observable
import io.reactivex.ObservableTransformer
import io.reactivex.Scheduler
import org.simple.clinic.drugs.PrescriptionRepository
import org.simple.clinic.facility.Facility
import org.simple.clinic.facility.FacilityRepository
import org.simple.clinic.user.UserSession
import org.simple.clinic.util.nullIfBlank
import org.simple.clinic.util.scheduler.SchedulersProvider
import java.util.UUID

class CustomPrescriptionEntryEffectHandler @AssistedInject constructor(
    @Assisted val uiActions: CustomPrescriptionEntryUiActions,
    val schedulersProvider: SchedulersProvider,
    val userSession: UserSession,
    val facilityRepository: FacilityRepository,
    val prescriptionRepository: PrescriptionRepository
) {

  @AssistedInject.Factory
  interface Factory {
    fun create(uiActions: CustomPrescriptionEntryUiActions): CustomPrescriptionEntryEffectHandler
  }

  fun build()
      : ObservableTransformer<CustomPrescriptionEntryEffect, CustomPrescriptionEntryEvent> {
    return RxMobius
        .subtypeEffectHandler<CustomPrescriptionEntryEffect, CustomPrescriptionEntryEvent>()
        .addTransformer(SaveCustomPrescription::class.java, saveCustomPrescription(schedulersProvider.io()))
        .addAction(CloseSheet::class.java, uiActions::finish, schedulersProvider.ui())
        .build()
  }

  private fun saveCustomPrescription(io: Scheduler): ObservableTransformer<SaveCustomPrescription, CustomPrescriptionEntryEvent> {
    return ObservableTransformer { effects ->
      effects
          .observeOn(io)
          .map {
            val user = userSession.loggedInUserImmediate()!!
            val currentFacility = facilityRepository.currentFacilityImmediate(user)!!
            SavePrescription(
                patientUuid = it.patientUuid,
                name = it.drugName,
                dosage = it.dosage,
                facility = currentFacility
            )
          }
          .flatMap { savePrescription ->
            prescriptionRepository
                .savePrescription(
                    patientUuid = savePrescription.patientUuid,
                    name = savePrescription.name,
                    dosage = savePrescription.dosage.nullIfBlank(),
                    rxNormCode = null,
                    isProtocolDrug = false,
                    facility = savePrescription.facility
                )
                .andThen(Observable.just(CustomPrescriptionSaved))
          }
    }
  }
}

private data class SavePrescription(
    val patientUuid: UUID,
    val name: String,
    val dosage: String?,
    val facility: Facility
)

