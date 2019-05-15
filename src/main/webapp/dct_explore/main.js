(window["webpackJsonp"] = window["webpackJsonp"] || []).push([["main"],{

/***/ "./src/$$_lazy_route_resource lazy recursive":
/*!**********************************************************!*\
  !*** ./src/$$_lazy_route_resource lazy namespace object ***!
  \**********************************************************/
/*! no static exports found */
/***/ (function(module, exports) {

function webpackEmptyAsyncContext(req) {
	// Here Promise.resolve().then() is used instead of new Promise() to prevent
	// uncaught exception popping up in devtools
	return Promise.resolve().then(function() {
		var e = new Error("Cannot find module '" + req + "'");
		e.code = 'MODULE_NOT_FOUND';
		throw e;
	});
}
webpackEmptyAsyncContext.keys = function() { return []; };
webpackEmptyAsyncContext.resolve = webpackEmptyAsyncContext;
module.exports = webpackEmptyAsyncContext;
webpackEmptyAsyncContext.id = "./src/$$_lazy_route_resource lazy recursive";

/***/ }),

/***/ "./src/app/app.component.css":
/*!***********************************!*\
  !*** ./src/app/app.component.css ***!
  \***********************************/
/*! no static exports found */
/***/ (function(module, exports) {

module.exports = "\n/*# sourceMappingURL=data:application/json;base64,eyJ2ZXJzaW9uIjozLCJzb3VyY2VzIjpbXSwibmFtZXMiOltdLCJtYXBwaW5ncyI6IiIsImZpbGUiOiJzcmMvYXBwL2FwcC5jb21wb25lbnQuY3NzIn0= */"

/***/ }),

/***/ "./src/app/app.component.html":
/*!************************************!*\
  !*** ./src/app/app.component.html ***!
  \************************************/
/*! no static exports found */
/***/ (function(module, exports) {

module.exports = "<app-interface></app-interface>\n"

/***/ }),

/***/ "./src/app/app.component.ts":
/*!**********************************!*\
  !*** ./src/app/app.component.ts ***!
  \**********************************/
/*! exports provided: AppComponent */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
__webpack_require__.r(__webpack_exports__);
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "AppComponent", function() { return AppComponent; });
/* harmony import */ var tslib__WEBPACK_IMPORTED_MODULE_0__ = __webpack_require__(/*! tslib */ "./node_modules/tslib/tslib.es6.js");
/* harmony import */ var _angular_core__WEBPACK_IMPORTED_MODULE_1__ = __webpack_require__(/*! @angular/core */ "./node_modules/@angular/core/fesm5/core.js");


var AppComponent = /** @class */ (function () {
    function AppComponent() {
        this.title = 'data-curation-tool';
    }
    AppComponent = tslib__WEBPACK_IMPORTED_MODULE_0__["__decorate"]([
        Object(_angular_core__WEBPACK_IMPORTED_MODULE_1__["Component"])({
            selector: 'app-root',
            template: __webpack_require__(/*! ./app.component.html */ "./src/app/app.component.html"),
            styles: [__webpack_require__(/*! ./app.component.css */ "./src/app/app.component.css")]
        })
    ], AppComponent);
    return AppComponent;
}());



/***/ }),

/***/ "./src/app/app.module.ts":
/*!*******************************!*\
  !*** ./src/app/app.module.ts ***!
  \*******************************/
/*! exports provided: AppModule */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
__webpack_require__.r(__webpack_exports__);
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "AppModule", function() { return AppModule; });
/* harmony import */ var tslib__WEBPACK_IMPORTED_MODULE_0__ = __webpack_require__(/*! tslib */ "./node_modules/tslib/tslib.es6.js");
/* harmony import */ var _angular_platform_browser__WEBPACK_IMPORTED_MODULE_1__ = __webpack_require__(/*! @angular/platform-browser */ "./node_modules/@angular/platform-browser/fesm5/platform-browser.js");
/* harmony import */ var _angular_core__WEBPACK_IMPORTED_MODULE_2__ = __webpack_require__(/*! @angular/core */ "./node_modules/@angular/core/fesm5/core.js");
/* harmony import */ var _angular_forms__WEBPACK_IMPORTED_MODULE_3__ = __webpack_require__(/*! @angular/forms */ "./node_modules/@angular/forms/fesm5/forms.js");
/* harmony import */ var _angular_common__WEBPACK_IMPORTED_MODULE_4__ = __webpack_require__(/*! @angular/common */ "./node_modules/@angular/common/fesm5/common.js");
/* harmony import */ var _angular_common_locales_fr_CA__WEBPACK_IMPORTED_MODULE_5__ = __webpack_require__(/*! @angular/common/locales/fr-CA */ "./node_modules/@angular/common/locales/fr-CA.js");
/* harmony import */ var _angular_common_locales_fr_CA__WEBPACK_IMPORTED_MODULE_5___default = /*#__PURE__*/__webpack_require__.n(_angular_common_locales_fr_CA__WEBPACK_IMPORTED_MODULE_5__);
/* harmony import */ var _angular_material__WEBPACK_IMPORTED_MODULE_6__ = __webpack_require__(/*! @angular/material */ "./node_modules/@angular/material/esm5/material.es5.js");
/* harmony import */ var _angular_platform_browser_animations__WEBPACK_IMPORTED_MODULE_7__ = __webpack_require__(/*! @angular/platform-browser/animations */ "./node_modules/@angular/platform-browser/fesm5/animations.js");
/* harmony import */ var _app_component__WEBPACK_IMPORTED_MODULE_8__ = __webpack_require__(/*! ./app.component */ "./src/app/app.component.ts");
/* harmony import */ var _ddi_service__WEBPACK_IMPORTED_MODULE_9__ = __webpack_require__(/*! ./ddi.service */ "./src/app/ddi.service.ts");
/* harmony import */ var _interface_interface_component__WEBPACK_IMPORTED_MODULE_10__ = __webpack_require__(/*! ./interface/interface.component */ "./src/app/interface/interface.component.ts");
/* harmony import */ var _angular_common_http__WEBPACK_IMPORTED_MODULE_11__ = __webpack_require__(/*! @angular/common/http */ "./node_modules/@angular/common/fesm5/http.js");
/* harmony import */ var _var_group_var_group_component__WEBPACK_IMPORTED_MODULE_12__ = __webpack_require__(/*! ./var-group/var-group.component */ "./src/app/var-group/var-group.component.ts");
/* harmony import */ var _var_var_component__WEBPACK_IMPORTED_MODULE_13__ = __webpack_require__(/*! ./var/var.component */ "./src/app/var/var.component.ts");
/* harmony import */ var _var_dialog_var_dialog_component__WEBPACK_IMPORTED_MODULE_14__ = __webpack_require__(/*! ./var-dialog/var-dialog.component */ "./src/app/var-dialog/var-dialog.component.ts");
/* harmony import */ var _var_stat_dialog_var_stat_dialog_component__WEBPACK_IMPORTED_MODULE_15__ = __webpack_require__(/*! ./var-stat-dialog/var-stat-dialog.component */ "./src/app/var-stat-dialog/var-stat-dialog.component.ts");
/* harmony import */ var _chart_chart_component__WEBPACK_IMPORTED_MODULE_16__ = __webpack_require__(/*! ./chart/chart.component */ "./src/app/chart/chart.component.ts");






// the second parameter 'fr' is optional
Object(_angular_common__WEBPACK_IMPORTED_MODULE_4__["registerLocaleData"])(_angular_common_locales_fr_CA__WEBPACK_IMPORTED_MODULE_5___default.a, 'fr-CA');











var AppModule = /** @class */ (function () {
    function AppModule() {
    }
    AppModule = tslib__WEBPACK_IMPORTED_MODULE_0__["__decorate"]([
        Object(_angular_core__WEBPACK_IMPORTED_MODULE_2__["NgModule"])({
            declarations: [
                _app_component__WEBPACK_IMPORTED_MODULE_8__["AppComponent"],
                _interface_interface_component__WEBPACK_IMPORTED_MODULE_10__["InterfaceComponent"],
                _var_group_var_group_component__WEBPACK_IMPORTED_MODULE_12__["VarGroupComponent"],
                _var_var_component__WEBPACK_IMPORTED_MODULE_13__["VarComponent"],
                _var_dialog_var_dialog_component__WEBPACK_IMPORTED_MODULE_14__["VarDialogComponent"],
                _var_stat_dialog_var_stat_dialog_component__WEBPACK_IMPORTED_MODULE_15__["VarStatDialogComponent"],
                _chart_chart_component__WEBPACK_IMPORTED_MODULE_16__["ChartComponent"]
            ],
            imports: [
                _angular_platform_browser__WEBPACK_IMPORTED_MODULE_1__["BrowserModule"],
                _angular_forms__WEBPACK_IMPORTED_MODULE_3__["FormsModule"],
                _angular_forms__WEBPACK_IMPORTED_MODULE_3__["ReactiveFormsModule"],
                _angular_common_http__WEBPACK_IMPORTED_MODULE_11__["HttpClientModule"],
                _angular_platform_browser_animations__WEBPACK_IMPORTED_MODULE_7__["BrowserAnimationsModule"],
                _angular_material__WEBPACK_IMPORTED_MODULE_6__["MatButtonModule"],
                _angular_material__WEBPACK_IMPORTED_MODULE_6__["MatTabsModule"],
                _angular_material__WEBPACK_IMPORTED_MODULE_6__["MatIconModule"],
                _angular_material__WEBPACK_IMPORTED_MODULE_6__["MatToolbarModule"],
                _angular_material__WEBPACK_IMPORTED_MODULE_6__["MatTableModule"],
                _angular_material__WEBPACK_IMPORTED_MODULE_6__["MatSortModule"],
                _angular_material__WEBPACK_IMPORTED_MODULE_6__["MatProgressSpinnerModule"],
                _angular_material__WEBPACK_IMPORTED_MODULE_6__["MatPaginatorModule"],
                _angular_material__WEBPACK_IMPORTED_MODULE_6__["MatFormFieldModule"],
                _angular_material__WEBPACK_IMPORTED_MODULE_6__["MatInputModule"],
                _angular_material__WEBPACK_IMPORTED_MODULE_6__["MatDialogModule"],
                _angular_material__WEBPACK_IMPORTED_MODULE_6__["MatSidenavModule"],
                _angular_material__WEBPACK_IMPORTED_MODULE_6__["MatListModule"],
                _angular_material__WEBPACK_IMPORTED_MODULE_6__["MatCheckboxModule"],
                _angular_material__WEBPACK_IMPORTED_MODULE_6__["MatSelectModule"],
                _angular_material__WEBPACK_IMPORTED_MODULE_6__["MatGridListModule"],
                _angular_material__WEBPACK_IMPORTED_MODULE_6__["MatChipsModule"],
                _angular_material__WEBPACK_IMPORTED_MODULE_6__["MatSnackBarModule"],
                _angular_material__WEBPACK_IMPORTED_MODULE_6__["MatTooltipModule"],
                _angular_material__WEBPACK_IMPORTED_MODULE_6__["MatAutocompleteModule"],
            ],
            exports: [],
            entryComponents: [_var_dialog_var_dialog_component__WEBPACK_IMPORTED_MODULE_14__["VarDialogComponent"], _var_stat_dialog_var_stat_dialog_component__WEBPACK_IMPORTED_MODULE_15__["VarStatDialogComponent"]],
            providers: [_ddi_service__WEBPACK_IMPORTED_MODULE_9__["DdiService"]],
            bootstrap: [_app_component__WEBPACK_IMPORTED_MODULE_8__["AppComponent"]]
        })
    ], AppModule);
    return AppModule;
}());



/***/ }),

/***/ "./src/app/chart/chart.component.css":
/*!*******************************************!*\
  !*** ./src/app/chart/chart.component.css ***!
  \*******************************************/
/*! no static exports found */
/***/ (function(module, exports) {

module.exports = "\n/*# sourceMappingURL=data:application/json;base64,eyJ2ZXJzaW9uIjozLCJzb3VyY2VzIjpbXSwibmFtZXMiOltdLCJtYXBwaW5ncyI6IiIsImZpbGUiOiJzcmMvYXBwL2NoYXJ0L2NoYXJ0LmNvbXBvbmVudC5jc3MifQ== */"

/***/ }),

/***/ "./src/app/chart/chart.component.html":
/*!********************************************!*\
  !*** ./src/app/chart/chart.component.html ***!
  \********************************************/
/*! no static exports found */
/***/ (function(module, exports) {

module.exports = "<div #chart ></div>\n"

/***/ }),

/***/ "./src/app/chart/chart.component.ts":
/*!******************************************!*\
  !*** ./src/app/chart/chart.component.ts ***!
  \******************************************/
/*! exports provided: ChartComponent */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
__webpack_require__.r(__webpack_exports__);
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "ChartComponent", function() { return ChartComponent; });
/* harmony import */ var tslib__WEBPACK_IMPORTED_MODULE_0__ = __webpack_require__(/*! tslib */ "./node_modules/tslib/tslib.es6.js");
/* harmony import */ var _angular_core__WEBPACK_IMPORTED_MODULE_1__ = __webpack_require__(/*! @angular/core */ "./node_modules/@angular/core/fesm5/core.js");
/* harmony import */ var d3__WEBPACK_IMPORTED_MODULE_2__ = __webpack_require__(/*! d3 */ "./node_modules/d3/index.js");



