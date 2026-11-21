import { client } from '../client'
import type { Result, PageResult } from '../../types/result'
import type {
  Lead, LeadCommand,
  Customer, CustomerCommand,
  Contact, ContactCommand,
  FollowUp, FollowUpCommand,
  PoolConfig, PoolConfigCommand,
  Business, BusinessCommand, BusinessDetail,
  Product, ProductCommand,
  Contract, ContractCommand, ContractDetail,
  PaymentPlan, PaymentPlanCommand,
  Repayment, RepaymentCommand,
  Invoice, InvoiceCommand,
  AfterSale, AfterSaleCommand, AfterSaleDetail,
  Approval, ApprovalDetail,
  CrmTask, TaskCommand,
  CrmDashboard, FunnelStage, MemberStat, MonthTrend,
  CustomerInsight,
  AdvancedQuery, QueryFieldMeta, SavedFilterVO, SavedFilterRequest,
} from '../../types/crm'

// Re-export the domain types so views can import everything from one module.
export type {
  Lead, LeadCommand,
  Customer, CustomerCommand,
  Contact, ContactCommand,
  FollowUp, FollowUpCommand,
  PoolConfig, PoolConfigCommand,
  Business, BusinessCommand, BusinessDetail,
  Product, ProductCommand,
  Contract, ContractCommand, ContractDetail,
  PaymentPlan, PaymentPlanCommand,
  Repayment, RepaymentCommand,
  Invoice, InvoiceCommand,
  AfterSale, AfterSaleCommand, AfterSaleDetail,
  Approval, ApprovalDetail,
  CrmTask, TaskCommand,
  CrmDashboard, FunnelStage, MemberStat, MonthTrend,
  CustomerInsight,
  AdvancedQuery, ConditionGroup, QueryCondition, QueryFieldMeta, QueryFieldOption,
  QueryFieldType, FilterOperator, SavedFilterVO, SavedFilterRequest,
} from '../../types/crm'

/** Common list-page query params (relative to baseURL /api/v1). */
type Q = Record<string, any>

// ── 线索 /crm/leads ──────────────────────────────────────────────────────────
export const leadApi = {
  page: (params?: Q) => client.get<any, Result<PageResult<Lead>>>('/crm/leads', { params }),
  get: (id: string) => client.get<any, Result<Lead>>(`/crm/leads/${id}`),
  create: (data: LeadCommand) => client.post<any, Result<string>>('/crm/leads', data),
  update: (id: string, data: LeadCommand) => client.put<any, Result<void>>(`/crm/leads/${id}`, data),
  convert: (id: string) => client.post<any, Result<string>>(`/crm/leads/${id}/convert`),
  discard: (id: string) => client.post<any, Result<void>>(`/crm/leads/${id}/discard`),
  changeOwner: (id: string, params: { ownerId: string; ownerName?: string; deptId?: string }) =>
    client.put<any, Result<void>>(`/crm/leads/${id}/owner`, null, { params }),
  follow: (id: string, nextTime?: string) =>
    client.put<any, Result<void>>(`/crm/leads/${id}/follow`, null, { params: { nextTime } }),
  delete: (id: string) => client.delete<any, Result<void>>(`/crm/leads/${id}`),
  /** 条件查询(动态高级筛选)。 */
  search: (query: AdvancedQuery) => client.post<any, Result<PageResult<Lead>>>('/crm/leads/search', query),
}

// ── 客户 /crm/customers ──────────────────────────────────────────────────────
export const customerApi = {
  page: (params?: Q) => client.get<any, Result<PageResult<Customer>>>('/crm/customers', { params }),
  get: (id: string) => client.get<any, Result<Customer>>(`/crm/customers/${id}`),
  detail: (id: string) =>
    client.get<any, Result<{ customer: Customer; contacts: Contact[]; followUps: FollowUp[] }>>(`/crm/customers/${id}/detail`),
  create: (data: CustomerCommand) => client.post<any, Result<string>>('/crm/customers', data),
  update: (id: string, data: CustomerCommand) => client.put<any, Result<void>>(`/crm/customers/${id}`, data),
  claim: (id: string) => client.post<any, Result<void>>(`/crm/customers/${id}/claim`),
  release: (id: string, reason?: string) =>
    client.post<any, Result<void>>(`/crm/customers/${id}/release`, null, { params: { reason } }),
  transfer: (id: string, params: { ownerId: string; ownerName?: string; deptId?: string }) =>
    client.post<any, Result<void>>(`/crm/customers/${id}/transfer`, null, { params }),
  delete: (id: string) => client.delete<any, Result<void>>(`/crm/customers/${id}`),
  search: (query: AdvancedQuery) => client.post<any, Result<PageResult<Customer>>>('/crm/customers/search', query),
}

