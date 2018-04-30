USE [TTFRITyphoon]

-- typhoon list information
CREATE TABLE StationInfo (
	StnInfo_Id INT IDENTITY(1, 1) PRIMARY KEY,
	typhoon_name VARCHAR(20),
	typhoon_year INT,
	typhoon_number INT
)
