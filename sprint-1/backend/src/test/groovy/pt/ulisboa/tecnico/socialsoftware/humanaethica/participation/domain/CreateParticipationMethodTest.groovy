package pt.ulisboa.tecnico.socialsoftware.humanaethica.participation.domain

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import pt.ulisboa.tecnico.socialsoftware.humanaethica.BeanConfiguration
import pt.ulisboa.tecnico.socialsoftware.humanaethica.SpockTest
import pt.ulisboa.tecnico.socialsoftware.humanaethica.activity.domain.Activity
import pt.ulisboa.tecnico.socialsoftware.humanaethica.enrollment.domain.Enrollment
import pt.ulisboa.tecnico.socialsoftware.humanaethica.exceptions.ErrorMessage
import pt.ulisboa.tecnico.socialsoftware.humanaethica.exceptions.HEException
import pt.ulisboa.tecnico.socialsoftware.humanaethica.participation.dto.ParticipationDto
import pt.ulisboa.tecnico.socialsoftware.humanaethica.shift.domain.Shift
import pt.ulisboa.tecnico.socialsoftware.humanaethica.user.domain.Volunteer
import spock.lang.Unroll

import java.time.LocalDateTime


@DataJpaTest
class CreateParticipationMethodTest extends SpockTest {
    Activity activity = Mock()
    Volunteer volunteer = Mock()
    Volunteer otherVolunteer = Mock()
    Participation otherParticipation = Mock()
    Participation participation1 = Mock()
    Shift shift = Mock()
    Enrollment enrollment = Mock()
    def participationDto

    def setup() {
        given:
        participationDto = new ParticipationDto()
        shift.getActivity() >> activity
        enrollment.getVolunteer() >> volunteer
    }

    def "member creates a participation"() {
        given:
        participationDto.memberRating = 5
        participationDto.memberReview = MEMBER_REVIEW
        activity.getNumberOfParticipatingVolunteers() >> 2
        activity.getApplicationDeadline() >> TWO_DAYS_AGO
        activity.getEndingDate() >> ONE_DAY_AGO
        activity.getParticipantsNumberLimit() >> 3
        shift.getParticipantsNumberLimit() >> 3
        otherParticipation.getVolunteer() >> otherVolunteer
        shift.getParticipations() >> [otherParticipation]
        enrollment.getShifts() >> [shift]

        when:
        def result = new Participation(enrollment, shift, participationDto)

        then: "checks results"
        result.memberRating == 5
        result.memberReview ==  MEMBER_REVIEW
        result.acceptanceDate.isBefore(LocalDateTime.now())
        result.enrollment == enrollment
        result.shift == shift
        and: "check that it is added"
        1 * enrollment.setParticipation(_)
        1 * shift.addParticipation(_)
    }

    def "create participant and violate number of participants less or equal shift limit invariant"() {
        given:
        activity.getNumberOfParticipatingVolunteers() >> 1
        activity.getApplicationDeadline() >> TWO_DAYS_AGO
        activity.getEndingDate() >> ONE_DAY_AGO
        activity.getParticipantsNumberLimit() >> 1
        shift.getParticipantsNumberLimit() >> 1
        otherParticipation.getVolunteer() >> otherVolunteer
        shift.getParticipations() >> [otherParticipation, participation1]
        enrollment.getShifts() >> [shift]

        when:
        new Participation(enrollment, shift, participationDto)

        then:
        def error = thrown(HEException)
        error.getErrorMessage() == ErrorMessage.PARTICIPATION_IS_FULL
    }

    def "create participation and violate participate once invariant"() {
        given:
        activity.getNumberOfParticipatingVolunteers() >> 2
        activity.getApplicationDeadline() >> TWO_DAYS_AGO
        activity.getEndingDate() >> ONE_DAY_AGO
        activity.getParticipantsNumberLimit() >> 3
        shift.getParticipantsNumberLimit() >> 3
        otherParticipation.getVolunteer() >> volunteer
        participation1.getVolunteer() >> volunteer
        shift.getParticipations() >> [otherParticipation, participation1]
        enrollment.getShifts() >> [shift]

        when:
        new Participation(enrollment, shift, participationDto)

        then:
        def error = thrown(HEException)
        error.getErrorMessage() == ErrorMessage.PARTICIPATION_VOLUNTEER_IS_ALREADY_PARTICIPATING
    }

    def "create participation and violate acceptance after deadline invariant"() {
        given:
        activity.getNumberOfParticipatingVolunteers() >> 2
        activity.getApplicationDeadline() >> IN_ONE_DAY
        activity.getEndingDate() >> IN_TWO_DAYS
        activity.getParticipantsNumberLimit() >> 3
        shift.getParticipantsNumberLimit() >> 3
        otherParticipation.getVolunteer() >> otherVolunteer
        shift.getParticipations() >> [otherParticipation]
        enrollment.getShifts() >> [shift]
        and:
        participationDto.memberRating = null

        when:
        new Participation(enrollment, shift, participationDto)

        then:
        def error = thrown(HEException)
        error.getErrorMessage() == ErrorMessage.PARTICIPATION_ACCEPTANCE_BEFORE_DEADLINE
    }

    def "create participation and violate rating before end invariant"() {
        given:
        participationDto.memberReview = MEMBER_REVIEW
        participationDto.memberRating = 5
        activity.getNumberOfParticipatingVolunteers() >> 2
        activity.getApplicationDeadline() >> ONE_DAY_AGO
        activity.getEndingDate() >> IN_TWO_DAYS
        activity.getParticipantsNumberLimit() >> 3
        shift.getParticipantsNumberLimit() >> 3
        otherParticipation.getVolunteer() >> otherVolunteer
        shift.getParticipations() >> [otherParticipation]
        enrollment.getShifts() >> [shift]

        when:
        new Participation(enrollment, shift, participationDto)

        then:
        def error = thrown(HEException)
        error.getErrorMessage() == ErrorMessage.PARTICIPATION_RATING_BEFORE_END
    }

