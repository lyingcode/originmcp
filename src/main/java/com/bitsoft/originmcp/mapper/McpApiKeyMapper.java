package com.bitsoft.originmcp.mapper;

import com.bitsoft.originmcp.model.database.McpApiKey;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Optional;

/**
 * MyBatis mapper for mcp_api_keys table.
 */
@Mapper
public interface McpApiKeyMapper {

    @Select("SELECT * FROM mcp_api_keys WHERE enabled = TRUE AND (expires_at IS NULL OR expires_at > NOW())")
    List<McpApiKey> findAllEnabled();

    @Select("SELECT * FROM mcp_api_keys WHERE client_id = #{clientId} AND enabled = TRUE")
    Optional<McpApiKey> findByClientId(@Param("clientId") String clientId);

    @Select("SELECT * FROM mcp_api_keys WHERE api_key_hash = #{apiKeyHash} AND enabled = TRUE")
    Optional<McpApiKey> findByApiKeyHash(@Param("apiKeyHash") String apiKeyHash);

    @Select("SELECT * FROM mcp_api_keys")
    List<McpApiKey> findAll();
}
