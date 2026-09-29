package com.spongecoach.domain;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** One version of a Drill's Stages as a single JSON document (ADR-0018). Never changed once written. */
@Entity
@Table(name = "drill_script_version")
public class DrillScriptVersion extends PanacheEntityBase {

    @Id
    public UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "drill_id", nullable = false)
    public Drill drill;

    public int version;

    @Enumerated(EnumType.STRING)
    public DrillScriptSource source;

    @JdbcTypeCode(SqlTypes.JSON)
    public String script;

    @Column(name = "change_summary")
    public String changeSummary;

    @Column(name = "created_at")
    public Instant createdAt;

    public static DrillScriptVersion find(UUID drillId, int version) {
        return find("drill.id = ?1 and version = ?2", drillId, version).firstResult();
    }

    /** Newest first. */
    public static List<DrillScriptVersion> listForDrill(UUID drillId) {
        return list("drill.id = ?1 order by version desc", drillId);
    }
}
