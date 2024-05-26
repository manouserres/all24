package org.team100.lib.timing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.team100.lib.geometry.GeometryUtil;
import org.team100.lib.geometry.Pose2dWithMotion;
import org.team100.lib.motion.drivetrain.kinodynamics.SwerveKinodynamics;
import org.team100.lib.motion.drivetrain.kinodynamics.SwerveKinodynamicsFactory;
import org.team100.lib.path.Path100;
import org.team100.lib.path.PathDistanceSampler;
import org.team100.lib.path.PathIndexSampler;
import org.team100.lib.timing.TimingConstraint.MinMaxAcceleration;
import org.team100.lib.trajectory.Trajectory100;

import com.fasterxml.jackson.databind.KeyDeserializer;

import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.geometry.Translation2d;
import edu.wpi.first.math.geometry.Twist2d;

public class TimingUtilTest {

    public static final double kTestEpsilon = 1e-12;

    public static final List<Pose2dWithMotion> kWaypoints = Arrays.asList(
            new Pose2dWithMotion(new Pose2d(new Translation2d(0.0, 0.0), GeometryUtil.kRotationZero)),
            new Pose2dWithMotion(new Pose2d(new Translation2d(24.0, 0.0), GeometryUtil.kRotationZero)),
            new Pose2dWithMotion(new Pose2d(new Translation2d(36.0, 12.0), GeometryUtil.kRotationZero)),
            new Pose2dWithMotion(new Pose2d(new Translation2d(60.0, 12.0), GeometryUtil.kRotationZero)));

    public static final List<Rotation2d> kHeadings = List.of(
            GeometryUtil.fromDegrees(0),
            GeometryUtil.fromDegrees(0),
            GeometryUtil.fromDegrees(0),
            GeometryUtil.fromDegrees(0));

    public Trajectory100 buildAndCheckTrajectory(
            final PathDistanceSampler dist_view,
            double step_size,
            List<TimingConstraint> constraints,
            double start_vel,
            double end_vel,
            double max_vel,
            double max_acc) {
        TimingUtil u = new TimingUtil(constraints, max_vel, max_acc);
        Trajectory100 timed_traj = u.timeParameterizeTrajectory(
                dist_view,
                step_size,
                start_vel,
                end_vel);
        System.out.println("traj " + timed_traj.length());
        checkTrajectory(timed_traj, constraints, start_vel, end_vel, max_vel, max_acc);
        return timed_traj;
    }

    public Trajectory100 buildAndCheckTrajectory(
            final PathIndexSampler dist_view,
            double step_size,
            List<TimingConstraint> constraints,
            double start_vel,
            double end_vel,
            double max_vel,
            double max_acc) {
        TimingUtil u = new TimingUtil(constraints, max_vel, max_acc);
        Trajectory100 timed_traj = u.timeParameterizeTrajectory(
                dist_view,
                step_size,
                start_vel,
                end_vel);
        System.out.println("traj " + timed_traj.length());
        checkTrajectory(timed_traj, constraints, start_vel, end_vel, max_vel, max_acc);
        return timed_traj;
    }

    public void checkTrajectory(
            final Trajectory100 traj,
            List<TimingConstraint> constraints,
            double start_vel,
            double end_vel,
            double max_vel,
            double max_acc) {
        assertFalse(traj.isEmpty());
        assertEquals(traj.getPoint(0).state().velocityM_S(), start_vel, kTestEpsilon);
        assertEquals(traj.getPoint(traj.length() - 1).state().velocityM_S(), end_vel, kTestEpsilon);

        // Go state by state, verifying all constraints are satisfied and integration is
        // correct.
        for (int i = 0; i < traj.length(); ++i) {
            final TimedPose state = traj.getPoint(i).state();
            for (final TimingConstraint constraint : constraints) {
                assertTrue(state.velocityM_S() - kTestEpsilon <= constraint.getMaxVelocity(state.state()).getValue());
                final MinMaxAcceleration accel_limits = constraint.getMinMaxAcceleration(state.state(),
                        state.velocityM_S());
                assertTrue(state.acceleration() - kTestEpsilon <= accel_limits.getMaxAccel(),
                        String.format("%f %f", state.acceleration(), accel_limits.getMaxAccel()));
                assertTrue(state.acceleration() + kTestEpsilon >= accel_limits.getMinAccel(),
                        String.format("%f %f", state.acceleration(), accel_limits.getMinAccel()));

            }
            if (i > 0) {
                final TimedPose prev_state = traj.getPoint(i - 1).state();
                assertEquals(state.velocityM_S(),
                        prev_state.velocityM_S()
                                + (state.getTimeS() - prev_state.getTimeS()) * prev_state.acceleration(),
                        kTestEpsilon);
            }
        }
    }

