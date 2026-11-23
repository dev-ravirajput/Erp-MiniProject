package com.erp.controller;

import com.erp.dao.AdminReportDAO;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class AdminReportsServlet extends HttpServlet {
    private final AdminReportDAO dao=new AdminReportDAO();
    @Override protected void doGet(HttpServletRequest req,HttpServletResponse resp)throws ServletException,IOException{
        req.setCharacterEncoding("UTF-8");req.setAttribute("activeAdminSection","reports");
        String section=value(req,"section_id"),from=value(req,"from"),to=value(req,"to"),year=value(req,"academic_year");
        try{
            if(!from.isEmpty())LocalDate.parse(from);if(!to.isEmpty())LocalDate.parse(to);
            if(!from.isEmpty()&&!to.isEmpty()&&LocalDate.parse(from).isAfter(LocalDate.parse(to)))throw new IllegalArgumentException("Start date must not be after end date.");
            if(year.length()>20)throw new IllegalArgumentException("Choose a valid academic year.");
            List<Map<String,Object>> rows=dao.getAttendanceReport(section,from,to,year);
            if("csv".equals(req.getParameter("format"))){writeCsv(resp,rows);return;}
            req.setAttribute("attendanceReport",rows);
        }catch(Exception e){getServletContext().log("Could not load attendance report",e);req.setAttribute("loadError",e instanceof IllegalArgumentException?e.getMessage():"Report data could not be loaded. Check the database setup.");req.setAttribute("attendanceReport",Collections.emptyList());}
        req.setAttribute("sectionFilter",section);req.setAttribute("fromDate",from);req.setAttribute("toDate",to);req.setAttribute("yearFilter",year);
        try{req.setAttribute("sectionOptions",dao.getSections());req.setAttribute("academicYears",dao.getAcademicYears());}catch(Exception e){getServletContext().log("Could not load report filters",e);req.setAttribute("loadError","Class, section, or academic year options could not be loaded.");}
        req.getRequestDispatcher("/admin/reports.jsp").forward(req,resp);
    }
    private void writeCsv(HttpServletResponse resp,List<Map<String,Object>> rows)throws IOException{
        resp.setContentType("text/csv;charset=UTF-8");resp.setHeader("Content-Disposition","attachment; filename=attendance-report.csv");
        PrintWriter out=resp.getWriter();out.write('\ufeff');out.println("Academic year,Student,Admission number,Roll number,Sessions,Present,Absent,Late,Leave");
        for(Map<String,Object> row:rows){String[] columns={"academic_year","name","admission_no","roll_no","total","present","absent","late","leaves"};for(int i=0;i<columns.length;i++){if(i>0)out.print(',');out.print(csv(row.get(columns[i])));}out.println();}
        out.flush();
    }
    private String csv(Object value){if(value==null)return "";String text=value.toString();int start=0;while(start<text.length()&&Character.isWhitespace(text.charAt(start)))start++;if(start<text.length()&&"=+-@".indexOf(text.charAt(start))>=0)text="'"+text;return "\""+text.replace("\"","\"\"")+"\"";}
    private String value(HttpServletRequest req,String key){String v=req.getParameter(key);return v==null?"":v.trim();}
}
