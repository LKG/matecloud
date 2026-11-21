-- =============================================================================
-- V1.6.6 — mate-crm 条件查询字段元数据补充种子
--
-- 补齐 V1.6.5 白名单缺口: 列表页快捷栏 keyword 扇出 / 折算所需的
-- ID 类与文本类字段(followup.bizId、invoice.invoiceNo/contractId、
-- repayment.contractId、aftersale.customerId、task.bizName/content)。
-- INSERT IGNORE 幂等, 与 V1.6.5 风格一致。
-- =============================================================================

-- ---- followup 跟进记录 (mate_crm_follow_up) ----
INSERT IGNORE INTO `mate_crm_query_field`
    (`id`, `module_key`, `field_key`, `column_name`, `label`, `field_type`, `options_json`, `sort`)
VALUES
    ('qf_fu_08', 'followup', 'bizId',      'biz_id',      '业务 ID',   'TEXT', NULL, 8),
    ('qf_fu_09', 'followup', 'customerId', 'customer_id', '关联客户', 'TEXT', NULL, 9);

-- ---- invoice 发票 (mate_crm_invoice) ----
INSERT IGNORE INTO `mate_crm_query_field`
    (`id`, `module_key`, `field_key`, `column_name`, `label`, `field_type`, `options_json`, `sort`)
VALUES
    ('qf_inv_09', 'invoice', 'invoiceNo',  'invoice_no',  '发票号',   'TEXT', NULL, 9),
    ('qf_inv_10', 'invoice', 'contractId', 'contract_id', '合同 ID', 'TEXT', NULL, 10);

-- ---- repayment 回款记录 (mate_crm_repayment) ----
INSERT IGNORE INTO `mate_crm_query_field`
    (`id`, `module_key`, `field_key`, `column_name`, `label`, `field_type`, `options_json`, `sort`)
VALUES
    ('qf_repay_08', 'repayment', 'contractId', 'contract_id', '合同 ID', 'TEXT', NULL, 8);

-- ---- aftersale 售后工单 (mate_crm_after_sale) ----
INSERT IGNORE INTO `mate_crm_query_field`
    (`id`, `module_key`, `field_key`, `column_name`, `label`, `field_type`, `options_json`, `sort`)
VALUES
    ('qf_as_09', 'aftersale', 'customerId', 'customer_id', '客户 ID', 'TEXT', NULL, 9);

-- ---- task 待办任务 (mate_crm_task) ----
INSERT IGNORE INTO `mate_crm_query_field`
    (`id`, `module_key`, `field_key`, `column_name`, `label`, `field_type`, `options_json`, `sort`)
VALUES
    ('qf_task_09', 'task', 'bizName', 'biz_name', '关联业务', 'TEXT', NULL, 9),
    ('qf_task_10', 'task', 'content', 'content',  '任务描述', 'TEXT', NULL, 10);