    /**
     * Like the test below but with turning at the corners.
     * 
     * TODO: This is wrong.
     */
    @Test
    void testTurningInPlace() {
        Path100 traj = new Path100(Arrays.asList(
                new Pose2dWithMotion(new Pose2d(new Translation2d(0.0, 0.0), GeometryUtil.kRotationZero)),
                new Pose2dWithMotion(new Pose2d(new Translation2d(24.0, 0.0), GeometryUtil.kRotationZero)),
                new Pose2dWithMotion(new Pose2d(new Translation2d(24.0, 0.0), GeometryUtil.kRotation180)),
                new Pose2dWithMotion(new Pose2d(new Translation2d(36.0, 12.0), GeometryUtil.kRotation180)),
                new Pose2dWithMotion(new Pose2d(new Translation2d(36.0, 12.0), GeometryUtil.kRotationZero)),
                new Pose2dWithMotion(new Pose2d(new Translation2d(60.0, 12.0), GeometryUtil.kRotationZero))));
        PathDistanceSampler dist_view = new PathDistanceSampler(traj);

        // Triangle profile.
        Trajectory100 timed_traj = buildAndCheckTrajectory(dist_view,
                1.0,
                new ArrayList<TimingConstraint>(), 0.0, 0.0, 20.0, 5.0);
        assertEquals(66, timed_traj.length());
        assertNotNull(timed_traj);
        System.out.println(timed_traj);

        // Trapezoidal profile.
        timed_traj = buildAndCheckTrajectory(dist_view, 1.0, new ArrayList<TimingConstraint>(), 0.0, 0.0,
                10.0, 5.0);
        assertEquals(66, timed_traj.length());

        // Trapezoidal profile with start and end velocities.
        timed_traj = buildAndCheckTrajectory(dist_view, 1.0, new ArrayList<TimingConstraint>(), 5.0, 2.0,
                10.0, 5.0);
        assertEquals(66, timed_traj.length());
    }

