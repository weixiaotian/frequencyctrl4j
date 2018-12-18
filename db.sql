CREATE TABLE `mss_quota_frequency` (
  `quota_type` varchar(64) NOT NULL DEFAULT '',
  `interval` varchar(32) NOT NULL DEFAULT '',
  `quota` int(11) NOT NULL,
  PRIMARY KEY (`quota_type`,`interval`,`quota`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;