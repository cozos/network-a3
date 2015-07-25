import java.net.DatagramPacket;
import java.net.InetAddress;
import java.nio.ByteBuffer;


/**
 * Utils for Networks A3
 * 
 * The Packet format as defined by CS456 Networks A3.
 *
 * 512 bytes, in this order:
 *     Packet Type       32 bit unsigned integer, big endian
 *     Sequence Number   32 bit unsigned integer, big endian
 *     Packet Length     32 bit unsigned integer, big endian
 *     Payload           byte sequence, maximum 500 bytes
 *
 * The Packet Type field indicates the type of the packet. It is set as follows:
 *     0   Data Packet
 *     1   Acknowledgement (ACK) Packet
 *     2   End-Of-Transfer (EOT) Packet (see below for details)
 *
 */
public class CS456Packet {
  public static int WINDOW_SIZE = 10; 
  public static int SEQUENCE_MODULO = 256;
  
  public static int getSequenceNumber(int i) {
    return i % SEQUENCE_MODULO;
  }
  
  private byte[] raw;

  // Formatted fields
  private int packetType;
  private int sequenceNumber;
  private int length;
  private int payloadLength;
  private byte[] payload;
  private boolean acked = false; 
  
  /**
   * Given raw bytes, parse into understandable format.
   */
  public CS456Packet(byte[] packet){
    this.raw = packet;
    
    // Parse out fields.
    ByteBuffer buffer = ByteBuffer.wrap(packet);
    this.packetType = buffer.getInt();
    this.sequenceNumber = buffer.getInt() % SEQUENCE_MODULO;
    this.length = buffer.getInt();
    this.payloadLength = length - 12;
    this.payload = new byte[length - 12];
    buffer.get(payload, 0, length - 12);
  }
  
  /**
   * Given a type, sequenceNumber and payload, package into raw.
   */
  public CS456Packet(int packetType, int sequenceNumber, byte[] payload) {
    this.packetType = packetType;
    this.sequenceNumber = sequenceNumber % SEQUENCE_MODULO;
    this.length = payload.length + 12;
    this.payloadLength = payload.length;
    this.payload = payload;
    
    ByteBuffer buffer = ByteBuffer.allocate(payload.length + 12);
    buffer.putInt(packetType);
    buffer.putInt(sequenceNumber);
    buffer.putInt(payload.length + 12);
    buffer.put(payload, 0, payload.length);
    
    raw = buffer.array();
  }
  
  public DatagramPacket getDatagram(InetAddress host, int port) { 
    return new DatagramPacket(raw, length, host, port);
  }
  
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("Packet Type: ");
    sb.append(packetType);
    sb.append(" Sequence Number: ");
    sb.append(sequenceNumber);
    sb.append(" Length: ");
    sb.append(length);
    sb.append(" Payload: ");
    sb.append(new String(payload));
    return sb.toString();
  }
  
  /**
   * Prints a "SEND" log message if (send == true)
   * Prints a "RECV" log message if (send == false)
   */
  public void printLog(boolean send) {
    StringBuilder message = new StringBuilder();
    
    message.append("PKT ");
    
    if (send) {
      message.append("SEND ");
    } else {
      message.append("RECV ");
    }
    
    if (this.isACK()) {
      message.append("ACK ");
    } else if (this.isData()) {
      message.append("DAT ");
    } else {
      message.append("EOT ");
    }
    
    message.append(this.sequenceNumber + " ");
    message.append(this.length);
    
    System.out.println(message.toString());
  }
  
  public static int windowLowerBound(int base) {
    return (base - WINDOW_SIZE) % SEQUENCE_MODULO;
  }
  
  public static int windowUpperBound(int base) {
    return (base + WINDOW_SIZE) % SEQUENCE_MODULO;
  }
  
  public boolean isData() {
    return this.packetType == 0; 
  }
  
  public boolean isACK() {
    return this.packetType == 1; 
  }
  
  public boolean isEOT() {
    return this.packetType == 2; 
  }
  
  public byte[] getRaw() {
    return this.raw;
  }
  
  public int getSequenceNumber() {
    return this.sequenceNumber;
  }
  
  public int getLength() {
    return this.length;
  }
  
  public int getPayloadLength() {
    return payloadLength;
  }
  
  public String getPayload() {
    return new String(this.payload);
  }
  
  public byte[] getPayloadBytes() {
    return this.payload;
  }
  
  /**
   * Not to be confused with isACK, which gets the type of the Packet. 
   */
  public boolean isAcked() {
    return this.acked;
  }

  public void setAcked(boolean acked) {
    this.acked = acked;
  }
}
