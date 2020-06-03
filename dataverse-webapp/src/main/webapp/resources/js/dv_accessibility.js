/**
 * This script handles managing user settings regarding accessibility content (high contrast version, etc.).
 */

/**
 * Stores the information about the user settings (not the values themselves).
 * @type {Object.<string, string>} Setting name in storage, Setting class name prefix
 */
var accessibilityUserPreferencesData = {
    fontSize: "font-size",
    highContrastMode: "high-contrast"
}



/**
 * Get a setting value from the storage. null if not set.
 * @param string Setting name.
 * @return Setting value.
 */
function accessibilityGetSetting(key) {
    return localStorage.getItem(key);
}

/**
 * Change a setting in the storage.
 * @param string Setting name.
 * @param string Setting value.
 */
function accessibilitySetSetting(key, value) {
    localStorage.setItem(key, value);
}

/**
 * Remove a setting from the storage.
 * @param string Setting name.
 */
function accessibilityRemoveSetting(key) {
    localStorage.removeItem(key);
}



/**
 * Add a setting class to the body tag. Value is taken from the storage.
 * @param string Setting name.
 */
function accessibilityAddSettingClass(setting) {
    if (!(setting in accessibilityUserPreferencesData)) {
        return;
    }

    if (accessibilityGetSetting(setting)) {
        document.body.classList.add(accessibilityUserPreferencesData[setting] + "-" + accessibilityGetSetting(setting));
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
            element.classList.remove(element.classList[i]);
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
        return;
    }
    
    if (value && value !== "default") {
        accessibilitySetSetting(setting, value);
        accessibilityRemoveSettingClass(setting);
        accessibilityAddSettingClass(setting);

        if (setting === "fontSize") {
            accessibilityTogglenavbar(true);
        }
    }
    else {
        accessibilityRemoveSetting(setting);
        accessibilityRemoveSettingClass(setting);

        if (setting === "fontSize") {
            accessibilityTogglenavbar(false);
        }
    }
}

/**
 * Toggle the visibility of the mobile navbar.
 * @param boolean true -> visible, false -> hidden
 */
function accessibilityTogglenavbar(visible) {
    var navbar = document.getElementById("topNavBar");

    if (visible) {
        navbar.classList.add("in");
        navbar.setAttribute("aria-expanded", "true");
        navbar.style = "";
    }
    else {
        navbar.classList.remove("in");
        navbar.setAttribute("aria-expanded", "false");
        navbar.style = "height: 1px";
    }
}

/**
 * Load current settings from storage and apply them to the document.
 */
function accessibilityApplyAllClasses() {
    for (key in accessibilityUserPreferencesData) {
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
        }
    }
}



accessibilityApplyAllClasses();

/*
Commented out because some pages reloaded the header, removing button bindings.
Method invocation moved to dataverse_header.xhtml

document.addEventListener('DOMContentLoaded', (event) => {
    accessibilityBindButtonEvents();
});
*/
