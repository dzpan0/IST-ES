package pt.ulisboa.tecnico.socialsoftware.humanaethica.enrollment.dto;

import java.util.ArrayList;
import java.util.List;

import pt.ulisboa.tecnico.socialsoftware.humanaethica.enrollment.domain.Enrollment;
import pt.ulisboa.tecnico.socialsoftware.humanaethica.utils.DateHandler;
import pt.ulisboa.tecnico.socialsoftware.humanaethica.shift.dto.ShiftDto;

public class EnrollmentDto {
    private Integer id;
    private Integer volunteerId;
    private String volunteerName;
    private String motivation;
    private String enrollmentDateTime;
    private boolean isParticipating;
    private List<ShiftDto> shifts;


    public EnrollmentDto() {}

    public EnrollmentDto(Enrollment enrollment) {
        this(enrollment, true);
    }

    public EnrollmentDto(Enrollment enrollment, boolean deepCopyShifts) {
        this.id = enrollment.getId();
        this.volunteerId = enrollment.getVolunteer().getId();
        this.volunteerName = enrollment.getVolunteer().getName();
        this.motivation = enrollment.getMotivation();
        this.enrollmentDateTime = DateHandler.toISOString(enrollment.getEnrollmentDateTime());
        this.isParticipating = enrollment.getShifts().stream()
                .flatMap(shift -> shift.getParticipations().stream())
                .anyMatch(participation -> participation.getEnrollment().getVolunteer().getId().equals(enrollment.getVolunteer().getId()));

        if (deepCopyShifts) {
            this.shifts = enrollment.getShifts().stream().map(s -> new ShiftDto(s, false)).toList();
        } else {
            this.shifts = new ArrayList<>();
        }
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getActivityId() {
        return shifts.get(0).getActivityId();
    }

    public Integer getVolunteerId() {
        return volunteerId;
    }

    public void setVolunteerId(Integer volunteerId) {
        this.volunteerId = volunteerId;
    }

    public String getVolunteerName() {
        return volunteerName;
    }

    public void setVolunteerName(String volunteerName) {
        this.volunteerName = volunteerName;
    }

    public String getMotivation() {
        return motivation;
    }

    public void setMotivation(String motivation) {
        this.motivation = motivation;
    }

    public String getEnrollmentDateTime() {
        return enrollmentDateTime;
    }

    public void setEnrollmentDateTime(String enrollmentDateTime) {
        this.enrollmentDateTime = enrollmentDateTime;
    }

    public boolean isParticipating() {
        return isParticipating;
    }

    public void setParticipating(boolean participating) {
        isParticipating = participating;
    }

    public List<ShiftDto> getShifts() {
        return this.shifts;
    }

    public void setShifts(List<ShiftDto> shifts) {
        this.shifts = shifts;
    }

}
