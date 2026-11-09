package com.encorecraft.encorecarts.client.model.bb;

/**
 * One textured quad, positions relative to the owning bone pivot (pixels), UVs normalised to 0..1,
 * vertices ordered counter-clockwise when seen from the front. Triangles repeat their last vertex.
 */
public record BbQuad(int texture, float[] positions, float[] uvs, float normalX, float normalY, float normalZ) {
}
