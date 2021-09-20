/**
 * This script handles managing user settings regarding accessibility content (high contrast version, etc.).
 */

/**
 * Denotes whether to print debug info to console.
 * @type boolean true = enabled, false = disabled
 */

var accessibilityDebugEnabled = false;

/**
 * Prints debug info to console.
 * @param string Message to print.
 */

function accessibilityDebugLog(msg) {    
    if (accessibilityDebugEnabled) {
        console.log(msg);
    }
}

/**
 * Prints debug error to console.
 * @param string Message to print.
 */

function accessibilityDebugErr(msg) {
    if (accessibilityDebugEnabled) {
        console.err(msg);
    }
}



/**
 * Stores the information about the user settings (not the values themselves).
 * @type {Object.<string, string>} Setting name in storage, Setting class name prefix
 */
var accessibilityUserPreferencesData = {
    eightyCharactersLimit: "eighty-limit",
    wcagTextMode: "wcag-text",
    fontSize: "font-size",
    highContrastMode: "high-contrast"
}



/**
 * Get a setting value from the storage. null if not set.
 * @param string Setting name.
 * @return Setting value.
 */
function accessibilityGetSetting(key) {
    var returnValue = localStorage.getItem(key);
    accessibilityDebugLog("Reading key \"" + key + "\", received value \"" + returnValue + "\"");
    return returnValue;
}

/**
 * Change a setting in the storage.
 * @param string Setting name.
 * @param string Setting value.
 */
function accessibilitySetSetting(key, value) {
    localStorage.setItem(key, value);
    accessibilityDebugLog("Set key \"" + key + "\" value to \"" + value + "\"");
}

/**
 * Remove a setting from the storage.
 * @param string Setting name.
 */
function accessibilityRemoveSetting(key) {
    localStorage.removeItem(key);
    accessibilityDebugLog("Removed \"" + key + "\" from storage");
}



/**
 * Add a setting class to the body tag. Value is taken from the storage.
 * @param string Setting name.
 */
function accessibilityAddSettingClass(setting) {
    if (!(setting in accessibilityUserPreferencesData)) {
        accessibilityDebugErr("This setting is not defined in accessibilityUserPreferencesData");
        return;
    }

    if (accessibilityGetSetting(setting)) {
        var className = accessibilityUserPreferencesData[setting] + "-" + accessibilityGetSetting(setting)
        document.body.classList.add(className);
        accessibilityDebugLog("Added class \"" + className + "\" to body tag");
    }
}

/**
 * Remove all classes form the body tag belonging to the specified setting (ie. revert to default).
 * @param string Setting name.
 */
function accessibilityRemoveSettingClass(setting) {
    var element = document.body;
    var prefix = accessibilityUserPreferencesData[setting];

    for (var i = element.classList.length - 1; i >= 0; i--) {
        if (element.classList[i].startsWith(prefix)) {
            accessibilityDebugLog("Removed class \"" + element.classList[i] + "\" class from body tag");
            element.classList.remove(element.classList[i]);
        }
    }
}

/**
 * Set the aria-pressed attribute for WCAG buttons.
 * @param string Setting name.
 */
 function accessibilitySetAriaPressed(setting) {
    if (!(setting in accessibilityUserPreferencesData)) {
        accessibilityDebugErr("This setting is not defined in accessibilityUserPreferencesData");
        return;
    }

    var value = accessibilityGetSetting(setting);
    var buttons = document.querySelectorAll("#" + accessibilityUserPreferencesData[setting] + "-mode-selector button");

    for (var i=0; i<buttons.length; i++) {
        if (value && value === buttons[i].className || !value && buttons[i].className === "default") {
            accessibilityDebugLog("Set aria-pressed button state for \"" + setting + "\":\"" + buttons[i].className + "\" to \"true\"");
            buttons[i].setAttribute("aria-pressed", "true");
        }
        else {
            accessibilityDebugLog("Set aria-pressed button state for \"" + setting + "\":\"" + buttons[i].className + "\" to \"false\"");
            buttons[i].setAttribute("aria-pressed", "false");
        }
    }
}

/**
 * Apply setting changes to storage and body tag.
 * @param string Setting name.
 * @param string Setting value.
 */
