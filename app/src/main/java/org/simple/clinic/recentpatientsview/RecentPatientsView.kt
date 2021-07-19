package org.simple.clinic.recentpatientsview

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.material.composethemeadapter.MdcTheme
import com.spotify.mobius.android.MobiusLoopViewModel
import com.spotify.mobius.rx2.RxMobius
import org.simple.clinic.R
import org.simple.clinic.di.injector
import org.simple.clinic.navigation.v2.Router
import org.simple.clinic.patient.Gender
import org.simple.clinic.patient.PatientConfig
import org.simple.clinic.patient.RecentPatient
import org.simple.clinic.recentpatient.RecentPatientsScreen
import org.simple.clinic.summary.OpenIntention
import org.simple.clinic.summary.PatientSummaryScreenKey
import org.simple.clinic.ui.theme.ButtonDefaults
import org.simple.clinic.ui.theme.body0Medium
import org.simple.clinic.ui.theme.tag
import org.simple.clinic.util.UserClock
import org.simple.clinic.util.UtcClock
import org.simple.clinic.util.toLocalDateAtZone
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID
import javax.inject.Inject
import javax.inject.Named

typealias RecentPatientsViewModel = MobiusLoopViewModel<LatestRecentPatientsModel, LatestRecentPatientsEvent, LatestRecentPatientsEffect, Any>

class RecentPatientsView(
    context: Context,
    attrs: AttributeSet?
) : FrameLayout(context, attrs), LatestRecentPatientsUiActions {

  @Inject
  lateinit var utcClock: UtcClock

  @Inject
  lateinit var userClock: UserClock

  @Inject
  @Named("full_date")
  lateinit var dateFormatter: DateTimeFormatter

  @Inject
  lateinit var router: Router

  @Inject
  lateinit var effectHandlerFactory: LatestRecentPatientsEffectHandler.Factory

  @Inject
  lateinit var config: PatientConfig

  override fun onFinishInflate() {
    super.onFinishInflate()
    context.injector<Injector>().inject(this)

    addView(ComposeView(context = context).apply {
      setContent {
        MdcTheme {
          RecentPatientsView(
              userClock = userClock,
              dateFormatter = dateFormatter,
              viewModel = mobiusLoopViewModel(
                  defaultModel = LatestRecentPatientsModel.create(),
                  init = LatestRecentPatientsInit.create(config),
                  update = LatestRecentPatientsUpdate(),
                  effectHandler = effectHandlerFactory.create(this@RecentPatientsView)
              )
          )
        }
      }
    })
  }

  override fun openRecentPatientsScreen() {
    router.push(RecentPatientsScreen.Key())
  }

  override fun openPatientSummary(patientUuid: UUID) {
    router.push(
        PatientSummaryScreenKey(
            patientUuid = patientUuid,
            intention = OpenIntention.ViewExistingPatient,
            screenCreatedTimestamp = Instant.now(utcClock)
        )
    )
  }

  interface Injector {
    fun inject(target: RecentPatientsView)
  }
}

@Composable
private fun RecentPatientsView(
    userClock: UserClock,
    dateFormatter: DateTimeFormatter,
    viewModel: RecentPatientsViewModel,
) {
  Surface(
      modifier = Modifier
          .background(MaterialTheme.colors.background)
          .padding(top = 8.dp),
      color = Color.Unspecified
  ) {
    val state by viewModel.models.observeAsState(initial = viewModel.model)

    if (!state.hasLoadedRecentPatients) {
      ProgressIndicator()
      return@Surface
    }

    if (state.isAtLeastOneRecentPatientPresent) {
      RecentPatientsList(recentPatients = state.recentPatients!!,
          userClock = userClock,
          dateFormatter = dateFormatter,
          onItemClick = { patientUuid ->
            viewModel.dispatchEvent(RecentPatientItemClicked(patientUuid = patientUuid))
          },
          onSeeAllButtonClick = {
            viewModel.dispatchEvent(SeeAllItemClicked)
          })
    } else {
      NoRecentPatients()
    }
  }
}

@Composable
private fun ProgressIndicator() {
  Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.Center
  ) {
    CircularProgressIndicator(modifier = Modifier.padding(16.dp))
  }
}

