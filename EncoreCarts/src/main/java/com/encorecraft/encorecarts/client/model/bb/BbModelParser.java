package com.encorecraft.encorecarts.client.model.bb;

import com.encorecraft.encorecarts.EncoreCarts;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.platform.NativeImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Reads Blockbench project files ({@code .bbmodel}, "free"/"modded entity" style) as saved by Blockbench 4.x and 5.x:
 * groups, cubes (per-face and box UV, inflate, rotation), meshes, embedded textures and animations.
 * Hidden or non-exported groups/elements are skipped.
 */
public final class BbModelParser {
    private static final String[] FACES = {"east", "west", "up", "down", "south", "north"};

    private final String debugName;
    private final boolean legacyAxes;
    private final boolean projectBoxUv;
    private final float resolutionWidth;
    private final float resolutionHeight;
    private final List<BbTexture> textures = new ArrayList<>();
    private final Map<String, Integer> textureIndexByUuid = new HashMap<>();
    private final Map<String, JsonObject> elements = new HashMap<>();
    private final List<BbBone> bones = new ArrayList<>();
    @Nullable
    private BbBone implicitRoot;

    private BbModelParser(JsonObject root, String debugName) {
        this.debugName = debugName;
        JsonObject meta = root.has("meta") ? root.getAsJsonObject("meta") : new JsonObject();
        this.legacyAxes = compareVersions(string(meta, "format_version", "4.0"), "5.0") < 0;
        this.projectBoxUv = bool(meta, "box_uv", false);
        JsonObject resolution = root.has("resolution") ? root.getAsJsonObject("resolution") : new JsonObject();
        this.resolutionWidth = number(resolution, "width", 16);
        this.resolutionHeight = number(resolution, "height", 16);
    }

    public static BbModel parse(JsonObject root, String debugName) {
        return new BbModelParser(root, debugName).build(root);
    }

    private BbModel build(JsonObject root) {
        if (root.has("textures")) {
            for (JsonElement element : root.getAsJsonArray("textures")) {
                readTexture(element.getAsJsonObject());
            }
        }
        if (root.has("elements")) {
            for (JsonElement element : root.getAsJsonArray("elements")) {
                JsonObject obj = element.getAsJsonObject();
                elements.put(string(obj, "uuid", UUID.randomUUID().toString()), obj);
            }
        }
        if (root.has("outliner")) {
            for (JsonElement node : root.getAsJsonArray("outliner")) {
                if (node.isJsonObject()) {
                    readGroup(node.getAsJsonObject(), null, true);
                } else if (node.isJsonPrimitive()) {
                    addElement(implicitRoot(), elements.get(node.getAsString()));
                }
            }
        }
        Map<String, BbAnimation> animations = new LinkedHashMap<>();
        if (root.has("animations")) {
            for (JsonElement element : root.getAsJsonArray("animations")) {
                BbAnimation animation = readAnimation(element.getAsJsonObject());
                animations.putIfAbsent(animation.name, animation);
            }
        }
        return new BbModel(string(root, "name", debugName), bones, textures, animations);
    }

    private BbBone implicitRoot() {
        if (implicitRoot == null) {
            implicitRoot = new BbBone(bones.size(), "__root__", "__root__", null, 0, 0, 0, 0, 0, 0, true);
            bones.add(implicitRoot);
        }
        return implicitRoot;
    }

    private void readTexture(JsonObject obj) {
        String name = string(obj, "name", "texture" + textures.size());
        float uvWidth = number(obj, "uv_width", resolutionWidth);
        float uvHeight = number(obj, "uv_height", resolutionHeight);
        NativeImage image = null;
        String source = string(obj, "source", null);
        if (source != null && source.startsWith("data:")) {
            try {
                byte[] bytes = Base64.getDecoder().decode(source.substring(source.indexOf(',') + 1));
                image = NativeImage.read(new ByteArrayInputStream(bytes));
            } catch (IOException | IllegalArgumentException e) {
                EncoreCarts.LOGGER.warn("Model {}: cannot decode texture {}", debugName, name, e);
            }
        }
        if (obj.has("uuid")) {
            textureIndexByUuid.put(obj.get("uuid").getAsString(), textures.size());
        }
        textures.add(new BbTexture(name, uvWidth, uvHeight, image));
    }