var ChartComponent = /** @class */ (function () {
    function ChartComponent() {
        this.color_array = [
            '#3366cc',
            '#dc3912',
            '#ff9900',
            '#109618',
            '#990099',
            '#0099c6',
            '#dd4477',
            '#66aa00',
            '#b82e2e',
            '#316395',
            '#994499',
            '#22aa99',
            '#aaaa11',
            '#6633cc',
            '#e67300',
            '#8b0707',
            '#651067',
            '#329262',
            '#5574a6',
            '#3b3eac',
            '#b77322',
            '#16d620',
            '#b91383',
            '#f4359e',
            '#9c5935',
            '#a9c413',
            '#2a778d',
            '#668d1c',
            '#bea413',
            '#0c5922',
            '#743411'
        ];
        this.max_string_length = 13;
    }
    ChartComponent.prototype.ngOnInit = function () {
        this.createChart(this.data);
    };
    ChartComponent.prototype.createChart = function (_data) {
        var obj = this;
        var data = this.data;
        data = [];
        console.log(_data);
        for (var i = 0; i < _data.length; i++) {
            console.log('chart: data.length ' + _data.length);
            var freq = null;
            var freq_weight = null;
            if (typeof _data[i].catStat !== 'undefined') {
                console.log(_data[i].catStat);
                for (var j = 0; j < _data[i].catStat.length; j++) {
                    var sub_obj = _data[i].catStat[j];
                    if (sub_obj['@type'] === 'freq' && !sub_obj['@wgtd']) {
                        freq = sub_obj['#text'];
                    }
                    else {
                        if (sub_obj['@type'] === 'freq' && sub_obj['@wgtd'] && sub_obj['#text'] !== '') {
                            freq_weight = sub_obj['#text'];
                        }
                    }
                }
                // dataverse ddi exception
                if (!freq) {
                    freq = _data[i].catStat['#text'];
                }
            }
            var short_name = _data[i].labl['#text'];
            if (short_name.length > this.max_string_length) {
                short_name = short_name.substring(0, this.max_string_length) + '...';
            }
            short_name = short_name;
            // switching to weighted frequencies
            if (freq_weight != null) {
                freq = freq_weight;
            }
            data.push({
                name: short_name,
                freq: freq
            });
        }
        var max_height = (data.length + 1) * 25;
        // sort bars based on value
        data = data.sort(function (a, b) {
            return d3__WEBPACK_IMPORTED_MODULE_2__["ascending"](a.freq, b.freq);
        });
        // set the dimensions and margins of the graph
        var margin = { top: 0, right: 20, bottom: 30, left: 90 };
        var width = 500 - margin.left - margin.right;
        var height = max_height - margin.top - margin.bottom;
        // set the ranges
        var y = d3__WEBPACK_IMPORTED_MODULE_2__["scaleBand"]()
            .range([height, 0])
            .padding(0.3);
        var x = d3__WEBPACK_IMPORTED_MODULE_2__["scaleLinear"]().range([0, width]);
        var element = this.chartContainer.nativeElement;
        var svg = d3__WEBPACK_IMPORTED_MODULE_2__["select"](element)
            .append('svg')
            .attr('width', width + margin.left + margin.right)
            .attr('height', height + margin.top + margin.bottom)
            .append('g')
            .attr('transform', 'translate(' + margin.left + ',' + margin.top + ')');
        // format the data
        data.forEach(function (d) {
            d.freq = +d.freq;
        });
        // Scale the range of the data in the domains
        x.domain([
            0,
            d3__WEBPACK_IMPORTED_MODULE_2__["max"](data, function (d) {
                return d.freq;
            })
        ]);
        y.domain(data.map(function (d) {
            return d.name;
        }));
        // append the rectangles for the bar chart
        var count = 0;
        svg
            .selectAll('.bar')
            .data(data)
            .enter()
            .append('rect')
            .attr('class', 'bar')
            .attr('width', function (d) {
            return x(d.freq);
        })
            .attr('y', function (d) {
            return y(d.name);
        })
            .attr('fill', function (d) {
            count += 1;
            return obj.getColor(count);
        })
            .attr('height', y.bandwidth());
        // add the x Axis
        svg
            .append('g')
            .attr('transform', 'translate(0,' + height + ')')
            .call(d3__WEBPACK_IMPORTED_MODULE_2__["axisBottom"](x));
        // add the y Axis
        svg.append('g').call(d3__WEBPACK_IMPORTED_MODULE_2__["axisLeft"](y));
    };
    ChartComponent.prototype.getColor = function (num) {
        var color;
        if (num < this.color_array.length) {
            color = this.color_array[num];
        }
        else {
            color = this.getRandomColor();
        }
        return color;
    };
    ChartComponent.prototype.getRandomColor = function () {
        var letters = '0123456789ABCDEF'.split('');
        var color = '#';
        for (var i = 0; i < 6; i++) {
            color += letters[Math.floor(Math.random() * 16)];
        }
        return color;
    };
    tslib__WEBPACK_IMPORTED_MODULE_0__["__decorate"]([
        Object(_angular_core__WEBPACK_IMPORTED_MODULE_1__["ViewChild"])('chart'),
        tslib__WEBPACK_IMPORTED_MODULE_0__["__metadata"]("design:type", _angular_core__WEBPACK_IMPORTED_MODULE_1__["ElementRef"])
    ], ChartComponent.prototype, "chartContainer", void 0);
    tslib__WEBPACK_IMPORTED_MODULE_0__["__decorate"]([
        Object(_angular_core__WEBPACK_IMPORTED_MODULE_1__["Input"])(),
        tslib__WEBPACK_IMPORTED_MODULE_0__["__metadata"]("design:type", Array)
    ], ChartComponent.prototype, "data", void 0);
    ChartComponent = tslib__WEBPACK_IMPORTED_MODULE_0__["__decorate"]([
        Object(_angular_core__WEBPACK_IMPORTED_MODULE_1__["Component"])({
            selector: 'app-chart',
            template: __webpack_require__(/*! ./chart.component.html */ "./src/app/chart/chart.component.html"),
            styles: [__webpack_require__(/*! ./chart.component.css */ "./src/app/chart/chart.component.css")]
        }),
        tslib__WEBPACK_IMPORTED_MODULE_0__["__metadata"]("design:paramtypes", [])
    ], ChartComponent);
    return ChartComponent;
}());



/***/ }),

/***/ "./src/app/ddi.service.ts":
/*!********************************!*\
  !*** ./src/app/ddi.service.ts ***!
  \********************************/
/*! exports provided: DdiService */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
__webpack_require__.r(__webpack_exports__);
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "DdiService", function() { return DdiService; });
/* harmony import */ var tslib__WEBPACK_IMPORTED_MODULE_0__ = __webpack_require__(/*! tslib */ "./node_modules/tslib/tslib.es6.js");
/* harmony import */ var _angular_core__WEBPACK_IMPORTED_MODULE_1__ = __webpack_require__(/*! @angular/core */ "./node_modules/@angular/core/fesm5/core.js");
/* harmony import */ var _angular_common_http__WEBPACK_IMPORTED_MODULE_2__ = __webpack_require__(/*! @angular/common/http */ "./node_modules/@angular/common/fesm5/http.js");




var DdiService = /** @class */ (function () {
    function DdiService(http) {
        this.http = http;
    }
    DdiService.prototype.getDDI = function (url) {
        return this.http.get(url, { responseType: 'text' });
    };
    DdiService.prototype.putDDI = function (url, body, key) {
        console.log('my url ' + url);
        console.log('my key ' + key);
        var httpOptions = {
            headers: new _angular_common_http__WEBPACK_IMPORTED_MODULE_2__["HttpHeaders"]({
                'Content-Type': 'application/xml',
                'X-Dataverse-key': key
            })
        };
        console.log('Before sending');
        return this.http.put(url, body, httpOptions);
        // return this.http.post(url,body, httpOptions);
    };
    DdiService.prototype.getParameterByName = function (name) {
        var url = window.location.href;
        name = name.replace(/[\[\]]/g, '\\$&');
        var regex = new RegExp('[?&]' + name + '(=([^&#]*)|&|#|$)');
        var results = regex.exec(url);
        if (!results) {
            return null;
        }
        if (!results[2]) {
            return '';
        }
        return decodeURIComponent(results[2].replace(/\+/g, ' '));
    };
    DdiService.prototype.getBaseUrl = function () {
        var protocol = window.location.protocol;
        var host = window.location.hostname;
        var port = window.location.port;
        /*  if (port === '4200') {
            port = '8080';
          }*/
        var base_url = protocol + '//' + host;
        if (port != null || typeof port !== 'undefined') {
            base_url = base_url + ':' + port;
        }
        return base_url;
    };
    DdiService = tslib__WEBPACK_IMPORTED_MODULE_0__["__decorate"]([
        Object(_angular_core__WEBPACK_IMPORTED_MODULE_1__["Injectable"])(),
        tslib__WEBPACK_IMPORTED_MODULE_0__["__metadata"]("design:paramtypes", [_angular_common_http__WEBPACK_IMPORTED_MODULE_2__["HttpClient"]])
    ], DdiService);
    return DdiService;
}());



/***/ }),

/***/ "./src/app/interface/interface.component.css":
/*!***************************************************!*\
  !*** ./src/app/interface/interface.component.css ***!
  \***************************************************/
/*! no static exports found */
/***/ (function(module, exports) {

module.exports = ".small {\n  font-size: 12px;\n}\n\n.medium {\n  font-size: 14px;\n}\n\n/*# sourceMappingURL=data:application/json;base64,eyJ2ZXJzaW9uIjozLCJzb3VyY2VzIjpbInNyYy9hcHAvaW50ZXJmYWNlL2ludGVyZmFjZS5jb21wb25lbnQuY3NzIl0sIm5hbWVzIjpbXSwibWFwcGluZ3MiOiJBQUFBO0VBQ0UsZUFBZTtBQUNqQjs7QUFFQTtFQUNFLGVBQWU7QUFDakIiLCJmaWxlIjoic3JjL2FwcC9pbnRlcmZhY2UvaW50ZXJmYWNlLmNvbXBvbmVudC5jc3MiLCJzb3VyY2VzQ29udGVudCI6WyIuc21hbGwge1xuICBmb250LXNpemU6IDEycHg7XG59XG5cbi5tZWRpdW0ge1xuICBmb250LXNpemU6IDE0cHg7XG59XG4iXX0= */"

/***/ }),

/***/ "./src/app/interface/interface.component.html":
/*!****************************************************!*\
  !*** ./src/app/interface/interface.component.html ***!
  \****************************************************/
/*! no static exports found */
/***/ (function(module, exports) {

module.exports = "<div id=\"loading-details\" *ngIf=\"!ddi_loaded\" class=\"row content-area\">\n  <mat-progress-spinner mode=\"indeterminate\"></mat-progress-spinner>\n</div>\n\n<ng-container class=\"interface-container\">\n\n  <mat-toolbar class=\"interface-header\">\n    <mat-toolbar-row>\n    <span>{{title}}</span>\n    <span class=\"fill-space\"></span>\n    <span style=\"float:right\">\n        <button mat-icon-button color=\"accent\" (click)=\"onSave()\" i18n-matTooltip matTooltip=\"Download\">\n          <mat-icon i18n-aria-label aria-label=\"Download xml\">get_app</mat-icon>\n        </button>\n        <span class=\"small\" i18n>Export</span>\n      </span>\n    <span style=\"float:right\">\n      <button mat-icon-button color=\"accent\" (click)=\"sendToDV()\" i18n-matTooltip matTooltip=\"Save\">\n        <mat-icon i18n-aria-label aria-label=\"Save\">save</mat-icon>\n      </button>\n      <span class=\"small\" i18n>Save</span>\n    </span>\n    </mat-toolbar-row>\n    <mat-toolbar-row class=\"medium\">\n      <span>{{filename}}</span>\n    </mat-toolbar-row>\n    <mat-toolbar-row class=\"medium\">\n      <span>{{firstCitat}}&nbsp;</span>\n      <a href=\"{{doi}}\">{{doi}}</a>\n      <span>{{secondCitat}}</span>\n    </mat-toolbar-row>\n  </mat-toolbar>\n\n\n\n<mat-sidenav-container >\n  <mat-sidenav mode=\"side\" opened class=\"side_nav\" #scrollMe>\n    <app-var-group [_variable_groups]=\"_variable_groups\" (subSetRows)=\"broadcastSubSetRows($event)\" (selectGroup)=\"broadcastSelect($event)\" (draggedGroup)=\"broadcastDraggedGroup($event)\" (disableSelectGroup)=\"broadcastDeselectGroup()\"\n     (parentScrollNav)=\"scrollNav()\"\n    ></app-var-group>\n  </mat-sidenav>\n  <mat-sidenav-content>\n    <app-var [_variable_groups]=\"_variable_groups\" ></app-var>\n    </mat-sidenav-content>\n</mat-sidenav-container>\n\n</ng-container>\n"

/***/ }),

/***/ "./src/app/interface/interface.component.ts":
/*!**************************************************!*\
  !*** ./src/app/interface/interface.component.ts ***!
  \**************************************************/
/*! exports provided: InterfaceComponent */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
__webpack_require__.r(__webpack_exports__);
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "InterfaceComponent", function() { return InterfaceComponent; });
/* harmony import */ var tslib__WEBPACK_IMPORTED_MODULE_0__ = __webpack_require__(/*! tslib */ "./node_modules/tslib/tslib.es6.js");
/* harmony import */ var _angular_core__WEBPACK_IMPORTED_MODULE_1__ = __webpack_require__(/*! @angular/core */ "./node_modules/@angular/core/fesm5/core.js");
/* harmony import */ var _ddi_service__WEBPACK_IMPORTED_MODULE_2__ = __webpack_require__(/*! ../ddi.service */ "./src/app/ddi.service.ts");
/* harmony import */ var _assets_js_xml2json__WEBPACK_IMPORTED_MODULE_3__ = __webpack_require__(/*! ../../assets/js/xml2json */ "./src/assets/js/xml2json.js");
/* harmony import */ var _angular_material__WEBPACK_IMPORTED_MODULE_4__ = __webpack_require__(/*! @angular/material */ "./node_modules/@angular/material/esm5/material.es5.js");
/* harmony import */ var _var_var_component__WEBPACK_IMPORTED_MODULE_5__ = __webpack_require__(/*! ../var/var.component */ "./src/app/var/var.component.ts");
/* harmony import */ var file_saver__WEBPACK_IMPORTED_MODULE_6__ = __webpack_require__(/*! file-saver */ "./node_modules/file-saver/dist/FileSaver.min.js");
/* harmony import */ var file_saver__WEBPACK_IMPORTED_MODULE_6___default = /*#__PURE__*/__webpack_require__.n(file_saver__WEBPACK_IMPORTED_MODULE_6__);
/* harmony import */ var xml_writer__WEBPACK_IMPORTED_MODULE_7__ = __webpack_require__(/*! xml-writer */ "./node_modules/xml-writer/index.js");
/* harmony import */ var xml_writer__WEBPACK_IMPORTED_MODULE_7___default = /*#__PURE__*/__webpack_require__.n(xml_writer__WEBPACK_IMPORTED_MODULE_7__);