    /**
     * Like the test below but with turning at the corners.
     * 
     * TODO: This fails :(
     */
    // @Test
    void testTurningInPlaceIndexed() {
        // TODO: these poses are all motionless which is wrong.
        Path100 traj = new Path100(Arrays.asList(
                new Pose2dWithMotion(
                        new Pose2d(
                                new Translation2d(0.0, 0.0),
                                GeometryUtil.kRotationZero),
                        new Twist2d(1, 0, 0), 0, 0),
                new Pose2dWithMotion(
                        new Pose2d(
                                new Translation2d(24.0, 0.0),
                                GeometryUtil.kRotationZero),
                        new Twist2d(0, 0, 0), 0, 0),
                new Pose2dWithMotion(
                        new Pose2d(
                                new Translation2d(24.0, 0.0),
                                GeometryUtil.kRotation180),
                        new Twist2d(0, 0, 1), 0, 0),
                new Pose2dWithMotion(
                        new Pose2d(
                                new Translation2d(36.0, 12.0),
                                GeometryUtil.kRotation180),
                        new Twist2d(1, 1, 0), 0, 0),
                new Pose2dWithMotion(
                        new Pose2d(
                                new Translation2d(36.0, 12.0),
                                GeometryUtil.kRotationZero),
                        new Twist2d(0, 0, 1), 0, 0),
                new Pose2dWithMotion(
                        new Pose2d(
                                new Translation2d(60.0, 12.0),
                                GeometryUtil.kRotationZero),
                        new Twist2d(1, 0, 0), 0, 0)));
        PathIndexSampler dist_view = new PathIndexSampler(traj);

        // Triangle profile.
        Trajectory100 timed_traj = buildAndCheckTrajectory(dist_view,
                1.0,
                new ArrayList<TimingConstraint>(), 0.0, 0.0, 20.0, 5.0);
        assertEquals(66, timed_traj.length());
        assertNotNull(timed_traj);
        System.out.println(timed_traj);

        // Trapezoidal profile.
        timed_traj = buildAndCheckTrajectory(dist_view,
                1.0,
                new ArrayList<TimingConstraint>(),
                0.0, 0.0,
                10.0, 5.0);
        assertEquals(66, timed_traj.length());

        // Trapezoidal profile with start and end velocities.
        timed_traj = buildAndCheckTrajectory(dist_view,
                1.0,
                new ArrayList<TimingConstraint>(),
                5.0, 2.0,
                10.0, 5.0);
        assertEquals(66, timed_traj.length());
    }

    /**
     * This *just* turns in place, obviously it doesn't work at all with the
     * distance sampler so this is just the index sampler.
     * 
     * TODO: this fails anyway
     */
    // @Test
    void testJustTurningInPlace() {
        Path100 traj = new Path100(Arrays.asList(
                new Pose2dWithMotion(
                        new Pose2d(
                                new Translation2d(0.0, 0.0),
                                GeometryUtil.kRotationZero),
                        new Twist2d(0, 0, 1), 0, 0),
                new Pose2dWithMotion(
                        new Pose2d(
                                new Translation2d(0.0, 0.0),
                                GeometryUtil.kRotation180),
                        new Twist2d(0, 0, 1), 0, 0)));
        PathIndexSampler dist_view = new PathIndexSampler(traj);

        // Triangle profile.
        Trajectory100 timed_traj = buildAndCheckTrajectory(dist_view,
                1.0,
                new ArrayList<TimingConstraint>(), 0.0, 0.0, 20.0, 5.0);
        assertEquals(66, timed_traj.length());
        assertNotNull(timed_traj);
        System.out.println(timed_traj);

        // Trapezoidal profile.
        timed_traj = buildAndCheckTrajectory(dist_view, 1.0, new ArrayList<TimingConstraint>(), 0.0, 0.0,
                10.0, 5.0);
        assertEquals(66, timed_traj.length());

        // Trapezoidal profile with start and end velocities.
        timed_traj = buildAndCheckTrajectory(dist_view, 1.0, new ArrayList<TimingConstraint>(), 5.0, 2.0,
                10.0, 5.0);
        assertEquals(66, timed_traj.length());
    }

    /**
     * The path here is just four waypoints, so sharp corners.
     * 
     * The trajectory just notices velocity and acceleration along the path, so it
     * is totally infeasible at the corners.
     * 
     * TODO: this is wrong
     */
    @Test
    void testNoConstraints() {
        Path100 traj = new Path100(kWaypoints);
        PathDistanceSampler sampler = new PathDistanceSampler(traj);

        // Triangle profile.
        Trajectory100 timed_traj = buildAndCheckTrajectory(sampler,
                1.0,
                new ArrayList<TimingConstraint>(), 0.0, 0.0, 20.0, 5.0);
        assertEquals(66, timed_traj.length());
        assertNotNull(timed_traj);

        // Trapezoidal profile.
        timed_traj = buildAndCheckTrajectory(sampler,
                1.0, new ArrayList<TimingConstraint>(),
                0.0, 0.0,
                10.0, 5.0);
        assertEquals(66, timed_traj.length());

        // Trapezoidal profile with start and end velocities.
        timed_traj = buildAndCheckTrajectory(sampler,
                1.0, new ArrayList<TimingConstraint>(),
                5.0, 2.0,
                10.0, 5.0);
        System.out.println(timed_traj);

        assertEquals(66, timed_traj.length());
    }

