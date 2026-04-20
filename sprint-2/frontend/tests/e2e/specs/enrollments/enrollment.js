describe('Enrollment', () => {
  beforeEach(() => {
    cy.deleteAllButArs()
    cy.createDemoEntities();
    cy.createDatabaseInfoForEnrollments()
  });

  afterEach(() => {
    cy.deleteAllButArs()
  });

  it('create enrollment', () => {
    const MOTIVATION = 'I am very keen to help other people.';

    cy.intercept('POST', '/enrollments').as('enroll');
    cy.intercept('GET', '/activities/1/enrollments').as('enrollments');


    // member login and check that there is activity with 0 enrollments
    cy.demoMemberLogin()
    cy.get('[data-cy="institution"]').click();
    cy.get('[data-cy="activities"]').click();
    cy.get('[data-cy="memberActivitiesTable"] tbody tr')
      .should('have.length', 3)
      .eq(0)
      .children()
      .should('have.length', 14)
      .eq(3)
      .should('contain', 0)
    cy.logout();

    // volunteer login, creates enrollments
    cy.demoVolunteerLogin()
    cy.get('[data-cy="volunteerActivities"]').click();
    cy.get('[data-cy="volunteerActivitiesTable"] tbody tr')
      .eq(0)
      .find('[data-cy="applyButton"]').click();
    cy.get('[data-cy="motivationInput"]').type(MOTIVATION);
    cy.get('[data-cy="shiftsMultiSelect"]').click();
    cy.get('.v-menu__content.menuable__content__active .v-list-item')
      .eq(0)
      .click();
    cy.get('.v-menu__content.menuable__content__active .v-list-item')
      .eq(1)
      .click();
    cy.get('body').type('{esc}');
    cy.get('[data-cy="shiftsMultiSelect"]')
      .parents('.v-select')
      .find('.v-select__selections .v-chip')
      .should('have.length', 2);
    cy.get('[data-cy="saveEnrollment"]').click()
    cy.wait('@enroll')
    cy.logout()

    // member login and check that there is activity with 1 enrollment
    cy.demoMemberLogin()
    cy.get('[data-cy="institution"]').click();
    cy.get('[data-cy="activities"]').click();

    cy.get('[data-cy="memberActivitiesTable"] tbody tr')
      .eq(0)
      .children()
      .eq(3)
      .should('contain', 1)

    // open enrollments view for first activity
    cy.get('[data-cy="memberActivitiesTable"] tbody tr')
      .eq(0)
      .find('[data-cy="showEnrollments"]').click()
    cy.wait('@enrollments');
    // check that there are 1 enrollment
    cy.get('[data-cy="activityEnrollmentsTable"] tbody tr')
      .should('have.length', 1)
      .eq(0)
      .children()
      .should('have.length', 7)
      .eq(1)
      .should('contain', MOTIVATION)

    cy.logout();
  });

  it('update an enrollment', () => {
    const MOTIVATION2 = 'Motivation Example Two';
    
    // volunteer login and edit an errollment
    cy.demoVolunteerLogin()
    cy.get('[data-cy="volunteerEnrollments"]').click()
    cy.get('[data-cy="volunteerEnrollmentsTable"] tbody tr')
    .eq(0)
    .find('[data-cy="updateEnrollmentButton"]').click()
    
    cy.get('[data-cy="motivationInput"]').clear().type(MOTIVATION2);
    cy.get('[data-cy="saveEnrollment"]').click()
    cy.logout();

    // member check if the motivation is the new one
    cy.demoMemberLogin()
    cy.get('[data-cy="institution"]').click();
    cy.get('[data-cy="activities"]').click();

    cy.get('[data-cy="memberActivitiesTable"] tbody tr')
      .eq(1)
      .find('[data-cy="showEnrollments"]').click()

    cy.get('[data-cy="activityEnrollmentsTable"] tbody tr')
    .eq(0)
    .children()
    .eq(1)
    .should('contain', MOTIVATION2)
    
    cy.logout();

  });

  it('delete an enrollment', () => {
    const ACTIVTIY_NAME_ONE = 'A2';

    // volunteer delete an enrollment
    cy.demoVolunteerLogin()
    cy.get('[data-cy="volunteerEnrollments"]').click()
    cy.get('[data-cy="volunteerEnrollmentsTable"] tbody tr')
    .eq(0)
    .children()
    .eq(0).should('contain', ACTIVTIY_NAME_ONE);

    cy.get('[data-cy="volunteerEnrollmentsTable"] tbody tr')
    .eq(0)
    .find('[data-cy="deleteEnrollmentButton"]').click();
    
    // check where is the actvity that volunteer delete enrollment and check that we can enroll again
    cy.get('[data-cy="volunteerActivities"]').click();
    cy.get('[data-cy="volunteerActivitiesTable"] tbody tr')
    .eq(1)
    .children()
    .eq(0)
    .should('contain', ACTIVTIY_NAME_ONE);

    cy.get('[data-cy="volunteerActivitiesTable"] tbody tr')
    .eq(1)
    .children()
    .eq(10)
    .find('[data-cy="applyButton"]')
    .should('exist');

    cy.logout();

    // member verify that this Activity don't have enrollments
    cy.demoMemberLogin()
    cy.get('[data-cy="institution"]').click();
    cy.get('[data-cy="activities"]').click();

    cy.get('[data-cy="memberActivitiesTable"] tbody tr')
      .eq(1)
      .find('[data-cy="showEnrollments"]').click()
    
    cy.get('[data-cy="memberActivitiesTable"] tbody tr')
      .should('have.length', 0)

    cy.logout();
    
  });

  it('disables save when selected shifts overlap', () => {
    const MOTIVATION = 'I am very keen to help other people.';

    cy.demoVolunteerLogin();
    cy.get('[data-cy="volunteerActivities"]').click();

    cy.get('[data-cy="volunteerActivitiesTable"] tbody tr')
      .eq(0)
      .find('[data-cy="applyButton"]')
      .click();

    cy.get('[data-cy="motivationInput"]').type(MOTIVATION);

    // Select two non-overlapping shifts — save should exist
    cy.get('[data-cy="shiftsMultiSelect"]').click();
    cy.get('.v-menu__content.menuable__content__active .v-list-item')
      .eq(0)
      .click();
    cy.get('.v-menu__content.menuable__content__active .v-list-item')
      .eq(1)
      .click();
    cy.get('body').click(0, 0);
    cy.get('[data-cy="saveEnrollment"]').should('exist');

    // Deselect shifts 0 and 1 first
    cy.get('[data-cy="shiftsMultiSelect"]').click();
    cy.get('.v-menu__content.menuable__content__active .v-list-item')
      .eq(0)
      .click();
    cy.get('.v-menu__content.menuable__content__active .v-list-item')
      .eq(1)
      .click();

    // Select overlapping shifts
    cy.get('.v-menu__content.menuable__content__active .v-list-item')
      .eq(3)
      .click();
    cy.get('.v-menu__content.menuable__content__active .v-list-item')
      .eq(4)
      .click();
    cy.get('body').click(0, 0);
    cy.get('[data-cy="saveEnrollment"]').should('not.exist');

    cy.logout();
  });
});