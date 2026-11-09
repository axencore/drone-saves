package com.encorecraft.encorecarts.client.cutscene;

import java.util.List;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;

/** The ten camera compositions of the travel cutscene. */
final class CameraShots {
    /** Cart speed in blocks per tick (~4 blocks/s, a brisk trot). */
    static final double SPEED = 0.2;
    private static final double ROLLING_START = 1.5;

    record Context(Level level, CutscenePath path, Vec3 cart, Vec3 forward, Vec3 right, double distance,
                   double time, double progress, Vec3 driverHead, Vec3 passengerHead) {
        Vec3 at(double right, double up, double forward) {
            return new Vec3(cart.x + this.right.x * right + this.forward.x * forward, cart.y + up,
                    cart.z + this.right.z * right + this.forward.z * forward);
        }

        Vec3 onPath(double s, double right, double up) {
            Vec3 p = path.position(s);
            return new Vec3(p.x + this.right.x * right, p.y + up, p.z + this.right.z * right);
        }
    }

    abstract static class Shot {
        final String id;

        Shot(String id) {
            this.id = id;
        }

        /** Distance travelled by the cart from the station at scene tick {@code t}. */
        double distance(double t) {
            return rolling(t);
        }

        abstract CameraPose pose(Context c);
    }

    static final List<Shot> ALL = List.of(
            new Shot("side") {
                @Override
                CameraPose pose(Context c) {
                    Vec3 cam = c.at(7.5, 1.9, Mth.lerp(c.progress(), 2.5, -2.5));
                    return look(c, cam, c.at(0, 1.0, 0.4), 0, 58, 0.15, true);
                }
            },
            new Shot("inside") {
                @Override
                CameraPose pose(Context c) {
                    Vec3 head = c.passengerHead();
                    Vec3 cam = head.add(c.right().scale(0.35)).add(c.forward().scale(-0.85)).add(0, 0.3, 0);
                    return look(c, cam, c.at(0, 1.25, 8), 0, 75, 0.25, false);
                }
            },
            new Shot("road_behind") {
                @Override
                CameraPose pose(Context c) {
                    return look(c, c.at(0.4, 2.6, -2.3), c.at(0, 0.3, -12), 0, 70, 0.2, true);
                }
            },
            new Shot("head_on") {
                @Override
                CameraPose pose(Context c) {
                    Vec3 cam = c.onPath(c.distance() + Mth.lerp(c.progress(), 12, 7), 1.1, 1.3);
                    return look(c, cam, c.at(0, 1.2, 1.8), 0, 55, 0.2, true);
                }
            },
            new Shot("roadside") {
                @Override
                CameraPose pose(Context c) {
                    Vec3 cam = c.onPath(rolling(34) + 1.5, 5.0, 1.5);
                    return look(c, cam, c.at(0, 1.0, 0), 0, 50, 0.35, true);
                }
            },
            new Shot("crane") {
                @Override
                CameraPose pose(Context c) {
                    double u = c.progress();
                    Vec3 cam = c.at(2.5, Mth.lerp(u, 6.5, 10.5), -Mth.lerp(u, 7, 3));
                    return look(c, cam, c.at(0, 0.5, 2.5), 0, 60, 0.1, true);
                }
            },
            new Shot("orbit") {
                @Override
                CameraPose pose(Context c) {
                    double angle = Math.toRadians(Mth.lerp(c.progress(), -70, 70));
                    Vec3 cam = c.at(Math.cos(angle) * 6.5, 2.3, Math.sin(angle) * 6.5);
                    return look(c, cam, c.at(0, 1.1, 0), (float) Mth.lerp(c.progress(), -2, 2), 58, 0.1, true);
                }
            },
            new Shot("wheels") {
                @Override
                CameraPose pose(Context c) {
                    Vec3 cam = c.onPath(rolling(30), 2.4, 0.35);
                    return look(c, cam, c.at(0, 0.75, 0.5), 0, 78, 0.3, true);
                }
            },
            new Shot("coachman_shoulder") {
                @Override
                CameraPose pose(Context c) {
                    Vec3 head = c.driverHead();
                    Vec3 cam = head.add(c.right().scale(0.45)).add(c.forward().scale(-0.75)).add(0, 0.35, 0);
                    return look(c, cam, c.at(0, 1.0, 9), 0, 72, 0.3, false);
                }
            },
            new Shot("departure") {
                @Override
                double distance(double t) {
                    double accelTicks = 20;
                    double acceleration = SPEED / accelTicks;
                    return t < accelTicks ? 0.5 * acceleration * t * t : 0.5 * acceleration * accelTicks * accelTicks + SPEED * (t - accelTicks);
                }

                @Override
                CameraPose pose(Context c) {
                    Vec3 start = c.path().start();
                    Vec3 cam = new Vec3(start.x + c.right().x * 3.4 - c.forward().x * 3.8, start.y + 1.7,
                            start.z + c.right().z * 3.4 - c.forward().z * 3.8);
                    double u = c.progress();
                    return look(c, cam, c.at(0, 1.2, 0), 0, (float) Mth.lerp(u * u, 70, 38), 0.2, true);
                }
            });

    private CameraShots() {
    }

    static Shot get(int index) {
        return ALL.get(Math.floorMod(index, ALL.size()));
    }

    static double rolling(double t) {
        return ROLLING_START + SPEED * t;
    }

    private static CameraPose look(Context c, Vec3 desired, Vec3 target, float roll, float fov, double shake, boolean avoidBlocks) {
        Vec3 cam = avoidBlocks ? unobstructed(c.level(), target, desired) : desired;
        double dx = target.x - cam.x;
        double dy = target.y - cam.y;
        double dz = target.z - cam.z;
        float yaw = (float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90.0F;
        float pitch = (float) -(Mth.atan2(dy, Math.sqrt(dx * dx + dz * dz)) * Mth.RAD_TO_DEG);
        double t = c.time();
        yaw += (float) (shake * (Math.sin(t * 0.13) + 0.5 * Math.sin(t * 0.31 + 1.7)));
        pitch += (float) (shake * 0.7 * (Math.sin(t * 0.17 + 0.4) + 0.5 * Math.sin(t * 0.37 + 2.3)));
        return new CameraPose(cam.x, cam.y, cam.z, yaw, pitch, roll, fov);
    }

    /** Pulls the camera towards the target if terrain is in between, like the vanilla third-person camera. */
    private static Vec3 unobstructed(Level level, Vec3 target, Vec3 desired) {
        BlockHitResult hit = level.clip(new ClipContext(target, desired, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE,
                CollisionContext.empty()));
        if (hit.getType() == HitResult.Type.MISS) {
            return desired;
        }
        Vec3 dir = desired.subtract(target).normalize();
        return hit.getLocation().subtract(dir.scale(0.3));
    }
}
