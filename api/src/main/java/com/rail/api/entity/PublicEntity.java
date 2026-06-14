package com.rail.api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

@MappedSuperclass
@Getter
@Setter
public class PublicEntity extends BaseEntity {

    @UuidGenerator
    @Column(nullable = false, unique = true, updatable = false)
    private UUID pid;
}
