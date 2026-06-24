server:
port: 8080

spring:
application:
name: api-server

rabbitmq:
host: ${SPRING_RABBITMQ_HOST:localhost}
port: 5672
username: guest
password: guest

servlet:
multipart:
max-file-size: 20MB
max-request-size: 20MB

minio:
endpoint: ${MINIO_ENDPOINT:http://localhost:9000}
access-key: ${MINIO_ACCESS_KEY:minioadmin}
    secret-key: ${MINIO_SECRET_KEY:minioadmin}
    bucket: images-original

    rabbitmq:
    queues:
    resize: image.resize
    compress: image.compress
    watermark: image.watermark

    management:
    endpoints:
    web:
    exposure:
    include: health, info, metrics