# mate-file-starter

文件存储 Starter，封装 MinIO 对象存储。

## 提供的能力

- **文件上传** — 单文件 / 多文件上传到 MinIO
- **文件下载** — 流式下载
- **预签名 URL** — 生成临时访问链接（可设过期时间）
- **Bucket 管理** — 自动创建 Bucket

## 配置

```yaml
mate:
  file:
    minio:
      endpoint: http://127.0.0.1:9000
      access-key: ${MINIO_ACCESS_KEY:minioadmin}
      secret-key: ${MINIO_SECRET_KEY:minioadmin}
      bucket: mate-files
```

## 使用

```java
@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;

    @PostMapping("/upload")
    public Result<String> upload(@RequestParam MultipartFile file) {
        String url = fileService.upload(file);
        return Result.ok(url);
    }

    @GetMapping("/presign")
    public Result<String> presign(@RequestParam String objectName) {
        String url = fileService.getPresignedUrl(objectName, 1, TimeUnit.HOURS);
        return Result.ok(url);
    }
}
```