// ── 联系人 /crm/contacts ─────────────────────────────────────────────────────
export const contactApi = {
  page: (params?: Q) => client.get<any, Result<PageResult<Contact>>>('/crm/contacts', { params }),
  get: (id: string) => client.get<any, Result<Contact>>(`/crm/contacts/${id}`),
  create: (data: ContactCommand) => client.post<any, Result<string>>('/crm/contacts', data),
  update: (id: string, data: ContactCommand) => client.put<any, Result<void>>(`/crm/contacts/${id}`, data),
  setPrimary: (id: string) => client.put<any, Result<void>>(`/crm/contacts/${id}/primary`),
  delete: (id: string) => client.delete<any, Result<void>>(`/crm/contacts/${id}`),
  search: (query: AdvancedQuery) => client.post<any, Result<PageResult<Contact>>>('/crm/contacts/search', query),
}

// ── 跟进记录 /crm/followups ──────────────────────────────────────────────────
export const followUpApi = {
  page: (params?: Q) => client.get<any, Result<PageResult<FollowUp>>>('/crm/followups', { params }),
  get: (id: string) => client.get<any, Result<FollowUp>>(`/crm/followups/${id}`),
  save: (data: FollowUpCommand) => client.post<any, Result<string>>('/crm/followups', data),
  delete: (id: string) => client.delete<any, Result<void>>(`/crm/followups/${id}`),
  search: (query: AdvancedQuery) => client.post<any, Result<PageResult<FollowUp>>>('/crm/followups/search', query),
}

// ── 公海规则 /crm/pool-config ────────────────────────────────────────────────
export const poolConfigApi = {
  get: () => client.get<any, Result<PoolConfig>>('/crm/pool-config'),
  save: (data: PoolConfigCommand) => client.put<any, Result<void>>('/crm/pool-config', data),
}

// ── 商机 /crm/businesses ─────────────────────────────────────────────────────
export const businessApi = {
  page: (params?: Q) => client.get<any, Result<PageResult<Business>>>('/crm/businesses', { params }),
  detail: (id: string) => client.get<any, Result<BusinessDetail>>(`/crm/businesses/${id}`),
  create: (data: BusinessCommand) => client.post<any, Result<string>>('/crm/businesses', data),
  update: (id: string, data: BusinessCommand) => client.put<any, Result<void>>(`/crm/businesses/${id}`, data),
  /** 空 stage=逐级推进, 非空=直接到指定阶段。 */
  advance: (id: string, stage?: string) =>
    client.put<any, Result<void>>(`/crm/businesses/${id}/stage`, null, { params: { stage } }),
  win: (id: string, endRemark?: string) =>
    client.put<any, Result<void>>(`/crm/businesses/${id}/win`, null, { params: { endRemark } }),
  lose: (id: string, endRemark?: string) =>
    client.put<any, Result<void>>(`/crm/businesses/${id}/lose`, null, { params: { endRemark } }),
  delete: (id: string) => client.delete<any, Result<void>>(`/crm/businesses/${id}`),
  search: (query: AdvancedQuery) => client.post<any, Result<PageResult<Business>>>('/crm/businesses/search', query),
}

// ── 产品 /crm/products ───────────────────────────────────────────────────────
export const productApi = {
  page: (params?: Q) => client.get<any, Result<PageResult<Product>>>('/crm/products', { params }),
  onSale: () => client.get<any, Result<Product[]>>('/crm/products/on-sale'),
  get: (id: string) => client.get<any, Result<Product>>(`/crm/products/${id}`),
  create: (data: ProductCommand) => client.post<any, Result<string>>('/crm/products', data),
  update: (id: string, data: ProductCommand) => client.put<any, Result<void>>(`/crm/products/${id}`, data),
  switchStatus: (id: string, status: number) =>
    client.put<any, Result<void>>(`/crm/products/${id}/status`, null, { params: { status } }),
  delete: (id: string) => client.delete<any, Result<void>>(`/crm/products/${id}`),
  search: (query: AdvancedQuery) => client.post<any, Result<PageResult<Product>>>('/crm/products/search', query),
}

