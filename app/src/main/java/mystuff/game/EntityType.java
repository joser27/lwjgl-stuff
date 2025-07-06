package mystuff.game;

/**
 * Enum for different entity types to help with organization and filtering.
 * This makes it easier to group entities by category and handle them efficiently.
 */
public enum EntityType {
    // Player-related entities
    PLAYER("Player"),
    
    // NPCs and creatures
    CAT("Cat"),
    MAGE("Mage"),
    BEGGAR("Beggar"),
    
    // Environment objects
    TREE("Tree"),
    BROADLEAF_TREE("Broadleaf Tree"),
    OAK_TREE("Oak Tree"),
    PINE_TREE("Pine Tree"),
    
    // Items and objects
    ITEM("Item"),
    OBJECT("Object"),
    
    // Other/Unknown
    OTHER("Other");
    
    private final String displayName;
    
    EntityType(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    @Override
    public String toString() {
        return displayName;
    }
    
    /**
     * Get the entity type from a GameObject class.
     * This helps automatically categorize entities based on their class type.
     */
    public static EntityType fromClass(Class<?> clazz) {
        if (clazz == Player.class) return PLAYER;
        if (clazz == Cat.class) return CAT;
        if (clazz == Mage.class) return MAGE;
        if (clazz == Beggar.class) return BEGGAR;
        if (clazz == Tree.class) return TREE;
        if (clazz == BroadleafTree.class) return BROADLEAF_TREE;
        if (clazz == OakTree.class) return OAK_TREE;
        if (clazz == PineTree.class) return PINE_TREE;
        
        // Check for inheritance
        if (Tree.class.isAssignableFrom(clazz)) return TREE;
        
        return OTHER;
    }
    
    /**
     * Check if this entity type is a living creature.
     */
    public boolean isLivingCreature() {
        return this == PLAYER || this == CAT || this == MAGE || this == BEGGAR;
    }
    
    /**
     * Check if this entity type is a static environment object.
     */
    public boolean isStaticEnvironment() {
        return this == TREE || this == BROADLEAF_TREE || this == OAK_TREE || this == PINE_TREE;
    }
    
    /**
     * Check if this entity type can be interacted with.
     */
    public boolean isInteractable() {
        return this == PLAYER || this == CAT || this == MAGE || this == BEGGAR || this == ITEM;
    }
} 