package com.seibel.lod.core.file.metaData;

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
 * 4 bytes: metadata identifier bytes: "DHv0" (in ascii: 0x44 48 76 30) this signals the file is in the metadata format <br>
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
public abstract class MetaDataFile
{
    private static final Logger LOGGER = DhLoggerBuilder.getLogger();
    
    public static final int METADATA_SIZE = 64;
    public static final int METADATA_RESERVED_SIZE = 24;
	/** equivalent to "DHv0" */
    public static final int METADATA_IDENTITY_BYTES = 0x44_48_76_30;
	
    /** 
	 * Currently set to false because for some reason 
	 * Window is throwing PermissionDeniedException when trying to atomic replace a file... 
	 */
    public static final boolean USE_ATOMIC_MOVE_REPLACE = false;
	
	
	public volatile MetaData metaData = null;
	/** also defined in {@link MetaDataFile#metaData} */
    public final DhSectionPos pos;
	
    public File path;
	
	
	
	//==============//
	// constructors //
	//==============//
	
	/** Create a metaFile in this path. If the path has a file, throws FileAlreadyExistsException */
	protected MetaDataFile(File path, DhSectionPos pos) throws IOException
	{
		this.path = path;
		this.pos = pos;
		if (path.exists())
		{
			throw new FileAlreadyExistsException(path.toString());
		}
	}
	
	/** 
	 * Creates a {@link MetaDataFile} with the file at the given path. 
	 * @throws IOException if the file was formatted incorrectly
	 * @throws FileNotFoundException if no file exists for the given path
	 */
	protected MetaDataFile(File path) throws IOException, FileNotFoundException
	{
		this.path = path;
		if (!path.exists())
		{
			throw new FileNotFoundException("File not found at [" + path + "]");
		}
		
		validateMetaDataFile(this.path);
		this.metaData = readMetaDataFromFile(path);
		this.pos = this.metaData.pos;
	}
	/**
	 * Attempts to create a new {@link MetaDataFile} from the given file. 
	 * @throws IOException if the file was formatted incorrectly
	 */
	private static MetaData readMetaDataFromFile(File file) throws IOException
	{
        try (FileChannel channel = FileChannel.open(file.toPath(), StandardOpenOption.READ))
		{
            ByteBuffer buffer = ByteBuffer.allocate(METADATA_SIZE);
            channel.read(buffer, 0);
            channel.close();
            buffer.flip();

            int idBytes = buffer.getInt();
            if (idBytes != METADATA_IDENTITY_BYTES)
			{
                throw new IOException("Invalid file format: Metadata Identity byte check failed. Expected: [" + METADATA_IDENTITY_BYTES + "], Actual: [" + idBytes + "].");
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
	
	
	
	//================//
	// helper methods //
	//================//
	
	/** Throws an {@link IOException} if the given file isn't valid */
	private static void validateMetaDataFile(File file) throws IOException
	{
		if (!file.exists()) throw new IOException("File missing");
		if (!file.isFile()) throw new IOException("Not a file");
		if (!file.canRead()) throw new IOException("File not readable");
		if (!file.canWrite()) throw new IOException("File not writable");
	}
	
	/** Sets this object's {@link MetaDataFile#metaData} using the set {@link MetaDataFile#path} */
	protected void loadMetaData() throws IOException
	{
		validateMetaDataFile(this.path);
		this.metaData = readMetaDataFromFile(this.path);
		if (!this.metaData.pos.equals(this.pos))
		{
			LOGGER.warn("The file is from a different location than expected! Expected: [{}] but got [{}]. Ignoring file tag.", this.pos, this.metaData.pos);
			this.metaData.pos = this.pos;
		}
	}
	
	protected void writeData(IMetaDataWriter<OutputStream> dataWriter) throws IOException
	{
		LodUtil.assertTrue(this.metaData != null);
		if (this.path.exists())
		{
			validateMetaDataFile(this.path);
		}
		
		File writerFile;
		if (USE_ATOMIC_MOVE_REPLACE)
		{
			writerFile = new File(this.path.getPath() + ".tmp");
			writerFile.deleteOnExit();
		}
		else
		{
			writerFile = this.path;
		}
		
		try (FileChannel file = FileChannel.open(writerFile.toPath(),
				StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING))
		{
			{
				file.position(METADATA_SIZE);
				int checksum;
				try (OutputStream channelOut = new UnclosableOutputStream(Channels.newOutputStream(file)); // Prevent closing the channel
						BufferedOutputStream bufferedOut = new BufferedOutputStream(channelOut); // TODO: Is default buffer size ok? Do we even need to buffer?
						CheckedOutputStream checkedOut = new CheckedOutputStream(bufferedOut, new Adler32()))
				{ // TODO: Is Adler32 ok?
					dataWriter.writeBufferToFile(checkedOut);
					checksum = (int) checkedOut.getChecksum().getValue();
				}
				file.position(0);
				// Write metadata
				ByteBuffer buff = ByteBuffer.allocate(METADATA_SIZE);
				buff.putInt(METADATA_IDENTITY_BYTES);
				buff.putInt(this.pos.sectionX);
				buff.putInt(Integer.MIN_VALUE); // Unused
				buff.putInt(this.pos.sectionZ);
				buff.putInt(checksum);
				buff.put(this.pos.sectionDetail);
				buff.put(this.metaData.dataLevel);
				buff.put(this.metaData.loaderVersion);
				buff.put(Byte.MIN_VALUE); // Unused
				buff.putLong(this.metaData.dataTypeId);
				buff.putLong(this.metaData.dataVersion.get());
				LodUtil.assertTrue(buff.remaining() == METADATA_RESERVED_SIZE);
				buff.flip();
				file.write(buff);
			}
			file.close();
			if (USE_ATOMIC_MOVE_REPLACE)
			{
				// Atomic move / replace the actual file
				Files.move(writerFile.toPath(), this.path.toPath(), StandardCopyOption.ATOMIC_MOVE);
			}
		}
		finally
		{
			try
			{
				if (USE_ATOMIC_MOVE_REPLACE && writerFile.exists())
				{
					boolean fileRemoved = writerFile.delete(); // Delete temp file. Ignore errors if it fails.
				}
			}
			catch (SecurityException ignored) { }
		}
	}
	
	
	
	//================//
	// helper classes //
	//================//
	
	@FunctionalInterface
	public interface IMetaDataWriter<T>
	{
		void writeBufferToFile(T t) throws IOException;
	}
	
}
