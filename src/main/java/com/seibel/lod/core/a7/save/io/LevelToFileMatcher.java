package com.seibel.lod.core.a7.save.io;

import com.seibel.lod.core.config.Config;
import com.seibel.lod.core.handlers.dependencyInjection.SingletonHandler;
import com.seibel.lod.core.handlers.dimensionFinder.PlayerData;
import com.seibel.lod.core.handlers.dimensionFinder.SubDimCompare;
import com.seibel.lod.core.logging.ConfigBasedLogger;
import com.seibel.lod.core.objects.DHChunkPos;
import com.seibel.lod.core.util.LodUtil;
import com.seibel.lod.core.wrapperInterfaces.chunk.IChunkWrapper;
import com.seibel.lod.core.wrapperInterfaces.minecraft.IMinecraftClientWrapper;
import com.seibel.lod.core.wrapperInterfaces.world.ILevelWrapper;
import org.apache.logging.log4j.LogManager;

import java.io.File;
import java.io.IOException;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

public class LevelToFileMatcher implements AutoCloseable {
    private static final IMinecraftClientWrapper MC_CLIENT = SingletonHandler.INSTANCE.get(IMinecraftClientWrapper.class);
    public static final ConfigBasedLogger LOGGER = new ConfigBasedLogger(LogManager.getLogger(),
            () -> Config.Client.Advanced.Debugging.DebugSwitch.logFileSubDimEvent.get());

    private final ExecutorService matcherThread = LodUtil.makeSingleThreadPool("Level-To-File-Matcher");

    private PlayerData playerData = null;
    private PlayerData firstSeenPlayerData = null;

    /** If true the LodDimensionFileHelper is attempting to determine the folder for this dimension */
    private final AtomicBoolean determiningWorldFolder = new AtomicBoolean(false);
    private final ILevelWrapper currentLevel;
    private volatile File foundLevel = null;
    private final File[] potentialFiles;
    private final File levelsFolder;

    public LevelToFileMatcher(ILevelWrapper targetWorld, File levelsFolder, File[] potentialFiles) {
        this.currentLevel = targetWorld;
        this.potentialFiles = potentialFiles;
        this.levelsFolder = levelsFolder;
        if (potentialFiles.length == 0) {
            String newId = UUID.randomUUID().toString();
            LOGGER.info("No potential level files found. Creating a new sub dimension with ID {}...",
                    LodUtil.shortenString(newId, 8));
            foundLevel = new File(levelsFolder, newId);
        }
    }

    // May return null, where at this moment the level is not yet known
    public File tryGetLevel() {
        tick();
        return foundLevel;
    }

    public boolean isFindingLevel(ILevelWrapper level) {
        return Objects.equals(level, currentLevel);
    }

    private void tick() {
        if (foundLevel != null) return;
        // prevent multiple threads running at the same time
        if (determiningWorldFolder.getAndSet(true)) return;
        matcherThread.submit(() ->
        {
            try {
                // attempt to get the file handler
                File saveDir = attemptToDetermineSubDimensionFolder();
                if (saveDir != null) foundLevel = saveDir;
            } catch (IOException e) {
                LOGGER.error("Unable to set the dimension file handler for level [" + currentLevel + "]. Error: ", e);
            } finally {
                // make sure we unlock this method
                determiningWorldFolder.set(false);
            }
        });
    }

