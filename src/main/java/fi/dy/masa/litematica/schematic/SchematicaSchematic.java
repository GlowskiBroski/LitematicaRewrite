package fi.dy.masa.litematica.schematic;

import com.glow.litematicarecode.LitematicaRewrite;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.schematic.LitematicaSchematic.EntityInfo;
import fi.dy.masa.litematica.schematic.container.LitematicaBlockStateContainer;
import fi.dy.masa.litematica.schematic.conversion.SchematicConversionFixers.IStateFixer;
import fi.dy.masa.litematica.schematic.conversion.SchematicConversionMaps;
import fi.dy.masa.litematica.schematic.conversion.SchematicConverter;
import fi.dy.masa.litematica.util.DataFixerMode;
import fi.dy.masa.litematica.util.EntityUtils;
import fi.dy.masa.litematica.util.NbtUtils;
import fi.dy.masa.litematica.util.PositionUtils;
import fi.dy.masa.malilib.gui.Message.MessageType;
import fi.dy.masa.malilib.util.Constants;
import fi.dy.masa.malilib.util.InfoUtils;
import fi.dy.masa.malilib.util.NBTUtils;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.StructureMode;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.Vec3;
import java.io.File;
import java.util.*;

import static com.glow.litematicarecode.LitematicaRewrite.MC;

public class SchematicaSchematic {
    public static final String FILE_EXTENSION = ".schematic";

    private final SchematicConverter converter;
    private final BlockState[] palette = new BlockState[65536];
    private LitematicaBlockStateContainer blocks;
    private final Map<BlockPos, CompoundTag> tiles = new HashMap<>();
    private final List<CompoundTag> entities = new ArrayList<>();
    private Vec3i size = Vec3i.ZERO;
    private String fileName;
    private IdentityHashMap<BlockState, IStateFixer> postProcessingFilter;
    private boolean needsConversionPostProcessing;

    private SchematicaSchematic() {
        this.converter = SchematicConverter.createForSchematica();
    }

    public static SchematicaSchematic createFromWorld(Level world, BlockPos posStart, BlockPos size, boolean ignoreEntities) {
        SchematicaSchematic schematic = new SchematicaSchematic();

        schematic.readBlocksFromWorld(world, posStart, size);

        if (!ignoreEntities) {
            schematic.readEntitiesFromWorld(world, posStart, size);
        }

        return schematic;
    }

    @Nullable
    public static SchematicaSchematic createFromFile(File file) {
        SchematicaSchematic schematic = new SchematicaSchematic();

        if (schematic.readFromFile(file)) {
            return schematic;
        }

        return null;
    }

    public Vec3i getSize() {
        return this.size;
    }

    public Map<BlockPos, CompoundTag> getTiles() {
        return this.tiles;
    }

    public List<EntityInfo> getEntities() {
        List<EntityInfo> entityList = new ArrayList<>();
        final int size = this.entities.size();

        for (int i = 0; i < size; ++i) {
            CompoundTag entityData = this.entities.get(i);
            Vec3 posVec = NBTUtils.readEntityPositionFromTag(entityData);

            if (posVec != null && !entityData.isEmpty()) {
                entityList.add(new EntityInfo(posVec, entityData));
            }
        }

        return entityList;
    }

