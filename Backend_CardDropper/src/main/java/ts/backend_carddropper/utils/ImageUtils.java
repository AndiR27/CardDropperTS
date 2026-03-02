package ts.backend_carddropper.utils;

import lombok.experimental.UtilityClass;

@UtilityClass
public class ImageUtils {

    /**
     * Nettoie un nom de fichier en ne gardant que les caractères sûrs.
     */
    public String sanitizeFilename(String filename) {
        if (filename == null || filename.isBlank()) {
            return "image";
        }
        return filename.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