    /**
     * Currently this method checks a single chunk (where the player is)
     * and compares it against the same chunk position in the other dimension worlds to
     * guess which world the player is in.
     *
     * @throws IOException if the folder doesn't exist or can't be accessed
     */
    public File attemptToDetermineSubDimensionFolder() throws IOException
    {
        { // Update PlayerData
            PlayerData data = PlayerData.tryGetPlayerData(MC_CLIENT);
            if (data != null) {
                if (firstSeenPlayerData == null) {
                    firstSeenPlayerData = data;
                }
                playerData = data;
            }
        }

        // relevant positions
        DHChunkPos playerChunkPos = new DHChunkPos(playerData.playerBlockPos);
        int startingBlockPosX = playerChunkPos.getMinBlockX();
        int startingBlockPosZ = playerChunkPos.getMinBlockZ();

        // chunk from the newly loaded level
        IChunkWrapper newlyLoadedChunk = MC_CLIENT.getWrappedClientWorld().tryGetChunk(playerChunkPos);
        // check if this chunk is valid to test
        if (!CanDetermineLevelFolder(newlyLoadedChunk))
            return null;

        //TODO: Compute a ChunkData from current chunk.
        /*
        // generate a LOD to test against
        boolean lodGenerated = InternalApiShared.lodBuilder.generateLodNodeFromChunk(newlyLoadedDim, newlyLoadedChunk, new LodBuilderConfig(EDistanceGenerationMode.FULL), true, true);
        if (!lodGenerated)
            return null;

        // log the start of this attempt
        LOGGER.info("Attempting to determine sub-dimension for [" + MC_CLIENT.getCurrentDimension().getDimensionName() + "]");
        LOGGER.info("Player block pos in dimension: [" + playerData.playerBlockPos.getX() + "," + playerData.playerBlockPos.getY() + "," + playerData.playerBlockPos.getZ() + "]");

        // new chunk data
        long[][][] newChunkData = new long[LodUtil.CHUNK_WIDTH][LodUtil.CHUNK_WIDTH][];
        for (int x = 0; x < LodUtil.CHUNK_WIDTH; x++)
        {
            for (int z = 0; z < LodUtil.CHUNK_WIDTH; z++)
            {
                long[] array = newlyLoadedDim.getRegion(playerRegionPos.x, playerRegionPos.z).getAllData(LodUtil.BLOCK_DETAIL_LEVEL, x + startingBlockPosX, z + startingBlockPosZ);
                newChunkData[x][z] = array;
            }
        }
        boolean newChunkHasData = !isDataEmpty(newChunkData);

        // check if the chunk is actually empty
        if (!newChunkHasData)
        {
            if (newlyLoadedChunk.getHeight() != 0)
            {
                // the chunk isn't empty but the LOD is...

                String message = "Error: the chunk at (" + playerChunkPos.getX() + "," + playerChunkPos.getZ() + ") has a height of [" + newlyLoadedChunk.getHeight() + "] but the LOD generated is empty!";
                LOGGER.error(message);
            }
            else
            {
                String message = "Warning: The chunk at (" + playerChunkPos.getX() + "," + playerChunkPos.getZ() + ") is empty.";
                LOGGER.warn(message);
            }
            return null;
        }*/


        // compare each world with the newly loaded one
        SubDimCompare mostSimilarSubDim = null;

        File[] levelFolders = potentialFiles;
        LOGGER.info("Potential Sub Dimension folders: [" + levelFolders.length + "]");
        for (File testLevelFolder : levelFolders)
        {
            LOGGER.info("Testing level folder: [" + LodUtil.shortenString(testLevelFolder.getName(), 8) + "]");
            try
            {
                // TODO: Try load a data file overlapping the playerChunkPos from ClientOnlySaveStructure,
                //  and then use it to compare chunk data to current chunk.

                /*
                // get a LOD from this dimension folder
                LodDimension tempLodDim = new LodDimension(null, 1, null, false);
                tempLodDim.move(playerRegionPos);
                LodDimensionFileHandler tempFileHandler = new LodDimensionFileHandler(testLevelFolder, tempLodDim);
                LodRegion testRegion = tempFileHandler.loadRegionFromFile(LodUtil.BLOCK_DETAIL_LEVEL, playerRegionPos, VERTICAL_QUALITY_TO_TEST_WITH);
                // get data from this LOD
                long[][][] testChunkData = new long[LodUtil.CHUNK_WIDTH][LodUtil.CHUNK_WIDTH][];
                for (int x = 0; x < LodUtil.CHUNK_WIDTH; x++)
                {
                    for (int z = 0; z < LodUtil.CHUNK_WIDTH; z++)
                    {
                        long[] array = testRegion.getAllData(LodUtil.BLOCK_DETAIL_LEVEL, x + startingBlockPosX, z + startingBlockPosZ);
                        testChunkData[x][z] = array;
                    }
                }

                // get the player data for this dimension folder
                PlayerData testPlayerData = new PlayerData(testLevelFolder);
                LOGGER.info("Last known player pos: [" + testPlayerData.playerBlockPos.getX() + "," + testPlayerData.playerBlockPos.getY() + "," + testPlayerData.playerBlockPos.getZ() + "]");

                // check if the block positions are close
                int playerBlockDist = testPlayerData.playerBlockPos.getManhattanDistance(playerData.playerBlockPos);
                LOGGER.info("Player block position distance between saved sub dimension and first seen is [" + playerBlockDist + "]");

                // check if the chunk is actually empty
                if (isDataEmpty(testChunkData))
                {
                    String message = "The test chunk for dimension folder [" + LodUtil.shortenString(testLevelFolder.getName(), 8) + "] and chunk pos (" + playerChunkPos.getX() + "," + playerChunkPos.getZ() + ") is empty. This is expected if the position is outside the sub-dimension's generated area.";
                    LOGGER.info(message);
                    continue;
                }

                // compare the two LODs
                int equalDataPoints = 0;
                int totalDataPointCount = 0;
                for (int x = 0; x < LodUtil.CHUNK_WIDTH; x++)
                {
                    for (int z = 0; z < LodUtil.CHUNK_WIDTH; z++)
                    {
                        for (int y = 0; y < newChunkData[x][z].length; y++)
                        {
                            if (newChunkData[x][z][y] == testChunkData[x][z][y])
                            {
                                equalDataPoints++;
                            }
                            totalDataPointCount++;

                            if (!DataPointUtil.doesItExist(newChunkData[x][z][y]) || !DataPointUtil.doesItExist(testChunkData[x][z][y]))
                                break;
                        }
                    }
                }

                // determine if this world is closer to the newly loaded world
                SubDimCompare subDimCompare = new SubDimCompare(equalDataPoints, totalDataPointCount, playerBlockDist, testLevelFolder);
                if (mostSimilarSubDim == null || subDimCompare.compareTo(mostSimilarSubDim) > 0)
                {
                    mostSimilarSubDim = subDimCompare;
                }

                LOGGER.info("Sub dimension [" + LodUtil.shortenString(testLevelFolder.getName(), 8) + "...] is current dimension probability: " + LodUtil.shortenString(subDimCompare.getPercentEqual() + "", 5) + " (" + equalDataPoints + "/" + totalDataPointCount + ")");
*/
            }
            catch (Exception e)
            {
                // this sub dimension isn't formatted correctly
                // for now we are just assuming it is an unrelated file
            }
        }

        // TODO if two sub dimensions contain the same LODs merge them???

        // the first seen player data is no longer needed, the sub dimension has been determined
        firstSeenPlayerData = null;

        if (mostSimilarSubDim != null && mostSimilarSubDim.isValidSubDim())
        {
            // we found a world folder that is similar, use it

            LOGGER.info("Sub Dimension set to: [" +  LodUtil.shortenString(mostSimilarSubDim.folder.getName(), 8) + "...] with an equality of [" + mostSimilarSubDim.getPercentEqual() + "]");
            return mostSimilarSubDim.folder;
        }
        else
        {
            // no world folder was found, create a new one

            double highestEqualityPercent = mostSimilarSubDim != null ? mostSimilarSubDim.getPercentEqual() : 0;

            String newId = UUID.randomUUID().toString();
            String message = "No suitable sub dimension found. The highest equality was [" + LodUtil.shortenString(highestEqualityPercent + "", 5) + "]. Creating a new sub dimension with ID: " + LodUtil.shortenString(newId, 8) + "...";
            LOGGER.info(message);
            File folder = new File(levelsFolder, newId);
            folder.mkdirs();
            return folder;
        }
    }

    /** Returns true if the given chunk is valid to test */
    public boolean CanDetermineLevelFolder(IChunkWrapper chunk)
    {
        // we can only guess if the given chunk can be converted into a LOD
        return false; //FIXME: Fix this after LodBUilder is done.
        //return LodBuilder.canGenerateLodFromChunk(chunk);
    }

    @Override
    public void close() {
        matcherThread.shutdownNow();
    }
}
