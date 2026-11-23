<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="java.util.Map,java.util.LinkedHashMap,com.erp.util.HtmlUtil,com.erp.model.User" %>
<%
    Map<String, String> sections = new LinkedHashMap<>();
    sections.put("staff", "Staff Members");
    sections.put("attendance", "Attendance");
    sections.put("examinations", "Examinations");
    sections.put("timetable", "Timetable");
    sections.put("reports", "Reports");
    sections.put("settings", "Settings");
    String section = request.getParameter("section");
    String pageTitle = sections.get(section);
    if (pageTitle == null) { response.sendError(404); return; }
    request.setAttribute("activeAdminSection", section);
    User sessionUser = (User) session.getAttribute("user");
    String adminDisplayName = sessionUser != null && sessionUser.getName() != null ? sessionUser.getName() : "Administrator";
    String initial = adminDisplayName.isEmpty() ? "A" : adminDisplayName.substring(0, 1).toUpperCase();
%>
<!DOCTYPE html><html lang="en"><head><meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1.0"><title><%= HtmlUtil.escape(pageTitle) %> | EduERP</title><script src="https://cdn.tailwindcss.com"></script><link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.5.2/css/all.min.css"></head>
<body class="bg-slate-100 font-sans text-slate-800"><div class="min-h-screen lg:flex"><%@ include file="admin-nav.jspf" %><main class="min-w-0 flex-1 lg:ml-60"><%@ include file="admin-header.jspf" %>
    <section class="p-4 sm:p-6 lg:p-8"><div class="border border-slate-200 bg-white p-6 shadow-sm"><h2 class="text-xl font-bold text-slate-900"><%= HtmlUtil.escape(pageTitle) %></h2><p class="mt-2 text-sm text-slate-500">This section is not available yet.</p></div></section>
</main></div></body></html>
