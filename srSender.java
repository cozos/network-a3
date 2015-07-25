import java.io.IOException;
import java.net.DatagramPacket;


public class srSender extends Sender {
  public static int WINDOW_SIZE = 10; 
  
  public class TransitPacket {
    public int sequenceNumber;
    public long sentDate;
    
    public TransitPacket(int sequenceNumber) {
      this.sequenceNumber = sequenceNumber;
      this.sentDate = System.currentTimeMillis();
    }
    
    public boolean isTimedOut() {
      return (System.currentTimeMillis() - this.sentDate) > Sender.TIMEOUT; 
    }
  }
  
  private int latestAck = -1;
  private int latestSent = -1;
//  private List<TransitPacket> inTransit = Collections.synchronizedList(new ArrayList<TransitPacket>());
  
  public srSender(long timeout, String fileName) {
    super(timeout, fileName);
  }

  @Override
  public void start() {
    this.latestSent = 0;
    
    for (latestSent = 0; latestSent < fileContents.size(); latestSent++) {
      while (latestSent - latestAck > WINDOW_SIZE) {
        try {
          Thread.sleep(1);
        } catch (InterruptedException e) {}
      }
      
      byte[] chunk = this.fileContents.get(latestSent);
      
      // Package chunk in payload.
      CS456Packet packet = new CS456Packet(0, this.latestSent, chunk);
      
      // Construct Datagram out of raw bytes.
      byte[] rawPacket = packet.getRaw();
      DatagramPacket formattedPacket = new DatagramPacket(rawPacket, rawPacket.length, this.host, this.port); 
      
      // Send to socket.
      try {
        this.socket.send(formattedPacket);
        packet.printLog(true);
      } catch (IOException e) {}
      
      this.latestSent++;
    }
    
    // Send EOT packet.
    CS456Packet packetEOT = new CS456Packet(2, this.latestSent, new byte[0]);
    byte[] rawEOT = packetEOT.getRaw();
    DatagramPacket formattedPacketEOT = new DatagramPacket(rawEOT, rawEOT.length, this.host, this.port);
    try {
      socket.send(formattedPacketEOT);
    } catch (IOException e) {}
  }
  
  public static void main(String args[]) {
    if (args.length != 2) {
      System.out.println("Expected 2 arguments: timeout(ms) infile");
      System.exit(0);
    }
    
    srSender sender = new srSender(Long.valueOf(args[0]).longValue(), args[1]);
    sender.start();
  }
}
