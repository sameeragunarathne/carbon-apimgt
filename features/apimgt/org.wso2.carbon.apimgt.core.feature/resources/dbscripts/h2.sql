
CREATE TABLE `AM_API_POLICY` (
  `UUID` VARCHAR(255),
  `NAME` VARCHAR(255),
  `DISPLAY_NAME` VARCHAR(255),
  `DESCRIPTION` VARCHAR(1024),
  PRIMARY KEY (`UUID`),
  UNIQUE (`NAME`)
);

CREATE TABLE `AM_APPLICATION_POLICY` (
  `UUID` VARCHAR(255),
  `NAME` VARCHAR(512) NOT NULL,
  `DISPLAY_NAME` VARCHAR(512) NULL DEFAULT NULL,
  `DESCRIPTION` VARCHAR(1024) NULL DEFAULT NULL,
  `QUOTA_TYPE` VARCHAR(25) NOT NULL,
  `QUOTA` INT(11) NOT NULL,
  `QUOTA_UNIT` VARCHAR(10) NULL DEFAULT NULL,
  `UNIT_TIME` INT(11) NOT NULL,
  `TIME_UNIT` VARCHAR(25) NOT NULL,
  `IS_DEPLOYED` TINYINT(1) NOT NULL DEFAULT 0,
  `CUSTOM_ATTRIBUTES` BLOB DEFAULT NULL,
  PRIMARY KEY (UUID),
  UNIQUE (`NAME`),
  UNIQUE INDEX APP_NAME (NAME)
);

CREATE TABLE `AM_SUBSCRIPTION_POLICY` (
  `UUID` VARCHAR(255),
  `NAME` VARCHAR(255),
  `DISPLAY_NAME` VARCHAR(512),
  `DESCRIPTION` VARCHAR(1024),
  `QUOTA_TYPE` VARCHAR(30),
  `QUOTA` INTEGER,
  `QUOTA_UNIT` VARCHAR(30),
  `UNIT_TIME` INTEGER,
  `TIME_UNIT` VARCHAR(30),
  `RATE_LIMIT_COUNT` INTEGER,
  `RATE_LIMIT_TIME_UNIT` VARCHAR(30),
  `IS_DEPLOYED` BOOL,
  `CUSTOM_ATTRIBUTES` BLOB,
  `STOP_ON_QUOTA_REACH` BOOL,
  `BILLING_PLAN` VARCHAR(30),
  PRIMARY KEY (`UUID`),
  UNIQUE (`NAME`)
);

CREATE TABLE `AM_API` (
  `UUID` VARCHAR(255),
  `PROVIDER` VARCHAR(255),
  `NAME` VARCHAR(255),
  `CONTEXT` VARCHAR(255),
  `VERSION` VARCHAR(30),
  `IS_DEFAULT_VERSION` BOOLEAN,
  `DESCRIPTION` VARCHAR(1024),
  `VISIBILITY` VARCHAR(30),
  `IS_RESPONSE_CACHED` BOOLEAN,
  `CACHE_TIMEOUT` INTEGER,
  `TECHNICAL_OWNER` VARCHAR(255),
  `TECHNICAL_EMAIL` VARCHAR(255),
  `BUSINESS_OWNER` VARCHAR(255),
  `BUSINESS_EMAIL` VARCHAR(255),
  `LIFECYCLE_INSTANCE_ID` VARCHAR(255),
  `CURRENT_LC_STATUS` VARCHAR(255),
  `CORS_ENABLED` BOOLEAN,
  `CORS_ALLOW_ORIGINS` VARCHAR(512),
  `CORS_ALLOW_CREDENTIALS` BOOLEAN,
  `CORS_ALLOW_HEADERS` VARCHAR(512),
  `CORS_ALLOW_METHODS` VARCHAR(255),
  `CREATED_BY` VARCHAR(100),
  `CREATED_TIME` TIMESTAMP,
  `LAST_UPDATED_TIME` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `COPIED_FROM_API` VARCHAR(255),
  PRIMARY KEY (`UUID`),
  UNIQUE (`PROVIDER`,`NAME`,`VERSION`),
  UNIQUE (`CONTEXT`,`VERSION`)
);

CREATE TABLE `AM_API_VISIBLE_ROLES` (
  `API_ID` VARCHAR(255),
  `ROLE` VARCHAR(255),
  PRIMARY KEY (`API_ID`, `ROLE`),
  FOREIGN KEY (`API_ID`) REFERENCES `AM_API`(`UUID`) ON UPDATE CASCADE ON DELETE CASCADE
);

CREATE TABLE `AM_API_TAG_MAPPING` (
  `API_ID` VARCHAR(255),
  `TAG_ID` INTEGER,
  PRIMARY KEY (`API_ID`, `TAG_ID`),
  FOREIGN KEY (`API_ID`) REFERENCES `AM_API`(`UUID`) ON UPDATE CASCADE ON DELETE CASCADE
);