var InterfaceComponent = /** @class */ (function () {
    function InterfaceComponent(ddiService) {
        this.ddiService = ddiService;
        this.data = null; // store the xml
        this.ddi_loaded = false; // show the loading
        this._variable_groups = []; // store the variables in an array display
        this._variables = []; // store the variables to be broadcast to child
        this._id = null; // file id
        this._metaId = null;
        this._base_url = null;
    }
    InterfaceComponent.prototype.ngOnInit = function () {
        var uri = null;
        uri = this.ddiService.getParameterByName('uri');
        this._id = this.ddiService.getParameterByName('dfId');
        this._metaId = this.ddiService.getParameterByName('fileMetadataId');
        console.log(this._metaId);
        this._base_url = this.ddiService.getBaseUrl();
        if (!uri && this._id != null) {
            uri = this._base_url + '/api/access/datafile/' + this._id + '/metadata/ddi';
            if (this._metaId != null) {
                uri = uri + '?fileMetadataId=' + this._metaId;
            }
        }
        else {
            if (!uri) {
                // Just for testing purposes
                uri = this._base_url + '/assets/FOCN_SPSS_20150525_FORMATTED-ddi.xml';
                // uri = this._base_url + '/assets/arg-drones-E-2014-can.xml';
            }
        }
        this.getDDI(uri);
    };
    InterfaceComponent.prototype.getDDI = function (_uri) {
        var _this = this;
        var url = _uri;
        this.ddiService
            .getDDI(url)
            .subscribe(function (data) { return _this.processDDI(data); }, function (error) { return console.log(error); }, function () { return _this.completeDDI(); });
    };
    InterfaceComponent.prototype.scrollNav = function () {
        var elm = this.myScrollContainer['_elementRef'].nativeElement;
        elm.scrollTop = elm.scrollHeight;
    };
    InterfaceComponent.prototype.processDDI = function (data) {
        var parser = new DOMParser();
        this.data = parser.parseFromString(data, 'text/xml');
    };
    InterfaceComponent.prototype.completeDDI = function () {
        this.showVarsGroups();
        this.showVars();
        this.title = this.data
            .getElementsByTagName('stdyDscr')[0]
            .getElementsByTagName('titl')[0].textContent;
        var citation = this.data
            .getElementsByTagName('stdyDscr')[0]
            .getElementsByTagName('biblCit')[0].textContent;
        var start = citation.indexOf('http');
        var temp = citation.substr(start);
        var end = temp.indexOf(',');
        this.doi = temp.substr(0, end);
        this.firstCitat = citation.substr(0, start);
        this.firstCitat = this.firstCitat;
        this.secondCitat = temp.substr(end);
        this.secondCitat = this.secondCitat;
        this.filename = this.data
            .getElementsByTagName('fileDscr')[0]
            .getElementsByTagName('fileName')[0].textContent;
        this.showDDI();
    };
    InterfaceComponent.prototype.showVarsGroups = function () {
        var elm = this.data.getElementsByTagName('varGrp');
        for (var i = 0; i < elm.length; i++) {
            var obj = JSON.parse(Object(_assets_js_xml2json__WEBPACK_IMPORTED_MODULE_3__["xml2json"])(elm[i], ''));
            this._variable_groups.push(obj);
        }
    };
    InterfaceComponent.prototype.showVars = function () {
        var variables = [];
        var elm = this.data.getElementsByTagName('var');
        for (var _i = 0, elm_1 = elm; _i < elm_1.length; _i++) {
            var elm_in = elm_1[_i];
            variables.push(JSON.parse(Object(_assets_js_xml2json__WEBPACK_IMPORTED_MODULE_3__["xml2json"])(elm_in, '')));
        }
        // flatten the table structure so it can be sorted/filtered appropriately
        var flat_array = [];
        for (var i = 0; i < variables.length; i++) {
            var obj = variables[i];
            // make equivalent variable to allow sorting
            for (var j in obj.var) {
                if (j.indexOf('@') === 0) {
                    obj.var[j.substring(1).toLowerCase()] = obj.var[j];
                }
            }
            if (typeof obj.var.catgry !== 'undefined') {
                if (typeof obj.var.catgry.length === 'undefined') {
                    // If there is only one category
                    obj.var.catgry = [obj.var.catgry];
                }
                for (var k = 0; k < obj.var.catgry.length; k++) {
                    if (typeof obj.var.catgry[k].catStat !== 'undefined') {
                        if (typeof obj.var.catgry[k].catStat.length === 'undefined') {
                            obj.var.catgry[k].catStat = [obj.var.catgry[k].catStat];
                        }
                    }
                }
            }
            if (typeof obj.var.universe !== 'undefined') {
                if (typeof obj.var.universe.size === 'undefined') {
                    obj.var.universe = { '#text': obj.var.universe };
                }
            }
            if (typeof obj.var.notes !== 'undefined') {
                if (typeof obj.var.notes.length !== undefined && obj.var.notes.length === 2) {
                    obj.var.notes = { '#cdata': obj.var.notes[1]['#cdata'],
                        '#text': obj.var.notes[0]['#text'],
                        '@level': obj.var.notes[0]['@level'],
                        '@subject': obj.var.notes[0]['@subject'],
                        '@type': obj.var.notes[0]['@type'] };
                }
            }
            flat_array.push(obj.var);
        }
        this._variables = flat_array;
        this.child.onUpdateVars(this._variables);
    };
    // pass the selected ids to the var table for display
    InterfaceComponent.prototype.broadcastSubSetRows = function (ids) {
        this.child.onSubset(ids);
    };
    InterfaceComponent.prototype.broadcastSelect = function (_id) {
        // set the var table header to show the selection
        this.child.selectGroup(_id);
    };
    InterfaceComponent.prototype.broadcastDraggedGroup = function (_id) {
        this.child.draggedGroup(_id);
    };
    InterfaceComponent.prototype.broadcastDeselectGroup = function () {
        this.child.disableSelectGroup();
    };
    InterfaceComponent.prototype.showDDI = function () {
        this.ddi_loaded = true;
    };
    // Create the XML File
    InterfaceComponent.prototype.makeXML = function () {
        var doc = new xml_writer__WEBPACK_IMPORTED_MODULE_7__();
        doc.startDocument();
        doc.startElement('dataDscr');
        // add groups
        for (var _i = 0, _a = this._variable_groups; _i < _a.length; _i++) {
            var group = _a[_i];
            if (group.varGrp.labl !== null && group.varGrp.labl.trim() !== '') {
                doc.startElement('varGrp');
                doc.writeAttribute('ID', group.varGrp['@ID']);
                doc.writeAttribute('var', group.varGrp['@var']);
                doc.startElement('labl');
                doc.text(group.varGrp.labl);
                doc.endElement();
                doc.endElement();
            }
        }
        // add variables
        for (var i = 0; i < this._variables.length; i++) {
            // start variable (var)
            doc.startElement('var');
            doc.writeAttribute('ID', this._variables[i]['@ID']);
            doc.writeAttribute('name', this._variables[i]['@name']);
            if (typeof this._variables[i]['@intrvl'] !== 'undefined') {
                doc.writeAttribute('intrvl', this._variables[i]['@intrvl']);
            }
            if (typeof this._variables[i]['@wgt'] !== 'undefined' && this._variables[i]['@wgt'] !== '') {
                doc.writeAttribute('wgt', this._variables[i]['@wgt']);
            }
            if (typeof this._variables[i]['@wgt-var'] !== 'undefined' && this._variables[i]['@wgt-var'] !== '') {
                doc.writeAttribute('wgt-var', this._variables[i]['@wgt-var']);
            }
            // start location
            if (typeof this._variables[i].location !== 'undefined') {
                doc.startElement('location').writeAttribute('fileid', this._variables[i].location['@fileid']);
                doc.endElement();
            }
            // end location
            // start labl
            if (typeof this._variables[i].labl !== 'undefined') {
                doc.startElement('labl');
                doc.writeAttribute('level', this._variables[i].labl['@level']);
                doc.text(this._variables[i].labl['#text']);
                doc.endElement();
            }
            // end labl
            // start sumStat
            if (typeof this._variables[i].sumStat !== 'undefined') {
                if (typeof this._variables[i].sumStat.length !== 'undefined') {
                    for (var j = 0; j < this._variables[i].sumStat.length; j++) {
                        doc.startElement('sumStat');
                        doc.writeAttribute('type', this._variables[i].sumStat[j]['@type']);
                        doc.text(this._variables[i].sumStat[j]['#text']);
                        doc.endElement();
                    }
                }
            }
            // end sumStat
            // start catgry
            if (typeof this._variables[i].catgry !== 'undefined') {
                if (typeof this._variables[i].catgry.length !== 'undefined') {
                    for (var j = 0; j < this._variables[i].catgry.length; j++) {
                        doc.startElement('catgry');
                        if (typeof this._variables[i].catgry[j].catValu !== 'undefined') {
                            doc.startElement('catValu').text(this._variables[i].catgry[j].catValu);
                            doc.endElement();
                        }
                        if (typeof this._variables[i].catgry[j].labl !== 'undefined') {
                            doc.startElement('labl');
                            doc.writeAttribute('level', this._variables[i].catgry[j].labl['@level']);
                            doc.text(this._variables[i].catgry[j].labl['#text']);
                            doc.endElement();
                        }
                        if (typeof this._variables[i].catgry[j].catStat !== 'undefined') {
                            // frequency
                            if (typeof this._variables[i].catgry[j].catStat.length !== 'undefined') {
                                doc.startElement('catStat');
                                doc.writeAttribute('type', this._variables[i].catgry[j].catStat[0]['@type']);
                                doc.text(this._variables[i].catgry[j].catStat[0]['#text']);
                                doc.endElement();
                                // weighted frequency
                                if (this._variables[i].catgry[j].catStat.length > 1) {
                                    doc.startElement('catStat');
                                    doc.writeAttribute('wgtd', this._variables[i].catgry[j].catStat[1]['@wgtd']);
                                    doc.writeAttribute('type', this._variables[i].catgry[j].catStat[1]['@type']);
                                    doc.text(this._variables[i].catgry[j].catStat[1]['#text']);
                                    doc.endElement();
                                }
                            }
                        }
                        doc.endElement();
                    }
                }
            }
            // end catgry
            // start qstn
            if (typeof this._variables[i].qstn !== 'undefined' &&
                ((typeof this._variables[i].qstn.qstnLit !== 'undefined' && this._variables[i].qstn.qstnLit !== '') ||
                    (typeof this._variables[i].qstn.ivuInstr !== 'undefined' && this._variables[i].qstn.ivuInstr !== '') ||
                    (typeof this._variables[i].qstn.postQTxt !== 'undefined' && this._variables[i].qstn.postQTxt !== ''))) {
                doc.startElement('qstn');
                if (typeof this._variables[i].qstn.qstnLit !== 'undefined') {
                    doc.startElement('qstnLit').text(this._variables[i].qstn.qstnLit);
                    doc.endElement();
                }
                if (typeof this._variables[i].qstn.ivuInstr !== 'undefined') {
                    doc.startElement('ivuInstr').text(this._variables[i].qstn.ivuInstr);
                    doc.endElement();
                }
                if (typeof this._variables[i].qstn.postQTxt !== 'undefined') {
                    doc.startElement('postQTxt').text(this._variables[i].qstn.postQTxt);
                    doc.endElement();
                }
                doc.endElement();
            }
            // end qstn
            // start varFormat
            if (typeof this._variables[i].varFormat !== 'undefined') {
                doc.startElement('varFormat');
                doc.writeAttribute('type', this._variables[i].varFormat['@type']);
                doc.endElement();
            }
            // end varFormat
            // start notes
            if (typeof this._variables[i].notes !== 'undefined') {
                // start notes cdata
                if (typeof this._variables[i].notes['#cdata'] !== 'undefined' && this._variables[i].notes['#cdata'] !== '') {
                    doc.startElement('notes');
                    doc.startCData();
                    doc.writeCData(this._variables[i].notes['#cdata']);
                    doc.endCData();
                    doc.endElement();
                }
                // end notes cdata
                doc.startElement('notes');
                doc.writeAttribute('subject', this._variables[i].notes['@subject']);
                doc.writeAttribute('level', this._variables[i].notes['@level']);
                doc.writeAttribute('type', this._variables[i].notes['@type']);
                doc.text(this._variables[i].notes['#text']);
                doc.endElement();
            }
            // end notes
            // start universe
            if (typeof this._variables[i].universe !== 'undefined' && this._variables[i].universe['#text'] !== '') {
                doc.startElement('universe');
                doc.text(this._variables[i].universe['#text']);
                doc.endElement();
            }
            // end universe
            // end variable (var)
            doc.endElement();
        }
        doc.endDocument();
        return doc;
    };
    // Save the XML file locally
    InterfaceComponent.prototype.onSave = function () {
        var doc = this.makeXML();
        var text = new Blob([doc.toString()], { type: 'application/xml' });
        var tl = this.title + '.xml';
        file_saver__WEBPACK_IMPORTED_MODULE_6__["saveAs"](text, 'dct.xml');
    };
    // Send the XML to Dataverse
    InterfaceComponent.prototype.sendToDV = function () {
        var key = this.ddiService.getParameterByName('key');
        var doc = this.makeXML();
        if (key !== null) {
            var url = this._base_url + '/api/edit/' + this._id; // + "/" + this._metaId;
            this.ddiService
                .putDDI(url, doc.toString(), key)
                .subscribe(function (data) {
                console.log('Data ');
                console.log(data);
            }, function (error) {
                console.log('Error');
                console.log(error);
            }, function () { return console.log('Ok'); });
        }
        else {
            console.log('API Key missing');
        }
    };
    tslib__WEBPACK_IMPORTED_MODULE_0__["__decorate"]([
        Object(_angular_core__WEBPACK_IMPORTED_MODULE_1__["ViewChild"])(_var_var_component__WEBPACK_IMPORTED_MODULE_5__["VarComponent"]),
        tslib__WEBPACK_IMPORTED_MODULE_0__["__metadata"]("design:type", Object)
    ], InterfaceComponent.prototype, "child", void 0);
    tslib__WEBPACK_IMPORTED_MODULE_0__["__decorate"]([
        Object(_angular_core__WEBPACK_IMPORTED_MODULE_1__["ViewChild"])('scrollMe'),
        tslib__WEBPACK_IMPORTED_MODULE_0__["__metadata"]("design:type", _angular_core__WEBPACK_IMPORTED_MODULE_1__["ElementRef"])
    ], InterfaceComponent.prototype, "myScrollContainer", void 0);
    InterfaceComponent = tslib__WEBPACK_IMPORTED_MODULE_0__["__decorate"]([
        Object(_angular_core__WEBPACK_IMPORTED_MODULE_1__["Component"])({
            selector: 'app-interface',
            template: __webpack_require__(/*! ./interface.component.html */ "./src/app/interface/interface.component.html"),
            styles: [__webpack_require__(/*! ./interface.component.css */ "./src/app/interface/interface.component.css")]
        }),
        Object(_angular_core__WEBPACK_IMPORTED_MODULE_1__["NgModule"])({
            imports: [_angular_material__WEBPACK_IMPORTED_MODULE_4__["MatButtonModule"]],
            exports: [_angular_material__WEBPACK_IMPORTED_MODULE_4__["MatButtonModule"]]
        }),
        tslib__WEBPACK_IMPORTED_MODULE_0__["__metadata"]("design:paramtypes", [_ddi_service__WEBPACK_IMPORTED_MODULE_2__["DdiService"]])
    ], InterfaceComponent);
    return InterfaceComponent;
}());



/***/ }),

/***/ "./src/app/var-dialog/var-dialog.component.css":
/*!*****************************************************!*\
  !*** ./src/app/var-dialog/var-dialog.component.css ***!
  \*****************************************************/
/*! no static exports found */
/***/ (function(module, exports) {

module.exports = ".text-inside-grid > div {\n  justify-content: flex-start !important;\n}\n\n.field_width {\n  width: 480px !important;\n}\n\n.textarea_height {\n  height: 80px;\n}\n\n.mat-dialog-content {\n  overflow: visible;\n}\n\n/*# sourceMappingURL=data:application/json;base64,eyJ2ZXJzaW9uIjozLCJzb3VyY2VzIjpbInNyYy9hcHAvdmFyLWRpYWxvZy92YXItZGlhbG9nLmNvbXBvbmVudC5jc3MiXSwibmFtZXMiOltdLCJtYXBwaW5ncyI6IkFBQUE7RUFDRSxzQ0FBc0M7QUFDeEM7O0FBRUE7RUFDRSx1QkFBdUI7QUFDekI7O0FBRUE7RUFDRSxZQUFZO0FBQ2Q7O0FBRUE7RUFDRSxpQkFBaUI7QUFDbkIiLCJmaWxlIjoic3JjL2FwcC92YXItZGlhbG9nL3Zhci1kaWFsb2cuY29tcG9uZW50LmNzcyIsInNvdXJjZXNDb250ZW50IjpbIi50ZXh0LWluc2lkZS1ncmlkID4gZGl2IHtcbiAganVzdGlmeS1jb250ZW50OiBmbGV4LXN0YXJ0ICFpbXBvcnRhbnQ7XG59XG5cbi5maWVsZF93aWR0aCB7XG4gIHdpZHRoOiA0ODBweCAhaW1wb3J0YW50O1xufVxuXG4udGV4dGFyZWFfaGVpZ2h0IHtcbiAgaGVpZ2h0OiA4MHB4O1xufVxuXG4ubWF0LWRpYWxvZy1jb250ZW50IHtcbiAgb3ZlcmZsb3c6IHZpc2libGU7XG59XG4iXX0= */"

/***/ }),

/***/ "./src/app/var-dialog/var-dialog.component.html":
/*!******************************************************!*\
  !*** ./src/app/var-dialog/var-dialog.component.html ***!
  \******************************************************/
