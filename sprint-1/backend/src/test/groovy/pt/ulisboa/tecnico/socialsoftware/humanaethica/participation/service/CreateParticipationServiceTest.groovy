package pt.ulisboa.tecnico.socialsoftware.humanaethica.participation.service

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import pt.ulisboa.tecnico.socialsoftware.humanaethica.BeanConfiguration
import pt.ulisboa.tecnico.socialsoftware.humanaethica.SpockTest
import pt.ulisboa.tecnico.socialsoftware.humanaethica.activity.domain.Activity
import pt.ulisboa.tecnico.socialsoftware.humanaethica.auth.domain.AuthUser
import pt.ulisboa.tecnico.socialsoftware.humanaethica.enrollment.domain.Enrollment
import pt.ulisboa.tecnico.socialsoftware.humanaethica.participation.dto.ParticipationDto
import pt.ulisboa.tecnico.socialsoftware.humanaethica.exceptions.ErrorMessage
import pt.ulisboa.tecnico.socialsoftware.humanaethica.exceptions.HEException
import pt.ulisboa.tecnico.socialsoftware.humanaethica.user.domain.User
import spock.lang.Unroll

import java.time.LocalDateTime

@DataJpaTest
class CreateParticipationServiceTest extends SpockTest {
    public static final String EXIST = 'exist'
    public static final String NO_EXIST = 'noExist'
    def volunteer
    def member
    def activity
    def shift
    def enrollment

    def setup() {
        def institution = institutionService.getDemoInstitution()
        volunteer = createVolunteer(USER_1_NAME, USER_1_USERNAME, USER_1_EMAIL, AuthUser.Type.DEMO, User.State.ACTIVE)
        member = authUserService.loginDemoMemberAuth().getUser()

        def activityDto = createActivityDto(ACTIVITY_NAME_1,ACTIVITY_REGION_1,3,ACTIVITY_DESCRIPTION_1,
                TWO_DAYS_AGO, ONE_DAY_AGO, NOW, null)

        activity = new Activity(activityDto, institution, new ArrayList<>())
        activityRepository.save(activity)
        shift = createShift(activity, SHIFT_LIMIT_3, ONE_DAY_AGO.plusHours(1), NOW.minusHours(1), SHIFT_LOCATION_1)
        enrollment = new Enrollment()
        enrollment.setEnrollmentDateTime(TWO_DAYS_AGO.minusHours(1))
        enrollment.setVolunteer(volunteer);
        enrollment.setShifts([shift])
        enrollmentRepository.save(enrollment)
    }

    def 'create participation as member' () {
        given:  
        def participationDto = new ParticipationDto()
        participationDto.memberRating = 5
        participationDto.memberReview = MEMBER_REVIEW
        participationDto.shiftId = shift.id

        when:
        def result = participationService.createParticipation(shift.id, enrollment.id, participationDto)

        then:
        result.memberRating == 5
        result.memberReview == MEMBER_REVIEW
        result.enrollmentId == enrollment.id
        and:
        participationRepository.findAll().size() == 1
        def storedParticipation = participationRepository.findAll().get(0)
        storedParticipation.memberRating == 5
        storedParticipation.memberReview == MEMBER_REVIEW
        storedParticipation.acceptanceDate.isBefore(LocalDateTime.now())
        storedParticipation.shift.id == shift.id
        storedParticipation.enrollment.id == enrollment.id
    }


    @Unroll
    def 'invalid arguments: shiftId=#shiftId | enrollmentId=#enrollmentId'() {
        given:
        def participationDto = new ParticipationDto()
        participationDto.memberRating = 5
        participationDto.memberReview = MEMBER_REVIEW

        when:
        participationService.createParticipation(getShiftId(shiftId), getEnrollmentId(enrollmentId), getParticipationDto(participationValue,participationDto))

        then:
        def error = thrown(HEException)
        error.getErrorMessage() == errorMessage
        and:
        participationRepository.findAll().size() == 0

        where:
        shiftId  | enrollmentId | participationValue || errorMessage
        null     | EXIST        | EXIST              || ErrorMessage.SHIFT_NOT_FOUND
        NO_EXIST | EXIST        | EXIST              || ErrorMessage.SHIFT_NOT_FOUND
        EXIST    | NO_EXIST     | EXIST              || ErrorMessage.ENROLLMENT_NOT_FOUND
        EXIST    | null         | EXIST              || ErrorMessage.ENROLLMENT_NOT_FOUND
        EXIST    | EXIST        | null               || ErrorMessage.PARTICIPATION_REQUIRES_INFORMATION
    }

    @Unroll
    def 'invalid arguments: rating=#rating | review=#review'() {
        given:
        def participationDto = new ParticipationDto()
        participationDto.volunteerReview = review
        participationDto.volunteerRating = rating

        when:
        participationService.createParticipation(shift.id, enrollment.id, participationDto)

        then:
        def error = thrown(HEException)
        error.getErrorMessage() == errorMessage
        and:
        participationRepository.findAll().size() == 0

        where:
        review                              | rating || errorMessage
        "A".repeat(MAX_REVIEW_LENGTH + 1)   | 5      || ErrorMessage.PARTICIPATION_REVIEW_LENGTH_INVALID
        ""                                  | 5      || ErrorMessage.PARTICIPATION_REVIEW_LENGTH_INVALID
        VOLUNTEER_REVIEW                    | -1     || ErrorMessage.PARTICIPATION_RATING_BETWEEN_ONE_AND_FIVE
        VOLUNTEER_REVIEW                    | 10     || ErrorMessage.PARTICIPATION_RATING_BETWEEN_ONE_AND_FIVE
    }

    def getShiftId(shiftId) {
        if (shiftId == EXIST)
            return shift.id
        else if (shiftId == NO_EXIST)
            return 222
        else
            return null
    }

    def getEnrollmentId(enrollmentId) {
        if (enrollmentId == EXIST)
            return enrollment.id
        else if (enrollmentId == NO_EXIST)
            return 222
        else
            return null
    }

    def getParticipationDto(value, participationDto) {
        if (value == EXIST) {
            return participationDto
        }
        return null
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfiguration {}
}
