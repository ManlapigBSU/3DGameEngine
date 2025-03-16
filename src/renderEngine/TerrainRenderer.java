package renderEngine;

import Entities.Entity;
import Models.RawModel;
import Models.TexturedModel;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;
import shaders.TerrainShader;
import terrains.Terrain;
import textures.ModelTexture;
import toolbox.Maths;

import java.util.List;

public class TerrainRenderer {

    private TerrainShader shader;

    public  TerrainRenderer(TerrainShader shader, Matrix4f projectionMatrix) {

        this.shader = shader;
        shader.start();
        shader.loadProjectionMatrix(projectionMatrix);
        shader.stop();

    }

    public void render(List<Terrain> terrains) {

        for(Terrain terrain : terrains) {

            prepareTerrain(terrain);
            loadModelMatrix(terrain);
            GL11.glDrawElements(GL11.GL_TRIANGLES, terrain.getModel().getVertexCount(), GL11.GL_UNSIGNED_INT, 0);
            unbindTexturedModel();

        }

    }

    private void prepareTerrain(Terrain terrain) {

        RawModel rawModel = terrain.getModel();

        //bind vao
        GL30.glBindVertexArray(rawModel.getVaoID());

        //enables attribute list for vertex position, texture location, and normals
        GL20.glEnableVertexAttribArray(0);
        GL20.glEnableVertexAttribArray(1);
        GL20.glEnableVertexAttribArray(2);

        //gets shine
        ModelTexture texture = terrain.getTexture();
        shader.loadShineVariables(texture.getShineDamper(), texture.getReflectivity());

        //puts texture in texture bank
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture.getTextureID());


    }

    private void unbindTexturedModel() {

        //disables attribute lists
        GL20.glDisableVertexAttribArray(0);
        GL20.glDisableVertexAttribArray(1);
        GL20.glDisableVertexAttribArray(2);

        //unbinds vao
        GL30.glBindVertexArray(0);

    }

    private void loadModelMatrix(Terrain terrain) {

        //gets transform
        Matrix4f transformationMatrix = Maths.createTransformationMatrix(new Vector3f(terrain.getX(), 0, terrain.getZ()), 0, 0, 0, 1);
        shader.loadTransformationMatrix(transformationMatrix);

    }

}
