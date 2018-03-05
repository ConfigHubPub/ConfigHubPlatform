ALTER TABLE PropertyKey ADD CONSTRAINT UC_propertyKey UNIQUE (propertyKey, repositoryId);
ALTER TABLE PropertyKey ADD INDEX repositoryId (repositoryId);
ALTER TABLE Level ADD CONSTRAINT UC_name UNIQUE (name, depth, repositoryId);
ALTER TABLE Level ADD INDEX repositoryid (repositoryid);
ALTER TABLE Repository ADD CONSTRAINT UC_name UNIQUE (accountId, name);
ALTER TABLE SecurityProfile ADD CONSTRAINT UC_name UNIQUE (name, repositoryId);
ALTER TABLE Property ADD INDEX repository_id (repository_id, propertyKey_id);
ALTER TABLE Property_Audit ADD INDEX repository_id (repository_id, propertyKey_id);

ALTER TABLE ClientRequest ADD INDEX ClientRequestIndexes (repositoryId, ts, tokenId);




CREATE TABLE `PropertyKey` (
  `id` bigint(20) NOT NULL,
  `diffJson` longtext,
  `deprecated` bit(1) DEFAULT NULL,
  `propertyKey` varchar(255) NOT NULL,
  `pushValueEnabled` bit(1) DEFAULT NULL,
  `readme` longtext,
  `valueDataType` varchar(255) NOT NULL,
  `repositoryId` bigint(20) NOT NULL,
  `securityProfile_id` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKkgk1sxx690tdpxj9hsfv28uba` (`propertyKey`,`repositoryId`),
  KEY `FK96xe4gylfsnkofd6y707ktpqk` (`repositoryId`),
  KEY `FKlp5w4yjeg4n40cbn4difypkoi` (`securityProfile_id`),
  CONSTRAINT `FK96xe4gylfsnkofd6y707ktpqk` FOREIGN KEY (`repositoryId`) REFERENCES `Repository` (`id`),
  CONSTRAINT `FKlp5w4yjeg4n40cbn4difypkoi` FOREIGN KEY (`securityProfile_id`) REFERENCES `SecurityProfile` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1



CREATE TABLE `Property` (
  `id` bigint(20) NOT NULL,
  `diffJson` longtext,
  `active` bit(1) DEFAULT NULL,
  `contextJson` varchar(255) DEFAULT NULL,
  `contextWeight` int(11) NOT NULL,
  `value` longtext,
  `repositoryId` bigint(20) NOT NULL,
  `propertyKey_id` bigint(20) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKfxn0wmkpx8k8x2g2r5xyq3jup` (`repositoryId`),
  KEY `FKrcfwuulcd2xveojqygbw2j82c` (`propertyKey_id`),
  CONSTRAINT `FKfxn0wmkpx8k8x2g2r5xyq3jup` FOREIGN KEY (`repositoryId`) REFERENCES `Repository` (`id`),
  CONSTRAINT `FKrcfwuulcd2xveojqygbw2j82c` FOREIGN KEY (`propertyKey_id`) REFERENCES `PropertyKey` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1



CREATE TABLE `Level` (
  `id` bigint(20) NOT NULL,
  `diffJson` longtext,
  `depth` varchar(255) NOT NULL,
  `name` varchar(255) NOT NULL,
  `type` varchar(255) NOT NULL,
  `repositoryId` bigint(20) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKhgt86bme51u315n6co4rs5ov4` (`name`,`depth`,`repositoryId`),
  KEY `FKsxkfkathmadso50q5j6jwvojp` (`repositoryId`),
  CONSTRAINT `FKsxkfkathmadso50q5j6jwvojp` FOREIGN KEY (`repositoryId`) REFERENCES `Repository` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1


CREATE TABLE `AbsoluteFilePath` (
  `id` bigint(20) NOT NULL,
  `diffJson` longtext,
  `absPath` varchar(255) NOT NULL,
  `filename` varchar(255) NOT NULL,
  `path` varchar(255) DEFAULT NULL,
  `repositoryId` bigint(20) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKrncnvsdk64t5vjteltar1wwcw` (`absPath`,`repositoryId`),
  KEY `FKlxiorvj8k9qshy951bovehh1y` (`repositoryId`),
  CONSTRAINT `FKlxiorvj8k9qshy951bovehh1y` FOREIGN KEY (`repositoryId`) REFERENCES `Repository` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1


CREATE TABLE `Repository` (
  `id` bigint(20) NOT NULL,
  `diffJson` longtext,
  `accessControlEnabled` bit(1) DEFAULT NULL,
  `active` bit(1) DEFAULT NULL,
  `allowTokenFreeAPI` bit(1) DEFAULT NULL,
  `contextClustersEnabled` bit(1) DEFAULT NULL,
  `createDate` datetime NOT NULL,
  `demo` bit(1) DEFAULT NULL,
  `depth` varchar(255) NOT NULL,
  `depthLabels` varchar(255) NOT NULL,
  `description` varchar(255) DEFAULT NULL,
  `isPrivate` bit(1) NOT NULL,
  `name` varchar(255) NOT NULL,
  `securityProfilesEnabled` bit(1) DEFAULT NULL,
  `valueTypeEnabled` bit(1) DEFAULT NULL,
  `accountId` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKgsxkucdri6drlvmgecqpktajm` (`name`,`accountId`),
  KEY `FKhb1y76p3uwq7ldt1v3yw1toob` (`accountId`),
  CONSTRAINT `FKhb1y76p3uwq7ldt1v3yw1toob` FOREIGN KEY (`accountId`) REFERENCES `Account` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1





