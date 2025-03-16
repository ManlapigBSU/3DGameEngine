package engineTester;

import Entities.Camera;
import Entities.Entity;
import Entities.Light;
import Models.TexturedModel;
import org.lwjgl.opengl.Display;
import org.lwjgl.util.vector.Vector3f;
import renderEngine.*;
import Models.RawModel;
import terrains.Terrain;
import textures.ModelTexture;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MainGameLoop {

    public static void main(String[] args) {

        DisplayManager.createDisplay();

        Loader loader = new Loader();

        //for object
        RawModel rawCubeModel = OBJLoader.loadOBJModel("cubefix", loader);
        RawModel rawMogModel = OBJLoader.loadOBJModel("mog2", loader);

        //for texture
        TexturedModel texturedCubeModel = new TexturedModel(rawCubeModel, new ModelTexture(loader.loadTexture("grassblock")));
        TexturedModel texturedMogModel = new TexturedModel(rawMogModel, new ModelTexture(loader.loadTexture("2")));

        //modifies the texture
        ModelTexture shineCubeTexture = texturedCubeModel.getTexture();
        shineCubeTexture.setShineDamper(2);
        shineCubeTexture.setReflectivity(0);
        ModelTexture shineMogTexture = texturedMogModel.getTexture();
        shineMogTexture.setShineDamper(2);
        shineMogTexture.setReflectivity(1);


        Entity cubeEntity = new Entity(texturedCubeModel, new Vector3f(0.5f, 0.5f, -5), 0, 0, 0, 1);
        Light light = new Light(new Vector3f(3000, 2000, 2000), new Vector3f(1, 1, 1));

        Terrain terrain = new Terrain(-1, -1, loader, new ModelTexture(loader.loadTexture("grasstop")));
        Terrain terrain2 = new Terrain(0, -1, loader, new ModelTexture(loader.loadTexture("grasstop")));

        Camera camera  = new Camera();
        camera.setPosition(new Vector3f(0,1, 0));

        //mog entities
        List<Entity> allMogEntities = new ArrayList<>();

        Random random = new Random();

        /*for(int i = 0; i < 200; i++) {

            float x = random.nextFloat() * 100 - 50;
            float y = random.nextFloat() * 100 - 50;
            float z = random.nextFloat() * -300;
            float rotX = random.nextFloat() * 180f;
            float rotY = random.nextFloat()* 180f;
            allMogEntities.add(new Entity(texturedMogModel, new Vector3f(x, y, z), rotX, rotY, 0f, 1f));
        }*/

        //stores all entities
        List<Entity> allEntities = new ArrayList<>();
        allEntities.addAll(allMogEntities);
        allEntities.add(cubeEntity);

        //allEntities.add(new Entity(staticModel, new Vector3f(0, 0, -5), 0, 0, 0, 1));

        MasterRenderer renderer = new MasterRenderer();

        while(!Display.isCloseRequested()) {

            //game logic

            renderer.processTerrain(terrain);
            renderer.processTerrain(terrain2);

            for(Entity mog : allEntities) {
                renderer.processEntity(mog);
            }


            camera.move();
            renderer.render(light, camera);
            DisplayManager.updateDisplay();
        }


        //cleans up after the program
        renderer.cleanup();
        loader.cleanup();
        DisplayManager.closeDisplay();

    }
}

