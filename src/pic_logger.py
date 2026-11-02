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
