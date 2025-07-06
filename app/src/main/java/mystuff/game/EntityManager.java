package mystuff.game;

import mystuff.engine.GameObject;
import mystuff.engine.Window;
import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;

/**
 * Simple entity management system for organizing and updating game entities.
 * Prevents common bugs like concurrent modification and null pointer exceptions.
 */
public class EntityManager {
    // Main entity storage
    private List<GameObject> entities;
    
    // Entities to be added next frame (prevents concurrent modification)
    private List<GameObject> entitiesToAdd;
    
    // Entities to be removed next frame (prevents concurrent modification)
    private List<GameObject> entitiesToRemove;
    
    // Special references for commonly accessed entities
    private Player player;
    
    public EntityManager() {
        entities = new ArrayList<>();
        entitiesToAdd = new ArrayList<>();
        entitiesToRemove = new ArrayList<>();
    }
    
    /**
     * Add an entity to the world. Will be added at the start of the next frame.
     */
    public void addEntity(GameObject entity) {
        if (entity == null) {
            System.err.println("Warning: Attempted to add null entity!");
            return;
        }
        
        // Keep special reference to player for easy access
        if (entity instanceof Player) {
            player = (Player) entity;
        }
        
        entitiesToAdd.add(entity);
    }
    
    /**
     * Remove an entity from the world. Will be removed at the start of the next frame.
     */
    public void removeEntity(GameObject entity) {
        if (entity == null) {
            return;
        }
        
        // Clear special reference if removing player
        if (entity instanceof Player) {
            player = null;
        }
        
        entitiesToRemove.add(entity);
    }
    
    /**
     * Update all entities. Call this once per frame.
     */
    public void update(Window window, float deltaTime) {
        // Process entity additions and removals at the start of the frame
        processEntityChanges();
        
        // Update all entities
        for (GameObject entity : entities) {
            if (entity != null) {
                entity.update(window, deltaTime);
            }
        }
    }
    
    /**
     * Render all entities. Call this once per frame.
     */
    public void render() {
        for (GameObject entity : entities) {
            if (entity != null) {
                entity.render();
            }
        }
    }
    
    /**
     * Get all entities of a specific type.
     * Example: List<Cat> cats = getEntitiesOfType(Cat.class);
     */
    @SuppressWarnings("unchecked")
    public <T extends GameObject> List<T> getEntitiesOfType(Class<T> type) {
        List<T> result = new ArrayList<>();
        for (GameObject entity : entities) {
            if (entity != null && type.isInstance(entity)) {
                result.add((T) entity);
            }
        }
        return result;
    }
    
    /**
     * Get the first entity of a specific type, or null if none found.
     */
    @SuppressWarnings("unchecked")
    public <T extends GameObject> T getFirstEntityOfType(Class<T> type) {
        for (GameObject entity : entities) {
            if (entity != null && type.isInstance(entity)) {
                return (T) entity;
            }
        }
        return null;
    }
    
    /**
     * Get the player entity for easy access.
     */
    public Player getPlayer() {
        return player;
    }
    
    /**
     * Get the total number of entities.
     */
    public int getEntityCount() {
        return entities.size();
    }
    
    /**
     * Get all entities (read-only view).
     */
    public List<GameObject> getAllEntities() {
        return new ArrayList<>(entities);
    }
    
    /**
     * Remove all entities and clean up.
     */
    public void cleanup() {
        // Clean up all entities
        for (GameObject entity : entities) {
            if (entity != null) {
                // Call cleanup if the entity has it
                                 try {
                     if (entity instanceof Cat) {
                         ((Cat) entity).cleanup();
                     } else if (entity instanceof Mage) {
                         ((Mage) entity).cleanup();
                     } else if (entity instanceof Beggar) {
                         ((Beggar) entity).cleanup();
                     } else if (entity instanceof Player) {
                         ((Player) entity).cleanup();
                     } else if (entity instanceof Map) {
                         ((Map) entity).cleanup();
                     }
                 } catch (Exception e) {
                    System.err.println("Error cleaning up entity: " + e.getMessage());
                }
            }
        }
        
        entities.clear();
        entitiesToAdd.clear();
        entitiesToRemove.clear();
        player = null;
    }
    
    /**
     * Process entity additions and removals safely.
     */
    private void processEntityChanges() {
        // Add new entities
        if (!entitiesToAdd.isEmpty()) {
            entities.addAll(entitiesToAdd);
            entitiesToAdd.clear();
        }
        
        // Remove entities
        if (!entitiesToRemove.isEmpty()) {
            for (GameObject entity : entitiesToRemove) {
                entities.remove(entity);
            }
            entitiesToRemove.clear();
        }
    }
    
    /**
     * Check if an entity exists in the world.
     */
    public boolean containsEntity(GameObject entity) {
        return entities.contains(entity) || entitiesToAdd.contains(entity);
    }
    
    /**
     * Remove all entities of a specific type.
     */
    public <T extends GameObject> void removeAllEntitiesOfType(Class<T> type) {
        List<T> toRemove = getEntitiesOfType(type);
        for (T entity : toRemove) {
            removeEntity(entity);
        }
    }
} 