-- =============================================================================
-- V1.6.5 — mate-crm 条件查询字段元数据种子
--
-- 为各列表模块登记「可查字段」白名单(field_key→column_name), 驱动前端动态筛选器
-- 与后端安全翻译。SELECT 字段带 options_json(与页面现有下拉一致)。INSERT IGNORE 幂等。
-- =============================================================================

-- ---- lead 线索 (mate_crm_lead) ----
INSERT IGNORE INTO `mate_crm_query_field`
    (`id`, `module_key`, `field_key`, `column_name`, `label`, `field_type`, `options_json`, `sort`)
VALUES
    ('qf_lead_01', 'lead', 'leadName',    'lead_name',    '线索名称', 'TEXT',     NULL, 1),
    ('qf_lead_02', 'lead', 'contactName', 'contact_name', '联系人',   'TEXT',     NULL, 2),
    ('qf_lead_03', 'lead', 'mobile',      'mobile',       '手机号',   'TEXT',     NULL, 3),
    ('qf_lead_04', 'lead', 'source',      'source',       '线索来源', 'SELECT',   '[{"value":"SEARCH","label":"搜索"},{"value":"OFFICIAL_WEBSITE","label":"官网"},{"value":"EXHIBITION","label":"展会"},{"value":"REFERRAL","label":"转介绍"},{"value":"PHONE","label":"电话"},{"value":"OTHER","label":"其他"}]', 4),
    ('qf_lead_05', 'lead', 'status',      'status',       '状态',     'SELECT',   '[{"value":"FOLLOWING","label":"跟进中"},{"value":"CONVERTED","label":"已转化"},{"value":"INVALID","label":"已废弃"}]', 5),
    ('qf_lead_06', 'lead', 'industry',    'industry',     '行业',     'TEXT',     NULL, 6),
    ('qf_lead_07', 'lead', 'ownerName',   'owner_name',   '负责人',   'USER',     NULL, 7),
    ('qf_lead_08', 'lead', 'nextTime',    'next_time',    '下次跟进', 'DATETIME', NULL, 8),
    ('qf_lead_09', 'lead', 'createdAt',   'created_at',   '创建时间', 'DATETIME', NULL, 9);

-- ---- customer 客户 (mate_crm_customer) ----
INSERT IGNORE INTO `mate_crm_query_field`
    (`id`, `module_key`, `field_key`, `column_name`, `label`, `field_type`, `options_json`, `sort`)
VALUES
    ('qf_cust_01', 'customer', 'customerName',     'customer_name',      '客户名称',   'TEXT',     NULL, 1),
    ('qf_cust_02', 'customer', 'industry',         'industry',           '行业',       'TEXT',     NULL, 2),
    ('qf_cust_03', 'customer', 'level',            'level',              '客户等级',   'SELECT',   '[{"value":"A","label":"A 重点"},{"value":"B","label":"B 普通"},{"value":"C","label":"C 其他"}]', 3),
    ('qf_cust_04', 'customer', 'status',           'status',             '合作状态',   'SELECT',   '[{"value":"INITIAL","label":"初步接触"},{"value":"NEGOTIATING","label":"洽谈中"},{"value":"OPPORTUNITY","label":"机会成单"},{"value":"DORMANT","label":"沉睡"},{"value":"CHURNED","label":"流失"}]', 4),
    ('qf_cust_05', 'customer', 'poolStatus',       'pool_status',        '海池',       'SELECT',   '[{"value":"1","label":"公海"},{"value":"0","label":"私海"}]', 5),
    ('qf_cust_06', 'customer', 'phone',            'phone',              '联系电话',   'TEXT',     NULL, 6),
    ('qf_cust_07', 'customer', 'ownerName',        'owner_name',         '负责人',     'USER',     NULL, 7),
    ('qf_cust_08', 'customer', 'recentFollowTime', 'recent_follow_time', '最近跟进',   'DATETIME', NULL, 8),
    ('qf_cust_09', 'customer', 'createdAt',        'created_at',         '创建时间',   'DATETIME', NULL, 9);

-- ---- contact 联系人 (mate_crm_contact) ----
INSERT IGNORE INTO `mate_crm_query_field`
    (`id`, `module_key`, `field_key`, `column_name`, `label`, `field_type`, `options_json`, `sort`)
