'use strict';
angular.module('odesiApp').controller('mainCtrl', function($scope, $http, $route, $location, $cookies, $timeout, $anchorScroll, anchorSmoothScroll, getEntitlement){
		//main controller
		//determine language based on session cookie
	
		
		/*$http({
			url: '/checkLang',
			method: 'GET'
		}).success(function(data){
			var lang = data.substring(0,2);
			if(lang == 'fr' && !$cookies.language) {
				$cookies.language = 'fr';
				location.reload();

		}
		})*/
		
		
		$scope.lang = (!$cookies.language || $cookies.language === 'en') ? en : fr;
		$scope.htmllang = $cookies.language;
		$scope.baseUrl = $location.host() + ":" + $location.port();
		//paths for templates
		$scope.headerTemplatePath = 'templates/header.html';
		$scope.footerTemplatePath = 'templates/footer.html';
		$scope.myListTemplatePath = 'templates/my-list.html';
		$scope.searchFormTemplatePath = 'templates/search-form.html';
		//model to store current page view
		$scope.selectedNav = {};
		//model to store current search params to store in URL hash
		$scope.searchStateObj = {};
		//model that stores selected items in My List feature
		//using sessionStorage
		$scope.myList = window.sessionStorage.getItem('myList') ? URLON.parse(window.sessionStorage.getItem('myList')) : [];
		//function that handles language toggle
		$scope.switchLanguage = function(language){
			$cookies.language = language;
			location.reload();
		}

		
		
});