package utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import javax.servlet.http.Part;

public class MyMultiPart {
    private Part part;

    public MyMultiPart(Part part) {
        this.part = part;
    }

    public MyMultiPart() {
    }

    // Retourne le nom original du fichier (nom du fichier téléchargé)
    public String getOriginalFilename() {
        String contentDisposition = part.getHeader("content-disposition");
        if (contentDisposition != null) {
            for (String cd : contentDisposition.split(";")) {
                if (cd.trim().startsWith("filename")) {
                    return cd.substring(cd.indexOf('=') + 1).trim().replace("\"", "");
                }
            }
        }
        return null;
    }

    // Retourne la taille du fichier
    public long getSize() {
        return part.getSize();
    }

    // Retourne le type MIME du fichier
    public String getContentType() {
        return part.getContentType();
    }

    // Retourne les données du fichier sous forme de tableau de bytes
    public byte[] getBytes() throws IOException {
        try (InputStream inputStream = part.getInputStream()) {
            return inputStream.readAllBytes();
        }
    }

    // Retourne l'InputStream du fichier
    public InputStream getInputStream() throws IOException {
        return part.getInputStream();
    }

    // Retourne true si le fichier est vide
    public boolean isEmpty() {
        return getSize() == 0;
    }

    // Retourne le nom du champ dans le formulaire
    public String getName() {
        return part.getName();
    }

    // Écrit le fichier sur le disque à l'emplacement spécifié
    public void transferTo(File dest) throws IOException {
        try (InputStream inputStream = part.getInputStream();
                FileOutputStream outputStream = new FileOutputStream(dest)) {

            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
        }
    }

    // Écrit le fichier dans un dossier en utilisant son nom d'origine
    public void transferTo(String destDirectory) throws IOException {
        String fileName = getOriginalFilename();
        if (fileName != null) {
            File file = new File(destDirectory, fileName);
            transferTo(file);
        } else {
            throw new IOException("Le nom du fichier est introuvable.");
        }
    }
}
