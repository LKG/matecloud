-- ============================================================================
-- MateCRM: 产品行 product_id 放开非空约束
--
-- 商机报价行 / 合同产品行支持目录外自由行项(仅填名称+单价+数量),
-- 与领域模型 BusinessProduct.of(productId=null) 及前端表单一致。
-- ============================================================================

ALTER TABLE `mate_crm_business_product`
    MODIFY COLUMN `product_id` VARCHAR(64) NULL COMMENT '产品ID(可空: 目录外自由行项)';

ALTER TABLE `mate_crm_contract_product`
    MODIFY COLUMN `product_id` VARCHAR(64) NULL COMMENT '产品ID(可空: 目录外自由行项)';
