# RFC-001: Root POM & Project Skeleton

- **Status**: Draft
- **Created**: 2026-04-11
- **Author**: MateCloud Team

## 背景

从零搭建 matecloud 微服务脚手架，需要一个根 POM 来统一管理所有模块的版本依赖和构建配置。根 POM 是整个项目的基石，所有子模块继承于此，确保版本一致性和构建规范统一。

## 设计方案

### Change 1: Root pom.xml

Create `D:\codes\matecloud\pom.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>4.0.5</version>
        <relativePath/>
    </parent>

    <groupId>vip.mate</groupId>
    <artifactId>matecloud</artifactId>
    <version>1.0.0</version>
    <packaging>pom</packaging>
    <name>MateCloud</name>
    <description>MateCloud DDD Microservice Scaffold</description>

    <modules>
        <module>mate-common</module>
        <module>mate-starters</module>
        <module>mate-gateway</module>
        <module>mate-auth</module>
        <module>mate-biz</module>
        <module>mate-admin</module>
        <module>mate-cli</module>
    </modules>

    <properties>
        <!-- Java -->
        <java.version>21</java.version>
        <maven.compiler.source>21</maven.compiler.source>
        <maven.compiler.target>21</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>

        <!-- MateCloud Module Version -->
        <matecloud.version>1.0.0</matecloud.version>

        <!-- Spring Ecosystem -->
        <spring-boot.version>4.0.5</spring-boot.version>
        <spring-cloud.version>2025.1.1</spring-cloud.version>
        <spring-cloud-alibaba.version>2025.1.0.0</spring-cloud-alibaba.version>

        <!-- RPC -->
        <dubbo.version>3.3.6</dubbo.version>

        <!-- ORM -->
        <mybatis-plus.version>3.5.16</mybatis-plus.version>
        <mybatis-plus-generator.version>3.5.16</mybatis-plus-generator.version>
        <dynamic-datasource.version>4.3.1</dynamic-datasource.version>

        <!-- Database -->
        <mysql.version>8.3.0</mysql.version>
        <druid.version>1.2.24</druid.version>
        <p6spy.version>3.9.1</p6spy.version>

        <!-- Security -->
        <sa-token.version>1.45.0</sa-token.version>

        <!-- Cache & Redis -->
        <redisson.version>3.52.0</redisson.version>

        <!-- Serialization & Doc -->
        <fastjson2.version>2.0.53</fastjson2.version>
        <jackson-bom.version>2.18.3</jackson-bom.version>
        <smart-doc.version>3.0.9</smart-doc.version>

        <!-- Utils -->
        <lombok.version>1.18.36</lombok.version>
        <mapstruct.version>1.6.3</mapstruct.version>
        <hutool.version>5.8.35</hutool.version>
        <guava.version>33.4.0-jre</guava.version>
        <commons-lang3.version>3.17.0</commons-lang3.version>
        <commons-io.version>2.18.0</commons-io.version>
        <commons-collections4.version>4.5.0-M3</commons-collections4.version>
        <easyexcel.version>4.0.3</easyexcel.version>
        <transmittable-thread-local.version>2.14.5</transmittable-thread-local.version>

        <!-- OSS -->
        <minio.version>8.5.14</minio.version>
        <aws-s3.version>2.29.51</aws-s3.version>

        <!-- Logging -->
        <logstash-logback.version>8.0</logstash-logback.version>

        <!-- Build Plugin Versions -->
        <maven-compiler-plugin.version>3.13.0</maven-compiler-plugin.version>
        <spring-boot-maven-plugin.version>3.5.6</spring-boot-maven-plugin.version>
        <smart-doc-maven-plugin.version>3.0.9</smart-doc-maven-plugin.version>
    </properties>

    <dependencyManagement>
        <dependencies>
            <!-- ==================== BOM Imports ==================== -->
            <dependency>
                <groupId>org.springframework.cloud</groupId>
                <artifactId>spring-cloud-dependencies</artifactId>
                <version>${spring-cloud.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
            <dependency>
                <groupId>com.alibaba.cloud</groupId>
                <artifactId>spring-cloud-alibaba-dependencies</artifactId>
                <version>${spring-cloud-alibaba.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
            <dependency>
                <groupId>org.apache.dubbo</groupId>
                <artifactId>dubbo-bom</artifactId>
                <version>${dubbo.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>

            <!-- ==================== MateCloud Modules: mate-common ==================== -->
            <dependency>
                <groupId>vip.mate</groupId>
                <artifactId>mate-base</artifactId>
                <version>${matecloud.version}</version>
            </dependency>
            <dependency>
                <groupId>vip.mate</groupId>
                <artifactId>mate-api</artifactId>
                <version>${matecloud.version}</version>
            </dependency>

            <!-- ==================== MateCloud Modules: mate-starters ==================== -->
            <dependency>
                <groupId>vip.mate</groupId>
                <artifactId>mate-ds-starter</artifactId>
                <version>${matecloud.version}</version>
            </dependency>
            <dependency>
                <groupId>vip.mate</groupId>
                <artifactId>mate-web-starter</artifactId>
                <version>${matecloud.version}</version>
            </dependency>
            <dependency>
                <groupId>vip.mate</groupId>
                <artifactId>mate-nacos-starter</artifactId>
                <version>${matecloud.version}</version>
            </dependency>
            <dependency>
                <groupId>vip.mate</groupId>
                <artifactId>mate-rpc-starter</artifactId>
                <version>${matecloud.version}</version>
            </dependency>
            <dependency>
                <groupId>vip.mate</groupId>
                <artifactId>mate-redis-starter</artifactId>
                <version>${matecloud.version}</version>
            </dependency>
            <dependency>
                <groupId>vip.mate</groupId>
                <artifactId>mate-satoken-starter</artifactId>
                <version>${matecloud.version}</version>
            </dependency>
            <dependency>
                <groupId>vip.mate</groupId>
                <artifactId>mate-log-starter</artifactId>
                <version>${matecloud.version}</version>
            </dependency>
            <dependency>
                <groupId>vip.mate</groupId>
                <artifactId>mate-oss-starter</artifactId>
                <version>${matecloud.version}</version>
            </dependency>
            <dependency>
                <groupId>vip.mate</groupId>
                <artifactId>mate-excel-starter</artifactId>
                <version>${matecloud.version}</version>
            </dependency>
            <dependency>
                <groupId>vip.mate</groupId>
                <artifactId>mate-doc-starter</artifactId>
                <version>${matecloud.version}</version>
            </dependency>
            <dependency>
                <groupId>vip.mate</groupId>
                <artifactId>mate-gray-starter</artifactId>
                <version>${matecloud.version}</version>
            </dependency>
            <dependency>
                <groupId>vip.mate</groupId>
                <artifactId>mate-idempotent-starter</artifactId>
                <version>${matecloud.version}</version>
            </dependency>

            <!-- ==================== Third-party Dependencies ==================== -->
            <!-- MyBatis Plus -->
            <dependency>
                <groupId>com.baomidou</groupId>
                <artifactId>mybatis-plus-spring-boot3-starter</artifactId>
                <version>${mybatis-plus.version}</version>
            </dependency>
            <dependency>
                <groupId>com.baomidou</groupId>
                <artifactId>mybatis-plus-annotation</artifactId>
                <version>${mybatis-plus.version}</version>
            </dependency>
            <dependency>
                <groupId>com.baomidou</groupId>
                <artifactId>mybatis-plus-extension</artifactId>
                <version>${mybatis-plus.version}</version>
            </dependency>
            <dependency>
                <groupId>com.baomidou</groupId>
                <artifactId>mybatis-plus-generator</artifactId>
                <version>${mybatis-plus-generator.version}</version>
            </dependency>
            <dependency>
                <groupId>com.baomidou</groupId>
                <artifactId>dynamic-datasource-spring-boot3-starter</artifactId>
                <version>${dynamic-datasource.version}</version>
            </dependency>

            <!-- Database -->
            <dependency>
                <groupId>com.mysql</groupId>
                <artifactId>mysql-connector-j</artifactId>
                <version>${mysql.version}</version>
            </dependency>
            <dependency>
                <groupId>com.alibaba</groupId>
                <artifactId>druid-spring-boot-3-starter</artifactId>
                <version>${druid.version}</version>
            </dependency>
            <dependency>
                <groupId>p6spy</groupId>
                <artifactId>p6spy</artifactId>
                <version>${p6spy.version}</version>
            </dependency>

            <!-- Sa-Token -->
            <dependency>
                <groupId>cn.dev33</groupId>
                <artifactId>sa-token-spring-boot3-starter</artifactId>
                <version>${sa-token.version}</version>
            </dependency>
            <dependency>
                <groupId>cn.dev33</groupId>
                <artifactId>sa-token-reactor-spring-boot3-starter</artifactId>
                <version>${sa-token.version}</version>
            </dependency>
            <dependency>
                <groupId>cn.dev33</groupId>
                <artifactId>sa-token-dao-redis-jackson</artifactId>
                <version>${sa-token.version}</version>
            </dependency>
            <dependency>
                <groupId>cn.dev33</groupId>
                <artifactId>sa-token-core</artifactId>
                <version>${sa-token.version}</version>
            </dependency>

            <!-- Redisson -->
            <dependency>
                <groupId>org.redisson</groupId>
                <artifactId>redisson-spring-boot-starter</artifactId>
                <version>${redisson.version}</version>
            </dependency>

            <!-- Serialization -->
            <dependency>
                <groupId>com.alibaba.fastjson2</groupId>
                <artifactId>fastjson2</artifactId>
                <version>${fastjson2.version}</version>
            </dependency>

            <!-- Utils -->
            <dependency>
                <groupId>cn.hutool</groupId>
                <artifactId>hutool-all</artifactId>
                <version>${hutool.version}</version>
            </dependency>
            <dependency>
                <groupId>com.google.guava</groupId>
                <artifactId>guava</artifactId>
                <version>${guava.version}</version>
            </dependency>
            <dependency>
                <groupId>org.apache.commons</groupId>
                <artifactId>commons-lang3</artifactId>
                <version>${commons-lang3.version}</version>
            </dependency>
            <dependency>
                <groupId>commons-io</groupId>
                <artifactId>commons-io</artifactId>
                <version>${commons-io.version}</version>
            </dependency>
            <dependency>
                <groupId>org.apache.commons</groupId>
                <artifactId>commons-collections4</artifactId>
                <version>${commons-collections4.version}</version>
            </dependency>
            <dependency>
                <groupId>com.alibaba</groupId>
                <artifactId>easyexcel</artifactId>
                <version>${easyexcel.version}</version>
            </dependency>
            <dependency>
                <groupId>com.alibaba</groupId>
                <artifactId>transmittable-thread-local</artifactId>
                <version>${transmittable-thread-local.version}</version>
            </dependency>

            <!-- MapStruct -->
            <dependency>
                <groupId>org.mapstruct</groupId>
                <artifactId>mapstruct</artifactId>
                <version>${mapstruct.version}</version>
            </dependency>
            <dependency>
                <groupId>org.mapstruct</groupId>
                <artifactId>mapstruct-processor</artifactId>
                <version>${mapstruct.version}</version>
            </dependency>

            <!-- OSS -->
            <dependency>
                <groupId>io.minio</groupId>
                <artifactId>minio</artifactId>
                <version>${minio.version}</version>
            </dependency>
            <dependency>
                <groupId>software.amazon.awssdk</groupId>
                <artifactId>s3</artifactId>
                <version>${aws-s3.version}</version>
            </dependency>

            <!-- Logging -->
            <dependency>
                <groupId>net.logstash.logback</groupId>
                <artifactId>logstash-logback-encoder</artifactId>
                <version>${logstash-logback.version}</version>
            </dependency>
        </dependencies>
    </dependencyManagement>

    <build>
        <pluginManagement>
            <plugins>
                <!-- Maven Compiler Plugin: Java 21 + Lombok + MapStruct -->
                <plugin>
                    <groupId>org.apache.maven.plugins</groupId>
                    <artifactId>maven-compiler-plugin</artifactId>
                    <version>${maven-compiler-plugin.version}</version>
                    <configuration>
                        <source>${java.version}</source>
                        <target>${java.version}</target>
                        <encoding>${project.build.sourceEncoding}</encoding>
                        <annotationProcessorPaths>
                            <path>
                                <groupId>org.projectlombok</groupId>
                                <artifactId>lombok</artifactId>
                                <version>${lombok.version}</version>
                            </path>
                            <path>
                                <groupId>org.mapstruct</groupId>
                                <artifactId>mapstruct-processor</artifactId>
                                <version>${mapstruct.version}</version>
                            </path>
                            <path>
                                <groupId>org.projectlombok</groupId>
                                <artifactId>lombok-mapstruct-binding</artifactId>
                                <version>0.2.0</version>
                            </path>
                        </annotationProcessorPaths>
                        <compilerArgs>
                            <arg>-parameters</arg>
                        </compilerArgs>
                    </configuration>
                </plugin>

                <!-- Spring Boot Maven Plugin -->
                <plugin>
                    <groupId>org.springframework.boot</groupId>
                    <artifactId>spring-boot-maven-plugin</artifactId>
                    <version>${spring-boot-maven-plugin.version}</version>
                    <executions>
                        <execution>
                            <goals>
                                <goal>repackage</goal>
                            </goals>
                        </execution>
                    </executions>
                    <configuration>
                        <excludes>
                            <exclude>
                                <groupId>org.projectlombok</groupId>
                                <artifactId>lombok</artifactId>
                            </exclude>
                        </excludes>
                    </configuration>
                </plugin>

                <!-- Smart-Doc Maven Plugin -->
                <plugin>
                    <groupId>com.ly.smart-doc</groupId>
                    <artifactId>smart-doc-maven-plugin</artifactId>
                    <version>${smart-doc-maven-plugin.version}</version>
                    <configuration>
                        <configFile>./src/main/resources/smart-doc.json</configFile>
                    </configuration>
                </plugin>
            </plugins>
        </pluginManagement>

        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
            </plugin>
        </plugins>
    </build>

    <repositories>
        <repository>
            <id>aliyun-maven</id>
            <name>Aliyun Maven Repository</name>
            <url>https://maven.aliyun.com/repository/public</url>
            <releases>
                <enabled>true</enabled>
            </releases>
            <snapshots>
                <enabled>false</enabled>
            </snapshots>
        </repository>
    </repositories>

    <pluginRepositories>
        <pluginRepository>
            <id>aliyun-plugin</id>
            <name>Aliyun Plugin Repository</name>
            <url>https://maven.aliyun.com/repository/public</url>
            <releases>
                <enabled>true</enabled>
            </releases>
            <snapshots>
                <enabled>false</enabled>
            </snapshots>
        </pluginRepository>
    </pluginRepositories>

</project>
```

