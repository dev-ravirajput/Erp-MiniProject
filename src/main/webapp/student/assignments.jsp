<%@ page contentType="text/html;charset=UTF-8" import="java.util.List,java.util.Map,com.erp.util.HtmlUtil" %>
<%
if(request.getAttribute("studentProfile")==null){response.sendRedirect(request.getContextPath()+"/student/assignments");return;}
request.setAttribute("studentPage","assignments");
List<Map<String,Object>> rows=(List<Map<String,Object>>)request.getAttribute("assignments");
%>
<!DOCTYPE html><html lang="en"><head><meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>Assignments</title><script src="https://cdn.tailwindcss.com"></script><link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.5.2/css/all.min.css"></head>
<body class="bg-slate-100 text-slate-800"><%@include file="student-nav.jspf"%><section class="space-y-5 p-4 sm:p-8">
<div><h2 class="text-2xl font-bold">Assignments</h2><p class="mt-1 text-sm text-slate-500">Review instructions, submit work, and read teacher feedback.</p></div>
<%if(request.getAttribute("formError")!=null){%><p class="rounded-lg bg-rose-50 p-4 text-rose-700"><%=HtmlUtil.escape(request.getAttribute("formError"))%></p><%}%>
<%if("submitted".equals(request.getParameter("notice"))){%><p class="rounded-lg bg-emerald-50 p-4 text-emerald-700">Your work was submitted.</p><%}%>
<%if(request.getAttribute("loadError")!=null){%><p class="rounded-lg bg-rose-50 p-4 text-rose-700"><%=HtmlUtil.escape(request.getAttribute("loadError"))%></p><%}%>
<div class="grid gap-4 lg:grid-cols-2">
<%if(rows!=null)for(Map<String,Object> row:rows){Object submissionDate=row.get("submitted_at"),grade=row.get("grade"),stored=row.get("stored_filename");%>
<article class="space-y-4 rounded-xl border border-slate-200 bg-white p-5 shadow-sm">
<div class="flex items-start justify-between gap-3"><span class="rounded-full bg-indigo-50 px-2.5 py-1 text-xs font-semibold text-indigo-700"><%=HtmlUtil.escape(row.get("subject_name"))%></span><span class="text-right text-xs text-slate-500">Due<br><strong class="text-slate-700"><%=HtmlUtil.escape(row.get("due_date"))%></strong></span></div>
<div><h3 class="text-lg font-bold"><%=HtmlUtil.escape(row.get("title"))%></h3><p class="mt-2 whitespace-pre-line text-sm text-slate-600"><%=HtmlUtil.escape(row.get("description")==null?"No instructions provided.":row.get("description"))%></p><p class="mt-3 text-xs text-slate-500">Assigned <%=HtmlUtil.escape(row.get("assigned_date"))%> · <%=HtmlUtil.escape(row.get("teacher_name"))%></p></div>
<div class="rounded-lg bg-slate-50 p-4"><div class="flex flex-wrap items-center justify-between gap-2"><h4 class="font-semibold">Your submission</h4><%if(submissionDate!=null){%><span class="text-xs <%=row.get("submitted_late") instanceof Number&&((Number)row.get("submitted_late")).intValue()==1?"text-rose-700":"text-emerald-700"%>">Submitted <%=HtmlUtil.escape(submissionDate)%><%=row.get("submitted_late") instanceof Number&&((Number)row.get("submitted_late")).intValue()==1?" · Late":""%></span><%}else{%><span class="text-xs text-amber-700">Not submitted</span><%}%></div>
<%if(grade!=null){%><p class="mt-2 text-sm font-semibold">Grade: <%=HtmlUtil.escape(grade)%> / <%=HtmlUtil.escape(row.get("max_grade"))%></p><%}%>
<%if(row.get("feedback")!=null){%><p class="mt-1 whitespace-pre-line text-sm text-slate-600"><%=HtmlUtil.escape(row.get("feedback"))%></p><%}%>
<%if(stored!=null){%><a class="mt-2 inline-block text-sm font-semibold text-indigo-700" href="<%=request.getContextPath()%>/assignment-files/<%=HtmlUtil.escape(stored)%>">Download your uploaded file</a><%}%>
<form method="post" enctype="multipart/form-data" class="mt-3 space-y-3 border-t border-slate-200 pt-3"><input type="hidden" name="assignment_id" value="<%=row.get("id")%>">
<label class="block text-xs font-semibold text-slate-600">Written response<textarea name="submission_text" maxlength="10000" rows="3" class="mt-1 block w-full rounded-md border border-slate-300 px-3 py-2"><%=row.get("submission_text")==null?"":HtmlUtil.escape(row.get("submission_text"))%></textarea></label>
<label class="block text-xs font-semibold text-slate-600">Attach a file (PDF, TXT, PNG, JPEG; up to 10 MB)<input type="file" name="submission_file" accept=".pdf,.txt,.png,.jpg,.jpeg" class="mt-1 block w-full text-sm"></label>
<button class="rounded-md bg-indigo-700 px-4 py-2 text-sm font-semibold text-white">Submit or update work</button></form></div></article>
<%}%><%if(rows==null||rows.isEmpty()){%><div class="rounded-xl bg-white p-8 text-center text-slate-500 lg:col-span-2">There are no assignments for your class right now.</div><%}%>
</div></section></main></div></body></html>
