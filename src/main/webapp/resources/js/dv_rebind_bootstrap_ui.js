/*
 * Rebind bootstrap UI components after Primefaces ajax calls
 */
function bind_bsui_components(){
    // Breadcrumb Tree Keep Open
    $(document).on('click', '.dropdown-menu', function (e) {
        $(this).hasClass('keep-open'),
        e.stopPropagation();
    });
    // Collapse Header Icons
    $('div[id^="panelCollapse"]').on('shown.bs.collapse', function () {
      $(this).siblings('div.panel-heading').children('span.glyphicon').removeClass("glyphicon-chevron-down").addClass("glyphicon-chevron-up");
    });

    $('div[id^="panelCollapse"]').on('hidden.bs.collapse', function () {
      $(this).siblings('div.panel-heading').children('span.glyphicon').removeClass("glyphicon-chevron-up").addClass("glyphicon-chevron-down");
    });
    
    // Hide open tooltips
    $('div.tooltip').hide();

    // Tooltip + popover functionality
    bind_tooltip_popover();

    // Disabled
    disabledLinks();
    
    // Sharrre
    sharrre();
    
    // Custom Popover with HTML code snippet -- from dataverse_template
    popoverHTML();
    
    //Metrics
    //DISABLED TOGGLE UNTIL FURTHER DEVELOPMENT ON METRICS IS COMPLETED
    //metricsTabs();
    
    // Dialog Listener For Calling handleResizeDialog
    PrimeFaces.widget.Dialog.prototype.postShow = function() {
        var dialog_id = this.jq.attr('id').split(/[:]+/).pop();
        handleResizeDialog(dialog_id);
    }

}

function dataset_fileupload_rebind(){
    //console.log('dataset_fileupload_rebind');
    bind_bsui_components();
    // rebind for dropdown menus on restrict button
    $('.dropdown-toggle').dropdown();

}

function dataverseuser_page_rebind(){
    bind_bsui_components();
    // rebind for dropdown menus on dataverseuser.xhtml
    $('.dropdown-toggle').dropdown();

}

function bind_tooltip_popover(){
    // rebind tooltips and popover to all necessary elements
    $(".bootstrap-button-tooltip, [data-toggle='tooltip'], #citation span.glyphicon").tooltip({container: 'body'});
    $("span[data-toggle='popover']").popover();
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
 * Hide notification message
 */
function hide_info_msg(){
    if ($('div.messagePanel').length > 0){
        $('div.messagePanel').html('');
    }
}

/*
 * Show notification message
 */
function show_info_msg(mtitle, mtext){
   if ($('div.messagePanel').length > 0){
       edit_msg = '<div class="alert alert-dismissable alert-info"><button type="button" class="close" data-dismiss="alert" aria-hidden="true">x</button>'
                       + '<span class="glyphicon glyphicon-info-sign"/>'
                       + '<strong> ' + mtitle + '</strong> &#150; ' + mtext + '</div>';
       $('div.messagePanel').html(edit_msg );
   }else{
     //console.log('message panel does not exist');
   }
}


/*
 * Called after "Edit Dataverse" - "General Information"
 */
function post_edit_dv_general_info(){
    // show_info_msg('Edit Dataverse', 'Edit your dataverse and click Save Changes. Asterisks indicate required fields.');
    // hide_search_panels();
    bind_bsui_components();
}

/*
 * Used after cancelling "Edit Dataverse"
 */
function post_cancel_edit_dv(){
   // show_search_panels()
   // hide_info_msg();
   bind_bsui_components();
   initCarousel();
}

/*
 * Hide search panels when editing a dv
 * NO LONGER IN USE, INSTEAD ADDED p:fragment TO DV PG
 */
//function hide_search_panels(){
//    if($(".panelSearchForm").length>0){
//       $(".panelSearchForm").hide();
//        if($(".panelSearchForm").next().length>0){
//            $(".panelSearchForm").next().hide();
//        }
//   }
//}

/*
 * Show search panels when cancel a dv edit
 * NO LONGER IN USE, INSTEAD ADDED p:fragment TO DV PG
 */
//function show_search_panels(){
//    if($(".panelSearchForm").length>0){
//        if($(".panelSearchForm").next().length>0){
//            $(".panelSearchForm").next().show();
//        }
//       $(".panelSearchForm").show();
//   }
//}


/*
 * Called after "Upload + Edit Files"
 */
function post_edit_files(){
   bind_bsui_components();
}

/*
 * Called after "Edit Metadta"
 */
function post_edit_metadata(){
   bind_bsui_components();
}

/*
 * Called after "Edit Terms"
 */

function post_edit_terms(){
   bind_bsui_components();
}

/*
 *  Used when cancelling either "Upload + Edit Files" or "Edit Metadata"
 */
function post_cancel_edit_files_or_metadata(){
   bind_bsui_components();
}

/*
* Custom Popover with HTML code snippet
*/
function popoverHTML(popoverTitleHTML) {

   var popoverTemplateHTML = ['<div class="popover">',
       '<div class="arrow"></div>',
       '<h3 class="popover-title"></h3>',
       '<div class="popover-content">',
       '</div>',
       '</div>'].join('');

   var popoverContentHTML = ['<code>',
       '&lt;a&gt;, &lt;b&gt;, &lt;blockquote&gt;, &lt;br&gt;, &lt;code&gt;, &lt;del&gt;, &lt;dd&gt;, &lt;dl&gt;, &lt;dt&gt;, &lt;em&gt;, &lt;hr&gt;, &lt;h1&gt;-&lt;h3&gt;, &lt;i&gt;, &lt;img&gt;, &lt;kbd&gt;, &lt;li&gt;, &lt;ol&gt;, &lt;p&gt;, &lt;pre&gt;, &lt;s&gt;, &lt;sup&gt;, &lt;sub&gt;, &lt;strong&gt;, &lt;strike&gt;, &lt;ul&gt;',
       '</code>'].join('');

   $('body').popover({
       selector: 'span.popoverHTML',
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
            googlePlus: true
        },
        template: '<div id="sharrre-block" class="clearfix">\n\
                    <input type="hidden" id="sharrre-total" name="sharrre-total" value="{total}"/> \n\
                    <a href="#" class="sharrre-facebook"><span class="socicon socicon-facebook"/></a> \n\
                    <a href="#" class="sharrre-twitter"><span class="socicon socicon-twitter"/></a> \n\
                    <a href="#" class="sharrre-google"><span class="socicon socicon-google"/></a>\n\
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
            $(api.element).on('click', '.sharrre-google', function() {
                api.openPopup('googlePlus');
            });
            
            // Count not working... Coming soon...
            // var sharrrecount = $('#sharrre-total').val();
            // $('#sharrre-count').prepend(sharrrecount);
        }
    });
}

/*
 * Metrics Tabs
 * DISABLED TOGGLE UNTIL FURTHER DEVELOPMENT ON METRICS IS COMPLETED
 */
// function metricsTabs() {
    // $('#metrics-tabs a[data-toggle="tab"]').on('shown', function (e) {
        // e.target // activated tab
        // e.relatedTarget // previous tab
    // });
    // $('#metrics-tabs a[data-toggle="tab"]').mouseover(function(){
        // $(this).click();
    // });
    // $('#metrics-tabs a.first[data-toggle="tab"]').tab('show');
// }

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