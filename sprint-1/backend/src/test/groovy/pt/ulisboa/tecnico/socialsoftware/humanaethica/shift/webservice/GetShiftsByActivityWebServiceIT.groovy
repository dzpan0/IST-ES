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
import pt.ulisboa.tecnico.socialsoftware.humanaethica.shift.dto.ShiftDto
import pt.ulisboa.tecnico.socialsoftware.humanaethica.utils.DateHandler

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GetShiftsByActivityWebServiceIT extends SpockTest {
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

        def activityDto = createActivityDto(ACTIVITY_NAME_1, ACTIVITY_REGION_1, 5, ACTIVITY_DESCRIPTION_1,
                THREE_DAYS_AGO, TWO_DAYS_AGO, IN_TWO_DAYS, null)

        activity = new Activity(activityDto, institution, new ArrayList<>())
        activityRepository.save(activity)

        createShift(activity, SHIFT_LIMIT_1, ONE_DAY_AGO, IN_ONE_DAY, SHIFT_LOCATION_1)
        createShift(activity, SHIFT_LIMIT_2, ONE_DAY_AGO, IN_ONE_DAY, SHIFT_LOCATION_2)
    }

    def 'gets two shifts'() {
        when:
        def response = webClient.get()
                .uri('/activities/' + activity.id + '/shifts')
                .headers(httpHeaders -> httpHeaders.putAll(headers))
                .retrieve()
                .bodyToFlux(ShiftDto.class)
                .collectList()
                .block()

        then:
        response.size() == 2
        response.get(0).participantsNumberLimit == SHIFT_LIMIT_1
        DateHandler.toLocalDateTime(response.get(0).startingDate).withNano(0) == ONE_DAY_AGO.withNano(0)
        DateHandler.toLocalDateTime(response.get(0).endingDate).withNano(0) == IN_ONE_DAY.withNano(0)
        response.get(0).location == SHIFT_LOCATION_1
        response.get(1).participantsNumberLimit == SHIFT_LIMIT_2
        DateHandler.toLocalDateTime(response.get(1).startingDate).withNano(0) == ONE_DAY_AGO.withNano(0)
        DateHandler.toLocalDateTime(response.get(1).endingDate).withNano(0) == IN_ONE_DAY.withNano(0)
        response.get(1).location == SHIFT_LOCATION_2
    }

    def 'member gets two shifts'() {
        given:
        demoMemberLogin()

        when:
        def response = webClient.get()
                .uri('/activities/' + activity.id + '/shifts')
                .headers(httpHeaders -> httpHeaders.putAll(headers))
                .retrieve()
                .bodyToFlux(ShiftDto.class)
                .collectList()
                .block()

        then:
        response.size() == 2
        response.get(0).participantsNumberLimit == SHIFT_LIMIT_1
        DateHandler.toLocalDateTime(response.get(0).startingDate).withNano(0) == ONE_DAY_AGO.withNano(0)
        DateHandler.toLocalDateTime(response.get(0).endingDate).withNano(0) == IN_ONE_DAY.withNano(0)
        response.get(0).location == SHIFT_LOCATION_1
        response.get(1).participantsNumberLimit == SHIFT_LIMIT_2
        DateHandler.toLocalDateTime(response.get(1).startingDate).withNano(0) == ONE_DAY_AGO.withNano(0)
        DateHandler.toLocalDateTime(response.get(1).endingDate).withNano(0) == IN_ONE_DAY.withNano(0)
        response.get(1).location == SHIFT_LOCATION_2
    }

    def 'volunteer gets two shifts'() {
        given:
        demoVolunteerLogin()

        when:
        def response = webClient.get()
                .uri('/activities/' + activity.id + '/shifts')
                .headers(httpHeaders -> httpHeaders.putAll(headers))
                .retrieve()
                .bodyToFlux(ShiftDto.class)
                .collectList()
                .block()

        then:
        response.size() == 2
        response.get(0).participantsNumberLimit == SHIFT_LIMIT_1
        DateHandler.toLocalDateTime(response.get(0).startingDate).withNano(0) == ONE_DAY_AGO.withNano(0)
        DateHandler.toLocalDateTime(response.get(0).endingDate).withNano(0) == IN_ONE_DAY.withNano(0)
        response.get(0).location == SHIFT_LOCATION_1
        response.get(1).participantsNumberLimit == SHIFT_LIMIT_2
        DateHandler.toLocalDateTime(response.get(1).startingDate).withNano(0) == ONE_DAY_AGO.withNano(0)
        DateHandler.toLocalDateTime(response.get(1).endingDate).withNano(0) == IN_ONE_DAY.withNano(0)
        response.get(1).location == SHIFT_LOCATION_2
    }

    def 'admin gets two shifts'() {
        given:
        demoAdminLogin()

        when:
        def response = webClient.get()
                .uri('/activities/' + activity.id + '/shifts')
                .headers(httpHeaders -> httpHeaders.putAll(headers))
                .retrieve()
                .bodyToFlux(ShiftDto.class)
                .collectList()
                .block()

        then:
        response.size() == 2
        response.get(0).participantsNumberLimit == SHIFT_LIMIT_1
        DateHandler.toLocalDateTime(response.get(0).startingDate).withNano(0) == ONE_DAY_AGO.withNano(0)
        DateHandler.toLocalDateTime(response.get(0).endingDate).withNano(0) == IN_ONE_DAY.withNano(0)
        response.get(0).location == SHIFT_LOCATION_1
        response.get(1).participantsNumberLimit == SHIFT_LIMIT_2
        DateHandler.toLocalDateTime(response.get(1).startingDate).withNano(0) == ONE_DAY_AGO.withNano(0)
        DateHandler.toLocalDateTime(response.get(1).endingDate).withNano(0) == IN_ONE_DAY.withNano(0)
        response.get(1).location == SHIFT_LOCATION_2
    }

    def 'activity does not exist'() {
        when:
        def response = webClient.get()
                .uri('/activities/' + 222 + '/shifts')
                .headers(httpHeaders -> httpHeaders.putAll(headers))
                .retrieve()
                .bodyToFlux(ShiftDto.class)
                .collectList()
                .block()

        then:
        def error = thrown(WebClientResponseException)
        error.statusCode == HttpStatus.BAD_REQUEST
    }
}
