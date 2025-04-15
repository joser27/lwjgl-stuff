package mystuff.engine;

public interface IGameLogic {
    void init(Window window);
    void input(Window window);
    void update(float interval);
    void render(Window window);
    void cleanup();
} 