    private void readGroup(JsonObject obj, @Nullable BbBone parent, boolean parentVisible) {
        float[] origin = vec3(obj, "origin", 0, 0, 0);
        float[] rotation = vec3(obj, "rotation", 0, 0, 0);
        boolean visible = parentVisible && bool(obj, "visibility", true) && bool(obj, "export", true);
        BbBone bone = new BbBone(bones.size(), string(obj, "uuid", UUID.randomUUID().toString()), string(obj, "name", "group"),
                parent, origin[0], origin[1], origin[2],
                (float) Math.toRadians(rotation[0]), (float) Math.toRadians(rotation[1]), (float) Math.toRadians(rotation[2]), visible);
        bones.add(bone);
        if (parent != null) {
            parent.children.add(bone);
        }
        if (obj.has("children")) {
            for (JsonElement child : obj.getAsJsonArray("children")) {
                if (child.isJsonObject()) {
                    readGroup(child.getAsJsonObject(), bone, visible);
                } else if (child.isJsonPrimitive()) {
                    addElement(bone, elements.get(child.getAsString()));
                }
            }
        }
    }

    private void addElement(BbBone bone, @Nullable JsonObject element) {
        if (element == null || !bool(element, "visibility", true) || !bool(element, "export", true)) {
            return;
        }
        String type = string(element, "type", "cube");
        if ("cube".equals(type)) {
            addCube(bone, element);
        } else if ("mesh".equals(type)) {
            addMesh(bone, element);
        }
    }

    private void addCube(BbBone bone, JsonObject cube) {
        float[] from = vec3(cube, "from", 0, 0, 0);
        float[] to = vec3(cube, "to", 0, 0, 0);
        float[] origin = vec3(cube, "origin", 0, 0, 0);
        float[] rotation = vec3(cube, "rotation", 0, 0, 0);
        float[] stretch = vec3(cube, "stretch", 1, 1, 1);
        float inflate = number(cube, "inflate", 0);
        float[] f = new float[3];
        float[] t = new float[3];
        for (int i = 0; i < 3; i++) {
            float half = (to[i] - from[i]) / 2;
            float center = from[i] + half;
            f[i] = center - (half + inflate) * stretch[i];
            t[i] = center + (half + inflate) * stretch[i];
        }
        Quaternionf rot = new Quaternionf().rotationZYX((float) Math.toRadians(rotation[2]),
                (float) Math.toRadians(rotation[1]), (float) Math.toRadians(rotation[0]));
        boolean boxUv = cube.has("box_uv") ? bool(cube, "box_uv", false) : projectBoxUv;
        Map<String, float[]> boxUvs = boxUv ? boxUv(cube, from, to) : Map.of();
        JsonObject faces = cube.has("faces") ? cube.getAsJsonObject("faces") : new JsonObject();
        for (String dir : FACES) {
            if (!faces.has(dir) || !faces.get(dir).isJsonObject()) {
                continue;
            }
            JsonObject face = faces.getAsJsonObject(dir);
            Integer texture = textureIndex(face.get("texture"));
            if (texture == null) {
                continue;
            }
            float[] uv = boxUv ? boxUvs.get(dir) : vec4(face, "uv");
            if (uv == null) {
                continue;
            }
            float[][] corners = faceCorners(dir, f, t);
            float[][] uvs = {{uv[0], uv[1]}, {uv[2], uv[1]}, {uv[0], uv[3]}, {uv[2], uv[3]}};
            for (int r = (int) number(face, "rotation", 0); r > 0; r -= 90) {
                float[] a = uvs[0];
                uvs[0] = uvs[2];
                uvs[2] = uvs[3];
                uvs[3] = uvs[1];
                uvs[1] = a;
            }
            // Blockbench triangles (0,2,1) and (2,3,1) form the counter-clockwise quad 0,2,3,1.
            int[] order = {0, 2, 3, 1};
            float[] positions = new float[12];
            float[] uvOut = new float[8];
            BbTexture tex = textures.get(texture);
            for (int i = 0; i < 4; i++) {
                float[] c = corners[order[i]];
                Vector3f p = new Vector3f(c[0] - origin[0], c[1] - origin[1], c[2] - origin[2]).rotate(rot);
                positions[i * 3] = p.x + origin[0] - bone.pivotX;
                positions[i * 3 + 1] = p.y + origin[1] - bone.pivotY;
                positions[i * 3 + 2] = p.z + origin[2] - bone.pivotZ;
                uvOut[i * 2] = uvs[order[i]][0] / tex.uvWidth;
                uvOut[i * 2 + 1] = uvs[order[i]][1] / tex.uvHeight;
            }
            Vector3f normal = faceNormal(dir).rotate(rot);
            bone.quads.add(new BbQuad(texture, positions, uvOut, normal.x, normal.y, normal.z));
        }
    }

