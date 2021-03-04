/*
 * Rebind bootstrap UI components after Primefaces ajax calls
 */
function bind_bsui_components(){
    // Facet panel Filter Results btn toggle
    $(document).on('click', '[data-toggle=offcanvas]', function() {
        $('.row-offcanvas').toggleClass('active', 200);
    });
    
    // Collapse Header Icons
    $('div[id^="panelCollapse"]').on('shown.bs.collapse', function () {
      $(this).siblings('div.panel-heading').children('span.glyphicon').removeClass("glyphicon-chevron-down").addClass("glyphicon-chevron-up");
    });

    $('div[id^="panelCollapse"]').on('hidden.bs.collapse', function () {
      $(this).siblings('div.panel-heading').children('span.glyphicon').removeClass("glyphicon-chevron-up").addClass("glyphicon-chevron-down");
    });
    
    // Button dropdown menus 
    $('.dropdown-toggle').dropdown();
    
    // Hide open tooltips + popovers
    $('.bootstrap-button-tooltip, [data-toggle="tooltip"]').tooltip("hide");
    $("[data-toggle='popover']").popover("hide");

    // Tooltips + popovers
    bind_tooltip_popover();

    // Disabled pagination links
    disabledLinks();
    
    // Truncate checksums
    checksumTruncate();
    
    // Sharrre
    sharrre();
    
    // Custom Popover with HTML content
    popoverHTML();
    
    // clipboard.js click to copy
    clickCopyClipboard();
    
    // Scrolling autoComplete dropdown in popups
    handle_dropdown_popup_scroll();
    
    // Dialog Listener For Calling handleResizeDialog
    PrimeFaces.widget.Dialog.prototype.postShow = function() {
        var dialog_id = this.jq.attr('id').split(/[:]+/).pop();
        handleResizeDialog(dialog_id);
    }
}

function bind_tooltip_popover(){
    // rebind tooltips and popover to all necessary elements
    $(".bootstrap-button-tooltip, [data-toggle='tooltip']").tooltip({container: 'body'});
    $("[data-toggle='popover']").popover({container: 'body'});
    
    // CLOSE OPEN TOOLTIPS + POPOVERS ON BODY CLICKS
    $('body').on("touchstart", function(e){
        $(".bootstrap-button-tooltip, [data-toggle='tooltip']").each(function () {
            // hide any open tooltips when anywhere else in body is clicked
            if (!$(this).is(e.target) && $(this).has(e.target).length === 0 && $('div.tooltip').has(e.target).length === 0) {
                $(this).tooltip('hide');
            }////end if
        });
        $("a.popoverHTML, [data-toggle='popover']").each(function () {
            //the 'is' for buttons that trigger popups
            //the 'has' for icons within a button that triggers a popup
            if (!$(this).is(e.target) && $(this).has(e.target).length === 0 && $('div.popover').has(e.target).length === 0) {
                $(this).popover('hide');
            }
        });
    });
    
    // CLOSE OPEN TOOLTIPS ON BUTTON CLICKS
    $('.bootstrap-button-tooltip').on('click', function () {
        $(this).tooltip('hide');
    });
}

function toggle_dropdown(){
    $('.btn-group.open').removeClass('open');
}

function disabledLinks(){
    $('ul.pagination li').on('click', 'a', function (e) {
        if ($(this).parent().hasClass('disabled')){
            e.preventDefault();
        }
    });
}

/*
* Custom Popover with HTML code snippet
*/
function popoverHTML(popoverTitleHTML, popoverTagsHTML) {
   var popoverTemplateHTML = ['<div class="popover">',
       '<div class="arrow"></div>',
       '<h3 class="popover-title"></h3>',
       '<div class="popover-content">',
       '</div>',
       '</div>'].join('');
   var popoverContentHTML = ['<code>', popoverTagsHTML, '</code>'].join('');
   $('body').popover({
       selector: 'a.popoverHTML',
       title: popoverTitleHTML,
       trigger: 'hover',
       content: popoverContentHTML,
       template: popoverTemplateHTML,
       placement: "bottom",
       container: "#content",
       html: true
   });
}

/*
 * Equal Div Height
 */
function post_differences(){
       var dialogHeight = $('div[id$="detailsBlocks"].ui-dialog').outerHeight();
       var dialogHeader = $('div[id$="detailsBlocks"] .ui-dialog-titlebar').outerHeight();
       var dialogScroll = dialogHeight - dialogHeader;
       $('div[id$="detailsBlocks"] .ui-dialog-content').css('height', dialogScroll);
}

/*
 * Sharrre
 */
function sharrre(){
    $('#sharrre-widget').sharrre({
        share: {
            facebook: true,
            twitter: true,
            linkedin: true
        },
        template: '<div id="sharrre-block" class="clearfix">\n\
                    <input type="hidden" id="sharrre-total" name="sharrre-total" value="{total}"/> \n\
                    <a href="#" class="sharrre-facebook"><span class="socicon socicon-facebook"/></a> \n\
                    <a href="#" class="sharrre-twitter"><span class="socicon socicon-twitter"/></a> \n\
                    <a href="#" class="sharrre-linkedin"><span class="socicon socicon-linkedin"/></a>\n\
                    </div>',
        enableHover: false,
        enableTracking: true,
        urlCurl: '',
        render: function(api, options){
            $(api.element).on('click', '.sharrre-twitter', function() {
                api.openPopup('twitter');
            });
            $(api.element).on('click', '.sharrre-facebook', function() {
                api.openPopup('facebook');
            });
            $(api.element).on('click', '.sharrre-linkedin', function() {
                api.openPopup('linkedin');
            });
            
            // Count not working... Coming soon...
            // var sharrrecount = $('#sharrre-total').val();
            // $('#sharrre-count').prepend(sharrrecount);
        }
    });
}
    