    public void placeSchematicToWorld(Level world, BlockPos posStart, StructurePlaceSettings placement, int setBlockStateFlags) {
        final int width = this.size.getX();
        final int height = this.size.getY();
        final int length = this.size.getZ();
        final int numBlocks = width * height * length;

        if (this.blocks != null && numBlocks > 0 && this.blocks.getSize().equals(this.size)) {
            final Rotation rotation = placement.getRotation();
            final Mirror mirror = placement.getMirror();

            // Place blocks and read any TileEntity data
            for (int y = 0; y < height; ++y) {
                for (int z = 0; z < length; ++z) {
                    for (int x = 0; x < width; ++x) {
                        BlockState state = this.blocks.get(x, y, z);
                        BlockPos pos = new BlockPos(x, y, z);
                        CompoundTag teNBT = this.tiles.get(pos);

                        pos = StructureTemplate.calculateRelativePosition(placement, pos).offset(posStart);

                        state = state.mirror(mirror);
                        state = state.rotate(rotation);

                        if (teNBT != null) {
                            BlockEntity te = world.getBlockEntity(pos);

                            if (te != null) {
                                if (te instanceof Container) {
                                    ((Container) te).clearContent();
                                }

                                world.setBlock(pos, Blocks.BARRIER.defaultBlockState(), 0x14);
                            }
                        }

                        if (world.setBlock(pos, state, setBlockStateFlags) && teNBT != null) {
                            BlockEntity te = world.getBlockEntity(pos);

                            if (te != null) {
                                teNBT.putInt("x", pos.getX());
                                teNBT.putInt("y", pos.getY());
                                teNBT.putInt("z", pos.getZ());

                                try {
                                    te.loadWithComponents(teNBT, world.registryAccess());
                                } catch (Exception e) {
                                    LitematicaRewrite.LOGGER.warn("Failed to load TileEntity data for {} @ {}", state, pos);
                                }
                            }
                        }
                    }
                }
            }

            if ((setBlockStateFlags & 0x01) != 0) {
                // Update blocks
                for (int y = 0; y < height; ++y) {
                    for (int z = 0; z < length; ++z) {
                        for (int x = 0; x < width; ++x) {
                            BlockPos pos = new BlockPos(x, y, z);
                            CompoundTag teNBT = this.tiles.get(pos);

                            pos = StructureTemplate.calculateRelativePosition(placement, pos).offset(posStart);
                            world.blockUpdated(pos, world.getBlockState(pos).getBlock());

                            if (teNBT != null) {
                                BlockEntity te = world.getBlockEntity(pos);

                                if (te != null) {
                                    te.setChanged();
                                }
                            }
                        }
                    }
                }
            }

            if (!placement.isIgnoreEntities()) {
                this.addEntitiesToWorld(world, posStart, placement);
            }
        }
    }

