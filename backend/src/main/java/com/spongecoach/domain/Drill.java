package com.spongecoach.domain;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** An exercise drawn on a tactic board and animated by the app (ADR-0018). */
@Entity
@Table(name = "drill")
public class Drill extends PanacheEntityBase {

    @Id
    public UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false)
    public Team team;

    public String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "sketch_relation")
    public SketchRelation sketchRelation;

    @Enumerated(EnumType.STRING)
    public DrillStatus status;

    /** Why the last job failed; null unless {@link DrillStatus#FAILED}. */
    public String error;

    /** The {@link DrillScriptVersion} shown and edited; null until the interpreter produced one. */
    @Column(name = "current_version")
    public Integer currentVersion;

    @Column(name = "created_at")
    public Instant createdAt;

    @Column(name = "updated_at")
    public Instant updatedAt;

    @Column(name = "deleted_at")
    public Instant deletedAt;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "drill_drill_tag",
            joinColumns = @JoinColumn(name = "drill_id"),
            inverseJoinColumns = @JoinColumn(name = "drill_tag_id"))
    @OrderBy("position asc")
    public List<DrillTag> tags = new ArrayList<>();

    @OneToMany(mappedBy = "drill", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("position asc")
    public List<DrillSketch> sketches = new ArrayList<>();

    @OneToMany(mappedBy = "drill", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @OrderBy("position asc")
    public List<DrillMessage> messages = new ArrayList<>();

    /** Most recently changed first. */
    public static List<Drill> listActive() {
        return list("deletedAt is null order by updatedAt desc");
    }

    /** Drills whose job was running when the app stopped; they can never finish (ADR-0019). */
    public static List<Drill> listPending() {
        return list("status", DrillStatus.PENDING);
    }
}