CREATE TABLE `AM_TAGS` (
  `TAG_ID` INTEGER AUTO_INCREMENT,
  `TAG_NAME` VARCHAR(255),
  PRIMARY KEY (`TAG_ID`)
);

CREATE TABLE `AM_API_SUBSCRIPTION_POLICY_MAPPING` (
  `API_ID` VARCHAR(255),
  `SUBSCRIPTION_POLICY_ID` VARCHAR(255),
  PRIMARY KEY (`API_ID`, `SUBSCRIPTION_POLICY_ID`),
  FOREIGN KEY (`API_ID`) REFERENCES `AM_API`(`UUID`) ON UPDATE CASCADE ON DELETE CASCADE,
  FOREIGN KEY (`SUBSCRIPTION_POLICY_ID`) REFERENCES `AM_SUBSCRIPTION_POLICY`(`UUID`) ON UPDATE CASCADE ON DELETE CASCADE
);

CREATE TABLE `AM_API_ENDPOINTS` (
  `API_ID` VARCHAR(255),
  `ENVIRONMENT_CATEGORY` VARCHAR(30),
  `ENDPOINT_TYPE` VARCHAR(30),
  `IS_ENDPOINT_SECURED` BOOLEAN,
  `TPS` INTEGER,
  `AUTH_DIGEST` VARCHAR(30),
  `USERNAME` VARCHAR(255),
  `PASSWORD` VARCHAR(255),
  PRIMARY KEY (`API_ID`),
  FOREIGN KEY (`API_ID`) REFERENCES `AM_API`(`UUID`) ON UPDATE CASCADE ON DELETE CASCADE
);

CREATE TABLE `AM_API_SCOPES` (
  `API_ID` VARCHAR(255),
  `SCOPE_ID` INTEGER,
  PRIMARY KEY (`API_ID`, `SCOPE_ID`),
  FOREIGN KEY (`API_ID`) REFERENCES `AM_API`(`UUID`) ON UPDATE CASCADE ON DELETE CASCADE
);

CREATE TABLE `AM_API_URL_MAPPING` (
  `API_ID` VARCHAR(255),
  `HTTP_METHOD` VARCHAR(30),
  `URL_PATTERN` VARCHAR(255),
  `AUTH_SCHEME` VARCHAR(30),
  `API_POLICY_ID` VARCHAR(255),
  PRIMARY KEY (`API_ID`, `HTTP_METHOD`, `URL_PATTERN`),
  FOREIGN KEY (`API_ID`) REFERENCES `AM_API`(`UUID`) ON UPDATE CASCADE ON DELETE CASCADE,
  FOREIGN KEY (`API_POLICY_ID`) REFERENCES `AM_API_POLICY`(`UUID`)
);

CREATE TABLE `AM_APPLICATION` (
  `UUID` VARCHAR(255),
  `NAME` VARCHAR(255),
  `APPLICATION_POLICY_ID` VARCHAR(255),
  `CALLBACK_URL` VARCHAR(512),
  `DESCRIPTION` VARCHAR(1024),
  `APPLICATION_STATUS` VARCHAR(255),
  `GROUP_ID` VARCHAR(255) NULL DEFAULT NULL,
  `CREATED_BY` VARCHAR(100),
  `CREATED_TIME` TIMESTAMP,
  `UPDATED_BY` VARCHAR(100),
  `LAST_UPDATED_TIME` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`UUID`),
  UNIQUE (NAME),
  FOREIGN KEY (`APPLICATION_POLICY_ID`) REFERENCES `AM_APPLICATION_POLICY`(`UUID`) ON UPDATE CASCADE
);

CREATE TABLE `AM_APP_KEY_MAPPING` (
  `APPLICATION_ID` INTEGER,
  `CONSUMER_KEY` VARCHAR(255),
  `KEY_TYPE` VARCHAR(255),
  `STATE` VARCHAR(30),
  `CREATE_MODE` VARCHAR(30),
  PRIMARY KEY (`APPLICATION_ID`, `KEY_TYPE`),
  FOREIGN KEY (`APPLICATION_ID`) REFERENCES `AM_APPLICATION`(`UUID`) ON UPDATE CASCADE ON DELETE CASCADE
);

CREATE TABLE `AM_API_TRANSPORTS` (
  `API_ID` VARCHAR(255),
  `TRANSPORT` VARCHAR(30),
  PRIMARY KEY (`API_ID`, `TRANSPORT`),
  FOREIGN KEY (`API_ID`) REFERENCES `AM_API`(`UUID`) ON UPDATE CASCADE ON DELETE CASCADE
);


CREATE TABLE `AM_RESOURCE_CATEGORIES` (
  `RESOURCE_CATEGORY_ID` INTEGER AUTO_INCREMENT,
  `RESOURCE_CATEGORY` VARCHAR(255),
  PRIMARY KEY (`RESOURCE_CATEGORY_ID`),
  UNIQUE (`RESOURCE_CATEGORY`)
);

