import socket, threading, queue, struct
from multiprocessing import shared_memory

HOST='127.0.0.1'; PORT=45772; MEM_SIZE=128; MAX_PROGRAM=256
FMT='<21i'; SHM_SIZE=struct.calcsize(FMT); SHM_NAME='pic16f72_sim_shared'

class CPU:
    def __init__(self):
        self.sem=threading.Semaphore(1); self.reset(); self.program=[]
    def reset(self):
        self.W=0; self.memory=[0]*MEM_SIZE; self.PC=0
        self.flagZ=False; self.flagC=False; self.halted=False; self.lastResult=0
cpu=CPU(); commands=queue.Queue(maxsize=16); events=queue.Queue(maxsize=16); stop=threading.Event()
try: shm=shared_memory.SharedMemory(name=SHM_NAME,create=True,size=SHM_SIZE)
except FileExistsError: shm=shared_memory.SharedMemory(name=SHM_NAME,create=False,size=SHM_SIZE)


