package renderEngine;

import Models.RawModel;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

public class OBJLoader {

    // Track resources for proper cleanup
    private static List<Integer> vaos = new ArrayList<>();
    private static List<Integer> vbos = new ArrayList<>();
    private static List<Integer> textures = new ArrayList<>();

    // Helper class for face indices
    public static class Vector3i {
        public int x, y, z;

        public Vector3i(int x, int y, int z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }

    public static RawModel loadOBJModel(String fileName, Loader loader) {
        List<String> lines = readAllLines("res/models/" + fileName + ".obj");

        List<Vector3f> vertices = new ArrayList<>();
        List<Vector2f> textures = new ArrayList<>();
        List<Vector3f> normals = new ArrayList<>();
        List<Integer> indices = new ArrayList<>();
        List<Vector3i> faces = new ArrayList<>();

        for (String line : lines) {
            if (line == null || line.isEmpty()) continue;

            String[] tokens = line.split("\\s+");
            if (tokens.length == 0) continue;

            switch (tokens[0]) {
                case "v":
                    if (tokens.length >= 4) {
                        Vector3f vertex = new Vector3f(
                                Float.parseFloat(tokens[1]),
                                Float.parseFloat(tokens[2]),
                                Float.parseFloat(tokens[3])
                        );
                        vertices.add(vertex);
                    }
                    break;

                case "vt":
                    if (tokens.length >= 3) {
                        Vector2f texture = new Vector2f(
                                Float.parseFloat(tokens[1]),
                                Float.parseFloat(tokens[2])
                        );
                        textures.add(texture);
                    }
                    break;

                case "vn":
                    if (tokens.length >= 4) {
                        Vector3f normal = new Vector3f(
                                Float.parseFloat(tokens[1]),
                                Float.parseFloat(tokens[2]),
                                Float.parseFloat(tokens[3])
                        );
                        normals.add(normal);
                    }
                    break;

                case "f":
                    for (int i = 1; i < tokens.length; i++) {
                        processFace(tokens[i], faces);
                    }
                    break;

                default:
                    // Ignore other types of lines
                    break;
            }
        }

        // Process the collected data to build the model
        float[] verticesArray = new float[vertices.size() * 3];
        float[] textureArray = new float[vertices.size() * 2];
        float[] normalsArray = new float[vertices.size() * 3];

        // Initialize arrays with default values
        for (int i = 0; i < textureArray.length; i++) {
            textureArray[i] = 0;
        }

        for (int i = 0; i < normalsArray.length; i++) {
            normalsArray[i] = i % 3 == 1 ? 1 : 0; // Default normal is (0,1,0)
        }

        // Convert vertices to array
        int vertexPointer = 0;
        for (Vector3f vertex : vertices) {
            verticesArray[vertexPointer++] = vertex.x;
            verticesArray[vertexPointer++] = vertex.y;
            verticesArray[vertexPointer++] = vertex.z;
        }

        // Process vertices from faces to set up indices, textures, and normals
        for (Vector3i face : faces) {
            processVertex(face.x, face.y, face.z, textures, normals, indices, textureArray, normalsArray);
        }

        int[] indicesArray = new int[indices.size()];
        for (int i = 0; i < indices.size(); i++) {
            indicesArray[i] = indices.get(i);
        }

        // Return the raw model with added normal data
        return loader.loadToVAO(verticesArray, textureArray, normalsArray, indicesArray);
    }

    private static void processFace(String token, List<Vector3i> faces) {
        String[] lineToken = token.split("/");
        int length = lineToken.length;
        int pos = -1, coords = -1, normal = -1;

        pos = Integer.parseInt(lineToken[0]) - 1;

        if (length > 1) {
            String textCoord = lineToken[1];
            if (textCoord.length() > 0) {
                coords = Integer.parseInt(textCoord) - 1;
            }

            if (length > 2) {
                normal = Integer.parseInt(lineToken[2]) - 1;
            }
        }

        Vector3i face = new Vector3i(pos, coords, normal);
        faces.add(face);
    }

    private static void processVertex(int pos, int texCoord, int normal,
                                      List<Vector2f> texCoordList, List<Vector3f> normalList,
                                      List<Integer> indicesList, float[] texCoordArr, float[] normalArr) {
        indicesList.add(pos);

        if (texCoord >= 0) {
            Vector2f texCoordVec = texCoordList.get(texCoord);
            texCoordArr[pos * 2] = texCoordVec.x;
            texCoordArr[pos * 2 + 1] = 1 - texCoordVec.y;
        }

        if (normal >= 0) {
            Vector3f normalVec = normalList.get(normal);
            normalArr[pos * 3] = normalVec.x;
            normalArr[pos * 3 + 1] = normalVec.y;
            normalArr[pos * 3 + 2] = normalVec.z;
        }
    }

    private static List<String> readAllLines(String filename) {
        List<String> lines = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(new File(filename)))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
        } catch (Exception e) {
            System.err.println("Error reading file: " + filename);
            e.printStackTrace();
        }

        return lines;
    }

    // Register resources with the resource tracking lists
    public static void registerVAO(int vaoID) {
        vaos.add(vaoID);
    }

    public static void registerVBO(int vboID) {
        vbos.add(vboID);
    }

    public static void registerTexture(int textureID) {
        textures.add(textureID);
    }

    // Complete cleanup method for proper resource management
    public static void cleanup() {
        // Delete all VAOs
        for (int vao : vaos) {
            GL30.glDeleteVertexArrays(vao);
        }

        // Delete all VBOs
        for (int vbo : vbos) {
            GL15.glDeleteBuffers(vbo);
        }

        // Delete all textures
        for (int texture : textures) {
            GL11.glDeleteTextures(texture);
        }

        // Clear the lists
        vaos.clear();
        vbos.clear();
        textures.clear();

        System.out.println("OBJLoader resources cleaned up successfully");
    }
}