/*! no static exports found */
/***/ (function(module, exports) {

module.exports = "<div mat-dialog-content >\n  <form [formGroup]=\"form\" (ngSubmit)=\"submit(form)\">\n    <h1 mat-dialog-title i18n>Variable Information</h1>\n\n    <mat-dialog-content>\n      <mat-grid-list class=\"table-controls\" cols=\"2\" rowHeight=\"62\">\n        <mat-grid-tile colspan=\"1\" rowspan=\"1\">\n          <mat-form-field>\n            <input matInput formControlName=\"id\" i18n-placeholder placeholder=\"ID\" value=\"{{data['@ID']}}\"  >\n          </mat-form-field>\n        </mat-grid-tile>\n        <mat-grid-tile colspan=\"1\" rowspan=\"1\">\n          <mat-form-field>\n            <input matInput formControlName=\"name\" i18n-placeholder placeholder=\"Name\" value=\"{{data['@name']}}\" >\n          </mat-form-field>\n        </mat-grid-tile>\n\n        <mat-grid-tile colspan=\"2\" rowspan=\"1\">\n          <mat-form-field  class=\"field_width\">\n             <input matInput formControlName=\"labl\" i18n-placeholder placeholder=\"Label\" value=\"{{data.labl['#text']}}\">\n          </mat-form-field>\n        </mat-grid-tile>\n        <mat-grid-tile colspan=\"2\" rowspan=\"1\">\n          <mat-form-field  class=\"field_width\">\n            <input matInput formControlName=\"qstnLit\" i18n-placeholder placeholder=\"Literal Question\" value=\"{{data.qstn.qstnLit}}\">\n          </mat-form-field>\n        </mat-grid-tile>\n\n        <mat-grid-tile colspan=\"2\" rowspan=\"1\">\n          <mat-form-field  class=\"field_width\">\n            <input matInput formControlName=\"ivuInstr\" i18n-placeholder placeholder=\"Interviewer Instructions\" value=\"{{data.qstn.ivuInstr}}\">\n          </mat-form-field>\n        </mat-grid-tile>\n        <mat-grid-tile colspan=\"2\" rowspan=\"1\">\n          <mat-form-field  class=\"field_width\">\n            <input matInput formControlName=\"postQTxt\" i18n-placeholder placeholder=\"Post Question\" value=\"{{data.qstn.postQTxt}}\">\n          </mat-form-field>\n        </mat-grid-tile>\n        <mat-grid-tile colspan=\"2\" rowspan=\"1\">\n          <mat-form-field  class=\"field_width\">\n            <input matInput formControlName=\"universe\" i18n-placeholder placeholder=\"Universe\" value=\"{{data.universe['#text']}}\">\n          </mat-form-field>\n        </mat-grid-tile>\n        <mat-grid-tile colspan=\"2\" rowspan=\"2\">\n          <mat-form-field class=\"field_width\" >\n            <textarea matInput formControlName=\"notes\" class=\"textarea_height\" i18n-placeholder placeholder=\"Notes\" value=\"{{data.notes['#cdata']}}\" ></textarea>\n          </mat-form-field>\n        </mat-grid-tile>\n        <!--Group chip list-->\n        <mat-grid-tile colspan=\"2\" rowspan=\"1\" class=\"field_width\">\n          <mat-form-field class=\"field_width\">\n            <mat-select formControlName=\"_groups\" [(value)]=\"data['_groups']\"  i18n-placeholder placeholder=\"Group\" multiple=\"true\">\n              <mat-option *ngFor=\"let g of _variable_groups\" [value]=\"g.varGrp['@ID']\" >\n                {{ g.varGrp.labl}}\n              </mat-option>\n            </mat-select>\n          </mat-form-field>\n        </mat-grid-tile>\n\n        <mat-grid-tile colspan=\"1\" rowspan=\"1\">\n          <mat-form-field>\n            <mat-select formControlName=\"wgt_var\" [(value)]=\"data['@wgt-var']\" i18n-placeholder placeholder=\"Weight Variable\">\n              <mat-option *ngFor=\"let w of weights\" [value]=\"w\">\n                {{ w }}\n              </mat-option>\n              <mat-option i18n>Unweighted</mat-option>\n            </mat-select>\n          </mat-form-field>\n        </mat-grid-tile>\n        <mat-grid-tile colspan=\"1\" rowspan=\"1\">\n          <section class=\"example-section\">\n            <mat-checkbox formControlName=\"wgt\" align=\"end\" checked=\"{{data['@wgt']}}\" i18n>Is Weight</mat-checkbox>\n          </section>\n        </mat-grid-tile>\n        <mat-grid-tile colspan=\"1\" rowspan=\"1\">\n          <button mat-button type=\"submit\" i18n>Update</button>\n        </mat-grid-tile>\n        <mat-grid-tile colspan=\"1\" rowspan=\"1\">\n          <button mat-button type=\"button\" (click)=\"dialogRef.close()\" i18n>Cancel</button>\n        </mat-grid-tile>\n      </mat-grid-list>\n    </mat-dialog-content>\n  </form>\n</div>\n"

/***/ }),

/***/ "./src/app/var-dialog/var-dialog.component.ts":
/*!****************************************************!*\
  !*** ./src/app/var-dialog/var-dialog.component.ts ***!
  \****************************************************/
/*! exports provided: VarDialogComponent */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
__webpack_require__.r(__webpack_exports__);
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "VarDialogComponent", function() { return VarDialogComponent; });
/* harmony import */ var tslib__WEBPACK_IMPORTED_MODULE_0__ = __webpack_require__(/*! tslib */ "./node_modules/tslib/tslib.es6.js");
/* harmony import */ var _angular_core__WEBPACK_IMPORTED_MODULE_1__ = __webpack_require__(/*! @angular/core */ "./node_modules/@angular/core/fesm5/core.js");
/* harmony import */ var _angular_material__WEBPACK_IMPORTED_MODULE_2__ = __webpack_require__(/*! @angular/material */ "./node_modules/@angular/material/esm5/material.es5.js");
/* harmony import */ var _angular_forms__WEBPACK_IMPORTED_MODULE_3__ = __webpack_require__(/*! @angular/forms */ "./node_modules/@angular/forms/fesm5/forms.js");
/* harmony import */ var _ddi_service__WEBPACK_IMPORTED_MODULE_4__ = __webpack_require__(/*! ../ddi.service */ "./src/app/ddi.service.ts");





var VarDialogComponent = /** @class */ (function () {
    function VarDialogComponent(data, formBuilder, dialogRef, ddiService) {
        this.data = data;
        this.formBuilder = formBuilder;
        this.dialogRef = dialogRef;
        this.ddiService = ddiService;
        this.parentUpdateVar = new _angular_core__WEBPACK_IMPORTED_MODULE_1__["EventEmitter"]();
        this.edit_objs = [];
    }
    VarDialogComponent.prototype.ngOnInit = function () {
        // Note: data is passed as any array to allow for multi editing
        console.log('OnInit');
        console.log(this.data);
        if (this.data.length > 1) {
            var selected_ids = [];
            for (var j = 0; j < this.data.length; j++) {
                selected_ids.push(this.data[j]['@ID']);
            }
            var obj = {
                '@ID': selected_ids.join(','),
                _group_edit: true
            };
            this.edit_objs = this.data;
            this.data = obj;
        }
        else {
            this.data = this.data[0];
        }
        // add the groups - create an id with all of them
        var groups = [];
        for (var i = 0; i < this._variable_groups.length; i++) {
            var group_vars = this._variable_groups[i].varGrp['@var'].split(' ');
            if (group_vars.indexOf(this.data['@ID']) > -1) {
                groups.push(this._variable_groups[i].varGrp['@ID']);
            }
        }
        this.data['_groups'] = [groups]; // groups;
        this.addOmittedProperties(this.data);
        console.log('Universe text ' + this.data.universe['#text']);
        this.form = this.formBuilder.group({
            id: [
                { value: this.data ? this.data['@ID'] : '', disabled: true },
                _angular_forms__WEBPACK_IMPORTED_MODULE_3__["Validators"].required
            ],
            name: [
                { value: this.data ? this.data['@name'] : '', disabled: true },
                _angular_forms__WEBPACK_IMPORTED_MODULE_3__["Validators"].required
            ],
            labl: [this.data ? this.data['labl']['#text'] : '', _angular_forms__WEBPACK_IMPORTED_MODULE_3__["Validators"].required],
            qstnLit: this.data ? this.data.qstn['qstnLit'] : '',
            universe: this.data ? this.data.universe['#text'] : '',
            ivuInstr: this.data ? this.data.qstn['ivuInstr'] : '',
            postQTxt: this.data ? this.data.qstn['postQTxt'] : '',
            notes: this.data ? this.data.notes['#cdata'] : '',
            wgt: this.data ? this.data['@wgt'] : '',
            wgt_var: this.data ? this.data['@wgt-var'] : '',
            _groups: this.data ? this.data['_groups'] : []
        });
    };
    VarDialogComponent.prototype.isSelected = function (_id) {
        console.log(_id);
        return true;
    };
    VarDialogComponent.prototype.addOmittedProperties = function (_obj) {
        if (typeof _obj.qstn === 'undefined') {
            _obj.qstn = {};
        }
        if (typeof _obj.labl === 'undefined') {
            _obj.labl = { '#text': '' };
        }
        if (typeof _obj.universe === 'undefined') {
            _obj.universe = {
                '#text': '',
                '@clusion': ''
            };
        }
        if (typeof _obj.notes === 'undefined') {
            _obj.notes = {
                '#cdata': ''
            };
        }
        return _obj;
    };
    VarDialogComponent.prototype.updateObjValues = function (_obj, form) {
        // update label
        this.updateObjValue(_obj, 'labl.#text', form.controls.labl);
        // Literal Question - data.qstn.qstnLit
        this.updateObjValue(_obj, 'qstn.qstnLit', form.controls.qstnLit);
        // Interviewer Instructions - data.qstn.ivuInstr
        this.updateObjValue(_obj, 'qstn.ivuInstr', form.controls.ivuInstr);
        // Post Question - data.qstn.postQTxt
        this.updateObjValue(_obj, 'qstn.postQTxt', form.controls.postQTxt);
        // Universe - data.universe
        this.updateObjValue(_obj, 'universe.#text', form.controls.universe);
        // update notes if available data.notes
        // TODO surround notes with <![CDATA[ before saving back to xml
        this.updateObjValue(_obj, 'notes.#cdata', form.controls.notes);
        //
        this.updateObjValue(_obj, '@wgt-var', form.controls.wgt_var);
        //
        if (form.controls.wgt.value === true) {
            _obj['@wgt'] = 'wgt';
        }
        else {
            _obj['@wgt'] = '';
            // this.removeWeightedFrequencies(_obj);
        }
        // add variable to group
        if (form.controls._groups.dirty === true) {
            for (var i = 0; i < form.controls._groups.value.length; i++) {
                var group = this.getVariableGroup(form.controls._groups.value[i]);
                // check to see if the selected group already has the selected variable
                var vars = group['@var'].split(' ');
                if (vars.indexOf(_obj['@ID']) === -1) {
                    vars.push(_obj['@ID']);
                    // set the vars variable back to the group
                    group['@var'] = vars.join(' ');
                }
            }
        }
        return _obj;
    };
    VarDialogComponent.prototype.getVariableGroup = function (_id) {
        // loop though all the variables in the varaible groups and create a complete list
        for (var i = 0; i < this._variable_groups.length; i++) {
            if (this._variable_groups[i].varGrp['@ID'] === _id) {
                return this._variable_groups[i].varGrp;
            }
        }
    };
    VarDialogComponent.prototype.updateObjValue = function (_obj, _var, _control) {
        if (_control.dirty === true) {
            var stack = _var.split('.');
            while (stack.length > 1) {
                _obj = _obj[stack.shift()];
            }
            _obj[stack.shift()] = _control.value;
        }
    };
    VarDialogComponent.prototype.submit = function (form) {
        if (this.edit_objs.length > 0) {
            // take all the objects from the
            for (var i = 0; i < this.edit_objs.length; i++) {
                this.data = this.edit_objs[i];
                this.addOmittedProperties(this.data);
                this.updateObjValues(this.data, form);
                this.parentUpdateVar.emit(this.data);
            }
        }
        else {
            this.updateObjValues(this.data, form);
            this.parentUpdateVar.emit(this.data);
        }
        if (typeof this.data['@wgt-var'] !== 'undefined') {
            this.calculateWeightedFrequencies();
        }
        else {
            // Removing weighted frequency
            if (typeof this.data.catgry !== 'undefined') {
                for (var k = 0; k < this.data.catgry.length; k++) {
                    if (typeof this.data.catgry[k].catStat !== 'undefined' &&
                        this.data.catgry[k].catStat.length > 1) {
                        this.data.catgry[k].catStat.splice(1, 1);
                    }
                }
            }
        }
        this.dialogRef.close("" + form);
    };
    VarDialogComponent.prototype.calculateWeightedFrequencies = function () {
        var _this = this;
        console.log('Start calculate freq');
        console.log(this.data);
        var weight_variable = this.data['@wgt-var'];
        var variable = this.data['@ID'];
        console.log(weight_variable);
        if (typeof weight_variable !== 'undefined') {
            console.log('variable defined');
            var id = this.ddiService.getParameterByName('dfId');
            var base_url = this.ddiService.getBaseUrl();
            var key = this.ddiService.getParameterByName('key');
            var detail_url = base_url +
                '/api/access/datafile/' +
                id +
                '?format=subset&variables=' +
                variable +
                ',' +
                weight_variable +
                '&key=' +
                key;
            console.log(detail_url);
            this.ddiService
                .getDDI(detail_url)
                .subscribe(function (data) { return _this.processVariable(data); }, function (error) { return console.log(error); }, function () { return _this.completeVariable(); });
            //  http://localhost:8080/api/access/datafile/41?variables=v885
        }
        console.log('End calculate freq');
    };
    VarDialogComponent.prototype.processVariable = function (data) {
        this.weights_and_variable = data.split('\n');
    };
    VarDialogComponent.prototype.completeVariable = function () {
        var map_wf = new Map();
        for (var i = 1; i < this.weights_and_variable.length; i++) {
            var vr = this.weights_and_variable[i].split('\t');
            if (map_wf.has(vr[0])) {
                var wt = map_wf.get(vr[0]);
                wt = parseFloat(wt) + parseFloat(vr[1]);
                map_wf.set(vr[0], wt);
            }
            else {
                map_wf.set(vr[0], vr[1]);
            }
        }
        // map_wf.forEach((v, k) => {console.log(v + ' ' + k + ';'); });
        console.log('Complete');
        for (var i = 0; i < this.data.catgry.length; i++) {
            if (!map_wf.has(this.data.catgry[i].catValu)) {
                map_wf.set(this.data.catgry[i].catValu, 0);
            }
            if (typeof this.data.catgry[i].catStat !== 'undefined') {
                if (typeof this.data.catgry[i].catStat.length !== 'undefined') {
                    if (this.data.catgry[i].catStat.length > 1) {
                        this.data.catgry[i].catStat[1] = {
                            '@wgtd': 'wgtd',
                            '@type': 'freq',
                            '#text': map_wf.get(this.data.catgry[i].catValu)
                        };
                    }
                    else {
                        this.data.catgry[i].catStat.push({
                            '@wgtd': 'wgtd',
                            '@type': 'freq',
                            '#text': map_wf.get(this.data.catgry[i].catValu)
                        });
                    }
                }
            }
        }
    };
    tslib__WEBPACK_IMPORTED_MODULE_0__["__decorate"]([
        Object(_angular_core__WEBPACK_IMPORTED_MODULE_1__["Output"])(),
        tslib__WEBPACK_IMPORTED_MODULE_0__["__metadata"]("design:type", _angular_core__WEBPACK_IMPORTED_MODULE_1__["EventEmitter"])
    ], VarDialogComponent.prototype, "parentUpdateVar", void 0);
    VarDialogComponent = tslib__WEBPACK_IMPORTED_MODULE_0__["__decorate"]([
        Object(_angular_core__WEBPACK_IMPORTED_MODULE_1__["Component"])({
            selector: 'app-var-dialog',
            template: __webpack_require__(/*! ./var-dialog.component.html */ "./src/app/var-dialog/var-dialog.component.html"),
            styles: [__webpack_require__(/*! ./var-dialog.component.css */ "./src/app/var-dialog/var-dialog.component.css")]
        }),
        tslib__WEBPACK_IMPORTED_MODULE_0__["__param"](0, Object(_angular_core__WEBPACK_IMPORTED_MODULE_1__["Inject"])(_angular_material__WEBPACK_IMPORTED_MODULE_2__["MAT_DIALOG_DATA"])),
        tslib__WEBPACK_IMPORTED_MODULE_0__["__metadata"]("design:paramtypes", [Object, _angular_forms__WEBPACK_IMPORTED_MODULE_3__["FormBuilder"],
            _angular_material__WEBPACK_IMPORTED_MODULE_2__["MatDialogRef"],
            _ddi_service__WEBPACK_IMPORTED_MODULE_4__["DdiService"]])
    ], VarDialogComponent);
    return VarDialogComponent;
}());



