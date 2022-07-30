package com.seibel.lod.core.a7.save.io;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;
import java.util.zip.Adler32;
import java.util.zip.CheckedOutputStream;

import com.seibel.lod.core.a7.pos.DhSectionPos;
import com.seibel.lod.core.a7.util.UnclosableOutputStream;
import com.seibel.lod.core.logging.DhLoggerBuilder;
import com.seibel.lod.core.util.LodUtil;
import org.apache.logging.log4j.Logger;

public class MetaFile {
    public static final Logger LOGGER = DhLoggerBuilder.getLogger("FileMetadata");
    //Metadata format:
    //
    //    4 bytes: magic bytes: "DHv0" (in ascii: 0x44 48 76 30) (this also signal the metadata format)
    //    4 bytes: section X position
    //    4 bytes: section Y position (Unused, for future proofing)
    //    4 bytes: section Z position
    //
    //    4 bytes: data checksum //TODO: Implement checksum
    //    1 byte: section detail level
    //    1 byte: data detail level // Note: not sure if this is needed
    //    1 byte: loader version
    //    1 byte: unused
    //
    //    8 bytes: datatype identifier
    //
    //    8 bytes: timestamp

    // Used size: 40 bytes
    // Remaining space: 24 bytes
    // Total size: 64 bytes

    public static final int METADATA_SIZE = 64;
    public static final int METADATA_RESERVED_SIZE = 24;
    public static final int METADATA_MAGIC_BYTES = 0x44_48_76_30;

    // Currently set to false because for some reason Window is throwing PermissionDeniedException when trying to atomic replace a file...
    public static final boolean USE_ATOMIC_MOVE_REPLACE = false;

    public final DhSectionPos pos;

    public File path;
    public int checksum;
    public long timestamp;
    public byte dataLevel;

    //Loader stuff
    public long dataTypeId;
    public byte loaderVersion;

    private final ReentrantReadWriteLock assertLock = new ReentrantReadWriteLock();

    // Load a metaFile in this path. It also automatically read the metadata.
    protected MetaFile(File path) throws IOException {
        this.path = path;
        validateFile();
        LodUtil.assertTrue(assertLock.readLock().tryLock());
        try (FileChannel channel = FileChannel.open(path.toPath(), StandardOpenOption.READ)) {
            MappedByteBuffer buffer = channel.map(FileChannel.MapMode.READ_ONLY, 0, METADATA_SIZE);
            this.path = path;

            int magic = buffer.getInt();
            if (magic != METADATA_MAGIC_BYTES) {
                throw new IOException("Invalid file: Magic bytes check failed.");
            }
            int x = buffer.getInt();
            int y = buffer.getInt(); // Unused
            int z = buffer.getInt();
            checksum = buffer.getInt();
            byte detailLevel = buffer.get();
            dataLevel = buffer.get();
            loaderVersion = buffer.get();
            byte unused = buffer.get();
            dataTypeId = buffer.getLong();
            timestamp = buffer.getLong();
            LodUtil.assertTrue(buffer.remaining() == METADATA_RESERVED_SIZE);
            pos = new DhSectionPos(detailLevel, x, z);
        } finally {
            assertLock.readLock().unlock();
        }
    }

    // Make a new MetaFile. It doesn't load or write any metadata itself.
    protected MetaFile(File path, DhSectionPos pos) {
        this.path = path;
        this.pos = pos;
    }

    private void validateFile() throws IOException {
        if (!path.exists()) throw new IOException("File missing");
        if (!path.isFile()) throw new IOException("Not a file");
        if (!path.canRead()) throw new IOException("File not readable");
        if (!path.canWrite()) throw new IOException("File not writable");
    }

    protected void updateMetaData() throws IOException {
        validateFile();
        LodUtil.assertTrue(assertLock.readLock().tryLock());
        try (FileChannel channel = FileChannel.open(path.toPath(), StandardOpenOption.READ)) {
            MappedByteBuffer buffer = channel.map(FileChannel.MapMode.READ_ONLY, 0, METADATA_SIZE);
            int magic = buffer.getInt();
            if (magic != METADATA_MAGIC_BYTES) {
                throw new IOException("Invalid file: Magic bytes check failed.");
            }
            int x = buffer.getInt();
            int y = buffer.getInt(); // Unused
            int z = buffer.getInt();
            checksum = buffer.getInt();
            byte detailLevel = buffer.get();
            dataLevel = buffer.get();
            byte loaderVersion = buffer.get();
            byte unused = buffer.get();
            dataTypeId = buffer.getLong();
            timestamp = buffer.getLong();
            LodUtil.assertTrue(buffer.remaining() == METADATA_RESERVED_SIZE);

            DhSectionPos newPos = new DhSectionPos(detailLevel, x, z);
            if (!newPos.equals(pos)) {
                throw new IOException("Invalid file: Section position changed.");
            }
            this.loaderVersion = loaderVersion;
        } finally {
            assertLock.readLock().unlock();
        }
    }

    protected void writeData(Consumer<OutputStream> dataWriter) throws IOException {
        if (path.exists()) validateFile();
        File writerFile;
        if (USE_ATOMIC_MOVE_REPLACE) {
            writerFile = new File(path.getPath() + ".tmp");
            writerFile.deleteOnExit();
        } else {
            writerFile = path;
        }
        LodUtil.assertTrue(assertLock.writeLock().tryLock());
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
                buff.put(dataLevel);
                buff.put(loaderVersion);
                buff.put(Byte.MIN_VALUE); // Unused
                buff.putLong(dataTypeId);
                buff.putLong(timestamp);
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
            assertLock.writeLock().unlock();
            try {
                if (USE_ATOMIC_MOVE_REPLACE && writerFile.exists()) {
                    boolean i = writerFile.delete(); // Delete temp file. Ignore errors if fails.
                }
            } catch (Exception ignored) {}
        }
    }
}
