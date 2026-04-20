package pt.ulisboa.tecnico.socialsoftware.humanaethica.participation.service

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import pt.ulisboa.tecnico.socialsoftware.humanaethica.BeanConfiguration
import pt.ulisboa.tecnico.socialsoftware.humanaethica.SpockTest
import pt.ulisboa.tecnico.socialsoftware.humanaethica.activity.domain.Activity
import pt.ulisboa.tecnico.socialsoftware.humanaethica.auth.domain.AuthUser
import pt.ulisboa.tecnico.socialsoftware.humanaethica.enrollment.domain.Enrollment
import pt.ulisboa.tecnico.socialsoftware.humanaethica.exceptions.ErrorMessage
import pt.ulisboa.tecnico.socialsoftware.humanaethica.exceptions.HEException
import pt.ulisboa.tecnico.socialsoftware.humanaethica.participation.dto.ParticipationDto
import pt.ulisboa.tecnico.socialsoftware.humanaethica.user.domain.User

@DataJpaTest
class GetParticipationsByActivityServiceTest extends SpockTest {
    def activity
    def otherActivity
    def participationDto1
    def participationDto2
    def shift
    def otherShift
    def enrollment1
    def enrollment2
    def otherEnrollment

    def setup() {
        def institution = institutionService.getDemoInstitution()

        def activityDto = createActivityDto(ACTIVITY_NAME_1,ACTIVITY_REGION_1,3,ACTIVITY_DESCRIPTION_1,
                TWO_DAYS_AGO, ONE_DAY_AGO, NOW,null)

        activity = new Activity(activityDto, institution, new ArrayList<>())
        activityRepository.save(activity)

        activityDto.name = ACTIVITY_NAME_2
        otherActivity = new Activity(activityDto, institution, new ArrayList<>())
        activityRepository.save(otherActivity)

        participationDto1 = new ParticipationDto()
        participationDto1.memberRating = 1
        participationDto1.memberReview = MEMBER_REVIEW
        participationDto2 = new ParticipationDto()
        participationDto2.memberRating = 2
        participationDto2.memberReview  = MEMBER_REVIEW

        shift = createShift(activity, SHIFT_LIMIT_3, ONE_DAY_AGO.plusHours(1), NOW.minusHours(1), SHIFT_LOCATION_1)
        otherShift = createShift(otherActivity, SHIFT_LIMIT_3, ONE_DAY_AGO.plusHours(2), NOW.minusHours(2), SHIFT_LOCATION_1)

        def volunteer1 = createVolunteer(USER_1_NAME, USER_1_PASSWORD, USER_1_EMAIL, AuthUser.Type.NORMAL, User.State.APPROVED)
        def volunteer2 = createVolunteer(USER_2_NAME, USER_2_PASSWORD, USER_2_EMAIL, AuthUser.Type.NORMAL, User.State.APPROVED)
        def volunteer3 = createVolunteer(USER_3_NAME, USER_3_PASSWORD, USER_3_EMAIL, AuthUser.Type.NORMAL, User.State.APPROVED)
        enrollment1 = new Enrollment()
        enrollment1.setEnrollmentDateTime(TWO_DAYS_AGO.minusHours(1))
        enrollment1.setVolunteer(volunteer1);
        enrollment1.setShifts([shift])
        enrollmentRepository.save(enrollment1)
        enrollment2 = new Enrollment()
        enrollment2.setEnrollmentDateTime(TWO_DAYS_AGO.minusHours(1))
        enrollment2.setVolunteer(volunteer2);
        enrollment2.setShifts([shift])
        enrollmentRepository.save(enrollment2)
        otherEnrollment = new Enrollment()
        otherEnrollment.setEnrollmentDateTime(TWO_DAYS_AGO.minusHours(1))
        otherEnrollment.setVolunteer(volunteer3);
        otherEnrollment.setShifts([otherShift])
        enrollmentRepository.save(otherEnrollment)
    }

    def "get two participations of the same activity"() {
        given:
        def volunteerOne = createVolunteer(USER_1_NAME, USER_1_USERNAME, USER_1_EMAIL, AuthUser.Type.NORMAL, User.State.APPROVED)
        def volunteerTwo = createVolunteer(USER_2_NAME, USER_2_USERNAME, USER_2_EMAIL, AuthUser.Type.NORMAL, User.State.APPROVED)
        and:
        createParticipation(volunteerOne, enrollment1, shift, participationDto1)
        createParticipation(volunteerTwo, enrollment2, shift, participationDto2)

        when:
        def participations = participationService.getParticipationsByActivity(activity.id)

        then:
        participations.size() == 2
        participations.get(0).memberRating == 1
        participations.get(1).memberRating == 2
    }

    def "get one participation of an activity"() {
        given:
        def volunteer = createVolunteer(USER_1_NAME, USER_1_USERNAME, USER_1_EMAIL, AuthUser.Type.NORMAL, User.State.APPROVED)
        and:
        createParticipation(volunteer, enrollment1, shift, participationDto1)
        createParticipation(volunteer, otherEnrollment, otherShift, participationDto1)

        when:
        def participations = participationService.getParticipationsByActivity(activity.id)

        then:
        participations.size() == 1
        participations.get(0).memberRating == 1
    }

    def "activity does not exist or is null: activityId=#activityId"() {
        when:
        participationService.getParticipationsByActivity(activityId)

        then:
        def error = thrown(HEException)
        error.getErrorMessage() == errorMessage

        where:
        activityId || errorMessage
        null       || ErrorMessage.ACTIVITY_NOT_FOUND
        222        || ErrorMessage.ACTIVITY_NOT_FOUND
    }



    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfiguration {}
}
