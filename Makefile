# MateCloud Makefile
# Convenience targets for common dev / ops operations.

.PHONY: help build build-module up down restart logs clean test docs monolith run-monolith

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

monolith:  ## Build monolith JAR
	mvn clean package -pl mate-monolith -am -DskipTests -Pmonolith

run-monolith:  ## Run monolith mode (single JAR, no Dubbo)
	MATE_RPC_MODE=local java -jar mate-monolith/target/mate-monolith-1.0.0.jar