    /**
     * The centripetal constraint does nothing in the corners, because the
     * paths are straight; the corner "curvature" is not noticed.
     * 
     * The correct behavior is to slow to a stop at the corners.
     * 
     * TODO: this is wrong
     */
    @Test
    void testCentripetalConstraint() {
        Path100 traj = new Path100(kWaypoints);
        PathDistanceSampler sampler = new PathDistanceSampler(traj);
        SwerveKinodynamics limits = SwerveKinodynamicsFactory.get();

        // Triangle profile.
        Trajectory100 timed_traj = buildAndCheckTrajectory(sampler,
                1.0,
                List.of(new CentripetalAccelerationConstraint(limits, 1.0)), 0.0, 0.0, 20.0, 5.0);
        assertEquals(66, timed_traj.length());
        assertNotNull(timed_traj);
        System.out.println(timed_traj);

        // Trapezoidal profile.
        timed_traj = buildAndCheckTrajectory(sampler, 1.0, new ArrayList<TimingConstraint>(), 0.0, 0.0,
                10.0, 5.0);
        assertEquals(66, timed_traj.length());

        // Trapezoidal profile with start and end velocities.
        timed_traj = buildAndCheckTrajectory(sampler, 1.0, new ArrayList<TimingConstraint>(), 5.0, 2.0,
                10.0, 5.0);
        assertEquals(66, timed_traj.length());
    }

    @Test
    void testNoConstraintsIndexed() {
        Path100 traj = new Path100(kWaypoints);
        PathIndexSampler dist_view = new PathIndexSampler(traj);

        // Triangle profile.
        Trajectory100 timed_traj = buildAndCheckTrajectory(dist_view,
                0.0465, // to make 66 below
                new ArrayList<TimingConstraint>(), 0.0, 0.0, 20.0, 5.0);
        assertNotNull(timed_traj);
        assertEquals(66, timed_traj.length());
        System.out.println(timed_traj);

        // Trapezoidal profile.
        timed_traj = buildAndCheckTrajectory(dist_view,
                0.0465, // to make 66 below
                new ArrayList<TimingConstraint>(), 0.0, 0.0, 10.0, 5.0);
        assertEquals(66, timed_traj.length());

        // Trapezoidal profile with start and end velocities.
        timed_traj = buildAndCheckTrajectory(dist_view,
                0.0465, // to make 66 below
                new ArrayList<TimingConstraint>(),
                5.0, 2.0, 10.0, 5.0);
        assertEquals(66, timed_traj.length());

    }

