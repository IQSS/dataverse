import { login } from '../util'

describe('Dataverse User', function() {
  beforeEach(function() {
    login(cy)
  })

  it('allows the user to read and remove notifications', function() {
    cy.visit('/dataverseuser.xhtml?selectTab=notifications')

    // expect there to be one notification
    cy.get('.notification-item').should('have.length.of', 1)

    // remove the notification
    cy.get('.notification-item')
      .eq(0)
      .find('.notification-item-cell')
      .eq(1)
      .click()

    // expect there to be no more notifications
    cy.get('.notification-item').should('have.length.of', 0)
  })

  it('allows looking up account information', function() {
    cy.visit('/dataverseuser.xhtml?selectTab=accountInfo')

    cy.get('.form-control-static')
      .eq(0)
      .should('contain', 'dataverseAdmin')

    cy.get('.form-control-static')
      .eq(1)
      .should('contain', 'Dataverse')

    cy.get('.form-control-static')
      .eq(2)
      .should('contain', 'Admin')

    cy.get('.form-control-static')
      .eq(3)
      .should('contain', 'dataverse@mailinator.com')

    cy.get('.form-control-static')
      .eq(4)
      .should('contain', 'Not Verified')

    cy.get('.form-control-static')
      .eq(5)
      .should('contain', 'Dataverse.org')

    cy.get('.form-control-static')
      .eq(6)
      .should('contain', 'Admin')
  })

  it('enables recreation of the API token', function() {
    const tokenTabId =
      'div[id="dataverseUserForm:dataRelatedToMeView:apiTokenTab"]'
    cy.visit('/dataverseuser.xhtml?selectTab=apiTokenTab')

    cy.get(`${tokenTabId} pre code`)
      .should('not.be.empty')
      .then(oldToken => {
        // click the token recreation button
        cy.get(`${tokenTabId} button[type=submit]`).click()

        cy.get(`${tokenTabId} pre code`)
          .should('not.be.empty')
          .then(newToken => {
            // assert that the new token is different from the old one
            expect(newToken[0].textContent).to.not.eq(oldToken[0].textContent)
          })
      })
  })
})
