package com.fabiogrossi.dungeonsoundmanager.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;

public class SoundUtils {

    public static long calculateDurationOgg(final File oggFile) throws IOException {
        long rate = -1;
        long length = -1;

        long size = Files.size(oggFile.toPath());
        byte[] bytes = new byte[Math.toIntExact(size)];

        try (FileInputStream stream = new FileInputStream(oggFile)) {
            long readBytes = stream.read(bytes);
            if (readBytes != size) {
                throw new RuntimeException("Unable to read file correctly. Read bytes are not as much as the filesize");
            }
            for (int i = (int) (size - 1L - 8L - 2L - 4L); i >= 0 && i > length; i--) { //4 bytes for "OggS", 2 unused bytes, 8 bytes for length
                // Looking for length (value after last "OggS")
                if (bytes[i] == (byte) 'O' && bytes[i + 1] == (byte) 'g' && bytes[i + 2] == (byte) 'g' && bytes[i + 3] == (byte) 'S') {
                    byte[] byteArray = new byte[]{bytes[i + 6], bytes[i + 7], bytes[i + 8], bytes[i + 9], bytes[i + 10], bytes[i + 11], bytes[i + 12], bytes[i + 13]};
                    ByteBuffer bb = ByteBuffer.wrap(byteArray);
                    bb.order(ByteOrder.LITTLE_ENDIAN);
                    length = bb.getInt(0);
                }
            }
            for (int i = 0; i < size - 8 - 2 - 4 && rate < 0; i++) {
                // Looking for rate (first value after "vorbis")
                if (bytes[i] == (byte) 'v' && bytes[i + 1] == (byte) 'o' && bytes[i + 2] == (byte) 'r' && bytes[i + 3] == (byte) 'b' && bytes[i + 4] == (byte) 'i' && bytes[i + 5] == (byte) 's') {
                    byte[] byteArray = new byte[]{bytes[i + 11], bytes[i + 12], bytes[i + 13], bytes[i + 14]};
                    ByteBuffer bb = ByteBuffer.wrap(byteArray);
                    bb.order(ByteOrder.LITTLE_ENDIAN);
                    rate = bb.getInt(0);
                }

            }
        }
        return (length / rate) * 1000; // Milliseconds
    }
}
