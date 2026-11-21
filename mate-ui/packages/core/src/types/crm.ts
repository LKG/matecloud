/**
 * CRM domain types — aligned with mate-crm (vip.mate.crm) backend entities,
 * commands and read-model records.
 *
 * Paths below the shared axios client (baseURL = /api/v1) map to controllers
 * under /api/v1/crm/**. Dates are serialized as ISO strings:
 *   LocalDateTime → 'YYYY-MM-DDTHH:mm:ss'   LocalDate → 'YYYY-MM-DD'
 */

// ── Enumerations (string unions mirroring backend value objects) ────────────

/** 线索状态 */
export type LeadStatus = 'FOLLOWING' | 'CONVERTED' | 'INVALID'
/** 客户状态 */
export type CustomerStatus = 'INITIAL' | 'NEGOTIATING' | 'OPPORTUNITY' | 'DORMANT' | 'CHURNED'
/** 跟进挂载业务类型 */
export type FollowBizType = 'LEAD' | 'CUSTOMER' | 'CONTACT' | 'BUSINESS'
/** 商机阶段 */
export type BusinessStage = 'INITIAL' | 'DEMAND' | 'PLAN' | 'NEGOTIATION' | 'WIN' | 'LOSE'
/** 合同状态 */
export type ContractStatus = 'DRAFT' | 'PENDING' | 'APPROVED' | 'REJECTED' | 'ARCHIVED'
/** 审批业务类型 */
export type ApprovalBizType = 'CONTRACT'
/** 审批状态 */
export type ApprovalStatus = 'PENDING' | 'APPROVED' | 'REJECTED' | 'CANCELED'
/** 审批动作 */
export type ApprovalAction = 'SUBMIT' | 'APPROVE' | 'REJECT' | 'CANCEL'
/** 售后工单类型 */
export type AfterSaleType = 'COMPLAINT' | 'REPAIR' | 'CONSULT' | 'SERVICE'
/** 售后优先级 */
export type AfterSalePriority = 'HIGH' | 'MEDIUM' | 'LOW'
/** 售后状态 */
export type AfterSaleStatus = 'OPEN' | 'PROCESSING' | 'RESOLVED' | 'CLOSED'
/** 待办类型 */
export type TaskType = 'FOLLOW' | 'RETURN_VISIT' | 'MEETING' | 'OTHER'
/** 待办优先级 */
export type TaskPriority = 'HIGH' | 'MEDIUM' | 'LOW'
/** 待办状态 */
export type TaskStatus = 'OPEN' | 'DONE' | 'CANCELED'
/** 待办来源 */
export type TaskSource = 'MANUAL' | 'AI'

// ── Lead ────────────────────────────────────────────────────────────────────

export interface Lead {
  id: string
  leadName: string
  contactName?: string
  mobile?: string
  telephone?: string
  email?: string
  industry?: string
  source?: string
  status: LeadStatus
  address?: string
  remark?: string
  nextTime?: string
  convertedCustomerId?: string
  ownerId?: string
  ownerName?: string
  deptId?: string
  creatorId?: string
  creatorName?: string
  createdAt?: string
  updatedAt?: string
}

export interface LeadCommand {
  leadName: string
  contactName?: string
  mobile?: string
  telephone?: string
  email?: string
  industry?: string
  source?: string
  address?: string
  remark?: string
  nextTime?: string
}

// ── Customer ────────────────────────────────────────────────────────────────

export interface Customer {
  id: string
  customerName: string
  industry?: string
  level?: string
  source?: string
  status?: CustomerStatus
  phone?: string
  address?: string
  website?: string
  remark?: string
  /** 1=公海 0=私海 */
  poolStatus: number
  ownerId?: string
  ownerName?: string
  deptId?: string
  claimTime?: string
  toPoolTime?: string
  toPoolReason?: string
  recentFollowTime?: string
  nextTime?: string
  creatorId?: string
  creatorName?: string
  lockVersion?: number
  createdAt?: string
  updatedAt?: string
}

export interface CustomerCommand {
  customerName: string
  industry?: string
  level?: string
  source?: string
  status?: string
  phone?: string
  address?: string
  website?: string
  remark?: string
  toPublicPool?: boolean
}

