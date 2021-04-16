import { login } from '../util'

describe('Dataverse', function() {
  beforeEach(function() {
    login(cy)
  })

  it('can be created with minimal customization', function() {
    cy.visit('/dataverse.xhtml?ownerId=1')

    // setup aliases for used fields
    cy.get('input[id="dataverseForm:name"]').as('name')
    cy.get('input[id="dataverseForm:identifier"]').as('identifier')
    cy.get('select[id="dataverseForm:dataverseCategory"]').as(
      'categorySelection'
    )
    cy.get('input[id="dataverseForm:affiliation"]').as('affiliation')
    cy.get('textarea[id="dataverseForm:description"]').as('description')
    cy.get('input[id="dataverseForm:metadataRoot"]').as('metadataRoot')
    cy.get('input[id="dataverseForm:facetsRoot"]').as('facetsRoot')

    // assert that there are no alerts in the initial state
    cy.get('.alert:not(.alert-success)').should('not.exist')
    cy.get('.ui-message-error-detail').should('not.exist')

    // assert that the initial values have been set correctly
    cy.get('@name').should('have.value', 'Dataverse Admin Dataverse')
    cy.get('@identifier').should('be.empty')
    cy.get('@categorySelection').should('have.value', '')
    cy.get('@affiliation').should('have.value', 'Dataverse.org')
    cy.get('@description').should('be.empty')
    cy.get('@metadataRoot').should('be.checked')
    cy.get('@facetsRoot').should('be.checked')

    // try to create a dataverse
    cy.contains('Create Dataverse').click()

    // expect validation to fail for some key fields
    cy.get('.alert').should('exist')
    cy.get('.ui-message-error-detail').should('have.length', 2)

    // fill in the remaining forms
    cy.get('@identifier').type('testDv')
    cy.get('@categorySelection').select('DEPARTMENT')

    // create a dataverse
    cy.contains('Create Dataverse').click()

    // ensure that we have been redirected to the correct dataverse page
    cy.url().should('contain', '/dataverse/testDv')
    cy.get('.dataverseHeaderCell').should('contain', 'Unpublished')
  })

  it('allows datasets to be created', function() {
    cy.visit('/dataverse/testDv')

    // click through to the new dataset page
    // TODO: this should all be tested in a different spec
    cy.contains('Add Data').click()
    cy.contains('New Dataset').click()

    // fill in the necessary fields for the simplest new dataset
    cy.get('.panel-body').within(() => {
      cy.get('> :nth-child(1) input[type="text"]').type('Some dataset')
      cy.get('> :nth-child(4) textarea').type('It is about some data')
      cy.get('> :nth-child(5) tr:nth-child(5) div.ui-chkbox-box').click()
    })

    // save the new dataset
    cy.contains('Save Dataset').click()

    // ensure that we have been redirected to the correct dataverse page
    cy.url().should('contain', '/dataset.xhtml')
  })
})
