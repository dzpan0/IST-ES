package pt.ulisboa.tecnico.socialsoftware.humanaethica.enrollment.webservice

import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import pt.ulisboa.tecnico.socialsoftware.humanaethica.SpockTest
import pt.ulisboa.tecnico.socialsoftware.humanaethica.activity.domain.Activity
import pt.ulisboa.tecnico.socialsoftware.humanaethica.enrollment.dto.EnrollmentDto
import pt.ulisboa.tecnico.socialsoftware.humanaethica.shift.dto.ShiftDto

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CreateEnrollmentWebServiceIT extends SpockTest {
    @LocalServerPort
    private int port

    def shiftDto
    def activity
    def enrollmentDto

    def setup() {
        deleteAll()

        and: "setup web"
        webClient = WebClient.create("http://localhost:" + port)
        headers = new HttpHeaders()
        headers.setContentType(MediaType.APPLICATION_JSON)

        and: "setup institution"
        def institution = institutionService.getDemoInstitution()

        and: "setup activity"
        def activityDto = createActivityDto(ACTIVITY_NAME_1,ACTIVITY_REGION_1,1,ACTIVITY_DESCRIPTION_1,IN_ONE_DAY, IN_TWO_DAYS,IN_THREE_DAYS,null)
        activity = new Activity(activityDto, institution, new ArrayList<>())
        activityRepository.save(activity)

        and: "setup shift"
        def shift = createShift(activity, SHIFT_LIMIT_1, IN_TWO_DAYS.plusHours(1), IN_THREE_DAYS.minusHours(1), SHIFT_LOCATION_1)
        shiftDto = new ShiftDto(shift)

        and: "setup enrollment dto"
        enrollmentDto = new EnrollmentDto()
        enrollmentDto.motivation = ENROLLMENT_MOTIVATION_1
        enrollmentDto.shifts = [shiftDto]
    }

    def 'volunteer create enrollment'() {
        given:
        demoVolunteerLogin()

        when:
        def response = webClient.post()
                .uri('/activities/' + activity.id + '/enrollments')
                .headers(httpHeaders -> httpHeaders.putAll(headers))
                .bodyValue(enrollmentDto)
                .retrieve()
                .bodyToMono(EnrollmentDto.class)
                .block()

        then:
        response.motivation == ENROLLMENT_MOTIVATION_1
        and:
        enrollmentRepository.getEnrollmentsByActivityId(activity.id).size() == 1
        def storedEnrollment = enrollmentRepository.getEnrollmentsByActivityId(activity.id).get(0)
        storedEnrollment.motivation == ENROLLMENT_MOTIVATION_1

        cleanup:
        deleteAll()
    }

    def 'volunteer create enrollment with error'() {
        given:
        demoVolunteerLogin()
        and:
        enrollmentDto.motivation = null

        when:
        def response = webClient.post()
                .uri('/activities/' + activity.id + '/enrollments')
                .headers(httpHeaders -> httpHeaders.putAll(headers))
                .bodyValue(enrollmentDto)
                .retrieve()
                .bodyToMono(EnrollmentDto.class)
                .block()

        then:
        def error = thrown(WebClientResponseException)
        error.statusCode == HttpStatus.BAD_REQUEST
        enrollmentRepository.count() == 0

        cleanup:
        deleteAll()
    }

    def 'member cannot create enrollment'() {
        given:
        demoMemberLogin()

        when:
        def response = webClient.post()
                .uri('/activities/' + activity.id + '/enrollments')
                .headers(httpHeaders -> httpHeaders.putAll(headers))
                .bodyValue(enrollmentDto)
                .retrieve()
                .bodyToMono(EnrollmentDto.class)
                .block()

        then:
        def error = thrown(WebClientResponseException)
        error.statusCode == HttpStatus.FORBIDDEN
        enrollmentRepository.count() == 0

        cleanup:
        deleteAll()
    }

    def 'admin cannot create enrollment'() {
        given:
        demoAdminLogin()

        when:
        def response = webClient.post()
                .uri('/activities/' + activity.id + '/enrollments')
                .headers(httpHeaders -> httpHeaders.putAll(headers))
                .bodyValue(enrollmentDto)
                .retrieve()
                .bodyToMono(EnrollmentDto.class)
                .block()

        then:
        def error = thrown(WebClientResponseException)
        error.statusCode == HttpStatus.FORBIDDEN
        enrollmentRepository.count() == 0

        cleanup:
        deleteAll()
    }
}