    /** Vertex layout of Blockbench's box geometry (three_custom.js setShape). */
    private static float[][] faceCorners(String dir, float[] f, float[] t) {
        return switch (dir) {
            case "east" -> new float[][]{{t[0], t[1], t[2]}, {t[0], t[1], f[2]}, {t[0], f[1], t[2]}, {t[0], f[1], f[2]}};
            case "west" -> new float[][]{{f[0], t[1], f[2]}, {f[0], t[1], t[2]}, {f[0], f[1], f[2]}, {f[0], f[1], t[2]}};
            case "up" -> new float[][]{{f[0], t[1], f[2]}, {t[0], t[1], f[2]}, {f[0], t[1], t[2]}, {t[0], t[1], t[2]}};
            case "down" -> new float[][]{{f[0], f[1], t[2]}, {t[0], f[1], t[2]}, {f[0], f[1], f[2]}, {t[0], f[1], f[2]}};
            case "south" -> new float[][]{{f[0], t[1], t[2]}, {t[0], t[1], t[2]}, {f[0], f[1], t[2]}, {t[0], f[1], t[2]}};
            default -> new float[][]{{t[0], t[1], f[2]}, {f[0], t[1], f[2]}, {t[0], f[1], f[2]}, {f[0], f[1], f[2]}};
        };
    }

    private static Vector3f faceNormal(String dir) {
        return switch (dir) {
            case "east" -> new Vector3f(1, 0, 0);
            case "west" -> new Vector3f(-1, 0, 0);
            case "up" -> new Vector3f(0, 1, 0);
            case "down" -> new Vector3f(0, -1, 0);
            case "south" -> new Vector3f(0, 0, 1);
            default -> new Vector3f(0, 0, -1);
        };
    }

    /** Box UV layout (Blockbench cube.js updateUV). */
    private Map<String, float[]> boxUv(JsonObject cube, float[] from, float[] to) {
        float sx = (float) Math.floor(to[0] - from[0] + 1e-4);
        float sy = (float) Math.floor(to[1] - from[1] + 1e-4);
        float sz = (float) Math.floor(to[2] - from[2] + 1e-4);
        float[] offset = vec2(cube, "uv_offset");
        Map<String, float[][]> layout = new LinkedHashMap<>();
        layout.put("east", new float[][]{{0, sz}, {sz, sy}});
        layout.put("west", new float[][]{{sz + sx, sz}, {sz, sy}});
        layout.put("up", new float[][]{{sz + sx, sz}, {-sx, -sz}});
        layout.put("down", new float[][]{{sz + sx * 2, 0}, {-sx, sz}});
        layout.put("south", new float[][]{{sz * 2 + sx, sz}, {sx, sy}});
        layout.put("north", new float[][]{{sz, sz}, {sx, sy}});
        if (bool(cube, "mirror_uv", false)) {
            layout.values().forEach(entry -> {
                entry[0][0] += entry[1][0];
                entry[1][0] *= -1;
            });
            float[][] east = layout.get("east");
            layout.put("east", layout.get("west"));
            layout.put("west", east);
        }
        Map<String, float[]> result = new HashMap<>();
        layout.forEach((dir, entry) -> result.put(dir, new float[]{
                entry[0][0] + offset[0], entry[0][1] + offset[1],
                entry[0][0] + entry[1][0] + offset[0], entry[0][1] + entry[1][1] + offset[1]}));
        return result;
    }