// ── Contact ─────────────────────────────────────────────────────────────────

export interface Contact {
  id: string
  customerId: string
  customerName?: string
  contactName: string
  mobile?: string
  telephone?: string
  email?: string
  position?: string
  gender?: string
  /** 1=主联系人 */
  isPrimary?: number
  wechat?: string
  remark?: string
  ownerId?: string
  ownerName?: string
  deptId?: string
  creatorId?: string
  creatorName?: string
  createdAt?: string
  updatedAt?: string
}

export interface ContactCommand {
  customerId: string
  customerName?: string
  contactName: string
  mobile?: string
  telephone?: string
  email?: string
  position?: string
  gender?: string
  isPrimary?: number
  wechat?: string
  remark?: string
}

// ── Follow-up ───────────────────────────────────────────────────────────────

export interface FollowUp {
  id: string
  bizType: FollowBizType
  bizId: string
  bizName?: string
  customerId?: string
  content: string
  followType?: string
  nextContent?: string
  nextTime?: string
  userId?: string
  userName?: string
  deptId?: string
  createdAt?: string
}

export interface FollowUpCommand {
  bizType: FollowBizType
  bizId: string
  bizName?: string
  customerId?: string
  content: string
  followType?: string
  nextContent?: string
  nextTime?: string
}

// ── Pool config ──────────────────────────────────────────────────────────────

export interface PoolConfig {
  id?: string
  enabled?: number
  recycleDays?: number
  maxHold?: number
  remindDays?: number
}

export interface PoolConfigCommand {
  enabled?: number
  recycleDays?: number
  maxHold?: number
  remindDays?: number
}

// ── Business (opportunity) ────────────────────────────────────────────────────

export interface Business {
  id: string
  businessName: string
  customerId?: string
  customerName?: string
  contactName?: string
  source?: string
  stage: BusinessStage
  /** 1=进行中 0=结束 */
  statusFlag?: number
  money?: string | number
  dealProb?: number
  dealDate?: string
  nextStep?: string
  endRemark?: string
  remark?: string
  ownerId?: string
  ownerName?: string
  deptId?: string
  creatorId?: string
  creatorName?: string
  createdAt?: string
  updatedAt?: string
}

export interface BusinessProductLine {
  productId?: string
  productName?: string
  unitPrice?: number | string
  quantity?: number | string
  subtotal?: number | string
}

export interface BusinessCommand {
  businessName: string
  customerId?: string
  customerName?: string
  contactName?: string
  source?: string
  dealProb?: number
  dealDate?: string
  nextStep?: string
  remark?: string
  products?: BusinessProductLine[]
}

export interface BusinessDetail {
  business: Business
  products: BusinessProductLine[]
}

// ── Product ─────────────────────────────────────────────────────────────────

export interface Product {
  id: string
  productName: string
  productCode?: string
  category?: string
  price?: number | string
  unit?: string
  /** 1=上架 0=下架 */
  status?: number
  description?: string
  sort?: number
  creatorId?: string
  createdAt?: string
  updatedAt?: string
}

export interface ProductCommand {
  productName: string
  productCode?: string
  category?: string
  price?: number | string
  unit?: string
  description?: string
  sort?: number
}

// ── Contract ────────────────────────────────────────────────────────────────

export interface Contract {
  id: string
  contractNum?: string
  contractName: string
  customerId?: string
  customerName?: string
  businessId?: string
  contactName?: string
  money?: number | string
  receivedMoney?: number | string
  startTime?: string
  endTime?: string
  signTime?: string
  status: ContractStatus
  remark?: string
  attachment?: string
  ownerId?: string
  ownerName?: string
  deptId?: string
  creatorId?: string
  creatorName?: string
  lockVersion?: number
  createdAt?: string
  updatedAt?: string
}

export interface ContractProductLine {
  productId?: string
  productName?: string
  unitPrice?: number | string
  quantity?: number | string
  discount?: number | string
  subtotal?: number | string
}

export interface ContractCommand {
  contractNum?: string
  contractName: string
  customerId?: string
  customerName?: string
  businessId?: string
  contactName?: string
  startTime?: string
  endTime?: string
  signTime?: string
  remark?: string
  attachment?: string
  products?: ContractProductLine[]
}

