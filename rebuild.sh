mvn -N io.takari:maven:wrapper
./mvnw clean package -DskipTests
cp target/DrychanBot-1.0-SNAPSHOT.jar src/main/docker

# shellcheck disable=SC2164
cd src/main/docker
docker-compose down
docker rmi drychan-bot:latest
docker build -t drychan-bot .
docker-compose --env-file .env up