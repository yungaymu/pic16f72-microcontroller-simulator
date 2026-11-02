import logging
import os
import socket

HOST = "127.0.0.1"
PORT = 45773

os.makedirs("logs", exist_ok=True)

logging.basicConfig(
    filename="logs/simulator.log",
    level=logging.INFO,
    format="%(asctime)s | %(levelname)s | %(message)s",
    encoding="utf-8")
logger = logging.getLogger("PIC16F72")

with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as server:
    server.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
    server.bind((HOST, PORT))
    server.listen(10)

print(f"Logging process listening on {HOST}:{PORT}", flush=True)

while True:
        conn, _ = server.accept()

with conn:
            data = b""

 while True:
                chunk = conn.recv(4096)
                if not chunk:
                    break
                data += chunk

message = data.decode("utf-8", errors="replace").strip()