export interface ContractDetail {
  contract: Contract
  products: ContractProductLine[]
}

// ── Payment plan & repayment ───────────────────────────────────────────────

export interface PaymentPlan {
  id: string
  contractId: string
  contractNum?: string
  customerId?: string
  termNo?: number
  money?: number | string
  receivedMoney?: number | string
  checkingDate?: string
  returnType?: string
  /** 0待回款 1部分回款 2已结清 3已逾期 */
  status?: number
  remark?: string
  ownerId?: string
  ownerName?: string
  deptId?: string
  createdAt?: string
  updatedAt?: string
}

export interface PaymentPlanCommand {
  contractId: string
  contractNum?: string
  customerId?: string
  termNo?: number
  money?: number | string
  checkingDate?: string
  returnType?: string
  remark?: string
}

export interface Repayment {
  id: string
  number?: string
  contractId: string
  contractNum?: string
  planId?: string
  customerId?: string
  money?: number | string
  paymentMethod?: string
  returnTime?: string
  remark?: string
  attachment?: string
  ownerId?: string
  ownerName?: string
  deptId?: string
  creatorId?: string
  creatorName?: string
  createdAt?: string
  updatedAt?: string
}

export interface RepaymentCommand {
  contractId: string
  planId?: string
  money?: number | string
  paymentMethod?: string
  returnTime?: string
  remark?: string
  attachment?: string
}

// ── Invoice ─────────────────────────────────────────────────────────────────

export interface Invoice {
  id: string
  contractId: string
  contractNum?: string
  customerId?: string
  invoiceType?: string
  title?: string
  taxNo?: string
  bankName?: string
  bankAccount?: string
  addressInfo?: string
  money?: number | string
  taxRate?: number | string
  taxMoney?: number | string
  /** 0已申请 1已开票 2已驳回 3已作废 */
  status?: number
  invoiceNo?: string
  invoiceDate?: string
  remark?: string
  ownerId?: string
  ownerName?: string
  deptId?: string
  creatorId?: string
  creatorName?: string
  createdAt?: string
  updatedAt?: string
}

export interface InvoiceCommand {
  contractId: string
  invoiceType?: string
  title?: string
  taxNo?: string
  bankName?: string
  bankAccount?: string
  addressInfo?: string
  money?: number | string
  taxRate?: number | string
  remark?: string
}

// ── After-sale ──────────────────────────────────────────────────────────────

export interface AfterSale {
  id: string
  ticketNo?: string
  title: string
  customerId?: string
  customerName?: string
  contractId?: string
  type?: AfterSaleType
  priority?: AfterSalePriority
  status: AfterSaleStatus
  description?: string
  handlerId?: string
  handlerName?: string
  resolveTime?: string
  closeTime?: string
  rating?: number
  ratingRemark?: string
  ownerId?: string
  ownerName?: string
  deptId?: string
  creatorId?: string
  creatorName?: string
  createdAt?: string
  updatedAt?: string
}

export interface AfterSaleLog {
  id: string
  afterSaleId: string
  action: string
  content?: string
  operatorId?: string
  operatorName?: string
  createdAt?: string
}

export interface AfterSaleCommand {
  title: string
  customerId?: string
  customerName?: string
  contractId?: string
  type?: AfterSaleType
  priority?: AfterSalePriority
  description?: string
}

export interface AfterSaleDetail {
  afterSale: AfterSale
  logs: AfterSaleLog[]
}

// ── Approval ────────────────────────────────────────────────────────────────

export interface Approval {
  id: string
  bizType: ApprovalBizType
  bizId: string
  bizNo?: string
  title?: string
  applicantId?: string
  applicantName?: string
  deptId?: string
  status: ApprovalStatus
  /** -1=结束 */
  currentNode?: number
  createdAt?: string
  updatedAt?: string
}

export interface ApprovalNode {
  nodeName: string
  approverId?: string
  approverName?: string
}

export interface ApprovalRecord {
  id: string
  approvalId: string
  nodeIndex?: number
  nodeName?: string
  approverId?: string
  approverName?: string
  action: ApprovalAction
  remark?: string
  actedAt?: string
}

