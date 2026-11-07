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

def number(value):
    return int(value, 0)

def parse_program(text):
    source=[]; labels={}
    for raw in text.splitlines():
        line=raw.split(';',1)[0].strip()
        if not line: continue
        if ':' in line:
            label, rest=line.split(':',1); label=label.strip()
            if not label or not label.replace('_','').isalnum(): raise ValueError('Invalid label: '+label)
            if label in labels: raise ValueError('Duplicate label: '+label)
            labels[label]=len(source); line=rest.strip()
            if not line: continue
        parts=line.replace(',', ' ').split()
        source.append(parts)
        if len(source)>MAX_PROGRAM: raise ValueError('Program too long')
    out=[]
    allowed={'MOVLW','MOVWF','ADDLW','SUBLW','ANDLW','XORLW','XORWF','INCF','RLF','DECFSZ','BTFSC','BTFSS','CLRC','GOTO','HALT'}
    for parts in source:
        op=parts[0].upper(); args=parts[1:]
        if op not in allowed: raise ValueError('Unknown instruction: '+op)
        if op in {'CLRC','HALT'}:
            if args: raise ValueError(op+' takes no operand')
            operand=None
        elif op in {'BTFSC','BTFSS'}:
            if len(args)!=1 or args[0].upper()!='C': raise ValueError(op+' currently supports the C carry flag')
            operand='C'
        elif op=='GOTO':
            if len(args)!=1: raise ValueError('GOTO requires one label or address')
            operand=labels[args[0]] if args[0] in labels else number(args[0])
        else:
            if len(args)!=1: raise ValueError(op+' requires one operand')
            operand=number(args[0])
            if op in {'MOVWF','XORWF','INCF','RLF','DECFSZ'} and not 0<=operand<MEM_SIZE: raise ValueError('Memory address out of range: '+str(operand))
            if op in {'MOVLW','ADDLW','SUBLW','ANDLW','XORLW'} and not 0<=operand<=255: raise ValueError('Literal must be between 0 and 255')
        out.append((op,operand))
    return out

def execute(op,a):
    if op=='MOVLW': cpu.W=a&255; cpu.lastResult=cpu.W
    elif op=='MOVWF': cpu.memory[a]=cpu.W; cpu.lastResult=cpu.W
    elif op=='ADDLW':
        r=cpu.W+a; cpu.flagC=r>255; cpu.W=r&255; cpu.lastResult=cpu.W
    elif op=='SUBLW':
        r=a-cpu.W; cpu.flagC=r>=0; cpu.W=r&255; cpu.lastResult=cpu.W
    elif op=='ANDLW': cpu.W=cpu.W&a; cpu.lastResult=cpu.W
    elif op=='XORLW': cpu.W=cpu.W^a; cpu.lastResult=cpu.W
    elif op=='XORWF': cpu.memory[a]=(cpu.memory[a]^cpu.W)&255; cpu.lastResult=cpu.memory[a]
    elif op=='INCF': cpu.memory[a]=(cpu.memory[a]+1)&255; cpu.lastResult=cpu.memory[a]
    elif op=='CLRC': cpu.flagC=False; cpu.lastResult=0
    elif op=='RLF':
        old_c=1 if cpu.flagC else 0; value=cpu.memory[a]
        cpu.flagC=bool(value&0x80); cpu.memory[a]=((value<<1)|old_c)&255; cpu.lastResult=cpu.memory[a]
    elif op=='DECFSZ':
        cpu.memory[a]=(cpu.memory[a]-1)&255; cpu.lastResult=cpu.memory[a]
        if cpu.memory[a]==0: cpu.PC+=1
    elif op=='BTFSC':
        if not cpu.flagC: cpu.PC+=1
        cpu.lastResult=int(cpu.flagC)
    elif op=='BTFSS':
        if cpu.flagC: cpu.PC+=1
        cpu.lastResult=int(cpu.flagC)
    elif op=='GOTO': cpu.PC=a
    elif op=='HALT': cpu.halted=True
        
