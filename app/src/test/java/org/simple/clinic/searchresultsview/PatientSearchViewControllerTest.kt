package org.simple.clinic.searchresultsview

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.eq
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import io.reactivex.Observable
import io.reactivex.plugins.RxJavaPlugins
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import junitparams.JUnitParamsRunner
import junitparams.Parameters
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.simple.clinic.facility.FacilityRepository
import org.simple.clinic.patient.PatientMocker
import org.simple.clinic.patient.PatientRepository
import org.simple.clinic.patient.PatientSearchResults
import org.simple.clinic.searchresultsview.SearchResultsItemType.InCurrentFacilityHeader
import org.simple.clinic.searchresultsview.SearchResultsItemType.NotInCurrentFacilityHeader
import org.simple.clinic.searchresultsview.SearchResultsItemType.SearchResultRow
import org.simple.clinic.user.UserSession
import org.simple.clinic.util.RxErrorsRule
import org.simple.clinic.widgets.UiEvent

@RunWith(JUnitParamsRunner::class)
class PatientSearchViewControllerTest {

  @get:Rule
  val rxErrorsRule = RxErrorsRule()

  private val screen = mock<PatientSearchView>()
  private val patientRepository = mock<PatientRepository>()
  private val userSession = mock<UserSession>()
  private val facilityRepository = mock<FacilityRepository>()

  private val controller = PatientSearchViewController(
      patientRepository = patientRepository,
      userSession = userSession,
      facilityRepository = facilityRepository,
      bloodPressureDao = mock()
  )
  private val uiEvents = PublishSubject.create<UiEvent>()

  private val currentFacility = PatientMocker.facility()
  private val user = PatientMocker.loggedInUser()

  private val patientName = "name"

  @Before
  fun setUp() {
    RxJavaPlugins.setIoSchedulerHandler { Schedulers.trampoline() }

    whenever(userSession.requireLoggedInUser()).thenReturn(Observable.just(user))
    whenever(facilityRepository.currentFacility(user)).thenReturn(Observable.just(currentFacility))
    whenever(patientRepository.search(eq(patientName), eq(currentFacility), any())).thenReturn(Observable.never())
    uiEvents.compose(controller).subscribe { uiChange -> uiChange(screen) }
  }

  @Test
  fun `when searching patients by name returns results, the results should be displayed`() {
    val patientSearchResult1 = PatientMocker.patientSearchResult()
    val patientSearchResult2 = PatientMocker.patientSearchResult()

    val patientSearchResults = PatientSearchResults(
        visitedCurrentFacility = listOf(patientSearchResult1),
        notVisitedCurrentFacility = listOf(patientSearchResult2)
    )
    whenever(patientRepository.search(eq(patientName), eq(currentFacility), any()))
        .thenReturn(Observable.just(patientSearchResults))

    uiEvents.onNext(SearchResultsViewCreated)
    uiEvents.onNext(SearchPatientCriteria(searchPatientBy = SearchPatientBy.Name(searchText = patientName)))

    verify(screen).updateSearchResults(listOf(
        InCurrentFacilityHeader(facilityName = currentFacility.name),
        SearchResultRow(
            searchResult = patientSearchResult1,
            currentFacility = currentFacility
        ),
        NotInCurrentFacilityHeader,
        SearchResultRow(
            searchResult = patientSearchResult2,
            currentFacility = currentFacility
        )
    ))
    verify(screen).setEmptyStateVisible(false)
  }

  @Test
  fun `when searching patients by name returns no results, the empty state should be displayed`() {
    val emptySearchResults = PatientSearchResults(
        visitedCurrentFacility = emptyList(),
        notVisitedCurrentFacility = emptyList()
    )
    whenever(patientRepository.search(eq(patientName), eq(currentFacility), any()))
        .thenReturn(Observable.just(emptySearchResults))

    uiEvents.onNext(SearchResultsViewCreated)
    uiEvents.onNext(SearchPatientCriteria(searchPatientBy = SearchPatientBy.Name(searchText = patientName)))

    verify(screen).updateSearchResults(emptyList())
    verify(screen).setEmptyStateVisible(true)
  }

  @Test
  fun `when there are patients only in current facility, then "Other Results" header should not be shown`() {
    val patientSearchResult1 = PatientMocker.patientSearchResult()
    val patientSearchResult2 = PatientMocker.patientSearchResult()

    val patientSearchResults = PatientSearchResults(
        visitedCurrentFacility = listOf(patientSearchResult1, patientSearchResult2),
        notVisitedCurrentFacility = emptyList()
    )
    whenever(patientRepository.search(eq(patientName), eq(currentFacility), any()))
        .thenReturn(Observable.just(patientSearchResults))

    uiEvents.onNext(SearchResultsViewCreated)
    uiEvents.onNext(SearchPatientCriteria(searchPatientBy = SearchPatientBy.Name(searchText = patientName)))

    verify(screen).updateSearchResults(listOf(
        InCurrentFacilityHeader(facilityName = currentFacility.name),
        SearchResultRow(
            searchResult = patientSearchResult1,
            currentFacility = currentFacility
        ),
        SearchResultRow(
            searchResult = patientSearchResult2,
            currentFacility = currentFacility
        )
    ))
    verify(screen).setEmptyStateVisible(false)
  }

  @Test
  fun `when there are patients only in other facilities, then current facility header with "no results" should be shown`() {
    val patientSearchResult1 = PatientMocker.patientSearchResult()
    val patientSearchResult2 = PatientMocker.patientSearchResult()

    val patientSearchResults = PatientSearchResults(
        visitedCurrentFacility = emptyList(),
        notVisitedCurrentFacility = listOf(patientSearchResult1, patientSearchResult2)
    )
    whenever(patientRepository.search(eq(patientName), eq(currentFacility), any()))
        .thenReturn(Observable.just(patientSearchResults))

    uiEvents.onNext(SearchResultsViewCreated)
    uiEvents.onNext(SearchPatientCriteria(searchPatientBy = SearchPatientBy.Name(searchText = patientName)))

    verify(screen).updateSearchResults(listOf(
        InCurrentFacilityHeader(facilityName = currentFacility.name),
        SearchResultsItemType.NoPatientsInCurrentFacility,
        NotInCurrentFacilityHeader,
        SearchResultRow(
            searchResult = patientSearchResult1,
            currentFacility = currentFacility
        ),
        SearchResultRow(
            searchResult = patientSearchResult2,
            currentFacility = currentFacility
        )
    ))
    verify(screen).setEmptyStateVisible(false)
  }

  @Test
  fun `when search result clicked then SearchResultClicked event should be emitted`() {
    val searchResult = PatientMocker.patientSearchResult()
    val searchResultClicked = SearchResultClicked(searchResult)
    uiEvents.onNext(searchResultClicked)

    verify(screen).searchResultClicked(searchResultClicked)
  }

  @Test
  @Parameters(method = "params for search by parameters on register click")
  fun `when register new patient clicked then RegisterNewPatient event should be emitted`(searchPatientBy: SearchPatientBy) {
    uiEvents.onNext(SearchResultsViewCreated)
    uiEvents.onNext(SearchPatientCriteria(searchPatientBy))
    uiEvents.onNext(RegisterNewPatientClicked)

    verify(screen).registerNewPatient(RegisterNewPatient(searchPatientBy))
  }

  @Suppress("Unused")
  private fun `params for search by parameters on register click`(): List<SearchPatientBy> {
    return listOf(
        SearchPatientBy.Name(searchText = patientName),
        SearchPatientBy.PhoneNumber(searchText = "123456")
    )
  }
}
