package pt.ulisboa.tecnico.socialsoftware.humanaethica.shift.domain

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import pt.ulisboa.tecnico.socialsoftware.humanaethica.BeanConfiguration
import pt.ulisboa.tecnico.socialsoftware.humanaethica.SpockTest
import pt.ulisboa.tecnico.socialsoftware.humanaethica.exceptions.ErrorMessage
import pt.ulisboa.tecnico.socialsoftware.humanaethica.exceptions.HEException
import pt.ulisboa.tecnico.socialsoftware.humanaethica.activity.domain.Activity
import pt.ulisboa.tecnico.socialsoftware.humanaethica.shift.dto.ShiftDto
import pt.ulisboa.tecnico.socialsoftware.humanaethica.utils.DateHandler

import java.time.LocalDateTime
import spock.lang.Unroll

@DataJpaTest
class CreateShiftMethodTest extends SpockTest {
    Activity activity = Mock()
    Shift otherShift = Mock()
    def shiftDto

    def setup() {
        given: "shift info"
        shiftDto = new ShiftDto()
        shiftDto.participantsNumberLimit = SHIFT_LIMIT_1
        shiftDto.startingDate = DateHandler.toISOString(ONE_DAY_AGO)
        shiftDto.endingDate = DateHandler.toISOString(IN_ONE_DAY)
        shiftDto.location = SHIFT_LOCATION_1
    }

    def "create valid shift from dto"() {
        given: "activity info"
        def shifts = []
        activity.addShift(_) >> { Shift s -> shifts.add(s) }
        activity.getShifts() >> shifts
        activity.getStartingDate() >> TWO_DAYS_AGO
        activity.getEndingDate() >> IN_TWO_DAYS
        activity.getState() >> Activity.State.APPROVED
        activity.getParticipantsNumberLimit() >> ACTIVITY_LIMIT_1

        when:
        def shift = new Shift(activity, shiftDto)

        then: "dto attributes are set"
        shift.getStartingDate() == ONE_DAY_AGO
        shift.getEndingDate() == IN_ONE_DAY
        shift.getParticipantsNumberLimit() == SHIFT_LIMIT_1
        shift.getActivity() == activity
        shift.getLocation() == SHIFT_LOCATION_1

        and: "association added"
        1 * activity.addShift(_)
    }

    @Unroll
    def "create shift with missing attribute: activityNull=#activityNull | start=#start | end=#end | limit=#limit | location=#location"() {
        given: "shift info"
        shiftDto = new ShiftDto()
        shiftDto.setParticipantsNumberLimit(limit)
        shiftDto.setStartingDate(start instanceof LocalDateTime ? DateHandler.toISOString(start) : start as String)
        shiftDto.setEndingDate(end instanceof LocalDateTime ? DateHandler.toISOString(end) : end as String)
        shiftDto.setLocation(location)
        def act = activityNull ? null : activity

        and: "activity info"
        activity.getStartingDate() >> TWO_DAYS_AGO
        activity.getEndingDate() >> IN_TWO_DAYS
        activity.getState() >> Activity.State.APPROVED
        activity.getParticipantsNumberLimit() >> ACTIVITY_LIMIT_1

        when:
        new Shift(act, shiftDto)

        then:
        def error = thrown(HEException)
        error.getErrorMessage() == errorMessage

        where:
        activityNull | start           | end            | limit             | location             || errorMessage
        true         | ONE_DAY_AGO     | IN_ONE_DAY     | SHIFT_LIMIT_1     | SHIFT_LOCATION_1     || ErrorMessage.SHIFT_MISSING_ACTIVITY
        false        | null            | IN_ONE_DAY     | SHIFT_LIMIT_1     | SHIFT_LOCATION_1     || ErrorMessage.SHIFT_MISSING_START
        false        | ONE_DAY_AGO     | null           | SHIFT_LIMIT_1     | SHIFT_LOCATION_1     || ErrorMessage.SHIFT_MISSING_END
        false        | ONE_DAY_AGO     | IN_ONE_DAY     | null              | SHIFT_LOCATION_1     || ErrorMessage.SHIFT_MISSING_LIMIT
        false        | ONE_DAY_AGO     | IN_ONE_DAY     | SHIFT_LIMIT_1     | null                 || ErrorMessage.SHIFT_MISSING_LOCATION
        false        | ONE_DAY_AGO     | IN_ONE_DAY     | SHIFT_LIMIT_1     | " "                  || ErrorMessage.SHIFT_MISSING_LOCATION
    }

