import java.nio.ByteBuffer;


/**
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
  private byte[] raw;

  // Formatted fields
  private int packetType;
  private int sequenceNumber;
  private int length;
  private byte[] payload;
  
  /**
   * Given raw bytes, parse into understandable format.
   */
  public CS456Packet(byte[] packet){
    this.raw = packet;
    
    // Parse out fields.
    ByteBuffer buffer = ByteBuffer.wrap(packet);
    this.packetType = buffer.getInt();
    this.sequenceNumber = buffer.getInt();
    this.length = buffer.getInt();
    this.payload = new byte[length - 12];
    buffer.get(payload, 0, length - 12);
  }
  
  /**
   * Given a type, sequenceNumber and payload, package into raw.
   */
  public CS456Packet(int packetType, int sequenceNumber, byte[] payload) {
    this.packetType = packetType;
    this.sequenceNumber = sequenceNumber;
    this.length = payload.length + 12;
    this.payload = payload;
    
    ByteBuffer buffer = ByteBuffer.allocate(payload.length + 12);
    buffer.putInt(packetType);
    buffer.putInt(sequenceNumber);
    buffer.putInt(payload.length + 12);
    buffer.put(payload, 0, payload.length);
    
    raw = buffer.array();
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
}