package me.gorgeousone.netherview.packet;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.BlockPosition;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.Pair;
import com.comphenix.protocol.wrappers.WrappedBlockData;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import me.gorgeousone.netherview.blockcache.Transform;
import me.gorgeousone.netherview.geometry.BlockVec;
import me.gorgeousone.netherview.portal.ProjectionEntity;
import me.gorgeousone.netherview.utils.FacingUtils;
import me.gorgeousone.netherview.utils.TimeUtils;
import me.gorgeousone.netherview.wrapper.WrappedBoundingBox;
import me.gorgeousone.netherview.wrapper.blocktype.BlockType;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Painting;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class PacketHandler {
	
	private final ItemStack pumpkin = new ItemStack(Material.CARVED_PUMPKIN);
	private final ProtocolManager protocolManager;
	private final Set<Integer> markedPacketIds;
	
	public PacketHandler() {
		
		protocolManager = ProtocolLibrary.getProtocolManager();
		markedPacketIds = new HashSet<>();
	}
	
	private void sendCustomPacket(Player player, PacketContainer packet) {
		
		int packetId = System.identityHashCode(packet.getHandle());
		
		markedPacketIds.add(packetId);
		
		try {
			protocolManager.sendServerPacket(player, packet);
		} catch (Exception e) {
			markedPacketIds.remove(packetId);
			throw new RuntimeException("Failed to send packet " + packet, e);
		}
	}
	
	private void sendPacket(Player player, PacketContainer packet) {
		
		if (packet == null) {
			return;
		}
		
		try {
			protocolManager.sendServerPacket(player, packet);
		} catch (Exception e) {
			throw new RuntimeException("Failed to send packet " + packet, e);
		}
	}
	
	public boolean isCustomPacket(PacketContainer packet) {
		
		int packetId = System.identityHashCode(packet.getHandle());
		return markedPacketIds.contains(packetId);
	}
	
	public void refreshFakeBlock(Player player, BlockPosition blockPos, BlockType projectedBlockType) {
		
		PacketContainer fakeBlockPacket = protocolManager.createPacket(PacketType.Play.Server.BLOCK_CHANGE);
		
		fakeBlockPacket.getBlockPositionModifier().write(0, blockPos);
		fakeBlockPacket.getBlockData().write(0, projectedBlockType.getWrapped());
		
		sendCustomPacket(player, fakeBlockPacket);
	}
	
	public void removeFakeBlocks(Player player, Map<BlockVec, BlockType> blockCopies) {
		
		World playerWorld = player.getWorld();
		Map<BlockVec, BlockType> updatedBlockCopies = new HashMap<>();
		
		for (BlockVec blockPos : blockCopies.keySet())
			updatedBlockCopies.put(blockPos.clone(), BlockType.of(blockPos.toBlock(playerWorld)));
		
		displayFakeBlocks(player, updatedBlockCopies);
	}
	
	public void displayFakeBlocks(Player player, Map<BlockVec, BlockType> blockCopies) {
		sendMultipleFakeBlocks1_16_2(player, blockCopies);
	}
	
	private void sendMultipleFakeBlocks1_16_2(Player player, Map<BlockVec, BlockType> blockCopies) {
		
		Map<BlockVec, Map<BlockVec, BlockType>> sortedBlockTypes = getSortedBy16x16x16(blockCopies);
		
		for (BlockVec chunkPos : sortedBlockTypes.keySet()) {
			
			Map<BlockVec, BlockType> blockInChunk = sortedBlockTypes.get(chunkPos);
			PacketContainer fakeBlocksPacket = protocolManager.createPacket(PacketType.Play.Server.MULTI_BLOCK_CHANGE);
			
			fakeBlocksPacket.getSectionPositions().write(0, chunkPos.toBlockPos());
			fakeBlocksPacket.getShortArrays().write(0, createChunkLocsArray1_16_2(blockInChunk.keySet()));
			fakeBlocksPacket.getBlockDataArrays().write(0, createBlockInfoArray1_16_2(blockInChunk.values()));
			sendCustomPacket(player, fakeBlocksPacket);
		}
	}
	
	private Map<BlockVec, Map<BlockVec, BlockType>> getSortedBy16x16x16(Map<BlockVec, BlockType> blockCopies) {
		
		Map<BlockVec, Map<BlockVec, BlockType>> sortedBlockCopies = new HashMap<>();
		
		for (Map.Entry<BlockVec, BlockType> entry : blockCopies.entrySet()) {
			
			BlockVec blockPos = entry.getKey();
			BlockVec cubePos = new BlockVec(blockPos.getX() >> 4, blockPos.getY() >> 4, blockPos.getZ() >> 4);
			
			sortedBlockCopies.computeIfAbsent(cubePos, map -> new HashMap<>());
			sortedBlockCopies.get(cubePos).put(blockPos, entry.getValue());
		}
		
		return sortedBlockCopies;
	}
	
	private WrappedBlockData[] createBlockInfoArray1_16_2(Collection<BlockType> blocksTypesInChunk) {
		
		WrappedBlockData[] blockInfoArray = new WrappedBlockData[blocksTypesInChunk.size()];
		int i = 0;
		
		for (BlockType blockType : blocksTypesInChunk) {
			blockInfoArray[i] = blockType.getWrapped();
			++i;
		}
		
		return blockInfoArray;
	}
	
	private short[] createChunkLocsArray1_16_2(Collection<BlockVec> blockLocsInChunk) {
		
		short[] chunkLocs = new short[blockLocsInChunk.size()];
		int i = 0;
		
		for (BlockVec loc : blockLocsInChunk) {
			chunkLocs[i] = loc.toChunkShort();
			++i;
		}
		
		return chunkLocs;
	}
	
	public void hideProjectedEntities(Player player, Set<ProjectionEntity> entities) {
		
		if (entities.isEmpty()) {
			return;
		}
		
		int[] entityIds = new int[entities.size()];
		int i = 0;
		
		for (ProjectionEntity entity : entities) {
			entityIds[i] = entity.getFakeId();
			++i;
		}
		
		PacketContainer destroyPacket = protocolManager.createPacket(PacketType.Play.Server.ENTITY_DESTROY);
		destroyPacket.getIntLists().write(0, Arrays.stream(entityIds).boxed().toList());
		sendPacket(player, destroyPacket);
	}
	
	public void hideProjectedEntity(Player player, ProjectionEntity entity) {
		hideEntitiesById(player, new int[]{entity.getFakeId()});
	}
	
	public void hideEntities(Player player, Set<Entity> entities) {
		
		if (entities.isEmpty()) {
			return;
		}
		
		int[] entityIds = new int[entities.size()];
		int i = 0;
		
		for (Entity entity : entities) {
			entityIds[i] = entity.getEntityId();
			++i;
		}
		
		hideEntitiesById(player, entityIds);
	}
	
	private void hideEntitiesById(Player player, int[] entityIds) {
		
		PacketContainer destroyPacket = protocolManager.createPacket(PacketType.Play.Server.ENTITY_DESTROY);
		destroyPacket.getIntLists().write(0, Arrays.stream(entityIds).boxed().toList());
		sendPacket(player, destroyPacket);
	}
	
	public void showEntities(Player player, Set<Entity> visibleEntities) {
		
		for (Entity entity : visibleEntities) {
			showEntity(player, entity, entity.getEntityId(), new Transform(), false);
		}
	}
	
	public void showEntity(Player player,
	                       Entity entity,
	                       int entityId,
	                       Transform transform,
	                       boolean isProjection) {
		
		if (entity == null || entity.isDead()) {
			return;
		}
		
		Location entityLoc = transform.transformLoc(entity.getLocation());
		
		switch (entity.getType()) {
			
			case EXPERIENCE_ORB:
				return;
			
			case PAINTING:
				sendPacket(player, createPaintingPacket((Painting) entity, entityLoc, entityId, transform));
				break;
			
			case PLAYER:
				sendPacket(player, createEntitySpawnPacket(entity, entityLoc, entityId));
				sendPacket(player, createHeadRotation(entity, entityLoc.getYaw()));
				showEquipment(player, (LivingEntity) entity, entityId, isProjection);
				break;
			
			default:
				if (entity instanceof LivingEntity) {
					sendPacket(player, createEntitySpawnPacket(entity, entityLoc, entityId));
					sendPacket(player, createHeadRotation(entity, entityLoc.getYaw()));
					showEquipment(player, (LivingEntity) entity, entityId, isProjection);
				} else {
					sendPacket(player, createEntitySpawnPacket(entity, entityLoc, entityId));
				}
		}
		
		sendPacket(player, createMetadataPacket(entity));
	}
	
	private PacketContainer createHeadRotation(Entity entity,
	                                           float yaw) {
		
		PacketContainer headRotPacket = protocolManager.createPacket(PacketType.Play.Server.ENTITY_HEAD_ROTATION);
		headRotPacket.getIntegers().write(0, entity.getEntityId());
		headRotPacket.getBytes().write(0, (byte) (yaw * 265 / 360));
		return headRotPacket;
	}
	
	/**
	 * Creates a spawn packet using the unified SPAWN_ENTITY packet (available since 1.20.2).
	 * In 1.20.2+ Mojang merged NAMED_ENTITY_SPAWN, SPAWN_ENTITY_LIVING, and SPAWN_ENTITY_PAINTING
	 * into the single SPAWN_ENTITY packet.
	 */
	private PacketContainer createEntitySpawnPacket(Entity entity,
	                                                Location entityLoc,
	                                                int entityId) {
		
		PacketContainer spawnPacket = protocolManager.createPacket(PacketType.Play.Server.SPAWN_ENTITY);
		spawnPacket.getIntegers().write(0, entityId);
		spawnPacket.getUUIDs().write(0, entity.getUniqueId());
		spawnPacket.getEntityTypeModifier().write(0, entity.getType());
		writeEntityPos(spawnPacket, entityLoc);
		writeEntityAngle(spawnPacket, entityLoc);
		// velocity data is optional in 1.21 SPAWN_ENTITY, leave default
		return spawnPacket;
	}
	
	private PacketContainer createPaintingPacket(Painting painting,
	                                             Location location,
	                                             int entityId,
	                                             Transform transform) {
		
		PacketContainer spawnPacket = protocolManager.createPacket(PacketType.Play.Server.SPAWN_ENTITY);
		spawnPacket.getIntegers().write(0, entityId);
		spawnPacket.getUUIDs().write(0, painting.getUniqueId());
		spawnPacket.getEntityTypeModifier().write(0, painting.getType());
		
		int halfHeight = (int) (WrappedBoundingBox.of(painting).getHeight() / 2);
		BlockPosition blockPosition = new BlockPosition(location.toVector()).subtract(new BlockPosition(0, halfHeight, 0));
		
		BlockFace rotatedFace = FacingUtils.getRotatedFace(painting.getFacing(), transform.getQuarterTurns());
		EnumWrappers.Direction rotatedDirection = FacingUtils.getBlockFaceToDirection(rotatedFace);
		
		spawnPacket.getBlockPositionModifier().write(0, blockPosition);
		spawnPacket.getDirections().write(0, rotatedDirection);
		
		return spawnPacket;
	}
	
	private void writeEntityPos(PacketContainer spawnPacket, Location entityLoc) {
		
		spawnPacket.getDoubles()
				.write(0, entityLoc.getX())
				.write(1, entityLoc.getY())
				.write(2, entityLoc.getZ());
	}
	
	private void writeEntityAngle(PacketContainer spawnPacket, Location entityLoc) {
		
		spawnPacket.getBytes()
				.write(0, (byte) (int) (entityLoc.getYaw() * 256 / 360))
				.write(1, (byte) (int) (entityLoc.getPitch() * 256 / 360));
	}
	
	public void sendEntityMoveLook(Player player,
	                               ProjectionEntity entity,
	                               Vector relMove,
	                               double newYaw,
	                               double newPitch,
	                               boolean isOnGround) {
		
		PacketContainer moveLookPacket = protocolManager.createPacket(PacketType.Play.Server.REL_ENTITY_MOVE_LOOK);
		
		moveLookPacket.getIntegers().write(0, entity.getFakeId());
		
		moveLookPacket.getShorts()
				.write(0, (short) (relMove.getX() * 4096))
				.write(1, (short) (relMove.getY() * 4096))
				.write(2, (short) (relMove.getZ() * 4096));
		
		moveLookPacket.getBytes()
				.write(0, (byte) (newYaw * 256 / 360))
				.write(1, (byte) (newPitch * 256 / 360));
		
		moveLookPacket.getBooleans()
				.write(0, isOnGround)
				.write(1, true);
		
		PacketContainer headRotPacket = protocolManager.createPacket(PacketType.Play.Server.ENTITY_HEAD_ROTATION);
		headRotPacket.getIntegers().write(0, entity.getFakeId());
		headRotPacket.getBytes().write(0, (byte) (int) (newYaw * 265 / 360));
		
		sendPacket(player, moveLookPacket);
		sendPacket(player, headRotPacket);
	}
	
	private PacketContainer createMetadataPacket(Entity entity) {
		
		PacketContainer metaPacket = protocolManager.createPacket(PacketType.Play.Server.ENTITY_METADATA);
		metaPacket.getIntegers().write(0, entity.getEntityId());
		
		WrappedDataWatcher dataWatcher = new WrappedDataWatcher();
		metaPacket.getWatchableCollectionModifier().write(0, dataWatcher.getWatchableObjects());
		return metaPacket;
	}
	
	private void showEquipment(Player player, LivingEntity entity, int entityId, boolean isProjection) {
		
		Map<EnumWrappers.ItemSlot, ItemStack> equipmentMap = getEquipmentList(entity, isProjection);
		sendEquipment1_16(player, entityId, equipmentMap);
	}
	
	private void sendEquipment1_16(Player player,
	                               int entityId,
	                               Map<EnumWrappers.ItemSlot, ItemStack> equipmentMap) {
		
		List<Pair<EnumWrappers.ItemSlot, ItemStack>> equipmentList = new ArrayList<>();
		
		for (EnumWrappers.ItemSlot slot : equipmentMap.keySet()) {
			
			ItemStack item = equipmentMap.get(slot);
			
			if (item == null || item.getType() == Material.AIR) {
				continue;
			}
			
			equipmentList.add(new Pair<>(slot, item));
		}
		
		if (equipmentList.isEmpty()) {
			return;
		}
		
		PacketContainer equipmentPacket = protocolManager.createPacket(PacketType.Play.Server.ENTITY_EQUIPMENT);
		equipmentPacket.getIntegers().write(0, entityId);
		equipmentPacket.getSlotStackPairLists().write(0, equipmentList);
		sendPacket(player, equipmentPacket);
	}
	
	public Map<EnumWrappers.ItemSlot, ItemStack> getEquipmentList(LivingEntity entity, boolean isProjection) {
		
		EntityEquipment equipment = entity.getEquipment();
		Map<EnumWrappers.ItemSlot, ItemStack> equipmentMap = new HashMap<>();
		
		if (equipment == null) {
			return equipmentMap;
		}
		
		equipmentMap.put(EnumWrappers.ItemSlot.MAINHAND, equipment.getItemInMainHand());
		equipmentMap.put(EnumWrappers.ItemSlot.OFFHAND, equipment.getItemInOffHand());
		equipmentMap.put(EnumWrappers.ItemSlot.FEET, equipment.getBoots());
		equipmentMap.put(EnumWrappers.ItemSlot.LEGS, equipment.getLeggings());
		equipmentMap.put(EnumWrappers.ItemSlot.CHEST, equipment.getChestplate());
		
		if (isProjection && TimeUtils.isSpooktober()) {
			equipmentMap.put(EnumWrappers.ItemSlot.HEAD, pumpkin);
		} else {
			equipmentMap.put(EnumWrappers.ItemSlot.HEAD, equipment.getHelmet());
		}
		
		return equipmentMap;
	}
}