    def "create shift with location too short"() {
        given: "shift info"
        shiftDto = new ShiftDto()
        shiftDto.setParticipantsNumberLimit(SHIFT_LIMIT_1)
        shiftDto.setStartingDate(DateHandler.toISOString(ONE_DAY_AGO))
        shiftDto.setEndingDate(DateHandler.toISOString(IN_ONE_DAY))
        shiftDto.setLocation('x' * 19)

        and: "activity info"
        activity.getStartingDate() >> TWO_DAYS_AGO
        activity.getEndingDate() >> IN_TWO_DAYS
        activity.getState() >> Activity.State.APPROVED
        activity.getParticipantsNumberLimit() >> ACTIVITY_LIMIT_1

        when:
        new Shift(activity, shiftDto)

        then:
        def error = thrown(HEException)
        error.getErrorMessage() == ErrorMessage.SHIFT_STRING_LENGTH
    }

    def "create shift with location too long"() {
        given: "shift info"
        shiftDto = new ShiftDto()
        shiftDto.setParticipantsNumberLimit(SHIFT_LIMIT_1)
        shiftDto.setStartingDate(DateHandler.toISOString(ONE_DAY_AGO))
        shiftDto.setEndingDate(DateHandler.toISOString(IN_ONE_DAY))
        shiftDto.setLocation('x' * 201)

        and: "activity info"
        activity.getStartingDate() >> TWO_DAYS_AGO
        activity.getEndingDate() >> IN_TWO_DAYS
        activity.getState() >> Activity.State.APPROVED
        activity.getParticipantsNumberLimit() >> ACTIVITY_LIMIT_1

        when:
        new Shift(activity, shiftDto)

        then:
        def error = thrown(HEException)
        error.getErrorMessage() == ErrorMessage.SHIFT_STRING_LENGTH
    }

    @Unroll
    def "create shift and violate start date before end date invariant: start=#start | end=#end"() {
        given: "shift dto"
        def shiftDto = new ShiftDto()
        shiftDto.setParticipantsNumberLimit(SHIFT_LIMIT_1)
        shiftDto.setStartingDate(start instanceof LocalDateTime ? DateHandler.toISOString(start) : start as String)
        shiftDto.setEndingDate(end instanceof LocalDateTime ? DateHandler.toISOString(end) : end as String)
        shiftDto.setLocation(SHIFT_LOCATION_1)

        and: "activity info"
        activity.getStartingDate() >> TWO_DAYS_AGO
        activity.getEndingDate() >> IN_TWO_DAYS
        activity.getState() >> Activity.State.APPROVED
        activity.getParticipantsNumberLimit() >> ACTIVITY_LIMIT_1

        when:
        new Shift(activity, shiftDto)

        then:
        def error = thrown(HEException)
        error.getErrorMessage() == errorMessage

        where:
        start       | end        || errorMessage
        IN_TWO_DAYS | IN_ONE_DAY || ErrorMessage.SHIFT_START_AFTER_END
        IN_ONE_DAY  | IN_ONE_DAY || ErrorMessage.SHIFT_START_AFTER_END
    }

