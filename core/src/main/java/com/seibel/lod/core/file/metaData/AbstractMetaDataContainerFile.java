package com.seibel.lod.core.file.metaData;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.zip.Adler32;
import java.util.zip.CheckedOutputStream;

import com.seibel.lod.core.pos.DhSectionPos;
import com.seibel.lod.core.util.objects.UnclosableOutputStream;
import com.seibel.lod.core.logging.DhLoggerBuilder;
import com.seibel.lod.core.util.LodUtil;
import org.apache.logging.log4j.Logger;

/**
 * This represents the data appended to any file we write. <br>
 * Contains a {@link BaseMetaData} which holds most of the necessary values written to the file. <br><br>
 * 
 * Used size: 40 bytes <br>
 * Remaining space: 24 bytes <br>
 * Total size: 64 bytes <br><br><br>
 * 
 * 
 * <Strong>Metadata format: </Strong><br><br>
 * <code>
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
 * </code>
 */
public abstract class AbstractMetaDataContainerFile
{
    private static final Logger LOGGER = DhLoggerBuilder.getLogger();
    
    public static final int METADATA_SIZE = 64;
    public static final int METADATA_RESERVED_SIZE = 24;
	/** equivalent to "DHv0" */
    public static final int METADATA_IDENTITY_BYTES = 0x44_48_76_30;
	
    /** 
	 * James tested this on windows (2023-02-18) and didn't have any issues,
	 * so it will be turned on for now. If there turns out to be issues 
	 * we can always 
	 * Currently set to false because for some reason 
	 * Window is throwing PermissionDeniedException when trying to atomic replace a file... 
	 */
    public static final boolean USE_ATOMIC_MOVE_REPLACE = true;
	
	
	/** 
	 * Will be null if no file exists for this object. <br>
	 * NOTE: Only use {@link BaseMetaData#pos} when initially setting up this object, afterwards the standalone {@link AbstractMetaDataContainerFile#pos} should be used. 
	 */
	public volatile BaseMetaData metaData = null;
	
	/** Should be used instead of the position inside {@link AbstractMetaDataContainerFile#metaData} */
    public final DhSectionPos pos;
	
    public File file;
	
	
	
	//==============//
	// constructors //
	//==============//
	
	/** Create a metaFile in this path. If the path has a file, throws FileAlreadyExistsException */
	protected AbstractMetaDataContainerFile(File file, DhSectionPos pos) throws IOException
	{
		this.file = file;
		this.pos = pos;
		if (file.exists())
		{
			throw new FileAlreadyExistsException(file.toString());
		}
	}
	
