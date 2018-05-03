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
ORDER BY ai.base_time
;