/***/ }),

/***/ "./src/app/var-group/var-group.component.css":
/*!***************************************************!*\
  !*** ./src/app/var-group/var-group.component.css ***!
  \***************************************************/
/*! no static exports found */
/***/ (function(module, exports) {

module.exports = "\n/*# sourceMappingURL=data:application/json;base64,eyJ2ZXJzaW9uIjozLCJzb3VyY2VzIjpbXSwibmFtZXMiOltdLCJtYXBwaW5ncyI6IiIsImZpbGUiOiJzcmMvYXBwL3Zhci1ncm91cC92YXItZ3JvdXAuY29tcG9uZW50LmNzcyJ9 */"

/***/ }),

/***/ "./src/app/var-group/var-group.component.html":
/*!****************************************************!*\
  !*** ./src/app/var-group/var-group.component.html ***!
  \****************************************************/
/*! no static exports found */
/***/ (function(module, exports) {

module.exports = "<mat-nav-list class=\"button-header\" xmlns=\"http://www.w3.org/1999/html\">\n\n  <a mat-list-item (click)=\"addTab()\"><span i18n>Add Group</span><span class=\"fill-space\"></span><mat-icon>playlist_add</mat-icon></a>\n\n</mat-nav-list>\n<mat-nav-list>\n\n  <a mat-list-item (click)=\"showAll()\" [ngClass]=\"{'active': all_active }\" i18n>All</a>\n\n  <a mat-list-item id=\"{{tab.varGrp['@ID']}}\" draggable=\"true\" [disableRipple]=\"true\"  (dragstart)=\"dragstart($event);onGroupClick(tab);trackDragRow(tab)\" (dragenter)=\"dragenter($event,tab)\" (dragend)=\"dragend($event)\"  *ngFor=\"let tab of _variable_groups\" (click)=\"onGroupClick(tab)\" (dblclick)=\"onGroupDblClick(tab)\" [ngClass]=\"{'active': tab.active }\" >\n    <mat-icon aria-label=\"drag\" i18n-matTooltip matTooltip=\"Drag Me\" style=\"margin-left:-7px\">drag_indicator</mat-icon>\n    <span *ngIf=\"!tab.editable\">{{tab.varGrp.labl}}</span>\n    <mat-form-field *ngIf=\"tab.editable\" class=\"fixed_width\">\n      <input matInput maxLength=\"50\" value=\"{{tab.varGrp.labl}}\" #titleInput  (keyup.enter) =\"renameGroupComplete(tab,titleInput.value )\">\n        <span button mat-icon-button matSuffix color=\"primary\"> <mat-icon i18n-aria-label aria-label=\"Done\" (click)=\"renameGroupComplete(tab,titleInput.value )\">done</mat-icon></span>\n        <span button mat-icon-button matSuffix color=\"primary\"> <mat-icon i18n-aria-label aria-label=\"Clear\" (click)=\"renameGroupCancel(tab)\">clear</mat-icon></span>\n        <span button mat-icon-button matSuffix color=\"primary\"> <mat-icon i18n-aria-label aria-label=\"Delete\" (click)=\"groupDelete(tab)\">delete</mat-icon></span>\n    </mat-form-field>\n\n    <span class=\"fill-space\"></span>\n    <button *ngIf=\"!tab.editable\" mat-icon-button color=\"primary\"><mat-icon aria-label=\"Edit\" (click)=\"renameGroup(tab)\">edit</mat-icon></button>\n\n  </a>\n\n</mat-nav-list>\n"

/***/ }),

/***/ "./src/app/var-group/var-group.component.ts":
/*!**************************************************!*\
  !*** ./src/app/var-group/var-group.component.ts ***!
  \**************************************************/
/*! exports provided: VarGroupComponent */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
__webpack_require__.r(__webpack_exports__);
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "VarGroupComponent", function() { return VarGroupComponent; });
/* harmony import */ var tslib__WEBPACK_IMPORTED_MODULE_0__ = __webpack_require__(/*! tslib */ "./node_modules/tslib/tslib.es6.js");
/* harmony import */ var _angular_core__WEBPACK_IMPORTED_MODULE_1__ = __webpack_require__(/*! @angular/core */ "./node_modules/@angular/core/fesm5/core.js");


var VarGroupComponent = /** @class */ (function () {
    function VarGroupComponent() {
        this.all_active = true;
        this.subSetRows = new _angular_core__WEBPACK_IMPORTED_MODULE_1__["EventEmitter"]();
        this.parentScrollNav = new _angular_core__WEBPACK_IMPORTED_MODULE_1__["EventEmitter"]();
        this.selectGroup = new _angular_core__WEBPACK_IMPORTED_MODULE_1__["EventEmitter"]();
        this.draggedGroup = new _angular_core__WEBPACK_IMPORTED_MODULE_1__["EventEmitter"]();
        this.disableSelectGroup = new _angular_core__WEBPACK_IMPORTED_MODULE_1__["EventEmitter"]();
        this.dragged_over_dir = 'before';
    }
    VarGroupComponent.prototype.ngOnInit = function () { };
    // Add a new group to the list and scroll to show it!
    VarGroupComponent.prototype.addTab = function () {
        // get the next id
        var ids = [];
        for (var _i = 0, _a = this._variable_groups; _i < _a.length; _i++) {
            var i = _a[_i];
            ids.push(Number(i.varGrp['@ID'].substring(2)));
        }
        ids.sort();
        var _id = 'VG';
        if (ids.length > 0) {
            _id += String(ids[ids.length - 1] + 1);
        }
        else {
            _id += '1';
        }
        var var_group = {};
        var_group.varGrp = {
            labl: '',
            '@var': '',
            '@ID': _id
        };
        var_group.varGrp['@var'] = '';
        this._variable_groups.push(var_group);
        var obj = this;
        obj._variable_groups[obj._variable_groups.length - 1].editable = true;
        setTimeout(function () {
            console.log('set time out');
            obj.parentScrollNav.emit();
            obj.onGroupClick(var_group);
        }, 100);
    };
    VarGroupComponent.prototype.onGroupClick = function (_obj) {
        var obj = _obj;
        var vars = obj.varGrp['@var'].split(' ');
        this.subSetRows.emit(vars);
        this.showActive(obj.varGrp['@ID']);
        this.selectGroup.emit(obj.varGrp['@ID']);
    };
    VarGroupComponent.prototype.onGroupDblClick = function (_obj) {
        this.renameGroup(_obj);
    };
    VarGroupComponent.prototype.renameGroup = function (_obj) {
        console.log('Rename group');
        _obj.editable = true;
    };
    VarGroupComponent.prototype.renameGroupComplete = function (_obj, _val) {
        console.log('renameGroupComplete');
        if (_val !== null && _val.trim() !== '') {
            _obj.varGrp.labl = _val.trim();
            _obj.editable = false;
        }
    };
    VarGroupComponent.prototype.renameGroupCancel = function (_obj) {
        console.log('renameGroupCancel');
        if (_obj.varGrp.labl !== null && _obj.varGrp.labl.trim() !== '') {
            _obj.editable = false;
        }
    };
    VarGroupComponent.prototype.groupDelete = function (_obj) {
        console.log('delete group');
        for (var i = 0; i < this._variable_groups.length; i++) {
            if (this._variable_groups[i].varGrp['@ID'] === _obj.varGrp['@ID']) {
                this._variable_groups.splice(i, 1);
            }
        }
    };
    VarGroupComponent.prototype.showActive = function (_id) {
        this.all_active = false;
        // show it's active
        for (var i = 0; i < this._variable_groups.length; i++) {
            if (this._variable_groups[i].varGrp['@ID'] === _id) {
                this._variable_groups[i].active = true;
            }
            else {
                this._variable_groups[i].active = false;
            }
        }
    };
    VarGroupComponent.prototype.showAll = function () {
        this.showActive();
        this.all_active = true;
        this.subSetRows.emit();
        this.disableSelectGroup.emit();
    };
    VarGroupComponent.prototype.dragstart = function ($event) {
        this.source = $event.currentTarget;
        $event.dataTransfer.effectAllowed = 'move';
    };
    VarGroupComponent.prototype.trackDragRow = function (_row) {
        this.dragged_obj = _row;
    };
    VarGroupComponent.prototype.dragenter = function ($event, _row) {
        var target = $event.currentTarget;
        if (!this.source) {
            // broadcast
            this.draggedGroup.emit($event.currentTarget.id);
            return;
        }
        if (_row === this.dragged_obj) {
            return;
        }
        this.dragged_over_obj = _row; // keep track of the dragged over obj to later update the list
        // need to determine how
        if (this.isbefore(this.source, target)) {
            target.parentNode.insertBefore(this.source, target); // insert before
            this.dragged_over_dir = 'before';
        }
        else {
            target.parentNode.insertBefore(this.source, target.nextSibling); // insert after
            this.dragged_over_dir = 'after';
        }
    };
    VarGroupComponent.prototype.isbefore = function (a, b) {
        if (a.parentNode === b.parentNode) {
            for (var cur = a; cur; cur = cur.previousSibling) {
                if (cur === b) {
                    return true;
                }
            }
        }
        return false;
    };
    VarGroupComponent.prototype.dragend = function ($event) {
        // make sure we've dragged over something
        if (!this.dragged_over_obj) {
            return;
        }
        // update the master list
        // remove the object
        this._variable_groups.splice(this._variable_groups
            .map(function (e) {
            return e.varGrp['@ID'];
        })
            .indexOf(this.dragged_obj.varGrp['@ID']), 1);
        var index = this._variable_groups
            .map(function (e) {
            return e.varGrp['@ID'];
        })
            .indexOf(this.dragged_over_obj.varGrp['@ID']);
        if (this.dragged_over_dir === 'before') {
            this._variable_groups.splice(index, 0, this.dragged_obj);
        }
        else {
            this._variable_groups.splice(index + 1, 0, this.dragged_obj);
        }
        delete this.dragged_obj;
        delete this.source; // remove reference
    };
    tslib__WEBPACK_IMPORTED_MODULE_0__["__decorate"]([
        Object(_angular_core__WEBPACK_IMPORTED_MODULE_1__["Input"])(),
        tslib__WEBPACK_IMPORTED_MODULE_0__["__metadata"]("design:type", Object)
    ], VarGroupComponent.prototype, "_variable_groups", void 0);
    tslib__WEBPACK_IMPORTED_MODULE_0__["__decorate"]([
        Object(_angular_core__WEBPACK_IMPORTED_MODULE_1__["Output"])(),
        tslib__WEBPACK_IMPORTED_MODULE_0__["__metadata"]("design:type", _angular_core__WEBPACK_IMPORTED_MODULE_1__["EventEmitter"])
    ], VarGroupComponent.prototype, "subSetRows", void 0);
    tslib__WEBPACK_IMPORTED_MODULE_0__["__decorate"]([
        Object(_angular_core__WEBPACK_IMPORTED_MODULE_1__["Output"])(),
        tslib__WEBPACK_IMPORTED_MODULE_0__["__metadata"]("design:type", _angular_core__WEBPACK_IMPORTED_MODULE_1__["EventEmitter"])
    ], VarGroupComponent.prototype, "parentScrollNav", void 0);
    tslib__WEBPACK_IMPORTED_MODULE_0__["__decorate"]([
        Object(_angular_core__WEBPACK_IMPORTED_MODULE_1__["Output"])(),
        tslib__WEBPACK_IMPORTED_MODULE_0__["__metadata"]("design:type", _angular_core__WEBPACK_IMPORTED_MODULE_1__["EventEmitter"])
    ], VarGroupComponent.prototype, "selectGroup", void 0);
    tslib__WEBPACK_IMPORTED_MODULE_0__["__decorate"]([
        Object(_angular_core__WEBPACK_IMPORTED_MODULE_1__["Output"])(),
        tslib__WEBPACK_IMPORTED_MODULE_0__["__metadata"]("design:type", _angular_core__WEBPACK_IMPORTED_MODULE_1__["EventEmitter"])
    ], VarGroupComponent.prototype, "draggedGroup", void 0);
    tslib__WEBPACK_IMPORTED_MODULE_0__["__decorate"]([
        Object(_angular_core__WEBPACK_IMPORTED_MODULE_1__["Output"])(),
        tslib__WEBPACK_IMPORTED_MODULE_0__["__metadata"]("design:type", _angular_core__WEBPACK_IMPORTED_MODULE_1__["EventEmitter"])
    ], VarGroupComponent.prototype, "disableSelectGroup", void 0);
    VarGroupComponent = tslib__WEBPACK_IMPORTED_MODULE_0__["__decorate"]([
        Object(_angular_core__WEBPACK_IMPORTED_MODULE_1__["Component"])({
            selector: 'app-var-group',
            template: __webpack_require__(/*! ./var-group.component.html */ "./src/app/var-group/var-group.component.html"),
            styles: [__webpack_require__(/*! ./var-group.component.css */ "./src/app/var-group/var-group.component.css")]
        }),
        tslib__WEBPACK_IMPORTED_MODULE_0__["__metadata"]("design:paramtypes", [])
    ], VarGroupComponent);
    return VarGroupComponent;
}());



/***/ }),

/***/ "./src/app/var-stat-dialog/var-stat-dialog.component.css":
/*!***************************************************************!*\
  !*** ./src/app/var-stat-dialog/var-stat-dialog.component.css ***!
  \***************************************************************/
