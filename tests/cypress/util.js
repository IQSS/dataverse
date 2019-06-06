/**
 * Function that performs a programmatic login
 * @param {Cypress} cy
 */
export function login(cy, cb) {
  // need to visit the login page to get the CSRF token
  cy.visit('/loginpage.xhtml')

  // a session needs to have been setup
  cy.getCookie('JSESSIONID').should('exist')

  // extract the CSRF token from the view state
  cy.get('#loginForm input[name="javax.faces.ViewState"]').then(viewState => {
    cy.request({
      method: 'POST',
      url: 'http://localhost:8084/loginpage.xhtml',
      form: true,
      body: {
        'javax.faces.partial.ajax': true,
        'javax.faces.source': 'loginForm:login',
        'javax.faces.partial.execute': '@all',
        'javax.faces.partial.render': '@all',
        'loginForm:login': 'loginForm:login',
        loginForm: 'loginForm',
        'loginForm:credentialsContainer:0:credValue': 'dataverseAdmin',
        'loginForm:credentialsContainer:1:sCredValue': 'admin1',
        'javax.faces.ViewState': viewState[0].value,
      },
    }).then(cb ? cb : () => null)
  })
}
