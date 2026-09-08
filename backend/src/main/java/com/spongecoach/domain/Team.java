package com.spongecoach.domain;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "team")
public class Team extends PanacheEntityBase {

    @Id
    public UUID id;

    public String name;

    public static Team theTeam() {
        return findAll().firstResult();
    }
}
