package pt.ulisboa.tecnico.socialsoftware.humanaethica.shift.service

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import pt.ulisboa.tecnico.socialsoftware.humanaethica.BeanConfiguration
import pt.ulisboa.tecnico.socialsoftware.humanaethica.SpockTest
import pt.ulisboa.tecnico.socialsoftware.humanaethica.activity.domain.Activity
import pt.ulisboa.tecnico.socialsoftware.humanaethica.exceptions.ErrorMessage
import pt.ulisboa.tecnico.socialsoftware.humanaethica.exceptions.HEException
import spock.lang.Unroll

import java.time.LocalDateTime

@DataJpaTest
class GetShiftsByActivityServiceTest extends SpockTest {
    public static final String EXIST = 'exist'
    public static final String NO_EXIST = 'noExist'
    def activity
    def otherActivity

    def setup() {
        def institution = institutionService.getDemoInstitution()

        def activityDto = createActivityDto(ACTIVITY_NAME_1, ACTIVITY_REGION_1, ACTIVITY_LIMIT_1, ACTIVITY_DESCRIPTION_1,
                THREE_DAYS_AGO, TWO_DAYS_AGO, IN_TWO_DAYS, null)

        activity = new Activity(activityDto, institution, new ArrayList<>())
        activityRepository.save(activity)

        activityDto.name = ACTIVITY_NAME_2
        otherActivity = new Activity(activityDto, institution, new ArrayList<>())
        activityRepository.save(otherActivity)
    }

    def 'get two shifts of the same activity'() {
        given:
        def shift1 = createShift(activity, SHIFT_LIMIT_1, ONE_DAY_AGO, IN_ONE_DAY, SHIFT_LOCATION_1)
        def shift2 = createShift(activity, SHIFT_LIMIT_2, ONE_DAY_AGO, IN_ONE_DAY, SHIFT_LOCATION_2)

        when:
        def shifts = shiftService.getShiftsByActivity(activity.id)

        then:
        shifts.size() == 2
        shifts.get(0).participantsNumberLimit == SHIFT_LIMIT_1
        shifts.get(0).location == SHIFT_LOCATION_1
        shifts.get(1).participantsNumberLimit == SHIFT_LIMIT_2
        shifts.get(1).location == SHIFT_LOCATION_2
    }

    def 'get one shifts of an activity'() {
        given:
        def shift1 = createShift(activity, SHIFT_LIMIT_1, ONE_DAY_AGO, IN_ONE_DAY, SHIFT_LOCATION_1)
        def shift2 = createShift(otherActivity, SHIFT_LIMIT_2, ONE_DAY_AGO, IN_ONE_DAY, SHIFT_LOCATION_2)

        when:
        def shifts = shiftService.getShiftsByActivity(activity.id)

        then:
        shifts.size() == 1
        shifts.get(0).participantsNumberLimit == SHIFT_LIMIT_1
        shifts.get(0).location == SHIFT_LOCATION_1
    }

    def 'get shifts of an activity with no shifts'() {
        when:
        def result = shiftService.getShiftsByActivity(activity.id)

        then:
        result.size() == 0
    }

    @Unroll
    def 'get shifts of an activity with invalid arguments: activityId=#activityId'() {
        when:
        shiftService.getShiftsByActivity(getActivityId(activityId))

        then:
        def error = thrown(HEException)
        error.getErrorMessage() == ErrorMessage.ACTIVITY_NOT_FOUND

        where:
        activityId << [null, NO_EXIST]
    }

    def getActivityId(activityId) {
        if (activityId == EXIST)
            return activity.id
        else if (activityId == NO_EXIST)
            return 222
        return null
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfiguration {}
}