### Change 2: Module Parent POMs

#### mate-common/pom.xml

Create `D:\codes\matecloud\mate-common\pom.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>vip.mate</groupId>
        <artifactId>matecloud</artifactId>
        <version>1.0.0</version>
        <relativePath>../pom.xml</relativePath>
    </parent>

    <artifactId>mate-common</artifactId>
    <packaging>pom</packaging>
    <name>mate-common</name>
    <description>Common modules - pure type libraries, NO auto-configuration</description>

    <modules>
        <module>mate-base</module>
        <module>mate-api</module>
    </modules>

</project>
```

#### mate-starters/pom.xml

Create `D:\codes\matecloud\mate-starters\pom.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>vip.mate</groupId>
        <artifactId>matecloud</artifactId>
        <version>1.0.0</version>
        <relativePath>../pom.xml</relativePath>
    </parent>

    <artifactId>mate-starters</artifactId>
    <packaging>pom</packaging>
    <name>mate-starters</name>
    <description>Auto-configuration starter modules</description>

    <modules>
        <module>mate-ds-starter</module>
        <module>mate-web-starter</module>
        <module>mate-nacos-starter</module>
        <module>mate-rpc-starter</module>
        <module>mate-redis-starter</module>
        <module>mate-satoken-starter</module>
        <module>mate-log-starter</module>
        <module>mate-oss-starter</module>
        <module>mate-excel-starter</module>
        <module>mate-doc-starter</module>
        <module>mate-gray-starter</module>
        <module>mate-idempotent-starter</module>
    </modules>

</project>
```

