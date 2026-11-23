package com.erp.controller;

import com.erp.dao.AppSettingsDAO;
import com.erp.util.BrandingStorage;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;
import jakarta.servlet.annotation.MultipartConfig;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import javax.imageio.ImageIO;
import java.net.URI;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

@MultipartConfig(maxFileSize=5*1024*1024, maxRequestSize=11*1024*1024)
public class AdminSettingsServlet extends HttpServlet {
    private final AppSettingsDAO dao=new AppSettingsDAO();
    @Override protected void doGet(HttpServletRequest req,HttpServletResponse resp)throws ServletException,IOException{
        req.setAttribute("activeAdminSection","settings");try{req.setAttribute("settings",dao.getSettings());}
        catch(SQLException e){getServletContext().log("Could not load admin settings",e);req.setAttribute("loadError","Settings table is unavailable. Run database/005_app_settings.sql.");req.setAttribute("settings",defaults());}
        req.getRequestDispatcher("/admin/settings.jsp").forward(req,resp);
    }
    @Override protected void doPost(HttpServletRequest req,HttpServletResponse resp)throws ServletException,IOException{
        req.setCharacterEncoding("UTF-8");req.setAttribute("activeAdminSection","settings");Map<String,String> values=new LinkedHashMap<>();
        Map<String,String> previous=defaults();try{previous.putAll(dao.getSettings());}catch(SQLException ignored){}
        for(String key:new String[]{"site_name","logo_url","favicon_url"}){String value=req.getParameter(key);values.put(key,"site_name".equals(key)?(value==null?"":value.trim()):previous.getOrDefault(key,""));}
        String error=null;if(values.get("site_name").isEmpty()||values.get("site_name").length()>100)error="Website name is required and must be 100 characters or fewer.";
        try {
            for(String field:new String[]{"logo_file","favicon_file"}) { Part part=req.getPart(field); if(part!=null&&part.getSize()>0) { String key="logo_file".equals(field)?"logo_url":"favicon_url"; values.put(key,saveImage(part)); } }
        } catch(Exception e) { error="Upload a valid PNG, JPEG, or GIF image no larger than 5 MB."; }
        if(error==null&&(values.get("logo_url").length()>500||values.get("favicon_url").length()>500))error="Logo and favicon paths must be 500 characters or fewer.";
        if(error==null&&(!validAsset(values.get("logo_url"))||!validAsset(values.get("favicon_url"))))error="Logo and favicon must use an HTTPS/HTTP URL or a local path beginning with one slash.";
        if(error!=null){req.setAttribute("settings",values);req.setAttribute("formError",error);req.getRequestDispatcher("/admin/settings.jsp").forward(req,resp);return;}
        try{dao.saveSettings(values);resp.sendRedirect(req.getContextPath()+"/admin/settings?notice=updated");}
        catch(SQLException e){getServletContext().log("Could not save website settings",e);req.setAttribute("settings",values);req.setAttribute("formError","Settings could not be saved. Run migration 005 and check the database connection.");req.getRequestDispatcher("/admin/settings.jsp").forward(req,resp);}
    }
    private String saveImage(Part part)throws Exception{
        String type=part.getContentType();String ext="image/png".equalsIgnoreCase(type)?".png":"image/jpeg".equalsIgnoreCase(type)?".jpg":"image/gif".equalsIgnoreCase(type)?".gif":null;
        if(ext==null||part.getSize()>5L*1024*1024)throw new IOException("Unsupported image");
        Path dir=BrandingStorage.directory();Files.createDirectories(dir);
        Path target=dir.resolve(UUID.randomUUID()+ext);
        try(java.io.InputStream in=part.getInputStream()){if(ImageIO.read(in)==null)throw new IOException("Invalid image");}
        try(java.io.InputStream in=part.getInputStream()){Files.copy(in,target);}
        return "/uploads/branding/"+target.getFileName();
    }
    private boolean validAsset(String value){if(value.isEmpty())return true;if(value.startsWith("/")&&!value.startsWith("//"))return !value.contains("\\")&&!value.contains("\r")&&!value.contains("\n");try{URI uri=URI.create(value);return ("https".equalsIgnoreCase(uri.getScheme())||"http".equalsIgnoreCase(uri.getScheme()))&&uri.getHost()!=null;}catch(IllegalArgumentException e){return false;}}
    private Map<String,String> defaults(){Map<String,String> m=new LinkedHashMap<>();m.put("site_name","EduERP");m.put("logo_url","");m.put("favicon_url","");return m;}
}
