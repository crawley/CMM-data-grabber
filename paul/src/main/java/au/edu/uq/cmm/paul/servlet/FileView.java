package au.edu.uq.cmm.paul.servlet;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.springframework.web.servlet.view.AbstractView;

/**
 * This view can be used to implement downloading of files from the 
 * ingestion queue area.  Note - you are likely to get better performance if
 * you use a native code webserver instead of using Tomcat for file downloads.
 * However, there's a good chance that this won't be significant.
 * 
 * @author scrawley
 */
public class FileView extends AbstractView {
    private static final Logger LOG = Logger.getLogger(FileView.class);

    @Override
    protected void renderMergedOutputModel(Map<String, Object> model,
            HttpServletRequest request, HttpServletResponse response) 
                    throws IOException {
        File file = (File) model.get("file");
        String contentType = (String) model.get("contentType");
        try (FileInputStream fis = new FileInputStream(file)) {
            response.setContentType(contentType);
            long length = file.length();
            if (length <= Integer.MAX_VALUE) {
                response.setContentLength((int) length);
            }
            response.setStatus(HttpServletResponse.SC_OK);
            try (OutputStream os = response.getOutputStream()) {
                byte[] buffer = new byte[8192];
                int nosRead;
                while ((nosRead = fis.read(buffer)) > 0) {
                    os.write(buffer, 0, nosRead);
                }
            }
        } catch (FileNotFoundException ex) {
            LOG.info("Cannot access file: " + ex.getLocalizedMessage());
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        }
    }

}