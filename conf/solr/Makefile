DIST_DIR           := ./dist
DOWNLOAD_DIR       := $(DIST_DIR)/download
EXTRACT_DIR        := $(DIST_DIR)/extract
POM_LOCATION       := ../../pom.xml
XSLT_LOCATION      := ./config
XSLT_FILES         := $(wildcard $(XSLT_LOCATION)/*.xslt)
SOLR_VERS_FILE     := $(DIST_DIR)/solr-version
SOLR_TARGZ         := $(DOWNLOAD_DIR)/solr.tar.gz
SOLR_DEFAULT_CS    := $(EXTRACT_DIR)/_default
SOLR_CONFIGSET     := $(DIST_DIR)/dataverse_cs
SOLR_CFG_SRC       := $(SOLR_DEFAULT_CS)/conf/solrconfig.xml
SOLR_CFG_PATCHED   := $(SOLR_CONFIGSET)/conf/solrconfig.xml
SOLR_CFG_TMP       := $(DIST_DIR)/solrconfig.xml.tmp
SOLR_SCHEMA_DST    := $(SOLR_CONFIGSET)/conf/schema.xml
SOLR_SCHEMA_SRC    := ./schema/schema.xml
SOLR_CONFIGSET_ZIP := $(DIST_DIR)/solr-configset.zip

.PHONY: all clean clean-all get-solr prepare-solr patch-solr package

all: patch-solr

clean:
	rm -rf $(EXTRACT_DIR) $(SOLR_CONFIGSET) $(SOLR_CONFIGSET_ZIP) $(SOLR_VERS_FILE)

clean-all:
	rm -rf $(DIST_DIR)

get-solr: $(DOWNLOAD_DIR) $(SOLR_TARGZ)

prepare-solr: $(EXTRACT_DIR) get-solr $(SOLR_CONFIGSET)

patch-solr: prepare-solr $(SOLR_CFG_PATCHED) $(SOLR_SCHEMA_DST)

package: patch-solr $(SOLR_CONFIGSET_ZIP)

$(SOLR_CONFIGSET_ZIP): $(SOLR_CFG_PATCHED) $(SOLR_SCHEMA_DST)
	$(info Zipping the Dataverse Solr ConfigSet for distribution)
	cd "$(SOLR_CONFIGSET)" && zip -9Tru "$(abspath $(SOLR_CONFIGSET_ZIP))" *

$(SOLR_SCHEMA_DST): $(SOLR_SCHEMA_SRC)
	$(info Delete managed-schema and copy static schema into Dataverse ConfigSet)
	-rm $(SOLR_CONFIGSET)/conf/managed-schema
	cp "$(SOLR_SCHEMA_SRC)" "$(SOLR_SCHEMA_DST)"

$(SOLR_CFG_PATCHED): $(XSLT_FILES)
	$(info Dataverse ConfigSet: Copy default solrconfig.xml and patch with XSLT)
	cp "$(SOLR_CFG_SRC)" "$(SOLR_CFG_PATCHED)"
	for XSLT in $(XSLT_FILES); do xsltproc "$$XSLT" "$(SOLR_CFG_PATCHED)" > "$(SOLR_CFG_TMP)" && mv "$(SOLR_CFG_TMP)" "$(SOLR_CFG_PATCHED)"; done

$(SOLR_CONFIGSET): $(SOLR_DEFAULT_CS)
	$(info Copy default ConfigSet into a Dataverse flavored one)
	[ -d "$(SOLR_CONFIGSET)" ] && rm -rf "$(SOLR_CONFIGSET)" || true
	cp -a "$(SOLR_DEFAULT_CS)" "$(SOLR_CONFIGSET)"
	rm "$(SOLR_CFG_PATCHED)" # initialy delete the config file - we will add a patched one in the next step

$(SOLR_DEFAULT_CS): $(SOLR_TARGZ) $(SOLR_VERS_FILE)
	$(info Extracting default ConfigSet from distribution)
	$(eval SOLR_VERSION := $(shell cat "$(SOLR_VERS_FILE)"))
	$(eval SOLR_TARGZ_SUBPATH := solr-$(SOLR_VERSION)/server/solr/configsets/_default)
	tar -xmzf "$(SOLR_TARGZ)" -C "$(EXTRACT_DIR)" "$(SOLR_TARGZ_SUBPATH)"
	cp -r "$(EXTRACT_DIR)/$(SOLR_TARGZ_SUBPATH)" "$(EXTRACT_DIR)"
	rm -rf "$(EXTRACT_DIR)/solr-$(SOLR_VERSION)"

$(SOLR_TARGZ): $(SOLR_VERS_FILE)
	$(info Downloading Solr binary distribution)
	$(eval SOLR_VERSION := $(shell cat "$(SOLR_VERS_FILE)"))
	$(eval SOLR_DOWNLOAD_URL := https://archive.apache.org/dist/lucene/solr/$(SOLR_VERSION)/solr-$(SOLR_VERSION).tgz)
	curl -fsS "$(SOLR_DOWNLOAD_URL)" -o "$(SOLR_TARGZ)"

$(SOLR_VERS_FILE): $(POM_LOCATION)
	$(info Extracting Solr version from Maven POM)
	[ -d "$(DIST_DIR)" ] || mkdir -p "$(DIST_DIR)"
	mvn -f "$(POM_LOCATION)" help:evaluate -Dexpression=solr.version -q -DforceStdout > "$(SOLR_VERS_FILE)"

$(DOWNLOAD_DIR):
	[ -d "$(DOWNLOAD_DIR)" ] || mkdir -p "$(DOWNLOAD_DIR)"

$(EXTRACT_DIR):
	[ -d "$(EXTRACT_DIR)" ] || mkdir -p "$(EXTRACT_DIR)"

