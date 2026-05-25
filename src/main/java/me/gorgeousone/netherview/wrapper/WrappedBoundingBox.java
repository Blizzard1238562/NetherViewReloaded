package me.gorgeousone.netherview.wrapper;

import me.gorgeousone.netherview.blockcache.BlockCache;
import me.gorgeousone.netherview.geometry.viewfrustum.ViewFrustum;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A wrapper for the extent of a bounding boxes of an entity throughout different Minecraft versions.
 */
public class WrappedBoundingBox {
	
	private final double widthX;
	private final double widthZ;
	private final double height;
	private final List<Vector> vertices;
	
	public WrappedBoundingBox(Entity entity, Location entityLoc, double widthX, double height, double widthZ) {
		
		this.widthX = widthX;
		this.widthZ = widthZ;
		this.height = height;
		
		Vector min = entityLoc.clone().subtract(
				widthX / 2,
				0,
				widthZ / 2).toVector();
		
		Vector max = entityLoc.clone().add(
				widthX / 2,
				height,
				widthZ / 2).toVector();
		
		vertices = new ArrayList<>(Arrays.asList(
				min,
				new Vector(max.getX(), min.getY(), min.getZ()),
				new Vector(min.getX(), min.getY(), max.getZ()),
				new Vector(max.getX(), min.getY(), max.getZ()),
				new Vector(min.getX(), max.getY(), min.getZ()),
				new Vector(max.getX(), max.getY(), min.getZ()),
				new Vector(min.getX(), max.getY(), max.getZ()),
				max
		));
	}
	
	public double getWidthX() {
		return widthX;
	}
	
	public double getWidthZ() {
		return widthZ;
	}
	
	public double getHeight() {
		return height;
	}
	
	/**
	 * Returns the 8 vertices of the entity's bounding box
	 */
	public List<Vector> getVertices() {
		return vertices;
	}
	
	public static WrappedBoundingBox of(Entity entity) {
		return of(entity, entity.getLocation());
	}
	
	public static WrappedBoundingBox of(Entity entity, Location entityLoc) {
		
		BoundingBox box = entity.getBoundingBox();
		
		return new WrappedBoundingBox(
				entity,
				entityLoc,
				box.getWidthX(),
				box.getHeight(),
				box.getWidthZ());
	}
	
	/**
	 * Returns true if any of the 8 vertices of the bounding box are inside of the block cache.
	 */
	public boolean intersectsBlockCache(BlockCache cache) {
		
		for (Vector vertex : getVertices()) {
			
			if (cache.contains(vertex)) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Returns true if any of the 8 vertices of the bounding box are inside of the view frustum.
	 */
	public boolean intersectsFrustum(ViewFrustum viewFrustum) {
		
		for (Vector vertex : getVertices()) {
			
			if (viewFrustum.contains(vertex)) {
				return true;
			}
		}
		return false;
	}
}