    private void addMesh(BbBone bone, JsonObject mesh) {
        if (!mesh.has("vertices") || !mesh.has("faces")) {
            return;
        }
        float[] origin = vec3(mesh, "origin", 0, 0, 0);
        float[] rotation = vec3(mesh, "rotation", 0, 0, 0);
        Quaternionf rot = new Quaternionf().rotationZYX((float) Math.toRadians(rotation[2]),
                (float) Math.toRadians(rotation[1]), (float) Math.toRadians(rotation[0]));
        Map<String, Vector3f> vertices = new HashMap<>();
        for (Map.Entry<String, JsonElement> entry : mesh.getAsJsonObject("vertices").entrySet()) {
            JsonArray a = entry.getValue().getAsJsonArray();
            Vector3f v = new Vector3f(a.get(0).getAsFloat(), a.get(1).getAsFloat(), a.get(2).getAsFloat()).rotate(rot);
            vertices.put(entry.getKey(), v.add(origin[0] - bone.pivotX, origin[1] - bone.pivotY, origin[2] - bone.pivotZ));
        }
        for (Map.Entry<String, JsonElement> entry : mesh.getAsJsonObject("faces").entrySet()) {
            JsonObject face = entry.getValue().getAsJsonObject();
            Integer texture = textureIndex(face.get("texture"));
            if (texture == null || !face.has("vertices")) {
                continue;
            }
            List<String> keys = new ArrayList<>();
            face.getAsJsonArray("vertices").forEach(k -> keys.add(k.getAsString()));
            keys.removeIf(k -> !vertices.containsKey(k));
            if (keys.size() < 3 || keys.size() > 4) {
                continue;
            }
            Vector3f a = vertices.get(keys.get(0));
            Vector3f normal = new Vector3f(vertices.get(keys.get(1))).sub(a)
                    .cross(new Vector3f(vertices.get(keys.get(2))).sub(a));
            if (normal.lengthSquared() > 0) {
                normal.normalize();
            }
            if (keys.size() == 4) {
                sortAroundCentroid(keys, vertices, normal);
            }
            JsonObject uvMap = face.has("uv") ? face.getAsJsonObject("uv") : new JsonObject();
            BbTexture tex = textures.get(texture);
            float[] positions = new float[12];
            float[] uvs = new float[8];
            for (int i = 0; i < 4; i++) {
                String key = keys.get(Math.min(i, keys.size() - 1));
                Vector3f v = vertices.get(key);
                positions[i * 3] = v.x;
                positions[i * 3 + 1] = v.y;
                positions[i * 3 + 2] = v.z;
                float[] uv = uvMap.has(key) ? vec2(uvMap, key) : new float[]{0, 0};
                uvs[i * 2] = uv[0] / tex.uvWidth;
                uvs[i * 2 + 1] = uv[1] / tex.uvHeight;
            }
            bone.quads.add(new BbQuad(texture, positions, uvs, normal.x, normal.y, normal.z));
        }
    }

