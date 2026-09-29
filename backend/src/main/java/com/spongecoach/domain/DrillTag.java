package com.spongecoach.domain;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.List;
import java.util.UUID;

/** A label from the fixed, seeded list a coach puts on a {@link Drill}; a hint to the interpreter, never a fact. */
@Entity
@Table(name = "drill_tag")
public class DrillTag extends PanacheEntityBase {

    @Id
    public UUID id;

    public String name;

    public int position;

    public static List<DrillTag> listOrdered() {
        return list("order by position asc");
    }
}
