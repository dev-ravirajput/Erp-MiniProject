<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="java.util.Map,java.util.List,java.util.Set,java.util.HashSet,java.util.Collection,com.erp.util.HtmlUtil,com.erp.model.User" %>
<%
    User sessionUser = (User) session.getAttribute("user");
    String adminDisplayName = sessionUser != null && sessionUser.getName() != null ? sessionUser.getName() : "Administrator";
    String initial = adminDisplayName.isEmpty() ? "A" : adminDisplayName.substring(0, 1).toUpperCase();
    String mode = (String) request.getAttribute("mode");
    boolean editing = "update".equals(mode);
    String pageTitle = editing ? "Edit teacher" : "Add teacher";
    Map<String, ?> teacher = (Map<String, ?>) request.getAttribute("teacher");
    List<Map<String, Object>> classOptions = (List<Map<String, Object>>) request.getAttribute("classOptions");
    List<Map<String, Object>> sectionOptions = (List<Map<String, Object>>) request.getAttribute("sectionOptions");
    List<Map<String, Object>> subjectOptions = (List<Map<String, Object>>) request.getAttribute("subjectOptions");
    Set<String> selectedSubjects = new HashSet<>();
    Object subjectValue = teacher.get("subject_ids");
    if (subjectValue instanceof Collection) { for (Object value : (Collection<?>) subjectValue) selectedSubjects.add(String.valueOf(value)); }
    else if (subjectValue != null && !String.valueOf(subjectValue).isEmpty()) { for (String value : String.valueOf(subjectValue).split(",")) selectedSubjects.add(value); }
