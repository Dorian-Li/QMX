-- 创建库
create database if not exists QMXdatabase;

-- 切换库
use QMXdatabase;

-- -- 碳块喷涂情况
-- create table if not exists TK
-- (
--     id         bigint auto_increment comment 'id' primary key,
--     devName         varchar(50)                     not null  comment '设备名称',
--     numHourly        int                             not null comment '每小时喷涂数量',
--     numDaily         int                             not null comment '每日喷涂数量',
--     time       datetime default CURRENT_TIMESTAMP not null comment '创建时间'
-- );


-- -- 碳块锁定数据
-- create table if not exists SD
-- (
--     id         bigint auto_increment comment 'id' primary key,
--     devName         varchar(50)                     not null  comment '设备名称',
--     status          int                             not null comment '工作状态',
--     time       datetime default CURRENT_TIMESTAMP not null comment '创建时间'

-- );

-- -- 喷涂数据
-- create table if not exists PT
-- (
--     id         bigint auto_increment comment 'id' primary key,
--     devName         varchar(50)                     not null  comment '设备名称',
--     status          int                             not null comment '工作状态',
--     pressure_dev    double                             not null comment  '喷涂机压力',
--     pressure_tube   double                             not null comment  '喷涂管路压力',
--     time       datetime default CURRENT_TIMESTAMP not null comment '创建时间'

-- );

-- -- 喷涂房数据
-- create table if not exists PTF
-- (
--     id         bigint auto_increment comment 'id' primary key,
--     devName         varchar(50)                     not null  comment '设备名称',
--     status          int                             not null comment '工作状态',
--     time       datetime default CURRENT_TIMESTAMP not null comment '创建时间'

-- );

-- -- 上料数据
-- create table if not exists SL
-- (
--     id         bigint auto_increment comment 'id' primary key,
--     devName         varchar(50)                     not null  comment '设备名称',
--     status           int                             not null comment '工作状态',
--     level            double                             not null comment '料筒液位',
--     pressure         double                             not null comment '上料管路压力',
--     time       datetime default CURRENT_TIMESTAMP not null comment '创建时间'
-- );

-- -- 喷涂质量检测数据
-- create table if not exists ZLJC
-- (
--     id         bigint auto_increment comment 'id' primary key,
--     status           int                             not null comment '工作状态',
--     targetNum        int                             not null comment '喷涂目标数量',
--     completeNum      int                             not null comment '喷涂完成块数',
--     passNum          int                             not null comment '喷涂合格块数',
--     failNum          int                             not null comment '喷涂不合格块数',
--     rate            double                             not null comment '喷涂合格率',
--     time       datetime default CURRENT_TIMESTAMP not null comment '创建时间'
-- );


--  类型表
-- 0x01 设备状态
create table if not exists device_status(
    id        bigint primary key auto_increment,
    devName   varchar(64)                     not null comment '设备名',
    status    int                             not null comment '设备状态：0/1',
    time      datetime default current_timestamp not null comment '采集时间'
) engine=InnoDB default charset=utf8mb4 comment='0x01 设备状态';

create index idx_device_status_dev_time on device_status(devName, time);

-- 0x02 传感器
create table if not exists sensor(
    id           bigint primary key auto_increment,
    devName      varchar(64)                     not null comment '设备名',
    value        double                          null     comment '指标值',
    time         datetime default current_timestamp not null comment '采集时间'
) engine=InnoDB default charset=utf8mb4 comment='0x02 实时传感器（KV）';

create index idx_sensor_dev_time on sensor(devName, time);

-- 0x03 喷涂情况
create table if not exists spray_record (
    id           bigint primary key auto_increment,
    devName      varchar(64)                     null     comment '设备',
    stage        int                             null     comment '喷涂阶段',
    rate         double                          null     comment '喷涂合格率',
    time         datetime default current_timestamp not null comment '采集时间'
) engine=InnoDB default charset=utf8mb4 comment='0x03 喷涂记录';

create index idx_spray_record_dev_time on spray_record(devName, time);

-- 0x04 喷涂产量（小时）
create table if not exists product_hourly(
    id         bigint primary key auto_increment,
    numHourly      int                         null     comment '该小时产量',
    time       datetime default current_timestamp not null comment '采集时间'
) engine=InnoDB default charset=utf8mb4 comment='0x04 小时产量明细';

-- 0x04 喷涂产量（周）
create table if not exists product_week(
    id         bigint primary key auto_increment,
    numWeekly  int                             null     comment '该周产量',
    time       datetime default current_timestamp not null comment '采集时间'
) engine=InnoDB default charset=utf8mb4 comment='0x04 周产量明细';


-- 0x05 控制参数（KV）
-- create table if not exists control_param_05 (
--     id          bigint primary key auto_increment,
--     devName     varchar(64)                     not null,
--     param_key   varchar(64)                     not null,
--     param_value double                          null,
--     batch_id    varchar(64)                     null     comment '批次ID（同一包内的关联ID）',
--     time        datetime default current_timestamp not null
-- ) engine=InnoDB default charset=utf8mb4 comment='0x05 控制参数（KV）';

-- create index idx_control_param_dev_key_time on control_param_05(devName, param_key, time);
-- create index idx_control_param_batch        on control_param_05(batch_id);

-- 0x06 运动参数（KV）
-- create table if not exists motion_param_06 (
--     id          bigint primary key auto_increment,
--     devName     varchar(64)                     not null,
--     param_key   varchar(64)                     not null,
--     param_value double                          null,
--     batch_id    varchar(64)                     null     comment '批次ID（同一包内的关联ID）',
--     time        datetime default current_timestamp not null
-- ) engine=InnoDB default charset=utf8mb4 comment='0x06 运动参数（KV）';

-- create index idx_motion_param_dev_key_time on motion_param_06(devName, param_key, time);
-- create index idx_motion_param_batch        on motion_param_06(batch_id);

