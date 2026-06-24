package com.imagepipeline.worker.consumer;

import com.imagepipeline.worker.model.ImageTaskMessage;
import com.imagepipeline.worker.processor.ResizeProcessor;
import com.imagepipeline.worker.service.StorageService;
import com.rabbitmq.client.Channel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import static org.mockito.Mockito.*;

/**
 * Unit tests for ResizeWorker.
 *
 * We mock StorageService, ResizeProcessor and Channel — no RabbitMQ or MinIO needed.
 * These tests run in milliseconds and verify the ACK/NACK logic.
 */
@ExtendWith(MockitoExtension.class)
class ResizeWorkerTest {

    @Mock
    private StorageService storageService;

    @Mock
    private ResizeProcessor resizeProcessor;

    @Mock
    private Channel channel;

    @InjectMocks
    private ResizeWorker resizeWorker;

    private ImageTaskMessage message;
    private final long deliveryTag = 1L;

    @BeforeEach
    void setUp() {
        message = new ImageTaskMessage("test-image.jpg", "RESIZE", 200, 200);
    }

    @Test
    void whenProcessingSucceeds_thenAckIsSent() throws Exception {
        // Arrange
        InputStream fakeStream = new ByteArrayInputStream(new byte[]{1, 2, 3});
        byte[] fakeResult = new byte[]{4, 5, 6};

        when(storageService.download("test-image.jpg")).thenReturn(fakeStream);
        when(resizeProcessor.process("test-image.jpg", fakeStream)).thenReturn(fakeResult);
        when(resizeProcessor.buildResultKey("test-image.jpg")).thenReturn("thumb_test-image.jpg");

        // Act
        resizeWorker.handleResize(message, channel, deliveryTag);

        // Assert
        verify(storageService).upload("thumb_test-image.jpg", fakeResult, "image/jpeg");
        verify(channel).basicAck(deliveryTag, false);
        verify(channel, never()).basicNack(anyLong(), anyBoolean(), anyBoolean());
    }

    @Test
    void whenStorageDownloadFails_thenNackIsSent() throws Exception {
        // Arrange
        when(storageService.download("test-image.jpg"))
                .thenThrow(new RuntimeException("MinIO connection refused"));

        // Act
        resizeWorker.handleResize(message, channel, deliveryTag);

        // Assert
        verify(channel).basicNack(deliveryTag, false, false);
        verify(channel, never()).basicAck(anyLong(), anyBoolean());
    }

    @Test
    void whenProcessorFails_thenNackIsSent() throws Exception {
        // Arrange
        InputStream fakeStream = new ByteArrayInputStream(new byte[]{1, 2, 3});
        when(storageService.download("test-image.jpg")).thenReturn(fakeStream);
        when(resizeProcessor.process(anyString(), any()))
                .thenThrow(new RuntimeException("Thumbnailator error"));

        // Act
        resizeWorker.handleResize(message, channel, deliveryTag);

        // Assert
        verify(channel).basicNack(deliveryTag, false, false);
        verify(channel, never()).basicAck(anyLong(), anyBoolean());
    }
}
