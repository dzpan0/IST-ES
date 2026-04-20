package pt.ulisboa.tecnico.socialsoftware.humanaethica.shift.domain;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import pt.ulisboa.tecnico.socialsoftware.humanaethica.activity.domain.Activity;
import pt.ulisboa.tecnico.socialsoftware.humanaethica.enrollment.domain.Enrollment;
import pt.ulisboa.tecnico.socialsoftware.humanaethica.shift.dto.ShiftDto;
import pt.ulisboa.tecnico.socialsoftware.humanaethica.exceptions.HEException;
import pt.ulisboa.tecnico.socialsoftware.humanaethica.exceptions.ErrorMessage;
import pt.ulisboa.tecnico.socialsoftware.humanaethica.participation.domain.Participation;
import pt.ulisboa.tecnico.socialsoftware.humanaethica.utils.DateHandler;

@Entity
@Table(name = "shift")
public class Shift {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @Column(name = "id", nullable = false)
    private Integer id;
    private LocalDateTime startingDate;
    private LocalDateTime endingDate;
    private Integer participantsNumberLimit;
    private String location;

    @ManyToOne
    private Activity activity;

    @ManyToMany(mappedBy = "shifts")
    private List<Enrollment> enrollments = new ArrayList<>();

    @OneToMany(mappedBy = "shift")
    private List<Participation> participations = new ArrayList<>();

    public Shift() {
    }

    public Shift(Activity activity, ShiftDto shiftDto) {
        setStartingDate(DateHandler.toLocalDateTime(shiftDto.getStartingDate()));
        setEndingDate(DateHandler.toLocalDateTime(shiftDto.getEndingDate()));
        setParticipantsNumberLimit(shiftDto.getParticipantsNumberLimit());
        setLocation(shiftDto.getLocation());
        setActivity(activity);

        verifyInvariants();
    }

    public Integer getId() {
        return this.id;
    }

    public LocalDateTime getStartingDate() {
        return this.startingDate;
    }

    public void setStartingDate(LocalDateTime startingDate) {
        this.startingDate = startingDate;
    }

    public LocalDateTime getEndingDate() {
        return this.endingDate;
    }

    public void setEndingDate(LocalDateTime endingDate) {
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

    public Activity getActivity() {
        return this.activity;
    }

    public void setActivity(Activity activity) {
        this.activity = activity;
        if (activity != null) {
            activity.addShift(this);
        }
    }

    public List<Enrollment> getEnrollments() {
        return this.enrollments;
    }

    public void addEnrollment(Enrollment enrollment) {
        this.enrollments.add(enrollment);
        enrollment.getShifts().add(this);
    }

    public void removeEnrollment(Enrollment enrollment) {
        this.enrollments.remove(enrollment);
        enrollment.getShifts().remove(this);
    }

    public List<Participation> getParticipations() {
        return participations;
    }

    public void addParticipation(Participation participation) {
        this.participations.add(participation);
    }

    public void deleteParticipation(Participation participation) {
        this.participations.remove(participation);
    }

    /*
     * ---------- invariants ---------------------------------------------------
     */
    private static final int MIN_STRING_SIZE = 20;
    private static final int MAX_STRING_SIZE = 200;

    private void verifyInvariants() {
        startingDateIsRequired();
        endingDateIsRequired();
        participantsNumberLimitIsRequired();
        locationIsRequired();
        activityIsRequired();
        stringSizeCheck();
        startBeforeEnd();
        withinActivityPeriod();
        participantNumberLimitCheck();
        approvedActivity();
        capacityCheck();
    }

    private void startingDateIsRequired() {
        if (getStartingDate() == null) { // fails if the ending date is undefined
            throw new HEException(ErrorMessage.SHIFT_MISSING_START);
        }
    }

    private void endingDateIsRequired() {
        if (getEndingDate() == null) { // fails if the ending date is undefined
            throw new HEException(ErrorMessage.SHIFT_MISSING_END);
        }
    }

    private void participantsNumberLimitIsRequired() {
        if (getParticipantsNumberLimit() == null) { // fails if the participant limit is undefined
            throw new HEException(ErrorMessage.SHIFT_MISSING_LIMIT);
        }
    }



    private void locationIsRequired() {
        if (getLocation() == null || this.location.trim().isEmpty()) { // fails if location is null or just whitespace
            throw new HEException(ErrorMessage.SHIFT_MISSING_LOCATION);
        }
    }

    private void activityIsRequired() {
        if (getActivity() == null) { // fails if there is no activity assigned to the shift
            throw new HEException(ErrorMessage.SHIFT_MISSING_ACTIVITY);
        }
    }

    private void stringSizeCheck() {
        int len = getLocation().length();
        if (len < MIN_STRING_SIZE || len > MAX_STRING_SIZE) { // fails if location is smaller than 20 or longer than 200 characters
            throw new HEException(ErrorMessage.SHIFT_STRING_LENGTH);
        }
    }

    private void startBeforeEnd() {
        if (!getStartingDate().isBefore(getEndingDate())) {
            throw new HEException(ErrorMessage.SHIFT_START_AFTER_END);
        }
    }

    private void withinActivityPeriod() {
        if (this.startingDate.isBefore(this.activity.getStartingDate()) ||
                this.endingDate.isAfter(this.activity.getEndingDate())) {
            throw new HEException(ErrorMessage.SHIFT_NOT_WITHIN_ACTIVITY_PERIOD);
        }
    }

    private void participantNumberLimitCheck() {
        if (this.participantsNumberLimit <= 0) { // fails if participant limit is not a positive integer (1 or more)
            throw new HEException(ErrorMessage.SHIFT_INVALID_LIMIT);
        }
    }

    private void approvedActivity() {
        if (this.activity.getState() != Activity.State.APPROVED) {
            throw new HEException(ErrorMessage.ACTIVITY_NOT_APPROVED, this.activity.getName());
        }
    }

    private void capacityCheck() {
        int sum = getActivity().getShifts().stream().mapToInt(Shift::getParticipantsNumberLimit).sum();
        if (sum > getActivity().getParticipantsNumberLimit()) {
            throw new HEException(ErrorMessage.SHIFT_CAPACITY_GREATER_THAN_ACTIVITY);
        }
    }
}
