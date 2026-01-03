#!/usr/bin/env node

/**
 * MCP Server 测试脚本
 * 用于验证 MCP Server 是否正确暴露 tools
 *
 * 使用方法:
 * 1. 确保已安装 Node.js
 * 2. 打包项目: mvn clean package
 * 3. 运行测试: node test-mcp-server.js
 */

const { spawn } = require('child_process');
const path = require('path');

// MCP Server JAR 路径
const jarPath = path.join(__dirname, 'target', 'originmcp-0.0.1-SNAPSHOT.jar');

console.log('🚀 MCP Server 测试工具');
console.log('===========================================');
console.log('JAR 路径:', jarPath);
console.log('===========================================\n');

// 启动 MCP Server 进程
const mcpServer = spawn('java', ['-jar', jarPath], {
    stdio: ['pipe', 'pipe', 'pipe']
});

let stdoutBuffer = '';
let stderrBuffer = '';
let testsPassed = 0;
let testsFailed = 0;

// 监听标准输出
mcpServer.stdout.on('data', (data) => {
    const output = data.toString();
    stdoutBuffer += output;

    try {
        // 尝试解析 JSON-RPC 响应
        const lines = output.split('\n').filter(line => line.trim());
        lines.forEach(line => {
            try {
                const json = JSON.parse(line);
                if (json.jsonrpc === '2.0') {
                    console.log('\n✅ 收到 MCP 响应:');
                    console.log(JSON.stringify(json, null, 2));
                    console.log('-------------------------------------------');

                    // 验证响应
                    if (json.id === 1 && json.result && json.result.tools) {
                        console.log('\n📋 可用工具列表:');
                        json.result.tools.forEach(tool => {
                            console.log(`  - ${tool.name}: ${tool.description}`);
                        });

                        if (json.result.tools.length === 2) {
                            console.log('\n✅ 测试通过: 工具数量正确 (2个)');
                            testsPassed++;
                        } else {
                            console.log(`\n❌ 测试失败: 预期2个工具,实际${json.result.tools.length}个`);
                            testsFailed++;
                        }
                    }

                    if (json.id === 2 && json.result && json.result.content) {
                        console.log('\n🌤️  天气查询结果:');
                        json.result.content.forEach(item => {
                            console.log(item.text);
                        });

                        if (json.result.content[0].text.includes('Temperature')) {
                            console.log('\n✅ 测试通过: 成功获取天气数据');
                            testsPassed++;
                        } else {
                            console.log('\n❌ 测试失败: 天气数据格式异常');
                            testsFailed++;
                        }
                    }
                }
            } catch (e) {
                // 忽略非 JSON 输出
            }
        });
    } catch (e) {
        // 忽略解析错误
    }
});

// 监听标准错误
mcpServer.stderr.on('data', (data) => {
    const error = data.toString();
    stderrBuffer += error;

    // 只显示重要错误,忽略 Spring Boot 启动日志
    if (error.includes('ERROR') || error.includes('Exception')) {
        console.error('❌ 错误:', error);
    }
});

// 发送 tools/list 请求
setTimeout(() => {
    console.log('📤 发送请求: tools/list\n');

    const listToolsRequest = {
        jsonrpc: '2.0',
        id: 1,
        method: 'tools/list',
        params: {}
    };

    mcpServer.stdin.write(JSON.stringify(listToolsRequest) + '\n');
}, 3000);

// 发送 tools/call 请求
setTimeout(() => {
    console.log('\n📤 发送请求: tools/call (getWeather, location=shenzhen)\n');

    const callToolRequest = {
        jsonrpc: '2.0',
        id: 2,
        method: 'tools/call',
        params: {
            name: 'getWeather',
            arguments: {
                location: 'shenzhen'
            }
        }
    };

    mcpServer.stdin.write(JSON.stringify(callToolRequest) + '\n');
}, 6000);

// 结束测试
setTimeout(() => {
    console.log('\n===========================================');
    console.log('📊 测试总结');
    console.log('===========================================');
    console.log(`✅ 通过: ${testsPassed}`);
    console.log(`❌ 失败: ${testsFailed}`);
    console.log(`📝 总计: ${testsPassed + testsFailed}`);

    if (testsFailed === 0 && testsPassed > 0) {
        console.log('\n🎉 所有测试通过!MCP Server 工作正常!');
    } else if (testsPassed === 0) {
        console.log('\n⚠️  警告: 没有收到预期的 MCP 响应');
        console.log('   请检查:');
        console.log('   1. JAR 文件是否存在');
        console.log('   2. Java 版本是否正确 (需要 Java 21)');
        console.log('   3. 查看错误日志');
    } else {
        console.log('\n⚠️  部分测试失败,请检查日志');
    }

    console.log('\n如果需要查看完整日志:');
    console.log('  stdout:', stdoutBuffer.length, '字节');
    console.log('  stderr:', stderrBuffer.length, '字节');

    mcpServer.kill();
    process.exit(testsFailed === 0 ? 0 : 1);
}, 10000);

// 错误处理
mcpServer.on('error', (error) => {
    console.error('❌ 启动 MCP Server 失败:', error.message);
    console.error('\n请检查:');
    console.error('  1. Java 是否已安装: java -version');
    console.error('  2. JAR 文件是否存在:', jarPath);
    console.error('  3. 是否已运行: mvn clean package');
    process.exit(1);
});

mcpServer.on('exit', (code, signal) => {
    if (code !== 0 && code !== null && signal !== 'SIGTERM') {
        console.error(`\n❌ MCP Server 异常退出`);
        console.error(`   退出码: ${code}`);
        console.error(`   信号: ${signal}`);

        if (stderrBuffer) {
            console.error('\n错误日志:');
            console.error(stderrBuffer);
        }
    }
});

console.log('⏳ 正在启动 MCP Server...');
console.log('   (这可能需要几秒钟)');
console.log('');
