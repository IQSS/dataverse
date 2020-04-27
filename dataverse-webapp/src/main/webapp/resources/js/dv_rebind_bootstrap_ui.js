
/**
 * Overrides default javascript handling for some of primefaces components.
 * When upgrading primefaces version be sure to check this function as
 * it depends heavily on primefaces internal code.
 */
function reinitializePrimefacesComponentsJS() {
    if (PrimeFaces.widget.SelectCheckboxMenu) {
        var originalSelectCheckboxMenuBindKeyEvents = PrimeFaces.widget.SelectCheckboxMenu.prototype.bindKeyEvents;
        var originalSelectCheckboxMenuBindPanelKeyEvents = PrimeFaces.widget.SelectCheckboxMenu.prototype.bindPanelKeyEvents;
        var originalSelectCheckboxMenuRenderHeader = PrimeFaces.widget.SelectCheckboxMenu.prototype.renderHeader;
        var originalSelectCheckboxMenuHide = PrimeFaces.widget.SelectCheckboxMenu.prototype.hide;
        
        // Adds i18n handling on elements read by screen reader
        PrimeFaces.widget.SelectCheckboxMenu.prototype.renderHeader = function() {
            originalSelectCheckboxMenuRenderHeader.apply(this);
            
            if (this.toggler) {
                this.toggler.find('> .ui-helper-hidden-accessible > input').attr('aria-label', PrimeFaces.getLocaleSettings().selectAllSelectCheckboxMenu);
            }
            if (this.filterInput) {
                this.filterInput.attr('aria-label', PrimeFaces.getLocaleSettings().filterInputSelectCheckboxMenu);
            }
            if (this.closer) {
                this.closer.attr('aria-label', PrimeFaces.getLocaleSettings().closeText);
            }
        };
        
        // Rebinds original primefaces key events for proper handling of TAB key
        // in situations when "select all" option is disabled
        // Note that this is mostly copy-paste of original function
        // with slight changes (mentioned in comments)
        // see: forms.selectcheckboxmenu.js in primefaces repository
        PrimeFaces.widget.SelectCheckboxMenu.prototype.bindKeyEvents = function() {
            originalSelectCheckboxMenuBindKeyEvents.apply(this);
            
            var $this = this;
            this.keyboardTarget.off('keydown.selectCheckboxMenu'); // turn off default key handling
            
            this.keyboardTarget.on('keydown.selectCheckboxMenu', function(e) { // replace with our key handling
                var keyCode = $.ui.keyCode,
                key = e.which;

                if($this.cfg.dynamic && !$this.isDynamicLoaded) {
                    $this._renderPanel();
                }

                switch(key) {
                    case keyCode.ENTER:
                    case keyCode.SPACE:
                        if ($this.panel.is(":hidden"))
                            $this.show();
                        else
                            $this.hide(true);

                        e.preventDefault();
                    break;

                    case keyCode.DOWN:
                        if (e.altKey) {
                            if ($this.panel.is(":hidden"))
                                $this.show();
                            else
                                $this.hide(true);
                        }

                        e.preventDefault();
                    break;

                    case keyCode.TAB: // change: adjusted to support hidden "select all"
                        if($this.panel.is(':visible')) {
                            var selectAllInput = $this.toggler.find('> div.ui-helper-hidden-accessible > input:visible');
                            if ($this.cfg.showHeader && selectAllInput.length > 0) {
                                selectAllInput.trigger('focus');
                            } else if ($this.cfg.showHeader && $this.filterInput.length > 0) {
                                $this.filterInput.trigger('focus')
                            } else {
                                $this.itemContainer.children('li:not(.ui-state-disabled):first').find('div.ui-helper-hidden-accessible > input').trigger('focus');
                            }
                            e.preventDefault();
                        }

                    break;

                    case keyCode.ESCAPE:
                        $this.hide();
                    break;
                };
            });
        }
        
        // Added tab handling on panel
        // When pressing tab key from last tabbable element it moves focus to first tabbable element
        // When pressing alt+tab key from first tabbable element it moves focus to last tabbable element
        PrimeFaces.widget.SelectCheckboxMenu.prototype.bindPanelKeyEvents = function() {
            originalSelectCheckboxMenuBindPanelKeyEvents.apply(this);
            var $this = this;
            var focusableElementsString = 'a[href]:visible, input:not([disabled]):visible, select:not([disabled]):visible, textarea:not([disabled]):visible, button:not([disabled]):visible, [tabindex="0"]:visible';
            
            this.panel.on('keydown.selectCheckboxMenuLockTab', focusableElementsString, function(e) {
                var keyCode = $.ui.keyCode;
                var focusableElements = $this.panel.find(focusableElementsString);
                var key = e.which;

                if (key === keyCode.TAB) {
                    if (e.shiftKey && e.target === focusableElements.first().get(0)) {
                        e.preventDefault();
                        focusableElements.last().focus();
                    }
                    if (!e.shiftKey && e.target === focusableElements.last().get(0)) {
                        e.preventDefault();
                        focusableElements.first().focus();
                    }
                }
                
            });
        };
        
        // Handles focus change when closing panel
        PrimeFaces.widget.SelectCheckboxMenu.prototype.hide = function(animate) {
            originalSelectCheckboxMenuHide.apply(this, [animate]);
            this.keyboardTarget.focus();
        };
    }
    
    if (PrimeFaces.widget.FileUpload) {

        // Added update of progress for screen readers
        // Note that this is mostly copy-paste of original function
        PrimeFaces.widget.FileUpload.prototype.init = function(cfg) {
            // skip calling PrimeFaces.widget.FileUpload.init() on purpose
            PrimeFaces.widget.DeferredWidget.prototype.init.call(this, cfg);
            //this._super(cfg);
            
            if(this.cfg.disabled) {
                return;
            }

            this.ucfg = {};
            this.form = this.jq.closest('form');
            this.buttonBar = this.jq.children('.ui-fileupload-buttonbar');
            this.chooseButton = this.buttonBar.children('.ui-fileupload-choose');
            this.uploadButton = this.buttonBar.children('.ui-fileupload-upload');
            this.cancelButton = this.buttonBar.children('.ui-fileupload-cancel');
            this.content = this.jq.children('.ui-fileupload-content');
            this.filesTbody = this.content.find('> div.ui-fileupload-files > div');
            this.sizes = ['Bytes', 'KB', 'MB', 'GB', 'TB'];
            this.files = [];
            this.fileAddIndex = 0;
            this.cfg.invalidFileMessage = this.cfg.invalidFileMessage || 'Invalid file type';
            this.cfg.invalidSizeMessage = this.cfg.invalidSizeMessage || 'Invalid file size';
            this.cfg.fileLimitMessage = this.cfg.fileLimitMessage || 'Maximum number of files exceeded';
            this.cfg.messageTemplate = this.cfg.messageTemplate || '{name} {size}';
            this.cfg.previewWidth = this.cfg.previewWidth || 80;
            this.uploadedFileCount = 0;
            this.fileId = 0;

            this.renderMessages();

            this.bindEvents();

            var $this = this,
                postURL = this.form.attr('action'),
                encodedURLfield = this.form.children("input[name*='javax.faces.encodedURL']");

            //portlet support
            var porletFormsSelector = null;
            if(encodedURLfield.length > 0) {
                porletFormsSelector = 'form[action="' + postURL + '"]';
                postURL = encodedURLfield.val();
            }
            
            this.ucfg = {
                    url: postURL,
                    portletForms: porletFormsSelector,
                    paramName: this.id,
                    dataType: 'xml',
                    dropZone: (this.cfg.dnd === false) ? null : this.jq,
                    sequentialUploads: this.cfg.sequentialUploads,
                    formData: function() {
                        return $this.createPostData();
                    },
                    beforeSend: function(xhr, settings) {
                        xhr.setRequestHeader('Faces-Request', 'partial/ajax');
                        xhr.pfSettings = settings;
                        xhr.pfArgs = {}; // default should be an empty object
                    },
                    start: function(e) {
                        if($this.cfg.onstart) {
                            $this.cfg.onstart.call($this);
                        }
                    },
                    add: function(e, data) {
                        $this.chooseButton.removeClass('ui-state-hover ui-state-focus');

                        if($this.fileAddIndex === 0) {
                            $this.clearMessages();
                        }

                        if($this.cfg.fileLimit && ($this.uploadedFileCount + $this.files.length + 1) > $this.cfg.fileLimit) {
                            $this.clearMessages();
                            $this.showMessage({
                                summary: $this.cfg.fileLimitMessage
                            });

                            return;
                        }

                        var file = data.files ? data.files[0] : null;
                        if(file) {
                            var validMsg = $this.validate(file);

                            if(validMsg) {
                                $this.showMessage({
                                    summary: validMsg,
                                    filename: file.name,
                                    filesize: file.size
                                });

                                $this.postSelectFile(data);
                            }
                            else {
                                if($this.cfg.onAdd) {
                                    $this.cfg.onAdd.call($this, file, function(processedFile) {
                                        file = processedFile;
                                        data.files[0] = processedFile;
                                        $this.addFileToRow(file, data);
                                    });
                                }
                                else {
                                    $this.addFileToRow(file, data);
                                }
                            }
                        }
                    },
                    send: function(e, data) {
                        if(!window.FormData) {
                            for(var i = 0; i < data.files.length; i++) {
                                var file = data.files[i];
                                if(file.row) {
                                    file.row.children('.ui-fileupload-progress').find('> .ui-progressbar').attr('aria-valuenow', 100); // change: Added update of progress for screen readers
                                    file.row.children('.ui-fileupload-progress').find('> .ui-progressbar > .ui-progressbar-value')
                                            .addClass('ui-progressbar-value-legacy')
                                            .css({
                                                width: '100%',
                                                display: 'block'
                                            });
                                }
                            }
                        }
                    },
                    fail: function(e, data) {
                        if (data.errorThrown === 'abort') {
                            if ($this.cfg.oncancel) {
                                $this.cfg.oncancel.call($this);
                            }
                            return;
                        }
                        if($this.cfg.onerror) {
                            $this.cfg.onerror.call($this, data.jqXHR, data.textStatus, data.jqXHR.pfArgs);
                        }
                    },
                    progress: function(e, data) {
                        if(window.FormData) {
                            var progress = parseInt(data.loaded / data.total * 100, 10);

                            for(var i = 0; i < data.files.length; i++) {
                                var file = data.files[i];
                                if(file.row) {
                                    file.row.children('.ui-fileupload-progress').find('> .ui-progressbar').attr('aria-valuenow', progress); // change: Added update of progress for screen readers
                                    file.row.children('.ui-fileupload-progress').find('> .ui-progressbar > .ui-progressbar-value').css({
                                        width: progress + '%',
                                        display: 'block'
                                    });
                                }
                            }
                        }
                    },
                    done: function(e, data) {
                        $this.uploadedFileCount += data.files.length;
                        $this.removeFiles(data.files);

                        PrimeFaces.ajax.Response.handle(data.result, data.textStatus, data.jqXHR, null);
                    },
                    always: function(e, data) {
                        if($this.cfg.oncomplete) {
                            $this.cfg.oncomplete.call($this, data.jqXHR.pfArgs, data);
                        }
                    }
                };

                this.jq.fileupload(this.ucfg);
                this.input = $(this.jqId + '_input');
        };
        
        var originalFileUploadRenderMessages = PrimeFaces.widget.FileUpload.prototype.renderMessages;

        // Adds i18n support for elements read by screen reader
        PrimeFaces.widget.FileUpload.prototype.renderMessages = function() {
            originalFileUploadRenderMessages.apply(this);
            this.clearMessageLink.attr('aria-label', PrimeFaces.getLocaleSettings().closeText);
            this.clearMessageLink.attr('role', 'button');
        };
        
        var originalAddFileToRow = PrimeFaces.widget.FileUpload.prototype.addFileToRow;
        
        // Adds i18n support for elements read by screen reader
        PrimeFaces.widget.FileUpload.prototype.addFileToRow = function(file, data) {
            originalAddFileToRow.apply(this, [file, data]);
            
            this.files.forEach(function(file) {
                file.row.find('.ui-fileupload-cancel')
                    .attr('aria-label', PrimeFaces.getLocaleSettings().cancelFileUpload + PrimeFaces.escapeHTML(file.name));
            });
        };
    }
    if(PrimeFaces.widget.Message) {
        // copy of original primefaces messages java script handling with
        // fixed bug introduced in pf 8.0
        PrimeFaces.widget.Message = PrimeFaces.widget.BaseWidget.extend({
            
            init: function(cfg) {
                this._super(cfg);
                
                var text = this.jq.children('div').children('.ui-message-error-detail').text(); // change: error detail is enclosed with div tag
                
                if(text) {
                   var target = $(PrimeFaces.escapeClientId(this.cfg.target));
                   
                   if (this.cfg.tooltip) {
                      target.data('tooltip', text);
                   }
                   
                   target.attr('aria-describedby', this.id + '_error-detail');
                } 
           }
        });
    }
    
    if(PrimeFaces.widget.Dialog) {
        // Dialog Listener For Calling handleResizeDialog
        var originalDialogPostShow = PrimeFaces.widget.Dialog.prototype.postShow;
        var originalDialogOnHide = PrimeFaces.widget.Dialog.prototype.onHide;
        
        PrimeFaces.widget.Dialog.prototype.postShow = function() {
            originalDialogPostShow.apply(this);
            this.jq.attr('aria-live', 'off');
            
            var dialog_id = this.jq.attr('id').split(/[:]+/).pop();
            var dialog_titlebar_el = document.getElementById(this.id).querySelector(".ui-dialog-title");
            handleResizeDialog(dialog_id);

            $(window).on("resize", null, {dialog_id: dialog_id}, fixBodyWidth);
        }
        PrimeFaces.widget.Dialog.prototype.onHide = function(a, b) {
            originalDialogOnHide.apply(this, [a,b]);
            
            fixBodyWidth(false);

            $(window).off("resize", fixBodyWidth);
        }
        
        // Change default focus element
        PrimeFaces.widget.Dialog.prototype.applyFocus = function() {
            var $this = this;
            
            if(this.cfg.focus) {
                PrimeFaces.expressions.SearchExpressionFacade.resolveComponentsAsSelector(this.cfg.focus).focus();
            } else {
                // change: override default focus to title element instead of first input
                // change: focus inside timeout to fix problems for some dialogs - for example downloadPopup
                $this.jq.find('.ui-dialog-title').attr('tabindex', 0);
                setTimeout(function(){ $this.jq.find('.ui-dialog-title').focus(); }, 0);
            }
        };
    }
}
reinitializePrimefacesComponentsJS();

