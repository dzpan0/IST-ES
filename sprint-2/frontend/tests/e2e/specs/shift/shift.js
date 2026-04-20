describe('Shift', () => {
  beforeEach(() => {
    cy.deleteAllButArs()
    cy.createDemoEntities();
    cy.createDatabaseInfoForShifts();

    cy.demoMemberLogin()
  });

  afterEach(() => {
    cy.logout();
    cy.deleteAllButArs()
  });

  it('create shift successfully', () => {
    const LOCATION =
      'This is a very long location description that will be used for testing shift creation purposes';
    const PARTICIPANTS = '2';

    // Intercept API calls
    cy.intercept('GET', '/users/2/getInstitution').as('getInstitutions');
    cy.intercept('GET', '/activities/*/shifts').as('getShifts');
    cy.intercept('POST', '/activities/*/shift').as('createShift');

    // Navigate to create shift form
    cy.get('[data-cy="institution"]').click();
    cy.get('[data-cy="activities"]').click();
    cy.wait('@getInstitutions');

    cy.get('[data-cy="memberActivitiesTable"] tbody tr')
      .eq(1)
      .find('[data-cy="showShifts"]')
      .click();
    cy.wait('@getShifts');

    cy.get('[data-cy="newShift"]').click();

    // Fill in shift form
    cy.get('[data-cy="locationInput"]').type(LOCATION);
    cy.get('[data-cy="participantsNumberInput"]').type(PARTICIPANTS);

    // Select dates
    cy.get('#startingDateInput-input').click();
    cy.get('#startingDateInput-wrapper.date-time-picker')
      .find('.datepicker-day-text')
      .eq(2)
      .click({ force: true });
    cy.get('#endingDateInput-input').click();
    cy.get('#endingDateInput-wrapper.date-time-picker')
      .find('.datepicker-day-text')
      .eq(3)
      .click({ force: true });

    // Save the shift
    cy.get('body').click(0, 0);
    cy.get('[data-cy="saveShift"]').should('be.enabled');
    cy.get('[data-cy="saveShift"]').click();
    cy.wait('@createShift');

    // Check results
    cy.get('[data-cy="activityShiftsTable"] tbody tr')
      .should('have.length', 1)
      .eq(0)
      .children()
      .should('have.length', 4);
    cy.get('[data-cy="activityShiftsTable"] tbody tr')
      .last()
      .should('contain', LOCATION)
      .should('contain', PARTICIPANTS);
  });

  it('rejects shift creation when start date is later than end date', () => {
    const LOCATION =
      'This is a very long location description that will be used for invalid date test cases';
    const PARTICIPANTS = '2';

    cy.intercept('GET', '/users/2/getInstitution').as('activities');
    cy.intercept('GET', '/activities/*/shifts').as('getShifts');
    cy.intercept('POST', '/activities/*/shift').as('createShiftInvalid');

    // Navigate to create shift form
    cy.get('[data-cy="institution"]').click();
    cy.get('[data-cy="activities"]').click();
    cy.wait('@activities');

    cy.get('[data-cy="memberActivitiesTable"] tbody tr')
      .eq(1)
      .find('[data-cy="showShifts"]')
      .click();
    cy.wait('@getShifts');

    cy.get('[data-cy="newShift"]').click();
    cy.get('[data-cy="locationInput"]').type(LOCATION);
    cy.get('[data-cy="participantsNumberInput"]').type(PARTICIPANTS);

    // Intentionally choose a later start date and an earlier end date.
    cy.get('#startingDateInput-input').click();
    cy.get('#startingDateInput-wrapper.date-time-picker')
      .find('.datepicker-day-text')
      .eq(3)
      .click({ force: true });

    cy.get('#endingDateInput-input').click();
    cy.get('#endingDateInput-wrapper.date-time-picker')
      .find('.datepicker-day-text')
      .eq(2)
      .click({ force: true });

    cy.get('body').click(0, 0);
    cy.get('[data-cy="saveShift"]').click();

    // Check error
    cy.wait('@createShiftInvalid').then((interception) => {
      expect(interception.response.statusCode).to.equal(400);
      expect(interception.response.body.message).to.equal(
        'Shift start time must be before end time',
      );
    });

    cy.get('.v-alert')
      .should('be.visible')
      .and('contain.text', 'Error: Shift start time must be before end time');
  });

  it('disabled New Shift when activity is not approved', () => {
    cy.intercept('GET', '/users/2/getInstitution').as('activities');

    // Navigate to Activity Shifts page
    cy.get('[data-cy="institution"]').click();
    cy.get('[data-cy="activities"]').click();
    cy.wait('@activities');

    // Check New Shift button is disabled for SUSPENDED activity
    cy.intercept('GET', '/activities/*/shifts').as('getShifts');
    cy.get('[data-cy="memberActivitiesTable"] tbody tr')
      .eq(2)
      .should('contain.text', 'SUSPENDED')
      .find('[data-cy="showShifts"]')
      .click();
    cy.wait('@getShifts');
    cy.get('[data-cy="newShift"]').should('be.disabled');
    cy.get('[data-cy="getActivities"]').click();

    // Check New Shift button is disabled for REPORTED activity
    cy.intercept('GET', '/activities/*/shifts').as('getShifts');
    cy.get('[data-cy="memberActivitiesTable"] tbody tr')
      .eq(3)
      .should('contain.text', 'REPORTED')
      .find('[data-cy="showShifts"]')
      .click();
    cy.wait('@getShifts');
    cy.get('[data-cy="newShift"]').should('be.disabled');
  });

  it('disables save button when location length is invalid', () => {
    const SHORT = 'Too short';
    const LONG = 'A'.repeat(201);

    cy.intercept('GET', '/users/2/getInstitution').as('getInstitutions');
    cy.intercept('GET', '/activities/*/shifts').as('getShifts');

    cy.get('[data-cy="institution"]').click();
    cy.get('[data-cy="activities"]').click();
    cy.wait('@getInstitutions');

    cy.get('[data-cy="memberActivitiesTable"] tbody tr')
      .eq(1)
      .find('[data-cy="showShifts"]')
      .click();
    cy.wait('@getShifts');

    cy.get('[data-cy="newShift"]').click();
    
    // Too short
    cy.get('[data-cy="locationInput"]').type(SHORT);
    cy.get('[data-cy="saveShift"]').should('be.disabled');

    // No location
    cy.get('[data-cy="locationInput"]').clear();
    cy.get('[data-cy="saveShift"]').should('be.disabled');

    // Too long
    cy.get('[data-cy="locationInput"]').type(LONG);
    cy.get('[data-cy="saveShift"]').should('be.disabled');
  });

  it('rejects shift creation when dates are outside activity range', () => {
    const LOCATION =
      'This is a very long location description that will be used for invalid date test case';
    const PARTICIPANTS = '2';

    cy.intercept('GET', '/users/2/getInstitution').as('activities');
    cy.intercept('GET', '/activities/*/shifts').as('getShifts');
    cy.intercept('POST', '/activities/*/shift').as('createShiftInvalid');

    cy.get('[data-cy="institution"]').click();
    cy.get('[data-cy="activities"]').click();
    cy.wait('@activities');

    cy.get('[data-cy="memberActivitiesTable"] tbody tr')
      .eq(1)
      .find('[data-cy="showShifts"]')
      .click();
    cy.wait('@getShifts');

    const checkInvalidDates = (startIndex, endIndex) => {
      cy.get('[data-cy="newShift"]').click();
      cy.get('[data-cy="locationInput"]').type(LOCATION);
      cy.get('[data-cy="participantsNumberInput"]').type(PARTICIPANTS);

      cy.get('#startingDateInput-input').click();
      cy.get('#startingDateInput-wrapper.date-time-picker')
        .find('.datepicker-day-text')
        .eq(startIndex)
        .click({ force: true });

      cy.get('#endingDateInput-input').click();
      cy.get('#endingDateInput-wrapper.date-time-picker')
        .find('.datepicker-day-text')
        .eq(endIndex)
        .click({ force: true });

      cy.get('body').click(0, 0);
      cy.get('[data-cy="saveShift"]').click();

      cy.wait('@createShiftInvalid').then((interception) => {
        expect(interception.response.statusCode).to.equal(400);
        expect(interception.response.body.message).to.equal(
          'Shift dates must be within activity date range',
        );
      });

      cy.get('.v-alert')
        .should('be.visible')
        .and('contain.text', 'Error: Shift dates must be within activity date range');

      // Click out of alert and close dialog for next test
      cy.get('.v-alert__dismissible:visible').first().click();
      cy.get('[data-cy="cancelShift"]').click();
    };

    // Start before activity start, end within range
    checkInvalidDates(0, 2);

    // Start within range, end after activity
    checkInvalidDates(2, 5);

    // Both outside activity range
    checkInvalidDates(0, 5);
  });

  it('rejects shift creation when total participants exceed activity limit', () => {
    const LOCATION =
      'This is a very long location description that will be used for participant limit test';

    cy.intercept('GET', '/users/2/getInstitution').as('activities');
    cy.intercept('GET', '/activities/*/shifts').as('getShifts');
    cy.intercept('POST', '/activities/*/shift').as('createShift');

    cy.get('[data-cy="institution"]').click();
    cy.get('[data-cy="activities"]').click();
    cy.wait('@activities');

    // Use activity A2 with a participant limit of 5
    cy.get('[data-cy="memberActivitiesTable"] tbody tr')
      .eq(1)
      .find('[data-cy="showShifts"]')
      .click();
    cy.wait('@getShifts');

    const createShift = (participants) => {
      cy.get('[data-cy="newShift"]').click();
      cy.get('[data-cy="locationInput"]').type(LOCATION);
      cy.get('[data-cy="participantsNumberInput"]').type(participants);

      cy.get('#startingDateInput-input').click();
      cy.get('#startingDateInput-wrapper.date-time-picker')
        .find('.datepicker-day-text')
        .eq(2)
        .click({ force: true });

      cy.get('#endingDateInput-input').click();
      cy.get('#endingDateInput-wrapper.date-time-picker')
        .find('.datepicker-day-text')
        .eq(3)
        .click({ force: true });

      cy.get('body').click(0, 0);
      cy.get('[data-cy="saveShift"]').click();
    };

    // Create first shift with 3 participants successfully
    createShift('3');
    cy.wait('@createShift').then((interception) => {
      expect(interception.response.statusCode).to.equal(200);
    });
    cy.get('[data-cy="activityShiftsTable"] tbody tr').should('have.length', 1);

    // Create second shift with 3 participants, exceeding limit and failing
    createShift('3');
    cy.wait('@createShift').then((interception) => {
      expect(interception.response.statusCode).to.equal(400);
      expect(interception.response.body.message).to.equal(
        'Total participants of shifts exceeds activity limit',
      );
    });

    cy.get('.v-alert')
      .should('be.visible')
      .and('contain.text', 'Error: Total participants of shifts exceeds activity limit');
  });
});