VALUES
    ('qf_ct_01', 'contact', 'contactName', 'contact_name', '联系人',   'TEXT',     NULL, 1),
    ('qf_ct_02', 'contact', 'mobile',      'mobile',       '手机号',   'TEXT',     NULL, 2),
    ('qf_ct_03', 'contact', 'email',       'email',        '邮箱',     'TEXT',     NULL, 3),
    ('qf_ct_04', 'contact', 'position',    'position',     '职务',     'TEXT',     NULL, 4),
    ('qf_ct_05', 'contact', 'gender',      'gender',       '性别',     'SELECT',   '[{"value":"MALE","label":"男"},{"value":"FEMALE","label":"女"}]', 5),
    ('qf_ct_06', 'contact', 'customerName','customer_name','所属客户', 'TEXT',     NULL, 6),
    ('qf_ct_07', 'contact', 'createdAt',   'created_at',   '创建时间', 'DATETIME', NULL, 7);

-- ---- followup 跟进记录 (mate_crm_follow_up) ----
INSERT IGNORE INTO `mate_crm_query_field`
    (`id`, `module_key`, `field_key`, `column_name`, `label`, `field_type`, `options_json`, `sort`)
VALUES
    ('qf_fu_01', 'followup', 'bizType',    'biz_type',    '对象类型', 'SELECT',   '[{"value":"LEAD","label":"线索"},{"value":"CUSTOMER","label":"客户"},{"value":"CONTACT","label":"联系人"},{"value":"BUSINESS","label":"商机"}]', 1),
    ('qf_fu_02', 'followup', 'bizName',    'biz_name',    '对象名称', 'TEXT',     NULL, 2),
    ('qf_fu_03', 'followup', 'followType', 'follow_type', '跟进方式', 'SELECT',   '[{"value":"PHONE","label":"电话"},{"value":"VISIT","label":"拜访"},{"value":"WECHAT","label":"微信"},{"value":"EMAIL","label":"邮件"},{"value":"MEETING","label":"会议"},{"value":"OTHER","label":"其他"}]', 3),
    ('qf_fu_04', 'followup', 'content',    'content',     '跟进内容', 'TEXT',     NULL, 4),
    ('qf_fu_05', 'followup', 'userName',   'user_name',   '记录人',   'USER',     NULL, 5),
    ('qf_fu_06', 'followup', 'nextTime',   'next_time',   '下次跟进', 'DATETIME', NULL, 6),
    ('qf_fu_07', 'followup', 'createdAt',  'created_at',  '跟进时间', 'DATETIME', NULL, 7);

-- ---- business 商机 (mate_crm_business) ----
INSERT IGNORE INTO `mate_crm_query_field`
    (`id`, `module_key`, `field_key`, `column_name`, `label`, `field_type`, `options_json`, `sort`)
VALUES
    ('qf_biz_01', 'business', 'businessName', 'business_name', '商机名称', 'TEXT',   NULL, 1),
    ('qf_biz_02', 'business', 'customerName', 'customer_name', '所属客户', 'TEXT',   NULL, 2),
    ('qf_biz_03', 'business', 'stage',        'stage',         '阶段',     'SELECT', '[{"value":"INITIAL","label":"初步接触"},{"value":"DEMAND","label":"需求确认"},{"value":"PLAN","label":"方案报价"},{"value":"NEGOTIATION","label":"商务谈判"},{"value":"WIN","label":"赢单"},{"value":"LOSE","label":"输单"}]', 3),
    ('qf_biz_04', 'business', 'money',        'money',         '预计金额', 'NUMBER', NULL, 4),
    ('qf_biz_05', 'business', 'dealProb',     'deal_prob',     '赢率%',    'NUMBER', NULL, 5),
    ('qf_biz_06', 'business', 'dealDate',     'deal_date',     '预计成交', 'DATE',   NULL, 6),
    ('qf_biz_07', 'business', 'ownerName',    'owner_name',    '负责人',   'USER',   NULL, 7),
    ('qf_biz_08', 'business', 'createdAt',    'created_at',    '创建时间', 'DATETIME', NULL, 8);

-- ---- product 产品 (mate_crm_product) ----
INSERT IGNORE INTO `mate_crm_query_field`
    (`id`, `module_key`, `field_key`, `column_name`, `label`, `field_type`, `options_json`, `sort`)