/*
 * Rebind bootstrap UI components after Primefaces ajax calls
 */
function bind_bsui_components(){
    // Breadcrumb Tree Keep Open
    $(document).on('click', '.dropdown-menu', function (e) {
        $(this).hasClass('keep-open'),
        e.stopPropagation();
    });
    
    // Collapse Header Icons
    $('div[id^="panelCollapse"]').on('shown.bs.collapse', function () {
      $(this).siblings('.panel-heading').children('span.glyphicon').removeClass("glyphicon-chevron-down").addClass("glyphicon-chevron-up");
    });

    $('div[id^="panelCollapse"]').on('hidden.bs.collapse', function () {
      $(this).siblings('.panel-heading').children('span.glyphicon').removeClass("glyphicon-chevron-up").addClass("glyphicon-chevron-down");
    });
    
    // Button dropdown menus 
    $('.dropdown-toggle').dropdown();
    
    // Hide open tooltips + popovers
    $('.bootstrap-button-tooltip, [data-toggle="tooltip"]').tooltip("hide");
    $("[data-toggle='popover']").popover("hide");

    // Tooltips + popovers
    bind_tooltip_popover();

    // Disabled pagination links
    disabledLinks();
    
    // Sharrre
    sharrre();
    
    // Custom Popover with HTML content
    popoverHTML();
    
}