    def "create participant and violate number of participants less or equal limit invariant"() {
        given:
        activity.getNumberOfParticipatingVolunteers() >> 2
        activity.getApplicationDeadline() >> TWO_DAYS_AGO
        activity.getEndingDate() >> ONE_DAY_AGO
        activity.getParticipantsNumberLimit() >> 1
        shift.getParticipantsNumberLimit() >> 1
        otherParticipation.getVolunteer() >> otherVolunteer
        shift.getParticipations() >> [otherParticipation]
        enrollment.getShifts() >> [shift]

        when:
        new Participation(enrollment, shift, participationDto)

        then:
        def error = thrown(HEException)
        error.getErrorMessage() == ErrorMessage.PARTICIPATION_IS_FULL
    }

    @Unroll
    def "create participation and violate member rating in range 1..5: rating=#rating"() {
        given:
        activity.getNumberOfParticipatingVolunteers() >> 2
        activity.getApplicationDeadline() >> TWO_DAYS_AGO
        activity.getEndingDate() >> ONE_DAY_AGO
        activity.getParticipantsNumberLimit() >> 3
        shift.getParticipantsNumberLimit() >> 3
        otherParticipation.getVolunteer() >> otherVolunteer
        shift.getParticipations() >> [otherParticipation]
        enrollment.getShifts() >> [shift]
        and:
        participationDto.memberRating = rating

        when:
        new Participation(enrollment, shift, participationDto)

        then:
        def error = thrown(HEException)
        error.getErrorMessage() == ErrorMessage.PARTICIPATION_RATING_BETWEEN_ONE_AND_FIVE

        where:
        rating << [-5,0,6,20]
    }

    @Unroll
    def "create participation and violate volunteer rating in range 1..5: rating=#rating"() {
        given:
        activity.getNumberOfParticipatingVolunteers() >> 2
        activity.getApplicationDeadline() >> TWO_DAYS_AGO
        activity.getEndingDate() >> ONE_DAY_AGO
        activity.getParticipantsNumberLimit() >> 3
        shift.getParticipantsNumberLimit() >> 3
        otherParticipation.getVolunteer() >> otherVolunteer
        shift.getParticipations() >> [otherParticipation]
        enrollment.getShifts() >> [shift]
        and:
        participationDto.volunteerRating = rating

        when:
        new Participation(enrollment, shift, participationDto)

        then:
        def error = thrown(HEException)
        error.getErrorMessage() == ErrorMessage.PARTICIPATION_RATING_BETWEEN_ONE_AND_FIVE

        where:
        rating << [-5,0,6,20]
    }

    @Unroll
    def "create participation and violate volunteer review length: review=#review"() {
        given:
        activity.getNumberOfParticipatingVolunteers() >> 2
        activity.getApplicationDeadline() >> TWO_DAYS_AGO
        activity.getEndingDate() >> ONE_DAY_AGO
        activity.getParticipantsNumberLimit() >> 3
        shift.getParticipantsNumberLimit() >> 3
        otherParticipation.getVolunteer() >> otherVolunteer
        shift.getParticipations() >> [otherParticipation]
        enrollment.getShifts() >> [shift]
        and:
        participationDto.volunteerReview = review

        when:
        new Participation(enrollment, shift, participationDto)

        then:
        def error = thrown(HEException)
        error.getErrorMessage() == ErrorMessage.PARTICIPATION_REVIEW_LENGTH_INVALID

        where:
        review << ["", "123456789","a".repeat(MAX_REVIEW_LENGTH + 1)]
    }

    @Unroll
    def "create participation and violate member review length: review=#review"() {
        given:
        activity.getNumberOfParticipatingVolunteers() >> 2
        activity.getApplicationDeadline() >> TWO_DAYS_AGO
        activity.getEndingDate() >> ONE_DAY_AGO
        activity.getParticipantsNumberLimit() >> 3
        shift.getParticipantsNumberLimit() >> 3
        otherParticipation.getVolunteer() >> otherVolunteer
        shift.getParticipations() >> [otherParticipation]
        enrollment.getShifts() >> [shift]
        and:
        participationDto.memberReview = review

        when:
        new Participation(enrollment, shift, participationDto)

        then:
        def error = thrown(HEException)
        error.getErrorMessage() == ErrorMessage.PARTICIPATION_REVIEW_LENGTH_INVALID

        where:
        review << ["", "123456789","a".repeat(MAX_REVIEW_LENGTH + 1)]
    }

    def "create participation and violate participation's enrollment must contain participation's shift invariant"() {
        given: "activity info"
        activity.getNumberOfParticipatingVolunteers() >> 2
        activity.getApplicationDeadline() >> TWO_DAYS_AGO
        activity.getEndingDate() >> ONE_DAY_AGO
        activity.getParticipantsNumberLimit() >> 3

        and: "shift infos"
        shift.getParticipations() >> [otherParticipation]
        shift.getParticipantsNumberLimit() >> 3
        def otherShift = Mock(Shift)

        and: "enrollment info"
        enrollment.getShifts() >> [otherShift]

        when:
        new Participation(enrollment, shift, participationDto)

        then:
        def error = thrown(HEException)
        error.getErrorMessage() == ErrorMessage.PARTICIPATION_SHIFT_NOT_IN_ENROLLMENT
    }


    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfiguration {}
}