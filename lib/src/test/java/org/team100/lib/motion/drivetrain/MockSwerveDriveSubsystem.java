package org.team100.lib.motion.drivetrain;

import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Translation2d;
import edu.wpi.first.math.geometry.Twist2d;
import edu.wpi.first.math.kinematics.ChassisSpeeds;
import edu.wpi.first.math.kinematics.SwerveDriveKinematics;
import edu.wpi.first.math.kinematics.SwerveModuleState;

public class MockSwerveDriveSubsystem implements SwerveDriveSubsystemInterface {

    public Pose2d pose = new Pose2d();
    ChassisSpeeds speeds = new ChassisSpeeds();
    public Twist2d twist = new Twist2d();
    public boolean stopped = false;
    SwerveDriveKinematics kinematics = new SwerveDriveKinematics(
            new Translation2d(0.1, 0.1),
            new Translation2d(0.1, -0.1),
            new Translation2d(-0.1, 0.1),
            new Translation2d(-0.1, -0.1));

    @Override
    public Pose2d getPose() {
        return pose;
    }

    @Override
    public void resetPose(Pose2d robotPose) {
        pose = robotPose;
    }

    @Override
    public void stop() {
        stopped = true;
    }

    @Override
    public SwerveDriveSubsystem get() {
        return null;
    }

    @Override
    public ChassisSpeeds speeds() {
        return speeds;
    }

    @Override
    public void driveInFieldCoords(Twist2d twist) {
        this.twist=twist;
    }

    @Override
    public void setChassisSpeeds(ChassisSpeeds speeds) {
        this.speeds = speeds;
    }

    @Override
    public void setRawModuleStates(SwerveModuleState[] states) {
        this.speeds = kinematics.toChassisSpeeds(states);
    }
}