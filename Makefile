FLYWAY=flyway -configFile=sql/flyway.conf -locations=filesystem:sql/
FLYWAY_TEST=flyway -configFile=sql/flyway.test.conf -locations=filesystem:sql/

DB=middlewarehouse_development
DB_TEST=middlewarehouse_test
DB_USER=middlewarehouse

SUBDIRS = api common controllers models repositories routes services
TESTDIRS = $(SUBDIRS:%=test-%)

build:
	go build -o middlewarehouse main.go

build-linux:
	GOOS=linux go build -o middlewarehouse main.go

migrate:
	${FLYWAY} migrate

migrate-test:
	${FLYWAY_TEST} migrate

reset: drop-db drop-user create-user create-db migrate migrate-test

reset-test:
	dropdb --if-exists ${DB_TEST}
	createdb ${DB_TEST}
	@make migrate-test

drop-db:
	dropdb --if-exists ${DB}
	dropdb --if-exists ${DB_TEST}

create-db:
	createdb ${DB}
	createdb ${DB_TEST}

drop-user:
	dropuser --if-exists ${DB_USER}

create-user:
	createuser -s ${DB_USER}

test: $(TESTDIRS)
$(TESTDIRS): PACKAGE = $(@:test-%=%)
$(TESTDIRS):
	cd $(PACKAGE) && GOENV=test go test ./...