    @Unroll
    def "create shift and violate start and end date within activity period invariant: start=#start | end=#end"() {
        given: "shift dto"
        shiftDto = new ShiftDto()
        shiftDto.participantsNumberLimit = SHIFT_LIMIT_1
        shiftDto.setStartingDate(start instanceof LocalDateTime ? DateHandler.toISOString(start) : start as String)
        shiftDto.setEndingDate(end instanceof LocalDateTime ? DateHandler.toISOString(end) : end as String)
        shiftDto.location = SHIFT_LOCATION_1

        and: "activity info"
        activity.getStartingDate() >> TWO_DAYS_AGO
        activity.getEndingDate() >> IN_TWO_DAYS
        activity.getState() >> Activity.State.APPROVED
        activity.getParticipantsNumberLimit() >> ACTIVITY_LIMIT_1

        when:
        new Shift(activity, shiftDto)

        then:
        def error = thrown(HEException)
        error.getErrorMessage() == errorMessage

        where:
        start          | end           || errorMessage
        THREE_DAYS_AGO | NOW           || ErrorMessage.SHIFT_NOT_WITHIN_ACTIVITY_PERIOD
        NOW            | IN_THREE_DAYS || ErrorMessage.SHIFT_NOT_WITHIN_ACTIVITY_PERIOD
        THREE_DAYS_AGO | IN_THREE_DAYS || ErrorMessage.SHIFT_NOT_WITHIN_ACTIVITY_PERIOD
    }

    @Unroll
    def "create shift with invalid participant limit: limit=#limit"() {
        given: "shift info"
        shiftDto = new ShiftDto()
        shiftDto.participantsNumberLimit = limit
        shiftDto.startingDate = DateHandler.toISOString(ONE_DAY_AGO)
        shiftDto.endingDate = DateHandler.toISOString(IN_ONE_DAY)
        shiftDto.location = SHIFT_LOCATION_1

        and: "activity info"
        activity.getStartingDate() >> TWO_DAYS_AGO
        activity.getEndingDate() >> IN_TWO_DAYS
        activity.getState() >> Activity.State.APPROVED
        activity.getParticipantsNumberLimit() >> ACTIVITY_LIMIT_1

        when:
        new Shift(activity, shiftDto)

        then:
        def error = thrown(HEException)
        error.getErrorMessage() == ErrorMessage.SHIFT_INVALID_LIMIT

        where:
        limit << [0, -1]
    }

    @Unroll
    def "create shift and violate activity is approved invariant: activityState=#activityState"() {
        given: "shift dto"
        shiftDto = new ShiftDto()
        shiftDto.participantsNumberLimit = SHIFT_LIMIT_1
        shiftDto.startingDate = DateHandler.toISOString(ONE_DAY_AGO)
        shiftDto.endingDate = DateHandler.toISOString(IN_ONE_DAY)
        shiftDto.location = SHIFT_LOCATION_1

        and: "activity info"
        activity.getStartingDate() >> TWO_DAYS_AGO
        activity.getEndingDate() >> IN_TWO_DAYS
        activity.getState() >> activityState
        activity.getParticipantsNumberLimit() >> ACTIVITY_LIMIT_1

        when:
        new Shift(activity, shiftDto)

        then:
        def error = thrown(HEException)
        error.getErrorMessage() == errorMessage

        where:
        activityState            || errorMessage
        Activity.State.REPORTED  || ErrorMessage.ACTIVITY_NOT_APPROVED
        Activity.State.SUSPENDED || ErrorMessage.ACTIVITY_NOT_APPROVED
    }

    def "create shift and violate shift capacity being smaller or equal to activity capacity invariant"() {
        given: "shift info"
        shiftDto = new ShiftDto()
        shiftDto.participantsNumberLimit = SHIFT_LIMIT_1
        shiftDto.startingDate = DateHandler.toISOString(TWO_DAYS_AGO.plusHours(1))
        shiftDto.endingDate = DateHandler.toISOString(ONE_DAY_AGO.minusHours(1))
        shiftDto.location = SHIFT_LOCATION_1

        and: "other shift info"
        otherShift.getParticipantsNumberLimit() >> SHIFT_LIMIT_2

        and: "activity info"
        def shifts = [otherShift]
        activity.addShift(_) >> { Shift s -> shifts.add(s) }
        activity.getStartingDate() >> TWO_DAYS_AGO
        activity.getEndingDate() >> IN_TWO_DAYS
        activity.getState() >> Activity.State.APPROVED
        activity.getParticipantsNumberLimit() >> SHIFT_LIMIT_1
        activity.getShifts() >> shifts

        when:
        def shift = new Shift(activity, shiftDto)

        then:
        def error = thrown(HEException)
        error.getErrorMessage() == ErrorMessage.SHIFT_CAPACITY_GREATER_THAN_ACTIVITY
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfiguration {}
}
