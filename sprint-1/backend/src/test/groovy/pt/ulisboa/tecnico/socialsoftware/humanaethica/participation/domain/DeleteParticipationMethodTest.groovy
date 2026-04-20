package pt.ulisboa.tecnico.socialsoftware.humanaethica.participation.domain

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import pt.ulisboa.tecnico.socialsoftware.humanaethica.BeanConfiguration
import pt.ulisboa.tecnico.socialsoftware.humanaethica.SpockTest
import pt.ulisboa.tecnico.socialsoftware.humanaethica.activity.domain.Activity
import pt.ulisboa.tecnico.socialsoftware.humanaethica.activity.dto.ActivityDto
import pt.ulisboa.tecnico.socialsoftware.humanaethica.auth.domain.AuthUser
import pt.ulisboa.tecnico.socialsoftware.humanaethica.enrollment.domain.Enrollment
import pt.ulisboa.tecnico.socialsoftware.humanaethica.institution.domain.Institution
import pt.ulisboa.tecnico.socialsoftware.humanaethica.participation.dto.ParticipationDto
import pt.ulisboa.tecnico.socialsoftware.humanaethica.shift.domain.Shift
import pt.ulisboa.tecnico.socialsoftware.humanaethica.shift.dto.ShiftDto
import pt.ulisboa.tecnico.socialsoftware.humanaethica.theme.domain.Theme
import pt.ulisboa.tecnico.socialsoftware.humanaethica.user.domain.User
import pt.ulisboa.tecnico.socialsoftware.humanaethica.utils.DateHandler

import java.time.LocalDateTime

@DataJpaTest
class DeleteParticipationMethodTest extends SpockTest {
    Institution institution = Mock()
    Theme theme = Mock()
    Activity otherActivity = Mock()
    Enrollment enrollment = Mock()
    Enrollment otherEnrollment = Mock()

    def activity
    def volunteer
    def participation
    def otherParticipation
    def participationDto
    def shift

    def setup() {
        otherActivity.getName() >> ACTIVITY_NAME_2
        theme.getState() >> Theme.State.APPROVED
        institution.getActivities() >> [otherActivity]
        given: "an activity"
        def themes = [theme]
        def activityDto
        activityDto = new ActivityDto()
        activityDto.name = ACTIVITY_NAME_1
        activityDto.region = ACTIVITY_REGION_1
        activityDto.participantsNumberLimit = 2
        activityDto.description = ACTIVITY_DESCRIPTION_1
        activityDto.startingDate = DateHandler.toISOString(TWO_DAYS_AGO)
        activityDto.endingDate = DateHandler.toISOString(ONE_DAY_AGO)
        activityDto.applicationDeadline = DateHandler.toISOString(LocalDateTime.now().minusDays(3))
        activity = new Activity(activityDto, institution, themes)
        and: "a shift"
        def shiftDto
        shiftDto = new ShiftDto()
        shiftDto.participantsNumberLimit = SHIFT_LIMIT_2
        shiftDto.startingDate = DateHandler.toISOString(TWO_DAYS_AGO.plusHours(1))
        shiftDto.endingDate = DateHandler.toISOString(ONE_DAY_AGO.minusHours(1))
        shiftDto.location = SHIFT_LOCATION_1
        shift = new Shift(activity, shiftDto)
        and: "a volunteer"
        volunteer = createVolunteer(USER_1_NAME, USER_1_PASSWORD, USER_1_EMAIL, AuthUser.Type.NORMAL, User.State.APPROVED)
        enrollment.getVolunteer() >> volunteer
        and: "a participation"
        participationDto = new ParticipationDto()
        participationDto.memberRating = 4
        participationDto.memberReview= MEMBER_REVIEW
        participationDto.volunteerRating= 5
        participationDto.volunteerReview= VOLUNTEER_REVIEW
        and: "a enrollment"
        enrollment.getShifts() >> [shift]

        participation = new Participation(enrollment, shift, participationDto)
    }

    def "delete participation"() {
        when: "a participation is deleted"
        participation.delete()

        then: "checks results"
        shift.getParticipations().size() == 0
    }

    def "delete one of multiple participations in activity"() {
        given: "another participation for the same activity"
        def otherVolunteer = createVolunteer(USER_2_NAME, USER_2_PASSWORD, USER_2_EMAIL, AuthUser.Type.NORMAL, User.State.APPROVED)
        otherEnrollment.getVolunteer() >> otherVolunteer
        otherEnrollment.getShifts() >> [shift]
        otherParticipation = new Participation(otherEnrollment, shift, participationDto)

        when: "one participation is deleted"
        participation.delete()

        then: "the other participation remains"
        shift.getParticipations().size() == 1
        shift.getParticipations().contains(otherParticipation)
    }

    def "delete one of multiple participations in volunteer"() {
        given: "another participation for the same volunteer"
        def themes = [theme]
        def activityDto
        activityDto = new ActivityDto()
        activityDto.name = ACTIVITY_NAME_1
        activityDto.region = ACTIVITY_REGION_1
        activityDto.participantsNumberLimit = 2
        activityDto.description = ACTIVITY_DESCRIPTION_1
        activityDto.startingDate = DateHandler.toISOString(TWO_DAYS_AGO)
        activityDto.endingDate = DateHandler.toISOString(ONE_DAY_AGO)
        activityDto.applicationDeadline = DateHandler.toISOString(LocalDateTime.now().minusDays(3))
        def otherActivity = new Activity(activityDto, institution, themes)
        def shiftDto
        shiftDto = new ShiftDto()
        shiftDto.participantsNumberLimit = SHIFT_LIMIT_2
        shiftDto.startingDate = DateHandler.toISOString(TWO_DAYS_AGO.plusHours(1))
        shiftDto.endingDate = DateHandler.toISOString(ONE_DAY_AGO.minusHours(1))
        shiftDto.location = SHIFT_LOCATION_1
        def otherShift = new Shift(otherActivity, shiftDto)
        participationDto = new ParticipationDto()
        participationDto.memberRating = 4
        participationDto.memberReview= MEMBER_REVIEW
        participationDto.volunteerRating= 5
        participationDto.volunteerReview= VOLUNTEER_REVIEW
        otherEnrollment.getVolunteer() >> volunteer
        otherEnrollment.getShifts() >> [otherShift]
        otherParticipation = new Participation(otherEnrollment, otherShift, participationDto)


        when: "one participation is deleted"
        participation.delete()

        then: "the other participation remains"
        otherShift.getParticipations().size() == 1
        otherShift.getParticipations().contains(otherParticipation)
        shift.getParticipations().size() == 0
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfiguration {}
}