function accessibilityApplySetting(setting, value) {
    if (!(setting in accessibilityUserPreferencesData)) {
        accessibilityDebugErr("This setting is not defined in accessibilityUserPreferencesData");
        return;
    }

    if (value && value === "toggle") {
        if (document.body.classList.contains(accessibilityUserPreferencesData[setting] + "-" + value)) {
            accessibilityDebugLog("Toggling setting \"" + setting + "\" off");
            accessibilityRemoveSetting(setting);
            accessibilityRemoveSettingClass(setting);
            accessibilitySetAriaPressed(setting);
        }
        else {
            accessibilityDebugLog("Toggling setting \"" + setting + "\" on");
            accessibilitySetSetting(setting, value);
            accessibilityRemoveSettingClass(setting);
            accessibilityAddSettingClass(setting);
            accessibilitySetAriaPressed(setting);
        }
    }
    else if (value && value !== "default") {
        accessibilityDebugLog("Changing setting \"" + setting + "\"to \"" + value + "\"");
        accessibilitySetSetting(setting, value);
        accessibilityRemoveSettingClass(setting);
        accessibilityAddSettingClass(setting);
        accessibilitySetAriaPressed(setting);

        if (setting === "fontSize") {
            accessibilityToggleNavbar(true);
        }
    }
    else {
        accessibilityDebugLog("Changing setting \"" + setting + "\"to default");
        accessibilityRemoveSetting(setting);
        accessibilityRemoveSettingClass(setting);
        accessibilitySetAriaPressed(setting);

        if (setting === "fontSize") {
            accessibilityToggleNavbar(false);
        }
    }
}

/**
 * Toggle the navbar-expanded class on body tag.
 *  @param state true - add class, false - remove class, undefined - toggle class
 */
 function accessibilityToggleNavbarBodyCass(state) {

    if (state === undefined) {
        accessibilityDebugLog("Toggling navbar-expanded class");
        document.body.classList.toggle("navbar-expanded");
    }
    else if (state) {
        accessibilityDebugLog("Adding navbar-expanded class");
        document.body.classList.add("navbar-expanded");
    }
    else {
        accessibilityDebugLog("Removing navbar-expanded class");
        document.body.classList.remove("navbar-expanded");
    }
}

/**
 * Toggle the visibility of the mobile navbar.
 * @param boolean true -> visible, false -> hidden
 */
function accessibilityToggleNavbar(visible) {
    var navbar = document.getElementById("topNavBar");

    if (window.innerWidth > 768) {
        if (visible) {
            navbar.classList.add("in");
            navbar.setAttribute("aria-expanded", "true");
            navbar.style = "";
            accessibilityToggleNavbarBodyCass(true);
            accessibilityDebugLog("Toggled navbar to its visible state");
        }
        else {
            navbar.classList.remove("in");
            navbar.setAttribute("aria-expanded", "false");
            navbar.style = "height: 1px";
            accessibilityToggleNavbarBodyCass(false);
            accessibilityDebugLog("Toggled navbar to its hidden state");
        }
    }
}

/**
 * Load current settings from storage and apply them to the document.
 */
function accessibilityApplyAllClasses() {
    for (key in accessibilityUserPreferencesData) {
        accessibilityDebugLog("Parsing accessibility setting \"" + key + "\"");
        accessibilityAddSettingClass(key);
    }
}

/**
 * Bind events to buttons in the header.
 */
function accessibilityBindButtonEvents() {
    for (key in accessibilityUserPreferencesData) {
        var buttons = document.querySelectorAll("#" + accessibilityUserPreferencesData[key] + "-mode-selector button");
        // not using for...of loop to keep IE compatibility
        for (var i=0; i<buttons.length; i++) {
            buttons[i].setAttribute("data-accessibility", key);
            buttons[i].addEventListener("click", function() {
                accessibilityApplySetting(this.dataset.accessibility, this.className);
            }, false);
            accessibilityDebugLog("Bound button event to \"" + key + "\"");
        }

        accessibilitySetAriaPressed(key);
    }

    document.querySelector(".navbar-toggle").addEventListener("click", function() {
        accessibilityToggleNavbarBodyCass();
    }, false);
}



accessibilityApplyAllClasses();

/*
Commented out because some pages reloaded the header, removing button bindings.
Method invocation moved to dataverse_header.xhtml

document.addEventListener('DOMContentLoaded', (event) => {
    accessibilityBindButtonEvents();
});
*/
