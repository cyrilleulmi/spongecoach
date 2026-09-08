package com.spongecoach.domain;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "skill")
public class Skill extends PanacheEntityBase {

    @Id
    public UUID id;

    public String name;

    public String color;

    @Column(name = "deleted_at")
    public Instant deletedAt;

    public static List<Skill> listActive() {
        return list("deletedAt is null order by name asc");
    }
}
