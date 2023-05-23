var MYDATA_DEBUG_ON = false; // activate to show json, form info, etc
var SHOW_PAGINATION = false; // pagination is available
var APPEND_CARDS_TO_BOTTOM = false;  // always starts as false

// bundle text variables
var mydataresult = '';
var mydataresults = '';
var mydataviewnext = '';
var mydatamore = '';
var draft = '';
var inreview = '';
var unpublished = '';
var published = '';
var deaccessioned = '';
var mydatato = '';
var mydataof = '';

function bind_checkbox_labels(){
    // This should be generalized to one function....once css is set
 
    // ----------------------------------
    // action: Click label next to checkbox; 
    // events: (a) check adjacent checkbox 
    //         (b) unclick other checkboxes in group; and 
    //         (c) submit form
    // ----------------------------------
    bind_checkbox_labels_by_names('mydata_dvobject_label', 'div_dvobject_types');
    bind_checkbox_labels_by_names('mydata_pubstate_label', 'div_published_states');
    bind_checkbox_labels_by_names('mydata_role_label', 'div_role_states');
    bind_checkbox_labels_by_names('mydata_validity_label', 'div_dataset_valid');

}

function bind_checkbox_labels_by_names(link_class_name, div_id_name){
    // result type
    $('a.' + link_class_name).on('click',function(){ 
        $("#selected_page").val('1');
        // Locate the closest checkbox
        var selected_checkbox = $(this).parent().closest('div').find('input[type=checkbox]');       
        $("#" + div_id_name + " input[type=checkbox]").each(function(){
            if ($(this).prop('id')== selected_checkbox.prop('id')){
                $(this).prop('checked', true);
           } else{
               $(this).prop('checked', false);
           }       
        });
       regular_search();
    });
}

function select_all_mydata_checkboxes(){
    $("#my_data_filter_column input[type=checkbox]").each(function(){
        $(this).prop('checked', true);
    });
}

function bold_checkbox_labels(){
    $("#my_data_filter_column input[type=checkbox]").each(function(){
        if ($(this).prop('checked')){
            $(this).parent().addClass('facetSelected');
       } else{
           $(this).parent().removeClass('facetSelected');
       }       
    });
}

//-----------------------------------------
//  Called when mydata_page loads
//  Binds checkboxes, buttons, etc
//-----------------------------------------
function init_mydata_page(){

   //console.log('init_mydata_page');
   $('#div-more-cards-link').hide();
   // var env = new nunjucks.Environment(new nunjucks.WebLoader('/mydata_templates'), true);
    nunjucks.configure({ autoescape: true });

    // Capture checkbox clicks
    //
    $('input:checkbox').on('click',function(){ 
        $("#selected_page").val('1');
        regular_search();
    });
        
    
    // DvObject checkbox labels have different action
    bind_checkbox_labels();

    // Search button next to search box
    $('#btn_find_my_data').on('click',function(e){ 
        e.preventDefault();
        $("#selected_page").val('1');
        select_all_mydata_checkboxes();
        regular_search();
    });
   
    // Capture pressing return in search box
    $('#mydata_search_term').keypress(function(e) {
      if (e.which == '13') {
          e.preventDefault();
          $("#selected_page").val('1');
            select_all_mydata_checkboxes();
            regular_search();
      }
    });

    // Capture pressing return in user identifier box
    $('#userIdentifier').keypress(function(e) {
      if (e.which == '13') {
          regular_search();
      }
    });

    // Normal form submit
    /*   mydata_filter_form removed b/c of JSF conflict (ahh..)
    $( "#mydata_filter_form" ).submit(function( event ) {
      //alert( "Handler for .submit() called." );
      event.preventDefault();
      $("#selected_page").val('1');
      regular_search();

    });
    */
    // Run the initial search after user loads .xhtml page
    regular_search(); // run initial search

} // end init_mydata_page


//-----------------------------------------
// clear page elements before displaying new results
//-----------------------------------------
function clearForNewSearchResults(){
    reset_filter_counts();
    clearWarningAlert();
    clearCardResults();
    clearPaginationResults();
    clearJsonResults();
}
function clearJsonResults(){ $("#div-json-results").html('');}
function clearWarningAlert(){ $('#div-result-message').html('');}
function clearCardResults(){ $('#div-card-results').html('');}
function clearPaginationResults(){ $("#div-pagination").html(''); }

