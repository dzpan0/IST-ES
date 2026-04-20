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
import pt.ulisboa.tecnico.socialsoftware.humanaethica.enrollment.dto.EnrollmentDto
import pt.ulisboa.tecnico.socialsoftware.humanaethica.participation.dto.ParticipationDto
import pt.ulisboa.tecnico.socialsoftware.humanaethica.shift.dto.ShiftDto

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CreateParticipationWebServiceIT extends SpockTest {
    @LocalServerPort
    private int port

    def activity
    def shift
    def participationDtoMember
    def participationDtoVolunteer
    def enrollmentDto

    def setup() {
        deleteAll()

        webClient = WebClient.create("http://localhost:" + port)
        headers = new HttpHeaders()
        headers.setContentType(MediaType.APPLICATION_JSON)

        def institution = institutionService.getDemoInstitution()

        def activityDto = createActivityDto(ACTIVITY_NAME_1, ACTIVITY_REGION_1, 3, ACTIVITY_DESCRIPTION_1,
                NOW.plusDays(1), NOW.plusDays(2), NOW.plusDays(3), null)

        activity = new Activity(activityDto, institution, new ArrayList<>())
        activityRepository.save(activity)

        def volunteer = authUserService.loginDemoVolunteerAuth().getUser()

        shift = createShift(activity, SHIFT_LIMIT_3, IN_TWO_DAYS.plusHours(1), IN_THREE_DAYS.minusHours(1), SHIFT_LOCATION_1)
        def shiftDto = new ShiftDto(shift)

        enrollmentDto = new EnrollmentDto()
        enrollmentDto.volunteerId = volunteer.getId()
        enrollmentDto.motivation = ENROLLMENT_MOTIVATION_1
        enrollmentDto.shifts = [shiftDto]

        enrollmentDto = enrollmentService.createEnrollment(volunteer.id, enrollmentDto)

        activity.setStartingDate(NOW.minusDays(4))
        activity.setEndingDate(NOW.minusDays(3))
        activity.setApplicationDeadline(NOW.minusDays(5))
        activityRepository.save(activity)

        participationDtoMember = new ParticipationDto()
        participationDtoMember.memberRating = 5
        participationDtoMember.memberReview = MEMBER_REVIEW
        participationDtoMember.shiftId = shift.id
        participationDtoMember.enrollmentId = enrollmentDto.id

        participationDtoVolunteer = new ParticipationDto()
        participationDtoVolunteer.volunteerRating = 5
        participationDtoVolunteer.volunteerReview = VOLUNTEER_REVIEW
        participationDtoVolunteer.shiftId = shift.id
        participationDtoVolunteer.enrollmentId = enrollmentDto.id
    }

    def 'member create participation'() {
        given:
        demoMemberLogin()

        when:
        def response = webClient.post()
                .uri('/activities/' + shift.id + '/' + enrollmentDto.id + '/participations')
                .headers(httpHeaders -> httpHeaders.putAll(headers))
                .bodyValue(participationDtoMember)
                .retrieve()
                .bodyToMono(ParticipationDto.class)
                .block()

        then:
        response.memberRating == 5
        response.memberReview == MEMBER_REVIEW
        response.shiftId == shift.id
        response.enrollmentId == enrollmentDto.id
        and:
        participationRepository.getParticipationsByActivityId(activity.id).size() == 1
        def storedParticipant = participationRepository.getParticipationsByActivityId(activity.id).get(0)
        storedParticipant.memberRating == 5
        storedParticipant.memberReview == MEMBER_REVIEW
        storedParticipant.shift.id == shift.id
        storedParticipant.enrollment.id == enrollmentDto.id


        cleanup:
        deleteAll()
    }

    def 'member create participation with error'() {
        given:
        demoMemberLogin()
        and:
        participationDtoMember.memberRating = 10

        when:
        webClient.post()
                .uri('/activities/' + shift.id + '/' + enrollmentDto.id + '/participations')
                .headers(httpHeaders -> httpHeaders.putAll(headers))
                .bodyValue(participationDtoMember)
                .retrieve()
                .bodyToMono(ParticipationDto.class)
                .block()

        then:
        def error = thrown(WebClientResponseException)
        error.statusCode == HttpStatus.BAD_REQUEST
        participationRepository.count() == 0

        cleanup:
        deleteAll()
    }

    def 'admin cannot create participation'() {
        given:
        demoAdminLogin()

        when:
        webClient.post()
                .uri('/activities/' + shift.id + '/' + enrollmentDto.id + '/participations')
                .headers(httpHeaders -> httpHeaders.putAll(headers))
                .bodyValue(participationDtoMember)
                .retrieve()
                .bodyToMono(ParticipationDto.class)
                .block()

        then:
        def error = thrown(WebClientResponseException)
        error.statusCode == HttpStatus.FORBIDDEN
        participationRepository.count() == 0

        cleanup:
        deleteAll()
    }

    def 'volunteer cannot create participation'() {
        given:
        demoVolunteerLogin()

        when:
        webClient.post()
                .uri('/activities/' + shift.id + '/' + enrollmentDto.id + '/participations')
                .headers(httpHeaders -> httpHeaders.putAll(headers))
                .bodyValue(participationDtoMember)
                .retrieve()
                .bodyToMono(ParticipationDto.class)
                .block()

        then:
        def error = thrown(WebClientResponseException)
        error.statusCode == HttpStatus.FORBIDDEN
        participationRepository.count() == 0

        cleanup:
        deleteAll()
    }
}
