package pt.ulisboa.tecnico.socialsoftware.humanaethica.enrollment.domain

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import pt.ulisboa.tecnico.socialsoftware.humanaethica.BeanConfiguration
import pt.ulisboa.tecnico.socialsoftware.humanaethica.SpockTest
import pt.ulisboa.tecnico.socialsoftware.humanaethica.exceptions.ErrorMessage
import pt.ulisboa.tecnico.socialsoftware.humanaethica.exceptions.HEException
import pt.ulisboa.tecnico.socialsoftware.humanaethica.activity.domain.Activity
import pt.ulisboa.tecnico.socialsoftware.humanaethica.enrollment.dto.EnrollmentDto
import pt.ulisboa.tecnico.socialsoftware.humanaethica.user.domain.Volunteer
import pt.ulisboa.tecnico.socialsoftware.humanaethica.shift.domain.Shift
import spock.lang.Unroll

import java.time.LocalDateTime

@DataJpaTest
class CreateEnrollmentMethodTest extends SpockTest {
    Activity activity = Mock()
    Activity otherActivity = Mock()
    Volunteer volunteer = Mock()
    Volunteer otherVolunteer = Mock()
    Enrollment otherEnrollment = Mock()
    Shift shift = Mock()
    Shift otherShift = Mock()
    def enrollmentDto

    def setup() {
        given: "enrollment info"
        enrollmentDto = new EnrollmentDto()
        enrollmentDto.motivation = ENROLLMENT_MOTIVATION_1
    }

    @Unroll
    def "create enrollment"() {
        given: "activity info"
        activity.getEnrollments() >> [otherEnrollment]
        activity.getApplicationDeadline() >> IN_ONE_DAY
        activity.getId() >> ACTIVITY_ID_1

        and: "shift infos"
        shift.getActivity() >> activity
        shift.getStartingDate() >> start
        shift.getEndingDate() >> end
        shift.getId() >> SHIFT_ID_1
        otherShift.getActivity() >> activity
        otherShift.getStartingDate() >> otherStart
        otherShift.getEndingDate() >> otherEnd
        otherShift.getId() >> SHIFT_ID_2

        and: "enrollment info"
        otherEnrollment.getVolunteer() >> otherVolunteer

        when:
        def result = new Enrollment(volunteer, [shift, otherShift], enrollmentDto)

        then: "checks results"
        result.motivation == ENROLLMENT_MOTIVATION_1
        result.enrollmentDateTime.isBefore(LocalDateTime.now())
        result.volunteer == volunteer
        result.getShifts().size() == 2

        and: "check that it is added"
        1 * volunteer.addEnrollment(_)
        1 * shift.addEnrollment(_)
        1 * otherShift.addEnrollment(_)

        where:
        start       | end         | otherStart  | otherEnd
        ONE_DAY_AGO | IN_ONE_DAY  | IN_ONE_DAY  | IN_TWO_DAYS
        IN_ONE_DAY  | IN_TWO_DAYS | ONE_DAY_AGO | IN_ONE_DAY
    }

    @Unroll
    def "create enrollment and violate motivation is required invariant: motivation=#motivation"() {
        given: "activity info"
        activity.getEnrollments() >> [otherEnrollment]
        activity.getApplicationDeadline() >> IN_ONE_DAY
        activity.getId() >> ACTIVITY_ID_1

        and: "shift info"
        shift.getActivity() >> activity

        and: "enrollment infos"
        enrollmentDto.motivation = motivation
        otherEnrollment.getVolunteer() >> otherVolunteer

        when:
        new Enrollment(volunteer, [shift], enrollmentDto)

        then:
        def error = thrown(HEException)
        error.getErrorMessage() == errorMessage

        where:
        motivation || errorMessage
        null       || ErrorMessage.ENROLLMENT_REQUIRES_MOTIVATION
        "   "      || ErrorMessage.ENROLLMENT_REQUIRES_MOTIVATION
        "< 10"     || ErrorMessage.ENROLLMENT_REQUIRES_MOTIVATION
    }