	/** 
	 * Creates a {@link AbstractMetaDataContainerFile} with the file at the given path. 
	 * @throws IOException if the file was formatted incorrectly
	 * @throws FileNotFoundException if no file exists for the given path
	 */
	protected AbstractMetaDataContainerFile(File file) throws IOException, FileNotFoundException
	{
		this.file = file;
		if (!file.exists())
		{
			throw new FileNotFoundException("File not found at ["+ file +"]");
		}
		
		validateMetaDataFile(this.file);
		this.metaData = readMetaDataFromFile(file);
		this.pos = this.metaData.pos;
	}
	/**
	 * Attempts to create a new {@link AbstractMetaDataContainerFile} from the given file. 
	 * @throws IOException if the file was formatted incorrectly
	 */
	private static BaseMetaData readMetaDataFromFile(File file) throws IOException
	{
        try (FileChannel channel = FileChannel.open(file.toPath(), StandardOpenOption.READ))
		{
            ByteBuffer byteBuffer = ByteBuffer.allocate(METADATA_SIZE);
            channel.read(byteBuffer, 0);
            channel.close();
            byteBuffer.flip();

            int idBytes = byteBuffer.getInt();
            if (idBytes != METADATA_IDENTITY_BYTES)
			{
                throw new IOException("Invalid file format: Metadata Identity byte check failed. Expected: ["+METADATA_IDENTITY_BYTES+"], Actual: ["+idBytes+"].");
            }
			
            int x = byteBuffer.getInt();
            int y = byteBuffer.getInt(); // Unused
            int z = byteBuffer.getInt();
            int checksum = byteBuffer.getInt();
            byte detailLevel = byteBuffer.get();
            byte dataLevel = byteBuffer.get();
            byte loaderVersion = byteBuffer.get();
            byte unused = byteBuffer.get();
            long dataTypeId = byteBuffer.getLong();
            long unusedTimestamp = byteBuffer.getLong(); // not currently implemented
            LodUtil.assertTrue(byteBuffer.remaining() == METADATA_RESERVED_SIZE);
            DhSectionPos dataPos = new DhSectionPos(detailLevel, x, z);
			
            return new BaseMetaData(dataPos, checksum, dataLevel, dataTypeId, loaderVersion);
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
	
	/** Sets this object's {@link AbstractMetaDataContainerFile#metaData} using the set {@link AbstractMetaDataContainerFile#file} */
	protected void loadMetaData() throws IOException
	{
		validateMetaDataFile(this.file);
		this.metaData = readMetaDataFromFile(this.file);
		if (!this.metaData.pos.equals(this.pos))
		{
			LOGGER.warn("The file is from a different location than expected! Expected: ["+this.pos+"] but got ["+this.metaData.pos+"]. Ignoring file tag.");
			this.metaData.pos = this.pos;
		}
	}
	
	protected void writeData(IMetaDataWriter<OutputStream> dataWriter) throws IOException
	{
		LodUtil.assertTrue(this.metaData != null);
		if (this.file.exists())
		{
			validateMetaDataFile(this.file);
		}
		
		File tempFile;
		if (USE_ATOMIC_MOVE_REPLACE)
		{
			tempFile = new File(this.file.getPath() + ".tmp");
			tempFile.deleteOnExit();
		}
		else
		{
			tempFile = this.file;
		}
		
		try (FileChannel file = FileChannel.open(tempFile.toPath(),
				StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING))
		{
			{
				file.position(METADATA_SIZE);
				int checksum;
				try (OutputStream channelOut = new UnclosableOutputStream(Channels.newOutputStream(file)); // Prevent closing the channel
						BufferedOutputStream bufferedOut = new BufferedOutputStream(channelOut); // TODO: Is default buffer size ok? Do we even need to buffer?
						CheckedOutputStream checkedOut = new CheckedOutputStream(bufferedOut, new Adler32()))
				{ 
					// TODO: Is Adler32 ok?
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
				buff.put(this.pos.sectionDetailLevel);
				buff.put(this.metaData.dataLevel);
				buff.put(this.metaData.loaderVersion);
				buff.put(Byte.MIN_VALUE); // Unused
				buff.putLong(this.metaData.dataTypeId);
				buff.putLong(Long.MAX_VALUE); //buff.putLong(this.metaData.dataVersion.get()); // not currently implemented
				LodUtil.assertTrue(buff.remaining() == METADATA_RESERVED_SIZE);
				buff.flip();
				file.write(buff);
			}
			
			file.close();
			if (USE_ATOMIC_MOVE_REPLACE)
			{
				// Atomic move / replace the actual file
				Files.move(tempFile.toPath(), this.file.toPath(), StandardCopyOption.ATOMIC_MOVE); // TODO couldn't StandardCopyOption.REPLACE_EXISTING also work here?
			}
		}
		finally
		{
			String tempDeleteErrorMessage = null;
			try
			{
				// Delete temp file if it exists (this generally means there was an issue saving)
				if (USE_ATOMIC_MOVE_REPLACE && tempFile.exists())
				{
					boolean fileRemoved = tempFile.delete();
					if (!fileRemoved)
					{
						tempDeleteErrorMessage = "Unable to remove Temporary file at: "+tempFile.getPath();
					}
				}
			}
			catch (SecurityException exception) 
			{
				tempDeleteErrorMessage = "Security error: ["+exception.getMessage()+"] when attempting to remove Temporary file at: "+tempFile.getPath();
			}
			
			if (tempDeleteErrorMessage != null)
			{
				LOGGER.error(tempDeleteErrorMessage);
			}
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