//-----------------------------------------
// Show alert with error message
//-----------------------------------------
function setWarningAlert(alert_msg){
    clearCardResults();
    clearPaginationResults();
    var alert_html = '<div class="alert alert-warning" role="alert">' + alert_msg + '</div>';
    $('#div-result-message').html(alert_html);
}

//-----------------------------------------
// Bind pager buttons and "more" scroll button
//-----------------------------------------
function bindPages(){
    // bind pager buttons
    $("a.page_link").click(function(evt) {
       evt.preventDefault(); // stop link from using href
       var page_num = $(this).attr('rel');
       $("#selected_page").val(page_num);  // update the selected page in the form
       regular_search();    // run search
   });
   // bind the "more" button
   $( "#lnk_add_more_cards").unbind( "click" ); // undo last bind
   $("#lnk_add_more_cards").click(function(evt){
       evt.preventDefault(); // stop link from using href
       var page_num = $(this).attr('rel');
       //console.log('use page: ' + page_num);
       $("#selected_page").val(page_num);  // update the selected page in the form
       search_add_more_cards();    // run search
   });
}

/*-----------------------------------------
  These were "flags" at the top of the page
  showing selected categories--they have been removed

  Bind filter tags to (a) uncheck checkbox 
   + (b) submit search
 ----------------------------------------- */
function bind_filter_remove_tags(){
    //console.log('bind_filter_remove_tags');
     $("a.lnk_cbox_remove").click(function(evt) {
       evt.preventDefault(); // stop link from using href
       var cbox_id = $(this).attr('rel');
        //console.log('cbox_id: ' + cbox_id);
       
       $("#" + cbox_id).prop('checked', false);
       regular_search();    // run search
   });  
}

//-----------------------------------------
// Search activated by "more" scroll button
//-----------------------------------------
function search_add_more_cards(){
    //console.log('search_add_more_cards init');
    APPEND_CARDS_TO_BOTTOM = true;
    submit_my_data_search();
}
//-----------------------------------------
// Fresh search from checkboxes, pagers, search box, etc
//-----------------------------------------
function regular_search(){
    //console.log('regular_search init');
    APPEND_CARDS_TO_BOTTOM = false;
    submit_my_data_search();
}


// --------------------------------
// Reset the counts for DvObject and Publication Status facets 
// --------------------------------
var DTYPE_COUNT_VARS = ["datasets_count", "dataverses_count", "files_count"];   // matches variables in JSON
var PUB_STATUS_COUNT_VARS = ["published_count", "unpublished_count", "draft_count", "deaccessioned_count", "in_review_count"];   // matches variables in JSON

function reset_filter_counts(){
     $.each( DTYPE_COUNT_VARS, function( key, attr_name ) {
          $('#id_' + attr_name).html('');
     });
    $.each( PUB_STATUS_COUNT_VARS, function( key, attr_name ) {
          $('#id_' + attr_name).html('');
     });    
}

/* ----------------------------------------------------
 Update the counts for DvObject and Publication Status facets 
 
 Expected JSON for Dataverses, Datasets, Files:    
    "dvobject_counts":{"datasets_count":568,"dataverses_count":26,"files_count":11}}         

 Expected JSON for Publication Statuses:    
    "pubstatus_counts":{"draft_count":439,"deaccessioned_count":2,"unpublished_count":441,"published_count":25}, 
 ---------------------------------------------------- */
function update_filter_counts(json_info){
   
    /* ----------------------------------------------------
      (1) Update dvobject counts        
     ---------------------------------------------------- */
    // Example: "total_dvobject_counts": {"files_count":10,"dataverses_count":25,"datasets_count":324}
    // --------------------------------------------------
    var dcounts = json_info.data.dvobject_counts;
    really_update_filter_counts(DTYPE_COUNT_VARS, dcounts);
    
    
    /* ----------------------------------------------------
     (2) Update publication status counts        
    ---------------------------------------------------- */    
    var pub_counts = json_info.data.pubstatus_counts;
    really_update_filter_counts(PUB_STATUS_COUNT_VARS, pub_counts);
   
}

