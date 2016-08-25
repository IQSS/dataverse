'use strict';

var app = angular.module('odesiApp', ['ngSanitize', 'ngCookies', 'ngRoute', 'googlechart','ui.bootstrap'])//don't forget to add dependencies for 'odesiApp'

app.config(['$routeProvider', '$locationProvider',
    function($routeProvider) {
        $routeProvider
        .
        otherwise({
           templateUrl: 'partials/details.html',
			controller: 'detailsCtrl'
        });
    }
]).filter('tablePaginate', function() {
		return function(input, currPage, maxPages) {
			currPage = parseInt(currPage, 10); //Make string input int
			maxPages = parseInt(maxPages, 10);
			var start, end;		
			if (maxPages > 4){
				if ((currPage+2) >= maxPages){
					start = maxPages-4;
				}else if (currPage > 3){
					start = currPage-2;
				}else if (currPage <= 3){
					start = 1;
				}		
				end = start + 4;
			}else {
				start = 1;
				end = maxPages;
			}
			for (var i=start; i<=end; i++)
				input.push(i);
			return input;
		};
	}).filter('startFrom', function() {
		return function(input, start) {
			if (input){
				start = +start; //parse to int
				return input.slice(start);
			}
		}
	}).factory("searchParams",function(){
        // This is used to pass search parameters from the search to details to populate the form.
		return {};
	}).factory("variableQuery",function(){
        // This is used to pass search parameters from the search to details to get matching variables.
		return {};
	}).factory("variableClick",function(){
        // This is used to pass search parameters from the search to details to make matching variables tab active on load.
		return {};
	}).factory("getEntitlement",function($http){
		return{
		    getEntitled : function() {
		        return $http({
		            url: 'entitlements',
		            method: 'GET'
		        })
		    }
		 }
	}).service('sharedVariableStore', function () {
		//store all the variables in a global object for reuse
		 var variableStore={};
		 return {
	            getVariableStore: function () {
	                return variableStore;
	            },
	            setVariableStore: function(value) {
	            	variableStore = value;
					
	            }
	        };
    }).service('filterService', function() {
		var searchText = "";
		return {
			getFilter: function () {
				return searchText;
			},
			setFilter: function(value) {
				searchText = value;
			}
		}
    });