#### mate-biz/pom.xml

Create `D:\codes\matecloud\mate-biz\pom.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>vip.mate</groupId>
        <artifactId>matecloud</artifactId>
        <version>1.0.0</version>
        <relativePath>../pom.xml</relativePath>
    </parent>

    <artifactId>mate-biz</artifactId>
    <packaging>pom</packaging>
    <name>mate-biz</name>
    <description>Business service modules</description>

    <modules>
        <module>mate-system</module>
    </modules>

</project>
```

### Change 3: .gitignore

Create `D:\codes\matecloud\.gitignore`

```gitignore
# === Maven ===
target/
!.mvn/wrapper/maven-wrapper.jar
pom.xml.tag
pom.xml.releaseBackup
pom.xml.versionsBackup
pom.xml.next
release.properties
dependency-reduced-pom.xml
buildNumber.properties
.mvn/timing.properties

# === IDE - IntelliJ IDEA ===
.idea/
*.iws
*.iml
*.ipr
out/

# === IDE - Eclipse ===
.apt_generated
.classpath
.factorypath
.project
.settings
.springBeans
.sts4-cache
bin/

# === IDE - VS Code ===
.vscode/

# === OS ===
.DS_Store
Thumbs.db
ehthumbs.db
Desktop.ini

# === Logs ===
*.log
logs/

# === Environment ===
.env
*.env.local

# === Compiled ===
*.class
*.jar
*.war
*.ear

# === Package files ===
*.tar.gz
*.rar
*.zip

# === JRebel ===
rebel.xml

# === Docker ===
docker-compose.override.yml
```