/*! no static exports found */
/***/ (function(module, exports) {

module.exports = ".table-bordered {\n  border: 1px solid #ddd;\n  width: 100%;\n  border-spacing: 0;\n  border-collapse: collapse;\n}\n\n.table-bordered tr > td,\n.table-bordered tr > th {\n  border: 1px solid #ddd;\n  padding: 5px;\n  text-align: left;\n}\n\n/*# sourceMappingURL=data:application/json;base64,eyJ2ZXJzaW9uIjozLCJzb3VyY2VzIjpbInNyYy9hcHAvdmFyLXN0YXQtZGlhbG9nL3Zhci1zdGF0LWRpYWxvZy5jb21wb25lbnQuY3NzIl0sIm5hbWVzIjpbXSwibWFwcGluZ3MiOiJBQUFBO0VBQ0Usc0JBQXNCO0VBQ3RCLFdBQVc7RUFDWCxpQkFBaUI7RUFDakIseUJBQXlCO0FBQzNCOztBQUVBOztFQUVFLHNCQUFzQjtFQUN0QixZQUFZO0VBQ1osZ0JBQWdCO0FBQ2xCIiwiZmlsZSI6InNyYy9hcHAvdmFyLXN0YXQtZGlhbG9nL3Zhci1zdGF0LWRpYWxvZy5jb21wb25lbnQuY3NzIiwic291cmNlc0NvbnRlbnQiOlsiLnRhYmxlLWJvcmRlcmVkIHtcbiAgYm9yZGVyOiAxcHggc29saWQgI2RkZDtcbiAgd2lkdGg6IDEwMCU7XG4gIGJvcmRlci1zcGFjaW5nOiAwO1xuICBib3JkZXItY29sbGFwc2U6IGNvbGxhcHNlO1xufVxuXG4udGFibGUtYm9yZGVyZWQgdHIgPiB0ZCxcbi50YWJsZS1ib3JkZXJlZCB0ciA+IHRoIHtcbiAgYm9yZGVyOiAxcHggc29saWQgI2RkZDtcbiAgcGFkZGluZzogNXB4O1xuICB0ZXh0LWFsaWduOiBsZWZ0O1xufVxuIl19 */"

/***/ }),

/***/ "./src/app/var-stat-dialog/var-stat-dialog.component.html":
/*!****************************************************************!*\
  !*** ./src/app/var-stat-dialog/var-stat-dialog.component.html ***!
  \****************************************************************/
/*! no static exports found */
/***/ (function(module, exports) {

module.exports = "<div mat-dialog-content>\n\n  <h1 mat-dialog-title>{{data[\"@name\"]}}: {{data[\"labl\"][\"#text\"]}}</h1>\n\n  <app-chart [data]=\"sortedCategories\"></app-chart>\n\n  <table class=\"table-bordered\">\n  <tr>\n      <th i18n>Values</th>\n      <th i18n>Categories</th>\n      <th i18n>Count</th>\n      <th i18n>Weighted Count</th>\n  </tr>\n    <tr *ngFor=\"let row of sortedCategories\">\n      <td>{{row.catValu}}</td>\n      <td>{{row.labl[\"#text\"]}}</td>\n      <td *ngIf=\"isUndefined(row.catStat); else elseBlock\"></td>\n      <ng-template #elseBlock>\n        <td *ngIf=\"isUndefined(row.catStat.length); else elseBlock2\"></td>\n        <ng-template #elseBlock2>\n          <td>{{row.catStat[0][\"#text\"]}}</td>\n        </ng-template>\n      </ng-template>\n      <td *ngIf=\"isUndefined(row.catStat); else elseBloc\"></td>\n      <ng-template #elseBloc>\n        <td *ngIf=\"doesExist(row.catStat.length); else elseBloc2\">{{row.catStat[1][\"#text\"]}}</td>\n        <ng-template #elseBloc2>\n          <td></td>\n        </ng-template>\n      </ng-template>\n    </tr>\n  </table>\n\n  <mat-dialog-actions>\n      <button mat-button mat-dialog-close i18n>Close</button>\n  </mat-dialog-actions>\n\n</div>\n"

/***/ }),

/***/ "./src/app/var-stat-dialog/var-stat-dialog.component.ts":
/*!**************************************************************!*\
  !*** ./src/app/var-stat-dialog/var-stat-dialog.component.ts ***!
  \**************************************************************/
/*! exports provided: VarStatDialogComponent */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
__webpack_require__.r(__webpack_exports__);
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "VarStatDialogComponent", function() { return VarStatDialogComponent; });
/* harmony import */ var tslib__WEBPACK_IMPORTED_MODULE_0__ = __webpack_require__(/*! tslib */ "./node_modules/tslib/tslib.es6.js");
/* harmony import */ var _angular_core__WEBPACK_IMPORTED_MODULE_1__ = __webpack_require__(/*! @angular/core */ "./node_modules/@angular/core/fesm5/core.js");
/* harmony import */ var _angular_material__WEBPACK_IMPORTED_MODULE_2__ = __webpack_require__(/*! @angular/material */ "./node_modules/@angular/material/esm5/material.es5.js");



var VarStatDialogComponent = /** @class */ (function () {
    function VarStatDialogComponent(data) {
        this.data = data;
        this.sortedCategories = [];
    }
    VarStatDialogComponent.prototype.ngOnInit = function () {
        if (typeof this.data.catgry !== 'undefined') {
            console.log(this.data.catgry);
            if (typeof this.data.catgry.length === 'undefined') {
                this.sortedCategories.push(this.data.catgry);
            }
            else {
                for (var _i = 0, _a = this.data.catgry; _i < _a.length; _i++) {
                    var i = _a[_i];
                    console.log(i);
                    this.sortedCategories.push(i);
                }
            }
            console.log(this.sortedCategories);
            this.sortedCategories.sort(function (a, b) {
                return a.catValu - b.catValu;
            });
        }
    };
    VarStatDialogComponent.prototype.isUndefined = function (val) {
        return typeof val === 'undefined';
    };
    VarStatDialogComponent.prototype.doesExist = function (val) {
        return typeof val !== 'undefined' && val > 1;
    };
    VarStatDialogComponent = tslib__WEBPACK_IMPORTED_MODULE_0__["__decorate"]([
        Object(_angular_core__WEBPACK_IMPORTED_MODULE_1__["Component"])({
            selector: 'app-var-stat-dialog',
            template: __webpack_require__(/*! ./var-stat-dialog.component.html */ "./src/app/var-stat-dialog/var-stat-dialog.component.html"),
            styles: [__webpack_require__(/*! ./var-stat-dialog.component.css */ "./src/app/var-stat-dialog/var-stat-dialog.component.css")]
        }),
        tslib__WEBPACK_IMPORTED_MODULE_0__["__param"](0, Object(_angular_core__WEBPACK_IMPORTED_MODULE_1__["Inject"])(_angular_material__WEBPACK_IMPORTED_MODULE_2__["MAT_DIALOG_DATA"])),
        tslib__WEBPACK_IMPORTED_MODULE_0__["__metadata"]("design:paramtypes", [Object])
    ], VarStatDialogComponent);
    return VarStatDialogComponent;
}());



/***/ }),

/***/ "./src/app/var/var.component.css":
/*!***************************************!*\
  !*** ./src/app/var/var.component.css ***!
  \***************************************/
/*! no static exports found */
/***/ (function(module, exports) {

module.exports = ".grey {\n  background: #ebebeb;\n}\n\n.active {\n  background: #6d9cff;\n}\n\n.mat-column-drag {\n  flex:  0 0 40px;\n}\n\n.mat-column-select {\n  flex:  0 0 40px;\n}\n\n.mat-column-control {\n  flex:  0 0 30px;\n}\n\n.mat-column-id {\n  flex:  0 0 100px;\n}\n\n.mat-column-name {\n  flex:  0 0 100px;\n}\n\n.mat-column-labl {\n  flex:  0 0 500px;\n}\n\n/*# sourceMappingURL=data:application/json;base64,eyJ2ZXJzaW9uIjozLCJzb3VyY2VzIjpbInNyYy9hcHAvdmFyL3Zhci5jb21wb25lbnQuY3NzIl0sIm5hbWVzIjpbXSwibWFwcGluZ3MiOiJBQUFBO0VBQ0UsbUJBQW1CO0FBQ3JCOztBQUVBO0VBQ0UsbUJBQW1CO0FBQ3JCOztBQUVBO0VBQ0UsZUFBZTtBQUNqQjs7QUFFQTtFQUNFLGVBQWU7QUFDakI7O0FBRUE7RUFDRSxlQUFlO0FBQ2pCOztBQUVBO0VBQ0UsZ0JBQWdCO0FBQ2xCOztBQUVBO0VBQ0UsZ0JBQWdCO0FBQ2xCOztBQUVBO0VBQ0UsZ0JBQWdCO0FBQ2xCIiwiZmlsZSI6InNyYy9hcHAvdmFyL3Zhci5jb21wb25lbnQuY3NzIiwic291cmNlc0NvbnRlbnQiOlsiLmdyZXkge1xuICBiYWNrZ3JvdW5kOiAjZWJlYmViO1xufVxuXG4uYWN0aXZlIHtcbiAgYmFja2dyb3VuZDogIzZkOWNmZjtcbn1cblxuLm1hdC1jb2x1bW4tZHJhZyB7XG4gIGZsZXg6ICAwIDAgNDBweDtcbn1cblxuLm1hdC1jb2x1bW4tc2VsZWN0IHtcbiAgZmxleDogIDAgMCA0MHB4O1xufVxuXG4ubWF0LWNvbHVtbi1jb250cm9sIHtcbiAgZmxleDogIDAgMCAzMHB4O1xufVxuXG4ubWF0LWNvbHVtbi1pZCB7XG4gIGZsZXg6ICAwIDAgMTAwcHg7XG59XG5cbi5tYXQtY29sdW1uLW5hbWUge1xuICBmbGV4OiAgMCAwIDEwMHB4O1xufVxuXG4ubWF0LWNvbHVtbi1sYWJsIHtcbiAgZmxleDogIDAgMCA1MDBweDtcbn1cbiJdfQ== */"

/***/ }),

/***/ "./src/app/var/var.component.html":
/*!****************************************!*\
  !*** ./src/app/var/var.component.html ***!
  \****************************************/
/*! no static exports found */
/***/ (function(module, exports) {

module.exports = "<mat-grid-list class=\"table-controls\" cols=\"3\" rowHeight=\"58\">\n  <mat-grid-tile>\n    <mat-form-field>\n      <input matInput [formControl]=\"searchFilter\" placeholder=\"Search\" />\n      <span matSuffix><mat-icon>search</mat-icon></span>\n    </mat-form-field>\n  </mat-grid-tile>\n  <mat-divider [vertical]=\"true\"></mat-divider>\n  <mat-grid-tile>\n    <mat-select\n      color=\"primary\"\n      (change)=\"addToGroup($event.value)\"\n      placeholder=\"Add Selected to Group\"\n      #group_select\n      disabled=\"true\"\n      [hidden]=\"group_select.hidden\"\n    >\n      <mat-option *ngFor=\"let g of _variable_groups\" [value]=\"g.varGrp['@ID']\">\n        {{ g.varGrp.labl }}\n      </mat-option>\n    </mat-select>\n  </mat-grid-tile>\n</mat-grid-list>\n\n<mat-table class=\"mat-elevation-z8\" [dataSource]=\"datasource\" matSort>\n  <!-- drag Icon Column -->\n  <ng-container matColumnDef=\"drag\">\n    <mat-header-cell *matHeaderCellDef mat-sort-header> </mat-header-cell>\n    <mat-cell *matCellDef=\"let row\">\n      <mat-icon aria-label=\"drag\" matTooltip=\"Drag Me\">drag_indicator</mat-icon>\n    </mat-cell>\n  </ng-container>\n\n  <!-- Checkbox Column -->\n  <ng-container matColumnDef=\"select\">\n    <mat-header-cell *matHeaderCellDef>\n      <mat-checkbox\n        (change)=\"$event ? masterToggle() : null\"\n        [checked]=\"selection.hasValue() && isAllSelected()\"\n        [indeterminate]=\"selection.hasValue() && !isAllSelected()\"\n      >\n      </mat-checkbox>\n    </mat-header-cell>\n    <mat-cell *matCellDef=\"let row\">\n      <mat-checkbox\n        (click)=\"$event.stopPropagation()\"\n        (change)=\"$event ? selection.toggle(row) : null; checkSelection()\"\n        [checked]=\"selection.isSelected(row)\"\n      >\n      </mat-checkbox>\n    </mat-cell>\n  </ng-container>\n  <!-- MINUS PLUS Column -->\n\n  <ng-container matColumnDef=\"control\">\n    <mat-header-cell *matHeaderCellDef\n      ><div style=\"width:40px;\"></div\n    ></mat-header-cell>\n    <mat-cell *matCellDef=\"let row; let i = index\">\n      <button\n        *ngIf=\"row._show\"\n        mat-icon-button\n        color=\"accent\"\n        (click)=\"onRemove(row['@ID'])\"\n      >\n        <mat-icon aria-label=\"remove\">indeterminate_check_box</mat-icon>\n      </button>\n      <button\n        *ngIf=\"!row._show\"\n        mat-icon-button\n        color=\"accent\"\n        (click)=\"onAdd(row['@ID'])\"\n      >\n        <mat-icon aria-label=\"add\">add_box</mat-icon>\n      </button>\n    </mat-cell>\n  </ng-container>\n\n  <!-- ID Column -->\n  <ng-container matColumnDef=\"id\">\n    <mat-header-cell *matHeaderCellDef mat-sort-header i18n> ID </mat-header-cell>\n    <mat-cell *matCellDef=\"let row\">\n      <mat-chip-list>\n        <mat-chip\n          [color]=\"row['_in_group'] == true ? 'accent' : 'primary'\"\n          selected=\"true\"\n        >\n          {{ row['id'] }}\n        </mat-chip></mat-chip-list\n      >\n    </mat-cell>\n  </ng-container>\n\n  <ng-container matColumnDef=\"_order\">\n    <mat-header-cell *matHeaderCellDef mat-sort-header> order </mat-header-cell>\n    <mat-cell *matCellDef=\"let row\"> {{ row['_order'] }} </mat-cell>\n  </ng-container>\n\n  <ng-container matColumnDef=\"name\">\n    <mat-header-cell *matHeaderCellDef mat-sort-header i18n> Name </mat-header-cell>\n    <mat-cell *matCellDef=\"let row\"> {{ row['@name'] }} </mat-cell>\n  </ng-container>\n  <ng-container matColumnDef=\"labl\">\n    <mat-header-cell *matHeaderCellDef mat-sort-header i18n> Label </mat-header-cell>\n    <mat-cell *matCellDef=\"let row\"> {{ row.labl['#text'] }} </mat-cell>\n  </ng-container>\n  <ng-container matColumnDef=\"catgry\">\n    <mat-header-cell *matHeaderCellDef mat-sort-header i18n>\n      Category\n    </mat-header-cell>\n    <!--\n      <mat-cell *matCellDef=\"let row\"> <span *ngIf=\"row.catgry\">{{row.catgry.length}}</span> </mat-cell>\n  --></ng-container>\n\n  <ng-container matColumnDef=\"wgt-var\">\n    <mat-header-cell *matHeaderCellDef mat-sort-header i18n>\n      Weight\n    </mat-header-cell>\n    <mat-cell *matCellDef=\"let row\">\n      <span *ngIf=\"row['@wgt-var']\">{{ row['@wgt-var'] }}</span></mat-cell\n    >\n  </ng-container>\n\n  <ng-container matColumnDef=\"view\">\n    <mat-header-cell *matHeaderCellDef i18n>View</mat-header-cell>\n\n    <mat-cell *matCellDef=\"let row; let i = index\">\n      <button\n        mat-icon-button\n        color=\"accent\"\n        (click)=\"onView(row['@ID'])\"\n        i18n-matTooltip\n        matTooltip=\"View\"\n      >\n        <mat-icon i18n-aria-label aria-label=\"View\">visibility</mat-icon>\n      </button>\n    </mat-cell>\n  </ng-container>\n\n  <ng-container matColumnDef=\"action\">\n    <mat-header-cell *matHeaderCellDef>\n      <button\n        mat-icon-button\n        color=\"accent\"\n        (click)=\"onEditSelected()\"\n        i18n-matTooltip\n        matTooltip=\"Group Edit\"\n        #group_edit\n        disabled=\"true\"\n      >\n        <mat-icon i18n-aria-label aria-label=\"Group Edit\">edit</mat-icon>\n      </button>\n    </mat-header-cell>\n\n    <mat-cell *matCellDef=\"let row; let i = index\">\n      <button\n        mat-icon-button\n        color=\"accent\"\n        (click)=\"onEdit(row['@ID'])\"\n        i18n-matTooltip\n        matTooltip=\"Edit\"\n      >\n        <mat-icon i18n-aria-label aria-label=\"Edit\">edit</mat-icon>\n      </button>\n    </mat-cell>\n  </ng-container>\n\n  <mat-header-row *matHeaderRowDef=\"getDisplayedColumns()\"></mat-header-row>\n  <mat-row\n    *matRowDef=\"let row; columns: getDisplayedColumns()\"\n    draggable=\"true\"\n    (dragstart)=\"dragstart($event); highlightRow(row); trackDragRow(row)\"\n    (dragend)=\"dragend($event)\"\n    (dragenter)=\"dragenter($event, row)\"\n    [ngClass]=\"{ grey: row._show != true, active: row._active == true }\"\n  >\n  </mat-row>\n</mat-table>\n<mat-paginator [pageSizeOptions]=\"[10, 25, 50]\"></mat-paginator>\n"

/***/ }),

