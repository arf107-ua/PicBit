# Image Processing Pipeline

Distributed image processing system built with Java 17, Spring Boot 3, 
RabbitMQ and MinIO.

## Architecture

- **api-server**: REST API that receives images and enqueues processing tasks
- **image-worker**: Workers that consume tasks and apply transformations

## How to run

```bash
docker-compose up --build
```

API available at `http://localhost:8080`
RabbitMQ dashboard at `http://localhost:15672` (guest/guest)
MinIO dashboard at `http://localhost:9001` (minioadmin/minioadmin)

## Upload an image

```bash
curl -X POST http://localhost:8080/upload \
  -F "file=@photo.jpg"
```

## Authors
- Adrián Requena Fernández (https://github.com/arf107-ua) 
- Hugo Fernández Campos(https://github.com/hfrade) 
