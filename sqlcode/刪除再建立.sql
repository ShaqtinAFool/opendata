USE ty;

DROP TABLE BestTrackContent;
DROP TABLE BestTrackInfo;
DROP TABLE AnalysisContent;
DROP TABLE AnalysisInfo;
DROP TABLE EnsembleContent;
DROP TABLE EnsembleInfo;
DROP TABLE ForecastContent;
DROP TABLE ForecastInfo;
DROP TABLE CentreInfo;
DROP TABLE TyphoonInfo;


# typhoon list information
CREATE TABLE TyphoonInfo (
	ty_info_id INT AUTO_INCREMENT,
	ty_name_en VARCHAR(20),
	ty_name_tw VARCHAR(20),
	ty_year INT,
	ty_number INT,
	PRIMARY KEY (`ty_info_id`)
) DEFAULT CHARSET = utf8;

# centre list information
CREATE TABLE CentreInfo (
	centre_info_id INT AUTO_INCREMENT,
	centre VARCHAR(10) NOT NULL,
	model VARCHAR(45),
	resolution VARCHAR(45),
	geopotential_height INT,
	data_source VARCHAR(20) NOT NULL,
	PRIMARY KEY (`centre_info_id`)
) DEFAULT CHARSET = utf8;

# BestTrack major information
CREATE TABLE BestTrackInfo (
	bt_info_id INT AUTO_INCREMENT,
	ty_info_id INT NOT NULL,
	centre_info_id INT NOT NULL,
	base_time DATETIME NOT NULL,
	PRIMARY KEY (`bt_info_id`),
	FOREIGN KEY (ty_info_id) REFERENCES TyphoonInfo(ty_info_id),
	FOREIGN KEY (centre_info_id) REFERENCES CentreInfo(centre_info_id)
) DEFAULT CHARSET = utf8;

# analysis major information
CREATE TABLE AnalysisInfo (
	a_info_id INT AUTO_INCREMENT,
	ty_info_id INT NOT NULL,
	centre_info_id INT NOT NULL,
	base_time DATETIME NOT NULL,
	PRIMARY KEY (`a_info_id`),
	FOREIGN KEY (ty_info_id) REFERENCES TyphoonInfo(ty_info_id),
	FOREIGN KEY (centre_info_id) REFERENCES CentreInfo(centre_info_id)
) DEFAULT CHARSET = utf8;

# forecast major information
CREATE TABLE ForecastInfo (
	f_info_id INT AUTO_INCREMENT,
	ty_info_id INT NOT NULL,
	centre_info_id INT NOT NULL,
	base_time DATETIME NOT NULL,
	PRIMARY KEY (`f_info_id`),
	FOREIGN KEY (ty_info_id) REFERENCES TyphoonInfo(ty_info_id),
	FOREIGN KEY (centre_info_id) REFERENCES CentreInfo(centre_info_id)
) DEFAULT CHARSET = utf8;

# ensemble major information
CREATE TABLE EnsembleInfo (
	e_info_id INT AUTO_INCREMENT,
	ty_info_id INT NOT NULL,
	centre_info_id INT NOT NULL,
	base_time DATETIME NOT NULL,
	member VARCHAR(10) NOT NULL,
	PRIMARY KEY (`e_info_id`),
	FOREIGN KEY (ty_info_id) REFERENCES TyphoonInfo(ty_info_id),
	FOREIGN KEY (centre_info_id) REFERENCES CentreInfo(centre_info_id)
) DEFAULT CHARSET = utf8;

# BestTrack detail information
CREATE TABLE BestTrackContent (
	bt_cont_id INT AUTO_INCREMENT,
	bt_info_id INT NOT NULL,
	lat DECIMAL(10,3) NOT NULL,
	lon DECIMAL(10,3) NOT NULL,
	min_pres INT NOT NULL,
	wind DECIMAL(4,1),
	wind_unit VARCHAR(10),
	development VARCHAR(40),
	data_type VARCHAR(20) NOT NULL,
	PRIMARY KEY (`bt_cont_id`),
	FOREIGN KEY (bt_info_id) REFERENCES BestTrackInfo(bt_info_id)
) DEFAULT CHARSET = utf8;

# analysis detail information
CREATE TABLE AnalysisContent (
	a_cont_id INT AUTO_INCREMENT,
	a_info_id INT NOT NULL,
	lat DECIMAL(10,3) NOT NULL,
	lon DECIMAL(10,3) NOT NULL,
	min_pres INT NOT NULL,
	wind DECIMAL(4,1),
	wind_unit VARCHAR(10),
	development VARCHAR(40),
	data_type VARCHAR(20) NOT NULL,
	PRIMARY KEY (`a_cont_id`),
	FOREIGN KEY (a_info_id) REFERENCES AnalysisInfo(a_info_id)
) DEFAULT CHARSET = utf8;

# Forecast detail information
CREATE TABLE ForecastContent (
	f_cont_id INT AUTO_INCREMENT,
	f_info_id INT NOT NULL,
	lat DECIMAL(10,3) NOT NULL,
	lon DECIMAL(10,3) NOT NULL,
	min_pres INT NOT NULL,
	wind DECIMAL(4,1),
	wind_unit VARCHAR(10),
	valid_time DATETIME NOT NULL,
	valid_hour INT NOT NULL,
	PRIMARY KEY (`f_cont_id`),
	FOREIGN KEY (f_info_id) REFERENCES ForecastInfo(f_info_id)
) DEFAULT CHARSET = utf8;

# Ensemble detail information
CREATE TABLE EnsembleContent (
	e_cont_id INT AUTO_INCREMENT,
	e_info_id INT NOT NULL,
	lat DECIMAL(10,3) NOT NULL,
	lon DECIMAL(10,3) NOT NULL,
	min_pres INT NOT NULL,
	wind DECIMAL(4,1),
	wind_unit VARCHAR(10),
	valid_time DATETIME NOT NULL,
	valid_hour INT NOT NULL,
	PRIMARY KEY (`e_cont_id`),
	FOREIGN KEY (e_info_id) REFERENCES EnsembleInfo(e_info_id)
) DEFAULT CHARSET = utf8;