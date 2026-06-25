package com.imagepipeline.api.config;

import com.imagepipeline.api.model.pg.ResizeJob;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Same reason as NotificationTypeConverter:
 * job_status is a custom PostgreSQL CREATE TYPE enum, not a plain VARCHAR.
 */
@Converter
public class JobStatusConverter
        implements AttributeConverter<ResizeJob.JobStatus, String> {

    @Override
    public String convertToDatabaseColumn(ResizeJob.JobStatus status) {
        return status != null ? status.name() : null;
    }

    @Override
    public ResizeJob.JobStatus convertToEntityAttribute(String value) {
        return value != null ? ResizeJob.JobStatus.valueOf(value) : null;
    }
}