    public void placeSchematicDirectlyToChunks(Level world, BlockPos posStart, StructurePlaceSettings placement) {
        final int width = this.size.getX();
        final int height = this.size.getY();
        final int length = this.size.getZ();
        final int numBlocks = width * height * length;
        BlockPos posEnd = posStart.offset(this.size).offset(-1, -1, -1);
        //BlockPos posEnd = Template.transformedBlockPos(placement, (new BlockPos(this.size)).add(-1, -1, -1)).add(posStart);

        if (this.blocks != null && numBlocks > 0 && this.blocks.getSize().equals(this.size) && PositionUtils.arePositionsWithinWorld(world, posStart, posEnd)) {
            final BlockPos posMin = PositionUtils.getMinCorner(posStart, posEnd);
            final BlockPos posMax = PositionUtils.getMaxCorner(posStart, posEnd);
            final int cxStart = posMin.getX() >> 4;
            final int czStart = posMin.getZ() >> 4;
            final int cxEnd = posMax.getX() >> 4;
            final int czEnd = posMax.getZ() >> 4;
            BlockPos.MutableBlockPos posMutable = new BlockPos.MutableBlockPos();

            for (int cz = czStart; cz <= czEnd; ++cz) {
                for (int cx = cxStart; cx <= cxEnd; ++cx) {
                    final int xMinChunk = Math.max(cx << 4, posMin.getX());
                    final int zMinChunk = Math.max(cz << 4, posMin.getZ());
                    final int xMaxChunk = Math.min((cx << 4) + 15, posMax.getX());
                    final int zMaxChunk = Math.min((cz << 4) + 15, posMax.getZ());
                    LevelChunk chunk = world.getChunk(cx, cz);

                    if (chunk == null) {
                        continue;
                    }

                    for (int y = posMin.getY(), ySrc = 0; ySrc < height; ++y, ++ySrc) {
                        for (int z = zMinChunk, zSrc = zMinChunk - posStart.getZ(); z <= zMaxChunk; ++z, ++zSrc) {
                            for (int x = xMinChunk, xSrc = xMinChunk - posStart.getX(); x <= xMaxChunk; ++x, ++xSrc) {
                                BlockState state = this.blocks.get(xSrc, ySrc, zSrc);

                                posMutable.set(xSrc, ySrc, zSrc);
                                CompoundTag teNBT = this.tiles.get(posMutable);

                                // TODO The rotations need to be transformed back to get the correct source position in the schematic...
                                /*
                                pos = Template.transformedBlockPos(placement, pos).add(posStart);

                                state = state.withMirror(mirror);
                                state = state.withRotation(rotation);
                                */

                                BlockPos pos = new BlockPos(x, y, z);

                                if (teNBT != null) {
                                    BlockEntity te = chunk.getBlockEntity(pos, LevelChunk.EntityCreationType.CHECK);

                                    if (te != null) {
                                        if (te instanceof Container) {
                                            ((Container) te).clearContent();
                                        }

                                        world.setBlock(pos, Blocks.BARRIER.defaultBlockState(), 0x14);
                                    }
                                }

                                chunk.setBlockState(pos, state, false);

                                if (teNBT != null) {
                                    BlockEntity te = chunk.getBlockEntity(pos, LevelChunk.EntityCreationType.CHECK);

                                    if (te != null) {
                                        teNBT.putInt("x", pos.getX());
                                        teNBT.putInt("y", pos.getY());
                                        teNBT.putInt("z", pos.getZ());

                                        try {
                                            te.loadWithComponents(teNBT, world.registryAccess());
                                        } catch (Exception e) {
                                            LitematicaRewrite.LOGGER.warn("Failed to load TileEntity data for {} @ {}", state, pos);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (!placement.isIgnoreEntities()) {
                this.addEntitiesToWorld(world, posStart, placement);
            }
        }
    }

    private void addEntitiesToWorld(Level world, BlockPos posStart, StructurePlaceSettings placement) {
        Mirror mirror = placement.getMirror();
        Rotation rotation = placement.getRotation();

        for (CompoundTag tag : this.entities) {
            Vec3 relativePos = NBTUtils.readEntityPositionFromTag(tag);
            Vec3 transformedRelativePos = PositionUtils.getTransformedPosition(relativePos, mirror, rotation);
            Vec3 realPos = transformedRelativePos.add(posStart.getX(), posStart.getY(), posStart.getZ());
            Entity entity = EntityUtils.createEntityAndPassengersFromNBT(tag, world);

            if (entity != null) {
                float rotationYaw = entity.mirror(mirror);
                rotationYaw = rotationYaw + (entity.getYRot() - entity.rotate(rotation));
                entity.moveTo(realPos.x, realPos.y, realPos.z, rotationYaw, entity.getXRot());
                EntityUtils.spawnEntityAndPassengersInWorld(entity, world);
            }
        }
    }

    public Map<BlockPos, String> getDataStructureBlocks(BlockPos posStart, StructurePlaceSettings placement) {
        Map<BlockPos, String> map = new HashMap<>();

        for (Map.Entry<BlockPos, CompoundTag> entry : this.tiles.entrySet()) {
            CompoundTag tag = entry.getValue();

            if (tag.getString("id").equals("minecraft:structure_block") && StructureMode.valueOf(tag.getString("mode")) == StructureMode.DATA) {
                BlockPos pos = entry.getKey();
                pos = StructureTemplate.calculateRelativePosition(placement, pos).offset(posStart);
                map.put(pos, tag.getString("metadata"));
            }
        }

        return map;
    }

    private void readBlocksFromWorld(Level world, BlockPos posStart, BlockPos size) {
        final int startX = posStart.getX();
        final int startY = posStart.getY();
        final int startZ = posStart.getZ();
        final int endX = startX + size.getX();
        final int endY = startY + size.getY();
        final int endZ = startZ + size.getZ();
        BlockPos.MutableBlockPos posMutable = new BlockPos.MutableBlockPos(0, 0, 0);

        this.blocks = new LitematicaBlockStateContainer(size.getX(), size.getY(), size.getZ());
        this.tiles.clear();
        this.size = size;

        for (int y = startY; y < endY; ++y) {
            for (int z = startZ; z < endZ; ++z) {
                for (int x = startX; x < endX; ++x) {
                    int relX = x - startX;
                    int relY = y - startY;
                    int relZ = z - startZ;

                    posMutable.set(x, y, z);
                    BlockState state = world.getBlockState(posMutable);
                    this.blocks.set(relX, relY, relZ, state);

                    BlockEntity te = world.getBlockEntity(posMutable);

                    if (te != null) {
                        try {
                            CompoundTag nbt = te.saveWithId(world.registryAccess());
                            BlockPos pos = new BlockPos(relX, relY, relZ);
                            NBTUtils.writeBlockPosToTag(pos, nbt);

                            this.tiles.put(pos, nbt);
                        } catch (Exception e) {
                            LitematicaRewrite.LOGGER.warn("SchematicaSchematic: Exception while trying to store TileEntity data for block '{}' at {}", state, posMutable.toString(), e);
                        }
                    }
                }
            }
        }
    }

    private void readEntitiesFromWorld(Level world, BlockPos posStart, BlockPos size) {
        this.entities.clear();
        //List<Entity> entities = world.getEntities(null, PositionUtils.createEnclosingAABB(posStart, posStart.offset(size)), (e) -> !(e instanceof Player));
        List<Entity> entities = world.getEntities(MC.player, PositionUtils.createEnclosingAABB(posStart, posStart.offset(size)), (e) -> !(e instanceof Player));

        for (Entity entity : entities) {
            CompoundTag tag = new CompoundTag();

            if (entity.save(tag)) {
                Vec3 pos = new Vec3(entity.getX() - posStart.getX(), entity.getY() - posStart.getY(), entity.getZ() - posStart.getZ());
                NBTUtils.writeEntityPositionToTag(pos, tag);

                this.entities.add(tag);
            }
        }
    }

    public boolean readFromNBT(CompoundTag nbt) {
        if (this.readBlocksFromNBT(nbt)) {
            this.readEntitiesFromNBT(nbt);
            this.readTileEntitiesFromNBT(nbt);

            try {
                this.postProcessBlocks();
            } catch (Exception e) {
                LitematicaRewrite.LOGGER.error("SchematicaSchematic: Exception while post-processing blocks for '{}'", this.fileName, e);
            }

            return true;
        } else {
            LitematicaRewrite.LOGGER.error("SchematicaSchematic: Missing block data in the schematic '{}'", this.fileName);
            return false;
        }
    }

    private boolean readPaletteFromNBT(CompoundTag nbt) {
        Arrays.fill(this.palette, Blocks.AIR.defaultBlockState());

        // Schematica palette
        if (nbt.contains("SchematicaMapping", Constants.NBT.TAG_COMPOUND)) {
            CompoundTag tag = nbt.getCompound("SchematicaMapping");
            Set<String> keys = tag.getAllKeys();

            for (String key : keys) {
                int id = tag.getShort(key);

                if (id < 0 || id >= 4096) {
                    String str = String.format("SchematicaSchematic: Invalid ID '%d' in SchematicaMapping for block '%s', range: 0 - 4095", id, key);
                    InfoUtils.showGuiOrInGameMessage(MessageType.ERROR, str);
                    LitematicaRewrite.LOGGER.warn(str);
                    return false;
                }

                if (!this.converter.getConvertedStatesForBlock(id, key, this.palette)) {
                    String str = String.format("SchematicaSchematic: Missing/non-existing block '%s' in SchematicaMapping", key);
                    InfoUtils.showGuiOrInGameMessage(MessageType.ERROR, str);
                    LitematicaRewrite.LOGGER.warn(str);
                }
            }
        }
        // MCEdit2 palette
        else if (nbt.contains("BlockIDs", Constants.NBT.TAG_COMPOUND)) {
            CompoundTag tag = nbt.getCompound("BlockIDs");
            Set<String> keys = tag.getAllKeys();

            for (String idStr : keys) {
                String key = tag.getString(idStr);
                int id;

                try {
                    id = Integer.parseInt(idStr);
                } catch (NumberFormatException e) {
                    String str = String.format("SchematicaSchematic: Invalid ID '%d' (not a number) in MCEdit2 palette for block '%s'", idStr, key);
                    InfoUtils.showGuiOrInGameMessage(MessageType.ERROR, str);
                    LitematicaRewrite.LOGGER.warn(str);
                    return false;
                }

                if (id < 0 || id >= 4096) {
                    String str = String.format("SchematicaSchematic: Invalid ID '%d' in MCEdit2 palette for block '%s', range: 0 - 4095", id, key);
                    InfoUtils.showGuiOrInGameMessage(MessageType.ERROR, str);
                    LitematicaRewrite.LOGGER.warn(str);
                    return false;
                }

                if (!this.converter.getConvertedStatesForBlock(id, key, this.palette)) {
                    String str = String.format("SchematicaSchematic: Missing/non-existing block '%s' in MCEdit2 palette", key);
                    InfoUtils.showGuiOrInGameMessage(MessageType.ERROR, str);
                    LitematicaRewrite.LOGGER.warn(str);
                }
            }
        }
        // No palette, use old vanilla IDs
        else {
            this.converter.getVanillaBlockPalette(this.palette);
        }

        if (this.converter.createPostProcessStateFilter(this.palette)) {
            this.postProcessingFilter = this.converter.getPostProcessStateFilter();
            this.needsConversionPostProcessing = true;
        }

        return true;
    }

    private boolean readBlocksFromNBT(CompoundTag nbt) {
        if (!nbt.contains("Blocks", Constants.NBT.TAG_BYTE_ARRAY) || !nbt.contains("Data", Constants.NBT.TAG_BYTE_ARRAY) || !nbt.contains("Width", Constants.NBT.TAG_SHORT) || !nbt.contains("Height", Constants.NBT.TAG_SHORT) || !nbt.contains("Length", Constants.NBT.TAG_SHORT)) {
            return false;
        }

        // This method was implemented based on
        // https://minecraft.gamepedia.com/Schematic_file_format
        // as it was on 2018-04-18.

        final int sizeX = nbt.getShort("Width");
        final int sizeY = nbt.getShort("Height");
        final int sizeZ = nbt.getShort("Length");
        final byte[] blockIdsByte = nbt.getByteArray("Blocks");
        final byte[] metaArr = nbt.getByteArray("Data");
        final int numBlocks = blockIdsByte.length;
        final int layerSize = sizeX * sizeZ;

        if (numBlocks != (sizeX * sizeY * sizeZ)) {
            String str = String.format("SchematicaSchematic: Mismatched block array size compared to the width/height/length,\nblocks: %d, W x H x L: %d x %d x %d", numBlocks, sizeX, sizeY, sizeZ);
            InfoUtils.showGuiOrInGameMessage(MessageType.ERROR, str);
            return false;
        }

        if (numBlocks != metaArr.length) {
            String str = String.format("SchematicaSchematic: Mismatched block ID and metadata array sizes, blocks: %d, meta: %d", numBlocks, metaArr.length);
            InfoUtils.showGuiOrInGameMessage(MessageType.ERROR, str);
            return false;
        }

        if (!this.readPaletteFromNBT(nbt)) {
            InfoUtils.showGuiOrInGameMessage(MessageType.ERROR, "SchematicaSchematic: Failed to read the block palette");
            return false;
        }

        this.size = new Vec3i(sizeX, sizeY, sizeZ);
        this.blocks = new LitematicaBlockStateContainer(sizeX, sizeY, sizeZ);

        // Old Schematica format
        if (nbt.contains("Add", Constants.NBT.TAG_BYTE_ARRAY)) {
            // FIXME is this array 4 or 8 bits per block?
            InfoUtils.showGuiOrInGameMessage(MessageType.ERROR, "SchematicaSchematic: Old Schematica format detected, not currently implemented...");
            return false;
        }

        byte[] add = null;

        if (nbt.contains("AddBlocks", Constants.NBT.TAG_BYTE_ARRAY)) {
            add = nbt.getByteArray("AddBlocks");
            final int expectedAddLength = (int) Math.ceil((double) blockIdsByte.length / 2D);

            if (add.length != expectedAddLength) {
                String str = String.format("SchematicaSchematic: Add array size mismatch, blocks: %d, add: %d, expected add: %d", numBlocks, add.length, expectedAddLength);

                if (add.length < expectedAddLength) {
                    InfoUtils.showGuiOrInGameMessage(MessageType.ERROR, str);
                    return false;
                } else {
                    InfoUtils.showGuiOrInGameMessage(MessageType.WARNING, str);
                }
            }
        }

        final int loopMax;

        // Even number of blocks, we can handle two position (meaning one full add byte) at a time
        if ((numBlocks % 2) == 0) {
            loopMax = numBlocks - 1;
        } else {
            loopMax = numBlocks - 2;
        }

        int byteId;
        int bi, ai;
        BlockState state;

        // Handle two positions per iteration, ie. one full byte of the add array
        for (bi = 0, ai = 0; bi < loopMax; bi += 2, ai++) {
            final int addValue = add != null ? add[ai] : 0;

            byteId = blockIdsByte[bi] & 0xFF;
            state = this.palette[((addValue & 0xF0) << 8) | (byteId << 4) | metaArr[bi]];
            int x = bi % sizeX;
            int y = bi / layerSize;
            int z = (bi % layerSize) / sizeX;
            this.blocks.set(x, y, z, state);

            x = (bi + 1) % sizeX;
            y = (bi + 1) / layerSize;
            z = ((bi + 1) % layerSize) / sizeX;
            byteId = blockIdsByte[bi + 1] & 0xFF;
            state = this.palette[((addValue & 0x0F) << 12) | (byteId << 4) | metaArr[bi + 1]];
            this.blocks.set(x, y, z, state);
        }

        // Odd number of blocks, handle the last position
        if ((numBlocks % 2) != 0) {
            final int addValue = add != null ? add[ai] : 0;
            byteId = blockIdsByte[bi] & 0xFF;
            state = this.palette[((addValue & 0xF0) << 8) | (byteId << 4) | metaArr[bi]];
            int x = bi % sizeX;
            int y = bi / layerSize;
            int z = (bi % layerSize) / sizeX;
            this.blocks.set(x, y, z, state);
        }

        return true;
    }

    private void postProcessBlocks() {
        if (this.needsConversionPostProcessing) {
            SchematicConverter.postProcessBlocks(this.blocks, this.tiles, this.postProcessingFilter);
        }
    }

    private void readEntitiesFromNBT(CompoundTag nbt) {
        this.entities.clear();
        ListTag tagList = nbt.getList("Entities", Constants.NBT.TAG_COMPOUND);
        int minecraftDataVersion = Configs.Generic.DATAFIXER_DEFAULT_SCHEMA.getIntegerValue();
        DataFixerMode.Schema effective = DataFixerMode.getEffectiveSchema(minecraftDataVersion);

        if (effective != null) {
            LitematicaRewrite.LOGGER.info("SchematicaSchematic: executing Vanilla DataFixer for Entities DataVersion {} -> {}", minecraftDataVersion, LitematicaSchematic.MINECRAFT_DATA_VERSION);
        } else {
            LitematicaRewrite.LOGGER.warn("SchematicaSchematic: Effective Schema has been bypassed.  Not applying Vanilla Data Fixer for Entities DataVersion {}", minecraftDataVersion);
        }

        for (int i = 0; i < tagList.size(); ++i) {
            if (effective != null) {
                this.entities.add(SchematicConversionMaps.updateEntity(tagList.getCompound(i), minecraftDataVersion));
            } else {
                this.entities.add(tagList.getCompound(i));
            }
        }
    }

    private void readTileEntitiesFromNBT(CompoundTag nbt) {
        this.tiles.clear();
        ListTag tagList = nbt.getList("TileEntities", Constants.NBT.TAG_COMPOUND);
        int minecraftDataVersion = Configs.Generic.DATAFIXER_DEFAULT_SCHEMA.getIntegerValue();
        DataFixerMode.Schema effective = DataFixerMode.getEffectiveSchema(minecraftDataVersion);

        if (effective != null) {
            LitematicaRewrite.LOGGER.info("SchematicaSchematic: executing Vanilla DataFixer for Tile Entities DataVersion {} -> {}", minecraftDataVersion, LitematicaSchematic.MINECRAFT_DATA_VERSION);
        } else {
            LitematicaRewrite.LOGGER.warn("SchematicaSchematic: Effective Schema has been bypassed.  Not applying Vanilla Data Fixer for Tile Entities DataVersion {}", minecraftDataVersion);
        }

        for (int i = 0; i < tagList.size(); ++i) {
            CompoundTag tag = tagList.getCompound(i);
            BlockPos pos = new BlockPos(tag.getInt("x"), tag.getInt("y"), tag.getInt("z"));
            Vec3i size = this.blocks.getSize();

            if (pos.getX() >= 0 && pos.getX() < size.getX() && pos.getY() >= 0 && pos.getY() < size.getY() && pos.getZ() >= 0 && pos.getZ() < size.getZ()) {
                if (effective != null) {
                    this.tiles.put(pos, SchematicConversionMaps.updateBlockEntity(SchematicConversionMaps.checkForIdTag(tag), minecraftDataVersion));
                } else {
                    this.tiles.put(pos, SchematicConversionMaps.checkForIdTag(tag));
                }
            }
        }
    }

    public boolean readFromFile(File file) {
        if (file.exists() && file.isFile() && file.canRead()) {
            this.fileName = file.getName();

            try {
                CompoundTag nbt = NbtUtils.readNbtFromFile(file);
                return this.readFromNBT(nbt);
            } catch (Exception e) {
                LitematicaRewrite.LOGGER.error("SchematicaSchematic: Failed to read Schematic data from file '{}'", file.getAbsolutePath());
            }
        }

        return false;
    }

    /*
    private void createPalette()
    {
        if (this.palette == null)
        {
            this.palette = new Block[4096];
            ILitematicaBlockStatePalette litematicaPalette = this.blocks.getPalette();
            final int numBlocks = litematicaPalette.getPaletteSize();

            for (int i = 0; i < numBlocks; ++i)
            {
                IBlockState state = litematicaPalette.getBlockState(i);
                Block block = state.getBlock();
                int id = Block.getIdFromBlock(block);

                if (id >= this.palette.length)
                {
                    throw new IllegalArgumentException(String.format("Block id %d for block '%s' is out of range, max allowed = %d!",
                            id, state, this.palette.length - 1));
                }

                this.palette[id] = block;
            }
        }
    }

    private void writePaletteToNBT(NBTTagCompound nbt)
    {
        NBTTagCompound tag = new NBTTagCompound();

        for (int i = 0; i < this.palette.length; ++i)
        {
            Block block = this.palette[i];

            if (block != null)
            {
                ResourceLocation rl = IRegistry.BLOCK.getKey(block);

                if (rl != null)
                {
                    tag.putShort(rl.toString(), (short) (i & 0xFFF));
                }
            }
        }

        nbt.put("SchematicaMapping", tag);
    }

    private void writeBlocksToNBT(NBTTagCompound nbt)
    {
        nbt.putShort("Width", (short) this.size.getX());
        nbt.putShort("Height", (short) this.size.getY());
        nbt.putShort("Length", (short) this.size.getZ());
        nbt.putString("Materials", "Alpha");

        final int numBlocks = this.size.getX() * this.size.getY() * this.size.getZ();
        final int loopMax = (int) Math.floor((double) numBlocks / 2D);
        final int addSize = (int) Math.ceil((double) numBlocks / 2D);
        final byte[] blockIdsArr = new byte[numBlocks];
        final byte[] metaArr = new byte[numBlocks];
        final byte[] addArr = new byte[addSize];
        final int sizeX = this.size.getX();
        final int sizeZ = this.size.getZ();
        final int layerSize = sizeX * sizeZ;
        int numAdd = 0;
        int bi, ai;

        for (bi = 0, ai = 0; ai < loopMax; bi += 2, ++ai)
        {
            int x = bi % sizeX;
            int y = bi / layerSize;
            int z = (bi % layerSize) / sizeX;
            IBlockState state1 = this.blocks.get(x, y, z);

            x = (bi + 1) % sizeX;
            y = (bi + 1) / layerSize;
            z = ((bi + 1) % layerSize) / sizeX;
            IBlockState state2 = this.blocks.get(x, y, z);

            int id1 = Block.getIdFromBlock(state1.getBlock());
            int id2 = Block.getIdFromBlock(state2.getBlock());
            int add = ((id1 >>> 4) & 0xF0) | ((id2 >>> 8) & 0x0F);
            blockIdsArr[bi    ] = (byte) (id1 & 0xFF);
            blockIdsArr[bi + 1] = (byte) (id2 & 0xFF);

            if (add != 0)
            {
                addArr[ai] = (byte) add;
                numAdd++;
            }

            metaArr[bi    ] = (byte) state1.getBlock().getMetaFromState(state1);
            metaArr[bi + 1] = (byte) state2.getBlock().getMetaFromState(state2);
        }

        // Odd number of blocks, handle the last position
        if ((numBlocks % 2) != 0)
        {
            int x = bi % sizeX;
            int y = bi / layerSize;
            int z = (bi % layerSize) / sizeX;
            IBlockState state = this.blocks.get(x, y, z);

            int id = Block.getIdFromBlock(state.getBlock());
            int add = (id >>> 4) & 0xF0;
            blockIdsArr[bi] = (byte) (id & 0xFF);

            if (add != 0)
            {
                addArr[ai] = (byte) add;
                numAdd++;
            }

            metaArr[bi] = (byte) state.getBlock().getMetaFromState(state);
        }

        nbt.putByteArray("Blocks", blockIdsArr);
        nbt.putByteArray("Data", metaArr);

        if (numAdd > 0)
        {
            nbt.putByteArray("AddBlocks", addArr);
        }
    }

    private NBTTagCompound writeToNBT()
    {
        NBTTagCompound nbt = new NBTTagCompound();

        this.createPalette();
        this.writeBlocksToNBT(nbt);
        this.writePaletteToNBT(nbt);

        NBTTagList tagListTiles = new NBTTagList();
        NBTTagList tagListEntities = new NBTTagList();

        for (NBTTagCompound tag : this.entities)
        {
            tagListEntities.add(tag);
        }

        for (NBTTagCompound tag : this.tiles.values())
        {
            tagListTiles.add(tag);
        }

        nbt.put("TileEntities", tagListTiles);
        nbt.put("Entities", tagListEntities);

        return nbt;
    }

    public boolean writeToFile(File dir, String fileNameIn, boolean override, IStringConsumer feedback)
    {
        String fileName = fileNameIn;

        if (fileName.endsWith(FILE_EXTENSION) == false)
        {
            fileName = fileName + FILE_EXTENSION;
        }

        File fileSchematic = new File(dir, fileName);

        try
        {
            if (dir.exists() == false && dir.mkdirs() == false)
            {
                feedback.setString(StringUtils.translate("litematica.error.schematic_write_to_file_failed.directory_creation_failed", dir.getAbsolutePath()));
                return false;
            }

            if (override == false && fileSchematic.exists())
            {
                feedback.setString(StringUtils.translate("litematica.error.schematic_write_to_file_failed.exists", fileSchematic.getAbsolutePath()));
                return false;
            }

            FileOutputStream os = new FileOutputStream(fileSchematic);
            CompressedStreamTools.writeCompressed(this.writeToNBT(), os);
            os.close();

            return true;
        }
        catch (Exception e)
        {
            feedback.setString(StringUtils.translate("litematica.error.schematic_write_to_file_failed.exception", fileSchematic.getAbsolutePath()));
        }

        return false;
    }
    */
}
