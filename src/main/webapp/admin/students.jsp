<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="java.util.List,java.util.Map,com.erp.util.HtmlUtil,com.erp.model.User" %>
<%
    User sessionUser = (User) session.getAttribute("user");
    String adminDisplayName = sessionUser != null && sessionUser.getName() != null ? sessionUser.getName() : "Administrator";
    String initial = adminDisplayName.isEmpty() ? "A" : adminDisplayName.substring(0, 1).toUpperCase();
    String pageTitle = "Students";
    List<Map<String, Object>> students = (List<Map<String, Object>>) request.getAttribute("students");
    String status = (String) request.getAttribute("status");
    String query = (String) request.getAttribute("query");
    String notice = request.getParameter("notice");
    String noticeText = "";
    if ("created".equals(notice)) noticeText = "Student account added successfully.";
    else if ("updated".equals(notice)) noticeText = "Student account updated.";
    else if ("status-updated".equals(notice)) noticeText = "Student account status updated.";
    else if ("deleted".equals(notice)) noticeText = "Student account deleted.";
    else if ("error".equals(notice)) noticeText = "That change could not be completed.";
%>
<!DOCTYPE html><html lang="en"><head>
    <meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Students | EduERP</title><script src="https://cdn.tailwindcss.com"></script>
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.5.2/css/all.min.css">
</head><body class="bg-slate-100 font-sans text-slate-800"><div class="min-h-screen lg:flex">
    <%@ include file="admin-nav.jspf" %>
    <main class="min-w-0 flex-1 lg:ml-60">
        <%@ include file="admin-header.jspf" %>
        <section class="space-y-5 p-4 sm:p-6 lg:p-8">
            <div class="flex flex-col justify-between gap-4 sm:flex-row sm:items-end"><div><h2 class="text-2xl font-bold text-slate-900">Student directory</h2><p class="mt-1 text-sm text-slate-500">Student accounts come from users; enrollment details come from student profiles.</p></div>
                <a href="<%= request.getContextPath() %>/admin/students?action=create" class="inline-flex items-center justify-center gap-2 rounded-md bg-indigo-700 px-4 py-2.5 text-sm font-semibold text-white hover:bg-indigo-800"><i class="fa-solid fa-user-plus"></i> Add student</a></div>
            <% if (!noticeText.isEmpty()) { %><div class="border-l-4 <%= "error".equals(notice) ? "border-rose-500 bg-rose-50 text-rose-800" : "border-emerald-500 bg-emerald-50 text-emerald-800" %> px-4 py-3 text-sm" role="status"><%= HtmlUtil.escape(noticeText) %></div><% } %>
            <% if (request.getAttribute("loadError") != null) { %><div class="border-l-4 border-rose-500 bg-rose-50 px-4 py-3 text-sm text-rose-800" role="alert"><%= HtmlUtil.escape(request.getAttribute("loadError")) %></div><% } %>
            <form action="<%= request.getContextPath() %>/admin/students" method="get" class="flex flex-col gap-2 sm:flex-row">
                <label class="sr-only" for="student-search">Search students</label><div class="flex rounded-md border border-slate-300 bg-white focus-within:border-indigo-600 focus-within:ring-2 focus-within:ring-indigo-100"><span class="flex items-center pl-3 text-slate-400"><i class="fa-solid fa-magnifying-glass"></i></span><input id="student-search" class="min-w-0 px-3 py-2 text-sm outline-none" type="search" name="q" value="<%= HtmlUtil.escape(query) %>" placeholder="Name, email, admission or roll no."></div>
                <label class="sr-only" for="status-filter">Filter by status</label><select id="status-filter" name="status" class="rounded-md border border-slate-300 bg-white px-3 py-2 text-sm"><option value="all" <%= "all".equals(status) ? "selected" : "" %>>All statuses</option><option value="active" <%= "active".equals(status) ? "selected" : "" %>>Active</option><option value="inactive" <%= "inactive".equals(status) ? "selected" : "" %>>Inactive</option></select>
                <button class="rounded-md border border-slate-300 bg-white px-3 py-2 text-sm font-semibold hover:bg-slate-50" type="submit">Filter</button>
            </form>
            <div class="overflow-hidden border border-slate-200 bg-white shadow-sm"><div class="flex items-center justify-between border-b border-slate-200 px-4 py-3 sm:px-5"><h3 class="font-semibold text-slate-900">All students</h3><span class="text-sm text-slate-500"><%= students == null ? 0 : students.size() %> accounts</span></div>
                <div class="overflow-x-auto"><table class="w-full min-w-[950px] text-left text-sm"><thead class="bg-slate-50 text-xs uppercase tracking-wide text-slate-500"><tr><th class="px-5 py-3 font-semibold">Student</th><th class="px-4 py-3 font-semibold">Admission no.</th><th class="px-4 py-3 font-semibold">Roll no.</th><th class="px-4 py-3 font-semibold">Phone</th><th class="px-4 py-3 font-semibold">Status</th><th class="px-5 py-3 text-right font-semibold">Actions</th></tr></thead><tbody class="divide-y divide-slate-100">
                    <% if (students == null || students.isEmpty()) { %><tr><td colspan="6" class="px-5 py-12 text-center text-slate-500"><i class="fa-solid fa-user-graduate mb-3 block text-2xl text-slate-300"></i>No student accounts match this view.</td></tr>
                    <% } else { for (Map<String, Object> student : students) {
                        Object statusValue = student.get("status");
                        boolean active = statusValue instanceof Boolean ? (Boolean) statusValue
                                : statusValue instanceof Number ? ((Number) statusValue).intValue() != 0
                                : "1".equals(String.valueOf(statusValue)) || "true".equalsIgnoreCase(String.valueOf(statusValue));
                    %>
                    <tr class="hover:bg-slate-50/70"><td class="px-5 py-4"><div class="font-semibold text-slate-900"><%= HtmlUtil.escape(student.get("name")) %></div><div class="mt-1 text-xs text-slate-500"><%= HtmlUtil.escape(student.get("email")) %></div></td><td class="px-4 py-4 font-medium text-slate-700"><%= HtmlUtil.escape(student.get("admission_no")) %></td><td class="px-4 py-4 text-slate-600"><%= HtmlUtil.escape(student.get("roll_no")) %></td><td class="px-4 py-4 text-slate-600"><%= HtmlUtil.escape(student.get("phone")) %></td><td class="px-4 py-4"><span class="inline-flex rounded-sm <%= active ? "bg-emerald-50 text-emerald-700" : "bg-amber-50 text-amber-700" %> px-2 py-1 text-xs font-semibold"><%= active ? "Active" : "Inactive" %></span></td>
                        <td class="px-5 py-4"><div class="flex justify-end gap-2"><a class="rounded-md border border-slate-300 px-3 py-1.5 text-xs font-semibold text-slate-700 hover:bg-slate-100" href="<%= request.getContextPath() %>/admin/students?action=edit&amp;id=<%= student.get("id") %>"><i class="fa-solid fa-pen mr-1"></i>Edit</a><form action="<%= request.getContextPath() %>/admin/students" method="post"><input type="hidden" name="action" value="toggle"><input type="hidden" name="id" value="<%= student.get("id") %>"><button class="rounded-md border border-amber-300 px-3 py-1.5 text-xs font-semibold text-amber-800 hover:bg-amber-50" type="submit"><i class="fa-solid fa-power-off mr-1"></i><%= active ? "Deactivate" : "Activate" %></button></form><form action="<%= request.getContextPath() %>/admin/students" method="post" onsubmit="return confirm('Soft delete this student account?');"><input type="hidden" name="action" value="delete"><input type="hidden" name="id" value="<%= student.get("id") %>"><button class="rounded-md border border-rose-300 px-3 py-1.5 text-xs font-semibold text-rose-700 hover:bg-rose-50" type="submit"><i class="fa-solid fa-trash mr-1"></i>Delete</button></form></div></td>
                    </tr><% } } %>
                </tbody></table></div>
            </div>
        <%int currentPage=request.getAttribute("page") instanceof Number?((Number)request.getAttribute("page")).intValue():1;int pageCount=request.getAttribute("pageCount") instanceof Number?((Number)request.getAttribute("pageCount")).intValue():1;long totalCount=request.getAttribute("totalCount") instanceof Number?((Number)request.getAttribute("totalCount")).longValue():0;String paginationQuery=String.valueOf(request.getAttribute("query")),paginationStatus=String.valueOf(request.getAttribute("status"));%>
<nav class="flex flex-wrap items-center justify-between gap-3 rounded-xl bg-white px-4 py-3 text-sm shadow-sm"><span class="text-slate-500"><%=totalCount%> records · page <%=currentPage%> of <%=pageCount%></span><div class="flex gap-2"><%if(currentPage>1){%><a class="rounded border px-3 py-1.5" href="?q=<%=HtmlUtil.escape(paginationQuery)%>&amp;status=<%=HtmlUtil.escape(paginationStatus)%>&amp;page=<%=currentPage-1%>">Previous</a><%}%><%if(currentPage<pageCount){%><a class="rounded border px-3 py-1.5" href="?q=<%=HtmlUtil.escape(paginationQuery)%>&amp;status=<%=HtmlUtil.escape(paginationStatus)%>&amp;page=<%=currentPage+1%>">Next</a><%}%></div></nav></section>
    </main>
</div></body></html>
