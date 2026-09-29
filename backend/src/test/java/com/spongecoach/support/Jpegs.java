package com.spongecoach.support;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;

/** Real, tiny JPEGs standing in for photos of the tactic board. */
public final class Jpegs {

    private Jpegs() {
    }

    /** A white board with one blue stroke, as a JPEG. */
    public static byte[] sketch() {
        BufferedImage image = new BufferedImage(48, 64, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, 48, 64);
        graphics.setColor(Color.BLUE);
        graphics.drawLine(8, 56, 40, 8);
        graphics.dispose();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            ImageIO.write(image, "jpg", out);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return out.toByteArray();
    }
}
