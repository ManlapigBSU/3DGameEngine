package renderEngine;

import Entities.Entity;
import Models.RawModel;
import Models.TexturedModel;
import org.lwjgl.opengl.*;
import org.lwjgl.util.vector.Matrix;
import org.lwjgl.util.vector.Matrix4f;
import shaders.StaticShader;
import textures.ModelTexture;
import toolbox.Maths;

import java.util.List;
import java.util.Map;

public class EntityRenderer {

    private StaticShader shader;

    public EntityRenderer(StaticShader shader, Matrix4f projectionMatrix) {

        this.shader = new StaticShader();
        shader.start();
        shader.loadProjectionMatrix(projectionMatrix);
        shader.stop();
    }


    public void render(Map<TexturedModel, List<Entity>> entities) {

        for(TexturedModel model : entities.keySet()) {

            prepareTextureModel(model);
            List<Entity> batch = entities.get(model);
            for(Entity entity : batch) {

                prepareInstance(entity);
                GL11.glDrawElements(GL11.GL_TRIANGLES, model.getRawModel().getVertexCount(), GL11.GL_UNSIGNED_INT, 0);
            }
            unbindTexturedModel();

        }

    }

    private void prepareTextureModel(TexturedModel model) {

        RawModel rawModel = model.getRawModel();

        //bind vao
        GL30.glBindVertexArray(rawModel.getVaoID());

        //enables attribute list for vertex position, texture location, and normals
        GL20.glEnableVertexAttribArray(0);
        GL20.glEnableVertexAttribArray(1);
        GL20.glEnableVertexAttribArray(2);

        //gets shine
        ModelTexture texture = model.getTexture();
        shader.loadShineVariables(texture.getShineDamper(), texture.getReflectivity());

        //puts texture in texture bank
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, model.getTexture().getTextureID());


    }

    private void unbindTexturedModel() {

        //disables attribute lists
        GL20.glDisableVertexAttribArray(0);
        GL20.glDisableVertexAttribArray(1);
        GL20.glDisableVertexAttribArray(2);

        //unbinds vao
        GL30.glBindVertexArray(0);

    }

    private void prepareInstance(Entity entity) {

        //gets transform
        Matrix4f transformationMatrix = Maths.createTransformationMatrix(entity.getPosition(), entity.getRotX(), entity.getRotY(), entity.getRotZ(), entity.getScale());
        shader.loadTransformationMatrix(transformationMatrix);

    }

}