VALUES
    ('qf_prod_01', 'product', 'productName', 'product_name', '产品名称', 'TEXT',   NULL, 1),
    ('qf_prod_02', 'product', 'productCode', 'product_code', '产品编号', 'TEXT',   NULL, 2),
    ('qf_prod_03', 'product', 'category',    'category',     '分类',     'TEXT',   NULL, 3),
    ('qf_prod_04', 'product', 'price',       'price',        '标准售价', 'NUMBER', NULL, 4),
    ('qf_prod_05', 'product', 'status',      'status',       '上架状态', 'SELECT', '[{"value":"1","label":"上架"},{"value":"0","label":"下架"}]', 5),
    ('qf_prod_06', 'product', 'createdAt',   'created_at',   '创建时间', 'DATETIME', NULL, 6);

-- ---- contract 合同 (mate_crm_contract) ----
INSERT IGNORE INTO `mate_crm_query_field`
    (`id`, `module_key`, `field_key`, `column_name`, `label`, `field_type`, `options_json`, `sort`)
VALUES
    ('qf_ctrc_01', 'contract', 'contractNum',    'contract_num',   '合同编号', 'TEXT',   NULL, 1),
    ('qf_ctrc_02', 'contract', 'contractName',   'contract_name',  '合同名称', 'TEXT',   NULL, 2),
    ('qf_ctrc_03', 'contract', 'customerName',   'customer_name',  '客户名称', 'TEXT',   NULL, 3),
    ('qf_ctrc_04', 'contract', 'money',          'money',          '合同总额', 'NUMBER', NULL, 4),
    ('qf_ctrc_05', 'contract', 'receivedMoney',  'received_money', '已回款',   'NUMBER', NULL, 5),
    ('qf_ctrc_06', 'contract', 'status',         'status',         '状态',     'SELECT', '[{"value":"DRAFT","label":"草稿"},{"value":"PENDING","label":"审批中"},{"value":"APPROVED","label":"已通过"},{"value":"REJECTED","label":"已驳回"},{"value":"ARCHIVED","label":"已归档"}]', 6),
    ('qf_ctrc_07', 'contract', 'signTime',       'sign_time',      '签约日期', 'DATE',   NULL, 7),
    ('qf_ctrc_08', 'contract', 'endTime',        'end_time',       '到期日期', 'DATE',   NULL, 8),
    ('qf_ctrc_09', 'contract', 'ownerName',      'owner_name',     '负责人',   'USER',   NULL, 9),
    ('qf_ctrc_10', 'contract', 'createdAt',      'created_at',     '创建时间', 'DATETIME', NULL, 10);

-- ---- repayment 回款记录 (mate_crm_repayment) ----
INSERT IGNORE INTO `mate_crm_query_field`
    (`id`, `module_key`, `field_key`, `column_name`, `label`, `field_type`, `options_json`, `sort`)
VALUES
    ('qf_repay_01', 'repayment', 'number',        'number',         '回款编号', 'TEXT',   NULL, 1),
    ('qf_repay_02', 'repayment', 'contractNum',   'contract_num',   '合同编号', 'TEXT',   NULL, 2),
    ('qf_repay_03', 'repayment', 'money',         'money',          '回款金额', 'NUMBER', NULL, 3),
    ('qf_repay_04', 'repayment', 'paymentMethod', 'payment_method', '回款方式', 'SELECT', '[{"value":"CASH","label":"现金"},{"value":"TRANSFER","label":"转账"},{"value":"CHECK","label":"支票"},{"value":"OTHER","label":"其他"}]', 4),
    ('qf_repay_05', 'repayment', 'returnTime',    'return_time',    '回款日期', 'DATE',   NULL, 5),
    ('qf_repay_06', 'repayment', 'ownerName',     'owner_name',     '负责人',   'USER',   NULL, 6),
    ('qf_repay_07', 'repayment', 'createdAt',     'created_at',     '登记时间', 'DATETIME', NULL, 7);

-- ---- invoice 发票 (mate_crm_invoice) ----
INSERT IGNORE INTO `mate_crm_query_field`
    (`id`, `module_key`, `field_key`, `column_name`, `label`, `field_type`, `options_json`, `sort`)
