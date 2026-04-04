package com.bitsoft.originmcp.mapper;

import com.bitsoft.originmcp.model.database.McpToolParameter;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * MyBatis mapper for mcp_tool_parameter table.
 */
@Mapper
public interface McpToolParameterMapper {

    @Select("SELECT * FROM mcp_tool_parameter WHERE tool_id = #{toolId} ORDER BY parameter_order")
    List<McpToolParameter> findByToolId(Long toolId);
}