/***/ "./src/app/var/var.component.ts":
/*!**************************************!*\
  !*** ./src/app/var/var.component.ts ***!
  \**************************************/
/*! exports provided: VarComponent */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
__webpack_require__.r(__webpack_exports__);
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "VarComponent", function() { return VarComponent; });
/* harmony import */ var tslib__WEBPACK_IMPORTED_MODULE_0__ = __webpack_require__(/*! tslib */ "./node_modules/tslib/tslib.es6.js");
/* harmony import */ var _angular_core__WEBPACK_IMPORTED_MODULE_1__ = __webpack_require__(/*! @angular/core */ "./node_modules/@angular/core/fesm5/core.js");
/* harmony import */ var _angular_material__WEBPACK_IMPORTED_MODULE_2__ = __webpack_require__(/*! @angular/material */ "./node_modules/@angular/material/esm5/material.es5.js");
/* harmony import */ var _var_dialog_var_dialog_component__WEBPACK_IMPORTED_MODULE_3__ = __webpack_require__(/*! ../var-dialog/var-dialog.component */ "./src/app/var-dialog/var-dialog.component.ts");
/* harmony import */ var _var_stat_dialog_var_stat_dialog_component__WEBPACK_IMPORTED_MODULE_4__ = __webpack_require__(/*! ../var-stat-dialog/var-stat-dialog.component */ "./src/app/var-stat-dialog/var-stat-dialog.component.ts");
/* harmony import */ var _angular_forms__WEBPACK_IMPORTED_MODULE_5__ = __webpack_require__(/*! @angular/forms */ "./node_modules/@angular/forms/fesm5/forms.js");
/* harmony import */ var _angular_cdk_collections__WEBPACK_IMPORTED_MODULE_6__ = __webpack_require__(/*! @angular/cdk/collections */ "./node_modules/@angular/cdk/esm5/collections.es5.js");







var VarComponent = /** @class */ (function () {
    function VarComponent(dialog, ref, snackBar) {
        this.dialog = dialog;
        this.ref = ref;
        this.snackBar = snackBar;
        this.searchFilter = new _angular_forms__WEBPACK_IMPORTED_MODULE_5__["FormControl"]();
        this.filterValues = { search: '', _show: true };
        this.mode = 'all';
        this.dragged_over_dir = 'before';
        //
        this._variable_groups_vars = [];
        this.selection = new _angular_cdk_collections__WEBPACK_IMPORTED_MODULE_6__["SelectionModel"](true, []);
    }
    VarComponent.prototype.getDisplayedColumns = function () {
        var displayedColumns = []; // 'order_arrows'
        if (this.mode === 'all') {
            displayedColumns = [
                'drag',
                'select',
                'id',
                'name',
                'labl',
                'wgt-var',
                'view',
                'action'
            ];
        }
        else {
            displayedColumns = [
                'drag',
                'control',
                'id',
                'name',
                'labl',
                'wgt-var',
                'view',
                'action'
            ];
        }
        return displayedColumns;
    };
    /** Whether the number of selected elements matches the total number of rows. */
    VarComponent.prototype.isAllSelected = function () {
        var numSelected = this.selection.selected.length;
        var numRows = this.datasource.data.length;
        return numSelected === numRows;
    };
    /** Selects all rows if they are not all selected; otherwise clear selection. */
    VarComponent.prototype.masterToggle = function () {
        if (this.isAllSelected()) {
            this.selection.clear();
        }
        else {
            for (var i = 0; i < this.datasource.data.length; i++) {
                if (this.datasource.data[i]._show === true) {
                    this.selection.select(this.datasource.data[i]);
                }
            }
        }
        this.checkSelection();
    };
    VarComponent.prototype.ngOnInit = function () {
        var _this = this;
        this.searchFilter.valueChanges.subscribe(function (value) {
            _this.filterValues['search'] = value;
            _this.datasource.filter = JSON.stringify(_this.filterValues);
        });
        this.group_select['hidden'] = true;
    };
    // Entry point - when data has been loaded
    VarComponent.prototype.onUpdateVars = function (data) {
        this._variables = data;
        console.log(this._variables);
        // make sure all the data is set to show
        for (var i = 0; i < this._variables.length; i++) {
            this._variables[i]._show = true;
            // also make sure it has a label
            if (typeof this._variables[i].labl === 'undefined') {
                this._variables[i].labl = { '#text': '' };
            }
        }
        // show if var is _in_group
        this.updateGroupsVars();
        this.datasource = new _angular_material__WEBPACK_IMPORTED_MODULE_2__["MatTableDataSource"](this._variables);
        this.datasource.sort = this.sort;
        this.datasource.paginator = this.paginator;
        // sorting
        this.datasource.sortingDataAccessor = function (datasort, property) {
            switch (property) {
                case 'id':
                    return +datasort['@ID'].replace(/\D/g, '');
                case 'name':
                    return datasort['@name'];
                case 'labl':
                    return datasort.labl['#text'];
                case '_order':
                    return datasort._order;
                default:
                    return '';
            }
        };
        // filter
        this.datasource.filterPredicate = this.createFilter();
    };
    VarComponent.prototype.createFilter = function () {
        var filterFunction = function (data, filter) {
            var searchTerms = JSON.parse(filter);
            try {
                return (data['@ID']
                    .toString()
                    .toLowerCase()
                    .indexOf(searchTerms.search.toLowerCase()) !== -1 ||
                    data['@name']
                        .toString()
                        .toLowerCase()
                        .indexOf(searchTerms.search.toLowerCase()) !== -1 ||
                    data['labl']['#text']
                        .toString()
                        .toLowerCase()
                        .indexOf(searchTerms.search.toLowerCase()) !== -1);
            }
            catch (e) {
                return false;
            }
        };
        return filterFunction;
    };
    VarComponent.prototype.onEdit = function (_id) {
        this.id = _id;
        // get the data
        this.openDialog([this.getObjByID(_id, this._variables)]);
    };
    VarComponent.prototype.onSubset = function (_ids) {
        if (_ids == null) {
            this.mode = 'all';
        }
        else {
            this.mode = 'group';
        }
        var data = [];
        var ungrouped_count = 0;
        var obj;
        for (var i = 0; i < this._variables.length; i++) {
            obj = this._variables[i];
            if (this.mode === 'group') {
                if (_ids.indexOf(obj['@ID']) !== -1) {
                    obj._order = _ids.indexOf(obj['@ID']);
                    obj._show = true;
                }
                else {
                    ungrouped_count += 1;
                    obj._order = 99999 + ungrouped_count;
                    obj._show = false;
                }
            }
            else if (this.mode === 'all') {
                obj._order = null;
                obj._show = true;
            }
        }
        obj._active = false;
        this.filterValues['_show'] = true;
        this.datasource.filter = JSON.stringify(this.filterValues);
        // Showing all
        this.checkSelection(); // and enable group dropdown if applicable
        if (this.mode === 'group') {
            this.sortByOrder();
        }
        else {
            this.sort.sort({ id: '', start: 'asc', disableClear: false });
        }
    };
    // when a single row has been updated
    VarComponent.prototype.onUpdateVar = function () {
        this.removeWeightedFreq();
        this.ref.detectChanges();
    };
    VarComponent.prototype.removeWeightedFreq = function () {
        console.log('Start removeWeightedFreq');
        var weights = this.getWeights();
        var weights_set = new Set(weights);
        console.log(weights);
        console.log(this._variables);
        for (var i = 0; i < this._variables.length; i++) {
            console.log('wgt-var:' + this._variables[i]['@wgt-var']);
            if (typeof this._variables[i]['@wgt-var'] !== 'undefined') {
                if (this._variables[i]['@wgt-var'] !== '') {
                    if (!weights_set.has(this._variables[i]['@wgt-var'])) {
                        this._variables[i]['@wgt-var'] = '';
                        for (var k = 0; k < this._variables[i].catgry.length; k++) {
                            this._variables[i].catgry[k].catStat.splice(1, 1);
                        }
                    }
                }
            }
        }
        console.log('End removeWeightedFreq');
    };
    // get the var
    VarComponent.prototype.getObjByID = function (_id, _data) {
        for (var _i = 0, _data_1 = _data; _i < _data_1.length; _i++) {
            var i = _data_1[_i];
            var obj = i;
            if (obj['@ID'] === _id) {
                return obj;
            }
        }
    };
    // get the group
    VarComponent.prototype.getObjByIDNested = function (_id, _data) {
        for (var i = 0; i < _data.length; i++) {
            var obj = _data[i];
            if (obj.varGrp['@ID'] === _id) {
                return obj;
            }
        }
    };
    VarComponent.prototype.getWeights = function () {
        var weights = [];
        console.log('Get weights ' + this._variables.length);
        for (var i = 0; i < this._variables.length; i++) {
            if (this._variables[i]['@wgt'] === 'wgt') {
                weights.push(this._variables[i]['@ID']);
                console.log(this._variables[i]['@ID']);
            }
        }
        console.log('End get weights');
        return weights;
    };
    VarComponent.prototype.openDialog = function (data) {
        var _this = this;
        this.dialogRef = this.dialog.open(_var_dialog_var_dialog_component__WEBPACK_IMPORTED_MODULE_3__["VarDialogComponent"], {
            width: '550px',
            data: data,
            panelClass: 'field_width'
        });
        console.log('Open dialog');
        var weights = this.getWeights();
        console.log('w ' + weights.length);
        this.dialogRef.componentInstance.weights = weights;
        this.dialogRef.componentInstance._variable_groups = this._variable_groups;
        var sub = this.dialogRef.componentInstance.parentUpdateVar.subscribe(function () {
            _this.onUpdateVar();
        });
    };
    VarComponent.prototype.checkSelection = function () {
        // when are in all view mode
        if (this.mode === 'all') {
            if (this.selection.selected.length > 0) {
                this.group_select['disabled'] = 'false';
                this.group_select['hidden'] = false;
            }
            else {
                this.group_select['disabled'] = 'true';
                this.group_select['hidden'] = true;
            }
        }
        if (this.selection.selected.length > 1) {
            this.group_edit['disabled'] = 'false';
        }
        else {
            this.group_edit['disabled'] = 'true';
        }
    };
    VarComponent.prototype.addToGroup = function (_id) {
        // first get the group
        var obj = this.getObjByIDNested(_id, this._variable_groups);
        var vars = obj.varGrp['@var'].split(' ');
        for (var _i = 0, _a = this.selection.selected; _i < _a.length; _i++) {
            var i = _a[_i];
            var selected = i;
            if (vars.indexOf(selected['@ID']) === -1) {
                vars.push(selected['@ID']);
            }
        }
        // reset vars to new selection
        obj.varGrp['@var'] = vars.join(' ');
        // reset the dropdown
        this.group_select['value'] = 0;
        //
        this.updateGroupsVars();
    };
    VarComponent.prototype.selectGroup = function (_id) {
        this.group_select['value'] = _id;
        this.selection.clear();
    };
    VarComponent.prototype.disableSelectGroup = function () {
        this.group_select['value'] = 0;
    };
    VarComponent.prototype.updateGroupsVars = function () {
        this.getVariableGroupsVars();
        for (var i = 0; i < this._variables.length; i++) {
            if (this._variable_groups_vars.indexOf(this._variables[i]['@ID']) > -1) {
                this._variables[i]._in_group = true;
            }
            else {
                this._variables[i]._in_group = false;
            }
        }
    };
    VarComponent.prototype.getVariableGroupsVars = function () {
        this._variable_groups_vars = [];
        // loop though all the variables in the varaible groups and create a complete list
        for (var i = 0; i < this._variable_groups.length; i++) {
            var obj = this._variable_groups[i];
            var vars = obj.varGrp['@var'].split(' ');
            for (var j = 0; j < vars.length; j++) {
                if (this._variable_groups_vars.indexOf(vars[j]) === -1) {
                    this._variable_groups_vars.push(vars[j]);
                }
            }
        }
    };
    VarComponent.prototype.sortByOrder = function () {
        this.sort.sort({ id: '', start: 'asc', disableClear: false });
        this.sort.sort({ id: '_order', start: 'asc', disableClear: false });
    };
    VarComponent.prototype.dragstart = function ($event) {
        this.source = $event.currentTarget;
        $event.dataTransfer.effectAllowed = 'move';
    };
    VarComponent.prototype.trackDragRow = function (_row) {
        this.dragged_obj = _row;
    };
    //
    VarComponent.prototype.dragenter = function ($event, _row) {
        var target = $event.currentTarget;
        // let new_dragged_over_obj=false;
        if (_row === this.dragged_obj) {
            return;
        }
        this.dragged_over_obj = _row; // keep track of the dragged over obj to later update the list
        //
        if (this.isbefore(this.source, target)) {
            target.parentNode.insertBefore(this.source, target); // insert before
            this.dragged_over_dir = 'before';
        }
        else {
            // note that "after" triggers once per new_dragged_over_obj and thus must be stored to ensure proper placement of dragged object
            target.parentNode.insertBefore(this.source, target.nextSibling); // insert after
            this.dragged_over_dir = 'after';
        }
    };
    VarComponent.prototype.isbefore = function (a, b) {
        if (a.parentNode === b.parentNode) {
            for (var cur = a; cur; cur = cur.previousSibling) {
                if (cur === b) {
                    return true;
                }
            }
        }
        return false;
    };
    VarComponent.prototype.highlightRow = function (_row) {
        _row._active = true;
    };
    VarComponent.prototype.dragend = function ($event) {
        this.dragged_obj._active = false; // remove the highlight
        var id = this.dragged_obj['@ID'];
        if (this.dragged_group) {
            // add the dragged var to the dragged group
            var objgr = this.getObjByIDNested(this.dragged_group, this._variable_groups);
            var vars = objgr.varGrp['@var'].split(' ');
            vars.push(id);
            objgr.varGrp['@var'] = vars.join(' ');
            // if we are currently looking at the group which has been dragged to update it
            if (this.group_select['value'] === this.dragged_group) {
                this.updateGroupsVars();
                this.onSubset(vars);
            }
            delete this.dragged_group;
            return;
        }
        // take the last dragged over item and place the dragged item either before or after
        var obj = this.updateGroupVars('drag', id);
        this.showMSG('Changed the position of ' + id);
    };
    VarComponent.prototype.onAdd = function (_id) {
        var obj = this.updateGroupVars('add', _id);
        this.showMSG('Added ' + _id + ' to ' + obj.varGrp.labl);
    };
    VarComponent.prototype.onRemove = function (_id) {
        var obj = this.updateGroupVars('remove', _id);
        this.showMSG('Removed ' + _id + ' from ' + obj.varGrp.labl);
    };
    VarComponent.prototype.updateGroupVars = function (_type, _id) {
        var obj = this.getObjByIDNested(this.group_select['value'], this._variable_groups);
        var vars = obj.varGrp['@var'].split(' ');
        //
        if (_type === 'remove') {
            vars.splice(vars.indexOf(_id), 1); // remove from array
        }
        else if (_type === 'add') {
            vars.push(_id); // add to end of array
        }
        else if (_type === 'drag') {
            // check if this.dragged_over_obj is not part of the group
            // check to see if this var is part of the group -- otherwise add it.
            if (vars.indexOf(_id) > -1) {
                vars.splice(vars.indexOf(_id), 1); // remove from array
            }
            // find out the position of the dragged_over_obj
            var index = vars.indexOf(this.dragged_over_obj['@ID']);
            if (this.dragged_over_dir === 'before' && this.dragged_over_obj._show) {
                // case 1. add before
                vars.splice(index, 0, _id);
            }
            else if (this.dragged_over_dir === 'after' &&
                this.dragged_over_obj._show) {
                // case 2. add after
                vars.splice(index + 1, 0, _id);
            }
        }
        //
        obj.varGrp['@var'] = vars.join(' ');
        // reset the table
        this.updateGroupsVars();
        this.onSubset(vars);
        return obj;
    };
    VarComponent.prototype.onEditSelected = function () {
        var selected_objs = [];
        // show the popup but only allow certain fields be be updated
        for (var _i = 0, _a = this.selection.selected; _i < _a.length; _i++) {
            var i = _a[_i];
            var selected = i;
            selected_objs.push(selected);
        }
        this.openDialog(selected_objs);
    };
    VarComponent.prototype.draggedGroup = function (_id) {
        this.dragged_group = _id;
    };
    VarComponent.prototype.onView = function (_id) {
        var data = this.getObjByID(_id, this._variables);
        // open a dialog showing the variables
        this.dialogStatRef = this.dialog.open(_var_stat_dialog_var_stat_dialog_component__WEBPACK_IMPORTED_MODULE_4__["VarStatDialogComponent"], {
            width: '550px',
            data: data,
            panelClass: 'field_width'
        });
    };
    VarComponent.prototype.showMSG = function (_msg) {
        this.snackBar.open(_msg, '', {
            duration: 1000
        });
    };
    tslib__WEBPACK_IMPORTED_MODULE_0__["__decorate"]([
        Object(_angular_core__WEBPACK_IMPORTED_MODULE_1__["Input"])(),
        tslib__WEBPACK_IMPORTED_MODULE_0__["__metadata"]("design:type", Object)
    ], VarComponent.prototype, "_variable_groups", void 0);
    tslib__WEBPACK_IMPORTED_MODULE_0__["__decorate"]([
        Object(_angular_core__WEBPACK_IMPORTED_MODULE_1__["ViewChild"])('group_select'),
        tslib__WEBPACK_IMPORTED_MODULE_0__["__metadata"]("design:type", _angular_core__WEBPACK_IMPORTED_MODULE_1__["ElementRef"])
    ], VarComponent.prototype, "group_select", void 0);
    tslib__WEBPACK_IMPORTED_MODULE_0__["__decorate"]([
        Object(_angular_core__WEBPACK_IMPORTED_MODULE_1__["ViewChild"])('group_edit'),
        tslib__WEBPACK_IMPORTED_MODULE_0__["__metadata"]("design:type", _angular_core__WEBPACK_IMPORTED_MODULE_1__["ElementRef"])
    ], VarComponent.prototype, "group_edit", void 0);
    tslib__WEBPACK_IMPORTED_MODULE_0__["__decorate"]([
        Object(_angular_core__WEBPACK_IMPORTED_MODULE_1__["ViewChild"])(_angular_material__WEBPACK_IMPORTED_MODULE_2__["MatSort"]),
        tslib__WEBPACK_IMPORTED_MODULE_0__["__metadata"]("design:type", _angular_material__WEBPACK_IMPORTED_MODULE_2__["MatSort"])
    ], VarComponent.prototype, "sort", void 0);
    tslib__WEBPACK_IMPORTED_MODULE_0__["__decorate"]([
        Object(_angular_core__WEBPACK_IMPORTED_MODULE_1__["ViewChild"])(_angular_material__WEBPACK_IMPORTED_MODULE_2__["MatPaginator"]),
        tslib__WEBPACK_IMPORTED_MODULE_0__["__metadata"]("design:type", _angular_material__WEBPACK_IMPORTED_MODULE_2__["MatPaginator"])
    ], VarComponent.prototype, "paginator", void 0);
    VarComponent = tslib__WEBPACK_IMPORTED_MODULE_0__["__decorate"]([
        Object(_angular_core__WEBPACK_IMPORTED_MODULE_1__["Component"])({
            selector: 'app-var',
            template: __webpack_require__(/*! ./var.component.html */ "./src/app/var/var.component.html"),
            styles: [__webpack_require__(/*! ./var.component.css */ "./src/app/var/var.component.css")]
        }),
        tslib__WEBPACK_IMPORTED_MODULE_0__["__metadata"]("design:paramtypes", [_angular_material__WEBPACK_IMPORTED_MODULE_2__["MatDialog"],
            _angular_core__WEBPACK_IMPORTED_MODULE_1__["ChangeDetectorRef"],
            _angular_material__WEBPACK_IMPORTED_MODULE_2__["MatSnackBar"]])
    ], VarComponent);
    return VarComponent;
}());



