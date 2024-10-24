package model;

import javax.servlet.http.Part;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import javax.servlet.ServletException;

public class Fichier {
    String name;
    String path;
    byte[] content = new byte[0];
    public Fichier(String name, String path, byte[] content) {
        setName(name);
        setPath(path);
        setContent(content);
    }
    public Fichier(Part filePart) throws Exception {
        try{
            setContent(Fichier.partToByte(filePart));
        }
        catch(Exception e){
            throw new ServletException("Bug ato amin constructeur");
        }
        
    }
    public boolean isReady() {
        return getContent().length > 0;
    }
    public static byte[] partToByte(Part filePart) throws Exception {
        try {
            InputStream inputStream = filePart.getInputStream();
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            byte[] bytes = outputStream.toByteArray();
            outputStream.close();
            inputStream.close();
            return bytes;
            
        } catch (Exception e) {
            throw new ServletException("Tsy mety partToByte");
        }
       
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public String getPath() {
        return path;
    }
    public void setPath(String path) {
        this.path = path;
    }
    public byte[] getContent() {
        return content;
    }
    public void setContent(byte[] content) {
        this.content = content;
    }
}
