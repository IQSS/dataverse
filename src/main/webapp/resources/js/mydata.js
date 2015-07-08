var MYDATA_DEBUG_ON = true;
var APPEND_CARDS_TO_BOTTOM = false;
var SHOW_PAGINATION = false;

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

}

function bind_checkbox_labels_by_names(link_class_name, div_id_name){
    // roles
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


//-----------------------------------------
//  Called when mydata_page loads
//  Binds checkboxes, buttons, etc
//-----------------------------------------
function init_mydata_page(){

   $('#div-more-cards-link').hide();
   // var env = new nunjucks.Environment(new nunjucks.WebLoader('/mydata_templates'), true);
    //nunjucks.configure({ autoescape: true });

    // Capture checkbox clicks
    //
    $('input:checkbox').on('click',function(){ 
        $("#selected_page").val('1');
        regular_search();
    });
    
    bind_checkbox_labels();
    
    // Find button next to search box
    $('#btn_find_my_data').on('click',function(){ 
        $("#selected_page").val('1');
        regular_search();
    });

    

    // Capture pressing return in search box
    $('#mydata_search_term').keypress(function(e) {
      if (e.which == '13') {
          $("#selected_page").val('1');
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
    $( "#mydata_filter_form" ).submit(function( event ) {
      //alert( "Handler for .submit() called." );
      event.preventDefault();
      $("#selected_page").val('1');
      regular_search();

    });

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
       console.log('use page: ' + page_num);
       $("#selected_page").val(page_num);  // update the selected page in the form
       search_add_more_cards();    // run search
   });
}

//-----------------------------------------
// Bind filter tags to (a) uncheck checkbox 
//  + (b) submit search
//-----------------------------------------
function bind_filter_remove_tags(){
    console.log('bind_filter_remove_tags');
     $("a.lnk_cbox_remove").click(function(evt) {
       evt.preventDefault(); // stop link from using href
       var cbox_id = $(this).attr('rel');
        console.log('cbox_id: ' + cbox_id);
       
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
    console.log('regular_search init');
    APPEND_CARDS_TO_BOTTOM = false;
    submit_my_data_search();
}


// --------------------------------
// Reset the counts for Dataverses, Datasets, Files
// --------------------------------
var DTYPE_COUNT_VARS = ["datasets_count", "dataverses_count", "files_count"];
var PUB_TYPE_COUNT_VARS = ["published_count", "unpublished_count", "draft_count"];

function reset_filter_counts(){
     $.each( DTYPE_COUNT_VARS, function( key, attr_name ) {
          $('#id_' + attr_name).html('');
     });
    $.each( PUB_TYPE_COUNT_VARS, function( key, attr_name ) {
          $('#id_' + attr_name).html('');
     });
     
}

// --------------------------------
// Update the counts for Dataverses, Datasets, Files
// --------------------------------
// Expected JSON:    {"datasets_count":568,"dataverses_count":26,"files_count":11}}            
function update_filter_counts(json_info){
    
    var dcounts = json_info.data.dvobject_counts;
    $.each( DTYPE_COUNT_VARS, function( key, attr_name ) {
        //console.log('attr_name: ' + attr_name);
        if(attr_name in dcounts){                    
            $('#id_' + attr_name).html('(' + dcounts[attr_name] + ')');                    
        }else{
            $('#id_' + attr_name).html('');
        }
    });
    
    var pub_counts = json_info.data.pubstatus_counts;
    $.each( PUB_TYPE_COUNT_VARS, function( key, attr_name ) {
        //console.log('attr_name: ' + attr_name);
        if(attr_name in pub_counts){                    
            $('#id_' + attr_name).html('(' + pub_counts[attr_name] + ')');                    
        }else{
            $('#id_' + attr_name).html('');
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
   
        // --------------------------------
        //  If this isn't the last page, show
        //  a "more results" link after the last card
        // --------------------------------
        if (pagination_json.hasNextPageNumber === true){
            $('#lnk_add_more_cards').attr("rel", pagination_json.nextPageNumber);
            console.log("update link to: " + pagination_json.nextPageNumber);
            $('#div-more-cards-link').show();
           
            var result_label = 'results';
            if (pagination_json.numberNextResults == 1){
                result_label = 'result';
            }
            $('#lnk_add_more_cards').html('View next ' + pagination_json.numberNextResults + ' ' + result_label + ' (' + pagination_json.remainingCards + ' more)');
        }
        bindPages();
    }
}            
            
// --------------------------------
// Run the actual search!
// --------------------------------
function submit_my_data_search(){
   console.log('submit_my_data_search');
    // --------------------------------
    // Prelims: 
    // --------------------------------
    // Hide the "show more cards" button (may already be hidden)
    $('#div-more-cards-link').hide();
    // If needed, clear existing cards and pager
    if (!APPEND_CARDS_TO_BOTTOM){
        clearForNewSearchResults();
    }

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
       console.log('formData: ' + formData);

    $.getJSON( RETRIEVE_DATA_API_PATH + '?' + formData, function(data) {

        //  For debugging, show the returned JSON
        if (MYDATA_DEBUG_ON){
            $("#div-json-results").html(JSON.stringify(data));
        }

        // --------------------------------
        // (2a) Does the result look good?
        // --------------------------------
        // No, show error messsage and get out of here
               console.log('results: ' + data);

        if (!data.success){
            setWarningAlert(data.error_message);
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
            bind_filter_remove_tags();
        }
    });
}
