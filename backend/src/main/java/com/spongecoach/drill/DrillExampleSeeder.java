package com.spongecoach.drill;

import com.spongecoach.domain.Drill;
import com.spongecoach.domain.DrillMessage;
import com.spongecoach.domain.DrillMessageAuthor;
import com.spongecoach.domain.DrillScriptSource;
import com.spongecoach.domain.DrillSketch;
import com.spongecoach.domain.DrillStatus;
import com.spongecoach.domain.DrillTag;
import com.spongecoach.domain.SketchRelation;
import com.spongecoach.domain.Team;
import io.quarkus.logging.Log;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Loads the example Drills from {@code docs/drills/examples} in dev mode, so the app has drills to
 * look at without spending API credits (ADR-0018). Each example folder holds the photos, the
 * coach's {@code expected.md} (for the tags and how the photos relate), and {@code animation.json}:
 * Claude's answer from a real run of {@code gradlew drillExamples}.
 *
 * <p>An example is added once, by name, and never again, even after a coach deleted it. Only on in
 * the dev profile: the photos live in the repository, not in a deployed app.
 */
@ApplicationScoped
public class DrillExampleSeeder {

    @ConfigProperty(name = "spongecoach.drills.seed-examples", defaultValue = "false")
    boolean enabled;

    @ConfigProperty(name = "spongecoach.drills.examples-dir", defaultValue = "../docs/drills/examples")
    String examplesDir;

    void onStart(@Observes StartupEvent event) {
        if (!enabled) {
            return;
        }
        Path root = Path.of(examplesDir);
        if (!Files.isDirectory(root)) {
            Log.warnf("drill examples: %s not found, nothing seeded", root.toAbsolutePath());
            return;
        }
        try (Stream<Path> dirs = Files.list(root)) {
            dirs.filter(dir -> Files.exists(dir.resolve("animation.json"))).sorted().forEach(this::seed);
        } catch (IOException e) {
            Log.warnf(e, "drill examples: could not read %s", root.toAbsolutePath());
        }
    }

    private void seed(Path example) {
        String name = displayName(example.getFileName().toString());
        try {
            Interpretation animation = DrillJson.readLenient(Files.readString(example.resolve("animation.json")), Interpretation.class);
            String expected = Files.exists(example.resolve("expected.md")) ? Files.readString(example.resolve("expected.md")) : "";
            List<byte[]> photos = photos(example);
            List<String> errors = DrillScriptValidator.validate(animation.script(), java.util.stream.IntStream.rangeClosed(1, photos.size()).boxed().collect(java.util.stream.Collectors.toSet()));
            if (!errors.isEmpty()) {
                Log.warnf("drill examples: %s is not playable, skipped: %s", name, errors);
                return;
            }
            QuarkusTransaction.requiringNew().run(() -> {
                if (Drill.count("name", name) > 0) {
                    return;
                }
                create(name, animation, expected, photos);
                Log.infof("drill examples: added %s", name);
            });
        } catch (IOException e) {
            Log.warnf(e, "drill examples: could not read %s", example);
        }
    }

    private static void create(String name, Interpretation animation, String expected, List<byte[]> photos) {
        Instant now = Instant.now();
        Drill drill = new Drill();
        drill.id = UUID.randomUUID();
        drill.team = Team.theTeam();
        drill.name = name;
        drill.sketchRelation = relation(expected);
        drill.status = DrillStatus.READY;
        drill.createdAt = now;
        drill.updatedAt = now;
        for (String tagName : tags(expected)) {
            DrillTag tag = DrillTag.find("name", tagName).firstResult();
            if (tag != null) {
                drill.tags.add(tag);
            }
        }
        for (int i = 0; i < photos.size(); i++) {
            int position = i + 1;
            DrillSketch sketch = new DrillSketch();
            sketch.id = UUID.randomUUID();
            sketch.drill = drill;
            sketch.position = position;
            sketch.image = photos.get(i);
            sketch.reading = animation.readings() == null ? null : animation.readings().stream()
                    .filter(reading -> reading.sketch() == position)
                    .findFirst()
                    .map(DrillJson::write)
                    .orElse(null);
            drill.sketches.add(sketch);
        }
        drill.persist();

        var version = DrillJobs.newVersion(drill, animation.script(), DrillScriptSource.AI, null);
        DrillMessage reply = new DrillMessage();
        reply.id = UUID.randomUUID();
        reply.drill = drill;
        reply.position = 1;
        reply.author = DrillMessageAuthor.INTERPRETER;
        reply.content = animation.message();
        reply.scriptVersion = version.version;
        reply.createdAt = now;
        reply.persist();
    }

    /** The photos in file order, prepared like an upload. */
    private static List<byte[]> photos(Path example) throws IOException {
        List<Path> files;
        try (Stream<Path> list = Files.list(example)) {
            files = list.filter(file -> file.toString().toLowerCase().endsWith(".jpg")).sorted().toList();
        }
        List<byte[]> photos = new ArrayList<>();
        for (Path file : files) {
            photos.add(SketchImages.uprightJpeg(Files.readAllBytes(file)));
        }
        return photos;
    }

    /** "bresil" → "Bresil"; "3v2" stays. */
    static String displayName(String folder) {
        return folder.isEmpty() ? folder : Character.toUpperCase(folder.charAt(0)) + folder.substring(1);
    }

    /** The tags an {@code expected.md} names on its {@code **Tags:**} line. */
    static List<String> tags(String expected) {
        Matcher matcher = Pattern.compile("\\*\\*Tags:\\*\\*\\s*(.+)").matcher(expected);
        if (!matcher.find()) {
            return List.of();
        }
        return Stream.of(matcher.group(1).split(",")).map(String::trim).filter(tag -> !tag.isEmpty()).toList();
    }

    /** How an {@code expected.md} says its photos relate. */
    static SketchRelation relation(String expected) {
        Matcher matcher = Pattern.compile("\\*\\*Photo relation:\\*\\*\\s*(\\w+)").matcher(expected);
        String relation = matcher.find() ? matcher.group(1).toLowerCase() : "";
        return switch (relation) {
            case "progression" -> SketchRelation.PROGRESSION;
            case "continuation" -> SketchRelation.CONTINUOUS;
            default -> SketchRelation.MIXED;
        };
    }
}
