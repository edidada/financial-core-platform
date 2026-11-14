.PHONY: build test run invariants e2e clean help

JAVA_HOME ?= $(shell echo $$JAVA_HOME)
APP_JAR   := app/financial-core-app/target/financial-core-app-*.jar
BASE      ?= http://localhost:18080

help:
	@echo "targets: build test run invariants e2e clean"

build:
	mvn -B -ntp clean package

test:
	mvn -B -ntp test

# 凭据通过环境变量传入：FCP_DB_USER / FCP_DB_PASSWORD
run: build
	FCP_SERVER_PORT=18080 java -jar $(APP_JAR)

invariants:
	curl -sf $(BASE)/v1/invariants | python3 -m json.tool

e2e:
	bash tests/end-to-end/e2e.sh $(BASE)

dual-db-test:
	@echo "双库集成测试见 .github/workflows/test.yml（MySQL 8.4 + PostgreSQL 16）"

clean:
	mvn -B -ntp clean