    private static void sortAroundCentroid(List<String> keys, Map<String, Vector3f> vertices, Vector3f normal) {
        Vector3f centroid = new Vector3f();
        keys.forEach(k -> centroid.add(vertices.get(k)));
        centroid.div(keys.size());
        Vector3f axisU = new Vector3f(vertices.get(keys.get(0))).sub(centroid);
        if (axisU.lengthSquared() == 0 || normal.lengthSquared() == 0) {
            return;
        }
        axisU.normalize();
        Vector3f axisV = new Vector3f(normal).cross(axisU);
        keys.sort(Comparator.comparingDouble(k -> {
            Vector3f d = new Vector3f(vertices.get(k)).sub(centroid);
            return Math.atan2(d.dot(axisV), d.dot(axisU));
        }));
    }

    private BbAnimation readAnimation(JsonObject obj) {
        String name = string(obj, "name", "animation");
        List<BbAnimation.BoneTrack> tracks = new ArrayList<>();
        if (obj.has("animators")) {
            for (Map.Entry<String, JsonElement> entry : obj.getAsJsonObject("animators").entrySet()) {
                JsonObject animator = entry.getValue().getAsJsonObject();
                String type = string(animator, "type", "bone");
                if (!"bone".equals(type) || !animator.has("keyframes")) {
                    continue;
                }
                List<BbAnimation.Keyframe> rotation = new ArrayList<>();
                List<BbAnimation.Keyframe> position = new ArrayList<>();
                List<BbAnimation.Keyframe> scale = new ArrayList<>();
                for (JsonElement kfElement : animator.getAsJsonArray("keyframes")) {
                    JsonObject kf = kfElement.getAsJsonObject();
                    String channel = string(kf, "channel", "");
                    switch (channel) {
                        case "rotation" -> rotation.add(readKeyframe(kf, BbAnimation.Channel.ROTATION));
                        case "position" -> position.add(readKeyframe(kf, BbAnimation.Channel.POSITION));
                        case "scale" -> scale.add(readKeyframe(kf, BbAnimation.Channel.SCALE));
                        default -> {
                        }
                    }
                }
                Comparator<BbAnimation.Keyframe> byTime = Comparator.comparingDouble(k -> k.time);
                rotation.sort(byTime);
                position.sort(byTime);
                scale.sort(byTime);
                tracks.add(new BbAnimation.BoneTrack(entry.getKey(), string(animator, "name", ""),
                        rotation.toArray(BbAnimation.Keyframe[]::new), position.toArray(BbAnimation.Keyframe[]::new),
                        scale.toArray(BbAnimation.Keyframe[]::new)));
            }
        }
        return new BbAnimation(name, BbAnimation.parseLoop(string(obj, "loop", "once")), number(obj, "length", 0), tracks);
    }

    private BbAnimation.Keyframe readKeyframe(JsonObject kf, BbAnimation.Channel channel) {
        double fallback = channel == BbAnimation.Channel.SCALE ? 1 : 0;
        JsonArray dataPoints = kf.has("data_points") ? kf.getAsJsonArray("data_points") : new JsonArray();
        int count = Math.max(1, Math.min(2, dataPoints.size()));
        Molang.Expr[][] points = new Molang.Expr[count][3];
        String[] axes = {"x", "y", "z"};
        for (int p = 0; p < count; p++) {
            JsonObject dp = p < dataPoints.size() ? dataPoints.get(p).getAsJsonObject() : new JsonObject();
            for (int axis = 0; axis < 3; axis++) {
                points[p][axis] = expression(dp.get(axes[axis]), fallback);
            }
        }
        float[] leftTime = vec3(kf, "bezier_left_time", -0.1F, -0.1F, -0.1F);
        float[] leftValue = vec3(kf, "bezier_left_value", 0, 0, 0);
        float[] rightTime = vec3(kf, "bezier_right_time", 0.1F, 0.1F, 0.1F);
        float[] rightValue = vec3(kf, "bezier_right_value", 0, 0, 0);
        if (legacyAxes && channel != BbAnimation.Channel.SCALE) {
            // Blockbench < 5 stored X (and rotation Y) inverted; 5.0 flips them on load, so do we.
            for (Molang.Expr[] point : points) {
                point[0] = point[0].negate();
                if (channel == BbAnimation.Channel.ROTATION) {
                    point[1] = point[1].negate();
                }
            }
            leftValue[0] = -leftValue[0];
            rightValue[0] = -rightValue[0];
            if (channel == BbAnimation.Channel.ROTATION) {
                leftValue[1] = -leftValue[1];
                rightValue[1] = -rightValue[1];
            }
        }
        return new BbAnimation.Keyframe(number(kf, "time", 0),
                BbAnimation.parseInterpolation(string(kf, "interpolation", "linear")),
                points, leftTime, leftValue, rightTime, rightValue);
    }

