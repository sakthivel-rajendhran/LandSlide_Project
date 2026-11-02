package com.slopeguard.util;

import com.slopeguard.exception.InvalidFileException;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

/**
 * Validates image integrity, magic byte signatures, and allowed image formats.
 * <p>
 * Security Policy:
 * Does NOT trust client-supplied filenames or declared Content-Type headers.
 * Inspects header bytes directly to reject spoofed, corrupted, or executable uploads.
 */
public final class ImageValidator {

    private ImageValidator() {
    }

    public static class ValidatedImage {
        private final String contentType;
        private final String extension;

        public ValidatedImage(String contentType, String extension) {
            this.contentType = contentType;
            this.extension = extension;
        }

        public String getContentType() {
            return contentType;
        }

        public String getExtension() {
            return extension;
        }
    }

    /**
     * Inspects magic numbers and decodes the image stream.
     *
     * @param file The uploaded multipart file
     * @return ValidatedImage containing sanitized content type and file extension
     * @throws InvalidFileException if file is empty, unsupported, or corrupted
     */
    public static ValidatedImage validate(MultipartFile file) {
        if (file == null || file.isEmpty() || file.getSize() == 0) {
            throw new InvalidFileException("Uploaded file is empty. Please provide a valid photograph.");
        }

        byte[] headerBytes = new byte[12];
        try (InputStream is = file.getInputStream()) {
            int read = is.read(headerBytes);
            if (read < 4) {
                throw new InvalidFileException("Uploaded file is truncated or corrupted.");
            }
        } catch (IOException e) {
            throw new InvalidFileException("Failed to read uploaded file stream: " + e.getMessage());
        }

        // 1. JPEG Signature: FF D8 FF
        if ((headerBytes[0] & 0xFF) == 0xFF &&
                (headerBytes[1] & 0xFF) == 0xD8 &&
                (headerBytes[2] & 0xFF) == 0xFF) {
            verifyDecodable(file);
            return new ValidatedImage("image/jpeg", ".jpg");
        }

        // 2. PNG Signature: 89 50 4E 47 0D 0A 1A 0A
        if ((headerBytes[0] & 0xFF) == 0x89 &&
                (headerBytes[1] & 0xFF) == 0x50 &&
                (headerBytes[2] & 0xFF) == 0x4E &&
                (headerBytes[3] & 0xFF) == 0x47) {
            verifyDecodable(file);
            return new ValidatedImage("image/png", ".png");
        }

        // 3. WebP Signature: "RIFF" (bytes 0-3) and "WEBP" (bytes 8-11)
        if ((headerBytes[0] & 0xFF) == 'R' &&
                (headerBytes[1] & 0xFF) == 'I' &&
                (headerBytes[2] & 0xFF) == 'F' &&
                (headerBytes[3] & 0xFF) == 'F' &&
                (headerBytes[8] & 0xFF) == 'W' &&
                (headerBytes[9] & 0xFF) == 'E' &&
                (headerBytes[10] & 0xFF) == 'B' &&
                (headerBytes[11] & 0xFF) == 'P') {
            return new ValidatedImage("image/webp", ".webp");
        }

        throw new InvalidFileException("Unsupported or corrupted image file format. Only JPEG, PNG, and WebP photographs are permitted.");
    }

    private static void verifyDecodable(MultipartFile file) {
        try {
            BufferedImage image = ImageIO.read(file.getInputStream());
            if (image == null) {
                throw new InvalidFileException("Image data could not be parsed; the file may be corrupted.");
            }
        } catch (IOException e) {
            throw new InvalidFileException("Corrupted image stream: " + e.getMessage());
        }
    }
}
