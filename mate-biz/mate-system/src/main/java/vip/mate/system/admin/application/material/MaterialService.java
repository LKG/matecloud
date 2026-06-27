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
package vip.mate.system.admin.application.material;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import vip.mate.base.exception.BizException;
import vip.mate.base.result.PageResult;
import vip.mate.starter.file.core.FileTemplate;
import vip.mate.starter.file.model.MultipartUpload;
import vip.mate.starter.file.model.UploadResult;
import vip.mate.starter.tenant.core.TenantContext;
import vip.mate.system.admin.infrastructure.dao.MaterialCategoryDao;
import vip.mate.system.admin.infrastructure.dao.MaterialDao;
import vip.mate.system.admin.infrastructure.dao.po.MaterialCategoryPO;
import vip.mate.system.admin.infrastructure.dao.po.MaterialPO;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 素材管理:分组(CRUD)+ 素材(分页/上传/重命名/移动/删除/预签名)。
 * 素材内容存入当前启用的对象存储({@link FileTemplate}),仅元数据落库。
 *
 * @author mateaix
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MaterialService {

    /** 未分组的虚拟分组标识(前端用) */
    public static final String UNGROUPED = "0";

    private static final Set<String> IMAGE_EXT = Set.of("jpg", "jpeg", "png", "gif", "webp", "bmp");
    private static final Set<String> VIDEO_EXT = Set.of("mp4", "mov", "webm", "mkv", "avi", "m4v");
    private static final Set<String> DOC_EXT = Set.of("doc", "docx", "xls", "xlsx", "ppt", "pptx",
            "pdf", "txt", "csv", "md", "zip", "rar", "7z");

    private static final Map<String, Set<String>> WHITELIST = Map.of(
            "image", IMAGE_EXT, "video", VIDEO_EXT, "doc", DOC_EXT);
    private static final Map<String, Long> MAX_SIZE = Map.of(
            "image", 10L * 1024 * 1024, "video", 200L * 1024 * 1024, "doc", 50L * 1024 * 1024);

    private final MaterialDao materialDao;
    private final MaterialCategoryDao categoryDao;
    private final FileTemplate fileTemplate;

    // ---- VOs ----

    public record CategoryVO(String id, String name, Integer sort, long count) {}

    public record CategoryListVO(long total, long ungrouped, List<CategoryVO> categories) {}

    public record MaterialVO(String id, String type, String categoryId, String name, String objectName,
                             String url, long size, String ext, String mime, String uploadedBy,
                             LocalDateTime createdAt) {}

    /** 分片上传初始化结果 —— 前端持有 objectName + uploadId 逐片直传,最后回传 complete。 */
    public record MultipartInitVO(String bucket, String objectName, String uploadId) {}

    // ---- 分组 ----

    public CategoryListVO categories(String type) {
        checkType(type);
        String tenant = currentTenant();
        Map<String, Long> counts = counts(type, tenant);
        long total = counts.values().stream().mapToLong(Long::longValue).sum();
        long ungrouped = counts.getOrDefault(null, 0L);

        List<CategoryVO> list = categoryDao.selectList(new LambdaQueryWrapper<MaterialCategoryPO>()
                        .eq(MaterialCategoryPO::getTenantId, tenant)
                        .eq(MaterialCategoryPO::getType, type)
                        .orderByAsc(MaterialCategoryPO::getSort)
                        .orderByAsc(MaterialCategoryPO::getCreatedAt))
                .stream()
                .map(c -> new CategoryVO(c.getId(), c.getName(), c.getSort(),
                        counts.getOrDefault(c.getId(), 0L)))
                .toList();
        return new CategoryListVO(total, ungrouped, list);
    }

    @Transactional(rollbackFor = Exception.class)
    public String createCategory(String type, String name) {
        checkType(type);
        if (name == null || name.isBlank()) {
            throw new BizException("SYSA001", "分组名不能为空");
        }
        MaterialCategoryPO po = new MaterialCategoryPO();
        po.setTenantId(currentTenant());
        po.setType(type);
        po.setName(name.trim());
        po.setSort(0);
        categoryDao.insert(po);
        return po.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void renameCategory(String id, String name) {
        if (name == null || name.isBlank()) {
            throw new BizException("SYSA001", "分组名不能为空");
        }
        MaterialCategoryPO po = categoryDao.selectById(id);
        if (po == null) {
            throw new BizException("SYSB002", "分组不存在");
        }
        po.setName(name.trim());
        categoryDao.updateById(po);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteCategory(String id) {
        MaterialPO move = new MaterialPO();
        move.setCategoryId(null);
        // 该分组下素材移到「未分组」(category_id 置空),再删分组
        materialDao.update(move, new LambdaQueryWrapper<MaterialPO>()
                .eq(MaterialPO::getCategoryId, id));
        categoryDao.deleteById(id);
    }

    // ---- 素材 ----

    public PageResult<MaterialVO> page(String type, String categoryId, String keyword,
                                       int pageNum, int pageSize) {
        checkType(type);
        LambdaQueryWrapper<MaterialPO> w = new LambdaQueryWrapper<MaterialPO>()
                .eq(MaterialPO::getTenantId, currentTenant())
                .eq(MaterialPO::getType, type);
        if (UNGROUPED.equals(categoryId)) {
            w.isNull(MaterialPO::getCategoryId);
        } else if (categoryId != null && !categoryId.isBlank()) {
            w.eq(MaterialPO::getCategoryId, categoryId);
        }
        if (keyword != null && !keyword.isBlank()) {
            w.like(MaterialPO::getName, keyword.trim());
        }
        w.orderByDesc(MaterialPO::getCreatedAt);
        Page<MaterialPO> page = materialDao.selectPage(new Page<>(pageNum, pageSize), w);
        List<MaterialVO> list = page.getRecords().stream().map(this::toVO).toList();
        return PageResult.of(list, page.getTotal());
    }

    /**
     * 单次整传(走应用服务器)。受 servlet multipart 上限约束(见 mate-defaults.yml),
     * 大文件(尤其视频)请走分片直传 {@link #initMultipart}/{@link #completeMultipart}。
     */
    @Transactional(rollbackFor = Exception.class)
    public MaterialVO upload(MultipartFile file, String type, String categoryId) {
        checkType(type);
        if (file == null || file.isEmpty()) {
            throw new BizException("SYSA001", "文件不能为空");
        }
        String originalName = file.getOriginalFilename() == null ? "file" : file.getOriginalFilename();
        String ext = validate(type, originalName, file.getSize());

        UploadResult r = fileTemplate.upload(file);

        MaterialPO po = persist(type, categoryId, originalName, ext, file.getContentType(),
                r.objectName(), r.url(), file.getSize());
        log.info("[material] uploaded(single) type={} name={} object={}", type, originalName, r.objectName());
        return toVO(po);
    }

    // ---- 分片上传(大文件浏览器直传对象存储,不经应用服务器)----

    /**
     * 初始化分片上传。先按 type 校验扩展名与声明大小(快速失败),再向对象存储申请 uploadId。
     * {@code declaredSize} 为前端声明的文件总大小,用于尽早拦截超限。
     */
    public MultipartInitVO initMultipart(String type, String filename, String contentType, long declaredSize) {
        checkType(type);
        if (!fileTemplate.supportsMultipart()) {
            throw new BizException("SYSA002", "当前对象存储不支持分片上传");
        }
        String originalName = (filename == null || filename.isBlank()) ? "file" : filename;
        validate(type, originalName, declaredSize);
        MultipartUpload mu = fileTemplate.initMultipartUpload(null, contentType);
        log.info("[material] init multipart type={} name={} object={} uploadId={}",
                type, originalName, mu.objectName(), mu.uploadId());
        return new MultipartInitVO(mu.bucket(), mu.objectName(), mu.uploadId());
    }

    /** 取某一分片(1-based)的预签名直传 URL。 */
    public String partUrl(String objectName, String uploadId, int partNumber, int expirySeconds) {
        return fileTemplate.presignedPartUrl(null, objectName, uploadId, partNumber,
                Duration.ofSeconds(expirySeconds));
    }

    /**
     * 合并分片并落素材元数据。落库大小以对象存储合并后的<b>真实字节数</b>为准
     * (而非前端声明值),并据此复核上限,杜绝谎报 size 绕过限制。
     *
     * <p>不加 {@code @Transactional}:合并是一次网络调用,不应占着 DB 连接;落库仅单条
     * insert,本身即原子。
     */
    public MaterialVO completeMultipart(String type, String categoryId, String objectName, String uploadId,
                                        String filename, long declaredSize, String contentType) {
        checkType(type);
        String originalName = (filename == null || filename.isBlank()) ? "file" : filename;
        String ext = checkExt(type, originalName);

        UploadResult r = fileTemplate.completeMultipartUpload(null, objectName, uploadId);
        long realSize = r.size();

        // 以真实大小复核上限:超限则删除已合并对象,避免落库 + 留存超大文件
        if (realSize > MAX_SIZE.get(type)) {
            safeDelete(r.objectName());
            throw new BizException("SYSA002", "文件超出大小限制(" + (MAX_SIZE.get(type) / 1024 / 1024) + "MB)");
        }

        MaterialPO po = persist(type, categoryId, originalName, ext, contentType,
                r.objectName(), r.url(), realSize);
        log.info("[material] uploaded(multipart) type={} name={} object={} size={}",
                type, originalName, r.objectName(), realSize);
        return toVO(po);
    }

    /** 放弃分片上传,释放已上传分片(无落库记录,直接丢弃)。 */
    public void abortMultipart(String objectName, String uploadId) {
        fileTemplate.abortMultipartUpload(null, objectName, uploadId);
        log.info("[material] abort multipart object={} uploadId={}", objectName, uploadId);
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(String id, String name, String categoryId, boolean moveCategory) {
        MaterialPO po = materialDao.selectById(id);
        if (po == null) {
            throw new BizException("SYSB002", "素材不存在");
        }
        if (name != null && !name.isBlank()) {
            po.setName(name.trim());
        }
        if (moveCategory) {
            po.setCategoryId(normalizeCategory(categoryId));
        }
        materialDao.updateById(po);
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(String id) {
        MaterialPO po = materialDao.selectById(id);
        if (po == null) {
            return;
        }
        // 存储删除失败不阻断元数据删除(可能是已被外部清理 / provider 切换)
        safeDelete(po.getObjectName());
        materialDao.deleteById(id);
    }

    public String presignedUrl(String id) {
        MaterialPO po = materialDao.selectById(id);
        if (po == null) {
            throw new BizException("SYSB002", "素材不存在");
        }
        return fileTemplate.presignedUrl(po.getObjectName(), Duration.ofHours(1));
    }

    // ---- helpers ----

    /** 校验扩展名白名单,返回小写扩展名。 */
    private String checkExt(String type, String originalName) {
        String ext = ext(originalName);
        if (!WHITELIST.get(type).contains(ext)) {
            throw new BizException("SYSA002", "不支持的文件类型: ." + ext);
        }
        return ext;
    }

    /** 校验扩展名 + 大小上限,返回小写扩展名。 */
    private String validate(String type, String originalName, long size) {
        String ext = checkExt(type, originalName);
        if (size > MAX_SIZE.get(type)) {
            throw new BizException("SYSA002", "文件超出大小限制(" + (MAX_SIZE.get(type) / 1024 / 1024) + "MB)");
        }
        return ext;
    }

    /** 删除对象存储中的对象,失败仅告警不抛(用于清理/兜底场景)。 */
    private void safeDelete(String objectName) {
        try {
            fileTemplate.delete(objectName);
        } catch (Exception e) {
            log.warn("[material] storage delete failed for {}: {}", objectName, e.getMessage());
        }
    }

    /** 落素材元数据(单次/分片共用)。 */
    private MaterialPO persist(String type, String categoryId, String name, String ext, String mime,
                              String objectName, String url, long size) {
        MaterialPO po = new MaterialPO();
        po.setTenantId(currentTenant());
        po.setCategoryId(normalizeCategory(categoryId));
        po.setType(type);
        po.setName(name);
        po.setObjectName(objectName);
        po.setUrl(url);
        po.setSize(size);
        po.setExt(ext);
        po.setMime(mime);
        po.setUploadedBy(currentUser());
        materialDao.insert(po);
        return po;
    }

    private MaterialVO toVO(MaterialPO p) {
        return new MaterialVO(p.getId(), p.getType(), p.getCategoryId(), p.getName(), p.getObjectName(),
                p.getUrl(), p.getSize() == null ? 0 : p.getSize(), p.getExt(), p.getMime(),
                p.getUploadedBy(), p.getCreatedAt());
    }

    private Map<String, Long> counts(String type, String tenant) {
        Map<String, Long> map = new HashMap<>();
        List<Map<String, Object>> rows = materialDao.selectMaps(new QueryWrapper<MaterialPO>()
                .select("category_id AS cid, count(*) AS cnt")
                .eq("tenant_id", tenant)
                .eq("type", type)
                .groupBy("category_id"));
        for (Map<String, Object> row : rows) {
            Object cid = row.get("cid");
            Object cnt = row.get("cnt");
            map.put(cid == null ? null : String.valueOf(cid),
                    cnt instanceof Number n ? n.longValue() : 0L);
        }
        return map;
    }

    private String normalizeCategory(String categoryId) {
        return (categoryId == null || categoryId.isBlank() || UNGROUPED.equals(categoryId)) ? null : categoryId;
    }

    private void checkType(String type) {
        if (!WHITELIST.containsKey(type)) {
            throw new BizException("SYSA001", "未知素材类型: " + type);
        }
    }

    private static String ext(String filename) {
        int dot = filename.lastIndexOf('.');
        return dot >= 0 && dot < filename.length() - 1
                ? filename.substring(dot + 1).toLowerCase()
                : "";
    }

    private String currentUser() {
        Object id = StpUtil.getLoginIdDefaultNull();
        return id == null ? null : String.valueOf(id);
    }

    private String currentTenant() {
        // 多租户开启时由租户 Web 过滤器写入 X-Tenant-Id → ThreadLocal;关闭时为空 → 回退 "0"。
        String t = TenantContext.getTenantId();
        return (t == null || t.isBlank()) ? "0" : t;
    }
}
