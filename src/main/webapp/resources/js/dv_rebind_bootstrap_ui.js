/* 
 * Rebind bootstrap UI components after Primefaces ajax calls
 */
function bind_bsui_components(){
    //console.log('bind_bsui_components');
    // Breadcrumb Tree Keep Open
    $(document).on('click', '.dropdown-menu', function (e) {
        $(this).hasClass('keep-open'),
        e.stopPropagation();
    });
    // Collapse Header Icons
    $('div[id^="collapse"]').on('shown.bs.collapse', function () {
      //console.log('hello block');
      $(this).siblings('div.panel-heading').children('span.glyphicon').removeClass("glyphicon-chevron-down").addClass("glyphicon-chevron-up");
    });

    $('div[id^="collapse"]').on('hidden.bs.collapse', function () {
      //console.log('goodbye block');
       $(this).siblings('div.panel-heading').children('span.glyphicon').removeClass("glyphicon-chevron-up").addClass("glyphicon-chevron-down");
    });

    // Permissions + Dataset Tooltips/Popovers                
    $("[data-toggle='tooltip'], #citation span.glyphicon").tooltip({container: 'body'});
    $("a[data-toggle='popover']").popover();

}

/*
 * show breadcrumb navigation
 */
function show_breadcrumb(){
   $('#breadcrumbNavBlock').show();  
}

/*
 * hide breadcrumb navigation
 */
function hide_breadcrumb(){
    $('#breadcrumbNavBlock').hide();
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
     //  alert('msg panel exists');
       edit_msg = '<div class="alert alert-dismissable alert-info"><button type="button" class="close" data-dismiss="alert" aria-hidden="true">x</button>'
                       + '<span class="glyphicon glyphicon-info-sign"></span>'
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
    show_info_msg('Edit Dataverse', 'Edit your dataverse and click Save Changes. Asterisks indicate required fields.');
    post_edit_dv();
}

/*
 * Called after "Edit Dataverse" - "Setup"
 */
function post_edit_dv_setup(){
    show_info_msg('Dataverse Setup', 'Edit the Metadata Blocks and Facets you want to associate with your dataverse. Note: facets will appear in the order shown on the list.'); 
    post_edit_dv();
}
/*
 * Called after "Edit Dataverse" -  "General Information" or "Setup"
 */
function post_edit_dv(){
   hide_breadcrumb();
   hide_search_panels();
   bind_bsui_components();               
   //console.log('hide after edit3');
}

/*
 * Used after cancelling "Edit Dataverse"
 */
function post_cancel_edit_dv(){
   show_breadcrumb();
   show_search_panels()
   hide_info_msg();    
   bind_bsui_components();
   initCarousel();
   //console.log('show after cancel edit3');
}

/*
 * Hide search panels when editing a dv
 */
function hide_search_panels(){
    if($(".panelSerchForm").length>0){
       $(".panelSerchForm").hide();
        if($(".panelSerchForm").next().length>0){
            $(".panelSerchForm").next().hide();
        }
   }
}

/*
 * Show search panels when cancel a dv edit
 */

function show_search_panels(){
    if($(".panelSerchForm").length>0){
        if($(".panelSerchForm").next().length>0){
            $(".panelSerchForm").next().show();
        }
       $(".panelSerchForm").show();
   }
}

/*
 * Called after "Upload + Edit Files"
 */
function post_edit_files(){
   //console.log('post_edit_files');
   hide_breadcrumb();
   bind_bsui_components();
   addDeleteTooltip();
   //show_info_msg('Upload + Edit Dataset Files', 'You can drag and drop your files from your desktop, directly into the upload widget.');
}

function addDeleteTooltip(){
    var fileChckbx = $('div[id$="filesTable"] table td.ui-selection-column div.ui-chkbox-box span.ui-chkbox-icon');
    $(fileChckbx).wrapInner('<a href="#" data-toggle="tooltip" data-container="body" data-trigger="hover" data-placement="top" data-original-title="Delete file" onclick="event.preventDefault();" style="width:16px;height:16px;display:block;"></a>');
    $(fileChckbx).children('a[data-toggle="tooltip"]').tooltip();
}

/*
 * Called after "Edit Metadta"
 */
function post_edit_metadata(){
   //console.log('post_edit_metadata');
   hide_breadcrumb();
   bind_bsui_components();
   show_info_msg('Edit Dataset Metadata ', 'Add more metadata about your dataset to help others easily find it.');

}

/*
 *  Used when cancelling either "Upload + Edit Files" or "Edit Metadata"
 */
function post_cancel_edit_files_or_metadata(){
   //console.log('post_cancel_edit_metadata');
   show_breadcrumb();
   bind_bsui_components();
   hide_info_msg();
}

/*
 * Dialog Height-Scrollable
 */
function post_differences(){
       var dialogHeight = $('div[id$="detailsBlocks"].ui-dialog').outerHeight();
       var dialogHeader = $('div[id$="detailsBlocks"] .ui-dialog-titlebar').outerHeight();
       var dialogScroll = dialogHeight - dialogHeader;
       $('div[id$="detailsBlocks"] .ui-dialog-content').css('height', dialogScroll);
}
