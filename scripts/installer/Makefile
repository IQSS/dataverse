INSTALLER_ZIP_DIR=dvinstall
DISTRIBUTION_WAR_FILE=${INSTALLER_ZIP_DIR}/dataverse.war
GLASSFISH_SETUP_SCRIPT=${INSTALLER_ZIP_DIR}/as-setup.sh
API_SCRIPTS=${INSTALLER_ZIP_DIR}/setup-datasetfields.sh ${INSTALLER_ZIP_DIR}/setup-users.sh ${INSTALLER_ZIP_DIR}/setup-builtin-roles.sh ${INSTALLER_ZIP_DIR}/setup-dvs.sh ${INSTALLER_ZIP_DIR}/data ${INSTALLER_ZIP_DIR}/setup-identity-providers.sh  ${INSTALLER_ZIP_DIR}/setup-all.sh ${INSTALLER_ZIP_DIR}/post-install-api-block.sh
JHOVE_CONFIG=${INSTALLER_ZIP_DIR}/jhove.conf
JHOVE_SCHEMA=${INSTALLER_ZIP_DIR}/jhoveConfig.xsd
SOLR_SCHEMA=${INSTALLER_ZIP_DIR}/schema.xml ${INSTALLER_ZIP_DIR}/schema_dv_mdb_fields.xml ${INSTALLER_ZIP_DIR}/schema_dv_mdb_copies.xml ${INSTALLER_ZIP_DIR}/updateSchemaMDB.sh
SOLR_CONFIG=${INSTALLER_ZIP_DIR}/solrconfig.xml
PYTHON_FILES=${INSTALLER_ZIP_DIR}/README_python.txt ${INSTALLER_ZIP_DIR}/installConfig.py ${INSTALLER_ZIP_DIR}/installUtils.py ${INSTALLER_ZIP_DIR}/install.py ${INSTALLER_ZIP_DIR}/installAppServer.py ${INSTALLER_ZIP_DIR}/requirements.txt ${INSTALLER_ZIP_DIR}/default.config ${INSTALLER_ZIP_DIR}/interactive.config
INSTALL_SCRIPT=${INSTALLER_ZIP_DIR}/install

installer:	dvinstall.zip

clean: 
	/bin/rm -rf ${INSTALLER_ZIP_DIR} dvinstall.zip

dvinstall.zip:	${GLASSFISH_SETUP_SCRIPT} ${JSF_PATCH_DIR} ${DISTRIBUTION_WAR_FILE} ${API_SCRIPTS} ${JHOVE_CONFIG} ${JHOVE_SCHEMA} ${SOLR_SCHEMA} ${SOLR_CONFIG} ${PYTHON_FILES} ${INSTALL_SCRIPT}
	@echo making installer...
	zip -r dvinstall.zip ${GLASSFISH_SETUP_SCRIPT} ${JSF_PATCH_DIR} ${DISTRIBUTION_WAR_FILE} ${API_SCRIPTS} ${JHOVE_CONFIG} ${JHOVE_SCHEMA} ${SOLR_SCHEMA} ${SOLR_CONFIG} ${PYTHON_FILES} ${INSTALL_SCRIPT}
	@echo 
	@echo "Done!"

${INSTALLER_ZIP_DIR}:
	mkdir -p ${INSTALLER_ZIP_DIR}

${INSTALL_SCRIPT}: install ${INSTALLER_ZIP_DIR}
	@echo copying the installer script
	/bin/cp install ${INSTALLER_ZIP_DIR}

${DISTRIBUTION_WAR_FILE}: 
	@echo copying war file...
	@VERSION_NUMBER=`grep -m1 '<version>' ../../pom.xml | sed 's/ *<\/*version>//g'`; export VERSION_NUMBER; \
	BUILT_WAR_FILE=../../target/dataverse-$$VERSION_NUMBER.war; export BUILT_WAR_FILE; \
	if [ -f $$BUILT_WAR_FILE ]; \
	then \
		/bin/cp $$BUILT_WAR_FILE ${DISTRIBUTION_WAR_FILE}; \
	else \
		echo "ERROR: can't find application .war file ($${BUILT_WAR_FILE})!"; \
		echo "Build the Dataverse application war file, then try again."; \
		exit 1; \
	fi

${GLASSFISH_SETUP_SCRIPT}:	as-setup.sh ${INSTALLER_ZIP_DIR}
	@echo copying glassfish setup
	/bin/cp as-setup.sh ${INSTALLER_ZIP_DIR}

${API_SCRIPTS}:	../api/setup-datasetfields.sh ../api/setup-users.sh ../api/setup-dvs.sh ../api/setup-identity-providers.sh ../api/setup-all.sh ../api/post-install-api-block.sh ../api/setup-builtin-roles.sh ../api/data ${INSTALLER_ZIP_DIR}
	@echo copying api scripts
	/bin/cp -R ../api/setup-datasetfields.sh ../api/setup-users.sh ../api/setup-dvs.sh ../api/setup-identity-providers.sh ../api/setup-all.sh ../api/post-install-api-block.sh ../api/setup-builtin-roles.sh ../api/data ${INSTALLER_ZIP_DIR}

${JHOVE_CONFIG}:	../../conf/jhove/jhove.conf ${INSTALLER_ZIP_DIR}
	@echo copying jhove config file
	/bin/cp ../../conf/jhove/jhove.conf ${INSTALLER_ZIP_DIR}

${JHOVE_SCHEMA}:	../../conf/jhove/jhoveConfig.xsd ${INSTALLER_ZIP_DIR}
	@echo copying jhove schema file
	/bin/cp ../../conf/jhove/jhoveConfig.xsd ${INSTALLER_ZIP_DIR}

${SOLR_SCHEMA}:		../../conf/solr/8.8.1/schema.xml ../../conf/solr/8.8.1/schema_dv_mdb_fields.xml ../../conf/solr/8.8.1/schema_dv_mdb_copies.xml ../../conf/solr/8.8.1/updateSchemaMDB.sh ${INSTALLER_ZIP_DIR}
	@echo copying Solr schema file
	/bin/cp ../../conf/solr/8.8.1/schema*.xml ../../conf/solr/8.8.1/updateSchemaMDB.sh ${INSTALLER_ZIP_DIR}

${SOLR_CONFIG}:		../../conf/solr/8.8.1/solrconfig.xml ${INSTALLER_ZIP_DIR}
	@echo copying Solr config file
	/bin/cp ../../conf/solr/8.8.1/solrconfig.xml ${INSTALLER_ZIP_DIR}

${PYTHON_FILES}: README_python.txt install.py installConfig.py installAppServer.py installUtils.py requirements.txt default.config interactive.config ${INSTALLER_ZIP_DIR}
	@echo copying Python installer files
	/bin/cp README_python.txt install.py installConfig.py installAppServer.py installUtils.py requirements.txt default.config interactive.config ${INSTALLER_ZIP_DIR}
