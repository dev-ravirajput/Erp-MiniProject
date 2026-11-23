package com.erp.controller;

import com.erp.dao.TeacherPortalDAO;
import com.erp.dao.NotificationDAO;
import com.erp.dao.AssignmentSubmissionDAO;
import com.erp.model.User;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class TeacherPortalServlet extends HttpServlet {
    private final TeacherPortalDAO dao=new TeacherPortalDAO();
    private final NotificationDAO notificationDAO=new NotificationDAO();
    private final AssignmentSubmissionDAO submissionDAO=new AssignmentSubmissionDAO();
    @Override protected void doGet(HttpServletRequest req,HttpServletResponse resp)throws ServletException,IOException{
        User user=(User)req.getSession(false).getAttribute("user");
        String page=req.getServletPath().substring("/teacher/".length());
        String stage="teacher profile";
        req.setAttribute("teacherPage",page);
        try {
            long teacherId=dao.teacherId(user.getId());
            Map<String,Object> profile=dao.profile(user.getId());
            if(profile==null){resp.sendError(404,"Teacher profile was not found.");return;} 
            req.setAttribute("teacherProfile",profile);

            stage="teaching assignments";
            req.setAttribute("teachingAssignments",dao.assignments(user.getId()));
            stage="student list";
            req.setAttribute("students",dao.students(user.getId()));
            switch(page){
                case "dashboard":
                    stage="dashboard data";
                    req.setAttribute("classes",dao.classes(user.getId()));
                    req.setAttribute("assignments",dao.assignmentsList(teacherId));
                    req.setAttribute("exams",dao.exams(user.getId()));
                    break;
                case "profile": break;
                case "students":
                    stage="student list";
                    break;
                case "classes":
                    stage="class list";
                    req.setAttribute("classes",dao.classes(user.getId()));
                    break;
                case "attendance": {
                    stage="attendance roster";
                    String date=value(req,"date");if(date.isEmpty())date=LocalDate.now().toString();
                    String sessionLabel=value(req,"session_label");if(sessionLabel.isEmpty())sessionLabel="Class";
                    req.setAttribute("date",date);req.setAttribute("sessionLabel",sessionLabel);req.setAttribute("attendanceRows",List.of());
                    req.setAttribute("assignmentId",value(req,"assignment_id"));
                    if(!value(req,"assignment_id").isEmpty())req.setAttribute("attendanceRows",dao.attendanceFor(Long.parseLong(value(req,"assignment_id")),user.getId(),date,sessionLabel));
                    break;
                }
                case "assignments":
                    stage="assignment records";
                    req.setAttribute("assignments",dao.assignmentsList(teacherId));
                    String submissionsFor=value(req,"submissions_for");
                    if(!submissionsFor.isEmpty()){long selectedAssignment=Long.parseLong(submissionsFor);req.setAttribute("submissionsFor",selectedAssignment);req.setAttribute("submissions",submissionDAO.teacherSubmissions(teacherId,selectedAssignment));}
                    if("edit".equals(req.getParameter("action"))){Long id=parse(req.getParameter("id"));Map<String,Object> item=id==null?null:dao.assignment(teacherId,id);if(item==null){resp.sendError(404,"Assignment was not found.");return;}req.setAttribute("assignmentForm",item);}
                    break;
                case "marks":
                    stage="exam roster";
                    req.setAttribute("exams",dao.exams(user.getId()));req.setAttribute("examId",value(req,"exam_id"));req.setAttribute("markRows",List.of());
                    if(!value(req,"exam_id").isEmpty())req.setAttribute("markRows",dao.examRoster(Long.parseLong(value(req,"exam_id")),user.getId()));
                    break;
                default:resp.sendError(404);return;
            }
            req.getRequestDispatcher("/teacher/"+page+".jsp").forward(req,resp);
        } catch(Exception e) {
            if(e instanceof NumberFormatException){resp.sendError(400,"Invalid selection.");return;}
            getServletContext().log("Could not load teacher "+stage+" on page "+page+" for user "+user.getId(),e);
            String diagnostic=e instanceof SQLException sqlException?"SQLState "+sqlException.getSQLState()+", code "+sqlException.getErrorCode():e.getClass().getSimpleName();
            req.setAttribute("loadError","Could not load "+stage+" ("+diagnostic+"). Check the Tomcat log for details.");
            req.setAttribute("teacherProfile",fallback(user));req.setAttribute("teachingAssignments",new ArrayList<>());
            req.setAttribute("classes",new ArrayList<>());req.setAttribute("assignments",new ArrayList<>());
            req.setAttribute("students",new ArrayList<>());
            req.setAttribute("exams",new ArrayList<>());req.setAttribute("attendanceRows",new ArrayList<>());
            req.setAttribute("markRows",new ArrayList<>());req.setAttribute("date",LocalDate.now().toString());
            req.getRequestDispatcher("/teacher/"+page+".jsp").forward(req,resp);
        }
    }
    @Override protected void doPost(HttpServletRequest req,HttpServletResponse resp)throws ServletException,IOException{
        User user=(User)req.getSession(false).getAttribute("user");String page=req.getServletPath().substring("/teacher/".length());String base=req.getContextPath()+"/teacher/"+page;
        try{long teacherId=dao.teacherId(user.getId());switch(page){
            case "attendance":{long assignmentId=requiredId(req,"assignment_id");String date=value(req,"date"),label=value(req,"session_label");if(label.isEmpty())label="Class";if(label.length()>40)throw new IllegalArgumentException("Session label must be 40 characters or fewer.");LocalDate.parse(date);String[] ids=req.getParameterValues("student_id"),statuses=req.getParameterValues("status");if(ids==null||statuses==null||ids.length==0||ids.length!=statuses.length)throw new IllegalArgumentException("Select an attendance status for each student.");dao.saveAttendance(teacherId,assignmentId,date,label,ids,statuses);resp.sendRedirect(base+"?assignment_id="+assignmentId+"&date="+date+"&session_label="+java.net.URLEncoder.encode(label,java.nio.charset.StandardCharsets.UTF_8)+"&notice=saved");return;}
            case "assignments":{String action=value(req,"action");if("delete".equals(action)){dao.deleteAssignment(teacherId,requiredId(req,"id"));resp.sendRedirect(base+"?notice=deleted");return;}if("grade_submission".equals(action)){long assignmentId=requiredId(req,"assignment_id");long submissionId=requiredId(req,"submission_id");String grade=value(req,"grade"),feedback=value(req,"feedback");if(feedback.length()>4000)throw new IllegalArgumentException("Feedback must be 4,000 characters or fewer.");submissionDAO.grade(teacherId,submissionId,grade,feedback);resp.sendRedirect(base+"?submissions_for="+assignmentId+"&notice=graded");return;}boolean update="update".equals(action);Long id=update?requiredId(req,"id"):null;long assignmentId=requiredId(req,"assignment_id");String title=value(req,"title"),description=value(req,"description"),assigned=value(req,"assigned_date"),due=value(req,"due_date");if(title.isEmpty()||title.length()>200)throw new IllegalArgumentException("Title is required and must be 200 characters or fewer.");if(description.length()>10000)throw new IllegalArgumentException("Description is too long.");LocalDate start=LocalDate.parse(assigned),end=LocalDate.parse(due);if(end.isBefore(start))throw new IllegalArgumentException("Due date cannot be earlier than assigned date.");long savedAssignmentId=dao.saveAssignment(teacherId,id,assignmentId,title,description,assigned,due);try{notificationDAO.notifyAssignmentStudents(savedAssignmentId,teacherId,update?"Assignment updated":"New assignment",title+" is due "+due+".","/student/assignments");}catch(SQLException e){getServletContext().log("Assignment saved, but student notifications could not be created",e);}resp.sendRedirect(base+"?notice=saved");return;}
            case "marks":{String action=value(req,"action");long examId=requiredId(req,"exam_id");if("publish".equals(action)){dao.publishResults(user.getId(),examId);try{notificationDAO.notifyPublishedResults(examId,user.getId());}catch(SQLException e){getServletContext().log("Results published, but student notifications could not be created",e);}resp.sendRedirect(base+"?exam_id="+examId+"&notice=published");return;}if("unpublish".equals(action)){dao.unpublishResults(user.getId(),examId);resp.sendRedirect(base+"?exam_id="+examId+"&notice=unpublished");return;}String[] ids=req.getParameterValues("student_id"),marks=req.getParameterValues("marks"),remarks=req.getParameterValues("remarks");if(ids==null||marks==null||remarks==null||ids.length!=marks.length||ids.length!=remarks.length)throw new IllegalArgumentException("The marks form is incomplete.");dao.saveMarks(user.getId(),examId,ids,marks,remarks);resp.sendRedirect(base+"?exam_id="+examId+"&notice=saved");return;}
            default:resp.sendError(405);}
        }catch(java.time.DateTimeException e){req.setAttribute("formError","Enter valid dates for the selected records.");preserveAssignmentForm(req,page);req.setAttribute("teacherPage",page);doGet(req,resp);}catch(IllegalArgumentException e){req.setAttribute("formError",e.getMessage());preserveAssignmentForm(req,page);req.setAttribute("teacherPage",page);doGet(req,resp);}catch(SQLException e){getServletContext().log("Teacher portal update failed",e);req.setAttribute("formError",e.getMessage()!=null?e.getMessage():"The information could not be saved.");preserveAssignmentForm(req,page);req.setAttribute("teacherPage",page);doGet(req,resp);}
    }
    private String value(HttpServletRequest r,String k){String v=r.getParameter(k);return v==null?"":v.trim();}
    private Long parse(String raw){try{return raw==null?null:Long.valueOf(raw);}catch(NumberFormatException e){return null;}}
    private long requiredId(HttpServletRequest r,String key){Long id=parse(value(r,key));if(id==null||id<1)throw new IllegalArgumentException("Select a valid class, assignment, exam, or record.");return id;}
    private void preserveAssignmentForm(HttpServletRequest r,String page){if(!"assignments".equals(page))return;Map<String,Object> m=new LinkedHashMap<>();m.put("id",parse(value(r,"id")));m.put("assignment_id",value(r,"assignment_id"));for(String k:new String[]{"title","description","assigned_date","due_date"})m.put(k,value(r,k));r.setAttribute("assignmentForm",m);}
    private Map<String,Object> fallback(User u){Map<String,Object> m=new LinkedHashMap<>();m.put("name",u.getName());m.put("email",u.getEmail());return m;}
}
