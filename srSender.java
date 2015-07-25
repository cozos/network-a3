import java.io.IOException;
import java.net.DatagramPacket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class srSender extends Sender {
  
  public class TransitPacket {
    public CS456Packet packet;
    public long sentDate;
    public boolean acked = false;
    
    public TransitPacket(CS456Packet packet) {
      this.packet = packet;
      this.sentDate = System.currentTimeMillis();
    }
    
    public void ack() {
      this.acked = true;
    }
    
    public boolean isAcked() {
      return this.acked;
    }
    
    public void resetTimer() {
      this.sentDate = System.currentTimeMillis();
    }
    
    public boolean isTimedOut() {
      return (System.currentTimeMillis() - this.sentDate) > Sender.TIMEOUT; 
    }
    
    public boolean shouldSend() {
      return (this.isTimedOut() && !this.isAcked());
    }
  }
  
  private Lock lock = new ReentrantLock();
  private int windowBase;
  private List<TransitPacket> inTransit = new ArrayList<TransitPacket>();
  
  public srSender(long timeout, String fileName) {
    super(timeout, fileName);
    
    this.windowBase = 0;
    for(int i = 0; i < Math.min(CS456Packet.WINDOW_SIZE, this.packetQueue.size()); i++) {
      this.inTransit.add(new TransitPacket(this.packetQueue.get(0)));
      this.packetQueue.remove(0);
    }
  }

  private TransitPacket findTransitPacket(int sequenceNumber) {
    for (TransitPacket transitPacket : this.inTransit) {
      if (transitPacket.packet.getSequenceNumber() == sequenceNumber) {
        return transitPacket;
      }
    }
    return null;
  }
  
  private void shiftWindow() {
    while (!this.inTransit.isEmpty() && this.inTransit.get(0).isAcked()) {
      if (!this.packetQueue.isEmpty()) {
        this.inTransit.add(new TransitPacket(this.packetQueue.get(0)));
        this.packetQueue.get(0).printLog(true);
        this.packetQueue.remove(0);
      }
      this.inTransit.remove(0);
      this.windowBase++;
    }
  }
  
  public void send() {
    while(!this.inTransit.isEmpty()) {
      lock.lock();
      
      try {
        for (TransitPacket transitPacket : this.inTransit) {
          if (transitPacket.shouldSend()) {
            this.socket.send(transitPacket.packet.getDatagram(this.host, this.port));
            transitPacket.resetTimer();
          }
        }
      } catch (IOException e) {}
      finally {
        lock.unlock();
      }
      
      try {
        Thread.sleep(1);
      } catch (InterruptedException e) {} 
    }
    
    // Send EOT packet.
    CS456Packet packetEOT = new CS456Packet(2, this.windowBase + CS456Packet.WINDOW_SIZE, new byte[0]);
    try { 
      socket.send(packetEOT.getDatagram(this.host, this.port));
      packetEOT.printLog(true);
    } catch (IOException e) {}
  }
  
  public void listen() {
    DatagramPacket ackPacket = new DatagramPacket(new byte[512], 512);
    while (this.foundEOTPacket == false) {
      try {
        socket.receive(ackPacket);
      } catch (IOException e) {}

      lock.lock();
      try {
        CS456Packet parsedAckPacket = new CS456Packet(ackPacket.getData());
        parsedAckPacket.printLog(false);
        
        if (parsedAckPacket.isEOT()) {
          this.foundEOTPacket = true;
        } else {
          TransitPacket transitPacket = findTransitPacket(parsedAckPacket.getSequenceNumber());
          if (transitPacket != null) {
            transitPacket.ack();
          }
          
          if (parsedAckPacket.getSequenceNumber() == CS456Packet.getSequenceNumber(this.windowBase)) {
            this.shiftWindow();
          }
        }
      } finally {
        lock.unlock();
      }
    }
  }
  
  @Override
  public void start() {
    Thread sender = new Thread() {
      public void run() {
        send();
      }
    };
    
    Thread listener = new Thread() {
      public void run() {
        listen();
      }
    };
    
    listener.start();
    sender.start();
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
