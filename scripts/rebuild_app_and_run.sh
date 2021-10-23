cd ../src/main/docker
docker-compose down -d app
docker rmi drychan-bot:latest
docker build -t drychan-bot .
docker-compose --env-file .env up -d app