// ── 合同 /crm/contracts ──────────────────────────────────────────────────────
export const contractApi = {
  page: (params?: Q) => client.get<any, Result<PageResult<Contract>>>('/crm/contracts', { params }),
  detail: (id: string) => client.get<any, Result<ContractDetail>>(`/crm/contracts/${id}`),
  create: (data: ContractCommand) => client.post<any, Result<string>>('/crm/contracts', data),
  fromBusiness: (businessId: string) => client.post<any, Result<string>>(`/crm/contracts/from-business/${businessId}`),
  update: (id: string, data: ContractCommand) => client.put<any, Result<void>>(`/crm/contracts/${id}`, data),
  submit: (id: string) => client.put<any, Result<void>>(`/crm/contracts/${id}/submit`),
  archive: (id: string) => client.put<any, Result<void>>(`/crm/contracts/${id}/archive`),
  delete: (id: string) => client.delete<any, Result<void>>(`/crm/contracts/${id}`),
  search: (query: AdvancedQuery) => client.post<any, Result<PageResult<Contract>>>('/crm/contracts/search', query),
}

// ── 回款 /crm/repayments (计划 + 登记) ───────────────────────────────────────
export const repaymentApi = {
  pagePlans: (params?: Q) => client.get<any, Result<PageResult<PaymentPlan>>>('/crm/repayments/plans', { params }),
  listPlansByContract: (contractId: string) =>
    client.get<any, Result<PaymentPlan[]>>(`/crm/repayments/contracts/${contractId}/plans`),
  createPlan: (data: PaymentPlanCommand) => client.post<any, Result<string>>('/crm/repayments/plans', data),
  updatePlan: (id: string, data: PaymentPlanCommand) => client.put<any, Result<void>>(`/crm/repayments/plans/${id}`, data),
  deletePlan: (id: string) => client.delete<any, Result<void>>(`/crm/repayments/plans/${id}`),
  pageRepayments: (params?: Q) => client.get<any, Result<PageResult<Repayment>>>('/crm/repayments', { params }),
  listRepaymentsByPlan: (planId: string) =>
    client.get<any, Result<Repayment[]>>(`/crm/repayments/plans/${planId}/records`),
  register: (data: RepaymentCommand) => client.post<any, Result<string>>('/crm/repayments', data),
  deleteRepayment: (id: string) => client.delete<any, Result<void>>(`/crm/repayments/${id}`),
  /** 回款记录条件查询(动态高级筛选)。 */
  searchRecords: (query: AdvancedQuery) => client.post<any, Result<PageResult<Repayment>>>('/crm/repayments/search', query),
}

// ── 发票 /crm/invoices ───────────────────────────────────────────────────────
export const invoiceApi = {
  page: (params?: Q) => client.get<any, Result<PageResult<Invoice>>>('/crm/invoices', { params }),
  get: (id: string) => client.get<any, Result<Invoice>>(`/crm/invoices/${id}`),
  apply: (data: InvoiceCommand) => client.post<any, Result<string>>('/crm/invoices', data),
  issue: (id: string, params: { invoiceNo?: string; invoiceDate?: string }) =>
    client.put<any, Result<void>>(`/crm/invoices/${id}/issue`, null, { params }),
  reject: (id: string) => client.put<any, Result<void>>(`/crm/invoices/${id}/reject`),
  voidInvoice: (id: string) => client.put<any, Result<void>>(`/crm/invoices/${id}/void`),
  delete: (id: string) => client.delete<any, Result<void>>(`/crm/invoices/${id}`),
  search: (query: AdvancedQuery) => client.post<any, Result<PageResult<Invoice>>>('/crm/invoices/search', query),
}

// ── 售后 /crm/aftersales ─────────────────────────────────────────────────────
export const afterSaleApi = {
  page: (params?: Q) => client.get<any, Result<PageResult<AfterSale>>>('/crm/aftersales', { params }),
  detail: (id: string) => client.get<any, Result<AfterSaleDetail>>(`/crm/aftersales/${id}`),
  create: (data: AfterSaleCommand) => client.post<any, Result<string>>('/crm/aftersales', data),
  accept: (id: string) => client.put<any, Result<void>>(`/crm/aftersales/${id}/accept`),
  process: (id: string, content?: string) =>
    client.put<any, Result<void>>(`/crm/aftersales/${id}/process`, null, { params: { content } }),
  resolve: (id: string, content?: string) =>
    client.put<any, Result<void>>(`/crm/aftersales/${id}/resolve`, null, { params: { content } }),
  close: (id: string, content?: string) =>
    client.put<any, Result<void>>(`/crm/aftersales/${id}/close`, null, { params: { content } }),
  rate: (id: string, rating: number, remark?: string) =>
    client.put<any, Result<void>>(`/crm/aftersales/${id}/rate`, null, { params: { rating, remark } }),
  delete: (id: string) => client.delete<any, Result<void>>(`/crm/aftersales/${id}`),
  search: (query: AdvancedQuery) => client.post<any, Result<PageResult<AfterSale>>>('/crm/aftersales/search', query),
}

