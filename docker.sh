# mysql
# docker run --name mysql-container -e MYSQL_ROOT_PASSWORD=1234 -d -p 3306:3306 mysql:8.2.0
docker start mysql-container

# Kafka
# docker run -d --name kafka-container -p 9092:9092 apache/kafka:latest

docker start kafka-container
