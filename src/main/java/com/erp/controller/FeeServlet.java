package com.erp.controller;

import com.erp.dao.ClassDAO;
import com.erp.dao.FeeDAO;
import com.erp.model.User;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public class FeeServlet extends HttpServlet {
    private final FeeDAO dao=new FeeDAO();private final ClassDAO classDAO=new ClassDAO();
    @Override protected void doGet(HttpServletRequest req,HttpServletResponse resp)throws ServletException,IOException{
        req.setAttribute("activeAdminSection","fees");
        try{req.setAttribute("invoices",dao.invoices());req.setAttribute("feePlans",dao.plans());req.setAttribute("studentOptions",dao.invoiceStudentOptions());req.setAttribute("classOptions",classDAO.getActiveClassesForSelect());String raw=req.getParameter("invoice_id");if(raw!=null){long id=Long.parseLong(raw);req.setAttribute("paymentInvoice",dao.invoice(id));req.setAttribute("payments",dao.payments(id));}String receiptRaw=req.getParameter("receipt_id");if(receiptRaw!=null)req.setAttribute("receipt",dao.paymentReceipt(Long.parseLong(receiptRaw)));}
        catch(Exception e){getServletContext().log("Could not load fee ledger",e);req.setAttribute("loadError","Fee information could not be loaded. Apply database/008_academic_workflows.sql and database/009_academic_year_collation.sql.");}
        req.getRequestDispatcher("/admin/fees.jsp").forward(req,resp);
    }
    @Override protected void doPost(HttpServletRequest req,HttpServletResponse resp)throws ServletException,IOException{
        req.setCharacterEncoding("UTF-8");User user=(User)req.getSession(false).getAttribute("user");String action=req.getParameter("action");
        try{
            if("create_plan".equals(action)){long classId=positive(req.getParameter("class_id"));String title=value(req,"title"),due=value(req,"due_date");BigDecimal amount=money(value(req,"amount"));LocalDate.parse(due);if(title.isEmpty()||title.length()>150)throw new IllegalArgumentException("Enter a fee plan title up to 150 characters.");dao.createPlan(classId,title,amount,due,user.getId());resp.sendRedirect(req.getContextPath()+"/admin/fees?notice=plan_created");return;}
            if("create_invoice".equals(action)){long student=positive(req.getParameter("student_id")),plan=positive(req.getParameter("plan_id"));BigDecimal concession=nonnegativeMoney(value(req,"concession_amount"));dao.createInvoiceFromPlan(student,plan,concession,user.getId());resp.sendRedirect(req.getContextPath()+"/admin/fees?notice=invoice_created");return;}
            if("record_payment".equals(action)){long invoice=positive(req.getParameter("invoice_id"));BigDecimal amount=money(value(req,"amount"));String method=value(req,"payment_method"),reference=value(req,"reference_no"),notes=value(req,"notes");if(!List.of("CASH","BANK_TRANSFER","CARD","ONLINE","OTHER").contains(method))throw new IllegalArgumentException("Select a valid payment method.");if(reference.length()>100||notes.length()>500)throw new IllegalArgumentException("Payment reference or notes are too long.");dao.recordPayment(invoice,amount,method,reference,notes,user.getId());resp.sendRedirect(req.getContextPath()+"/admin/fees?invoice_id="+invoice+"&notice=payment_recorded");return;}
            if("record_refund".equals(action)){long payment=positive(req.getParameter("payment_id")),invoice=positive(req.getParameter("invoice_id"));BigDecimal amount=money(value(req,"amount"));String reference=value(req,"reference_no"),notes=value(req,"notes");if(reference.length()>100||notes.length()>500)throw new IllegalArgumentException("Refund reference or notes are too long.");dao.refund(invoice,payment,amount,reference,notes,user.getId());resp.sendRedirect(req.getContextPath()+"/admin/fees?invoice_id="+invoice+"&notice=refund_recorded");return;}
            resp.sendError(400);
        }catch(IllegalArgumentException|java.time.DateTimeException e){req.setAttribute("formError",e.getMessage());doGet(req,resp);}catch(SQLException e){getServletContext().log("Fee operation failed",e);req.setAttribute("formError",e.getMessage());doGet(req,resp);}
    }
    private BigDecimal money(String raw){BigDecimal value=new BigDecimal(raw);if(value.signum()<=0||value.scale()>2)throw new IllegalArgumentException("Enter a positive amount with at most two decimals.");return value;}
    private BigDecimal nonnegativeMoney(String raw){BigDecimal value=raw.isBlank()?BigDecimal.ZERO:new BigDecimal(raw);if(value.signum()<0||value.scale()>2)throw new IllegalArgumentException("Enter a concession of zero or more with at most two decimals.");return value;}
    private String value(HttpServletRequest req,String key){String v=req.getParameter(key);return v==null?"":v.trim();}
    private long positive(String raw){try{long value=Long.parseLong(raw);if(value>0)return value;}catch(Exception ignored){}throw new IllegalArgumentException("Select a valid student, class, plan, invoice, or payment.");}
}