// ── 审批 /crm/approvals ──────────────────────────────────────────────────────
export const approvalApi = {
  pageMine: (params?: Q) => client.get<any, Result<PageResult<Approval>>>('/crm/approvals/mine', { params }),
  pageTodo: (params?: Q) => client.get<any, Result<PageResult<Approval>>>('/crm/approvals/todo', { params }),
  detail: (id: string) => client.get<any, Result<ApprovalDetail>>(`/crm/approvals/${id}`),
  submitContract: (contractId: string) => client.post<any, Result<string>>(`/crm/approvals/contract/${contractId}`),
  approve: (id: string, remark?: string) =>
    client.put<any, Result<void>>(`/crm/approvals/${id}/approve`, null, { params: { remark } }),
  reject: (id: string, remark?: string) =>
    client.put<any, Result<void>>(`/crm/approvals/${id}/reject`, null, { params: { remark } }),
  cancel: (id: string, remark?: string) =>
    client.put<any, Result<void>>(`/crm/approvals/${id}/cancel`, null, { params: { remark } }),
}

// ── 待办 /crm/tasks ──────────────────────────────────────────────────────────
export const taskApi = {
  page: (params?: Q) => client.get<any, Result<PageResult<CrmTask>>>('/crm/tasks', { params }),
  today: () => client.get<any, Result<CrmTask[]>>('/crm/tasks/today'),
  get: (id: string) => client.get<any, Result<CrmTask>>(`/crm/tasks/${id}`),
  create: (data: TaskCommand) => client.post<any, Result<string>>('/crm/tasks', data),
  update: (id: string, data: TaskCommand) => client.put<any, Result<void>>(`/crm/tasks/${id}`, data),
  complete: (id: string) => client.put<any, Result<void>>(`/crm/tasks/${id}/done`),
  cancel: (id: string) => client.put<any, Result<void>>(`/crm/tasks/${id}/cancel`),
  delete: (id: string) => client.delete<any, Result<void>>(`/crm/tasks/${id}`),
  search: (query: AdvancedQuery) => client.post<any, Result<PageResult<CrmTask>>>('/crm/tasks/search', query),
}

// ── 条件查询支撑 /crm/query-fields + /crm/saved-filters ───────────────────
export const queryApi = {
  /** 可查字段元数据(按模块)。 */
  fields: (module: string) => client.get<any, Result<QueryFieldMeta[]>>('/crm/query-fields', { params: { module } }),
  /** 个人常用筛选 CRUD(按登录用户隔离)。 */
  savedFilter: {
    list: (module?: string) => client.get<any, Result<SavedFilterVO[]>>('/crm/saved-filters', { params: { module } }),
    save: (data: SavedFilterRequest) => client.post<any, Result<string>>('/crm/saved-filters', data),
    remove: (id: string) => client.delete<any, Result<void>>(`/crm/saved-filters/${id}`),
  },
}

// ── 看板统计 /crm/stats ──────────────────────────────────────────────────────
export const statsApi = {
  overview: () => client.get<any, Result<CrmDashboard>>('/crm/stats/overview'),
  funnel: () => client.get<any, Result<FunnelStage[]>>('/crm/stats/funnel'),
  memberRanking: (limit = 10) =>
    client.get<any, Result<MemberStat[]>>('/crm/stats/member-ranking', { params: { limit } }),
  trend: (months = 6) => client.get<any, Result<MonthTrend[]>>('/crm/stats/trend', { params: { months } }),
}

// ── AI 洞察 /crm/ai ──────────────────────────────────────────────────────────
export const crmAiApi = {
  insight: (customerId: string) => client.get<any, Result<CustomerInsight>>(`/crm/ai/insight/${customerId}`),
}

/** Aggregated CRM API namespace. */
export const crmApi = {
  lead: leadApi,
  customer: customerApi,
  contact: contactApi,
  followUp: followUpApi,
  poolConfig: poolConfigApi,
  business: businessApi,
  product: productApi,
  contract: contractApi,
  repayment: repaymentApi,
  invoice: invoiceApi,
  afterSale: afterSaleApi,
  approval: approvalApi,
  task: taskApi,
  stats: statsApi,
  ai: crmAiApi,
  query: queryApi,
}
