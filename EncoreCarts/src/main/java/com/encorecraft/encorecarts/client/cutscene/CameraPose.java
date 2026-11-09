package com.encorecraft.encorecarts.client.cutscene;

/** Camera state for one frame of the travel cutscene. */
public record CameraPose(double x, double y, double z, float yaw, float pitch, float roll, float fov) {
}
