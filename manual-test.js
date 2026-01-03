const { spawn } = require('child_process');

console.log('手动测试 MCP 协议通信');
console.log('=====================================\n');

const mcpServer = spawn('java', ['-jar', 'target/originmcp-0.0.1-SNAPSHOT.jar']);

// Buffer stderr
mcpServer.stderr.on('data', (data) => {
  console.error(`[STDERR]: ${data}`);
});

// Read stdout line by line
let buffer = '';
mcpServer.stdout.on('data', (data) => {
  buffer += data.toString();
  console.log('[STDOUT]:', data.toString());
});

mcpServer.on('error', (error) => {
  console.error('Error:', error);
});

mcpServer.on('close', (code) => {
  console.log(`\n进程退出,代码: ${code}`);
});

// Wait for server to start
setTimeout(() => {
  console.log('\n发送 initialize 请求...\n');
  const initRequest = JSON.stringify({
    jsonrpc: "2.0",
    id: 1,
    method: "initialize",
    params: {
      protocolVersion: "2024-11-05",
      capabilities: {},
      clientInfo: {
        name: "test-client",
        version: "1.0.0"
      }
    }
  }) + '\n';

  console.log('请求内容:', initRequest);
  mcpServer.stdin.write(initRequest);

  // Wait for response
  setTimeout(() => {
    console.log('\n5秒后关闭...');
    mcpServer.kill();
  }, 5000);
}, 2000);
