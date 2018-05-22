# 分析
SELECT *,ci.centre
FROM AnalysisContent AS c
INNER JOIN AnalysisInfo AS i ON i.a_info_id = c.a_info_id
INNER JOIN CentreInfo AS ci ON ci.centre_info_id = i.centre_info_id
WHERE i.centre_info_id IN (11,12,13,14)
;
# 預報
SELECT *,ci.centre
FROM ForecastContent AS c
INNER JOIN ForecastInfo AS i ON i.f_info_id = c.f_info_id
INNER JOIN CentreInfo AS ci ON ci.centre_info_id = i.centre_info_id
WHERE i.centre_info_id IN (11,12,13,14)
;
# 系集
SELECT *,ci.centre
FROM EnsembleContent AS c
INNER JOIN EnsembleInfo AS i ON i.e_info_id = c.e_info_id
INNER JOIN CentreInfo AS ci ON ci.centre_info_id = i.centre_info_id
WHERE ci.centre = 'NCEP'

SELECT *,ci.centre
FROM EnsembleInfo AS i
INNER JOIN CentreInfo AS ci ON ci.centre_info_id = i.centre_info_id
WHERE ci.centre = 'NCEP'
; 

# 刪除
-- START TRANSACTION;
-- DELETE c.*
-- FROM EnsembleContent AS c
-- INNER JOIN EnsembleInfo AS i ON i.e_info_id = c.e_info_id
-- INNER JOIN CentreInfo AS ci ON ci.centre_info_id = i.centre_info_id
-- WHERE ci.centre = 'NCEP'
-- COMMIT ;
-- ROLLBACK
-- ;

-- START TRANSACTION;
-- DELETE i.*
-- FROM EnsembleInfo AS i
-- INNER JOIN CentreInfo AS ci ON ci.centre_info_id = i.centre_info_id
-- WHERE ci.centre = 'NCEP'
-- COMMIT ;
-- ROLLBACK
-- ;


