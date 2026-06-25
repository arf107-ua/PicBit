package com.imagepipeline.api.config;

import com.imagepipeline.api.model.pg.Notification;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Needed because Notification.NotificationType is a PostgreSQL custom CREATE TYPE enum.
 * Hibernate's @Enumerated(EnumType.STRING) doesn't handle custom PG types correctly —
 * this converter explicitly casts to/from String so the JDBC driver can handle it.
 */
@Converter
public class NotificationTypeConverter
        implements AttributeConverter<Notification.NotificationType, String> {

    @Override
    public String convertToDatabaseColumn(Notification.NotificationType type) {
        return type != null ? type.name() : null;
    }

    @Override
    public Notification.NotificationType convertToEntityAttribute(String value) {
        return value != null ? Notification.NotificationType.valueOf(value) : null;
    }
}
