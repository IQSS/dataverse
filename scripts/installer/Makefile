INSTALLER_ZIP_DIR=dvinstall
DISTRIBUTION_FILE=${INSTALLER_ZIP_DIR}/glassfish
GLASSFISH_SETUP_SCRIPT=${INSTALLER_ZIP_DIR}/glassfish-setup.sh
API_SCRIPTS=${INSTALLER_ZIP_DIR}/setup-datasetfields.sh ${INSTALLER_ZIP_DIR}/setup-users.sh ${INSTALLER_ZIP_DIR}/setup-builtin-roles.sh ${INSTALLER_ZIP_DIR}/setup-dvs.sh ${INSTALLER_ZIP_DIR}/data ${INSTALLER_ZIP_DIR}/setup-identity-providers.sh  ${INSTALLER_ZIP_DIR}/setup-all.sh ${INSTALLER_ZIP_DIR}/post-install-api-block.sh
SOLR_SCHEMA=${INSTALLER_ZIP_DIR}/schema.xml
SOLR_CONFIG=${INSTALLER_ZIP_DIR}/solrconfig.xml
SOLR_ROR_SCHEMA=${INSTALLER_ZIP_DIR}/rorSuggestions/schema.xml
SOLR_ROR_CONFIG=${INSTALLER_ZIP_DIR}/rorSuggestions/solrconfig.xml
INSTALL_SCRIPT=${INSTALLER_ZIP_DIR}/install

installer:	dvinstall.zip

clean: 
	/bin/rm -rf ${INSTALLER_ZIP_DIR} dvinstall.zip

dvinstall.zip:	${GLASSFISH_SETUP_SCRIPT} ${DISTRIBUTION_FILE} ${API_SCRIPTS} ${DB_SCRIPT} ${JHOVE_CONFIG} ${JHOVE_SCHEMA} ${SOLR_SCHEMA} ${SOLR_CONFIG} ${SOLR_ROR_SCHEMA} ${SOLR_ROR_CONFIG} ${INSTALL_SCRIPT}
	@echo making installer...
	zip -r dvinstall.zip ${GLASSFISH_SETUP_SCRIPT} ${POSTGRES_DRIVERS} ${DISTRIBUTION_FILE} ${API_SCRIPTS} ${DB_SCRIPT} ${SOLR_SCHEMA} ${SOLR_CONFIG} ${SOLR_ROR_SCHEMA} ${SOLR_ROR_CONFIG} ${INSTALL_SCRIPT}
	@echo 
	@echo "Done!"

${INSTALL_SCRIPT}: install
	@echo copying the installer script
	mkdir -p ${INSTALLER_ZIP_DIR}
	/bin/cp install ${INSTALLER_ZIP_DIR}

${DISTRIBUTION_FILE}:
	@echo copying distribution file...
	@mkdir -p ${INSTALLER_ZIP_DIR}
	@BUILT_DIST_DIR=../../dataverse-dist/target/dist; export BUILT_DIST_DIR; \
	if [ -d $$BUILT_DIST_DIR ]; \
	then \
		/bin/cp -r $$BUILT_DIST_DIR/* ${INSTALLER_ZIP_DIR}; \
	else \
		echo "ERROR: can't find application distribution ($${BUILT_DIST_DIR})!"; \
		echo "Build the Dataverse application distribution, then try again."; \
		exit 1; \
	fi

${GLASSFISH_SETUP_SCRIPT}:	glassfish-setup.sh
	@echo copying glassfish setup
	mkdir -p ${INSTALLER_ZIP_DIR}
	/bin/cp glassfish-setup.sh ${INSTALLER_ZIP_DIR}

${API_SCRIPTS}:	../api/setup-datasetfields.sh ../api/setup-users.sh ../api/setup-dvs.sh ../api/setup-identity-providers.sh ../api/setup-all.sh ../api/post-install-api-block.sh ../api/setup-builtin-roles.sh ../api/data
	@echo copying api scripts
	@mkdir -p ${INSTALLER_ZIP_DIR}
	/bin/cp -R ../api/setup-datasetfields.sh ../api/setup-users.sh ../api/setup-dvs.sh ../api/setup-identity-providers.sh ../api/setup-all.sh ../api/post-install-api-block.sh ../api/setup-builtin-roles.sh ../api/data ${INSTALLER_ZIP_DIR}

${DB_SCRIPT}:	../database/reference_data.sql
	@echo copying reference data sql script
	@mkdir -p ${INSTALLER_ZIP_DIR}
	/bin/cp ../database/reference_data.sql ${INSTALLER_ZIP_DIR}

${SOLR_SCHEMA}:		../../conf/solr/7.3.1/schema.xml
	@echo copying Solr schema file
	@mkdir -p ${INSTALLER_ZIP_DIR}
	/bin/cp ../../conf/solr/7.3.1/schema.xml ${INSTALLER_ZIP_DIR}

${SOLR_CONFIG}:		../../conf/solr/7.3.1/solrconfig.xml
	@echo copying Solr config file
	@mkdir -p ${INSTALLER_ZIP_DIR}
	/bin/cp ../../conf/solr/7.3.1/solrconfig.xml ${INSTALLER_ZIP_DIR}

${SOLR_ROR_SCHEMA}:     ../../conf/solr/7.3.1/ror/schema.xml
	@echo copying Solr schema file
	@mkdir -p ${INSTALLER_ZIP_DIR}/rorSuggestions
	/bin/cp ../../conf/solr/7.3.1/ror/schema.xml ${INSTALLER_ZIP_DIR}/rorSuggestions

${SOLR_ROR_CONFIG}:     ../../conf/solr/7.3.1/ror/solrconfig.xml
	@echo copying Solr config file
	@mkdir -p ${INSTALLER_ZIP_DIR}/rorSuggestions
	/bin/cp ../../conf/solr/7.3.1/ror/solrconfig.xml ${INSTALLER_ZIP_DIR}/rorSuggestions