def step():
    with cpu.sem:
        if cpu.halted: return ['Program already halted.'],None
        if not 0<=cpu.PC<len(cpu.program): cpu.halted=True; return ['No more instructions. Program halted.'],'HALTED'
        pc=cpu.PC; op,a=cpu.program[pc]; cpu.PC+=1; wb=cpu.W
        track=op in ('MOVWF','INCF','XORWF','RLF','DECFSZ'); mb=cpu.memory[a] if track else -1
        old_c=cpu.flagC; execute(op,a); cpu.flagZ=(cpu.lastResult==0)
        operand='' if a is None else str(a)
        lines=['Instruction : HALT' if op=='HALT' else f'Instruction : {op} {operand}',f'PC : {pc:04d}','Execution Trace','----------------------------','FETCH   [OK]',('DECODE  [OK] -> HALT' if op=='HALT' else f'DECODE  [OK] -> {op} (operand={operand})'),'EXECUTE [OK]','Result','----------------------------']
        if wb!=cpu.W: lines.append(f'W : {wb} -> {cpu.W}')
        if track and mb!=cpu.memory[a]: lines.append(f'Memory[{a}] : {mb} -> {cpu.memory[a]}')
        if old_c!=cpu.flagC: lines.append(f'C Flag : {old_c} -> {cpu.flagC}')
        lines += [f'PC : {pc} -> {cpu.PC}',f"Z Flag : {'true' if cpu.flagZ else 'false'}   C Flag : {'true' if cpu.flagC else 'false'}"]
        if cpu.halted: lines.append('>>> PROGRAM TERMINATED <<<'); ev='HALTED'
        else: ev=f'EXECUTED {op} PC={cpu.PC}'
        return lines,ev

def handle(cmd, payload):
    event(f'COMMAND {cmd}')
    if cmd=='LOAD':
        try: program=parse_program(payload or '')
        except Exception as e:
            event(f'ERROR LOAD: {e}'); return [f'ERROR {e}']
        with cpu.sem:
            cpu.program=program; cpu.reset()
        event(f'LOADED {len(cpu.program)} instructions')
        return [f'OK {len(cpu.program)} instructions']
    if cmd=='STEP':
        lines,ev=step()
        if ev: event(ev)
        return lines
    if cmd=='RESET':
        with cpu.sem: cpu.reset()
        event('RESET'); return ['OK']
    if cmd=='QUIT': stop.set(); return ['OK']
    return ['ERROR Unknown command: '+cmd]

def executor():
    while not stop.is_set():
        try: cmd,payload,conn=commands.get(timeout=.2)
        except queue.Empty: continue
        try:
            for line in handle(cmd,payload): conn.sendall((line+'\n').encode())
            conn.sendall(b'---END---\n'); publish()
        except (BrokenPipeError,ConnectionResetError): pass
        commands.task_done()

def event_worker():
    while not stop.is_set():
        try: msg=events.get(timeout=.2)
        except queue.Empty: continue
        print('[EVENT]',msg,flush=True); send_to_logger(msg); events.task_done()

def client(conn):
    f=conn.makefile('r',encoding='utf-8',newline='\n')
    try:
        while not stop.is_set():
            line=f.readline()
            if not line: break
            line=line.rstrip('\r\n')
            if line=='LOAD':
                ls=[]
                while True:
                    x=f.readline()
                    if not x: return
                    x=x.rstrip('\r\n')
                    if x=='END': break
                    ls.append(x)
                commands.put(('LOAD','\n'.join(ls),conn))
            elif line in ('STEP','RESET','QUIT'):
                commands.put((line,None,conn))
                if line=='QUIT': break
    finally:
        f.close(); conn.close()

def main():
    publish(); threading.Thread(target=executor,daemon=True).start(); threading.Thread(target=event_worker,daemon=True).start()
    with socket.socket() as s:
        s.setsockopt(socket.SOL_SOCKET,socket.SO_REUSEADDR,1); s.bind((HOST,PORT)); s.listen(4); s.settimeout(.5)
        print(f'PIC16F72 Python backend listening on {HOST}:{PORT}',flush=True)
        try:
            while not stop.is_set():
                try: c,_=s.accept()
                except socket.timeout: continue
                threading.Thread(target=client,args=(c,),daemon=True).start()
        finally:
            send_to_logger("SHUTDOWN"); stop.set(); shm.close()
            try: shm.unlink()
            except FileNotFoundError: pass
if __name__=='__main__': main()








