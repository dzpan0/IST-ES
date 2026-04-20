package pt.ulisboa.tecnico.socialsoftware.humanaethica.shift.webservice

import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import pt.ulisboa.tecnico.socialsoftware.humanaethica.SpockTest
import pt.ulisboa.tecnico.socialsoftware.humanaethica.activity.domain.Activity
import pt.ulisboa.tecnico.socialsoftware.humanaethica.auth.domain.AuthUser
import pt.ulisboa.tecnico.socialsoftware.humanaethica.institution.domain.Institution
import pt.ulisboa.tecnico.socialsoftware.humanaethica.participation.dto.ParticipationDto
import pt.ulisboa.tecnico.socialsoftware.humanaethica.shift.dto.ShiftDto
import pt.ulisboa.tecnico.socialsoftware.humanaethica.user.domain.User
import pt.ulisboa.tecnico.socialsoftware.humanaethica.utils.DateHandler

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CreateShiftWebServiceIT extends SpockTest {
    @LocalServerPort
    private int port

    def activity
    def shiftDto

    def setup() {
        deleteAll()

        webClient = WebClient.create("http://localhost:" + port)
        headers = new HttpHeaders()
        headers.setContentType(MediaType.APPLICATION_JSON)

        def institution = institutionService.getDemoInstitution()

        def activityDto = createActivityDto(ACTIVITY_NAME_1, ACTIVITY_REGION_1, ACTIVITY_LIMIT_1, ACTIVITY_DESCRIPTION_1,
                THREE_DAYS_AGO, TWO_DAYS_AGO, IN_TWO_DAYS, null)

        activity = new Activity(activityDto, institution, new ArrayList<>())
        activityRepository.save(activity)

        shiftDto = new ShiftDto()
        shiftDto.participantsNumberLimit = SHIFT_LIMIT_1
        shiftDto.startingDate = DateHandler.toISOString(ONE_DAY_AGO.withNano(0))
        shiftDto.endingDate = DateHandler.toISOString(IN_ONE_DAY.withNano(0))
        shiftDto.location = SHIFT_LOCATION_1
    }

    def 'member create shift'() {
        given:
        demoMemberLogin()

        when:
        def response = webClient.post()
                .uri('/activities/' + activity.id + '/shifts')
                .headers(httpHeaders -> httpHeaders.putAll(headers))
                .bodyValue(shiftDto)
                .retrieve()
                .bodyToMono(ShiftDto.class)
                .block()

        then: "the return data is correct"
        response.participantsNumberLimit == SHIFT_LIMIT_1
        response.startingDate == DateHandler.toISOString(ONE_DAY_AGO.withNano(0))
        response.endingDate == DateHandler.toISOString(IN_ONE_DAY.withNano(0))
        response.location == SHIFT_LOCATION_1

        and: "the shift is saved in the database"
        shiftRepository.findAll().size() == 1
        def storedShift = shiftRepository.findAll().get(0)
        storedShift.participantsNumberLimit == SHIFT_LIMIT_1
        storedShift.startingDate == ONE_DAY_AGO.withNano(0)
        storedShift.endingDate == IN_ONE_DAY.withNano(0)
        storedShift.location == SHIFT_LOCATION_1
        storedShift.activity.id == activity.id

        cleanup:
        deleteAll()
    }

    def 'member create shift with error'() {
        given:
        demoMemberLogin()
        and:
        shiftDto.location = 'short'

        when:
        def response = webClient.post()
                .uri('/activities/' + activity.id + '/shifts')
                .headers(httpHeaders -> httpHeaders.putAll(headers))
                .bodyValue(shiftDto)
                .retrieve()
                .bodyToMono(ShiftDto.class)
                .block()

        then:
        def error = thrown(WebClientResponseException)
        error.statusCode == HttpStatus.BAD_REQUEST
        shiftRepository.count() == 0

        cleanup:
        deleteAll()
    }

    def 'member of another institution cannot create shift'() {
        given:
        def otherInstitution = new Institution(INSTITUTION_1_NAME, INSTITUTION_1_EMAIL, INSTITUTION_1_NIF)
        institutionRepository.save(otherInstitution)
        createMember(USER_3_NAME,USER_3_USERNAME,USER_3_PASSWORD,USER_3_EMAIL, AuthUser.Type.NORMAL, otherInstitution, User.State.APPROVED)
        normalUserLogin(USER_3_USERNAME, USER_3_PASSWORD)

        when:
        webClient.post()
                .uri('/activities/' + activity.id + '/shifts')
                .headers(httpHeaders -> httpHeaders.putAll(headers))
                .bodyValue(shiftDto)
                .retrieve()
                .bodyToMono(ShiftDto.class)
                .block()

        then:
        def error = thrown(WebClientResponseException)
        error.statusCode == HttpStatus.FORBIDDEN
    }

    def 'volunteer cannot create shift'() {
        given:
        demoVolunteerLogin()

        when:
        def response = webClient.post()
                .uri('/activities/' + activity.id + '/shifts')
                .headers(httpHeaders -> httpHeaders.putAll(headers))
                .bodyValue(shiftDto)
                .retrieve()
                .bodyToMono(ShiftDto.class)
                .block()

        then:
        def error = thrown(WebClientResponseException)
        error.statusCode == HttpStatus.FORBIDDEN
        shiftRepository.count() == 0

        cleanup:
        deleteAll()
    }

    def 'admin cannot create shift'() {
        given:
        demoAdminLogin()

        when:
        def response = webClient.post()
                .uri('/activities/' + activity.id + '/shifts')
                .headers(httpHeaders -> httpHeaders.putAll(headers))
                .bodyValue(shiftDto)
                .retrieve()
                .bodyToMono(ShiftDto.class)
                .block()

        then:
        def error = thrown(WebClientResponseException)
        error.statusCode == HttpStatus.FORBIDDEN
        shiftRepository.count() == 0

        cleanup:
        deleteAll()
    }
}

