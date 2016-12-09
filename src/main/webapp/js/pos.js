var exec_disabled = new Array();

function bap_is_exec_disabled(configName) {
    return exec_disabled[configName.value];
}

function bap_get_configName_from_publisher(publisher) {
    return $(publisher).getElementsBySelector('.ssh-config-name')[0];
}

function bap_is_exec_disabled_for_publisher(publisher) {
    var configName = bap_get_configName_from_publisher(publisher);
    return bap_is_exec_disabled(configName);
}

function bap_get_publisher_from_setting(setting) {
    var transfer = $(setting).ancestors()[3];
    return bap_get_publisher_from_transfer(transfer);
}

function bap_get_publisher_from_transfer(transfer) {
    return $(transfer).ancestors()[5];
}

function bap_get_publisher_from_configName(configName) {
    return $(configName).ancestors()[3];
}

function bap_is_exec_disabled_for_transfer(transfer) {
    var publisher = bap_get_publisher_from_transfer(transfer);
    return bap_is_exec_disabled_for_publisher(publisher);
}

function bap_get_configName(setting) {
    var publisher = bap_get_publisher_from_setting(setting);
    return bap_get_configName_from_publisher(publisher);
}

function bap_get_configName_qs(setting) {
    return "&configName=" + encodeURIComponent(bap_get_configName(setting).value);
}

function bap_show_row(row) {
    row.show();
    row.next().show();
}

function bap_hide_row(row) {
    row.hide();
    row.next().hide();
}

function bap_set_exec_control_visibility(container, show_or_hide_row) {
    $(container).getElementsBySelector('.ssh-exec-control').each(function(sshControl) {
        var row = sshControl.ancestors()[1];
        show_or_hide_row(row);
    });
    // hack for pty as can't get class onto a checkbox
    $(container).getElementsBySelector('input[name="_.usePty"]').each(function(sshControl) {
        var row = sshControl.ancestors()[1];
        show_or_hide_row(row);
    });
    // hack for agentForwarding as can't get class onto a checkbox
    $(container).getElementsBySelector('input[name="_.useAgentForwarding"]').each(function(sshControl) {
        var row = sshControl.ancestors()[1];
        show_or_hide_row(row);
    });
}

function bap_show_exec_controls(container) {
    bap_set_exec_control_visibility(container, bap_show_row);
}

function bap_hide_exec_controls(container) {
    bap_set_exec_control_visibility(container, bap_hide_row);
}

function bap_blur_inputs(container) {
    $(container).getElementsBySelector('input').each(function(inputControl) {
        fireEvent(inputControl, 'change');
    });
    $(container).getElementsBySelector('textarea').each(function(inputControl) {
        fireEvent(inputControl, 'change');
    });
}

var sshBehave = {
    "TABLE.ssh-transfer" : function(table) {
        if (bap_is_exec_disabled_for_transfer(table)) {
            bap_hide_exec_controls(table);
        } else {
            bap_show_exec_controls(table);
        }
        bap_blur_inputs(table);
    },
    "SELECT.ssh-config-name" : function(configName) {
        configName.onchange = function() {
            var publisher = bap_get_publisher_from_configName(configName);
            if (bap_is_exec_disabled(configName)) {
                bap_hide_exec_controls(publisher);
            } else {
                bap_show_exec_controls(publisher);
            }
            bap_blur_inputs(publisher);
        };
    }
};

Behaviour.register(sshBehave);