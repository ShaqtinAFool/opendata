# 最佳路徑
USE ty;
SELECT 
	btc.bt_cont_id,
	ti.ty_name_en,
	ti.ty_name_tw,
	ti.ty_year,
	ti.ty_number,
	ci.centre,
	ci.model,
	bti.base_time,
	btc.lat,
	btc.lon,
	btc.min_pres
	
FROM BestTrackInfo AS bti
INNER JOIN BestTrackContent AS btc ON btc.bt_info_id = bti.bt_info_id
INNER JOIN CentreInfo AS ci ON ci.centre_info_id = bti.centre_info_id
INNER JOIN TyphoonInfo AS ti ON ti.ty_info_id = bti.ty_info_id
#WHERE ti.ty_info_id = 248
ORDER BY ti.ty_number,bti.base_time
LIMIT 10
;

# 分析
USE ty;
SELECT 
	ac.a_cont_id,
	ti.ty_name_en,
	ti.ty_name_tw,
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
LIMIT 10
;

# 預報
USE ty;
SELECT 
	fc.f_cont_id,
	ti.ty_name_en,
	ti.ty_name_tw,
	ti.ty_year,
	ti.ty_number,
	ci.centre,
	ci.model,
	fi.base_time,
	fc.valid_time,
	fc.lat,
	fc.lon,
	fc.min_pres
FROM ForecastInfo AS fi
INNER JOIN ForecastContent AS fc ON fc.f_info_id = fi.f_info_id
INNER JOIN CentreInfo AS ci ON ci.centre_info_id = fi.centre_info_id
INNER JOIN TyphoonInfo AS ti ON ti.ty_info_id = fi.ty_info_id
WHERE ci.centre = 'CWB'
ORDER BY ti.ty_number,ci.centre,ci.model,fi.base_time
LIMIT 10
;

# 系集
USE ty;
SELECT 
	ec.e_cont_id,
	ti.ty_name_en,
	ti.ty_name_tw,
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
WHERE ei.base_time >= '2018-04-01'
ORDER BY ei.member,ti.ty_number,ei.base_time
LIMIT 10
;