/*
 * Truncate file checksums
 */
function checksumTruncate(){
    $('span.checksum-truncate').each(function () {
        $(this).toggleClass('sr-only').toggleClass('visisble');
        var checksumText = $(this).text();
        var checksumLength = checksumText.length;
        if (checksumLength > 25) {
            // COUNT " " IN TYPE LABEL, UNF HAS NONE
            var prefixCount = (checksumText.match(/ /g) || []).length;
            
            // INDEX OF LAST ":" IN TYPE LABEL, UNF HAS MORE THAN ONE
            var labelIndex = checksumText.lastIndexOf(':');
            
            // COUNT "=" IN UNF SUFFIX
            var suffixCount = (checksumText.match(/=/g) || []).length;
            
            // TRUNCATE MIDDLE W/ "..." + FIRST/LAST 3 CHARACTERS
            // CHECK IF UNF LABEL, LESS THAN ONE " "
            if (prefixCount < 0) {
                $(this).text(checksumText.substr(0,(labelIndex + 3)) + '...' + checksumText.substr((checksumLength - suffixCount - 3),checksumLength));
            }
            else {
                $(this).text(checksumText.substr(0,(labelIndex + 5)) + '...' + checksumText.substr((checksumLength - suffixCount - 3),checksumLength));
            }
        }
    });
    $('span.checksum-tooltip').on('inserted.bs.tooltip', function () {
        $("body div.tooltip-inner").css("word-break", "break-all");
    });
}

function clickCopyClipboard(){
    // clipboard.js click to copy
    // pass selector to clipboard
    var clipboard = new ClipboardJS('button.btn-copy, span.checksum-truncate, span.btn-copy');

    clipboard.on('success', (e)=> {
        // DEV TOOL DEBUG
        // console.log(e);

        // check which selector was clicked
        // swap icon for success ok
        if ($(e.trigger).hasClass('glyphicon')) {
            $(e.trigger).removeClass('glyphicon-copy').addClass('glyphicon-ok text-success');
            // then swap icon back to clipboard
            // https://stackoverflow.com/a/54270499
            setTimeout(()=> { // use arrow function
                $(e.trigger).removeClass('glyphicon-ok text-success').addClass('glyphicon-copy')
            }, 2000);
        }
        else {
            $(e.trigger).next('.btn-copy.glyphicon').removeClass('glyphicon-copy').addClass('glyphicon-ok text-success');
            setTimeout(()=> {
                $(e.trigger).next('.btn-copy.glyphicon').removeClass('glyphicon-ok text-success').addClass('glyphicon-copy')
            }, 2000);
        }
    });
    clipboard.on('error', (e)=> {
        console.log(e);
    });
}

/*
 * Select dataset/file citation onclick event
 */
function selectText(ele) {
    try {
        var div = document.createRange();
        div.setStartBefore(ele);
        div.setEndAfter(ele);
        window.getSelection().addRange(div)
    } catch (e) {
        // for internet explorer
        div = document.selection.createRange();
        div.moveToElementText(ele);
        div.select()
    }
}

/*
 * Dialog Height-Scrollable
 */
function handleResizeDialog(dialog) {
        var el = $('div[id$="' + dialog + '"]');
        var doc = $('body');
        var win = $(window);
        var elPos = '';
        
        function calculateResize() {
            var overlay = $('#' + dialog + '_modal');
            var bodyHeight = '';
            var bodyWidth = '';
        
            // position:fixed is maybe cool, but it makes the dialog not scrollable on browser level, even if document is big enough
            if (el.height() > win.height()) {
                bodyHeight = el.height() + 'px';
                elPos = 'absolute';
            }
            if (el.width() > win.width()) {
                bodyWidth = el.width() + 'px';
                elPos = 'absolute';
            }
            el.css('position', elPos);
            doc.css('width', bodyWidth);
            doc.css('height', bodyHeight);
            
            var pos = el.offset();
            if (pos.top + el.height() > doc.height()) {
                    pos.top = doc.height() - el.height();
                    overlay.css('height', bodyHeight);
                }
            if (pos.left + el.width() > doc.width()) {
                    pos.left = doc.width() - el.width();
                    overlay.css('width', bodyWidth);
                }
            var offsetX = 0;
            var offsetY = 0;
            if (elPos != 'absolute') {
                offsetX = $(window).scrollLeft();
                offsetY = $(window).scrollTop();
            }
            // scroll fix for position fixed
            if (pos.left < offsetX)
                pos.left = offsetX;
            if (pos.top < offsetY)
                pos.top = offsetY;
            el.offset(pos);
        }
        
        calculateResize();
        
        el.find('textarea').each(function(index){
            $(this).on('keyup change cut paste focus', function(){
                calculateResize();
            });
        });
}

/*
 * fixes autoComplete dropdown in popups not moving with page scroll
 */
function handle_dropdown_popup_scroll(){
    $( window ).scroll(function() {
        var isActive = $(".DropdownPopupPanel").is(':visible');
        if(isActive) {
            $(".DropdownPopupPanel").position({
                my: "left top",
                at: "left bottom",
                of: $(".DropdownPopup")
            });
        }
    });
}