function bind_tooltip_popover(){
    // rebind tooltips and popover to all necessary elements
    $(".bootstrap-button-tooltip, [data-toggle='tooltip']").tooltip({container: 'body'});
    $("[data-toggle='popover']").popover({container: 'body'});
    
    // CLOSE OPEN TOOLTIPS + POPOVERS ON BODY CLICKS
    $('body').on("touchstart", function(e){
        $(".bootstrap-button-tooltip, [data-toggle='tooltip']").each(function () {
            // hide any open tooltips when anywhere else in body is clicked
            if (!$(this).is(e.target) && $(this).has(e.target).length === 0 && $('div.tooltip').has(e.target).length === 0) {
                $(this).tooltip('hide');
            }////end if
        });
        $("a.popoverHTML, [data-toggle='popover']").each(function () {
            //the 'is' for buttons that trigger popups
            //the 'has' for icons within a button that triggers a popup
            if (!$(this).is(e.target) && $(this).has(e.target).length === 0 && $('div.popover').has(e.target).length === 0) {
                $(this).popover('hide');
            }
        });
    });
    
    // CLOSE OPEN TOOLTIPS ON BUTTON CLICKS
    $('.bootstrap-button-tooltip').on('click', function () {
        $(this).tooltip('hide');
    });
}

function toggle_dropdown(){
    $('.btn-group.open').removeClass('open');
}

