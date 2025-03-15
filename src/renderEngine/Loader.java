package renderEngine;

import Models.RawModel;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

public class Loader {

    // For memory management, delete in memory after use
    private List<Integer> vaos = new ArrayList<Integer>();
    private List<Integer> vbos = new ArrayList<Integer>();
    private List<Integer> textures = new ArrayList<Integer>();

    // Add the method to load a model with normal vectors
    public RawModel loadToVAO(float[] positions, float[] textureCoords, float[] normals, int[] indices) {
        // Creates vaoId
        int vaoID = createVao();

        // Binds indices to vao
        bindIndicesBuffer(indices);

        // Stores vertex coordinates in vbo and places it in vao
        storeDataInAttributeList(0, 3, positions);

        // Stores texture coords
        storeDataInAttributeList(1, 2, textureCoords);

        // Stores normal vectors
        storeDataInAttributeList(2, 3, normals);

        // Unbinds
        unbindVAO();

        // Return the created model
        return new RawModel(vaoID, indices.length);
    }

    // Keep the original method for backward compatibility
    public RawModel loadToVAO(float[] positions, float[] textureCoords, int[] indices) {
        // Creates vaoId
        int vaoID = createVao();

        // Binds indices to vao
        bindIndicesBuffer(indices);

        // Stores vertex coordinates in vbo and places it in vao
        storeDataInAttributeList(0, 3, positions);

        // Stores texture coords
        storeDataInAttributeList(1, 2, textureCoords);

        // Unbinds
        unbindVAO();

        // Return the created model
        return new RawModel(vaoID, indices.length);
    }

    public int loadTexture(String fileName) {
        int textureID = 0;
        String filePath = "res/textures/" + fileName + ".png";
        File textureFile = new File(filePath);

        // Check if the requested texture exists, if not, use checkerboard
        if (!textureFile.exists()) {
            System.out.println("Texture " + fileName + " not found, using checkerboard pattern instead.");
            return createCheckerboardTexture();
        }

        try {
            // Load the image using Java's ImageIO
            BufferedImage bufferedImage = ImageIO.read(textureFile);
            int width = bufferedImage.getWidth();
            int height = bufferedImage.getHeight();

            // Convert the buffered image to a byte buffer
            int[] pixels = new int[width * height];
            bufferedImage.getRGB(0, 0, width, height, pixels, 0, width);
            ByteBuffer buffer = BufferUtils.createByteBuffer(width * height * 4); // 4 for RGBA

            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int pixel = pixels[y * width + x];
                    buffer.put((byte) ((pixel >> 16) & 0xFF)); // Red
                    buffer.put((byte) ((pixel >> 8) & 0xFF));  // Green
                    buffer.put((byte) (pixel & 0xFF));         // Blue
                    buffer.put((byte) ((pixel >> 24) & 0xFF)); // Alpha
                }
            }
            buffer.flip();

            // Create a new OpenGL texture
            textureID = GL11.glGenTextures();
            textures.add(textureID);

            // Register with OBJLoader for centralized resource tracking
            OBJLoader.registerTexture(textureID);

            // Bind the texture
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureID);

            // Setup texture scaling filtering
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);

            // Send texture to GPU
            GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, width, height, 0,
                    GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, buffer);

            // Generate mipmaps for better performance at different distances
            GL30.glGenerateMipmap(GL11.GL_TEXTURE_2D);

        } catch (IOException e) {
            System.err.println("Error loading texture: " + fileName);
            e.printStackTrace();
            return createCheckerboardTexture();
        }

        return textureID;
    }

    private int createCheckerboardTexture() {
        // Create a simple procedural checkerboard texture
        int width = 16;
        int height = 16;
        int squareSize = 4;

        ByteBuffer buffer = BufferUtils.createByteBuffer(width * height * 4);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                boolean isPurple = ((x / squareSize) + (y / squareSize)) % 2 == 0;

                if (isPurple) {
                    buffer.put((byte) 128).put((byte) 0).put((byte) 128).put((byte) 255); // Purple (R:128, G:0, B:128, A:255)
                } else {
                    buffer.put((byte) 0).put((byte) 0).put((byte) 0).put((byte) 255); // Black (R:0, G:0, B:0, A:255)
                }
            }
        }
        buffer.flip();

        int textureID = GL11.glGenTextures();
        textures.add(textureID);

        // Register with OBJLoader for centralized resource tracking
        OBJLoader.registerTexture(textureID);

        GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureID);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, width, height, 0,
                GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, buffer);

        // Generate mipmaps
        GL30.glGenerateMipmap(GL11.GL_TEXTURE_2D);

        return textureID;
    }

    private int createVao() {
        // Creates an empty vao and returns the id
        int vaoID = GL30.glGenVertexArrays();

        // Adds every new vao to the vao list
        vaos.add(vaoID);

        // Register with OBJLoader for centralized resource tracking
        OBJLoader.registerVAO(vaoID);

        // To activate need to bind
        GL30.glBindVertexArray(vaoID);
        return vaoID;
    }

    private void storeDataInAttributeList(int attributeNumber, int coordinateSize, float[] data) {
        // Creates an empty vbo
        int vboID = GL15.glGenBuffers();

        // Adds every new vbo to the vbo list
        vbos.add(vboID);

        // Register with OBJLoader for centralized resource tracking
        OBJLoader.registerVBO(vboID);

        // Type of VBO
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vboID);

        // Converts the array into buffer
        FloatBuffer buffer = storeDataInFloatBuffer(data);

        // Store into vbo the data "buffer" to be used for
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, buffer, GL15.GL_STATIC_DRAW);

        // Put vbo into vao |no of attrib list, vao thing, type of data, if data is normalized, distance between vertices, offset
        GL20.glVertexAttribPointer(attributeNumber, coordinateSize, GL11.GL_FLOAT, false, 0, 0);

        // Unbind vbo
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
    }

    private void unbindVAO() {
        // Unbinds vao
        GL30.glBindVertexArray(0);
    }

    // Converts float array "data" or "positions" into float buffer
    private FloatBuffer storeDataInFloatBuffer(float[] data) {
        // Creates empty float buffer
        FloatBuffer buffer = BufferUtils.createFloatBuffer(data.length);

        // Puts data in the float buffer aka vertex data
        buffer.put(data);

        // Prepares buffer to be read by opengl
        buffer.flip();
        return buffer;
    }

    // Creates indices for index rendering
    private void bindIndicesBuffer(int[] indices) {
        int vboID = GL15.glGenBuffers();
        vbos.add(vboID);

        // Register with OBJLoader for centralized resource tracking
        OBJLoader.registerVBO(vboID);

        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, vboID);
        IntBuffer buffer = storeDataInIntBuffer(indices);
        GL15.glBufferData(GL15.GL_ELEMENT_ARRAY_BUFFER, buffer, GL15.GL_STATIC_DRAW);
    }

    private IntBuffer storeDataInIntBuffer(int[] data) {
        IntBuffer buffer = BufferUtils.createIntBuffer(data.length);
        buffer.put(data);
        buffer.flip();
        return buffer;
    }

    public void cleanup() {
        // For every vao in the array
        for(int vao : vaos) {
            // Delete the id
            GL30.glDeleteVertexArrays(vao);
        }

        // For every vbo in the array
        for(int vbo : vbos) {
            // Delete the id
            GL15.glDeleteBuffers(vbo);
        }

        // For every texture
        for(int texture : textures) {
            GL11.glDeleteTextures(texture);
        }

        // Clear the lists after deletion
        vaos.clear();
        vbos.clear();
        textures.clear();

        System.out.println("Loader resources cleaned up successfully");
    }
}