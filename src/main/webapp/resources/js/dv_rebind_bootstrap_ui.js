/* 
 * Rebind bootstrap UI components
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
    $("a[data-toggle='tooltip'], #citation span.glyphicon").tooltip({container: 'body'});
    $("a[data-toggle='popover']").popover();

}

/*
 * Called after "Edit Dataverse"
 */
function post_edit_dv(){
    
   hide_breadcrumb();
   bind_bsui_components();
   var dv_srch_panel = $('#dv-sidecolumn').parent();
   if (dv_srch_panel.length > 0){
        dv_srch_panel.hide();
    }
}

/*
 * Used after cancelling "Edit Dataverse"
 */
function post_cancel_edit_dv(){
   show_breadcrumb();
   bind_bsui_components();
   var dv_srch_panel = $('#dv-sidecolumn').parent();
   if (dv_srch_panel.length > 0){
        dv_srch_panel.show();
    }
}

/*
 * Called after "Upload + Edit Files"
 */
function post_edit_files(){
   //console.log('post_edit_files');
   hide_breadcrumb();
   bind_bsui_components();
   show_info_msg('Upload + Edit Dataset Files', 'You can drag and drop your files from your desktop, directly into the upload widget.');
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
   hide_edit_msg();
}
function show_breadcrumb(){
   $('#breadcrumbNavBlock').show();  // show breadcrumb navigation
}
function hide_breadcrumb(){
    $('#breadcrumbNavBlock').hide();  // hide breadcrumb navigation
}

/*
 * Hide notification message
 */
function hide_edit_msg(){
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

