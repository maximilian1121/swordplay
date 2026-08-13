package ca.maximilian.swordfight;

import net.minestom.server.coordinate.Pos;

public class Util {
    public static Pos calculateLookAt(Pos source, Pos target) {
        double dx = target.x() - source.x();
        double dy = target.y() - source.y();
        double dz = target.z() - source.z();

        double distanceXZ = Math.sqrt(dx * dx + dz * dz);

        double yaw = Math.toDegrees(Math.atan2(dz, dx)) - 90;
        if (yaw < 0) {
            yaw += 360;
        }

        double pitch = -Math.toDegrees(Math.atan2(dy, distanceXZ));

        return new Pos(source.x(), source.y(), source.z(), (float) yaw, (float) pitch);
    }
}
