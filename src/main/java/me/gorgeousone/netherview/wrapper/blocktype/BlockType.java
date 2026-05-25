package me.gorgeousone.netherview.wrapper.blocktype;

import com.comphenix.protocol.wrappers.WrappedBlockData;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;

public abstract class BlockType {
	
	public static BlockType of(Block block) {
		return new AquaticBlockType(block);
	}
	
	public static BlockType of(Material material) {
		return new AquaticBlockType(material);
	}
	
	public static BlockType of(BlockState state) {
		return new AquaticBlockType(state);
	}
	
	public static BlockType of(String serialized) {
		return new AquaticBlockType(serialized);
	}
	
	public abstract BlockType rotate(int quarterTurns);
	
	public abstract WrappedBlockData getWrapped();
	
	public abstract boolean isOccluding();
	
	public abstract BlockType clone();
}