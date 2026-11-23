package com.erp.dao;

import com.erp.util.DBConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Stores a compact, non-sensitive audit record for successful write requests. */
public class AuditDAO {
    public void record(Long actorId, String action, String entityType, Long entityId, String details) throws SQLException {
        String sql = "INSERT INTO audit_log(actor_user_id,action,entity_type,entity_id,details) VALUES(?,?,?,?,?)";
        try (Connection con = DBConnection.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            if (actorId == null) ps.setNull(1, java.sql.Types.BIGINT); else ps.setLong(1, actorId);
            ps.setString(2, action);
            ps.setString(3, entityType);
            if (entityId == null) ps.setNull(4, java.sql.Types.BIGINT); else ps.setLong(4, entityId);
            ps.setString(5, details);
            ps.executeUpdate();
        }
    }
    public List<Map<String,Object>> recent()throws SQLException{
        List<Map<String,Object>> rows=new ArrayList<>();String sql="SELECT a.id,a.action,a.entity_type,a.entity_id,a.details,a.created_at,u.name actor_name,u.email actor_email FROM audit_log a LEFT JOIN users u ON u.id=a.actor_user_id ORDER BY a.created_at DESC,a.id DESC LIMIT 250";
        try(Connection c=DBConnection.getConnection();PreparedStatement p=c.prepareStatement(sql);ResultSet r=p.executeQuery()){while(r.next()){Map<String,Object> row=new LinkedHashMap<>();for(String key:new String[]{"id","action","entity_type","entity_id","details","created_at","actor_name","actor_email"})row.put(key,r.getObject(key));rows.add(row);}}return rows;
    }
}
