var app = require('ampersand-app');
var AmpersandView = require('ampersand-view');
var templates = require('../templates');
var AccountsDropdownView = require('../views/accounts-dropdown');
var ContactsView = require('../views/contacts');
var AddContactView = require('../views/add-contact');
var SarosApi = require('../saros-api');
var dictionary = require('../dictionary');

module.exports = AmpersandView.extend({
    template: templates.mainPage,
    // Add the dictionary to the context so that the template
    // engine can use it.
    d: dictionary,
    autoRender: true,
    subviews: {
        accounts: {
            container: '[data-hook=account-list]',
            prepareView: function(el) {

                return new AccountsDropdownView({
                    el: el,
                    collection: app.accounts
                });
            }
        },
        contacts: {
            container: '[data-hook=contact-container]',
            prepareView: function(el) {
                return new ContactsView({
                    el: el,
                    collection: app.contacts
                });
            }
        }        
    },
    events: {
        'click [data-hook=connection-button]': 'toggleConnect',
        'click [data-hook=add-contact]': 'openAddContactDialog',
        'click [data-hook=start-session]': 'startSession',
    },
    bindings: {
        'model.activeAccountLabel': '[data-hook=active-account]',
        'model.connectionStateLabel': '[data-hook=connection-button]',
        'model.isReady': {
            type: 'booleanClass',
            yes: '',
            no: 'disabled',
            hook: 'connection-button'
        },
        'model.isConnected': {
            type: 'booleanClass',
            yes: 'disabled',
            no: '',
            selector: '.account'
        }
    },
    toggleConnect: function() {

        this.model.updateConnectionState();
    },
    openAddContactDialog: function() {

        new AddContactView();
    },
    startSession: function() {

        SarosApi.showStartSessionWizard();
    }
});
