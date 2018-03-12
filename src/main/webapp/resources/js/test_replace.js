        
        var getUrlParameter = function getUrlParameter(sParam) {
            var sPageURL = decodeURIComponent(window.location.search.substring(1)),
                sURLVariables = sPageURL.split('&'),
                sParameterName,
                i;

            for (i = 0; i < sURLVariables.length; i++) {
                sParameterName = sURLVariables[i].split('=');

                if (sParameterName[0] === sParam) {
                    return sParameterName[1] === undefined ? true : sParameterName[1];
                }
            }
        }; // end getUrlParameter
        
        function mlog(m){
            console.log(m);
        } // end mlog
        
        function publish_dataset(dataset_id){
            if (dataset_id===null) {
                return;
            }
     
            var publish_url = '/api/v1/datasets/' + dataset_id + '/actions/:publish?type=major';
            
            mlog('publish_url: ' + publish_url);
            
            $.getJSON( publish_url, function(data) {
                mlog('data: ' + data)
                /*
                if (data.status == "OK"){
                    location.reload();
                }else{
                    alert("Failed to publish!\n" + data.message);
                }*/
            
            })
            .done(function(data) { 
                if (data.status == "OK"){
                    location.reload();
                }
                mlog('looks good');
             })
            .fail(function(jqXHR, textStatus, errorThrown) { 
                alert('Sorry! Failed to publish this dataset.');
                location.reload();

                mlog('getJSON request failed! ' + textStatus); 
                mlog('getJSON request failed! errorThrown' + errorThrown); 
            })
            .always(function() {
                mlog('getJSON request ended!'); 
            
            });
            ;
        
         } // end publish_dataset
        
          $(document).ready(function() {
             $('#idPublishDataset').on('click',function(){ 
                publish_dataset(getUrlParameter('ds_id'));
             });
             
             //init_mydata_page(); // source: mydata.js 
           });