function disabledLinks(){
    $('ul.pagination li').on('click', 'a', function (e) {
        if ($(this).parent().hasClass('disabled')){
            e.preventDefault();
        }
    });
}

/*
* Custom Popover with HTML code snippet
*/
function popoverHTML(popoverTitleHTML) {
   var popoverTemplateHTML = ['<div class="popover">',
       '<div class="arrow"></div>',
       '<h3 class="popover-title"></h3>',
       '<div class="popover-content">',
       '</div>',
       '</div>'].join('');

   var popoverContentHTML = ['<code>',
       '&lt;a&gt;, &lt;b&gt;, &lt;blockquote&gt;, &lt;br&gt;, &lt;code&gt;, &lt;del&gt;, &lt;dd&gt;, &lt;dl&gt;, &lt;dt&gt;, &lt;em&gt;, &lt;hr&gt;, &lt;h1&gt;-&lt;h3&gt;, &lt;i&gt;, &lt;img&gt;, &lt;kbd&gt;, &lt;li&gt;, &lt;ol&gt;, &lt;p&gt;, &lt;pre&gt;, &lt;s&gt;, &lt;sup&gt;, &lt;sub&gt;, &lt;strong&gt;, &lt;strike&gt;, &lt;ul&gt;',
       '</code>'].join('');

   $('body').popover({
       selector: 'a.popoverHTML',
       title: popoverTitleHTML,
       trigger: 'hover',
       content: popoverContentHTML,
       template: popoverTemplateHTML,
       placement: "bottom",
       container: "#content",
       html: true
   });
}

