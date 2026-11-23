package com.erp.controller;

import com.erp.dao.AssignmentSubmissionDAO;
import com.erp.model.User;
import com.erp.util.AssignmentStorage;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.Map;

/** Streams assignment files only to the submitting student, an assigned teacher, or guardian. */
public class AssignmentFileServlet extends HttpServlet {
    private final AssignmentSubmissionDAO dao=new AssignmentSubmissionDAO();
    @Override protected void doGet(HttpServletRequest req,HttpServletResponse resp)throws IOException{
        Object value=req.getSession(false)==null?null:req.getSession(false).getAttribute("user");
        if(!(value instanceof User user)){resp.sendRedirect(req.getContextPath()+"/login.jsp?error=session");return;}
        String info=req.getPathInfo(),filename=info==null?"":info.substring(1);
        if(!filename.matches("[0-9a-fA-F-]{36}\\.(pdf|txt|png|jpg|jpeg)")){resp.sendError(404);return;}
        try{
            Map<String,Object> metadata=dao.fileForViewer(filename,user);if(metadata==null){resp.sendError(404);return;}
            Path dir=AssignmentStorage.directory(),file=dir.resolve(filename).normalize();if(!file.getParent().equals(dir)||!Files.isRegularFile(file)){resp.sendError(404);return;}
            String original=String.valueOf(metadata.get("original_filename"));String safe=original.replaceAll("[\\r\\n\\\"\\\\]","_");
            resp.setContentType("application/octet-stream");resp.setHeader("X-Content-Type-Options","nosniff");resp.setHeader("Content-Disposition","attachment; filename=\""+safe+"\"");resp.setContentLengthLong(Files.size(file));Files.copy(file,resp.getOutputStream());
        }catch(SQLException e){getServletContext().log("Could not authorize assignment file download",e);resp.sendError(500);}
    }
}
