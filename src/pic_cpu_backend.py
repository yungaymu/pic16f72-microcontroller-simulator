import socket, threading, queue, struct
from multiprocessing import shared_memory

HOST='127.0.0.1'; PORT=45772; MEM_SIZE=128; MAX_PROGRAM=256
FMT='<21i'; SHM_SIZE=struct.calcsize(FMT); SHM_NAME='pic16f72_sim_shared'