/* ----------------------------------------------------
  Example values:
  
  attr_name_list = ["datasets_count", "dataverses_count", "files_count"]; 
  json_counts = {"datasets_count":568,"dataverses_count":26,"files_count":11}        
 ---------------------------------------------------- */
function really_update_filter_counts(attr_name_list, json_counts){
    
    // Iterate through the attributes
    //
    $.each( attr_name_list, function( key, attr_name ) {
                
        var cnt_span_obj = $('#id_' + attr_name);   // Select the span that holds the count        
        var cbox_obj = cnt_span_obj.parent().parent().prev('input');  // Select the associated checkbox
        var is_cbox_checked = cbox_obj.prop('checked');  // Is checkbox checked?
        
        if (attr_name in json_counts){     // Is the count variable available in the JSON?               
            
            var facet_count = commaSeparateNumber(json_counts[attr_name]);    // YES, get the count

            if (is_cbox_checked){       // Is the checkbox selected?
                cnt_span_obj.html('(' + facet_count  +')'); // Yes, show the count, even if 0
            }else{
                // Checkbox not selected
                if (facet_count > 0){
                    cnt_span_obj.html('(' + facet_count  +')'); // But count is above 0, so show it
                }else{
                    cnt_span_obj.html('');  // Unchecked + no count,  blank it out
                }
            }                                    
        }else{
            cnt_span_obj.html('');  // Attribute name not returned, blank it out
        }
    });
}

/**
 * Create the pager inside the pagination div
 * 
 * Example pagination data:
 * 
 * {"isNecessary":true,"numResults":361,"docsPerPage":10,"selectedPageNumber":4,
 * "pageCount":37,"pageNumberList":[2,3,4,5,6],"hasPreviousPageNumber":true,
 * "previousPageNumber":3,"hasNextPageNumber":true,"nextPageNumber":5,
 * "startCardNumber":31,"endCardNumber":40}
 * 
 **/
function updatePagination(json_data){

    if (json_data.data.hasOwnProperty('pagination')){
        var pagination_json = json_data.data.pagination;        
        if (SHOW_PAGINATION){
            // Use nunjucks to create the pagination HTML
            var pagination_html =  nunjucks.render('mydata_templates/pagination.html', pagination_json);
        
            // Put the pagination HTML into the div
            $("#div-pagination").html(pagination_html);
        }else{
            // Use nunjucks to create the pagination HTML
            var result_msg_html =  nunjucks.render('mydata_templates/result_message_only.html', json_data);
        
            // Put the pagination HTML into the div
            $("#div-pagination").html(result_msg_html);
            
        }

        $('#result').html(mydataresult);
        $('#results').html(mydataresults);

        $('#to').html(mydatato);
        $('#of').html(mydataof);


        // --------------------------------
        //  If this isn't the last page, show
        //  a "more results" link after the last card
        // --------------------------------
        if (pagination_json.hasNextPageNumber === true){
            $('#lnk_add_more_cards').attr("rel", pagination_json.nextPageNumber);
            //console.log("update link to: " + pagination_json.nextPageNumber);
            $('#div-more-cards-link').show();

            var view_next = mydataviewnext;
            var more = mydatamore;
            var result_label = mydataresults;
            if (pagination_json.numberNextResults == 1){
                result_label = mydataresult;
            }
            $('#lnk_add_more_cards').html(view_next + ' ' + pagination_json.numberNextResults + ' ' + result_label + ' (' + pagination_json.remainingCards + ' ' + more +')');
        }
        bindPages();
    }
}            

// stackoverflow: http://stackoverflow.com/questions/3883342/add-commas-to-a-number-in-jquery
function commaSeparateNumber(val){
    while (/(\d+)(\d{3})/.test(val.toString())){
      val = val.toString().replace(/(\d+)(\d{3})/, '$1'+','+'$2');
    }
    return val;
}
  
// If an image failed to load, then display the default icon 
//
function check_card_images(){
    $("img").on("error",function () {
        if ($(this).hasClass('file_card_img')){
            $(this).next("span").show(); // show default icon in adjacent span
            $(this).hide(); // hide the actual image
            $(this).unbind("error"); 
        }
    });    
}
            
