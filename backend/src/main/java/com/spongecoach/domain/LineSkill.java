package com.spongecoach.domain;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "line_skill")
public class LineSkill extends PanacheEntityBase {

    @EmbeddedId
    public LineSkillId id = new LineSkillId();

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("line")
    @JoinColumn(name = "line_id")
    public Line line;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("skill")
    @JoinColumn(name = "skill_id")
    public Skill skill;

    public int rating;

    public static LineSkill find(UUID lineId, UUID skillId) {
        return findById(new LineSkillId(lineId, skillId));
    }

    public static List<LineSkill> listForLine(UUID lineId) {
        return list("line.id", lineId);
    }
}