    @Test
    void testConditionalVelocityConstraint() {
        Path100 traj = new Path100(kWaypoints);
        PathDistanceSampler dist_view = new PathDistanceSampler(traj);

        class ConditionalTimingConstraint implements TimingConstraint {
            @Override
            public NonNegativeDouble getMaxVelocity(Pose2dWithMotion state) {
                if (state.getTranslation().getX() >= 24.0) {
                    return new NonNegativeDouble(5.0);
                } else {
                    return new NonNegativeDouble(Double.POSITIVE_INFINITY);
                }
            }

            @Override
            public MinMaxAcceleration getMinMaxAcceleration(Pose2dWithMotion state,
                    double velocity) {
                return new TimingConstraint.MinMaxAcceleration(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
            }
        }

        // Trapezoidal profile.
        Trajectory100 timed_traj = buildAndCheckTrajectory(dist_view,
                1.0,
                Arrays.asList(new ConditionalTimingConstraint()), 0.0, 0.0, 10.0, 5.0);
        assertNotNull(timed_traj);
    }

    @Test
    void testConditionalVelocityConstraintIndex() {
        Path100 traj = new Path100(kWaypoints);
        PathIndexSampler dist_view = new PathIndexSampler(traj);

        class ConditionalTimingConstraint implements TimingConstraint {
            @Override
            public NonNegativeDouble getMaxVelocity(Pose2dWithMotion state) {
                if (state.getTranslation().getX() >= 24.0) {
                    return new NonNegativeDouble(5.0);
                } else {
                    return new NonNegativeDouble(Double.POSITIVE_INFINITY);
                }
            }

            @Override
            public MinMaxAcceleration getMinMaxAcceleration(Pose2dWithMotion state,
                    double velocity) {
                return new TimingConstraint.MinMaxAcceleration(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
            }
        }

        // Trapezoidal profile.
        Trajectory100 timed_traj = buildAndCheckTrajectory(dist_view,
                1.0,
                Arrays.asList(new ConditionalTimingConstraint()), 0.0, 0.0, 10.0, 5.0);
        assertNotNull(timed_traj);
    }

    @Test
    void testConditionalAccelerationConstraint() {
        Path100 traj = new Path100(kWaypoints);
        PathDistanceSampler dist_view = new PathDistanceSampler(traj);

        class ConditionalTimingConstraint implements TimingConstraint {
            @Override
            public NonNegativeDouble getMaxVelocity(Pose2dWithMotion state) {
                return new NonNegativeDouble(Double.POSITIVE_INFINITY);
            }

            @Override
            public MinMaxAcceleration getMinMaxAcceleration(Pose2dWithMotion state,
                    double velocity) {
                return new TimingConstraint.MinMaxAcceleration(-10.0, 10.0 / velocity);
            }
        }

        // Trapezoidal profile.
        Trajectory100 timed_traj = buildAndCheckTrajectory(dist_view,
                1.0,
                Arrays.asList(new ConditionalTimingConstraint()), 0.0, 0.0, 10.0, 5.0);
        assertNotNull(timed_traj);
    }

    @Test
    void testConditionalAccelerationConstraintIndex() {
        Path100 traj = new Path100(kWaypoints);
        PathIndexSampler dist_view = new PathIndexSampler(traj);

        class ConditionalTimingConstraint implements TimingConstraint {
            @Override
            public NonNegativeDouble getMaxVelocity(Pose2dWithMotion state) {
                return new NonNegativeDouble(Double.POSITIVE_INFINITY);
            }

            @Override
            public MinMaxAcceleration getMinMaxAcceleration(Pose2dWithMotion state,
                    double velocity) {
                return new TimingConstraint.MinMaxAcceleration(-10.0, 10.0 / velocity);
            }
        }

        // Trapezoidal profile.
        Trajectory100 timed_traj = buildAndCheckTrajectory(
                dist_view,
                1.0,
                Arrays.asList(new ConditionalTimingConstraint()),
                0.0, 0.0,
                10.0, 5.0);
        assertNotNull(timed_traj);
    }

    @Test
    void testAccel() {
        // average v = 0.5
        // dv = 1
        assertEquals(0.5, TimingUtil.accel(0, 1, 1.0), 0.001);
        assertEquals(1.0, TimingUtil.accel(0, 1, 0.5), 0.001);
        // average v = 1.5
        // dv = 1
        assertEquals(1.5, TimingUtil.accel(1, 2, 1.0), 0.001);
        // same case, backwards
        assertEquals(1.5, TimingUtil.accel(2, 1, -1.0), 0.001);
    }

    @Test
    void testV1() {
        // no v or a => no new v
        assertEquals(0.0, TimingUtil.v1(0, 0, 1.0));
        // no a => keep old v
        assertEquals(1.0, TimingUtil.v1(1, 0, 1.0));
        // a = 0.5 for 1 => final v is 1
        assertEquals(1.0, TimingUtil.v1(0, 0.5, 1.0));
        // same case, backwards
        assertEquals(0.0, TimingUtil.v1(1.0, 0.5, -1.0));
        // backwards with negative accel
        assertEquals(1.0, TimingUtil.v1(0.0, -0.5, -1.0));
    }

}
