var app = require('ampersand-app');
var View = require('ampersand-view');
var tmpl = require('./account-dropdown.hbs');

module.exports = View.extend({
    template: tmpl,
    bindings: {
        'model.label': '[data-hook=username]',
        'model.isActive': {
            type: 'booleanClass',
            yes: 'font-bold',
            no: ''
        }
    },
    events: {
        'click': 'setToActiveAccount'
    },
    setToActiveAccount: function() {
        app.state.activateAccount(this.model);
    }
});
