package com.seibel.lod.core.file;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.atomic.AtomicLong;
import java.util.zip.Adler32;
import java.util.zip.CheckedOutputStream;

import com.seibel.lod.core.pos.DhSectionPos;
import com.seibel.lod.core.util.objects.UnclosableOutputStream;
import com.seibel.lod.core.logging.DhLoggerBuilder;
import com.seibel.lod.core.util.LodUtil;
import org.apache.logging.log4j.Logger;

/**
 * Used size: 40 bytes <br>
 * Remaining space: 24 bytes <br>
 * Total size: 64 bytes <br> <br><br>
 * 
 * 
 * Metadata format: <br> <br>
 * 
 * 4 bytes: magic bytes: "DHv0" (in ascii: 0x44 48 76 30) (this also signals the metadata format) <br>
 * 4 bytes: section X position <br>
 * 4 bytes: section Y position (Unused, for future proofing) <br>
 * 4 bytes: section Z position <br> <br>
 * 
 * 4 bytes: data checksum <br> //TODO: Implement checksum
 * 1 byte: section detail level <br>
 * 1 byte: data detail level // Note: not sure if this is needed <br>
 * 1 byte: loader version <br>
 * 1 byte: unused <br> <br>
 * 
 * 8 bytes: datatype identifier <br> <br>
 * 
 * 8 bytes: data version
 */
public class MetaFile
{
    private static final Logger LOGGER = DhLoggerBuilder.getLogger();
    

    

    public static final int METADATA_SIZE = 64;
    public static final int METADATA_RESERVED_SIZE = 24;
    public static final int METADATA_MAGIC_BYTES = 0x44_48_76_30;

    /** Currently set to false because for some reason Window is throwing PermissionDeniedException when trying to atomic replace a file... */
    public static final boolean USE_ATOMIC_MOVE_REPLACE = false;

    public final DhSectionPos pos;

    public File path;

    @FunctionalInterface
    public interface IOConsumer<T> {
        void accept(T t) throws IOException;
    }

    public static class MetaData {
        public DhSectionPos pos;
        public int checksum;
        public AtomicLong dataVersion;
        public byte dataLevel;
        //Loader stuff
        public long dataTypeId;
        public byte loaderVersion;

        public MetaData(DhSectionPos pos, int checksum, long dataVersion, byte dataLevel, long dataTypeId, byte loaderVersion) {
            this.pos = pos;
            this.checksum = checksum;
            this.dataVersion = new AtomicLong(dataVersion);
            this.dataLevel = dataLevel;
            this.dataTypeId = dataTypeId;
            this.loaderVersion = loaderVersion;
        }
    }
    public volatile MetaData metaData = null;
    private static MetaData readMeta(File file) throws IOException {
        try (FileChannel channel = FileChannel.open(file.toPath(), StandardOpenOption.READ)) {
            ByteBuffer buffer = ByteBuffer.allocate(METADATA_SIZE);
            channel.read(buffer, 0);
            channel.close();
            buffer.flip();

            int magic = buffer.getInt();
            if (magic != METADATA_MAGIC_BYTES) {
                throw new IOException("Invalid file: Magic bytes check failed.");
            }
            int x = buffer.getInt();
            int y = buffer.getInt(); // Unused
            int z = buffer.getInt();
            int checksum = buffer.getInt();
            byte detailLevel = buffer.get();
            byte dataLevel = buffer.get();
            byte loaderVersion = buffer.get();
            byte unused = buffer.get();
            long dataTypeId = buffer.getLong();
            long timestamp = buffer.getLong();
            LodUtil.assertTrue(buffer.remaining() == METADATA_RESERVED_SIZE);
            DhSectionPos dataPos = new DhSectionPos(detailLevel, x, z);
            return new MetaData(dataPos, checksum, timestamp, dataLevel, dataTypeId, loaderVersion);
        }
    }

    private void validateFile() throws IOException {
        if (!path.exists()) throw new IOException("File missing");
        if (!path.isFile()) throw new IOException("Not a file");
        if (!path.canRead()) throw new IOException("File not readable");
        if (!path.canWrite()) throw new IOException("File not writable");
    }

    // Create a metaFile in this path. If the path has a file, throws FileAlreadyExistsException
    protected MetaFile(File path, DhSectionPos pos) throws IOException {
        this.path = path;
        this.pos = pos;
        if (path.exists()) throw new FileAlreadyExistsException(path.toString());
    }
    // Load a metaFile in this path
    protected MetaFile(File path) throws IOException {
        this.path = path;
        if (!path.exists()) throw new FileNotFoundException("File not found at " + path);
        validateFile();
        metaData = readMeta(path);
        pos = metaData.pos;
    }

    protected void loadMetaData() throws IOException {
        validateFile();
        metaData = readMeta(path);
        if (!metaData.pos.equals(pos)) {
            LOGGER.warn("The file is from a different location than expected! Expects {} but got {}. Ignoring file tag.", pos, metaData.pos);
            metaData.pos = pos;
        }
    }

    protected void writeData(IOConsumer<OutputStream> dataWriter) throws IOException {
        LodUtil.assertTrue(metaData != null);
        if (path.exists()) validateFile();
        File writerFile;
        if (USE_ATOMIC_MOVE_REPLACE) {
            writerFile = new File(path.getPath() + ".tmp");
            writerFile.deleteOnExit();
        } else {
            writerFile = path;
        }

        try (FileChannel file = FileChannel.open(writerFile.toPath(),
                StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            {
                file.position(METADATA_SIZE);
                int checksum;
                try (OutputStream channelOut = new UnclosableOutputStream(Channels.newOutputStream(file)); // Prevent closing the channel
                     BufferedOutputStream bufferedOut = new BufferedOutputStream(channelOut); // TODO: Is default buffer size ok? Do we even need to buffer?
                     CheckedOutputStream checkedOut = new CheckedOutputStream(bufferedOut, new Adler32())) { // TODO: Is Adler32 ok?
                    dataWriter.accept(checkedOut);
                    checksum = (int) checkedOut.getChecksum().getValue();
                }
                file.position(0);
                // Write metadata
                ByteBuffer buff = ByteBuffer.allocate(METADATA_SIZE);
                buff.putInt(METADATA_MAGIC_BYTES);
                buff.putInt(pos.sectionX);
                buff.putInt(Integer.MIN_VALUE); // Unused
                buff.putInt(pos.sectionZ);
                buff.putInt(checksum);
                buff.put(pos.sectionDetail);
                buff.put(metaData.dataLevel);
                buff.put(metaData.loaderVersion);
                buff.put(Byte.MIN_VALUE); // Unused
                buff.putLong(metaData.dataTypeId);
                buff.putLong(metaData.dataVersion.get());
                LodUtil.assertTrue(buff.remaining() == METADATA_RESERVED_SIZE);
                buff.flip();
                file.write(buff);
            }
            file.close();
            if (USE_ATOMIC_MOVE_REPLACE) {
                // Atomic move / replace the actual file
                Files.move(writerFile.toPath(), path.toPath(), StandardCopyOption.ATOMIC_MOVE);
            }
        } finally {
            try {
                if (USE_ATOMIC_MOVE_REPLACE && writerFile.exists()) {
                    boolean i = writerFile.delete(); // Delete temp file. Ignore errors if fails.
                }
            } catch (Exception ignored) {}
        }
    }
}
