var MYDATA_DEBUG_ON = true;
var APPEND_CARDS_TO_BOTTOM = false;
var SHOW_PAGINATION = true;

      
          
function init_mydata_page(){

   $('#div-more-cards-link').hide();
   // var env = new nunjucks.Environment(new nunjucks.WebLoader('/mydata_templates'), true);
    //nunjucks.configure({ autoescape: true });

    // Capture checkbox clicks
    //
    $('input:checkbox').on('click',function(){ 
        //var checkedId=$(this,'input').attr('id');
        //alert(checkedId);
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
  // superuser's only
    $('#userIdentifier').keypress(function(e) {
      if (e.which == '13') {
          regular_search();
      }
    });

    $( "#mydata_filter_form" ).submit(function( event ) {
      //alert( "Handler for .submit() called." );
      event.preventDefault();
      $("#selected_page").val('1');
      regular_search();

    });

    regular_search(); // run initial search
    //var selected = [];
    //$('#checkboxes input:checked').each(function() {
    //  selected.push($(this).attr('name'));
    //});
} // end init_mydata_page


function clearForNewSearchResults(){
    reset_dvobject_counts();
    clearWarningAlert();
    clearCardResults();
    clearPaginationResults();
    clearJsonResults();
}
function clearJsonResults(){
    $("#div-json-results").html('');
}
function clearWarningAlert(){
    $('#div-result-message').html('');
}
function clearCardResults(){
    $('#div-card-results').html('');
}
function clearPaginationResults(){
    if (!SHOW_PAGINATION){
        return;
    }
    console.log('clearPaginationResults');
    $("#div-pagination").html('');
}

function setWarningAlert(alert_msg){
    console.log('setWarningAlert');
    clearCardResults();
    clearPaginationResults();
    var alert_html = '<div class="alert alert-warning" role="alert">' + alert_msg + '</div>';
    console.log(alert_html);
    $('#div-result-message').html(alert_html);
}

function bindPages(){
   console.log("bindpages");
   $("a.page_link").click(function(evt) {
       evt.preventDefault(); // stop link from using href
       var page_num = $(this).attr('rel');
       $("#selected_page").val(page_num);  // update the selected page in the form
       regular_search();    // run search
   });
   
   $( "#lnk_add_more_cards").unbind( "click" );
   $("#lnk_add_more_cards").click(function(evt){
       evt.preventDefault(); // stop link from using href
       var page_num = $(this).attr('rel');
       $("#selected_page").val(page_num);  // update the selected page in the form
       search_add_more_cards();    // run search
   });
}

function search_add_more_cards(){
    console.log('search_add_more_cards init');
    APPEND_CARDS_TO_BOTTOM = true;
    submit_my_data_search();
}
function regular_search(){
    console.log('regular_search init');
    APPEND_CARDS_TO_BOTTOM = false;
    submit_my_data_search();
}


var DTYPE_COUNT_VARS = ["datasets_count", "dataverses_count", "files_count"];
// --------------------------------
// Reset the counts for Dataverses, Datasets, Files
// --------------------------------
function reset_dvobject_counts(){
     $.each( DTYPE_COUNT_VARS, function( key, attr_name ) {
          $('#id_' + attr_name).html('');
     });
}

// --------------------------------
// (5) Update the counts for Dataverses, Datasets, Files
// --------------------------------
// Expected JSON:    {"datasets_count":568,"dataverses_count":26,"files_count":11}}            
function update_dvobject_count(json_info){
    
    var dcounts = json_info.data.dvobject_counts;
    $.each( DTYPE_COUNT_VARS, function( key, attr_name ) {
        console.log('attr_name: ' + attr_name);
        if(attr_name in dcounts){                    
            $('#id_' + attr_name).html('(' + dcounts[attr_name] + ')');                    
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
        //console.log('render pagination');
        var pagination_json = json_data.data.pagination;
        
        
        if (SHOW_PAGINATION){
            // Use nunjucks to create the pagination HTML
            //
            var pagination_html =  nunjucks.render('mydata_templates/pagination.html', pagination_json);
        
            // Put the pagination HTML into the div
            //
            $("#div-pagination").html(pagination_html);
        }
   
        // --------------------------------
        // (3b) If this isn't the last page, show
        //  a "more results" link after the last card
        // --------------------------------
        if (pagination_json.hasNextPageNumber){
            $('#lnk_add_more_cards').attr("rel", pagination_json.nextPageNumber);
            console.log("update link to: " + pagination_json.nextPageNumber);
            $('#div-more-cards-link').show();
        }
        bindPages();
    }
}            
            
function submit_my_data_search(){


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
    var formData = $('#mydata_filter_form').serialize();

    // For debugging, show the search params
    if (MYDATA_DEBUG_ON){
        $("#div-search-params").html(formData);
    }

    // --------------------------------
    // (2) submit the form
    // --------------------------------
    $.getJSON( RETRIEVE_DATA_API_PATH + '?' + formData, function(data) {

        //  For debugging, show the returned JSON
        if (MYDATA_DEBUG_ON){
            $("#div-json-results").html(JSON.stringify(data));
        }

        // --------------------------------
        // (2a) Does the result look good?
        // --------------------------------
        // No, show error messsage and get out of here
        if (!data.success){
            setWarningAlert(data.error_message);
            return;
        }

        // --------------------------------
        // (2b) Looks good, let's make page
        // --------------------------------
        if (data.success){
            // --------------------------------
            // (3a) If JSON has pagination info, make a pager
            // --------------------------------
            updatePagination(data);
            
            // --------------------------------
            // (4) Let's render the cards
            // --------------------------------
            // Pass the solr docs to the cards template
            var card_html =  nunjucks.render('mydata_templates/cards.html', data);
            if (APPEND_CARDS_TO_BOTTOM){
                console.log('add cards to bottom results');
                // Add new cards after existing cards
                var newCardsDiv = $('<div></div>', { css: { 'display': 'none' }});
                newCardsDiv.html(card_html);
                
                $("#div-card-results").append(newCardsDiv);
                newCardsDiv.slideDown('slow');;
                
                //$("#div-card-results").append('<hr /><hr />' + card_html).show('slow');;
            }else{
                console.log('regular search results');
                // slow down showing of new cards
                /*var newCardsDiv = $('<div></div>', { css: { 'display': 'none' }});
                newCardsDiv.html(card_html);
                
                $("#div-card-results").html(newCardsDiv);
                newCardsDiv.slideDown('slow');
                */
                // Only show new cards
                $("#div-card-results").html(card_html);                            
            }
            
            // --------------------------------
            // (5) Update the item counts
            // --------------------------------
            // Expected JSON:    {"datasets_count":568,"dataverses_count":26,"files_count":11}}            
            update_dvobject_count(data);
    
            //$.each(data.data.items.solr_docs, function( index, value ) {
                 //alert( index + ": " + value );
            //    $("#div-card-results").append('<div class="well">' + JSON.stringify(value) + '</div>');
            //});


            //alert(card_html);
            //alert(JSON.stringify(pagination_json));
        }
//alert(data);
//makePager(data);
   //     $("#divPagerJSON").html('<pre>' + JSON.stringify(data) + '</pre>');
    });
}
