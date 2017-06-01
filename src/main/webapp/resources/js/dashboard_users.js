
var USER_LIST_DEBUG_ON = false;

function initPage(){
    nunjucks.configure({ autoescape: true });
    bindReturnKeyOnSearch();
    runRegularSearch();
}

function clearUserList(){
    $('#div-user-list').html('');
}

function clearWarningMessage(){
    $('#div-result-message').html('');    
}

function setWarningAlert(alert_msg){
    var alert_html = '<div class="alert alert-warning" role="alert">' + alert_msg + '</div>';
    $('#div-result-message').html(alert_html);
}

function runRegularSearch(){
    
    clearWarningMessage();
    clearUserList();

    //var selectedPage = $("#selectedPage").val();
    //var searchTerm = $("#searchTerm").val();
    var formData = $("#selectedPage").serialize() + '&' + $("#searchTerm").serialize();

    // Call the API endpoint
    $.getJSON( RETRIEVE_USER_DATA_API_PATH + '?' + formData, function(data) {

        //  For debugging, show the returned JSON
        if (USER_LIST_DEBUG_ON){
           $("#div-json-results").html(JSON.stringify(data));
        }

        // Didn't work, show error
        if ((!data.status)||(!data.status == 'OK')){
            setWarningAlert(data.errorMessage);
            return;
        }

        // Looks good, let's make a page
     
        // --------------------------------
        // (3) If JSON has pagination info, make a pager
        // --------------------------------
        updatePagination(data);
            
        // --------------------------------
        // (4) Let's list the users
        // --------------------------------
        // pass the list of names to the template
         var user_list_html =  nunjucks.render('mydata_templates/user_list.html', data);
        $("#div-user-list").html(user_list_html);                            
        

    });
}


function updatePagination(json_data){
     //   console.log(json_data);
    $("#div-pagination").html('');
    if (json_data.data.hasOwnProperty('pagination')){
        var pagination_json = json_data.data.pagination;        
        // Use nunjucks to create the pagination HTML
        var pagination_html =  nunjucks.render('mydata_templates/pagination.html', pagination_json);
            // Put the pagination HTML into the div
        $("#div-pagination").html(pagination_html);
        
        bindPages();
    }
}

//-----------------------------------------
// Bind pager buttons
//-----------------------------------------
function bindPages(){
    // bind pager buttons
    $("a.page_link").click(function(evt) {
       evt.preventDefault(); // stop link from using href
       var page_num = $(this).attr('rel');
       $("#selectedPage").val(page_num);  // update the selected page in the form
       runRegularSearch();    // run search
   });
}

function bindReturnKeyOnSearch(){
    
    // Capture pressing return in search box
    $('#searchTerm').keypress(function(e) {
      if (e.which == '13') {
          e.preventDefault();
          $("#selectedPage").val('1');
          runRegularSearch();
      }
    });
}
