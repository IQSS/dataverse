------------
-- Migrate the database to the workflow-enabled version
------------

------------
-- Add new workflows-related tables
------------

CREATE TABLE WORKFLOW (ID  SERIAL NOT NULL, NAME VARCHAR(255), PRIMARY KEY (ID));

CREATE TABLE WORKFLOWSTEPDATA (ID  SERIAL NOT NULL, 
                               PROVIDERID VARCHAR(255), 
                               STEPTYPE VARCHAR(255),
                               PARENT_ID BIGINT,
                               index INTEGER, PRIMARY KEY (ID));

CREATE TABLE PENDINGWORKFLOWINVOCATION ( INVOCATIONID VARCHAR(255) NOT NULL, 
                                         DOIPROVIDER VARCHAR(255),
                                         IPADDRESS VARCHAR(255),
                                         NEXTMINORVERSIONNUMBER BIGINT,
                                         NEXTVERSIONNUMBER BIGINT,
                                         PENDINGSTEPIDX INTEGER,
                                         TYPEORDINAL INTEGER,
                                         USERID VARCHAR(255),
                                         WORKFLOW_ID BIGINT,
                                         DATASET_ID BIGINT,
                                         PRIMARY KEY (INVOCATIONID));

CREATE TABLE WorkflowStepData_STEPPARAMETERS (WorkflowStepData_ID BIGINT,
                                              STEPPARAMETERS VARCHAR(2048),
                                              STEPPARAMETERS_KEY VARCHAR(255));

CREATE TABLE PendingWorkflowInvocation_LOCALDATA (PendingWorkflowInvocation_INVOCATIONID VARCHAR(255),
                                                  LOCALDATA VARCHAR(255), 
                                                  LOCALDATA_KEY VARCHAR(255));

ALTER TABLE WORKFLOWSTEPDATA 
        ADD CONSTRAINT FK_WORKFLOWSTEPDATA_PARENT_ID 
        FOREIGN KEY (PARENT_ID) REFERENCES WORKFLOW (ID);

ALTER TABLE PENDINGWORKFLOWINVOCATION 
    ADD CONSTRAINT FK_PENDINGWORKFLOWINVOCATION_WORKFLOW_ID 
    FOREIGN KEY (WORKFLOW_ID) REFERENCES WORKFLOW (ID);

ALTER TABLE PENDINGWORKFLOWINVOCATION
        ADD CONSTRAINT FK_PENDINGWORKFLOWINVOCATION_DATASET_ID 
        FOREIGN KEY (DATASET_ID) REFERENCES DVOBJECT (ID);

ALTER TABLE WorkflowStepData_STEPPARAMETERS 
        ADD CONSTRAINT FK_WorkflowStepData_STEPPARAMETERS_WorkflowStepData_ID
        FOREIGN KEY (WorkflowStepData_ID) REFERENCES WORKFLOWSTEPDATA (ID);

ALTER TABLE PendingWorkflowInvocation_LOCALDATA 
        ADD CONSTRAINT PndngWrkflwInvocationLOCALDATAPndngWrkflwInvocationINVOCATIONID 
        FOREIGN KEY (PendingWorkflowInvocation_INVOCATIONID) REFERENCES PENDINGWORKFLOWINVOCATION (INVOCATIONID);


------------
-- Add lockReason field to Dataset/DatasetVersion
------------
TBC

------------
-- Validate there are no double-reason locks (??)
------------
TBC

------------
-- Convert from boolean lock reasons to the enum-based one
------------
TBC

------------
-- Delete lock reasons columns
------------
TBC