@Composable
private fun NoRecentPatients() {
  Text(
      modifier = Modifier
          .padding(horizontal = 8.dp)
          .background(color = MaterialTheme.colors.surface,
              shape = RoundedCornerShape(4.dp))
          .fillMaxWidth()
          .padding(16.dp),
      text = stringResource(id = R.string.patients_recentpatients_no_recent_patients),
      textAlign = TextAlign.Center,
      style = MaterialTheme.typography.subtitle1,
      color = colorResource(id = R.color.color_on_surface_67)
  )
}

@Composable
private fun RecentPatientsList(
    recentPatients: List<RecentPatient>,
    userClock: UserClock,
    dateFormatter: DateTimeFormatter,
    onItemClick: (UUID) -> Unit,
    onSeeAllButtonClick: () -> Unit
) {
  Column {
    recentPatients.forEach { recentPatient ->
      val today = LocalDate.now(userClock)
      val lastSeenAt = dateFormatter.format(recentPatient.updatedAt.toLocalDateAtZone(userClock.zone))

      val patientRegisteredOnDate = recentPatient.patientRecordedAt.toLocalDateAtZone(userClock.zone)
      val isNewRegistration = today == patientRegisteredOnDate

      RecentPatientItem(gender = recentPatient.gender,
          fullName = recentPatient.fullName,
          lastSeenAt = lastSeenAt,
          isNewRegistration = isNewRegistration,
          onClick = { onItemClick(recentPatient.uuid) })
    }
    SeeAllButton(onClick = onSeeAllButtonClick)
  }
}

@Composable
private fun RecentPatientItem(
    gender: Gender,
    fullName: String,
    lastSeenAt: String,
    isNewRegistration: Boolean,
    onClick: () -> Unit
) {
  Card(modifier = Modifier
      .padding(
          start = 8.dp,
          end = 8.dp,
          bottom = 8.dp
      )
      .fillMaxWidth(),
      onClick = onClick
  ) {
    Row(modifier = Modifier.padding(16.dp)) {
      val genderIcon = when (gender) {
        Gender.Female -> R.drawable.ic_patient_female
        Gender.Male -> R.drawable.ic_patient_male
        Gender.Transgender -> R.drawable.ic_patient_transgender
        is Gender.Unknown -> R.drawable.ic_patient_unknown
      }
      Image(painter = painterResource(id = genderIcon), contentDescription = null)
      Spacer(modifier = Modifier.width(16.dp))

      Column {
        Text(text = fullName,
            style = MaterialTheme.typography.body0Medium,
            color = MaterialTheme.colors.primary)
        Spacer(modifier = Modifier.height(4.dp))

        if (isNewRegistration) {
          Text(text = stringResource(id = R.string.recent_patients_itemview_new_registration),
              style = MaterialTheme.typography.body2,
              color = colorResource(id = R.color.simple_green_500))
          Spacer(modifier = Modifier.height(12.dp))
        }

        Row {
          Text(text = stringResource(id = R.string.recent_patients_itemview_last_seen),
              style = MaterialTheme.typography.tag,
              color = colorResource(id = R.color.color_on_surface_67))

          Text(text = lastSeenAt,
              style = MaterialTheme.typography.body2,
              color = colorResource(id = R.color.color_on_surface_67))
        }
      }
    }
  }
}

@Composable
private fun SeeAllButton(
    onClick: () -> Unit
) {
  TextButton(
      onClick = onClick,
      modifier = Modifier
          .padding(all = 8.dp)
          .defaultMinSize(minHeight = ButtonDefaults.MinHeight)
          .fillMaxWidth(),
      contentPadding = PaddingValues(horizontal = 16.dp)
  ) {
    Text(text = stringResource(id = R.string.patients_recentpatients_see_all))
  }
}

@Composable
private fun mobiusLoopViewModel(
    defaultModel: LatestRecentPatientsModel,
    init: LatestRecentPatientsInit,
    update: LatestRecentPatientsUpdate,
    effectHandler: LatestRecentPatientsEffectHandler,
): RecentPatientsViewModel = viewModel(
    factory = object : ViewModelProvider.Factory {
      @Suppress("UNCHECKED_CAST")
      override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return MobiusLoopViewModel.create<LatestRecentPatientsModel, LatestRecentPatientsEvent, LatestRecentPatientsEffect, Any>(
            ::loop,
            defaultModel,
            init
        ) as T
      }

      private fun loop(viewEffectConsumer: Any) = RxMobius.loop(
          update,
          effectHandler.build()
      )
    }
)
