package com.bitsoft.originmcp.mapper;

import com.bitsoft.originmcp.model.database.McpToolDefinition;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * MyBatis mapper for mcp_tool_definition table.
 */
@Mapper
public interface McpToolDefinitionMapper {

    @Select("SELECT * FROM mcp_tool_definition WHERE enabled = TRUE ORDER BY priority DESC")
    List<McpToolDefinition> findAllEnabled();

    @Select("SELECT * FROM mcp_tool_definition WHERE enabled = TRUE AND tool_name = #{toolName}")
    McpToolDefinition findByToolName(String toolName);

    @Select("SELECT * FROM mcp_tool_definition ORDER BY priority DESC")
    List<McpToolDefinition> findAll();
}
