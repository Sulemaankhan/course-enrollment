package com.course.enrollment.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "idempotency_record")
public class IdempotencyRecord {

    @Id
    @Column(length = 200)
    private String id;

    @Lob
    @Column(nullable = false)
    private String responseJson;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected IdempotencyRecord() {
    }

    public IdempotencyRecord(String id, String responseJson, Instant createdAt) {
        this.id = id;
        this.responseJson = responseJson;
        this.createdAt = createdAt;
    }

    public String getId() {
        return id;
    }

    public String getResponseJson() {
        return responseJson;
    }
}
