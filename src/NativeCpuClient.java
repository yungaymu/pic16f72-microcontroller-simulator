import java.io.*; import java.net.*; import java.nio.charset.StandardCharsets; import java.util.*;
public class NativeCpuClient {
 private static final String HOST="127.0.0.1"; private static final int PORT=45772;
 private Socket socket; private BufferedWriter cmdOut; private BufferedReader respIn;
 public void connect() throws IOException { socket=new Socket(); socket.connect(new InetSocketAddress(HOST,PORT),5000); cmdOut=new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(),StandardCharsets.UTF_8)); respIn=new BufferedReader(new InputStreamReader(socket.getInputStream(),StandardCharsets.UTF_8)); }
 private void sendLine(String s)throws IOException{cmdOut.write(s);cmdOut.write("\n");cmdOut.flush();}
 private List<String> readUntilMarker()throws IOException{List<String> r=new ArrayList<>();String s;while((s=respIn.readLine())!=null){if(s.equals("---END---"))break;r.add(s);}return r;}
 public List<String> load(String p)throws IOException{sendLine("LOAD");for(String l:p.split("\\R",-1))sendLine(l);sendLine("END");return readUntilMarker();}
 public List<String> step()throws IOException{sendLine("STEP");return readUntilMarker();}
 public List<String> reset()throws IOException{sendLine("RESET");return readUntilMarker();}
 public void quit()throws IOException{sendLine("QUIT");readUntilMarker();socket.close();}
}
