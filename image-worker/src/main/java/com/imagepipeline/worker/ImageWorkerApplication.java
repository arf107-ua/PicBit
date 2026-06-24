spring:
application:
name: image-worker

rabbitmq:
host: ${SPRING_RABBITMQ_HOST:localhost}
port: 5672
username: guest
password: guest
listener:
simple:
acknowledge-mode: manual
prefetch: 1
retry:
enabled: true
initial-interval: 3s
max-attempts: 3
multiplier: 2

minio:
endpoint: ${MINIO_ENDPOINT:http://localhost:9000}
access-key: ${MINIO_ACCESS_KEY:minioadmin}
    secret-key: ${MINIO_SECRET_KEY:minioadmin}
    bucket-source: images-original
    bucket-output: images-processed

    rabbitmq:
    queues:
    resize: image.resize
    compress: image.compress
    watermark: image.watermark
    dead-letter: image.dead-letter

    image:
    resize:
    target-width: 200
    target-height: 200
    compress:
    quality: 0.75
    watermark:
    text: "© ImagePipeline"
    opacity: 0.4

    management:
    endpoints:
    web:
    exposure:
    include: health, info, metrics
    server:
    port: 8081