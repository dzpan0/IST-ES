package pt.ulisboa.tecnico.socialsoftware.humanaethica.shift.dto;

import java.util.ArrayList;
import java.util.List;

import pt.ulisboa.tecnico.socialsoftware.humanaethica.participation.dto.ParticipationDto;
import pt.ulisboa.tecnico.socialsoftware.humanaethica.shift.domain.Shift;
import pt.ulisboa.tecnico.socialsoftware.humanaethica.utils.DateHandler;
import pt.ulisboa.tecnico.socialsoftware.humanaethica.enrollment.dto.EnrollmentDto;

public class ShiftDto {
    private Integer id;
    private String startingDate;
    private String endingDate;
    private Integer participantsNumberLimit;
    private String location;
    private Integer activityId;
    private List<EnrollmentDto> enrollments;
    private List<ParticipationDto> participations;

    public ShiftDto() {}

    public ShiftDto(Shift shift) {
        this(shift, true);
    }

    public ShiftDto(Shift shift, boolean deepCopyEnrollments) {
        setId(shift.getId());
        setStartingDate(DateHandler.toISOString(shift.getStartingDate()));
        setEndingDate(DateHandler.toISOString(shift.getEndingDate()));
        setParticipantsNumberLimit(shift.getParticipantsNumberLimit());
        setLocation(shift.getLocation());
        if (shift.getActivity() != null) {
            setActivityId(shift.getActivity().getId());
        }
        
        if (deepCopyEnrollments) {
            this.enrollments = shift.getEnrollments().stream().map(e -> new EnrollmentDto(e, false)).toList();
        } else {
            this.enrollments = new ArrayList<>();
        }
        
        setActivityId(shift.getActivity().getId());
        setParticipations(shift.getParticipations().stream().map(ParticipationDto::new).toList());
    }

    public Integer getId() {
        return this.id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getStartingDate() {
        return this.startingDate;
    }

    public void setStartingDate(String startingDate) {
        this.startingDate = startingDate;
    }

    public String getEndingDate() {
        return this.endingDate;
    }

    public void setEndingDate(String endingDate) {
        this.endingDate = endingDate;
    }

    public Integer getParticipantsNumberLimit() {
        return this.participantsNumberLimit;
    }

    public void setParticipantsNumberLimit(Integer participantsNumberLimit) {
        this.participantsNumberLimit = participantsNumberLimit;
    }

    public String getLocation() {
        return this.location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public Integer getActivityId() {
        return this.activityId;
    }

    public void setActivityId(Integer activityId) {
        this.activityId = activityId;
    }

    public List<EnrollmentDto> getEnrollments() {
        return this.enrollments;
    }

    public void setEnrollments(List<EnrollmentDto> enrollments) {
        this.enrollments = enrollments;
    }

    public List<ParticipationDto> getParticipations() {
        return participations;
    }

    public void setParticipations(List<ParticipationDto> participations) {
        this.participations = participations;
    }
}