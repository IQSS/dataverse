INSTALLER_ZIP_DIR=dvinstall
DISTRIBUTION_WAR_FILE=${INSTALLER_ZIP_DIR}/dataverse.war
GLASSFISH_SETUP_SCRIPT=${INSTALLER_ZIP_DIR}/glassfish-setup.sh
POSTGRES_DRIVERS=${INSTALLER_ZIP_DIR}/pgdriver
API_SCRIPTS=${INSTALLER_ZIP_DIR}/setup-datasetfields.sh ${INSTALLER_ZIP_DIR}/setup-users.sh ${INSTALLER_ZIP_DIR}/setup-builtin-roles.sh ${INSTALLER_ZIP_DIR}/setup-dvs.sh ${INSTALLER_ZIP_DIR}/data ${INSTALLER_ZIP_DIR}/setup-identity-providers.sh  ${INSTALLER_ZIP_DIR}/setup-all.sh ${INSTALLER_ZIP_DIR}/post-install-api-block.sh
DB_SCRIPT=${INSTALLER_ZIP_DIR}/reference_data.sql
JHOVE_CONFIG=${INSTALLER_ZIP_DIR}/jhove.conf
JHOVE_SCHEMA=${INSTALLER_ZIP_DIR}/jhoveConfig.xsd
SOLR_SCHEMA=${INSTALLER_ZIP_DIR}/schema.xml
SOLR_CONFIG=${INSTALLER_ZIP_DIR}/solrconfig.xml
INSTALL_SCRIPT=${INSTALLER_ZIP_DIR}/install

installer:	dvinstall.zip

clean: 
	/bin/rm -rf ${INSTALLER_ZIP_DIR} dvinstall.zip

dvinstall.zip:	${GLASSFISH_SETUP_SCRIPT} ${POSTGRES_DRIVERS} ${DISTRIBUTION_WAR_FILE} ${API_SCRIPTS} ${DB_SCRIPT} ${JHOVE_CONFIG} ${JHOVE_SCHEMA} ${SOLR_SCHEMA} ${SOLR_CONFIG} ${INSTALL_SCRIPT}
	@echo making installer...
	zip -r dvinstall.zip ${GLASSFISH_SETUP_SCRIPT} ${POSTGRES_DRIVERS} ${DISTRIBUTION_WAR_FILE} ${API_SCRIPTS} ${DB_SCRIPT} ${JHOVE_CONFIG} ${JHOVE_SCHEMA} ${SOLR_SCHEMA} ${SOLR_CONFIG} ${INSTALL_SCRIPT}
	@echo 
	@echo "Done!"

${INSTALL_SCRIPT}: install
	@echo copying the installer script
	mkdir -p ${INSTALLER_ZIP_DIR}
	/bin/cp install ${INSTALLER_ZIP_DIR}

${DISTRIBUTION_WAR_FILE}: 
	@echo copying war file...
	@mkdir -p ${INSTALLER_ZIP_DIR}
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

${GLASSFISH_SETUP_SCRIPT}:	glassfish-setup.sh
	@echo copying glassfish setup
	mkdir -p ${INSTALLER_ZIP_DIR}
	/bin/cp glassfish-setup.sh ${INSTALLER_ZIP_DIR}

${POSTGRES_DRIVERS}:	 pgdriver/postgresql-42.2.2.jar
	@echo copying postgres driver
	@mkdir -p ${POSTGRES_DRIVERS}
	/bin/cp pgdriver/postgresql-42.2.2.jar ${INSTALLER_ZIP_DIR}/pgdriver

${API_SCRIPTS}:	../api/setup-datasetfields.sh ../api/setup-users.sh ../api/setup-dvs.sh ../api/setup-identity-providers.sh ../api/setup-all.sh ../api/post-install-api-block.sh ../api/setup-builtin-roles.sh ../api/data
	@echo copying api scripts
	@mkdir -p ${INSTALLER_ZIP_DIR}
	/bin/cp -R ../api/setup-datasetfields.sh ../api/setup-users.sh ../api/setup-dvs.sh ../api/setup-identity-providers.sh ../api/setup-all.sh ../api/post-install-api-block.sh ../api/setup-builtin-roles.sh ../api/data ${INSTALLER_ZIP_DIR}

${DB_SCRIPT}:	../database/reference_data.sql
	@echo copying reference data sql script
	@mkdir -p ${INSTALLER_ZIP_DIR}
	/bin/cp ../database/reference_data.sql ${INSTALLER_ZIP_DIR}

${JHOVE_CONFIG}:	../../conf/jhove/jhove.conf
	@echo copying jhove config file
	@mkdir -p ${INSTALLER_ZIP_DIR}
	/bin/cp ../../conf/jhove/jhove.conf ${INSTALLER_ZIP_DIR}

${JHOVE_SCHEMA}:	../../conf/jhove/jhoveConfig.xsd
	@echo copying jhove schema file
	@mkdir -p ${INSTALLER_ZIP_DIR}
	/bin/cp ../../conf/jhove/jhoveConfig.xsd ${INSTALLER_ZIP_DIR}

${SOLR_SCHEMA}:		../../conf/solr/7.3.0/schema.xml
	@echo copying Solr schema file
	@mkdir -p ${INSTALLER_ZIP_DIR}
	/bin/cp ../../conf/solr/7.3.0/schema.xml ${INSTALLER_ZIP_DIR}

${SOLR_CONFIG}:		../../conf/solr/7.3.0/solrconfig.xml
	@echo copying Solr config file
	@mkdir -p ${INSTALLER_ZIP_DIR}
	/bin/cp ../../conf/solr/7.3.0/solrconfig.xml ${INSTALLER_ZIP_DIR}
