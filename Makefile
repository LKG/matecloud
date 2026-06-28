# MateCloud Makefile
# Convenience targets for common dev / ops operations.

.PHONY: help build build-module up down restart logs clean test docs monolith run-monolith monolith-up monolith-down

help:
	@echo "MateCloud Makefile"
	@echo ""
	@echo "Targets:"
	@echo "  build           Build the whole reactor (mvn clean install -DskipTests)"
	@echo "  build-module    Build one module (usage: make build-module MODULE=mate-auth)"
	@echo "  test            Run all unit tests"
	@echo "  verify          Run integration tests (profile: integration-test)"
	@echo ""
	@echo "  infra-up        Start infrastructure only (mysql/redis/rabbitmq/nacos/minio)"
	@echo "  infra-down      Stop infrastructure"
	@echo ""
	@echo "  up              Start full stack (infra + services) via docker-compose"
	@echo "  down            Stop full stack"
	@echo "  restart         Restart the stack"
	@echo "  logs            Tail logs for all services (Ctrl+C to exit)"
	@echo "  clean           Remove build artifacts and docker volumes"
	@echo ""
	@echo "  docker-build    Build all service docker images"
	@echo "  docker-push     Push all service images to registry (REGISTRY=...)"
	@echo ""
	@echo "  monolith        Build the single-JVM monolith JAR (-Pmonolith)"
	@echo "  run-monolith    Run the monolith JAR locally (needs MySQL + Redis)"
	@echo "  monolith-up     Build + run the monolith in docker (infra + :8080)"
	@echo "  monolith-down   Stop the monolith container"

build:
	mvn clean install -DskipTests -B

build-module:
	mvn clean install -pl $(MODULE) -am -DskipTests -B

test:
	mvn test -B

verify:
	mvn verify -Pintegration-test -B

infra-up:
	docker-compose up -d mysql redis rabbitmq nacos minio

infra-down:
	docker-compose stop mysql redis rabbitmq nacos minio

up:
	docker-compose up -d

down:
	docker-compose down

restart:
	docker-compose restart

logs:
	docker-compose logs -f --tail=100

clean:
	mvn clean
	docker-compose down -v

docker-build:
	docker-compose build

docker-push:
	@if [ -z "$(REGISTRY)" ]; then echo "Set REGISTRY, e.g. REGISTRY=registry.example.com/mate"; exit 1; fi
	docker-compose build
	@for svc in mate-gateway mate-auth mate-system mate-notice; do \
		docker tag $$svc $(REGISTRY)/$$svc:latest; \
		docker push $(REGISTRY)/$$svc:latest; \
	done

docs:  ## Generate API documentation (Smart-Doc)
	mvn smart-doc:html -pl mate-biz/mate-system -q

monolith:  ## Build monolith JAR (mvn -Pmonolith)
	mvn -Pmonolith clean package -pl mate-monolith -am -DskipTests -B

run-monolith:  ## Run monolith locally (mode/Nacos come from mate-infra-local.yml)
	java -jar $$(ls mate-monolith/target/mate-monolith-*.jar | grep -v -- '-exec' | head -n1)

monolith-up:  ## Build + run monolith in docker (infra + single JVM on :8080)
	docker-compose --profile monolith up -d --build mysql redis mate-monolith

monolith-down:  ## Stop the monolith container
	docker-compose --profile monolith stop mate-monolith
