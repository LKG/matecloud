-- =============================================================================
-- V1.6.1 — mate-crm 初始化 Schema
--
-- CRM 销售全流程 18 张表(前缀 mate_crm_,可插拔规范 §1: 只 DDL 自己的表)。
-- 主线: 线索 → 客户(公海/私海) → 联系人/跟进 → 商机(阶段漏斗) → 合同(审批)
--       → 回款计划/回款 → 发票 → 售后工单; 辅助: 产品/审批流/待办/公海规则。
-- 无物理外键(应用层维护关系), 关系字段一律带冗余 name 便于列表免 join。
-- owner_id/dept_id 为数据权限(本人/部门/全部)过滤列; creator_id 为审计列。
-- =============================================================================

-- ---- 1. 线索 ----------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `mate_crm_lead` (
    `id`                    VARCHAR(64)  NOT NULL,
    `tenant_id`             VARCHAR(32)  NOT NULL DEFAULT '1' COMMENT 'Tenant ID',
    `lead_name`             VARCHAR(128) NOT NULL COMMENT '线索名称(公司/活动名)',
    `contact_name`          VARCHAR(64)  NOT NULL COMMENT '联系人姓名',
    `mobile`                VARCHAR(32)  DEFAULT NULL COMMENT '手机号',
    `telephone`             VARCHAR(32)  DEFAULT NULL COMMENT '电话',
    `email`                 VARCHAR(128) DEFAULT NULL COMMENT '邮箱',
    `industry`              VARCHAR(64)  DEFAULT NULL COMMENT '行业',
    `source`                VARCHAR(32)  NOT NULL DEFAULT 'OTHER' COMMENT '线索来源: SEARCH/OFFICIAL_WEBSITE/EXHIBITION/REFERRAL/PHONE/OTHER',
    `status`                VARCHAR(32)  NOT NULL DEFAULT 'FOLLOWING' COMMENT '状态: FOLLOWING跟进中/CONVERTED已转化/INVALID已废弃',
    `address`               VARCHAR(256) DEFAULT NULL COMMENT '地址',
    `remark`                VARCHAR(512) DEFAULT NULL COMMENT '备注',
    `next_time`             DATETIME     DEFAULT NULL COMMENT '下次跟进时间',
    `converted_customer_id` VARCHAR(64)  DEFAULT NULL COMMENT '转化后客户ID',
    `owner_id`              VARCHAR(64)  DEFAULT NULL COMMENT '负责人ID',
    `owner_name`            VARCHAR(64)  DEFAULT NULL COMMENT '负责人姓名',
    `dept_id`               VARCHAR(32)  DEFAULT NULL COMMENT '负责人部门ID(数据权限)',
    `creator_id`            VARCHAR(64)  DEFAULT NULL COMMENT '创建人ID',
    `creator_name`          VARCHAR(64)  DEFAULT NULL COMMENT '创建人姓名',
    `created_at`            DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`            DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`               TINYINT  NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_lead_status` (`status`),
    KEY `idx_lead_mobile` (`mobile`),
    KEY `idx_lead_owner` (`owner_id`),
    KEY `idx_lead_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='CRM 线索';

-- ---- 2. 客户 ----------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `mate_crm_customer` (
    `id`                  VARCHAR(64)  NOT NULL,
    `tenant_id`           VARCHAR(32)  NOT NULL DEFAULT '1' COMMENT 'Tenant ID',
    `customer_name`       VARCHAR(128) NOT NULL COMMENT '客户名称(查重键)',
    `industry`            VARCHAR(64)  DEFAULT NULL COMMENT '行业',
    `level`               VARCHAR(8)   DEFAULT 'C' COMMENT '客户等级: A重点/B普通/C其他',
    `source`              VARCHAR(32)  DEFAULT 'OTHER' COMMENT '客户来源',
    `status`              VARCHAR(32)  NOT NULL DEFAULT 'INITIAL' COMMENT '合作状态: INITIAL初步接触/NEGOTIATING洽谈中/OPPORTUNITY机会成单/DORMANT沉睡/CHURNED流失',
    `phone`               VARCHAR(32)  DEFAULT NULL COMMENT '联系电话(查重键)',
    `address`             VARCHAR(256) DEFAULT NULL COMMENT '详细地址',
    `website`             VARCHAR(128) DEFAULT NULL COMMENT '网址',
    `remark`              VARCHAR(512) DEFAULT NULL COMMENT '备注',
    `pool_status`         TINYINT      NOT NULL DEFAULT 0 COMMENT '1=公海 0=私海',
    `owner_id`            VARCHAR(64)  DEFAULT NULL COMMENT '负责人ID(公海为空)',
    `owner_name`          VARCHAR(64)  DEFAULT NULL COMMENT '负责人姓名',
    `dept_id`             VARCHAR(32)  DEFAULT NULL COMMENT '负责人部门ID(数据权限)',
    `claim_time`          DATETIME     DEFAULT NULL COMMENT '领取时间',
    `to_pool_time`        DATETIME     DEFAULT NULL COMMENT '进入公海时间',
    `to_pool_reason`      VARCHAR(256) DEFAULT NULL COMMENT '进入公海原因(释放/回收)',
    `recent_follow_time`  DATETIME     DEFAULT NULL COMMENT '最近跟进时间(公海回收依据)',
    `next_time`           DATETIME     DEFAULT NULL COMMENT '下次跟进时间',
    `creator_id`          VARCHAR(64)  DEFAULT NULL COMMENT '创建人ID',
    `creator_name`        VARCHAR(64)  DEFAULT NULL COMMENT '创建人姓名',
    `lock_version`        INT          NOT NULL DEFAULT 0 COMMENT '乐观锁',
    `created_at`          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`             TINYINT  NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_cust_name` (`customer_name`),
    KEY `idx_cust_phone` (`phone`),
    KEY `idx_cust_pool_owner` (`pool_status`, `owner_id`),
    KEY `idx_cust_follow` (`recent_follow_time`),
    KEY `idx_cust_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='CRM 客户(含公海/私海)';

-- ---- 3. 客户公海规则(单行配置) ------------------------------------------------
CREATE TABLE IF NOT EXISTS `mate_crm_pool_config` (
    `id`            VARCHAR(64) NOT NULL,
    `tenant_id`     VARCHAR(32) NOT NULL DEFAULT '1' COMMENT 'Tenant ID',
    `enabled`       TINYINT     NOT NULL DEFAULT 1 COMMENT '自动回收开关',
    `recycle_days`  INT         NOT NULL DEFAULT 30 COMMENT '私海客户 N 天无跟进自动回收公海',
    `max_hold`      INT         NOT NULL DEFAULT 200 COMMENT '单人私海客户上限(0=不限)',
    `remind_days`   INT         NOT NULL DEFAULT 7 COMMENT '回收前 N 天提醒',
    `created_at`    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`       TINYINT  NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_pool_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='CRM 客户公海规则配置';

-- ---- 4. 联系人 ----------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `mate_crm_contact` (
    `id`            VARCHAR(64)  NOT NULL,
    `tenant_id`     VARCHAR(32)  NOT NULL DEFAULT '1' COMMENT 'Tenant ID',
    `customer_id`   VARCHAR(64)  NOT NULL COMMENT '所属客户ID',
    `customer_name` VARCHAR(128) DEFAULT NULL COMMENT '所属客户名称(冗余)',
    `contact_name`  VARCHAR(64)  NOT NULL COMMENT '联系人姓名',
    `mobile`        VARCHAR(32)  DEFAULT NULL COMMENT '手机号',
    `telephone`     VARCHAR(32)  DEFAULT NULL COMMENT '电话',
    `email`         VARCHAR(128) DEFAULT NULL COMMENT '邮箱',
    `position`      VARCHAR(64)  DEFAULT NULL COMMENT '职务',
    `gender`        VARCHAR(8)   DEFAULT NULL COMMENT '性别: MALE/FEMALE',
    `is_primary`    TINYINT      NOT NULL DEFAULT 0 COMMENT '是否主联系人',
    `wechat`        VARCHAR(64)  DEFAULT NULL COMMENT '微信',
    `remark`        VARCHAR(512) DEFAULT NULL COMMENT '备注',
    `owner_id`      VARCHAR(64)  DEFAULT NULL COMMENT '负责人ID',
    `owner_name`    VARCHAR(64)  DEFAULT NULL COMMENT '负责人姓名',
    `dept_id`       VARCHAR(32)  DEFAULT NULL COMMENT '负责人部门ID(数据权限)',
    `creator_id`    VARCHAR(64)  DEFAULT NULL COMMENT '创建人ID',
    `creator_name`  VARCHAR(64)  DEFAULT NULL COMMENT '创建人姓名',
    `created_at`    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`       TINYINT  NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_contact_customer` (`customer_id`),
    KEY `idx_contact_mobile` (`mobile`),
    KEY `idx_contact_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='CRM 联系人';

-- ---- 5. 跟进记录 --------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `mate_crm_follow_up` (
    `id`           VARCHAR(64)  NOT NULL,
    `tenant_id`    VARCHAR(32)  NOT NULL DEFAULT '1' COMMENT 'Tenant ID',
    `biz_type`     VARCHAR(32)  NOT NULL COMMENT '跟进对象类型: LEAD/CUSTOMER/CONTACT/BUSINESS',
    `biz_id`       VARCHAR(64)  NOT NULL COMMENT '跟进对象ID',
    `biz_name`     VARCHAR(128) DEFAULT NULL COMMENT '跟进对象名称(冗余)',
    `customer_id`  VARCHAR(64)  DEFAULT NULL COMMENT '归属客户ID(公海回收统计用)',
    `content`      VARCHAR(2048) NOT NULL COMMENT '跟进内容',
    `follow_type`  VARCHAR(32)  NOT NULL DEFAULT 'PHONE' COMMENT '跟进方式: PHONE/VISIT/WECHAT/EMAIL/MEETING/OTHER',
    `next_content` VARCHAR(512) DEFAULT NULL COMMENT '下次跟进计划',
    `next_time`    DATETIME     DEFAULT NULL COMMENT '下次跟进时间',
    `user_id`      VARCHAR(64)  DEFAULT NULL COMMENT '记录人ID',
    `user_name`    VARCHAR(64)  DEFAULT NULL COMMENT '记录人姓名',
    `dept_id`      VARCHAR(32)  DEFAULT NULL COMMENT '记录人部门ID(数据权限)',
    `created_at`   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`      TINYINT  NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_follow_biz` (`biz_type`, `biz_id`),
    KEY `idx_follow_customer` (`customer_id`),
    KEY `idx_follow_user` (`user_id`),
    KEY `idx_follow_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='CRM 跟进记录';

-- ---- 6. 产品/价格表 -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS `mate_crm_product` (
    `id`           VARCHAR(64)   NOT NULL,
    `tenant_id`    VARCHAR(32)   NOT NULL DEFAULT '1' COMMENT 'Tenant ID',
    `product_name` VARCHAR(128)  NOT NULL COMMENT '产品名称',
    `product_code` VARCHAR(64)   DEFAULT NULL COMMENT '产品编号',
    `category`     VARCHAR(64)   DEFAULT NULL COMMENT '分类',
    `price`        DECIMAL(18,2) DEFAULT 0.00 COMMENT '标准售价',
    `unit`         VARCHAR(32)   DEFAULT NULL COMMENT '单位',
    `status`       TINYINT       NOT NULL DEFAULT 1 COMMENT '1=上架 0=下架',
    `description`  VARCHAR(1024) DEFAULT NULL COMMENT '产品描述',
    `sort`         INT           DEFAULT 0 COMMENT '排序',
    `creator_id`   VARCHAR(64)   DEFAULT NULL COMMENT '创建人ID',
    `created_at`   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`      TINYINT  NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_product_code` (`product_code`),
    KEY `idx_product_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='CRM 产品/价格表';

-- ---- 7. 商机 ------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `mate_crm_business` (
    `id`            VARCHAR(64)   NOT NULL,
    `tenant_id`     VARCHAR(32)   NOT NULL DEFAULT '1' COMMENT 'Tenant ID',
    `business_name` VARCHAR(128)  NOT NULL COMMENT '商机名称',
    `customer_id`   VARCHAR(64)   NOT NULL COMMENT '所属客户ID',
    `customer_name` VARCHAR(128)  DEFAULT NULL COMMENT '所属客户名称(冗余)',
    `contact_name`  VARCHAR(64)   DEFAULT NULL COMMENT '对接联系人(冗余)',
    `source`        VARCHAR(32)   DEFAULT NULL COMMENT '商机来源',
    `stage`         VARCHAR(32)   NOT NULL DEFAULT 'INITIAL' COMMENT '阶段: INITIAL初步接触/DEMAND需求确认/PLAN方案报价/NEGOTIATION商务谈判/WIN赢单/LOSE输单',
    `status_flag`   TINYINT       NOT NULL DEFAULT 1 COMMENT '1=进行中 0=已结束',
    `money`         DECIMAL(18,2) DEFAULT 0.00 COMMENT '预计金额',
    `deal_prob`     INT           DEFAULT 50 COMMENT '赢率%',
    `deal_date`     DATE          DEFAULT NULL COMMENT '预计成交日期',
    `next_step`     VARCHAR(512)  DEFAULT NULL COMMENT '下一步计划',
    `end_remark`    VARCHAR(512)  DEFAULT NULL COMMENT '结束备注(赢单/输单原因)',
    `remark`        VARCHAR(512)  DEFAULT NULL COMMENT '备注',
    `owner_id`      VARCHAR(64)   DEFAULT NULL COMMENT '负责人ID',
    `owner_name`    VARCHAR(64)   DEFAULT NULL COMMENT '负责人姓名',
    `dept_id`       VARCHAR(32)   DEFAULT NULL COMMENT '负责人部门ID(数据权限)',
    `creator_id`    VARCHAR(64)   DEFAULT NULL COMMENT '创建人ID',
    `creator_name`  VARCHAR(64)   DEFAULT NULL COMMENT '创建人姓名',
    `created_at`    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`       TINYINT  NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_business_customer` (`customer_id`),
    KEY `idx_business_stage` (`stage`),
    KEY `idx_business_owner` (`owner_id`),
    KEY `idx_business_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='CRM 商机(阶段漏斗)';

-- ---- 8. 商机产品行 ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `mate_crm_business_product` (
    `id`           VARCHAR(64)   NOT NULL,
    `tenant_id`    VARCHAR(32)   NOT NULL DEFAULT '1' COMMENT 'Tenant ID',
    `business_id`  VARCHAR(64)   NOT NULL COMMENT '商机ID',
    `product_id`   VARCHAR(64)   NOT NULL COMMENT '产品ID',
    `product_name` VARCHAR(128)  DEFAULT NULL COMMENT '产品名称(冗余)',
    `unit_price`   DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '单价',
    `quantity`     DECIMAL(18,4) NOT NULL DEFAULT 1 COMMENT '数量',
    `subtotal`     DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '小计(单价*数量)',
    `created_at`   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`      TINYINT  NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_bp_business` (`business_id`),
    KEY `idx_bp_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='CRM 商机产品行';

-- ---- 9. 合同 ------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `mate_crm_contract` (
    `id`             VARCHAR(64)   NOT NULL,
    `tenant_id`      VARCHAR(32)   NOT NULL DEFAULT '1' COMMENT 'Tenant ID',
    `contract_num`   VARCHAR(64)   NOT NULL COMMENT '合同编号(唯一)',
    `contract_name`  VARCHAR(128)  NOT NULL COMMENT '合同名称',
    `customer_id`    VARCHAR(64)   NOT NULL COMMENT '客户ID',
    `customer_name`  VARCHAR(128)  DEFAULT NULL COMMENT '客户名称(冗余)',
    `business_id`    VARCHAR(64)   DEFAULT NULL COMMENT '来源商机ID',
    `contact_name`   VARCHAR(64)   DEFAULT NULL COMMENT '签约联系人(冗余)',
    `money`          DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '合同总额(=产品行合计)',
    `received_money` DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '累计已回款',
    `start_time`     DATE          DEFAULT NULL COMMENT '合同开始日期',
    `end_time`       DATE          DEFAULT NULL COMMENT '合同到期日期',
    `sign_time`      DATE          DEFAULT NULL COMMENT '签约日期',
    `status`         VARCHAR(32)   NOT NULL DEFAULT 'DRAFT' COMMENT '状态: DRAFT草稿/PENDING审批中/APPROVED已通过/REJECTED已驳回/ARCHIVED已归档',
    `remark`         VARCHAR(512)  DEFAULT NULL COMMENT '备注',
    `attachment`     VARCHAR(512)  DEFAULT NULL COMMENT '合同附件URL',
    `owner_id`       VARCHAR(64)   DEFAULT NULL COMMENT '负责人ID',
    `owner_name`     VARCHAR(64)   DEFAULT NULL COMMENT '负责人姓名',
    `dept_id`        VARCHAR(32)   DEFAULT NULL COMMENT '负责人部门ID(数据权限)',
    `creator_id`     VARCHAR(64)   DEFAULT NULL COMMENT '创建人ID',
    `creator_name`   VARCHAR(64)   DEFAULT NULL COMMENT '创建人姓名',
    `lock_version`   INT           NOT NULL DEFAULT 0 COMMENT '乐观锁',
    `created_at`     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`        TINYINT  NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_contract_num` (`contract_num`),
    KEY `idx_contract_customer` (`customer_id`),
    KEY `idx_contract_status` (`status`),
    KEY `idx_contract_owner` (`owner_id`),
    KEY `idx_contract_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='CRM 合同';

-- ---- 10. 合同产品行 -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS `mate_crm_contract_product` (
    `id`           VARCHAR(64)   NOT NULL,
    `tenant_id`    VARCHAR(32)   NOT NULL DEFAULT '1' COMMENT 'Tenant ID',
    `contract_id`  VARCHAR(64)   NOT NULL COMMENT '合同ID',
    `product_id`   VARCHAR(64)   NOT NULL COMMENT '产品ID',
    `product_name` VARCHAR(128)  DEFAULT NULL COMMENT '产品名称(冗余)',
    `unit_price`   DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '单价',
    `quantity`     DECIMAL(18,4) NOT NULL DEFAULT 1 COMMENT '数量',
    `discount`     DECIMAL(8,4)  DEFAULT 1.0000 COMMENT '折扣率(0-1)',
    `subtotal`     DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '折后小计',
    `created_at`   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`      TINYINT  NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_cp_contract` (`contract_id`),
    KEY `idx_cp_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='CRM 合同产品行';

-- ---- 11. 回款计划 -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `mate_crm_payment_plan` (
    `id`           VARCHAR(64)   NOT NULL,
    `tenant_id`    VARCHAR(32)   NOT NULL DEFAULT '1' COMMENT 'Tenant ID',
    `contract_id`  VARCHAR(64)   NOT NULL COMMENT '合同ID',
    `contract_num` VARCHAR(64)   DEFAULT NULL COMMENT '合同编号(冗余)',
    `customer_id`  VARCHAR(64)   DEFAULT NULL COMMENT '客户ID(冗余)',
    `term_no`      INT           NOT NULL DEFAULT 1 COMMENT '期次',
    `money`        DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '计划回款金额',
    `received_money` DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '已回款金额',
    `checking_date` DATE         NOT NULL COMMENT '计划回款日期',
    `return_type`  VARCHAR(32)   DEFAULT 'TRANSFER' COMMENT '回款方式: CASH/TRANSFER/CHECK/OTHER',
    `status`       TINYINT       NOT NULL DEFAULT 0 COMMENT '0=待回款 1=部分回款 2=已回款 3=已逾期',
    `remark`       VARCHAR(512)  DEFAULT NULL COMMENT '备注',
    `owner_id`     VARCHAR(64)   DEFAULT NULL COMMENT '负责人ID',
    `owner_name`   VARCHAR(64)   DEFAULT NULL COMMENT '负责人姓名',
    `dept_id`      VARCHAR(32)   DEFAULT NULL COMMENT '负责人部门ID(数据权限)',
    `creator_id`   VARCHAR(64)   DEFAULT NULL COMMENT '创建人ID',
    `creator_name` VARCHAR(64)   DEFAULT NULL COMMENT '创建人姓名',
    `created_at`   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`      TINYINT  NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_plan_contract` (`contract_id`),
    KEY `idx_plan_status_date` (`status`, `checking_date`),
    KEY `idx_plan_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='CRM 回款计划(分期)';

-- ---- 12. 回款记录 -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `mate_crm_repayment` (
    `id`             VARCHAR(64)   NOT NULL,
    `tenant_id`      VARCHAR(32)   NOT NULL DEFAULT '1' COMMENT 'Tenant ID',
    `number`         VARCHAR(64)   DEFAULT NULL COMMENT '回款编号',
    `contract_id`    VARCHAR(64)   NOT NULL COMMENT '合同ID',
    `contract_num`   VARCHAR(64)   DEFAULT NULL COMMENT '合同编号(冗余)',
    `plan_id`        VARCHAR(64)   DEFAULT NULL COMMENT '关联回款计划ID(可空)',
    `customer_id`    VARCHAR(64)   DEFAULT NULL COMMENT '客户ID(冗余)',
    `money`          DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '回款金额',
    `payment_method` VARCHAR(32)   NOT NULL DEFAULT 'TRANSFER' COMMENT '回款方式: CASH/TRANSFER/CHECK/OTHER',
    `return_time`    DATE          NOT NULL COMMENT '回款日期',
    `remark`         VARCHAR(512)  DEFAULT NULL COMMENT '备注',
    `attachment`     VARCHAR(512)  DEFAULT NULL COMMENT '凭证附件URL',
    `owner_id`       VARCHAR(64)   DEFAULT NULL COMMENT '负责人ID',
    `owner_name`     VARCHAR(64)   DEFAULT NULL COMMENT '负责人姓名',
    `dept_id`        VARCHAR(32)   DEFAULT NULL COMMENT '负责人部门ID(数据权限)',
    `creator_id`     VARCHAR(64)   DEFAULT NULL COMMENT '创建人ID',
    `creator_name`   VARCHAR(64)   DEFAULT NULL COMMENT '创建人姓名',
    `created_at`     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`        TINYINT  NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_repay_contract` (`contract_id`),
    KEY `idx_repay_plan` (`plan_id`),
    KEY `idx_repay_time` (`return_time`),
    KEY `idx_repay_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='CRM 回款记录';

-- ---- 13. 发票 -----------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `mate_crm_invoice` (
    `id`           VARCHAR(64)   NOT NULL,
    `tenant_id`    VARCHAR(32)   NOT NULL DEFAULT '1' COMMENT 'Tenant ID',
    `contract_id`  VARCHAR(64)   NOT NULL COMMENT '合同ID',
    `contract_num` VARCHAR(64)   DEFAULT NULL COMMENT '合同编号(冗余)',
    `customer_id`  VARCHAR(64)   DEFAULT NULL COMMENT '客户ID',
    `invoice_type` VARCHAR(32)   NOT NULL DEFAULT 'GENERAL' COMMENT '类型: GENERAL普票/SPECIAL专票/ELECTRONIC电子票',
    `title`        VARCHAR(128)  NOT NULL COMMENT '发票抬头',
    `tax_no`       VARCHAR(64)   DEFAULT NULL COMMENT '纳税人识别号',
    `bank_name`    VARCHAR(128)  DEFAULT NULL COMMENT '开户行',
    `bank_account` VARCHAR(64)   DEFAULT NULL COMMENT '银行账号',
    `address_info` VARCHAR(256)  DEFAULT NULL COMMENT '注册地址',
    `money`        DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '开票金额',
    `tax_rate`     DECIMAL(8,4)  DEFAULT 0.0600 COMMENT '税率',
    `tax_money`    DECIMAL(18,2) DEFAULT 0.00 COMMENT '税额',
    `status`       TINYINT       NOT NULL DEFAULT 0 COMMENT '0=已申请 1=已开票 2=已驳回 3=已作废',
    `invoice_no`   VARCHAR(64)   DEFAULT NULL COMMENT '发票号',
    `invoice_date` DATE          DEFAULT NULL COMMENT '开票日期',
    `remark`       VARCHAR(512)  DEFAULT NULL COMMENT '备注',
    `owner_id`     VARCHAR(64)   DEFAULT NULL COMMENT '负责人ID',
    `owner_name`   VARCHAR(64)   DEFAULT NULL COMMENT '负责人姓名',
    `dept_id`      VARCHAR(32)   DEFAULT NULL COMMENT '负责人部门ID(数据权限)',
    `creator_id`   VARCHAR(64)   DEFAULT NULL COMMENT '创建人ID',
    `creator_name` VARCHAR(64)   DEFAULT NULL COMMENT '创建人姓名',
    `created_at`   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`      TINYINT  NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_invoice_contract` (`contract_id`),
    KEY `idx_invoice_status` (`status`),
    KEY `idx_invoice_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='CRM 发票';

-- ---- 14. 售后工单 -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `mate_crm_after_sale` (
    `id`            VARCHAR(64)   NOT NULL,
    `tenant_id`     VARCHAR(32)   NOT NULL DEFAULT '1' COMMENT 'Tenant ID',
    `ticket_no`     VARCHAR(64)   DEFAULT NULL COMMENT '工单编号',
    `title`         VARCHAR(128)  NOT NULL COMMENT '工单标题',
    `customer_id`   VARCHAR(64)   NOT NULL COMMENT '客户ID',
    `customer_name` VARCHAR(128)  DEFAULT NULL COMMENT '客户名称(冗余)',
    `contract_id`   VARCHAR(64)   DEFAULT NULL COMMENT '关联合同ID',
    `type`          VARCHAR(32)   NOT NULL DEFAULT 'SERVICE' COMMENT '类型: COMPLAINT投诉/REPAIR报修/CONSULT咨询/SERVICE服务',
    `priority`      VARCHAR(8)    NOT NULL DEFAULT 'MEDIUM' COMMENT '优先级: HIGH/MEDIUM/LOW',
    `status`        VARCHAR(32)   NOT NULL DEFAULT 'OPEN' COMMENT '状态: OPEN待受理/PROCESSING处理中/RESOLVED已解决/CLOSED已关闭',
    `description`   VARCHAR(2048) DEFAULT NULL COMMENT '问题描述',
    `handler_id`    VARCHAR(64)   DEFAULT NULL COMMENT '处理人ID',
    `handler_name`  VARCHAR(64)   DEFAULT NULL COMMENT '处理人姓名',
    `resolve_time`  DATETIME      DEFAULT NULL COMMENT '解决时间',
    `close_time`    DATETIME      DEFAULT NULL COMMENT '关闭时间',
    `rating`        INT           DEFAULT NULL COMMENT '满意度评价 1-5',
    `rating_remark` VARCHAR(512)  DEFAULT NULL COMMENT '评价备注',
    `owner_id`      VARCHAR(64)   DEFAULT NULL COMMENT '负责人ID',
    `owner_name`    VARCHAR(64)   DEFAULT NULL COMMENT '负责人姓名',
    `dept_id`       VARCHAR(32)   DEFAULT NULL COMMENT '负责人部门ID(数据权限)',
    `creator_id`    VARCHAR(64)   DEFAULT NULL COMMENT '创建人ID',
    `creator_name`  VARCHAR(64)   DEFAULT NULL COMMENT '创建人姓名',
    `created_at`    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`       TINYINT  NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_as_customer` (`customer_id`),
    KEY `idx_as_status` (`status`),
    KEY `idx_as_handler` (`handler_id`),
    KEY `idx_as_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='CRM 售后工单';

-- ---- 15. 售后工单处理日志 -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `mate_crm_after_sale_log` (
    `id`            VARCHAR(64)   NOT NULL,
    `tenant_id`     VARCHAR(32)   NOT NULL DEFAULT '1' COMMENT 'Tenant ID',
    `after_sale_id` VARCHAR(64)   NOT NULL COMMENT '工单ID',
    `action`        VARCHAR(32)   NOT NULL COMMENT '动作: CREATE/ACCEPT/PROCESS/FINISH/CLOSE/RATE',
    `content`       VARCHAR(1024) DEFAULT NULL COMMENT '处理说明',
    `operator_id`   VARCHAR(64)   DEFAULT NULL COMMENT '操作人ID',
    `operator_name` VARCHAR(64)   DEFAULT NULL COMMENT '操作人姓名',
    `created_at`    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`       TINYINT  NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_asl_ticket` (`after_sale_id`),
    KEY `idx_asl_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='CRM 售后工单处理日志';

-- ---- 16. 审批实例 -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `mate_crm_approval` (
    `id`             VARCHAR(64)  NOT NULL,
    `tenant_id`      VARCHAR(32)  NOT NULL DEFAULT '1' COMMENT 'Tenant ID',
    `biz_type`       VARCHAR(32)  NOT NULL COMMENT '业务类型: CONTRACT/INVOICE/DISCOUNT',
    `biz_id`         VARCHAR(64)  NOT NULL COMMENT '业务单据ID',
    `biz_no`         VARCHAR(64)  DEFAULT NULL COMMENT '业务单据编号(冗余)',
    `title`          VARCHAR(256) DEFAULT NULL COMMENT '审批标题',
    `applicant_id`   VARCHAR(64)  NOT NULL COMMENT '申请人ID',
    `applicant_name` VARCHAR(64)  DEFAULT NULL COMMENT '申请人姓名',
    `dept_id`        VARCHAR(32)  DEFAULT NULL COMMENT '申请人部门ID(数据权限)',
    `status`         VARCHAR(32)  NOT NULL DEFAULT 'PENDING' COMMENT '状态: PENDING审批中/APPROVED通过/REJECTED驳回/CANCELED撤回',
    `current_node`   INT          NOT NULL DEFAULT 0 COMMENT '当前审批节点下标(-1=已结束)',
    `nodes_json`     VARCHAR(2048) DEFAULT NULL COMMENT '审批链快照 [{nodeName,approverId,approverName}]',
    `created_at`     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`        TINYINT  NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_approval_biz` (`biz_type`, `biz_id`),
    KEY `idx_approval_applicant` (`applicant_id`),
    KEY `idx_approval_status` (`status`),
    KEY `idx_approval_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='CRM 审批实例(通用)';

-- ---- 17. 审批节点操作记录 -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `mate_crm_approval_record` (
    `id`            VARCHAR(64)  NOT NULL,
    `tenant_id`     VARCHAR(32)  NOT NULL DEFAULT '1' COMMENT 'Tenant ID',
    `approval_id`   VARCHAR(64)  NOT NULL COMMENT '审批实例ID',
    `node_index`    INT          NOT NULL COMMENT '节点下标',
    `node_name`     VARCHAR(64)  DEFAULT NULL COMMENT '节点名称',
    `approver_id`   VARCHAR(64)  DEFAULT NULL COMMENT '审批人ID',
    `approver_name` VARCHAR(64)  DEFAULT NULL COMMENT '审批人姓名',
    `action`        VARCHAR(32)  NOT NULL COMMENT '动作: SUBMIT/APPROVE/REJECT/CANCEL',
    `remark`        VARCHAR(512) DEFAULT NULL COMMENT '审批意见',
    `acted_at`      DATETIME     DEFAULT NULL COMMENT '操作时间',
    `created_at`    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`       TINYINT  NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_ar_approval` (`approval_id`),
    KEY `idx_ar_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='CRM 审批操作记录';

-- ---- 18. 待办任务(工作台, AI 可生成) ------------------------------------------
CREATE TABLE IF NOT EXISTS `mate_crm_task` (
    `id`          VARCHAR(64)  NOT NULL,
    `tenant_id`   VARCHAR(32)  NOT NULL DEFAULT '1' COMMENT 'Tenant ID',
    `title`       VARCHAR(128) NOT NULL COMMENT '任务标题',
    `type`        VARCHAR(32)  NOT NULL DEFAULT 'FOLLOW' COMMENT '类型: FOLLOW跟进/RETURN_VISIT回访/MEETING会议/OTHER其他',
    `biz_type`    VARCHAR(32)  DEFAULT NULL COMMENT '关联对象类型: CUSTOMER/BUSINESS/CONTRACT/LEAD',
    `biz_id`      VARCHAR(64)  DEFAULT NULL COMMENT '关联对象ID',
    `biz_name`    VARCHAR(128) DEFAULT NULL COMMENT '关联对象名称(冗余)',
    `content`     VARCHAR(1024) DEFAULT NULL COMMENT '任务内容',
    `due_time`    DATETIME     DEFAULT NULL COMMENT '截止时间',
    `priority`    VARCHAR(8)   NOT NULL DEFAULT 'MEDIUM' COMMENT '优先级: HIGH/MEDIUM/LOW',
    `status`      VARCHAR(32)  NOT NULL DEFAULT 'OPEN' COMMENT '状态: OPEN待办/DONE已完成/CANCELED已取消',
    `finish_time` DATETIME     DEFAULT NULL COMMENT '完成时间',
    `source`      VARCHAR(32)  NOT NULL DEFAULT 'MANUAL' COMMENT '来源: MANUAL手动/AI智能生成',
    `owner_id`    VARCHAR(64)  DEFAULT NULL COMMENT '负责人ID',
    `owner_name`  VARCHAR(64)  DEFAULT NULL COMMENT '负责人姓名',
    `dept_id`     VARCHAR(32)  DEFAULT NULL COMMENT '负责人部门ID(数据权限)',
    `creator_id`  VARCHAR(64)  DEFAULT NULL COMMENT '创建人ID',
    `creator_name` VARCHAR(64) DEFAULT NULL COMMENT '创建人姓名',
    `created_at`  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`     TINYINT  NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_task_owner_status` (`owner_id`, `status`),
    KEY `idx_task_due` (`due_time`),
    KEY `idx_task_biz` (`biz_type`, `biz_id`),
    KEY `idx_task_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='CRM 待办任务';
