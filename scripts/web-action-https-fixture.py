"""Isolated Action-only browser fixture; never deployed or packaged in Release."""
import http.server,ssl,sys
page="""<!doctype html><meta name=viewport content="width=device-width,initial-scale=1"><meta charset=utf-8><h1>任务照片</h1><button onclick="document.getElementById('photo').click()">从相册选择</button><input id=photo type=file accept="image/jpeg,image/png,image/webp" hidden><button id=send disabled>提交照片</button><p id=result>请选择照片</p><script>photo.onchange=()=>{send.disabled=!photo.files.length;result.textContent=photo.files.length?'照片已选择':'请选择照片'};send.onclick=async()=>{const response=await fetch('/upload',{method:'POST',body:photo.files[0]});result.textContent=response.ok?'照片已上传':'上传失败'}</script>""".encode()
class Handler(http.server.BaseHTTPRequestHandler):
 def log_message(self,*args):pass
 def do_GET(self):
  self.send_response(200);self.send_header('Content-Type','text/html; charset=utf-8');self.end_headers();self.wfile.write(page)
 def do_POST(self):
  body=self.rfile.read(int(self.headers.get('Content-Length',0)));assert body.startswith(b'\x89PNG'), 'selected photo missing'
  self.send_response(200);self.end_headers();self.wfile.write(b'ok')
server=http.server.ThreadingHTTPServer(('0.0.0.0',18443),Handler)
ctx=ssl.SSLContext(ssl.PROTOCOL_TLS_SERVER);ctx.load_cert_chain(sys.argv[1],sys.argv[2]);server.socket=ctx.wrap_socket(server.socket,server_side=True);server.serve_forever()
