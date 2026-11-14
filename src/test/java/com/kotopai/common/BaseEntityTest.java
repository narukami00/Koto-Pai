package com.kotopai.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * Unit tests for {@link BaseEntity}.
 *
 * <p>Uses a concrete test subclass since BaseEntity is abstract.
 */
class BaseEntityTest {

    @Entity
    @Table(name = "test_entities")
    static class TestEntity extends BaseEntity {
        private String name;

        TestEntity() {}

        TestEntity(String name) {
            this.name = name;
        }

        String getName() {
            return name;
        }
    }

    @Test
    void newEntity_hasNullIdBeforePersist() {
        TestEntity entity = new TestEntity("test");
        assertThat(entity.getId()).isNull();
    }

    @Test
    void newEntity_hasNullCreatedAtBeforePrePersist() {
        TestEntity entity = new TestEntity("test");
        assertThat(entity.getCreatedAt()).isNull();
    }

    @Test
    void prePersist_setsCreatedAt() {
        TestEntity entity = new TestEntity("test");

        // Simulate what JPA calls before INSERT
        entity.initCreatedAt();

        assertThat(entity.getCreatedAt()).isNotNull();
        assertThat(entity.getCreatedAt()).isBeforeOrEqualTo(Instant.now());
    }

    @Test
    void prePersist_doesNotOverwriteExistingCreatedAt() {
        TestEntity entity = new TestEntity("test");

        // First call sets createdAt
        entity.initCreatedAt();
        Instant firstTimestamp = entity.getCreatedAt();

        // Second call should NOT overwrite
        entity.initCreatedAt();
        assertThat(entity.getCreatedAt()).isEqualTo(firstTimestamp);
    }
}