VALUES
    ('qf_inv_01', 'invoice', 'contractNum',  'contract_num',  '合同编号', 'TEXT',   NULL, 1),
    ('qf_inv_02', 'invoice', 'title',        'title',         '发票抬头', 'TEXT',   NULL, 2),
    ('qf_inv_03', 'invoice', 'invoiceType',  'invoice_type',  '发票类型', 'SELECT', '[{"value":"GENERAL","label":"普票"},{"value":"SPECIAL","label":"专票"},{"value":"ELECTRONIC","label":"电子票"}]', 3),
    ('qf_inv_04', 'invoice', 'money',        'money',         '开票金额', 'NUMBER', NULL, 4),
    ('qf_inv_05', 'invoice', 'status',       'status',        '状态',     'SELECT', '[{"value":"0","label":"已申请"},{"value":"1","label":"已开票"},{"value":"2","label":"已驳回"},{"value":"3","label":"已作废"}]', 5),
    ('qf_inv_06', 'invoice', 'invoiceDate',  'invoice_date',  '开票日期', 'DATE',   NULL, 6),
    ('qf_inv_07', 'invoice', 'ownerName',    'owner_name',    '负责人',   'USER',   NULL, 7),
    ('qf_inv_08', 'invoice', 'createdAt',    'created_at',    '创建时间', 'DATETIME', NULL, 8);

-- ---- aftersale 售后工单 (mate_crm_after_sale) ----
INSERT IGNORE INTO `mate_crm_query_field`
    (`id`, `module_key`, `field_key`, `column_name`, `label`, `field_type`, `options_json`, `sort`)
VALUES
    ('qf_as_01', 'aftersale', 'ticketNo',     'ticket_no',     '工单编号', 'TEXT',   NULL, 1),
    ('qf_as_02', 'aftersale', 'title',        'title',         '工单标题', 'TEXT',   NULL, 2),
    ('qf_as_03', 'aftersale', 'customerName', 'customer_name', '客户名称', 'TEXT',   NULL, 3),
    ('qf_as_04', 'aftersale', 'type',         'type',          '类型',     'SELECT', '[{"value":"COMPLAINT","label":"投诉"},{"value":"REPAIR","label":"报修"},{"value":"CONSULT","label":"咨询"},{"value":"SERVICE","label":"服务"}]', 4),
    ('qf_as_05', 'aftersale', 'priority',     'priority',      '优先级',   'SELECT', '[{"value":"HIGH","label":"高"},{"value":"MEDIUM","label":"中"},{"value":"LOW","label":"低"}]', 5),
    ('qf_as_06', 'aftersale', 'status',       'status',        '状态',     'SELECT', '[{"value":"OPEN","label":"待受理"},{"value":"PROCESSING","label":"处理中"},{"value":"RESOLVED","label":"已解决"},{"value":"CLOSED","label":"已关闭"}]', 6),
    ('qf_as_07', 'aftersale', 'handlerName',  'handler_name',  '处理人',   'USER',   NULL, 7),
    ('qf_as_08', 'aftersale', 'createdAt',    'created_at',    '创建时间', 'DATETIME', NULL, 8);

-- ---- task 待办任务 (mate_crm_task) ----
INSERT IGNORE INTO `mate_crm_query_field`
    (`id`, `module_key`, `field_key`, `column_name`, `label`, `field_type`, `options_json`, `sort`)
VALUES
    ('qf_task_01', 'task', 'title',     'title',      '任务标题', 'TEXT',     NULL, 1),
    ('qf_task_02', 'task', 'type',      'type',       '类型',     'SELECT',   '[{"value":"FOLLOW","label":"跟进"},{"value":"RETURN_VISIT","label":"回访"},{"value":"MEETING","label":"会议"},{"value":"OTHER","label":"其他"}]', 2),
    ('qf_task_03', 'task', 'bizType',   'biz_type',   '关联对象', 'SELECT',   '[{"value":"CUSTOMER","label":"客户"},{"value":"BUSINESS","label":"商机"},{"value":"CONTRACT","label":"合同"},{"value":"LEAD","label":"线索"}]', 3),
    ('qf_task_04', 'task', 'priority',  'priority',   '优先级',   'SELECT',   '[{"value":"HIGH","label":"高"},{"value":"MEDIUM","label":"中"},{"value":"LOW","label":"低"}]', 4),
    ('qf_task_05', 'task', 'status',    'status',     '状态',     'SELECT',   '[{"value":"OPEN","label":"待办"},{"value":"DONE","label":"已完成"},{"value":"CANCELED","label":"已取消"}]', 5),
    ('qf_task_06', 'task', 'dueTime',   'due_time',   '截止时间', 'DATETIME', NULL, 6),
    ('qf_task_07', 'task', 'ownerName', 'owner_name', '负责人',   'USER',     NULL, 7),
    ('qf_task_08', 'task', 'createdAt', 'created_at', '创建时间', 'DATETIME', NULL, 8);
