package pt.ulisboa.tecnico.socialsoftware.humanaethica.shift.service

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import pt.ulisboa.tecnico.socialsoftware.humanaethica.BeanConfiguration
import pt.ulisboa.tecnico.socialsoftware.humanaethica.SpockTest
import pt.ulisboa.tecnico.socialsoftware.humanaethica.activity.domain.Activity
import pt.ulisboa.tecnico.socialsoftware.humanaethica.exceptions.ErrorMessage
import pt.ulisboa.tecnico.socialsoftware.humanaethica.exceptions.HEException
import pt.ulisboa.tecnico.socialsoftware.humanaethica.shift.dto.ShiftDto
import pt.ulisboa.tecnico.socialsoftware.humanaethica.utils.DateHandler
import spock.lang.Unroll

@DataJpaTest
class CreateShiftServiceTest extends SpockTest {
    public static final String EXIST = 'exist'
    public static final String NO_EXIST = 'noExist'
    def activity

    def setup() {
        def institution = institutionService.getDemoInstitution()

        def activityDto = createActivityDto(ACTIVITY_NAME_1, ACTIVITY_REGION_1, ACTIVITY_LIMIT_1, ACTIVITY_DESCRIPTION_1,
                THREE_DAYS_AGO, TWO_DAYS_AGO, IN_TWO_DAYS, null)

        activity = new Activity(activityDto, institution, new ArrayList<>())
        activityRepository.save(activity)
    }

    def 'create shift'() {
        given:
        def shiftDto = new ShiftDto()
        shiftDto.participantsNumberLimit = SHIFT_LIMIT_1
        shiftDto.startingDate = DateHandler.toISOString(ONE_DAY_AGO)
        shiftDto.endingDate = DateHandler.toISOString(IN_ONE_DAY)
        shiftDto.location = SHIFT_LOCATION_1

        when:
        def result = shiftService.createShift(activity.getId(), shiftDto)

        then: "the return data is correct"
        result.startingDate == DateHandler.toISOString(ONE_DAY_AGO)
        result.endingDate == DateHandler.toISOString(IN_ONE_DAY)
        result.participantsNumberLimit == SHIFT_LIMIT_1
        result.location == SHIFT_LOCATION_1

        and: "the shift is saved in the database"
        shiftRepository.findAll().size() == 1

        and: "the stored data is correct"
        def storedShift = shiftRepository.findAll().get(0)
        storedShift.startingDate == ONE_DAY_AGO
        storedShift.endingDate == IN_ONE_DAY
        storedShift.participantsNumberLimit == SHIFT_LIMIT_1
        storedShift.location == SHIFT_LOCATION_1
        storedShift.activity.id == activity.id
    }

    @Unroll
    def 'create shift with invalid arguments: activityId=#activityId | shiftDto=#shiftValue'() {
        given:
        def shiftDto = new ShiftDto()
        shiftDto.participantsNumberLimit = SHIFT_LIMIT_1
        shiftDto.startingDate = DateHandler.toISOString(ONE_DAY_AGO)
        shiftDto.endingDate = DateHandler.toISOString(IN_ONE_DAY)
        shiftDto.location = SHIFT_LOCATION_1

        when:
        shiftService.createShift(getActivityId(activityId), getShiftDto(shiftValue, shiftDto))

        then:
        def error = thrown(HEException)
        error.getErrorMessage() == errorMessage
        and:
        shiftRepository.findAll().size() == 0

        where:
        activityId | shiftValue || errorMessage
        null       | EXIST      || ErrorMessage.ACTIVITY_NOT_FOUND
        NO_EXIST   | EXIST      || ErrorMessage.ACTIVITY_NOT_FOUND
        EXIST   | null       || ErrorMessage.SHIFT_INVALID
    }

    def getActivityId(activityId) {
        if (activityId == EXIST)
            return activity.id
        else if (activityId == NO_EXIST)
            return 222
        return null
    }

    def getShiftDto(value, shiftDto) {
        if (value == EXIST)
            return shiftDto
        return null
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfiguration {}
}