CREATE TABLE `AM_API_RESOURCES` (
  `UUID` VARCHAR(255),
  `API_ID` VARCHAR(255),
  `RESOURCE_CATEGORY_ID` INTEGER,
  `DATA_TYPE` VARCHAR(255),
  `RESOURCE_TEXT_VALUE` VARCHAR(1024),
  `RESOURCE_BINARY_VALUE` BLOB,
  PRIMARY KEY (`UUID`),
  FOREIGN KEY (`API_ID`) REFERENCES `AM_API`(`UUID`) ON UPDATE CASCADE ON DELETE CASCADE,
  FOREIGN KEY (`RESOURCE_CATEGORY_ID`) REFERENCES `AM_RESOURCE_CATEGORIES`(`RESOURCE_CATEGORY_ID`)
);

CREATE TABLE `AM_API_DOC_META_DATA` (
  `UUID` VARCHAR(255),
  `NAME` VARCHAR(255),
  `SUMMARY` VARCHAR(1024),
  `TYPE` VARCHAR(255),
  `OTHER_TYPE_NAME` VARCHAR(255),
  `SOURCE_URL` VARCHAR(255),
  `SOURCE_TYPE` VARCHAR(255),
  `VISIBILITY` VARCHAR(30),
  PRIMARY KEY (`UUID`),
  FOREIGN KEY (`UUID`) REFERENCES `AM_API_RESOURCES`(`UUID`) ON UPDATE CASCADE ON DELETE CASCADE
 );

CREATE TABLE IF NOT EXISTS AM_SUBSCRIPTION (
  `UUID` VARCHAR(255),
  `TIER_ID` VARCHAR(50),
  `API_ID` VARCHAR(255),
  `APPLICATION_ID` VARCHAR(255),
  `SUB_STATUS` VARCHAR(50),
  `SUB_TYPE` VARCHAR(50),
  `CREATED_BY` VARCHAR(100),
  `CREATED_TIME` TIMESTAMP,
  `UPDATED_BY` VARCHAR(100),
  `UPDATED_TIME` TIMESTAMP,
  FOREIGN KEY(APPLICATION_ID) REFERENCES AM_APPLICATION(UUID) ON UPDATE CASCADE ON DELETE RESTRICT,
  FOREIGN KEY(API_ID) REFERENCES AM_API(UUID) ON UPDATE CASCADE ON DELETE RESTRICT,
  PRIMARY KEY (UUID)
);


INSERT INTO AM_API_POLICY (`UUID`, `NAME`, `DISPLAY_NAME`, `DESCRIPTION`) VALUES ('1', 'Unlimited', 'Unlimited', 'Unlimited');
INSERT INTO AM_API_POLICY (`UUID`, `NAME`, `DISPLAY_NAME`, `DESCRIPTION`) VALUES ('2', 'Gold', 'Gold', 'Gold');
INSERT INTO AM_API_POLICY (`UUID`, `NAME`, `DISPLAY_NAME`, `DESCRIPTION`) VALUES ('3', 'Bronze', 'Bronze', 'Bronze');
INSERT INTO AM_API_POLICY (`UUID`, `NAME`, `DISPLAY_NAME`, `DESCRIPTION`) VALUES ('4', 'Silver', 'Silver', 'Silver');

INSERT INTO AM_SUBSCRIPTION_POLICY (`UUID`, `NAME`, `DISPLAY_NAME`, `DESCRIPTION`) VALUES ('1', 'Unlimited', 'Unlimited', 'Unlimited');
INSERT INTO AM_SUBSCRIPTION_POLICY (`UUID`, `NAME`, `DISPLAY_NAME`, `DESCRIPTION`) VALUES ('2', 'Gold', 'Gold', 'Gold');
INSERT INTO AM_SUBSCRIPTION_POLICY (`UUID`, `NAME`, `DISPLAY_NAME`, `DESCRIPTION`) VALUES ('3', 'Silver', 'Silver', 'Silver');
INSERT INTO AM_SUBSCRIPTION_POLICY (`UUID`, `NAME`, `DISPLAY_NAME`, `DESCRIPTION`) VALUES ('4', 'Bronze', 'Bronze', 'Bronze');

INSERT INTO AM_APPLICATION_POLICY (UUID, NAME, DISPLAY_NAME, DESCRIPTION, QUOTA_TYPE, QUOTA, QUOTA_UNIT, UNIT_TIME,
            TIME_UNIT, IS_DEPLOYED) VALUES ('id-xxxxx-xxxxx', '50PerMin', '50PerMin', '50PerMin Tier', 'x', 10, 'REQ', 1, 's', 1);
INSERT INTO AM_APPLICATION_POLICY (UUID, NAME, DISPLAY_NAME, DESCRIPTION, QUOTA_TYPE, QUOTA, QUOTA_UNIT, UNIT_TIME,
            TIME_UNIT, IS_DEPLOYED) VALUES ('id-yyyyy-yyyyy', '20PerMin', '20PerMin', '20PerMin Tier', 'y', 50, 'REQ', 1, 's', 1);
Commit;