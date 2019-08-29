describe('Password Reset', function() {
  it('allows a valid user to request its password', function() {
    cy.visit('/passwordreset.xhtml')

    cy.get('input[name="passwordReset:email"]').type(
      'dataverse@mailinator.com{enter}'
    )

    cy.get('.alert').should('contain', 'Password Reset Initiated')
  })

  it('does not treat invalid users differently', function() {
    cy.visit('/passwordreset.xhtml')

    cy.get('input[name="passwordReset:email"]').type(
      'invalid_user@gmail.com{enter}'
    )

    cy.get('.alert').should('contain', 'Password Reset Initiated')
  })
})
