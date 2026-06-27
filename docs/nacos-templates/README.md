# Nacos Config Templates

This directory holds the **single** shared config every MateCloud deployment
needs. Drop it into Nacos on first deployment; never duplicate it per service.

## Files

| File | DataId in Nacos | Purpose |
|------|-----------------|---------|
| `mate-infra-dev.yml` | `mate-infra-dev.yml` | All infra (DB/Redis/MQ/MinIO) for dev profile |
| `mate-infra-prod.yml` | `mate-infra-prod.yml` | Same, for prod (create by copying dev and editing) |

## How to upload

### Via `mate-cli` (recommended)

```bash
# Publishes mate-infra-dev.yml to Nacos from this template
java -jar mate-cli/target/mate-cli.jar config init
```

### Via curl

```bash
curl -X POST 'http://127.0.0.1:8848/nacos/v1/cs/configs' \
  --data-urlencode 'dataId=mate-infra-dev.yml' \
  --data-urlencode 'group=DEFAULT_GROUP' \
  --data-urlencode 'tenant=dev' \
  --data-urlencode "content=$(cat mate-infra-dev.yml)"
```

### Via Nacos UI

1. Open http://127.0.0.1:8848/nacos
2. Configuration Management → Configurations → + New
3. DataId = `mate-infra-dev.yml`, Group = `DEFAULT_GROUP`, Namespace = `dev`
4. Paste the content of this file, format = YAML
5. Publish

## That's it.

No per-service Nacos files are required. Services read `mate-infra-dev.yml`
automatically (via their `application.yml` import) and fall back to classpath
defaults if Nacos is unreachable.
