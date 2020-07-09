package io.orangebeard.testlisteners.fitnesse.helper;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Base64;

public class ImageEncoder {

    public static String encode(File file) throws IOException {
        String base64Image = "";
        FileInputStream imageInFile = new FileInputStream(file);
        // Reading a Image file from file system
        byte[] imageData = new byte[(int) file.length()];
        int i = 0;
        while (i != -1) {
            i = imageInFile.read(imageData);
        }
        base64Image = Base64.getEncoder().encodeToString(imageData);
        return base64Image;
    }
}
