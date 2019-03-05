function bind_bsui_components(){
	bind_tooltip_popover();
}

function bind_tooltip_popover(){
    // bind tooltips + popovers to all necessary elements
    $jqTheme(".bootstrap-button-tooltip, [data-toggle='tooltip']").tooltip({container: 'body'});
    $jqTheme("[data-toggle='popver']").popover({container: 'body'});
    
    // CLOSE OPEN TOOLTIPS + POPOVERS ON BODY CLICKS
    $jqTheme('body').on("touchstart", function(e){
        $jqTheme(".bootstrap-button-tooltip, [data-toggle='tooltip']").each(function () {
            // hide any open tooltips when anywhere else in body is clicked
            if (!$jqTheme(this).is(e.target) && $jqTheme(this).has(e.target).length === 0 && $jqTheme('div.tooltip').has(e.target).length === 0) {
                $jqTheme(this).tooltip('hide');
            }////end if
        });
        $jqTheme("a.popoverHTML, [data-toggle='popover']").each(function () {
            //the 'is' for buttons that trigger popups
            //the 'has' for icons within a button that triggers a popup
            if (!$jqTheme(this).is(e.target) && $jqTheme(this).has(e.target).length === 0 && $jqTheme('div.popover').has(e.target).length === 0) {
                $jqTheme(this).popover('hide');
            }
        });
    });
    
    // CLOSE OPEN TOOLTIPS ON BUTTON CLICKS
    $jqTheme('.bootstrap-button-tooltip').on('click', function () {
        $jqTheme(this).tooltip('hide');
    });
}