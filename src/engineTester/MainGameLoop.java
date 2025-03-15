package engineTester;

import Entities.Camera;
import Entities.Entity;
import Models.TexturedModel;
import org.lwjgl.opengl.Display;
import org.lwjgl.util.vector.Vector3f;
import renderEngine.DisplayManager;
import renderEngine.Loader;
import Models.RawModel;
import renderEngine.OBJLoader;
import renderEngine.Renderer;
import shaders.StaticShader;
import textures.ModelTexture;

public class MainGameLoop {

    public static void main(String[] args) {

        DisplayManager.createDisplay();

        Loader loader = new Loader();
        StaticShader shader = new StaticShader();
        Renderer renderer = new Renderer(shader);


        RawModel model = OBJLoader.loadOBJModel("mog", loader);
        ModelTexture texture = new ModelTexture(loader.loadTexture("grassblock"));
        TexturedModel staticModel = new TexturedModel(model, texture);

        Entity entity = new Entity(staticModel, new Vector3f(0, 0, -5), 0, 0, 0, 1);

        Camera camera  = new Camera();

        while(!Display.isCloseRequested()) {

            //game logic
            entity.increasePosition(0, 0, 0);
            entity.increaseRotation(0, 0, 0);
            //entity.setPosition(new Vector3f(0, 0, 0));
            camera.move();
            renderer.prepare();
            shader.start();
            shader.loadViewMatrix(camera);
            renderer.render(entity, shader);
            shader.stop();
            DisplayManager.updateDisplay();
        }


        //cleans up after the program
        shader.cleanup();
        loader.cleanup();
        DisplayManager.closeDisplay();

    }
}

