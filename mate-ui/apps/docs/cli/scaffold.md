# 脚手架命令

## 创建业务模块

```bash
java -jar mate-cli.jar new module mate-order --port 9060
```

自动生成：
- `mate-biz/mate-order/pom.xml`（引入核心 Starter）
- DDD 四层包结构
- `application.yml`（配置 Nacos 导入）
- `MateOrderApplication.java`（启动类）
- 注册到 `mate-biz/pom.xml`

## 创建聚合

在已有模块内创建新的聚合根：

```bash
java -jar mate-cli.jar new aggregate Order --module mate-order
```

生成：
- `domain/model/aggregate/Order.java`
- `domain/adapter/repository/OrderRepository.java`
- `infrastructure/adapter/repository/OrderRepositoryImpl.java`
- `infrastructure/dao/OrderMapper.java`
- `infrastructure/dao/po/OrderPO.java`
- `application/convertor/OrderConvertor.java`

## 代码生成

根据已有数据库表生成完整 CRUD 代码：

```bash
java -jar mate-cli.jar gen code --table mate_order --module mate-order --service mate-system
```