export interface ApprovalDetail {
  approval: Approval
  nodes: ApprovalNode[]
  records: ApprovalRecord[]
}

// ── Task (workbench) ────────────────────────────────────────────────────────

export interface CrmTask {
  id: string
  title: string
  type?: TaskType
  bizType?: string
  bizId?: string
  bizName?: string
  content?: string
  dueTime?: string
  priority?: TaskPriority
  status: TaskStatus
  finishTime?: string
  source?: TaskSource
  ownerId?: string
  ownerName?: string
  deptId?: string
  creatorId?: string
  creatorName?: string
  createdAt?: string
  updatedAt?: string
}

export interface TaskCommand {
  title: string
  type?: TaskType
  bizType?: string
  bizId?: string
  bizName?: string
  content?: string
  dueTime?: string
  priority?: TaskPriority
  source?: TaskSource
}

// ── Stats read models ───────────────────────────────────────────────────────

export interface CrmDashboard {
  leadTotal: number
  leadConverted: number
  leadConvertRate: number | string
  customerPrivate: number
  customerPublic: number
  businessTotal: number
  businessWon: number
  businessLose: number
  winRate: number | string
  contractCount: number
  contractMoney: number | string
  receivedMoney: number | string
  receivedRate: number | string
  overduePlanCount: number
  pendingPlanMoney: number | string
  todayTaskCount: number
}

export interface FunnelStage {
  stage: BusinessStage
  count: number
  money: number | string
}

export interface MemberStat {
  ownerId: string
  ownerName: string
  contractCount: number
  contractMoney: number | string
}

export interface MonthTrend {
  month: string
  signMoney: number | string
  receivedMoney: number | string
}

// ── AI insight ──────────────────────────────────────────────────────────────

export interface CustomerInsight {
  customerId: string
  customerName: string
  summary: string
  businessTotal: number
  businessActive: number
  stalledBusinesses: string[]
  contractCount: number
  totalMoney: number | string
  receivedMoney: number | string
  receivableMoney: number | string
  lastFollowTime?: string
  daysSinceFollow?: number
  risks: string[]
  suggestions: string[]
}

// ── Shared page query ───────────────────────────────────────────────────────

export interface CrmPageQuery {
  keyword?: string
  pageNum?: number
  pageSize?: number
}

// ── 条件查询(动态高级筛选, 对齐 vip.mate.crm.query) ─────────────────────

/** 字段类型(与后端 FieldType 枚举一致) */
export type QueryFieldType = 'TEXT' | 'NUMBER' | 'DATE' | 'DATETIME' | 'SELECT' | 'USER' | 'DEPT' | 'BOOLEAN'
/** 筛选算子(与后端 FilterOperator 枚举一致) */
export type FilterOperator = 'EQ' | 'NE' | 'LIKE' | 'GT' | 'GE' | 'LT' | 'LE' | 'IN' | 'BETWEEN' | 'IS_NULL' | 'IS_NOT_NULL'

export interface QueryFieldOption {
  value: string
  label: string
}

/** 可查字段元数据(后端下发, 不含真实列名, 前端只认 fieldKey)。 */
export interface QueryFieldMeta {
  fieldKey: string
  label: string
  fieldType: QueryFieldType
  operators: FilterOperator[]
  options?: QueryFieldOption[] | null
}

export interface QueryCondition {
  fieldKey: string
  operator: FilterOperator
  values?: string[]
}

/** 条件组(组内 AND, 组间 OR)。 */
export interface ConditionGroup {
  conditions: QueryCondition[]
}

/** 高级查询统一请求体。 */
export interface AdvancedQuery {
  groups?: ConditionGroup[]
  orderField?: string
  orderAsc?: boolean
  mineOnly?: boolean
  pageNum?: number
  pageSize?: number
}

/** 个人常用筛选(后端 SavedFilterVO, query 已反序列化)。 */
export interface SavedFilterVO {
  id: string
  moduleKey: string
  name: string
  query: AdvancedQuery
  sort?: number
}

/** 保存/更新常用筛选请求(id 为空新增, 非空覆盖)。 */
export interface SavedFilterRequest {
  id?: string
  moduleKey: string
  name: string
  query: AdvancedQuery
  sort?: number
}
