package pt.ulisboa.tecnico.socialsoftware.humanaethica.enrollment.domain

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import pt.ulisboa.tecnico.socialsoftware.humanaethica.BeanConfiguration
import pt.ulisboa.tecnico.socialsoftware.humanaethica.SpockTest
import pt.ulisboa.tecnico.socialsoftware.humanaethica.exceptions.ErrorMessage
import pt.ulisboa.tecnico.socialsoftware.humanaethica.exceptions.HEException
import pt.ulisboa.tecnico.socialsoftware.humanaethica.activity.dto.ActivityDto
import pt.ulisboa.tecnico.socialsoftware.humanaethica.activity.domain.Activity
import pt.ulisboa.tecnico.socialsoftware.humanaethica.enrollment.dto.EnrollmentDto
import pt.ulisboa.tecnico.socialsoftware.humanaethica.theme.domain.Theme
import pt.ulisboa.tecnico.socialsoftware.humanaethica.auth.domain.AuthUser
import pt.ulisboa.tecnico.socialsoftware.humanaethica.user.domain.User
import pt.ulisboa.tecnico.socialsoftware.humanaethica.user.domain.Volunteer
import pt.ulisboa.tecnico.socialsoftware.humanaethica.shift.domain.Shift
import pt.ulisboa.tecnico.socialsoftware.humanaethica.utils.DateHandler
import pt.ulisboa.tecnico.socialsoftware.humanaethica.institution.domain.Institution
import spock.lang.Unroll

import java.time.LocalDateTime

@DataJpaTest
class UpdateEnrollmentMethodTest extends SpockTest {
    Institution institution = Mock()
    Activity activity = Mock()
    Enrollment otherEnrollment = Mock()
    Theme theme = Mock()
    Shift shift = Mock()

    def enrollment
    def enrollmentDtoOne
    def enrollmentDtoEdit

    def activity2
    def enrollmentTwo
    def volunteer


    def setup() {
        given: "activity info"
        activity.getEnrollments() >> [otherEnrollment]
        activity.getApplicationDeadline() >> IN_TWO_DAYS
        activity.getId() >> ACTIVITY_ID_1

        and: "shift info"
        shift.getActivity() >> activity

        and: "volunteer info"
        volunteer = createVolunteer(USER_1_NAME, USER_1_PASSWORD, USER_1_EMAIL, AuthUser.Type.NORMAL, User.State.APPROVED)

        and: "enrollment info"
        enrollmentDtoOne = new EnrollmentDto()
        enrollmentDtoOne.motivation = ENROLLMENT_MOTIVATION_1
        enrollment = new Enrollment(volunteer, [shift], EnrollmentDtoOne)
        enrollmentDtoEdit = new EnrollmentDto()
    }


    def "update enrollment"() {
        given:
        enrollmentDtoEdit.motivation = ENROLLMENT_MOTIVATION_2

        when:
        enrollment.update(enrollmentDtoEdit)

        then: "checks results"
        enrollment.getMotivation() == ENROLLMENT_MOTIVATION_2
        enrollment.enrollmentDateTime.isBefore(LocalDateTime.now())
        enrollment.activity == activity
        enrollment.volunteer == volunteer
        
    }

    @Unroll
    def "edit enrollment and violate motivation is required invariant: motivation=#motivation"() {
      given:
      enrollmentDtoEdit.motivation = motivation

      when:
      enrollment.update(enrollmentDtoEdit)

      then:
      def error = thrown(HEException)
      error.getErrorMessage() == errorMessage

      where:
        motivation || errorMessage
        null       || ErrorMessage.ENROLLMENT_REQUIRES_MOTIVATION
        "   "      || ErrorMessage.ENROLLMENT_REQUIRES_MOTIVATION
        "< 10"     || ErrorMessage.ENROLLMENT_REQUIRES_MOTIVATION
    }

    def "try to update enrollment after deadline"() {
        given: "misc infos"
        institution.getActivities() >> []
        theme.getState() >> Theme.State.APPROVED

        and: "activity infos"
        def activityDtoTwo
        def themes = [theme]
        activityDtoTwo = new ActivityDto()
        activityDtoTwo.name = ACTIVITY_NAME_1
        activityDtoTwo.region = ACTIVITY_REGION_1
        activityDtoTwo.participantsNumberLimit = 2
        activityDtoTwo.description = ACTIVITY_DESCRIPTION_1
        activityDtoTwo.startingDate = DateHandler.toISOString(IN_TWO_DAYS)
        activityDtoTwo.endingDate = DateHandler.toISOString(IN_THREE_DAYS)
        activityDtoTwo.applicationDeadline = DateHandler.toISOString(IN_ONE_DAY)
        activity2 = new Activity(activityDtoTwo, institution, themes)
        activity2.@id = ACTIVITY_ID_2
        enrollmentDtoEdit.motivation = ENROLLMENT_MOTIVATION_2

        and: "shift info"
        def shiftTwo = Mock(Shift)
        shiftTwo.getActivity() >> activity2
        shiftTwo.getId() >> SHIFT_ID_2

        and: "enrollment info"
        def enrollmentDtoTwo = new EnrollmentDto()
        enrollmentDtoTwo.motivation = ENROLLMENT_MOTIVATION_1
        enrollmentTwo = new Enrollment(volunteer, [shiftTwo], enrollmentDtoTwo)

        and: "change deadline"
        activity2.setApplicationDeadline(ONE_DAY_AGO)

        when:
        enrollmentTwo.update(enrollmentDtoEdit)

        then:
        def error = thrown(HEException)
        error.getErrorMessage() == ErrorMessage.ENROLLMENT_AFTER_DEADLINE
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfiguration {}
}