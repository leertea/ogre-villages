package dev.ogrevillages.worldgen;

import dev.ogrevillages.entity.OgreEntity;
import dev.ogrevillages.registry.ModEntities;
import dev.ogrevillages.registry.ModStructures;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

/**
 * Ein Teil des Ogre-Dorfs: entweder eine Hütte (7x7) oder der zentrale Platz (9x9).
 * Die Bauwerke werden im Code gebaut (keine NBT-Dateien nötig) und spawnen beim Bau ihre Ogres.
 */
public class OgreVillagePiece extends StructurePiece {
    public static final int HUT = 0;
    public static final int PLAZA = 1;

    private final int kind;
    private boolean spawned;

    public OgreVillagePiece(int kind, BoundingBox box, Direction orientation) {
        super(ModStructures.OGRE_VILLAGE_PIECE.get(), 0, box);
        this.kind = kind;
        setOrientation(orientation);
    }

    public OgreVillagePiece(CompoundTag tag) {
        super(ModStructures.OGRE_VILLAGE_PIECE.get(), tag);
        this.kind = tag.getInt("Kind");
        this.spawned = tag.getBoolean("Spawned");
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext ctx, CompoundTag tag) {
        tag.putInt("Kind", kind);
        tag.putBoolean("Spawned", spawned);
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator,
                            RandomSource random, BoundingBox chunkBB, ChunkPos chunkPos, BlockPos pivot) {
        if (kind == PLAZA) {
            buildPlaza(level, random, chunkBB);
        } else {
            buildHut(level, random, chunkBB);
        }
    }

    // ---------------------------------------------------------------- Hütte

    private void buildHut(WorldGenLevel level, RandomSource random, BoundingBox chunkBB) {
        BlockState air = Blocks.AIR.defaultBlockState();
        BlockState cobble = Blocks.COBBLESTONE.defaultBlockState();
        BlockState planks = Blocks.SPRUCE_PLANKS.defaultBlockState();
        BlockState log = Blocks.SPRUCE_LOG.defaultBlockState();
        BlockState glass = Blocks.GLASS.defaultBlockState();

        // Fundament (y0-2), Boden (y3), Innenraum freiräumen (y4-8)
        generateBox(level, chunkBB, 0, 0, 0, 6, 2, 6, cobble, cobble, false);
        generateBox(level, chunkBB, 0, 3, 0, 6, 3, 6, planks, planks, false);
        generateBox(level, chunkBB, 0, 4, 0, 6, 8, 6, air, air, false);

        // Wände (y4-6), Tür bei x=3, z=0
        for (int y = 4; y <= 6; y++) {
            for (int x = 0; x <= 6; x++) {
                for (int z = 0; z <= 6; z++) {
                    if (x != 0 && x != 6 && z != 0 && z != 6) {
                        continue;
                    }
                    boolean corner = (x == 0 || x == 6) && (z == 0 || z == 6);
                    BlockState s = corner ? log : planks;
                    boolean window = y == 5 && !corner
                            && ((z == 6 && x == 3) || ((x == 0 || x == 6) && z == 3));
                    if (window) {
                        s = glass;
                    }
                    if (z == 0 && x == 3 && y <= 5) {
                        s = air;
                    }
                    placeBlock(level, s, x, y, z, chunkBB);
                }
            }
        }

        // Dach (Pyramide)
        generateBox(level, chunkBB, 0, 7, 0, 6, 7, 6, planks, planks, false);
        generateBox(level, chunkBB, 1, 8, 1, 5, 8, 5, planks, planks, false);
        BlockState slab = Blocks.SPRUCE_SLAB.defaultBlockState();
        generateBox(level, chunkBB, 2, 9, 2, 4, 9, 4, slab, slab, false);

        // Einrichtung
        BlockState carpet = Blocks.RED_CARPET.defaultBlockState();
        for (int z = 2; z <= 4; z++) {
            placeBlock(level, carpet, 3, 4, z, chunkBB);
        }
        placeBlock(level, Blocks.CRAFTING_TABLE.defaultBlockState(), 1, 4, 1, chunkBB);
        placeBlock(level, Blocks.BARREL.defaultBlockState(), 5, 4, 1, chunkBB);
        placeBlock(level, Blocks.FURNACE.defaultBlockState(), 1, 4, 5, chunkBB);
        placeBlock(level, Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true), 3, 6, 3, chunkBB);

        // Bewohner
        if (!spawned) {
            BlockPos p = getWorldPos(3, 4, 3).immutable();
            if (chunkBB.isInside(p)) {
                spawned = true;
                int count = 1 + random.nextInt(2);
                for (int i = 0; i < count; i++) {
                    spawnOgre(level, p, random);
                }
            }
        }
    }

    // ---------------------------------------------------------------- Dorfplatz

    private void buildPlaza(WorldGenLevel level, RandomSource random, BoundingBox chunkBB) {
        BlockState air = Blocks.AIR.defaultBlockState();
        BlockState path = Blocks.DIRT_PATH.defaultBlockState();
        BlockState gravel = Blocks.COBBLESTONE.defaultBlockState();
        BlockState fence = Blocks.SPRUCE_FENCE.defaultBlockState();

        for (int x = 0; x <= 8; x++) {
            for (int z = 0; z <= 8; z++) {
                int dx = x - 4;
                int dz = z - 4;
                if (dx * dx + dz * dz > 20) {
                    continue;
                }
                for (int y = 1; y <= 4; y++) {
                    placeBlock(level, air, x, y, z, chunkBB);
                }
                boolean ring = dx * dx + dz * dz <= 2;
                placeBlock(level, ring ? gravel : (random.nextInt(4) == 0 ? gravel : path), x, 0, z, chunkBB);
            }
        }

        placeBlock(level, Blocks.CAMPFIRE.defaultBlockState(), 4, 1, 4, chunkBB);

        int[][] posts = {{1, 1}, {7, 1}, {1, 7}, {7, 7}};
        for (int[] p : posts) {
            placeBlock(level, fence, p[0], 1, p[1], chunkBB);
            placeBlock(level, fence, p[0], 2, p[1], chunkBB);
            placeBlock(level, Blocks.LANTERN.defaultBlockState(), p[0], 3, p[1], chunkBB);
        }

        if (!spawned) {
            BlockPos p = getWorldPos(4, 1, 6).immutable();
            if (chunkBB.isInside(p)) {
                spawned = true;
                spawnOgre(level, p, random);
            }
        }
    }

    // ---------------------------------------------------------------- Spawn

    private void spawnOgre(WorldGenLevel level, BlockPos pos, RandomSource random) {
        OgreEntity ogre = ModEntities.OGRE.get().create(level.getLevel());
        if (ogre == null) {
            return;
        }
        ogre.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, random.nextFloat() * 360.0F, 0.0F);
        ogre.finalizeSpawn((ServerLevelAccessor) level, level.getCurrentDifficultyAt(pos), MobSpawnType.STRUCTURE, null);
        ogre.setPersistenceRequired();
        ogre.restrictTo(pos, 24);
        level.addFreshEntityWithPassengers(ogre);
    }
}
