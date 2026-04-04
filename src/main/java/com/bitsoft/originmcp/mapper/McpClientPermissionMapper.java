package com.bitsoft.originmcp.mapper;

import com.bitsoft.originmcp.model.database.McpClientPermission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * MyBatis mapper for mcp_client_permissions table.
 */
@Mapper
public interface McpClientPermissionMapper {

    @Select("SELECT * FROM mcp_client_permissions WHERE client_id = #{clientId}")
    List<McpClientPermission> findByClientId(@Param("clientId") String clientId);

    @Select("SELECT tool_name FROM mcp_client_permissions WHERE client_id = #{clientId}")
    List<String> findToolNamesByClientId(@Param("clientId") String clientId);

    @Select("SELECT COUNT(*) > 0 FROM mcp_client_permissions WHERE client_id = #{clientId} AND tool_name = #{toolName}")
    boolean hasPermission(@Param("clientId") String clientId, @Param("toolName") String toolName);
}
