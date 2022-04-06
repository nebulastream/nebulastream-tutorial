# File indicating that docker containers are running
DOCKER_STAMP=docker.stamp
CONFIG_FILES=coordinator.yml worker-1.yml
TEST_FILES=test-data.csv test-query.json
OUTPUT_FILES=actual-output.csv

up: ${DOCKER_STAMP}

${DOCKER_STAMP}: ${CONFIG_FILES}
	docker-compose pull
	docker-compose up --remove-orphans
	touch ${DOCKER_STAMP}

down:
	docker-compose down
	rm -f ${DOCKER_STAMP}

test: actual-output.csv

actual-output.csv: ${TEST_FILES}
	curl -d@test-query.json http://localhost:8081/v1/nes/query/execute-query

compare: actual-output.csv
	diff -u actual-output.csv expected-output.csv

clean:
	rm ${OUTPUT_FILES}
