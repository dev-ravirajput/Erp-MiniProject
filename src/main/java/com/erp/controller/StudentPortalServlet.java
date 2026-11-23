package com.erp.controller;

import com.erp.dao.StudentPortalDAO;
import com.erp.model.User;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;
import jakarta.servlet.annotation.MultipartConfig;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.Locale;
import java.util.UUID;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import com.erp.dao.AssignmentSubmissionDAO;
import com.erp.util.AssignmentStorage;

/** Serves read-only student pages using the identity in the authenticated session. */
@MultipartConfig(maxFileSize=10L*1024*1024,maxRequestSize=12L*1024*1024)
public class StudentPortalServlet extends HttpServlet {
    private final StudentPortalDAO dao=new StudentPortalDAO();
    private final AssignmentSubmissionDAO submissionDAO=new AssignmentSubmissionDAO();
    @Override protected void doGet(HttpServletRequest req,HttpServletResponse resp)throws ServletException,IOException{
        User user=(User)req.getSession(false).getAttribute("user");String path=req.getServletPath();String page=path.substring(path.lastIndexOf('/')+1);
        String view="/student/"+page+".jsp";
        try{
            Map<String,Object> profile=dao.getProfile(user.getId());if(profile==null){resp.sendError(HttpServletResponse.SC_NOT_FOUND,"Student profile was not found.");return;}
            req.setAttribute("studentProfile",profile);
            if("dashboard".equals(page)){List<Map<String,Object>> attendance=dao.getAttendance(user.getId());long present=attendance.stream().filter(r->"PRESENT".equals(r.get("status"))||"LATE".equals(r.get("status"))).count();req.setAttribute("attendance",attendance.stream().limit(5).toList());req.setAttribute("attendanceTotal",attendance.size());req.setAttribute("attendancePresent",present);req.setAttribute("assignments",dao.getAssignments(user.getId()).stream().limit(5).toList());req.setAttribute("marks",dao.getMarks(user.getId()).stream().limit(5).toList());}
            else if("attendance".equals(page))req.setAttribute("attendance",dao.getAttendance(user.getId()));
            else if("assignments".equals(page))req.setAttribute("assignments",dao.getAssignments(user.getId()));
            else if("fees".equals(page))req.setAttribute("invoices",dao.getInvoices(user.getId()));
            else if("marks".equals(page)||"result".equals(page))req.setAttribute("marks",dao.getMarks(user.getId()));
            req.setAttribute("studentPage",page);req.getRequestDispatcher(view).forward(req,resp);
        }catch(SQLException e){getServletContext().log("Could not load student portal",e);req.setAttribute("loadError","Student information could not be loaded. Check the database setup and run database/006_student_academic_work.sql.");if(req.getAttribute("studentProfile")==null){Map<String,Object> fallback=new LinkedHashMap<>();fallback.put("name",user.getName());fallback.put("email",user.getEmail());req.setAttribute("studentProfile",fallback);}req.setAttribute("attendance",new ArrayList<>());req.setAttribute("assignments",new ArrayList<>());req.setAttribute("marks",new ArrayList<>());req.setAttribute("attendanceTotal",0);req.setAttribute("attendancePresent",0L);req.setAttribute("studentPage",page);req.getRequestDispatcher(view).forward(req,resp);}
    }

    @Override protected void doPost(HttpServletRequest req,HttpServletResponse resp)throws ServletException,IOException{
        if(!"/student/assignments".equals(req.getServletPath())){resp.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);return;}
        User user=(User)req.getSession(false).getAttribute("user");String stored=null,original=null,type=null;long size=0;
        try{
            long assignmentId=Long.parseLong(req.getParameter("assignment_id"));
            if(assignmentId<1)throw new IllegalArgumentException("Choose a valid assignment.");
            String text=req.getParameter("submission_text");text=text==null?"":text.trim();
            if(text.length()>10000)throw new IllegalArgumentException("Submission text must be 10,000 characters or fewer.");
            Part part=req.getPart("submission_file");
            if(part!=null&&part.getSize()>0){
                if(part.getSize()>10L*1024*1024)throw new IllegalArgumentException("Files must be 10 MB or smaller.");
                original=part.getSubmittedFileName()==null?"submission":Path.of(part.getSubmittedFileName()).getFileName().toString();
                if(original.length()>255)throw new IllegalArgumentException("File name is too long.");
                String ext=extension(original);if(!java.util.Set.of("pdf","txt","png","jpg","jpeg").contains(ext))throw new IllegalArgumentException("Upload a PDF, text, PNG, or JPEG file.");
                type=part.getContentType();if(type==null||!(type.equalsIgnoreCase("application/pdf")||type.toLowerCase(Locale.ROOT).startsWith("text/")||type.equalsIgnoreCase("image/png")||type.equalsIgnoreCase("image/jpeg")))throw new IllegalArgumentException("The uploaded file type is not supported.");
                Path dir=AssignmentStorage.directory();Files.createDirectories(dir);stored=UUID.randomUUID()+"."+ext;Path target=dir.resolve(stored).normalize();if(!target.getParent().equals(dir))throw new IOException("Invalid upload path.");try(var input=part.getInputStream()){Files.copy(input,target);}size=part.getSize();
            }
            if(text.isEmpty()&&stored==null)throw new IllegalArgumentException("Add text or choose a file before submitting.");
            submissionDAO.submit(user.getId(),assignmentId,text,stored,original,type,size);
            resp.sendRedirect(req.getContextPath()+"/student/assignments?notice=submitted");
        }catch(IllegalArgumentException e){deleteStored(stored);req.setAttribute("formError",e.getMessage());doGet(req,resp);}
        catch(SQLException e){deleteStored(stored);getServletContext().log("Assignment submission failed",e);req.setAttribute("formError",e.getMessage());doGet(req,resp);}
    }
    private String extension(String filename){int dot=filename.lastIndexOf('.');return dot<0?"":filename.substring(dot+1).toLowerCase(Locale.ROOT);}
    private void deleteStored(String stored){if(stored==null)return;try{Files.deleteIfExists(AssignmentStorage.directory().resolve(stored).normalize());}catch(IOException e){getServletContext().log("Could not remove unused assignment upload",e);}}
}