// --------------------------------
// Run the actual search!
// --------------------------------
function submit_my_data_search(){
    //console.log('submit_my_data_search');
    // --------------------------------
    // Prelims: 
    // --------------------------------
    // Hide the "show more cards" button (may already be hidden)
    $('#div-more-cards-link').hide();
    // If needed, clear existing cards and pager
    if (!APPEND_CARDS_TO_BOTTOM){
        clearForNewSearchResults();
    }
    
    // Style labels
    bold_checkbox_labels();

    // --------------------------------
    // (1) Get the form parameters
    // --------------------------------
    // ah, but with the horribly coded xhtml page, we can't use form tags...
    //var formData = $('#mydata_filter_form').serialize();
    var formData = $("#my_data_filter_column :input").serialize() + '&' + $("#my_data_filter_column2 :input").serialize() ;

    // For debugging, show the search params
    if (MYDATA_DEBUG_ON){
        $("#div-search-params").show();
        $("#div-search-params").html(formData);
    }

    // --------------------------------
    // (2) submit the form
    // --------------------------------
       //console.log('formData: ' + formData);
       
       $('#ajaxStatusPanel_start').show();

    $.getJSON( RETRIEVE_DATA_API_PATH + '?' + formData, function(data) {

        //  For debugging, show the returned JSON
        if (MYDATA_DEBUG_ON){
            $("#div-json-results").html(JSON.stringify(data));
        }

        // --------------------------------
        // (2a) Does the result look good?
        // --------------------------------
        // No, show error messsage and get out of here
        //console.log('results: ' + data);

        if (!data.success){
            setWarningAlert(data.error_message);
            $('#ajaxStatusPanel_start').hide();
            return;
        }

        // --------------------------------
        // (2b) Looks good, let's make page
        // --------------------------------
        if (data.success){
            // --------------------------------
            // (3) If JSON has pagination info, make a pager
            // --------------------------------
            updatePagination(data);
            
            // --------------------------------
            // (4) Let's render the cards
            // --------------------------------
            // Pass the solr docs to the cards template
            var card_html =  nunjucks.render('mydata_templates/cards_minimum.html', data);
            if (APPEND_CARDS_TO_BOTTOM){
                //console.log('add cards to bottom results');
                // Add new cards after existing cards
                var newCardsDiv = $('<div></div>', { css: { 'display': 'none' }});
                newCardsDiv.html(card_html);
                
                $("#div-card-results").append(newCardsDiv);
                newCardsDiv.slideDown('slow');;
                
            }else{
                //console.log('regular search results');
                // Only show new cards
                $("#div-card-results").html(card_html);                            
            }
            
            // --------------------------------
            // (5) Update the item counts
            // --------------------------------
            // Expected JSON:    {"datasets_count":568,"dataverses_count":26,"files_count":11}}            
            update_filter_counts(data);
            check_card_images();
            // bind_filter_remove_tags();
            $('#ajaxStatusPanel_start').hide();


            if ($("span.label.draft")) {
                var y = $("span.label.draft");
                for (var i = 0; i < y.length; i++) {
                    y[i].innerHTML = draft;
                }
            }
            if ($("span.label.inreview")) {
                var y = $("span.label.inreview");
                for (var i = 0; i < y.length; i++) {
                    y[i].innerHTML = inreview;
                }
            }
            if ($("span.label.published")) {
                var y = $("span.label.published");
                for (var i = 0; i < y.length; i++) {
                    y[i].innerHTML = published;
                }
            }
            if ($("span.label.unpublished")) {
                var y = $("span.label.unpublished");
                for (var i = 0; i < y.length; i++) {
                    y[i].innerHTML = unpublished;
                }
            }
            if ($("span.label.deaccessioned")) {
                var y = $("span.label.deaccessioned");
                for (var i = 0; i < y.length; i++) {
                    y[i].innerHTML = deaccessioned;
                }
            }
            if ($("span.label.incomplete")) {
                var y = $("span.label.incomplete");
                for (var i = 0; i < y.length; i++) {
                    y[i].innerHTML = incomplete;
                }
            }


            // --------------------------------
            // (6) Update address bar
            // --------------------------------
            /*if (history.pushState){
                window.history.pushState("object or string", "MyData", "/dataverseuser.xhtml?selectTab=dataRelatedToMe&" + formData);
            }*/
        }
    });
}
