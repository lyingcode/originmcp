const { spawn } = require('child_process');

console.log('🧪 完整 MCP 协议测试');
console.log('=====================================\n');

const mcpServer = spawn('java', ['-jar', 'target/originmcp-0.0.1-SNAPSHOT.jar']);

let buffer = '';
let initialized = false;

mcpServer.stderr.on('data', (data) => {
  // Suppress stderr for cleaner output
});

mcpServer.stdout.on('data', (data) => {
  buffer += data.toString();
  const lines = buffer.split('\n');

  for (let i = 0; i < lines.length - 1; i++) {
    const line = lines[i].trim();
    if (line) {
      try {
        const response = JSON.parse(line);
        console.log('\n📥 收到响应:');
        console.log(JSON.stringify(response, null, 2));

        if (response.id === 1 && !initialized) {
          initialized = true;
          console.log('\n✅ Initialize 成功!发送 initialized notification...\n');
          sendInitialized();

          setTimeout(() => {
            console.log('📤 发送 tools/list 请求...\n');
            sendToolsList();
          }, 500);
        } else if (response.id === 2) {
          console.log('\n✅ Tools 列表获取成功!');
          if (response.result && response.result.tools) {
            console.log(`\n📊 找到 ${response.result.tools.length} 个工具:`);
            response.result.tools.forEach(tool => {
              console.log(`  - ${tool.name}: ${tool.description || '无描述'}`);
            });
          }

          setTimeout(() => {
            console.log('\n✅ 测试完成!关闭服务器...');
            mcpServer.kill();
          }, 1000);
        }
      } catch (e) {
        console.log('[非JSON输出]:', line);
      }
    }
  }
  buffer = lines[lines.length - 1];
});

function sendInitialized() {
  const notification = JSON.stringify({
    jsonrpc: "2.0",
    method: "notifications/initialized",
    params: {}
  }) + '\n';
  mcpServer.stdin.write(notification);
}

function sendToolsList() {
  const request = JSON.stringify({
    jsonrpc: "2.0",
    id: 2,
    method: "tools/list",
    params: {}
  }) + '\n';
  mcpServer.stdin.write(request);
}

mcpServer.on('close', (code) => {
  console.log(`\n进程退出`);
  process.exit(0);
});

// Start with initialize
setTimeout(() => {
  console.log('📤 发送 initialize 请求...\n');
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
  mcpServer.stdin.write(initRequest);
}, 2000);
