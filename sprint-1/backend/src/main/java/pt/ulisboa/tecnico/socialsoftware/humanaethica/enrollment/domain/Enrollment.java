package pt.ulisboa.tecnico.socialsoftware.humanaethica.enrollment.domain;

import jakarta.persistence.*;
import pt.ulisboa.tecnico.socialsoftware.humanaethica.activity.domain.Activity;
import pt.ulisboa.tecnico.socialsoftware.humanaethica.enrollment.dto.EnrollmentDto;
import pt.ulisboa.tecnico.socialsoftware.humanaethica.exceptions.HEException;
import pt.ulisboa.tecnico.socialsoftware.humanaethica.participation.domain.Participation;
import pt.ulisboa.tecnico.socialsoftware.humanaethica.shift.domain.Shift;
import pt.ulisboa.tecnico.socialsoftware.humanaethica.user.domain.Volunteer;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static pt.ulisboa.tecnico.socialsoftware.humanaethica.exceptions.ErrorMessage.*;

@Entity
public class Enrollment {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @Column(name = "id", nullable = false)
    private Integer id;
    private String motivation;
    private LocalDateTime enrollmentDateTime;

    @ManyToOne
    private Volunteer volunteer;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "enrollment_shifts")
    private List<Shift> shifts = new ArrayList<>();

    @OneToOne(mappedBy = "enrollment", cascade = CascadeType.ALL)
    private Participation participation;

    public Enrollment() {}

    public Enrollment(Volunteer volunteer, List<Shift> shifts, EnrollmentDto enrollmentDto) {
        setVolunteer(volunteer);
        setMotivation(enrollmentDto.getMotivation());
        setEnrollmentDateTime(LocalDateTime.now());

        setShifts(shifts);
        verifyInvariants();
    }

    public void update(EnrollmentDto enrollmentDto) {  
        setMotivation(enrollmentDto.getMotivation());

        editOrDeleteEnrollmentBeforeDeadline();
        verifyInvariants();
    }

    public void delete(){
        volunteer.removeEnrollment(this);

        if (participation != null) {
            participation.delete();
        }

        getShifts().stream().forEach(shift -> shift.removeEnrollment(this));

        editOrDeleteEnrollmentBeforeDeadline();
        verifyInvariants();
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getMotivation() {
        return motivation;
    }

    public void setMotivation(String motivation) {
        this.motivation = motivation;
    }

    public LocalDateTime getEnrollmentDateTime() {
        return enrollmentDateTime;
    }

    public void setEnrollmentDateTime(LocalDateTime enrollmentDateTime) {
        this.enrollmentDateTime = enrollmentDateTime;
    }

    public Activity getActivity() {
        return this.shifts.get(0).getActivity();
    }

    public Volunteer getVolunteer() {
        return this.volunteer;
    }

    public void setVolunteer(Volunteer volunteer) {
        this.volunteer = volunteer;
        this.volunteer.addEnrollment(this);
    }

    public List<Shift> getShifts() {
        return this.shifts;
    }

    public Participation getParticipation() {
        return this.participation;
    }

    public void setParticipation(Participation participation) {
        this.participation = participation;
    }

    public void setShifts(List<Shift> shifts) {
        if (shifts != null) {
            if (!shifts.isEmpty()) {
                new ArrayList<>(shifts).stream().forEach(shift -> this.addShift(shift));
                matchingActivityCheck();
                overlappingShiftsCheck();
                new ArrayList<>(shifts).stream().forEach(shift -> shift.addEnrollment(this));
            } else {
                this.shifts = shifts;
            }
        }
    }

    private void addShift(Shift shift) {
        getShifts().add(shift);
    }

    private void verifyInvariants() {
        motivationIsRequired();
        enrollOnce();
        enrollBeforeDeadline();
        matchingActivityCheck();
        overlappingShiftsCheck();
    }

    private void motivationIsRequired() {
        if (this.motivation == null || this.motivation.trim().length() < 10) {
            throw new HEException(ENROLLMENT_REQUIRES_MOTIVATION);
        }
    }

    private void enrollOnce() {
        if (shifts.get(0).getActivity().getEnrollments().stream()
                .anyMatch(enrollment -> enrollment != this && enrollment.getVolunteer() == this.volunteer)) {
            throw new HEException(ENROLLMENT_VOLUNTEER_IS_ALREADY_ENROLLED);
        }
    }

    private void enrollBeforeDeadline() {
        if (this.enrollmentDateTime.isAfter(this.shifts.get(0).getActivity().getApplicationDeadline())) {
            throw new HEException(ENROLLMENT_AFTER_DEADLINE);
        }
    }

    private void editOrDeleteEnrollmentBeforeDeadline() {
        if (LocalDateTime.now().isAfter(this.shifts.get(0).getActivity().getApplicationDeadline())) {
            throw new HEException(ENROLLMENT_AFTER_DEADLINE);
        }
    }

    private void matchingActivityCheck() {
        int activityId = getShifts().get(0).getActivity().getId();
        if (getShifts().stream().anyMatch(shift -> shift.getActivity().getId() != activityId)) {
            throw new HEException(ENROLLMENT_SHIFTS_DONT_MATCH);
        }
    }

    private void overlappingShiftsCheck() {
        List<Shift> shiftList = getShifts();
        boolean hasOverlap = shiftList.stream()
            .anyMatch(a -> shiftList.stream()
                .filter(b -> b.getId() != a.getId())
                .anyMatch(b -> a.getStartingDate().isBefore(b.getEndingDate())
                            && b.getStartingDate().isBefore(a.getEndingDate())));
        if (hasOverlap) {
            throw new HEException(ENROLLMENT_SHIFTS_OVERLAP);
        }
    }

}
