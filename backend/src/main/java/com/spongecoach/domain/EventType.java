package com.spongecoach.domain;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.List;
import java.util.UUID;

/** Training or Match — seeded with those two, but an open list (ADR-0006 pattern). */
@Entity
@Table(name = "event_type")
public class EventType extends PanacheEntityBase {

    @Id
    public UUID id;

    public String name;

    public static List<EventType> listAllOrderedByName() {
        return list("order by name asc");
    }
}
