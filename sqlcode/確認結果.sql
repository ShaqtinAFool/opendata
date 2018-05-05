# 分析
USE ty;
SELECT 
	ac.a_cont_id,
	ti.ty_name_en,
	ti.ty_year,
	ti.ty_number,
	ci.centre,
	ci.model,
	ai.base_time,
	ac.lat,
	ac.lon,
	ac.min_pres
FROM AnalysisInfo AS ai
INNER JOIN AnalysisContent AS ac ON ac.a_info_id = ai.a_info_id
INNER JOIN CentreInfo AS ci ON ci.centre_info_id = ai.centre_info_id
INNER JOIN TyphoonInfo AS ti ON ti.ty_info_id = ai.ty_info_id
#where ac.a_info_id between 1 and 5
ORDER BY ti.ty_number,ai.base_time
;

# 系集
USE ty;
SELECT 
	ec.e_cont_id,
	ti.ty_name_en,
	ti.ty_year,
	ti.ty_number,
	ci.centre,
	ci.model,
	ei.base_time,
	ec.valid_time,
	ec.lat,
	ec.lon,
	ec.min_pres,
	ei.member
FROM EnsembleInfo AS ei
INNER JOIN EnsembleContent AS ec ON ec.e_info_id = ei.e_info_id
INNER JOIN CentreInfo AS ci ON ci.centre_info_id = ei.centre_info_id
INNER JOIN TyphoonInfo AS ti ON ti.ty_info_id = ei.ty_info_id
#where ec.e_info_id between 1 and 5
ORDER BY ei.member,ti.ty_number,ei.base_time
;

# 官方
USE ty;
SELECT 
	ac.a_cont_id,
	ti.ty_name_en,
	ti.ty_year,
	ti.ty_number,
	ci.centre,
	ci.model,
	ai.base_time,
	ac.lat,
	ac.lon,
	ac.min_pres
FROM AnalysisInfo AS ai
INNER JOIN AnalysisContent AS ac ON ac.a_info_id = ai.a_info_id
INNER JOIN CentreInfo AS ci ON ci.centre_info_id = ai.centre_info_id
INNER JOIN TyphoonInfo AS ti ON ti.ty_info_id = ai.ty_info_id
#where ac.a_info_id between 1 and 5
ORDER BY ti.ty_number,ai.base_time
;