    def "create enrollment and violate enrollment before deadline invariant"() {
        given: "activity info"
        activity.getEnrollments() >> [otherEnrollment]
        activity.getApplicationDeadline() >> ONE_DAY_AGO
        activity.getId() >> ACTIVITY_ID_1

        and: "shift info"
        shift.getActivity() >> activity
        
        and: "enrollment infos"
        enrollmentDto.motivation = ENROLLMENT_MOTIVATION_1
        otherEnrollment.getVolunteer() >> otherVolunteer

        when:
        new Enrollment(volunteer, [shift], enrollmentDto)

        then:
        def error = thrown(HEException)
        error.getErrorMessage() == ErrorMessage.ENROLLMENT_AFTER_DEADLINE
    }

    def "create enrollment and violate enroll once invariant"() {
        given: "activity info"
        activity.getEnrollments() >> [otherEnrollment]
        activity.getApplicationDeadline() >> IN_ONE_DAY
        activity.getId() >> ACTIVITY_ID_1

        and: "shift info"
        shift.getActivity() >> activity
        
        and: "enrollment infos"
        enrollmentDto.motivation = ENROLLMENT_MOTIVATION_1
        otherEnrollment.getVolunteer() >> volunteer

        when:
        new Enrollment(volunteer, [shift], enrollmentDto)

        then:
        def error = thrown(HEException)
        error.getErrorMessage() == ErrorMessage.ENROLLMENT_VOLUNTEER_IS_ALREADY_ENROLLED
    }

    @Unroll
    def "create enrollment and violate all activities must be from the same activity invariant: id1=#id1 | id2=#id2"() {
        given: "activity infos"
        activity.getEnrollments() >> [otherEnrollment]
        activity.getApplicationDeadline() >> ONE_DAY_AGO
        activity.getId() >> ACTIVITY_ID_1
        otherActivity.getEnrollments() >> [otherEnrollment]
        otherActivity.getApplicationDeadline() >> ONE_DAY_AGO
        otherActivity.getId() >> ACTIVITY_ID_2
        

        and: "shift infos"
        shift.getActivity() >> activity
        otherShift.getActivity() >> otherActivity

        and: "enrollment info"
        enrollmentDto.motivation = ENROLLMENT_MOTIVATION_1
        otherEnrollment.getVolunteer() >> volunteer

        when:
        new Enrollment(volunteer, [shift, otherShift], enrollmentDto)

        then:
        def error = thrown(HEException)
        error.getErrorMessage() == ErrorMessage.ENROLLMENT_SHIFTS_DONT_MATCH
    }

    @Unroll
    def "create enrollment and violate no overlapping shifts invariant: start=#start | end=#end | otherStart=#otherStart | otherEnd=#otherEnd"() {
        given: "activity info"
        activity.getEnrollments() >> [otherEnrollment]
        activity.getApplicationDeadline() >> IN_ONE_DAY
        activity.getId() >> ACTIVITY_ID_1

        and: "shift infos"
        shift.getActivity() >> activity
        shift.getStartingDate() >> start
        shift.getEndingDate() >> end
        shift.getId() >> SHIFT_ID_1
        otherShift.getActivity() >> activity
        otherShift.getStartingDate() >> otherStart
        otherShift.getEndingDate() >> otherEnd
        otherShift.getId() >> SHIFT_ID_2

        and: "enrollment infos"
        enrollmentDto.motivation = ENROLLMENT_MOTIVATION_1

        when:
        new Enrollment(volunteer, [shift, otherShift], enrollmentDto)

        then:
        def error = thrown(HEException)
        error.getErrorMessage() == ErrorMessage.ENROLLMENT_SHIFTS_OVERLAP

        where:
        start       | end        | otherStart  | otherEnd
        ONE_DAY_AGO | IN_ONE_DAY | NOW         | IN_TWO_DAYS
        ONE_DAY_AGO | IN_ONE_DAY | ONE_DAY_AGO | IN_ONE_DAY
    }


    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfiguration {}
}