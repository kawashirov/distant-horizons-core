package com.seibel.lod.core.a7.datatype.full;

// Static class for the data format:
// ID: blockState id    Y: Height(signed)    DP: Depth(signed?) (Depth means the length of the block!)
// BL: Block light     SL: Sky light
// =======Bit layout=======
// BL BL BL BL  SL SL SL SL <-- Top bits
// YY YY YY YY  YY YY YY YY
// YY YY YY YY  DP DP DP DP
// DP DP DP DP  DP DP DP DP
// ID ID ID ID  ID ID IO ID
// ID ID ID ID  ID ID IO ID
// ID ID ID ID  ID ID IO ID
// ID ID ID ID  ID ID IO ID <-- Bottom bits

import com.seibel.lod.core.util.LodUtil;
import org.jetbrains.annotations.Contract;

import static com.seibel.lod.core.a7.datatype.column.ColumnFormat.MAX_WORLD_Y_SIZE;

public class FullFormat {

    public static final int ID_WIDTH = 32;
    public static final int DP_WIDTH = 12;
    public static final int Y_WIDTH = 12;
    public static final int LIGHT_WIDTH = 8;
    public static final int ID_OFFSET = 0;
    public static final int DP_OFFSET = ID_OFFSET + ID_WIDTH;
    public static final int Y_OFFSET = DP_OFFSET + DP_WIDTH;
    public static final int LIGHT_OFFSET = Y_OFFSET + Y_WIDTH;


    public static final long ID_MASK = Integer.MAX_VALUE;
    public static final long INVERSE_ID_MASK = ~ID_MASK;
    public static final int DP_MASK = (int)Math.pow(2, DP_WIDTH) - 1;
    public static final int Y_MASK = (int)Math.pow(2, Y_WIDTH) - 1;
    public static final int LIGHT_MASK = (int)Math.pow(2, LIGHT_WIDTH) - 1;

    public static long encode(int id, int depth, int y, byte lightPair) {
        LodUtil.assertTrue(y >= 0 && y < MAX_WORLD_Y_SIZE, "Trying to create datapoint with y[{}] out of range!", y);
        LodUtil.assertTrue(depth > 0 && depth < MAX_WORLD_Y_SIZE, "Trying to create datapoint with depth[{}] out of range!", depth);
        LodUtil.assertTrue(y+depth <= MAX_WORLD_Y_SIZE, "Trying to create datapoint with y+depth[{}] out of range!", y+depth);
        long data = 0;
        data |= id & ID_MASK;
        data |= (long) (depth & DP_MASK) << DP_OFFSET;
        data |= (long) (y & Y_MASK) << Y_OFFSET;
        data |= (long) lightPair << LIGHT_OFFSET;
        LodUtil.assertTrue(getId(data) == id && getDepth(data) == depth && getY(data) == y && getLight(data) == Byte.toUnsignedInt(lightPair),
                "Trying to create datapoint with id[{}], depth[{}], y[{}], lightPair[{}] but got id[{}], depth[{}], y[{}], lightPair[{}]!",
                id, depth, y, Byte.toUnsignedInt(lightPair), getId(data), getDepth(data), getY(data), getLight(data));
        return data;
    }

    public static int getId(long data) {
        return (int) (data & ID_MASK);
    }

    public static int getDepth(long data) {
        return (int) ((data >> DP_OFFSET) & DP_MASK);
    }

    public static int getY(long data) {
        return (int) ((data >> Y_OFFSET) & Y_MASK);
    }

    public static int getLight(long data) {
        return (int) ((data >> LIGHT_OFFSET) & LIGHT_MASK);
    }

    public static String toString(long data) {
        return "[ID:" + getId(data) + ",Y:" + getY(data) + ",Depth:" + getY(data) + ",Light:" + getLight(data) + "]";
    }

    @Contract(pure = true)
    public static long remap(int[] mapping, long data) {
        return (data & INVERSE_ID_MASK) | mapping[(int)data];
    }
}
