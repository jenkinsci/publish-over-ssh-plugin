var exec_disabled = [];

function bap_is_exec_disabled(configName) {
    return exec_disabled[configName.value];
}

function bap_get_configName_from_publisher(publisher) {
    return publisher.querySelector('.ssh-config-name');
}

function bap_is_exec_disabled_for_publisher(publisher) {
    var configName = bap_get_configName_from_publisher(publisher);
    return bap_is_exec_disabled(configName);
}

function bap_get_publisher_from_setting(setting) {
    var transfer = setting.parentNode.parentNode.parentNode.parentNode;
    return bap_get_publisher_from_transfer(transfer);
}

function bap_get_publisher_from_transfer(transfer) {
    return transfer.parentNode.parentNode.parentNode.parentNode.parentNode.parentNode;
}

function bap_get_publisher_from_configName(configName) {
    return configName.parentNode.parentNode.parentNode.parentNode;
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
    row.style.display = "";
    row.nextElementSibling.style.display = "";
}

function bap_hide_row(row) {
    row.style.display = "none";
    row.nextElementSibling.style.display = "none";
}

function bap_set_exec_control_visibility(container, show_or_hide_row) {
    container.querySelectorAll('.ssh-exec-control').forEach(function(sshControl) {
        var row = sshControl.parentNode.parentNode;
        show_or_hide_row(row);
    });
    // hack for pty as can't get class onto a checkbox
    container.querySelectorAll('input[name="_.usePty"]').forEach(function(sshControl) {
        var row = sshControl.parentNode.parentNode;
        show_or_hide_row(row);
    });
    // hack for agentForwarding as can't get class onto a checkbox
    container.querySelectorAll('input[name="_.useAgentForwarding"]').forEach(function(sshControl) {
        var row = sshControl.parentNode.parentNode;
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
    container.querySelectorAll('input').forEach(function(inputControl) {
        fireEvent(inputControl, 'change');
    });
    container.querySelectorAll('textarea').forEach(function(inputControl) {
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