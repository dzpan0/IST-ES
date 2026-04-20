package pt.ulisboa.tecnico.socialsoftware.humanaethica.participation.webservice

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
import pt.ulisboa.tecnico.socialsoftware.humanaethica.enrollment.domain.Enrollment
import pt.ulisboa.tecnico.socialsoftware.humanaethica.enrollment.dto.EnrollmentDto
import pt.ulisboa.tecnico.socialsoftware.humanaethica.institution.domain.Institution
import pt.ulisboa.tecnico.socialsoftware.humanaethica.participation.dto.ParticipationDto
import pt.ulisboa.tecnico.socialsoftware.humanaethica.user.domain.User

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GetParticipationsByActivityWebServiceIT extends SpockTest {
    @LocalServerPort
    private int port

    def activity

    def setup() {
        deleteAll()

        webClient = WebClient.create("http://localhost:" + port)
        headers = new HttpHeaders()
        headers.setContentType(MediaType.APPLICATION_JSON)

        def institution = institutionService.getDemoInstitution()

        def activityDto = createActivityDto(ACTIVITY_NAME_1,ACTIVITY_REGION_1,3,ACTIVITY_DESCRIPTION_1,
                TWO_DAYS_AGO, ONE_DAY_AGO, NOW,null)

        activity = new Activity(activityDto, institution, new ArrayList<>())
        activityRepository.save(activity)

        def shift = createShift(activity, SHIFT_LIMIT_3, ONE_DAY_AGO.plusHours(1), NOW.minusHours(1), SHIFT_LOCATION_1)

        def volunteerOne = createVolunteer(USER_1_NAME, USER_1_USERNAME, USER_1_EMAIL, AuthUser.Type.NORMAL, User.State.APPROVED)
        def volunteerTwo = createVolunteer(USER_2_NAME, USER_2_USERNAME, USER_2_EMAIL, AuthUser.Type.NORMAL, User.State.APPROVED)

        def enrollment1 = new Enrollment()
        enrollment1.setEnrollmentDateTime(TWO_DAYS_AGO.minusHours(1))
        enrollment1.setVolunteer(volunteerOne);
        enrollment1.setShifts([shift])
        enrollmentRepository.save(enrollment1)
        def enrollment2 = new Enrollment()
        enrollment2.setEnrollmentDateTime(TWO_DAYS_AGO.minusHours(1))
        enrollment2.setVolunteer(volunteerTwo);
        enrollment2.setShifts([shift])
        enrollmentRepository.save(enrollment2)

        def participationDto1 = new ParticipationDto()
        participationDto1.memberRating = 1
        participationDto1.memberReview = MEMBER_REVIEW
        participationDto1.volunteerRating = 5
        participationDto1.volunteerReview = VOLUNTEER_REVIEW
        def participationDto2 = new ParticipationDto()
        participationDto2.memberRating = 2
        participationDto2.memberReview = MEMBER_REVIEW
        participationDto2.volunteerRating = 5
        participationDto2.volunteerReview = VOLUNTEER_REVIEW

        and:
        participationService.createParticipation(shift.id, enrollment1.id, participationDto1)
        participationService.createParticipation(shift.id, enrollment2.id, participationDto2)
    }

    def 'member gets two participations'() {
        given:
        demoMemberLogin()

        when:
        def response = webClient.get()
                .uri('/activities/' + activity.id + '/participations')
                .headers(httpHeaders -> httpHeaders.putAll(headers))
                .retrieve()
                .bodyToFlux(ParticipationDto.class)
                .collectList()
                .block()

        then:
        response.size() == 2
        response.get(0).memberRating == 1
        response.get(1).memberRating == 2
    }

    def 'member of another institution cannot get participations'() {
        given:
        def otherInstitution = new Institution(INSTITUTION_1_NAME, INSTITUTION_1_EMAIL, INSTITUTION_1_NIF)
        institutionRepository.save(otherInstitution)
        createMember(USER_3_NAME,USER_3_USERNAME,USER_3_PASSWORD,USER_3_EMAIL, AuthUser.Type.NORMAL, otherInstitution, User.State.APPROVED)
        normalUserLogin(USER_3_USERNAME, USER_3_PASSWORD)

        when:
       webClient.get()
                .uri('/activities/' + activity.id + '/participations')
                .headers(httpHeaders -> httpHeaders.putAll(headers))
                .retrieve()
                .bodyToFlux(ParticipationDto.class)
                .collectList()
                .block()

        then:
        def error = thrown(WebClientResponseException)
        error.statusCode == HttpStatus.FORBIDDEN
    }

    def 'volunteer cannot get participations'() {
        given:
        demoVolunteerLogin()

        when:
        webClient.get()
                .uri('/activities/' + activity.id + '/participations')
                .headers(httpHeaders -> httpHeaders.putAll(headers))
                .retrieve()
                .bodyToFlux(ParticipationDto.class)
                .collectList()
                .block()

        then:
        def error = thrown(WebClientResponseException)
        error.statusCode == HttpStatus.FORBIDDEN
    }

    def 'admin cannot get participations'() {
        given:
        demoAdminLogin()

        when:
        def response = webClient.get()
                .uri('/activities/' + activity.id + '/participations')
                .headers(httpHeaders -> httpHeaders.putAll(headers))
                .retrieve()
                .bodyToFlux(ParticipationDto.class)
                .collectList()
                .block()

        then:
        def error = thrown(WebClientResponseException)
        error.statusCode == HttpStatus.FORBIDDEN
    }

    def 'activity does not exist'() {
        given:
        demoMemberLogin()

        when:
        def response = webClient.get()
                .uri('/activities/' + 222 + '/participations')
                .headers(httpHeaders -> httpHeaders.putAll(headers))
                .retrieve()
                .bodyToFlux(ParticipationDto.class)
                .collectList()
                .block()

        then:
        def error = thrown(WebClientResponseException)
        error.statusCode == HttpStatus.FORBIDDEN
    }
}
