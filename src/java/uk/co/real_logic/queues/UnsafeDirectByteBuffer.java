package uk.co.real_logic.queues;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class UnsafeDirectByteBuffer {
    private static final long addressOffset;
    public static final int CACHE_LINE_SIZE = 64;
    public static final int PAGE_SIZE = UnsafeAccess.unsafe.pageSize();
    static {
	try {
	    addressOffset = UnsafeAccess.unsafe.objectFieldOffset(Buffer.class
		    .getDeclaredField("address"));
	} catch (Exception e) {
	    throw new RuntimeException(e);
	}
    }

    public static long getAddress(ByteBuffer buffy) {
	return UnsafeAccess.unsafe.getLong(buffy, addressOffset);
    }

    /**
     * put byte and skip position update and boundary checks
     * 
     * @param buffy
     * @param b
     */
    public static void putByte(long address, int position, byte b) {
	UnsafeAccess.unsafe.putByte(address + (position << 0), b);
    }

    public static void putByte(long address, byte b) {
	UnsafeAccess.unsafe.putByte(address, b);
    }
    public static ByteBuffer allocateAlignedByteBuffer(int capacity, long align){
	if(Long.bitCount(align) != 1){
	    throw new IllegalArgumentException("Alignment must be a power of 2");
	}
	ByteBuffer buffy = ByteBuffer.allocateDirect((int)(capacity+align)).order(ByteOrder.nativeOrder());
	long address = getAddress(buffy);
	if((address & (align-1)) == 0){
	    return buffy;
	}
	else{
	    int newPosition = (int)(align - (address & (align - 1)));
	    buffy.position(newPosition);
	    int newLimit = newPosition + capacity;
	    buffy.limit(newLimit);
	    return buffy.slice();
	}
    }
    public static boolean isPageAligned(ByteBuffer buffy){
	return isPageAligned(getAddress(buffy));
    }
    /**
     * This assumes cache line is 64b
     */
    public static boolean isCacheAligned(ByteBuffer buffy){
	return isCacheAligned(getAddress(buffy));
    }
    public static boolean isPageAligned(long address){
	return (address & (PAGE_SIZE-1)) == 0;
    }
    /**
     * This assumes cache line is 64b
     */
    public static boolean isCacheAligned(long address){
	return (address & (CACHE_LINE_SIZE-1)) == 0;
    }
    public static boolean isAligned(long address, long align){
	if(Long.bitCount(align) != 1){
	    throw new IllegalArgumentException("Alignment must be a power of 2");
	}
	return (address & (align-1)) == 0;
    }
}
