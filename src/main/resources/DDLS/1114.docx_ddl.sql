CREATE TABLE `1114` (
  `employee_id_id` VARCHAR(255) COMMENT '人员ID',
  `employee_number` VARCHAR(255) COMMENT '员工编号',
  `dept_qc_id` VARCHAR(255) COMMENT '组织全称',
  `employee_id` INT COMMENT '姓名',
  `dept_level1` VARCHAR(255) COMMENT '二级部门',
  `dept_level3` VARCHAR(255) COMMENT '三级部门',
  `dept_id_id` VARCHAR(255) COMMENT '组织ID',
  `employee_id_number` VARCHAR(255) COMMENT '编码',
  `depart_id` INT COMMENT '考勤组织',
  `position_id` INT COMMENT '考勤职位',
  `program_id` INT COMMENT '考勤方案',
  `begin_date` VARCHAR(255) COMMENT '生效时间',
  `operate_time` DATETIME COMMENT '同步/修改时间',
  `end_date` VARCHAR(255) COMMENT '失效时间',
  `source_id` INT COMMENT '来源ID',
  `source_type` INT COMMENT '来源类型',
  `modify_date` VARCHAR(255),
  `updater_id` VARCHAR(255) COMMENT '最后修改人',
  `statistic_data_work_onjob_days` VARCHAR(255) COMMENT '出勤天数',
  `employee_id_common_id` VARCHAR(255) COMMENT 'id',
  `statistic_data_work_outjob_days` VARCHAR(255) COMMENT '缺勤天数',
  `statistic_data_work_late_days` VARCHAR(255) COMMENT '迟到天数',
  `month_data_1` VARCHAR(255) COMMENT '1
四',
  `statistic_data_work_early_days` VARCHAR(255) COMMENT '早退天数',
  `statistic_data_work_absence_days` VARCHAR(255) COMMENT '旷工天数',
  `month_data_2` VARCHAR(255) COMMENT '2
五',
  `statistic_data_schedule_days` VARCHAR(255) COMMENT '排班天数(不包括排休)',
  `statistic_data_schedule_days_include_rest` VARCHAR(255) COMMENT '排班天数(包括排休)',
  `month_data_3` VARCHAR(255) COMMENT '3
六',
  `statistic_data_schedule_times` VARCHAR(255) COMMENT '排班工时',
  `month_data_4` VARCHAR(255) COMMENT '4
日',
  `month_data_5` VARCHAR(255) COMMENT '5
一',
  `month_data_6` VARCHAR(255) COMMENT '6
二',
  `month_data_7` VARCHAR(255) COMMENT '7
三',
  `month_data_8` VARCHAR(255) COMMENT '8
四',
  `month_data_9` VARCHAR(255) COMMENT '9
五',
  `month_data_10` VARCHAR(255) COMMENT '10
六',
  `month_data_11` VARCHAR(255) COMMENT '11
日',
  `month_data_12` VARCHAR(255) COMMENT '12
一',
  `month_data_13` VARCHAR(255) COMMENT '13
二',
  `month_data_14` VARCHAR(255) COMMENT '14
三',
  `month_data_15` VARCHAR(255) COMMENT '15
四',
  `month_data_16` VARCHAR(255) COMMENT '16
五',
  `month_data_17` VARCHAR(255) COMMENT '17
六',
  `month_data_18` VARCHAR(255) COMMENT '18
日',
  `month_data_19` VARCHAR(255) COMMENT '19
一',
  `month_data_20` VARCHAR(255) COMMENT '20
二',
  `month_data_2021-12-01` VARCHAR(255) COMMENT '01
三',
  `month_data_2022-01-01` VARCHAR(255) COMMENT '01
六',
  `month_data_21` VARCHAR(255) COMMENT '21
三',
  `month_data_22` VARCHAR(255) COMMENT '22
四',
  `month_data_23` VARCHAR(255) COMMENT '23
五',
  `month_data_24` VARCHAR(255) COMMENT '24
六',
  `month_data_25` VARCHAR(255) COMMENT '25
日',
  `month_data_26` VARCHAR(255) COMMENT '26
一',
  `month_data_27` VARCHAR(255) COMMENT '27
二',
  `month_data_28` VARCHAR(255) COMMENT '28
三',
  `month_data_29` VARCHAR(255) COMMENT '29
四',
  `month_data_30` VARCHAR(255) COMMENT '30
五',
  `month_data_2021-12-02` VARCHAR(255) COMMENT '02
四',
  `month_data_2022-01-02` VARCHAR(255) COMMENT '02
日',
  `month_data_31` VARCHAR(255) COMMENT '31
四',
  `month_data_2021-12-03` VARCHAR(255) COMMENT '03
五',
  `month_data_2022-01-03` VARCHAR(255) COMMENT '03
一',
  `month_data_2021-12-04` VARCHAR(255) COMMENT '04
六',
  `month_data_2022-01-04` VARCHAR(255) COMMENT '04
二',
  `month_data_2021-12-05` VARCHAR(255) COMMENT '05
日',
  `month_data_2022-01-05` VARCHAR(255) COMMENT '05
三',
  `month_data_2021-12-06` VARCHAR(255) COMMENT '06
一',
  `month_data_2022-01-06` VARCHAR(255) COMMENT '06
四',
  `month_data_2021-12-07` VARCHAR(255) COMMENT '07
二',
  `month_data_2022-01-07` VARCHAR(255) COMMENT '07
五',
  `month_data_2021-12-08` VARCHAR(255) COMMENT '08
三',
  `month_data_2022-01-08` VARCHAR(255) COMMENT '08
六',
  `month_data_2021-12-09` VARCHAR(255) COMMENT '09
四',
  `month_data_2022-01-09` VARCHAR(255) COMMENT '09
日',
  `month_data_2021-12-10` VARCHAR(255) COMMENT '10
五',
  `month_data_2022-01-10` VARCHAR(255) COMMENT '10
一',
  `month_data_2021-12-11` VARCHAR(255) COMMENT '11
六',
  `month_data_2022-01-11` VARCHAR(255) COMMENT '11
二',
  `month_data_2021-12-12` VARCHAR(255) COMMENT '12
日',
  `month_data_2022-01-12` VARCHAR(255) COMMENT '12
三',
  `month_data_2021-12-13` VARCHAR(255) COMMENT '13
一',
  `month_data_2022-01-13` VARCHAR(255) COMMENT '13
四',
  `month_data_2021-12-14` VARCHAR(255) COMMENT '14
二',
  `month_data_2022-01-14` VARCHAR(255) COMMENT '14
五',
  `month_data_2021-12-15` VARCHAR(255) COMMENT '15
三',
  `month_data_2022-01-15` VARCHAR(255) COMMENT '15
六',
  `month_data_2021-12-16` VARCHAR(255) COMMENT '16
四',
  `month_data_2022-01-16` VARCHAR(255) COMMENT '16
日',
  `month_data_2021-12-17` VARCHAR(255) COMMENT '17
五',
  `month_data_2022-01-17` VARCHAR(255) COMMENT '17
一',
  `month_data_2021-12-18` VARCHAR(255) COMMENT '18
六',
  `month_data_2022-01-18` VARCHAR(255) COMMENT '18
二',
  `month_data_2021-12-19` VARCHAR(255) COMMENT '19
日',
  `month_data_2022-01-19` VARCHAR(255) COMMENT '19
三',
  `month_data_2021-12-20` VARCHAR(255) COMMENT '20
一',
  `month_data_2022-01-20` VARCHAR(255) COMMENT '20
四',
  `month_data_2021-12-21` VARCHAR(255) COMMENT '21
二',
  `month_data_2022-01-21` VARCHAR(255) COMMENT '21
五',
  `month_data_2021-12-22` VARCHAR(255) COMMENT '22
三',
  `month_data_2022-01-22` VARCHAR(255) COMMENT '22
六',
  `month_data_2021-12-23` VARCHAR(255) COMMENT '23
四',
  `month_data_2022-01-23` VARCHAR(255) COMMENT '23
日',
  `month_data_2021-12-24` VARCHAR(255) COMMENT '24
五',
  `month_data_2022-01-24` VARCHAR(255) COMMENT '24
一',
  `month_data_2021-12-25` VARCHAR(255) COMMENT '25
六',
  `month_data_2022-01-25` VARCHAR(255) COMMENT '25
二',
  `month_data_2021-12-26` VARCHAR(255) COMMENT '26
日',
  `month_data_2022-01-26` VARCHAR(255) COMMENT '26
三',
  `month_data_2021-12-27` VARCHAR(255) COMMENT '27
一',
  `month_data_2022-01-27` VARCHAR(255) COMMENT '27
四',
  `month_data_2021-12-28` VARCHAR(255) COMMENT '28
二',
  `month_data_2022-01-28` VARCHAR(255) COMMENT '28
五',
  `month_data_2021-12-29` VARCHAR(255) COMMENT '29
三',
  `month_data_2022-01-29` VARCHAR(255) COMMENT '29
六',
  `month_data_2021-12-30` VARCHAR(255) COMMENT '30
四',
  `month_data_2022-01-30` VARCHAR(255) COMMENT '30
日',
  `month_data_2021-12-31` VARCHAR(255) COMMENT '31
五',
  `month_data_2022-01-31` VARCHAR(255) COMMENT '31
一',
  `statistic_data_user_define_chidao` VARCHAR(255) COMMENT '迟到时数',
  `statistic_data_user_define_zaotui` VARCHAR(255) COMMENT '早退时数',
  `statistic_data_user_define_xxx` VARCHAR(255) COMMENT '轮班小夜班',
  `statistic_data_user_define_cbbdyb` VARCHAR(255) COMMENT '轮班大夜班',
  `statistic_data_user_define_dgjynn` VARCHAR(255) COMMENT '一年内待工假',
  `statistic_data_user_define_dgjynw` VARCHAR(255) COMMENT '一年外待工假',
  `statistic_data_user_define_lgynbj` VARCHAR(255) COMMENT '六个月内病假',
  `statistic_data_user_define_lgywbj` VARCHAR(255) COMMENT '六个月外病假',
  `statistic_data_user_define_lbjb` VARCHAR(255) COMMENT '轮班加班',
  `statistic_data_work_onjob` VARCHAR(255) COMMENT '出勤',
  `statistic_data_work_rest` VARCHAR(255) COMMENT '休息',
  `statistic_data_work_outjob` VARCHAR(255) COMMENT '缺勤',
  `statistic_data_work_late` VARCHAR(255) COMMENT '迟到',
  `statistic_data_work_early` VARCHAR(255) COMMENT '早退',
  `statistic_data_work_absence` VARCHAR(255) COMMENT '旷工',
  `statistic_data_leave_normal` VARCHAR(255) COMMENT '事假',
  `statistic_data_leave_annual` VARCHAR(255) COMMENT '年休假',
  `statistic_data_leave_sick` VARCHAR(255) COMMENT '病假',
  `statistic_data_leave_maternity` VARCHAR(255) COMMENT '产假',
  `statistic_data_leave_antenatal` VARCHAR(255) COMMENT '孕检假',
  `statistic_data_leave_care` VARCHAR(255) COMMENT '护理假',
  `statistic_data_leave_funeral` VARCHAR(255) COMMENT '丧假',
  `statistic_data_leave_home` VARCHAR(255) COMMENT '探亲假',
  `statistic_data_leave_injury` VARCHAR(255) COMMENT '工伤假',
  `statistic_data_leave_feeding` VARCHAR(255) COMMENT '哺乳假',
  `statistic_data_leave_marital` VARCHAR(255) COMMENT '婚假',
  `statistic_data_leave_shift` VARCHAR(255) COMMENT '补休假',
  `statistic_data_overtime_normal` VARCHAR(255) COMMENT '平加',
  `statistic_data_overtime_rest` VARCHAR(255) COMMENT '休加',
  `statistic_data_overtime_holiday` VARCHAR(255) COMMENT '假加',
  `statistic_data_trip_normal` VARCHAR(255) COMMENT '外出',
  `statistic_data_trip_business` VARCHAR(255) COMMENT '出差',
  `statistic_data_leave_gestation` VARCHAR(255) COMMENT '护理父母假',
  `statistic_data_leave_familyplan` VARCHAR(255) COMMENT '倒班假',
  `statistic_data_leave_abortion` VARCHAR(255) COMMENT '流产假',
  `statistic_data_leave_pregnancy` VARCHAR(255) COMMENT '妊娠假',
  `statistic_data_leave_paternity` VARCHAR(255) COMMENT '幼儿看护假',
  `statistic_data_leave_nursing` VARCHAR(255) COMMENT '倒班节日增修假',
  `statistic_data_leave_statutory` VARCHAR(255) COMMENT '公假',
  `statistic_data_overtime_oncall` VARCHAR(255) COMMENT '值班',
  `statistic_data_leave_childcare` VARCHAR(255) COMMENT '待工假',
  `statistic_data_leave_pre_maternity` VARCHAR(255) COMMENT '产前假',
  `statistic_data_trip_abroad` VARCHAR(255) COMMENT '国外出差',
  `statistic_data_trip_user_defined1` VARCHAR(255) COMMENT '自定义出差1',
  `statistic_data_trip_user_defined2` VARCHAR(255) COMMENT '自定义出差2',
  `statistic_data_leave_user_defined` VARCHAR(255) COMMENT '癌症、精神病（医疗期内）',
  `statistic_data_leave_user_defined1` VARCHAR(255) COMMENT '癌症、精神病（医疗期满）',
  `statistic_data_leave_user_defined2` VARCHAR(255) COMMENT '工伤康复治疗假',
  `statistic_data_leave_user_defined4` VARCHAR(255) COMMENT '工伤申报假',
  `statistic_data_trip_user_defined3` VARCHAR(255) COMMENT '自定义出差3',
  `statistic_data_trip_user_defined4` VARCHAR(255) COMMENT '自定义出差4',
  `statistic_data_trip_user_defined5` VARCHAR(255) COMMENT '自定义出差5',
  `statistic_data_trip_user_defined6` VARCHAR(255) COMMENT '自定义出差6',
  `statistic_data_trip_user_defined7` VARCHAR(255) COMMENT '自定义出差7',
  `statistic_data_user_define_kgsc` VARCHAR(255) COMMENT '真实旷工时长',
  `statistic_data_user_define_zsjb` VARCHAR(255) COMMENT '真实加班',
  `origin_employee_id` VARCHAR(255) COMMENT '人员id',
  `origin_depart_id` VARCHAR(255) COMMENT '组织id',
  `origin_position_id` VARCHAR(255) COMMENT '岗位id',
  `origin_program_id` VARCHAR(255) COMMENT '方案id',
  `statistic_data_leave_user_defined3` VARCHAR(255) COMMENT '自定义休假3',
  `statistic_data_leave_user_defined5` VARCHAR(255) COMMENT '自定义休假5',
  `statistic_data_leave_user_defined6` VARCHAR(255) COMMENT '自定义休假6',
  `statistic_data_leave_user_defined7` VARCHAR(255) COMMENT '自定义休假7',
  `statistic_data_leave_user_defined8` VARCHAR(255) COMMENT '自定义休假8',
  `statistic_data_leave_user_defined9` VARCHAR(255) COMMENT '自定义休假9',
  `statistic_data_leave_user_defined10` VARCHAR(255) COMMENT '自定义休假10',
  `statistic_data_leave_user_defined11` VARCHAR(255) COMMENT '自定义休假11',
  `statistic_data_leave_user_defined12` VARCHAR(255) COMMENT '自定义休假12',
  `group_id` INT COMMENT '考勤组',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='1114';
