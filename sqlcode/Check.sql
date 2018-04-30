select distinct
	ty_info.ty_name_en,
	cen_info.centre,
	cen_info.model,
	xxx_info.base_time,
	xxx_con.lat,
	xxx_con.lon,
	xxx_con.min_pres,
	xxx_con.wind
from ty_analysisinfo as xxx_info
inner join ty_analysiscontent as xxx_con on xxx_con.a_info_id = xxx_info.a_info_id
inner join ty_centreinfo as cen_info on cen_info.centre_info_id = xxx_info.centre_info_id
inner join ty_typhooninfo  as ty_info on ty_info.ty_info_id = xxx_info.ty_info_id
order by ty_info.ty_name_en,xxx_info.base_time;

select distinct
	ty_info.ty_name_en,
	cen_info.centre,
	cen_info.model,
	xxx_info.base_time,
	xxx_con.valid_hour,
	xxx_con.lat,
	xxx_con.lon,
	xxx_con.min_pres,
	xxx_con.wind
from Ty_ForecastInfo as xxx_info
inner join Ty_ForecastContent as xxx_con on xxx_con.f_info_id = xxx_info.f_info_id
inner join ty_centreinfo as cen_info on cen_info.centre_info_id = xxx_info.centre_info_id
inner join ty_typhooninfo  as ty_info on ty_info.ty_info_id = xxx_info.ty_info_id
order by ty_info.ty_name_en,xxx_info.base_time;

select distinct
	ty_info.ty_name_en,
	cen_info.centre,
	cen_info.model,
	xxx_info.base_time,
	xxx_con.valid_hour,
	xxx_info.member,
	xxx_con.lat,
	xxx_con.lon,
	xxx_con.min_pres,
	xxx_con.wind
from ty_ensembleinfo as xxx_info
inner join ty_ensemblecontent as xxx_con on xxx_con.e_info_id = xxx_info.e_info_id
inner join ty_centreinfo as cen_info on cen_info.centre_info_id = xxx_info.centre_info_id
inner join ty_typhooninfo  as ty_info on ty_info.ty_info_id = xxx_info.ty_info_id
order by ty_info.ty_name_en,xxx_info.base_time,xxx_info.member,xxx_con.valid_hour;