### Change 4: docs/rfcs/README.md

Create `D:\codes\matecloud\docs\rfcs\README.md`

```markdown
# MateCloud RFC Index

| RFC | Title | Status |
|-----|-------|--------|
| RFC-001 | Root POM & Project Skeleton | Draft |
| RFC-002 | mate-common (mate-base + mate-api) | Draft |
| RFC-003 | Core Starters (ds, web, nacos, rpc) | Draft |
```

## 涉及文件

| 文件 | 类型 | 说明 |
|------|------|------|
| `pom.xml` | New | Root POM, 统一版本管理和构建配置 |
| `mate-common/pom.xml` | New | Common 聚合模块 POM |
| `mate-starters/pom.xml` | New | Starters 聚合模块 POM |
| `mate-biz/pom.xml` | New | Business 聚合模块 POM |
| `.gitignore` | New | Git ignore rules |
| `docs/rfcs/README.md` | New | RFC 索引文档 |

## 验证方案

1. 在项目根目录执行 `mvn validate`，应通过（需要先创建所有子模块的 pom.xml）
2. 所有模块目录存在且结构正确
3. 版本属性与 dependencyManagement 中的引用一致
4. `mvn help:effective-pom` 可正确解析所有 BOM 导入

## 注意事项

- Root POM 使用 `spring-boot-starter-parent` 作为 parent，继承 Spring Boot 的默认插件配置和依赖管理
- 所有内部模块版本通过 `${matecloud.version}` 统一管理
- `maven-compiler-plugin` 配置了 Lombok + MapStruct + lombok-mapstruct-binding 三个 annotation processor
- 使用 `-parameters` 编译参数以支持 Spring 参数名解析
- 阿里云 Maven 镜像加速国内依赖下载
