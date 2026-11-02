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


def send_to_logger(message):
    try:
        with socket.create_connection((HOST, 45773), timeout=1) as conn:
            conn.sendall(message.encode("utf-8"))
    except OSError:
        print("[LOGGING ERROR] Could not contact logging process", flush=True)

def publish():
    with cpu.sem: v=[cpu.W,cpu.PC,int(cpu.flagZ),int(cpu.flagC),int(cpu.halted)]+cpu.memory[16:32]
    shm.buf[:SHM_SIZE]=struct.pack(FMT,*v)

def event(msg):
    try: events.put_nowait(msg)
    except queue.Full: pass

def parse_program(text):
    out=[]
    for raw in text.splitlines():
        line=raw.split(';',1)[0].strip()
        if not line: continue
        p=line.split(); op=p[0].upper(); operand=int(p[1]) if len(p)>1 else 0
        if op not in {'MOVLW','MOVWF','ADDLW','SUBLW','ANDLW','INCF','GOTO','HALT'}: raise ValueError('Unknown instruction: '+op)
        out.append((op,operand))
        if len(out)>MAX_PROGRAM: raise ValueError('Program too long')
    return out
    
def execute(op,a):
    if op=='MOVLW': cpu.W=a&255; cpu.lastResult=cpu.W
    elif op=='MOVWF': cpu.memory[a]=cpu.W; cpu.lastResult=cpu.W
    elif op=='ADDLW':
        r=cpu.W+a; cpu.flagC=r>255; cpu.W=r&255; cpu.lastResult=cpu.W
    elif op=='SUBLW':
        r=a-cpu.W; cpu.flagC=r>=0; cpu.W=r&255; cpu.lastResult=cpu.W
    elif op=='ANDLW': cpu.W=cpu.W&a; cpu.lastResult=cpu.W
    elif op=='INCF': cpu.memory[a]=(cpu.memory[a]+1)&255; cpu.lastResult=cpu.memory[a]
    elif op=='GOTO': cpu.PC=a
    elif op=='HALT': cpu.halted=True



