package com.spongecoach.domain;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

/** One photo of the tactic board, as downscaled JPEG bytes (ADR-0018). */
@Entity
@Table(name = "drill_sketch")
public class DrillSketch extends PanacheEntityBase {

    @Id
    public UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "drill_id", nullable = false)
    public Drill drill;

    public int position;

    public String note;

    /** Lazy, so reading a Drill's sketch list never loads the photos. */
    @Basic(fetch = FetchType.LAZY)
    public byte[] image;

    /** Set when the photo was removed; it stays restorable, and its position is never reused (ADR-0020). */
    @Column(name = "deleted_at")
    public Instant deletedAt;

    /** The interpreter's latest symbol list for this photo, as JSON; null until read (ADR-0019). */
    @JdbcTypeCode(SqlTypes.JSON)
    public String reading;
}