%>
<!DOCTYPE html><html lang="en"><head><meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1.0"><title><%= editing ? "Edit teacher" : "Add teacher" %> | EduERP</title><script src="https://cdn.tailwindcss.com"></script><link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.5.2/css/all.min.css"></head>
<body class="bg-slate-100 font-sans text-slate-800"><div class="min-h-screen lg:flex"><%@ include file="admin-nav.jspf" %><main class="min-w-0 flex-1 lg:ml-60">
    <%@ include file="admin-header.jspf" %>
    <section class="p-4 sm:p-6 lg:p-8"><div class="mb-5"><h2 class="text-2xl font-bold text-slate-900">Teacher details</h2><p class="mt-1 text-sm text-slate-500">Account information is stored in users; employment details are stored in teachers.</p></div>
        <% if (request.getAttribute("formError") != null) { %><div class="mb-5 border-l-4 border-rose-500 bg-rose-50 px-4 py-3 text-sm text-rose-800" role="alert"><%= HtmlUtil.escape(request.getAttribute("formError")) %></div><% } %>
        <form action="<%= request.getContextPath() %>/admin/teachers" method="post" class="max-w-4xl border border-slate-200 bg-white shadow-sm"><input type="hidden" name="action" value="<%= editing ? "update" : "create" %>"><% if (editing) { %><input type="hidden" name="id" value="<%= HtmlUtil.escape(teacher.get("id")) %>"><% } %>
            <div class="grid grid-cols-1 gap-5 p-5 sm:grid-cols-2 sm:p-7">
                <div><label for="name" class="mb-1.5 block text-sm font-semibold text-slate-700">Full name *</label><input id="name" name="name" required maxlength="100" autocomplete="name" value="<%= HtmlUtil.escape(teacher.get("name")) %>" class="w-full rounded-md border border-slate-300 px-3 py-2.5 text-sm"></div>
                <div><label for="email" class="mb-1.5 block text-sm font-semibold text-slate-700">Email *</label><input id="email" name="email" type="email" required maxlength="150" autocomplete="email" value="<%= HtmlUtil.escape(teacher.get("email")) %>" class="w-full rounded-md border border-slate-300 px-3 py-2.5 text-sm"></div>
                <div><label for="employee_code" class="mb-1.5 block text-sm font-semibold text-slate-700">Employee code *</label><input id="employee_code" name="employee_code" required maxlength="50" value="<%= HtmlUtil.escape(teacher.get("employee_code")) %>" class="w-full rounded-md border border-slate-300 px-3 py-2.5 text-sm"></div>
                <div><label for="department" class="mb-1.5 block text-sm font-semibold text-slate-700">Department</label><input id="department" name="department" maxlength="100" value="<%= HtmlUtil.escape(teacher.get("department")) %>" class="w-full rounded-md border border-slate-300 px-3 py-2.5 text-sm"></div>
                <div><label for="qualification" class="mb-1.5 block text-sm font-semibold text-slate-700">Qualification</label><input id="qualification" name="qualification" maxlength="150" value="<%= HtmlUtil.escape(teacher.get("qualification")) %>" class="w-full rounded-md border border-slate-300 px-3 py-2.5 text-sm"></div>
                <div><label for="phone" class="mb-1.5 block text-sm font-semibold text-slate-700">Phone</label><input id="phone" name="phone" type="tel" maxlength="20" autocomplete="tel" value="<%= HtmlUtil.escape(teacher.get("phone")) %>" class="w-full rounded-md border border-slate-300 px-3 py-2.5 text-sm"></div>
                <div><label for="joining_date" class="mb-1.5 block text-sm font-semibold text-slate-700">Joining date</label><input id="joining_date" name="joining_date" type="date" value="<%= HtmlUtil.escape(teacher.get("joining_date")) %>" class="w-full rounded-md border border-slate-300 px-3 py-2.5 text-sm"></div>
                <div><label for="class_id" class="mb-1.5 block text-sm font-semibold text-slate-700">Class for subject assignments</label><select id="class_id" name="class_id" class="w-full rounded-md border border-slate-300 bg-white px-3 py-2.5 text-sm"><option value="">No class assignment</option><% if (classOptions != null) for (Map<String, Object> option : classOptions) { String optionId = String.valueOf(option.get("id")); %><option value="<%= optionId %>" data-year="<%= HtmlUtil.escape(option.get("academic_year")) %>" <%= optionId.equals(String.valueOf(teacher.get("class_id"))) ? "selected" : "" %>><%= HtmlUtil.escape(option.get("class_name")) %> — <%= HtmlUtil.escape(option.get("academic_year")) %></option><% } %></select></div>
                <div><label for="section_id" class="mb-1.5 block text-sm font-semibold text-slate-700">Section</label><select id="section_id" name="section_id" class="w-full rounded-md border border-slate-300 bg-white px-3 py-2.5 text-sm"><option value="">Select section</option><% if (sectionOptions != null) for (Map<String, Object> option : sectionOptions) { String optionId = String.valueOf(option.get("id")); %><option value="<%= optionId %>" data-class-id="<%= option.get("class_id") %>" <%= optionId.equals(String.valueOf(teacher.get("section_id"))) ? "selected" : "" %>><%= HtmlUtil.escape(option.get("section_name")) %></option><% } %></select></div>
                <div><label for="academic_year" class="mb-1.5 block text-sm font-semibold text-slate-700">Assignment academic year</label><input id="academic_year" name="academic_year" maxlength="20" placeholder="2026-2027" value="<%= HtmlUtil.escape(teacher.get("academic_year")) %>" class="w-full rounded-md border border-slate-300 px-3 py-2.5 text-sm"></div>
                <div class="sm:col-span-2"><div class="mb-1.5 flex flex-wrap items-center justify-between gap-2"><label for="subject_ids" class="block text-sm font-semibold text-slate-700">Subjects taught</label><div class="flex gap-2"><button type="button" id="select-all-subjects" class="text-xs font-semibold text-indigo-700 hover:underline">Select all</button><button type="button" id="deselect-all-subjects" class="text-xs font-semibold text-slate-600 hover:underline">Deselect all</button></div></div><select id="subject_ids" name="subject_ids" multiple size="6" class="w-full rounded-md border border-slate-300 bg-white px-3 py-2 text-sm"><% if (subjectOptions != null) for (Map<String, Object> option : subjectOptions) { String optionId = String.valueOf(option.get("id")); %><option value="<%= optionId %>" <%= selectedSubjects.contains(optionId) ? "selected" : "" %>><%= HtmlUtil.escape(option.get("subject_code")) %> — <%= HtmlUtil.escape(option.get("subject_name")) %></option><% } %></select><p class="mt-1 text-xs text-slate-500">Select a class, section, and academic year to assign one or more subjects. Existing assignments for other sections and years are retained.</p></div>
                <% if (!editing) { %><div><label for="password" class="mb-1.5 block text-sm font-semibold text-slate-700">Initial password *</label><input id="password" name="password" type="password" required minlength="8" autocomplete="new-password" class="w-full rounded-md border border-slate-300 px-3 py-2.5 text-sm"><p class="mt-1 text-xs text-slate-500">Must be at least 8 characters.</p></div><% } %>
            </div><div class="flex flex-col-reverse justify-end gap-3 border-t border-slate-200 bg-slate-50 px-5 py-4 sm:flex-row sm:px-7"><a href="<%= request.getContextPath() %>/admin/teachers" class="rounded-md border border-slate-300 bg-white px-4 py-2.5 text-center text-sm font-semibold text-slate-700 hover:bg-slate-100">Cancel</a><button type="submit" class="rounded-md bg-indigo-700 px-5 py-2.5 text-sm font-semibold text-white hover:bg-indigo-800"><i class="fa-solid fa-check mr-1"></i><%= editing ? "Save changes" : "Create teacher" %></button></div>
        </form>
    </section>
</main></div><script>
const classSelect = document.getElementById('class_id');
const sectionSelect = document.getElementById('section_id');
const yearInput = document.getElementById('academic_year');
function filterSections() {
    const classId = classSelect.value;
    for (const option of sectionSelect.options) {
        if (!option.value) { option.hidden = false; continue; }
        const visible = option.dataset.classId === classId;
        option.hidden = !visible; option.disabled = !visible;
        if (!visible && option.selected) sectionSelect.value = '';
    }
    const selectedClass = classSelect.options[classSelect.selectedIndex];
    if (selectedClass && selectedClass.dataset.year) yearInput.value = selectedClass.dataset.year;
}
classSelect.addEventListener('change', filterSections);
document.getElementById('select-all-subjects').addEventListener('click', () => { for (const option of document.getElementById('subject_ids').options) option.selected = true; });
document.getElementById('deselect-all-subjects').addEventListener('click', () => { for (const option of document.getElementById('subject_ids').options) option.selected = false; });
filterSections();
</script></body></html>
