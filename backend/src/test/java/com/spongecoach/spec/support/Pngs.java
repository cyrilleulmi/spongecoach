package com.spongecoach.spec.support;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.concurrent.ThreadLocalRandom;

/** PNG bytes for Avatar scenarios, standing in for what the painter uploads. */
public final class Pngs {

    private Pngs() {
    }

    /** A plain painted-looking image: a random fill with a circle on top, so no two are equal. */
    public static byte[] painted(int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        ThreadLocalRandom random = ThreadLocalRandom.current();
        graphics.setColor(new Color(random.nextInt(0xffffff)));
        graphics.fillRect(0, 0, width, height);
        graphics.setColor(new Color(random.nextInt(0xffffff)));
        graphics.fillOval(width / 4, height / 4, width / 2, height / 2);
        graphics.dispose();
        return encode(image);
    }

    /** Per-pixel noise barely compresses, so a 512 px square of it lands well over 200 KB. */
    public static byte[] noise(int side) {
        BufferedImage image = new BufferedImage(side, side, BufferedImage.TYPE_INT_RGB);
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int y = 0; y < side; y++) {
            for (int x = 0; x < side; x++) {
                image.setRGB(x, y, random.nextInt(0xffffff));
            }
        }
        return encode(image);
    }

    private static byte[] encode(BufferedImage image) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
