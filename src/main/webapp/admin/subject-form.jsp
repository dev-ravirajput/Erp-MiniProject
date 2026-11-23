<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="java.util.Map,com.erp.util.HtmlUtil,com.erp.model.User" %>
<%
    User sessionUser = (User) session.getAttribute("user");
    String adminDisplayName = sessionUser != null && sessionUser.getName() != null ? sessionUser.getName() : "Administrator";
    String initial = adminDisplayName.isEmpty() ? "A" : adminDisplayName.substring(0, 1).toUpperCase();
    String mode = (String) request.getAttribute("mode");
    boolean editing = "update".equals(mode);
    String pageTitle = (String) request.getAttribute("pageTitle");
    Map<String, ?> subject = (Map<String, ?>) request.getAttribute("subject");
%>
<!DOCTYPE html><html lang="en"><head><meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1.0"><title><%= HtmlUtil.escape(pageTitle) %> | EduERP</title><script src="https://cdn.tailwindcss.com"></script><link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.5.2/css/all.min.css"></head>
<body class="bg-slate-100 font-sans text-slate-800"><div class="min-h-screen lg:flex"><%@ include file="admin-nav.jspf" %><main class="min-w-0 flex-1 lg:ml-60"><%@ include file="admin-header.jspf" %>
    <section class="p-4 sm:p-6 lg:p-8"><div class="mb-5"><h2 class="text-2xl font-bold text-slate-900"><%= HtmlUtil.escape(pageTitle) %></h2><p class="mt-1 text-sm text-slate-500">Set the subject code and the maximum marks available for it.</p></div>
        <% if (request.getAttribute("formError") != null) { %><div class="mb-5 border-l-4 border-rose-500 bg-rose-50 px-4 py-3 text-sm text-rose-800" role="alert"><%= HtmlUtil.escape(request.getAttribute("formError")) %></div><% } %>
        <form action="<%= request.getContextPath() %>/admin/subjects" method="post" class="max-w-2xl border border-slate-200 bg-white shadow-sm"><input type="hidden" name="action" value="<%= editing ? "update" : "create" %>"><% if (editing) { %><input type="hidden" name="id" value="<%= HtmlUtil.escape(subject.get("id")) %>"><% } %>
            <div class="grid gap-5 p-5 sm:grid-cols-2 sm:p-7"><div><label for="subject_code" class="mb-1.5 block text-sm font-semibold text-slate-700">Subject code *</label><input id="subject_code" name="subject_code" required maxlength="30" value="<%= HtmlUtil.escape(subject.get("subject_code")) %>" placeholder="e.g. MATH101" class="w-full rounded-md border border-slate-300 px-3 py-2.5 text-sm"></div><div><label for="subject_name" class="mb-1.5 block text-sm font-semibold text-slate-700">Subject name *</label><input id="subject_name" name="subject_name" required maxlength="100" value="<%= HtmlUtil.escape(subject.get("subject_name")) %>" placeholder="e.g. Mathematics" class="w-full rounded-md border border-slate-300 px-3 py-2.5 text-sm"></div><div><label for="max_marks" class="mb-1.5 block text-sm font-semibold text-slate-700">Maximum marks *</label><input id="max_marks" name="max_marks" type="number" min="1" max="2147483647" required value="<%= subject.get("max_marks") == null ? "100" : HtmlUtil.escape(subject.get("max_marks")) %>" class="w-full rounded-md border border-slate-300 px-3 py-2.5 text-sm"></div></div>
            <div class="flex justify-end gap-3 border-t border-slate-200 bg-slate-50 px-5 py-4 sm:px-7"><a href="<%= request.getContextPath() %>/admin/subjects" class="rounded-md border border-slate-300 bg-white px-4 py-2.5 text-sm font-semibold hover:bg-slate-100">Cancel</a><button type="submit" class="rounded-md bg-indigo-700 px-5 py-2.5 text-sm font-semibold text-white hover:bg-indigo-800"><i class="fa-solid fa-check mr-1"></i><%= editing ? "Save changes" : "Create subject" %></button></div>
        </form>
    </section>
</main></div></body></html>