/*
 * Equal Div Height
 */
function post_differences(){
       var dialogHeight = $('div[id$="detailsBlocks"].ui-dialog').outerHeight();
       var dialogHeader = $('div[id$="detailsBlocks"] .ui-dialog-titlebar').outerHeight();
       var dialogScroll = dialogHeight - dialogHeader;
       $('div[id$="detailsBlocks"] .ui-dialog-content').css('height', dialogScroll);
}

/*
 * Sharrre
 */
function sharrre(){
    var language = $('#sharrre-widget').data('language');
    
    var sharrreLocales = {
            pl: {
                'sharrre.button.facebook.title': 'Udostępnij na Facebooku',
                'sharrre.button.twitter.title': 'Udostępnij na Twitterze',
                'sharrre.button.newWindonw.info': '(otwierane w nowym oknie)'
            },
            en: {
                'sharrre.button.facebook.title': 'Share on Facebook',
                'sharrre.button.twitter.title': 'Share in Twitter',
                'sharrre.button.newWindonw.info': '(opens in new window)'
            }
    };
    
    var currentLocales = sharrreLocales[language];
    if (currentLocales === null) {
        currentLocales = sharrreLocales.en;
    }
    
    $('#sharrre-widget').sharrre({
        share: {
            facebook: true,
            twitter: true
        },
        locales: currentLocales,
        template: '<div id="sharrre-block" class="clearfix">\n\
                    <input type="hidden" id="sharrre-total" name="sharrre-total" value="{total}"/> \n\
                    <a href="#" class="sharrre-facebook" title="{sharrre.button.facebook.title} {sharrre.button.newWindonw.info}" aria-label="{sharrre.button.facebook.title} {sharrre.button.newWindonw.info}"><span class="socicon socicon-facebook" aria-hidden="true"/></a> \n\
                    <a href="#" class="sharrre-twitter" title="{sharrre.button.twitter.title} {sharrre.button.newWindonw.info}" aria-label="{sharrre.button.twitter.title} {sharrre.button.newWindonw.info}"><span class="socicon socicon-twitter" aria-hidden="true"/></a> \n\
                    </div>',
        enableHover: false,
        enableTracking: true,
        urlCurl: '',
        render: function(api, options){
            var elementHtml = $(api.element).html();
            
            $.each(options.locales, function(key, translation) {
                elementHtml = elementHtml.split('{' + key + '}').join(translation);
            });
            $(api.element).html(elementHtml);
            
            $(api.element).on('click', '.sharrre-twitter', function() {
                api.openPopup('twitter');
            });
            $(api.element).on('click', '.sharrre-facebook', function() {
                api.openPopup('facebook');
            });
        }
    });
}

/*
 * Select dataset/file citation onclick event
 */
function selectText(ele) {
    try {
        var div = document.createRange();
        div.setStartBefore(ele);
        div.setEndAfter(ele);
        window.getSelection().addRange(div)
    } catch (e) {
        // for internet explorer
        div = document.selection.createRange();
        div.moveToElementText(ele);
        div.select()
    }
}

