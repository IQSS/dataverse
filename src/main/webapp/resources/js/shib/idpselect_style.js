/* 
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

$(document).ready(function() {
    $('#idpSelectIdPListTile form, #idpSelectIdPEntryTile form').addClass("form-inline");
    
    $('#idpSelectIdPListTile > div.IdPSelectTextDiv, #idpSelectIdPEntryTile > div.IdPSelectTextDiv').addClass("help-block");

    $('#idpSelectSelector, #idpSelectInput').addClass("form-control");

    $('#idpSelectListButton, #idpSelectSelectButton').addClass("btn btn-default");
    
    $('a.IdPSelectHelpButton').addClass('pull-right');
    
    //$('ul.IdPSelectDropDown').addClass('dropdown-menu');
});