/***/ }),

/***/ "./src/assets/js/xml2json.js":
/*!***********************************!*\
  !*** ./src/assets/js/xml2json.js ***!
  \***********************************/
/*! exports provided: xml2json */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
__webpack_require__.r(__webpack_exports__);
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "xml2json", function() { return xml2json; });
/*	This work is licensed under Creative Commons GNU LGPL License.

	License: http://creativecommons.org/licenses/LGPL/2.1/
   Version: 0.9
	Author:  Stefan Goessner/2006
	Web:     http://goessner.net/
*/
function xml2json(xml, tab) {
   var X = {
      toObj: function(xml) {
         var o = {};
         if (xml.nodeType==1) {   // element node ..
            if (xml.attributes.length)   // element with attributes  ..
               for (var i=0; i<xml.attributes.length; i++)
                  o["@"+xml.attributes[i].nodeName] = (xml.attributes[i].nodeValue||"").toString();
            if (xml.firstChild) { // element has child nodes ..
               var textChild=0, cdataChild=0, hasElementChild=false;
               for (var n=xml.firstChild; n; n=n.nextSibling) {
                  if (n.nodeType==1) hasElementChild = true;
                  else if (n.nodeType==3 && n.nodeValue.match(/[^ \f\n\r\t\v]/)) textChild++; // non-whitespace text
                  else if (n.nodeType==4) cdataChild++; // cdata section node
               }
               if (hasElementChild) {
                  if (textChild < 2 && cdataChild < 2) { // structured element with evtl. a single text or/and cdata node ..
                     X.removeWhite(xml);
                     for (var n=xml.firstChild; n; n=n.nextSibling) {
                        if (n.nodeType == 3)  // text node
                           o["#text"] = X.escape(n.nodeValue);
                        else if (n.nodeType == 4)  // cdata node
                           o["#cdata"] = X.escape(n.nodeValue);
                        else if (o[n.nodeName]) {  // multiple occurence of element ..
                           if (o[n.nodeName] instanceof Array)
                              o[n.nodeName][o[n.nodeName].length] = X.toObj(n);
                           else
                              o[n.nodeName] = [o[n.nodeName], X.toObj(n)];
                        }
                        else  // first occurence of element..
                           o[n.nodeName] = X.toObj(n);
                     }
                  }
                  else { // mixed content
                     if (!xml.attributes.length)
                        o = X.escape(X.innerXml(xml));
                     else
                        o["#text"] = X.escape(X.innerXml(xml));
                  }
               }
               else if (textChild) { // pure text
                  if (!xml.attributes.length)
                     o = X.escape(X.innerXml(xml));
                  else
                     o["#text"] = X.escape(X.innerXml(xml));
               }
               else if (cdataChild) { // cdata
                  if (cdataChild > 1)
                     o = X.escape(X.innerXml(xml));
                  else
                     for (var n=xml.firstChild; n; n=n.nextSibling)
                        o["#cdata"] = X.escape(n.nodeValue);
               }
            }
            if (!xml.attributes.length && !xml.firstChild) o = null;
         }
         else if (xml.nodeType==9) { // document.node
            o = X.toObj(xml.documentElement);
         }
         else
            alert("unhandled node type: " + xml.nodeType);
         return o;
      },
      toJson: function(o, name, ind) {
         var json = name ? ("\""+name+"\"") : "";
         if (o instanceof Array) {
            for (var i=0,n=o.length; i<n; i++)
               o[i] = X.toJson(o[i], "", ind+"\t");
            json += (name?":[":"[") + (o.length > 1 ? ("\n"+ind+"\t"+o.join(",\n"+ind+"\t")+"\n"+ind) : o.join("")) + "]";
         }
         else if (o == null)
            json += (name&&":") + "null";
         else if (typeof(o) == "object") {
            var arr = [];
            for (var m in o)
               arr[arr.length] = X.toJson(o[m], m, ind+"\t");
            json += (name?":{":"{") + (arr.length > 1 ? ("\n"+ind+"\t"+arr.join(",\n"+ind+"\t")+"\n"+ind) : arr.join("")) + "}";
         }
         else if (typeof(o) == "string")
            json += (name&&":") + "\"" + o.toString() + "\"";
         else
            json += (name&&":") + o.toString();
         return json;
      },
      innerXml: function(node) {
         var s = ""
         if ("innerHTML" in node)
            s = node.innerHTML;
         else {
            var asXml = function(n) {
               var s = "";
               if (n.nodeType == 1) {
                  s += "<" + n.nodeName;
                  for (var i=0; i<n.attributes.length;i++)
                     s += " " + n.attributes[i].nodeName + "=\"" + (n.attributes[i].nodeValue||"").toString() + "\"";
                  if (n.firstChild) {
                     s += ">";
                     for (var c=n.firstChild; c; c=c.nextSibling)
                        s += asXml(c);
                     s += "</"+n.nodeName+">";
                  }
                  else
                     s += "/>";
               }
               else if (n.nodeType == 3)
                  s += n.nodeValue;
               else if (n.nodeType == 4)
                  s += "<![CDATA[" + n.nodeValue + "]]>";
               return s;
            };
            for (var c=node.firstChild; c; c=c.nextSibling)
               s += asXml(c);
         }
         return s;
      },
      escape: function(txt) {

        return txt.replace(/^\s+|\s+$/g, '')
                   .replace(/[\\]/g, "\\\\")
                   .replace(/[\"]/g, '\\"')
                   .replace(/[\n]/g, '\\n')
                   .replace(/[\r]/g, '\\r')

      },
      removeWhite: function(e) {
         e.normalize();
         for (var n = e.firstChild; n; ) {
            if (n.nodeType == 3) {  // text node
               if (!n.nodeValue.match(/[^ \f\n\r\t\v]/)) { // pure whitespace text node
                  var nxt = n.nextSibling;
                  e.removeChild(n);
                  n = nxt;
               }
               else
                  n = n.nextSibling;
            }
            else if (n.nodeType == 1) {  // element node
               X.removeWhite(n);
               n = n.nextSibling;
            }
            else                      // any other node
               n = n.nextSibling;
         }
         return e;
      }
   };
   if (xml.nodeType == 9) // document node
      xml = xml.documentElement;
   var json = X.toJson(X.toObj(X.removeWhite(xml)), xml.nodeName, "\t");
   return "{\n" + tab + (tab ? json.replace(/\t/g, tab) : json.replace(/\t|\n/g, "")) + "\n}";
}


/***/ }),

/***/ "./src/environments/environment.ts":
/*!*****************************************!*\
  !*** ./src/environments/environment.ts ***!
  \*****************************************/
/*! exports provided: environment */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
__webpack_require__.r(__webpack_exports__);
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "environment", function() { return environment; });
// This file can be replaced during build by using the `fileReplacements` array.
// `ng build --prod` replaces `environment.ts` with `environment.prod.ts`.
// The list of file replacements can be found in `angular.json`.
var environment = {
    production: false
};
/*
 * For easier debugging in development mode, you can import the following file
 * to ignore zone related error stack frames such as `zone.run`, `zoneDelegate.invokeTask`.
 *
 * This import should be commented out in production mode because it will have a negative impact
 * on performance if an error is thrown.
 */
// import 'zone.js/dist/zone-error';  // Included with Angular CLI.


/***/ }),

/***/ "./src/main.ts":
/*!*********************!*\
  !*** ./src/main.ts ***!
  \*********************/
/*! no exports provided */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
__webpack_require__.r(__webpack_exports__);
/* harmony import */ var _angular_core__WEBPACK_IMPORTED_MODULE_0__ = __webpack_require__(/*! @angular/core */ "./node_modules/@angular/core/fesm5/core.js");
/* harmony import */ var _angular_platform_browser_dynamic__WEBPACK_IMPORTED_MODULE_1__ = __webpack_require__(/*! @angular/platform-browser-dynamic */ "./node_modules/@angular/platform-browser-dynamic/fesm5/platform-browser-dynamic.js");
/* harmony import */ var _app_app_module__WEBPACK_IMPORTED_MODULE_2__ = __webpack_require__(/*! ./app/app.module */ "./src/app/app.module.ts");
/* harmony import */ var _environments_environment__WEBPACK_IMPORTED_MODULE_3__ = __webpack_require__(/*! ./environments/environment */ "./src/environments/environment.ts");
/* harmony import */ var hammerjs__WEBPACK_IMPORTED_MODULE_4__ = __webpack_require__(/*! hammerjs */ "./node_modules/hammerjs/hammer.js");
/* harmony import */ var hammerjs__WEBPACK_IMPORTED_MODULE_4___default = /*#__PURE__*/__webpack_require__.n(hammerjs__WEBPACK_IMPORTED_MODULE_4__);





if (_environments_environment__WEBPACK_IMPORTED_MODULE_3__["environment"].production) {
    Object(_angular_core__WEBPACK_IMPORTED_MODULE_0__["enableProdMode"])();
}
Object(_angular_platform_browser_dynamic__WEBPACK_IMPORTED_MODULE_1__["platformBrowserDynamic"])().bootstrapModule(_app_app_module__WEBPACK_IMPORTED_MODULE_2__["AppModule"])
    .catch(function (err) { return console.error(err); });


/***/ }),

/***/ 0:
/*!***************************!*\
  !*** multi ./src/main.ts ***!
  \***************************/
/*! no static exports found */
/***/ (function(module, exports, __webpack_require__) {

module.exports = __webpack_require__(/*! /home/lubitchv/work/Dataverse-Data-Curation-Tool/data-curation-tool/src/main.ts */"./src/main.ts");


/***/ })

},[[0,"runtime","vendor"]]]);
//# sourceMappingURL=main.js.map