/*
 * Dialog Height-Scrollable
 */
function handleResizeDialog(dialogElement) {
    var dialog = $('div[id$="' + dialogElement + '"]');
    var doc = $('body');
    var win = $(window);
    var dialogPos = '';
    
    function calculateResize() {
        var overlay = $('#' + dialogElement + '_modal');
        var bodyHeight = '';
        var bodyWidth = '';
    
        // position:fixed is maybe cool, but it makes the dialog not scrollable on browser level, even if document is big enough
        if (dialog.height() > win.height()) {
            bodyHeight = dialog.height() + 'px';
            dialogPos = 'absolute';
        }
        if (dialog.width() > win.width()) {
            bodyWidth = dialog.width() + 'px';
            dialogPos = 'absolute';
        }
        dialog.css('position', dialogPos);
        doc.css('width', bodyWidth);
        doc.css('height', bodyHeight);
        
        var pos = dialog.offset();
        if (pos.top + dialog.height() > doc.height()) {
                pos.top = doc.height() - dialog.height();
                overlay.css('height', bodyHeight);
            }
        if (pos.left + dialog.width() > doc.width()) {
                pos.left = doc.width() - dialog.width();
                overlay.css('width', bodyWidth);
            }
        var offsetX = 0;
        var offsetY = 0;
        if (dialogPos != 'absolute') {
            offsetX = $(window).scrollLeft();
            offsetY = $(window).scrollTop();
        }
        // scroll fix for position fixed
        if (pos.left < offsetX)
            pos.left = offsetX;
        if (pos.top < offsetY)
            pos.top = offsetY;
            dialog.offset(pos);
    }
    
    calculateResize();
    
    dialog.find('textarea').each(function(index){
        $(this).on('keyup change cut paste focus', function(){
            calculateResize();
        });
    });

    fixBodyWidth(dialog);
}

/*
 * fixes body style attribute being set incorrectly and/or nor removed when closing modal
 */
function fixBodyWidth(dialogElement) {
    if (typeof dialogElement === 'string') {
        var dialog = $('div[id$="' + dialogElement + '"]');
    }
    else if (typeof dialogElement === 'object') {
        var dialog = $('div[id$="' + dialogElement.data.dialog_id + '"]');
    }

    var doc = $('body');
    var win = $(window);
    var wrapper = $('#body-wrapper');

    if (!dialogElement || dialog.width() < win.width()) {
        // if no modal is shown or it fits on the screen, remove the values
        doc.css('width', '');
        wrapper.css('max-width', '');
    }
    else {
        doc.css('width', dialog.width() + 'px');
        wrapper.css('max-width', document.documentElement.clientWidth + 'px');
    }
}


/*
 * fixes autoComplete dropdown in popups not moving with page scroll
 */
function handle_dropdown_popup_scroll(){
    $( window ).scroll(function() {
        var isActive = $(".DropdownPopupPanel").is(':visible');
        if(isActive) {
            $(".DropdownPopupPanel").position({
                my: "left top",
                at: "left bottom",
                of: $(".DropdownPopup")
            });
        }
    });
}

/*
 * Fixed nested submenus not-navigable by keyboard
 */
function handle_keydown_submenus(element, key){
    var $parent  = $(element.parentNode.parentNode.parentNode);

    var desc = ' li:not(.disabled):visible a';
    var $items = $parent.find('.dropdown-menu' + desc);

    if (!$items.length) {
        return
    }

    var index = $items.index(element);

    if (key === 38 && index === 0) {
        $(element.parentNode.parentNode.parentNode.previousElementSibling).find('a').focus();
    }
    if (key === 38 && index > 0) {
        index--;
        $items.eq(index);
        $items.eq(index).trigger('focus');
    }
    else if (key === 40 && index < $items.length - 1) {
        index++;
        $items.eq(index);
        $items.eq(index).trigger('focus');
    }
    else if (key === 40 && index === $items.length - 1) {
        $(element.parentNode.parentNode.parentNode.nextElementSibling).find('a').focus();
    }
}
$(document).ready(function() {
    $(".dropdown-menu .dropdown-menu").keydown(function(event) {
        handle_keydown_submenus(event.target, event.keyCode)
    });
    /* Fix focus being set on tabindex="-1" element when using arrows */
    $(".no-focus").focus(function(event) {
        /* Timeout is necessary, submenu must appear first to set focus on it */
        setTimeout(function(){ 
            $(event.target).next().find("li a").first().focus(); 
        }, 1);
    });
});