    private static Molang.Expr expression(@Nullable JsonElement element, double fallback) {
        if (element == null || element.isJsonNull()) {
            return Molang.constant(fallback);
        }
        if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber()) {
            return Molang.constant(element.getAsDouble());
        }
        String text = element.getAsString();
        return text.isBlank() ? Molang.constant(fallback) : Molang.compile(text);
    }

    @Nullable
    private Integer textureIndex(@Nullable JsonElement element) {
        if (element == null || element.isJsonNull()) {
            return null;
        }
        if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber()) {
            int index = element.getAsInt();
            return index >= 0 && index < textures.size() ? index : null;
        }
        if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isBoolean()) {
            return null;
        }
        return textureIndexByUuid.get(element.getAsString());
    }

    static int compareVersions(String a, String b) {
        String[] pa = a.split("\\.");
        String[] pb = b.split("\\.");
        for (int i = 0; i < Math.max(pa.length, pb.length); i++) {
            int va = i < pa.length ? parseIntSafe(pa[i]) : 0;
            int vb = i < pb.length ? parseIntSafe(pb[i]) : 0;
            if (va != vb) {
                return Integer.compare(va, vb);
            }
        }
        return 0;
    }

    private static int parseIntSafe(String s) {
        try {
            return Integer.parseInt(s.replaceAll("\\D", ""));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static String string(JsonObject obj, String key, String fallback) {
        JsonElement e = obj.get(key);
        return e != null && e.isJsonPrimitive() ? e.getAsString() : fallback;
    }

    private static boolean bool(JsonObject obj, String key, boolean fallback) {
        JsonElement e = obj.get(key);
        return e != null && e.isJsonPrimitive() && e.getAsJsonPrimitive().isBoolean() ? e.getAsBoolean() : fallback;
    }

    private static float number(JsonObject obj, String key, float fallback) {
        JsonElement e = obj.get(key);
        if (e == null || !e.isJsonPrimitive()) {
            return fallback;
        }
        try {
            return e.getAsFloat();
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    private static float[] vec3(JsonObject obj, String key, float x, float y, float z) {
        JsonElement e = obj.get(key);
        if (e == null || !e.isJsonArray() || e.getAsJsonArray().size() < 3) {
            return new float[]{x, y, z};
        }
        JsonArray a = e.getAsJsonArray();
        return new float[]{parse(a.get(0), x), parse(a.get(1), y), parse(a.get(2), z)};
    }

    @Nullable
    private static float[] vec4(JsonObject obj, String key) {
        JsonElement e = obj.get(key);
        if (e == null || !e.isJsonArray() || e.getAsJsonArray().size() < 4) {
            return null;
        }
        JsonArray a = e.getAsJsonArray();
        return new float[]{parse(a.get(0), 0), parse(a.get(1), 0), parse(a.get(2), 0), parse(a.get(3), 0)};
    }

    private static float[] vec2(JsonObject obj, String key) {
        JsonElement e = obj.get(key);
        if (e == null || !e.isJsonArray() || e.getAsJsonArray().size() < 2) {
            return new float[]{0, 0};
        }
        JsonArray a = e.getAsJsonArray();
        return new float[]{parse(a.get(0), 0), parse(a.get(1), 0)};
    }

    private static float parse(JsonElement e, float fallback) {
        try {
            return e.getAsFloat();
        } catch (RuntimeException ex) {
            return fallback;
        }
    }
}
