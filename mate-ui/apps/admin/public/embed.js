/**
 * MateCloud 聊天气泡嵌入脚本 — 一行 script 在任意网站右下角挂出聊天气泡。
 * 用法:
 *   <script src="https://<管理端域名>/embed.js"
 *           data-chat-url="https://<管理端域名>/#/public/agent-chat?agentId=...&apiKey=..."></script>
 * 行为: 气泡按钮 → 点击展开聊天面板 (iframe 懒加载) → 再点/ESC 收起; 移动端全屏。
 */
(function () {
  var script = document.currentScript;
  var chatUrl = script && script.getAttribute('data-chat-url');
  if (!chatUrl) { console.warn('[mate-embed] 缺少 data-chat-url'); return; }
  var color = (script && script.getAttribute('data-color')) || '#406ed3';

  var btn = document.createElement('button');
  btn.setAttribute('aria-label', '打开聊天');
  btn.style.cssText = 'position:fixed;right:20px;bottom:20px;width:56px;height:56px;border-radius:50%;' +
    'border:none;cursor:pointer;z-index:2147483646;display:flex;align-items:center;justify-content:center;' +
    'background:' + color + ';box-shadow:0 8px 24px rgba(0,0,0,.18);transition:transform .15s ease;';
  btn.onmouseenter = function () { btn.style.transform = 'scale(1.06)'; };
  btn.onmouseleave = function () { btn.style.transform = 'scale(1)'; };
  btn.innerHTML = '<svg viewBox="0 0 24 24" width="26" height="26" fill="none" stroke="#fff" stroke-width="2">' +
    '<path d="M21 11.5a8.38 8.38 0 0 1-.9 3.8 8.5 8.5 0 0 1-7.6 4.7 8.38 8.38 0 0 1-3.8-.9L3 21l1.9-5.7a8.38 8.38 0 0 1-.9-3.8 8.5 8.5 0 0 1 4.7-7.6 8.38 8.38 0 0 1 3.8-.9h.5a8.48 8.48 0 0 1 8 8z"/></svg>';

  var panel = null;
  var open = false;

  function buildPanel() {
    panel = document.createElement('div');
    var mobile = window.innerWidth < 640;
    panel.style.cssText = mobile
      ? 'position:fixed;inset:0;z-index:2147483647;background:#fff;'
      : 'position:fixed;right:20px;bottom:88px;width:400px;height:600px;max-height:calc(100vh - 110px);' +
        'z-index:2147483647;border-radius:16px;overflow:hidden;box-shadow:0 12px 48px rgba(0,0,0,.22);background:#fff;';
    var frame = document.createElement('iframe');
    frame.src = chatUrl;
    frame.allow = 'microphone *';
    frame.style.cssText = 'width:100%;height:100%;border:0;';
    panel.appendChild(frame);
    if (mobile) {
      var close = document.createElement('button');
      close.textContent = '✕';
      close.setAttribute('aria-label', '关闭聊天');
      close.style.cssText = 'position:absolute;top:10px;right:12px;width:32px;height:32px;border:none;' +
        'border-radius:50%;background:rgba(0,0,0,.35);color:#fff;font-size:15px;cursor:pointer;';
      close.onclick = toggle;
      panel.appendChild(close);
    }
    document.body.appendChild(panel);
  }

  function toggle() {
    open = !open;
    if (open && !panel) buildPanel();
    if (panel) panel.style.display = open ? 'block' : 'none';
    btn.innerHTML = open
      ? '<svg viewBox="0 0 24 24" width="24" height="24" fill="none" stroke="#fff" stroke-width="2"><path d="M18 6 6 18"/><path d="m6 6 12 12"/></svg>'
      : '<svg viewBox="0 0 24 24" width="26" height="26" fill="none" stroke="#fff" stroke-width="2"><path d="M21 11.5a8.38 8.38 0 0 1-.9 3.8 8.5 8.5 0 0 1-7.6 4.7 8.38 8.38 0 0 1-3.8-.9L3 21l1.9-5.7a8.38 8.38 0 0 1-.9-3.8 8.5 8.5 0 0 1 4.7-7.6 8.38 8.38 0 0 1 3.8-.9h.5a8.48 8.48 0 0 1 8 8z"/></svg>';
  }

  btn.onclick = toggle;
  document.addEventListener('keydown', function (e) { if (e.key === 'Escape' && open) toggle(); });
  document.body.appendChild(btn);
})();
