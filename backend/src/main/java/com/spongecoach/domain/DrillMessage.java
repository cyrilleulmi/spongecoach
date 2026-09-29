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
import java.util.UUID;

/**
 * One turn of the conversation with the interpreter, append-only (ADR-0019): a coach's answers or
 * chat message, or the interpreter's reply (Clarifying questions, or the script version it made).
 */
@Entity
@Table(name = "drill_message")
public class DrillMessage extends PanacheEntityBase {

    @Id
    public UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "drill_id", nullable = false)
    public Drill drill;

    public int position;

    @Enumerated(EnumType.STRING)
    public DrillMessageAuthor author;

    public String content;

    /** The interpreter's Clarifying questions, as JSON; null unless it asked. */
    @JdbcTypeCode(SqlTypes.JSON)
    public String questions;

    /** The coach's answers to those questions, as JSON; null for a plain chat message. */
    @JdbcTypeCode(SqlTypes.JSON)
    public String answers;

    /** The script version this interpreter reply produced; null if it only asked. */
    @Column(name = "script_version")
    public Integer scriptVersion;

    @Column(name = "created_at")
    public Instant createdAt;
}
