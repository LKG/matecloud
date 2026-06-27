/*
 * Copyright (c) 2024-2026 Beijing Daotiandi Technology Co., Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package vip.mate.system.admin.application.model;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vip.mate.base.channel.FieldSpec;
import vip.mate.base.channel.ProviderDescriptor;
import vip.mate.base.exception.BizException;
import vip.mate.base.model.ModelEndpoint;
import vip.mate.starter.channel.ChannelCrypto;
import vip.mate.starter.tenant.core.TenantContext;
import vip.mate.system.admin.application.model.ModelProviderDescriptors.VendorSpec;
import vip.mate.system.admin.infrastructure.dao.ModelDefaultDao;
import vip.mate.system.admin.infrastructure.dao.ModelGatewayDao;
import vip.mate.system.admin.infrastructure.dao.ModelProviderDao;
import vip.mate.system.admin.infrastructure.dao.po.ModelDefaultPO;
import vip.mate.system.admin.infrastructure.dao.po.ModelGatewayPO;
import vip.mate.system.admin.infrastructure.dao.po.ModelProviderPO;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

/**
 * Admin management of AI model configuration: providers (credentials), per-type
 * defaults, and the optional OpenAI-compatible gateway.
 *
 * <p>Mirrors {@code ChannelConfigService}: each provider's credential fields come from a
 * self-describing {@link ProviderDescriptor} ({@link ModelProviderDescriptors}); the
 * stored {@code config_json} is AES-encrypted as a whole via {@link ChannelCrypto},
 * secrets are masked on read and preserved on write (blank / masked = keep stored).
 * Tenant resolution is {@code TENANT} → {@code GLOBAL}.
 *
 * @author mateaix
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ModelConfigService {

    /** Masked placeholder returned for secret fields that have a stored value. */
    public static final String MASK = "******";

    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    private final ModelProviderDao providerDao;
    private final ModelDefaultDao defaultDao;
    private final ModelGatewayDao gatewayDao;
    private final ObjectMapper objectMapper;
    private final ChannelCrypto crypto;

    // ---- views ----

    public record ProviderView(String id, String vendor, String name, List<String> modalities,
                               boolean enabled, Integer sort, List<FieldSpec> fields,
                               Map<String, Object> values, List<String> models) {}

    public record DescriptorView(String vendor, String name, String describe,
                                 List<String> modalities, List<FieldSpec> fields,
                                 List<String> suggestedModels) {}

    public record SystemModelView(String modelType, String providerId, String providerName,
                                  String vendor, String model) {}

    public record GatewayView(boolean enabled, String gatewayType, String baseUrl,
                              String token, String defaultGroup, String modelMapping) {}

    public record TestResult(boolean success, String message) {}

    // ---- providers ----

    /** List configured providers for the tenant, with secret fields masked. */
    public List<ProviderView> providers(String tenant) {
        List<ModelProviderPO> rows = providerDao.selectList(new LambdaQueryWrapper<ModelProviderPO>()
                .eq(ModelProviderPO::getTenantId, tenant)
                .orderByAsc(ModelProviderPO::getSort));
        List<ProviderView> out = new ArrayList<>(rows.size());
        for (ModelProviderPO po : rows) {
            VendorSpec spec = ModelProviderDescriptors.get(po.getVendor());
            List<FieldSpec> fields = spec == null ? List.of() : spec.descriptor().getFields();
            out.add(new ProviderView(po.getId(), po.getVendor(), po.getName(),
                    splitModalities(po.getModalities()),
                    Integer.valueOf(1).equals(po.getEnabled()), po.getSort(),
                    fields, maskedValues(fields, po.getConfigJson()),
                    parseModels(po.getModels())));
        }
        return out;
    }

    /**
     * Create (id blank) or update a provider. Secret fields left blank / masked keep
     * the stored value; {@code config_json} is encrypted as a whole.
     */
    @Transactional(rollbackFor = Exception.class)
    public String saveProvider(String id, String vendor, String name, List<String> modalities,
                               boolean enabled, Integer sort, Map<String, Object> values,
                               List<String> models, String tenant) {
        VendorSpec spec = ModelProviderDescriptors.get(vendor);
        if (spec == null) {
            throw new BizException("SYSA001", "Unknown model vendor: " + vendor);
        }
        ProviderDescriptor descriptor = spec.descriptor();

        ModelProviderPO po = (id == null || id.isBlank()) ? null : providerDao.selectById(id);
        Map<String, Object> stored = parse(po == null ? null : po.getConfigJson());

        Map<String, Object> merged = new LinkedHashMap<>(stored);
        if (values != null) {
            for (FieldSpec f : descriptor.getFields()) {
                Object incoming = values.get(f.key());
                if (f.secret()) {
                    String s = incoming == null ? null : String.valueOf(incoming);
                    if (s == null || s.isBlank() || MASK.equals(s)) {
                        continue; // keep stored secret
                    }
                    merged.put(f.key(), s);
                } else if (values.containsKey(f.key())) {
                    merged.put(f.key(), incoming);
                }
            }
        }

        String json;
        try {
            json = crypto.encrypt(objectMapper.writeValueAsString(merged));
        } catch (Exception e) {
            throw new BizException("SYSA002", "Invalid model config: " + e.getMessage());
        }

        String modalitiesCsv = modalities == null ? null : String.join(",", modalities);
        String modelsJson = writeModels(models);
        if (po == null) {
            po = new ModelProviderPO();
            po.setTenantId(tenant);
            po.setScope("0".equals(tenant) ? "GLOBAL" : "TENANT");
            po.setVendor(spec.vendor().name());
            po.setName(name);
            po.setModalities(modalitiesCsv);
            po.setConfigJson(json);
            po.setModels(modelsJson);
            po.setEnabled(enabled ? 1 : 0);
            po.setSort(sort == null ? 0 : sort);
            providerDao.insert(po);
        } else {
            po.setVendor(spec.vendor().name());
            po.setName(name);
            po.setModalities(modalitiesCsv);
            po.setConfigJson(json);
            po.setModels(modelsJson);
            po.setEnabled(enabled ? 1 : 0);
            po.setSort(sort == null ? po.getSort() : sort);
            providerDao.updateById(po);
        }
        log.info("[model] saved provider {} vendor={} enabled={}", po.getId(), vendor, enabled);
        return po.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteProvider(String id) {
        providerDao.deleteById(id);
        // Drop any default assignment that pointed at it.
        defaultDao.delete(new LambdaQueryWrapper<ModelDefaultPO>()
                .eq(ModelDefaultPO::getProviderId, id));
        log.info("[model] deleted provider {}", id);
    }

    /**
     * Live-fetch the model list from the provider using its stored credentials, by
     * calling the vendor's OpenAI-compatible {@code /models} endpoint. The
     * static {@link ModelCatalog} stays only as an offline fallback / new-provider prefill.
     *
     * <p>Credentials are decrypted server-side only; the cleartext key never leaves the
     * service. On any network / auth / parse failure a {@link BizException} carries the
     * root cause so the operator sees why, and manual model maintenance still works.
     */
    public List<String> fetchModels(String id) {
        ModelProviderPO po = providerDao.selectById(id);
        if (po == null) {
            throw new BizException("SYSA001", "Provider not found: " + id);
        }
        Map<String, Object> cfg = parse(po.getConfigJson());
        ModelVendor vendor;
        try {
            vendor = ModelVendor.valueOf(po.getVendor().trim().toUpperCase());
        } catch (Exception e) {
            vendor = ModelVendor.CUSTOM;
        }
        try {
            return switch (vendor) {
                case OLLAMA -> fetchOllama(cfg);
                case ANTHROPIC -> fetchAnthropic(cfg);
                case AZURE -> fetchAzure(cfg);
                default -> fetchOpenAiCompatible(cfg);
            };
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            String detail = explain(e);
            log.warn("[model] fetch-models failed provider={} vendor={} — {}", id, vendor, detail);
            throw new BizException("SYSE001", "拉取模型列表失败:" + detail);
        }
    }

    /** Ollama native: {@code GET {baseUrl}/api/tags} → models[].name / .model. */
    private List<String> fetchOllama(Map<String, Object> cfg) throws Exception {
        String base = requireBase(str(cfg.get("baseUrl")));
        String url = trimSlash(base) + "/api/tags";
        var root = getJson(url, Map.of());
        List<String> out = new ArrayList<>();
        var models = root.get("models");
        if (models != null && models.isArray()) {
            for (var m : models) {
                String v = text(m, "name");
                if (v == null) {
                    v = text(m, "model");
                }
                if (v != null) {
                    out.add(v);
                }
            }
        }
        return finish(out);
    }

    /** Anthropic: {@code GET {baseUrl}/v1/models} with x-api-key + anthropic-version. */
    private List<String> fetchAnthropic(Map<String, Object> cfg) throws Exception {
        String base = requireBase(str(cfg.get("baseUrl")));
        String url = trimSlash(base) + "/v1/models";
        Map<String, String> headers = new LinkedHashMap<>();
        String key = str(cfg.get("apiKey"));
        if (key != null && !key.isBlank()) {
            headers.put("x-api-key", key);
        }
        headers.put("anthropic-version", "2023-06-01");
        var root = getJson(url, headers);
        return finish(collectData(root, "id"));
    }

    /** Azure OpenAI: {@code GET {endpoint}/openai/models?api-version=...} with api-key. */
    private List<String> fetchAzure(Map<String, Object> cfg) throws Exception {
        String endpoint = requireBase(firstNonBlank(str(cfg.get("endpoint")), str(cfg.get("baseUrl"))));
        String apiVersion = firstNonBlank(str(cfg.get("apiVersion")), "2024-02-15-preview");
        String url = trimSlash(endpoint) + "/openai/models?api-version=" + apiVersion;
        Map<String, String> headers = new LinkedHashMap<>();
        String key = str(cfg.get("apiKey"));
        if (key != null && !key.isBlank()) {
            headers.put("api-key", key);
        }
        var root = getJson(url, headers);
        return finish(collectData(root, "id"));
    }

    /** OpenAI-compatible (default): {@code GET {base}/models} or {@code {base}/v1/models}. */
    private List<String> fetchOpenAiCompatible(Map<String, Object> cfg) throws Exception {
        String base = requireBase(firstNonBlank(str(cfg.get("baseUrl")),
                str(cfg.get("endpoint"))));
        String trimmed = trimSlash(base);
        String url = trimmed.endsWith("/v1") ? trimmed + "/models" : trimmed + "/v1/models";
        Map<String, String> headers = new LinkedHashMap<>();
        String key = firstNonBlank(str(cfg.get("apiKey")), str(cfg.get("token")));
        if (key != null && !key.isBlank()) {
            headers.put("Authorization", "Bearer " + key);
        }
        var root = getJson(url, headers);
        return finish(collectData(root, "id"));
    }

    /** Collect {@code data[].<field>} ids from a JSON envelope. */
    private List<String> collectData(JsonNode root, String field) {
        List<String> out = new ArrayList<>();
        var data = root.get("data");
        if (data != null && data.isArray()) {
            for (var n : data) {
                String v = text(n, field);
                if (v != null) {
                    out.add(v);
                }
            }
        }
        return out;
    }

    private static String text(JsonNode node, String field) {
        if (node == null) {
            return null;
        }
        var v = node.get(field);
        if (v == null || v.isNull()) {
            return null;
        }
        String s = v.asText();
        return (s == null || s.isBlank()) ? null : s.trim();
    }

    /** GET a URL with headers (8s read timeout), parse the body as a JSON tree. */
    private JsonNode getJson(String url, Map<String, String> headers)
            throws Exception {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(8))
                .header("Accept", "application/json")
                .GET();
        headers.forEach(b::header);
        HttpResponse<String> resp = HTTP.send(b.build(), HttpResponse.BodyHandlers.ofString());
        int code = resp.statusCode();
        if (code < 200 || code >= 300) {
            throw new BizException("SYSE001", "供应商返回 HTTP " + code
                    + (resp.body() == null || resp.body().isBlank() ? "" : ": " + trunc(resp.body())));
        }
        return objectMapper.readTree(resp.body());
    }

    /** De-duplicate + sort the collected ids (empty result is itself a failure). */
    private List<String> finish(List<String> ids) {
        TreeSet<String> set = new TreeSet<>(ids);
        if (set.isEmpty()) {
            throw new BizException("SYSE001", "供应商未返回任何模型");
        }
        return new ArrayList<>(set);
    }

    private static String requireBase(String base) {
        if (base == null || base.isBlank()) {
            throw new BizException("SYSA001", "Base URL not configured");
        }
        return base.trim();
    }

    private static String trimSlash(String s) {
        String t = s.trim();
        while (t.endsWith("/")) {
            t = t.substring(0, t.length() - 1);
        }
        return t;
    }

    private static String trunc(String s) {
        String t = s.strip();
        return t.length() > 200 ? t.substring(0, 200) + "…" : t;
    }

    /** Lightweight connectivity probe against the provider's base URL (no Spring AI). */
    public TestResult testProvider(String id) {
        ModelProviderPO po = providerDao.selectById(id);
        if (po == null) {
            throw new BizException("SYSA001", "Provider not found: " + id);
        }
        Map<String, Object> cfg = parse(po.getConfigJson());
        String baseUrl = firstNonBlank(str(cfg.get("baseUrl")), str(cfg.get("endpoint")));
        return probe(baseUrl);
    }

    // ---- descriptors / modalities ----

    public List<DescriptorView> descriptors() {
        List<DescriptorView> out = new ArrayList<>();
        for (VendorSpec spec : ModelProviderDescriptors.all()) {
            ProviderDescriptor d = spec.descriptor();
            out.add(new DescriptorView(spec.vendor().name(), d.getName(), d.getDescribe(),
                    spec.modalities().stream().map(Enum::name).toList(), d.getFields(),
                    ModelCatalog.suggested(spec.vendor())));
        }
        return out;
    }

    public List<String> modalities() {
        return Stream.of(ModelType.values()).map(Enum::name).toList();
    }

    // ---- system models (defaults per type) ----

    /** For each model type, the configured default (joined with provider name/vendor). */
    public List<SystemModelView> systemModels(String tenant) {
        List<ModelDefaultPO> defaults = defaultDao.selectList(new LambdaQueryWrapper<ModelDefaultPO>()
                .eq(ModelDefaultPO::getTenantId, tenant));
        Map<String, ModelDefaultPO> byType = new LinkedHashMap<>();
        for (ModelDefaultPO d : defaults) {
            byType.put(d.getModelType(), d);
        }
        List<SystemModelView> out = new ArrayList<>();
        for (ModelType type : ModelType.values()) {
            ModelDefaultPO d = byType.get(type.name());
            if (d == null) {
                out.add(new SystemModelView(type.name(), null, null, null, null));
                continue;
            }
            ModelProviderPO p = providerDao.selectById(d.getProviderId());
            out.add(new SystemModelView(type.name(), d.getProviderId(),
                    p == null ? null : p.getName(), p == null ? null : p.getVendor(), d.getModel()));
        }
        return out;
    }

    /** Assign (or update) the default provider+model for a model type. */
    @Transactional(rollbackFor = Exception.class)
    public void saveSystemModel(String modelType, String providerId, String model, String tenant) {
        ModelType type = parseType(modelType);
        if (providerDao.selectById(providerId) == null) {
            throw new BizException("SYSA001", "Provider not found: " + providerId);
        }
        ModelDefaultPO existing = defaultDao.selectOne(new LambdaQueryWrapper<ModelDefaultPO>()
                .eq(ModelDefaultPO::getTenantId, tenant)
                .eq(ModelDefaultPO::getModelType, type.name())
                .last("LIMIT 1"));
        if (existing == null) {
            ModelDefaultPO po = new ModelDefaultPO();
            po.setTenantId(tenant);
            po.setModelType(type.name());
            po.setProviderId(providerId);
            po.setModel(model);
            defaultDao.insert(po);
        } else {
            existing.setProviderId(providerId);
            existing.setModel(model);
            defaultDao.updateById(existing);
        }
        log.info("[model] default {} -> provider={} model={}", type, providerId, model);
    }

    // ---- gateway ----

    public GatewayView gateway(String tenant) {
        ModelGatewayPO po = findGateway(tenant);
        if (po == null) {
            return new GatewayView(false, "NEW_API", null, "", null, null);
        }
        boolean hasToken = po.getTokenCipher() != null && !po.getTokenCipher().isBlank();
        return new GatewayView(Integer.valueOf(1).equals(po.getEnabled()), po.getGatewayType(),
                po.getBaseUrl(), hasToken ? MASK : "", po.getDefaultGroup(), po.getModelMapping());
    }

    /** Save gateway config; blank / masked token keeps the stored one. */
    @Transactional(rollbackFor = Exception.class)
    public void saveGateway(boolean enabled, String gatewayType, String baseUrl, String token,
                            String defaultGroup, String modelMapping, String tenant) {
        ModelGatewayPO po = findGateway(tenant);
        if (po == null) {
            po = new ModelGatewayPO();
            po.setTenantId(tenant);
        }
        po.setEnabled(enabled ? 1 : 0);
        po.setGatewayType(gatewayType == null ? "NEW_API" : gatewayType);
        po.setBaseUrl(baseUrl);
        po.setDefaultGroup(defaultGroup);
        po.setModelMapping(modelMapping);
        if (token != null && !token.isBlank() && !MASK.equals(token)) {
            po.setTokenCipher(crypto.encrypt(token));
        }
        if (po.getId() == null) {
            gatewayDao.insert(po);
        } else {
            gatewayDao.updateById(po);
        }
        log.info("[model] saved gateway tenant={} enabled={}", tenant, enabled);
    }

    public TestResult testGateway(String tenant) {
        ModelGatewayPO po = findGateway(tenant);
        if (po == null || po.getBaseUrl() == null || po.getBaseUrl().isBlank()) {
            throw new BizException("SYSA001", "Gateway base URL not configured");
        }
        return probe(po.getBaseUrl());
    }

    // ---- internal resolve (spec only, no secret to UI) ----

    /**
     * Resolve the endpoint spec for a model type (TENANT → GLOBAL). Credentials are not
     * decrypted here; the value object carries only the routing spec for internal use.
     */
    public ModelEndpoint resolve(String modelType, String tenant) {
        ModelType type = parseType(modelType);
        ModelGatewayPO gw = findGateway(tenant);
        if (gw != null && Integer.valueOf(1).equals(gw.getEnabled())
                && gw.getBaseUrl() != null && !gw.getBaseUrl().isBlank()) {
            return new ModelEndpoint(ModelEndpoint.SUPPLY_GATEWAY, ModelVendor.GATEWAY.name(),
                    gw.getBaseUrl(), null, null);
        }
        ModelDefaultPO def = defaultDao.selectOne(new LambdaQueryWrapper<ModelDefaultPO>()
                .eq(ModelDefaultPO::getTenantId, tenant)
                .eq(ModelDefaultPO::getModelType, type.name())
                .last("LIMIT 1"));
        if (def == null && !"0".equals(tenant)) {
            def = defaultDao.selectOne(new LambdaQueryWrapper<ModelDefaultPO>()
                    .eq(ModelDefaultPO::getTenantId, "0")
                    .eq(ModelDefaultPO::getModelType, type.name())
                    .last("LIMIT 1"));
        }
        if (def == null) {
            return null;
        }
        ModelProviderPO p = providerDao.selectById(def.getProviderId());
        if (p == null) {
            return null;
        }
        Map<String, Object> cfg = parse(p.getConfigJson());
        String baseUrl = firstNonBlank(str(cfg.get("baseUrl")), str(cfg.get("endpoint")));
        return new ModelEndpoint(ModelEndpoint.SUPPLY_LOCAL, p.getVendor(), baseUrl, null, def.getModel());
    }

    // ---- helpers ----

    public String currentTenant() {
        String t = TenantContext.getTenantId();
        return (t == null || t.isBlank()) ? "0" : t;
    }

    private ModelGatewayPO findGateway(String tenant) {
        return gatewayDao.selectOne(new LambdaQueryWrapper<ModelGatewayPO>()
                .eq(ModelGatewayPO::getTenantId, tenant)
                .last("LIMIT 1"));
    }

    private ModelType parseType(String modelType) {
        try {
            return ModelType.valueOf(modelType == null ? "" : modelType.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BizException("SYSA001", "Unknown model type: " + modelType);
        }
    }

    /** HTTP HEAD/GET probe; any answered status (even 4xx) proves reachability. */
    private TestResult probe(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new BizException("SYSA001", "Base URL not configured");
        }
        try {
            HttpRequest req = HttpRequest.newBuilder(URI.create(baseUrl.trim()))
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();
            HttpResponse<Void> resp = HTTP.send(req, HttpResponse.BodyHandlers.discarding());
            return new TestResult(true, "连接成功(HTTP " + resp.statusCode() + ")");
        } catch (Exception e) {
            String detail = explain(e);
            log.warn("[model] probe failed {} — {}", baseUrl, detail);
            return new TestResult(false, detail);
        }
    }

    /** Root-cause-aware failure message (mirrors ChannelConfigService#explain). */
    private static String explain(Throwable e) {
        Throwable root = e;
        Set<Throwable> seen = new HashSet<>();
        while (root.getCause() != null && root.getCause() != root && seen.add(root)) {
            root = root.getCause();
        }
        String rootMsg = root.getMessage();
        return (rootMsg == null || rootMsg.isBlank())
                ? root.getClass().getSimpleName()
                : root.getClass().getSimpleName() + ": " + rootMsg;
    }

    private Map<String, Object> maskedValues(List<FieldSpec> fields, String configJson) {
        Map<String, Object> stored = parse(configJson);
        Map<String, Object> out = new LinkedHashMap<>();
        for (FieldSpec f : fields) {
            Object v = stored.get(f.key());
            if (f.secret()) {
                out.put(f.key(), (v != null && !String.valueOf(v).isBlank()) ? MASK : "");
            } else if (v != null) {
                out.put(f.key(), v);
            }
        }
        return out;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parse(String json) {
        if (json == null || json.isBlank()) {
            return new LinkedHashMap<>();
        }
        try {
            return objectMapper.readValue(crypto.decrypt(json), Map.class);
        } catch (Exception e) {
            log.warn("[model] bad config_json, treating as empty: {}", e.getMessage());
            return new LinkedHashMap<>();
        }
    }

    /** Parse the stored {@code models} JSON array into a list (null / blank / bad → empty). */
    private List<String> parseModels(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            List<String> list = objectMapper.readValue(json, new TypeReference<List<String>>() {});
            return list == null ? List.of() : list;
        } catch (Exception e) {
            log.warn("[model] bad models json, treating as empty: {}", e.getMessage());
            return List.of();
        }
    }

    /** Serialize the model id list to a JSON array (null / empty → null column). */
    private String writeModels(List<String> models) {
        if (models == null || models.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(models);
        } catch (Exception e) {
            throw new BizException("SYSA002", "Invalid models list: " + e.getMessage());
        }
    }

    private static List<String> splitModalities(String csv) {
        if (csv == null || csv.isBlank()) {
            return List.of();
        }
        return Arrays.stream(csv.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
    }

    private static String str(Object o) {
        return o == null ? null : String.valueOf(o);
    }

    private static String firstNonBlank(String... vals) {
        for (String v : vals) {
            if (v != null && !v.isBlank()) {
                return v;
            }
        }
        return null;
    }
}
