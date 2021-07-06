
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
        
        PrimeFaces.widget.SelectCheckboxMenu.prototype.updateLabel = function() {
            var checkedItems = this.jq.find(':checked'),
            labelText = '';

            if(checkedItems && checkedItems.length) {
                for(var i = 0; i < checkedItems.length; i++) {
                    if(i != 0) {
                        labelText = labelText + this.cfg.labelSeparator;
                    }
                    labelText = labelText + $(checkedItems[i]).next().text();
                }
            }
            else {
                if (this.cfg.emptyLabel) {
                    labelText = this.cfg.emptyLabel;
                } else {
                    labelText = this.defaultLabel;
                }
            }
    
            this.label.text(labelText);
            this.labelContainer.attr('title', labelText);
            this.keyboardTarget.val(labelText);
        }
    }
    
    if (PrimeFaces.widget.SelectOneMenu) {
        var originalSelectOneMenuInitContents = PrimeFaces.widget.SelectOneMenu.prototype.initContents;
        var originalSelectOneMenuChangeAriaValue = PrimeFaces.widget.SelectOneMenu.prototype.changeAriaValue;
        var originalSelectOneMenuHighlightItem = PrimeFaces.widget.SelectOneMenu.prototype.highlightItem;
        var originalSelectOneMenuShow = PrimeFaces.widget.SelectOneMenu.prototype.show;
        
        PrimeFaces.widget.SelectOneMenu.prototype.initContents = function() {
            originalSelectOneMenuInitContents.apply(this);
            if(this.cfg.filter) {
                var filterElement = this.panel.find('> div.ui-selectonemenu-filter-container > input.ui-selectonemenu-filter');
                
                //this.panel.attr('role', 'combobox');
                //this.panel.attr('aria-owns', this.itemsContainer.attr('id'))
                
                filterElement.attr('role', 'combobox');
                filterElement.attr('aria-owns', this.itemsContainer.attr('id'));
                filterElement.attr('aria-controls', this.itemsContainer.attr('id'));
                filterElement.attr('aria-label', 'Filtruj opcje');
            }
        };
        
        PrimeFaces.widget.SelectOneMenu.prototype.changeAriaValue = function(item) {
            var filterElement = this.panel.find('> div.ui-selectonemenu-filter-container > input.ui-selectonemenu-filter');
            var itemId = item.attr('id');

            this.focusInput.attr('aria-activedescendant', itemId)
                    .attr('aria-describedby', itemId);
            this.itemsContainer.attr('aria-activedescendant', itemId);
            //this.itemsContainer.removeAttr('aria-activedescendant');
            
            if(this.cfg.filter) {
                filterElement.attr('aria-activedescendant', itemId);
            }
        };
        
        PrimeFaces.widget.SelectOneMenu.prototype.highlightItem = function(item) {
            this.items.attr('aria-selected', false);
            this.items.filter('.ui-state-highlight').removeClass('ui-state-highlight');

            if(item.length > 0) {
                item.addClass('ui-state-highlight');
                item.attr('aria-selected', true);
                this.setLabel(item.data('label'));
                this.changeAriaValue(item);
            }
        };
        
        PrimeFaces.widget.SelectOneMenu.prototype.show = function() {
            if (this.cfg.filter) {
                this.filterInput.val('');
                this.filter(this.filterInput.val());
            }
            //this.highlightItem(this.items.eq(0));
            originalSelectOneMenuShow.apply(this);
        }
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
                        if($this.cfg.oncomplete && data.jqXHR) {
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

    if(PrimeFaces.widget.TextEditor) {
        PrimeFaces.widget.TextEditor.prototype._render = function() {
            var $this = this;

            //toolbar
            this.toolbar = $(this.jqId + '_toolbar');
            if(!this.toolbar.length && this.cfg.toolbarVisible) {
                this.jq.prepend(this.toolbarTemplate);
                this.toolbar = this.jq.children('.ui-editor-toolbar')
                this.toolbar.attr('id', this.id + '_toolbar');
            }

            //configuration
            if(this.cfg.height) {
                this.editorContainer.height(this.cfg.height);
            }

            this.cfg.formats = [
                'bold',
                'header',
                'italic',
                'list',
                'blockquote',
                'script',
                'strike',
                'underline',
                'code',
            ];
            this.cfg.theme = 'snow';
            this.cfg.modules = {
                toolbar: this.cfg.toolbarVisible ? PrimeFaces.escapeClientId(this.id + '_toolbar') : false,
                clipboard: {
                    matchVisual: false
                },
                keyboard: {
                    bindings: {
                        tab: {
                            key: 9,
                            handler: function() {
                                return true;
                            }
                        },
                        'remove tab': {
                            key: 9,
                            shiftKey: true,
                            collapsed: true,
                            prefix: /\t$/,
                            handler: function() {
                                return true;
                            }
                        },
                        list: {
                            key: 'Tab',
                            shiftKey: true,
                            format: ['list'],
                            collapsed: true,
                            handler: function () {
                                return true;
                            }
                        },
                        indent: {
                            key: 'Tab',
                            format: ['list', 'blockquote'],
                            handler() {
                                return true;
                            },
                        },
                        outdent: {
                            key: 'Tab',
                            shiftKey: true,
                            format: ['list', 'blockquote'],
                            handler() {
                                return true;
                            },
                        },
                        'indent code-block': {
                            key: 'Tab',
                            shiftKey: true,
                            format: ['blockquote'],
                            handler() {
                                return true;
                            }
                        },
                        'outdent code-block': {
                            key: 'Tab',
                            shiftKey: true,
                            format: ['blockquote'],
                            handler() {
                                return true;
                            }
                        }
                    }
                }
            };

            //initialize
            this.editor = new Quill(PrimeFaces.escapeClientId(this.id) + '_editor', this.cfg);

            var events = ["keyup", "keydown", "click", "dblclick", "keypress", "mousedown", "mousemove", "mouseout",
                "mouseover", "mouseup"];

            $.each(events, function(index, value) {
                $this.registerEvent(value);
            });

            //set initial value
            this.input.val(this.getEditorValue());

            //update input on change
            this.editor.on('text-change', function(delta, oldDelta, source) {
                $this.input.val($this.getEditorValue());
                $this.callBehavior('change');
            });
            this.editor.on('selection-change', function(range, oldRange, source) {
                if(range && !oldRange) {
                    $this.callBehavior('focus');
                }
                if(!range && oldRange) {
                    $this.callBehavior('blur');
                }
                if(range && oldRange) {
                    $this.callBehavior('select');
                }
            });
            
            var toolbarObserver = new MutationObserver(function(mutations, observer) {
                for (var i=0; i<mutations.length; ++i) {
                    var mutation = mutations[i];
                    var mutationTarget = $(mutation.target);
                    
                    if (mutation.type === 'attributes' && mutation.attributeName === 'class') {
                        mutationTarget.attr('aria-pressed', mutationTarget.hasClass('ql-active'));
                    }
                }
            });
            toolbarObserver.observe(this.toolbar[0].children[0], { attributes: true, attributeFilter: [ "class" ], childList: true, subtree: true });
            
            $(this.jq).find('.ql-tooltip.ql-hidden').remove();
            
            $(this.jq).find('.ql-editor').attr('aria-label', this.jq.data('editor-aria-label'))
        }
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
    
    if (PrimeFaces.widget.PickList) {
        var originalPickListInit = PrimeFaces.widget.PickList.prototype.init;
        var originalPickListDisableButton = PrimeFaces.widget.PickList.prototype.disableButton;
        var originalPickListEnableButton = PrimeFaces.widget.PickList.prototype.enableButton;
        
        PrimeFaces.widget.PickList.prototype.init = function(cfg) {
            originalPickListInit.apply(this, [cfg]);
            
            this.sourceCaption = this.sourceList.prev('.ui-picklist-caption'),
            this.targetCaption = this.targetList.prev('.ui-picklist-caption');
             
            if (this.sourceCaption.find('.ui- nu').length > 0) {
                this.sourceCaption = this.sourceCaption.find('.ui-selectonemenu').find('label');
                
                this.sourceList.attr('aria-label', this.sourceCaption.text());
                this.sourceInput.attr('title', this.sourceCaption.text());
            }
            
            if (this.sourceFilter) {
                if (this.sourceCaption.length) {
                    this.sourceFilter.attr('aria-label', PrimeFaces.getLocaleSettings().filterPickList + ' ' + this.sourceCaption.text());
                } else {
                    this.sourceFilter.attr('aria-label', PrimeFaces.getLocaleSettings().filterPickList);
                }
            }
            if (this.targetFilter) {
                if (this.targetCaption.length) {
                    this.targetFilter.attr('aria-label', PrimeFaces.getLocaleSettings().filterPickList + ' ' + this.targetCaption.text());
                } else {
                    this.targetFilter.attr('aria-label', PrimeFaces.getLocaleSettings().filterPickList);
                }
            }
            
        };
        
        PrimeFaces.widget.PickList.prototype.disableButton = function(button) {
            originalPickListDisableButton.apply(this, [button]);
            button.attr('aria-disabled', true);
        };
        PrimeFaces.widget.PickList.prototype.enableButton = function(button) {
            originalPickListEnableButton.apply(this, [button]);
            button.attr('aria-disabled', false);
        }
    }
    

    if (PrimeFaces.widget.AutoComplete) {
        var originalAutocompleteInvokeItemSelectBehavior = PrimeFaces.widget.AutoComplete.prototype.invokeItemSelectBehavior;
        var originalAutocompleteRemoveItem = PrimeFaces.widget.AutoComplete.prototype.removeItem;
        var originalAutocompleteBindStaticEvents = PrimeFaces.widget.AutoComplete.prototype.bindStaticEvents;
        
        
        PrimeFaces.widget.AutoComplete.prototype.invokeItemSelectBehavior = function(event, itemValue) {
            originalAutocompleteInvokeItemSelectBehavior.apply(this, [event, itemValue]);
            if (this.cfg.multiple) {
                this.displayAriaStatus(PrimeFaces.getLocaleSettings().ariaSelectAutoComplete + $(event.target).attr('data-item-label'));
            }
        };
        PrimeFaces.widget.AutoComplete.prototype.removeItem = function(event, item) {
            originalAutocompleteRemoveItem.apply(this, [event, item]);
            if (this.cfg.multiple) {
                this.displayAriaStatus(PrimeFaces.getLocaleSettings().ariaUnselectAutoComplete + $(item).text());
            }
        }
        PrimeFaces.widget.AutoComplete.prototype.bindStaticEvents = function() {
            originalAutocompleteBindStaticEvents.apply(this);
            
            var $this = this;
            
            $( window ).scroll(function() {
                var isActive = $this.panel.is(':visible');
                if(isActive) {
                    $this.alignPanel();
                }
            });
        }
    }
    
    if (PrimeFaces.widget.DefaultCommand) {
        var originalDefaultCommandInit = PrimeFaces.widget.DefaultCommand.prototype.init;
        
        PrimeFaces.widget.DefaultCommand.prototype.init = function(cfg) {
            originalDefaultCommandInit.apply(this, [cfg]);
            
            var closestForm = this.jqTarget.closest('form');
            closestForm.off('keydown.' + this.id).on('keydown.' + this.id, {scopeEnter: false}, function (e, data) {
                var keyCode = $.ui.keyCode;

                data = data || e.data;
                if (($this.scope && data.scopeEnter && data.scopeDefaultCommandId === $this.id)
                        || (!$this.scope && !data.scopeEnter && (e.which == keyCode.ENTER))) {
                    //do not proceed if target is a textarea,button or link - change: added contenteditable
                    if ($(e.target).is('textarea,button,input[type="submit"],a,[contenteditable="true"]')) {
                        return true;
                    }

                    if (!$this.jqTarget.is(':disabled, .ui-state-disabled')) {
                        $this.jqTarget.click();
                    }
                    e.preventDefault();
                    e.stopImmediatePropagation();
                }
            });
        }
    }

    /* disable swipe events for tabView and paginator, to allow scrolling contents without unexpected behavior */
    if (PrimeFaces.widget.Paginator) {
        PrimeFaces.widget.Paginator.prototype.bindSwipeEvents = function() {} 
    }
    if (PrimeFaces.widget.TabView) {
        PrimeFaces.widget.TabView.prototype.bindSwipeEvents = function() {}
    }
    
    if (PrimeFaces.widget.Paginator) {
        var originalPaginatorInit = PrimeFaces.widget.Paginator.prototype.init;
        var originalPaginatorUpdatePageLinks = PrimeFaces.widget.Paginator.prototype.updatePageLinks;
        
        PrimeFaces.widget.Paginator.prototype.init = function(cfg) {
            originalPaginatorInit.apply(this, [cfg]);
            
            var currentPage = this.pagesContainer.find('.ui-paginator-page.ui-state-active');
            currentPage.attr('aria-label', currentPage.attr('aria-label') + ' ' + PrimeFaces.getLocaleSettings().ariaCurrentPagePaginator)
        }
        
        PrimeFaces.widget.Paginator.prototype.updatePageLinks = function() {
            originalPaginatorUpdatePageLinks.apply(this);
            
            var currentPage = this.pagesContainer.find('.ui-paginator-page.ui-state-active');
            currentPage.attr('aria-label', currentPage.attr('aria-label') + ' ' + PrimeFaces.getLocaleSettings().ariaCurrentPagePaginator)
        }
    }

    if (PrimeFaces.widget.BlockUI) {
        var originalBlockUIRender = PrimeFaces.widget.BlockUI.prototype.render;
        var originalBlockUIShow = PrimeFaces.widget.BlockUI.prototype.show;
        var originalBlockUIHide = PrimeFaces.widget.BlockUI.prototype.hide;
        
        // BlockUI component will sometimes change css position property of block element
        // to properly position overlay over block element. This doesn't play nice
        // with fix_submenus_overflow() - calculations done there to properly position
        // submenus will be incorrect.
        // As a workaround we change css position of block element only for the time
        // when overlay is visible.
        PrimeFaces.widget.BlockUI.prototype.render= function() {
            this.originalBlockPosition = this.block.css("position");
            
            originalBlockUIRender.apply(this);
            
            this.block.css('position', this.originalBlockPosition);
        }
        PrimeFaces.widget.BlockUI.prototype.show= function() {
            var position = this.block.css('position');
            if (position !== "fixed" && position  !== "absolute") {
                this.block.css('position', 'relative');
            }
            
            originalBlockUIShow.apply(this);
        }
        PrimeFaces.widget.BlockUI.prototype.hide= function() {
            originalBlockUIHide.apply(this);
            this.block.css('position', this.originalBlockPosition);
        }
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
    hideTooltipsAndPopovers();

    // Tooltips + popovers
    bind_tooltip_popover();

    // Disabled pagination links
    disabledLinks();
    
    // Sharrre
    sharrre();
    
    // Custom Popover with HTML content
    popoverHTML();
    
}

/*
    Binds popovers and tooltips according to the wcag 1.4.13
 */
function bind_tooltip_popover(){

    bindPopovers();
    bindTooltips();
    bindEscapeKey();
}

function hideTooltipsAndPopovers() {
    $('.bootstrap-button-tooltip, [data-toggle="tooltip"]').tooltip("hide");
    $("[data-toggle='popover']").popover("hide");
}

function bindTooltips() {
    $('.bootstrap-button-tooltip, [data-toggle="tooltip"]')
        .tooltip({container: 'body', trigger: 'manual'})
        .on("mouseover", event => {
            var closestTooltipToggle = $(event.target).closest('.bootstrap-button-tooltip, [data-toggle="tooltip"]');
            
            if (closestTooltipToggle.data()['bs.tooltip'].tip().hasClass('in')) { // check if tooltip is visible
                return;
            }
            
            $(event.target).tooltip("show");
            $(".tooltip").on("mouseleave", function () {
                $(event.target).tooltip("hide");
            });
        })
        .on("mouseout", event => {
            setTimeout(() => {
                var closestTooltipToggle = $(event.target).closest('.bootstrap-button-tooltip, [data-toggle="tooltip"]');
                
                if (closestTooltipToggle.is(":hover")) {
                    return;
                }
                if (!$(".tooltip:hover").length) {
                    closestTooltipToggle.tooltip("hide");
                }
            }, 200);
        })
        .on("focus", event => {
            $(event.target).tooltip("show");
        })
        .on("blur", event => {
            $(event.target).tooltip("hide");
        })
}

function bindPopovers() {
    $('[data-toggle="popover"]')
        .attr("tabindex", 0)
        .popover({container: 'body', trigger: 'manual'})
        .on("mouseover", event => {
            $(event.currentTarget).popover('show');
            $(".popover").on("mouseleave", function () {
                $(event.currentTarget).popover("hide");
            });
        })
        .on("mouseout", event => {
            setTimeout(() => {
                if (!$("[data-toggle='popover']:hover").length && !$(".popover-content:hover").length) $(event.currentTarget).popover("hide");
            }, 50);
        })
        .on("focus", event => {
            $(event.currentTarget).popover("show");
        })
        .on("blur", event => {
            $(event.currentTarget).popover("hide");
        })
}

function bindEscapeKey() {
    $("body").on("keydown", event => {
        if (event.key === "Esc" || event.key === "Escape") {
            $('[data-toggle="tooltip"], .bootstrap-button-tooltip').tooltip("hide");
            $('[data-toggle="popover"]').popover("hide");
        }
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

/* Fix dropdown submenus not overflowing in scrollable tables */
/* Source: https://codepen.io/andykono/pen/WvPgvz */

function fix_submenus_overflow($element) {
    var selector = ".ui-datatable:not(.headerless-table):not(.no-min-width)";

    if (!$element) {
        $element = $("body");
    }

    //check if not already bound
    if ($element.find(selector).length > 0 && $element.find(selector).attr("data-fix-overflow") === "true") {
        return;
    }
    else {
        $element.find(selector).attr("data-fix-overflow", "true");
    }

    //add BT DD show event
    $element.find(selector + " .btn-group").on("show.bs.dropdown", function() {
        var $btnDropDown = $(this).find(".dropdown-toggle");
        var $listHolder = $(this).find("> .dropdown-menu");
        //reset position property for DD container
        $(this).css("position", "static");

        if ($listHolder.hasClass("pull-right")) {
            $listHolder.css({
                "width": $listHolder.outerWidth(true),
                "top": ($btnDropDown.offset().top + $btnDropDown.outerHeight(true)) + "px",
                "left": ($btnDropDown.offset().left - $listHolder.outerWidth(true) + $btnDropDown.outerWidth(true)) + "px"
            });
        }
        else {
            $listHolder.css({
                "top": ($btnDropDown.offset().top + $btnDropDown.outerHeight(true)) + "px",
                "left": $btnDropDown.offset().left + "px"
            });
        }

        $listHolder.data("open", true);
    });

    //add BT DD hide event
    $element.find(selector + " .btn-group").on("hidden.bs.dropdown", function() {
        var $listHolder = $(this).find("> .dropdown-menu");
        $listHolder.data("open", false);
    });

    //add on scroll for table holder
    $element.find(selector).scroll(function() {
        var $ddHolder = $(this).find(".btn-group")
        var $btnDropDown = $(this).find(".dropdown-toggle");
        var $listHolder = $(this).find("> .dropdown-menu");

        if ($listHolder.data("open")) {
            if ($listHolder.hasClass("pull-right")) {
                $listHolder.css({
                    "width": $listHolder.outerWidth(true),
                    "top": ($btnDropDown.offset().top + $btnDropDown.outerHeight(true)) + "px",
                    "left": ($btnDropDown.offset().left - $listHolder.outerWidth(true) + $btnDropDown.outerWidth(true)) + "px"
                });
            }
            else {
                $listHolder.css({
                    "top": ($btnDropDown.offset().top + $btnDropDown.outerHeight(true)) + "px",
                    "left": $btnDropDown.offset().left + "px"
                });
            }
            $ddHolder.toggleClass("open", ($btnDropDown.offset().left > $(this).offset().left))
        }
    })
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

    fix_submenus_overflow();

    var $tables = $(".ui-datatable:not(.headerless-table):not(.no-min-width)");
    $tables.parent().on("DOMSubtreeModified", function () {
        fix_submenus_overflow($(this));
    });
});
