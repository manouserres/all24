package org.team100.lib.motion.drivetrain.manual;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.team100.lib.motion.drivetrain.SpeedLimits;

import edu.wpi.first.math.geometry.Twist2d;
import edu.wpi.first.math.kinematics.ChassisSpeeds;

public class ManualChassisSpeedsTest {
    private static final double kDelta = 0.001;

    @Test
    void testChassisSpeedsZero() {
        SpeedLimits limits = new SpeedLimits(1, 1, 1, 1);
        ManualChassisSpeeds manual = new ManualChassisSpeeds(limits);
        Twist2d input =  new Twist2d();
        ChassisSpeeds speeds = manual.apply(input);
        assertEquals(0, speeds.vxMetersPerSecond, kDelta);
        assertEquals(0, speeds.vyMetersPerSecond, kDelta);
        assertEquals(0, speeds.omegaRadiansPerSecond, kDelta);
    }

    @Test
    void testChassisSpeedsNonzero() {
        SpeedLimits limits = new SpeedLimits(1, 1, 1, 1);
        ManualChassisSpeeds manual = new ManualChassisSpeeds(limits);
        Twist2d input = new Twist2d(1, 2, 3);
        ChassisSpeeds speeds = manual.apply(input);
        assertEquals(1, speeds.vxMetersPerSecond, kDelta);
        assertEquals(1, speeds.vyMetersPerSecond, kDelta); // speed limit
        assertEquals(1, speeds.omegaRadiansPerSecond, kDelta); // speed limit
    }
}