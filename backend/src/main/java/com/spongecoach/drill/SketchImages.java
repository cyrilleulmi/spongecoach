package com.spongecoach.drill;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

/**
 * Makes a phone photo what the browser uploads (`image-prep.ts`, ADR-0018): upright, at most
 * {@link #LONG_EDGE} on its long side, JPEG. Used where the backend reads photos straight from
 * disk: the example seeder and the example test.
 *
 * <p>Phones store a portrait photo as landscape pixels plus an EXIF rotation. The browser applies
 * it; ImageIO doesn't, so it's done here, or Claude sees the board sideways.
 */
public final class SketchImages {

    public static final int LONG_EDGE = 1568;
    private static final float QUALITY = 0.85f;

    private SketchImages() {
    }

    public static byte[] uprightJpeg(byte[] photo) throws IOException {
        BufferedImage original = rotate(ImageIO.read(new ByteArrayInputStream(photo)), exifOrientation(photo));
        double scale = Math.min(1.0, (double) LONG_EDGE / Math.max(original.getWidth(), original.getHeight()));
        int width = (int) Math.round(original.getWidth() * scale);
        int height = (int) Math.round(original.getHeight() * scale);
        BufferedImage scaled = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = scaled.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        graphics.drawImage(original, 0, 0, width, height, null);
        graphics.dispose();

        ImageWriter writer = ImageIO.getImageWritersByFormatName("jpg").next();
        ImageWriteParam param = writer.getDefaultWriteParam();
        param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        param.setCompressionQuality(QUALITY);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (MemoryCacheImageOutputStream stream = new MemoryCacheImageOutputStream(out)) {
            writer.setOutput(stream);
            writer.write(null, new IIOImage(scaled, null, null), param);
        } finally {
            writer.dispose();
        }
        return out.toByteArray();
    }

    /** Orientations 3, 6 and 8 (the rotations); mirrored ones don't come out of phone cameras. */
    private static BufferedImage rotate(BufferedImage image, int orientation) {
        int quarterTurns = switch (orientation) {
            case 6 -> 1;
            case 3 -> 2;
            case 8 -> 3;
            default -> 0;
        };
        if (quarterTurns == 0) {
            return image;
        }
        boolean swap = quarterTurns % 2 == 1;
        int width = swap ? image.getHeight() : image.getWidth();
        int height = swap ? image.getWidth() : image.getHeight();
        BufferedImage rotated = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = rotated.createGraphics();
        graphics.translate(width / 2.0, height / 2.0);
        graphics.rotate(quarterTurns * Math.PI / 2);
        graphics.translate(-image.getWidth() / 2.0, -image.getHeight() / 2.0);
        graphics.drawImage(image, 0, 0, null);
        graphics.dispose();
        return rotated;
    }

    /** The EXIF orientation tag of a JPEG, or 1 when it has none. */
    static int exifOrientation(byte[] jpeg) {
        ByteBuffer data = ByteBuffer.wrap(jpeg);
        int i = 2;
        while (i + 4 < jpeg.length && (jpeg[i] & 0xff) == 0xff) {
            int marker = jpeg[i + 1] & 0xff;
            int length = data.order(ByteOrder.BIG_ENDIAN).getShort(i + 2) & 0xffff;
            if (marker == 0xe1 && new String(jpeg, i + 4, 4, StandardCharsets.US_ASCII).equals("Exif")) {
                int tiff = i + 10;
                data.order(jpeg[tiff] == 'I' ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN);
                int ifd = tiff + data.getInt(tiff + 4);
                int entries = data.getShort(ifd) & 0xffff;
                for (int entry = 0; entry < entries; entry++) {
                    int at = ifd + 2 + entry * 12;
                    if ((data.getShort(at) & 0xffff) == 0x0112) {
                        return data.getShort(at + 8) & 0xffff;
                    }
                }
                return 1;
            }
            if (marker == 0xda) {
                break;
            }
            i += 2 + length;
        }
        return 1;
    }
}
